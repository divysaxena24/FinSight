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
            <a href="${pageContext.request.contextPath}/alerts" style="color: var(--primary); font-weight: 700;">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container" style="max-width: 1000px; margin-top: 30px;">
        <div class="page-header">
            <h1 class="page-title">⚠️ Unusual Spending Alerts</h1>
            <p class="page-subtitle">Historical-spending-based anomaly detection for unusual transactions and spending spikes</p>
        </div>

        <c:if test="${not empty sessionScope.successMessage}">
            <div class="alert alert-success" style="margin-bottom: 20px;">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <c:choose>
            <c:when test="${empty alerts}">
                <div class="card" style="padding: 40px; text-align: center;">
                    <div style="font-size: 2.5rem; margin-bottom: 10px;">🛡️</div>
                    <h3 style="margin: 0; color: #16a34a;">No Unusual Transactions Detected</h3>
                    <p style="color: #64748b; margin-top: 8px;">All recent simulator transactions conform to your observed spending baselines and category medians.</p>
                </div>
            </c:when>
            <c:otherwise>
                <div style="display: flex; flex-direction: column; gap: 15px;">
                    <c:forEach var="a" items="${alerts}">
                        <div class="card" style="padding: 20px; border-left: 5px solid ${a.severity == 'HIGH' ? '#dc2626' : '#f59e0b'};">
                            <div style="display: flex; justify-content: space-between; align-items: flex-start;">
                                <div>
                                    <div style="display: flex; gap: 10px; align-items: center;">
                                        <span class="badge" style="background: ${a.severity == 'HIGH' ? '#fee2e2' : '#fef3c7'}; color: ${a.severity == 'HIGH' ? '#991b1b' : '#b45309'}; font-weight: 700; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem;">
                                            <c:out value="${a.severity}"/> SEVERITY
                                        </span>
                                        <h3 style="font-size: 1.1rem; margin: 0; color: #0f172a;"><c:out value="${a.title}"/></h3>
                                    </div>
                                    <p style="margin: 8px 0 0 0; color: #475569; font-size: 0.95rem;"><c:out value="${a.reason}"/></p>
                                    <div style="font-size: 0.8rem; color: #64748b; margin-top: 6px;">
                                        Reference: <code style="font-family: monospace;"><c:out value="${a.transactionRef}"/></code> &bull; Flagged on: <c:out value="${a.createdAt}"/>
                                    </div>
                                </div>
                                <div style="text-align: right;">
                                    <c:choose>
                                        <c:when test="${a.status == 'NEW'}">
                                            <form action="${pageContext.request.contextPath}/alerts" method="post">
                                                <input type="hidden" name="csrfToken" value="${csrfToken}">
                                                <input type="hidden" name="alertId" value="${a.id}">
                                                <button type="submit" class="btn btn-secondary btn-sm">Acknowledge</button>
                                            </form>
                                        </c:when>
                                        <c:otherwise>
                                            <span style="font-size: 0.8rem; color: #16a34a; font-weight: 600;">✓ Acknowledged</span>
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
</body>
</html>

