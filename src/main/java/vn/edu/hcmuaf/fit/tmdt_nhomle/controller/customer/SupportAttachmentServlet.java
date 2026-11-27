package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.SupportDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
@WebServlet(name="supportAttachmentServlet",value="/support/attachment")
public class SupportAttachmentServlet extends HttpServlet {
 @Override protected void doGet(HttpServletRequest req,HttpServletResponse resp)throws IOException{
  User user=AuthFilter.currentUser(req);if(user==null){resp.sendRedirect(req.getContextPath()+"/login");return;}
  long id;try{id=Long.parseLong(req.getParameter("id"));}catch(RuntimeException e){resp.sendError(404);return;}if(id<=0){resp.sendError(404);return;}
  SupportDao.Attachment file=user.isAdmin()?SupportDao.findAttachmentForAdmin(id):SupportDao.findAttachmentForUser(id,user.getId());
  if(file==null||file.data()==null){resp.sendError(404);return;}String name=URLEncoder.encode(file.fileName(),StandardCharsets.UTF_8).replace("+","%20");
  resp.setContentType(file.contentType());resp.setHeader("X-Content-Type-Options","nosniff");resp.setHeader("Cache-Control","private, no-store");
  resp.setHeader("Content-Disposition","attachment; filename=\"attachment\"; filename*=UTF-8''"+name);resp.setContentLengthLong(file.data().length);resp.getOutputStream().write(file.data());
 }
}
