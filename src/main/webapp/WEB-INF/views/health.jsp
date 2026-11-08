<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Financial Health Monitor</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .score-circle {
            width: 140px;
            height: 140px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            flex-direction: column;
            margin: 0 auto 15px auto;
            color: #fff;
            box-shadow: 0 4px 10px rgba(0,0,0,0.15);
        }
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
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health" style="color: var(--primary); font-weight: 700;">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container" style="max-width: 1000px; margin-top: 30px;">
        <div class="page-header">
            <h1 class="page-title">📈 Financial Health Monitor</h1>
            <p class="page-subtitle">Deterministic scoring and explainable insights into your personal financial wellness</p>
        </div>

        <div style="display: grid; grid-template-columns: 320px 1fr; gap: 25px;">
            <!-- Health Score Card -->
            <div class="card" style="padding: 30px; text-align: center;">
                <div class="score-circle" style="background: ${healthReport.score >= 70 ? 'linear-gradient(135deg, #10b981, #059669)' : (healthReport.score >= 50 ? 'linear-gradient(135deg, #f59e0b, #d97706)' : 'linear-gradient(135deg, #ef4444, #b91c1c)')};">
                    <span style="font-size: 2.75rem; font-weight: 900; line-height: 1;">${healthReport.score}</span>
                    <span style="font-size: 0.8rem; text-transform: uppercase; letter-spacing: 1px; opacity: 0.9;">out of 100</span>
                </div>
                <h3 style="margin: 0; font-size: 1.3rem;"><c:out value="${healthReport.statusCategory}"/></h3>
                <p style="color: #64748b; font-size: 0.85rem; margin-top: 6px;">Based on savings rate, cash flow, and account liquidity</p>

                <div style="margin-top: 20px; text-align: left; border-top: 1px solid #f1f5f9; padding-top: 15px;">
                    <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                        <span style="color: #64748b; font-size: 0.9rem;">Savings Rate</span>
                        <strong><fmt:formatNumber value="${healthReport.savingsRate}" minFractionDigits="1"/>%</strong>
                    </div>
                    <div style="display: flex; justify-content: space-between; margin-bottom: 8px;">
                        <span style="color: #64748b; font-size: 0.9rem;">Net Cash Flow</span>
                        <strong style="color: ${healthReport.monthlyCashFlow >= 0 ? '#16a34a' : '#dc2626'};">
                            ₹<fmt:formatNumber value="${healthReport.monthlyCashFlow}" minFractionDigits="2"/>
                        </strong>
                    </div>
                </div>
            </div>

            <!-- Detailed Breakdown -->
            <div class="card" style="padding: 30px;">
                <h3 style="font-size: 1.15rem; margin-bottom: 15px;">💡 Explainable Financial Breakdown</h3>

                <!-- Positive Habits -->
                <c:if test="${not empty healthReport.positiveHabits}">
                    <div style="margin-bottom: 20px;">
                        <h4 style="font-size: 0.95rem; color: #16a34a; margin-bottom: 8px;">✨ Positive Financial Habits</h4>
                        <ul style="padding-left: 20px; color: #334155; font-size: 0.9rem; line-height: 1.6;">
                            <c:forEach var="item" items="${healthReport.positiveHabits}">
                                <li><c:out value="${item}"/></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>

                <!-- Areas Requiring Attention -->
                <c:if test="${not empty healthReport.areasRequiringAttention}">
                    <div style="margin-bottom: 20px;">
                        <h4 style="font-size: 0.95rem; color: #dc2626; margin-bottom: 8px;">⚠️ Areas Requiring Attention</h4>
                        <ul style="padding-left: 20px; color: #334155; font-size: 0.9rem; line-height: 1.6;">
                            <c:forEach var="item" items="${healthReport.areasRequiringAttention}">
                                <li><c:out value="${item}"/></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>

                <!-- Improvement Opportunities -->
                <c:if test="${not empty healthReport.improvementOpportunities}">
                    <div>
                        <h4 style="font-size: 0.95rem; color: #0284c7; margin-bottom: 8px;">🎯 Improvement Opportunities</h4>
                        <ul style="padding-left: 20px; color: #334155; font-size: 0.9rem; line-height: 1.6;">
                            <c:forEach var="item" items="${healthReport.improvementOpportunities}">
                                <li><c:out value="${item}"/></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
            </div>
        </div>
    </div>
</body>
</html>

