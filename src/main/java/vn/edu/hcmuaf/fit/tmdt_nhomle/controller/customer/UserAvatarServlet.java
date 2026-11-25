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
    private static final Pattern STORED_KEY = Pattern.compile("Store/Avatar/([a-f0-9]{32}\\.jpg)");
    private static final Pattern LEGACY_KEY = Pattern.compile("avatars/([a-f0-9]{32}\\.jpg)");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User current = AuthFilter.currentUser(request);
        if (current == null) { response.sendError(401); return; }
        User user = UserDao.findById(current.getId()).orElse(null);
        if (user == null || user.getAvatarUrl() == null) { response.sendError(404); return; }

        Path file;
        var stored = STORED_KEY.matcher(user.getAvatarUrl());
        var legacy = LEGACY_KEY.matcher(user.getAvatarUrl());
        if (stored.matches()) {
            Path folder = avatarFolder(request);
            if (folder == null) { response.sendError(404); return; }
            file = folder.resolve(stored.group(1)).normalize();
            if (!file.startsWith(folder)) { response.sendError(404); return; }
        } else if (legacy.matches()) {
            String configured = System.getenv("PK_UPLOAD_DIR");
            Path root = configured == null || configured.isBlank()
                    ? Path.of(System.getProperty("user.home"), ".printkraft-uploads")
                    : Path.of(configured.trim());
            Path folder = root.resolve("avatars").normalize();
            file = folder.resolve(legacy.group(1)).normalize();
            if (!file.startsWith(folder)) { response.sendError(404); return; }
        } else {
            response.sendError(404);
            return;
        }

        if (!Files.isRegularFile(file)) { response.sendError(404); return; }
        response.setContentType("image/jpeg");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, no-store");
        response.setContentLengthLong(Files.size(file));
        try (var in = Files.newInputStream(file); var out = response.getOutputStream()) { in.transferTo(out); }
    }

    private static Path avatarFolder(HttpServletRequest request) {
        String configured = System.getenv("PK_AVATAR_DIR");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim()).toAbsolutePath().normalize();
        }
        String webPath = request.getServletContext().getRealPath("/Store/Avatar");
        if (webPath == null || webPath.isBlank()) return null;
        return Path.of(webPath).toAbsolutePath().normalize();
    }
}
