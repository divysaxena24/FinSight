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
<body class="budgets-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <button type="button" class="budgets-mobile-toggle" aria-controls="budgets-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="budgets-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets" class="active" aria-current="page">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container budgets-container">
        <div class="page-header">
            <h1 class="page-title">🎯 Smart Monthly Budget Planner</h1>
            <p class="page-subtitle">Deterministic budget recommendations derived from your historical spending patterns</p>
        </div>

        <c:if test="${not empty sessionScope.successMessage}">
            <div class="alert alert-success" role="status">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <c:if test="${not empty sessionScope.errorMessage}">
            <div class="alert alert-danger" role="alert">
                <span><c:out value="${sessionScope.errorMessage}"/></span>
            </div>
            <c:remove var="errorMessage" scope="session"/>
        </c:if>

        <div class="budgets-grid">
            <!-- Recommended Budgets from Engine -->
            <div class="card budget-panel">
                <h2 class="budget-panel-title">💡 Recommended Category Budgets</h2>
                <c:choose>
                    <c:when test="${empty recommendations}">
                        <div class="budget-empty-recommendations">
                            <strong>ℹ️ Insufficient Spending History:</strong><br>
                            FinSight's deterministic recommendation engine requires at least one full cycle of spending history to generate accurate, non-arbitrary budget targets. Execute simulated withdrawals, transfers, or upload a statement to establish baselines.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="rec" items="${recommendations}">
                            <div class="budget-recommendation">
                                <div>
                                    <div class="budget-category"><c:out value="${rec.category}"/></div>
                                    <div class="budget-rationale"><c:out value="${rec.calculationRationale}"/></div>
                                </div>
                                <div class="budget-recommendation-action">
                                    <div class="budget-recommendation-amount">₹<fmt:formatNumber value="${rec.recommendedBudget}" minFractionDigits="2"/></div>
                                    <form action="${pageContext.request.contextPath}/budgets" method="post" class="budget-accept-form">
                                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                                        <input type="hidden" name="category" value="${rec.category}">
                                        <input type="hidden" name="monthlyLimit" value="${rec.recommendedBudget}">
                                        <input type="hidden" name="monthYear" value="${currentMonthYear}">
                                        <button type="submit" class="btn btn-primary btn-sm">Accept Target</button>
                                    </form>
                                </div>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Active / Accepted Monthly Budgets -->
            <div class="card budget-panel">
                <h2 class="budget-panel-title">📋 Active Budgets (<c:out value="${currentMonthYear}"/>)</h2>

                <form action="${pageContext.request.contextPath}/budgets" method="post" class="budget-entry-form">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="monthYear" value="${currentMonthYear}">

                    <select name="category" class="form-control" required>
                        <option value="Shopping">Shopping</option>
                        <option value="Food & Dining">Food & Dining</option>
                        <option value="Utilities">Bills & Utilities</option>
                        <option value="Entertainment">Entertainment</option>
                        <option value="Travel">Travel</option>
                        <option value="Healthcare">Healthcare</option>
                        <option value="Other">Other</option>
                    </select>

                    <input type="number" step="0.01" min="1" name="monthlyLimit" placeholder="Limit (₹)" class="form-control" required>
                    <button type="submit" class="btn btn-primary btn-sm">Save</button>
                </form>

                <c:choose>
                    <c:when test="${empty acceptedBudgets}">
                        <p class="budget-empty-active">No active category budgets saved for this month yet. Accept a recommended target or enter a custom limit above.</p>
                    </c:when>
                    <c:otherwise>
                        <div class="budget-table-wrapper">
                        <table class="data-table budget-table">
                            <thead>
                                    <tr>
                                    <th>Category</th>
                                        <th class="amount-column">Monthly Target</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="b" items="${acceptedBudgets}">
                                    <tr>
                                        <td class="budget-table-category"><c:out value="${b.category}"/></td>
                                        <td class="amount-column budget-table-amount">
                                            ₹<fmt:formatNumber value="${b.monthlyLimit}" minFractionDigits="2"/>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
    <script>
        (function() {
            var navToggle = document.querySelector('.budgets-mobile-toggle');
            var navLinks = document.getElementById('budgets-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('budgets-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }
        })();
    </script>
</body>
</html>

