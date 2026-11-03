package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.UserAuthService;
import java.io.IOException;

@WebServlet(name="userForgotPasswordV2",value="/forgot-password")
public class ForgotPasswordController extends UserControllerSupport {
    private static final String GENERIC="Nếu email tồn tại và đang hoạt động, mã xác thực đã được gửi. Vui lòng kiểm tra hộp thư.";
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        prepare(req);req.getRequestDispatcher("/WEB-INF/views/user/forgot-password.jsp").forward(req,resp);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");prepare(req);
        String action=value(req.getParameter("action"));String email=value(req.getParameter("email"));
        try{
            if("send".equals(action)){
                if(email.isBlank()||email.length()>190)throw new IllegalArgumentException("Vui lòng nhập email hợp lệ.");
                UserAuthService.requestPasswordReset(email);req.setAttribute("step","reset");req.setAttribute("email",email);req.setAttribute("info",GENERIC);
            }else if("reset".equals(action)){
                String pass=req.getParameter("password");String confirm=req.getParameter("confirmPassword");
                if(pass==null||!pass.equals(confirm))throw new IllegalArgumentException("Mật khẩu xác nhận không khớp.");
                if(!UserAuthService.resetPassword(email,req.getParameter("otp"),pass))throw new IllegalArgumentException("Mã OTP không đúng, đã hết hạn hoặc đã vượt số lần thử.");
                req.setAttribute("resetOk","Đặt lại mật khẩu thành công. Hãy đăng nhập bằng mật khẩu mới.");
                req.getRequestDispatcher("/WEB-INF/views/user/login.jsp").forward(req,resp);return;
            }else throw new IllegalArgumentException("Thao tác không hợp lệ.");
        }catch(RuntimeException e){req.setAttribute("error",e.getMessage());req.setAttribute("step","reset");req.setAttribute("email",email);}
        req.getRequestDispatcher("/WEB-INF/views/user/forgot-password.jsp").forward(req,resp);
    }
}
