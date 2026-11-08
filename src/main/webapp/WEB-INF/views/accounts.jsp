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
    <style>
        .account-card {
            background: #fff;
            border-radius: 12px;
            padding: 24px;
            box-shadow: 0 4px 6px -1px rgba(0,0,0,0.06);
            border: 1px solid #e2e8f0;
            transition: transform 0.15s ease;
        }
        .account-card:hover { transform: translateY(-2px); }
        .acc-number { font-family: monospace; font-size: 1.15rem; font-weight: 700; color: #1e293b; letter-spacing: 1px; }
        .acc-bal { font-size: 1.75rem; font-weight: 800; color: #0f172a; margin: 12px 0; }
    </style>
</head>
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts" style="color: var(--primary); font-weight: 700;">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container" style="max-width: 1100px; margin-top: 30px;">
        <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
            <div>
                <h1 class="page-title">🏦 Simulated Banking Accounts</h1>
                <p class="page-subtitle">Total Available Balance across all accounts: <strong>₹<fmt:formatNumber value="${totalBalance}" minFractionDigits="2"/></strong></p>
            </div>
            <div>
                <!-- Create Account Trigger -->
                <button type="button" class="btn btn-primary" onclick="document.getElementById('createModal').style.display='block'">
                    + Open New Account
                </button>
            </div>
        </div>

        <c:if test="${not empty sessionScope.successMessage}">
            <div class="alert alert-success" style="margin-bottom: 20px;">
                <span><c:out value="${sessionScope.successMessage}"/></span>
            </div>
            <c:remove var="successMessage" scope="session"/>
        </c:if>

        <c:if test="${not empty sessionScope.errorMessage}">
            <div class="alert alert-danger" style="margin-bottom: 20px;">
                <span><c:out value="${sessionScope.errorMessage}"/></span>
            </div>
            <c:remove var="errorMessage" scope="session"/>
        </c:if>

        <!-- Account Cards Grid -->
        <c:choose>
            <c:when test="${empty accounts}">
                <div class="card" style="padding: 40px; text-align: center;">
                    <h3>No Simulated Accounts Found</h3>
                    <p style="color: #64748b; margin: 10px 0 20px 0;">Open your first simulated savings or current account to deposit funds and execute simulated transfers.</p>
                    <button type="button" class="btn btn-primary" onclick="document.getElementById('createModal').style.display='block'">Open Savings Account</button>
                </div>
            </c:when>
            <c:otherwise>
                <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 20px;">
                    <c:forEach var="acc" items="${accounts}">
                        <div class="account-card">
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <span class="badge" style="background: ${acc.accountType == 'SAVINGS' ? '#e0f2fe' : '#fef3c7'}; color: ${acc.accountType == 'SAVINGS' ? '#0369a1' : '#b45309'}; padding: 4px 10px; border-radius: 20px; font-weight: 700; font-size: 0.75rem;">
                                    <c:out value="${acc.accountType}"/> ACCOUNT
                                </span>
                                <span style="font-size: 0.8rem; color: #16a34a; font-weight: 600;">● Active</span>
                            </div>
                            <div class="acc-bal">₹<fmt:formatNumber value="${acc.balance}" minFractionDigits="2"/></div>
                            <div style="margin-bottom: 15px;">
                                <div style="font-size: 0.75rem; color: #64748b; text-transform: uppercase;">Account Number</div>
                                <div class="acc-number"><c:out value="${acc.accountNumber}"/></div>
                            </div>
                            <div style="display: flex; gap: 10px;">
                                <a href="${pageContext.request.contextPath}/accounts?id=${acc.id}" class="btn btn-secondary btn-sm" style="flex: 1; text-align: center;">History</a>
                                <a href="${pageContext.request.contextPath}/banking?action=deposit&accountId=${acc.id}" class="btn btn-primary btn-sm" style="flex: 1; text-align: center;">Deposit</a>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </c:otherwise>
        </c:choose>

        <!-- Selected Account History Details -->
        <c:if test="${not empty selectedAccount}">
            <div class="card" style="margin-top: 35px; padding: 25px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <div>
                        <h2 style="font-size: 1.25rem; margin: 0;">Ledger History: <span style="font-family: monospace;"><c:out value="${selectedAccount.accountNumber}"/></span></h2>
                        <span style="font-size: 0.85rem; color: #64748b;">
                            Current Balance: <strong>₹<fmt:formatNumber value="${selectedAccount.balance}" minFractionDigits="2"/></strong>
                            &bull; Reconciled Ledger: <strong>₹<fmt:formatNumber value="${reconciledBalance}" minFractionDigits="2"/></strong>
                        </span>
                    </div>
                    <div>
                        <a href="${pageContext.request.contextPath}/banking?action=transfer&sourceAccountId=${selectedAccount.id}" class="btn btn-primary btn-sm">Transfer from this Account</a>
                    </div>
                </div>

                <div class="table-responsive">
                    <table class="table" style="width: 100%; border-collapse: collapse;">
                        <thead>
                            <tr style="border-bottom: 2px solid #e2e8f0; text-align: left; font-size: 0.85rem; color: #64748b;">
                                <th>Reference</th>
                                <th>Date</th>
                                <th>Type</th>
                                <th>Description</th>
                                <th>Category</th>
                                <th style="text-align: right;">Amount</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="tx" items="${accountHistory}">
                                <tr style="border-bottom: 1px solid #f1f5f9; font-size: 0.9rem;">
                                    <td style="font-family: monospace; font-weight: 600;"><c:out value="${tx.referenceNumber}"/></td>
                                    <td><c:out value="${tx.createdAt}"/></td>
                                    <td>
                                        <span class="badge" style="background: #f1f5f9; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: 700;">
                                            <c:out value="${tx.transactionType}"/>
                                        </span>
                                    </td>
                                    <td><c:out value="${tx.description}"/></td>
                                    <td><c:out value="${tx.category}"/></td>
                                    <td style="text-align: right; font-weight: 700;">
                                        <c:choose>
                                            <c:when test="${tx.destinationAccountId == selectedAccount.id}">
                                                <span style="color: #16a34a;">+₹<fmt:formatNumber value="${tx.amount}" minFractionDigits="2"/></span>
                                            </c:when>
                                            <c:otherwise>
                                                <span style="color: #dc2626;">-₹<fmt:formatNumber value="${tx.amount}" minFractionDigits="2"/></span>
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
        <div id="createModal" style="display: none; position: fixed; z-index: 1000; left: 0; top: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5);">
            <div style="background: #fff; max-width: 450px; margin: 10% auto; padding: 30px; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.2);">
                <h3 style="margin-top: 0;">Open New Simulated Account</h3>
                <form action="${pageContext.request.contextPath}/accounts" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="create">

                    <div class="form-group" style="margin-bottom: 20px;">
                        <label>Account Type</label>
                        <select name="accountType" class="form-control" required>
                            <option value="SAVINGS">Savings Account</option>
                            <option value="CURRENT">Current Account</option>
                        </select>
                    </div>

                    <div style="display: flex; gap: 10px; justify-content: flex-end;">
                        <button type="button" class="btn btn-secondary" onclick="document.getElementById('createModal').style.display='none'">Cancel</button>
                        <button type="submit" class="btn btn-primary">Create Account</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</body>
</html>

