package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintWorkflowDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.CustomPrintPricingDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AddressDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CustomPrintRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceEstimate;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceOption;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopPricingConfig;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomerAccountService;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PrintPriceEstimator;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ThreeDModelAnalyzer;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.AjaxResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.io.IOException;
import java.math.BigDecimal;
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
    private static final Set<String> EXTENSIONS = Set.of("obj");
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();
        if ("/my-designs/file".equals(path)) {
            serveFile(req, resp);
            return;
        }
        if ("/design".equals(path) && "suitableShops".equalsIgnoreCase(req.getParameter("action"))) {
            try { handleEligibleShops(req, resp); }
            catch (IllegalArgumentException e) { writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage()); }
            return;
        }
        if ("/design".equals(path) && "draft".equalsIgnoreCase(req.getParameter("action"))) {
            try { handleDraftDetail(req,resp); }
            catch (IllegalArgumentException e) { writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage()); }
            return;
        }

        User user = currentUser(req);
        prepare(req);
        if ("/my-designs".equals(path)) {
            req.setAttribute("requests", CustomPrintWorkflowDao.listByUser(user.getId()));
            req.getRequestDispatcher("/WEB-INF/views/user/design-list.jsp").forward(req, resp);
        } else if ("/design-request".equals(path)) {
            resp.sendRedirect(req.getContextPath() + "/design");
        } else {
            req.getRequestDispatcher("/customer/design.jsp").forward(req, resp);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        if ("/my-designs".equals(req.getServletPath())) {
            handleCustomerRequestAction(req, resp);
            return;
        }
        if ("/design-request".equals(req.getServletPath())) {
            resp.sendRedirect(req.getContextPath() + "/design");
            return;
        }
        if (!"/design".equals(req.getServletPath())) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        String action = req.getParameter("action");
        if ("uploadDraft".equals(action) || "reanalyze".equals(action)
                || "submitDirect".equals(action) || "submitQuote".equals(action)) {
            try {
                if ("uploadDraft".equals(action)) handleDraftUpload(req, resp);
                else if ("reanalyze".equals(action)) handleReanalysis(req, resp);
                else handleSubmit(req, resp, "submitQuote".equals(action));
            } catch (IllegalArgumentException e) {
                writeError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } catch (Exception e) {
                System.getLogger(CustomPrintController.class.getName()).log(System.Logger.Level.ERROR,
                        "Custom print workflow request failed", e);
                writeError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Không thể xử lý yêu cầu in 3D lúc này.");
            }
            return;
        }
        if ("uploadAndOrder".equalsIgnoreCase(action) || "order".equalsIgnoreCase(action)) {
            writeError(resp, HttpServletResponse.SC_BAD_REQUEST, "Luồng cũ đã được thay bằng yêu cầu in có xác thực giá ở máy chủ.");
            return;
        }
        AjaxResponse.send(resp, HttpServletResponse.SC_BAD_REQUEST,
                "{\"success\":false,\"message\":\"Thao tác không được hỗ trợ.\"}");
    }

    private void handleDraftUpload(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.setCharacterEncoding("UTF-8"); resp.setContentType("application/json;charset=UTF-8");
        User user=currentUser(req); if(user==null){writeError(resp,401,"Vui lòng đăng nhập trước.");return;}
        Part part;
        try { part=req.getPart("file"); }
        catch (IllegalStateException e) {
            writeError(resp,HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE,"Dung lượng tệp tối đa là 100 MB.");
            return;
        }
        if(part==null||part.getSize()<=0)throw new IllegalArgumentException("Vui lòng chọn tệp mô hình 3D.");
        if(part.getSize()>100L*1024*1024)throw new IllegalArgumentException("Dung lượng tệp tối đa là 100 MB.");
        String technology=enumParameter(req.getParameter("technology"),Set.of("FDM","SLA","SLS"),"Công nghệ in không hợp lệ.");
        String material=enumParameter(req.getParameter("material"),Set.of("PLA","ABS","PETG","TPU","RESIN","RESIN-STD","RESIN-CLEAR","RESIN-FLEX","NYLON","NYLON-PA12","NYLON-GF"),"Vật liệu không hợp lệ.");
        int quantity=requiredInt(req.getParameter("quantity"),"Số lượng không hợp lệ.");
        if(quantity<1||quantity>100000)throw new IllegalArgumentException("Số lượng phải từ 1 đến 100.000.");
        String original=cleanFilename(part.getSubmittedFileName()),ext=extension(original);
        if(!EXTENSIONS.contains(ext))throw new IllegalArgumentException("Chỉ nhận tệp OBJ.");
        Path folder=privateStorageRoot(req).resolve("designs").normalize();Files.createDirectories(folder);
        String key=UUID.randomUUID().toString().replace("-","")+"."+ext;Path stored=folder.resolve(key).normalize();
        if(!stored.startsWith(folder))throw new IllegalArgumentException("Tên tệp không hợp lệ.");
        boolean storedFileCreated=false;
        boolean draftPersisted=false;
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(var in=part.getInputStream();var out=Files.newOutputStream(stored,StandardOpenOption.CREATE_NEW)){
                storedFileCreated=true;
                byte[] buffer=new byte[8192];int read;while((read=in.read(buffer))!=-1){digest.update(buffer,0,read);out.write(buffer,0,read);}
            }
            String unit=ThreeDModelAnalyzer.normalizeUnit(req.getParameter("unit"));
            ModelAnalysis analysis=new ThreeDModelAnalyzer().analyze(stored,ext,unit);
            if(analysis.status()==ModelAnalysis.Status.INVALID)throw new IllegalArgumentException(analysis.message());
            CustomPrintWorkflowDao.CreatedDraft draft=CustomPrintWorkflowDao.createDraft(user.getId(),original,
                    "designs/"+key,detectedMime(ext),part.getSize(),HexFormat.of().formatHex(digest.digest()),analysis,
                    technology,material,quantity);
            draftPersisted=true;
            Map<String,Object> data=new java.util.LinkedHashMap<>();data.put("success",true);data.put("requestId",draft.requestId());
            data.put("requestCode",draft.requestCode());data.put("analysis",analysisJson(analysis));resp.getWriter().write(Json.stringify(data));
        }catch(Exception e){if(storedFileCreated&&!draftPersisted)Files.deleteIfExists(stored);throw e;}
    }

    private void handleReanalysis(HttpServletRequest req,HttpServletResponse resp)throws Exception{
        User user=currentUser(req);if(user==null){writeError(resp,401,"Vui lòng đăng nhập trước.");return;}
        long id=parseLong(req.getParameter("requestId"),-1);
        CustomPrintRequest request=CustomPrintWorkflowDao.findByIdForUser(id,user.getId()).orElseThrow(()->new IllegalArgumentException("Không tìm thấy hồ sơ in."));
        Path file=resolveStoredFile(req,request.storageUrl());String unit=ThreeDModelAnalyzer.normalizeUnit(req.getParameter("unit"));
        ModelAnalysis analysis=new ThreeDModelAnalyzer().analyze(file,extension(request.originalFilename()),unit);
        if(analysis.status()==ModelAnalysis.Status.INVALID)throw new IllegalArgumentException(analysis.message());
        CustomPrintWorkflowDao.updateDraftAnalysis(id,user.getId(),analysis);
        resp.setContentType("application/json;charset=UTF-8");resp.getWriter().write(Json.stringify(Map.of("success",true,"analysis",analysisJson(analysis))));
    }

    private void handleSubmit(HttpServletRequest req,HttpServletResponse resp,boolean quote)throws IOException{
        resp.setCharacterEncoding("UTF-8");resp.setContentType("application/json;charset=UTF-8");User user=currentUser(req);
        if(user==null){writeError(resp,401,"Vui lòng đăng nhập trước.");return;}
        long requestId=parseLong(req.getParameter("requestId"),-1),shopId=parseLong(req.getParameter("shopId"),-1);
        CustomPrintRequest draft=CustomPrintWorkflowDao.findByIdForUser(requestId,user.getId()).orElseThrow(()->new IllegalArgumentException("Không tìm thấy hồ sơ in thuộc tài khoản của bạn."));
        String tech=enumParameter(req.getParameter("technology"),Set.of("FDM","SLA","SLS"),"Công nghệ in không hợp lệ.");
        String material=enumParameter(req.getParameter("material"),Set.of("PLA","ABS","PETG","TPU","RESIN","RESIN-STD","RESIN-CLEAR","RESIN-FLEX","NYLON","NYLON-PA12","NYLON-GF"),"Vật liệu không hợp lệ.");
        int infill=requiredInt(req.getParameter("infill"),"Tỷ lệ infill không hợp lệ.");
        int quantity=requiredInt(req.getParameter("quantity"),"Số lượng không hợp lệ.");
        if(infill<0||infill>100||quantity<1||quantity>100000)throw new IllegalArgumentException("Infill hoặc số lượng không hợp lệ.");
        BigDecimal layer=parsePositiveDecimal(req.getParameter("layerHeight"),"Layer height không hợp lệ.");
        boolean confirmed="true".equalsIgnoreCase(req.getParameter("dimensionsConfirmed"));
        String color=value(req.getParameter("color")),finish=value(req.getParameter("finish")),note=value(req.getParameter("note"));
        if(note.length()>1000)throw new IllegalArgumentException("Ghi chú tối đa 1000 ký tự.");
        PrintPriceEstimate estimate=null;ShopPricingConfig pricing=CustomPrintPricingDao.findActive(shopId,tech,material).orElse(null);
        if(pricing!=null&&draft.volumeCm3()!=null&&("READY".equals(draft.analysisStatus())||"NEEDS_CONFIRMATION".equals(draft.analysisStatus()))){
            List<PrintPriceOption> selected=selectedOptions(pricing,color,finish);
            estimate=new PrintPriceEstimator().estimate(analysisFrom(draft),pricing.profile(),infill,layer,quantity,selected,pricing.discounts(),confirmed);
        }
        if(!quote&&estimate==null)throw new IllegalArgumentException("Mô hình hoặc bảng giá chưa đủ dữ liệu đặt trực tiếp; hãy gửi yêu cầu báo giá.");
        if(quote)CustomPrintWorkflowDao.submitQuote(user.getId(),requestId,shopId,tech,material,infill,layer,quantity,color,finish,note,estimate,confirmed);
        else CustomPrintWorkflowDao.prepareDirect(user.getId(),requestId,shopId,tech,material,infill,layer,quantity,color,finish,note,estimate,confirmed);
        String next=quote?req.getContextPath()+"/my-designs?created=1&code="+java.net.URLEncoder.encode(draft.requestCode(),StandardCharsets.UTF_8)
                :req.getContextPath()+"/order?mode=customPrint&requestId="+requestId;
        Map<String,Object> data=new java.util.LinkedHashMap<>();data.put("success",true);data.put("redirectUrl",next);
        data.put("estimatedTotal",estimate==null?null:estimate.totalEstimate());resp.getWriter().write(Json.stringify(data));
    }

    private void handleCustomerRequestAction(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        User user=currentUser(req);if(user==null){resp.sendRedirect(req.getContextPath()+"/login");return;}
        try{if("declineQuote".equals(req.getParameter("action")))CustomPrintWorkflowDao.declineByCustomer(user.getId(),parseLong(req.getParameter("requestId"),-1));
            else if("newQuoteDraft".equals(req.getParameter("action"))){long id=CustomPrintWorkflowDao.createQuoteDraftForExistingFile(user.getId(),parseLong(req.getParameter("requestId"),-1));resp.sendRedirect(req.getContextPath()+"/design?requestId="+id);return;}
            else throw new IllegalArgumentException("Thao tác báo giá không hợp lệ.");
            resp.sendRedirect(req.getContextPath()+"/my-designs?updated=1");
        }catch(IllegalArgumentException e){resp.sendRedirect(req.getContextPath()+"/my-designs?error="+java.net.URLEncoder.encode(e.getMessage(),StandardCharsets.UTF_8));}
    }

    private void handleEligibleShops(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        User user=currentUser(req);if(user==null){resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);return;}
        long id=parseLong(req.getParameter("requestId"),-1);
        CustomPrintRequest draft=CustomPrintWorkflowDao.findByIdForUser(id,user.getId()).orElseThrow(()->new IllegalArgumentException("Không tìm thấy hồ sơ in."));
        String tech=enumParameter(req.getParameter("tech"),Set.of("FDM","SLA","SLS"),"Công nghệ không hợp lệ.");
        String material=enumParameter(req.getParameter("material"),Set.of("PLA","ABS","PETG","TPU","RESIN","RESIN-STD","RESIN-CLEAR","RESIN-FLEX","NYLON","NYLON-PA12","NYLON-GF"),"Vật liệu không hợp lệ.");
        int infill=requiredInt(req.getParameter("infill"),"Tỷ lệ infill không hợp lệ.");
        int quantity=requiredInt(req.getParameter("quantity"),"Số lượng không hợp lệ.");
        if(infill<0||infill>100||quantity<1||quantity>100000)
            throw new IllegalArgumentException("Tỷ lệ infill hoặc số lượng không hợp lệ.");
        BigDecimal layer=parsePositiveDecimal(req.getParameter("layerHeight"),"Layer height không hợp lệ.");
        resp.setContentType("application/json;charset=UTF-8");resp.setHeader("Cache-Control","no-store");
        var customerLocation=AddressDao.findByUser(user.getId()).stream().filter(vn.edu.hcmuaf.fit.tmdt_nhomle.model.Address::hasCoordinates).findFirst().orElse(null);
        resp.getWriter().write(Json.stringify(CustomPrintPricingDao.recommend(user.getId(),draft,tech,material,infill,layer,quantity,
                req.getParameter("color"),req.getParameter("finish"),"true".equalsIgnoreCase(req.getParameter("dimensionsConfirmed")),
                customerLocation==null?null:customerLocation.getLatitude(),customerLocation==null?null:customerLocation.getLongitude(),req.getParameter("q"))));
    }

    private void handleDraftDetail(HttpServletRequest req,HttpServletResponse resp)throws IOException{
        User user=currentUser(req);if(user==null){resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);return;}
        long id=parseLong(req.getParameter("requestId"),-1);CustomPrintRequest draft=CustomPrintWorkflowDao.findByIdForUser(id,user.getId())
                .orElseThrow(()->new IllegalArgumentException("Không tìm thấy hồ sơ in của bạn."));
        Map<String,Object> data=new java.util.LinkedHashMap<>();data.put("success",true);data.put("requestId",draft.id());data.put("requestCode",draft.requestCode());
        data.put("filename",draft.originalFilename());data.put("analysis",analysisJson(analysisFrom(draft)));
        resp.setContentType("application/json;charset=UTF-8");resp.setHeader("Cache-Control","no-store");resp.getWriter().write(Json.stringify(data));
    }

    private static Map<String,Object> analysisJson(ModelAnalysis a){Map<String,Object> m=new java.util.LinkedHashMap<>();m.put("status",a.status().name());m.put("format",a.format());m.put("unit",a.unit());m.put("widthMm",a.widthMm());m.put("depthMm",a.depthMm());m.put("heightMm",a.heightMm());m.put("volumeCm3",a.volumeCm3());m.put("surfaceAreaCm2",a.surfaceAreaCm2());m.put("triangleCount",a.triangleCount());m.put("message",a.message());return m;}
    private static ModelAnalysis analysisFrom(CustomPrintRequest r){return new ModelAnalysis(ModelAnalysis.Status.valueOf(r.analysisStatus()),extension(r.originalFilename()),r.sourceUnit(),
            r.widthMm()==null?0:r.widthMm().doubleValue(),r.depthMm()==null?0:r.depthMm().doubleValue(),r.heightMm()==null?0:r.heightMm().doubleValue(),
            r.volumeCm3()==null?0:r.volumeCm3().doubleValue(),r.surfaceAreaCm2()==null?0:r.surfaceAreaCm2().doubleValue(),r.triangleCount()==null?0:r.triangleCount(),r.analysisMessage());}
    private static List<PrintPriceOption> selectedOptions(ShopPricingConfig c,String color,String finish){List<PrintPriceOption> selected=new java.util.ArrayList<>();for(String[] choice:new String[][]{{"COLOR",color},{"FINISH",finish}}){if(choice[1]==null||choice[1].isBlank())continue;selected.add(c.options().stream().filter(o->o.active()&&o.type().equals(choice[0])&&o.value().equals(choice[1])).findFirst().orElseThrow(()->new IllegalArgumentException("Xưởng không hỗ trợ tùy chọn đã chọn.")));}return selected;}
    private static String enumParameter(String raw,Set<String> allowed,String message){String value=raw==null?"":raw.trim().toUpperCase(Locale.ROOT);if(!allowed.contains(value))throw new IllegalArgumentException(message);return value;}
    private static BigDecimal parsePositiveDecimal(String raw,String message){try{BigDecimal n=new BigDecimal(raw);if(n.signum()<=0||n.compareTo(new BigDecimal("10"))>0)throw new IllegalArgumentException(message);return n;}catch(NumberFormatException|NullPointerException e){throw new IllegalArgumentException(message);}}
    private static String cleanFilename(String submitted){String s=submitted==null?"model.obj":submitted.replace('\\','/');s=s.substring(s.lastIndexOf('/')+1).trim();if(s.isBlank())s="model.obj";return s.length()>255?s.substring(s.length()-255):s;}
    private static String detectedMime(String ext){return "obj".equals(ext)?"model/obj":"application/octet-stream";}
    private Path resolveStoredFile(HttpServletRequest req,String storageUrl)throws IOException{if(storageUrl==null||!storageUrl.matches("designs/[a-f0-9]{32}\\.obj"))throw new IllegalArgumentException("Đường dẫn tệp không hợp lệ.");Path root=privateStorageRoot(req),base=root.resolve("designs").normalize(),file=root.resolve(storageUrl).normalize();if(!file.startsWith(base)||!Files.isRegularFile(file))throw new IllegalArgumentException("Tệp thiết kế không còn tồn tại.");return file;}
    private void serveFile(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = currentUser(req);
        if (user == null) { resp.sendError(HttpServletResponse.SC_UNAUTHORIZED); return; }
        long id = parseLong(req.getParameter("id"), -1);
        var file = CustomerAccountService.ownedDesignFile(user.getId(), id);
        if (file.isEmpty()) { resp.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        String key = String.valueOf(file.get().get("storage_url"));
        if (!key.matches("designs/[a-f0-9]{32}\\.obj")) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Path root = privateStorageRoot(req);
        Path base = root.resolve("designs").normalize();
        Path actual = root.resolve(key).normalize();
        if (!actual.startsWith(base) || !Files.isRegularFile(actual)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resp.setContentType("application/octet-stream");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "private, no-store");
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

    private Path privateStorageRoot(HttpServletRequest req) throws IOException {
        Path root = uploadRoot();
        Files.createDirectories(root);
        String webRootPath = req.getServletContext().getRealPath("/");
        if (webRootPath != null) {
            Path realRoot = root.toRealPath();
            Path realWebRoot = Path.of(webRootPath).toRealPath();
            if (realRoot.startsWith(realWebRoot)) {
                throw new IOException("PK_UPLOAD_DIR must be outside the deployed web root.");
            }
            return realRoot;
        }
        return root;
    }

    private static void writeError(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.setStatus(status);
        resp.getWriter().write("{\"success\":false,\"message\":"
                + AjaxResponse.quote(message == null || message.isBlank() ? "Dữ liệu đặt in không hợp lệ." : message) + "}");
    }

    private static int requiredInt(String raw, String message) {
        try {
            if (raw == null || raw.isBlank()) throw new NumberFormatException();
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(message);
        }
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
