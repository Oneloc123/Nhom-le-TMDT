package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReturnRefundDao;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

public final class ReturnRefundEvidenceStore {
    public static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    public static final int MAX_INITIAL_FILES = 4;
    public static final int MAX_TOTAL_FILES = 8;
    private static final long MAX_IMAGE_PIXELS = 20_000_000L;

    private ReturnRefundEvidenceStore() { }

    public record SavedEvidence(ReturnRefundDao.EvidenceUpload record, Path absolutePath) { }

    public static SavedEvidence save(Part part, ServletContext context) throws IOException {
        if (part == null || part.getSize() <= 0) throw new IllegalArgumentException("Tệp bằng chứng đang trống.");
        if (part.getSize() > MAX_FILE_BYTES) throw new IllegalArgumentException("Mỗi ảnh bằng chứng tối đa 5 MB.");
        String original = safeOriginalName(part.getSubmittedFileName());
        byte[] bytes;
        try (InputStream in = part.getInputStream()) {
            bytes = in.readNBytes((int) MAX_FILE_BYTES + 1);
        }
        if (bytes.length == 0 || bytes.length > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("Dung lượng ảnh phải lớn hơn 0 và không vượt quá 5 MB.");
        }
        String format = validateRasterImage(bytes);
        String extension = "PNG".equals(format) ? ".png" : ".jpg";
        String mimeType = "PNG".equals(format) ? "image/png" : "image/jpeg";
        String sha256 = sha256(bytes);
        LocalDate today = LocalDate.now();
        String key = "returns/" + today.getYear() + "/" + String.format(Locale.ROOT, "%02d", today.getMonthValue())
                + "/" + String.format(Locale.ROOT, "%02d", today.getDayOfMonth()) + "/" + UUID.randomUUID() + extension;
        Path destination = resolve(key, context);
        Files.createDirectories(destination.getParent());
        Files.write(destination, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        return new SavedEvidence(new ReturnRefundDao.EvidenceUpload(key, original, mimeType, bytes.length, sha256), destination);
    }

    public static Path resolve(String storageKey, ServletContext context) throws IOException {
        if (storageKey == null || !storageKey.matches("returns/[0-9]{4}/[0-9]{2}/[0-9]{2}/[a-f0-9-]{36}\\.(jpg|png)")) {
            throw new IllegalArgumentException("Khóa tệp bằng chứng không hợp lệ.");
        }
        Path root = configuredRoot();
        String deployed = context == null ? null : context.getRealPath("/");
        if (deployed != null && !deployed.isBlank()) {
            Path webRoot = Path.of(deployed).toAbsolutePath().normalize();
            if (root.startsWith(webRoot)) throw new IllegalStateException("PK_UPLOAD_DIR không được nằm trong thư mục triển khai web.");
        }
        Path file = root.resolve(storageKey).normalize();
        if (!file.startsWith(root) || !file.getParent().startsWith(root)) {
            throw new IllegalArgumentException("Đường dẫn tệp bằng chứng không hợp lệ.");
        }
        return file;
    }

    public static void delete(SavedEvidence evidence) {
        if (evidence == null) return;
        try { Files.deleteIfExists(evidence.absolutePath()); } catch (IOException ignored) { }
    }

    private static Path configuredRoot() {
        String configured = System.getenv("PK_UPLOAD_DIR");
        Path root = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), ".printkraft-uploads")
                : Path.of(configured.trim());
        return root.toAbsolutePath().normalize();
    }

    private static String validateRasterImage(byte[] bytes) throws IOException {
        try (ByteArrayInputStream input = new ByteArrayInputStream(bytes);
             ImageInputStream imageInput = ImageIO.createImageInputStream(input)) {
            if (imageInput == null) throw new IllegalArgumentException("Tệp không phải ảnh JPEG hoặc PNG hợp lệ.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new IllegalArgumentException("Chỉ nhận ảnh JPEG hoặc PNG.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName().toUpperCase(Locale.ROOT);
                if (!"JPEG".equals(format) && !"JPG".equals(format) && !"PNG".equals(format))
                    throw new IllegalArgumentException("Chỉ nhận ảnh JPEG hoặc PNG.");
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Kích thước ảnh vượt giới hạn an toàn (20 megapixel).");
                }
                if (reader.read(0) == null) throw new IllegalArgumentException("Không giải mã được ảnh bằng chứng.");
                return "PNG".equals(format) ? "PNG" : "JPEG";
            } finally { reader.dispose(); }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Ảnh bằng chứng không hợp lệ.", e);
        }
    }

    private static String safeOriginalName(String raw) {
        String value = raw == null ? "evidence" : raw.replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").trim();
        if (value.isBlank()) value = "evidence";
        return value.length() <= 180 ? value : value.substring(value.length() - 180);
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 không khả dụng.", e); }
    }
}
