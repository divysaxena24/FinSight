<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Add Transaction</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">
            💰 FinSight
        </a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/addTransactionPage" style="color: var(--primary);">+ Add Transaction</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container">
        <div class="form-card">
            <div style="text-align: center; margin-bottom: 2rem;">
                <h2 style="font-size: 1.6rem; color: var(--dark);">Add Transaction</h2>
                <p style="color: var(--text-muted); font-size: 0.9rem; margin-top: 0.25rem;">
                    Record a new income or expense entry
                </p>
            </div>

            <c:if test="${not empty sessionScope.errorMessage}">
                <div class="alert alert-danger">
                    <span><c:out value="${sessionScope.errorMessage}"/></span>
                </div>
                <c:remove var="errorMessage" scope="session"/>
            </c:if>

            <form action="${pageContext.request.contextPath}/addTransaction" method="post" id="addTxForm">
                <input type="hidden" name="overrideBudget" id="expenseOverrideBudget" value="false">

                <div class="form-group">
                    <label for="type">Transaction Type</label>
                    <select id="type" name="type" class="form-control" required>
                        <option value="Expense" selected>Expense (Money Out)</option>
                        <option value="Income">Income (Money In)</option>
                    </select>
                </div>

                <div class="form-group">
                    <label for="category">Category</label>
                    <input type="text" id="category" name="category" list="categoryOptions" class="form-control" 
                           placeholder="e.g. Shopping, Food & Dining, Utilities" required autocomplete="off">
                    <datalist id="categoryOptions">
                        <option value="Shopping">
                        <option value="Food & Dining">
                        <option value="Utilities">
                        <option value="Entertainment">
                        <option value="Travel">
                        <option value="Healthcare">
                        <option value="Other">
                    </datalist>
                </div>

                <div class="form-group">
                    <label for="amount">Amount (₹)</label>
                    <input type="number" step="0.01" min="0.01" id="amount" name="amount" class="form-control" 
                           placeholder="0.00" required>
                    <div id="expenseBudgetWarning" class="budget-limit-warning" style="display: none;"></div>
                </div>

                <div class="form-group">
                    <label for="description">Description (Optional)</label>
                    <textarea id="description" name="description" class="form-control" 
                              placeholder="Add notes or merchant details..."></textarea>
                </div>

                <div class="form-group">
                    <label for="transactionDate">Date</label>
                    <input type="date" id="transactionDate" name="transactionDate" class="form-control" required>
                </div>

                <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 1rem;">
                    Save Transaction
                </button>
            </form>

            <div style="text-align: center; margin-top: 1.5rem;">
                <a href="${pageContext.request.contextPath}/dashboard" style="color: var(--text-muted); font-size: 0.9rem; text-decoration: none;">
                    ← Cancel and return to Dashboard
                </a>
            </div>
        </div>
    </div>

    <!-- Budget Exceeded Modal -->
    <div class="modal-backdrop-budget" id="expenseBudgetModal" style="display: none;">
        <div class="modal-card-budget">
            <div class="modal-budget-icon">⚠️</div>
            <h3 class="modal-budget-title">Budget Limit Exceeded</h3>
            <p id="expenseModalMessage" class="modal-budget-desc">
                You are crossing your budget amount limit! Do you still want to continue?
            </p>
            <div class="modal-budget-actions">
                <button type="button" class="btn btn-secondary" id="btnCancelExpensePay">Cancel / Change Amount</button>
                <button type="button" class="btn btn-danger" id="btnConfirmExpensePay" style="background: #dc2626; border-color: #dc2626; color: #fff;">Yes, Save Anyway</button>
            </div>
        </div>
    </div>

    <script>
        const contextPath = '${pageContext.request.contextPath}';
        document.getElementById('transactionDate').valueAsDate = new Date();

        const typeSelect = document.getElementById('type');
        const categoryInput = document.getElementById('category');
        const amountInput = document.getElementById('amount');
        const warningEl = document.getElementById('expenseBudgetWarning');
        const formEl = document.getElementById('addTxForm');
        const overrideInput = document.getElementById('expenseOverrideBudget');

        const modalEl = document.getElementById('expenseBudgetModal');
        const modalMsg = document.getElementById('expenseModalMessage');
        const btnCancel = document.getElementById('btnCancelExpensePay');
        const btnConfirm = document.getElementById('btnConfirmExpensePay');

        let budgetExceededState = false;
        let budgetTimer = null;

        function checkExpenseBudget() {
            if (typeSelect.value !== 'Expense') {
                warningEl.style.display = 'none';
                budgetExceededState = false;
                return;
            }
            const cat = categoryInput.value.trim();
            const amt = amountInput.value.trim();
            if (!cat || !amt || parseFloat(amt) <= 0) {
                warningEl.style.display = 'none';
                budgetExceededState = false;
                return;
            }

            fetch(contextPath + '/budgets?action=checkBudget&category=' + encodeURIComponent(cat) + '&amount=' + encodeURIComponent(amt))
                .then(res => res.json())
                .then(data => {
                    if (data.hasBudget) {
                        budgetExceededState = data.exceeds;
                        if (data.exceeds) {
                            warningEl.className = 'budget-limit-warning exceeded-alert';
                            warningEl.innerHTML = '<span class="budget-warning-icon">⚠️</span><span><strong>Budget Limit Exceeded:</strong> Monthly limit for ' + escapeHtml(data.category) + ' is ₹' + Number(data.limit).toFixed(2) + '. Current spent: ₹' + Number(data.spent).toFixed(2) + ', Remaining: ₹' + Number(data.remaining).toFixed(2) + '. This expense crosses your budget limit by ₹' + Number(data.excessAmount).toFixed(2) + '.</span>';
                            warningEl.style.display = 'flex';
                        } else {
                            warningEl.className = 'budget-limit-warning';
                            warningEl.innerHTML = '<span class="budget-warning-icon">ℹ️</span><span><strong>Budget Status:</strong> ' + escapeHtml(data.category) + ' limit is ₹' + Number(data.limit).toFixed(2) + '. Remaining after this expense: ₹' + (Number(data.remaining) - Number(amt)).toFixed(2) + '.</span>';
                            warningEl.style.display = 'flex';
                        }
                    } else {
                        warningEl.style.display = 'none';
                        budgetExceededState = false;
                    }
                })
                .catch(() => {});
        }

        function escapeHtml(t) {
            return (t || '').replace(/[&<>"']/g, m => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' })[m]);
        }

        function triggerCheck() {
            clearTimeout(budgetTimer);
            budgetTimer = setTimeout(checkExpenseBudget, 300);
        }

        categoryInput.addEventListener('input', triggerCheck);
        amountInput.addEventListener('input', triggerCheck);
        typeSelect.addEventListener('change', triggerCheck);

        formEl.addEventListener('submit', function(e) {
            if (overrideInput.value === 'true') return;
            if (typeSelect.value === 'Expense' && budgetExceededState) {
                e.preventDefault();
                modalMsg.textContent = 'You are crossing your ' + categoryInput.value + ' budget amount limit! Do you still want to continue saving this expense?';
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
    </script>
</body>
</html>
