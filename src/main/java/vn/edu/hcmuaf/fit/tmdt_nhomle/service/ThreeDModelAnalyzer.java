package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** Reads formats we can validate without a CAD/slicer dependency. Unsupported geometry is routed to a quote. */
public final class ThreeDModelAnalyzer {
    private static final long MAX_ANALYZED_TRIANGLES = 50_000;
    private static final int MAX_OBJ_VERTICES = 200_000;
    private static final long MAX_ARCHIVE_MODEL_BYTES = 100L * 1024 * 1024;
    private static final double EDGE_QUANTIZATION = 10_000.0;

    public ModelAnalysis analyze(Path file, String extension, String requestedUnit) throws IOException {
        if (file == null || !Files.isRegularFile(file) || Files.size(file) <= 0) {
            return result(ModelAnalysis.Status.INVALID, extension, requestedUnit, "Tệp mô hình rỗng hoặc không đọc được.");
        }
        String format = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
        String unit = normalizeUnit(requestedUnit);
        double scale = unitScale(unit);
        return switch (format) {
            case "stl" -> analyzeStl(file, unit, scale);
            case "obj" -> analyzeObj(file, unit, scale);
            case "3mf" -> validate3mf(file, unit);
            case "step", "stp" -> validateStep(file, format, unit);
            default -> result(ModelAnalysis.Status.INVALID, format, unit, "Định dạng mô hình không được hỗ trợ.");
        };
    }

    public static String normalizeUnit(String value) {
        if (value == null || value.isBlank()) return "mm";
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "mm", "millimeter", "millimeters" -> "mm";
            case "cm", "centimeter", "centimeters" -> "cm";
            case "in", "inch", "inches" -> "in";
            default -> throw new IllegalArgumentException("Đơn vị mô hình chỉ có thể là mm, cm hoặc inch.");
        };
    }

    private static double unitScale(String unit) {
        return switch (unit) {
            case "cm" -> 10.0;
            case "in" -> 25.4;
            default -> 1.0;
        };
    }

    private ModelAnalysis analyzeStl(Path file, String unit, double scale) throws IOException {
        long size = Files.size(file);
        if (size < 84) return result(ModelAnalysis.Status.INVALID, "stl", unit, "Tệp STL quá ngắn hoặc bị lỗi.");
        byte[] header = new byte[84];
        try (var input = new BufferedInputStream(Files.newInputStream(file))) {
            if (input.readNBytes(header, 0, header.length) != header.length) {
                return result(ModelAnalysis.Status.INVALID, "stl", unit, "Không đọc được phần đầu của tệp STL.");
            }
        }
        long triangleCount = Integer.toUnsignedLong(ByteBuffer.wrap(header, 80, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getInt());
        long expectedLength = 84L + triangleCount * 50L;
        if (triangleCount > 0 && expectedLength == size) {
            if (triangleCount > MAX_ANALYZED_TRIANGLES) {
                return result(ModelAnalysis.Status.QUOTE_REQUIRED, "stl", unit,
                        "Mô hình có quá nhiều tam giác để ước tính ổn định; xưởng cần kiểm tra file.");
            }
            return analyzeBinaryStl(file, unit, scale, triangleCount);
        }
        return analyzeAsciiStl(file, unit, scale);
    }

    private ModelAnalysis analyzeBinaryStl(Path file, String unit, double scale, long count) throws IOException {
        Mesh mesh = new Mesh();
        try (var input = new BufferedInputStream(Files.newInputStream(file))) {
            input.skipNBytes(84);
            byte[] triangle = new byte[50];
            ByteBuffer data = ByteBuffer.wrap(triangle).order(ByteOrder.LITTLE_ENDIAN);
            for (long i = 0; i < count; i++) {
                if (input.readNBytes(triangle, 0, triangle.length) != triangle.length) {
                    return result(ModelAnalysis.Status.INVALID, "stl", unit, "Tệp STL nhị phân bị cắt cụt.");
                }
                data.position(12); // Skip the stored normal; calculate geometry from vertices.
                Point3 a = point(data.getFloat(), data.getFloat(), data.getFloat(), scale);
                Point3 b = point(data.getFloat(), data.getFloat(), data.getFloat(), scale);
                Point3 c = point(data.getFloat(), data.getFloat(), data.getFloat(), scale);
                mesh.add(a, b, c);
                data.position(48);
            }
        } catch (IllegalArgumentException e) {
            return result(ModelAnalysis.Status.INVALID, "stl", unit, e.getMessage());
        }
        return finish(mesh, "stl", unit);
    }

    private ModelAnalysis analyzeAsciiStl(Path file, String unit, double scale) throws IOException {
        Mesh mesh = new Mesh();
        Point3[] vertices = new Point3[3];
        int vertexCount = 0;
        boolean sawSolid = false;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.US_ASCII)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.regionMatches(true, 0, "solid", 0, 5)) sawSolid = true;
                if (!trimmed.regionMatches(true, 0, "vertex", 0, 6)) continue;
                String[] fields = trimmed.split("\\s+");
                if (fields.length != 4) return result(ModelAnalysis.Status.INVALID, "stl", unit, "Tọa độ vertex trong STL không hợp lệ.");
                vertices[vertexCount++] = point(parse(fields[1]), parse(fields[2]), parse(fields[3]), scale);
                if (vertexCount == 3) {
                    mesh.add(vertices[0], vertices[1], vertices[2]);
                    vertexCount = 0;
                    if (mesh.triangles > MAX_ANALYZED_TRIANGLES) {
                        return result(ModelAnalysis.Status.QUOTE_REQUIRED, "stl", unit,
                                "Mô hình có quá nhiều tam giác để ước tính ổn định; xưởng cần kiểm tra file.");
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            return result(ModelAnalysis.Status.INVALID, "stl", unit, e.getMessage());
        }
        if (!sawSolid || vertexCount != 0 || mesh.triangles == 0) {
            return result(ModelAnalysis.Status.INVALID, "stl", unit, "Tệp STL ASCII không có các mặt tam giác hợp lệ.");
        }
        return finish(mesh, "stl", unit);
    }

    private ModelAnalysis analyzeObj(Path file, String unit, double scale) throws IOException {
        List<Point3> vertices = new ArrayList<>();
        Mesh mesh = new Mesh();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                if (trimmed.startsWith("v ") || trimmed.startsWith("v\t")) {
                    String[] fields = trimmed.split("\\s+");
                    if (fields.length < 4) return result(ModelAnalysis.Status.INVALID, "obj", unit, "Một đỉnh OBJ thiếu tọa độ.");
                    if (vertices.size() >= MAX_OBJ_VERTICES) {
                        return result(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit,
                                "Mô hình có quá nhiều đỉnh để phân tích ổn định; xưởng cần kiểm tra file.");
                    }
                    vertices.add(point(parse(fields[1]), parse(fields[2]), parse(fields[3]), scale));
                } else if (trimmed.startsWith("f ") || trimmed.startsWith("f\t")) {
                    String[] fields = trimmed.split("\\s+");
                    if (fields.length < 4) return result(ModelAnalysis.Status.INVALID, "obj", unit, "Mặt OBJ phải có ít nhất ba đỉnh.");
                    if (fields.length > 258) {
                        return result(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit,
                                "Mặt OBJ có quá nhiều đỉnh để phân tích an toàn; xưởng cần kiểm tra file.");
                    }
                    List<Point3> face = new ArrayList<>(fields.length - 1);
                    for (int i = 1; i < fields.length; i++) face.add(vertex(vertices, fields[i]));
                    if (!isSafeConvexPlanarFace(face)) {
                        return result(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit,
                                "Mặt đa giác OBJ không lồi hoặc không phẳng; xưởng cần kiểm tra file trước khi ước tính.");
                    }
                    Point3 first = face.get(0);
                    for (int i = 2; i < face.size(); i++) {
                        mesh.add(first, face.get(i - 1), face.get(i));
                        if (mesh.triangles > MAX_ANALYZED_TRIANGLES) {
                            return result(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit,
                                    "Mô hình có quá nhiều tam giác để ước tính ổn định; xưởng cần kiểm tra file.");
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            return result(ModelAnalysis.Status.INVALID, "obj", unit, e.getMessage());
        }
        if (vertices.isEmpty() || mesh.triangles == 0) {
            return result(ModelAnalysis.Status.INVALID, "obj", unit, "Tệp OBJ rỗng hoặc không có mặt mô hình.");
        }
        return finish(mesh, "obj", unit);
    }

    private ModelAnalysis validate3mf(Path file, String unit) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry model = zip.getEntry("3D/3dmodel.model");
            if (model == null || model.isDirectory() || model.getSize() <= 0
                    || model.getSize() > MAX_ARCHIVE_MODEL_BYTES) {
                return result(ModelAnalysis.Status.INVALID, "3mf", unit, "Gói 3MF không có mô hình 3D hợp lệ.");
            }
            try (InputStream bounded = new BoundedInputStream(zip.getInputStream(model), MAX_ARCHIVE_MODEL_BYTES)) {
                XMLInputFactory factory = XMLInputFactory.newFactory();
                factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
                factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
                XMLStreamReader reader = factory.createXMLStreamReader(bounded, StandardCharsets.UTF_8.name());
                boolean validModelRoot = false;
                while (reader.hasNext()) {
                    if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                        validModelRoot = "model".equals(reader.getLocalName())
                                && "http://schemas.microsoft.com/3dmanufacturing/core/2015/02".equals(reader.getNamespaceURI());
                        break;
                    }
                }
                while (reader.hasNext()) reader.next(); // Consume the complete XML so malformed archives are rejected.
                reader.close();
                if (!validModelRoot) return result(ModelAnalysis.Status.INVALID, "3mf", unit, "Tệp 3MF không chứa tài liệu model hợp lệ.");
            } catch (XMLStreamException | IOException | IllegalArgumentException e) {
                return result(ModelAnalysis.Status.INVALID, "3mf", unit, "Tài liệu bên trong tệp 3MF bị lỗi hoặc không an toàn.");
            }
            return result(ModelAnalysis.Status.QUOTE_REQUIRED, "3mf", unit,
                    "Tệp 3MF hợp lệ nhưng chưa thể phân tích hình học tin cậy; vui lòng yêu cầu xưởng báo giá.");
        } catch (java.util.zip.ZipException e) {
            return result(ModelAnalysis.Status.INVALID, "3mf", unit, "Tệp 3MF không phải gói ZIP hợp lệ.");
        }
    }

    private ModelAnalysis validateStep(Path file, String format, String unit) throws IOException {
        byte[] start = new byte[(int) Math.min(Files.size(file), 256)];
        try (var input = Files.newInputStream(file)) {
            if (input.readNBytes(start, 0, start.length) != start.length) {
                return result(ModelAnalysis.Status.INVALID, format, unit, "Không đọc được tệp STEP.");
            }
        }
        String header = new String(start, StandardCharsets.US_ASCII).replace("\uFEFF", "").trim();
        if (!header.startsWith("ISO-10303-21;")) {
            return result(ModelAnalysis.Status.INVALID, format, unit, "Tệp STEP không có chữ ký ISO-10303-21 hợp lệ.");
        }
        long dataMarkerEnd = -1, dataEndMarkerEnd = -1;
        boolean endFile = false;
        String carry = "";
        long processed = 0;
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, read, StandardCharsets.US_ASCII).toUpperCase(Locale.ROOT);
                String combined = carry + chunk;
                long combinedStart = processed - carry.length();
                if (dataMarkerEnd < 0) {
                    int index = combined.indexOf("DATA;");
                    if (index >= 0) dataMarkerEnd = combinedStart + index + 5;
                }
                if (dataMarkerEnd >= 0 && dataEndMarkerEnd < 0) {
                    int from = (int) Math.max(0, dataMarkerEnd - combinedStart);
                    int index = combined.indexOf("ENDSEC;", from);
                    if (index >= 0) dataEndMarkerEnd = combinedStart + index + 7;
                }
                if (dataEndMarkerEnd >= 0) {
                    int from = (int) Math.max(0, dataEndMarkerEnd - combinedStart);
                    if (combined.indexOf("END-ISO-10303-21;", from) >= 0) endFile = true;
                }
                processed += read;
                carry = combined.substring(Math.max(0, combined.length() - 32));
            }
        }
        if (dataMarkerEnd < 0 || dataEndMarkerEnd < 0 || !endFile) {
            return result(ModelAnalysis.Status.INVALID, format, unit, "Tệp STEP thiếu phần DATA hoặc dấu kết thúc chuẩn ISO-10303-21.");
        }
        return result(ModelAnalysis.Status.QUOTE_REQUIRED, format, unit,
                "Tệp STEP/STP đã được nhận dạng nhưng chưa có bộ phân tích CAD; vui lòng yêu cầu xưởng báo giá.");
    }

    private ModelAnalysis finish(Mesh mesh, String format, String unit) {
        if (mesh.triangles == 0 || mesh.edges.isEmpty()) {
            return result(ModelAnalysis.Status.INVALID, format, unit, "Mô hình rỗng hoặc không có tam giác hợp lệ.");
        }
        if (mesh.degenerateTriangles > 0) {
            return result(ModelAnalysis.Status.INVALID, format, unit, "Mô hình chứa mặt tam giác suy biến.");
        }
        if (mesh.edges.values().stream().anyMatch(edge -> edge.count != 2 || edge.directionBalance != 0)) {
            return result(ModelAnalysis.Status.QUOTE_REQUIRED, format, unit,
                    "Mesh không kín hoặc không-manifold; xưởng cần kiểm tra trước khi báo giá.");
        }
        if (Math.abs(mesh.signedVolume) <= 1.0e-9) {
            return result(ModelAnalysis.Status.QUOTE_REQUIRED, format, unit,
                    "Hướng mặt hoặc thể tích mesh không đủ tin cậy để ước tính.");
        }
        double width = mesh.maxX - mesh.minX;
        double depth = mesh.maxY - mesh.minY;
        double height = mesh.maxZ - mesh.minZ;
        double volumeCm3 = Math.abs(mesh.signedVolume) / 1000.0;
        double areaCm2 = mesh.surfaceArea / 100.0;
        if (!Double.isFinite(width + depth + height + volumeCm3 + areaCm2)
                || width <= 0 || depth <= 0 || height <= 0 || volumeCm3 <= 0) {
            return result(ModelAnalysis.Status.INVALID, format, unit, "Kích thước hoặc thể tích mô hình không hợp lệ.");
        }
        ModelAnalysis.Status status = Math.min(width, Math.min(depth, height)) < 0.1
                || Math.max(width, Math.max(depth, height)) > 1000
                ? ModelAnalysis.Status.NEEDS_CONFIRMATION : ModelAnalysis.Status.READY;
        String message = status == ModelAnalysis.Status.NEEDS_CONFIRMATION
                ? "Kích thước sau khi đổi sang mm bất thường. Hãy xác nhận đơn vị và kích thước trước khi tiếp tục."
                : "Đã phân tích mesh kín; giá dưới đây vẫn là ước tính, không phải kết quả slicer.";
        return new ModelAnalysis(status, format, unit, width, depth, height, volumeCm3, areaCm2,
                mesh.triangles, message);
    }

    private static Point3 vertex(List<Point3> vertices, String token) {
        String rawIndex = token.split("/", -1)[0];
        int index;
        try {
            index = Integer.parseInt(rawIndex);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Chỉ số đỉnh trong OBJ không hợp lệ.");
        }
        int resolved = index > 0 ? index - 1 : vertices.size() + index;
        if (index == 0 || resolved < 0 || resolved >= vertices.size()) {
            throw new IllegalArgumentException("Mặt OBJ tham chiếu tới đỉnh không tồn tại.");
        }
        return vertices.get(resolved);
    }

    /**
     * Fan triangulation is reliable for triangles and convex planar polygons. Concave,
     * self-intersecting, and non-planar faces are routed to a workshop for review.
     */
    private static boolean isSafeConvexPlanarFace(List<Point3> face) {
        if (face.size() == 3) return true;

        Point3 origin = face.get(0);
        double scale = 0;
        for (Point3 point : face) {
            scale = Math.max(scale, distance(origin, point));
        }
        if (!Double.isFinite(scale) || scale <= 0) return false;

        double normalX = 0, normalY = 0, normalZ = 0;
        for (int i = 1; i < face.size() - 1; i++) {
            Point3 a = subtract(face.get(i), origin);
            Point3 b = subtract(face.get(i + 1), origin);
            normalX += a.y * b.z - a.z * b.y;
            normalY += a.z * b.x - a.x * b.z;
            normalZ += a.x * b.y - a.y * b.x;
        }
        double normalLength = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        double areaTolerance = Math.max(scale * scale * 1.0e-12, 1.0e-12);
        if (!Double.isFinite(normalLength) || normalLength <= areaTolerance) return false;
        normalX /= normalLength;
        normalY /= normalLength;
        normalZ /= normalLength;

        double planeTolerance = Math.max(scale * 1.0e-8, 1.0e-9);
        double sideTolerance = Math.max(scale * scale * 1.0e-10, 1.0e-12);
        for (Point3 point : face) {
            Point3 relative = subtract(point, origin);
            double distanceFromPlane = relative.x * normalX + relative.y * normalY + relative.z * normalZ;
            if (Math.abs(distanceFromPlane) > planeTolerance) return false;
        }

        // Every vertex must lie on the interior side of every directed edge. This
        // also rejects self-intersecting star polygons that a turn-sign check can miss.
        for (int i = 0; i < face.size(); i++) {
            Point3 start = face.get(i);
            Point3 end = face.get((i + 1) % face.size());
            Point3 edge = subtract(end, start);
            if (distance(start, end) <= Math.max(scale * 1.0e-10, 1.0e-12)) return false;
            boolean hasInteriorPoint = false;
            for (Point3 point : face) {
                Point3 relative = subtract(point, start);
                double side = (edge.y * relative.z - edge.z * relative.y) * normalX
                        + (edge.z * relative.x - edge.x * relative.z) * normalY
                        + (edge.x * relative.y - edge.y * relative.x) * normalZ;
                if (side < -sideTolerance) return false;
                if (side > sideTolerance) hasInteriorPoint = true;
            }
            if (!hasInteriorPoint) return false;
        }
        return true;
    }

    private static Point3 subtract(Point3 left, Point3 right) {
        return new Point3(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    private static double distance(Point3 left, Point3 right) {
        Point3 delta = subtract(left, right);
        return Math.sqrt(delta.x * delta.x + delta.y * delta.y + delta.z * delta.z);
    }

    private static Point3 point(double x, double y, double z, double scale) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Tệp mô hình chứa tọa độ không hữu hạn.");
        }
        return new Point3(x * scale, y * scale, z * scale);
    }

    private static double parse(String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tệp mô hình chứa tọa độ không hợp lệ.");
        }
    }

    private static ModelAnalysis result(ModelAnalysis.Status status, String format, String unit, String message) {
        return new ModelAnalysis(status, format == null ? "" : format, unit == null ? "mm" : unit,
                0, 0, 0, 0, 0, 0, message);
    }

    private record Point3(double x, double y, double z) { }

    private record VertexKey(long x, long y, long z) implements Comparable<VertexKey> {
        static VertexKey of(Point3 point) {
            return new VertexKey(Math.round(point.x * EDGE_QUANTIZATION),
                    Math.round(point.y * EDGE_QUANTIZATION), Math.round(point.z * EDGE_QUANTIZATION));
        }
        @Override public int compareTo(VertexKey other) {
            int compared = Long.compare(x, other.x);
            if (compared == 0) compared = Long.compare(y, other.y);
            if (compared == 0) compared = Long.compare(z, other.z);
            return compared;
        }
    }

    private record EdgeKey(VertexKey first, VertexKey second) {
        static EdgeKey of(Point3 a, Point3 b) {
            VertexKey left = VertexKey.of(a), right = VertexKey.of(b);
            return left.compareTo(right) <= 0 ? new EdgeKey(left, right) : new EdgeKey(right, left);
        }
        static int direction(Point3 a, Point3 b) {
            return VertexKey.of(a).compareTo(VertexKey.of(b)) <= 0 ? 1 : -1;
        }
    }

    private static final class EdgeUse {
        private int count;
        private int directionBalance;
    }

    private static final class BoundedInputStream extends FilterInputStream {
        private long remaining;
        BoundedInputStream(InputStream input, long maximumBytes) { super(input); remaining = maximumBytes; }
        @Override public int read() throws IOException {
            if (remaining == 0) {
                if (super.read() == -1) return -1;
                throw new IOException("3MF model entry exceeds the analysis size limit");
            }
            int value = super.read();
            if (value >= 0) remaining--;
            return value;
        }
        @Override public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining == 0) return read() == -1 ? -1 : 0;
            int count = super.read(buffer, offset, (int) Math.min(length, remaining));
            if (count > 0) remaining -= count;
            return count;
        }
    }

    private static final class Mesh {
        private final Map<EdgeKey, EdgeUse> edges = new HashMap<>();
        private long triangles;
        private long degenerateTriangles;
        private double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        private double signedVolume;
        private double surfaceArea;
        private Point3 volumeOrigin;

        void add(Point3 a, Point3 b, Point3 c) {
            triangles++;
            if (volumeOrigin == null) volumeOrigin = a;
            include(a); include(b); include(c);
            double abX = b.x - a.x, abY = b.y - a.y, abZ = b.z - a.z;
            double acX = c.x - a.x, acY = c.y - a.y, acZ = c.z - a.z;
            double crossX = abY * acZ - abZ * acY;
            double crossY = abZ * acX - abX * acZ;
            double crossZ = abX * acY - abY * acX;
            double crossMagnitude = Math.sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ);
            if (!Double.isFinite(crossMagnitude) || crossMagnitude <= 1.0e-12) degenerateTriangles++;
            surfaceArea += crossMagnitude / 2.0;
            // Translate around a fixed mesh vertex to reduce floating-point cancellation
            // when otherwise small models are stored far from the origin.
            double ax = a.x - volumeOrigin.x, ay = a.y - volumeOrigin.y, az = a.z - volumeOrigin.z;
            double bx = b.x - volumeOrigin.x, by = b.y - volumeOrigin.y, bz = b.z - volumeOrigin.z;
            double cx = c.x - volumeOrigin.x, cy = c.y - volumeOrigin.y, cz = c.z - volumeOrigin.z;
            double tetraVolume = (ax * (by * cz - bz * cy)
                    + ay * (bz * cx - bx * cz)
                    + az * (bx * cy - by * cx)) / 6.0;
            signedVolume += tetraVolume;
            addEdge(a, b);
            addEdge(b, c);
            addEdge(c, a);
        }

        private void addEdge(Point3 first, Point3 second) {
            EdgeKey key = EdgeKey.of(first, second);
            EdgeUse use = edges.computeIfAbsent(key, ignored -> new EdgeUse());
            use.count++;
            use.directionBalance += EdgeKey.direction(first, second);
        }

        private void include(Point3 point) {
            minX = Math.min(minX, point.x); minY = Math.min(minY, point.y); minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x); maxY = Math.max(maxY, point.y); maxZ = Math.max(maxZ, point.z);
        }
    }
}
