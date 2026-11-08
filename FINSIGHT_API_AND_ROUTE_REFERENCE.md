# FinSight — API & Route Reference Manual

**Auditor:** Principal Fintech API Architect & Technical Auditor  
**Application Context Path:** `/FinSight`  
**Container:** Apache Tomcat 10.1 (Jakarta EE 10 / Servlet 6.0)  

---

## 1. ENDPOINT ROUTING INVENTORY

| Route / Pattern | Servlet Class | HTTP Methods | Auth Required | CSRF Required | Parameters & Description |
|---|---|---|---|---|---|
| `/login`, `/login.jsp` | `LoginServlet` | GET, POST | No | No | `email`, `password`. Authenticates user, rotates session ID (`request.changeSessionId()`), and redirects to `/dashboard`. |
| `/register`, `/register.jsp`| `RegisterServlet` | GET, POST | No | No | `name`, `email`, `password`, `phone`, `transactionPin`. Creates new user account. |
| `/logout` | `LogoutServlet` | GET | Yes | No | Invalidates `HttpSession` and redirects to `/login`. |
| `/dashboard`, `/dashboard.jsp`| `DashboardServlet` | GET | Yes | No | `range`, `startDate`, `endDate`. Loads simulator balance, recent transactions, trend line chart JSON, and category doughnut chart JSON. |
| `/accounts` | `AccountsServlet` | GET, POST | Yes | POST: Yes | **GET:** `id` (optional). Lists user's accounts, calculates total balance, and runs ledger reconciliation for selected account.<br/>**POST:** `action=create`, `accountType` (`SAVINGS`/`CURRENT`). Creates new account. |
| `/banking` | `BankingOperationsServlet` | GET, POST | Yes | POST: Yes | **GET:** `action` (`deposit`, `withdraw`, `transfer`, `receipt`). Renders corresponding view.<br/>**POST:** Executes deposit, withdrawal, or transfer with PIN verification, idempotency checking, and double-entry ledger posting. |
| `/viewTransaction`, `/viewTransaction.jsp` | `ViewTransactionServlet` | GET | Yes | No | Unifies simulator transfers/deposits with statement/manual expense records into a single sorted chronological list with client-side filter pills. |
| `/budgets` | `FinancialIntelligenceServlet` | GET, POST | Yes | POST: Yes | **GET:** Displays current monthly budgets and smart recommendations.<br/>**POST:** `category`, `monthlyLimit`, `monthYear`. Persists/updates monthly budget cap. |
| `/alerts` | `FinancialIntelligenceServlet` | GET, POST | Yes | POST: Yes | **GET:** Lists anomaly alerts.<br/>**POST:** `alertId`. Acknowledges an anomaly alert. |
| `/health` | `FinancialIntelligenceServlet` | GET | Yes | No | Renders holistic 100-point financial health scorecard and actionable recommendations. |
| `/statement-analysis` | `StatementAnalysisServlet` | GET, POST | Yes | POST: Yes | **GET:** `action` (`upload`, `review`, `report`).<br/>**POST:** Uploads PDF statements, updates staged transaction rows, and atomically imports user-approved transactions. |
| `/profile` | `ProfileServlet` | GET, POST | Yes | POST: Yes | **GET:** Renders user security and profile settings.<br/>**POST:** `action` (`updateProfile`, `changePassword`, `setPin`, `changePin`). Manages credentials and PINs. |
| `/addTransaction`, `/addTransactionPage` | `AddTransactionServlet`, `AddTransactionPageServlet` | GET, POST | Yes | POST: Yes | Manually records a personal expense or income entry. |
| `/editTransaction`, `/updateTransaction` | `EditTransactionServlet`, `UpdateTransactionServlet` | GET, POST | Yes | POST: Yes | Fetches and updates an existing manual expense/income transaction. |
| `/deleteTransaction` | `DeleteTransactionServlet` | POST | Yes | Yes | `id`. Deletes an existing manual expense record. |

---

## 2. DETAILED ENDPOINT BEHAVIOR & SECURITY CHECKS

### 2.1 `/banking` (Banking Operations)
- **POST Action `transfer`:**
  - **Required Parameters:** `sourceAccountId` (int), `recipientAccountNumber` (string), `amount` (decimal), `transactionPin` (string), `csrfToken` (string), `idempotencyKey` (UUID).
  - **Ownership Check:** Evaluates `sourceAccount.getUserId() == sessionUser.getId()`. If violated, aborts with `"Source account not found or access denied."`.
  - **PIN Lockout:** Checked via `UserDAO.verifyTransactionPin()`. 3 failed attempts lock account for 15 minutes.
  - **Double-Entry Execution:** Calls `LedgerDAO.executeTransfer()`, which debits sender, credits recipient, and inserts balanced ledger entries.
- **GET Action `receipt` (`/banking?action=receipt&ref=TRF-...`):**
  - **IDOR Defense:** Validates that either `sourceAccountId` or `destinationAccountId` belongs to the requesting user's accounts (`BankingOperationsServlet.java:68-81`). If an unauthorized user tries to view another user's receipt by guessing the reference ID, the server returns `HTTP 403 Forbidden`.

### 2.2 `/statement-analysis` (Bank Statement Pipeline)
- **Multipart Config:** Configured on `StatementAnalysisServlet`:
  - `fileSizeThreshold = 1 MB`
  - `maxFileSize = 10 MB`
  - `maxRequestSize = 12 MB`
- **Actions:**
  - `action=upload`: Accepts PDF statement, runs PDFBox line extraction, parses transactions via `StatementParserRouter`, applies category rules via `RuleBasedTransactionCategorizer`, and stages them in `StagingManager`.
  - `action=import`: Accepts user-checked indices and approved edits, passing them to `StatementImportService.importTransactions()` for atomic batch insert.

