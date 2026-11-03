package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderStatus;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OrderMessageDao {
    private OrderMessageDao() { }
    public static List<Map<String,Object>> threadsForUser(long userId){
        List<Map<String,Object>> rows=new ArrayList<>();
        String sql="SELECT o.order_id,o.order_code,x.shop_id,COALESCE((SELECT oi2.shop_name_snapshot FROM order_items oi2 WHERE oi2.order_id=o.order_id AND oi2.shop_id=x.shop_id ORDER BY oi2.order_item_id LIMIT 1),s.shop_name,'Xưởng in') shop_name,"
                +"(SELECT MAX(m.created_at) FROM order_messages m WHERE m.order_id=o.order_id AND m.shop_id=x.shop_id) last_message_at,"
                +"(SELECT m2.message_body FROM order_messages m2 WHERE m2.order_id=o.order_id AND m2.shop_id=x.shop_id ORDER BY m2.created_at DESC,m2.message_id DESC LIMIT 1) last_message,"
                +"(SELECT COUNT(*) FROM order_messages m3 WHERE m3.order_id=o.order_id AND m3.shop_id=x.shop_id AND m3.recipient_user_id=? AND m3.read_at IS NULL) unread_count "
                +"FROM orders o JOIN (SELECT DISTINCT order_id,shop_id FROM order_items WHERE shop_id IS NOT NULL) x ON x.order_id=o.order_id "
                +"LEFT JOIN print_shops s ON s.shop_id=x.shop_id WHERE o.user_id=? ORDER BY last_message_at DESC,o.placed_at DESC";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,userId);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("orderId",r.getLong("order_id"));m.put("orderCode",r.getString("order_code"));m.put("shopId",r.getLong("shop_id"));m.put("shopName",r.getString("shop_name"));m.put("lastMessage",r.getString("last_message"));m.put("unread",r.getLong("unread_count"));var ts=r.getTimestamp("last_message_at");m.put("lastMessageAt",ts==null?"":ts.toLocalDateTime().toString().replace('T',' '));rows.add(m);}}return rows;}
        catch(SQLException e){throw DataAccessException.of(e);}
    }
    public static List<Map<String,Object>> threadForUser(long orderId,long shopId,long userId){
        if(!ownsThread(orderId,shopId,userId))return null;
        try(Connection c=Database.getConnection();PreparedStatement mark=c.prepareStatement("UPDATE order_messages SET read_at=NOW() WHERE order_id=? AND shop_id=? AND recipient_user_id=? AND read_at IS NULL")){mark.setLong(1,orderId);mark.setLong(2,shopId);mark.setLong(3,userId);mark.executeUpdate();}
        catch(SQLException e){throw DataAccessException.of(e);}
        List<Map<String,Object>> rows=new ArrayList<>();String sql="SELECT m.message_id,m.sender_user_id,m.recipient_user_id,m.message_body,m.read_at,m.created_at,u.full_name FROM order_messages m LEFT JOIN users u ON u.user_id=m.sender_user_id JOIN orders o ON o.order_id=m.order_id WHERE m.order_id=? AND m.shop_id=? AND o.user_id=? ORDER BY m.created_at,m.message_id";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,orderId);p.setLong(2,shopId);p.setLong(3,userId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("message_id"));m.put("senderId",r.getLong("sender_user_id"));m.put("recipientId",r.getObject("recipient_user_id"));m.put("body",r.getString("message_body"));m.put("sender",r.getString("full_name"));m.put("mine",r.getLong("sender_user_id")==userId);var ts=r.getTimestamp("created_at");m.put("createdAt",ts==null?"":ts.toLocalDateTime().toString().replace('T',' '));rows.add(m);}}return rows;}
        catch(SQLException e){throw DataAccessException.of(e);}
    }
    public static boolean ownsThread(long orderId,long shopId,long userId){
        String sql="SELECT o.order_id FROM orders o JOIN order_items oi ON oi.order_id=o.order_id AND oi.shop_id=? WHERE o.order_id=? AND o.user_id=? LIMIT 1";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,shopId);p.setLong(2,orderId);p.setLong(3,userId);try(ResultSet r=p.executeQuery()){return r.next();}}
        catch(SQLException e){throw DataAccessException.of(e);}
    }
    public static List<Map<String,Object>> threadsForWorkshop(long ownerUserId,long shopId){
        List<Map<String,Object>> rows=new ArrayList<>();
        String sql="SELECT o.order_id,o.order_code,o.user_id,u.full_name customer_name,(SELECT MAX(m.created_at) FROM order_messages m WHERE m.order_id=o.order_id AND m.shop_id=?) last_message_at,(SELECT m2.message_body FROM order_messages m2 WHERE m2.order_id=o.order_id AND m2.shop_id=? ORDER BY m2.created_at DESC,m2.message_id DESC LIMIT 1) last_message,(SELECT COUNT(*) FROM order_messages m3 WHERE m3.order_id=o.order_id AND m3.shop_id=? AND m3.recipient_user_id=? AND m3.read_at IS NULL) unread_count FROM orders o JOIN order_items oi ON oi.order_id=o.order_id JOIN users u ON u.user_id=o.user_id JOIN print_shops ps ON ps.shop_id=oi.shop_id WHERE ps.shop_id=? AND ps.owner_user_id=? GROUP BY o.order_id,o.order_code,o.user_id,u.full_name,o.placed_at ORDER BY last_message_at DESC,o.placed_at DESC LIMIT 100";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,shopId);p.setLong(2,shopId);p.setLong(3,shopId);p.setLong(4,ownerUserId);p.setLong(5,shopId);p.setLong(6,ownerUserId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("orderId",r.getLong("order_id"));m.put("orderCode",r.getString("order_code"));m.put("customerId",r.getLong("user_id"));m.put("customerName",r.getString("customer_name"));m.put("lastMessage",r.getString("last_message"));m.put("unread",r.getLong("unread_count"));var ts=r.getTimestamp("last_message_at");m.put("lastMessageAt",ts==null?"":ts.toLocalDateTime().toString().replace('T',' '));rows.add(m);}}return rows;}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static List<Map<String,Object>> threadForWorkshop(long orderId,long shopId,long ownerUserId){
        long customerId;
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT o.user_id FROM orders o JOIN order_items oi ON oi.order_id=o.order_id JOIN print_shops ps ON ps.shop_id=oi.shop_id WHERE o.order_id=? AND ps.shop_id=? AND ps.owner_user_id=? LIMIT 1")){p.setLong(1,orderId);p.setLong(2,shopId);p.setLong(3,ownerUserId);try(ResultSet r=p.executeQuery()){if(!r.next())return null;customerId=r.getLong(1);}}
        catch(SQLException e){throw DataAccessException.of(e);}
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE order_messages SET read_at=NOW() WHERE order_id=? AND shop_id=? AND recipient_user_id=? AND read_at IS NULL")){p.setLong(1,orderId);p.setLong(2,shopId);p.setLong(3,ownerUserId);p.executeUpdate();}
        catch(SQLException e){throw DataAccessException.of(e);}
        List<Map<String,Object>> rows=new ArrayList<>();String sql="SELECT m.message_id,m.sender_user_id,m.recipient_user_id,m.message_body,m.read_at,m.created_at,u.full_name FROM order_messages m LEFT JOIN users u ON u.user_id=m.sender_user_id WHERE m.order_id=? AND m.shop_id=? ORDER BY m.created_at,m.message_id";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,orderId);p.setLong(2,shopId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("message_id"));m.put("senderId",r.getLong("sender_user_id"));m.put("recipientId",r.getObject("recipient_user_id"));m.put("body",r.getString("message_body"));m.put("sender",r.getString("full_name"));m.put("mine",r.getLong("sender_user_id")==ownerUserId);var ts=r.getTimestamp("created_at");m.put("createdAt",ts==null?"":ts.toLocalDateTime().toString().replace('T',' '));rows.add(m);}}return rows;}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void sendFromWorkshop(long orderId,long shopId,long ownerUserId,String body){
        if(body==null||body.isBlank()||body.trim().length()>2000)throw new IllegalArgumentException("Tin nhắn cần từ 1 đến 2.000 ký tự.");
        long customerId;
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            try(PreparedStatement p=c.prepareStatement("SELECT shop_id FROM print_shops WHERE shop_id=? AND owner_user_id=? FOR UPDATE")){p.setLong(1,shopId);p.setLong(2,ownerUserId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Xưởng không thuộc tài khoản của bạn.");}}
            try(PreparedStatement p=c.prepareStatement("SELECT o.user_id,o.status,o.cancelled_at FROM orders o JOIN order_items oi ON oi.order_id=o.order_id WHERE o.order_id=? AND oi.shop_id=? LIMIT 1 FOR UPDATE")){p.setLong(1,orderId);p.setLong(2,shopId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Đơn hàng không thuộc xưởng này.");customerId=r.getLong("user_id");String status=r.getString("status");java.sql.Timestamp canceled=r.getTimestamp("cancelled_at");if(OrderStatus.CANCELLED.equals(status)&&canceled!=null&&canceled.toLocalDateTime().isBefore(java.time.LocalDateTime.now().minusDays(30)))throw new IllegalArgumentException("Cuộc trò chuyện của đơn đã hủy quá 30 ngày nên không thể gửi thêm tin.");}}
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM order_messages WHERE sender_user_id=? AND created_at>=NOW()-INTERVAL 1 MINUTE")){p.setLong(1,ownerUserId);try(ResultSet r=p.executeQuery()){if(r.next()&&r.getInt(1)>=5)throw new IllegalArgumentException("Bạn gửi quá nhanh. Giới hạn là 5 tin nhắn mỗi phút.");}}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO order_messages(order_id,shop_id,sender_user_id,recipient_user_id,message_body,created_at) VALUES(?,?,?,?,?,NOW())")){p.setLong(1,orderId);p.setLong(2,shopId);p.setLong(3,ownerUserId);p.setLong(4,customerId);p.setString(5,body.trim());p.executeUpdate();}
            c.commit();
        }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}throw e;}finally{c.setAutoCommit(true);}}
        catch(SQLException e){throw DataAccessException.of(e);}
        NotificationService.notifyUser(customerId,"ORDER_MESSAGE","Xưởng đã phản hồi","Bạn có tin nhắn mới về đơn hàng.","/messages?orderId="+orderId+"&shopId="+shopId,"ORDER",orderId);
    }

    public static void send(long orderId,long shopId,long userId,String body){
        long ownerId;
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            String status;java.sql.Timestamp canceled;
            try(PreparedStatement p=c.prepareStatement("SELECT status,cancelled_at FROM orders WHERE order_id=? AND user_id=? FOR UPDATE")){p.setLong(1,orderId);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn.");status=r.getString(1);canceled=r.getTimestamp(2);}}
            if(OrderStatus.CANCELLED.equals(status)&&canceled!=null&&canceled.toLocalDateTime().isBefore(java.time.LocalDateTime.now().minusDays(30)))throw new IllegalArgumentException("Cuộc trò chuyện của đơn đã hủy quá 30 ngày nên không thể gửi thêm tin.");
            try(PreparedStatement p=c.prepareStatement("SELECT ps.owner_user_id FROM print_shops ps JOIN order_items oi ON oi.shop_id=ps.shop_id WHERE oi.order_id=? AND ps.shop_id=? LIMIT 1 FOR UPDATE")){p.setLong(1,orderId);p.setLong(2,shopId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Xưởng không thuộc đơn hàng này.");ownerId=r.getLong(1);if(r.wasNull()||ownerId<=0)throw new IllegalArgumentException("Xưởng chưa có tài khoản nhận tin nhắn.");}}
            try(PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM order_messages WHERE sender_user_id=? AND created_at>=NOW()-INTERVAL 1 MINUTE")){p.setLong(1,userId);try(ResultSet r=p.executeQuery()){if(r.next()&&r.getInt(1)>=5)throw new IllegalArgumentException("Bạn gửi quá nhanh. Giới hạn là 5 tin nhắn mỗi phút.");}}
            try(PreparedStatement p=c.prepareStatement("INSERT INTO order_messages(order_id,shop_id,sender_user_id,recipient_user_id,message_body,created_at) VALUES(?,?,?,?,?,NOW())")){p.setLong(1,orderId);p.setLong(2,shopId);p.setLong(3,userId);p.setLong(4,ownerId);p.setString(5,body);p.executeUpdate();}
            c.commit();
        }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}throw e;}finally{c.setAutoCommit(true);}}
        catch(SQLException e){throw DataAccessException.of(e);}
        NotificationService.notifyUser(ownerId,"ORDER_MESSAGE","Tin nhắn mới từ khách hàng","Bạn có tin nhắn mới về một đơn hàng.","/workshop/messages?orderId="+orderId+"&shopId="+shopId,"ORDER",orderId);
    }
}
