package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.UserSessionRegistry;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CartService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PortalRedirect;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.UserAuthService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;
import java.io.IOException;

@WebServlet(name="userLoginV2", value="/login")
public class LoginController extends UserControllerSupport {
    @Override protected void doGet(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException{
        if(currentUser(request)!=null){response.sendRedirect(request.getContextPath()+PortalRedirect.afterLogin(currentUser(request)));return;}
        prepare(request);request.getRequestDispatcher("/WEB-INF/views/user/login.jsp").forward(request,response);
    }
    @Override protected void doPost(HttpServletRequest request,HttpServletResponse response)throws ServletException,IOException{
        request.setCharacterEncoding("UTF-8");prepare(request);
        String email=value(request.getParameter("email"));String password=request.getParameter("password");
        try{
            User user=UserAuthService.authenticate(email,password,request.getRemoteAddr(),request.getHeader("User-Agent"));
            HttpSession old=request.getSession(false);
            // Gộp giỏ khách trước khi thay session để không mất sản phẩm đã chọn.
            try{new CartService().mergeGuestCart(old,user);}catch(RuntimeException e){
                System.getLogger(LoginController.class.getName()).log(System.Logger.Level.WARNING,"Không thể gộp giỏ khách vào tài khoản.",e);
            }
            if(old!=null)old.invalidate();
            HttpSession session=request.getSession(true);session.setMaxInactiveInterval(30*60);
            session.setAttribute("currentUser",user);session.setAttribute("csrfToken",Csrf.ensureToken(request));
            UserSessionRegistry.register(user.getId(),session);
            String target=safeRedirect(request.getParameter("redirect"),PortalRedirect.afterLogin(user));
            response.sendRedirect(request.getContextPath()+target);
        }catch(RuntimeException e){
            request.setAttribute("error",e.getMessage()==null?"Không thể đăng nhập lúc này.":e.getMessage());
            request.setAttribute("email",email);request.getRequestDispatcher("/WEB-INF/views/user/login.jsp").forward(request,response);
        }
    }
}
