package vn.edu.hcmuaf.fit.tmdt_nhomle.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Notification {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final long id;
    private final String title;
    private final String message;
    private final String url;          // có thể null
    private final LocalDateTime createdAt;
    private volatile boolean read;

    public Notification(long id, String title, String message, String url,
                        LocalDateTime createdAt, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.url = url;
        this.createdAt = createdAt;
        this.read = read;
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getUrl() { return url; }
    public LocalDateTime getCreatedAtRaw() { return createdAt; }

    /** EL ${item.createdAt} sẽ gọi getCreatedAt() → trả chuỗi đã format. */
    public String getCreatedAt() {
        return createdAt == null ? "" : FMT.format(createdAt);
    }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}