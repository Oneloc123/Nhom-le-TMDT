package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.UserSessionRegistry;
import java.io.IOException;

@WebServlet(name="userLogoutV2",value="/logout")
public class LogoutController extends UserControllerSupport {
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        HttpSession session=req.getSession(false);User user=currentUser(req);
        if(session!=null){if(user!=null)UserSessionRegistry.unregister(user.getId(),session);session.invalidate();}
        resp.sendRedirect(req.getContextPath()+"/login?loggedOut=1");
    }
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        // Không thực hiện thay đổi trạng thái bằng GET.
        resp.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);resp.setHeader("Allow","POST");
    }
}
