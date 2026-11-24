<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Transfer Funds</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="transfer-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <div class="nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer" style="color: var(--primary); font-weight: 700;">Transfer</a>
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

    <div class="container transfer-container">
        <div class="page-header transfer-header">
            <div>
                <h1 class="page-title">🔄 Fund Transfer</h1>
                <p class="page-subtitle">Choose between manual entry or instant QR-based transfer to send funds securely</p>
            </div>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger" style="margin-bottom: 20px;">
                <span><c:out value="${errorMessage}"/></span>
            </div>
        </c:if>

        <!-- Quick Banner to View / Share My Own QR Code -->
        <div class="receive-qr-prompt">
            <div class="receive-qr-prompt-info">
                <span style="font-size: 1.25rem;">📲</span>
                <span>Want someone to transfer funds to you? Share your Account QR Code.</span>
            </div>
            <button type="button" class="btn-open-my-qr" id="openMyQrBtn">
                Show My QR Code
            </button>
        </div>

        <!-- Transfer Mode Switcher: Manual vs QR Transaction -->
        <div class="transfer-mode-switcher" role="tablist">
            <button type="button" class="transfer-mode-btn active" id="manualModeBtn" onclick="switchTransferMode('manual')" role="tab" aria-selected="true" aria-controls="manualSection">
                <span class="transfer-mode-icon">✍️</span>
                <span class="transfer-mode-text">
                    <span class="transfer-mode-title">Manual Transaction</span>
                    <span class="transfer-mode-desc">Enter account number and details manually</span>
                </span>
            </button>
            <button type="button" class="transfer-mode-btn" id="qrModeBtn" onclick="switchTransferMode('qr')" role="tab" aria-selected="false" aria-controls="qrSection">
                <span class="transfer-mode-icon">⚡</span>
                <span class="transfer-mode-text">
                    <span class="transfer-mode-title">QR Code Transaction</span>
                    <span class="transfer-mode-desc">Scan, upload, or 1-tap for fastest transfer</span>
                </span>
            </button>
        </div>

        <!-- ============================================================== -->
        <!-- OPTION 1: MANUAL TRANSACTION SECTION                           -->
        <!-- ============================================================== -->
        <div id="manualSection" class="transfer-card" role="tabpanel" aria-labelledby="manualModeBtn">
            <form action="${pageContext.request.contextPath}/banking" method="post" id="manualTransferForm">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <input type="hidden" name="action" value="transfer">
                <input type="hidden" name="transferMode" value="manual">
                <input type="hidden" name="idempotencyKey" value="<%= java.util.UUID.randomUUID().toString() %>">

                <div class="form-group" style="margin-bottom: 20px;">
                    <label for="manualSourceAccount" style="font-weight: 600;">From: Source Account</label>
                    <select id="manualSourceAccount" name="sourceAccountId" class="form-control" required>
                        <c:forEach var="acc" items="${accounts}">
                            <option value="${acc.id}" ${param.sourceAccountId == acc.id ? 'selected' : ''}>
                                <c:out value="${acc.accountNumber}"/> (<c:out value="${acc.accountType}"/>) — Balance: ₹<fmt:formatNumber value="${acc.balance}" minFractionDigits="2"/>
                            </option>
                        </c:forEach>
                    </select>
                </div>

                <div class="form-group" style="margin-bottom: 20px;">
                    <label for="manualRecipientAccount" style="font-weight: 600;">To: Recipient Account Number</label>
                    <input type="text" id="manualRecipientAccount" name="recipientAccountNumber" class="form-control"
                           placeholder="e.g. FS-1234567890" required style="font-family: monospace; font-size: 1rem;" autocomplete="off"
                           value="${paramRecipientAccountNumber != null ? paramRecipientAccountNumber : ''}">
                    <div id="manualValidationStatus" class="account-validation-status">
                        <small style="color: #64748b;">Enter the full recipient FinSight account number.</small>
                    </div>
                </div>

                <div class="form-group" style="margin-bottom: 20px;">
                    <label for="manualAmount" style="font-weight: 600;">Transfer Amount (₹)</label>
                    <input type="number" id="manualAmount" step="0.01" min="1" max="10000000" name="amount"
                           class="form-control" placeholder="e.g. 2000.00" required
                           value="${paramAmount != null ? paramAmount : ''}">
                    <div id="manualBudgetWarning" class="budget-limit-warning" style="display: none;"></div>
                </div>

                <div class="form-group" style="margin-bottom: 20px;">
                    <label for="manualCategory" style="font-weight: 600;">Category</label>
                    <select id="manualCategory" name="category" class="form-control" required>
                        <option value="Shopping" ${paramCategory == 'Shopping' ? 'selected' : ''}>Shopping</option>
                        <option value="Food" ${paramCategory == 'Food' || paramCategory == 'Food & Dining' ? 'selected' : ''}>Food</option>
                        <option value="Rent" ${paramCategory == 'Rent' ? 'selected' : ''}>Rent</option>
                        <option value="Bills & Utilities" ${paramCategory == 'Bills & Utilities' || paramCategory == 'Utilities' ? 'selected' : ''}>Bills & Utilities</option>
                        <option value="Entertainment" ${paramCategory == 'Entertainment' ? 'selected' : ''}>Entertainment</option>
                        <option value="Travel" ${paramCategory == 'Travel' ? 'selected' : ''}>Travel</option>
                        <option value="Healthcare" ${paramCategory == 'Healthcare' ? 'selected' : ''}>Healthcare</option>
                        <option value="General Transfer" ${paramCategory == 'General Transfer' || paramCategory == 'Transfer' ? 'selected' : ''}>General Transfer</option>
                        <option value="Other" ${paramCategory == 'Other' ? 'selected' : ''}>Other</option>
                    </select>
                </div>

                <input type="hidden" name="description" id="manualDescription" value="Transfer">
                <input type="hidden" name="overrideBudget" id="manualOverrideBudget" value="false">

                <div class="form-group" style="margin-bottom: 25px;">
                    <label for="manualPin" style="color: #0f172a; font-weight: 700;">🔑 Authorize with Transaction PIN</label>
                    <input type="password" id="manualPin" name="transactionPin" class="form-control"
                           required pattern="\d{4,6}" maxlength="6" placeholder="Enter 4–6 digit PIN" autocomplete="off">
                </div>

                <button type="submit" class="btn btn-primary" style="width: 100%; padding: 12px; font-size: 1rem; font-weight: 600;">
                    Authorize & Transfer Funds
                </button>
            </form>
        </div>

        <!-- ============================================================== -->
        <!-- OPTION 2: QR BASED TRANSACTION SECTION                         -->
        <!-- ============================================================== -->
        <div id="qrSection" class="transfer-card" role="tabpanel" aria-labelledby="qrModeBtn" style="display: none;">
            <div id="qrLibNotice" class="alert alert-warning" style="display: none; margin-bottom: 1.5rem;">
                <strong>Notice:</strong> Camera scanner engine is unavailable. You can still use the <strong>Quick Payees</strong> or <strong>Paste Code</strong> options for instant transfers.
            </div>
            
            <!-- STAGE A: QR SCAN / SELECTION VIEW -->
            <div id="qrScannerStage">
                <!-- QR Sub-tabs (Camera, Upload, Quick Directory, Paste) -->
                <div class="qr-methods-nav">
                    <button type="button" class="qr-method-tab active" id="tabCamera">
                        <span>📷</span> Camera Scan
                    </button>
                    <button type="button" class="qr-method-tab" id="tabUpload">
                        <span>📁</span> Upload Image
                    </button>
                    <button type="button" class="qr-method-tab" id="tabPaste">
                        <span>📋</span> Paste Code
                    </button>
                    <button type="button" class="qr-method-tab" id="tabDirectory">
                        <span>⚡</span> Quick Payees
                    </button>
                </div>

                <!-- 1. Live Camera Scan View -->
                <div id="viewCamera" class="qr-method-view">
                    <div class="qr-camera-viewport" id="cameraViewport">
                        <video id="qrVideo" class="qr-camera-video" muted playsinline></video>
                        <div class="qr-viewfinder-overlay" id="qrOverlay" style="display: none;">
                            <div class="qr-laser-line"></div>
                        </div>
                        <div class="qr-camera-status" id="cameraStatus">
                            Camera inactive. Click 'Start Scanner' below.
                        </div>
                    </div>

                    <div style="display: flex; gap: 0.75rem; justify-content: center; margin-bottom: 1.5rem;">
                        <button type="button" class="btn btn-primary" id="btnToggleCamera">
                            ▶️ Start Camera Scanner
                        </button>
                    </div>
                </div>

                <!-- 2. Upload QR Image View -->
                <div id="viewUpload" class="qr-method-view" style="display: none;">
                    <div class="qr-upload-box" id="qrDropZone">
                        <input type="file" id="qrFileInput" accept="image/*" style="display: none;">
                        <span class="qr-upload-icon">🖼️</span>
                        <div class="qr-upload-title">Drop QR Code Image Here</div>
                        <div class="qr-upload-subtitle">or click to browse from device (JPG, PNG, WebP)</div>
                    </div>
                    <div id="uploadFeedback" style="text-align: center; font-size: 0.85rem; color: #dc2626; min-height: 1.25rem;"></div>
                </div>

                <!-- 3. Direct Paste / Code View -->
                <div id="viewPaste" class="qr-method-view" style="display: none;">
                    <div style="background: #f8fafc; border: 1px solid var(--border); border-radius: 8px; padding: 1.5rem; text-align: left; max-width: 600px; margin: 0 auto 1.5rem auto;">
                        <label for="qrRawTextInput" style="font-weight: 600; display: block; margin-bottom: 0.5rem; color: #0f172a;">
                            Paste QR Payload, FinSight URI, or Account Number:
                        </label>
                        <input type="text" id="qrRawTextInput" class="form-control" placeholder="e.g. FS-4183970722 or finsight://pay?acc=FS-4183970722" style="font-family: monospace; font-size: 0.95rem; margin-bottom: 1rem;">
                        <div style="display: flex; gap: 0.75rem;">
                            <button type="button" class="btn btn-primary" id="btnProcessPaste" style="font-weight: 600;">
                                ⚡ Process QR & Continue
                            </button>
                        </div>
                    </div>
                </div>

                <!-- 4. Quick Payee Directory (Fast 1-click Pay) -->
                <div id="viewDirectory" class="qr-method-view" style="display: none;">
                    <div class="quick-payees-wrapper">
                        <div class="quick-payees-header">
                            <span>Select a recipient for instant auto-filled transfer:</span>
                        </div>
                        <c:choose>
                            <c:when test="${not empty otherAccounts}">
                                <div class="quick-payees-grid">
                                    <c:forEach var="payee" items="${otherAccounts}">
                                        <div class="quick-payee-card">
                                            <div>
                                                <div class="quick-payee-avatar">
                                                    <c:out value="${payee.holderName != null ? payee.holderName.substring(0, 1).toUpperCase() : 'U'}"/>
                                                </div>
                                                <div class="quick-payee-name" title="${payee.holderName}">
                                                    <c:out value="${payee.holderName != null ? payee.holderName : 'Verified User'}"/>
                                                </div>
                                                <div class="quick-payee-acc"><c:out value="${payee.accountNumber}"/></div>
                                            </div>
                                            <button type="button" class="btn-quick-pay"
                                                    data-acc="${payee.accountNumber}"
                                                    data-name="${payee.holderName}">
                                                ⚡ Scan & Pay
                                            </button>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <div style="text-align: center; padding: 2rem; color: #64748b; background: #f8fafc; border-radius: 8px;">
                                    No other users found in the directory. You can scan or upload any FinSight QR code!
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <!-- STAGE B: SCANNED PAYEE & INSTANT CONFIRMATION FORM -->
            <div id="qrConfirmationStage" style="display: none;">
                <!-- Verified Payee Badge Banner -->
                <div class="verified-payee-banner">
                    <div class="verified-payee-info">
                        <div class="verified-shield-icon">✓</div>
                        <div class="verified-payee-details">
                            <h3 id="scannedHolderName">Verified Recipient</h3>
                            <div class="verified-payee-acc-tag" id="scannedAccountBadge">FS-0000000000</div>
                        </div>
                    </div>
                    <button type="button" class="btn-rescan-qr" id="btnRescanQR">
                        🔄 Scan Another QR
                    </button>
                </div>

                <div id="qrSelfTransferAlert" class="alert alert-warning" style="display: none; margin-bottom: 15px;"></div>

                <!-- Fast Execution Form -->
                <form action="${pageContext.request.contextPath}/banking" method="post" id="qrTransferForm">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="transfer">
                    <input type="hidden" name="transferMode" value="qr">
                    <input type="hidden" name="idempotencyKey" value="<%= java.util.UUID.randomUUID().toString() %>">
                    <input type="hidden" id="qrRecipientAccount" name="recipientAccountNumber" value="">

                    <div class="form-group" style="margin-bottom: 20px;">
                        <label for="qrSourceAccount" style="font-weight: 600;">From: Source Account</label>
                        <select id="qrSourceAccount" name="sourceAccountId" class="form-control" required>
                            <c:forEach var="acc" items="${accounts}">
                                <option value="${acc.id}" data-acc="${acc.accountNumber}" ${paramSourceAccountId == acc.id ? 'selected' : ''}>
                                    <c:out value="${acc.accountNumber}"/> (<c:out value="${acc.accountType}"/>) — Balance: ₹<fmt:formatNumber value="${acc.balance}" minFractionDigits="2"/>
                                </option>
                            </c:forEach>
                        </select>
                    </div>

                    <div class="form-group" style="margin-bottom: 20px;">
                        <label for="qrAmount" style="font-weight: 600;">Transfer Amount (₹)</label>
                        <input type="number" id="qrAmount" step="0.01" min="1" max="10000000" name="amount"
                               class="form-control" placeholder="e.g. 500.00" required autofocus>
                        <div id="qrBudgetWarning" class="budget-limit-warning" style="display: none;"></div>
                    </div>

                    <div class="form-group" style="margin-bottom: 20px;">
                        <label for="qrCategory" style="font-weight: 600;">Category</label>
                        <select id="qrCategory" name="category" class="form-control" required>
                            <option value="Shopping">Shopping</option>
                            <option value="Food">Food</option>
                            <option value="Rent">Rent</option>
                            <option value="Bills & Utilities">Bills & Utilities</option>
                            <option value="Entertainment">Entertainment</option>
                            <option value="Travel">Travel</option>
                            <option value="Healthcare">Healthcare</option>
                            <option value="General Transfer" selected>General Transfer</option>
                            <option value="Other">Other</option>
                        </select>
                    </div>

                    <input type="hidden" name="description" id="qrDescription" value="Instant QR Payment">
                    <input type="hidden" name="overrideBudget" id="qrOverrideBudget" value="false">

                    <div class="form-group" style="margin-bottom: 25px;">
                        <label for="qrPin" style="color: #0f172a; font-weight: 700;">🔑 Authorize with Transaction PIN</label>
                        <input type="password" id="qrPin" name="transactionPin" class="form-control"
                               required pattern="\d{4,6}" maxlength="6" placeholder="Enter 4–6 digit PIN" autocomplete="off">
                    </div>

                    <button type="submit" id="qrSubmitBtn" class="btn btn-primary" style="width: 100%; padding: 12px; font-size: 1rem; font-weight: 700; background: #16a34a; border-color: #16a34a;">
                        ⚡ Complete Instant Transfer
                    </button>
                </form>
            </div>

        </div>
    </div>

    <!-- ============================================================== -->
    <!-- MODAL: MY ACCOUNT QR CODE (RECEIVE MONEY)                      -->
    <!-- ============================================================== -->
    <div class="qr-modal-backdrop" id="myQrModal">
        <div class="qr-modal-card">
            <button type="button" class="qr-modal-close" id="closeMyQrBtn" aria-label="Close">&times;</button>
            <h2 style="font-size: 1.25rem; font-weight: 700; color: #0f172a; margin-bottom: 0.25rem;">
                Receive Funds via QR
            </h2>
            <p style="font-size: 0.85rem; color: #64748b; margin-bottom: 1.25rem;">
                Let others scan this code to transfer money directly into your account
            </p>

            <c:choose>
                <c:when test="${not empty accounts}">
                    <div style="margin-bottom: 1rem; text-align: left;">
                        <label style="font-size: 0.825rem; font-weight: 600; color: #475569;">Select Receiving Account:</label>
                        <select id="myQrAccountSelect" class="form-control" style="font-size: 0.875rem;">
                            <c:forEach var="acc" items="${accounts}">
                                <option value="${acc.accountNumber}" data-name="${user.name}">
                                    <c:out value="${acc.accountNumber}"/> (<c:out value="${acc.accountType}"/>)
                                </option>
                            </c:forEach>
                        </select>
                    </div>

                    <div style="margin-bottom: 1rem; text-align: left;">
                        <label style="font-size: 0.825rem; font-weight: 600; color: #475569;">Optional: Request Specific Amount (₹):</label>
                        <input type="number" id="myQrCustomAmount" class="form-control" placeholder="Leave empty for any amount" min="1" step="0.01" style="font-size: 0.875rem;">
                    </div>

                    <div class="qr-code-wrapper" id="myQrCodeContainer"></div>

                    <div>
                        <div class="qr-acc-copy-pill" id="copyAccPill" title="Click to copy account number">
                            <span id="displayAccountCopy">FS-0000000000</span>
                            <span style="font-size: 0.8rem;">📋 Copy</span>
                        </div>
                    </div>

                    <div style="display: flex; gap: 0.5rem; justify-content: center;">
                        <button type="button" class="btn btn-secondary btn-sm" id="btnDownloadQr">
                            ⬇️ Download QR Image
                        </button>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="alert alert-warning">
                        You do not have an active account yet to generate a QR code. Please open an account first!
                    </div>
                </c:otherwise>
            </c:choose>
        </div>
    </div>

    <!-- ============================================================== -->
    <!-- MODAL: BUDGET LIMIT EXCEEDED WARNING & CONFIRMATION            -->
    <!-- ============================================================== -->
    <div class="modal-backdrop-budget" id="budgetConfirmModal" style="display: none;">
        <div class="modal-card-budget">
            <div class="modal-budget-icon">⚠️</div>
            <h3 class="modal-budget-title">Budget Limit Exceeded</h3>
            <p id="budgetModalMessage" class="modal-budget-desc">
                You are crossing your budget amount limit! Do you still want to continue paying?
            </p>
            <div class="modal-budget-actions">
                <button type="button" class="btn btn-secondary" id="btnCancelBudgetPay">Cancel / Change Amount</button>
                <button type="button" class="btn btn-danger" id="btnConfirmBudgetPay" style="background: #dc2626; border-color: #dc2626; color: #fff;">Yes, Continue Paying</button>
            </div>
        </div>
    </div>

    <!-- Scripts -->
    <script src="${pageContext.request.contextPath}/js/jsqr.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/qrcode.min.js"></script>
    <script src="${pageContext.request.contextPath}/js/qr-scanner.js"></script>
    <script>
    // Global Tab Switching function: available immediately before any DOM event
    window.switchTransferMode = function(mode) {
        try {
            const manualModeBtn = document.getElementById('manualModeBtn');
            const qrModeBtn = document.getElementById('qrModeBtn');
            const manualSection = document.getElementById('manualSection');
            const qrSection = document.getElementById('qrSection');

            if (mode === 'qr') {
                if (qrModeBtn) {
                    qrModeBtn.classList.add('active');
                    qrModeBtn.setAttribute('aria-selected', 'true');
                }
                if (manualModeBtn) {
                    manualModeBtn.classList.remove('active');
                    manualModeBtn.setAttribute('aria-selected', 'false');
                }
                if (manualSection) manualSection.style.display = 'none';
                if (qrSection) qrSection.style.display = 'block';
            } else {
                if (manualModeBtn) {
                    manualModeBtn.classList.add('active');
                    manualModeBtn.setAttribute('aria-selected', 'true');
                }
                if (qrModeBtn) {
                    qrModeBtn.classList.remove('active');
                    qrModeBtn.setAttribute('aria-selected', 'false');
                }
                if (manualSection) manualSection.style.display = 'block';
                if (qrSection) qrSection.style.display = 'none';
                if (window.qrScannerInstance && typeof window.qrScannerInstance.stopCamera === 'function') {
                    window.qrScannerInstance.stopCamera();
                }
            }
        } catch (e) {
            console.error('switchTransferMode error:', e);
        }
    };

    document.addEventListener('DOMContentLoaded', function() {
        const contextPath = '${pageContext.request.contextPath}';

        // -------------------------------------------------------------
        // 1. Tab Switching: Manual vs QR Mode
        // -------------------------------------------------------------
        const manualModeBtn = document.getElementById('manualModeBtn');
        const qrModeBtn = document.getElementById('qrModeBtn');
        const manualSection = document.getElementById('manualSection');
        const qrSection = document.getElementById('qrSection');

        if (manualModeBtn) {
            manualModeBtn.addEventListener('click', () => window.switchTransferMode('manual'));
        }
        if (qrModeBtn) {
            qrModeBtn.addEventListener('click', () => window.switchTransferMode('qr'));
        }

        if (typeof window.jsQR === 'undefined' && !('BarcodeDetector' in window)) {
            const libNotice = document.getElementById('qrLibNotice');
            if (libNotice) libNotice.style.display = 'block';
        }

        const urlParams = new URLSearchParams(window.location.search);
        const initialMode = '${paramTransferMode != null ? paramTransferMode : param.mode}';
        const initialRecipient = '<c:out value="${paramRecipientAccountNumber != null ? paramRecipientAccountNumber : ''}"/>';
        const initialAmount = '<c:out value="${paramAmount != null ? paramAmount : ''}"/>';

        if (initialMode === 'qr' || urlParams.get('mode') === 'qr') {
            window.switchTransferMode('qr');
            if (initialRecipient) {
                setTimeout(() => handleQrPayload(initialRecipient), 200);
            }
        }

        // -------------------------------------------------------------
        // 2. Manual Mode Live Account Lookup / Validation
        // -------------------------------------------------------------
        const manualRecipientInput = document.getElementById('manualRecipientAccount');
        const manualStatusEl = document.getElementById('manualValidationStatus');
        let lookupTimer = null;

        if (manualRecipientInput && manualStatusEl) {
            manualRecipientInput.addEventListener('input', function() {
                const accNo = this.value.trim();
                clearTimeout(lookupTimer);
                if (!accNo) {
                    manualStatusEl.className = 'account-validation-status';
                    manualStatusEl.innerHTML = '<small style="color: #64748b;">Enter the full recipient FinSight account number.</small>';
                    return;
                }

                manualStatusEl.className = 'account-validation-status status-checking';
                manualStatusEl.innerHTML = '<span>Checking account...</span>';

                lookupTimer = setTimeout(() => {
                    verifyAccount(accNo, function(result) {
                        if (result.valid) {
                            if (result.isOwnAccount) {
                                manualStatusEl.className = 'account-validation-status status-warning';
                                manualStatusEl.innerHTML = '<span>⚠️ Note: This is your own account (' + escapeHtml(result.holderName) + ')</span>';
                            } else {
                                manualStatusEl.className = 'account-validation-status status-valid';
                                manualStatusEl.innerHTML = '<span>✓ Verified Payee: <strong>' + escapeHtml(result.holderName) + '</strong> (' + escapeHtml(result.accountType) + ')</span>';
                            }
                        } else {
                            manualStatusEl.className = 'account-validation-status status-invalid';
                            manualStatusEl.innerHTML = '<span>❌ ' + escapeHtml(result.message || 'Account not found') + '</span>';
                        }
                    });
                }, 400);
            });
        }

        function verifyAccount(accountNumber, callback) {
            fetch(contextPath + '/banking?action=lookupAccount&accountNumber=' + encodeURIComponent(accountNumber))
                .then(res => res.json())
                .then(data => callback(data))
                .catch(err => {
                    console.error('Account lookup error:', err);
                    callback({ valid: false, message: 'Could not verify account' });
                });
        }

        function escapeHtml(text) {
            if (!text) return '';
            return text.replace(/[&<>"']/g, function(m) {
                return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' })[m];
            });
        }

        // -------------------------------------------------------------
        // 3. QR Code Scanner & Methods Setup
        // -------------------------------------------------------------
        const tabCamera = document.getElementById('tabCamera');
        const tabUpload = document.getElementById('tabUpload');
        const tabPaste = document.getElementById('tabPaste');
        const tabDirectory = document.getElementById('tabDirectory');
        const viewCamera = document.getElementById('viewCamera');
        const viewUpload = document.getElementById('viewUpload');
        const viewPaste = document.getElementById('viewPaste');
        const viewDirectory = document.getElementById('viewDirectory');
        const btnProcessPaste = document.getElementById('btnProcessPaste');
        const qrRawTextInput = document.getElementById('qrRawTextInput');

        function switchQrMethod(method) {
            [tabCamera, tabUpload, tabPaste, tabDirectory].forEach(t => t && t.classList.remove('active'));
            [viewCamera, viewUpload, viewPaste, viewDirectory].forEach(v => v && (v.style.display = 'none'));

            if (method === 'camera') {
                if (tabCamera) tabCamera.classList.add('active');
                if (viewCamera) viewCamera.style.display = 'block';
            } else if (method === 'upload') {
                if (tabUpload) tabUpload.classList.add('active');
                if (viewUpload) viewUpload.style.display = 'block';
                if (qrScanner) qrScanner.stopCamera();
            } else if (method === 'paste') {
                if (tabPaste) tabPaste.classList.add('active');
                if (viewPaste) viewPaste.style.display = 'block';
                if (qrScanner) qrScanner.stopCamera();
                if (qrRawTextInput) qrRawTextInput.focus();
            } else if (method === 'directory') {
                if (tabDirectory) tabDirectory.classList.add('active');
                if (viewDirectory) viewDirectory.style.display = 'block';
                if (qrScanner) qrScanner.stopCamera();
            }
        }

        if (tabCamera) tabCamera.addEventListener('click', () => switchQrMethod('camera'));
        if (tabUpload) tabUpload.addEventListener('click', () => switchQrMethod('upload'));
        if (tabPaste) tabPaste.addEventListener('click', () => switchQrMethod('paste'));
        if (tabDirectory) tabDirectory.addEventListener('click', () => switchQrMethod('directory'));

        if (btnProcessPaste && qrRawTextInput) {
            btnProcessPaste.addEventListener('click', () => {
                const val = qrRawTextInput.value.trim();
                if (!val) {
                    alert('Please paste an account number or FinSight QR link.');
                    return;
                }
                handleQrPayload(val);
            });
            qrRawTextInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    btnProcessPaste.click();
                }
            });
        }

        // Initialize QR Scanner Engine
        const qrVideo = document.getElementById('qrVideo');
        const qrOverlay = document.getElementById('qrOverlay');
        const cameraStatus = document.getElementById('cameraStatus');
        const btnToggleCamera = document.getElementById('btnToggleCamera');

        let qrScanner = null;
        try {
            if (qrVideo && typeof FinSightQRScanner !== 'undefined') {
                qrScanner = new FinSightQRScanner({
                    videoElement: qrVideo,
                    overlayElement: qrOverlay,
                    onScanSuccess: handleQrPayload,
                    onScanError: function(msg) {
                        cameraStatus.textContent = msg;
                        btnToggleCamera.textContent = '▶️ Retry Camera';
                    }
                });
                window.qrScannerInstance = qrScanner;

                btnToggleCamera.addEventListener('click', function() {
                    if (qrScanner.isScanning) {
                        qrScanner.stopCamera();
                        cameraStatus.textContent = 'Camera scanner stopped.';
                        btnToggleCamera.textContent = '▶️ Start Camera Scanner';
                    } else {
                        cameraStatus.textContent = 'Starting camera viewfinder...';
                        qrScanner.startCamera().then(() => {
                            cameraStatus.textContent = 'Point camera at any FinSight QR code';
                            btnToggleCamera.textContent = '⏹️ Stop Camera';
                        }).catch(err => {
                            cameraStatus.textContent = 'Camera permission was denied or camera is unavailable. You can upload a QR image or select from Quick Payees.';
                            btnToggleCamera.textContent = '▶️ Retry Camera';
                        });
                    }
                });
            } else if (cameraStatus) {
                cameraStatus.textContent = 'Camera scanning is not supported by your browser. Please upload a QR code image or choose a Quick Payee.';
            }
        } catch (scannerInitErr) {
            console.warn('[FinSightQR] Scanner initialization failed:', scannerInitErr);
            if (cameraStatus) {
                cameraStatus.textContent = 'Camera scanner unavailable. Please upload a QR image or choose from Quick Payees.';
            }
        }

        // Upload QR Image Handling
        const qrDropZone = document.getElementById('qrDropZone');
        const qrFileInput = document.getElementById('qrFileInput');
        const uploadFeedback = document.getElementById('uploadFeedback');

        if (qrDropZone && qrFileInput) {
            qrDropZone.addEventListener('click', () => qrFileInput.click());
            qrDropZone.addEventListener('dragover', (e) => {
                e.preventDefault();
                qrDropZone.classList.add('dragover');
            });
            qrDropZone.addEventListener('dragleave', () => qrDropZone.classList.remove('dragover'));
            qrDropZone.addEventListener('drop', (e) => {
                e.preventDefault();
                qrDropZone.classList.remove('dragover');
                if (e.dataTransfer.files && e.dataTransfer.files[0]) {
                    processImageFile(e.dataTransfer.files[0]);
                }
            });
            qrFileInput.addEventListener('change', function() {
                if (this.files && this.files[0]) {
                    processImageFile(this.files[0]);
                }
            });
        }

        function processImageFile(file) {
            uploadFeedback.style.color = '#2563eb';
            uploadFeedback.textContent = 'Analyzing image for QR code...';
            qrScanner.decodeImageFile(file).then(code => {
                uploadFeedback.textContent = '';
                handleQrPayload(code);
            }).catch(err => {
                uploadFeedback.style.color = '#dc2626';
                uploadFeedback.textContent = err.message || 'Could not detect QR code in this image.';
            });
        }

        // Quick Payee Click Handlers
        document.querySelectorAll('.btn-quick-pay').forEach(btn => {
            btn.addEventListener('click', function() {
                const acc = this.dataset.acc;
                const name = this.dataset.name || '';
                const payload = FinSightQRPayload.buildURI(acc, name, null, 'Quick Pay Transfer');
                handleQrPayload(payload);
            });
        });

        // -------------------------------------------------------------
        // 4. Handle Scanned / Selected QR Payload
        // -------------------------------------------------------------
        const qrScannerStage = document.getElementById('qrScannerStage');
        const qrConfirmationStage = document.getElementById('qrConfirmationStage');
        const scannedHolderName = document.getElementById('scannedHolderName');
        const scannedAccountBadge = document.getElementById('scannedAccountBadge');
        const qrRecipientAccount = document.getElementById('qrRecipientAccount');
        const qrAmount = document.getElementById('qrAmount');
        const qrDescription = document.getElementById('qrDescription');
        const btnRescanQR = document.getElementById('btnRescanQR');

        function handleQrPayload(rawData) {
            const parsed = FinSightQRPayload.parse(rawData);
            if (!parsed || !parsed.accountNumber) {
                alert('Invalid QR code format. Please scan a valid FinSight QR code.');
                return;
            }

            // Verify the account exists
            verifyAccount(parsed.accountNumber, function(result) {
                if (result.valid) {
                    scannedHolderName.textContent = result.holderName || parsed.name || 'Verified Recipient';
                    scannedAccountBadge.textContent = result.accountNumber + ' (' + result.accountType + ')';
                    qrRecipientAccount.value = result.accountNumber;

                    if (parsed.amount && parsed.amount > 0) {
                        qrAmount.value = parsed.amount.toFixed(2);
                    } else if (initialAmount && !qrAmount.value) {
                        qrAmount.value = initialAmount;
                    }

                    if (parsed.description) {
                        qrDescription.value = parsed.description;
                    }

                    // Check source vs recipient account
                    const sourceSelect = document.getElementById('qrSourceAccount');
                    const qrSelfTransferAlert = document.getElementById('qrSelfTransferAlert');
                    const qrSubmitBtn = document.getElementById('qrSubmitBtn');

                    function checkSelfTransfer() {
                        if (!sourceSelect) return;
                        const selectedOpt = sourceSelect.options[sourceSelect.selectedIndex];
                        const sourceAccNo = selectedOpt ? (selectedOpt.getAttribute('data-acc') || selectedOpt.text.split(' ')[0].trim()) : '';
                        if (sourceAccNo && sourceAccNo.toUpperCase() === result.accountNumber.toUpperCase()) {
                            let switched = false;
                            for (let i = 0; i < sourceSelect.options.length; i++) {
                                const opt = sourceSelect.options[i];
                                const optAcc = opt.getAttribute('data-acc') || opt.text.split(' ')[0].trim();
                                if (optAcc.toUpperCase() !== result.accountNumber.toUpperCase()) {
                                    sourceSelect.selectedIndex = i;
                                    switched = true;
                                    break;
                                }
                            }
                            if (!switched) {
                                if (qrSelfTransferAlert) {
                                    qrSelfTransferAlert.innerHTML = '⚠️ <strong>Same Account Warning:</strong> You cannot transfer funds from ' + escapeHtml(sourceAccNo) + ' to itself. Please select or scan another recipient account.';
                                    qrSelfTransferAlert.style.display = 'block';
                                }
                                if (qrSubmitBtn) qrSubmitBtn.disabled = true;
                                return;
                            }
                        }
                        if (qrSelfTransferAlert) qrSelfTransferAlert.style.display = 'none';
                        if (qrSubmitBtn) qrSubmitBtn.disabled = false;
                    }

                    if (sourceSelect) {
                        sourceSelect.removeEventListener('change', checkSelfTransfer);
                        sourceSelect.addEventListener('change', checkSelfTransfer);
                        checkSelfTransfer();
                    }

                    // Transition UI to Confirmation Stage
                    qrScannerStage.style.display = 'none';
                    qrConfirmationStage.style.display = 'block';

                    if (qrAmount.value) {
                        triggerQrBudgetCheck();
                    }

                    // Focus on Amount or PIN
                    setTimeout(() => {
                        if (!qrAmount.value) {
                            qrAmount.focus();
                        } else {
                            const pinField = document.getElementById('qrPin');
                            if (pinField) pinField.focus();
                        }
                    }, 100);
                } else {
                    alert('QR Code Scanned, but account ' + parsed.accountNumber + ' was not found or is inactive.');
                }
            });
        }

        btnRescanQR.addEventListener('click', function() {
            qrConfirmationStage.style.display = 'none';
            qrScannerStage.style.display = 'block';
            qrRecipientAccount.value = '';
            qrAmount.value = '';
            const qrSelfTransferAlert = document.getElementById('qrSelfTransferAlert');
            if (qrSelfTransferAlert) qrSelfTransferAlert.style.display = 'none';
            const qrSubmitBtn = document.getElementById('qrSubmitBtn');
            if (qrSubmitBtn) qrSubmitBtn.disabled = false;
            switchQrMethod('camera');
        });

        // -------------------------------------------------------------
        // 5. "My Account QR Code" Modal Logic (Receive Money)
        // -------------------------------------------------------------
        const openMyQrBtn = document.getElementById('openMyQrBtn');
        const myQrModal = document.getElementById('myQrModal');
        const closeMyQrBtn = document.getElementById('closeMyQrBtn');
        const myQrAccountSelect = document.getElementById('myQrAccountSelect');
        const myQrCustomAmount = document.getElementById('myQrCustomAmount');
        const myQrCodeContainer = document.getElementById('myQrCodeContainer');
        const displayAccountCopy = document.getElementById('displayAccountCopy');
        const copyAccPill = document.getElementById('copyAccPill');
        const btnDownloadQr = document.getElementById('btnDownloadQr');

        let myQrCodeInstance = null;

        function updateMyQrCode() {
            if (!myQrAccountSelect || !myQrCodeContainer) return;
            const selectedOpt = myQrAccountSelect.options[myQrAccountSelect.selectedIndex];
            if (!selectedOpt) return;

            const accNo = selectedOpt.value;
            const holderName = selectedOpt.dataset.name || '';
            const amountVal = myQrCustomAmount ? myQrCustomAmount.value.trim() : '';

            displayAccountCopy.textContent = accNo;

            // Build payload URI
            const uri = FinSightQRPayload.buildURI(accNo, holderName, amountVal ? parseFloat(amountVal) : null, 'Payment to ' + holderName);

            if (!myQrCodeInstance) {
                myQrCodeInstance = new QRCode(myQrCodeContainer, {
                    text: uri,
                    width: 200,
                    height: 200,
                    colorDark: '#0f172a',
                    colorLight: '#ffffff'
                });
            } else {
                myQrCodeInstance.makeCode(uri);
            }
        }

        if (openMyQrBtn && myQrModal) {
            openMyQrBtn.addEventListener('click', () => {
                myQrModal.classList.add('show');
                updateMyQrCode();
            });

            closeMyQrBtn.addEventListener('click', () => {
                myQrModal.classList.remove('show');
            });

            myQrModal.addEventListener('click', (e) => {
                if (e.target === myQrModal) {
                    myQrModal.classList.remove('show');
                }
            });

            if (myQrAccountSelect) {
                myQrAccountSelect.addEventListener('change', updateMyQrCode);
            }
            if (myQrCustomAmount) {
                myQrCustomAmount.addEventListener('input', updateMyQrCode);
            }

            if (copyAccPill) {
                copyAccPill.addEventListener('click', function() {
                    const acc = displayAccountCopy.textContent.trim();
                    navigator.clipboard.writeText(acc).then(() => {
                        const originalHTML = copyAccPill.innerHTML;
                        copyAccPill.innerHTML = '<span style="color: #16a34a; font-weight: 700;">✓ Copied to clipboard!</span>';
                        setTimeout(() => copyAccPill.innerHTML = originalHTML, 1800);
                    });
                });
            }

            if (btnDownloadQr) {
                btnDownloadQr.addEventListener('click', function() {
                    const canvas = myQrCodeContainer.querySelector('canvas');
                    if (canvas) {
                        const link = document.createElement('a');
                        link.download = 'FinSight-QR-' + displayAccountCopy.textContent.trim() + '.png';
                        link.href = canvas.toDataURL('image/png');
                        link.click();
                    }
                });
            }
        }

        // -------------------------------------------------------------
        // 6. Real-Time Budget Limit Check & Warning Confirmation
        // -------------------------------------------------------------
        const manualCategory = document.getElementById('manualCategory');
        const manualAmount = document.getElementById('manualAmount');
        const manualBudgetWarning = document.getElementById('manualBudgetWarning');
        const manualOverrideBudget = document.getElementById('manualOverrideBudget');
        const manualTransferForm = document.getElementById('manualTransferForm');

        const qrCategory = document.getElementById('qrCategory');
        const qrAmount = document.getElementById('qrAmount');
        const qrBudgetWarning = document.getElementById('qrBudgetWarning');
        const qrOverrideBudget = document.getElementById('qrOverrideBudget');
        const qrTransferForm = document.getElementById('qrTransferForm');

        const budgetConfirmModal = document.getElementById('budgetConfirmModal');
        const budgetModalMessage = document.getElementById('budgetModalMessage');
        const btnCancelBudgetPay = document.getElementById('btnCancelBudgetPay');
        const btnConfirmBudgetPay = document.getElementById('btnConfirmBudgetPay');

        let pendingSubmitForm = null;
        let manualBudgetState = { exceeds: false, message: '' };
        let qrBudgetState = { exceeds: false, message: '' };

        function checkBudget(category, amount, warningEl, stateObj) {
            if (!category || !amount || parseFloat(amount) <= 0) {
                warningEl.style.display = 'none';
                stateObj.exceeds = false;
                stateObj.message = '';
                return;
            }

            fetch(contextPath + '/banking?action=checkBudget&category=' + encodeURIComponent(category) + '&amount=' + encodeURIComponent(amount))
                .then(res => res.json())
                .then(data => {
                    if (data.hasBudget) {
                        stateObj.exceeds = data.exceeds;
                        stateObj.message = data.message || '';
                        if (data.exceeds) {
                            warningEl.className = 'budget-limit-warning exceeded-alert';
                            warningEl.innerHTML = '<span class="budget-warning-icon">⚠️</span><span><strong>Budget Limit Warning:</strong> Monthly limit for ' + escapeHtml(data.category) + ' is ₹' + Number(data.limit).toFixed(2) + '. Current spent: ₹' + Number(data.spent).toFixed(2) + ', Remaining: ₹' + Number(data.remaining).toFixed(2) + '. This payment of ₹' + Number(amount).toFixed(2) + ' crosses your budget by ₹' + Number(data.excessAmount).toFixed(2) + '.</span>';
                            warningEl.style.display = 'flex';
                        } else {
                            warningEl.className = 'budget-limit-warning';
                            warningEl.innerHTML = '<span class="budget-warning-icon">ℹ️</span><span><strong>Budget Status:</strong> ' + escapeHtml(data.category) + ' limit is ₹' + Number(data.limit).toFixed(2) + '. Remaining after this transfer: ₹' + (Number(data.remaining) - Number(amount)).toFixed(2) + '.</span>';
                            warningEl.style.display = 'flex';
                        }
                    } else {
                        warningEl.style.display = 'none';
                        stateObj.exceeds = false;
                        stateObj.message = '';
                    }
                })
                .catch(err => {
                    console.error('Budget check error:', err);
                });
        }

        let manualBudgetTimer = null;
        function triggerManualBudgetCheck() {
            clearTimeout(manualBudgetTimer);
            manualBudgetTimer = setTimeout(() => {
                checkBudget(manualCategory.value, manualAmount.value, manualBudgetWarning, manualBudgetState);
            }, 300);
        }

        if (manualCategory && manualAmount) {
            manualCategory.addEventListener('change', function() {
                if (manualDescription) {
                    manualDescription.value = 'Transfer for ' + this.value;
                }
                triggerManualBudgetCheck();
            });
            manualAmount.addEventListener('input', triggerManualBudgetCheck);
            if (manualAmount.value) triggerManualBudgetCheck();
        }

        let qrBudgetTimer = null;
        function triggerQrBudgetCheck() {
            clearTimeout(qrBudgetTimer);
            qrBudgetTimer = setTimeout(() => {
                checkBudget(qrCategory.value, qrAmount.value, qrBudgetWarning, qrBudgetState);
            }, 300);
        }

        if (qrCategory && qrAmount) {
            qrCategory.addEventListener('change', function() {
                if (qrDescription) {
                    qrDescription.value = 'Instant QR Transfer for ' + this.value;
                }
                triggerQrBudgetCheck();
            });
            qrAmount.addEventListener('input', triggerQrBudgetCheck);
            if (qrAmount.value) triggerQrBudgetCheck();
        }

        function interceptSubmit(e, form, overrideInput, stateObj, categoryEl) {
            if (overrideInput && overrideInput.value === 'true') {
                return;
            }
            if (stateObj.exceeds) {
                e.preventDefault();
                pendingSubmitForm = form;
                budgetModalMessage.textContent = 'You are crossing your ' + categoryEl.value + ' budget amount limit! Do you still want to continue paying?';
                budgetConfirmModal.style.display = 'flex';
            }
        }

        if (manualTransferForm) {
            manualTransferForm.addEventListener('submit', function(e) {
                interceptSubmit(e, manualTransferForm, manualOverrideBudget, manualBudgetState, manualCategory);
            });
        }

        if (qrTransferForm) {
            qrTransferForm.addEventListener('submit', function(e) {
                interceptSubmit(e, qrTransferForm, qrOverrideBudget, qrBudgetState, qrCategory);
            });
        }

        if (btnCancelBudgetPay) {
            btnCancelBudgetPay.addEventListener('click', function() {
                budgetConfirmModal.style.display = 'none';
                pendingSubmitForm = null;
            });
        }

        if (btnConfirmBudgetPay) {
            btnConfirmBudgetPay.addEventListener('click', function() {
                budgetConfirmModal.style.display = 'none';
                if (pendingSubmitForm) {
                    const overrideInput = pendingSubmitForm.querySelector('input[name="overrideBudget"]');
                    if (overrideInput) overrideInput.value = 'true';
                    pendingSubmitForm.submit();
                }
            });
        }
    });
    </script>
</body>
</html>
