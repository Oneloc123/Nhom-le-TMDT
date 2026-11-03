package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@WebServlet(name = "trackingServlet", urlPatterns = {"/order-tracking", "/track", "/api/orders/tracking"})
public class TrackingServlet extends HttpServlet {
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user=AuthFilter.currentUser(request);
        if(user==null){response.sendRedirect(request.getContextPath()+"/login");return;}
        String code=safeParameter(request.getParameter("code"),40);
        String action=request.getParameter("action");
        if(!"toggleShare".equals(action)){response.sendError(HttpServletResponse.SC_BAD_REQUEST);return;}
        boolean enabled="true".equalsIgnoreCase(request.getParameter("enabled"))||"1".equals(request.getParameter("enabled"));
        try{
            OrderDao.setTrackingEnabledForUser(code,user.getId(),enabled);
            response.sendRedirect(request.getContextPath()+"/order-tracking?code="+java.net.URLEncoder.encode(code,java.nio.charset.StandardCharsets.UTF_8)+"&shareUpdated=1");
        }catch(IllegalArgumentException|DataAccessException e){
            response.sendRedirect(request.getContextPath()+"/order-tracking?code="+java.net.URLEncoder.encode(code==null?"":code,java.nio.charset.StandardCharsets.UTF_8)+"&shareError=1");
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getServletPath();
        try {
            if ("/track".equals(path)) {
                publicTracking(request, response);
            } else if ("/api/orders/tracking".equals(path)) {
                apiTracking(request, response);
            } else {
                privateTracking(request, response);
            }
        } catch (DataAccessException e) {
            if ("/api/orders/tracking".equals(path)) {
                sendJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        Map.of("success", false, "message", "Không thể tải tiến độ đơn hàng lúc này."));
            } else {
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Không thể tải tiến độ đơn hàng lúc này.");
            }
        }
    }

    private void privateTracking(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) { response.sendRedirect(request.getContextPath() + "/login"); return; }
        String code = safeParameter(request.getParameter("code"), 40);
        Order order = OrderDao.findByCodeForUser(code, user.getId()).orElse(null);
        if (order == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        OrderDao.loadDetails(order);
        request.setAttribute("order", order);
        request.setAttribute("trackingEvents", timeline(order.getOrderId(), false));
        String token = OrderDao.ensureTrackingTokenForUser(order.getOrderId(), user.getId());
        boolean shareEnabled = OrderDao.isTrackingEnabledForUser(order.getOrderId(), user.getId());
        request.setAttribute("trackingShareEnabled", shareEnabled);
        request.setAttribute("trackingShareUrl", shareEnabled ? request.getContextPath() + "/track?token=" + token : null);
        request.getRequestDispatcher("/customer/order-tracking.jsp").forward(request, response);
    }

    private void apiTracking(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) { sendJson(response, HttpServletResponse.SC_UNAUTHORIZED, Map.of("success", false)); return; }
        String code = safeParameter(request.getParameter("code"), 40);
        Order order = OrderDao.findByCodeForUser(code, user.getId()).orElse(null);
        if (order == null) { sendJson(response, HttpServletResponse.SC_NOT_FOUND, Map.of("success", false)); return; }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("success", true);
        data.put("orderCode", order.getOrderCode());
        data.put("status", order.getStatus());
        data.put("paymentStatus", order.getPaymentStatus());
        data.put("shippingStatus", order.getShippingStatus());
        data.put("timeline", timeline(order.getOrderId(), false));
        sendJson(response, HttpServletResponse.SC_OK, data);
    }

    private void publicTracking(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = safeParameter(request.getParameter("token"), 64);
        if (token == null || !token.matches("[a-f0-9]{64}")) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        String sql = "SELECT order_id, order_code, status, payment_status, shipping_status, placed_at, completed_at "
                + "FROM orders WHERE tracking_token=? AND tracking_enabled=1";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                long orderId = rs.getLong("order_id");
                Map<String, Object> tracking = new LinkedHashMap<>();
                tracking.put("orderCode", rs.getString("order_code"));
                tracking.put("status", rs.getString("status"));
                tracking.put("paymentStatus", rs.getString("payment_status"));
                tracking.put("shippingStatus", rs.getString("shipping_status"));
                tracking.put("placedAt", format(rs.getTimestamp("placed_at")));
                tracking.put("completedAt", format(rs.getTimestamp("completed_at")));
                tracking.put("events", timeline(orderId, true));
                request.setAttribute("tracking", tracking);
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        request.getRequestDispatcher("/customer/track-public.jsp").forward(request, response);
    }

    private static List<Map<String, Object>> timeline(long orderId, boolean publicView) {
        List<Map<String, Object>> events = new ArrayList<>();
        try (Connection connection = Database.getConnection()) {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT new_status, note, changed_at FROM order_status_history WHERE order_id=? ORDER BY changed_at,history_id")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String status = rs.getString("new_status");
                        String note = rs.getString("note");
                        addEvent(events, publicView ? status : status, publicView ? null : note,
                                "Trạng thái đơn hàng", rs.getTimestamp("changed_at"));
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT pu.stage, pu.progress_percent, pu.message, pu.created_at FROM production_updates pu "
                            + "JOIN order_items oi ON oi.order_item_id=pu.order_item_id WHERE oi.order_id=? ORDER BY pu.created_at,pu.production_update_id")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String stage = safeStage(rs.getString("stage"));
                        int progress = rs.getInt("progress_percent");
                        String title = "Tiến độ sản xuất: " + stage + (progress >= 0 && progress <= 100 ? " (" + progress + "%)" : "");
                        addEvent(events, title, publicView ? null : rs.getString("message"), "Xưởng in", rs.getTimestamp("created_at"));
                    }
                }
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT se.event_title, se.description, se.occurred_at FROM shipment_events se WHERE se.order_id=? ORDER BY se.occurred_at,se.shipment_event_id")) {
                ps.setLong(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String title = publicView ? "Cập nhật vận chuyển" : safeText(rs.getString("event_title"), 180);
                        addEvent(events, title, publicView ? null : rs.getString("description"), "Vận chuyển", rs.getTimestamp("occurred_at"));
                    }
                }
            }
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
        events.sort(Comparator.comparing(event -> (LocalDateTime) event.get("sortAt")));
        for (Map<String, Object> event : events) event.remove("sortAt");
        return events;
    }

    private static void addEvent(List<Map<String, Object>> events, String title, String details, String source, Timestamp timestamp) {
        if (timestamp == null) return;
        LocalDateTime at = timestamp.toLocalDateTime();
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("title", safeText(title, 180));
        if (details != null && !details.isBlank()) event.put("details", safeText(details, 1000));
        event.put("source", source);
        event.put("when", at.format(DISPLAY_TIME));
        event.put("sortAt", at);
        events.add(event);
    }

    private static String safeStage(String stage) {
        if (stage == null) return "Đang cập nhật";
        return switch (stage.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ACCEPTED" -> "Xưởng đã nhận việc";
            case "PRINTING" -> "Đang in";
            case "POST_PROCESSING" -> "Đang hoàn thiện";
            case "READY_FOR_SHIPPING" -> "Sẵn sàng giao";
            case "SHIPPED" -> "Đang vận chuyển";
            case "DELIVERED" -> "Đã giao";
            case "COMPLETED" -> "Hoàn thành";
            default -> "Đang cập nhật";
        };
    }

    private static String safeText(String text, int max) {
        if (text == null) return "";
        String value = text.strip();
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String safeParameter(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.length() > max ? null : trimmed;
    }

    private static String format(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(DISPLAY_TIME);
    }

    private static void sendJson(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(Json.stringify(data));
    }
}
