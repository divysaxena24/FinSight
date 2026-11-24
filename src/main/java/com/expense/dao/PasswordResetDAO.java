package com.expense.dao;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;

import org.mindrot.jbcrypt.BCrypt;

import com.expense.util.DBConnection;

public class PasswordResetDAO {

    public enum ResetResult {
        SUCCESS,
        INVALID_OR_EXPIRED,
        FAILURE
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long TOKEN_LIFETIME_SECONDS = 30 * 60;

    public String createToken(int userId) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = sha256(rawToken);
        long now = Instant.now().getEpochSecond();

        String invalidateSql = "UPDATE password_reset_tokens SET used_at = ? "
                + "WHERE user_id = ? AND used_at IS NULL";
        String insertSql = "INSERT INTO password_reset_tokens "
                + "(user_id, token_hash, expires_at, used_at, created_at) VALUES (?, ?, ?, NULL, ?)";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return null;

            con.setAutoCommit(false);
            try (PreparedStatement invalidate = con.prepareStatement(invalidateSql);
                 PreparedStatement insert = con.prepareStatement(insertSql)) {
                invalidate.setLong(1, now);
                invalidate.setInt(2, userId);
                invalidate.executeUpdate();

                insert.setInt(1, userId);
                insert.setString(2, tokenHash);
                insert.setLong(3, now + TOKEN_LIFETIME_SECONDS);
                insert.setLong(4, now);
                insert.executeUpdate();
                con.commit();
                return rawToken;
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                return null;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return false;

        String sql = "SELECT 1 FROM password_reset_tokens "
                + "WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return false;
            ps.setString(1, sha256(rawToken));
            ps.setLong(2, Instant.now().getEpochSecond());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ResetResult resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()
                || newPassword == null || newPassword.length() < 6) {
            return ResetResult.FAILURE;
        }

        String tokenHash = sha256(rawToken);
        long now = Instant.now().getEpochSecond();
        String findSql = "SELECT id, user_id FROM password_reset_tokens "
                + "WHERE token_hash = ? AND used_at IS NULL AND expires_at > ?";
        String updatePasswordSql = "UPDATE user SET password = ? WHERE id = ?";
        String consumeSql = "UPDATE password_reset_tokens SET used_at = ? "
                + "WHERE id = ? AND used_at IS NULL";
        String invalidateSql = "UPDATE password_reset_tokens SET used_at = ? "
                + "WHERE user_id = ? AND used_at IS NULL";

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) return ResetResult.FAILURE;

            con.setAutoCommit(false);
            try (PreparedStatement find = con.prepareStatement(findSql)) {
                find.setString(1, tokenHash);
                find.setLong(2, now);

                int tokenId;
                int userId;
                try (ResultSet rs = find.executeQuery()) {
                    if (!rs.next()) {
                        con.rollback();
                        return ResetResult.INVALID_OR_EXPIRED;
                    }
                    tokenId = rs.getInt("id");
                    userId = rs.getInt("user_id");
                }

                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
                try (PreparedStatement updatePassword = con.prepareStatement(updatePasswordSql);
                     PreparedStatement consume = con.prepareStatement(consumeSql);
                     PreparedStatement invalidate = con.prepareStatement(invalidateSql)) {
                    updatePassword.setString(1, hashedPassword);
                    updatePassword.setInt(2, userId);
                    if (updatePassword.executeUpdate() != 1) {
                        con.rollback();
                        return ResetResult.FAILURE;
                    }

                    consume.setLong(1, now);
                    consume.setInt(2, tokenId);
                    consume.executeUpdate();

                    invalidate.setLong(1, now);
                    invalidate.setInt(2, userId);
                    invalidate.executeUpdate();
                }

                con.commit();
                return ResetResult.SUCCESS;
            } catch (SQLException e) {
                con.rollback();
                e.printStackTrace();
                return ResetResult.FAILURE;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResetResult.FAILURE;
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String part = Integer.toHexString(b & 0xff);
                if (part.length() == 1) hex.append('0');
                hex.append(part);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
