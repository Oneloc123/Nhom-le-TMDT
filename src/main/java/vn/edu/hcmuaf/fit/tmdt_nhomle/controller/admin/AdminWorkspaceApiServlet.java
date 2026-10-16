package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.AdminDatabaseException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.AdminWorkspaceStore;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** Database-backed JSON API consumed by the Admin SPA. */
@WebServlet(name = "adminWorkspaceApiServlet", urlPatterns = "/api/admin/*")
public class AdminWorkspaceApiServlet extends HttpServlet {
    private static final int MAX_BODY_LENGTH = 65_536;

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        dispatch(req, resp, "GET");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        dispatch(req, resp, "POST");
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        dispatch(req, resp, "PUT");
    }

    @Override protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        dispatch(req, resp, "PATCH");
    }

    private void dispatch(HttpServletRequest request, HttpServletResponse response, String method)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        User admin = currentAdmin(request);
        // Tạm bỏ xác thực API trong lúc test UI theo yêu cầu; bật lại khối này sau khi test.
        /*
        if (admin == null) {
            error(response, 401, "UNAUTHENTICATED", "Vui lòng đăng nhập bằng tài khoản Admin.");
            return;
        }
        if (!admin.isAdmin() || !admin.isActive()) {
            error(response, 403, "FORBIDDEN", "Tài khoản không có quyền quản trị.");
            return;
        }
        */

        List<String> path = new ArrayList<>();
        if (request.getPathInfo() != null) {
            for (String part : request.getPathInfo().split("/")) if (!part.isBlank()) path.add(part);
        }
        try {
            if (path.equals(List.of("bootstrap")) && "GET".equals(method)) {
                Map<String, Object> payload = AdminWorkspaceStore.bootstrap();
                payload.put("dataSource", "mysql");
                json(response, 200, payload);
            } else if (path.equals(List.of("me")) && "GET".equals(method)) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("name", admin == null ? "Admin test" : admin.getFullName());
                payload.put("email", admin == null ? "admin@printkraft.vn" : admin.getEmail());
                payload.put("role", "Platform Admin");
                json(response, 200, payload);
            } else if (path.equals(List.of("dashboard")) && "GET".equals(method)) {
                json(response, 200, dashboard());
            } else if (!path.isEmpty() && path.get(0).equals("users")) {
                users(request, response, method, path);
            } else if (!path.isEmpty() && path.get(0).equals("sellers")) {
                records(request, response, method, path, "sellers", true);
            } else if (!path.isEmpty() && path.get(0).equals("products")) {
                records(request, response, method, path, "products", true);
            } else if (!path.isEmpty() && path.get(0).equals("orders")) {
                records(request, response, method, path, "orders", false);
            } else if (path.equals(List.of("transactions")) && "GET".equals(method)) {
                json(response, 200, page(AdminWorkspaceStore.findAll("transactions"), request));
            } else if (path.equals(List.of("finance", "revenue")) && "GET".equals(method)) {
                json(response, 200, revenue());
            } else if (path.equals(List.of("finance", "deposits")) && "GET".equals(method)) {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("sellers", AdminWorkspaceStore.findAll("sellers"));
                result.put("transactions", AdminWorkspaceStore.findAll("depositTransactions"));
                json(response, 200, result);
            } else if (path.equals(List.of("finance", "commission"))) {
                commission(request, response, method);
            } else if (path.equals(List.of("finance", "commission", "rules")) && "POST".equals(method)) {
                createCommissionRule(request, response);
            } else if (path.equals(List.of("analytics")) && "GET".equals(method)) {
                json(response, 200, analytics());
            } else if (path.equals(List.of("map")) && "GET".equals(method)) {
                json(response, 200, Map.of("sellers", AdminWorkspaceStore.findAll("sellers"),
                        "orders", AdminWorkspaceStore.findAll("orders")));
            } else if (path.equals(List.of("settings"))) {
                settings(request, response, method);
            } else {
                error(response, 404, "NOT_FOUND", "Không tìm thấy API.");
            }
        } catch (BadRequest e) {
            error(response, 400, "BAD_REQUEST", e.getMessage());
        } catch (AdminDatabaseException e) {
            log("Admin workspace database is unavailable", e);
            error(response, 503, "DATABASE_UNAVAILABLE", e.getMessage());
        } catch (IllegalArgumentException e) {
            error(response, 400, "BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            log("Admin workspace API request failed", e);
            error(response, 500, "INTERNAL_ERROR", "Không thể xử lý yêu cầu quản trị.");
        }
    }

    private void users(HttpServletRequest request, HttpServletResponse response, String method,
                       List<String> path) throws IOException {
        if (path.size() == 1 && "GET".equals(method)) {
            String q = lower(request.getParameter("q"));
            String status = request.getParameter("status");
            List<Map<String, Object>> rows = AdminWorkspaceStore.findAll("users").stream()
                    .filter(row -> q.isEmpty() || contains(row, q, "id", "name", "email", "phone"))
                    .filter(row -> status == null || status.isBlank() || "All".equalsIgnoreCase(status)
                            || status.equals(row.get("status")))
                    .toList();
            json(response, 200, page(rows, request));
            return;
        }
        if (path.size() == 1 && "POST".equals(method)) {
            Map<String, Object> body = body(request);
            String email = requiredString(body, "email");
            String name = requiredString(body, "name");
            String id = "CUS-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT);
            Map<String, Object> user = new LinkedHashMap<>(body);
            user.put("id", id);
            user.put("name", name);
            user.put("email", email);
            user.putIfAbsent("phone", "");
            user.putIfAbsent("orders", 0);
            user.putIfAbsent("totalSpent", 0);
            user.putIfAbsent("joined", LocalDate.now().toString());
            user.putIfAbsent("status", "Active");
            user.putIfAbsent("initials", initials(name));
            user.putIfAbsent("color", "lilac");
            user.remove("password");
            AdminWorkspaceStore.save("users", id, user);
            json(response, 201, user);
            return;
        }
        if (path.size() == 2 && "GET".equals(method)) {
            jsonOrNotFound(response, AdminWorkspaceStore.find("users", path.get(1)), "Không tìm thấy người dùng.");
            return;
        }
        if (path.size() == 2 && "PUT".equals(method)) {
            Map<String, Object> updates = body(request);
            updateRecord(response, "users", path.get(1), updates, row -> {
                for (String key : List.of("name", "email", "phone", "orders", "totalSpent", "status")) {
                    if (updates.containsKey(key)) row.put(key, updates.get(key));
                }
                return row;
            });
            return;
        }
        if (path.size() == 3 && "status".equals(path.get(2)) && "PATCH".equals(method)) {
            String status = requiredString(body(request), "status");
            if (!List.of("Active", "Suspended", "Inactive").contains(status)) {
                throw new BadRequest("Trạng thái người dùng không hợp lệ.");
            }
            updateRecord(response, "users", path.get(1), Map.of("status", status), row -> {
                row.put("status", status);
                return row;
            });
            return;
        }
        methodNotAllowed(response, "GET, POST, PUT, PATCH");
    }

    private void records(HttpServletRequest request, HttpServletResponse response, String method,
                         List<String> path, String type, boolean writable) throws IOException {
        if (path.size() == 1 && "GET".equals(method)) {
            String q = lower(request.getParameter("q"));
            String status = request.getParameter("status");
            String city = request.getParameter("city");
            List<Map<String, Object>> rows = AdminWorkspaceStore.findAll(type).stream()
                    .filter(row -> q.isEmpty() || contains(row, q, "id", "name", "ownerName", "location", "email", "category"))
                    .filter(row -> status == null || status.isBlank() || "All".equalsIgnoreCase(status)
                            || status.equals(row.get("status")))
                    .filter(row -> city == null || city.isBlank() || "All".equalsIgnoreCase(city)
                            || city.equals(row.get("city")))
                    .toList();
            json(response, 200, page(rows, request));
            return;
        }
        if (path.size() == 2 && "GET".equals(method)) {
            jsonOrNotFound(response, AdminWorkspaceStore.find(type, path.get(1)), "Không tìm thấy dữ liệu.");
            return;
        }
        if (path.size() == 3 && "status".equals(path.get(2)) && "PATCH".equals(method) && writable) {
            Map<String, Object> requestBody = body(request);
            String status = requiredString(requestBody, "status");
            List<String> allowed = "sellers".equals(type)
                    ? List.of("Active", "Inactive", "Suspended")
                    : List.of("Pending Review", "Published", "Rejected", "Hidden", "Draft");
            if (!allowed.contains(status)) throw new BadRequest("Trạng thái không hợp lệ.");
            updateRecord(response, type, path.get(1), requestBody, row -> {
                row.put("status", status);
                if (requestBody.containsKey("rejectionReason")) row.put("rejectionReason", requestBody.get("rejectionReason"));
                if (!"Rejected".equals(status)) row.remove("rejectionReason");
                return row;
            });
            return;
        }
        if (path.size() == 3 && "PATCH".equals(method) && writable) {
            Map<String, Object> updates = body(request);
            updateRecord(response, type, path.get(1), updates, row -> {
                row.putAll(updates);
                return row;
            });
            return;
        }
        methodNotAllowed(response, writable ? "GET, PATCH" : "GET");
    }

    private void commission(HttpServletRequest request, HttpServletResponse response, String method)
            throws IOException {
        Map<String, Object> settings = AdminWorkspaceStore.getSettings("marketplace");
        if ("GET".equals(method)) {
            json(response, 200, Map.of("rate", settings.getOrDefault("commissionRate", 10),
                    "rules", AdminWorkspaceStore.findAll("commissionRules")));
            return;
        }
        if (!"PUT".equals(method)) {
            methodNotAllowed(response, "GET, PUT");
            return;
        }
        BigDecimal rate = decimal(body(request).get("rate"), "rate");
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequest("Commission phải nằm trong khoảng 0 đến 100.");
        }
        settings.put("commissionRate", rate);
        AdminWorkspaceStore.saveSettings("marketplace", settings);
        AdminWorkspaceStore.update("commissionRules", "COM-01", row -> {
            row.put("rate", rate);
            return row;
        });
        json(response, 200, Map.of("rate", rate));
    }

    private void createCommissionRule(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Map<String, Object> rule = body(request);
        String id = requiredString(rule, "id");
        BigDecimal rate = decimal(rule.get("rate"), "rate");
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BadRequest("Commission phải nằm trong khoảng 0 đến 100.");
        }
        rule.put("rate", rate);
        rule.putIfAbsent("enabled", false);
        AdminWorkspaceStore.save("commissionRules", id, rule);
        json(response, 201, rule);
    }

    private void settings(HttpServletRequest request, HttpServletResponse response, String method)
            throws IOException {
        if ("GET".equals(method)) {
            json(response, 200, AdminWorkspaceStore.getSettings("marketplace"));
            return;
        }
        if (!"PUT".equals(method)) {
            methodNotAllowed(response, "GET, PUT");
            return;
        }
        Map<String, Object> current = AdminWorkspaceStore.getSettings("marketplace");
        current.putAll(body(request));
        validateSettings(current);
        AdminWorkspaceStore.saveSettings("marketplace", current);
        json(response, 200, current);
    }

    private static void validateSettings(Map<String, Object> settings) {
        for (String key : List.of("platformName", "currency", "timezone")) {
            if (!(settings.get(key) instanceof String value) || value.isBlank()) {
                throw new BadRequest(key + " không được để trống.");
            }
        }
        if (!(settings.get("requireProductReview") instanceof Boolean)) {
            throw new BadRequest("requireProductReview phải là boolean.");
        }
        BigDecimal rate = decimal(settings.get("commissionRate"), "commissionRate");
        BigDecimal deposit = decimal(settings.get("requiredDeposit"), "requiredDeposit");
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0
                || deposit.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequest("Commission phải từ 0 đến 100 và tiền cọc không âm.");
        }
        for (String key : List.of("printingTechnologies", "materials", "surfaceQualities")) {
            if (!(settings.get(key) instanceof List<?>)) throw new BadRequest(key + " phải là danh sách.");
        }
        settings.put("commissionRate", rate);
        settings.put("requiredDeposit", deposit.longValue());
    }

    private static Map<String, Object> dashboard() {
        List<Map<String, Object>> users = AdminWorkspaceStore.findAll("users");
        List<Map<String, Object>> sellers = AdminWorkspaceStore.findAll("sellers");
        List<Map<String, Object>> products = AdminWorkspaceStore.findAll("products");
        List<Map<String, Object>> orders = AdminWorkspaceStore.findAll("orders");
        List<Map<String, Object>> transactions = AdminWorkspaceStore.findAll("transactions");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", users.size());
        result.put("activeSellers", sellers.stream().filter(row -> "Active".equals(row.get("status"))).count());
        result.put("sellers", sellers.size());
        result.put("orders", orders.size());
        result.put("productsPendingReview", products.stream().filter(row -> "Pending Review".equals(row.get("status"))).count());
        result.put("gmv", sum(transactions, "gross"));
        result.put("platformRevenue", sum(transactions, "commission"));
        result.put("dataSource", "mysql");
        return result;
    }

    private static Map<String, Object> revenue() {
        List<Map<String, Object>> transactions = AdminWorkspaceStore.findAll("transactions");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("transactions", transactions);
        result.put("gmv", sum(transactions, "gross"));
        result.put("platformRevenue", sum(transactions, "commission"));
        result.put("sellerRevenue", sum(transactions, "sellerRevenue"));
        result.put("refunds", sum(transactions, "refund"));
        result.put("dataSource", "mysql");
        return result;
    }

    private static Map<String, Object> analytics() {
        Map<String, Object> result = new LinkedHashMap<>(dashboard());
        result.put("usersByStatus", count(AdminWorkspaceStore.findAll("users"), "status"));
        result.put("ordersByStatus", count(AdminWorkspaceStore.findAll("orders"), "status"));
        result.put("productsByCategory", count(AdminWorkspaceStore.findAll("products"), "category"));
        result.put("storesByCity", count(AdminWorkspaceStore.findAll("sellers"), "city"));
        return result;
    }

    private static Map<String, Long> count(List<Map<String, Object>> rows, String field) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) counts.merge(String.valueOf(row.getOrDefault(field, "Khác")), 1L, Long::sum);
        return counts;
    }

    private static long sum(List<Map<String, Object>> rows, String field) {
        return rows.stream().map(row -> row.get(field)).filter(Number.class::isInstance)
                .map(Number.class::cast).mapToLong(Number::longValue).sum();
    }

    private static void updateRecord(HttpServletResponse response, String type, String id,
                                     Map<String, Object> updates,
                                     UnaryOperator<Map<String, Object>> updater) throws IOException {
        Optional<Map<String, Object>> updated = AdminWorkspaceStore.update(type, id, updater);
        if (updated.isPresent()) json(response, 200, updated.get());
        else error(response, 404, "NOT_FOUND", "Không tìm thấy dữ liệu.");
    }

    private static void jsonOrNotFound(HttpServletResponse response, Optional<Map<String, Object>> record,
                                       String message) throws IOException {
        if (record.isPresent()) json(response, 200, record.get());
        else error(response, 404, "NOT_FOUND", message);
    }

    private static Object page(List<Map<String, Object>> all, HttpServletRequest request) {
        int page = positiveInt(request.getParameter("page"), 1);
        int size = Math.min(100, positiveInt(request.getParameter("size"), 100));
        int from = (int) Math.min(all.size(), ((long) page - 1) * size);
        int to = Math.min(all.size(), from + size);
        return Map.of("items", all.subList(from, to), "page", page, "size", size, "total", all.size());
    }

    private static boolean contains(Map<String, Object> row, String needle, String... keys) {
        for (String key : keys) {
            if (String.valueOf(row.getOrDefault(key, "")).toLowerCase(Locale.ROOT).contains(needle)) return true;
        }
        return false;
    }

    private static Map<String, Object> body(HttpServletRequest request) throws IOException {
        if (request.getContentType() == null || !request.getContentType().toLowerCase(Locale.ROOT)
                .startsWith("application/json")) {
            throw new BadRequest("Yêu cầu ghi phải dùng Content-Type: application/json.");
        }
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[2048];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                if (text.length() + count > MAX_BODY_LENGTH) throw new BadRequest("JSON body vượt quá 64 KB.");
                text.append(buffer, 0, count);
            }
        }
        return AdminJson.parseObject(text.toString());
    }

    private static Map<String, Object> bodyUnchecked(HttpServletRequest request) {
        try {
            return body(request);
        } catch (IOException e) {
            throw new BadRequest("Không đọc được JSON body.");
        }
    }

    private static String requiredString(Map<String, Object> body, String key) {
        Object value = body.get(key);
        if (!(value instanceof String text) || text.isBlank()) throw new BadRequest("Thiếu trường " + key + ".");
        return text.trim();
    }

    private static BigDecimal decimal(Object value, String field) {
        if (!(value instanceof Number) && !(value instanceof String)) throw new BadRequest(field + " phải là số.");
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new BadRequest(field + " không phải số hợp lệ.");
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
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        return parts.length == 1 ? parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT)
                : (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private static User currentAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute("currentUser");
        return value instanceof User user ? user : null;
    }

    private static void json(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(AdminJson.stringify(data));
    }

    private static void error(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        json(response, status, Map.of("error", Map.of("code", code, "message", message)));
    }

    private static void methodNotAllowed(HttpServletResponse response, String allow) throws IOException {
        response.setHeader("Allow", allow);
        error(response, 405, "METHOD_NOT_ALLOWED", "Phương thức HTTP không được hỗ trợ.");
    }

    private static final class BadRequest extends RuntimeException {
        private BadRequest(String message) { super(message); }
    }
}
