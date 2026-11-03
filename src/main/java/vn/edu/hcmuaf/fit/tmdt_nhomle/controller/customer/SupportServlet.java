package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import java.util.Map;
import java.util.Set;

@WebServlet(name="supportServlet",value="/support")
public class SupportServlet extends HttpServlet {
    private static final Set<String> CATEGORIES=Set.of("SUPPORT","COMPLAINT","REFUND","PAYMENT","QUALITY","SHIPPING","ACCOUNT","OTHER");
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        User user=AuthFilter.currentUser(req);if(user==null){res.sendRedirect(req.getContextPath()+"/login");return;}
        req.setAttribute("tickets",SupportDao.listForUser(user.getId()));req.setAttribute("orders",ordersForUser(user.getId()));
        long id=parseLong(req.getParameter("id"),-1);
        if(id>0){Map<String,Object> ticket=SupportDao.findForUser(id,user.getId());if(ticket==null){res.sendError(404);return;}req.setAttribute("ticket",ticket);req.setAttribute("ticketMessages",SupportDao.messagesForUserTicket(id,user.getId()));}
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
                long created=SupportDao.create(user.getId(),orderId,shopId,category,subject,priority,body);
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
        }catch(IllegalArgumentException|DataAccessException e){req.setAttribute("error",e.getMessage()==null?"Không thể xử lý ticket.":e.getMessage());req.setAttribute("tickets",SupportDao.listForUser(user.getId()));req.setAttribute("orders",ordersForUser(user.getId()));if(ticketId>0){req.setAttribute("ticket",SupportDao.findForUser(ticketId,user.getId()));req.setAttribute("ticketMessages",SupportDao.messagesForUserTicket(ticketId,user.getId()));}req.getRequestDispatcher("/customer/support.jsp").forward(req,res);}
    }
    private List<Map<String,Object>> ordersForUser(long userId){List<Map<String,Object>> rows=new ArrayList<>();String sql="SELECT order_id,order_code,status,placed_at FROM orders WHERE user_id=? ORDER BY placed_at DESC LIMIT 100";
        try(Connection c=Database.getConnection();PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,userId);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong("order_id"));m.put("code",r.getString("order_code"));m.put("status",r.getString("status"));rows.add(m);}}return rows;}catch(SQLException e){throw DataAccessException.of(e);}}
    private static String trim(String s){return s==null?"":s.trim();}private static long parseLong(String s,long d){try{return Long.parseLong(s);}catch(RuntimeException e){return d;}}
    private static Long nullableLong(String s){if(s==null||s.isBlank()||"0".equals(s))return null;try{return Long.parseLong(s);}catch(NumberFormatException e){throw new IllegalArgumentException("Mã đơn hàng không hợp lệ.");}}
}
