package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.SupportDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import java.io.IOException;
import java.util.Set;

@WebServlet(name="adminTicketServlet", value="/admin/tickets")
public class AdminTicketServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        User admin=AuthFilter.currentUser(req);if(admin==null||!admin.isAdmin()){res.sendError(403);return;}
        req.setAttribute("tickets",SupportDao.listForAdmin(req.getParameter("status"),req.getParameter("priority"),req.getParameter("q")));
        long id=parseId(req.getParameter("id"));
        if(id>0){var ticket=SupportDao.findForAdmin(id);if(ticket==null){res.sendError(404);return;}req.setAttribute("ticket",ticket);req.setAttribute("ticketMessages",SupportDao.messagesForAdmin(id));}
        req.getRequestDispatcher("/WEB-INF/admin/tickets.jsp").forward(req,res);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse res)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");User admin=AuthFilter.currentUser(req);if(admin==null||!admin.isAdmin()){res.sendError(403);return;}
        long id=parseId(req.getParameter("ticketId"));if(id<1){res.sendError(400);return;}
        try{
            String status=value(req.getParameter("status"),"IN_PROGRESS").toUpperCase(java.util.Locale.ROOT);
            String priority=value(req.getParameter("priority"),"NORMAL").toUpperCase(java.util.Locale.ROOT);
            Long assigned=null;String raw=req.getParameter("assignedAdminId");if(raw!=null&&!raw.isBlank())try{assigned=Long.parseLong(raw);if(assigned<=0)throw new NumberFormatException();}catch(NumberFormatException ex){throw new IllegalArgumentException("Mã Admin được giao không hợp lệ.");}
            boolean internal="1".equals(req.getParameter("internal"));
            SupportDao.updateByAdmin(id,admin.getId(),status,priority,assigned,req.getParameter("reply"),internal);
            res.sendRedirect(req.getContextPath()+"/admin/tickets?id="+id+"&updated=1");
        }catch(IllegalArgumentException|DataAccessException e){req.setAttribute("error",e.getMessage());req.setAttribute("tickets",SupportDao.listForAdmin(req.getParameter("status"),req.getParameter("priority"),req.getParameter("q")));var ticket=SupportDao.findForAdmin(id);req.setAttribute("ticket",ticket);req.setAttribute("ticketMessages",SupportDao.messagesForAdmin(id));req.getRequestDispatcher("/WEB-INF/admin/tickets.jsp").forward(req,res);}
    }
    private static long parseId(String raw){try{return Long.parseLong(raw);}catch(Exception e){return -1;}}
    private static String value(String raw,String fallback){return raw==null||raw.isBlank()?fallback:raw.trim();}
}
