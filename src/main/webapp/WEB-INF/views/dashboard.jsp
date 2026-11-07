<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Dashboard</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">
            💰 FinSight
        </a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard" style="color: var(--primary);">Dashboard</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-primary btn-sm">+ Add Transaction</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Welcome, <c:out value="${user.name}"/></h1>
                <p class="page-subtitle">Personal financial overview and performance snapshot</p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-success">+ Record New Transaction</a>
            </div>
        </div>

        <c:if test="${not empty sessionScope.successMessage}">
            <div class="alert alert-success">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <c:if test="${not empty sessionScope.errorMessage}">
            <div class="alert alert-danger">
                <span><c:out value="${sessionScope.errorMessage}"/></span>
            </div>
            <c:remove var="errorMessage" scope="session"/>
        </c:if>

        <div class="cards-grid">
            <div class="metric-card income">
                <div class="label">Total Income</div>
                <div class="amount">₹<fmt:formatNumber value="${summary.totalIncome}" pattern="#,##0.00"/></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.5rem;">Cumulative earnings</div>
            </div>

            <div class="metric-card expense">
                <div class="label">Total Expenses</div>
                <div class="amount">₹<fmt:formatNumber value="${summary.totalExpense}" pattern="#,##0.00"/></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.5rem;">Cumulative spending</div>
            </div>

            <div class="metric-card balance">
                <div class="label">Net Balance</div>
                <div class="amount">₹<fmt:formatNumber value="${summary.balance}" pattern="#,##0.00"/></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.5rem;">
                    <c:choose>
                        <c:when test="${summary.balance >= 0}">Healthy Cashflow</c:when>
                        <c:otherwise><span style="color: var(--danger);">Deficit (Expenses exceed income)</span></c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <div class="quick-actions">
            <h3 style="font-size: 1.15rem; color: var(--dark);">Quick Navigation & Management</h3>
            <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 0.25rem;">
                Manage records and track personal expenses seamlessly. Total recorded transactions: <strong><c:out value="${summary.transactionCount}"/></strong>
            </p>
            <div class="action-buttons">
                <a href="${pageContext.request.contextPath}/viewTransaction" class="btn btn-secondary">
                    📋 View All Transactions
                </a>
                <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-primary">
                    ➕ Add Income / Expense
                </a>
            </div>
        </div>
    </div>
</body>
</html>

