package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ChangeRequestPolicy;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class OrderChangeRequestDao {
    private static final Set<String> TYPES = Set.of("CHANGE_MATERIAL", "CHANGE_COLOR", "CHANGE_QUANTITY",
            "REPLACE_DESIGN", "CHANGE_ADDRESS", "CANCEL_ITEM", "CANCEL_ORDER", "OTHER");
    private OrderChangeRequestDao() { }

    public static List<Map<String, Object>> listForCustomer(long userId) {
        String sql = "SELECT r.change_request_id,r.order_id,o.order_code,r.order_item_id,r.shop_id,r.request_type,r.reason,"
                + "r.payload_json,r.status,r.shop_response,r.price_delta,r.cancel_fee,r.customer_confirmed_at,r.expires_at,r.created_at "
                + "FROM order_change_requests r JOIN orders o ON o.order_id=r.order_id "
                + "WHERE r.customer_user_id=? ORDER BY r.created_at DESC,r.change_request_id DESC LIMIT 100";
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setLong(1,userId); try(ResultSet r=p.executeQuery()) { while(r.next()) {
                Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("change_request_id"));m.put("orderId",r.getLong("order_id"));m.put("orderCode",r.getString("order_code"));
                m.put("itemId",nullableLong(r,"order_item_id"));m.put("shopId",nullableLong(r,"shop_id"));m.put("type",r.getString("request_type"));m.put("reason",r.getString("reason"));m.put("payload",r.getString("payload_json"));m.put("status",r.getString("status"));m.put("response",r.getString("shop_response"));m.put("priceDelta",r.getBigDecimal("price_delta"));m.put("cancelFee",r.getBigDecimal("cancel_fee"));m.put("confirmedAt",dateTime(r.getTimestamp("customer_confirmed_at")));m.put("expiresAt",dateTime(r.getTimestamp("expires_at")));m.put("createdAt",dateTime(r.getTimestamp("created_at")));rows.add(m);
            }} return rows;
        } catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static List<Map<String,Object>> listForWorkshop(long actorId,long shopId,String status) {
        String sql="SELECT r.change_request_id,r.order_id,o.order_code,r.order_item_id,r.customer_user_id,u.full_name AS customer_name,r.request_type,r.reason,r.payload_json,r.status,r.shop_response,r.price_delta,r.cancel_fee,r.created_at "
                +"FROM order_change_requests r JOIN orders o ON o.order_id=r.order_id JOIN users u ON u.user_id=r.customer_user_id JOIN print_shops s ON s.shop_id=r.shop_id "
                +"WHERE s.shop_id=? AND s.owner_user_id=? AND (? IS NULL OR r.status=?) ORDER BY r.created_at DESC,r.change_request_id DESC LIMIT 100";
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,shopId);p.setLong(2,actorId);p.setString(3,blankToNull(status));p.setString(4,blankToNull(status));try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("change_request_id"));m.put("orderId",r.getLong("order_id"));m.put("orderCode",r.getString("order_code"));m.put("itemId",nullableLong(r,"order_item_id"));m.put("customer",r.getString("customer_name"));m.put("type",r.getString("request_type"));m.put("reason",r.getString("reason"));m.put("payload",r.getString("payload_json"));m.put("status",r.getString("status"));m.put("response",r.getString("shop_response"));m.put("priceDelta",r.getBigDecimal("price_delta"));m.put("cancelFee",r.getBigDecimal("cancel_fee"));m.put("createdAt",dateTime(r.getTimestamp("created_at")));rows.add(m);}}return rows;}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static long create(long userId,String orderCode,Long itemId,String type,String reason,String proposedValue) {
        if(type==null||!TYPES.contains(type))throw new IllegalArgumentException("Loại yêu cầu không hợp lệ.");
        if(reason==null||reason.trim().length()<3||reason.trim().length()>1000)throw new IllegalArgumentException("Lý do phải có từ 3 đến 1000 ký tự.");
        if(proposedValue!=null&&proposedValue.length()>1800)throw new IllegalArgumentException("Nội dung đề xuất quá dài.");
        long requestId=-1; Long shopId=null; boolean appliedAutomatically=false; String orderStatus=null;
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            try{
                long orderId;String paymentStatus;
                try(PreparedStatement p=c.prepareStatement("SELECT order_id,status,payment_status FROM orders WHERE order_code=? AND user_id=? FOR UPDATE")){p.setString(1,orderCode);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn.");orderId=r.getLong("order_id");orderStatus=r.getString("status");paymentStatus=r.getString("payment_status");}}
                if(OrderStatus.COMPLETED.equals(orderStatus)||OrderStatus.CANCELLED.equals(orderStatus)||OrderStatus.SHIPPED.equals(orderStatus))throw new IllegalArgumentException("Đơn hàng đã hoàn tất/hủy/giao hàng; vui lòng tạo ticket hỗ trợ hoặc đổi trả.");
                if(itemId!=null){try(PreparedStatement p=c.prepareStatement("SELECT shop_id FROM order_items WHERE order_item_id=? AND order_id=? FOR UPDATE")){p.setLong(1,itemId);p.setLong(2,orderId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Sản phẩm được chọn không thuộc đơn này.");long sid=r.getLong(1);shopId=r.wasNull()?null:sid;}}}
                else if(!"CANCEL_ORDER".equals(type)&&!"CHANGE_ADDRESS".equals(type)&&!"OTHER".equals(type))throw new IllegalArgumentException("Vui lòng chọn sản phẩm liên quan.");
                if(shopId==null){try(PreparedStatement p=c.prepareStatement("SELECT shop_id FROM order_items WHERE order_id=? AND shop_id IS NOT NULL ORDER BY order_item_id LIMIT 1")){p.setLong(1,orderId);try(ResultSet r=p.executeQuery()){if(r.next()){long sid=r.getLong(1);shopId=r.wasNull()?null:sid;}}}}
                try(PreparedStatement p=c.prepareStatement("SELECT change_request_id FROM order_change_requests WHERE order_id=? AND COALESCE(order_item_id,0)=COALESCE(?,0) AND request_type=? AND status='PENDING' LIMIT 1")){p.setLong(1,orderId);if(itemId==null)p.setNull(2,java.sql.Types.BIGINT);else p.setLong(2,itemId);p.setString(3,type);try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalArgumentException("Đã có yêu cầu cùng loại đang chờ xử lý cho đơn/sản phẩm này.");}}
                String stage=null;int progress=0;boolean hasUpdates=false;
                try(PreparedStatement p=c.prepareStatement("SELECT pu.stage,pu.progress_percent FROM production_updates pu JOIN order_items oi ON oi.order_item_id=pu.order_item_id WHERE oi.order_id=? ORDER BY pu.created_at DESC,pu.production_update_id DESC LIMIT 1")){p.setLong(1,orderId);try(ResultSet r=p.executeQuery()){if(r.next()){hasUpdates=true;stage=r.getString(1);progress=r.getInt(2);}}}
                ChangeRequestPolicy.Decision decision=ChangeRequestPolicy.decide(hasUpdates,stage,progress);
                if(decision==ChangeRequestPolicy.Decision.BLOCKED)throw new IllegalArgumentException("Đơn hàng đã sẵn sàng giao hoặc đã giao; vui lòng tạo ticket hỗ trợ/đổi trả.");
                boolean canAutoCancel="CANCEL_ORDER".equals(type)&&!hasUpdates&&OrderStatus.canTransition(orderStatus,OrderStatus.CANCELLED);
                boolean canAutoAddress="CHANGE_ADDRESS".equals(type)&&!hasUpdates;
                if(canAutoAddress&&(proposedValue==null||!proposedValue.trim().matches("\\d{1,18}")))throw new IllegalArgumentException("Đề xuất đổi địa chỉ phải là mã địa chỉ đã lưu của bạn.");
                String payload=Json.stringify(Map.of("proposedValue",proposedValue==null?"":proposedValue.trim(),"policy",decision.name()));
                String sql="INSERT INTO order_change_requests(order_id,order_item_id,customer_user_id,shop_id,request_type,reason,payload_json,status,expires_at,created_at,updated_at) VALUES(?,?,?,?,?,?,?, ?,NOW()+INTERVAL 24 HOUR,NOW(),NOW())";
                try(PreparedStatement p=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){p.setLong(1,orderId);if(itemId==null)p.setNull(2,java.sql.Types.BIGINT);else p.setLong(2,itemId);p.setLong(3,userId);if(shopId==null)p.setNull(4,java.sql.Types.BIGINT);else p.setLong(4,shopId);p.setString(5,type);p.setString(6,reason.trim());p.setString(7,payload);p.setString(8,(canAutoCancel||canAutoAddress)?"APPLIED":"PENDING");p.executeUpdate();try(ResultSet keys=p.getGeneratedKeys()){if(keys.next())requestId=keys.getLong(1);}}
                if(canAutoCancel){applyOrderCancellation(c,orderId,userId,reason.trim(),paymentStatus,orderStatus);appliedAutomatically=true;}
                else if(canAutoAddress){applyAddressChange(c,orderId,userId,Long.parseLong(proposedValue.trim()));appliedAutomatically=true;}
                c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}finally{c.setAutoCommit(true);}
        }catch(SQLException e){throw DataAccessException.of(e);}
        if(shopId!=null)notifyShop(shopId,userId,requestId,appliedAutomatically);
        return requestId;
    }

    public static void withdraw(long requestId,long userId){
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE order_change_requests SET status='WITHDRAWN',updated_at=NOW() WHERE change_request_id=? AND customer_user_id=? AND status='PENDING'")){p.setLong(1,requestId);p.setLong(2,userId);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Chỉ có thể rút yêu cầu đang chờ xử lý của chính bạn.");}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void respond(long actorId,long shopId,long requestId,String action,String note,BigDecimal priceDelta,BigDecimal cancelFee){
        if(!"ACCEPTED".equals(action)&&!"REJECTED".equals(action))throw new IllegalArgumentException("Phản hồi không hợp lệ.");
        if(priceDelta==null||cancelFee==null||priceDelta.abs().compareTo(new BigDecimal("1000000000"))>0||cancelFee.compareTo(BigDecimal.ZERO)<0||cancelFee.compareTo(new BigDecimal("1000000000"))>0)throw new IllegalArgumentException("Phụ phí hoặc phí hủy không hợp lệ.");
        if(note!=null&&note.length()>1000)throw new IllegalArgumentException("Phản hồi tối đa 1000 ký tự.");
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            long orderId;Long itemId;String type;String status;String stage=null;int progress=0;boolean hasUpdates=false;
            try(PreparedStatement p=c.prepareStatement("SELECT r.order_id,r.order_item_id,r.request_type,r.status FROM order_change_requests r JOIN print_shops s ON s.shop_id=r.shop_id WHERE r.change_request_id=? AND r.shop_id=? AND s.owner_user_id=? FOR UPDATE")){p.setLong(1,requestId);p.setLong(2,shopId);p.setLong(3,actorId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Yêu cầu không tồn tại hoặc không thuộc xưởng của bạn.");orderId=r.getLong(1);long rawItem=r.getLong(2);itemId=r.wasNull()?null:rawItem;type=r.getString(3);status=r.getString(4);}}
            if(!"PENDING".equals(status))throw new IllegalArgumentException("Yêu cầu này không còn ở trạng thái chờ xử lý.");
            try(PreparedStatement p=c.prepareStatement("SELECT pu.stage,pu.progress_percent FROM production_updates pu JOIN order_items oi ON oi.order_item_id=pu.order_item_id WHERE oi.order_id=? ORDER BY pu.created_at DESC,pu.production_update_id DESC LIMIT 1")){p.setLong(1,orderId);try(ResultSet r=p.executeQuery()){if(r.next()){hasUpdates=true;stage=r.getString(1);progress=r.getInt(2);}}}
            ChangeRequestPolicy.Decision decision=ChangeRequestPolicy.decide(hasUpdates,stage,progress);
            if("ACCEPTED".equals(action)&&decision==ChangeRequestPolicy.Decision.BLOCKED)throw new IllegalArgumentException("Công đoạn hiện tại không cho phép chấp nhận yêu cầu.");
            String sql="UPDATE order_change_requests SET status=?,shop_response=?,price_delta=?,cancel_fee=?,updated_at=NOW(),expires_at=NULL WHERE change_request_id=? AND status='PENDING'";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setString(1,action);p.setString(2,note==null?null:note.trim());p.setBigDecimal(3,priceDelta);p.setBigDecimal(4,cancelFee);p.setLong(5,requestId);if(p.executeUpdate()!=1)throw new SQLException("Yêu cầu đã được thay đổi đồng thời.");}
            c.commit();
        }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException r){e.addSuppressed(r);}throw e;}finally{c.setAutoCommit(true);}}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void confirm(long requestId,long userId){
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            long orderId;Long itemId;String type;String status;String payload;String note;String paymentStatus;
            try(PreparedStatement p=c.prepareStatement("SELECT r.order_id,r.order_item_id,r.request_type,r.status,r.payload_json,r.reason,o.status,o.payment_status FROM order_change_requests r JOIN orders o ON o.order_id=r.order_id WHERE r.change_request_id=? AND r.customer_user_id=? FOR UPDATE")){p.setLong(1,requestId);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Yêu cầu không tồn tại hoặc không thuộc tài khoản của bạn.");orderId=r.getLong(1);long raw=r.getLong(2);itemId=r.wasNull()?null:raw;type=r.getString(3);status=r.getString(4);payload=r.getString(5);note=r.getString(6);paymentStatus=r.getString(8);}}
            if(!"ACCEPTED".equals(status))throw new IllegalArgumentException("Chỉ xác nhận yêu cầu đã được xưởng chấp nhận.");
            if("CANCEL_ORDER".equals(type)){String current=OrderDao.lockStatus(c,orderId,userId);if(current==null||!OrderStatus.canTransition(current,OrderStatus.CANCELLED))throw new IllegalArgumentException("Trạng thái đơn hàng không còn cho phép hủy.");applyOrderCancellation(c,orderId,userId,note,paymentStatus,current);}
            else if("CANCEL_ITEM".equals(type)&&itemId!=null){applyItemCancellation(c,orderId,itemId,userId);}
            else if("CHANGE_ADDRESS".equals(type)){String proposed=extractProposed(payload);if(proposed==null||!proposed.matches("\\d{1,18}"))throw new IllegalArgumentException("Yêu cầu không có địa chỉ mới hợp lệ.");applyAddressChange(c,orderId,userId,Long.parseLong(proposed));}
            else {
                try(PreparedStatement p=c.prepareStatement("UPDATE order_change_requests SET customer_confirmed_at=NOW(),updated_at=NOW() WHERE change_request_id=? AND customer_user_id=? AND status='ACCEPTED'")){p.setLong(1,requestId);p.setLong(2,userId);p.executeUpdate();}
                c.commit();return;
            }
            try(PreparedStatement p=c.prepareStatement("UPDATE order_change_requests SET status='APPLIED',customer_confirmed_at=NOW(),updated_at=NOW() WHERE change_request_id=? AND customer_user_id=? AND status='ACCEPTED'")){p.setLong(1,requestId);p.setLong(2,userId);p.executeUpdate();}
            c.commit();
        }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException r){e.addSuppressed(r);}throw e;}finally{c.setAutoCommit(true);}}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    private static void applyOrderCancellation(Connection c,long orderId,long userId,String reason,String paymentStatus,String currentStatus)throws SQLException{
        boolean paid=OrderStatus.PAYMENT_PAID.equalsIgnoreCase(paymentStatus)||hasPaid(c,orderId);
        String pstatus=paid?"Cần hoàn tiền":OrderStatus.PAYMENT_CANCELLED;
        try(PreparedStatement p=c.prepareStatement("UPDATE orders SET status=?,payment_status=?,cancel_reason=?,cancelled_at=NOW() WHERE order_id=? AND user_id=?")){p.setString(1,OrderStatus.CANCELLED);p.setString(2,pstatus);p.setString(3,clip(reason,500));p.setLong(4,orderId);p.setLong(5,userId);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Không thể hủy đơn hàng.");}
        try(PreparedStatement p=c.prepareStatement("UPDATE order_items SET status=? WHERE order_id=?")){p.setString(1,OrderStatus.CANCELLED);p.setLong(2,orderId);p.executeUpdate();}
        try(PreparedStatement p=c.prepareStatement("UPDATE payments SET status='CANCELLED',updated_at=NOW() WHERE order_id=? AND UPPER(status) IN ('PENDING','CREATED','INITIATED')")){p.setLong(1,orderId);p.executeUpdate();}
        if(paid)try(PreparedStatement p=c.prepareStatement("UPDATE payments SET status='REFUND_REQUIRED',updated_at=NOW() WHERE order_id=? AND UPPER(status) IN ('PAID','SUCCESS','COMPLETED')")){p.setLong(1,orderId);p.executeUpdate();}
        try(PreparedStatement p=c.prepareStatement("INSERT INTO order_status_history(order_id,changed_by_user_id,old_status,new_status,note,changed_at) VALUES(?,?,?,?,?,NOW())")){p.setLong(1,orderId);p.setLong(2,userId);p.setString(3,currentStatus);p.setString(4,OrderStatus.CANCELLED);p.setString(5,clip("Yêu cầu hủy được áp dụng: "+reason,500));p.executeUpdate();}
    }

    private static void applyItemCancellation(Connection c,long orderId,long itemId,long userId)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("UPDATE order_items SET status=? WHERE order_item_id=? AND order_id=? AND status<>?")){p.setString(1,OrderStatus.CANCELLED);p.setLong(2,itemId);p.setLong(3,orderId);p.setString(4,OrderStatus.CANCELLED);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Sản phẩm đã bị hủy hoặc không thuộc đơn hàng.");}
        BigDecimal subtotal=BigDecimal.ZERO;int remaining=0;
        try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*),COALESCE(SUM(line_total),0) FROM order_items WHERE order_id=? AND status<>?")){p.setLong(1,orderId);p.setString(2,OrderStatus.CANCELLED);try(ResultSet r=p.executeQuery()){if(r.next()){remaining=r.getInt(1);subtotal=r.getBigDecimal(2);}}}
        BigDecimal shipping=new BigDecimal("30000");if(subtotal.compareTo(new BigDecimal("2000000"))>=0)shipping=BigDecimal.ZERO;
        try(PreparedStatement p=c.prepareStatement("UPDATE orders SET subtotal=?,shipping_fee=?,total_amount=?,payment_status=CASE WHEN payment_status=? THEN 'Cần hoàn tiền' ELSE payment_status END, status=CASE WHEN ?=0 THEN ? ELSE status END, cancelled_at=CASE WHEN ?=0 THEN NOW() ELSE cancelled_at END WHERE order_id=? AND user_id=?")){p.setBigDecimal(1,subtotal);p.setBigDecimal(2,shipping);p.setBigDecimal(3,subtotal.add(shipping));p.setString(4,OrderStatus.PAYMENT_PAID);p.setInt(5,remaining);p.setString(6,OrderStatus.CANCELLED);p.setInt(7,remaining);p.setLong(8,orderId);p.setLong(9,userId);p.executeUpdate();}
        if(remaining==0){try(PreparedStatement p=c.prepareStatement("UPDATE payments SET status='REFUND_REQUIRED',updated_at=NOW() WHERE order_id=? AND UPPER(status) IN ('PAID','SUCCESS','COMPLETED')")){p.setLong(1,orderId);p.executeUpdate();}}
    }

    private static void applyAddressChange(Connection c,long orderId,long userId,long addressId)throws SQLException{
        String recipient,phone,address;
        BigDecimal lat,lon;String note;
        try(PreparedStatement p=c.prepareStatement("SELECT recipient_name,recipient_phone,province,district,ward,address_line,formatted_address,latitude,longitude,delivery_note FROM addresses WHERE address_id=? AND user_id=? AND is_deleted=0 FOR UPDATE")){p.setLong(1,addressId);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Địa chỉ mới không tồn tại hoặc không thuộc tài khoản của bạn.");recipient=r.getString("recipient_name");phone=r.getString("recipient_phone");String formatted=r.getString("formatted_address");if(formatted!=null&&!formatted.isBlank())address=formatted;else address=joinAddress(r.getString("address_line"),r.getString("ward"),r.getString("district"),r.getString("province"));lat=r.getBigDecimal("latitude");lon=r.getBigDecimal("longitude");note=r.getString("delivery_note");}}
        try(PreparedStatement p=c.prepareStatement("UPDATE orders SET address_id=?,recipient_name_snapshot=?,recipient_phone_snapshot=?,shipping_address_snapshot=?,shipping_latitude=?,shipping_longitude=?,delivery_note_snapshot=? WHERE order_id=? AND user_id=?")){p.setLong(1,addressId);p.setString(2,recipient);p.setString(3,phone);p.setString(4,address);p.setBigDecimal(5,lat);p.setBigDecimal(6,lon);p.setString(7,note);p.setLong(8,orderId);p.setLong(9,userId);p.executeUpdate();}
    }

    private static boolean hasPaid(Connection c,long orderId)throws SQLException{try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM payments WHERE order_id=? AND UPPER(status) IN ('PAID','SUCCESS','COMPLETED') LIMIT 1")){p.setLong(1,orderId);try(ResultSet r=p.executeQuery()){return r.next();}}}
    private static String extractProposed(String payload){if(payload==null)return null;java.util.regex.Matcher m=java.util.regex.Pattern.compile("\\\"proposedValue\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").matcher(payload);return m.find()?m.group(1):null;}
    private static void notifyShop(long shopId,long userId,long requestId,boolean applied){try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT owner_user_id FROM print_shops WHERE shop_id=? AND owner_user_id IS NOT NULL")){p.setLong(1,shopId);try(ResultSet r=p.executeQuery()){if(r.next()){long owner=r.getLong(1);NotificationService.notifyUser(owner,"ORDER_CHANGE_REQUEST",applied?"Yêu cầu đã tự áp dụng":"Yêu cầu thay đổi đơn mới", "Yêu cầu #"+requestId+" đã được khách gửi.","/workshop/orders","ORDER_CHANGE_REQUEST",requestId);}}}catch(SQLException e){throw DataAccessException.of(e);}}
    private static Long nullableLong(ResultSet r,String col)throws SQLException{long v=r.getLong(col);return r.wasNull()?null:v;}
    private static String dateTime(Timestamp t){return t==null?"":t.toLocalDateTime().toString().replace('T',' ');}
    private static String blankToNull(String s){return s==null||s.isBlank()?null:s.trim();}
    private static String clip(String value,int max){if(value==null)return null;String v=value.trim();return v.length()<=max?v:v.substring(0,max);}
    private static String joinAddress(String...parts){List<String> v=new ArrayList<>();for(String p:parts)if(p!=null&&!p.isBlank())v.add(p.trim());return String.join(", ",v);}
}
