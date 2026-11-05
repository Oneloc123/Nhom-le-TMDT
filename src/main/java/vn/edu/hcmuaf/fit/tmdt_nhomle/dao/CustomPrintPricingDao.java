package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceOption;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintQuantityDiscount;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopPricingConfig;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopPrintPriceProfile;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.CustomPrintRequest;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.PrintPriceEstimate;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.PrintPriceEstimator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Price profile configuration and lookup, scoped to an owning workshop where it is mutated. */
public final class CustomPrintPricingDao {
    private static final String PROFILE_COLUMNS = "profile_id,shop_id,printing_method,material,"
            + "material_price_per_gram,material_density_g_cm3,material_waste_factor,machine_cost_per_hour,"
            + "deposition_rate_cm3_per_hour,wall_thickness_mm,support_material_factor,infill_usage_factor,"
            + "reference_layer_height_mm,technology_factor,complexity_factor,setup_hours,post_processing_fee,"
            + "minimum_order_price,minimum_layer_height_mm,maximum_layer_height_mm,minimum_infill_percent,"
            + "maximum_infill_percent,production_days,use_material_cost,use_machine_cost,use_infill_factor,"
            + "use_layer_height_factor,use_technology_factor,use_complexity_factor,use_post_processing_fee,"
            + "use_quantity_discount,use_minimum_price,is_active";

    private CustomPrintPricingDao() { }

    public static Optional<ShopPricingConfig> findActive(long shopId, String technology, String material) {
        try (Connection conn = Database.getConnection()) {
            return findActive(conn, shopId, technology, material);
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static Optional<ShopPricingConfig> findActive(Connection conn, long shopId,
                                                         String technology, String material) throws SQLException {
        String sql = "SELECT " + PROFILE_COLUMNS + " FROM shop_print_price_profiles"
                + " WHERE shop_id=? AND printing_method=? AND material=? AND is_active=1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shopId); ps.setString(2, normalize(technology)); ps.setString(3, normalize(material));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                ShopPrintPriceProfile profile = mapProfile(rs);
                return Optional.of(new ShopPricingConfig(profile, options(conn, profile.profileId()), discounts(conn, profile.profileId())));
            }
        }
    }

    public static List<Map<String, Object>> listForWorkshop(long ownerUserId, long shopId) {
        List<Map<String, Object>> profiles = new ArrayList<>();
        String sql = "SELECT p.* FROM shop_print_price_profiles p JOIN print_shops s ON s.shop_id=p.shop_id"
                + " WHERE p.shop_id=? AND s.owner_user_id=? ORDER BY p.printing_method,p.material";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, shopId); ps.setLong(2, ownerUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String field : List.of("profile_id", "printing_method", "material", "material_price_per_gram",
                            "material_density_g_cm3", "material_waste_factor", "machine_cost_per_hour",
                            "deposition_rate_cm3_per_hour", "wall_thickness_mm", "support_material_factor",
                            "infill_usage_factor", "reference_layer_height_mm", "technology_factor", "complexity_factor",
                            "setup_hours", "post_processing_fee", "minimum_order_price", "minimum_layer_height_mm",
                            "maximum_layer_height_mm", "minimum_infill_percent", "maximum_infill_percent",
                            "production_days", "use_material_cost", "use_machine_cost", "use_infill_factor",
                            "use_layer_height_factor", "use_technology_factor", "use_complexity_factor",
                            "use_post_processing_fee", "use_quantity_discount", "use_minimum_price", "is_active")) row.put(field, rs.getObject(field));
                    row.put("options", optionMaps(conn, rs.getLong("profile_id")));
                    row.put("discounts", discountMaps(conn, rs.getLong("profile_id")));
                    profiles.add(row);
                }
            }
            return profiles;
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void saveProfile(long ownerUserId, long shopId, Map<String, String> input) {
        String technology = enumValue(input.get("technology"), "FDM", "SLA", "SLS", "FDM");
        String material = enumValue(input.get("material"), "PLA", "ABS", "PETG", "TPU", "RESIN", "RESIN-STD",
                "RESIN-CLEAR", "RESIN-FLEX", "NYLON", "NYLON-PA12", "NYLON-GF", "PLA");
        BigDecimal materialPrice = decimal(input, "materialPricePerGram", true, new BigDecimal("0.0001"), new BigDecimal("1000000"));
        BigDecimal density = decimal(input, "materialDensity", true, new BigDecimal("0.01"), new BigDecimal("100"));
        BigDecimal waste = decimal(input, "materialWasteFactor", false, BigDecimal.ZERO, new BigDecimal("5"));
        BigDecimal machine = decimal(input, "machineCostPerHour", false, BigDecimal.ZERO, new BigDecimal("100000000"));
        BigDecimal rate = decimal(input, "depositionRate", true, new BigDecimal("0.001"), new BigDecimal("100000"));
        BigDecimal wall = decimal(input, "wallThickness", true, new BigDecimal("0.1"), new BigDecimal("50"));
        BigDecimal support = decimal(input, "supportFactor", false, BigDecimal.ZERO, new BigDecimal("5"));
        BigDecimal infillUse = decimal(input, "infillUsageFactor", true, new BigDecimal("0.01"), new BigDecimal("10"));
        BigDecimal referenceLayer = decimal(input, "referenceLayerHeight", true, new BigDecimal("0.01"), new BigDecimal("10"));
        BigDecimal technologyFactor = decimal(input, "technologyFactor", true, new BigDecimal("0.01"), new BigDecimal("100"));
        BigDecimal complexity = decimal(input, "complexityFactor", true, new BigDecimal("0.01"), new BigDecimal("100"));
        BigDecimal setup = decimal(input, "setupHours", false, BigDecimal.ZERO, new BigDecimal("1000"));
        BigDecimal processing = decimal(input, "postProcessingFee", false, BigDecimal.ZERO, new BigDecimal("100000000"));
        BigDecimal minimum = decimal(input, "minimumOrderPrice", false, BigDecimal.ZERO, new BigDecimal("100000000"));
        BigDecimal minLayer = decimal(input, "minimumLayerHeight", true, new BigDecimal("0.01"), new BigDecimal("10"));
        BigDecimal maxLayer = decimal(input, "maximumLayerHeight", true, new BigDecimal("0.01"), new BigDecimal("10"));
        int minInfill = integer(input, "minimumInfill", 0, 100, 0);
        int maxInfill = integer(input, "maximumInfill", 0, 100, 100);
        int days = integer(input, "productionDays", 1, 365, 3);
        if (minLayer.compareTo(maxLayer) > 0 || minInfill > maxInfill) throw new IllegalArgumentException("Phạm vi thông số không hợp lệ.");
        boolean active = Boolean.parseBoolean(input.getOrDefault("active", "true"));
        boolean[] components={enabled(input,"useMaterialCost"),enabled(input,"useMachineCost"),enabled(input,"useInfillFactor"),
                enabled(input,"useLayerHeightFactor"),enabled(input,"useTechnologyFactor"),enabled(input,"useComplexityFactor"),
                enabled(input,"usePostProcessingFee"),enabled(input,"useQuantityDiscount"),enabled(input,"useMinimumPrice")};
        String sql = "INSERT INTO shop_print_price_profiles(shop_id,printing_method,material,material_price_per_gram,"
                + "material_density_g_cm3,material_waste_factor,machine_cost_per_hour,deposition_rate_cm3_per_hour,"
                + "wall_thickness_mm,support_material_factor,infill_usage_factor,reference_layer_height_mm,technology_factor,"
                + "complexity_factor,setup_hours,post_processing_fee,minimum_order_price,minimum_layer_height_mm,"
                + "maximum_layer_height_mm,minimum_infill_percent,maximum_infill_percent,production_days,use_material_cost,"
                + "use_machine_cost,use_infill_factor,use_layer_height_factor,use_technology_factor,use_complexity_factor,"
                + "use_post_processing_fee,use_quantity_discount,use_minimum_price,is_active)"
                + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE"
                + " material_price_per_gram=VALUES(material_price_per_gram),material_density_g_cm3=VALUES(material_density_g_cm3),"
                + " material_waste_factor=VALUES(material_waste_factor),machine_cost_per_hour=VALUES(machine_cost_per_hour),"
                + " deposition_rate_cm3_per_hour=VALUES(deposition_rate_cm3_per_hour),wall_thickness_mm=VALUES(wall_thickness_mm),"
                + " support_material_factor=VALUES(support_material_factor),infill_usage_factor=VALUES(infill_usage_factor),"
                + " reference_layer_height_mm=VALUES(reference_layer_height_mm),technology_factor=VALUES(technology_factor),"
                + " complexity_factor=VALUES(complexity_factor),setup_hours=VALUES(setup_hours),post_processing_fee=VALUES(post_processing_fee),"
                + " minimum_order_price=VALUES(minimum_order_price),minimum_layer_height_mm=VALUES(minimum_layer_height_mm),"
                + " maximum_layer_height_mm=VALUES(maximum_layer_height_mm),minimum_infill_percent=VALUES(minimum_infill_percent),"
                + " maximum_infill_percent=VALUES(maximum_infill_percent),production_days=VALUES(production_days),"
                + " use_material_cost=VALUES(use_material_cost),use_machine_cost=VALUES(use_machine_cost),use_infill_factor=VALUES(use_infill_factor),"
                + " use_layer_height_factor=VALUES(use_layer_height_factor),use_technology_factor=VALUES(use_technology_factor),"
                + " use_complexity_factor=VALUES(use_complexity_factor),use_post_processing_fee=VALUES(use_post_processing_fee),"
                + " use_quantity_discount=VALUES(use_quantity_discount),use_minimum_price=VALUES(use_minimum_price),is_active=VALUES(is_active),updated_at=NOW()";
        try (Connection conn = Database.getConnection()) {
            requireOwnedShop(conn, ownerUserId, shopId);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int i = 1;
                ps.setLong(i++, shopId); ps.setString(i++, technology); ps.setString(i++, material);
                for (BigDecimal value : List.of(materialPrice, density, waste, machine, rate, wall, support, infillUse,
                        referenceLayer, technologyFactor, complexity, setup, processing, minimum, minLayer, maxLayer)) ps.setBigDecimal(i++, value);
                ps.setInt(i++, minInfill); ps.setInt(i++, maxInfill); ps.setInt(i++, days);
                for(boolean enabled:components)ps.setBoolean(i++,enabled);ps.setBoolean(i, active);
                ps.executeUpdate();
            }
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void saveOption(long ownerUserId, long shopId, long profileId, String type,
                                  String value, String label, BigDecimal fee, boolean active) {
        type = enumValue(type, "COLOR", "FINISH", "COLOR");
        value = required(value, 100); label = required(label, 120);
        if (fee == null || fee.signum() < 0 || fee.compareTo(new BigDecimal("100000000")) > 0) throw new IllegalArgumentException("Phí tùy chọn không hợp lệ.");
        String sql = "INSERT INTO shop_print_price_options(profile_id,option_type,option_value,label,fee,is_active)"
                + " SELECT p.profile_id,?,?,?,?,? FROM shop_print_price_profiles p JOIN print_shops s ON s.shop_id=p.shop_id"
                + " WHERE p.profile_id=? AND p.shop_id=? AND s.owner_user_id=? ON DUPLICATE KEY UPDATE"
                + " label=VALUES(label),fee=VALUES(fee),is_active=VALUES(is_active)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type); ps.setString(2, value); ps.setString(3, label); ps.setBigDecimal(4, fee); ps.setBoolean(5, active);
            ps.setLong(6, profileId); ps.setLong(7, shopId); ps.setLong(8, ownerUserId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Bảng giá không thuộc xưởng này.");
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static void saveDiscount(long ownerUserId, long shopId, long profileId, int minimumQuantity, BigDecimal rate) {
        if (minimumQuantity < 2 || minimumQuantity > 100_000 || rate == null || rate.signum() < 0 || rate.compareTo(new BigDecimal("0.90")) > 0)
            throw new IllegalArgumentException("Mức chiết khấu không hợp lệ.");
        String sql = "INSERT INTO shop_print_quantity_discount_tiers(profile_id,minimum_quantity,discount_rate)"
                + " SELECT p.profile_id,?,? FROM shop_print_price_profiles p JOIN print_shops s ON s.shop_id=p.shop_id"
                + " WHERE p.profile_id=? AND p.shop_id=? AND s.owner_user_id=? ON DUPLICATE KEY UPDATE discount_rate=VALUES(discount_rate)";
        try (Connection conn = Database.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minimumQuantity); ps.setBigDecimal(2, rate); ps.setLong(3, profileId); ps.setLong(4, shopId); ps.setLong(5, ownerUserId);
            if (ps.executeUpdate() != 1) throw new IllegalArgumentException("Bảng giá không thuộc xưởng này.");
        } catch (SQLException e) { throw DataAccessException.of(e); }
    }

    public static List<Map<String,Object>> recommend(long userId, CustomPrintRequest request, String technology,
                                                      String material, int infill, BigDecimal layer,
                                                      int quantity, String color, String finish, boolean dimensionsConfirmed,
                                                      Double customerLatitude,Double customerLongitude,String search) {
        String sql="SELECT s.shop_id,s.shop_name,s.slug,s.logo_url,s.province,s.district,s.average_rating,s.estimated_lead_days,s.maximum_custom_print_quantity,"
                +"s.capabilities,s.max_build_dimensions,s.accepted_file_formats,s.latitude shop_latitude,s.longitude shop_longitude,"
                +"(SELECT COUNT(*) FROM reviews rv WHERE rv.shop_id=s.shop_id AND rv.is_published=1) review_count,p.* FROM print_shops s"
                +" LEFT JOIN shop_print_price_profiles p ON p.shop_id=s.shop_id AND p.printing_method=? AND p.material=? AND p.is_active=1"
                +" WHERE s.verification_status='VERIFIED' AND (s.status IS NULL OR s.status='ACTIVE')";
        List<Map<String,Object>> rows=new ArrayList<>();
        String ext=extension(request.originalFilename());
        if (!"obj".equals(ext)) return rows;
        try(Connection conn=Database.getConnection();PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setString(1,normalize(technology));ps.setString(2,normalize(material));
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    long shopId=rs.getLong("shop_id");String name=rs.getString("shop_name");
                    int maximumQuantity=rs.getInt("maximum_custom_print_quantity");
                    if(quantity>maximumQuantity)continue;
                    String location=join(rs.getString("district"),rs.getString("province"));
                    if(search!=null&&!search.isBlank()&&!(contains(name,search)||contains(location,search)||contains(rs.getString("capabilities"),search)))continue;
                    if(!supportsFormat(rs.getString("accepted_file_formats"),ext))continue;
                    double[] maxDims=parseDimensions(rs.getString("max_build_dimensions"));
                    boolean hasDims=request.widthMm()!=null&&request.depthMm()!=null&&request.heightMm()!=null;
                    if(hasDims&&maxDims!=null&&!fits(request.widthMm().doubleValue(),request.depthMm().doubleValue(),request.heightMm().doubleValue(),maxDims))continue;
                    if((!hasDims||maxDims==null)&&"DIRECT".equalsIgnoreCase(request.workflowType()))continue;
                    PrintPriceEstimate estimate=null;
                    Object rawProfileId=rs.getObject("profile_id");Long profileId=rawProfileId==null?null:((Number)rawProfileId).longValue();ShopPrintPriceProfile profile=null;ShopPricingConfig config=null;
                    if(profileId!=null){
                        profile=mapProfile(rs);config=new ShopPricingConfig(profile,options(conn,profile.profileId()),discounts(conn,profile.profileId()));
                        if((color!=null&&!color.isBlank()&&config.options().stream().noneMatch(o->"COLOR".equals(o.type())&&o.active()&&o.value().equals(color)))
                                ||(finish!=null&&!finish.isBlank()&&config.options().stream().noneMatch(o->"FINISH".equals(o.type())&&o.active()&&o.value().equals(finish))))continue;
                        try{
                            if(maxDims==null)throw new IllegalArgumentException("Workshop build volume is not configured.");
                            ModelAnalysis.Status status=ModelAnalysis.Status.valueOf(request.analysisStatus());
                            ModelAnalysis model=new ModelAnalysis(status,ext,request.sourceUnit(),num(request.widthMm()),num(request.depthMm()),
                                    num(request.heightMm()),num(request.volumeCm3()),num(request.surfaceAreaCm2()),
                                    request.triangleCount()==null?0:request.triangleCount(),request.analysisMessage());
                            List<PrintPriceOption> selected=config.options().stream().filter(o->
                                    ("COLOR".equals(o.type())&&o.value().equals(color))||("FINISH".equals(o.type())&&o.value().equals(finish))).toList();
                            estimate=new PrintPriceEstimator().estimate(model,profile,infill,layer,quantity,selected,config.discounts(),dimensionsConfirmed);
                        }catch(IllegalArgumentException ignored){ /* Quotes remain available when estimation cannot be trusted. */ }
                    }else if(!hasCapability(rs.getString("capabilities"),technology)||!hasCapability(rs.getString("capabilities"),material)){
                        continue;
                    }
                    Map<String,Object> row=new LinkedHashMap<>();row.put("shopId",shopId);row.put("shopName",name);row.put("slug",rs.getString("slug"));
                    row.put("location",location);row.put("logoUrl",rs.getString("logo_url"));row.put("rating",rs.getBigDecimal("average_rating"));
                    row.put("reviewCount",rs.getLong("review_count"));row.put("estimatedLeadDays",rs.getInt("estimated_lead_days"));
                    row.put("maximumQuantity",maximumQuantity);
                    row.put("capabilities",rs.getString("capabilities"));row.put("maxBuildDimensions",rs.getString("max_build_dimensions"));
                    BigDecimal storedLatitude=rs.getBigDecimal("shop_latitude"),storedLongitude=rs.getBigDecimal("shop_longitude");
                    Double shopLatitude=storedLatitude==null?null:storedLatitude.doubleValue(),shopLongitude=storedLongitude==null?null:storedLongitude.doubleValue();
                    row.put("distanceKm",customerLatitude==null||customerLongitude==null||shopLatitude==null||shopLongitude==null?null:distanceKm(customerLatitude,customerLongitude,shopLatitude,shopLongitude));
                    row.put("estimatedTotal",estimate==null?null:estimate.totalEstimate());row.put("productionDays",estimate==null?(profile==null?(rs.getInt("estimated_lead_days")>0?rs.getInt("estimated_lead_days"):3):profile.productionDays()):estimate.productionDays());
                    row.put("estimateDisclaimer",estimate==null?"Cần xưởng kiểm tra mô hình trước khi báo giá.":estimate.disclaimer());
                    row.put("options",profile==null?List.of():optionMaps(conn,profile.profileId()));row.put("matchScore",estimate==null?70:100);
                    rows.add(row);
                }
            }
            rows.sort(java.util.Comparator
                    .comparingInt((Map<String,Object> row)->((Number)row.get("matchScore")).intValue()).reversed()
                    .thenComparing((Map<String,Object> row)->(BigDecimal)row.get("estimatedTotal"),java.util.Comparator.nullsLast(BigDecimal::compareTo))
                    .thenComparingInt(row->((Number)row.get("productionDays")).intValue())
                    .thenComparing((Map<String,Object> row)->(Double)row.get("distanceKm"),java.util.Comparator.nullsLast(Double::compareTo))
                    .thenComparing((Map<String,Object> row)->(BigDecimal)row.get("rating"),java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())));
            return rows;
        }catch(SQLException e){throw DataAccessException.of(e);}
    }

    private static List<PrintPriceOption> options(Connection conn, long profileId) throws SQLException {
        List<PrintPriceOption> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT option_id,profile_id,option_type,option_value,label,fee,is_active"
                + " FROM shop_print_price_options WHERE profile_id=? AND is_active=1")) {
            ps.setLong(1, profileId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(new PrintPriceOption(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBigDecimal(6), rs.getBoolean(7))); }
        }
        return rows;
    }
    private static List<PrintQuantityDiscount> discounts(Connection conn, long profileId) throws SQLException {
        List<PrintQuantityDiscount> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT minimum_quantity,discount_rate FROM shop_print_quantity_discount_tiers WHERE profile_id=?")) {
            ps.setLong(1, profileId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) rows.add(new PrintQuantityDiscount(rs.getInt(1), rs.getBigDecimal(2))); }
        }
        return rows;
    }
    private static List<Map<String,Object>> optionMaps(Connection c,long id)throws SQLException {
        List<Map<String,Object>> rows=new ArrayList<>(); try(PreparedStatement p=c.prepareStatement("SELECT option_id,option_type,option_value,label,fee,is_active FROM shop_print_price_options WHERE profile_id=? ORDER BY option_type,label")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getLong(1));m.put("type",r.getString(2));m.put("value",r.getString(3));m.put("label",r.getString(4));m.put("fee",r.getBigDecimal(5));m.put("active",r.getBoolean(6));rows.add(m);}}}return rows;
    }
    private static List<Map<String,Object>> discountMaps(Connection c,long id)throws SQLException {
        List<Map<String,Object>> rows=new ArrayList<>(); try(PreparedStatement p=c.prepareStatement("SELECT minimum_quantity,discount_rate FROM shop_print_quantity_discount_tiers WHERE profile_id=? ORDER BY minimum_quantity")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){while(r.next()){Map<String,Object> m=new LinkedHashMap<>();m.put("minimumQuantity",r.getInt(1));m.put("discountRate",r.getBigDecimal(2));rows.add(m);}}}return rows;
    }
    private static ShopPrintPriceProfile mapProfile(ResultSet r) throws SQLException {
        return new ShopPrintPriceProfile(r.getLong("profile_id"),r.getLong("shop_id"),r.getString("printing_method"),r.getString("material"),
                r.getBigDecimal("material_price_per_gram"),r.getBigDecimal("material_density_g_cm3"),r.getBigDecimal("material_waste_factor"),
                r.getBigDecimal("machine_cost_per_hour"),r.getBigDecimal("deposition_rate_cm3_per_hour"),r.getBigDecimal("wall_thickness_mm"),
                r.getBigDecimal("support_material_factor"),r.getBigDecimal("infill_usage_factor"),r.getBigDecimal("reference_layer_height_mm"),
                r.getBigDecimal("technology_factor"),r.getBigDecimal("complexity_factor"),r.getBigDecimal("setup_hours"),
                r.getBigDecimal("post_processing_fee"),r.getBigDecimal("minimum_order_price"),r.getBigDecimal("minimum_layer_height_mm"),
                r.getBigDecimal("maximum_layer_height_mm"),r.getInt("minimum_infill_percent"),r.getInt("maximum_infill_percent"),
                r.getInt("production_days"),r.getBoolean("use_material_cost"),r.getBoolean("use_machine_cost"),
                r.getBoolean("use_infill_factor"),r.getBoolean("use_layer_height_factor"),r.getBoolean("use_technology_factor"),
                r.getBoolean("use_complexity_factor"),r.getBoolean("use_post_processing_fee"),r.getBoolean("use_quantity_discount"),
                r.getBoolean("use_minimum_price"),r.getBoolean("is_active"));
    }
    private static void requireOwnedShop(Connection c,long owner,long shop)throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT 1 FROM print_shops WHERE shop_id=? AND owner_user_id=? FOR UPDATE")){p.setLong(1,shop);p.setLong(2,owner);try(ResultSet r=p.executeQuery()){if(!r.next())throw new IllegalArgumentException("Xưởng không thuộc tài khoản này.");}}
    }
    private static String extension(String filename){if(filename==null)return "";int dot=filename.lastIndexOf('.');return dot<0?"":filename.substring(dot+1).toLowerCase(java.util.Locale.ROOT);}
    private static boolean hasCapability(String capabilities,String requested){
        if(capabilities==null||requested==null)return false;
        String normalized=requested.trim().toLowerCase(java.util.Locale.ROOT);
        for(String token:capabilities.toLowerCase(java.util.Locale.ROOT).split("[,;|/\\s]+")){
            String clean=token.replaceAll("^[^a-z0-9]+|[^a-z0-9]+$","");
            if(normalized.equals(clean))return true;
        }
        return false;
    }
    private static boolean supportsFormat(String formats,String ext){if(formats==null||formats.isBlank())return false;for(String value:formats.toLowerCase(java.util.Locale.ROOT).split("[,;\\s]+")){String clean=value.replace(".","").trim();if(clean.equals(ext))return true;}return false;}
    private static double[] parseDimensions(String text){
        if(text==null||text.isBlank())return null;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?<![A-Za-z0-9.])[-+]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)").matcher(text);
        double[] dimensions=new double[3];int count=0;
        while(m.find()){
            if(count==3)return null;
            try{dimensions[count]=Double.parseDouble(m.group());}catch(NumberFormatException e){return null;}
            if(!Double.isFinite(dimensions[count])||dimensions[count]<=0)return null;
            count++;
        }
        if(count!=3)return null;
        String lower=text.toLowerCase(java.util.Locale.ROOT);
        boolean centimeters=java.util.regex.Pattern.compile("(?i)(?<![a-z])cm(?![a-z])").matcher(lower).find();
        boolean inches=java.util.regex.Pattern.compile("(?i)(?<![a-z])(?:in|inch|inches)(?![a-z])").matcher(lower).find();
        if(centimeters&&inches)return null;
        double scale=inches?25.4:centimeters?10.0:1.0;
        for(int i=0;i<dimensions.length;i++){dimensions[i]*=scale;if(!Double.isFinite(dimensions[i])||dimensions[i]>100_000)return null;}
        return dimensions;
    }
    private static boolean fits(double x,double y,double z,double[] capacity){if(capacity==null)return false;double[] item={x,y,z};java.util.Arrays.sort(item);java.util.Arrays.sort(capacity);return item[0]<=capacity[0]&&item[1]<=capacity[1]&&item[2]<=capacity[2];}
    private static boolean contains(String value,String q){return value!=null&&value.toLowerCase(java.util.Locale.ROOT).contains(q.trim().toLowerCase(java.util.Locale.ROOT));}
    private static double distanceKm(double lat1,double lon1,double lat2,double lon2){double r=6371.0088;double dLat=Math.toRadians(lat2-lat1),dLon=Math.toRadians(lon2-lon1);double a=Math.sin(dLat/2)*Math.sin(dLat/2)+Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))*Math.sin(dLon/2)*Math.sin(dLon/2);return Math.round(r*2*Math.atan2(Math.sqrt(a),Math.sqrt(1-a))*10.0)/10.0;}
    private static String join(String a,String b){return (a==null?"":a)+(a!=null&&b!=null?", ":"")+(b==null?"":b);}
    private static double num(BigDecimal value){return value==null?0:value.doubleValue();}
    private static String normalize(String s){return s==null?"":s.trim().toUpperCase(java.util.Locale.ROOT);}
    private static String enumValue(String s,String... allowed){String v=normalize(s);for(int i=0;i<allowed.length;i++)if(v.equals(allowed[i]))return v;throw new IllegalArgumentException("Giá trị cấu hình không hợp lệ.");}
    private static String required(String s,int max){if(s==null||s.isBlank()||s.trim().length()>max)throw new IllegalArgumentException("Thiếu hoặc quá dài tên tùy chọn.");return s.trim();}
    private static BigDecimal decimal(Map<String,String> in,String key,boolean required,BigDecimal min,BigDecimal max){String raw=in.get(key);if((raw==null||raw.isBlank())&&!required)return BigDecimal.ZERO;if(raw==null||raw.isBlank())throw new IllegalArgumentException("Thiếu giá trị: "+key);try{BigDecimal v=new BigDecimal(raw.trim());if(v.compareTo(min)<0||v.compareTo(max)>0)throw new IllegalArgumentException("Giá trị ngoài phạm vi: "+key);return v;}catch(NumberFormatException e){throw new IllegalArgumentException("Giá trị không hợp lệ: "+key);}}
    private static int integer(Map<String,String> in,String key,int min,int max,int fallback){String s=in.get(key);if(s==null||s.isBlank())return fallback;try{int v=Integer.parseInt(s);if(v<min||v>max)throw new IllegalArgumentException("Giá trị ngoài phạm vi: "+key);return v;}catch(NumberFormatException e){throw new IllegalArgumentException("Giá trị không hợp lệ: "+key);}}
    private static boolean enabled(Map<String,String> in,String key){if(!in.containsKey(key))return true;String value=in.get(key);return "true".equalsIgnoreCase(value)||"on".equalsIgnoreCase(value);}
}
