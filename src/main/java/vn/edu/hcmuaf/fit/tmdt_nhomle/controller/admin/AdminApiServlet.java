package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.PrintOrderStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.StoreStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.UserStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.OrderItem;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintOrder;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Store;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.AdminSettingsStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/** Authenticated JSON API for the admin workspace. */
public class AdminApiServlet extends HttpServlet {
    private static final int MAX_BODY_LENGTH = 65_536;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        dispatch(request, response, "GET");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        dispatch(request, response, "POST");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        dispatch(request, response, "PUT");
    }

    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        dispatch(request, response, "PATCH");
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response, String method)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        User admin = currentAdmin(request);
        if (admin == null) {
            error(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHENTICATED",
                    "Vui lòng đăng nhập bằng tài khoản Admin.");
            return;
        }
        if (!admin.isAdmin() || !User.STATUS_ACTIVE.equals(admin.getStatus())) {
            error(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN",
                    "Tài khoản không có quyền truy cập chức năng quản trị.");
            return;
        }

        String[] path = request.getPathInfo() == null ? new String[0]
                : request.getPathInfo().split("/");
        List<String> parts = new ArrayList<>();
        for (String part : path) if (!part.isBlank()) parts.add(part);
        try {
            if (parts.isEmpty()) {
                error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Không tìm thấy API.");
            } else if (parts.equals(List.of("me")) && "GET".equals(method)) {
                json(response, HttpServletResponse.SC_OK, userDto(admin));
            } else if (parts.equals(List.of("dashboard")) && "GET".equals(method)) {
                json(response, HttpServletResponse.SC_OK, dashboard());
            } else if (parts.size() == 2 && "GET".equals(method) && parts.get(0).equals("sellers")) {
                int id = parseId(parts.get(1));
                Store store = StoreStore.findAll().stream().filter(item -> item.getId() == id)
                        .findFirst().orElse(null);
                if (store == null) error(response, HttpServletResponse.SC_NOT_FOUND,
                        "NOT_FOUND", "Không tìm thấy xưởng.");
                else json(response, HttpServletResponse.SC_OK, storeDto(store));
            } else if (parts.size() == 2 && "GET".equals(method) && parts.get(0).equals("products")) {
                int id = parseId(parts.get(1));
                Product product = ProductStore.findById(id).orElse(null);
                if (product == null) error(response, HttpServletResponse.SC_NOT_FOUND,
                        "NOT_FOUND", "Không tìm thấy sản phẩm.");
                else json(response, HttpServletResponse.SC_OK, productDto(product));
            } else if (parts.size() == 2 && "GET".equals(method) && parts.get(0).equals("orders")) {
                Order order = OrderStore.findByNumber(parts.get(1)).orElse(null);
                if (order == null) error(response, HttpServletResponse.SC_NOT_FOUND,
                        "NOT_FOUND", "Không tìm thấy đơn hàng.");
                else json(response, HttpServletResponse.SC_OK, orderDto(order));
            } else if (parts.get(0).equals("users")) {
                users(request, response, method, parts, admin);
            } else if (parts.equals(List.of("sellers")) && "GET".equals(method)) {
                json(response, HttpServletResponse.SC_OK, page(StoreStore.findAll().stream()
                        .map(AdminApiServlet::storeDto).toList(), request));
            } else if (parts.equals(List.of("products")) && "GET".equals(method)) {
                json(response, HttpServletResponse.SC_OK, page(ProductStore.findAll().stream()
                        .map(AdminApiServlet::productDto).toList(), request));
            } else if (parts.equals(List.of("orders")) && "GET".equals(method)) {
                json(response, HttpServletResponse.SC_OK, page(OrderStore.findAll().stream()
                        .map(AdminApiServlet::orderDto).toList(), request));
            } else if (parts.equals(List.of("analytics")) && "GET".equals(method)) {
                json(response, HttpServletResponse.SC_OK, analytics());
            } else if (parts.equals(List.of("map")) && "GET".equals(method)) {
                json(response, HttpServletResponse.SC_OK, mapData());
            } else if (parts.equals(List.of("finance", "revenue")) && "GET".equals(method)) {
                json(response, HttpServletResponse.SC_OK, revenue());
            } else if (parts.equals(List.of("finance", "deposits"))) {
                error(response, HttpServletResponse.SC_NOT_IMPLEMENTED,
                        "DEPOSIT_MODEL_UNAVAILABLE", "Dự án chưa có model hoặc kho lưu tiền cọc seller.");
            } else if (parts.equals(List.of("finance", "commission"))) {
                commission(request, response, method);
            } else if (parts.equals(List.of("settings"))) {
                settings(request, response, method);
            } else if (parts.size() >= 2 && (parts.get(0).equals("sellers") || parts.get(0).equals("products"))) {
                error(response, HttpServletResponse.SC_NOT_IMPLEMENTED,
                        "MODERATION_MODEL_UNAVAILABLE", "Model hiện tại chưa lưu trạng thái seller/sản phẩm để cập nhật.");
            } else {
                error(response, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Không tìm thấy API.");
            }
        } catch (BadRequest e) {
            error(response, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", e.getMessage());
        } catch (IllegalArgumentException e) {
            error(response, HttpServletResponse.SC_BAD_REQUEST, "BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            log("Admin API request failed", e);
            error(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Không thể xử lý yêu cầu quản trị.");
        }
    }

    private void users(HttpServletRequest request, HttpServletResponse response, String method,
                       List<String> path, User admin) throws IOException {
        if (path.size() == 1) {
            if ("GET".equals(method)) {
                String query = lower(request.getParameter("q"));
                String role = upper(request.getParameter("role"));
                String status = upper(request.getParameter("status"));
                Predicate<User> matches = user -> (query.isEmpty()
                        || lower(user.getEmail()).contains(query)
                        || lower(user.getFullName()).contains(query)
                        || lower(user.getPhone()).contains(query))
                        && (role.isEmpty() || role.equals(user.getRole()))
                        && (status.isEmpty() || status.equals(user.getStatus()));
                List<Map<String, Object>> rows = UserStore.findAll().stream().filter(matches)
                        .map(AdminApiServlet::userDto).toList();
                json(response, HttpServletResponse.SC_OK, page(rows, request));
                return;
            }
            if ("POST".equals(method)) {
                Map<String, Object> body = body(request);
                String email = string(body, "email", true).trim();
                String password = string(body, "password", true);
                String fullName = string(body, "fullName", true).trim();
                if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
                    throw new BadRequest("Email không đúng định dạng.");
                }
                if (password.length() < 6) throw new BadRequest("Mật khẩu phải có ít nhất 6 ký tự.");
                if (fullName.isBlank()) throw new BadRequest("Họ tên không được để trống.");
                String role = body.containsKey("role") ? upper(string(body, "role", true)) : User.ROLE_CUSTOMER;
                if (!User.ROLE_ADMIN.equals(role) && !User.ROLE_CUSTOMER.equals(role)) {
                    throw new BadRequest("Role chỉ nhận ADMIN hoặc CUSTOMER.");
                }
                User created;
                synchronized (UserStore.class) {
                    if (UserStore.findByEmail(email).isPresent()) throw new BadRequest("Email đã tồn tại.");
                    created = UserStore.create(email, password, fullName,
                            string(body, "phone", false), string(body, "address", false),
                            role, User.STATUS_ACTIVE, "local");
                }
                json(response, HttpServletResponse.SC_CREATED, userDto(created));
                return;
            }
            methodNotAllowed(response, "GET, POST");
            return;
        }

        int id = parseId(path.get(1));
        User target = UserStore.findById(id).orElse(null);
        if (target == null) throw new BadRequest("Không tìm thấy người dùng.");
        if (path.size() == 2 && "GET".equals(method)) {
            json(response, HttpServletResponse.SC_OK, userDto(target));
            return;
        }
        if (path.size() == 2 && "PUT".equals(method)) {
            Map<String, Object> body = body(request);
            synchronized (UserStore.class) {
                target = UserStore.findById(id).orElseThrow(() -> new BadRequest("Không tìm thấy người dùng."));
                if (body.containsKey("fullName")) {
                    String name = string(body, "fullName", true).trim();
                    if (name.isEmpty()) throw new BadRequest("Họ tên không được để trống.");
                    target.setFullName(name);
                }
                if (body.containsKey("phone")) target.setPhone(string(body, "phone", false));
                if (body.containsKey("address")) target.setAddress(string(body, "address", false));
                if (body.containsKey("role")) {
                    String role = upper(string(body, "role", true));
                    if (!User.ROLE_ADMIN.equals(role) && !User.ROLE_CUSTOMER.equals(role)) {
                        throw new BadRequest("Role chỉ nhận ADMIN hoặc CUSTOMER.");
                    }
                    if (User.ROLE_CUSTOMER.equals(role) && target.isAdmin()
                            && User.STATUS_ACTIVE.equals(target.getStatus())
                            && UserStore.findAll().stream().filter(User::isAdmin)
                            .filter(user -> User.STATUS_ACTIVE.equals(user.getStatus())).count() <= 1) {
                        throw new BadRequest("Không thể hạ quyền quản trị viên đang hoạt động cuối cùng.");
                    }
                    target.setRole(role);
                }
                UserStore.update(target);
            }
            json(response, HttpServletResponse.SC_OK, userDto(target));
            return;
        }
        if (path.size() == 3 && "status".equals(path.get(2))
                && ("PATCH".equals(method) || "POST".equals(method))) {
            String nextStatus = upper(string(body(request), "status", true));
            if (!User.STATUS_ACTIVE.equals(nextStatus) && !User.STATUS_LOCKED.equals(nextStatus)) {
                throw new BadRequest("Status chỉ nhận ACTIVE hoặc LOCKED.");
            }
            synchronized (UserStore.class) {
                target = UserStore.findById(id).orElseThrow(() -> new BadRequest("Không tìm thấy người dùng."));
                if (target.getId() == admin.getId() && User.STATUS_LOCKED.equals(nextStatus)) {
                    throw new BadRequest("Không thể tự khóa tài khoản đang đăng nhập.");
                }
                if (target.isAdmin() && User.STATUS_ACTIVE.equals(target.getStatus())
                        && User.STATUS_LOCKED.equals(nextStatus)
                        && UserStore.findAll().stream().filter(User::isAdmin)
                        .filter(user -> User.STATUS_ACTIVE.equals(user.getStatus())).count() <= 1) {
                    throw new BadRequest("Không thể khóa quản trị viên đang hoạt động cuối cùng.");
                }
                target.setStatus(nextStatus);
                UserStore.update(target);
            }
            json(response, HttpServletResponse.SC_OK, userDto(target));
            return;
        }
        methodNotAllowed(response, "GET, PUT, PATCH");
    }

    private void commission(HttpServletRequest request, HttpServletResponse response, String method)
            throws IOException {
        if ("GET".equals(method)) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("rate", AdminSettingsStore.get().get("commissionRate"));
            result.put("appliedToTransactions", false);
            result.put("note", "Cấu hình được lưu ở bộ nhớ server; thanh toán hiện chưa áp dụng commission.");
            json(response, HttpServletResponse.SC_OK, result);
        } else if ("PUT".equals(method)) {
            BigDecimal rate = decimal(body(request).get("rate"), "rate");
            if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BadRequest("Commission phải nằm trong khoảng 0 đến 100.");
            }
            json(response, HttpServletResponse.SC_OK,
                    Map.of("rate", AdminSettingsStore.update(Map.of("commissionRate", rate)).get("commissionRate"),
                            "appliedToTransactions", false));
        } else {
            methodNotAllowed(response, "GET, PUT");
        }
    }

    private void settings(HttpServletRequest request, HttpServletResponse response, String method)
            throws IOException {
        if ("GET".equals(method)) {
            json(response, HttpServletResponse.SC_OK, AdminSettingsStore.get());
            return;
        }
        if (!"PUT".equals(method)) {
            methodNotAllowed(response, "GET, PUT");
            return;
        }
        Map<String, Object> body = body(request);
        Map<String, Object> next = new LinkedHashMap<>(AdminSettingsStore.get());
        next.putAll(body);
        validateSettings(next);
        json(response, HttpServletResponse.SC_OK, AdminSettingsStore.update(next));
    }

    private static void validateSettings(Map<String, Object> values) {
        for (String key : List.of("platformName", "currency", "timezone")) {
            if (!(values.get(key) instanceof String text) || text.isBlank()) {
                throw new BadRequest(key + " không được để trống.");
            }
        }
        if (!(values.get("requireProductReview") instanceof Boolean)) {
            throw new BadRequest("requireProductReview phải là true hoặc false.");
        }
        BigDecimal deposit = decimal(values.get("requiredDeposit"), "requiredDeposit");
        if (deposit.compareTo(BigDecimal.ZERO) < 0) throw new BadRequest("Tiền cọc không được âm.");
        try {
            deposit.longValueExact();
        } catch (ArithmeticException e) {
            throw new BadRequest("Tiền cọc phải là số nguyên VND hợp lệ.");
        }
        BigDecimal rate = decimal(values.get("commissionRate"), "commissionRate");
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequest("Commission phải nằm trong khoảng 0 đến 100.");
        }
        for (String key : List.of("printingTechnologies", "materials", "surfaceQualities")) {
            if (!(values.get(key) instanceof List<?> list)
                    || list.stream().anyMatch(item -> !(item instanceof String text) || text.isBlank())) {
                throw new BadRequest(key + " phải là danh sách chuỗi không rỗng.");
            }
        }
        values.put("requiredDeposit", deposit.longValueExact());
        values.put("commissionRate", rate);
    }

    private static Map<String, Object> dashboard() {
        List<User> users = UserStore.findAll();
        List<Order> orders = OrderStore.findAll();
        List<PrintOrder> printOrders = PrintOrderStore.findAll();
        long paid = orders.stream().filter(order -> Order.PAYMENT_PAID.equals(order.getPaymentStatus()))
                .mapToLong(order -> order.getTotal().longValue()).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", users.size());
        result.put("customers", users.stream().filter(user -> User.ROLE_CUSTOMER.equals(user.getRole())).count());
        result.put("admins", users.stream().filter(User::isAdmin).count());
        result.put("lockedUsers", users.stream().filter(user -> User.STATUS_LOCKED.equals(user.getStatus())).count());
        result.put("sellers", StoreStore.findAll().size());
        result.put("products", ProductStore.findAll().size());
        result.put("marketplaceOrders", orders.size());
        result.put("printJobs", printOrders.size());
        result.put("marketplacePaidRevenueVnd", paid);
        result.put("printJobRevenueVnd", PrintOrderStore.revenue());
        result.put("marketplaceOrdersByStatus", countBy(orders, Order::getStatus));
        result.put("printJobsByStatus", countBy(printOrders, PrintOrder::getStatus));
        result.put("dataSource", "in-memory");
        return result;
    }

    private static Map<String, Object> analytics() {
        Map<String, Object> result = dashboard();
        Map<String, Long> productsByCategory = new LinkedHashMap<>();
        for (Product product : ProductStore.findAll()) {
            productsByCategory.merge(product.getCategory(), 1L, Long::sum);
        }
        result.put("productsByCategory", productsByCategory);
        result.put("storesByLocation", countBy(StoreStore.findAll(), Store::getLocation));
        return result;
    }

    private static Map<String, Object> revenue() {
        List<Order> paidOrders = OrderStore.findAll().stream()
                .filter(order -> Order.PAYMENT_PAID.equals(order.getPaymentStatus())).toList();
        long gross = paidOrders.stream().mapToLong(order -> order.getTotal().longValue()).sum();
        BigDecimal rate = (BigDecimal) AdminSettingsStore.get().get("commissionRate");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("paidOrderCount", paidOrders.size());
        result.put("grossPaidVnd", gross);
        result.put("configuredCommissionRate", rate);
        result.put("projectedCommissionVnd", BigDecimal.valueOf(gross).multiply(rate)
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP).longValue());
        result.put("commissionIsSettled", false);
        result.put("printJobRevenueVnd", PrintOrderStore.revenue());
        result.put("dataSource", "in-memory");
        return result;
    }

    private static Map<String, Object> mapData() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stores", StoreStore.findAll().stream().map(AdminApiServlet::storeDto).toList());
        result.put("locationsAreCoordinates", false);
        result.put("note", "Kho dữ liệu hiện chỉ có địa chỉ dạng văn bản, chưa lưu tọa độ bản đồ.");
        return result;
    }

    private static <T> Map<String, Long> countBy(List<T> values,
                                                 java.util.function.Function<T, String> label) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (T value : values) result.merge(label.apply(value), 1L, Long::sum);
        return result;
    }

    private static Object page(List<Map<String, Object>> all, HttpServletRequest request) {
        int page = positiveInt(request.getParameter("page"), 1);
        int size = Math.min(100, positiveInt(request.getParameter("size"), 25));
        int from = (int) Math.min(all.size(), ((long) page - 1) * size);
        int to = Math.min(all.size(), from + size);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", all.subList(from, to));
        result.put("page", page);
        result.put("size", size);
        result.put("total", all.size());
        return result;
    }

    private static Map<String, Object> userDto(User user) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", user.getId());
        result.put("email", user.getEmail());
        result.put("fullName", user.getFullName());
        result.put("phone", user.getPhone());
        result.put("address", user.getAddress());
        result.put("role", user.getRole());
        result.put("status", user.getStatus());
        result.put("provider", user.getProvider());
        result.put("createdAt", user.getCreatedAt());
        return result;
    }

    private static Map<String, Object> storeDto(Store store) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", store.getId());
        result.put("name", store.getName());
        result.put("description", store.getDescription());
        result.put("specialty", store.getSpecialty());
        result.put("location", store.getLocation());
        result.put("rating", store.getRating());
        result.put("completedOrders", store.getCompletedOrders());
        return result;
    }

    private static Map<String, Object> productDto(Product product) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", product.getId());
        result.put("name", product.getName());
        result.put("category", product.getCategory());
        result.put("description", product.getDescription());
        result.put("printMethod", product.getPrintMethod());
        result.put("price", product.getPrice());
        result.put("minimumQuantity", product.getMinimumQuantity());
        result.put("rating", product.getRating());
        result.put("reviewCount", product.getReviewCount());
        result.put("storeId", product.getStoreId());
        result.put("storeName", product.getStoreName());
        return result;
    }

    private static Map<String, Object> orderDto(Order order) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNumber", order.getOrderNumber());
        result.put("customerId", order.getCustomerId());
        result.put("customerName", order.getCustomerName());
        result.put("phone", order.getPhone());
        result.put("address", order.getAddress());
        result.put("items", order.getItems().stream().map(AdminApiServlet::orderItemDto).toList());
        result.put("itemCount", order.getItemCount());
        result.put("total", order.getTotal());
        result.put("createdAt", order.getCreatedAt());
        result.put("status", order.getStatus());
        result.put("paymentStatus", order.getPaymentStatus());
        result.put("paymentMethod", order.getPaymentMethod());
        return result;
    }

    private static Map<String, Object> orderItemDto(OrderItem item) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productId", item.getProductId());
        result.put("productName", item.getProductName());
        result.put("unitPrice", item.getUnitPrice());
        result.put("quantity", item.getQuantity());
        result.put("subtotal", item.getSubtotal());
        return result;
    }

    private static User currentAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object current = session == null ? null : session.getAttribute("currentUser");
        return current instanceof User user ? user : null;
    }

    private static Map<String, Object> body(HttpServletRequest request) throws IOException {
        if (request.getContentType() == null || !request.getContentType().toLowerCase(Locale.ROOT)
                .startsWith("application/json")) {
            throw new BadRequest("Yêu cầu cập nhật phải gửi Content-Type: application/json.");
        }
        StringBuilder json = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[2048];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                if (json.length() + count > MAX_BODY_LENGTH) throw new BadRequest("JSON body vượt quá giới hạn 64 KB.");
                json.append(buffer, 0, count);
            }
        }
        return AdminJson.parseObject(json.toString());
    }

    private static String string(Map<String, Object> values, String key, boolean required) {
        Object value = values.get(key);
        if (value == null) {
            if (required) throw new BadRequest("Thiếu trường " + key + ".");
            return "";
        }
        if (!(value instanceof String text)) throw new BadRequest(key + " phải là chuỗi.");
        return text;
    }

    private static BigDecimal decimal(Object value, String key) {
        if (!(value instanceof Number) && !(value instanceof String)) {
            throw new BadRequest(key + " phải là số.");
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BadRequest(key + " không đúng định dạng số.");
        }
    }

    private static int parseId(String value) {
        try {
            int id = Integer.parseInt(value);
            if (id < 1) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException e) {
            throw new BadRequest("ID không hợp lệ.");
        }
    }

    private static int positiveInt(String value, int fallback) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT).trim();
    }

    private static void json(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(AdminJson.stringify(value));
    }

    private static void error(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        json(response, status, Map.of("error", Map.of("code", code, "message", message)));
    }

    private static void methodNotAllowed(HttpServletResponse response, String allowed) throws IOException {
        response.setHeader("Allow", allowed);
        error(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "Phương thức HTTP không được hỗ trợ cho API này.");
    }

    private static final class BadRequest extends RuntimeException {
        private BadRequest(String message) { super(message); }
    }
}
