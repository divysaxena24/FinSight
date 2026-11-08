# FinSight — AI Assistant & Developer Handoff Context

**Project Location:** `D:\DIVY\webDev\Projects\FinSight`  
**Primary Language & Stack:** Java 17, Jakarta Servlets 6.0, GlassFish JSTL 3.0, SQLite 3.45.3 (JDBC), Apache Tomcat 10.1, Apache PDFBox 3.0.3, jBCrypt 0.4  
**Date of Context Snapshot:** 2026-11-08  
**Total Automated Tests:** 177 Tests (21 Classes, 0 Failures, 0 Errors, BUILD SUCCESS)  
**Production Database Path:** `data/finsight.db` (SHA-256: `235E255563F4AE5FC749AD5824156C49BF48A6C45C60E46DCB92E2605518294C`)  

---

## 1. ABSOLUTE RULES FOR FUTURE AI ASSISTANTS

1. **NEVER RESET OR CORRUPT `data/finsight.db`:**
   - Any test or experimental script must configure an isolated temporary database path via `DBConnection.setThreadDatabasePath(...)` or `-Dsqlite.db.path=...`.
   - Never run destructive SQL (`DROP TABLE`, `DELETE FROM`) against production.
2. **MAINTAIN MONETARY INVARIANTS:**
   - Every financial operation in the simulator must write balanced double-entry ledger entries ($\sum \text{Debits} = \sum \text{Credits}$).
   - Deposits must debit `FS-SYSTEM-CLEARING` and credit the customer account.
   - Withdrawals must debit the customer account and credit `FS-SYSTEM-CLEARING`.
   - Transfers must debit the sender and credit the recipient.
3. **DO NOT MIGRATE TO SPRING BOOT OR REACT:**
   - FinSight is intentionally built as a lightweight, robust Jakarta Servlet / JSP / JDBC / SQLite monolithic application. Preserve this architecture.
4. **LOCAL TOMCAT DEPLOYMENT:**
   - Tomcat 10.1 is located at `C:\tools\apache-tomcat-10.1`.
   - Application WAR is deployed to `C:\tools\apache-tomcat-10.1\webapps\FinSight.war`.
   - Access URL: `http://localhost:8080/FinSight/`.

---

## 2. REPOSITORY & PACKAGE STRUCTURE

```
src/
├── main/
│   ├── java/
│   │   ├── com/expense/
│   │   │   ├── controller/      # Servlets: LoginServlet, RegisterServlet, DashboardServlet, ViewTransactionServlet
│   │   │   ├── dao/             # UserDAO (BCrypt auth & lockout), TransactionDAO (Personal/statement records)
│   │   │   ├── model/           # User, Transaction, UnifiedTransactionItem
│   │   │   ├── service/         # TransactionService (Summaries, date range resolution, chart datasets)
│   │   │   └── util/            # DBConnection (SQLite connection pool, WAL mode, busy_timeout, schema init)
│   │   └── com/finsight/
│   │       ├── controller/      # StatementAnalysisServlet (PDF upload, staging review, approved import)
│   │       ├── engine/          # StatementAnalysisEngine, SecurePdfExtractor, GenericBankStatementParser
│   │       ├── security/        # CsrfTokenManager (Per-session anti-CSRF token verification)
│   │       └── simulator/
│   │           ├── controller/  # AccountsServlet, BankingOperationsServlet, FinancialIntelligenceServlet
│   │           ├── dao/         # AccountDAO, LedgerDAO, IntelligenceDAO
│   │           ├── model/       # Account, SimulatorTransaction, LedgerEntry, CategoryBudget, AnomalyAlert
│   │           └── service/     # FinancialIntelligenceService (Health score, baseline spending, budget engine)
│   └── webapp/
│       ├── css/style.css        # Clean responsive modern CSS design system
│       └── WEB-INF/views/       # 20 JSP views (dashboard, accounts, deposit, withdraw, transfer, receipts, etc.)
└── test/
    └── java/                    # 21 test classes (177 tests) covering all banking, PDF, and intelligence flows
```

---

## 3. KEY ARCHITECTURAL & CODING CONVENTIONS

1. **Database Connections:**
   - Obtain connection via `DBConnection.getConnection()`.
   - Always close connections in `try-with-resources`.
   - For multi-step financial operations, explicitly use `con.setAutoCommit(false)` and `con.commit()` with `con.rollback()` in the `catch` block.
2. **Idempotency Implementation:**
   - Web forms embed a unique UUID `idempotencyKey` hidden parameter per page load.
   - `LedgerDAO` inspects `simulator_transactions.idempotency_key`:
     - If key exists with identical payload: returns cached original transaction.
     - If key exists with different payload: returns `Conflict: Idempotency key already used with differing transaction payload`.
3. **Session Security:**
   - In `LoginServlet`, upon successful authentication, call `request.changeSessionId()` before setting `session.setAttribute("user", user)`.
4. **Receipt IDOR Protection:**
   - When fetching transactions in `BankingOperationsServlet`, verify that the transaction's source or destination account ID belongs to `user.getId()`.

---

## 4. COMMAND CHEAT SHEET

- **Run Full Automated Test Suite (Isolated Test DB):**
  ```cmd
  C:\tools\apache-maven-3.9.9\bin\mvn.cmd test -Dsqlite.db.path=target/test-data/isolated.db
  ```
- **Package Application WAR:**
  ```cmd
  C:\tools\apache-maven-3.9.9\bin\mvn.cmd clean package -DskipTests
  ```
- **Deploy to Tomcat:**
  ```powershell
  Copy-Item target\FinSight.war C:\tools\apache-tomcat-10.1\webapps\FinSight.war -Force
  ```
- **Verify Production Database SHA-256:**
  ```powershell
  Get-FileHash 'data\finsight.db' -Algorithm SHA256
  ```

---

## 5. IMMEDIATE NEXT TASKS (RECOMMENDED ORDER)

1. **Beneficiary Directory (P1):** Create `beneficiaries` table (`user_id`, `nickname`, `account_number`) and add selector dropdown in `transfer.jsp`.
2. **Account Statement CSV & PDF Export (P1):** Add an export servlet generating downloadable CSV/PDF statement files from `viewTransaction.jsp`.
3. **Account Lifecycle Controls (P1):** Expose "Freeze Account" and "Unfreeze Account" action buttons on `accounts.jsp`.
4. **Integer Paise Schema Migration (P2):** Migrate SQLite currency columns from `REAL` to integer `paise` using the tested additive migration plan.

