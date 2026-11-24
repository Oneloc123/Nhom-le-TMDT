package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.OrderDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ProductDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Order;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Product;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ShopOffer;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Json;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only business context for the hybrid chatbot.
 * All customer-specific queries are scoped to the authenticated user.
 */
final class ChatbotDataService {
    private static final Pattern ORDER_CODE = Pattern.compile("(?i)\\bPK-\\d{8}-\\d{6}\\b");
    private static final Set<String> STOP_WORDS = Set.of(
            "toi", "minh", "ban", "cho", "hoi", "tim", "kiem", "mua", "co", "can",
            "muon", "xem", "san", "pham", "gia", "bao", "nhieu", "loai", "nao",
            "giup", "tu", "van", "ve", "la", "mot", "nhung", "voi", "cua", "khong",
            "hay", "shop", "printkraft", "xin", "chao", "nhe", "duoc", "khong"
    );
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private ChatbotDataService() { }

    static Context build(String question, Long userId) {
        String normalized = ChatbotService.normalize(question);
        Map<String, Object> facts = new LinkedHashMap<>();
        List<ChatbotService.Action> actions = new ArrayList<>();
        String intent = "faq";

        ChatbotService.Answer faq = ChatbotService.answerFaq(question);
        if (!faq.fallback()) {
            facts.put("faqGuidance", faq.text());
            intent = faq.intent();
            actions.addAll(faq.actions());
        }

        if (isProductQuestion(normalized)) {
            intent = "product";
            String query = productQuery(normalized);
            List<Map<String, Object>> products = new ArrayList<>();
            if (!query.isBlank()) {
                for (Product product : ProductDao.search(
                        new ProductDao.SearchCriteria(query, null, null, null, null, "rating"), 1, 5)) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", product.getName());
                    item.put("category", product.getCategoryName());
                    item.put("description", safe(product.getDescription(), 350));
                    item.put("rating", product.getRating());
                    item.put("reviewCount", product.getReviewCount());
                    List<Map<String, Object>> offers = new ArrayList<>();
                    for (ShopOffer offer : product.getOffers()) {
                        Map<String, Object> offerData = new LinkedHashMap<>();
                        offerData.put("shop", offer.getShopName());
                        offerData.put("price", offer.getUnitPrice());
                        offerData.put("material", offer.getMaterial());
                        offerData.put("printingMethod", offer.getPrintingMethod());
                        offerData.put("minimumQuantity", offer.getMinimumQuantity());
                        offerData.put("productionDays", offer.getProductionDays());
                        offerData.put("stockStatus", offer.getStockStatus());
                        offers.add(offerData);
                    }
                    item.put("offers", offers);
                    products.add(item);
                }
            }
            facts.put("matchingProducts", products);
            actions.add(new ChatbotService.Action("Xem tất cả sản phẩm", "/products"));
        }

        if (isOrderQuestion(normalized)) {
            intent = "order";
            if (userId == null) {
                facts.put("orderLookup", "Khách chưa đăng nhập. Yêu cầu đăng nhập để xem đơn hàng cá nhân.");
                actions.add(new ChatbotService.Action("Đăng nhập", "/login"));
            } else {
                Matcher matcher = ORDER_CODE.matcher(question == null ? "" : question);
                if (matcher.find()) {
                    Order order = OrderDao.findByCodeForUser(matcher.group().toUpperCase(), userId).orElse(null);
                    if (order == null) {
                        facts.put("orderLookup", "Không tìm thấy mã đơn thuộc tài khoản hiện tại.");
                    } else {
                        facts.put("orders", List.of(orderFacts(order)));
                        actions.add(new ChatbotService.Action("Theo dõi đơn hàng", "/order-tracking?code=" + order.getOrderCode()));
                    }
                } else {
                    List<Map<String, Object>> orders = new ArrayList<>();
                    for (Order order : OrderDao.listByUser(userId, 1, 5)) {
                        orders.add(orderFacts(order));
                    }
                    facts.put("recentOrders", orders);
                    actions.add(new ChatbotService.Action("Mở lịch sử đơn hàng", "/purchase-history"));
                }
            }
        }

        if (isSupportQuestion(normalized)) {
            intent = "support";
            facts.put("supportInstruction",
                    "Chatbot chỉ hướng dẫn. Khách cần đăng nhập và tự gửi yêu cầu tại /support; không tự tạo ticket, không xác nhận hoàn tiền hoặc khiếu nại.");
            actions.add(new ChatbotService.Action("Tạo yêu cầu hỗ trợ", "/support"));
        }

        return new Context(intent, Json.stringify(facts), List.copyOf(actions));
    }

    private static Map<String, Object> orderFacts(Order order) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("orderCode", order.getOrderCode());
        item.put("status", order.getStatus());
        item.put("paymentStatus", order.getPaymentStatus());
        item.put("shippingStatus", order.getShippingStatus());
        item.put("total", order.getTotalAmount());
        item.put("placedAt", order.getPlacedAt() == null ? null : order.getPlacedAt().format(DATE_TIME));
        item.put("estimatedDeliveryAt", order.getEstimatedDeliveryAt() == null
                ? null : order.getEstimatedDeliveryAt().format(DATE_TIME));
        return item;
    }

    private static boolean isProductQuestion(String q) {
        return containsAny(q, "san pham", "tim kiem", "co ban", "mau nao", "chat lieu",
                "vat lieu", "nhua", "pla", "abs", "petg", "resin", "gia", "bao nhieu",
                "mua", "danh muc", "in 3d");
    }

    private static boolean isOrderQuestion(String q) {
        return containsAny(q, "don hang", "theo doi", "ma don", "giao hang", "van chuyen",
                "tinh trang don", "thanh toan", "da dat", "bao gio nhan");
    }

    private static boolean isSupportQuestion(String q) {
        return containsAny(q, "ho tro", "khieu nai", "doi tra", "hoan tien", "bao hanh",
                "hang loi", "gap nhan vien", "nhan vien", "loi don", "loi san pham");
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(term)) return true;
        return false;
    }

    private static String productQuery(String normalized) {
        StringBuilder result = new StringBuilder();
        for (String token : normalized.split("\\s+")) {
            if (token.length() > 1 && !STOP_WORDS.contains(token)) {
                if (!result.isEmpty()) result.append(' ');
                result.append(token);
            }
        }
        return result.toString();
    }

    private static String safe(String value, int max) {
        if (value == null) return "";
        String clean = value.strip();
        return clean.length() <= max ? clean : clean.substring(0, max);
    }

    record Context(String intent, String factsJson, List<ChatbotService.Action> actions) { }
}
