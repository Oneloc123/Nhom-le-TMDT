package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.DesignFileDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.SuggestedShop;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ShopRecommendationService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.AjaxResponse;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@WebServlet(name = "designServlet", value = "/design")
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,        // 1 MB
        maxFileSize = 100 * 1024 * 1024,       // 100 MB
        maxRequestSize = 110 * 1024 * 1024     // 110 MB
)
public class DesignServlet extends HttpServlet {

    private final ShopRecommendationService recommendationService = new ShopRecommendationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String action = request.getParameter("action");
        if ("suitableShops".equalsIgnoreCase(action)) {
            handleSuitableShops(request, response);
            return;
        }

        request.getRequestDispatcher("/customer/design.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String action = request.getParameter("action");

        if ("order".equalsIgnoreCase(action) || "uploadAndOrder".equalsIgnoreCase(action)) {
            handleUploadAndOrder(request, response);
            return;
        }

        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Thao tác không được hỗ trợ.");
    }

    private void handleSuitableShops(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String tech = request.getParameter("tech");
        String material = request.getParameter("material");
        double sizeX = parseDouble(request.getParameter("sizeX"), 0);
        double sizeY = parseDouble(request.getParameter("sizeY"), 0);
        double sizeZ = parseDouble(request.getParameter("sizeZ"), 0);
        String ext = request.getParameter("ext");

        List<SuggestedShop> shops = recommendationService.getSuggestedShops(tech, material, sizeX, sizeY, sizeZ, ext);

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < shops.size(); i++) {
            SuggestedShop s = shops.get(i);
            if (i > 0) json.append(",");
            json.append("{")
                    .append("\"shopId\":").append(s.getShopId()).append(",")
                    .append("\"shopName\":").append(AjaxResponse.quote(s.getShopName())).append(",")
                    .append("\"slug\":").append(AjaxResponse.quote(s.getSlug() == null ? "" : s.getSlug())).append(",")
                    .append("\"location\":").append(AjaxResponse.quote(s.getLocation() == null ? "" : s.getLocation())).append(",")
                    .append("\"rating\":").append(s.getRating()).append(",")
                    .append("\"completedOrders\":").append(s.getCompletedOrders()).append(",")
                    .append("\"estimatedLeadDays\":").append(s.getEstimatedLeadDays() == null ? 3 : s.getEstimatedLeadDays()).append(",")
                    .append("\"capabilities\":").append(AjaxResponse.quote(s.getCapabilities() == null ? "" : s.getCapabilities())).append(",")
                    .append("\"maxBuildDimensions\":").append(AjaxResponse.quote(s.getMaxBuildDimensions() == null ? "" : s.getMaxBuildDimensions())).append(",")
                    .append("\"matchScore\":").append(s.getMatchScore()).append(",")
                    .append("\"matchBadge\":").append(AjaxResponse.quote(s.getMatchBadge() == null ? "" : s.getMatchBadge())).append(",")
                    .append("\"matchReason\":").append(AjaxResponse.quote(s.getMatchReason() == null ? "" : s.getMatchReason())).append(",")
                    .append("\"bestMatch\":").append(s.isBestMatch())
                    .append("}");
        }
        json.append("]");

        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(json.toString());
    }

    private void handleUploadAndOrder(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json;charset=UTF-8");

        User user = AuthFilter.currentUser(request);
        if (user == null) {
            String loginUrl = request.getContextPath() + "/login?next=" + URLEncoder.encode("/design", StandardCharsets.UTF_8);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"success\":false,\"requireLogin\":true,\"loginUrl\":\"" + loginUrl + "\",\"message\":\"Vui lòng đăng nhập để tiến hành đặt đơn.\"}");
            return;
        }

        Part filePart = null;
        try {
            filePart = request.getPart("file");
        } catch (Exception ignored) {
        }

        if (filePart == null || filePart.getSize() == 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng chọn hoặc kéo thả tệp thiết kế 3D.\"}");
            return;
        }

        String submittedName = filePart.getSubmittedFileName();
        if (submittedName == null || submittedName.isBlank()) {
            submittedName = "model.stl";
        }
        String cleanName = Paths.get(submittedName).getFileName().toString();
        String ext = cleanName.contains(".") ? cleanName.substring(cleanName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!List.of("stl", "obj", "step", "stp").contains(ext)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Định dạng tệp không được hỗ trợ. Chỉ nhận .stl, .obj, .step, .stp.\"}");
            return;
        }

        // Lưu file vật lý
        String uploadDirRealPath = getServletContext().getRealPath("/uploads/designs");
        Path uploadDirPath;
        if (uploadDirRealPath != null) {
            uploadDirPath = Paths.get(uploadDirRealPath);
        } else {
            uploadDirPath = Paths.get(System.getProperty("user.dir"), "uploads", "designs");
        }
        if (!Files.exists(uploadDirPath)) {
            Files.createDirectories(uploadDirPath);
        }

        String storedFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + cleanName;
        Path targetPath = uploadDirPath.resolve(storedFileName);
        Files.copy(filePart.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String storageUrl = "/uploads/designs/" + storedFileName;
        long designFileId = DesignFileDao.insert((long) user.getId(), null, cleanName, storageUrl, null, "ACTIVE");

        long shopId = parseLong(request.getParameter("shopId"), 0L);
        if (shopId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Vui lòng chọn xưởng in thực hiện.\"}");
            return;
        }

        int quantity = (int) parseLong(request.getParameter("quantity"), 1L);
        if (quantity < 1) quantity = 1;

        BigDecimal unitPrice = parseBigDecimal(request.getParameter("unitPrice"));
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"success\":false,\"message\":\"Đơn giá không hợp lệ.\"}");
            return;
        }

        String customTitle = request.getParameter("customTitle");
        if (customTitle == null || customTitle.isBlank()) {
            customTitle = "In 3D: " + cleanName;
        }

        String checkoutUrl = request.getContextPath() + "/order?mode=customDesign"
                + "&shopId=" + shopId
                + "&designFileId=" + designFileId
                + "&customTitle=" + URLEncoder.encode(customTitle, StandardCharsets.UTF_8)
                + "&unitPrice=" + unitPrice
                + "&quantity=" + quantity;

        response.getWriter().write("{\"success\":true,\"checkoutUrl\":" + AjaxResponse.quote(checkoutUrl) + "}");
    }

    private double parseDouble(String value, double fallback) {
        try {
            return (value == null || value.isBlank()) ? fallback : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return (value == null || value.isBlank()) ? fallback : Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return (value == null || value.isBlank()) ? null : new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
