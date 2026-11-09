package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.DesignFile;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.ModelAnalysis;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Optional;

public final class DesignFileDao {

    private DesignFileDao() {
    }

    public static long insert(Connection conn, Long userId, Long productId,
                              String originalFilename, String storageUrl,
                              String previewUrl, String status) throws SQLException {
        String sql = "INSERT INTO design_files (user_id, product_id, original_filename, storage_url, preview_url, status, created_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, NOW())";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (userId != null) {
                ps.setLong(1, userId);
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            if (productId != null) {
                ps.setLong(2, productId);
            } else {
                ps.setNull(2, Types.BIGINT);
            }
            ps.setString(3, originalFilename);
            ps.setString(4, storageUrl);
            ps.setString(5, previewUrl);
            ps.setString(6, status == null ? "ACTIVE" : status);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Không lấy được ID bản ghi design_files vừa tạo.");
    }

    public static long insertAnalyzed(Connection conn, long userId, String originalFilename,
                                      String storageUrl, String mimeType, long fileSizeBytes,
                                      String sha256Hash, ModelAnalysis analysis) throws SQLException {
        String sql = "INSERT INTO design_files (user_id, product_id, original_filename, storage_url,"
                + " preview_url, status, created_at, mime_type, file_size_bytes, sha256_hash,"
                + " analysis_status, source_unit, width_mm, depth_mm, height_mm, volume_cm3,"
                + " surface_area_cm2, triangle_count, analysis_message)"
                + " VALUES (?, NULL, ?, ?, NULL, 'ACTIVE', NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, originalFilename);
            ps.setString(3, storageUrl);
            ps.setString(4, mimeType);
            ps.setLong(5, fileSizeBytes);
            ps.setString(6, sha256Hash);
            ps.setString(7, analysis.status().name());
            ps.setString(8, analysis.unit());
            setMeasurement(ps, 9, analysis.widthMm());
            setMeasurement(ps, 10, analysis.depthMm());
            setMeasurement(ps, 11, analysis.heightMm());
            setMeasurement(ps, 12, analysis.volumeCm3());
            setMeasurement(ps, 13, analysis.surfaceAreaCm2());
            ps.setLong(14, analysis.triangleCount());
            ps.setString(15, analysis.message());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Không lấy được ID file thiết kế vừa tạo.");
    }

    public static long insert(Long userId, Long productId, String originalFilename,
                              String storageUrl, String previewUrl, String status) {
        try (Connection conn = Database.getConnection()) {
            return insert(conn, userId, productId, originalFilename, storageUrl, previewUrl, status);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<DesignFile> findById(long designFileId) {
        String sql = "SELECT design_file_id, user_id, product_id, original_filename, storage_url, preview_url, status, created_at,"
                + " mime_type, file_size_bytes, sha256_hash, analysis_status, source_unit, width_mm, depth_mm, height_mm,"
                + " volume_cm3, surface_area_cm2, triangle_count, analysis_message"
                + " FROM design_files WHERE design_file_id = ? AND deleted_at IS NULL";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, designFileId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DesignFile df = new DesignFile();
                    df.setDesignFileId(rs.getLong("design_file_id"));
                    long uid = rs.getLong("user_id");
                    df.setUserId(rs.wasNull() ? null : uid);
                    long pid = rs.getLong("product_id");
                    df.setProductId(rs.wasNull() ? null : pid);
                    df.setOriginalFilename(rs.getString("original_filename"));
                    df.setStorageUrl(rs.getString("storage_url"));
                    df.setPreviewUrl(rs.getString("preview_url"));
                    df.setStatus(rs.getString("status"));
                    df.setCreatedAt(rs.getTimestamp("created_at"));
                    df.setMimeType(rs.getString("mime_type"));
                    long fileSize = rs.getLong("file_size_bytes");
                    df.setFileSizeBytes(rs.wasNull() ? null : fileSize);
                    df.setSha256Hash(rs.getString("sha256_hash"));
                    df.setAnalysisStatus(rs.getString("analysis_status"));
                    df.setSourceUnit(rs.getString("source_unit"));
                    df.setWidthMm(rs.getBigDecimal("width_mm"));
                    df.setDepthMm(rs.getBigDecimal("depth_mm"));
                    df.setHeightMm(rs.getBigDecimal("height_mm"));
                    df.setVolumeCm3(rs.getBigDecimal("volume_cm3"));
                    df.setSurfaceAreaCm2(rs.getBigDecimal("surface_area_cm2"));
                    long triangles = rs.getLong("triangle_count");
                    df.setTriangleCount(rs.wasNull() ? null : triangles);
                    df.setAnalysisMessage(rs.getString("analysis_message"));
                    return Optional.of(df);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    private static void setMeasurement(PreparedStatement ps, int index, double value) throws SQLException {
        if (!Double.isFinite(value) || value <= 0) ps.setNull(index, Types.DECIMAL);
        else ps.setBigDecimal(index, java.math.BigDecimal.valueOf(value));
    }
}
