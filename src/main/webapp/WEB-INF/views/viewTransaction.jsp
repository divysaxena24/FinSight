<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Unified Transaction History</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .filter-bar {
            display: flex;
            gap: 10px;
            margin-bottom: 20px;
            flex-wrap: wrap;
            align-items: center;
            justify-content: space-between;
        }
        .filter-pills {
            display: flex;
            gap: 8px;
        }
        .filter-btn {
            background: #f1f5f9;
            border: 1px solid #cbd5e1;
            padding: 6px 14px;
            border-radius: 20px;
            font-size: 0.85rem;
            font-weight: 600;
            cursor: pointer;
            color: #475569;
            transition: all 0.2s;
        }
        .filter-btn.active, .filter-btn:hover {
            background: var(--primary);
            color: white;
            border-color: var(--primary);
        }
        .badge-deposit {
            background: #ecfdf5;
            color: #047857;
            border: 1px solid #a7f3d0;
        }
        .badge-withdrawal {
            background: #fef2f2;
            color: #b91c1c;
            border: 1px solid #fecaca;
        }
        .badge-transfer {
            background: #eff6ff;
            color: #1d4ed8;
            border: 1px solid #bfdbfe;
        }
    </style>
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">
            💰 FinSight
        </a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction" style="color: var(--primary); font-weight: 700;">Transactions</a>
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
                <h1 class="page-title">Transaction History</h1>
                <p class="page-subtitle">Unified log of all banking operations, transfers, income, and expenses</p>
            </div>
            <div style="display: flex; gap: 10px;">
                <a href="${pageContext.request.contextPath}/banking?action=transfer" class="btn btn-primary">+ Transfer</a>
                <a href="${pageContext.request.contextPath}/banking?action=deposit" class="btn btn-secondary">+ Deposit</a>
                <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-success">+ Expense / Income</a>
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

        <div class="filter-bar">
            <div class="filter-pills">
                <button type="button" class="filter-btn active" onclick="filterTransactions('all', this)">All (${unifiedTransactions != null ? unifiedTransactions.size() : 0})</button>
                <button type="button" class="filter-btn" onclick="filterTransactions('Transfer', this)">Transfers</button>
                <button type="button" class="filter-btn" onclick="filterTransactions('Deposit', this)">Deposits</button>
                <button type="button" class="filter-btn" onclick="filterTransactions('Expense', this)">Expenses</button>
                <button type="button" class="filter-btn" onclick="filterTransactions('Income', this)">Income</button>
            </div>
            <div>
                <input type="text" id="searchInput" placeholder="Search description, category, ref..." 
                       onkeyup="searchTable()" 
                       style="padding: 6px 12px; border-radius: 6px; border: 1px solid #cbd5e1; font-size: 0.875rem; width: 260px;">
            </div>
        </div>

        <div class="table-container">
            <c:choose>
                <c:when test="${not empty unifiedTransactions}">
                    <table class="data-table" id="transactionsTable">
                        <thead>
                            <tr>
                                <th>Date</th>
                                <th>Reference</th>
                                <th>Type</th>
                                <th>Category</th>
                                <th>Description</th>
                                <th style="text-align: right;">Amount</th>
                                <th style="text-align: center;">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="tx" items="${unifiedTransactions}">
                                <tr data-type="${tx.type}">
                                    <td><c:out value="${tx.date}"/></td>
                                    <td>
                                        <code style="font-size: 0.8rem; background: #f8fafc; padding: 2px 5px; border-radius: 4px; border: 1px solid #e2e8f0;">
                                            <c:out value="${tx.reference}"/>
                                        </code>
                                    </td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${tx.type == 'Deposit'}">
                                                <span class="badge badge-deposit">Deposit</span>
                                            </c:when>
                                            <c:when test="${tx.type == 'Transfer'}">
                                                <span class="badge badge-transfer">Transfer</span>
                                            </c:when>
                                            <c:when test="${tx.type == 'Withdrawal'}">
                                                <span class="badge badge-withdrawal">Withdrawal</span>
                                            </c:when>
                                            <c:when test="${tx.type == 'Income'}">
                                                <span class="badge badge-income">Income</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge badge-expense"><c:out value="${tx.type}"/></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td><strong><c:out value="${tx.category}"/></strong></td>
                                    <td>
                                        <c:choose>
                                            <c:when test="${not empty tx.description}">
                                                <c:out value="${tx.description}"/>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="color: var(--text-muted); font-style: italic;">No description</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td style="text-align: right; font-weight: 700; color: ${tx.credit ? 'var(--success)' : 'var(--danger)'};">
                                        ${tx.credit ? '+' : '-'}₹<fmt:formatNumber value="${tx.amount}" minFractionDigits="2" maxFractionDigits="2"/>
                                    </td>
                                    <td style="text-align: center;">
                                        <c:choose>
                                            <c:when test="${tx.source == 'SIMULATOR'}">
                                                <a href="${pageContext.request.contextPath}/banking?action=receipt&ref=${tx.reference}" class="btn btn-secondary btn-sm" style="padding: 3px 8px; font-size: 0.75rem;">
                                                    Receipt
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <div style="display: inline-flex; gap: 0.35rem; justify-content: center;">
                                                    <a href="${pageContext.request.contextPath}/editTransaction?id=${tx.manualId}" class="btn btn-secondary btn-sm" style="padding: 3px 8px; font-size: 0.75rem;">
                                                        Edit
                                                    </a>
                                                    <form action="${pageContext.request.contextPath}/deleteTransaction" method="post" style="display: inline;" 
                                                          onsubmit="return confirm('Are you sure you want to delete this transaction?');">
                                                        <input type="hidden" name="id" value="${tx.manualId}">
                                                        <button type="submit" class="btn btn-danger btn-sm" style="padding: 3px 8px; font-size: 0.75rem;">Delete</button>
                                                    </form>
                                                </div>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </c:when>
                <c:otherwise>
                    <div class="empty-state">
                        <h3>No transactions found</h3>
                        <p>You haven't performed any banking transactions or recorded any expenses yet.</p>
                        <div style="margin-top: 1.25rem; display: flex; gap: 10px; justify-content: center;">
                            <a href="${pageContext.request.contextPath}/banking?action=deposit" class="btn btn-primary">+ Deposit Money</a>
                            <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-secondary">+ Record Expense</a>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <script>
        function filterTransactions(type, btn) {
            document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const rows = document.querySelectorAll('#transactionsTable tbody tr');
            rows.forEach(row => {
                const rowType = row.getAttribute('data-type');
                if (type === 'all' || rowType === type) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        }

        function searchTable() {
            const query = document.getElementById('searchInput').value.toLowerCase();
            const rows = document.querySelectorAll('#transactionsTable tbody tr');
            rows.forEach(row => {
                const text = row.innerText.toLowerCase();
                if (text.includes(query)) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        }
    </script>
</body>
</html>
