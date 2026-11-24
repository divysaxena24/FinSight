<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Statement Financial Report</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"></script>
</head>
<body class="statement-report-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <button type="button" class="statement-report-mobile-toggle" aria-controls="statement-report-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="statement-report-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/statement-analysis" class="active" aria-current="page">Statement Analysis</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container statement-report-container">
        <div class="page-header statement-report-header">
            <div>
                <h1 class="page-title">📊 Statement Financial Report</h1>
                <p class="page-subtitle">
                    Statement Period: <strong>${result.effectiveStartDate}</strong> to <strong>${result.effectiveEndDate}</strong> 
                    | Bank: <strong><c:out value="${result.bankName}"/></strong>
                </p>
            </div>
            <div>
                <a href="${pageContext.request.contextPath}/statement-analysis" class="btn btn-primary">+ Analyze Another Statement</a>
            </div>
        </div>

        <c:if test="${not empty sessionScope.successMessage}">
            <div class="alert alert-success statement-report-success" role="status">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <!-- KPI Summary Cards -->
        <div class="kpi-grid">
            <div class="kpi-card">
                <div class="kpi-title">Total Income</div>
                <div class="kpi-val kpi-income">₹<fmt:formatNumber value="${report.summary.totalIncome}" minFractionDigits="2"/></div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Gross Expenses</div>
                <div class="kpi-val kpi-expense">₹<fmt:formatNumber value="${report.summary.grossExpenses}" minFractionDigits="2"/></div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Total Refunds</div>
                <div class="kpi-val kpi-refund">₹<fmt:formatNumber value="${report.summary.totalRefunds}" minFractionDigits="2"/></div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Net Expenses</div>
                <div class="kpi-val kpi-net-expense">₹<fmt:formatNumber value="${report.summary.netExpenses}" minFractionDigits="2"/></div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Net Cash Flow</div>
                <div class="kpi-val ${report.summary.netCashFlow >= 0 ? 'kpi-positive' : 'kpi-negative'}">
                    ₹<fmt:formatNumber value="${report.summary.netCashFlow}" minFractionDigits="2"/>
                </div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Savings Rate</div>
                <div class="kpi-val kpi-savings">
                    <c:choose>
                        <c:when test="${report.summary.totalIncome == 0}">
                            0.0%
                        </c:when>
                        <c:otherwise>
                            <fmt:formatNumber value="${report.summary.savingsRate}" minFractionDigits="1"/>%
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Average Daily Spend</div>
                <div class="kpi-val">₹<fmt:formatNumber value="${report.summary.averageDailySpend}" minFractionDigits="2"/></div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Total Transactions</div>
                <div class="kpi-val"><c:out value="${report.summary.totalTransactionCount}"/></div>
            </div>
        </div>

        <!-- Charts Section -->
        <div class="chart-grid">
            <div class="card report-chart-card">
            <h2>Expense Breakdown by Category</h2>
            <div class="report-chart-area"><canvas id="categoryChart"></canvas></div>
            </div>
            <div class="card report-chart-card">
            <h2>Monthly Cash Flow (Income vs Net Expense)</h2>
            <div class="report-chart-area"><canvas id="cashflowChart"></canvas></div>
            </div>
        </div>

        <!-- Financial Insights & Budget Recommendations -->
        <div class="report-insights-grid">
            <!-- Spending Intelligence Insights -->
            <div class="card report-section-card">
            <h2>💡 Financial Insights & Alerts</h2>
                <c:choose>
                    <c:when test="${empty result.financialInsights}">
                        <p class="report-empty-text">No unusual spending spikes or anomalous patterns detected.</p>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="ins" items="${result.financialInsights}">
                            <div class="insight-card ${ins.severity}">
                                <div class="insight-title"><c:out value="${ins.title}"/></div>
                                <div class="insight-explanation"><c:out value="${ins.explanation}"/></div>
                                <div class="insight-evidence"><em><c:out value="${ins.calculationEvidence}"/></em></div>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Budget Recommendations -->
            <div class="card report-section-card">
                <h2>🎯 Monthly Budget Recommendations</h2>
                <c:choose>
                    <c:when test="${empty result.budgetRecommendations}">
                        <div class="report-empty-panel">
                            <strong>ℹ️ Insufficient Multi-Month History:</strong><br>
                            Budget recommendations require at least 2 consecutive monthly statement cycles. Continue uploading statements to generate deterministic recommendations.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="rec" items="${result.budgetRecommendations}">
                            <div class="budget-recommendation-row">
                                <div>
                                    <div class="recommendation-category"><c:out value="${rec.category}"/></div>
                                    <div class="recommendation-rationale"><c:out value="${rec.calculationRationale}"/></div>
                                </div>
                                <div class="recommendation-amount">
                                    <div>₹<fmt:formatNumber value="${rec.recommendedBudget}" minFractionDigits="2"/></div>
                                    <span>Target Budget</span>
                                </div>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        <!-- Transaction Explorer -->
        <div class="card report-transactions-card">
            <div class="report-transactions-header">
                <h2>📋 Statement Transactions Explorer</h2>
                <span>Showing ${report.transactions.size()} statement transactions</span>
            </div>

            <!-- Filters -->
            <div class="report-filter-controls">
                <input type="text" id="reportSearchInput" placeholder="Search narration, merchant, or category..." class="form-control report-search-input">
                <select id="reportTypeFilter" class="form-control">
                    <option value="">All Types</option>
                    <option value="Income">Income</option>
                    <option value="Expense">Expense</option>
                    <option value="Refund">Refund</option>
                    <option value="Transfer">Transfer</option>
                </select>
                <select id="reportCatFilter" class="form-control">
                    <option value="">All Categories</option>
                    <c:forEach var="entry" items="${report.categoryExpenses}">
                        <option value="${entry.key}"><c:out value="${entry.key}"/></option>
                    </c:forEach>
                </select>
            </div>

            <div class="table-responsive report-table-wrapper">
                <table class="table report-table">
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Description</th>
                            <th>Merchant</th>
                            <th>Category</th>
                            <th>Type</th>
                            <th class="report-amount-heading">Amount</th>
                        </tr>
                    </thead>
                    <tbody id="reportTxTableBody">
                        <c:forEach var="tx" items="${report.transactions}">
                            <tr class="report-tx-row">
                                <td><c:out value="${tx.transactionDate}"/></td>
                                <td><c:out value="${tx.description}"/></td>
                                <td><c:out value="${not empty tx.merchant ? tx.merchant : '—'}"/></td>
                                <td><span class="badge report-category-badge"><c:out value="${tx.category}"/></span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${tx.refundOrReversal}">
                                            <span class="report-type-badge report-refund">Refund</span>
                                        </c:when>
                                        <c:when test="${tx.transactionType == 'INCOME'}">
                                            <span class="report-type-badge report-income">Income</span>
                                        </c:when>
                                        <c:when test="${tx.transactionType == 'TRANSFER'}">
                                            <span class="report-type-badge report-transfer">Transfer</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="report-type-badge report-expense">Expense</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td class="report-amount ${tx.transactionType == 'INCOME' ? 'report-income-amount' : ''}">
                                    ${tx.transactionType == 'INCOME' ? '+' : '-'}₹<fmt:formatNumber value="${tx.amount}" minFractionDigits="2"/>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <script>
        (function() {
            var navToggle = document.querySelector('.statement-report-mobile-toggle');
            var navLinks = document.getElementById('statement-report-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('statement-report-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }
        })();

        // Category Chart Data from Backend
        const categoryLabels = [
            <c:forEach var="entry" items="${report.categoryExpenses}" varStatus="loop">
                "<c:out value="${entry.key}"/>"<c:if test="${!loop.last}">,</c:if>
            </c:forEach>
        ];
        const categoryData = [
            <c:forEach var="entry" items="${report.categoryExpenses}" varStatus="loop">
                ${entry.value}<c:if test="${!loop.last}">,</c:if>
            </c:forEach>
        ];

        if (categoryLabels.length > 0) {
            new Chart(document.getElementById('categoryChart'), {
                type: 'doughnut',
                data: {
                    labels: categoryLabels,
                    datasets: [{
                        data: categoryData,
                        backgroundColor: ['#6366f1', '#ec4899', '#f59e0b', '#10b981', '#3b82f6', '#8b5cf6', '#14b8a6', '#f43f5e']
                    }]
                },
                options: { responsive: true, maintainAspectRatio: false }
            });
        }

        // Monthly Cashflow Chart Data from Backend
        const monthlyLabels = [
            <c:forEach var="entry" items="${report.monthlyCashFlows}" varStatus="loop">
                "${entry.key}"<c:if test="${!loop.last}">,</c:if>
            </c:forEach>
        ];
        const monthlyIncome = [
            <c:forEach var="entry" items="${report.monthlyCashFlows}" varStatus="loop">
                ${entry.value.income}<c:if test="${!loop.last}">,</c:if>
            </c:forEach>
        ];
        const monthlyExpense = [
            <c:forEach var="entry" items="${report.monthlyCashFlows}" varStatus="loop">
                ${entry.value.netExpense}<c:if test="${!loop.last}">,</c:if>
            </c:forEach>
        ];

        if (monthlyLabels.length > 0) {
            new Chart(document.getElementById('cashflowChart'), {
                type: 'bar',
                data: {
                    labels: monthlyLabels,
                    datasets: [
                        { label: 'Income', data: monthlyIncome, backgroundColor: '#22c55e' },
                        { label: 'Net Expense', data: monthlyExpense, backgroundColor: '#ef4444' }
                    ]
                },
                options: { responsive: true, maintainAspectRatio: false }
            });
        }

        // Live filtering in Transaction Explorer
        const searchInput = document.getElementById('reportSearchInput');
        const typeFilter = document.getElementById('reportTypeFilter');
        const catFilter = document.getElementById('reportCatFilter');

        function filterReportRows() {
            const query = searchInput ? searchInput.value.toLowerCase() : '';
            const selectedType = typeFilter ? typeFilter.value.toLowerCase() : '';
            const selectedCat = catFilter ? catFilter.value.toLowerCase() : '';

            document.querySelectorAll('.report-tx-row').forEach(row => {
                const text = row.innerText.toLowerCase();
                const matchQuery = !query || text.includes(query);
                const matchType = !selectedType || text.includes(selectedType);
                const matchCat = !selectedCat || text.includes(selectedCat);
                row.style.display = (matchQuery && matchType && matchCat) ? '' : 'none';
            });
        }

        if (searchInput) searchInput.addEventListener('input', filterReportRows);
        if (typeFilter) typeFilter.addEventListener('change', filterReportRows);
        if (catFilter) catFilter.addEventListener('change', filterReportRows);
    </script>
</body>
</html>

