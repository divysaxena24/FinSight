package com.expense.util;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/expense_db";
    private static final String DEFAULT_USER = "root";

    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            String envUrl = System.getenv("DB_URL");
            String url = (envUrl != null && !envUrl.trim().isEmpty()) ? envUrl : DEFAULT_URL;

            String envUser = System.getenv("DB_USERNAME");
            String user = (envUser != null && !envUser.trim().isEmpty()) ? envUser : DEFAULT_USER;

            String password = System.getenv("DB_PASSWORD");
            if (password == null) {
                password = "";
            }

            return DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
