package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomerAccountService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@WebServlet(name="customerOrdersV2",urlPatterns={"/purchase-history","/order-detail"})
public class CustomerOrdersController extends UserControllerSupport {
    private static final Set<String> FILTERS=Set.of("ALL","Chờ thanh toán","Đã xác nhận","Đang xử lý","Đang giao hàng","Đã hoàn thành","Đã hủy");
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=currentUser(req);prepare(req);String path=req.getServletPath();
        if("/order-detail".equals(path)){
            String code=value(req.getParameter("code"));var order=CustomerAccountService.orderDetail(user.getId(),code);
            if(order.isEmpty()){resp.setStatus(HttpServletResponse.SC_NOT_FOUND);req.getRequestDispatcher("/WEB-INF/views/errors/404.jsp").forward(req,resp);return;}
            req.setAttribute("order",order.get());req.getRequestDispatcher("/WEB-INF/views/user/order-detail.jsp").forward(req,resp);return;
        }
        String status=value(req.getParameter("status"));if(!FILTERS.contains(status))status="ALL";
        int page=parsePage(req.getParameter("page"));int size=10;long total=CustomerAccountService.countOrders(user.getId(),status);
        int totalPages=(int)Math.max(1,Math.ceil(total/(double)size));if(page>totalPages)page=totalPages;
        req.setAttribute("orders",CustomerAccountService.listOrders(user.getId(),status,page,size));req.setAttribute("status",status);
        req.setAttribute("currentPage",page);req.setAttribute("totalPages",totalPages);req.setAttribute("totalOrders",total);
        req.getRequestDispatcher("/WEB-INF/views/user/orders.jsp").forward(req,resp);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=currentUser(req);String code=value(req.getParameter("code"));String action=value(req.getParameter("action"));
        if(!"cancel".equals(action)){resp.sendError(HttpServletResponse.SC_BAD_REQUEST);return;}
        String reason=value(req.getParameter("reason"));
        if(reason.length()>500){resp.sendError(HttpServletResponse.SC_BAD_REQUEST,"Lý do hủy quá dài.");return;}
        boolean cancelled=CustomerAccountService.cancelOrder(user.getId(),code,reason);
        if(!cancelled){req.setAttribute("error","Đơn hàng không còn đủ điều kiện hủy hoặc không thuộc tài khoản của bạn.");}
        else {resp.sendRedirect(req.getContextPath()+"/order-detail?code="+java.net.URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8)+"&cancelled=1");return;}
        prepare(req);var order=CustomerAccountService.orderDetail(user.getId(),code);
        if(order.isEmpty()){resp.sendError(HttpServletResponse.SC_NOT_FOUND);return;}
        req.setAttribute("order",order.get());req.getRequestDispatcher("/WEB-INF/views/user/order-detail.jsp").forward(req,resp);
    }
    private int parsePage(String value){try{return Math.max(1,Integer.parseInt(value));}catch(Exception e){return 1;}}
}
