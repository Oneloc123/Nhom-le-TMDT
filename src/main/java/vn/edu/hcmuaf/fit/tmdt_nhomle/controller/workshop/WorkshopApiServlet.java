package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.workshop;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.WorkshopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintWorkflowDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintPricingDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderChangeRequestDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderMessageDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
                if (path.length == 3 && "custom-print-files".equals(path[0])) {
                    serveCustomPrintFile(request, actor, shopId, path, response);
                    return;
                } else if (path.length == 1 && "custom-print-pricing".equals(path[0])) {
                    data = CustomPrintPricingDao.listForWorkshop(actor.getId(), shopId);
                } else if (path.length == 1 && "custom-print-requests".equals(path[0])) {
                    data = CustomPrintWorkflowDao.listForWorkshop(actor.getId(), shopId, request.getParameter("q"), request.getParameter("status"));
                } else if (path.length == 2 && "custom-print-requests".equals(path[0])) {
                    long id=positiveId(path[1]);
                    data=CustomPrintWorkflowDao.listForWorkshop(actor.getId(),shopId,null,"ALL").stream()
                            .filter(row->((Number)row.get("id")).longValue()==id).findFirst()
                            .orElseThrow(()->new WorkshopDao.NotFoundException("Không tìm thấy yêu cầu in 3D của xưởng này."));
                } else if (path.length == 1 && "dashboard".equals(path[0])) {
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
                } else if (path.length == 1 && "change-requests".equals(path[0])) {
                    data = OrderChangeRequestDao.listForWorkshop(actor.getId(), shopId,
                            normalizeStatusFilter(request.getParameter("status")));
                } else if (path.length == 1 && "messages".equals(path[0])) {
                    data = OrderMessageDao.threadsForWorkshop(actor.getId(), shopId);
                } else if (path.length == 2 && "messages".equals(path[0])) {
                    data = OrderMessageDao.threadForWorkshop(positiveId(path[1]), shopId, actor.getId());
                    if (data == null) throw new WorkshopDao.NotFoundException("Không tìm thấy hội thoại thuộc xưởng này.");
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
            if (path.length == 1 && "custom-print-pricing".equals(path[0])) {
                CustomPrintPricingDao.saveProfile(actor.getId(),shopId,values(request,
                        "technology","material","materialPricePerGram","materialDensity","materialWasteFactor",
                        "machineCostPerHour","depositionRate","wallThickness","supportFactor","infillUsageFactor",
                        "referenceLayerHeight","technologyFactor","complexityFactor","setupHours","postProcessingFee",
                        "minimumOrderPrice","minimumLayerHeight","maximumLayerHeight","minimumInfill","maximumInfill",
                        "productionDays","active","useMaterialCost","useMachineCost","useInfillFactor",
                        "useLayerHeightFactor","useTechnologyFactor","useComplexityFactor","usePostProcessingFee",
                        "useQuantityDiscount","useMinimumPrice"));
            } else if (path.length == 3 && "custom-print-pricing".equals(path[0]) && "options".equals(path[2])) {
                CustomPrintPricingDao.saveOption(actor.getId(),shopId,positiveId(path[1]),request.getParameter("type"),
                        request.getParameter("value"),request.getParameter("label"),decimalParam(request.getParameter("fee"),java.math.BigDecimal.ZERO),
                        Boolean.parseBoolean(request.getParameter("active")));
            } else if (path.length == 3 && "custom-print-pricing".equals(path[0]) && "discounts".equals(path[2])) {
                CustomPrintPricingDao.saveDiscount(actor.getId(),shopId,positiveId(path[1]),intParam(request,"minimumQuantity",0),
                        decimalParam(request.getParameter("discountRate"),java.math.BigDecimal.ZERO));
            } else if (path.length == 3 && "custom-print-requests".equals(path[0]) && "quote".equals(path[2])) {
                CustomPrintWorkflowDao.respondToQuote(actor.getId(),shopId,positiveId(path[1]),
                        decimalParam(request.getParameter("amount"),java.math.BigDecimal.ZERO),
                        intParam(request,"productionDays",0),request.getParameter("note"));
            } else if (path.length == 3 && "custom-print-requests".equals(path[0]) && "close".equals(path[2])) {
                CustomPrintWorkflowDao.closeByWorkshop(actor.getId(),shopId,positiveId(path[1]),request.getParameter("note"));
            } else if (path.length == 1 && "profile".equals(path[0])) {
                WorkshopDao.updateProfile(actor.getId(), shopId, values(request,
                        "name", "contactPhone", "contactEmail", "addressLine", "province", "district",
                        "description", "logoUrl", "capabilities", "acceptedFileFormats",
                        "maxBuildDimensions", "operatingHours", "estimatedLeadDays", "maximumCustomPrintQuantity",
                        "serviceNotes", "latitude", "longitude"));
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
            } else if (path.length == 3 && "change-requests".equals(path[0]) && "respond".equals(path[2])) {
                java.math.BigDecimal priceDelta = decimalParam(request.getParameter("priceDelta"), java.math.BigDecimal.ZERO);
                java.math.BigDecimal cancelFee = decimalParam(request.getParameter("cancelFee"), java.math.BigDecimal.ZERO);
                OrderChangeRequestDao.respond(actor.getId(), shopId, positiveId(path[1]), request.getParameter("action"),
                        request.getParameter("note"), priceDelta, cancelFee);
            } else if (path.length == 2 && "messages".equals(path[0])) {
                String body = request.getParameter("message");
                if (body != null && (body.matches("(?s).*\\b(?:mật khẩu|password|otp|cvv|pin ngân hàng)\\b.*") || body.matches("(?s).*\\b(?:\\d[ -]?){13,19}\\b.*"))) {
                    throw new IllegalArgumentException("Không gửi mật khẩu, OTP hoặc thông tin thẻ thanh toán trong hội thoại.");
                }
                OrderMessageDao.sendFromWorkshop(positiveId(path[1]), shopId, actor.getId(), body);
            } else if (path.length == 4 && "orders".equals(path[0]) && "items".equals(path[1])
                    && "progress".equals(path[3])) {
                WorkshopDao.addProductionUpdate(actor.getId(), shopId, positiveId(path[2]), actor.getId(),
                        request.getParameter("stage"), intParam(request, "progress", -1), request.getParameter("message"),
                        optionalEta(request.getParameter("estimatedDeliveryDate")));
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

    static String normalizeStatusFilter(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw.trim())) return null;
        return raw.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static LocalDate optionalEta(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày dự kiến phải theo định dạng YYYY-MM-DD.");
        }
    }

    private static void serveCustomPrintFile(HttpServletRequest request,User actor,long shopId,String[] path,HttpServletResponse response)throws IOException{
        long id=positiveId(path[2]);
        var file="requests".equals(path[1])?CustomPrintWorkflowDao.fileForWorkshopRequest(actor.getId(),shopId,id)
                :"orders".equals(path[1])?CustomPrintWorkflowDao.fileForWorkshopOrder(actor.getId(),shopId,id):java.util.Optional.<CustomPrintWorkflowDao.ProtectedFile>empty();
        if(file.isEmpty()){response.sendError(HttpServletResponse.SC_NOT_FOUND);return;}
        String key=file.get().storageUrl();
        if(key==null||!key.matches("designs/[a-f0-9]{32}\\.obj")){response.sendError(HttpServletResponse.SC_NOT_FOUND);return;}
        String configured=System.getenv("PK_UPLOAD_DIR");Path root=(configured==null||configured.isBlank())
                ?Path.of(System.getProperty("user.home"),".printkraft-uploads").toAbsolutePath().normalize()
                :Path.of(configured.trim()).toAbsolutePath().normalize();
        Files.createDirectories(root);
        String webRootPath=request.getServletContext().getRealPath("/");
        if(webRootPath!=null){Path realWebRoot=Path.of(webRootPath).toRealPath();root=root.toRealPath();if(root.startsWith(realWebRoot)){response.sendError(HttpServletResponse.SC_NOT_FOUND);return;}}
        Path base=root.resolve("designs").normalize(),actual=root.resolve(key).normalize();
        if(!actual.startsWith(base)||!Files.isRegularFile(actual)){response.sendError(HttpServletResponse.SC_NOT_FOUND);return;}
        response.setContentType("application/octet-stream");response.setHeader("X-Content-Type-Options","nosniff");
        response.setHeader("Cache-Control","private, no-store");
        response.setHeader("Content-Disposition","attachment; filename*=UTF-8''"+
                URLEncoder.encode(file.get().originalFilename(),StandardCharsets.UTF_8).replace("+","%20"));
        response.setContentLengthLong(Files.size(actual));
        try(var input=Files.newInputStream(actual);var output=response.getOutputStream()){input.transferTo(output);}
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

    private static java.math.BigDecimal decimalParam(String raw, java.math.BigDecimal fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try { return new java.math.BigDecimal(raw.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Giá trị tiền tệ không hợp lệ."); }
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
