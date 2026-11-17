package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.SupportSlaPolicy;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SupportDao {
    private static final SecureRandom RANDOM=new SecureRandom();
    private SupportDao() { }
    public record Attachment(long id,long ticketId,long uploadedByUserId,String fileName,String contentType,long size,byte[] data) { }


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
        return create(userId,orderId,shopId,category,subject,priority,body,null,null,null);
    }
    public static long create(long userId,Long orderId,Long shopId,String category,String subject,String priority,String body,String fileName,String contentType,byte[] fileData){
        if(fileData!=null&&fileData.length>8*1024*1024)throw new IllegalArgumentException("Tệp đính kèm không được vượt quá 8 MB.");
        if(fileData!=null&&(fileName==null||contentType==null))throw new IllegalArgumentException("Thông tin tệp đính kèm không hợp lệ.");
        String normalizedPriority = priority == null ? "NORMAL" : priority.trim().toUpperCase(java.util.Locale.ROOT);
        int slaHours = SupportSlaPolicy.responseWindowHours(normalizedPriority);
        String ticketSql="INSERT INTO support_tickets (ticket_code,user_id,order_id,shop_id,category,subject,priority,status,sla_due_at,created_at,updated_at) VALUES (?,?,?,?,?,?,?,'OPEN',DATE_ADD(NOW(),INTERVAL ? HOUR),NOW(),NOW())";
        try(Connection c=Database.getConnection()){c.setAutoCommit(false);try{
            try(PreparedStatement lock=c.prepareStatement("SELECT user_id FROM users WHERE user_id=? AND deleted_at IS NULL FOR UPDATE")){lock.setLong(1,userId);try(ResultSet r=lock.executeQuery()){if(!r.next())throw new IllegalArgumentException("Tài khoản không còn tồn tại.");}}
            if(orderId!=null){try(PreparedStatement own=c.prepareStatement("SELECT order_id FROM orders WHERE order_id=? AND user_id=? FOR UPDATE")){own.setLong(1,orderId);own.setLong(2,userId);try(ResultSet r=own.executeQuery()){if(!r.next())throw new IllegalArgumentException("Đơn hàng được chọn không thuộc tài khoản của bạn.");}}}
            if(orderId!=null && shopId!=null){try(PreparedStatement shop=c.prepareStatement("SELECT order_item_id FROM order_items WHERE order_id=? AND shop_id=? LIMIT 1")){shop.setLong(1,orderId);shop.setLong(2,shopId);try(ResultSet r=shop.executeQuery()){if(!r.next())throw new IllegalArgumentException("Xưởng được chọn không thuộc đơn hàng này.");}}}
            try(PreparedStatement limit=c.prepareStatement("SELECT COUNT(*) FROM support_tickets WHERE user_id=? AND created_at>=NOW()-INTERVAL 1 HOUR")){limit.setLong(1,userId);try(ResultSet r=limit.executeQuery()){if(r.next()&&r.getInt(1)>=5)throw new IllegalArgumentException("Bạn đã tạo quá nhiều ticket trong một giờ. Vui lòng thử lại sau.");}}
            long id=-1;String code;
            int tries=0;
            while(id<0&&tries++<5){code="TK-"+LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)+"-"+String.format("%04d",RANDOM.nextInt(10000));
                try(PreparedStatement p=c.prepareStatement(ticketSql,Statement.RETURN_GENERATED_KEYS)){p.setString(1,code);p.setLong(2,userId);setNullableLong(p,3,orderId);setNullableLong(p,4,shopId);p.setString(5,category);p.setString(6,subject);p.setString(7,normalizedPriority);p.setInt(8,slaHours);p.executeUpdate();try(ResultSet keys=p.getGeneratedKeys()){if(keys.next())id=keys.getLong(1);}}
                catch(SQLException duplicate){if(duplicate.getErrorCode()!=1062)throw duplicate;}
            }
            if(id<0)throw new SQLException("Không tạo được mã ticket duy nhất.");
            insertMessage(c,id,userId,body);
            if(fileData!=null&&fileData.length>0)insertAttachment(c,id,userId,fileName,contentType,fileData);
            c.commit();return id;
        }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}throw e;}finally{c.setAutoCommit(true);}}
        catch(SQLException e){throw DataAccessException.of(e);}
    }


    public static List<Map<String,Object>> attachmentsForUser(long ticketId,long userId){return attachmentMetadata(ticketId,userId);}
    public static List<Map<String,Object>> attachmentsForAdmin(long ticketId){return attachmentMetadata(ticketId,null);}
    private static List<Map<String,Object>> attachmentMetadata(long ticketId,Long userId){
        List<Map<String,Object>> rows=new ArrayList<>();
        String sql="SELECT a.attachment_id,a.original_filename,a.content_type,a.file_size,a.created_at FROM support_ticket_attachments a JOIN support_tickets t ON t.ticket_id=a.ticket_id WHERE a.ticket_id=?"+(userId==null?"":" AND t.user_id=?")+" ORDER BY a.created_at,a.attachment_id";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,ticketId);if(userId!=null)p.setLong(2,userId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("attachment_id"));m.put("fileName",r.getString("original_filename"));m.put("contentType",r.getString("content_type"));m.put("size",r.getLong("file_size"));m.put("createdAt",dateTime(r,"created_at"));rows.add(m);}}return rows;}catch(SQLException e){throw DataAccessException.of(e);}
    }
    public static Attachment findAttachmentForUser(long attachmentId,long userId){return findAttachment(attachmentId,userId);}
    public static Attachment findAttachmentForAdmin(long attachmentId){return findAttachment(attachmentId,null);}
    private static Attachment findAttachment(long attachmentId,Long userId){
        String sql="SELECT a.attachment_id,a.ticket_id,a.uploaded_by_user_id,a.original_filename,a.content_type,a.file_size,a.file_data FROM support_ticket_attachments a JOIN support_tickets t ON t.ticket_id=a.ticket_id WHERE a.attachment_id=?"+(userId==null?"":" AND t.user_id=?");
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,attachmentId);if(userId!=null)p.setLong(2,userId);try(ResultSet r=p.executeQuery()){if(!r.next())return null;return new Attachment(r.getLong("attachment_id"),r.getLong("ticket_id"),r.getLong("uploaded_by_user_id"),r.getString("original_filename"),r.getString("content_type"),r.getLong("file_size"),r.getBytes("file_data"));}}catch(SQLException e){throw DataAccessException.of(e);}
    }
    private static void insertAttachment(Connection c,long ticketId,long userId,String name,String type,byte[] bytes)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO support_ticket_attachments(ticket_id,uploaded_by_user_id,original_filename,content_type,file_size,file_data,created_at) VALUES(?,?,?,?,?,?,NOW())")){p.setLong(1,ticketId);p.setLong(2,userId);p.setString(3,name);p.setString(4,type);p.setLong(5,bytes.length);p.setBytes(6,bytes);p.executeUpdate();}
    }

    public static void reply(long ticketId,long userId,String body){
        if(body==null||body.trim().isEmpty()||body.trim().length()>5000)
            throw new IllegalArgumentException("Nội dung phản hồi phải có từ 1 đến 5000 ký tự.");
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            try{
                String status=null,code=null,subject=null,priority=null;
                try(PreparedStatement p=c.prepareStatement(
                        "SELECT status,ticket_code,subject,priority FROM support_tickets WHERE ticket_id=? AND user_id=? FOR UPDATE")){
                    p.setLong(1,ticketId);p.setLong(2,userId);
                    try(ResultSet r=p.executeQuery()){
                        if(r.next()){status=r.getString("status");code=r.getString("ticket_code");subject=r.getString("subject");priority=r.getString("priority");}
                    }
                }
                if(status==null)throw new IllegalArgumentException("Ticket không tồn tại hoặc không thuộc tài khoản của bạn.");
                if("CLOSED".equalsIgnoreCase(status))throw new IllegalArgumentException("Ticket đã đóng. Hãy dùng thao tác mở lại nếu vẫn trong thời hạn cho phép.");
                insertMessage(c,ticketId,userId,body.trim());
                String update="UPDATE support_tickets SET status='IN_PROGRESS',"
                        + "sla_due_at=CASE WHEN status='RESOLVED' THEN DATE_ADD(NOW(),INTERVAL"
                        + " CASE UPPER(priority) WHEN 'HIGH' THEN 4 WHEN 'LOW' THEN 72 ELSE 24 END HOUR)"
                        + " WHEN status='WAITING_CUSTOMER' AND first_response_at IS NULL THEN DATE_ADD(NOW(),INTERVAL"
                        + " COALESCE(NULLIF(sla_remaining_seconds,0),CASE UPPER(priority) WHEN 'HIGH' THEN 14400 WHEN 'LOW' THEN 259200 ELSE 86400 END) SECOND)"
                        + " WHEN sla_due_at IS NULL AND first_response_at IS NULL THEN DATE_ADD(NOW(),INTERVAL"
                        + " CASE UPPER(priority) WHEN 'HIGH' THEN 4 WHEN 'LOW' THEN 72 ELSE 24 END HOUR) ELSE sla_due_at END,"
                        + "first_response_at=CASE WHEN status='RESOLVED' THEN NULL ELSE first_response_at END,"
                        + "sla_remaining_seconds=CASE WHEN status='WAITING_CUSTOMER' AND first_response_at IS NULL THEN NULL ELSE sla_remaining_seconds END,"
                        + "sla_paused_at=CASE WHEN status='WAITING_CUSTOMER' AND first_response_at IS NULL THEN NULL ELSE sla_paused_at END,"
                        + "updated_at=NOW() WHERE ticket_id=? AND user_id=?";
                try(PreparedStatement p=c.prepareStatement(update)){p.setLong(1,ticketId);p.setLong(2,userId);p.executeUpdate();}
                try(PreparedStatement p=c.prepareStatement(
                        "SELECT user_id FROM users WHERE LOWER(role)='admin' AND UPPER(status)='ACTIVE' AND deleted_at IS NULL")){
                    try(ResultSet r=p.executeQuery()){
                        while(r.next()){
                            long adminId=r.getLong(1);
                            NotificationService.notifyUser(c,adminId,"SUPPORT_CUSTOMER_REPLY","Khách hàng đã phản hồi ticket",
                                    "Khách hàng đã phản hồi ticket "+code+": "+subject,
                                    "/admin/tickets?id="+ticketId,"SUPPORT_TICKET",ticketId);
                        }
                    }
                }
                c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void reopen(long ticketId,long userId){
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            try{
                String priority=null,code=null;
                try(PreparedStatement p=c.prepareStatement(
                        "SELECT priority,ticket_code FROM support_tickets WHERE ticket_id=? AND user_id=?"
                      + " AND status IN ('RESOLVED','CLOSED')"
                      + " AND COALESCE(closed_at,resolved_at,updated_at)>=NOW()-INTERVAL 7 DAY FOR UPDATE")){
                    p.setLong(1,ticketId);p.setLong(2,userId);
                    try(ResultSet r=p.executeQuery()){if(r.next()){priority=r.getString("priority");code=r.getString("ticket_code");}}
                }
                if(priority==null)throw new IllegalArgumentException("Chỉ có thể mở lại ticket đã xử lý trong vòng 7 ngày.");
                int hours=SupportSlaPolicy.responseWindowHours(priority);
                try(PreparedStatement p=c.prepareStatement(
                        "UPDATE support_tickets SET status='OPEN',resolved_at=NULL,closed_at=NULL,first_response_at=NULL,"
                      + "sla_due_at=DATE_ADD(NOW(),INTERVAL ? HOUR),sla_remaining_seconds=NULL,sla_paused_at=NULL,updated_at=NOW()"
                      + " WHERE ticket_id=? AND user_id=? AND status IN ('RESOLVED','CLOSED')")){
                    p.setInt(1,hours);p.setLong(2,ticketId);p.setLong(3,userId);
                    if(p.executeUpdate()!=1)throw new IllegalArgumentException("Ticket đã thay đổi; hãy tải lại trang.");
                }
                try(PreparedStatement p=c.prepareStatement(
                        "SELECT user_id FROM users WHERE LOWER(role)='admin' AND UPPER(status)='ACTIVE' AND deleted_at IS NULL")){
                    try(ResultSet r=p.executeQuery()){
                        while(r.next()) NotificationService.notifyUser(c,r.getLong(1),"SUPPORT_REOPENED","Ticket được mở lại",
                                "Khách hàng đã mở lại ticket "+code+".","/admin/tickets?id="+ticketId,"SUPPORT_TICKET",ticketId);
                    }
                }
                c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){throw DataAccessException.of(e);}
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
        return listForAdmin(status, priority, query, null);
    }

    public static List<Map<String,Object>> listForAdmin(String status, String priority, String query, String slaFilter) {
        List<Map<String,Object>> rows = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT t.ticket_id,t.ticket_code,t.user_id,u.full_name,u.email,"
                + "t.order_id,t.shop_id,t.category,t.subject,t.priority,t.status,t.assigned_admin_user_id,"
                + "t.sla_due_at,t.sla_remaining_seconds,t.sla_paused_at,t.first_response_at,t.created_at,t.updated_at,"
                + "CASE WHEN t.status='CLOSED' THEN 'CLOSED' WHEN t.status='RESOLVED' THEN 'RESOLVED'"
                + " WHEN t.status='WAITING_CUSTOMER' THEN 'PAUSED'"
                + " WHEN t.first_response_at IS NOT NULL THEN 'RESPONDED'"
                + " WHEN t.sla_due_at IS NULL THEN 'NO_SLA'"
                + " WHEN t.sla_due_at<NOW() THEN 'OVERDUE'"
                + " WHEN t.sla_due_at<=DATE_ADD(NOW(),INTERVAL 4 HOUR) THEN 'DUE_SOON' ELSE 'ON_TRACK' END sla_state"
                + " FROM support_tickets t JOIN users u ON u.user_id=t.user_id"
                + " WHERE (? IS NULL OR t.status=?) AND (? IS NULL OR t.priority=?)"
                + " AND (? IS NULL OR t.ticket_code LIKE ? OR t.subject LIKE ? OR u.email LIKE ? OR u.full_name LIKE ?)");
        String s = allowedOrNull(status, Set.of("OPEN","IN_PROGRESS","WAITING_CUSTOMER","RESOLVED","CLOSED"));
        String pval = allowedOrNull(priority, Set.of("LOW","NORMAL","HIGH"));
        String q = query == null || query.isBlank() ? null : query.trim().substring(0, Math.min(120, query.trim().length()));
        String sla = slaFilter == null || slaFilter.isBlank() || "ALL".equalsIgnoreCase(slaFilter.trim())
                ? null : slaFilter.trim().toUpperCase(java.util.Locale.ROOT);
        if (sla != null) {
            switch (sla) {
                case "OVERDUE" -> sql.append(" AND t.status IN ('OPEN','IN_PROGRESS') AND t.first_response_at IS NULL AND t.sla_due_at<NOW()");
                case "DUE_SOON" -> sql.append(" AND t.status IN ('OPEN','IN_PROGRESS') AND t.first_response_at IS NULL AND t.sla_due_at BETWEEN NOW() AND DATE_ADD(NOW(),INTERVAL 4 HOUR)");
                case "PAUSED" -> sql.append(" AND t.status='WAITING_CUSTOMER'");
                case "ON_TRACK" -> sql.append(" AND t.status IN ('OPEN','IN_PROGRESS') AND t.first_response_at IS NULL AND t.sla_due_at>DATE_ADD(NOW(),INTERVAL 4 HOUR)");
                case "RESPONDED" -> sql.append(" AND t.first_response_at IS NOT NULL");
                default -> throw new IllegalArgumentException("Bộ lọc SLA không hợp lệ.");
            }
        }
        sql.append(" ORDER BY CASE t.priority WHEN 'HIGH' THEN 0 WHEN 'NORMAL' THEN 1 ELSE 2 END,"
                + " CASE WHEN t.status='WAITING_CUSTOMER' THEN 1 ELSE 0 END,t.sla_due_at IS NULL,t.sla_due_at,t.created_at LIMIT 200");
        try (Connection c=Database.getConnection(); PreparedStatement p=c.prepareStatement(sql.toString())) {
            p.setString(1,s);p.setString(2,s);p.setString(3,pval);p.setString(4,pval);p.setString(5,q);
            p.setString(6,q==null?null:"%"+q+"%");p.setString(7,q==null?null:"%"+q+"%");
            p.setString(8,q==null?null:"%"+q+"%");p.setString(9,q==null?null:"%"+q+"%");
            try(ResultSet r=p.executeQuery()) {
                while(r.next()) {
                    Map<String,Object> m=new LinkedHashMap<>();
                    m.put("id",r.getLong("ticket_id"));m.put("code",r.getString("ticket_code"));
                    m.put("userId",r.getLong("user_id"));m.put("customer",r.getString("full_name"));m.put("email",r.getString("email"));
                    m.put("orderId",(Long)r.getObject("order_id"));m.put("shopId",(Long)r.getObject("shop_id"));
                    m.put("category",r.getString("category"));m.put("subject",r.getString("subject"));
                    m.put("priority",r.getString("priority"));m.put("status",r.getString("status"));
                    m.put("assignedAdminId",(Long)r.getObject("assigned_admin_user_id"));
                    m.put("slaDueAt",dateTime(r,"sla_due_at"));m.put("slaRemainingSeconds",nullableLong(r,"sla_remaining_seconds"));
                    m.put("slaPausedAt",dateTime(r,"sla_paused_at"));m.put("firstResponseAt",dateTime(r,"first_response_at"));
                    m.put("slaState",r.getString("sla_state"));m.put("createdAt",dateTime(r,"created_at"));m.put("updatedAt",dateTime(r,"updated_at"));
                    rows.add(m);
                }
            }
            return rows;
        } catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static Map<String,Object> findForAdmin(long ticketId) {
        String sql="SELECT t.ticket_id,t.ticket_code,t.user_id,u.full_name,u.email,t.order_id,t.shop_id,t.category,t.subject,"
                + "t.priority,t.status,t.assigned_admin_user_id,t.sla_due_at,t.sla_remaining_seconds,t.sla_paused_at,"
                + "t.first_response_at,t.created_at,t.updated_at,t.resolved_at,t.closed_at,t.customer_rating,"
                + "CASE WHEN t.status='CLOSED' THEN 'CLOSED' WHEN t.status='RESOLVED' THEN 'RESOLVED'"
                + " WHEN t.status='WAITING_CUSTOMER' THEN 'PAUSED' WHEN t.first_response_at IS NOT NULL THEN 'RESPONDED'"
                + " WHEN t.sla_due_at IS NULL THEN 'NO_SLA' WHEN t.sla_due_at<NOW() THEN 'OVERDUE'"
                + " WHEN t.sla_due_at<=DATE_ADD(NOW(),INTERVAL 4 HOUR) THEN 'DUE_SOON' ELSE 'ON_TRACK' END sla_state"
                + " FROM support_tickets t JOIN users u ON u.user_id=t.user_id WHERE t.ticket_id=?";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setLong(1,ticketId);
            try(ResultSet r=p.executeQuery()){
                if(!r.next())return null;
                Map<String,Object> m=new LinkedHashMap<>();
                m.put("id",r.getLong("ticket_id"));m.put("code",r.getString("ticket_code"));m.put("userId",r.getLong("user_id"));
                m.put("customer",r.getString("full_name"));m.put("email",r.getString("email"));
                m.put("orderId",(Long)r.getObject("order_id"));m.put("shopId",(Long)r.getObject("shop_id"));
                m.put("category",r.getString("category"));m.put("subject",r.getString("subject"));
                m.put("priority",r.getString("priority"));m.put("status",r.getString("status"));
                m.put("assignedAdminId",(Long)r.getObject("assigned_admin_user_id"));
                m.put("slaDueAt",dateTime(r,"sla_due_at"));m.put("slaRemainingSeconds",nullableLong(r,"sla_remaining_seconds"));
                m.put("slaPausedAt",dateTime(r,"sla_paused_at"));m.put("firstResponseAt",dateTime(r,"first_response_at"));
                m.put("slaState",r.getString("sla_state"));m.put("createdAt",dateTime(r,"created_at"));
                m.put("updatedAt",dateTime(r,"updated_at"));m.put("resolvedAt",dateTime(r,"resolved_at"));
                m.put("closedAt",dateTime(r,"closed_at"));m.put("rating",(Integer)r.getObject("customer_rating"));
                m.put("events",rows("SELECT e.old_status oldStatus,e.new_status newStatus,e.old_priority oldPriority,"
                        + "e.new_priority newPriority,e.old_assigned_admin_user_id oldAssignee,e.new_assigned_admin_user_id newAssignee,"
                        + "e.note,e.created_at createdAt,u.full_name adminName FROM support_ticket_events e"
                        + " LEFT JOIN users u ON u.user_id=e.admin_user_id WHERE e.ticket_id=?"
                        + " ORDER BY e.created_at,e.event_id",ticketId));
                return m;
            }
        } catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static List<Map<String,Object>> messagesForAdmin(long ticketId) {
        List<Map<String,Object>> rows=new ArrayList<>();
        String sql="SELECT m.ticket_message_id,m.sender_user_id,m.message_body,m.is_internal,m.created_at,u.full_name,u.role"
                + " FROM support_ticket_messages m LEFT JOIN users u ON u.user_id=m.sender_user_id"
                + " WHERE m.ticket_id=? ORDER BY m.created_at,m.ticket_message_id";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setLong(1,ticketId);
            try(ResultSet r=p.executeQuery()){
                while(r.next()){
                    Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("ticket_message_id"));
                    m.put("senderId",r.getLong("sender_user_id"));m.put("body",r.getString("message_body"));
                    m.put("internal",r.getBoolean("is_internal"));m.put("sender",r.getString("full_name"));
                    m.put("role",r.getString("role"));m.put("createdAt",dateTime(r,"created_at"));rows.add(m);
                }
            }
            return rows;
        } catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void updateByAdmin(long ticketId,long adminId,String status,String priority,Long assignedAdminId,String reply,boolean internal) {
        String nextStatus = status == null ? "" : status.trim().toUpperCase(java.util.Locale.ROOT);
        String nextPriority = priority == null ? "" : priority.trim().toUpperCase(java.util.Locale.ROOT);
        if(!SupportSlaPolicy.isValidStatus(nextStatus))throw new IllegalArgumentException("Trạng thái ticket không hợp lệ.");
        SupportSlaPolicy.responseWindowHours(nextPriority);
        if(assignedAdminId!=null){
            try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(
                    "SELECT user_id FROM users WHERE user_id=? AND LOWER(role)='admin' AND UPPER(status)='ACTIVE' AND deleted_at IS NULL")){
                p.setLong(1,assignedAdminId);
                try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Người được giao phải là tài khoản Admin đang hoạt động.");}
            }catch(SQLException e){throw DataAccessException.of(e);}
        }
        if(reply!=null&&!reply.isBlank()&&reply.trim().length()>5000)throw new IllegalArgumentException("Phản hồi tối đa 5.000 ký tự.");
        String message = reply == null ? "" : reply.trim();
        try(Connection c=Database.getConnection()){
            c.setAutoCommit(false);
            try {
                long customerId;
                String ticketCode;
                String oldStatus,oldPriority;
                Long oldAssignee;
                LocalDateTime oldDue,oldPausedAt,oldFirstResponse;
                Long oldRemaining;
                try(PreparedStatement p=c.prepareStatement(
                        "SELECT user_id,ticket_code,status,priority,assigned_admin_user_id,sla_due_at,sla_remaining_seconds,"
                      + "sla_paused_at,first_response_at FROM support_tickets WHERE ticket_id=? FOR UPDATE")){
                    p.setLong(1,ticketId);
                    try(ResultSet r=p.executeQuery()){
                        if(!r.next())throw new IllegalArgumentException("Ticket không tồn tại.");
                        customerId=r.getLong("user_id");ticketCode=r.getString("ticket_code");
                        oldStatus=r.getString("status");oldPriority=r.getString("priority");
                        oldAssignee=(Long)r.getObject("assigned_admin_user_id");
                        Timestamp dueTimestamp=r.getTimestamp("sla_due_at");
                        oldDue=dueTimestamp==null?null:dueTimestamp.toLocalDateTime();
                        Object remainingRaw=r.getObject("sla_remaining_seconds");
                        oldRemaining=remainingRaw instanceof Number n?n.longValue():null;
                        Timestamp pausedTimestamp=r.getTimestamp("sla_paused_at");
                        oldPausedAt=pausedTimestamp==null?null:pausedTimestamp.toLocalDateTime();
                        Timestamp responseTimestamp=r.getTimestamp("first_response_at");
                        oldFirstResponse=responseTimestamp==null?null:responseTimestamp.toLocalDateTime();
                    }
                }
                if(!SupportSlaPolicy.canTransition(oldStatus,nextStatus)){
                    throw new IllegalArgumentException("Không thể chuyển ticket từ "+oldStatus+" sang "+nextStatus+".");
                }
                LocalDateTime now=databaseNow(c);
                LocalDateTime nextDue=oldDue;
                Long nextRemaining=oldRemaining;
                LocalDateTime nextPausedAt=oldPausedAt;
                LocalDateTime firstResponse=oldFirstResponse;
                boolean publicReply=!message.isBlank()&&!internal;
                if(publicReply&&firstResponse==null)firstResponse=now;

                if("RESOLVED".equals(nextStatus)||"CLOSED".equals(nextStatus)||firstResponse!=null){
                    nextDue=null;nextRemaining=null;nextPausedAt=null;
                }else if("WAITING_CUSTOMER".equals(nextStatus)){
                    if(!"WAITING_CUSTOMER".equalsIgnoreCase(oldStatus)){
                        long remaining=oldDue==null
                                ? SupportSlaPolicy.responseWindowHours(nextPriority)*3600L
                                : Math.max(0,ChronoUnit.SECONDS.between(now,oldDue));
                        nextRemaining=remaining;nextPausedAt=now;nextDue=null;
                    }
                }else if("WAITING_CUSTOMER".equalsIgnoreCase(oldStatus)){
                    long remaining=oldRemaining==null?0L:Math.max(0L,oldRemaining);
                    nextDue=now.plusSeconds(remaining>0?remaining:SupportSlaPolicy.responseWindowHours(nextPriority)*3600L);
                    nextRemaining=null;nextPausedAt=null;
                }else if(oldDue==null||!nextPriority.equalsIgnoreCase(oldPriority)){
                    nextDue=now.plusHours(SupportSlaPolicy.responseWindowHours(nextPriority));
                    nextRemaining=null;nextPausedAt=null;
                }

                String update="UPDATE support_tickets SET status=?,priority=?,assigned_admin_user_id=?,sla_due_at=?,"
                        + "sla_remaining_seconds=?,sla_paused_at=?,first_response_at=?,"
                        + "resolved_at=CASE WHEN ?='RESOLVED' THEN COALESCE(resolved_at,NOW()) WHEN ? IN ('OPEN','IN_PROGRESS','WAITING_CUSTOMER') THEN NULL ELSE resolved_at END,"
                        + "closed_at=CASE WHEN ?='CLOSED' THEN COALESCE(closed_at,NOW()) WHEN ?<>'CLOSED' THEN NULL ELSE closed_at END,"
                        + "updated_at=NOW() WHERE ticket_id=?";
                try(PreparedStatement p=c.prepareStatement(update)){
                    p.setString(1,nextStatus);p.setString(2,nextPriority);
                    if(assignedAdminId==null)p.setNull(3,java.sql.Types.BIGINT);else p.setLong(3,assignedAdminId);
                    if(nextDue==null)p.setNull(4,java.sql.Types.TIMESTAMP);else p.setTimestamp(4,Timestamp.valueOf(nextDue));
                    if(nextRemaining==null)p.setNull(5,java.sql.Types.BIGINT);else p.setLong(5,nextRemaining);
                    if(nextPausedAt==null)p.setNull(6,java.sql.Types.TIMESTAMP);else p.setTimestamp(6,Timestamp.valueOf(nextPausedAt));
                    if(firstResponse==null)p.setNull(7,java.sql.Types.TIMESTAMP);else p.setTimestamp(7,Timestamp.valueOf(firstResponse));
                    p.setString(8,nextStatus);p.setString(9,nextStatus);p.setString(10,nextStatus);p.setString(11,nextStatus);
                    p.setLong(12,ticketId);
                    if(p.executeUpdate()!=1)throw new IllegalArgumentException("Ticket đã thay đổi ở nơi khác.");
                }
                if(!message.isBlank()){
                    try(PreparedStatement p=c.prepareStatement(
                            "INSERT INTO support_ticket_messages(ticket_id,sender_user_id,message_body,is_internal,created_at) VALUES(?,?,?,?,NOW())")){
                        p.setLong(1,ticketId);p.setLong(2,adminId);p.setString(3,message);p.setBoolean(4,internal);p.executeUpdate();
                    }
                }
                boolean stateChanged=!oldStatus.equalsIgnoreCase(nextStatus)||!oldPriority.equalsIgnoreCase(nextPriority)
                        ||!java.util.Objects.equals(oldAssignee,assignedAdminId);
                if(stateChanged){
                    try(PreparedStatement p=c.prepareStatement(
                            "INSERT INTO support_ticket_events(ticket_id,admin_user_id,old_status,new_status,old_priority,new_priority,"
                          + "old_assigned_admin_user_id,new_assigned_admin_user_id,note,created_at) VALUES(?,?,?,?,?,?,?,?,?,NOW())")){
                        p.setLong(1,ticketId);p.setLong(2,adminId);p.setString(3,oldStatus);p.setString(4,nextStatus);
                        p.setString(5,oldPriority);p.setString(6,nextPriority);
                        if(oldAssignee==null)p.setNull(7,java.sql.Types.BIGINT);else p.setLong(7,oldAssignee);
                        if(assignedAdminId==null)p.setNull(8,java.sql.Types.BIGINT);else p.setLong(8,assignedAdminId);
                        String note=internal?"Đã thêm ghi chú nội bộ":message;
                        p.setString(9,note.isBlank()?null:note.substring(0,Math.min(1000,note.length())));p.executeUpdate();
                    }
                }
                if((publicReply||!oldStatus.equalsIgnoreCase(nextStatus))&&customerId>0){
                    String notificationMessage=publicReply
                            ? "Admin đã phản hồi ticket "+ticketCode+"."
                            : "Ticket "+ticketCode+" đã chuyển trạng thái: "+nextStatus+".";
                    NotificationService.notifyUser(c,customerId,"SUPPORT_UPDATED","Cập nhật yêu cầu hỗ trợ",
                            notificationMessage,"/support?id="+ticketId,"SUPPORT_TICKET",ticketId);
                }
                c.commit();
            }catch(SQLException|RuntimeException e){try{c.rollback();}catch(SQLException rollback){e.addSuppressed(rollback);}throw e;}
            finally{c.setAutoCommit(true);}
        }catch(SQLException e){throw DataAccessException.of(e);}
    }

    private static List<Map<String,Object>> rows(String sql,long id){
        List<Map<String,Object>> result=new ArrayList<>();
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){
            p.setLong(1,id);
            try(ResultSet r=p.executeQuery()){
                ResultSetMetaData meta=r.getMetaData();
                while(r.next()){
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=meta.getColumnCount();i++){
                        Object value=r.getObject(i);
                        if(value instanceof Timestamp timestamp)value=timestamp.toLocalDateTime().toString().replace('T',' ');
                        row.put(meta.getColumnLabel(i),value);
                    }
                    result.add(row);
                }
            }
            return result;
        }catch(SQLException e){throw DataAccessException.of(e);}
    }

    private static String allowedOrNull(String value,Set<String> allowed){if(value==null||value.isBlank())return null;String v=value.trim().toUpperCase(java.util.Locale.ROOT);return allowed.contains(v)?v:null;}

    private static void insertMessage(Connection c,long id,long sender,String body)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("INSERT INTO support_ticket_messages(ticket_id,sender_user_id,message_body,is_internal,created_at) VALUES (?,?,?,0,NOW())")){p.setLong(1,id);p.setLong(2,sender);p.setString(3,body);p.executeUpdate();}
    }
    private static void setNullableLong(PreparedStatement p,int i,Long value)throws SQLException{if(value==null)p.setNull(i,java.sql.Types.BIGINT);else p.setLong(i,value);}
    private static Long nullableLong(ResultSet r,String field)throws SQLException{Object value=r.getObject(field);return value instanceof Number n?n.longValue():null;}
    private static LocalDateTime databaseNow(Connection c)throws SQLException{
        try(PreparedStatement p=c.prepareStatement("SELECT NOW()");ResultSet r=p.executeQuery()){
            if(!r.next())throw new SQLException("Không lấy được thời gian cơ sở dữ liệu.");
            Timestamp timestamp=r.getTimestamp(1);
            if(timestamp==null)throw new SQLException("Thời gian cơ sở dữ liệu rỗng.");
            return timestamp.toLocalDateTime();
        }
    }
    private static String dateTime(ResultSet r,String field)throws SQLException{var t=r.getTimestamp(field);return t==null?"":t.toLocalDateTime().toString().replace('T',' ');}
}
