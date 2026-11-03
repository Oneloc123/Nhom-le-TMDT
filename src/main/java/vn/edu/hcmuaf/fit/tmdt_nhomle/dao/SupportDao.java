package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SupportDao {
    private static final SecureRandom RANDOM=new SecureRandom();
    private SupportDao() { }

    public static List<Map<String,Object>> listForUser(long userId){
        List<Map<String,Object>> rows=new ArrayList<>();String sql="SELECT ticket_id,ticket_code,category,subject,priority,status,created_at,updated_at FROM support_tickets WHERE user_id=? ORDER BY created_at DESC,ticket_id DESC";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,userId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("ticket_id"));m.put("code",r.getString("ticket_code"));m.put("category",r.getString("category"));m.put("subject",r.getString("subject"));m.put("priority",r.getString("priority"));m.put("status",r.getString("status"));m.put("createdAt",dateTime(r,"created_at"));m.put("updatedAt",dateTime(r,"updated_at"));rows.add(m);}}return rows;}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static Map<String,Object> findForUser(long ticketId,long userId){
        String sql="SELECT ticket_id,ticket_code,category,subject,priority,status,created_at,updated_at,resolved_at,closed_at,customer_rating,order_id,shop_id FROM support_tickets WHERE ticket_id=? AND user_id=?";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,ticketId);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){if(!r.next())return null;Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("ticket_id"));m.put("code",r.getString("ticket_code"));m.put("category",r.getString("category"));m.put("subject",r.getString("subject"));m.put("priority",r.getString("priority"));m.put("status",r.getString("status"));m.put("createdAt",dateTime(r,"created_at"));m.put("updatedAt",dateTime(r,"updated_at"));m.put("resolvedAt",dateTime(r,"resolved_at"));m.put("closedAt",dateTime(r,"closed_at"));m.put("rating",(Integer)r.getObject("customer_rating"));m.put("orderId",(Long)r.getObject("order_id"));m.put("shopId",(Long)r.getObject("shop_id"));return m;}}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static List<Map<String,Object>> messagesForUserTicket(long ticketId,long userId){
        if(findForUser(ticketId,userId)==null)return List.of();List<Map<String,Object>> rows=new ArrayList<>();
        String sql="SELECT m.ticket_message_id,m.sender_user_id,m.message_body,m.created_at,u.full_name FROM support_ticket_messages m LEFT JOIN users u ON u.user_id=m.sender_user_id WHERE m.ticket_id=? AND m.is_internal=0 ORDER BY m.created_at,m.ticket_message_id";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,ticketId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("ticket_message_id"));m.put("senderId",r.getLong("sender_user_id"));m.put("body",r.getString("message_body"));m.put("sender",r.getString("full_name"));m.put("createdAt",dateTime(r,"created_at"));rows.add(m);}}return rows;}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static long create(long userId,Long orderId,Long shopId,String category,String subject,String priority,String body){
        String ticketSql="INSERT INTO support_tickets (ticket_code,user_id,order_id,shop_id,category,subject,priority,status,created_at,updated_at) VALUES (?,?,?,?,?,?,?,'OPEN',NOW(),NOW())";
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            try(PreparedStatement lock=c.prepareStatement("SELECT user_id FROM users WHERE user_id=? AND deleted_at IS NULL FOR UPDATE")){lock.setLong(1,userId);try(ResultSet r=lock.executeQuery()){if(!r.next())throw new IllegalArgumentException("Tài khoản không còn tồn tại.");}}
            if(orderId!=null){try(PreparedStatement own=c.prepareStatement("SELECT order_id FROM orders WHERE order_id=? AND user_id=? FOR UPDATE")){own.setLong(1,orderId);own.setLong(2,userId);try(ResultSet r=own.executeQuery()){if(!r.next())throw new IllegalArgumentException("Đơn hàng được chọn không thuộc tài khoản của bạn.");}}}
            if(orderId!=null && shopId!=null){try(PreparedStatement shop=c.prepareStatement("SELECT order_item_id FROM order_items WHERE order_id=? AND shop_id=? LIMIT 1")){shop.setLong(1,orderId);shop.setLong(2,shopId);try(ResultSet r=shop.executeQuery()){if(!r.next())throw new IllegalArgumentException("Xưởng được chọn không thuộc đơn hàng này.");}}}
            try(PreparedStatement limit=c.prepareStatement("SELECT COUNT(*) FROM support_tickets WHERE user_id=? AND created_at>=NOW()-INTERVAL 1 HOUR")){limit.setLong(1,userId);try(ResultSet r=limit.executeQuery()){if(r.next()&&r.getInt(1)>=5)throw new IllegalArgumentException("Bạn đã tạo quá nhiều ticket trong một giờ. Vui lòng thử lại sau.");}}
            long id=-1;String code;
            int tries=0;
            while(id<0&&tries++<5){code="TK-"+LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+String.format("%04d",RANDOM.nextInt(10000));
                try(PreparedStatement p=c.prepareStatement(ticketSql,Statement.RETURN_GENERATED_KEYS)){p.setString(1,code);p.setLong(2,userId);setNullableLong(p,3,orderId);setNullableLong(p,4,shopId);p.setString(5,category);p.setString(6,subject);p.setString(7,priority);p.executeUpdate();try(ResultSet keys=p.getGeneratedKeys()){if(keys.next())id=keys.getLong(1);}}
                catch(SQLException duplicate){if(duplicate.getErrorCode()!=1062)throw duplicate;}
            }
            if(id<0)throw new SQLException("Không tạo được mã ticket duy nhất.");
            insertMessage(c,id,userId,body);
            c.commit();return id;
        }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}throw e;}finally{c.setAutoCommit(true);}}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void reply(long ticketId,long userId,String body){
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            String status=null;try(PreparedStatement p=c.prepareStatement("SELECT status FROM support_tickets WHERE ticket_id=? AND user_id=? FOR UPDATE")){p.setLong(1,ticketId);p.setLong(2,userId);try(ResultSet r=p.executeQuery()){if(r.next())status=r.getString(1);}}
            if(status==null)throw new IllegalArgumentException("Ticket không tồn tại hoặc không thuộc tài khoản của bạn.");
            if("CLOSED".equalsIgnoreCase(status))throw new IllegalArgumentException("Ticket đã đóng. Hãy dùng thao tác mở lại nếu vẫn trong thời hạn cho phép.");
            insertMessage(c,ticketId,userId,body);
            try(PreparedStatement p=c.prepareStatement("UPDATE support_tickets SET status='IN_PROGRESS',updated_at=NOW() WHERE ticket_id=? AND user_id=?")){p.setLong(1,ticketId);p.setLong(2,userId);p.executeUpdate();}
            c.commit();
        }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}throw e;}finally{c.setAutoCommit(true);}}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void reopen(long ticketId,long userId){
        String sql="UPDATE support_tickets SET status='OPEN',resolved_at=NULL,closed_at=NULL,updated_at=NOW() WHERE ticket_id=? AND user_id=? AND status IN ('RESOLVED','CLOSED') AND COALESCE(closed_at,resolved_at,updated_at)>=NOW()-INTERVAL 7 DAY";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,ticketId);p.setLong(2,userId);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Chỉ có thể mở lại ticket đã xử lý trong vòng 7 ngày.");}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void rate(long ticketId,long userId,int rating){
        if(rating<1||rating>5)throw new IllegalArgumentException("Điểm hài lòng phải từ 1 đến 5.");
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("UPDATE support_tickets SET customer_rating=?,updated_at=NOW() WHERE ticket_id=? AND user_id=? AND status IN ('RESOLVED','CLOSED')")){p.setInt(1,rating);p.setLong(2,ticketId);p.setLong(3,userId);if(p.executeUpdate()!=1)throw new IllegalArgumentException("Chỉ đánh giá ticket đã được giải quyết.");}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void notifyAdmins(String subject,long ticketId,String code){
        String sql="SELECT user_id FROM users WHERE LOWER(role)='admin' AND deleted_at IS NULL AND UPPER(status)='ACTIVE'";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql);ResultSet r=p.executeQuery()){
            while(r.next())NotificationService.notifyUser(r.getLong(1),"SUPPORT_TICKET","Ticket hỗ trợ mới",subject,"/admin/tickets?id="+ticketId,"SUPPORT_TICKET",ticketId);
        }catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static List<Map<String,Object>> listForAdmin(String status, String priority, String query) {
        List<Map<String,Object>> rows = new ArrayList<>();
        String sql = "SELECT t.ticket_id,t.ticket_code,t.user_id,u.full_name,u.email,t.order_id,t.shop_id,t.category,t.subject,t.priority,t.status,t.assigned_admin_user_id,t.sla_due_at,t.created_at,t.updated_at "
                + "FROM support_tickets t JOIN users u ON u.user_id=t.user_id WHERE (? IS NULL OR t.status=?) AND (? IS NULL OR t.priority=?) "
                + "AND (? IS NULL OR t.ticket_code LIKE ? OR t.subject LIKE ? OR u.email LIKE ? OR u.full_name LIKE ?) "
                + "ORDER BY CASE t.priority WHEN 'HIGH' THEN 0 WHEN 'NORMAL' THEN 1 ELSE 2 END, t.sla_due_at IS NULL, t.sla_due_at, t.created_at LIMIT 200";
        String s = allowedOrNull(status, Set.of("OPEN","IN_PROGRESS","WAITING_CUSTOMER","RESOLVED","CLOSED"));
        String pval = allowedOrNull(priority, Set.of("LOW","NORMAL","HIGH"));
        String q = query == null || query.isBlank() ? null : query.trim().substring(0, Math.min(120, query.trim().length()));
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setString(1,s);p.setString(2,s);p.setString(3,pval);p.setString(4,pval);p.setString(5,q);
            p.setString(6,q==null?null:"%"+q+"%");p.setString(7,q==null?null:"%"+q+"%");p.setString(8,q==null?null:"%"+q+"%");p.setString(9,q==null?null:"%"+q+"%");
            try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("ticket_id"));m.put("code",r.getString("ticket_code"));m.put("userId",r.getLong("user_id"));m.put("customer",r.getString("full_name"));m.put("email",r.getString("email"));m.put("orderId",(Long)r.getObject("order_id"));m.put("shopId",(Long)r.getObject("shop_id"));m.put("category",r.getString("category"));m.put("subject",r.getString("subject"));m.put("priority",r.getString("priority"));m.put("status",r.getString("status"));m.put("assignedAdminId",(Long)r.getObject("assigned_admin_user_id"));m.put("slaDueAt",dateTime(r,"sla_due_at"));m.put("createdAt",dateTime(r,"created_at"));m.put("updatedAt",dateTime(r,"updated_at"));rows.add(m);}}
            return rows;
        } catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static Map<String,Object> findForAdmin(long ticketId) {
        String sql="SELECT t.ticket_id,t.ticket_code,t.user_id,u.full_name,u.email,t.order_id,t.shop_id,t.category,t.subject,t.priority,t.status,t.assigned_admin_user_id,t.sla_due_at,t.created_at,t.updated_at,t.resolved_at,t.closed_at,t.customer_rating FROM support_tickets t JOIN users u ON u.user_id=t.user_id WHERE t.ticket_id=?";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,ticketId);try(ResultSet r=p.executeQuery()){if(!r.next())return null;Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("ticket_id"));m.put("code",r.getString("ticket_code"));m.put("userId",r.getLong("user_id"));m.put("customer",r.getString("full_name"));m.put("email",r.getString("email"));m.put("orderId",(Long)r.getObject("order_id"));m.put("shopId",(Long)r.getObject("shop_id"));m.put("category",r.getString("category"));m.put("subject",r.getString("subject"));m.put("priority",r.getString("priority"));m.put("status",r.getString("status"));m.put("assignedAdminId",(Long)r.getObject("assigned_admin_user_id"));m.put("slaDueAt",dateTime(r,"sla_due_at"));m.put("createdAt",dateTime(r,"created_at"));m.put("updatedAt",dateTime(r,"updated_at"));m.put("resolvedAt",dateTime(r,"resolved_at"));m.put("closedAt",dateTime(r,"closed_at"));m.put("rating",(Integer)r.getObject("customer_rating"));return m;}}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static List<Map<String,Object>> messagesForAdmin(long ticketId) {
        List<Map<String,Object>> rows=new ArrayList<>();
        String sql="SELECT m.ticket_message_id,m.sender_user_id,m.message_body,m.is_internal,m.created_at,u.full_name,u.role FROM support_ticket_messages m LEFT JOIN users u ON u.user_id=m.sender_user_id WHERE m.ticket_id=? ORDER BY m.created_at,m.ticket_message_id";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,ticketId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("ticket_message_id"));m.put("senderId",r.getLong("sender_user_id"));m.put("body",r.getString("message_body"));m.put("internal",r.getBoolean("is_internal"));m.put("sender",r.getString("full_name"));m.put("role",r.getString("role"));m.put("createdAt",dateTime(r,"created_at"));rows.add(m);}}return rows;}
        catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void updateByAdmin(long ticketId,long adminId,String status,String priority,Long assignedAdminId,String reply,boolean internal) {
        if(!Set.of("OPEN","IN_PROGRESS","WAITING_CUSTOMER","RESOLVED","CLOSED").contains(status))throw new IllegalArgumentException("Trạng thái ticket không hợp lệ.");
        if(!Set.of("LOW","NORMAL","HIGH").contains(priority))throw new IllegalArgumentException("Mức ưu tiên không hợp lệ.");
        if(assignedAdminId!=null){try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement("SELECT user_id FROM users WHERE user_id=? AND LOWER(role)='admin' AND UPPER(status)='ACTIVE' AND deleted_at IS NULL")){p.setLong(1,assignedAdminId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Người được giao phải là tài khoản Admin đang hoạt động.");}}catch(SQLException e){throw DataAccessException.of(e);}}
        if(reply!=null&&!reply.isBlank()&&reply.trim().length()>5000)throw new IllegalArgumentException("Phản hồi tối đa 5.000 ký tự.");
        long customerId;
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            try(PreparedStatement p=c.prepareStatement("SELECT user_id,status FROM support_tickets WHERE ticket_id=? FOR UPDATE")){p.setLong(1,ticketId);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Ticket không tồn tại.");customerId=r.getLong(1);}}
            String update="UPDATE support_tickets SET status=?,priority=?,assigned_admin_user_id=?,sla_due_at=CASE WHEN ? IN ('RESOLVED','CLOSED') THEN NULL WHEN sla_due_at IS NULL THEN DATE_ADD(NOW(),INTERVAL 24 HOUR) ELSE sla_due_at END,resolved_at=CASE WHEN ?='RESOLVED' THEN NOW() WHEN ? IN ('OPEN','IN_PROGRESS','WAITING_CUSTOMER') THEN NULL ELSE resolved_at END,closed_at=CASE WHEN ?='CLOSED' THEN NOW() WHEN ?<>'CLOSED' THEN NULL ELSE closed_at END,updated_at=NOW() WHERE ticket_id=?";
            try(PreparedStatement p=c.prepareStatement(update)){p.setString(1,status);p.setString(2,priority);if(assignedAdminId==null)p.setNull(3,java.sql.Types.BIGINT);else p.setLong(3,assignedAdminId);p.setString(4,status);p.setString(5,status);p.setString(6,status);p.setString(7,status);p.setString(8,status);p.setLong(9,ticketId);p.executeUpdate();}
            if(reply!=null&&!reply.isBlank()){try(PreparedStatement p=c.prepareStatement("INSERT INTO support_ticket_messages(ticket_id,sender_user_id,message_body,is_internal,created_at) VALUES(?,?,?,?,NOW())")){p.setLong(1,ticketId);p.setLong(2,adminId);p.setString(3,reply.trim());p.setBoolean(4,internal);p.executeUpdate();}}
            c.commit();
        }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}throw e;}finally{c.setAutoCommit(true);}}
        catch(SQLException e){throw DataAccessException.of(e);}
        if((reply!=null&&!reply.isBlank()&&!internal)||"RESOLVED".equals(status)||"CLOSED".equals(status))NotificationService.notifyUser(customerId,"SUPPORT_UPDATED","Cập nhật yêu cầu hỗ trợ","Ticket #"+ticketId+" đã được cập nhật trạng thái.","/support?id="+ticketId,"SUPPORT_TICKET",ticketId);
    }

    private static String allowedOrNull(String value,Set<String> allowed){if(value==null||value.isBlank())return null;String v=value.trim().toUpperCase(java.util.Locale.ROOT);return allowed.contains(v)?v:null;}

    private static void insertMessage(Connection c,long id,long sender,String body)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO support_ticket_messages(ticket_id,sender_user_id,message_body,is_internal,created_at) VALUES (?,?,?,0,NOW())")){p.setLong(1,id);p.setLong(2,sender);p.setString(3,body);p.executeUpdate();}
    }
    private static void setNullableLong(PreparedStatement p,int i,Long value)throws SQLException{if(value==null)p.setNull(i,java.sql.Types.BIGINT);else p.setLong(i,value);}
    private static String dateTime(ResultSet r,String field)throws SQLException{var t=r.getTimestamp(field);return t==null?"":t.toLocalDateTime().toString().replace('T',' ');}
}
