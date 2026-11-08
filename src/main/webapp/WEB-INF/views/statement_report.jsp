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
    <style>
        .kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-top: 20px; }
        .kpi-card { background: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e2e8f0; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
        .kpi-title { font-size: 0.85rem; color: #64748b; font-weight: 600; text-transform: uppercase; }
        .kpi-val { font-size: 1.5rem; font-weight: 700; margin-top: 8px; }
        .chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 25px; }
        .insight-card { border-left: 4px solid var(--primary); padding: 15px; margin-bottom: 12px; background: #f8fafc; border-radius: 0 6px 6px 0; }
        .insight-card.ALERT { border-left-color: #ef4444; }
        .insight-card.WARNING { border-left-color: #f59e0b; }
        .insight-card.INFO { border-left-color: #3b82f6; }
    </style>
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statement Analysis</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container" style="max-width: 1200px; margin-top: 25px;">
        <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
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
            <div class="alert alert-success" style="margin-top: 15px;">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <!-- KPI Summary Cards -->
        <div class="kpi-grid">
            <div class="kpi-card">
                <div class="kpi-title">Total Income</div>
                <div class="kpi-val" style="color: #16a34a;">₹<fmt:formatNumber value="${report.summary.totalIncome}" minFractionDigits="2"/></div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Gross Expenses</div>
                <div class="kpi-val" style="color: #dc2626;">₹<fmt:formatNumber value="${report.summary.grossExpenses}" minFractionDigits="2"/></div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Total Refunds</div>
                <div class="kpi-val" style="color: #4f46e5;">₹<fmt:formatNumber value="${report.summary.totalRefunds}" minFractionDigits="2"/></div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Net Expenses</div>
                <div class="kpi-val" style="color: #e11d48;">₹<fmt:formatNumber value="${report.summary.netExpenses}" minFractionDigits="2"/></div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Net Cash Flow</div>
                <div class="kpi-val" style="color: ${report.summary.netCashFlow >= 0 ? '#16a34a' : '#dc2626'};">
                    ₹<fmt:formatNumber value="${report.summary.netCashFlow}" minFractionDigits="2"/>
                </div>
            </div>
            <div class="kpi-card">
                <div class="kpi-title">Savings Rate</div>
                <div class="kpi-val" style="color: #0284c7;">
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
            <div class="card" style="padding: 20px;">
                <h3 style="font-size: 1.1rem; margin-bottom: 15px;">Expense Breakdown by Category</h3>
                <canvas id="categoryChart" style="max-height: 280px;"></canvas>
            </div>
            <div class="card" style="padding: 20px;">
                <h3 style="font-size: 1.1rem; margin-bottom: 15px;">Monthly Cash Flow (Income vs Net Expense)</h3>
                <canvas id="cashflowChart" style="max-height: 280px;"></canvas>
            </div>
        </div>

        <!-- Financial Insights & Budget Recommendations -->
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-top: 25px;">
            <!-- Spending Intelligence Insights -->
            <div class="card" style="padding: 20px;">
                <h3 style="font-size: 1.1rem; margin-bottom: 15px;">💡 Financial Insights & Alerts</h3>
                <c:choose>
                    <c:when test="${empty result.financialInsights}">
                        <p style="color: #64748b;">No unusual spending spikes or anomalous patterns detected.</p>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="ins" items="${result.financialInsights}">
                            <div class="insight-card ${ins.severity}">
                                <div style="font-weight: 700; color: #1e293b;"><c:out value="${ins.title}"/></div>
                                <div style="font-size: 0.88rem; color: #475569; margin-top: 4px;"><c:out value="${ins.explanation}"/></div>
                                <div style="font-size: 0.78rem; color: #64748b; margin-top: 6px;"><em><c:out value="${ins.calculationEvidence}"/></em></div>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Budget Recommendations -->
            <div class="card" style="padding: 20px;">
                <h3 style="font-size: 1.1rem; margin-bottom: 15px;">🎯 Monthly Budget Recommendations</h3>
                <c:choose>
                    <c:when test="${empty result.budgetRecommendations}">
                        <div style="padding: 15px; background: #f8fafc; border-radius: 6px; color: #64748b;">
                            <strong>ℹ️ Insufficient Multi-Month History:</strong><br>
                            Budget recommendations require at least 2 consecutive monthly statement cycles. Continue uploading statements to generate deterministic recommendations.
                        </div>
                    </c:when>
                    <c:otherwise>
                        <c:forEach var="rec" items="${result.budgetRecommendations}">
                            <div style="padding: 12px; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center;">
                                <div>
                                    <div style="font-weight: 600;"><c:out value="${rec.category}"/></div>
                                    <div style="font-size: 0.8rem; color: #64748b;"><c:out value="${rec.calculationRationale}"/></div>
                                </div>
                                <div style="text-align: right;">
                                    <div style="font-size: 1.1rem; font-weight: 700; color: var(--primary);">₹<fmt:formatNumber value="${rec.recommendedBudget}" minFractionDigits="2"/></div>
                                    <div style="font-size: 0.75rem; color: #64748b;">Target Budget</div>
                                </div>
                            </div>
                        </c:forEach>
                    </c:otherwise>
                </c:choose>
            </div>
        <!-- Transaction Explorer -->
        <div class="card" style="margin-top: 25px; padding: 20px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                <h3 style="font-size: 1.1rem; margin: 0;">📋 Statement Transactions Explorer</h3>
                <span style="font-size: 0.85rem; color: #64748b;">Showing ${report.transactions.size()} statement transactions</span>
            </div>

            <!-- Filters -->
            <div style="display: flex; gap: 15px; align-items: center; margin-bottom: 15px;">
                <input type="text" id="reportSearchInput" placeholder="Search narration, merchant, or category..." class="form-control" style="flex: 2;">
                <select id="reportTypeFilter" class="form-control" style="flex: 1;">
                    <option value="">All Types</option>
                    <option value="Income">Income</option>
                    <option value="Expense">Expense</option>
                    <option value="Refund">Refund</option>
                    <option value="Transfer">Transfer</option>
                </select>
                <select id="reportCatFilter" class="form-control" style="flex: 1;">
                    <option value="">All Categories</option>
                    <c:forEach var="entry" items="${report.categoryExpenses}">
                        <option value="${entry.key}"><c:out value="${entry.key}"/></option>
                    </c:forEach>
                </select>
            </div>

            <div class="table-responsive" style="overflow-x: auto;">
                <table class="table" style="width: 100%; border-collapse: collapse;">
                    <thead>
                        <tr style="border-bottom: 2px solid #e2e8f0; text-align: left; font-size: 0.85rem; color: #64748b;">
                            <th>Date</th>
                            <th>Description</th>
                            <th>Merchant</th>
                            <th>Category</th>
                            <th>Type</th>
                            <th style="text-align: right;">Amount</th>
                        </tr>
                    </thead>
                    <tbody id="reportTxTableBody">
                        <c:forEach var="tx" items="${report.transactions}">
                            <tr style="border-bottom: 1px solid #f1f5f9;" class="report-tx-row">
                                <td><c:out value="${tx.transactionDate}"/></td>
                                <td><c:out value="${tx.description}"/></td>
                                <td><c:out value="${not empty tx.merchant ? tx.merchant : '—'}"/></td>
                                <td><span class="badge" style="background: #f1f5f9; color: #334155; padding: 3px 8px; border-radius: 4px; font-size: 0.8rem;"><c:out value="${tx.category}"/></span></td>
                                <td>
                                    <c:choose>
                                        <c:when test="${tx.refundOrReversal}">
                                            <span style="background: #e0e7ff; color: #4338ca; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 600;">Refund</span>
                                        </c:when>
                                        <c:when test="${tx.transactionType == 'INCOME'}">
                                            <span style="background: #dcfce7; color: #15803d; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 600;">Income</span>
                                        </c:when>
                                        <c:when test="${tx.transactionType == 'TRANSFER'}">
                                            <span style="background: #f1f5f9; color: #475569; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 600;">Transfer</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span style="background: #fee2e2; color: #b91c1c; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 600;">Expense</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td style="text-align: right; font-weight: 600; color: ${tx.transactionType == 'INCOME' ? '#16a34a' : '#1e293b'};">
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

