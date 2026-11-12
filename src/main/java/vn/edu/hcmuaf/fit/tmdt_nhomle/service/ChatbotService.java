package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Centralized FAQ/intent handler for the customer chatbot.
 * Answers here are deterministic and must not be presented as live order data.
 */
public final class ChatbotService {
    private ChatbotService() {
    }

    public static Answer answer(String question) {
        String text = normalize(question);

        if (text.isBlank()) {
            return reply("fallback", "Bạn hãy nhập câu hỏi để mình hỗ trợ nhé.", true,
                    "Mở trang hỗ trợ", "/support");
        }

        if (text.matches("^(chao|xin chao|hello|hi|hey|alo)(\\b|$)")) {
            return reply("greeting",
                    "Xin chào! Mình là trợ lý tự động của PrintKraft. Bạn cần tìm sản phẩm, theo dõi đơn hàng, hỏi về báo giá hay cần hỗ trợ vấn đề nào?",
                    false, "Xem sản phẩm", "/products", "Theo dõi đơn", "/order-tracking");
        }

        if (matches(text, "nhan vien", "nguoi that", "gap ho tro", "ho tro vien",
                "tu van vien", "gap tu van", "lien he nhan vien", "ket noi nhan vien")) {
            return reply("human_support",
                    "Mình có thể hướng dẫn các bước cơ bản. Để nhân viên tiếp nhận và phản hồi trong tài khoản, hãy đăng nhập rồi tạo yêu cầu tại trang Hỗ trợ. Ghi rõ vấn đề và mã đơn nếu có để đội ngũ kiểm tra nhanh hơn.",
                    false, "Mở trang hỗ trợ", "/support", "Đăng nhập", "/login");
        }

        if (matches(text, "theo doi", "van chuyen", "giao hang", "don hang o dau",
                "kiem tra don", "ma van don", "ship", "thoi gian giao", "tinh trang don")) {
            return reply("order_tracking",
                    "Bạn mở trang Theo dõi đơn hàng và nhập mã đơn để xem trạng thái cập nhật. Nếu đơn chậm hơn dự kiến, hãy chuẩn bị mã đơn khi gửi yêu cầu hỗ trợ. Trợ lý chưa tự đọc trạng thái đơn trong cuộc trò chuyện này.",
                    false, "Theo dõi đơn hàng", "/order-tracking", "Hỗ trợ giao hàng", "/support");
        }

        if (matches(text, "bao gia", "gia si", "gia doanh nghiep", "don hang so luong",
                "bao gia in", "bao nhieu tien", "chi phi in")) {
            return reply("quote",
                    "Với đơn hàng số lượng lớn hoặc yêu cầu riêng, bạn có thể tạo yêu cầu báo giá để trao đổi về sản phẩm, số lượng, vật liệu và thời gian sản xuất. Báo giá cụ thể cần được xác nhận trong hệ thống.",
                    false, "Tạo yêu cầu báo giá", "/quotes", "Xem sản phẩm", "/products");
        }

        if (matches(text, "doi tra", "hoan tien", "tra hang", "hang loi",
                "san pham loi", "khieu nai", "bao hanh", "nhan sai hang")) {
            return reply("return_refund",
                    "Để yêu cầu đổi trả, hoàn tiền hoặc báo sản phẩm lỗi, hãy đăng nhập rồi tạo ticket trong mục Hỗ trợ. Mô tả tình trạng, mã đơn và đính kèm hình ảnh liên quan nếu có để nhân viên kiểm tra. Chatbot không thể tự xác nhận hoặc thực hiện hoàn tiền.",
                    false, "Đến trang hỗ trợ", "/support", "Đơn hàng của tôi", "/purchase-history");
        }

        if (matches(text, "thanh toan", "vnpay", "the ngan hang",
                "loi thanh toan", "khong thanh toan", "da bi tru tien", "giao dich")) {
            return reply("payment",
                    "Khi thanh toán, hãy kiểm tra lại thông tin đơn và trạng thái giao dịch. Nếu đã bị trừ tiền nhưng đơn chưa cập nhật, đừng thanh toán lặp lại ngay; hãy lưu mã giao dịch và liên hệ hỗ trợ để nhân viên xác minh.",
                    false, "Xem đơn hàng", "/purchase-history", "Hỗ trợ thanh toán", "/support");
        }

        if (matches(text, "dang nhap", "dang ky", "tai khoan", "quen mat khau",
                "doi mat khau", "otp", "xac minh email")) {
            return reply("account",
                    "Bạn có thể đăng nhập hoặc tạo tài khoản để quản lý địa chỉ, đơn hàng, yêu thích và yêu cầu báo giá. Nếu quên mật khẩu, dùng liên kết “Quên mật khẩu” trên trang đăng nhập.",
                    false, "Đăng nhập / đăng ký", "/login", "Đơn hàng", "/purchase-history");
        }

        if (matches(text, "thiet ke", "tuy chinh", "3d", "mo hinh",
                "file in", "upload file", "ban ve", "in theo yeu cau")) {
            return reply("custom_design",
                    "PrintKraft hỗ trợ luồng thiết kế/in 3D và sản phẩm tùy chỉnh. Hãy mở khu vực Thiết kế 3D, chuẩn bị file thiết kế cùng kích thước và yêu cầu mong muốn trước khi gửi.",
                    false, "Mở Thiết kế 3D", "/design", "Hỏi về báo giá", "/quotes");
        }

        if (matches(text, "gio hang", "them vao gio", "xoa san pham",
                "dat hang", "khong dat duoc", "sua so luong")) {
            return reply("cart_checkout",
                    "Bạn có thể xem lại số lượng và tạm tính trong giỏ hàng, sau đó cập nhật hoặc xóa sản phẩm trước khi đặt hàng. Nếu thanh toán gặp lỗi, hãy lưu thông báo lỗi và gửi yêu cầu hỗ trợ.",
                    false, "Mở giỏ hàng", "/cart", "Hỗ trợ đặt hàng", "/support");
        }

        if (matches(text, "san pham", "ao thun", "coc", "poster",
                "mua gi", "danh muc", "xem hang", "tim kiem")) {
            return reply("products",
                    "Bạn có thể xem danh mục sản phẩm, tìm theo tên hoặc lọc theo danh mục và khoảng giá. Mở trang Sản phẩm để xem các lựa chọn hiện có.",
                    false, "Khám phá sản phẩm", "/products");
        }

        return reply("fallback",
                "Mình chưa xác định chính xác câu hỏi này. Bạn thử diễn đạt ngắn gọn hơn hoặc chọn một chủ đề gợi ý; nếu cần xử lý riêng, hãy mở trang Hỗ trợ để gửi yêu cầu cho nhân viên.",
                true, "Trang hỗ trợ", "/support", "Câu hỏi về sản phẩm", "/products");
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean matches(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static Answer reply(String intent, String text, boolean fallback, String... labelAndPath) {
        List<Action> actions = new ArrayList<>();
        for (int i = 0; i + 1 < labelAndPath.length; i += 2) {
            actions.add(new Action(labelAndPath[i], labelAndPath[i + 1]));
        }
        return new Answer(intent, text, List.copyOf(actions), fallback);
    }

    public record Action(String label, String path) {
    }

    public record Answer(String intent, String text, List<Action> actions, boolean fallback) {
    }
}
