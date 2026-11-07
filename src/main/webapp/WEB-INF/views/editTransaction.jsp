<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Edit Transaction</title>
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
            <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-primary btn-sm">+ Add Transaction</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container">
        <div class="form-card">
            <div style="text-align: center; margin-bottom: 2rem;">
                <h2 style="font-size: 1.6rem; color: var(--dark);">Edit Transaction</h2>
                <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 0.25rem;">
                    Update details for Transaction #<c:out value="${transaction.id}"/>
                </p>
            </div>

            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-danger">
                    <span><c:out value="${sessionScope.errorMessage}"/></span>
                </div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>

            <form action="${pageContext.request.contextPath}/updateTransaction" method="post">
                <input type="hidden" name="id" value="${transaction.id}">

                <div class="form-group">
                    <label for="type">Transaction Type</label>
                    <select id="type" name="type" class="form-control" required>
                        <option value="Income" ${transaction.type == 'Income' ? 'selected' : ''}>Income (Money In)</option>
                        <option value="Expense" ${transaction.type == 'Expense' ? 'selected' : ''}>Expense (Money Out)</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="category">Category</label>
                    <input type="text" id="category" name="category" class="form-control" 
                           value="<c:out value="${transaction.category}"/>" required>
                </div>

                <div class="form-group">
                    <label for="amount">Amount (₹)</label>
                    <input type="number" step="0.01" min="0.01" id="amount" name="amount" class="form-control" 
                           value="${transaction.amount}" required>
                </div>

                <div class="form-group">
                    <label for="description">Description (Optional)</label>
                    <textarea id="description" name="description" class="form-control"><c:out value="${transaction.description}"/></textarea>
                </div>

                <div class="form-group">
                    <label for="transactionDate">Date</label>
                    <input type="date" id="transactionDate" name="transactionDate" class="form-control" 
                           value="${transaction.transactionDate}" required>
                </div>

                <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 1rem;">
                    Update Transaction
                </button>
            </form>

            <div style="text-align: center; margin-top: 1.5rem;">
                <a href="${pageContext.request.contextPath}/viewTransaction" style="color: var(--text-muted); font-size: 0.9rem; text-decoration: none;">
                    ← Cancel and return to Transactions
                </a>
            </div>
        </div>
    </div>
</body>
</html>

