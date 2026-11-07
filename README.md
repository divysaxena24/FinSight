# FinSight — Smart Personal Expense Analytics Platform

FinSight is a secure, analytics-driven personal expense and financial tracking platform built with Java Servlets, JSP, JDBC, and MySQL, targeting Apache Tomcat 10.1 and Java 17.

## Origin and Attribution

This project is adapted from and builds upon the open-source **[ExpenseManagementSystem](https://github.com/PoojithaCoder-10/ExpenseManagementSystem)** by [PoojithaCoder-10](https://github.com/PoojithaCoder-10).
Original architecture and base CRUD modules by PoojithaCoder-10 under the MIT / open-source spirit.

## Tech Stack
- **Target Runtime**: Apache Tomcat 10.1 (Jakarta EE 10 / Servlet 6.0)
- **Java**: Java 17+ (LTS)
- **Build Tool**: Apache Maven 3.9+
- **Database**: MySQL 8.0+

## Database Setup
1. Import schema:
   ```sql
   mysql -u root -p < database/schema.sql
   ```
2. Configure database credentials via environment variables:
   - `DB_URL` (default: `jdbc:mysql://localhost:3306/expense_db`)
   - `DB_USERNAME` (default: `root`)
   - `DB_PASSWORD` (set to your MySQL password)

## Build
```powershell
mvn clean package
```
Output artifact: `target/FinSight.war`

