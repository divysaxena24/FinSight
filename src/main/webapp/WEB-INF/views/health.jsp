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
</head>
<body class="health-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <button type="button" class="health-mobile-toggle" aria-controls="health-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="health-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health" class="active" aria-current="page">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container health-container">
        <div class="page-header">
            <h1 class="page-title">📈 Financial Health Monitor</h1>
            <p class="page-subtitle">Deterministic scoring and explainable insights into your personal financial wellness</p>
        </div>

        <div class="health-grid">
            <!-- Health Score Card -->
            <div class="card health-score-card">
                <div class="score-circle ${healthReport.score >= 70 ? 'health-score-good' : (healthReport.score >= 50 ? 'health-score-medium' : 'health-score-low')}">
                    <span class="health-score-value">${healthReport.score}</span>
                    <span class="health-score-label">out of 100</span>
                </div>
                <h2 class="health-status"><c:out value="${healthReport.statusCategory}"/></h2>
                <p class="health-score-description">Based on savings rate, cash flow, and account liquidity</p>

                <div class="health-metrics">
                    <div class="health-metric">
                        <span>Savings Rate</span>
                        <strong><fmt:formatNumber value="${healthReport.savingsRate}" minFractionDigits="1"/>%</strong>
                    </div>
                    <div class="health-metric">
                        <span>Net Cash Flow</span>
                        <strong class="${healthReport.monthlyCashFlow >= 0 ? 'health-positive' : 'health-negative'}">
                            ₹<fmt:formatNumber value="${healthReport.monthlyCashFlow}" minFractionDigits="2"/>
                        </strong>
                    </div>
                </div>
            </div>

            <!-- Detailed Breakdown -->
            <div class="card health-breakdown-card">
                <h2 class="health-breakdown-title">💡 Explainable Financial Breakdown</h2>

                <!-- Positive Habits -->
                <c:if test="${not empty healthReport.positiveHabits}">
                    <div class="health-section health-positive-section">
                        <h3>✨ Positive Financial Habits</h3>
                        <ul>
                            <c:forEach var="item" items="${healthReport.positiveHabits}">
                                <li><c:out value="${item}"/></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>

                <!-- Areas Requiring Attention -->
                <c:if test="${not empty healthReport.areasRequiringAttention}">
                    <div class="health-section health-attention-section">
                        <h3>⚠️ Areas Requiring Attention</h3>
                        <ul>
                            <c:forEach var="item" items="${healthReport.areasRequiringAttention}">
                                <li><c:out value="${item}"/></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>

                <!-- Improvement Opportunities -->
                <c:if test="${not empty healthReport.improvementOpportunities}">
                    <div class="health-section health-opportunity-section">
                        <h3>🎯 Improvement Opportunities</h3>
                        <ul>
                            <c:forEach var="item" items="${healthReport.improvementOpportunities}">
                                <li><c:out value="${item}"/></li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
            </div>
        </div>
    </div>
    <script>
        (function() {
            var navToggle = document.querySelector('.health-mobile-toggle');
            var navLinks = document.getElementById('health-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('health-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }
        })();
    </script>
</body>
</html>

