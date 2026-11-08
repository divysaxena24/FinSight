package com.expense;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import com.expense.util.DBConnection;

/**
 * Verifies that test database operations remain isolated from the production database.
 */
public class DatabaseIsolationTest {

    @Test
    public void testProductionDatabasePreservationDuringIsolatedTestOperations() throws Exception {
        // 1. Verify production database path resolution without thread override
        String defaultPath = DBConnection.getDatabasePath();
        assertNotNull(defaultPath);

        // 2. Point thread local override to isolated temp database
        File testDir = new File("target/test-data");
        if (!testDir.exists()) {
            testDir.mkdirs();
        }
        File isolatedDb = new File(testDir, "isolation-verification-" + System.currentTimeMillis() + ".db");
        DBConnection.setThreadDatabasePath(isolatedDb.getAbsolutePath());

        try {
            assertEquals(isolatedDb.getAbsolutePath(), DBConnection.getDatabasePath());

            // Open connection to isolated DB and execute write/delete
            try (Connection con = DBConnection.getConnection();
                 Statement stmt = con.createStatement()) {
                assertNotNull(con);
                stmt.execute("INSERT INTO user(name, email, password) VALUES('IsoUser', 'isouser@test.com', 'pwd');");
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user WHERE email = 'isouser@test.com';")) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }

                // Simulate destructive statement that previously wiped production
                stmt.execute("DELETE FROM user;");
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM user;")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1));
                }
            }
        } finally {
            DBConnection.clearThreadDatabasePath();
            if (isolatedDb.exists()) {
                isolatedDb.delete();
            }
        }

        // Verify thread local path was restored
        assertFalse(isolatedDb.getAbsolutePath().equals(DBConnection.getDatabasePath()));
    }
}

