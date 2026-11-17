package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.NotificationDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Notification;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;
import java.io.IOException;
import java.util.List;
@WebServlet("/notifications")
public class NotificationsServlet extends HttpServlet {
 private static final int PAGE_SIZE=6;
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
  Long uid=resolveUserId(req,resp);if(uid==null)return;String filter=NotificationDao.normalizeFilter(req.getParameter("filter"));int page=parsePositiveInt(req.getParameter("page"),1);
  List<Notification> list=NotificationDao.listForUser(uid,filter,page,PAGE_SIZE);int total=NotificationDao.countForUser(uid,filter);int unread=NotificationDao.unreadCount(uid);int pages=Math.max(1,(int)Math.ceil((double)total/PAGE_SIZE));
  req.setAttribute("notifications",list);req.setAttribute("unreadNotifications",unread);req.setAttribute("allNotifications",NotificationDao.countForUser(uid,"ALL"));req.setAttribute("readNotifications",NotificationDao.countForUser(uid,"READ"));req.setAttribute("filter",filter);req.setAttribute("page",Math.min(page,pages));req.setAttribute("totalPages",pages);req.setAttribute("unreadMessages",unread);
  req.getRequestDispatcher("/customer/notifications.jsp").forward(req,resp);
 }
 @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
  Long uid=resolveUserId(req,resp);if(uid==null)return;if(!Csrf.validate(req)){resp.sendError(403,"CSRF token không hợp lệ");return;}
  String filter=NotificationDao.normalizeFilter(req.getParameter("filter"));String action=req.getParameter("action");
  if("markAll".equals(action))NotificationDao.markAllRead(uid);
  else if("markOne".equals(action)){int id=parsePositiveInt(req.getParameter("id"),-1);if(id>0)NotificationDao.markRead(uid,id);}
  else{resp.sendError(400,"Thao tác thông báo không hợp lệ");return;}
  resp.sendRedirect(req.getContextPath()+"/notifications?read=1&filter="+filter);
 }
 private Long resolveUserId(HttpServletRequest req,HttpServletResponse resp)throws IOException{Object value=req.getSession().getAttribute("currentUser");if(!(value instanceof User user)){resp.sendRedirect(req.getContextPath()+"/login");return null;}return (long)user.getId();}
 private int parsePositiveInt(String v,int fallback){if(v==null||v.isBlank())return fallback;try{int n=Integer.parseInt(v.trim());return n>0?n:fallback;}catch(NumberFormatException e){return fallback;}}
}
