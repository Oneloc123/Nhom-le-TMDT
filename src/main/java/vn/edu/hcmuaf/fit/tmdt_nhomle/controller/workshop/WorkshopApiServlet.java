package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.workshop;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.WorkshopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet(name = "workshopApiServlet", urlPatterns = "/workshop/api/*")
public class WorkshopApiServlet extends HttpServlet {
    private static final System.Logger LOG = System.getLogger(WorkshopApiServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            User actor = AuthFilter.currentUser(request);
            String[] path = path(request);
            Object data;
            if (path.length == 1 && "session".equals(path[0])) {
                data = Map.of("name", actor.getFullName() == null ? "Xưởng in" : actor.getFullName(),
                        "email", actor.getEmail() == null ? "" : actor.getEmail(),
                        "shops", WorkshopDao.listOwnedShops(actor.getId()));
            } else {
                long shopId = shopId(request);
                if (path.length == 1 && "dashboard".equals(path[0])) {
                    data = WorkshopDao.dashboard(actor.getId(), shopId);
                } else if (path.length == 1 && "profile".equals(path[0])) {
                    data = WorkshopDao.profile(actor.getId(), shopId);
                } else if (path.length == 1 && "categories".equals(path[0])) {
                    data = WorkshopDao.categories();
                } else if (path.length == 1 && "offers".equals(path[0])) {
                    data = WorkshopDao.listOffers(actor.getId(), shopId, request.getParameter("q"),
                            request.getParameter("status"), optionalId(request.getParameter("category")),
                            intParam(request, "page", 1), intParam(request, "size", 20));
                } else if (path.length == 1 && "orders".equals(path[0])) {
                    data = WorkshopDao.listOrders(actor.getId(), shopId, request.getParameter("q"),
                            request.getParameter("status"), intParam(request, "page", 1), intParam(request, "size", 20));
                } else if (path.length == 2 && "orders".equals(path[0])) {
                    data = WorkshopDao.orderItemDetail(actor.getId(), shopId, positiveId(path[1])).orElseThrow(
                            () -> new WorkshopDao.NotFoundException("Không tìm thấy dòng đơn hàng thuộc xưởng này."));
                } else if (path.length == 1 && "quotes".equals(path[0])) {
                    data = WorkshopDao.listQuotes(actor.getId(), shopId, request.getParameter("q"),
                            request.getParameter("status"), intParam(request, "page", 1), intParam(request, "size", 20));
                } else if (path.length == 1 && "reviews".equals(path[0])) {
                    data = WorkshopDao.reviews(actor.getId(), shopId);
                } else {
                    throw new WorkshopDao.NotFoundException("Workshop API không tồn tại.");
                }
            }
            send(response, HttpServletResponse.SC_OK, Map.of("data", data));
        } catch (WorkshopDao.NotFoundException e) {
            send(response, HttpServletResponse.SC_NOT_FOUND, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            send(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", safeMessage(e)));
        } catch (DataAccessException e) {
            LOG.log(System.Logger.Level.ERROR, "Workshop API read failed", e);
            send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Không thể đọc dữ liệu xưởng. Vui lòng thử lại sau."));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            User actor = AuthFilter.currentUser(request);
            String[] path = path(request);
            long shopId = shopId(request);
            if (path.length == 1 && "profile".equals(path[0])) {
                WorkshopDao.updateProfile(actor.getId(), shopId, values(request,
                        "name", "contactPhone", "contactEmail", "addressLine", "province", "district",
                        "description", "logoUrl", "capabilities", "acceptedFileFormats",
                        "maxBuildDimensions", "operatingHours", "estimatedLeadDays", "serviceNotes"));
            } else if (path.length == 1 && "offers".equals(path[0])) {
                long productId = WorkshopDao.createProductAndOffer(actor.getId(), shopId, values(request,
                        "name", "description", "brand", "imageUrl", "categoryId", "customizable", "sku",
                        "printingMethod", "material", "unitPrice", "minimumQuantity", "productionDays", "stockStatus"));
                send(response, HttpServletResponse.SC_CREATED, Map.of("data", Map.of("productId", productId)));
                return;
            } else if (path.length == 2 && "products".equals(path[0])) {
                WorkshopDao.updateProductContent(actor.getId(), shopId, positiveId(path[1]), values(request,
                        "name", "description", "brand", "imageUrl", "categoryId", "customizable"));
            } else if (path.length == 2 && "offers".equals(path[0])) {
                WorkshopDao.updateOffer(actor.getId(), shopId, positiveId(path[1]), values(request,
                        "sku", "printingMethod", "material", "unitPrice", "minimumQuantity", "productionDays", "stockStatus"));
            } else if (path.length == 3 && "offers".equals(path[0]) && "active".equals(path[2])) {
                String active = request.getParameter("active");
                if (!"true".equals(active) && !"false".equals(active)) {
                    throw new IllegalArgumentException("Trạng thái chào bán không hợp lệ.");
                }
                WorkshopDao.setOfferActive(actor.getId(), shopId, positiveId(path[1]), Boolean.parseBoolean(active));
            } else if (path.length == 3 && "quotes".equals(path[0]) && "respond".equals(path[2])) {
                WorkshopDao.respondToQuote(actor.getId(), shopId, positiveId(path[1]),
                        request.getParameter("action"), request.getParameter("amount"), request.getParameter("note"));
            } else if (path.length == 4 && "orders".equals(path[0]) && "items".equals(path[1])
                    && "progress".equals(path[3])) {
                WorkshopDao.addProductionUpdate(actor.getId(), shopId, positiveId(path[2]), actor.getId(),
                        request.getParameter("stage"), intParam(request, "progress", -1), request.getParameter("message"));
            } else {
                throw new WorkshopDao.NotFoundException("Workshop API không tồn tại.");
            }
            send(response, HttpServletResponse.SC_OK, Map.of("data", Map.of("updated", true)));
        } catch (WorkshopDao.NotFoundException e) {
            send(response, HttpServletResponse.SC_NOT_FOUND, Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            send(response, HttpServletResponse.SC_BAD_REQUEST, Map.of("error", safeMessage(e)));
        } catch (DataAccessException e) {
            LOG.log(System.Logger.Level.ERROR, "Workshop API write failed", e);
            send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    Map.of("error", "Không thể lưu thay đổi xưởng. Vui lòng thử lại sau."));
        }
    }

    private static String[] path(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank() || "/".equals(path)) return new String[0];
        return path.substring(1).split("/");
    }

    private static long shopId(HttpServletRequest request) {
        return positiveId(request.getParameter("shopId"));
    }

    private static Long optionalId(String raw) {
        return raw == null || raw.isBlank() ? null : positiveId(raw);
    }

    private static long positiveId(String raw) {
        try { long id = Long.parseLong(raw); if (id > 0) return id; }
        catch (NumberFormatException | NullPointerException ignored) { }
        throw new IllegalArgumentException("Mã xưởng hoặc bản ghi không hợp lệ.");
    }

    private static int intParam(HttpServletRequest request, String name, int fallback) {
        String raw = request.getParameter(name);
        if (raw == null || raw.isBlank()) return fallback;
        try { return Integer.parseInt(raw); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Tham số phân trang không hợp lệ."); }
    }

    private static Map<String, String> values(HttpServletRequest request, String... names) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : names) values.put(name, request.getParameter(name));
        return values;
    }

    private static String safeMessage(IllegalArgumentException e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? "Dữ liệu gửi lên không hợp lệ." : e.getMessage();
    }

    private static void send(HttpServletResponse response, int status, Object payload) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write(Json.stringify(payload));
    }
}
