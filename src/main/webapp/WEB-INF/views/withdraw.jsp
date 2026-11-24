<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Withdraw Funds</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="withdraw-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <button type="button" class="withdraw-mobile-toggle" aria-controls="withdraw-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="withdraw-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw" class="active" aria-current="page">Withdraw</a>
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

    <div class="container withdraw-container">
        <div class="page-header text-center">
            <h1 class="page-title">🏧 Withdraw Simulated Funds</h1>
            <p class="page-subtitle">Withdraw funds with Transaction PIN authorization</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger" role="alert">
                <span><c:out value="${errorMessage}"/></span>
            </div>
        </c:if>

        <div class="card withdraw-card">
            <form action="${pageContext.request.contextPath}/banking" method="post">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="withdraw">
                <input type="hidden" name="idempotencyKey" value="<%= java.util.UUID.randomUUID().toString() %>">

                <input type="hidden" name="overrideBudget" id="withdrawOverrideBudget" value="false">

                <div class="form-group">
                    <label for="withdrawAccount">Source Account</label>
                    <select id="withdrawAccount" name="accountId" class="form-control" required>
                        <c:forEach var="acc" items="${accounts}">
                            <option value="${acc.id}">
                                <c:out value="${acc.accountNumber}"/> (<c:out value="${acc.accountType}"/>) — Balance: ₹<fmt:formatNumber value="${acc.balance}" minFractionDigits="2"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group">
                    <label for="withdrawAmount">Withdrawal Amount (₹)</label>
                    <input type="number" id="withdrawAmount" step="0.01" min="1" max="10000000" name="amount" class="form-control" placeholder="e.g. 2000.00" required autofocus>
                    <div id="withdrawBudgetWarning" class="budget-limit-warning" style="display: none;"></div>
                </div>

                <div class="form-group">
                    <label for="withdrawCategory">Category</label>
                    <select id="withdrawCategory" name="category" class="form-control">
                        <option value="Shopping">Shopping</option>
                        <option value="Food & Dining">Food & Dining</option>
                        <option value="Utilities">Bills & Utilities</option>
                        <option value="Entertainment">Entertainment</option>
                        <option value="Travel">Travel</option>
                        <option value="Healthcare">Healthcare</option>
                        <option value="Other">Other Expense</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="withdrawDescription">Description</label>
                    <input type="text" id="withdrawDescription" name="description" class="form-control" placeholder="e.g. ATM cash withdrawal / store purchase" required>
                </div>

                <div class="form-group withdraw-pin-group">
                    <label for="transactionPin">🔑 Authorize with Transaction PIN</label>
                    <input type="password" id="transactionPin" name="transactionPin" class="form-control" required pattern="\d{4,6}" maxlength="6" placeholder="Enter 4–6 digit PIN" inputmode="numeric" autocomplete="current-password" aria-describedby="withdrawPinHelp">
                    <small id="withdrawPinHelp">Failed attempts are limited to protect your account.</small>
                </div>

                <button type="submit" class="btn btn-primary withdraw-submit-button">
                    Authorize & Withdraw
                </button>
            </form>
        </div>
    </div>

    <!-- Budget Exceeded Modal -->
    <div class="modal-backdrop-budget" id="withdrawBudgetModal" style="display: none;">
        <div class="modal-card-budget">
            <div class="modal-budget-icon">⚠️</div>
            <h3 class="modal-budget-title">Budget Limit Warning</h3>
            <p id="withdrawModalMessage" class="modal-budget-desc">
                You are crossing your budget amount limit! Do you still want to continue?
            </p>
            <div class="modal-budget-actions">
                <button type="button" class="btn btn-secondary" id="btnCancelWithdrawPay">Cancel / Change Amount</button>
                <button type="button" class="btn btn-danger" id="btnConfirmWithdrawPay" style="background: #dc2626; border-color: #dc2626; color: #fff;">Yes, Continue Withdrawing</button>
            </div>
        </div>
    </div>

    <script>
        (function() {
            var contextPath = '${pageContext.request.contextPath}';
            var navToggle = document.querySelector('.withdraw-mobile-toggle');
            var navLinks = document.getElementById('withdraw-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('withdraw-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }

            var categorySelect = document.getElementById('withdrawCategory');
            var amountInput = document.getElementById('withdrawAmount');
            var warningEl = document.getElementById('withdrawBudgetWarning');
            var formEl = document.querySelector('form');
            var overrideInput = document.getElementById('withdrawOverrideBudget');

            var modalEl = document.getElementById('withdrawBudgetModal');
            var modalMsg = document.getElementById('withdrawModalMessage');
            var btnCancel = document.getElementById('btnCancelWithdrawPay');
            var btnConfirm = document.getElementById('btnConfirmWithdrawPay');

            var budgetExceededState = false;
            var budgetTimer = null;

            function checkWithdrawBudget() {
                var cat = categorySelect.value;
                var amt = amountInput.value.trim();
                if (!cat || !amt || parseFloat(amt) <= 0) {
                    warningEl.style.display = 'none';
                    budgetExceededState = false;
                    return;
                }

                fetch(contextPath + '/banking?action=checkBudget&category=' + encodeURIComponent(cat) + '&amount=' + encodeURIComponent(amt))
                    .then(function(res) { return res.json(); })
                    .then(function(data) {
                        if (data.hasBudget) {
                            budgetExceededState = data.exceeds;
                            if (data.exceeds) {
                                warningEl.className = 'budget-limit-warning exceeded-alert';
                                warningEl.innerHTML = '<span class="budget-warning-icon">⚠️</span><span><strong>Budget Limit Exceeded:</strong> Monthly limit for ' + data.category + ' is ₹' + Number(data.limit).toFixed(2) + '. Current spent: ₹' + Number(data.spent).toFixed(2) + ', Remaining: ₹' + Number(data.remaining).toFixed(2) + '. This withdrawal crosses your limit by ₹' + Number(data.excessAmount).toFixed(2) + '.</span>';
                                warningEl.style.display = 'flex';
                            } else {
                                warningEl.className = 'budget-limit-warning';
                                warningEl.innerHTML = '<span class="budget-warning-icon">ℹ️</span><span><strong>Budget Status:</strong> ' + data.category + ' limit is ₹' + Number(data.limit).toFixed(2) + '. Remaining after this withdrawal: ₹' + (Number(data.remaining) - Number(amt)).toFixed(2) + '.</span>';
                                warningEl.style.display = 'flex';
                            }
                        } else {
                            warningEl.style.display = 'none';
                            budgetExceededState = false;
                        }
                    })
                    .catch(function() {});
            }

            categorySelect.addEventListener('change', function() {
                clearTimeout(budgetTimer);
                budgetTimer = setTimeout(checkWithdrawBudget, 300);
            });

            amountInput.addEventListener('input', function() {
                clearTimeout(budgetTimer);
                budgetTimer = setTimeout(checkWithdrawBudget, 300);
            });

            formEl.addEventListener('submit', function(e) {
                if (overrideInput.value === 'true') return;
                if (budgetExceededState) {
                    e.preventDefault();
                    modalMsg.textContent = 'You are crossing your ' + categorySelect.value + ' budget amount limit! Do you still want to continue paying?';
                    modalEl.style.display = 'flex';
                }
            });

            btnCancel.addEventListener('click', function() {
                modalEl.style.display = 'none';
            });

            btnConfirm.addEventListener('click', function() {
                modalEl.style.display = 'none';
                overrideInput.value = 'true';
                formEl.submit();
            });
        })();
    </script>
</body>
</html>

