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
    private static final ThreadLocal<String> threadLocalDbPath = new ThreadLocal<>();
    private static final java.util.Set<String> initializedDatabases = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /**
     * Allows tests to set a thread-isolated database path.
     */
    public static void setThreadDatabasePath(String path) {
        if (path == null) {
            threadLocalDbPath.remove();
        } else {
            threadLocalDbPath.set(path);
        }
    }

    /**
     * Clears any thread-isolated database override.
     */
    public static void clearThreadDatabasePath() {
        threadLocalDbPath.remove();
    }

    public static String getDatabasePath() {
        // 1. Thread-local override (ideal for fine-grained test isolation)
        String threadPath = threadLocalDbPath.get();
        if (threadPath != null && !threadPath.trim().isEmpty()) {
            return threadPath.trim();
        }

        // 2. JVM System property (ideal for maven surefire isolation, e.g. -Dsqlite.db.path=...)
        String propPath = System.getProperty("sqlite.db.path");
        if (propPath != null && !propPath.trim().isEmpty()) {
            return propPath.trim();
        }

        // 3. Environment variable override
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

            // org.sqlite.SQLiteConfig allows setting busy_timeout and journal_mode at native driver config level
            org.sqlite.SQLiteConfig config = new org.sqlite.SQLiteConfig();
            config.enforceForeignKeys(true);
            config.setBusyTimeout(10000); // 10 second timeout for lock acquisition
            config.setJournalMode(org.sqlite.SQLiteConfig.JournalMode.WAL);
            config.setSynchronous(org.sqlite.SQLiteConfig.SynchronousMode.NORMAL);

            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            Connection con = config.createConnection(url);

            // Auto-initialize schema per distinct database path if tables do not exist
            String normalizedKey = dbFile.getAbsolutePath();
            if (!initializedDatabases.contains(normalizedKey)) {
                synchronized (initializedDatabases) {
                    if (!initializedDatabases.contains(normalizedKey)) {
                        initializeSchema(con);
                        initializedDatabases.add(normalizedKey);
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

            // 1. Core user table
            stmt.execute("CREATE TABLE IF NOT EXISTS user ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name TEXT NOT NULL, "
                    + "email TEXT NOT NULL UNIQUE, "
                    + "password TEXT NOT NULL);");

            // Ensure system user 0 exists for institutional accounts
            stmt.execute("INSERT OR IGNORE INTO user (id, name, email, password) "
                    + "VALUES (0, 'FinSight System', 'system@finsight.internal', 'SYSTEM_INTERNAL_ACCOUNT');");

            // Non-destructive additions to user table
            addColumnIfNotExists(stmt, "user", "phone", "TEXT");
            addColumnIfNotExists(stmt, "user", "transaction_pin", "TEXT");
            addColumnIfNotExists(stmt, "user", "pin_attempts", "INTEGER DEFAULT 0");
            addColumnIfNotExists(stmt, "user", "pin_locked_until", "TEXT");
            addColumnIfNotExists(stmt, "user", "created_at", "TEXT");

            // 2. Core legacy/imported transactions table
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
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_tx_user_date_type ON transactions(user_id, transaction_date, type);");

            // 3. Simulated Banking Accounts
            stmt.execute("CREATE TABLE IF NOT EXISTS accounts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "account_number TEXT NOT NULL UNIQUE, "
                    + "account_type TEXT NOT NULL CHECK(account_type IN ('SAVINGS', 'CURRENT', 'CLEARING')), "
                    + "balance REAL NOT NULL DEFAULT 0.00, "
                    + "status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE', 'INACTIVE', 'LOCKED')), "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE);");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_accounts_account_number ON accounts(account_number);");

            // Ensure system clearing account exists
            stmt.execute("INSERT OR IGNORE INTO accounts (id, user_id, account_number, account_type, balance, status) "
                    + "VALUES (999999, 0, 'FS-SYSTEM-CLEARING', 'CLEARING', 0.00, 'ACTIVE');");

            // 4. Simulator Transactions (High-level financial transaction records)
            stmt.execute("CREATE TABLE IF NOT EXISTS simulator_transactions ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "reference_number TEXT NOT NULL UNIQUE, "
                    + "transaction_type TEXT NOT NULL CHECK(transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')), "
                    + "amount REAL NOT NULL, "
                    + "description TEXT, "
                    + "category TEXT, "
                    + "status TEXT NOT NULL DEFAULT 'COMPLETED' CHECK(status IN ('PENDING', 'COMPLETED', 'FAILED', 'REVERSED')), "
                    + "source_account_id INTEGER, "
                    + "destination_account_id INTEGER, "
                    + "idempotency_key TEXT UNIQUE, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (source_account_id) REFERENCES accounts(id), "
                    + "FOREIGN KEY (destination_account_id) REFERENCES accounts(id));");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sim_tx_ref ON simulator_transactions(reference_number);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sim_tx_src ON simulator_transactions(source_account_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_sim_tx_dst ON simulator_transactions(destination_account_id);");

            // 5. Double-Entry Ledger Entries (Strict auditable immutable ledger)
            // Note: Restrictive cascade behavior to protect historical ledger entries
            stmt.execute("CREATE TABLE IF NOT EXISTS ledger_entries ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "transaction_id INTEGER NOT NULL, "
                    + "account_id INTEGER NOT NULL, "
                    + "entry_type TEXT NOT NULL CHECK(entry_type IN ('DEBIT', 'CREDIT')), "
                    + "amount REAL NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (transaction_id) REFERENCES simulator_transactions(id) ON DELETE RESTRICT, "
                    + "FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT);");

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ledger_tx_id ON ledger_entries(transaction_id);");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ledger_acc_id ON ledger_entries(account_id);");

            // 6. User Budgets
            stmt.execute("CREATE TABLE IF NOT EXISTS budgets ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "category TEXT NOT NULL, "
                    + "monthly_limit REAL NOT NULL, "
                    + "month_year TEXT NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE(user_id, category, month_year), "
                    + "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE);");

            // 7. Anomaly Alerts
            stmt.execute("CREATE TABLE IF NOT EXISTS anomaly_alerts ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "transaction_ref TEXT NOT NULL, "
                    + "severity TEXT NOT NULL, "
                    + "title TEXT NOT NULL, "
                    + "reason TEXT NOT NULL, "
                    + "risk_score REAL NOT NULL, "
                    + "status TEXT NOT NULL DEFAULT 'NEW' CHECK(status IN ('NEW', 'ACKNOWLEDGED')), "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE);");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addColumnIfNotExists(Statement stmt, String tableName, String columnName, String columnDef) {
        try {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDef + ";");
        } catch (SQLException ignored) {
            // Column already exists in SQLite
        }
    }
}
