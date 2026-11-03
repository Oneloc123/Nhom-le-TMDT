package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.MapProvider;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.MapProviderFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@WebServlet(name = "geoProxyServlet", urlPatterns = {"/api/geo/search", "/api/geo/reverse"})
public class GeoProxyServlet extends HttpServlet {
    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final MapProvider MAP_PROVIDER = MapProviderFactory.configured();
    private static final ConcurrentHashMap<String, CacheEntry> CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, AtomicLong> USER_RATE = new ConcurrentHashMap<>();
    private static long lastProviderRequest;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) { response.sendError(401); return; }
        String path = request.getServletPath();
        String endpoint;
        String key;
        if (path.endsWith("/search")) {
            String query = trim(request.getParameter("q"));
            if (query.length() < 3 || query.length() > 180) { response.sendError(400, "Từ khóa cần từ 3 đến 180 ký tự."); return; }
            endpoint = MAP_PROVIDER.searchUri(query).toString();
            key = MAP_PROVIDER.name() + ":search:" + query.toLowerCase(java.util.Locale.ROOT);
        } else {
            String latRaw = trim(request.getParameter("lat")); String lonRaw = trim(request.getParameter("lon"));
            double lat, lon;
            try { lat = Double.parseDouble(latRaw); lon = Double.parseDouble(lonRaw); }
            catch (NumberFormatException e) { response.sendError(400, "Tọa độ không hợp lệ."); return; }
            if (!Double.isFinite(lat) || !Double.isFinite(lon) || lat < 8 || lat > 24 || lon < 102 || lon > 110) {
                response.sendError(400, "Tọa độ nằm ngoài phạm vi Việt Nam."); return;
            }
            endpoint = MAP_PROVIDER.reverseUri(lat, lon).toString();
            key = MAP_PROVIDER.name() + ":reverse:" + String.format(java.util.Locale.ROOT, "%.5f,%.5f", lat, lon);
        }
        CacheEntry cached = CACHE.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAt > now) {
            write(response, cached.json); return;
        }
        AtomicLong last = USER_RATE.computeIfAbsent((long) user.getId(), ignored -> new AtomicLong());
        long previous = last.getAndSet(now);
        if (previous > 0 && now - previous < 1000) { response.setHeader("Retry-After", "2"); response.sendError(429, "Bạn thao tác quá nhanh. Vui lòng chờ một chút."); return; }
        try {
            synchronized (GeoProxyServlet.class) {
                long wait = 1100 - (System.currentTimeMillis() - lastProviderRequest);
                if (wait > 0) Thread.sleep(wait);
                lastProviderRequest = System.currentTimeMillis();
            }
            HttpRequest upstream = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(12))
                    .header("User-Agent", MAP_PROVIDER.userAgent())
                    .header("Accept", "application/json") .header("Accept-Language", "vi,en;q=0.8").GET().build();
            HttpResponse<String> result = CLIENT.send(upstream, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (result.statusCode() != 200 || result.body().length() > 1_000_000) {
                response.sendError(502, "Dịch vụ bản đồ tạm thời không phản hồi. Bạn có thể ghim vị trí và nhập địa chỉ thủ công."); return;
            }
            CACHE.put(key, new CacheEntry(result.body(), System.currentTimeMillis() + Duration.ofMinutes(5).toMillis()));
            if (CACHE.size() > 500) CACHE.entrySet().removeIf(entry -> entry.getValue().expiresAt < System.currentTimeMillis());
            write(response, result.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); response.sendError(503, "Yêu cầu bản đồ bị gián đoạn.");
        } catch (Exception e) {
            System.getLogger(GeoProxyServlet.class.getName()).log(System.Logger.Level.WARNING, "Dịch vụ địa lý chưa phản hồi.", e);
            response.sendError(502, "Không thể tra cứu bản đồ lúc này. Bạn có thể nhập địa chỉ thủ công.");
        }
    }

    private static void write(HttpServletResponse response, String json) throws IOException {
        response.setCharacterEncoding("UTF-8"); response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "private, max-age=60"); response.getWriter().write(json);
    }
    private static String trim(String value) { return value == null ? "" : value.trim(); }
    private record CacheEntry(String json, long expiresAt) { }
}
