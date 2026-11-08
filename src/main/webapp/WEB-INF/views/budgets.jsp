<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Smart Budget Planner</title>
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
            <a href="${pageContext.request.contextPath}/budgets" style="color: var(--primary); font-weight: 700;">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container" style="max-width: 1100px; margin-top: 30px;">
        <div class="page-header">
            <h1 class="page-title">🎯 Smart Monthly Budget Planner</h1>
            <p class="page-subtitle">Deterministic budget recommendations derived from your historical spending patterns</p>
        </div>

        <c:if test="${not empty sessionScope.successMessage}">
            <div class="alert alert-success" style="margin-bottom: 20px;">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <c:if test="${not empty sessionScope.errorMessage}">
            <div class="alert alert-danger" style="margin-bottom: 20px;">
                <span><c:out value="${sessionScope.errorMessage}"/></span>
            </div>
            <c:remove var="errorMessage" scope="session"/>
        </c:if>

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 25px;">
            <!-- Recommended Budgets from Engine -->
            <div class="card" style="padding: 25px;">
                <h3 style="font-size: 1.15rem; margin-bottom: 15px;">💡 Recommended Category Budgets</h3>
                <c:choose>
                    <c:when test="${empty recommendations}">
                        <div style="padding: 20px; background: #f8fafc; border-radius: 6px; color: #64748b; font-size: 0.95rem;">
                            <strong>ℹ️ Insufficient Spending History:</strong><br>
                            FinSight's deterministic recommendation engine requires at least one full cycle of spending history to generate accurate, non-arbitrary budget targets. Execute simulated withdrawals, transfers, or upload a statement to establish baselines.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="rec" items="${recommendations}">
                            <div style="padding: 15px; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center;">
                                <div>
                                    <div style="font-weight: 700; color: #1e293b;"><c:out value="${rec.category}"/></div>
                                    <div style="font-size: 0.8rem; color: #64748b; margin-top: 2px;"><c:out value="${rec.calculationRationale}"/></div>
                                </div>
                                <div style="text-align: right;">
                                    <div style="font-size: 1.15rem; font-weight: 800; color: var(--primary);">₹<fmt:formatNumber value="${rec.recommendedBudget}" minFractionDigits="2"/></div>
                                    <form action="${pageContext.request.contextPath}/budgets" method="post" style="display: inline; margin-top: 5px;">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="category" value="${rec.category}">
                                        <input type="hidden" name="monthlyLimit" value="${rec.recommendedBudget}">
                                        <input type="hidden" name="monthYear" value="${currentMonthYear}">
                                        <button type="submit" class="btn btn-primary btn-sm" style="padding: 3px 8px; font-size: 0.75rem;">Accept Target</button>
                                    </form>
                                </div>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Active / Accepted Monthly Budgets -->
            <div class="card" style="padding: 25px;">
                <h3 style="font-size: 1.15rem; margin-bottom: 15px;">📋 Active Budgets (<c:out value="${currentMonthYear}"/>)</h3>

                <form action="${pageContext.request.contextPath}/budgets" method="post" style="display: flex; gap: 10px; margin-bottom: 20px;">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="monthYear" value="${currentMonthYear}">

                    <select name="category" class="form-control" style="flex: 2;" required>
                        <option value="Shopping">Shopping</option>
                        <option value="Food & Dining">Food & Dining</option>
                        <option value="Utilities">Bills & Utilities</option>
                        <option value="Entertainment">Entertainment</option>
                        <option value="Travel">Travel</option>
                        <option value="Healthcare">Healthcare</option>
                        <option value="Other">Other</option>
                    </select>

                    <input type="number" step="0.01" min="1" name="monthlyLimit" placeholder="Limit (₹)" class="form-control" style="flex: 2;" required>
                    <button type="submit" class="btn btn-primary btn-sm" style="flex: 1;">Save</button>
                </form>

                <c:choose>
                    <c:when test="${empty acceptedBudgets}">
                        <p style="color: #64748b; font-size: 0.9rem;">No active category budgets saved for this month yet. Accept a recommended target or enter a custom limit above.</p>
                    </c:when>
                    <c:otherwise>
                        <table class="table" style="width: 100%; border-collapse: collapse;">
                            <thead>
                                <tr style="border-bottom: 2px solid #e2e8f0; text-align: left; font-size: 0.85rem; color: #64748b;">
                                    <th>Category</th>
                                    <th style="text-align: right;">Monthly Target</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="b" items="${acceptedBudgets}">
                                    <tr style="border-bottom: 1px solid #f1f5f9; font-size: 0.9rem;">
                                        <td style="font-weight: 600;"><c:out value="${b.category}"/></td>
                                        <td style="text-align: right; font-weight: 700; color: #0f172a;">
                                            ₹<fmt:formatNumber value="${b.monthlyLimit}" minFractionDigits="2"/>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</body>
</html>

