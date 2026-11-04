package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** DAO dành riêng cho màn hình khách hàng. Mọi truy vấn đơn/địa chỉ đều khóa theo user_id. */
public final class CustomerAccountDao {
    private CustomerAccountDao() { }

    public static Map<String,Object> dashboard(long userId) {
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("totalOrders", scalar("SELECT COUNT(*) FROM orders WHERE user_id=?", userId));
        result.put("processingOrders", scalar("SELECT COUNT(*) FROM orders WHERE user_id=? AND status NOT IN (?,?)", userId,
                OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        result.put("completedOrders", scalar("SELECT COUNT(*) FROM orders WHERE user_id=? AND status=?", userId, OrderStatus.COMPLETED));
        result.put("customRequests", scalar("SELECT COUNT(*) FROM custom_print_requests WHERE user_id=?", userId));
        result.put("latestOrders", listOrders(userId, "", 1, 5));
        result.put("defaultAddress", defaultAddress(userId).orElse(null));
        return result;
    }

    public static List<Map<String,Object>> listAddresses(long userId) {
        String sql = "SELECT address_id,user_id,recipient_name,recipient_phone,province,district,ward,address_line,"
                + "postal_code,label,is_default,delivery_note,created_at FROM addresses "
                + "WHERE user_id=? AND COALESCE(is_deleted,0)=0 ORDER BY is_default DESC,address_id DESC";
        return queryMaps(sql, p -> p.setLong(1, userId));
    }

    public static Optional<Map<String,Object>> addressById(long userId, long addressId) {
        String sql = "SELECT address_id,user_id,recipient_name,recipient_phone,province,district,ward,address_line,postal_code,label,is_default,delivery_note "
                + "FROM addresses WHERE user_id=? AND address_id=? AND COALESCE(is_deleted,0)=0";
        return queryMaps(sql, p -> { p.setLong(1,userId); p.setLong(2,addressId); }).stream().findFirst();
    }

    public static Optional<Map<String,Object>> defaultAddress(long userId) {
        String sql = "SELECT address_id,recipient_name,recipient_phone,province,district,ward,address_line,label,delivery_note "
                + "FROM addresses WHERE user_id=? AND COALESCE(is_deleted,0)=0 ORDER BY is_default DESC,address_id DESC LIMIT 1";
        List<Map<String,Object>> found = queryMaps(sql, p -> p.setLong(1,userId));
        return found.stream().findFirst();
    }

    public static void saveAddress(long userId, long addressId, Map<String,String> form) {
        String recipient = required(form.get("recipientName"), "Tên người nhận", 120);
        String phone = required(form.get("recipientPhone"), "Số điện thoại", 25);
        if (!phone.matches("[0-9+ .()-]{8,25}")) throw new IllegalArgumentException("Số điện thoại người nhận không hợp lệ.");
        String province = required(form.get("province"), "Tỉnh/thành", 100);
        String district = required(form.get("district"), "Quận/huyện", 100);
        String ward = clean(form.get("ward"), 100);
        String line = required(form.get("addressLine"), "Địa chỉ cụ thể", 255);
        String label = allowedLabel(form.get("label"));
        String note = clean(form.get("deliveryNote"), 500);
        boolean makeDefault = "1".equals(form.get("isDefault")) || "true".equalsIgnoreCase(form.get("isDefault"));

        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                lockUser(c, userId);
                if (addressId > 0) {
                    if (!ownsAddress(c,addressId,userId)) throw new DataAccessException("Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn.");
                    try (PreparedStatement p = c.prepareStatement("UPDATE addresses SET recipient_name=?,recipient_phone=?,province=?,district=?,ward=?,address_line=?,label=?,delivery_note=?,is_default=?,updated_at=NOW() WHERE address_id=? AND user_id=? AND COALESCE(is_deleted,0)=0")) {
                        bindAddressValues(p,recipient,phone,province,district,ward,line,label,note);
                        p.setBoolean(9,makeDefault); p.setLong(10,addressId); p.setLong(11,userId); p.executeUpdate();
                    }
                } else {
                    try (PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM addresses WHERE user_id=? AND COALESCE(is_deleted,0)=0")) {
                        p.setLong(1,userId); try (ResultSet r=p.executeQuery()) {
                            if (r.next() && r.getInt(1) >= 20) throw new DataAccessException("Mỗi tài khoản được lưu tối đa 20 địa chỉ.");
                        }
                    }
                    try (PreparedStatement p = c.prepareStatement("INSERT INTO addresses(user_id,recipient_name,recipient_phone,province,district,ward,address_line,label,delivery_note,is_default,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,NOW(),NOW())")) {
                        p.setLong(1,userId); p.setString(2,recipient); p.setString(3,phone); p.setString(4,province);
                        p.setString(5,district); p.setString(6,ward); p.setString(7,line); p.setString(8,label);
                        p.setString(9,note); p.setBoolean(10,makeDefault); p.executeUpdate();
                    }
                }
                if (makeDefault) normalizeDefault(c,userId,addressId);
                else ensureOneDefault(c,userId);
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static boolean deleteAddress(long userId, long addressId) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                lockUser(c,userId);
                try (PreparedStatement p = c.prepareStatement("UPDATE addresses SET is_deleted=1,is_default=0,updated_at=NOW() WHERE address_id=? AND user_id=? AND COALESCE(is_deleted,0)=0")) {
                    p.setLong(1,addressId); p.setLong(2,userId);
                    if (p.executeUpdate()!=1) { c.rollback(); return false; }
                }
                ensureOneDefault(c,userId); c.commit(); return true;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static boolean setDefaultAddress(long userId, long addressId) {
        try (Connection c = Database.getConnection()) {
            c.setAutoCommit(false);
            try {
                lockUser(c,userId);
                if (!ownsAddress(c,addressId,userId)) { c.rollback(); return false; }
                try (PreparedStatement p = c.prepareStatement("UPDATE addresses SET is_default=0 WHERE user_id=? AND COALESCE(is_deleted,0)=0")) {
                    p.setLong(1,userId); p.executeUpdate();
                }
                try (PreparedStatement p = c.prepareStatement("UPDATE addresses SET is_default=1,updated_at=NOW() WHERE address_id=? AND user_id=? AND COALESCE(is_deleted,0)=0")) {
                    p.setLong(1,addressId); p.setLong(2,userId); p.executeUpdate();
                }
                c.commit(); return true;
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static long countOrders(long userId, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM orders WHERE user_id=?");
        boolean filter = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);
        if (filter) sql.append(" AND status=?");
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(sql.toString())) {
            p.setLong(1,userId); if(filter)p.setString(2,status);
            try(ResultSet r=p.executeQuery()){return r.next()?r.getLong(1):0;}
        } catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static List<Map<String,Object>> listOrders(long userId, String status, int page, int pageSize) {
        int size = Math.max(1,Math.min(pageSize,50));
        int offset = Math.max(0,page-1)*size;
        boolean filter = status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status);
        String sql = "SELECT order_id,order_code,status,payment_status,shipping_status,total_amount,currency,placed_at,completed_at,cancelled_at "
                + "FROM orders WHERE user_id=?" + (filter?" AND status=?":"") + " ORDER BY placed_at DESC,order_id DESC LIMIT ? OFFSET ?";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            int i=1;p.setLong(i++,userId);if(filter)p.setString(i++,status);p.setInt(i++,size);p.setInt(i,offset);
            try(ResultSet r=p.executeQuery()){
                List<Map<String,Object>> rows=new ArrayList<>();while(r.next()){
                    Map<String,Object> m=new LinkedHashMap<>();m.put("orderId",r.getLong("order_id"));m.put("orderCode",r.getString("order_code"));
                    m.put("status",r.getString("status"));m.put("paymentStatus",r.getString("payment_status"));m.put("shippingStatus",r.getString("shipping_status"));
                    m.put("totalAmount",r.getBigDecimal("total_amount"));m.put("currency",r.getString("currency"));m.put("placedAt",r.getTimestamp("placed_at"));
                    m.put("completedAt",r.getTimestamp("completed_at"));m.put("cancelledAt",r.getTimestamp("cancelled_at"));
                    m.put("itemCount",countOrderItems(c,r.getLong("order_id")));rows.add(m);
                }return rows;
            }
        }catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static Optional<Map<String,Object>> orderDetail(long userId, String orderCode) {
        String sql = "SELECT order_id,order_code,status,payment_status,shipping_status,subtotal,shipping_fee,total_amount,currency,"
                + "recipient_name_snapshot,recipient_phone_snapshot,shipping_address_snapshot,customer_note,placed_at,completed_at,cancelled_at,cancel_reason "
                + "FROM orders WHERE order_code=? AND user_id=? LIMIT 1";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setString(1,orderCode);p.setLong(2,userId);
            try(ResultSet r=p.executeQuery()){
                if(!r.next())return Optional.empty();
                long orderId=r.getLong("order_id");Map<String,Object> o=new LinkedHashMap<>();
                o.put("orderId",orderId);o.put("orderCode",r.getString("order_code"));o.put("status",r.getString("status"));
                o.put("paymentStatus",r.getString("payment_status"));o.put("shippingStatus",r.getString("shipping_status"));
                o.put("subtotal",r.getBigDecimal("subtotal"));o.put("shippingFee",r.getBigDecimal("shipping_fee"));o.put("totalAmount",r.getBigDecimal("total_amount"));
                o.put("currency",r.getString("currency"));o.put("recipientName",r.getString("recipient_name_snapshot"));
                o.put("recipientPhone",r.getString("recipient_phone_snapshot"));o.put("shippingAddress",r.getString("shipping_address_snapshot"));
                o.put("customerNote",r.getString("customer_note"));o.put("placedAt",r.getTimestamp("placed_at"));o.put("completedAt",r.getTimestamp("completed_at"));
                o.put("cancelledAt",r.getTimestamp("cancelled_at"));o.put("cancelReason",r.getString("cancel_reason"));
                o.put("items",orderItems(c,orderId));o.put("timeline",orderTimeline(c,orderId));
                o.put("canCancel",OrderStatus.cancellableByCustomer(r.getString("status")));
                return Optional.of(o);
            }
        }catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static boolean cancelOrder(long userId, String orderCode, String reason) {
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            try{
                long orderId;String oldStatus;String paymentStatus;
                try(PreparedStatement p=c.prepareStatement("SELECT order_id,status,payment_status FROM orders WHERE order_code=? AND user_id=? FOR UPDATE")){
                    p.setString(1,orderCode);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){
                        if(!r.next()){c.rollback();return false;}orderId=r.getLong(1);oldStatus=r.getString(2);paymentStatus=r.getString(3);
                    }
                }
                if(!OrderStatus.cancellableByCustomer(oldStatus)){c.rollback();return false;}
                try(PreparedStatement p=c.prepareStatement("UPDATE orders SET status=?,payment_status=CASE WHEN payment_status=? THEN 'Chờ hoàn tiền' ELSE payment_status END,cancel_reason=?,cancelled_at=NOW() WHERE order_id=? AND user_id=?")){
                    p.setString(1,OrderStatus.CANCELLED);p.setString(2,OrderStatus.PAYMENT_PAID);p.setString(3,clean(reason,500));p.setLong(4,orderId);p.setLong(5,userId);p.executeUpdate();
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO order_status_history(order_id,changed_by_user_id,old_status,new_status,note,changed_at) VALUES(?,?,?,?,?,NOW())")){
                    p.setLong(1,orderId);p.setLong(2,userId);p.setString(3,oldStatus);p.setString(4,OrderStatus.CANCELLED);p.setString(5,"Khách hàng hủy đơn: "+(clean(reason,300)==null?"Không ghi lý do":clean(reason,300)));p.executeUpdate();
                }
                c.commit();return true;
            }catch(SQLException|RuntimeException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}
        }catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static List<Map<String,Object>> listCustomRequests(long userId) {
        String sql="SELECT r.custom_request_id,r.request_code,r.material,r.technology technology,r.quantity,r.status,r.quoted_amount,r.created_at,"
                +"f.original_filename,f.design_file_id,f.width_mm,f.height_mm,f.depth_mm,f.source_unit FROM custom_print_requests r "
                +"JOIN design_files f ON f.design_file_id=r.design_file_id AND f.user_id=r.user_id "
                +"WHERE r.user_id=? AND f.deleted_at IS NULL "
                +"ORDER BY r.created_at DESC,r.custom_request_id DESC";
        return queryMaps(sql,p->p.setLong(1,userId));
    }

    public static Map<String,Object> createCustomRequest(long userId, String originalName, String storageUrl,
            String mimeType, long fileSize, String sha256, String material, String technology,
            int quantity, BigDecimal width, BigDecimal height, BigDecimal depth, String note) {
        throw new IllegalArgumentException("Use the /design workflow to upload, analyze and route a model to a workshop.");
    }

    public static Optional<Map<String,Object>> customRequest(long userId,long requestId) {
        String sql="SELECT r.custom_request_id,r.request_code,r.design_file_id,r.material,r.technology technology,r.quantity,"
                +"f.width_mm,f.height_mm,f.depth_mm,f.source_unit,r.customer_note,r.status,r.quoted_amount,"
                +"r.seller_response_note admin_note,r.created_at,r.updated_at,f.original_filename,f.storage_url,f.file_size_bytes "
                +"FROM custom_print_requests r JOIN design_files f ON f.design_file_id=r.design_file_id AND f.user_id=r.user_id "
                +"WHERE r.custom_request_id=? AND r.user_id=? AND f.deleted_at IS NULL LIMIT 1";
        List<Map<String,Object>> rows=queryMaps(sql,p->{p.setLong(1,requestId);p.setLong(2,userId);});
        return rows.stream().findFirst();
    }

    public static Optional<Map<String,Object>> ownedDesignFile(long userId,long designFileId) {
        String sql="SELECT design_file_id,storage_url,original_filename,mime_type FROM design_files WHERE design_file_id=? AND user_id=? AND deleted_at IS NULL";
        return queryMaps(sql,p->{p.setLong(1,designFileId);p.setLong(2,userId);}).stream().findFirst();
    }

    private static long scalar(String sql,Object... values){
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            for(int i=0;i<values.length;i++)p.setObject(i+1,values[i]);try(ResultSet r=p.executeQuery()){return r.next()?r.getLong(1):0;}
        }catch(SQLException e){throw DataAccessException.of(e);}
    }
    private interface Binder{void bind(PreparedStatement p)throws SQLException;}
    private static List<Map<String,Object>> queryMaps(String sql,Binder binder){
        List<Map<String,Object>> rows=new ArrayList<>();
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            binder.bind(p);try(ResultSet r=p.executeQuery()){
                ResultSetMetaData md=r.getMetaData();int n=md.getColumnCount();
                while(r.next()){Map<String,Object> m=new LinkedHashMap<>();for(int i=1;i<=n;i++)m.put(md.getColumnLabel(i),r.getObject(i));rows.add(m);}
            }return rows;
        }catch(SQLException e){throw DataAccessException.of(e);}
    }
    private static void bindAddressValues(PreparedStatement p,String name,String phone,String province,String district,String ward,String line,String label,String note)throws SQLException{
        p.setString(1,name);p.setString(2,phone);p.setString(3,province);p.setString(4,district);p.setString(5,ward);p.setString(6,line);p.setString(7,label);p.setString(8,note);
    }
    private static void lockUser(Connection c,long userId)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT user_id FROM users WHERE user_id=? AND deleted_at IS NULL FOR UPDATE")){
            p.setLong(1,userId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new DataAccessException("Tài khoản không tồn tại.");}
        }
    }
    private static boolean ownsAddress(Connection c,long addressId,long userId)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT address_id FROM addresses WHERE address_id=? AND user_id=? AND COALESCE(is_deleted,0)=0 FOR UPDATE")){
            p.setLong(1,addressId);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){return r.next();}
        }
    }
    private static void normalizeDefault(Connection c,long userId,long addressId)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("UPDATE addresses SET is_default=0 WHERE user_id=? AND COALESCE(is_deleted,0)=0")){p.setLong(1,userId);p.executeUpdate();}
        long selected=addressId;
        if(selected<=0){try(PreparedStatement p=c.prepareStatement("SELECT address_id FROM addresses WHERE user_id=? AND COALESCE(is_deleted,0)=0 ORDER BY address_id DESC LIMIT 1")){p.setLong(1,userId);try(ResultSet r=p.executeQuery()){if(r.next())selected=r.getLong(1);}}}
        if(selected>0){try(PreparedStatement p=c.prepareStatement("UPDATE addresses SET is_default=1,updated_at=NOW() WHERE address_id=? AND user_id=? AND COALESCE(is_deleted,0)=0")){p.setLong(1,selected);p.setLong(2,userId);p.executeUpdate();}}
    }
    private static void ensureOneDefault(Connection c,long userId)throws SQLException{
        int count=0;
        try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM addresses WHERE user_id=? AND is_default=1 AND COALESCE(is_deleted,0)=0")){
            p.setLong(1,userId);try(ResultSet r=p.executeQuery()){if(r.next())count=r.getInt(1);}
        }
        // Dọn dữ liệu cũ nếu trước đây một user có nhiều địa chỉ mặc định.
        if(count!=1){
            long selected=0;
            try(PreparedStatement p=c.prepareStatement("SELECT address_id FROM addresses WHERE user_id=? AND is_default=1 AND COALESCE(is_deleted,0)=0 ORDER BY address_id DESC LIMIT 1")){
                p.setLong(1,userId);try(ResultSet r=p.executeQuery()){if(r.next())selected=r.getLong(1);}
            }
            normalizeDefault(c,userId,selected);
        }
    }
    private static int countOrderItems(Connection c,long orderId)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM order_items WHERE order_id=?")){p.setLong(1,orderId);try(ResultSet r=p.executeQuery()){return r.next()?r.getInt(1):0;}}
    }
    private static List<Map<String,Object>> orderItems(Connection c,long orderId)throws SQLException{
        String sql="SELECT order_item_id,product_name_snapshot,shop_name_snapshot,quantity,unit_price,line_total,status,progress_percent,estimated_delivery_at FROM order_items WHERE order_id=? ORDER BY order_item_id";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,orderId);try(ResultSet r=p.executeQuery()){
            List<Map<String,Object>> rows=new ArrayList<>();while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("itemId",r.getLong("order_item_id"));m.put("productName",r.getString("product_name_snapshot"));m.put("shopName",r.getString("shop_name_snapshot"));m.put("quantity",r.getInt("quantity"));m.put("unitPrice",r.getBigDecimal("unit_price"));m.put("lineTotal",r.getBigDecimal("line_total"));m.put("status",r.getString("status"));m.put("progress",r.getInt("progress_percent"));m.put("estimatedDelivery",r.getTimestamp("estimated_delivery_at"));rows.add(m);}return rows;
        }}
    }
    private static List<Map<String,Object>> orderTimeline(Connection c,long orderId)throws SQLException{
        String sql="SELECT old_status,new_status,note,changed_at FROM order_status_history WHERE order_id=? ORDER BY changed_at,history_id";
        try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,orderId);try(ResultSet r=p.executeQuery()){
            List<Map<String,Object>> rows=new ArrayList<>();while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("oldStatus",r.getString(1));m.put("newStatus",r.getString(2));m.put("note",r.getString(3));m.put("changedAt",r.getTimestamp(4));rows.add(m);}return rows;
        }}
    }
    private static void setDecimal(PreparedStatement p,int i,BigDecimal v)throws SQLException{if(v==null)p.setNull(i,Types.DECIMAL);else p.setBigDecimal(i,v);}
    private static String required(String s,String label,int max){String v=s==null?"":s.trim();if(v.isEmpty()||v.length()>max)throw new IllegalArgumentException(label+" không hợp lệ.");return v;}
    private static String clean(String s,int max){if(s==null||s.isBlank())return null;String v=s.trim();return v.length()>max?v.substring(0,max):v;}
    private static String allowedLabel(String v){return switch(v==null?"":v.trim()){case "Nhà riêng","Văn phòng","Xưởng"->v.trim();default->"Khác";};}
}
