<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Profile & Security</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body class="profile-page">
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <button type="button" class="profile-mobile-toggle" aria-controls="profile-nav-links" aria-expanded="false">
            <span class="sr-only">Toggle navigation</span>
            <span aria-hidden="true">☰</span>
        </button>
        <div class="nav-links" id="profile-nav-links">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/accounts">My Accounts</a>
            <a href="${pageContext.request.contextPath}/banking?action=transfer">Transfer</a>
            <a href="${pageContext.request.contextPath}/banking?action=deposit">Deposit</a>
            <a href="${pageContext.request.contextPath}/banking?action=withdraw">Withdraw</a>
            <a href="${pageContext.request.contextPath}/viewTransaction">Transactions</a>
            <a href="${pageContext.request.contextPath}/budgets">Budgets</a>
            <a href="${pageContext.request.contextPath}/alerts">Alerts</a>
            <a href="${pageContext.request.contextPath}/health">Financial Health</a>
            <a href="${pageContext.request.contextPath}/statement-analysis">Statements</a>
            <a href="${pageContext.request.contextPath}/profile" class="active" aria-current="page">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container profile-container">
        <div class="page-header">
            <h1 class="page-title">👤 Profile & Security Settings</h1>
            <p class="page-subtitle">Manage your personal details, credentials, and transaction PIN</p>
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

        <div class="profile-grid">
            <!-- Personal Details -->
            <div class="card profile-panel">
                <h2 class="profile-panel-title">Personal Details</h2>
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="updateProfile">

                    <div class="form-group">
                        <label for="profileName">Full Name</label>
                        <input type="text" id="profileName" name="name" class="form-control" value="<c:out value="${user.name}"/>" required autocomplete="name">
                    </div>

                    <div class="form-group">
                        <label for="profileEmail">Email Address</label>
                        <input type="email" id="profileEmail" class="form-control profile-readonly-field" value="<c:out value="${user.email}"/>" disabled>
                    </div>

                    <div class="form-group">
                        <label for="profilePhone">Phone Number</label>
                        <input type="tel" id="profilePhone" name="phone" class="form-control" value="<c:out value="${user.phone}"/>" placeholder="+91 9876543210" autocomplete="tel">
                    </div>

                    <div class="form-group">
                        <label for="memberSince">Member Since</label>
                        <input type="text" id="memberSince" class="form-control profile-readonly-field" value="<c:out value="${user.createdAt != null ? user.createdAt : 'Active'}"/>" disabled>
                    </div>

                    <button type="submit" class="btn btn-primary profile-submit-button">Save Profile</button>
                </form>
            </div>

            <!-- Password Change -->
            <div class="card profile-panel">
                <h2 class="profile-panel-title">Change Password</h2>
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="updatePassword">

                    <div class="form-group">
                        <label for="oldPassword">Current Password</label>
                        <input type="password" id="oldPassword" name="oldPassword" class="form-control" required autocomplete="current-password">
                    </div>

                    <div class="form-group">
                        <label for="newPassword">New Password (Min 6 chars)</label>
                        <input type="password" id="newPassword" name="newPassword" class="form-control" required minlength="6" autocomplete="new-password">
                    </div>

                    <div class="form-group">
                        <label for="confirmPassword">Confirm New Password</label>
                        <input type="password" id="confirmPassword" name="confirmPassword" class="form-control" required minlength="6" autocomplete="new-password">
                    </div>

                    <button type="submit" class="btn btn-secondary profile-submit-button">Update Password</button>
                </form>
            </div>
        </div>

        <!-- Transaction PIN Management -->
        <div class="card profile-pin-card">
            <div class="profile-pin-header">
                <div>
                    <h2 class="profile-panel-title">🔑 Transaction PIN Security</h2>
                    <p class="profile-pin-description">
                        Your 4–6 digit numeric PIN authorizes withdrawals and transfers. It is never stored in plain text.
                    </p>
                </div>
                <div>
                    <c:choose>
                        <c:when test="${user.hasTransactionPin()}">
                            <span class="badge profile-pin-active">● PIN Active</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge profile-pin-inactive">● PIN Not Configured</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <c:choose>
                <c:when test="${!user.hasTransactionPin()}">
                    <form action="${pageContext.request.contextPath}/profile" method="post" class="profile-pin-form">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="action" value="setupPin">

                        <div class="form-group">
                            <label for="pin">Create Transaction PIN (4–6 digits)</label>
                            <input type="password" id="pin" name="pin" class="form-control" required pattern="\d{4,6}" placeholder="Enter 4-6 digits" inputmode="numeric" autocomplete="new-password">
                        </div>

                        <div class="form-group">
                            <label for="confirmPin">Confirm Transaction PIN</label>
                            <input type="password" id="confirmPin" name="confirmPin" class="form-control" required pattern="\d{4,6}" placeholder="Confirm digits" inputmode="numeric" autocomplete="new-password">
                        </div>

                        <button type="submit" class="btn btn-primary">Set Transaction PIN</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <form action="${pageContext.request.contextPath}/profile" method="post" class="profile-pin-form">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="action" value="changePin">

                        <div class="form-group">
                            <label for="currentPin">Current Transaction PIN</label>
                            <input type="password" id="currentPin" name="currentPin" class="form-control" required pattern="\d{4,6}" inputmode="numeric" autocomplete="current-password">
                        </div>

                        <div class="form-group">
                            <label for="newPin">New Transaction PIN (4–6 digits)</label>
                            <input type="password" id="newPin" name="newPin" class="form-control" required pattern="\d{4,6}" inputmode="numeric" autocomplete="new-password">
                        </div>

                        <div class="form-group">
                            <label for="confirmNewPin">Confirm New Transaction PIN</label>
                            <input type="password" id="confirmNewPin" name="confirmNewPin" class="form-control" required pattern="\d{4,6}" inputmode="numeric" autocomplete="new-password">
                        </div>

                        <button type="submit" class="btn btn-secondary">Change Transaction PIN</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <script>
        (function() {
            var navToggle = document.querySelector('.profile-mobile-toggle');
            var navLinks = document.getElementById('profile-nav-links');
            if (navToggle && navLinks) {
                navToggle.addEventListener('click', function() {
                    var isOpen = navLinks.classList.toggle('profile-nav-open');
                    navToggle.setAttribute('aria-expanded', String(isOpen));
                });
            }
        })();
    </script>
</body>
</html>

