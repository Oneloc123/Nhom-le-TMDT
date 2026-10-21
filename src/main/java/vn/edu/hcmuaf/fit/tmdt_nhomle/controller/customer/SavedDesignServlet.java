package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.SavedDesignDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.SavedDesign;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.JsonValidator;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * “Thiết kế 3D đã lưu” — CRUD bản cấu hình custom sản phẩm in 3D của khách.
 *
 * <p>Mọi thao tác lấy {@code userId} từ phiên đăng nhập (không nhận từ request);
 * DAO luôn khoá {@code WHERE user_id = ?} nên không thể đọc/sửa/xoá thiết kế của
 * người khác bằng cách đổi id (chống IDOR).</p>
 *
 * <p>Route:</p>
 * <ul>
 *   <li>{@code GET /saved-designs} — danh sách (q, page) + fallback không AJAX</li>
 *   <li>{@code GET /saved-designs?action=view&id=} — chi tiết 1 bản (JSON/AJAX)</li>
 *   <li>{@code POST /saved-designs} — action=create|update|delete|duplicate</li>
 * </ul>
 */
@WebServlet(name = "savedDesignServlet", value = "/saved-designs")
public class SavedDesignServlet extends HttpServlet {

    private static final int MAX_QUANTITY = 10_000;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            // AuthFilter đã xử lý (redirect/401) — đây là lớp phòng thủ thứ hai.
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String action = nvl(request.getParameter("action"), "list");
        if ("view".equals(action)) {
            view(request, response, user);
            return;
        }

        String q = request.getParameter("q");
        int page = parseInt(request.getParameter("page"), 1);
        Map<String, Object> list;
        try {
            list = SavedDesignDao.listByUser(user.getId(), q, page,
                    SavedDesignDao.DEFAULT_PAGE_SIZE);
        } catch (IllegalArgumentException e) {
            if (RegisterServlet.wantsJson(request)) {
                RegisterServlet.respond(request, response, false, e.getMessage(), null);
            } else {
                request.setAttribute("error", e.getMessage());
                list = SavedDesignDao.listByUser(user.getId(), null, 1,
                        SavedDesignDao.DEFAULT_PAGE_SIZE);
                render(request, response, user, list, null);
            }
            return;
        }
        render(request, response, user, list, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String action = nvl(request.getParameter("action"), "create");
        try {
            switch (action) {
                case "create" -> createOrUpdate(request, response, user, null);
                case "update" -> createOrUpdate(request, response, user,
                        parseId(request.getParameter("id")));
                case "delete" -> delete(request, response, user);
                case "duplicate" -> duplicate(request, response, user);
                default -> fail(request, response, "Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException e) {
            fail(request, response, e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    private void view(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        Long id = parseId(request.getParameter("id"));
        if (id == null) {
            fail(request, response, "Mã thiết kế không hợp lệ.");
            return;
        }
        Optional<SavedDesign> design = SavedDesignDao.findByIdForUser(id, user.getId());
        if (design.isEmpty()) {
            // Không phân biệt "không tồn tại" vs "không thuộc về bạn".
            fail(request, response, "Không tìm thấy thiết kế hoặc bạn không có quyền xem.");
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("design", toMap(design.get()));
        RegisterServlet.respond(request, response, true, "Đã tải thiết kế.", data);
    }

    /**
     * Chuyển model sang Map cho JSON — util.Json chỉ serialize được Map/Iterable/
     * primitives, nếu để thẳng object sẽ thành toString() vô dụng cho giao diện.
     */
    static Map<String, Object> toMap(SavedDesign d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("designId", d.getDesignId());
        map.put("userId", d.getUserId());
        map.put("productId", d.getProductId());
        map.put("designFileId", d.getDesignFileId());
        map.put("designName", d.getDesignName());
        map.put("configJson", d.getConfigJson());
        map.put("material", d.getMaterial());
        map.put("color", d.getColor());
        map.put("dimensions", d.getDimensions());
        map.put("quantity", d.getQuantity());
        map.put("previewUrl", d.getPreviewUrl());
        map.put("notes", d.getNotes());
        map.put("status", d.getStatus());
        map.put("version", d.getVersion());
        map.put("createdAt", d.getCreatedAt());
        map.put("updatedAt", d.getUpdatedAt());
        map.put("productName", d.getProductName());
        map.put("productCustomizable", d.getProductCustomizable());
        map.put("productActive", d.getProductActive());
        return map;
    }

    private void createOrUpdate(HttpServletRequest request, HttpServletResponse response,
                                User user, Long designId) throws IOException {
        SavedDesign design = new SavedDesign();
        design.setUserId(user.getId());
        if (designId != null) {
            design.setDesignId(designId);
        }

        String error = bind(request, design);
        if (error == null && designId != null) {
            // Kiểm tra quyền sở hữu TRƯỚC khi cho phép ghi.
            if (SavedDesignDao.findByIdForUser(designId, user.getId()).isEmpty()) {
                error = "Không tìm thấy thiết kế hoặc bạn không có quyền sửa.";
            }
        }
        if (error == null && design.getProductId() != null) {
            Product product = ProductDao.findById(design.getProductId()).orElse(null);
            if (product == null) {
                error = "Sản phẩm tham chiếu không tồn tại.";
            } else if (product.isActive() && !product.isCustomizable()) {
                error = "Sản phẩm này không hỗ trợ tùy chỉnh nên chưa thể lưu thiết kế.";
            } else {
                design.setProductName(product.getName());
            }
        }
        if (error != null) {
            fail(request, response, error);
            return;
        }

        SavedDesign saved;
        if (designId == null) {
            design.setStatus(nvl(request.getParameter("status"), SavedDesign.STATUS_DRAFT));
            design.setVersion(1);
            saved = SavedDesignDao.insert(design);
        } else {
            design.setStatus(nvl(request.getParameter("status"), SavedDesign.STATUS_DRAFT));
            if (!SavedDesignDao.update(design)) {
                fail(request, response, "Không thể cập nhật thiết kế.");
                return;
            }
            saved = SavedDesignDao.findByIdForUser(designId, user.getId()).orElse(design);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("design", toMap(saved));
        success(request, response,
                designId == null ? "Đã lưu thiết kế." : "Đã cập nhật thiết kế.", data);
    }

    private void delete(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        Long id = parseId(request.getParameter("id"));
        if (id == null) {
            fail(request, response, "Mã thiết kế không hợp lệ.");
            return;
        }
        boolean deleted = SavedDesignDao.delete(id, user.getId());
        if (deleted) {
            success(request, response, "Đã xoá thiết kế.", null);
        } else {
            fail(request, response, "Không tìm thấy thiết kế hoặc bạn không có quyền xoá.");
        }
    }

    private void duplicate(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        Long id = parseId(request.getParameter("id"));
        if (id == null) {
            fail(request, response, "Mã thiết kế không hợp lệ.");
            return;
        }
        Optional<SavedDesign> copy = SavedDesignDao.duplicate(id, user.getId());
        if (copy.isEmpty()) {
            fail(request, response, "Không tìm thấy thiết kế để nhân bản.");
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("design", toMap(copy.get()));
        success(request, response, "Đã nhân bản thiết kế.", data);
    }

    /** Trả JSON cho AJAX; redirect về danh sách cho submit form thường (không JS). */
    private void success(HttpServletRequest request, HttpServletResponse response,
                         String message, Map<String, Object> data) throws IOException {
        if (RegisterServlet.wantsJson(request)) {
            RegisterServlet.respond(request, response, true, message, data);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/saved-designs?saved="
                + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8));
    }

    // ------------------------------------------------------------------
    /** Ghi dữ liệu form vào model + validate phía server. Trả về lỗi (null nếu ổn). */
    private String bind(HttpServletRequest request, SavedDesign design) {
        String name = nvl(request.getParameter("designName")).trim();
        if (name.isEmpty()) {
            return "Tên thiết kế không được để trống.";
        }
        if (name.length() > 150) {
            return "Tên thiết kế tối đa 150 ký tự.";
        }
        design.setDesignName(name);

        String config = nvl(request.getParameter("configJson")).trim();
        String configError = JsonValidator.validate(config);
        if (configError != null) {
            return configError;
        }
        design.setConfigJson(config.isEmpty() ? null : config);

        String material = nvl(request.getParameter("material")).trim();
        String color = nvl(request.getParameter("color")).trim();
        String dimensions = nvl(request.getParameter("dimensions")).trim();
        String notes = nvl(request.getParameter("notes")).trim();
        String previewUrl = nvl(request.getParameter("previewUrl")).trim();
        if (material.length() > 60 || color.length() > 60 || dimensions.length() > 120
                || notes.length() > 500 || previewUrl.length() > 500) {
            return "Dữ liệu thiết kế vượt quá độ dài cho phép.";
        }
        // Chặn URL tuyệt đối/ javascript: — chỉ nhận đường dẫn nội bộ hoặc để trống.
        if (!previewUrl.isEmpty() && !(previewUrl.startsWith("/") || previewUrl.startsWith("images/"))) {
            return "Ảnh xem trước phải là đường dẫn nội bộ trong hệ thống.";
        }
        design.setMaterial(material);
        design.setColor(color);
        design.setDimensions(dimensions);
        design.setNotes(notes);
        design.setPreviewUrl(previewUrl);

        int quantity = parseInt(request.getParameter("quantity"), 1);
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            return "Số lượng phải từ 1 đến " + MAX_QUANTITY + ".";
        }
        design.setQuantity(quantity);

        Long productId = parseId(request.getParameter("productId"));
        design.setProductId(productId);

        Long fileId = parseId(request.getParameter("designFileId"));
        design.setDesignFileId(fileId);

        return null;
    }

    private void render(HttpServletRequest request, HttpServletResponse response, User user,
                        Map<String, Object> list, String message)
            throws ServletException, IOException {
        request.setAttribute("user", user);
        request.setAttribute("designs", list.get("items"));
        request.setAttribute("total", list.get("total"));
        request.setAttribute("page", list.get("page"));
        request.setAttribute("pages", list.get("pages"));
        request.setAttribute("q", request.getParameter("q"));
        // Select “Sản phẩm” khi tạo/sửa thiết kế: chỉ sản phẩm đang hiển thị
        // và cho phép tùy chỉnh.
        request.setAttribute("productOptions", ProductDao.listCustomizableOptions(200));
        // Nhận thông báo từ redirect (không-JS) : ?saved= / ?error=
        if (request.getParameter("saved") != null) {
            request.setAttribute("message", request.getParameter("saved"));
        } else if (request.getParameter("error") != null) {
            request.setAttribute("error", request.getParameter("error"));
        }
        if (message != null) {
            request.setAttribute("message", message);
        }
        request.getRequestDispatcher("/customer/saved-designs.jsp").forward(request, response);
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        if (RegisterServlet.wantsJson(request)) {
            RegisterServlet.respond(request, response, false, message, null);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/saved-designs?error="
                + java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8));
    }

    private static Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(value.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    private static String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
