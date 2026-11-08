<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Statement Analysis</title>
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
            <a href="${pageContext.request.contextPath}/statement-analysis" style="color: var(--primary); font-weight: 700;">📄 Statement Analysis</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container" style="max-width: 800px; margin-top: 30px;">
        <div class="page-header text-center">
            <h1 class="page-title">📄 Bank Statement Analysis</h1>
            <p class="page-subtitle">Deterministic, bank-grade e-statement analysis with zero LLM hallucinations</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger" style="margin-bottom: 20px;">
                <span><c:out value="${errorMessage}"/></span>
            </div>
        </c:if>

        <div class="card" style="padding: 30px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);">
            <form action="${pageContext.request.contextPath}/statement-analysis?action=upload" method="post" enctype="multipart/form-data">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <div class="form-group" style="margin-bottom: 25px;">
                    <label style="font-weight: 600; display: block; margin-bottom: 8px;">Select Digital Bank Statement PDF (Max 10 MB)</label>
                    <input type="file" name="statementPdf" accept="application/pdf" required class="form-control" style="padding: 10px;">
                    <small style="color: #64748b; display: block; margin-top: 5px;">Must be a digitally generated PDF e-statement with text content. Scanned images are rejected.</small>
                </div>

                <button type="submit" class="btn btn-primary btn-block" style="padding: 12px; font-size: 1rem; width: 100%;">
                    🔍 Securely Analyze Statement
                </button>
            </form>
        </div>

        <div class="card" style="margin-top: 25px; padding: 20px; background: #f8fafc; border: 1px dashed #cbd5e1;">
            <h3 style="font-size: 1.1rem; margin-bottom: 10px;">🛡️ FinSight Privacy & Security Guarantee</h3>
            <ul style="color: #475569; font-size: 0.9rem; margin-left: 20px; line-height: 1.6;">
                <li>100% deterministic local Java execution — zero external AI or cloud API leakage.</li>
                <li>Your statement is never uploaded to third parties or persisted without explicit approval.</li>
                <li>You review and approve every extracted transaction before it enters your ledger.</li>
            </ul>
        </div>
    </div>
</body>
</html>

