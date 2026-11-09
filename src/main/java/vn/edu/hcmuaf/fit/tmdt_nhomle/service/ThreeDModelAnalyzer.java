package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Validates OBJ geometry without relying on a CAD or slicer library. */
public final class ThreeDModelAnalyzer {
    private static final long MAX_FILE_BYTES = 100L * 1024 * 1024;
    private static final long MAX_TRIANGLES = 50_000;
    private static final int MAX_VERTICES = 200_000;
    private static final int MAX_FACE_VERTICES = 256;
    private static final int MAX_LINE_LENGTH = 1_048_576;
    private static final double EDGE_QUANTIZATION = 10_000.0;

    public ModelAnalysis analyze(Path file, String extension, String requestedUnit) {
        String unit;
        try {
            unit = normalizeUnit(requestedUnit);
        } catch (IllegalArgumentException e) {
            return result(ModelAnalysis.Status.INVALID, "obj", "mm", e.getMessage());
        }
        if (extension == null || !"obj".equalsIgnoreCase(extension.trim())) {
            return result(ModelAnalysis.Status.INVALID, extension, unit, "Chỉ hỗ trợ tệp OBJ.");
        }
        if (file == null || !Files.isRegularFile(file)) {
            return result(ModelAnalysis.Status.INVALID, "obj", unit, "Không tìm thấy tệp OBJ.");
        }
        Path filename = file.getFileName();
        if (filename == null || !filename.toString().toLowerCase(Locale.ROOT).endsWith(".obj")) {
            return result(ModelAnalysis.Status.INVALID, "obj", unit, "Chỉ hỗ trợ tệp có phần mở rộng .obj.");
        }
        try {
            long size = Files.size(file);
            if (size <= 0) return result(ModelAnalysis.Status.INVALID, "obj", unit, "Tệp OBJ rỗng.");
            if (size > MAX_FILE_BYTES) return result(ModelAnalysis.Status.INVALID, "obj", unit, "Tệp OBJ vượt giới hạn 100 MB.");
            return analyzeObj(file, unit, unitScale(unit));
        } catch (IOException e) {
            return result(ModelAnalysis.Status.INVALID, "obj", unit, "Không đọc được tệp OBJ hoặc tệp chứa UTF-8 không hợp lệ.");
        }
    }

    public static String normalizeUnit(String value) {
        if (value == null || value.isBlank()) return "mm";
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "mm", "millimeter", "millimeters" -> "mm";
            case "cm", "centimeter", "centimeters" -> "cm";
            case "in", "inch", "inches" -> "in";
            default -> throw new IllegalArgumentException("Đơn vị chỉ có thể là mm, cm hoặc inch.");
        };
    }

    private static double unitScale(String unit) {
        return switch (unit) {
            case "cm" -> 10.0;
            case "in" -> 25.4;
            default -> 1.0;
        };
    }

    private ModelAnalysis analyzeObj(Path file, String unit, double scale) throws IOException {
        List<Point3> vertices = new ArrayList<>();
        int textureCoordinates = 0;
        int normals = 0;
        Mesh mesh = new Mesh();
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(file), decoder))) {
            String line;
            boolean firstLine = true;
            while ((line = readBoundedLine(reader)) != null) {
                if (firstLine) {
                    line = line.startsWith("\uFEFF") ? line.substring(1) : line;
                    firstLine = false;
                }
                int comment = line.indexOf('#');
                String content = (comment < 0 ? line : line.substring(0, comment)).trim();
                if (content.isEmpty()) continue;
                String[] fields = content.split("\\s+");
                switch (fields[0]) {
                    case "v" -> {
                        if (fields.length != 4 && fields.length != 5) {
                            return result(ModelAnalysis.Status.INVALID, "obj", unit, "Vertex OBJ phải có ba tọa độ và tùy chọn tọa độ w.");
                        }
                        if (vertices.size() >= MAX_VERTICES) {
                            return result(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit, "Mô hình có quá nhiều đỉnh để phân tích tự động; xưởng cần kiểm tra file.");
                        }
                        double x = parse(fields[1]), y = parse(fields[2]), z = parse(fields[3]);
                        if (fields.length == 5) {
                            double w = parse(fields[4]);
                            if (w == 0) return result(ModelAnalysis.Status.INVALID, "obj", unit, "Tọa độ w của vertex OBJ không hợp lệ.");
                            x /= w; y /= w; z /= w;
                        }
                        vertices.add(point(x, y, z, scale));
                    }
                    case "vt" -> {
                        if (fields.length < 2 || fields.length > 4) return result(ModelAnalysis.Status.INVALID, "obj", unit, "Tọa độ texture OBJ không hợp lệ.");
                        for (int i = 1; i < fields.length; i++) parse(fields[i]);
                        textureCoordinates++;
                    }
                    case "vn" -> {
                        if (fields.length != 4) return result(ModelAnalysis.Status.INVALID, "obj", unit, "Vector pháp tuyến OBJ không hợp lệ.");
                        parse(fields[1]); parse(fields[2]); parse(fields[3]);
                        normals++;
                    }
                    case "f" -> {
                        if (fields.length < 4) return result(ModelAnalysis.Status.INVALID, "obj", unit, "Mặt OBJ phải có ít nhất ba đỉnh.");
                        if (fields.length > MAX_FACE_VERTICES + 1) {
                            return result(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit, "Mặt OBJ có quá nhiều đỉnh để phân tích an toàn; xưởng cần kiểm tra file.");
                        }
                        List<Point3> face = new ArrayList<>(fields.length - 1);
                        for (int i = 1; i < fields.length; i++) {
                            face.add(faceVertex(vertices, textureCoordinates, normals, fields[i]));
                        }
                        if (!isSafeConvexPlanarFace(face)) {
                            return result(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit,
                                    "Mặt OBJ không lồi hoặc không phẳng; xưởng cần kiểm tra trước khi báo giá.");
                        }
                        Point3 first = face.get(0);
                        for (int i = 2; i < face.size(); i++) {
                            mesh.add(first, face.get(i - 1), face.get(i));
                            if (mesh.triangles > MAX_TRIANGLES) {
                                return result(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit, "Mô hình có quá nhiều tam giác để phân tích tự động; xưởng cần kiểm tra file.");
                            }
                        }
                    }
                    // Object/group/material/smoothing declarations do not reset the global OBJ indices.
                    case "o", "g", "s", "usemtl", "mtllib" -> { }
                    // Other non-geometry records do not change the mesh used for print analysis.
                    default -> { }
                }
            }
        } catch (IllegalArgumentException e) {
            return result(ModelAnalysis.Status.INVALID, "obj", unit, e.getMessage());
        }
        if (vertices.isEmpty() || mesh.triangles == 0) {
            return result(ModelAnalysis.Status.INVALID, "obj", unit, "Tệp OBJ phải có vertex và face hợp lệ.");
        }
        return finish(mesh, unit);
    }

    private static String readBoundedLine(BufferedReader reader) throws IOException {
        StringBuilder line = new StringBuilder();
        int character;
        while ((character = reader.read()) != -1) {
            if (character == '\n') return line.toString();
            if (character == '\r') {
                reader.mark(1);
                if (reader.read() != '\n') reader.reset();
                return line.toString();
            }
            if (line.length() >= MAX_LINE_LENGTH) throw new IOException("OBJ line exceeds the analysis limit.");
            line.append((char) character);
        }
        return line.isEmpty() ? null : line.toString();
    }

    private static Point3 faceVertex(List<Point3> vertices, int textureCoordinates, int normals, String token) {
        String[] indexes = token.split("/", -1);
        if (indexes.length < 1 || indexes.length > 3 || indexes[0].isEmpty()) {
            throw new IllegalArgumentException("Chỉ số mặt OBJ không hợp lệ.");
        }
        int vertexIndex = resolveIndex(indexes[0], vertices.size(), "vertex");
        if (indexes.length >= 2 && !indexes[1].isEmpty()) resolveIndex(indexes[1], textureCoordinates, "texture");
        if (indexes.length == 3) {
            if (indexes[1].isEmpty() && indexes[2].isEmpty()) throw new IllegalArgumentException("Chỉ số mặt OBJ không hợp lệ.");
            if (indexes[2].isEmpty()) throw new IllegalArgumentException("Mặt OBJ thiếu chỉ số normal.");
            resolveIndex(indexes[2], normals, "normal");
        }
        if (indexes.length == 2 && indexes[1].isEmpty()) throw new IllegalArgumentException("Chỉ số mặt OBJ không hợp lệ.");
        return vertices.get(vertexIndex);
    }

    private static int resolveIndex(String raw, int count, String kind) {
        try {
            int index = Integer.parseInt(raw);
            int resolved = index > 0 ? index - 1 : count + index;
            if (index == 0 || resolved < 0 || resolved >= count) throw new NumberFormatException();
            return resolved;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Mặt OBJ tham chiếu tới chỉ số " + kind + " không tồn tại.");
        }
    }

    private static ModelAnalysis finish(Mesh mesh, String unit) {
        if (mesh.degenerateTriangles > 0) return result(ModelAnalysis.Status.INVALID, "obj", unit, "Mô hình chứa mặt tam giác suy biến.");
        double width = mesh.maxX - mesh.minX;
        double depth = mesh.maxY - mesh.minY;
        double height = mesh.maxZ - mesh.minZ;
        double areaCm2 = mesh.surfaceArea / 100.0;
        if (!Double.isFinite(width + depth + height + areaCm2) || width <= 0 || depth <= 0 || height <= 0 || areaCm2 <= 0) {
            return result(ModelAnalysis.Status.INVALID, "obj", unit, "Kích thước hoặc diện tích bề mặt mô hình không hợp lệ.");
        }
        if (Math.max(width, Math.max(depth, height)) > 100_000) {
            return new ModelAnalysis(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit, width, depth, height,
                    0, areaCm2, mesh.triangles, "Mô hình có kích thước vượt phạm vi định giá tự động; xưởng cần kiểm tra.");
        }
        boolean closed = mesh.edges.values().stream().allMatch(edge -> edge.count == 2 && edge.directionBalance == 0);
        if (!closed) {
            return new ModelAnalysis(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit, width, depth, height,
                    0, areaCm2, mesh.triangles, "Mesh không kín hoặc không-manifold; xưởng cần kiểm tra trước khi báo giá.");
        }
        double volumeCm3 = Math.abs(mesh.signedVolume) / 1000.0;
        if (!Double.isFinite(volumeCm3) || volumeCm3 <= 1.0e-9) {
            return new ModelAnalysis(ModelAnalysis.Status.QUOTE_REQUIRED, "obj", unit, width, depth, height,
                    0, areaCm2, mesh.triangles, "Hướng mặt hoặc thể tích mesh không đủ tin cậy để ước tính.");
        }
        ModelAnalysis.Status status = Math.min(width, Math.min(depth, height)) < 0.1
                || Math.max(width, Math.max(depth, height)) > 1000
                ? ModelAnalysis.Status.NEEDS_CONFIRMATION : ModelAnalysis.Status.READY;
        String message = status == ModelAnalysis.Status.NEEDS_CONFIRMATION
                ? "Kích thước sau khi đổi sang mm bất thường. Hãy xác nhận đơn vị và kích thước trước khi tiếp tục."
                : "Đã phân tích mesh kín; giá hiển thị là ước tính, không phải kết quả slicer.";
        return new ModelAnalysis(status, "obj", unit, width, depth, height, volumeCm3,
                areaCm2, mesh.triangles, message);
    }

    private static boolean isSafeConvexPlanarFace(List<Point3> face) {
        if (face.size() == 3) return true;
        Point3 origin = face.get(0);
        double scale = 0;
        for (Point3 point : face) scale = Math.max(scale, distance(origin, point));
        if (!Double.isFinite(scale) || scale <= 0) return false;
        double normalX = 0, normalY = 0, normalZ = 0;
        for (int i = 1; i < face.size() - 1; i++) {
            Point3 a = subtract(face.get(i), origin), b = subtract(face.get(i + 1), origin);
            normalX += a.y * b.z - a.z * b.y;
            normalY += a.z * b.x - a.x * b.z;
            normalZ += a.x * b.y - a.y * b.x;
        }
        double normalLength = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        double areaTolerance = Math.max(scale * scale * 1.0e-12, 1.0e-12);
        if (!Double.isFinite(normalLength) || normalLength <= areaTolerance) return false;
        normalX /= normalLength; normalY /= normalLength; normalZ /= normalLength;
        double planeTolerance = Math.max(scale * 1.0e-8, 1.0e-9);
        double sideTolerance = Math.max(scale * scale * 1.0e-10, 1.0e-12);
        for (Point3 point : face) {
            Point3 relative = subtract(point, origin);
            if (Math.abs(relative.x * normalX + relative.y * normalY + relative.z * normalZ) > planeTolerance) return false;
        }
        for (int i = 0; i < face.size(); i++) {
            Point3 start = face.get(i), end = face.get((i + 1) % face.size()), edge = subtract(end, start);
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

    private static Point3 point(double x, double y, double z, double scale) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Tệp OBJ chứa tọa độ không hữu hạn.");
        }
        double scaledX = x * scale, scaledY = y * scale, scaledZ = z * scale;
        if (!Double.isFinite(scaledX) || !Double.isFinite(scaledY) || !Double.isFinite(scaledZ)
                || Math.abs(scaledX) > 1.0e12 || Math.abs(scaledY) > 1.0e12 || Math.abs(scaledZ) > 1.0e12) {
            throw new IllegalArgumentException("Tọa độ OBJ vượt giới hạn phân tích an toàn.");
        }
        return new Point3(scaledX, scaledY, scaledZ);
    }

    private static double parse(String value) {
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Tệp OBJ chứa tọa độ không hợp lệ.");
        }
    }

    private static Point3 subtract(Point3 left, Point3 right) {
        return new Point3(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    private static double distance(Point3 left, Point3 right) {
        Point3 delta = subtract(left, right);
        return Math.sqrt(delta.x * delta.x + delta.y * delta.y + delta.z * delta.z);
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
            double ax = a.x - volumeOrigin.x, ay = a.y - volumeOrigin.y, az = a.z - volumeOrigin.z;
            double bx = b.x - volumeOrigin.x, by = b.y - volumeOrigin.y, bz = b.z - volumeOrigin.z;
            double cx = c.x - volumeOrigin.x, cy = c.y - volumeOrigin.y, cz = c.z - volumeOrigin.z;
            signedVolume += (ax * (by * cz - bz * cy) + ay * (bz * cx - bx * cz) + az * (bx * cy - by * cx)) / 6.0;
            addEdge(a, b); addEdge(b, c); addEdge(c, a);
        }
        private void addEdge(Point3 a, Point3 b) {
            EdgeUse use = edges.computeIfAbsent(EdgeKey.of(a, b), ignored -> new EdgeUse());
            use.count++;
            use.directionBalance += EdgeKey.direction(a, b);
        }
        private void include(Point3 point) {
            minX = Math.min(minX, point.x); minY = Math.min(minY, point.y); minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x); maxY = Math.max(maxY, point.y); maxZ = Math.max(maxZ, point.z);
        }
    }
}
