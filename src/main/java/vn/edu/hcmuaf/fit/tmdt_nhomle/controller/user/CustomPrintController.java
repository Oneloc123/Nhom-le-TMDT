package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.DesignFileDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ShopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.SuggestedShop;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomerAccountService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ShopRecommendationService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.AjaxResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.security.MessageDigest;

@WebServlet(name = "customPrintRequestsV2", urlPatterns = {"/design", "/design-request", "/my-designs", "/my-designs/file"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,
        maxFileSize = 100L * 1024 * 1024,
        maxRequestSize = 110L * 1024 * 1024
)
public class CustomPrintController extends UserControllerSupport {
    private static final Set<String> EXTENSIONS = Set.of("stl", "obj", "step", "stp", "3mf");
    private static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("1000000000.00");

    private final ShopRecommendationService recommendationService = new ShopRecommendationService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/my-designs/file".equals(path)) {
            serveFile(req, resp);
            return;
        }
        if ("/design".equals(path) && "suitableShops".equalsIgnoreCase(req.getParameter("action"))) {
            handleSuitableShops(req, resp);
            return;
        }

        User user = currentUser(req);
        prepare(req);
        if ("/my-designs".equals(path)) {
            req.setAttribute("requests", CustomerAccountService.listCustomRequests(user.getId()));
            req.getRequestDispatcher("/WEB-INF/views/user/design-list.jsp").forward(req, resp);
        } else if ("/design-request".equals(path)) {
            req.getRequestDispatcher("/WEB-INF/views/user/design.jsp").forward(req, resp);
        } else {
            req.getRequestDispatcher("/customer/design.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if ("/design-request".equals(req.getServletPath())) {
            handleQuoteRequest(req, resp);
            return;
        }
        if (!"/design".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String action = req.getParameter("action");
        if ("order".equalsIgnoreCase(action) || "uploadAndOrder".equalsIgnoreCase(action)) {
            handleUploadAndOrder(req, resp);
            return;
        }
        AjaxResponse.send(resp, HttpServletResponse.SC_BAD_REQUEST,
                "{\"success\":false,\"message\":\"Thao tác không được hỗ trợ.\"}");
    }

    private void handleSuitableShops(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String tech = req.getParameter("tech");
        String material = req.getParameter("material");
        double sizeX = parseDimension(req.getParameter("sizeX"));
        double sizeY = parseDimension(req.getParameter("sizeY"));
        double sizeZ = parseDimension(req.getParameter("sizeZ"));
        String ext = req.getParameter("ext");

        List<SuggestedShop> shops = recommendationService.getSuggestedShops(tech, material, sizeX, sizeY, sizeZ, ext);
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < shops.size(); i++) {
            SuggestedShop s = shops.get(i);
            if (i > 0) json.append(',');
            json.append('{')
                    .append("\"shopId\":").append(s.getShopId()).append(',')
                    .append("\"shopName\":").append(AjaxResponse.quote(s.getShopName())).append(',')
                    .append("\"slug\":").append(AjaxResponse.quote(s.getSlug() == null ? "" : s.getSlug())).append(',')
                    .append("\"location\":").append(AjaxResponse.quote(s.getLocation() == null ? "" : s.getLocation())).append(',')
                    .append("\"rating\":").append(Double.isFinite(s.getRating()) ? s.getRating() : 0).append(',')
                    .append("\"completedOrders\":").append(s.getCompletedOrders()).append(',')
                    .append("\"estimatedLeadDays\":").append(s.getEstimatedLeadDays() == null ? 3 : s.getEstimatedLeadDays()).append(',')
                    .append("\"capabilities\":").append(AjaxResponse.quote(s.getCapabilities() == null ? "" : s.getCapabilities())).append(',')
                    .append("\"maxBuildDimensions\":").append(AjaxResponse.quote(s.getMaxBuildDimensions() == null ? "" : s.getMaxBuildDimensions())).append(',')
                    .append("\"matchScore\":").append(s.getMatchScore()).append(',')
                    .append("\"matchBadge\":").append(AjaxResponse.quote(s.getMatchBadge() == null ? "" : s.getMatchBadge())).append(',')
                    .append("\"matchReason\":").append(AjaxResponse.quote(s.getMatchReason() == null ? "" : s.getMatchReason())).append(',')
                    .append("\"bestMatch\":").append(s.isBestMatch())
                    .append('}');
        }
        json.append(']');
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(json.toString());
    }

    private void handleUploadAndOrder(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        User user = currentUser(req);
        if (user == null) {
            String loginUrl = req.getContextPath() + "/login?next="
                    + URLEncoder.encode("/design", StandardCharsets.UTF_8);
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.getWriter().write("{\"success\":false,\"requireLogin\":true,\"loginUrl\":"
                    + AjaxResponse.quote(loginUrl) + ",\"message\":\"Vui lòng đăng nhập để tiến hành đặt đơn.\"}");
            return;
        }

        Path stored = null;
        boolean designRecordCreated = false;
        try {
            long shopId = parseLong(req.getParameter("shopId"), -1);
            if (shopId <= 0 || ShopDao.findById(shopId).isEmpty()) {
                throw new IllegalArgumentException("Vui lòng chọn một xưởng in đang hoạt động.");
            }

            int quantity = parseInt(req.getParameter("quantity"), 1);
            if (quantity < 1 || quantity > 100_000) {
                throw new IllegalArgumentException("Số lượng đặt in phải từ 1 đến 100.000.");
            }
            BigDecimal unitPrice = parsePrice(req.getParameter("unitPrice"));
            String customTitle = value(req.getParameter("customTitle"));
            if (customTitle.isBlank()) customTitle = "In 3D theo yêu cầu";
            if (customTitle.length() > 240) customTitle = customTitle.substring(0, 240);

            Part part = req.getPart("file");
            if (part == null || part.getSize() <= 0) {
                throw new IllegalArgumentException("Vui lòng chọn hoặc kéo thả tệp thiết kế 3D.");
            }
            if (part.getSize() > 100L * 1024 * 1024) {
                throw new IllegalArgumentException("Dung lượng tệp tối đa là 100 MB.");
            }

            String submitted = part.getSubmittedFileName();
            if (submitted == null || submitted.isBlank()) submitted = "model.stl";
            String cleaned = submitted.replace('\\', '/');
            cleaned = cleaned.substring(cleaned.lastIndexOf('/') + 1).trim();
            if (cleaned.isBlank()) cleaned = "model.stl";
            if (cleaned.length() > 255) cleaned = cleaned.substring(cleaned.length() - 255);
            String ext = extension(cleaned);
            if (!EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException("Định dạng chưa được hỗ trợ. Chỉ nhận STL, OBJ, STEP, STP hoặc 3MF.");
            }

            Path root = uploadRoot();
            Path folder = root.resolve("designs").normalize();
            Files.createDirectories(folder);
            String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            stored = folder.resolve(storedName).normalize();
            if (!stored.startsWith(folder)) throw new IllegalArgumentException("Tên tệp không hợp lệ.");
            try (var input = part.getInputStream()) {
                Files.copy(input, stored);
            }

            long designFileId;
            try {
                designFileId = DesignFileDao.insert((long)user.getId(), null, cleaned, "designs/" + storedName, null, "ACTIVE");
                designRecordCreated = true;
            } catch (RuntimeException e) {
                Files.deleteIfExists(stored);
                stored = null;
                throw e;
            }

            String checkoutUrl = req.getContextPath() + "/order?mode=customDesign"
                    + "&shopId=" + shopId
                    + "&designFileId=" + designFileId
                    + "&customTitle=" + URLEncoder.encode(customTitle, StandardCharsets.UTF_8)
                    + "&unitPrice=" + unitPrice.toPlainString()
                    + "&quantity=" + quantity;
            resp.getWriter().write("{\"success\":true,\"checkoutUrl\":" + AjaxResponse.quote(checkoutUrl) + "}");
        } catch (IllegalArgumentException e) {
            if (stored != null && !designRecordCreated) {
                try { Files.deleteIfExists(stored); } catch (IOException ignored) { }
            }
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            if (stored != null && !designRecordCreated) {
                try { Files.deleteIfExists(stored); } catch (IOException ignored) { }
            }
            System.getLogger(CustomPrintController.class.getName()).log(System.Logger.Level.ERROR,
                    "Unable to create custom print checkout", e);
            writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Chưa thể tạo đơn in 3D. Vui lòng thử lại sau.");
        }
    }

    private void handleQuoteRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        User user = currentUser(req);
        if (user == null) {
            resp.sendRedirect(req.getContextPath() + "/login");
            return;
        }
        prepare(req);
        Path stored = null;
        boolean quoteCreated = false;
        try {
            Part file = req.getPart("modelFile");
            if (file == null || file.getSize() <= 0) {
                throw new IllegalArgumentException("Vui lòng chọn file mô hình 3D.");
            }
            if (file.getSize() > 50L * 1024 * 1024) {
                throw new IllegalArgumentException("File mô hình tối đa 50 MB.");
            }
            String supplied = file.getSubmittedFileName();
            String normalized = (supplied == null ? "model.stl" : supplied.replace('\\', '/'));
            String original = normalized.substring(normalized.lastIndexOf('/') + 1);
            if (original.isBlank()) original = "model.stl";
            if (original.length() > 255) original = original.substring(original.length() - 255);
            String ext = extension(original);
            if (!EXTENSIONS.contains(ext)) {
                throw new IllegalArgumentException("Chỉ nhận file STL, OBJ, STEP, STP hoặc 3MF.");
            }

            String material = value(req.getParameter("material")).toUpperCase(Locale.ROOT);
            String technology = value(req.getParameter("technology")).toUpperCase(Locale.ROOT);
            if (!Set.of("PLA", "ABS", "PETG", "TPU", "RESIN").contains(material)) {
                throw new IllegalArgumentException("Vật liệu không hợp lệ.");
            }
            if (!Set.of("FDM", "SLA").contains(technology)) {
                throw new IllegalArgumentException("Công nghệ in không hợp lệ.");
            }
            int quantity = parseInt(req.getParameter("quantity"), 1);
            if (quantity < 1 || quantity > 100) {
                throw new IllegalArgumentException("Số lượng phải từ 1 đến 100.");
            }
            BigDecimal width = dimension(req.getParameter("widthMm"));
            BigDecimal height = dimension(req.getParameter("heightMm"));
            BigDecimal depth = dimension(req.getParameter("depthMm"));
            String note = value(req.getParameter("customerNote"));
            if (note.length() > 1000) throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự.");

            Path root = uploadRoot();
            Path folder = root.resolve("designs").normalize();
            Files.createDirectories(folder);
            String key = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            stored = folder.resolve(key).normalize();
            if (!stored.startsWith(folder)) throw new IllegalArgumentException("Tên file không hợp lệ.");

            String hash;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = file.getInputStream();
                 var output = Files.newOutputStream(stored, StandardOpenOption.CREATE_NEW)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
                hash = HexFormat.of().formatHex(digest.digest());
            }
            String storageUrl = "designs/" + key;
            String mime = file.getContentType();
            if (mime == null || mime.isBlank()) mime = "application/octet-stream";
            Map<String, Object> created = CustomerAccountService.createCustomRequest(
                    user.getId(), original, storageUrl, mime, file.getSize(), hash,
                    material, technology, quantity, width, height, depth, note);
            quoteCreated = true;
            String code = String.valueOf(created.get("requestCode"));
            resp.sendRedirect(req.getContextPath() + "/my-designs?created=1&code="
                    + URLEncoder.encode(code, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            if (stored != null && !quoteCreated) {
                try { Files.deleteIfExists(stored); } catch (IOException ignored) { }
            }
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/user/design.jsp").forward(req, resp);
        } catch (Exception e) {
            if (stored != null && !quoteCreated) {
                try { Files.deleteIfExists(stored); } catch (IOException ignored) { }
            }
            System.getLogger(CustomPrintController.class.getName()).log(System.Logger.Level.ERROR,
                    "Unable to create custom print quote request", e);
            req.setAttribute("error", "Không thể gửi yêu cầu in 3D lúc này. Vui lòng thử lại sau.");
            req.getRequestDispatcher("/WEB-INF/views/user/design.jsp").forward(req, resp);
        }
    }

    private static BigDecimal dimension(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            BigDecimal value = new BigDecimal(raw.trim());
            if (value.compareTo(BigDecimal.ZERO) <= 0 || value.compareTo(new BigDecimal("1000")) > 0) {
                throw new IllegalArgumentException("Kích thước cần lớn hơn 0 và tối đa 1000 mm.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Kích thước mô hình không hợp lệ.");
        }
    }

    private void serveFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = currentUser(req);
        if (user == null) { resp.sendError(HttpServletResponse.SC_UNAUTHORIZED); return; }
        long id = parseLong(req.getParameter("id"), -1);
        var file = CustomerAccountService.ownedDesignFile(user.getId(), id);
        if (file.isEmpty()) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        String key = String.valueOf(file.get().get("storage_url"));
        if (!key.matches("designs/[a-f0-9]{32}\\.(stl|obj|step|stp|3mf)")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path root = uploadRoot();
        Path base = root.resolve("designs").normalize();
        Path actual = root.resolve(key).normalize();
        if (!actual.startsWith(base) || !Files.isRegularFile(actual)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("application/octet-stream");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        String name = String.valueOf(file.get().get("original_filename"));
        resp.setHeader("Content-Disposition", "attachment; filename*=UTF-8''"
                + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20"));
        resp.setContentLengthLong(Files.size(actual));
        try (var in = Files.newInputStream(actual); var out = resp.getOutputStream()) {
            in.transferTo(out);
        }
    }

    private static Path uploadRoot() {
        String configured = System.getenv("PK_UPLOAD_DIR");
        return (configured == null || configured.isBlank())
                ? Path.of(System.getProperty("user.home"), ".printkraft-uploads").toAbsolutePath().normalize()
                : Path.of(configured.trim()).toAbsolutePath().normalize();
    }

    private static void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        resp.getWriter().write("{\"success\":false,\"message\":"
                + AjaxResponse.quote(message == null || message.isBlank() ? "Dữ liệu đặt in không hợp lệ." : message) + "}");
    }

    private static BigDecimal parsePrice(String raw) {
        try {
            if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Đơn giá không hợp lệ.");
            BigDecimal price = new BigDecimal(raw.trim()).setScale(2, RoundingMode.HALF_UP);
            if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(MAX_UNIT_PRICE) > 0) {
                throw new IllegalArgumentException("Đơn giá phải lớn hơn 0 và không vượt quá 1 tỷ đồng.");
            }
            return price;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Đơn giá không hợp lệ.");
        }
    }

    private static double parseDimension(String raw) {
        try {
            if (raw == null || raw.isBlank()) return 0;
            double value = Double.parseDouble(raw.trim());
            return Double.isFinite(value) && value > 0 && value <= 10_000 ? value : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseInt(String raw, int fallback) {
        try { return raw == null || raw.isBlank() ? fallback : Integer.parseInt(raw.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static long parseLong(String raw, long fallback) {
        try { return raw == null || raw.isBlank() ? fallback : Long.parseLong(raw.trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

}
