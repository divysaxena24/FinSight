<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Withdraw Funds</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw" style="color: var(--primary); font-weight: 700;">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container" style="max-width: 600px; margin-top: 35px;">
        <div class="page-header text-center">
            <h1 class="page-title">🏧 Withdraw Simulated Funds</h1>
            <p class="page-subtitle">Withdraw funds with Transaction PIN authorization</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger" style="margin-bottom: 20px;">
                <span><c:out value="${errorMessage}"/></span>
            </div>
        </c:if>

        <div class="card" style="padding: 30px;">
            <form action="${pageContext.request.contextPath}/banking" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="withdraw">
                <input type="hidden" name="idempotencyKey" value="<%= java.util.UUID.randomUUID().toString() %>">

                <div class="form-group" style="margin-bottom: 20px;">
                    <label>Source Account</label>
                    <select name="accountId" class="form-control" required>
                        <c:forEach var="acc" items="${accounts}">
                            <option value="${acc.id}">
                                <c:out value="${acc.accountNumber}"/> (<c:out value="${acc.accountType}"/>) — Balance: ₹<fmt:formatNumber value="${acc.balance}" minFractionDigits="2"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group" style="margin-bottom: 20px;">
                    <label>Withdrawal Amount (₹)</label>
                    <input type="number" step="0.01" min="1" max="10000000" name="amount" class="form-control" placeholder="e.g. 2000.00" required autofocus>
                </div>

                <div class="form-group" style="margin-bottom: 20px;">
                    <label>Category</label>
                    <select name="category" class="form-control">
                        <option value="Shopping">Shopping</option>
                        <option value="Food & Dining">Food & Dining</option>
                        <option value="Utilities">Bills & Utilities</option>
                        <option value="Entertainment">Entertainment</option>
                        <option value="Travel">Travel</option>
                        <option value="Healthcare">Healthcare</option>
                        <option value="Other">Other Expense</option>
                    </select>
                </div>

                <div class="form-group" style="margin-bottom: 20px;">
                    <label>Description</label>
                    <input type="text" name="description" class="form-control" placeholder="e.g. ATM cash withdrawal / store purchase" required>
                </div>

                <div class="form-group" style="margin-bottom: 25px;">
                    <label style="color: #0f172a; font-weight: 700;">🔑 Authorize with Transaction PIN</label>
                    <input type="password" name="transactionPin" class="form-control" required pattern="\d{4,6}" maxlength="6" placeholder="Enter 4–6 digit PIN">
                    <small style="color: #64748b;">Failed attempts are limited to protect your account.</small>
                </div>

                <button type="submit" class="btn btn-primary" style="width: 100%; padding: 12px; font-size: 1rem;">
                    Authorize & Withdraw
                </button>
            </form>
        </div>
    </div>
</body>
</html>

