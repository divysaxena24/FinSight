<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Deposit Funds</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit" style="color: var(--primary); font-weight: 700;">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
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
            <h1 class="page-title">💵 Deposit Simulated Funds</h1>
            <p class="page-subtitle">Add simulated funds to your FinSight banking accounts</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger" style="margin-bottom: 20px;">
                <span><c:out value="${errorMessage}"/></span>
            </div>
        </c:if>

        <div class="card" style="padding: 30px;">
            <form action="${pageContext.request.contextPath}/banking" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="deposit">
                <input type="hidden" name="idempotencyKey" value="<%= java.util.UUID.randomUUID().toString() %>">

                <div class="form-group" style="margin-bottom: 20px;">
                    <label>Select Target Account</label>
                    <select name="accountId" class="form-control" required>
                        <c:forEach var="acc" items="${accounts}">
                            <option value="${acc.id}" ${param.accountId == acc.id ? 'selected' : ''}>
                                <c:out value="${acc.accountNumber}"/> (<c:out value="${acc.accountType}"/>) — Balance: ₹<fmt:formatNumber value="${acc.balance}" minFractionDigits="2"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group" style="margin-bottom: 20px;">
                    <label>Deposit Amount (₹)</label>
                    <input type="number" step="0.01" min="1" max="10000000" name="amount" class="form-control" placeholder="e.g. 50000.00" required autofocus>
                </div>

                <div class="form-group" style="margin-bottom: 20px;">
                    <label>Source / Category</label>
                    <select name="category" class="form-control">
                        <option value="Salary">Salary</option>
                        <option value="Freelance">Freelance</option>
                        <option value="Investment">Investment Return</option>
                        <option value="Other">Other Deposit</option>
                    </select>
                </div>

                <div class="form-group" style="margin-bottom: 25px;">
                    <label>Description / Note</label>
                    <input type="text" name="description" class="form-control" placeholder="e.g. Monthly salary deposit" required>
                </div>

                <button type="submit" class="btn btn-primary" style="width: 100%; padding: 12px; font-size: 1rem;">
                    Confirm Simulated Deposit
                </button>
            </form>
        </div>
    </div>
</body>
</html>

