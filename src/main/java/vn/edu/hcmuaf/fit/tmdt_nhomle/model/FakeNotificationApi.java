package vn.edu.hcmuaf.fit.tmdt_nhomle.fake;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Notification;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fake API: lưu thông báo trong bộ nhớ, mỗi user có danh sách riêng.
 * Dữ liệu reset khi restart app (đủ để demo/chạy thử).
 */
public final class FakeNotificationApi {

    private static final FakeNotificationApi INSTANCE = new FakeNotificationApi();
    public static FakeNotificationApi getInstance() { return INSTANCE; }

    private final Map<Long, List<Notification>> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1000);

    private FakeNotificationApi() { /* singleton */ }

    private List<Notification> data(long userId) {
        return store.computeIfAbsent(userId, k -> seed());
    }

    private List<Notification> seed() {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> list = new ArrayList<>();

        list.add(new Notification(seq.incrementAndGet(),
                "Đơn hàng #PK20261101-001 đã được xác nhận",
                "Xưởng in đã tiếp nhận đơn hàng của bạn và đang chuẩn bị sản xuất.",
                "/purchase-history", now.minusHours(2), false));

        list.add(new Notification(seq.incrementAndGet(),
                "Yêu cầu in 3D đã có báo giá",
                "Yêu cầu thiết kế mẫu móc khóa 3D của bạn đã được báo giá. Vui lòng kiểm tra.",
                "/my-designs", now.minusHours(6), false));

        list.add(new Notification(seq.incrementAndGet(),
                "Đơn hàng #PK20261028-014 đang giao",
                "Đơn vị vận chuyển đã lấy hàng. Dự kiến giao trong 2–3 ngày tới.",
                "/purchase-history", now.minusDays(1), false));

        list.add(new Notification(seq.incrementAndGet(),
                "Tin nhắn mới từ xưởng in",
                "Xưởng đã phản hồi về yêu cầu in túi vải canvas của bạn.",
                "/messages", now.minusDays(1).minusHours(4), true));

        list.add(new Notification(seq.incrementAndGet(),
                "Sản phẩm yêu thích đang giảm giá",
                "Áo thun cotton in hình theo yêu cầu đang giảm 15% trong tuần này.",
                "/favorites", now.minusDays(2), true));

        list.add(new Notification(seq.incrementAndGet(),
                "Báo giá #Q-2026-088 đã sẵn sàng",
                "Báo giá cho 100 bộ poster A3 in offset đã có. Xem chi tiết và xác nhận.",
                "/quotes", now.minusDays(3), true));

        list.add(new Notification(seq.incrementAndGet(),
                "Chào mừng bạn đến với PrintKraft",
                "Cảm ơn bạn đã đăng ký. Khám phá ngay các dịch vụ in ấn và thiết kế 3D.",
                "/products", now.minusDays(7), true));

        list.add(new Notification(seq.incrementAndGet(),
                "Đơn hàng #PK20261015-003 đã giao thành công",
                "Cảm ơn bạn đã tin tưởng PrintKraft. Đừng quên đánh giá sản phẩm nhé!",
                "/purchase-history", now.minusDays(14), true));

        list.add(new Notification(seq.incrementAndGet(),
                "Cập nhật chính sách bảo mật",
                "Chúng tôi đã cập nhật chính sách bảo mật để phù hợp với quy định mới.",
                null, now.minusDays(20), true));

        return new CopyOnWriteArrayList<>(list);
    }

    /** Lấy danh sách đã phân trang, sắp xếp mới nhất trước. */
    public List<Notification> list(long userId, int page, int pageSize) {
        List<Notification> all = new ArrayList<>(data(userId));
        all.sort(Comparator.comparing(Notification::getCreatedAtRaw).reversed());

        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        int safePage = Math.min(Math.max(page, 1), totalPages);

        int from = (safePage - 1) * pageSize;
        int to = Math.min(total, from + pageSize);
        if (from >= total) return Collections.emptyList();
        return all.subList(from, to);
    }

    public int total(long userId) { return data(userId).size(); }

    public int unreadCount(long userId) {
        return (int) data(userId).stream().filter(n -> !n.isRead()).count();
    }

    public boolean markRead(long userId, long notificationId) {
        for (Notification n : data(userId)) {
            if (n.getId() == notificationId) {
                if (n.isRead()) return false;
                n.setRead(true);
                return true;
            }
        }
        return false;
    }

    public int markAllRead(long userId) {
        int changed = 0;
        for (Notification n : data(userId)) {
            if (!n.isRead()) { n.setRead(true); changed++; }
        }
        return changed;
    }
}