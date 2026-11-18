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
<body class="statement-upload-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">
            💰 FinSight
        </a>
        <button type="button" class="statement-mobile-toggle" aria-controls="statement-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="statement-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/statement-analysis" class="active" aria-current="page">📄 Statement Analysis</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container statement-upload-container">
        <div class="page-header text-center">
            <h1 class="page-title">📄 Bank Statement Analysis</h1>
            <p class="page-subtitle">Deterministic, bank-grade e-statement analysis with zero LLM hallucinations</p>
        </div>

        <c:if test="${not empty errorMessage}">
            <div class="alert alert-danger" role="alert">
                <span><c:out value="${errorMessage}"/></span>
            </div>
        </c:if>

        <div class="card statement-upload-card">
            <form action="${pageContext.request.contextPath}/statement-analysis?action=upload" method="post" enctype="multipart/form-data">
                <input type="hidden" name="csrfToken" value="${csrfToken}">
                <div class="form-group statement-file-group">
                    <label for="statementPdf">Select Digital Bank Statement PDF (Max 10 MB)</label>
                    <input type="file" id="statementPdf" name="statementPdf" accept="application/pdf" required class="form-control statement-file-input" aria-describedby="statementFileHelp">
                    <small id="statementFileHelp">Must be a digitally generated PDF e-statement with text content. Scanned images are rejected.</small>
                </div>

                <button type="submit" class="btn btn-primary statement-submit-button">
                    🔍 Securely Analyze Statement
                </button>
            </form>
        </div>

        <div class="card statement-privacy-card">
            <h2>🛡️ FinSight Privacy & Security Guarantee</h2>
            <ul>
                <li>100% deterministic local Java execution — zero external AI or cloud API leakage.</li>
                <li>Your statement is never uploaded to third parties or persisted without explicit approval.</li>
                <li>You review and approve every extracted transaction before it enters your ledger.</li>
            </ul>
        </div>
    </div>
    <script>
        (function() {
            var navToggle = document.querySelector('.statement-mobile-toggle');
            var navLinks = document.getElementById('statement-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('statement-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }
        })();
    </script>
</body>
</html>

