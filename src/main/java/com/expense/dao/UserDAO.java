package com.expense.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.mindrot.jbcrypt.BCrypt;

import com.expense.model.User;
import com.expense.util.DBConnection;

public class UserDAO {

    private static final int MAX_PIN_ATTEMPTS = 3;
    private static final int LOCKOUT_MINUTES = 15;
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Register User with BCrypt password hashing & optional phone and PIN
    public boolean registerUser(User user) {
        if (user == null || user.getEmail() == null || user.getPassword() == null) {
            return false;
        }

        String sql = "INSERT INTO user(name, email, password, phone, transaction_pin) VALUES(?, ?, ?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return false;
            }

            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));
            String hashedPin = null;
            if (user.getTransactionPin() != null && !user.getTransactionPin().trim().isEmpty()) {
                hashedPin = BCrypt.hashpw(user.getTransactionPin().trim(), BCrypt.gensalt(10));
            }

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, hashedPassword);
            ps.setString(4, user.getPhone() != null ? user.getPhone().trim() : null);
            ps.setString(5, hashedPin);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            // Catches SQLite unique constraint violation (SQLITE_CONSTRAINT_UNIQUE)
            e.printStackTrace();
            return false;
        }
    }

    // Login User with BCrypt verification (and plain text backward-compatibility fallback)
    public User loginUser(String email, String password) {
        if (email == null || password == null) {
            return null;
        }

        String sql = "SELECT id, name, email, password, phone, transaction_pin, pin_attempts, pin_locked_until FROM user WHERE email = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return null;
            }

            ps.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    boolean passwordMatches = false;

                    if (storedPassword != null && storedPassword.startsWith("$2a$")) {
                        try {
                            passwordMatches = BCrypt.checkpw(password, storedPassword);
                        } catch (Exception ex) {
                            passwordMatches = false;
                        }
                    } else if (storedPassword != null && storedPassword.equals(password)) {
                        // Plaintext fallback
                        passwordMatches = true;
                    }

                    if (passwordMatches) {
                        return mapUser(rs);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public User getUserById(int userId) {
        String sql = "SELECT id, name, email, password, phone, transaction_pin, pin_attempts, pin_locked_until FROM user WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return null;
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public User getUserByEmail(String email) {
        if (email == null) return null;
        String sql = "SELECT id, name, email, password, phone, transaction_pin, pin_attempts, pin_locked_until FROM user WHERE email = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return null;
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUser(rs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateProfile(int userId, String name, String phone) {
        String sql = "UPDATE user SET name = ?, phone = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return false;
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setInt(3, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePassword(int userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user == null) return false;

        boolean matches = false;
        if (user.getPassword() != null && user.getPassword().startsWith("$2a$")) {
            matches = BCrypt.checkpw(oldPassword, user.getPassword());
        } else if (user.getPassword() != null) {
            matches = user.getPassword().equals(oldPassword);
        }

        if (!matches) return false;

        String hashedNew = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
        String sql = "UPDATE user SET password = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return false;
            ps.setString(1, hashedNew);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setTransactionPin(int userId, String rawPin) {
        if (rawPin == null || !rawPin.matches("^\\d{4,6}$")) {
            return false;
        }
        String hashedPin = BCrypt.hashpw(rawPin.trim(), BCrypt.gensalt(10));
        String sql = "UPDATE user SET transaction_pin = ?, pin_attempts = 0, pin_locked_until = NULL WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con == null || ps == null) return false;
            ps.setString(1, hashedPin);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean changeTransactionPin(int userId, String currentPin, String newPin) {
        if (!verifyTransactionPin(userId, currentPin).isSuccess()) {
            return false;
        }
        return setTransactionPin(userId, newPin);
    }

    public static class PinVerificationResult {
        private final boolean success;
        private final boolean locked;
        private final String message;
        private final int remainingAttempts;

        public PinVerificationResult(boolean success, boolean locked, String message, int remainingAttempts) {
            this.success = success;
            this.locked = locked;
            this.message = message;
            this.remainingAttempts = remainingAttempts;
        }

        public boolean isSuccess() { return success; }
        public boolean isLocked() { return locked; }
        public String getMessage() { return message; }
        public int getRemainingAttempts() { return remainingAttempts; }
    }

    public PinVerificationResult verifyTransactionPin(int userId, String rawPin) {
        User user = getUserById(userId);
        if (user == null) {
            return new PinVerificationResult(false, false, "User not found.", 0);
        }

        if (!user.hasTransactionPin()) {
            return new PinVerificationResult(false, false, "Transaction PIN has not been set yet. Please set your PIN in Security settings.", 0);
        }

        LocalDateTime now = LocalDateTime.now();
        // Check if locked
        if (user.getPinLockedUntil() != null && !user.getPinLockedUntil().trim().isEmpty()) {
            try {
                LocalDateTime lockedUntil = LocalDateTime.parse(user.getPinLockedUntil(), ISO_FMT);
                if (now.isBefore(lockedUntil)) {
                    long minutesLeft = java.time.Duration.between(now, lockedUntil).toMinutes() + 1;
                    return new PinVerificationResult(false, true, "PIN verification is temporarily locked. Try again in " + minutesLeft + " minute(s).", 0);
                } else {
                    // Lockout expired, reset attempts
                    resetPinAttempts(userId);
                    user.setPinAttempts(0);
                }
            } catch (Exception ignored) {
                resetPinAttempts(userId);
            }
        }

        boolean matches = false;
        try {
            matches = BCrypt.checkpw(rawPin, user.getTransactionPin());
        } catch (Exception e) {
            matches = false;
        }

        if (matches) {
            resetPinAttempts(userId);
            return new PinVerificationResult(true, false, "PIN verified successfully.", MAX_PIN_ATTEMPTS);
        } else {
            int newAttempts = user.getPinAttempts() + 1;
            if (newAttempts >= MAX_PIN_ATTEMPTS) {
                LocalDateTime lockUntil = now.plusMinutes(LOCKOUT_MINUTES);
                recordPinLockout(userId, newAttempts, lockUntil.format(ISO_FMT));
                return new PinVerificationResult(false, true, "Incorrect PIN. Account PIN verification is now locked for " + LOCKOUT_MINUTES + " minutes.", 0);
            } else {
                recordPinAttempt(userId, newAttempts);
                int remaining = MAX_PIN_ATTEMPTS - newAttempts;
                return new PinVerificationResult(false, false, "Incorrect PIN. " + remaining + " attempt(s) remaining.", remaining);
            }
        }
    }

    public void resetPinAttempts(int userId) {
        String sql = "UPDATE user SET pin_attempts = 0, pin_locked_until = NULL WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con != null && ps != null) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recordPinAttempt(int userId, int attempts) {
        String sql = "UPDATE user SET pin_attempts = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con != null && ps != null) {
                ps.setInt(1, attempts);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recordPinLockout(int userId, int attempts, String lockedUntil) {
        String sql = "UPDATE user SET pin_attempts = ?, pin_locked_until = ? WHERE id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {
            if (con != null && ps != null) {
                ps.setInt(1, attempts);
                ps.setString(2, lockedUntil);
                ps.setInt(3, userId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private User mapUser(ResultSet rs) throws Exception {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        try {
            user.setPhone(rs.getString("phone"));
            user.setTransactionPin(rs.getString("transaction_pin"));
            user.setPinAttempts(rs.getInt("pin_attempts"));
            user.setPinLockedUntil(rs.getString("pin_locked_until"));
            user.setCreatedAt(rs.getString("created_at"));
        } catch (Exception ignored) {
            // In case columns were not in projection
        }
        return user;
    }
}
