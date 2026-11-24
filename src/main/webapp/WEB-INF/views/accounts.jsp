<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | My Accounts</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="accounts-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <button type="button" class="accounts-mobile-toggle" aria-controls="accounts-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="accounts-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts" class="active" aria-current="page">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/subscriptions">Subscriptions</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container accounts-container">
        <div class="page-header accounts-page-header">
            <div>
                <h1 class="page-title">🏦 Simulated Banking Accounts</h1>
                <p class="page-subtitle">Total Available Balance across all accounts: <strong>₹<fmt:formatNumber value="${totalBalance}" minFractionDigits="2"/></strong></p>
            </div>
            <div class="accounts-header-action">
                <!-- Create Account Trigger -->
                <button type="button" class="btn btn-primary account-modal-trigger" data-modal-target="createModal">
                    + Open New Account
                </button>
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

        <!-- Account Cards Grid -->
        <c:choose>
            <c:when test="${empty accounts}">
                <div class="card accounts-empty-state">
                    <div class="accounts-empty-icon" aria-hidden="true">🏦</div>
                    <h3>No Simulated Accounts Found</h3>
                    <p>Open your first simulated savings or current account to deposit funds and execute simulated transfers.</p>
                    <button type="button" class="btn btn-primary account-modal-trigger" data-modal-target="createModal">Open Savings Account</button>
                </div>
            </c:when>
            <c:otherwise>
                <div class="accounts-grid">
                    <c:forEach var="acc" items="${accounts}">
                        <div class="account-card">
                            <div class="account-card-header">
                                <h3 class="account-card-title"><c:out value="${acc.accountType}"/> Account</h3>
                                <span class="account-status"><span aria-hidden="true">●</span> Active</span>
                            </div>
                            <div class="acc-bal">₹<fmt:formatNumber value="${acc.balance}" minFractionDigits="2"/></div>
                            <div class="account-number-block">
                                <div class="account-number-label">Account Number</div>
                                <div class="acc-number"><c:out value="${acc.accountNumber}"/></div>
                            </div>
                            <div class="account-card-actions">
                                <a href="${pageContext.request.contextPath}/accounts?id=${acc.id}" class="btn btn-secondary btn-sm">History</a>
                                <a href="${pageContext.request.contextPath}/banking?action=deposit&accountId=${acc.id}" class="btn btn-primary btn-sm">Deposit</a>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>

        <!-- Selected Account History Details -->
        <c:if test="${not empty selectedAccount}">
            <div class="card account-ledger-card">
                <div class="account-ledger-header">
                    <div>
                        <h2>Ledger History: <span class="account-ledger-number"><c:out value="${selectedAccount.accountNumber}"/></span></h2>
                        <div class="account-ledger-summary">
                            <span>Current Balance: <strong class="account-ledger-balance">₹<fmt:formatNumber value="${selectedAccount.balance}" minFractionDigits="2"/></strong></span>
                            <span>Reconciled Ledger: <strong>₹<fmt:formatNumber value="${reconciledBalance}" minFractionDigits="2"/></strong></span>
                        </div>
                    </div>
                    <div class="account-ledger-action">
                        <a href="${pageContext.request.contextPath}/banking?action=transfer&sourceAccountId=${selectedAccount.id}" class="btn btn-primary btn-sm">Transfer from this Account</a>
                    </div>
                </div>

                <div class="table-container accounts-table-wrapper">
                    <table class="data-table accounts-table">
                        <thead>
                            <tr>
                                <th>Reference</th>
                                <th>Date</th>
                                <th>Type</th>
                                <th>Description</th>
                                <th>Category</th>
                                <th class="amount-column">Amount</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="tx" items="${accountHistory}">
                                <tr>
                                    <td class="transaction-reference"><c:out value="${tx.referenceNumber}"/></td>
                                    <td><c:out value="${tx.createdAt}"/></td>
                                    <td>
                                        <span class="badge account-transaction-type">
                                            <c:out value="${tx.transactionType}"/>
                                        </span>
                                    </td>
                                    <td><c:out value="${tx.description}"/></td>
                                    <td><c:out value="${tx.category}"/></td>
                                    <td class="amount-column transaction-amount">
                                        <c:choose>
                                            <c:when test="${tx.destinationAccountId == selectedAccount.id}">
                                                <span class="income-amount">+₹<fmt:formatNumber value="${tx.amount}" minFractionDigits="2"/></span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="expense-amount">-₹<fmt:formatNumber value="${tx.amount}" minFractionDigits="2"/></span>
                                            </c:otherwise>
                                        </c:choose>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </c:if>

        <!-- Create Account Modal Form -->
        <div id="createModal" class="account-modal" role="dialog" aria-modal="true" aria-labelledby="createModalTitle" aria-hidden="true">
            <div class="account-modal-panel" role="document">
                <button type="button" class="account-modal-close" aria-label="Close account creation dialog">&times;</button>
                <h3 id="createModalTitle">Open New Simulated Account</h3>
                <form action="${pageContext.request.contextPath}/accounts" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="create">

                    <div class="form-group">
                        <label for="accountType">Account Type</label>
                        <select id="accountType" name="accountType" class="form-control" required>
                            <option value="SAVINGS">Savings Account</option>
                            <option value="CURRENT">Current Account</option>
                        </select>
                    </div>

                    <div class="account-modal-actions">
                        <button type="button" class="btn btn-secondary account-modal-cancel">Cancel</button>
                        <button type="submit" class="btn btn-primary">Create Account</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
    <script>
        (function() {
            var modal = document.getElementById('createModal');
            var triggers = document.querySelectorAll('.account-modal-trigger');
            var closeButton = document.querySelector('.account-modal-close');
            var cancelButton = document.querySelector('.account-modal-cancel');
            var firstFocusable;
            var lastTrigger;

            function getFocusableElements() {
                return modal.querySelectorAll('button, select, input, textarea, a[href]');
            }

            function openModal(trigger) {
                lastTrigger = trigger;
                modal.classList.add('is-open');
                modal.setAttribute('aria-hidden', 'false');
                firstFocusable = modal.querySelector('#accountType');
                if (firstFocusable) firstFocusable.focus();
            }

            function closeModal() {
                modal.classList.remove('is-open');
                modal.setAttribute('aria-hidden', 'true');
                if (lastTrigger) lastTrigger.focus();
            }

            triggers.forEach(function(trigger) {
                trigger.addEventListener('click', function() {
                    openModal(trigger);
                });
            });
            closeButton.addEventListener('click', closeModal);
            cancelButton.addEventListener('click', closeModal);
            modal.addEventListener('click', function(event) {
                if (event.target === modal) closeModal();
            });
            document.addEventListener('keydown', function(event) {
                if (!modal.classList.contains('is-open')) return;
                if (event.key === 'Escape') {
                    closeModal();
                    return;
                }
                if (event.key !== 'Tab') return;
                var focusable = getFocusableElements();
                if (!focusable.length) return;
                if (event.shiftKey && document.activeElement === focusable[0]) {
                    event.preventDefault();
                    focusable[focusable.length - 1].focus();
                } else if (!event.shiftKey && document.activeElement === focusable[focusable.length - 1]) {
                    event.preventDefault();
                    focusable[0].focus();
                }
            });

            var navToggle = document.querySelector('.accounts-mobile-toggle');
            var navLinks = document.getElementById('accounts-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('accounts-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }
        })();
    </script>
</body>
</html>

