package com.expense.util;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    private static final String DEFAULT_DB_PATH = "D:/DIVY/webDev/Projects/FinSight/data/finsight.db";
    private static volatile boolean schemaInitialized = false;

    public static String getDatabasePath() {
        String envPath = System.getenv("SQLITE_DB_PATH");
        if (envPath != null && !envPath.trim().isEmpty()) {
            return envPath.trim();
        }

        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && envUrl.startsWith("jdbc:sqlite:")) {
            return envUrl.substring("jdbc:sqlite:".length()).trim();
        }

        return DEFAULT_DB_PATH;
    }

    public static Connection getConnection() {
        try {
            Class.forName("org.sqlite.JDBC");

            String dbPath = getDatabasePath();
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            Connection con = DriverManager.getConnection(url);

            // Enable SQLite foreign key constraints per connection
            try (Statement stmt = con.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }

            // Auto-initialize schema once if database tables do not exist
            if (!schemaInitialized) {
                synchronized (DBConnection.class) {
                    if (!schemaInitialized) {
                        initializeSchema(con);
                        schemaInitialized = true;
                    }
                }
            }

            return con;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void initializeSchema(Connection con) {
        try (Statement stmt = con.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("CREATE TABLE IF NOT EXISTS user ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "email TEXT NOT NULL UNIQUE, "
                    + "password TEXT NOT NULL);");

            stmt.execute("CREATE TABLE IF NOT EXISTS transactions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "type TEXT NOT NULL CHECK(type IN ('Income', 'Expense')), "
                    + "category TEXT, "
                    + "amount REAL NOT NULL, "
                    + "description TEXT, "
                    + "transaction_date TEXT NOT NULL, "
                    + "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE);");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date);");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
