<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Add Transaction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">
            💰 FinSight
        </a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/addTransactionPage" style="color: var(--primary);">+ Add Transaction</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container">
        <div class="form-card">
            <div style="text-align: center; margin-bottom: 2rem;">
                <h2 style="font-size: 1.6rem; color: var(--dark);">Add Transaction</h2>
                <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 0.25rem;">
                    Record a new income or expense entry
                </p>
            </div>

            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-danger">
                    <span><c:out value="${sessionScope.errorMessage}"/></span>
                </div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>

            <form action="${pageContext.request.contextPath}/addTransaction" method="post">
                <div class="form-group">
                    <label for="type">Transaction Type</label>
                    <select id="type" name="type" class="form-control" required>
                        <option value="Expense" selected>Expense (Money Out)</option>
                        <option value="Income">Income (Money In)</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="category">Category</label>
                    <input type="text" id="category" name="category" class="form-control" 
                           placeholder="e.g. Food, Groceries, Salary, Rent" required>
                </div>

                <div class="form-group">
                    <label for="amount">Amount (₹)</label>
                    <input type="number" step="0.01" min="0.01" id="amount" name="amount" class="form-control" 
                           placeholder="0.00" required>
                </div>

                <div class="form-group">
                    <label for="description">Description (Optional)</label>
                    <textarea id="description" name="description" class="form-control" 
                              placeholder="Add notes or merchant details..."></textarea>
                </div>

                <div class="form-group">
                    <label for="transactionDate">Date</label>
                    <input type="date" id="transactionDate" name="transactionDate" class="form-control" required>
                </div>

                <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 1rem;">
                    Save Transaction
                </button>
            </form>

            <div style="text-align: center; margin-top: 1.5rem;">
                <a href="${pageContext.request.contextPath}/dashboard" style="color: var(--text-muted); font-size: 0.9rem; text-decoration: none;">
                    ← Cancel and return to Dashboard
                </a>
            </div>
        </div>
    </div>

    <script>
        // Default date picker to today's date if not set
        document.getElementById('transactionDate').valueAsDate = new Date();
    </script>
</body>
</html>

