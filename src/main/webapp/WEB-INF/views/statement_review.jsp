<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Review Transactions</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="statement-review-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <button type="button" class="statement-review-mobile-toggle" aria-controls="statement-review-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="statement-review-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/statement-analysis" class="active" aria-current="page">Statement Analysis</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container statement-review-container">
        <div class="page-header statement-review-header">
            <div>
                <h1 class="page-title">🔍 Statement Review & Edit</h1>
                <p class="page-subtitle">
                    Recognized Format: <strong><c:out value="${staging.bankName}"/></strong> 
                    (Confidence: <fmt:formatNumber value="${staging.detectionConfidence * 100}" maxFractionDigits="0"/>%)
                    &bull; Valid Transactions: <strong>${staging.totalCount}</strong>
                    <c:if test="${not empty result.unparsedLines}">
                        &bull; Unparsed Lines: <strong>${result.unparsedLineCount}</strong>
                    </c:if>
                </p>
            </div>
            <div>
                <form action="${pageContext.request.contextPath}/statement-analysis?action=import" method="post" class="statement-import-form">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="stagingId" value="${staging.stagingId}">
                    <button type="submit" class="btn btn-success statement-import-button" id="importBtn">
                        ✅ Approve & Import Selected (<span id="selectedCountDisplay">${staging.selectedCount}</span>)
                    </button>
                </form>
            </div>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger">
                <span><c:out value="${errorMessage}"/></span>
            </div>
        </c:if>

        <!-- Import Guidance Notice -->
        <div class="card statement-policy-card">
            <div>
                <strong>ℹ️ Import Policy:</strong> Only checked rows will be imported into your ledger. 
                <span class="statement-duplicate-text">Confirmed duplicates</span> and
                <span class="statement-transfer-text">Account Transfers</span> are automatically excluded from Income/Expense metrics upon import.
            </div>
        </div>

        <c:if test="${not empty result.unparsedLines}">
            <div class="alert alert-warning statement-unparsed-alert">
                <strong>⚠️ Partial Parsing Notice:</strong> ${result.unparsedLineCount} line(s) could not be parsed:
                <ul>
                    <c:forEach var="unparsed" items="${result.unparsedLines}">
                        <li><code><c:out value="${unparsed}"/></code></li>
                    </c:forEach>
                </ul>
            </div>
        </c:if>

        <!-- Search & Filter Controls -->
        <div class="card statement-filter-card">
            <div class="statement-filter-controls">
                <input type="text" id="searchInput" placeholder="Search narration or merchant..." class="form-control statement-search-input">
                <select id="categoryFilter" class="form-control statement-category-filter">
                    <option value="">All Categories</option>
                    <option value="Food & Dining">Food & Dining</option>
                    <option value="Shopping">Shopping</option>
                    <option value="Transport">Transport</option>
                    <option value="Utilities">Utilities</option>
                    <option value="Housing & Rent">Housing & Rent</option>
                    <option value="Entertainment">Entertainment</option>
                    <option value="Salary">Salary</option>
                    <option value="Other">Other</option>
                </select>
                <button type="button" class="btn btn-secondary" onclick="selectAll(true)">Select All</button>
                <button type="button" class="btn btn-secondary" onclick="selectAll(false)">Deselect All</button>
            </div>
        </div>

        <!-- Transactions Table -->
        <div class="card statement-review-table-card">
            <div class="table-responsive statement-review-table-wrapper">
                <table class="table statement-review-table">
                    <thead>
                        <tr>
                            <th class="statement-select-column"><input type="checkbox" id="masterCheckbox" onchange="toggleMaster(this)"></th>
                            <th>Date</th>
                            <th>Description</th>
                            <th>Merchant</th>
                            <th>Category</th>
                            <th>Type</th>
                            <th>Amount</th>
                            <th>Status</th>
                        </tr>
                    </thead>
                    <tbody id="txTableBody">
                        <c:forEach var="item" items="${staging.itemsList}">
                            <tr data-tx-id="${item.stagingTxId}" class="tx-row">
                                <td>
                                    <input type="checkbox" class="tx-select" ${item.selected ? 'checked' : ''} 
                                           onchange="updateTx('${item.stagingTxId}', {selected: this.checked})">
                                </td>
                                <td>
                                    <input type="date" value="${item.transaction.transactionDate}" class="editable-cell" 
                                           onchange="updateTx('${item.stagingTxId}', {date: this.value})">
                                </td>
                                <td>
                                    <input type="text" value="<c:out value="${item.transaction.description}"/>" class="editable-cell statement-description-input"
                                           onchange="updateTx('${item.stagingTxId}', {description: this.value})">
                                </td>
                                <td>
                                    <input type="text" value="<c:out value="${item.transaction.merchant}"/>" class="editable-cell"
                                           onchange="updateTx('${item.stagingTxId}', {merchant: this.value})">
                                </td>
                                <td>
                                    <select class="editable-cell" onchange="updateTx('${item.stagingTxId}', {category: this.value})">
                                        <option value="${item.transaction.category}" selected><c:out value="${item.transaction.category}"/></option>
                                        <option value="Food & Dining">Food & Dining</option>
                                        <option value="Shopping">Shopping</option>
                                        <option value="Transport">Transport</option>
                                        <option value="Utilities">Utilities</option>
                                        <option value="Housing & Rent">Housing & Rent</option>
                                        <option value="Healthcare">Healthcare</option>
                                        <option value="Entertainment">Entertainment</option>
                                        <option value="Salary">Salary</option>
                                        <option value="Other">Other</option>
                                    </select>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${item.transaction.refundOrReversal}">
                                            <span class="badge-refund">Refund</span>
                                        </c:when>
                                        <c:when test="${item.transaction.transactionType == 'INCOME'}">
                                            <span class="badge-income">Income</span>
                                        </c:when>
                                        <c:otherwise>
                                            <span class="badge-expense">Expense</span>
                                        </c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <input type="number" step="0.01" value="${item.transaction.amount}" class="editable-cell statement-amount-input"
                                           onchange="updateTx('${item.stagingTxId}', {amount: this.value})">
                                </td>
                                <td>
                                    <c:if test="${item.transaction.duplicateCandidate}">
                                        <span class="badge-duplicate">Suspected Duplicate</span>
                                    </c:if>
                                    <c:if test="${item.transaction.confirmedDuplicate}">
                                        <span class="badge-duplicate statement-confirmed-duplicate">Confirmed Duplicate</span>
                                    </c:if>
                                    <c:if test="${item.transaction.manuallyCategorized}">
                                        <span class="statement-edited-label">(Edited)</span>
                                    </c:if>
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
            var navToggle = document.querySelector('.statement-review-mobile-toggle');
            var navLinks = document.getElementById('statement-review-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('statement-review-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }
        })();

        const stagingId = "${staging.stagingId}";
        const csrfToken = "${csrfToken}";

        function updateTx(stagingTxId, data) {
            const formData = new URLSearchParams();
            formData.append("csrfToken", csrfToken);
            formData.append("stagingId", stagingId);
            formData.append("stagingTxId", stagingTxId);
            for (const key in data) {
                formData.append(key, data[key]);
            }

            fetch("${pageContext.request.contextPath}/statement-analysis?action=update-tx", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: formData.toString()
            })
            .then(res => res.json())
            .then(json => {
                if (json.selectedCount !== undefined) {
                    document.getElementById("selectedCountDisplay").innerText = json.selectedCount;
                }
            })
            .catch(err => console.error("Error updating transaction:", err));
        }

        function selectAll(checked) {
            document.querySelectorAll(".tx-select").forEach(cb => {
                cb.checked = checked;
                const tr = cb.closest("tr");
                const txId = tr.getAttribute("data-tx-id");
                updateTx(txId, {selected: checked});
            });
        }

        function toggleMaster(master) {
            selectAll(master.checked);
        }

        // Live search & category filter
        document.getElementById("searchInput").addEventListener("input", filterRows);
        document.getElementById("categoryFilter").addEventListener("change", filterRows);

        function filterRows() {
            const query = document.getElementById("searchInput").value.toLowerCase();
            const cat = document.getElementById("categoryFilter").value.toLowerCase();

            document.querySelectorAll(".tx-row").forEach(row => {
                const text = row.innerText.toLowerCase();
                const matchQuery = !query || text.includes(query);
                const matchCat = !cat || text.includes(cat);
                row.style.display = (matchQuery && matchCat) ? "" : "none";
            });
        }
    </script>
</body>
</html>

