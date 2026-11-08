<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Transaction Receipt</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .receipt-card {
            background: #fff;
            max-width: 550px;
            margin: 30px auto;
            border-radius: 12px;
            padding: 30px;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);
            border: 1px solid #e2e8f0;
        }
        .receipt-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px dashed #e2e8f0; font-size: 0.95rem; }
    </style>
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container">
        <div class="receipt-card">
            <div style="text-align: center; margin-bottom: 25px;">
                <div style="font-size: 2.5rem;">✅</div>
                <h2 style="margin: 5px 0 0 0; color: #16a34a;">Transaction Successful</h2>
                <p style="color: #64748b; font-size: 0.9rem; margin: 4px 0 0 0;">Simulated banking transaction executed and balanced in ledger</p>
            </div>

            <c:choose>
                <c:when test="${not empty transaction}">
                    <div class="receipt-row">
                        <span style="color: #64748b;">Reference Number</span>
                        <strong style="font-family: monospace;"><c:out value="${transaction.referenceNumber}"/></strong>
                    </div>
                    <div class="receipt-row">
                        <span style="color: #64748b;">Transaction Type</span>
                        <strong><c:out value="${transaction.transactionType}"/></strong>
                    </div>
                    <div class="receipt-row">
                        <span style="color: #64748b;">Amount</span>
                        <strong style="font-size: 1.15rem; color: #0f172a;">₹<fmt:formatNumber value="${transaction.amount}" minFractionDigits="2"/></strong>
                    </div>
                    <div class="receipt-row">
                        <span style="color: #64748b;">Description</span>
                        <span><c:out value="${transaction.description}"/></span>
                    </div>
                    <div class="receipt-row">
                        <span style="color: #64748b;">Timestamp</span>
                        <span><c:out value="${transaction.createdAt}"/></span>
                    </div>
                    <div class="receipt-row" style="border-bottom: none;">
                        <span style="color: #64748b;">Ledger Status</span>
                        <span class="badge" style="background: #dcfce7; color: #166534; padding: 4px 10px; border-radius: 4px; font-weight: 700;">● <c:out value="${transaction.status}"/></span>
                    </div>
                </c:when>
                <c:otherwise>
                    <p style="color: #64748b; text-align: center;">Transaction receipt details unavailable.</p>
                </c:otherwise>
            </c:choose>

            <div style="display: flex; gap: 15px; margin-top: 30px;">
                <a href="${pageContext.request.contextPath}/accounts" class="btn btn-secondary" style="flex: 1; text-align: center;">View Accounts</a>
                <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary" style="flex: 1; text-align: center;">Go to Dashboard</a>
            </div>
        </div>
    </div>
</body>
</html>

