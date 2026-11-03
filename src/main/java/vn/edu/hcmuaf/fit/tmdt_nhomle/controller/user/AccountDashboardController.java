package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomerAccountService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import java.io.IOException;
import java.util.Map;

@WebServlet(name="userDashboardV2",value="/account")
public class AccountDashboardController extends UserControllerSupport {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=currentUser(req);prepare(req);
        User fresh=CustomerAccountService.findUser(user.getId());req.setAttribute("accountUser",fresh);
        Map<String,Object> dashboard=CustomerAccountService.dashboard(user.getId());
        req.setAttribute("dashboard",dashboard);req.setAttribute("defaultAddress",dashboard.get("defaultAddress"));
        req.getRequestDispatcher("/WEB-INF/views/user/dashboard.jsp").forward(req,resp);
    }
}
