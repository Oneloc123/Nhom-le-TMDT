package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomerAccountService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.UserSessionRegistry;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.UserAuthService;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.util.Locale;
import java.util.Iterator;

@WebServlet(name="userProfileV2",value="/profile")
@MultipartConfig(maxFileSize=3*1024*1024,maxRequestSize=4*1024*1024)
public class ProfileController extends UserControllerSupport {
    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        render(req,resp,null,null);
    }
    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        req.setCharacterEncoding("UTF-8");User current=currentUser(req);String action=value(req.getParameter("action"));
        try{
            User saved=CustomerAccountService.findUser(current.getId());
            switch(action){
                case "profile" -> updateProfile(req,saved);
                case "password" -> {
                    UserAuthService.changePassword(saved,req.getParameter("currentPassword"),req.getParameter("newPassword"));
                    HttpSession session=req.getSession(false);if(session!=null)UserSessionRegistry.invalidateOthers(saved.getId(),session);
                    saved=CustomerAccountService.findUser(saved.getId());req.getSession(false).setAttribute("currentUser",saved);
                    render(req,resp,"Mật khẩu đã được cập nhật.",null);return;
                }
                case "avatar" -> updateAvatar(req,saved);
                case "deleteAccount" -> {
                    UserAuthService.deleteAccount(saved,req.getParameter("deletePassword"));
                    HttpSession session=req.getSession(false);
                    if(session!=null){try{session.invalidate();}catch(IllegalStateException ignored){}}
                    resp.sendRedirect(req.getContextPath()+"/home?accountDeleted=1");return;
                }
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
            saved=CustomerAccountService.findUser(saved.getId());HttpSession s=req.getSession(false);if(s!=null)s.setAttribute("currentUser",saved);
            render(req,resp,"Thông tin tài khoản đã được cập nhật.",null);
        }catch(RuntimeException ex){render(req,resp,null,ex.getMessage()==null?"Không thể cập nhật thông tin.":ex.getMessage());}
    }
    private void updateProfile(HttpServletRequest req,User user){
        CustomerAccountService.updateProfile(user.getId(),req.getParameter("fullName"),req.getParameter("email"),
                req.getParameter("phone"),req.getParameter("birthDate"),req.getParameter("gender"),
                req.getParameter("notifyEmail")!=null);
    }
    private void updateAvatar(HttpServletRequest req,User user)throws ServletException,IOException{
        Part part=req.getPart("avatar");if(part==null||part.getSize()==0)throw new IllegalArgumentException("Vui lòng chọn ảnh đại diện.");
        if(part.getSize()>3L*1024*1024)throw new IllegalArgumentException("Ảnh đại diện tối đa 3 MB.");
        BufferedImage image;
        try (var input = part.getInputStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) throw new IllegalArgumentException("Không đọc được ảnh đã tải lên.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG hoặc PNG hợp lệ.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (!(format.equals("jpeg") || format.equals("jpg") || format.equals("png")))
                    throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG hoặc PNG.");
                if (width <= 0 || height <= 0 || width > 4000 || height > 4000 || (long) width * height > 16_000_000L)
                    throw new IllegalArgumentException("Kích thước ảnh tối đa 4000 × 4000 px và 16 megapixel.");
                image = reader.read(0);
            } finally { reader.dispose(); }
        }
        if(image==null)throw new IllegalArgumentException("Tệp tải lên không phải ảnh hợp lệ.");
        String key=UUID.randomUUID().toString().replace("-","")+".jpg";
        String configured=System.getenv("PK_UPLOAD_DIR");Path root=(configured==null||configured.isBlank())?Path.of(System.getProperty("user.home"),".printkraft-uploads"):Path.of(configured.trim());
        Path folder=root.resolve("avatars").normalize();Files.createDirectories(folder);Path target=folder.resolve(key).normalize();
        if(!target.startsWith(folder))throw new IllegalArgumentException("Đường dẫn ảnh không hợp lệ.");
        if(!ImageIO.write(image,"jpg",target.toFile()))throw new IllegalArgumentException("Không thể xử lý ảnh này.");
        CustomerAccountService.updateAvatar(user.getId(),"avatars/"+key);
    }
    private void render(HttpServletRequest req,HttpServletResponse resp,String success,String error)throws ServletException,IOException{
        prepare(req);User user=CustomerAccountService.findUser(currentUser(req).getId());req.setAttribute("accountUser",user);
        req.setAttribute("success",success);req.setAttribute("error",error);
        req.setAttribute("loginHistory",CustomerAccountService.loginHistory(user.getId(),10));
        req.getRequestDispatcher("/WEB-INF/views/user/profile.jsp").forward(req,resp);
    }
}
