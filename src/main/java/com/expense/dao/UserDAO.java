package com.expense.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.mindrot.jbcrypt.BCrypt;

import com.expense.model.User;
import com.expense.util.DBConnection;

public class UserDAO {

    // Register User with BCrypt password hashing
    public boolean registerUser(User user) {
        if (user == null || user.getEmail() == null || user.getPassword() == null) {
            return false;
        }

        String sql = "INSERT INTO user(name, email, password) VALUES(?, ?, ?)";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con != null ? con.prepareStatement(sql) : null) {

            if (con == null || ps == null) {
                return false;
            }

            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12));

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, hashedPassword);

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

        String sql = "SELECT id, name, email, password FROM user WHERE email = ?";

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
                        User user = new User();
                        user.setId(rs.getInt("id"));
                        user.setName(rs.getString("name"));
                        user.setEmail(rs.getString("email"));
                        return user;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
