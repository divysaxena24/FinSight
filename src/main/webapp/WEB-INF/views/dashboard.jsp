<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Dashboard</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <!-- Chart.js 4.4 CDN -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.4/dist/chart.umd.min.js"></script>
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">
            💰 FinSight
        </a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard" style="color: var(--primary); font-weight: 700;">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis" style="color: #0284c7; font-weight: 700;">📄 Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Welcome, <c:out value="${user.name}"/></h1>
                <p class="page-subtitle">Digital Banking Simulator & Personal Finance Intelligence</p>
            </div>
            <div style="display: flex; gap: 10px;">
                <a href="${pageContext.request.contextPath}/banking?action=deposit" class="btn btn-primary">+ Deposit</a>
                <a href="${pageContext.request.contextPath}/banking?action=transfer" class="btn btn-secondary">Transfer</a>
                <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-success">+ Record Expense</a>
            </div>
        </div>

        <!-- Simulator Summary Banner -->
        <div class="card" style="margin-bottom: 25px; padding: 20px; background: linear-gradient(135deg, #1e293b, #0f172a); color: #fff; border-radius: 12px;">
            <div style="display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 20px;">
                <div>
                    <span style="font-size: 0.85rem; text-transform: uppercase; color: #94a3b8; font-weight: 700; letter-spacing: 1px;">Total Simulator Balance</span>
                    <div style="font-size: 2.25rem; font-weight: 900; color: #f8fafc; margin-top: 4px;">
                        ₹<fmt:formatNumber value="${totalSimulatorBalance}" minFractionDigits="2"/>
                    </div>
                    <div style="font-size: 0.85rem; color: #cbd5e1; margin-top: 4px;">
                        Active Accounts: <strong>${userAccounts.size()}</strong> &bull; Financial Health Score: <strong style="color: #38bdf8;">${healthReport.score}/100 (<c:out value="${healthReport.statusCategory}"/>)</strong>
                    </div>
                </div>
                <div style="display: flex; gap: 10px;">
                    <a href="${pageContext.request.contextPath}/accounts" class="btn btn-sm" style="background: rgba(255,255,255,0.15); color: #fff; border: 1px solid rgba(255,255,255,0.25);">Manage Accounts</a>
                    <a href="${pageContext.request.contextPath}/health" class="btn btn-sm" style="background: rgba(255,255,255,0.15); color: #fff; border: 1px solid rgba(255,255,255,0.25);">Health Report</a>
                    <a href="${pageContext.request.contextPath}/budgets" class="btn btn-sm" style="background: rgba(255,255,255,0.15); color: #fff; border: 1px solid rgba(255,255,255,0.25);">Budget Targets</a>
                </div>
            </div>
        </div>

        <c:if test="${not empty sessionScope.successMessage}">
            <div class="alert alert-success">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <c:if test="${not empty sessionScope.errorMessage}">
            <div class="alert alert-danger">
                <span><c:out value="${sessionScope.errorMessage}"/></span>
            </div>
            <c:remove var="errorMessage" scope="session"/>
        </c:if>

        <c:if test="${not empty range.errorMessage}">
            <div class="alert alert-danger">
                <span><c:out value="${range.errorMessage}"/></span>
            </div>
        </c:if>

        <!-- Date Range Filter Toolbar -->
        <div class="filter-toolbar">
            <div class="filter-pills">
                <span style="font-size: 0.85rem; font-weight: 700; color: var(--secondary); text-transform: uppercase; letter-spacing: 0.05em; margin-right: 0.25rem;">
                    Period:
                </span>
                <a href="${pageContext.request.contextPath}/dashboard?range=1M" 
                   class="filter-pill ${range.filter == '1M' ? 'active' : ''}">Last 1 Month</a>
                <a href="${pageContext.request.contextPath}/dashboard?range=6M" 
                   class="filter-pill ${range.filter == '6M' ? 'active' : ''}">Last 6 Months</a>
                <a href="${pageContext.request.contextPath}/dashboard?range=1Y" 
                   class="filter-pill ${range.filter == '1Y' ? 'active' : ''}">Last 1 Year</a>
                <a href="${pageContext.request.contextPath}/dashboard?range=ALL" 
                   class="filter-pill ${range.filter == 'ALL' ? 'active' : ''}">All Time</a>
            </div>

            <form action="${pageContext.request.contextPath}/dashboard" method="get" class="custom-range-form">
                <input type="hidden" name="range" value="CUSTOM">
                <span style="font-size: 0.85rem; font-weight: 600; color: var(--secondary);">Custom:</span>
                <input type="date" name="startDate" class="form-control" 
                       value="<c:out value="${range.startDateString}"/>" required>
                <span style="color: var(--text-muted); font-size: 0.85rem;">to</span>
                <input type="date" name="endDate" class="form-control" 
                       value="<c:out value="${range.endDateString}"/>" required>
                <button type="submit" class="btn btn-secondary btn-sm" style="padding: 0.45rem 0.85rem;">Apply</button>
            </form>
        </div>

        <!-- Active Date Indicator -->
        <div style="margin-top: -1.25rem; margin-bottom: 1.75rem; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 0.5rem;">
            <div class="range-indicator">
                <span>Active Period:</span>
                <strong>
                    <c:choose>
                        <c:when test="${not empty range.startDateString and not empty range.endDateString}">
                            <c:out value="${range.startDateString}"/> to <c:out value="${range.endDateString}"/>
                        </c:when>
                        <c:otherwise>All Available Records</c:otherwise>
                    </c:choose>
                </strong>
                <span style="color: var(--text-muted); margin-left: 0.5rem;">(${range.filter})</span>
            </div>
            <div style="font-size: 0.875rem; color: var(--text-muted);">
                Transactions in range: <strong><c:out value="${summary.transactionCount}"/></strong>
            </div>
        </div>

        <!-- Metric Cards -->
        <div class="cards-grid">
            <div class="metric-card income">
                <div class="label">Total Income</div>
                <div class="amount">₹<fmt:formatNumber value="${summary.totalIncome}" pattern="#,##0.00"/></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.5rem;">Earnings in period</div>
            </div>

            <div class="metric-card expense">
                <div class="label">Total Expenses</div>
                <div class="amount">₹<fmt:formatNumber value="${summary.totalExpense}" pattern="#,##0.00"/></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.5rem;">Spending in period</div>
            </div>

            <div class="metric-card balance">
                <div class="label">Net Balance</div>
                <div class="amount">₹<fmt:formatNumber value="${summary.balance}" pattern="#,##0.00"/></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 0.5rem;">
                    <c:choose>
                        <c:when test="${summary.balance >= 0}">Healthy Cashflow</c:when>
                        <c:otherwise><span style="color: var(--danger);">Deficit (Expenses exceed income)</span></c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <!-- Visual Analytics Grid -->
        <div class="charts-grid">
            <!-- Monthly Income vs Expense Chart -->
            <div class="chart-card">
                <div class="chart-header">
                    <div>
                        <h2 class="chart-title">Income vs Expenses Trend</h2>
                        <p class="chart-subtitle">Monthly comparison of earnings against spending across the selected timeline</p>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${hasChartData}">
                        <div class="chart-wrapper">
                            <canvas id="monthlyTrendChart"></canvas>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-state" style="padding: 2.5rem 1rem;">
                            <p style="font-size: 1.05rem; font-weight: 600; color: var(--secondary);">No transaction activity in this timeline</p>
                            <p style="font-size: 0.9rem; color: var(--text-muted); margin-top: 0.25rem;">
                                Record transactions or adjust the date filter above to view your monthly financial trend.
                            </p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <!-- Category-wise Expense Breakdown -->
            <div class="chart-card">
                <div class="chart-header">
                    <div>
                        <h2 class="chart-title">Expense by Category</h2>
                        <p class="chart-subtitle">Breakdown of expenses for the selected timeline</p>
                    </div>
                </div>

                <c:choose>
                    <c:when test="${hasCategoryData}">
                        <div class="doughnut-wrapper">
                            <canvas id="categoryDoughnutChart"></canvas>
                        </div>
                        <div class="category-list" id="categoryBreakdownList">
                            <c:forEach var="item" items="${categoryData.items}">
                                <div class="category-item">
                                    <div class="category-item-left">
                                        <span class="category-dot" data-category="<c:out value='${item.category}'/>"></span>
                                        <span class="category-name"><c:out value="${item.category}"/></span>
                                    </div>
                                    <div class="category-item-right">
                                        <span class="category-amount">₹<fmt:formatNumber value="${item.totalExpense}" type="number" minFractionDigits="2" maxFractionDigits="2"/></span>
                                        <span class="category-pct"><c:out value="${item.percentage}"/>%</span>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="empty-state" style="padding: 2.5rem 1rem;">
                            <p style="font-size: 1.05rem; font-weight: 600; color: var(--secondary);">No expenses in this period</p>
                            <p style="font-size: 0.9rem; color: var(--text-muted); margin-top: 0.25rem;">
                                When you record expense transactions in this timeline, their category breakdown will appear here.
                            </p>
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>

        <!-- Recent Simulator Transactions -->
        <c:if test="${not empty recentSimTransactions}">
            <div class="card" style="margin-bottom: 30px; padding: 25px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                    <div>
                        <h3 style="font-size: 1.15rem; color: var(--dark); margin: 0;">Recent Simulated Banking Activity</h3>
                        <p style="color: var(--text-muted); font-size: 0.85rem; margin: 3px 0 0 0;">Latest deposits, withdrawals, and account-to-account transfers</p>
                    </div>
                    <a href="${pageContext.request.contextPath}/accounts" class="btn btn-secondary btn-sm">View All in Accounts</a>
                </div>
                <div style="overflow-x: auto;">
                    <table class="data-table" style="width: 100%;">
                        <thead>
                            <tr>
                                <th>Reference</th>
                                <th>Type</th>
                                <th>Category / Description</th>
                                <th style="text-align: right;">Amount</th>
                                <th>Status</th>
                                <th>Date & Time</th>
                                <th style="text-align: center;">Receipt</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="stx" items="${recentSimTransactions}">
                                <tr>
                                    <td><code><c:out value="${stx.referenceNumber}"/></code></td>
                                    <td>
                                        <span class="badge ${stx.transactionType == 'DEPOSIT' ? 'badge-income' : (stx.transactionType == 'TRANSFER' ? 'badge-transfer' : 'badge-expense')}">
                                            <c:out value="${stx.transactionType}"/>
                                        </span>
                                    </td>
                                    <td>
                                        <strong><c:out value="${stx.category}"/></strong>
                                        <c:if test="${not empty stx.description}">
                                            <div style="font-size: 0.8rem; color: #64748b;"><c:out value="${stx.description}"/></div>
                                        </c:if>
                                    </td>
                                    <td style="text-align: right; font-weight: 700; color: ${stx.transactionType == 'DEPOSIT' ? '#16a34a' : '#dc2626'};">
                                        ${stx.transactionType == 'DEPOSIT' ? '+' : '-'}₹<fmt:formatNumber value="${stx.amount}" minFractionDigits="2"/>
                                    </td>
                                    <td>
                                        <span class="badge" style="background: #ecfdf5; color: #047857; font-weight: 600;">
                                            <c:out value="${stx.status}"/>
                                        </span>
                                    </td>
                                    <td style="font-size: 0.85rem; color: #64748b;"><c:out value="${stx.createdAt}"/></td>
                                    <td style="text-align: center;">
                                        <a href="${pageContext.request.contextPath}/banking?action=receipt&ref=${stx.referenceNumber}" class="btn btn-secondary btn-sm" style="padding: 3px 8px; font-size: 0.75rem;">Receipt</a>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:if>

        <!-- Quick Actions -->
        <div class="quick-actions">
            <h3 style="font-size: 1.15rem; color: var(--dark);">Quick Navigation & Management</h3>
            <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 0.25rem;">
                Manage records and track personal expenses seamlessly. Total recorded transactions in this period: <strong><c:out value="${summary.transactionCount}"/></strong>
            </p>
            <div class="action-buttons">
                <a href="${pageContext.request.contextPath}/viewTransaction" class="btn btn-secondary">
                    📋 View All Transactions
                </a>
                <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-primary">
                    ➕ Add Income / Expense
                </a>
            </div>
        </div>
    </div>

    <!-- Safe Data Initialization -->
    <c:if test="${hasChartData}">
        <script id="chartDataPayload" type="application/json"><c:out value="${chartDataJson}" escapeXml="false"/></script>
    </c:if>

    <c:if test="${hasCategoryData}">
        <script id="categoryDataPayload" type="application/json"><c:out value="${categoryDataJson}" escapeXml="false"/></script>
    </c:if>

    <script>
        (function() {
            // Preset harmonious color palette for categories
            var palette = [
                '#3b82f6', // Blue
                '#ef4444', // Red
                '#10b981', // Emerald
                '#f59e0b', // Amber
                '#8b5cf6', // Violet
                '#ec4899', // Pink
                '#06b6d4', // Cyan
                '#f97316', // Orange
                '#64748b', // Slate
                '#14b8a6', // Teal
                '#a855f7', // Purple
                '#84cc16'  // Lime
            ];

            // 1. Render Monthly Trend Bar Chart
            try {
                var barPayloadEl = document.getElementById('chartDataPayload');
                var barCtx = document.getElementById('monthlyTrendChart');
                if (barPayloadEl && barCtx) {
                    var chartData = JSON.parse(barPayloadEl.textContent);
                    new Chart(barCtx, {
                        type: 'bar',
                        data: {
                            labels: chartData.labels,
                            datasets: [
                                {
                                    label: 'Income',
                                    data: chartData.income,
                                    backgroundColor: 'rgba(22, 163, 74, 0.85)',
                                    borderColor: '#16a34a',
                                    borderWidth: 1,
                                    borderRadius: 6,
                                    barPercentage: 0.7,
                                    categoryPercentage: 0.6
                                },
                                {
                                    label: 'Expenses',
                                    data: chartData.expense,
                                    backgroundColor: 'rgba(220, 38, 38, 0.85)',
                                    borderColor: '#dc2626',
                                    borderWidth: 1,
                                    borderRadius: 6,
                                    barPercentage: 0.7,
                                    categoryPercentage: 0.6
                                }
                            ]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    position: 'top',
                                    labels: {
                                        usePointStyle: true,
                                        boxWidth: 10,
                                        font: {
                                            size: 12,
                                            weight: '600'
                                        }
                                    }
                                },
                                tooltip: {
                                    backgroundColor: '#0f172a',
                                    titleFont: { size: 13, weight: '700' },
                                    bodyFont: { size: 12 },
                                    padding: 10,
                                    cornerRadius: 8,
                                    callbacks: {
                                        label: function(context) {
                                            var val = context.parsed.y || 0;
                                            return ' ' + context.dataset.label + ': ₹' + val.toLocaleString('en-IN', {
                                                minimumFractionDigits: 2,
                                                maximumFractionDigits: 2
                                            });
                                        }
                                    }
                                }
                            },
                            scales: {
                                x: {
                                    grid: { display: false },
                                    ticks: {
                                        font: { size: 12, weight: '500' },
                                        color: '#64748b'
                                    }
                                },
                                y: {
                                    beginAtZero: true,
                                    grid: {
                                        color: '#f1f5f9'
                                    },
                                    ticks: {
                                        font: { size: 12 },
                                        color: '#64748b',
                                        callback: function(val) {
                                            return '₹' + Number(val).toLocaleString('en-IN');
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            } catch (e) {
                console.error('Failed to render FinSight trend chart:', e);
            }

            // 2. Render Category Expense Doughnut Chart
            try {
                var catPayloadEl = document.getElementById('categoryDataPayload');
                var catCtx = document.getElementById('categoryDoughnutChart');
                if (catPayloadEl && catCtx) {
                    var catData = JSON.parse(catPayloadEl.textContent);
                    var bgColors = [];
                    var hoverColors = [];

                    for (var i = 0; i < catData.labels.length; i++) {
                        var color = palette[i % palette.length];
                        bgColors.push(color);
                        hoverColors.push(color);
                    }

                    // Assign corresponding colors to the category breakdown dots
                    var dots = document.querySelectorAll('.category-dot');
                    for (var d = 0; d < dots.length; d++) {
                        dots[d].style.backgroundColor = palette[d % palette.length];
                    }

                    new Chart(catCtx, {
                        type: 'doughnut',
                        data: {
                            labels: catData.labels,
                            datasets: [{
                                data: catData.data,
                                backgroundColor: bgColors,
                                hoverBackgroundColor: hoverColors,
                                borderWidth: 2,
                                borderColor: '#ffffff',
                                hoverOffset: 6
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            cutout: '68%',
                            plugins: {
                                legend: {
                                    display: false // Using compact custom list alongside chart for clarity
                                },
                                tooltip: {
                                    backgroundColor: '#0f172a',
                                    titleFont: { size: 13, weight: '700' },
                                    bodyFont: { size: 12 },
                                    padding: 10,
                                    cornerRadius: 8,
                                    callbacks: {
                                        label: function(context) {
                                            var val = context.raw || 0;
                                            var total = catData.totalExpenses || 1;
                                            var pct = ((val / total) * 100).toFixed(1);
                                            return ' ' + context.label + ': ₹' + val.toLocaleString('en-IN', {
                                                minimumFractionDigits: 2,
                                                maximumFractionDigits: 2
                                            }) + ' (' + pct + '%)';
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            } catch (err) {
                console.error('Failed to render FinSight category chart:', err);
            }
        })();
    </script>
</body>
</html>
