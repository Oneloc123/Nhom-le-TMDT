package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CustomPrintRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.DesignFile;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceEstimate;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.CustomPrintConfig;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.NotificationService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Persistence for one custom-print job used by both direct checkout and quote requests. */
public final class CustomPrintWorkflowDao {
    private static final String REQUEST_COLUMNS = "r.custom_request_id,r.user_id,r.design_file_id,r.request_code,"
            + "r.shop_id,r.workflow_type,r.technology AS printing_method,r.material,r.infill_percent,r.layer_height_mm,r.quantity,"
            + "r.color_option,r.post_processing_option,r.customer_note,r.estimated_total,r.quoted_amount,"
            + "r.seller_response_note,r.production_days,r.quote_valid_until,r.status,r.order_id,r.dimensions_confirmed,s.shop_name shop_name,"
            + "f.original_filename,f.storage_url,f.mime_type,f.file_size_bytes,f.sha256_hash,f.analysis_status,"
            + "f.source_unit,f.width_mm,f.depth_mm,f.height_mm,f.volume_cm3,f.surface_area_cm2,f.triangle_count,f.analysis_message";
    private static final String REQUEST_FROM = " FROM custom_print_requests r JOIN design_files f"
            + " ON f.design_file_id=r.design_file_id AND f.user_id=r.user_id LEFT JOIN print_shops s ON s.shop_id=r.shop_id";

    private CustomPrintWorkflowDao() { }

    public record CreatedDraft(long requestId, long designFileId, String requestCode) { }
    public record ProtectedFile(String storageUrl, String originalFilename, String mimeType) { }

    public static CreatedDraft createDraft(long userId, String originalFilename, String storageUrl,
                                           String mimeType, long fileSize, String sha256,
                                           ModelAnalysis analysis, String technology, String material, int quantity) {
        String code = "PK3D" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long fileId = DesignFileDao.insertAnalyzed(conn, userId, originalFilename, storageUrl,
                        mimeType, fileSize, sha256, analysis);
                long requestId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO custom_print_requests(user_id,design_file_id,request_code,technology,material,quantity,status,created_at,updated_at)"
                                + " VALUES(?,?,?,?,?,?,'DRAFT',NOW(),NOW())", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, userId); ps.setLong(2, fileId); ps.setString(3, code);
                    ps.setString(4, technology); ps.setString(5, material); ps.setInt(6, quantity); ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Không tạo được hồ sơ in 3D.");
                        requestId = keys.getLong(1);
                    }
                }
                insertHistory(conn, requestId, userId, null, "DRAFT", "Khách hàng tải mô hình 3D.");
                conn.commit();
                return new CreatedDraft(requestId, fileId, code);
            } catch (SQLException | RuntimeException e) {
                conn.rollback(); throw e;
            } finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static long createQuoteDraftForExistingFile(long userId,long sourceRequestId){
        String code="PK3D"+UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT);
        try(Connection conn=Database.getConnection()){
            conn.setAutoCommit(false);
            try{
                long fileId; String technology; String material; int quantity;
                try(PreparedStatement ps=conn.prepareStatement("SELECT r.design_file_id,r.technology,r.material,r.quantity FROM custom_print_requests r"
                        +" JOIN design_files f ON f.design_file_id=r.design_file_id AND f.user_id=r.user_id WHERE r.custom_request_id=?"
                        +" AND r.user_id=? AND f.deleted_at IS NULL FOR UPDATE")){
                    ps.setLong(1,sourceRequestId);ps.setLong(2,userId);try(ResultSet rs=ps.executeQuery()){
                        if(!rs.next())throw new IllegalArgumentException("Không tìm thấy tệp thiết kế thuộc tài khoản của bạn.");
                        fileId=rs.getLong(1);technology=rs.getString(2);material=rs.getString(3);quantity=rs.getInt(4);
                    }
                }
                long id;
                try(PreparedStatement ps=conn.prepareStatement("INSERT INTO custom_print_requests(user_id,design_file_id,request_code,technology,material,quantity,status,created_at,updated_at)"
                        +" VALUES(?,?,?,?,?,?,'DRAFT',NOW(),NOW())",Statement.RETURN_GENERATED_KEYS)){
                    ps.setLong(1,userId);ps.setLong(2,fileId);ps.setString(3,code);ps.setString(4,technology);
                    ps.setString(5,material);ps.setInt(6,quantity);ps.executeUpdate();try(ResultSet keys=ps.getGeneratedKeys()){
                        if(!keys.next())throw new SQLException("Không tạo được yêu cầu báo giá mới.");id=keys.getLong(1);
                    }
                }
                insertHistory(conn,id,userId,null,"DRAFT","Tạo yêu cầu riêng cho xưởng khác từ tệp đã lưu.");conn.commit();return id;
            }catch(SQLException|RuntimeException e){conn.rollback();throw e;}
            finally{conn.setAutoCommit(true);}
        }catch(SQLException e){throw DataAccessException.of(e);}
    }

    public static void validateDirectShopForOrder(Connection conn,CustomPrintRequest request)throws SQLException{
        if(request.shopId()==null)throw new IllegalArgumentException("Chưa chọn xưởng.");
        ensureShopAndOption(conn,request.shopId(),request,request.material(),request.printingMethod(),
                request.quantity() == null ? 0 : request.quantity());
    }

    public static List<CustomPrintRequest> listByUser(long userId) {
        String sql = "SELECT " + REQUEST_COLUMNS + REQUEST_FROM
                + " WHERE r.user_id=? AND f.deleted_at IS NULL ORDER BY r.created_at DESC,r.custom_request_id DESC";
        return query(sql, ps -> ps.setLong(1, userId));
    }

    public static Optional<CustomPrintRequest> findByIdForUser(long requestId, long userId) {
        String sql = "SELECT " + REQUEST_COLUMNS + REQUEST_FROM
                + " WHERE r.custom_request_id=? AND r.user_id=? AND f.deleted_at IS NULL LIMIT 1";
        return query(sql, ps -> { ps.setLong(1, requestId); ps.setLong(2, userId); }).stream().findFirst();
    }

    public static void updateDraftAnalysis(long requestId, long userId, ModelAnalysis analysis) {
        if (analysis == null) throw new IllegalArgumentException("Không đọc được mô hình.");
        String sql = "UPDATE design_files f JOIN custom_print_requests r ON r.design_file_id=f.design_file_id AND f.user_id=r.user_id"
                + " SET f.analysis_status=?,f.source_unit=?,f.width_mm=?,f.depth_mm=?,f.height_mm=?,f.volume_cm3=?,"
                + "f.surface_area_cm2=?,f.triangle_count=?,f.analysis_message=?"
                + " WHERE r.custom_request_id=? AND r.user_id=? AND r.status='DRAFT' AND r.order_id IS NULL AND f.deleted_at IS NULL";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            setAnalysis(ps, analysis); ps.setLong(10, requestId); ps.setLong(11, userId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Hồ sơ không còn ở trạng thái nháp.");
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static List<java.util.Map<String, Object>> listForWorkshop(long ownerUserId, long shopId,
                                                                       String search, String status) {
        StringBuilder sql = new StringBuilder("SELECT r.custom_request_id id,r.request_code,r.status,r.workflow_type,"
                + "r.technology technology,r.material,r.infill_percent,r.layer_height_mm,r.quantity,r.color_option,"
                + "r.post_processing_option,r.customer_note,r.estimated_total,r.quoted_amount,r.seller_response_note,"
                + "r.production_days,r.quote_valid_until,r.design_file_id,f.original_filename,f.file_size_bytes,"
                + "f.analysis_status,f.source_unit,f.width_mm,f.depth_mm,f.height_mm,f.volume_cm3,f.analysis_message,"
                + "u.full_name customer_name,u.email customer_email,u.phone customer_phone,r.created_at"
                + " FROM custom_print_requests r JOIN design_files f ON f.design_file_id=r.design_file_id AND f.user_id=r.user_id"
                + " JOIN users u ON u.user_id=r.user_id JOIN print_shops s ON s.shop_id=r.shop_id"
                + " WHERE s.owner_user_id=? AND r.shop_id=? AND r.workflow_type='QUOTE' AND f.deleted_at IS NULL");
        List<Object> args = new ArrayList<>(); args.add(ownerUserId); args.add(shopId);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) { sql.append(" AND r.status=?"); args.add(status.trim().toUpperCase(Locale.ROOT)); }
        if (search != null && !search.isBlank()) { sql.append(" AND (r.request_code LIKE ? OR f.original_filename LIKE ? OR u.full_name LIKE ?)"); String q="%"+search.trim()+"%";args.add(q);args.add(q);args.add(q); }
        sql.append(" ORDER BY FIELD(r.status,'PENDING_REVIEW','QUOTED','CUSTOMER_DECLINED','EXPIRED','ORDER_CREATED','CLOSED'),r.created_at DESC");
        List<java.util.Map<String,Object>> rows = new ArrayList<>();
        try (Connection conn=Database.getConnection(); PreparedStatement ps=conn.prepareStatement(sql.toString())) {
            for(int i=0;i<args.size();i++) ps.setObject(i+1,args.get(i));
            try(ResultSet rs=ps.executeQuery()){while(rs.next()){java.util.Map<String,Object> row=new java.util.LinkedHashMap<>();
                for(String key:List.of("id","request_code","status","workflow_type","technology","material","infill_percent","layer_height_mm","quantity","color_option","post_processing_option","customer_note","estimated_total","quoted_amount","seller_response_note","production_days","quote_valid_until","design_file_id","original_filename","file_size_bytes","analysis_status","source_unit","width_mm","depth_mm","height_mm","volume_cm3","analysis_message","customer_name","customer_email","customer_phone","created_at"))row.put(key,rs.getObject(key));
                rows.add(row);}}
            return rows;
        } catch(SQLException e){throw DataAccessException.of(e);}
    }

    private static void setAnalysis(PreparedStatement ps, ModelAnalysis a) throws SQLException {
        ps.setString(1,a.status().name()); ps.setString(2,a.unit());
        setDecimal(ps,3,a.widthMm()); setDecimal(ps,4,a.depthMm()); setDecimal(ps,5,a.heightMm());
        setDecimal(ps,6,a.volumeCm3()); setDecimal(ps,7,a.surfaceAreaCm2()); ps.setLong(8,a.triangleCount());
        setNullable(ps,9,a.message());
    }
    private static void setDecimal(PreparedStatement ps,int index,double value)throws SQLException {
        if(Double.isFinite(value)&&value>0)ps.setBigDecimal(index,BigDecimal.valueOf(value));else ps.setNull(index,java.sql.Types.DECIMAL);
    }

    public static Optional<CustomPrintRequest> findByIdForUser(Connection conn, long requestId,
                                                                long userId, boolean forUpdate) throws SQLException {
        String sql = "SELECT " + REQUEST_COLUMNS + REQUEST_FROM
                + " WHERE r.custom_request_id=? AND r.user_id=? AND f.deleted_at IS NULL"
                + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, requestId); ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? Optional.of(map(rs)) : Optional.empty(); }
        }
    }

    public static void prepareDirect(long userId, long requestId, long shopId, String technology,
                                     String material, int infill, BigDecimal layerHeight, int quantity,
                                     String color, String finish, String note, PrintPriceEstimate estimate,
                                     boolean dimensionsConfirmed) {
        updateDraft(userId, requestId, shopId, "DIRECT", technology, material, infill, layerHeight,
                quantity, color, finish, note, estimate, "READY_TO_ORDER", dimensionsConfirmed);
    }

    public static void submitQuote(long userId, long requestId, long shopId, String technology,
                                   String material, int infill, BigDecimal layerHeight, int quantity,
                                   String color, String finish, String note, PrintPriceEstimate estimate,
                                   boolean dimensionsConfirmed) {
        updateDraft(userId, requestId, shopId, "QUOTE", technology, material, infill, layerHeight,
                quantity, color, finish, note, estimate, "PENDING_REVIEW", dimensionsConfirmed);
        notifyShopOwner(shopId, requestId);
    }

    private static void updateDraft(long userId, long requestId, long shopId, String workflowType,
                                    String technology, String material, int infill, BigDecimal layerHeight,
                                    int quantity, String color, String finish, String note,
                                    PrintPriceEstimate estimate, String newStatus,
                                    boolean dimensionsConfirmed) {
        if (shopId <= 0 || ("DIRECT".equals(workflowType) && estimate == null)) {
            throw new IllegalArgumentException("Thiếu xưởng hoặc giá ước tính.");
        }
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                CustomPrintRequest request = findByIdForUser(conn, requestId, userId, true)
                        .orElseThrow(() -> new IllegalArgumentException("Hồ sơ in 3D không tồn tại."));
                if (!"DRAFT".equals(request.status()) && !"READY_TO_ORDER".equals(request.status())) {
                    throw new IllegalArgumentException("Hồ sơ in 3D đã được gửi hoặc xử lý.");
                }
                if ("QUOTE".equals(workflowType) && !"DRAFT".equals(request.status())) {
                    throw new IllegalArgumentException("Hãy tạo yêu cầu báo giá mới cho mô hình này.");
                }
                if ("DIRECT".equals(workflowType)) ensureShopAndOption(conn, shopId, request, material, technology, quantity);
                else ensureActiveShop(conn, shopId, request, technology, material, color, finish, quantity);
                if ("NEEDS_CONFIRMATION".equals(request.analysisStatus()) && !dimensionsConfirmed) {
                    throw new IllegalArgumentException("Hãy xác nhận kích thước mô hình trước khi tiếp tục.");
                }
                String sql = "UPDATE custom_print_requests SET shop_id=?,workflow_type=?,technology=?,material=?,"
                        + "infill_percent=?,layer_height_mm=?,quantity=?,color_option=?,post_processing_option=?,"
                        + "customer_note=?,estimated_total=?,dimensions_confirmed=?,status=?,submitted_at=NOW(),updated_at=NOW()"
                        + " WHERE custom_request_id=? AND user_id=? AND order_id IS NULL";
                String oldStatus = request.status();
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, shopId); ps.setString(2, workflowType); ps.setString(3, technology);
                    ps.setString(4, material); ps.setInt(5, infill); ps.setBigDecimal(6, layerHeight);
                    ps.setInt(7, quantity); setNullable(ps, 8, color); setNullable(ps, 9, finish);
                    setNullable(ps, 10, note);
                    if (estimate == null) ps.setNull(11, java.sql.Types.DECIMAL); else ps.setBigDecimal(11, estimate.totalEstimate());
                    ps.setBoolean(12, dimensionsConfirmed); ps.setString(13, newStatus);
                    ps.setLong(14, requestId); ps.setLong(15, userId);
                    if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Hồ sơ đã được cập nhật ở nơi khác.");
                }
                insertHistory(conn, requestId, userId, oldStatus, newStatus,
                        "Khách hàng " + ("QUOTE".equals(workflowType) ? "gửi yêu cầu báo giá." : "chọn đặt trực tiếp."));
                conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void respondToQuote(long ownerUserId, long shopId, long requestId,
                                      BigDecimal amount, int productionDays, String note) {
        if (amount == null || amount.signum() <= 0 || amount.compareTo(new BigDecimal("1000000000.00")) > 0) {
            throw new IllegalArgumentException("Giá báo phải lớn hơn 0 và không vượt quá 1 tỷ đồng.");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Giá báo chỉ được có tối đa 2 chữ số thập phân.");
        }
        if (productionDays < 1 || productionDays > 365) throw new IllegalArgumentException("Thời gian sản xuất phải từ 1 đến 365 ngày.");
        String response = clean(note, 500);
        if (response == null || response.isBlank()) throw new IllegalArgumentException("Hãy nhập ghi chú hoặc điều kiện báo giá.");
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requireOwnedShop(conn, ownerUserId, shopId);
                String oldStatus;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT status FROM custom_print_requests WHERE custom_request_id=? AND shop_id=? FOR UPDATE")) {
                    ps.setLong(1, requestId); ps.setLong(2, shopId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy yêu cầu thuộc xưởng này.");
                        oldStatus = rs.getString(1);
                    }
                }
                if (!"PENDING_REVIEW".equals(oldStatus)) throw new IllegalArgumentException("Yêu cầu không còn chờ xử lý.");
                try (PreparedStatement ps = conn.prepareStatement("UPDATE custom_print_requests SET quoted_amount=?,"
                        + "seller_response_note=?,production_days=?,quote_valid_until=TIMESTAMPADD(DAY,?,NOW()),quoted_at=NOW(),status='QUOTED',updated_at=NOW()"
                        + " WHERE custom_request_id=? AND shop_id=? AND status='PENDING_REVIEW'")) {
                    ps.setBigDecimal(1, amount.setScale(2, java.math.RoundingMode.UNNECESSARY));
                    ps.setString(2, response); ps.setInt(3, productionDays); ps.setInt(4, CustomPrintConfig.quoteValidityDays());
                    ps.setLong(5, requestId); ps.setLong(6, shopId);
                    if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Yêu cầu đã được xử lý.");
                }
                insertHistory(conn, requestId, ownerUserId, oldStatus, "QUOTED", "Xưởng gửi báo giá.");
                conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
        notifyCustomer(requestId,"Xưởng đã gửi báo giá in 3D. Hãy xem giá và thời hạn trong danh sách yêu cầu.");
    }

    public static void closeByWorkshop(long ownerUserId, long shopId, long requestId, String reason) {
        updatePendingQuote(ownerUserId, shopId, requestId, "CLOSED", clean(reason, 500), "Xưởng đóng yêu cầu báo giá.");
        notifyCustomer(requestId,"Xưởng đã đóng yêu cầu báo giá in 3D và gửi lý do phản hồi.");
    }

    public static void declineByCustomer(long userId, long requestId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                CustomPrintRequest request = findByIdForUser(conn, requestId, userId, true)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo giá."));
                if (!"QUOTED".equals(request.status()) || request.quoteValidUntil() == null) {
                    throw new IllegalArgumentException("Báo giá không còn hiệu lực để từ chối.");
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE custom_print_requests SET status='CUSTOMER_DECLINED',"
                        + "declined_at=NOW(),updated_at=NOW() WHERE custom_request_id=? AND user_id=? AND status='QUOTED'"
                        + " AND quote_valid_until>NOW() AND order_id IS NULL")) {
                    ps.setLong(1, requestId); ps.setLong(2, userId);
                    if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Báo giá đã được xử lý.");
                }
                insertHistory(conn, requestId, userId, "QUOTED", "CUSTOMER_DECLINED", "Khách hàng từ chối báo giá.");
                conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static boolean quoteIsStillValid(long requestId) {
        try (Connection conn = Database.getConnection()) { return quoteIsStillValid(conn, requestId); }
        catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static boolean quoteIsStillValid(Connection conn, long requestId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM custom_print_requests"
                + " WHERE custom_request_id=? AND status='QUOTED' AND quote_valid_until>NOW() AND order_id IS NULL")) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public static void expireQuotes() {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                List<long[]> expired = new ArrayList<>();
                try (PreparedStatement ps = conn.prepareStatement("SELECT custom_request_id,user_id FROM custom_print_requests"
                        + " WHERE status='QUOTED' AND quote_valid_until<=NOW() AND order_id IS NULL FOR UPDATE");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) expired.add(new long[]{rs.getLong(1), rs.getLong(2)});
                }
                try (PreparedStatement ps = conn.prepareStatement("UPDATE custom_print_requests SET status='EXPIRED',updated_at=NOW()"
                        + " WHERE custom_request_id=? AND status='QUOTED' AND quote_valid_until<=NOW() AND order_id IS NULL")) {
                    for (long[] row : expired) { ps.setLong(1, row[0]); ps.addBatch(); }
                    ps.executeBatch();
                }
                for (long[] row : expired) insertHistory(conn, row[0], null, "QUOTED", "EXPIRED", "Báo giá hết hạn sau thời hạn hiệu lực.");
                conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Optional<ProtectedFile> fileForWorkshopRequest(long ownerUserId, long shopId, long requestId) {
        String sql = "SELECT f.storage_url,f.original_filename,f.mime_type FROM custom_print_requests r"
                + " JOIN design_files f ON f.design_file_id=r.design_file_id AND f.user_id=r.user_id"
                + " JOIN print_shops s ON s.shop_id=r.shop_id"
                + " WHERE r.custom_request_id=? AND r.shop_id=? AND s.owner_user_id=? AND f.deleted_at IS NULL";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, requestId); ps.setLong(2, shopId); ps.setLong(3, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new ProtectedFile(rs.getString(1), rs.getString(2), rs.getString(3))) : Optional.empty();
            }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Optional<ProtectedFile> fileForWorkshopOrder(long ownerUserId, long shopId, long orderItemId) {
        String sql = "SELECT f.storage_url,f.original_filename,f.mime_type FROM order_items oi"
                + " JOIN orders o ON o.order_id=oi.order_id JOIN design_files f ON f.design_file_id=oi.design_file_id"
                + " JOIN print_shops s ON s.shop_id=oi.shop_id"
                + " WHERE oi.order_item_id=? AND oi.shop_id=? AND s.owner_user_id=? AND f.user_id=o.user_id AND f.deleted_at IS NULL";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, orderItemId); ps.setLong(2, shopId); ps.setLong(3, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(new ProtectedFile(rs.getString(1), rs.getString(2), rs.getString(3))) : Optional.empty();
            }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static void ensureShopAndOption(Connection conn, long shopId, CustomPrintRequest request,
                                            String material, String technology, int quantity) throws SQLException {
        ensureShopFileCompatibility(conn,shopId,request,true,quantity);
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM shop_print_price_profiles"
                + " WHERE shop_id=? AND printing_method=? AND material=? AND is_active=1")) {
            ps.setLong(1, shopId); ps.setString(2, technology); ps.setString(3, material);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getLong(1) != 1) throw new IllegalArgumentException("Xưởng chưa cấu hình giá cho công nghệ/vật liệu đã chọn.");
            }
        }
    }

    private static void ensureActiveShop(Connection conn, long shopId, CustomPrintRequest request,
                                         String technology, String material, String color, String finish,
                                         int quantity) throws SQLException {
        String capabilities = ensureShopFileCompatibility(conn,shopId,request,false,quantity);
        Long profileId = null;
        try (PreparedStatement ps = conn.prepareStatement("SELECT profile_id FROM shop_print_price_profiles"
                + " WHERE shop_id=? AND printing_method=? AND material=? AND is_active=1")) {
            ps.setLong(1, shopId); ps.setString(2, technology); ps.setString(3, material);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) profileId = rs.getLong(1); }
        }
        if (profileId == null && (!hasCapability(capabilities, technology) || !hasCapability(capabilities, material))) {
            throw new IllegalArgumentException("Xưởng chưa khai báo hỗ trợ công nghệ hoặc vật liệu đã chọn.");
        }
        if (profileId == null && ((color != null && !color.isBlank()) || (finish != null && !finish.isBlank()))) {
            throw new IllegalArgumentException("Xưởng chưa khai báo hỗ trợ màu hoặc gia công đã chọn.");
        }
        if (profileId != null) {
            requireActiveOption(conn, profileId, "COLOR", color);
            requireActiveOption(conn, profileId, "FINISH", finish);
        }
    }

    private static void requireActiveOption(Connection conn, long profileId, String type, String value) throws SQLException {
        if (value == null || value.isBlank()) return;
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM shop_print_price_options"
                + " WHERE profile_id=? AND option_type=? AND option_value=? AND is_active=1")) {
            ps.setLong(1, profileId); ps.setString(2, type); ps.setString(3, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Xưởng không hỗ trợ tùy chọn màu hoặc gia công đã chọn.");
            }
        }
    }

    private static String ensureShopFileCompatibility(Connection conn,long shopId,CustomPrintRequest request,
                                                       boolean requireKnownSize,int quantity)throws SQLException{
        String formats;String maxDimensions;String extension;String capabilities;int maximumQuantity;
        try(PreparedStatement ps=conn.prepareStatement("SELECT s.accepted_file_formats,s.max_build_dimensions,f.original_filename,s.capabilities,s.maximum_custom_print_quantity"
                +" FROM print_shops s JOIN design_files f ON f.design_file_id=? AND f.user_id=? AND f.deleted_at IS NULL"
                +" WHERE s.shop_id=? AND s.verification_status='VERIFIED' AND (s.status IS NULL OR s.status='ACTIVE')")){
            ps.setLong(1,request.designFileId());ps.setLong(2,request.userId());ps.setLong(3,shopId);
            try(ResultSet rs=ps.executeQuery()){if(!rs.next())throw new IllegalArgumentException("Xưởng không nhận yêu cầu hoặc tệp không còn khả dụng.");formats=rs.getString(1);maxDimensions=rs.getString(2);extension=rs.getString(3);capabilities=rs.getString(4);maximumQuantity=rs.getInt(5);}
        }
        if(quantity>0&&quantity>maximumQuantity)throw new IllegalArgumentException("Số lượng vượt khả năng nhận đơn hiện tại của xưởng.");
        String ext=extension==null||!extension.contains(".")?"":extension.substring(extension.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);
        boolean formatSupported=false;if(formats!=null)for(String accepted:formats.toLowerCase(Locale.ROOT).split("[,;\\s]+")){if(accepted.replace(".","").equals(ext)){formatSupported=true;break;}}
        if(!formatSupported)throw new IllegalArgumentException("Xưởng chưa khai báo nhận định dạng tệp này.");
        if(request.widthMm()!=null&&request.depthMm()!=null&&request.heightMm()!=null){
            double[] capacity=parseDimensions(maxDimensions);if(capacity==null)throw new IllegalArgumentException("Xưởng chưa khai báo kích thước máy in.");
            double[] model={request.widthMm().doubleValue(),request.depthMm().doubleValue(),request.heightMm().doubleValue()};java.util.Arrays.sort(capacity);java.util.Arrays.sort(model);
            if(model[0]>capacity[0]||model[1]>capacity[1]||model[2]>capacity[2])throw new IllegalArgumentException("Mô hình vượt kích thước máy in của xưởng.");
        }else if(requireKnownSize)throw new IllegalArgumentException("Không đủ kích thước đáng tin cậy để đặt trực tiếp; hãy yêu cầu báo giá.");
        return capabilities;
    }

    private static double[] parseDimensions(String raw){
        if(raw==null||raw.isBlank())return null;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?<![A-Za-z0-9.])[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)").matcher(raw);
        double[] dimensions=new double[3];int count=0;
        while(m.find()){
            if(count==3)return null;
            try{dimensions[count]=Double.parseDouble(m.group());}catch(NumberFormatException e){return null;}
            if(!Double.isFinite(dimensions[count])||dimensions[count]<=0)return null;
            count++;
        }
        if(count!=3)return null;
        String lower=raw.toLowerCase(Locale.ROOT);
        boolean centimeters=java.util.regex.Pattern.compile("(?i)(?<![a-z])cm(?![a-z])").matcher(lower).find();
        boolean inches=java.util.regex.Pattern.compile("(?i)(?<![a-z])(?:in|inch|inches)(?![a-z])").matcher(lower).find();
        if(centimeters&&inches)return null;
        double scale=inches?25.4:centimeters?10.0:1.0;
        for(int i=0;i<dimensions.length;i++){dimensions[i]*=scale;if(!Double.isFinite(dimensions[i])||dimensions[i]>100_000)return null;}
        return dimensions;
    }

    private static boolean hasCapability(String capabilities,String requested){
        if (capabilities == null || requested == null) return false;
        String normalized = requested.trim().toLowerCase(Locale.ROOT);
        for (String token : capabilities.toLowerCase(Locale.ROOT).split("[,;|/\\s]+")) {
            String clean = token.replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
            if (normalized.equals(clean)) return true;
        }
        return false;
    }

    private static void updatePendingQuote(long ownerUserId, long shopId, long requestId,
                                           String newStatus, String note, String historyNote) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                requireOwnedShop(conn, ownerUserId, shopId);
                String customerStatus;
                try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM custom_print_requests"
                        + " WHERE custom_request_id=? AND shop_id=? FOR UPDATE")) {
                    ps.setLong(1, requestId); ps.setLong(2, shopId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Không tìm thấy yêu cầu thuộc xưởng này.");
                        customerStatus = rs.getString(1);
                    }
                }
                if (!"PENDING_REVIEW".equals(customerStatus)) throw new IllegalArgumentException("Yêu cầu không còn chờ xử lý.");
                try (PreparedStatement ps = conn.prepareStatement("UPDATE custom_print_requests SET status=?,seller_response_note=?,updated_at=NOW()"
                        + " WHERE custom_request_id=? AND shop_id=? AND status='PENDING_REVIEW'")) {
                    ps.setString(1, newStatus); setNullable(ps, 2, note); ps.setLong(3, requestId); ps.setLong(4, shopId);
                    if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Yêu cầu đã được xử lý.");
                }
                insertHistory(conn, requestId, ownerUserId, customerStatus, newStatus, historyNote);
                conn.commit();
            } catch (SQLException | RuntimeException e) { conn.rollback(); throw e; }
            finally { conn.setAutoCommit(true); }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static void requireOwnedShop(Connection conn, long ownerUserId, long shopId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM print_shops WHERE shop_id=? AND owner_user_id=?")) {
            ps.setLong(1, shopId); ps.setLong(2, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getLong(1) != 1) throw new IllegalArgumentException("Xưởng không thuộc tài khoản này.");
            }
        }
    }

    private static void notifyShopOwner(long shopId, long requestId) {
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(
                "SELECT owner_user_id FROM print_shops WHERE shop_id=?")) {
            ps.setLong(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long ownerId = rs.getLong(1);
                    if (!rs.wasNull()) NotificationService.notifyUser(ownerId, "CUSTOM_PRINT_QUOTE",
                            "Yêu cầu in 3D mới", "Khách hàng vừa gửi yêu cầu báo giá cho xưởng.",
                            "/workshop/quotes", "CUSTOM_PRINT_REQUEST", requestId);
                }
            }
        } catch (SQLException | RuntimeException e) {
            System.getLogger(CustomPrintWorkflowDao.class.getName()).log(System.Logger.Level.WARNING,
                    "Could not notify workshop about custom print request", e);
        }
    }

    private static void notifyCustomer(long requestId,String message){
        try(Connection conn=Database.getConnection();PreparedStatement ps=conn.prepareStatement(
                "SELECT user_id,request_code FROM custom_print_requests WHERE custom_request_id=?")){
            ps.setLong(1,requestId);try(ResultSet rs=ps.executeQuery()){if(rs.next())NotificationService.notifyUser(rs.getLong(1),
                    "CUSTOM_PRINT_QUOTE","Cập nhật yêu cầu in 3D",message,"/my-designs","CUSTOM_PRINT_REQUEST",requestId);}
        }catch(SQLException|RuntimeException e){System.getLogger(CustomPrintWorkflowDao.class.getName()).log(System.Logger.Level.WARNING,"Could not notify customer about custom print quote",e);}
    }

    private static void insertHistory(Connection conn, long requestId, Long actorId,
                                      String oldStatus, String newStatus, String note) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO custom_print_request_history"
                + "(custom_request_id,changed_by_user_id,old_status,new_status,note,created_at) VALUES(?,?,?,?,?,NOW())")) {
            ps.setLong(1, requestId);
            if (actorId == null) ps.setNull(2, java.sql.Types.BIGINT); else ps.setLong(2, actorId);
            setNullable(ps, 3, oldStatus); ps.setString(4, newStatus); setNullable(ps, 5, note); ps.executeUpdate();
        }
    }

    private static List<CustomPrintRequest> query(String sql, Binder binder) {
        List<CustomPrintRequest> rows = new ArrayList<>();
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(map(rs)); }
            return rows;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    private static CustomPrintRequest map(ResultSet rs) throws SQLException {
        long shopId = rs.getLong("shop_id"); Long shop = rs.wasNull() ? null : shopId;
        long orderId = rs.getLong("order_id"); Long order = rs.wasNull() ? null : orderId;
        int infill = rs.getInt("infill_percent"); Integer infillValue = rs.wasNull() ? null : infill;
        int quantity = rs.getInt("quantity"); Integer quantityValue = rs.wasNull() ? null : quantity;
        int days = rs.getInt("production_days"); Integer daysValue = rs.wasNull() ? null : days;
        long size = rs.getLong("file_size_bytes"); Long fileSize = rs.wasNull() ? null : size;
        long triangleCount = rs.getLong("triangle_count"); Long triangles = rs.wasNull() ? null : triangleCount;
        return new CustomPrintRequest(rs.getLong("custom_request_id"), rs.getLong("user_id"),
                rs.getLong("design_file_id"), rs.getString("request_code"), shop, rs.getString("shop_name"),
                rs.getString("workflow_type"), rs.getString("printing_method"), rs.getString("material"),
                infillValue, rs.getBigDecimal("layer_height_mm"), quantityValue,
                rs.getString("color_option"), rs.getString("post_processing_option"), rs.getString("customer_note"),
                rs.getBigDecimal("estimated_total"), rs.getBigDecimal("quoted_amount"),
                rs.getString("seller_response_note"), daysValue, rs.getTimestamp("quote_valid_until"),
                rs.getString("status"), order, rs.getBoolean("dimensions_confirmed"),
                rs.getString("original_filename"), rs.getString("storage_url"),
                rs.getString("mime_type"), fileSize, rs.getString("sha256_hash"), rs.getString("analysis_status"),
                rs.getString("source_unit"), rs.getBigDecimal("width_mm"), rs.getBigDecimal("depth_mm"),
                rs.getBigDecimal("height_mm"), rs.getBigDecimal("volume_cm3"),
                rs.getBigDecimal("surface_area_cm2"), triangles, rs.getString("analysis_message"));
    }

    private static String clean(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    private static void setNullable(PreparedStatement ps, int index, String value) throws SQLException {
        if (value == null) ps.setNull(index, java.sql.Types.VARCHAR); else ps.setString(index, value);
    }

    @FunctionalInterface private interface Binder { void bind(PreparedStatement ps) throws SQLException; }
}
