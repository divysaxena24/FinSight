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
    <style>
        .table-responsive { overflow-x: auto; margin-top: 15px; }
        .badge-income { background: #dcfce7; color: #15803d; padding: 4px 8px; border-radius: 4px; font-weight: 600; font-size: 0.75rem; }
        .badge-expense { background: #fee2e2; color: #b91c1c; padding: 4px 8px; border-radius: 4px; font-weight: 600; font-size: 0.75rem; }
        .badge-refund { background: #e0e7ff; color: #4338ca; padding: 4px 8px; border-radius: 4px; font-weight: 600; font-size: 0.75rem; }
        .badge-duplicate { background: #fef3c7; color: #b45309; padding: 4px 8px; border-radius: 4px; font-weight: 600; font-size: 0.75rem; }
        .editable-cell { border: 1px solid transparent; padding: 4px; border-radius: 4px; }
        .editable-cell:focus { border-color: var(--primary); outline: none; background: #fff; }
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
                <form action="${pageContext.request.contextPath}/statement-analysis?action=import" method="post" style="display: inline;">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="stagingId" value="${staging.stagingId}">
                    <button type="submit" class="btn btn-success" id="importBtn" style="font-weight: 700; padding: 10px 20px;">
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
        <div class="card" style="margin-top: 15px; padding: 12px 18px; background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 6px;">
            <div style="font-size: 0.9rem; color: #166534;">
                <strong>ℹ️ Import Policy:</strong> Only checked rows will be imported into your ledger. 
                <span style="color: #991b1b; font-weight: 600;">Confirmed duplicates</span> and 
                <span style="color: #4338ca; font-weight: 600;">Account Transfers</span> are automatically excluded from Income/Expense metrics upon import.
            </div>
        </div>

        <c:if test="${not empty result.unparsedLines}">
            <div class="alert alert-warning" style="margin-top: 15px;">
                <strong>⚠️ Partial Parsing Notice:</strong> ${result.unparsedLineCount} line(s) could not be parsed:
                <ul style="margin-top: 5px; margin-left: 20px;">
                    <c:forEach var="unparsed" items="${result.unparsedLines}">
                        <li><code><c:out value="${unparsed}"/></code></li>
                    </c:forEach>
                </ul>
            </div>
        </c:if>

        <!-- Search & Filter Controls -->
        <div class="card" style="margin-top: 15px; padding: 15px;">
            <div style="display: flex; gap: 15px; align-items: center;">
                <input type="text" id="searchInput" placeholder="Search narration or merchant..." class="form-control" style="flex: 2;">
                <select id="categoryFilter" class="form-control" style="flex: 1;">
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
        <div class="card" style="margin-top: 20px; padding: 15px;">
            <div class="table-responsive">
                <table class="table" style="width: 100%; border-collapse: collapse;">
                    <thead>
                        <tr style="border-bottom: 2px solid #e2e8f0; text-align: left;">
                            <th style="width: 40px;"><input type="checkbox" id="masterCheckbox" onchange="toggleMaster(this)"></th>
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
                            <tr style="border-bottom: 1px solid #f1f5f9;" data-tx-id="${item.stagingTxId}" class="tx-row">
                                <td>
                                    <input type="checkbox" class="tx-select" ${item.selected ? 'checked' : ''} 
                                           onchange="updateTx('${item.stagingTxId}', {selected: this.checked})">
                                </td>
                                <td>
                                    <input type="date" value="${item.transaction.transactionDate}" class="editable-cell" 
                                           onchange="updateTx('${item.stagingTxId}', {date: this.value})">
                                </td>
                                <td>
                                    <input type="text" value="<c:out value="${item.transaction.description}"/>" class="editable-cell" style="width: 100%;"
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
                                    <input type="number" step="0.01" value="${item.transaction.amount}" class="editable-cell" style="width: 90px; text-align: right;"
                                           onchange="updateTx('${item.stagingTxId}', {amount: this.value})">
                                </td>
                                <td>
                                    <c:if test="${item.transaction.duplicateCandidate}">
                                        <span class="badge-duplicate">Suspected Duplicate</span>
                                    </c:if>
                                    <c:if test="${item.transaction.confirmedDuplicate}">
                                        <span class="badge-duplicate" style="background: #fecaca; color: #991b1b;">Confirmed Duplicate</span>
                                    </c:if>
                                    <c:if test="${item.transaction.manuallyCategorized}">
                                        <span style="font-size: 0.75rem; color: #64748b;">(Edited)</span>
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

