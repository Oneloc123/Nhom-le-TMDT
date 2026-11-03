package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomerAccountService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

@WebServlet(name="customPrintRequestsV2",urlPatterns={"/design","/my-designs","/my-designs/file"})
@MultipartConfig(fileSizeThreshold=1024*1024,maxFileSize=50L*1024*1024,maxRequestSize=55L*1024*1024)
public class CustomPrintController extends UserControllerSupport {
    private static final Set<String> EXTENSIONS=Set.of("stl","obj","step","stp","3mf");
    private static final Set<String> MATERIALS=Set.of("PLA","ABS","PETG","TPU","RESIN");
    private static final Set<String> TECHNOLOGIES=Set.of("FDM","SLA");

    @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        String path=req.getServletPath();
        if("/my-designs/file".equals(path)){serveFile(req,resp);return;}
        User user=currentUser(req);prepare(req);
        if("/my-designs".equals(path)){
            req.setAttribute("requests",CustomerAccountService.listCustomRequests(user.getId()));
            req.getRequestDispatcher("/WEB-INF/views/user/design-list.jsp").forward(req,resp);
        }else req.getRequestDispatcher("/WEB-INF/views/user/design.jsp").forward(req,resp);
    }

    @Override protected void doPost(HttpServletRequest req,HttpServletResponse resp)throws ServletException,IOException{
        User user=currentUser(req);req.setCharacterEncoding("UTF-8");prepare(req);Path stored=null;
        try{
            Part file=req.getPart("modelFile");if(file==null||file.getSize()==0)throw new IllegalArgumentException("Vui lòng chọn file mô hình 3D.");
            if(file.getSize()>50L*1024*1024)throw new IllegalArgumentException("File mô hình tối đa 50 MB.");
            String supplied=file.getSubmittedFileName();String normalizedName=(supplied==null?"model.stl":supplied.replace('\\','/'));String original=normalizedName.substring(normalizedName.lastIndexOf('/')+1);
            if(original.length()>255)original=original.substring(original.length()-255);
            String ext=extension(original);if(!EXTENSIONS.contains(ext))throw new IllegalArgumentException("Chỉ nhận file STL, OBJ, STEP, STP hoặc 3MF.");
            String material=value(req.getParameter("material")).toUpperCase(Locale.ROOT);
            String technology=value(req.getParameter("technology")).toUpperCase(Locale.ROOT);
            if(!MATERIALS.contains(material))throw new IllegalArgumentException("Vật liệu không hợp lệ.");
            if(!TECHNOLOGIES.contains(technology))throw new IllegalArgumentException("Công nghệ in không hợp lệ.");
            int quantity=parseInt(req.getParameter("quantity"),1);if(quantity<1||quantity>100)throw new IllegalArgumentException("Số lượng phải từ 1 đến 100.");
            BigDecimal width=dimension(req.getParameter("widthMm"));BigDecimal height=dimension(req.getParameter("heightMm"));BigDecimal depth=dimension(req.getParameter("depthMm"));
            String note=value(req.getParameter("customerNote"));if(note.length()>1000)throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự.");
            String configured=System.getenv("PK_UPLOAD_DIR");Path root=(configured==null||configured.isBlank())?Path.of(System.getProperty("user.home"),".printkraft-uploads"):Path.of(configured.trim());
            Path folder=root.resolve("designs").normalize();Files.createDirectories(folder);
            String key=UUID.randomUUID().toString().replace("-","")+"."+ext;stored=folder.resolve(key).normalize();if(!stored.startsWith(folder))throw new IllegalArgumentException("Tên file không hợp lệ.");
            String hash;try(var in=file.getInputStream();var out=Files.newOutputStream(stored,StandardOpenOption.CREATE_NEW)){MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){digest.update(buffer,0,n);out.write(buffer,0,n);}hash=HexFormat.of().formatHex(digest.digest());}
            String storageUrl="designs/"+key;String mime=file.getContentType();if(mime==null||mime.isBlank())mime="application/octet-stream";
            Map<String,Object> created=CustomerAccountService.createCustomRequest(user.getId(),original,storageUrl,mime,file.getSize(),hash,material,technology,quantity,width,height,depth,note);
            resp.sendRedirect(req.getContextPath()+"/my-designs?created=1&code="+URLEncoder.encode(String.valueOf(created.get("requestCode")),StandardCharsets.UTF_8));
        }catch(Exception e){if(stored!=null)try{Files.deleteIfExists(stored);}catch(IOException ignored){}
            req.setAttribute("error",e.getMessage()==null?"Không thể gửi yêu cầu in 3D.":e.getMessage());req.getRequestDispatcher("/WEB-INF/views/user/design.jsp").forward(req,resp);}
    }

    private void serveFile(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        User user=currentUser(req);long id=parseLong(req.getParameter("id"));var file=CustomerAccountService.ownedDesignFile(user.getId(),id);
        if(file.isEmpty()){resp.sendError(HttpServletResponse.SC_NOT_FOUND);return;}
        String key=String.valueOf(file.get().get("storage_url"));
        if(!key.matches("designs/[a-f0-9]{32}\\.(stl|obj|step|stp|3mf)")){resp.sendError(HttpServletResponse.SC_NOT_FOUND);return;}
        String configured=System.getenv("PK_UPLOAD_DIR");Path root=(configured==null||configured.isBlank())?Path.of(System.getProperty("user.home"),".printkraft-uploads"):Path.of(configured.trim());
        Path base=root.resolve("designs").normalize();Path actual=root.resolve(key).normalize();if(!actual.startsWith(base)||!Files.isRegularFile(actual)){resp.sendError(HttpServletResponse.SC_NOT_FOUND);return;}
        resp.setContentType("application/octet-stream");resp.setHeader("X-Content-Type-Options","nosniff");
        String name=String.valueOf(file.get().get("original_filename"));resp.setHeader("Content-Disposition","attachment; filename*=UTF-8''"+URLEncoder.encode(name,StandardCharsets.UTF_8).replace("+","%20"));
        resp.setContentLengthLong(Files.size(actual));try(var in=Files.newInputStream(actual);var out=resp.getOutputStream()){in.transferTo(out);}
    }
    private static String extension(String name){int i=name.lastIndexOf('.');return i<0?"":name.substring(i+1).toLowerCase(Locale.ROOT);}
    private static int parseInt(String raw,int fallback){try{return Integer.parseInt(raw);}catch(Exception e){return fallback;}}
    private static long parseLong(String raw){try{return Long.parseLong(raw);}catch(Exception e){return -1;}}
    private static BigDecimal dimension(String raw){if(raw==null||raw.isBlank())return null;try{BigDecimal v=new BigDecimal(raw.trim());if(v.compareTo(BigDecimal.ZERO)<=0||v.compareTo(new BigDecimal("1000"))>0)throw new IllegalArgumentException("Kích thước cần lớn hơn 0 và tối đa 1000 mm.");return v;}catch(NumberFormatException e){throw new IllegalArgumentException("Kích thước mô hình không hợp lệ.");}}
}
