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
</head>
<body class="transactions-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">
            💰 FinSight
        </a>
        <button type="button" class="transactions-mobile-toggle" aria-controls="transactions-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="transactions-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction" class="active" aria-current="page">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">📄 Statements</a>
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
            <div class="transactions-header-actions">
                <a href="${pageContext.request.contextPath}/banking?action=transfer" class="btn btn-primary">+ Transfer</a>
                <a href="${pageContext.request.contextPath}/banking?action=deposit" class="btn btn-secondary">+ Deposit</a>
                <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-success">+ Expense / Income</a>
            </div>
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

        <div class="filter-bar transactions-filter-bar">
            <div class="filter-pills">
                <button type="button" class="filter-btn active" onclick="filterTransactions('all', this)">All (${unifiedTransactions != null ? unifiedTransactions.size() : 0})</button>
                <button type="button" class="filter-btn" onclick="filterTransactions('Transfer', this)">Transfers</button>
                <button type="button" class="filter-btn" onclick="filterTransactions('Deposit', this)">Deposits</button>
                <button type="button" class="filter-btn" onclick="filterTransactions('Expense', this)">Expenses</button>
                <button type="button" class="filter-btn" onclick="filterTransactions('Income', this)">Income</button>
            </div>
            <div class="transactions-search">
                <input type="text" id="searchInput" placeholder="Search description, category, ref..." 
                       oninput="searchTable()" aria-label="Search transactions">
            </div>
        </div>

        <div class="table-container transactions-table-container">
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
                                <th class="amount-column">Amount</th>
                                <th class="actions-column">Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="tx" items="${unifiedTransactions}">
                                <tr data-type="${tx.type}">
                                    <td><c:out value="${tx.date}"/></td>
                                    <td>
                                        <code class="transaction-reference-code">
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
                                                <span class="transaction-no-description">No description</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                    <td class="amount-column transaction-amount ${tx.credit ? 'income-amount' : 'expense-amount'}">
                                        ${tx.credit ? '+' : '-'}₹<fmt:formatNumber value="${tx.amount}" minFractionDigits="2" maxFractionDigits="2"/>
                                    </td>
                                    <td class="actions-column">
                                        <c:choose>
                                            <c:when test="${tx.source == 'SIMULATOR'}">
                                                <a href="${pageContext.request.contextPath}/banking?action=receipt&ref=${tx.reference}" class="btn btn-secondary btn-sm transaction-action-button">
                                                    Receipt
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <div class="transaction-actions">
                                                    <a href="${pageContext.request.contextPath}/editTransaction?id=${tx.manualId}" class="btn btn-secondary btn-sm transaction-action-button">
                                                        Edit
                                                    </a>
                                                    <form action="${pageContext.request.contextPath}/deleteTransaction" method="post" class="transaction-delete-form"
                                                          onsubmit="return confirm('Are you sure you want to delete this transaction?');">
                                                        <input type="hidden" name="id" value="${tx.manualId}">
                                                        <button type="submit" class="btn btn-danger btn-sm transaction-action-button">Delete</button>
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
                        <div class="transactions-empty-actions">
                            <a href="${pageContext.request.contextPath}/banking?action=deposit" class="btn btn-primary">+ Deposit Money</a>
                            <a href="${pageContext.request.contextPath}/addTransactionPage" class="btn btn-secondary">+ Record Expense</a>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <script>
        (function() {
            var navToggle = document.querySelector('.transactions-mobile-toggle');
            var navLinks = document.getElementById('transactions-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('transactions-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }
        })();

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
