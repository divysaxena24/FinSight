<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | View Transactions</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">
            💰 FinSight
        </a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/viewTransaction" style="color: var(--primary);">Transactions</a>
            <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-primary btn-sm">+ Add Transaction</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Transaction History</h1>
                <p class="page-subtitle">All income and expense records recorded in your FinSight account</p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-success">+ Add Transaction</a>
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

        <div class="table-container">
            <c:choose>
                <c:when test="${not empty transactions}">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Type</th>
                                <th>Category</th>
                                <th>Description</th>
                                <th style="text-align: right;">Amount</th>
                                <th style="text-align: center;">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="tx" items="${transactions}">
                                <tr>
                                    <td><c:out value="${tx.transactionDate}"/></td>
                                    <td>
                                        <span class="badge ${tx.type == 'Income' ? 'badge-income' : 'badge-expense'}">
                                            <c:out value="${tx.type}"/>
                                        </span>
                                    </td>
                                    <td><strong><c:out value="${tx.category}"/></strong></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty tx.description}">
                                                <c:out value="${tx.description}"/>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="color: var(--text-muted); font-style: italic;">No description</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td style="text-align: right; font-weight: 700; color: ${tx.type == 'Income' ? 'var(--success)' : 'var(--danger)'};">
                                        ${tx.type == 'Income' ? '+' : '-'}₹<fmt:formatNumber value="${tx.amount}" pattern="#,##0.00"/>
                                    </td>
                                    <td style="text-align: center;">
                                        <div style="display: inline-flex; gap: 0.5rem; justify-content: center;">
                                            <a href="${pageContext.request.contextPath}/editTransaction?id=${tx.id}" class="btn btn-secondary btn-sm">
                                                Edit
                                            </a>
                                            <form action="${pageContext.request.contextPath}/deleteTransaction" method="post" style="display: inline;" 
                                                  onsubmit="return confirm('Are you sure you want to delete this transaction?');">
                                                <input type="hidden" name="id" value="${tx.id}">
                                                <button type="submit" class="btn btn-danger btn-sm">Delete</button>
                                            </form>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        <h3>No transactions found</h3>
                        <p>You haven't recorded any income or expenses yet.</p>
                        <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-primary" style="margin-top: 1.25rem;">
                            + Record Your First Transaction
                        </a>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</body>
</html>

