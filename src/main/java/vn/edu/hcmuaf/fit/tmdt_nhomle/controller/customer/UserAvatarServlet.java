package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

@WebServlet(name = "userAvatarServlet", value = "/account-avatar")
public class UserAvatarServlet extends HttpServlet {
    private static final Pattern SAFE_KEY = Pattern.compile("avatars/[a-f0-9]{32}\\.jpg");
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User current = AuthFilter.currentUser(request);
        if (current == null) { response.sendError(401); return; }
        User user = UserDao.findById(current.getId()).orElse(null);
        if (user == null || user.getAvatarUrl() == null || !SAFE_KEY.matcher(user.getAvatarUrl()).matches()) { response.sendError(404); return; }
        String configured = System.getenv("PK_UPLOAD_DIR");
        Path root = configured == null || configured.isBlank() ? Path.of(System.getProperty("user.home"), ".printkraft-uploads") : Path.of(configured.trim());
        Path folder = root.resolve("avatars").normalize(); Path file = root.resolve(user.getAvatarUrl()).normalize();
        if (!file.startsWith(folder) || !Files.isRegularFile(file)) { response.sendError(404); return; }
        response.setContentType("image/jpeg"); response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, no-store"); response.setContentLengthLong(Files.size(file));
        try (var in = Files.newInputStream(file); var out = response.getOutputStream()) { in.transferTo(out); }
    }
}
