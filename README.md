# 💰 FinSight — Digital Banking Simulator & Personal Finance Intelligence Platform

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-17%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Servlet](https://img.shields.io/badge/Jakarta%20Servlet-6.0-blue.svg)](https://jakarta.ee/specifications/servlet/6.0/)
[![Database](https://img.shields.io/badge/SQLite-3.45.3%20(WAL)-lightgrey.svg)](https://www.sqlite.org/)
[![Container](https://img.shields.io/badge/Apache%20Tomcat-10.1-yellow.svg)](https://tomcat.apache.org/)
[![Tests](https://img.shields.io/badge/Tests-177%20passed-success.svg)]()

FinSight is a secure, enterprise-grade digital banking simulator tightly integrated with personal finance analytics and local digital bank statement extraction. Built with Java 17, Jakarta Servlets 6.0, JSP/JSTL, JDBC, and embedded SQLite running in Write-Ahead Logging (WAL) mode.

---

## 🌟 Key Features

- **⚡ Digital Banking Simulator:**
  - Create simulated **Savings** and **Current** bank accounts with unique 13-digit account numbers (`FS-XXXXXXXXXX`).
  - Cash deposit, cash withdrawal with transaction PIN authorization, and account-to-account funds transfer.
  - Formally balanced double-entry accounting ($\sum \text{Debits} = \sum \text{Credits}$) with institutional settlement clearing (`FS-SYSTEM-CLEARING`).
  - Real-time balance reconciliation against historical immutable ledger entries.
  - Idempotency key fingerprint validation protecting against duplicate submissions or conflicting amounts.
- **🛡️ Enterprise Security:**
  - Salted BCrypt password hashing (cost factor 12) and transaction PIN hashing (cost factor 10).
  - PIN brute-force defense with a strict 3-attempt limit and 15-minute temporary lockout.
  - Per-session anti-CSRF token verification on all state-modifying requests.
  - Session fixation protection via `request.changeSessionId()` on successful authentication.
  - Insecure Direct Object Reference (IDOR) protection for transaction receipts and account data.
- **📄 Digital Bank Statement Analysis:**
  - Local digital PDF extraction via Apache PDFBox 3.0.3 with zero external LLM or cloud dependencies.
  - Rule-based transaction categorization and merchant name normalization.
  - Interactive staging and review workflow with atomic, user-approved batch database import.
- **📊 Personal Finance Intelligence:**
  - Dynamic spending baseline computation and anomaly spike alerts ($>2.5\times$ threshold).
  - Monthly budget recommendations (50/30/20 & historical spending baselines).
  - 100-point holistic Financial Health scorecard (Savings Rate, Liquidity Buffer, Diversification, Anomaly deductions).
  - Unified single-table transaction history with live search and client-side category filter pills.

---

## 🛠️ Technology Stack

- **Language:** Java 17 (LTS)
- **Web Layer:** Jakarta Servlet 6.0, JSP 3.1, GlassFish JSTL 3.0.1
- **Application Server:** Apache Tomcat 10.1 (Jakarta EE 10 compliant)
- **Persistence:** SQLite 3.45.3 (JDBC 3.45.3.0) with WAL mode & 10s `busy_timeout`
- **Security:** jBCrypt 0.4
- **PDF Extraction:** Apache PDFBox 3.0.3
- **JSON Processing:** Jackson Databind 2.17.2
- **Testing:** JUnit 5 (Jupiter 5.10.3) — 177 automated test cases
- **Frontend / UI:** Modern responsive CSS, Chart.js 4.x

---

## 📋 Prerequisites

Ensure you have the following installed on your machine:

1. **Java Development Kit (JDK) 17+**
   ```bash
   java -version
   ```
2. **Apache Maven 3.9+**
   ```bash
   mvn -version
   ```
3. **Apache Tomcat 10.1+** (Download from [tomcat.apache.org](https://tomcat.apache.org/download-10.cgi))
4. **Git**
   ```bash
   git --version
   ```

---

## 🚀 Cloning & Server Starting Guide

### Step 1: Clone the Repository

Clone the project to your local workspace:

```bash
git clone https://github.com/divysaxena24/FinSight.git
cd FinSight
```

---

### Step 2: Database Setup

FinSight uses an embedded SQLite database (`data/finsight.db`). 

- The application automatically initializes the required tables, indexes, and institutional clearing accounts (`FS-SYSTEM-CLEARING`) on its first launch.
- No external database server (like MySQL or PostgreSQL) installation is required!

---

### Step 3: Build the Application

Build the project and run the automated test suite to verify system integrity:

```bash
mvn clean test
```

Package the application into a deployable Web Application Archive (`WAR`):

```bash
mvn clean package -DskipTests
```

The compiled WAR file will be generated at:
```
target/FinSight.war
```

---

### Step 4: Deploy and Start Apache Tomcat

#### Option A: Deploy to an Installed Tomcat (Recommended)

1. **Copy the WAR file** to your Tomcat `webapps` directory:

   - **Windows (PowerShell):**
     ```powershell
     Copy-Item target\FinSight.war C:\tools\apache-tomcat-10.1\webapps\FinSight.war -Force
     ```
   - **Linux / macOS:**
     ```bash
     cp target/FinSight.war /opt/tomcat/webapps/FinSight.war
     ```

2. **Start the Tomcat Server:**

   - **Windows:**
     ```cmd
     C:\tools\apache-tomcat-10.1\bin\startup.bat
     ```
   - **Linux / macOS:**
     ```bash
     /opt/tomcat/bin/startup.sh
     ```

3. **Check Server Logs:**
   - Review `logs/catalina.out` or the console to verify successful context startup:
     ```
     INFO: Deployment of web application archive [.../FinSight.war] has finished
     ```

---

### Step 5: Access the Application

Once Tomcat is running, open your web browser and navigate to:

```
http://localhost:8080/FinSight/
```

- **New User:** Click on **"Create one here"** on the login page to register your simulated digital banking profile, set your transaction PIN, and start opening accounts.
- **Demo Access:** Register any email and password to instantly create simulated accounts and explore deposits, transfers, statements, and budget tracking.

---

### Step 6: Stopping the Server

When you are finished testing, stop Tomcat:

- **Windows:**
  ```cmd
  C:\tools\apache-tomcat-10.1\bin\shutdown.bat
  ```
- **Linux / macOS:**
  ```bash
  /opt/tomcat/bin/shutdown.sh
  ```

---

## 🧪 Running Automated Tests

All 177 unit, integration, and high-concurrency multi-threaded test cases run against isolated temporary SQLite databases without modifying production records:

```bash
# Run all tests with an isolated temporary test database
mvn test -Dsqlite.db.path=target/test-data/isolated.db
```

---

## 📂 Project Architecture

```
FinSight/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── com/expense/controller/   # Core controllers (Login, Register, Dashboard, ViewTransaction)
│   │   │   ├── com/expense/dao/          # UserDAO (BCrypt auth, PIN lockout), TransactionDAO
│   │   │   ├── com/expense/model/        # Domain entities (User, Transaction, UnifiedTransactionItem)
│   │   │   ├── com/expense/service/      # TransactionService (Summaries, date filters, analytics)
│   │   │   ├── com/expense/util/         # DBConnection (WAL mode, busy timeout, schema lifecycle)
│   │   │   ├── com/finsight/controller/  # StatementAnalysisServlet (PDF upload, review, import)
│   │   │   ├── com/finsight/engine/      # SecurePdfExtractor, StatementParserRouter, RuleCategorizer
│   │   │   ├── com/finsight/security/    # CsrfTokenManager
│   │   │   └── com/finsight/simulator/   # Digital Banking Simulator (Accounts, LedgerDAO, Intelligence)
│   │   └── webapp/
│   │       ├── css/style.css             # Responsive theme
│   │       └── WEB-INF/views/            # 20 JSP views (dashboard, accounts, transfer, receipts, etc.)
│   └── test/java/                        # 21 test classes (177 automated tests)
├── data/                                 # Embedded SQLite production database (finsight.db)
├── pom.xml                               # Maven build configuration
└── README.md                             # Documentation & Setup Guide
```

---

## 📄 License & Attribution

This project is open-source. Base CRUD foundation inspired by the open-source [ExpenseManagementSystem](https://github.com/PoojithaCoder-10/ExpenseManagementSystem) by PoojithaCoder-10. Built, enhanced, hardened, and expanded into a full-scale digital banking simulator and personal finance intelligence platform by [Divy Saxena](https://github.com/divysaxena24).
