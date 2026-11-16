<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Unusual Activity Alerts</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="alerts-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <button type="button" class="alerts-mobile-toggle" aria-controls="alerts-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="alerts-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts" class="active" aria-current="page">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container alerts-container">
        <div class="page-header">
            <h1 class="page-title">⚠️ Unusual Spending Alerts</h1>
            <p class="page-subtitle">Historical-spending-based anomaly detection for unusual transactions and spending spikes</p>
        </div>

        <c:if test="${not empty sessionScope.successMessage}">
            <div class="alert alert-success" role="status">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <c:choose>
            <c:when test="${empty alerts}">
                <div class="card alerts-empty-state">
                    <div class="alerts-empty-icon" aria-hidden="true">🛡️</div>
                    <h3>No Unusual Transactions Detected</h3>
                    <p>All recent simulator transactions conform to your observed spending baselines and category medians.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div class="alerts-list">
                    <c:forEach var="a" items="${alerts}">
                        <div class="card alert-card ${a.severity == 'HIGH' ? 'alert-card-high' : 'alert-card-warning'}">
                            <div class="alert-card-content">
                                <div>
                                    <div class="alert-card-heading">
                                        <span class="badge alert-severity-badge ${a.severity == 'HIGH' ? 'alert-severity-high' : 'alert-severity-warning'}">
                                            <c:out value="${a.severity}"/> SEVERITY
                                        </span>
                                        <h2><c:out value="${a.title}"/></h2>
                                    </div>
                                    <p class="alert-reason"><c:out value="${a.reason}"/></p>
                                    <div class="alert-metadata">
                                        Reference: <code><c:out value="${a.transactionRef}"/></code> &bull; Flagged on: <c:out value="${a.createdAt}"/>
                                    </div>
                                </div>
                                <div class="alert-card-action">
                                    <c:choose>
                                        <c:when test="${a.status == 'NEW'}">
                                            <form action="${pageContext.request.contextPath}/alerts" method="post">
                                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                                <input type="hidden" name="alertId" value="${a.id}">
                                                <button type="submit" class="btn btn-secondary btn-sm">Acknowledge</button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="alert-acknowledged">✓ Acknowledged</span>
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
    <script>
        (function() {
            var navToggle = document.querySelector('.alerts-mobile-toggle');
            var navLinks = document.getElementById('alerts-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('alerts-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }
        })();
    </script>
</body>
</html>

