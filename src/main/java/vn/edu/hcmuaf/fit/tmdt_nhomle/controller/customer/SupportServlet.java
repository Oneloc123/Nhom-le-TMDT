package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.SupportDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@MultipartConfig(fileSizeThreshold=1024*1024,maxFileSize=8L*1024*1024,maxRequestSize=9L*1024*1024)
@WebServlet(name="supportServlet",value="/support")
public class SupportServlet extends HttpServlet {
    private static final Set<String> CATEGORIES=Set.of("SUPPORT","COMPLAINT","REFUND","PAYMENT","QUALITY","SHIPPING","ACCOUNT","OTHER");
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        User user=AuthFilter.currentUser(req);if(user==null){res.sendRedirect(req.getContextPath()+"/login");return;}
        req.setAttribute("tickets",SupportDao.listForUser(user.getId()));req.setAttribute("orders",ordersForUser(user.getId()));
        long id=parseLong(req.getParameter("id"),-1);
        if(id>0){Map<String,Object> ticket=SupportDao.findForUser(id,user.getId());if(ticket==null){res.sendError(404);return;}req.setAttribute("ticket",ticket);req.setAttribute("ticketMessages",SupportDao.messagesForUserTicket(id,user.getId()));req.setAttribute("ticketAttachments",SupportDao.attachmentsForUser(id,user.getId()));}
        req.getRequestDispatcher("/customer/support.jsp").forward(req,res);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");User user=AuthFilter.currentUser(req);if(user==null){res.sendError(401);return;}
        String action=req.getParameter("action");long ticketId=parseLong(req.getParameter("ticketId"),-1);
        try{
            if("create".equals(action)){
                String category=trim(req.getParameter("category"));String subject=trim(req.getParameter("subject"));String body=trim(req.getParameter("message"));
                if(!CATEGORIES.contains(category))throw new IllegalArgumentException("Loại yêu cầu không hợp lệ.");
                if(subject.isBlank()||subject.length()>180)throw new IllegalArgumentException("Tiêu đề cần từ 1 đến 180 ký tự.");
                if(body.length()<10||body.length()>5000)throw new IllegalArgumentException("Nội dung cần từ 10 đến 5.000 ký tự.");
                String priority=Set.of("LOW","NORMAL","HIGH").contains(req.getParameter("priority"))?req.getParameter("priority"):"NORMAL";
                Long orderId=nullableLong(req.getParameter("orderId"));Long shopId=nullableLong(req.getParameter("shopId"));
                UploadedFile upload=readAttachment(req);long created=SupportDao.create(user.getId(),orderId,shopId,category,subject,priority,body,upload==null?null:upload.fileName(),upload==null?null:upload.contentType(),upload==null?null:upload.bytes());NotificationService.notifyUser(user.getId(),"SUPPORT_TICKET_CREATED","Yêu cầu hỗ trợ đã được gửi","Ticket của bạn đã được tiếp nhận trong hệ thống.","/support?id="+created,"SUPPORT_TICKET",created);
                Map<String,Object> ticket=SupportDao.findForUser(created,user.getId());
                SupportDao.notifyAdmins(subject,created,String.valueOf(ticket.get("code")));
                res.sendRedirect(req.getContextPath()+"/support?id="+created+"&created=1");return;
            }
            if("reply".equals(action)){
                String body=trim(req.getParameter("message"));if(body.length()<1||body.length()>5000)throw new IllegalArgumentException("Phản hồi cần từ 1 đến 5.000 ký tự.");
                SupportDao.reply(ticketId,user.getId(),body);res.sendRedirect(req.getContextPath()+"/support?id="+ticketId+"&replied=1");return;
            }
            if("reopen".equals(action)){SupportDao.reopen(ticketId,user.getId());res.sendRedirect(req.getContextPath()+"/support?id="+ticketId+"&reopened=1");return;}
            if("rate".equals(action)){SupportDao.rate(ticketId,user.getId(),(int)parseLong(req.getParameter("rating"),0));res.sendRedirect(req.getContextPath()+"/support?id="+ticketId+"&rated=1");return;}
            throw new IllegalArgumentException("Thao tác không hợp lệ.");
        }catch(IllegalArgumentException|DataAccessException|IllegalStateException e){req.setAttribute("error",e.getMessage()==null?"Không thể xử lý ticket.":e.getMessage());req.setAttribute("tickets",SupportDao.listForUser(user.getId()));req.setAttribute("orders",ordersForUser(user.getId()));if(ticketId>0){req.setAttribute("ticket",SupportDao.findForUser(ticketId,user.getId()));req.setAttribute("ticketMessages",SupportDao.messagesForUserTicket(ticketId,user.getId()));req.setAttribute("ticketAttachments",SupportDao.attachmentsForUser(ticketId,user.getId()));}req.getRequestDispatcher("/customer/support.jsp").forward(req,res);}
    }

    private static final long MAX_ATTACHMENT_BYTES=8L*1024*1024;
    private record UploadedFile(String fileName,String contentType,byte[] bytes) { }
    private UploadedFile readAttachment(HttpServletRequest req)throws IOException,ServletException{
        Part part=req.getPart("attachment");if(part==null||part.getSize()==0)return null;
        if(part.getSize()>MAX_ATTACHMENT_BYTES)throw new IllegalArgumentException("Tệp đính kèm không được vượt quá 8 MB.");
        String submitted=part.getSubmittedFileName();if(submitted==null||submitted.isBlank())throw new IllegalArgumentException("Tên tệp không hợp lệ.");
        int slash=Math.max(submitted.lastIndexOf('/'),submitted.lastIndexOf((char)92));String name=submitted.substring(slash+1).trim().replace((char)13,'_').replace((char)10,'_');
        if(name.isBlank())throw new IllegalArgumentException("Tên tệp không hợp lệ.");if(name.length()>255)name=name.substring(name.length()-255);
        int dot=name.lastIndexOf('.');String ext=dot<0?"":name.substring(dot+1).toLowerCase(Locale.ROOT);byte[] bytes;
        try(var in=part.getInputStream()){bytes=in.readAllBytes();}
        if(bytes.length==0)return null;if(bytes.length>MAX_ATTACHMENT_BYTES)throw new IllegalArgumentException("Tệp đính kèm không được vượt quá 8 MB.");
        String type=verifyFileType(ext,bytes);if(type==null)throw new IllegalArgumentException("Chỉ nhận ảnh JPG, PNG, WebP hoặc tài liệu PDF hợp lệ.");
        return new UploadedFile(name,type,bytes);
    }
    private static String verifyFileType(String ext,byte[] b){
        if("pdf".equals(ext)&&b.length>=5&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F'&&b[4]=='-')return "application/pdf";
        if("png".equals(ext)&&b.length>=8&&(b[0]&255)==137&&b[1]=='P'&&b[2]=='N'&&b[3]=='G'&&(b[4]&255)==13&&(b[5]&255)==10&&(b[6]&255)==26&&(b[7]&255)==10)return "image/png";
        if(("jpg".equals(ext)||"jpeg".equals(ext))&&b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "image/jpeg";
        if("webp".equals(ext)&&b.length>=12&&b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P')return "image/webp";
        return null;
    }

    private List<Map<String,Object>> ordersForUser(long userId){List<Map<String,Object>> rows=new ArrayList<>();String sql="SELECT order_id,order_code,status,placed_at FROM orders WHERE user_id=? ORDER BY placed_at DESC LIMIT 100";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,userId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("order_id"));m.put("code",r.getString("order_code"));m.put("status",r.getString("status"));rows.add(m);}}return rows;}catch(SQLException e){throw DataAccessException.of(e);}}
    private static String trim(String s){return s==null?"":s.trim();}private static long parseLong(String s,long d){try{return Long.parseLong(s);}catch(RuntimeException e){return d;}}
    private static Long nullableLong(String s){if(s==null||s.isBlank()||"0".equals(s))return null;try{return Long.parseLong(s);}catch(NumberFormatException e){throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");}}
}
