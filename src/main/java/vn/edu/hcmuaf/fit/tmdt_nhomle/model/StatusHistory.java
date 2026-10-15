package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.time.LocalDateTime;

/** Dòng lịch sử trạng thái đơn hàng (bảng order_status_history). */
public class StatusHistory {
    private long historyId;
    private long orderId;
    private Long changedByUserId;
    private String oldStatus;
    private String newStatus;
    private String note;
    private LocalDateTime changedAt;

    public long getHistoryId() { return historyId; }
    public void setHistoryId(long historyId) { this.historyId = historyId; }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public Long getChangedByUserId() { return changedByUserId; }
    public void setChangedByUserId(Long changedByUserId) { this.changedByUserId = changedByUserId; }

    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }

    public String getChangedAtText() {
        return changedAt == null ? "" : changedAt.format(java.time.format.DateTimeFormatter
                .ofPattern("dd/MM/yyyy HH:mm"));
    }
}
