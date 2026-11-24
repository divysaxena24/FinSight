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
            <a href="${pageContext.request.contextPath}/subscriptions">Subscriptions</a>
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

        <!-- Subscription Renewal Deadline Alerts -->
        <c:if test="${not empty subscriptionReminders}">
            <div style="margin-bottom: 2rem;">
                <h2 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 0.75rem; color: var(--dark); display: flex; align-items: center; gap: 0.5rem;">
                    <span>&#128197;</span> Upcoming Subscription Renewal Deadlines
                </h2>
                <c:forEach var="rem" items="${subscriptionReminders}">
                    <div class="alert" style="display: flex; align-items: center; justify-content: space-between; border-left: 6px solid ${rem.alertColor}; background: ${rem.alertBgColor}; color: ${rem.alertColor}; border: 1px solid ${rem.alertBorderColor}; padding: 14px 18px; border-radius: 10px; margin-bottom: 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.05);">
                        <div style="display: flex; align-items: center; gap: 12px;">
                            <span style="font-size: 1.5rem;">
                                <c:choose>
                                    <c:when test="${rem.alertLevel == 'red'}">&#9888;&#65039;</c:when>
                                    <c:when test="${rem.alertLevel == 'orange'}">&#9200;</c:when>
                                    <c:otherwise>&#128197;</c:otherwise>
                                </c:choose>
                            </span>
                            <div>
                                <strong style="font-size: 0.95rem; display: block; margin-bottom: 2px;">
                                    <c:out value="${rem.displayName}"/> (<c:out value="${rem.planName}"/>) &bull; Next Renewal: <c:out value="${rem.renewalLabel}"/>
                                </strong>
                                <span style="font-size: 0.9rem; line-height: 1.4;">
                                    <c:out value="${rem.reminderMessage}"/>
                                </span>
                            </div>
                        </div>
                        <div style="display: flex; align-items: center; gap: 8px; margin-left: 16px; flex-shrink: 0;">
                            <span class="badge" style="background: ${rem.alertLevel == 'red' ? '#fee2e2' : (rem.alertLevel == 'orange' ? '#fef3c7' : '#dbeafe')}; color: ${rem.alertLevel == 'red' ? '#991b1b' : (rem.alertLevel == 'orange' ? '#92400e' : '#1e40af')}; font-weight: 700; padding: 6px 10px; border-radius: 20px; font-size: 0.8rem;">
                                <c:choose>
                                    <c:when test="${rem.daysRemaining == 0}">Due Today!</c:when>
                                    <c:when test="${rem.daysRemaining == 1}">Due Tomorrow</c:when>
                                    <c:otherwise>${rem.daysRemaining} days left</c:otherwise>
                                </c:choose>
                            </span>
                            <a href="${pageContext.request.contextPath}/subscriptions" class="btn btn-sm btn-secondary" style="font-size: 0.82rem; padding: 6px 12px;">View</a>
                        </div>
                    </div>
                </c:forEach>
            </div>
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

