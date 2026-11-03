package vn.edu.hcmuaf.fit.tmdt_nhomle.dao;

import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.Database;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.DesignFile;

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

    public static long insert(Long userId, Long productId, String originalFilename,
                              String storageUrl, String previewUrl, String status) {
        try (Connection conn = Database.getConnection()) {
            return insert(conn, userId, productId, originalFilename, storageUrl, previewUrl, status);
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }

    public static Optional<DesignFile> findById(long designFileId) {
        String sql = "SELECT design_file_id, user_id, product_id, original_filename, storage_url, preview_url, status, created_at"
                + " FROM design_files WHERE design_file_id = ?";
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
                    return Optional.of(df);
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw DataAccessException.of(e);
        }
    }
}
