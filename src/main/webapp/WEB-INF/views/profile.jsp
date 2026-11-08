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
<body>
    <nav class="navbar">
        <a href="${pageContext.request.contextPath}/dashboard" class="brand">💰 FinSight</a>
        <div class="nav-links">
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
            <a href="${pageContext.request.contextPath}/profile" style="color: var(--primary); font-weight: 700;">Profile</a>
            <a href="${pageContext.request.contextPath}/logout" class="btn-logout">Logout</a>
        </div>
    </nav>

    <div class="container" style="max-width: 900px; margin-top: 30px;">
        <div class="page-header">
            <h1 class="page-title">👤 Profile & Security Settings</h1>
            <p class="page-subtitle">Manage your personal details, credentials, and transaction PIN</p>
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

        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
            <!-- Personal Details -->
            <div class="card" style="padding: 25px;">
                <h3 style="font-size: 1.15rem; margin-bottom: 15px;">Personal Details</h3>
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="updateProfile">

                    <div class="form-group" style="margin-bottom: 15px;">
                        <label>Full Name</label>
                        <input type="text" name="name" class="form-control" value="<c:out value="${user.name}"/>" required>
                    </div>

                    <div class="form-group" style="margin-bottom: 15px;">
                        <label>Email Address</label>
                        <input type="email" class="form-control" value="<c:out value="${user.email}"/>" disabled style="background: #f1f5f9;">
                    </div>

                    <div class="form-group" style="margin-bottom: 15px;">
                        <label>Phone Number</label>
                        <input type="tel" name="phone" class="form-control" value="<c:out value="${user.phone}"/>" placeholder="+91 9876543210">
                    </div>

                    <div class="form-group" style="margin-bottom: 20px;">
                        <label>Member Since</label>
                        <input type="text" class="form-control" value="<c:out value="${user.createdAt != null ? user.createdAt : 'Active'}"/>" disabled style="background: #f1f5f9;">
                    </div>

                    <button type="submit" class="btn btn-primary" style="width: 100%;">Save Profile</button>
                </form>
            </div>

            <!-- Password Change -->
            <div class="card" style="padding: 25px;">
                <h3 style="font-size: 1.15rem; margin-bottom: 15px;">Change Password</h3>
                <form action="${pageContext.request.contextPath}/profile" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <input type="hidden" name="action" value="updatePassword">

                    <div class="form-group" style="margin-bottom: 15px;">
                        <label>Current Password</label>
                        <input type="password" name="oldPassword" class="form-control" required>
                    </div>

                    <div class="form-group" style="margin-bottom: 15px;">
                        <label>New Password (Min 6 chars)</label>
                        <input type="password" name="newPassword" class="form-control" required minlength="6">
                    </div>

                    <div class="form-group" style="margin-bottom: 20px;">
                        <label>Confirm New Password</label>
                        <input type="password" name="confirmPassword" class="form-control" required minlength="6">
                    </div>

                    <button type="submit" class="btn btn-secondary" style="width: 100%;">Update Password</button>
                </form>
            </div>
        </div>

        <!-- Transaction PIN Management -->
        <div class="card" style="padding: 25px; margin-top: 25px;">
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px;">
                <div>
                    <h3 style="font-size: 1.15rem; margin: 0;">🔑 Transaction PIN Security</h3>
                    <p style="color: #64748b; font-size: 0.9rem; margin-top: 4px;">
                        Your 4–6 digit numeric PIN authorizes withdrawals and transfers. It is never stored in plain text.
                    </p>
                </div>
                <div>
                    <c:choose>
                        <c:when test="${user.hasTransactionPin()}">
                            <span class="badge" style="background: #dcfce7; color: #166534; padding: 6px 12px; border-radius: 20px; font-weight: 600;">● PIN Active</span>
                        </c:when>
                        <c:otherwise>
                            <span class="badge" style="background: #fee2e2; color: #991b1b; padding: 6px 12px; border-radius: 20px; font-weight: 600;">● PIN Not Configured</span>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>

            <c:choose>
                <c:when test="${!user.hasTransactionPin()}">
                    <form action="${pageContext.request.contextPath}/profile" method="post" style="max-width: 450px;">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="action" value="setupPin">

                        <div class="form-group" style="margin-bottom: 15px;">
                            <label>Create Transaction PIN (4–6 digits)</label>
                            <input type="password" name="pin" class="form-control" required pattern="\d{4,6}" placeholder="Enter 4-6 digits">
                        </div>

                        <div class="form-group" style="margin-bottom: 20px;">
                            <label>Confirm Transaction PIN</label>
                            <input type="password" name="confirmPin" class="form-control" required pattern="\d{4,6}" placeholder="Confirm digits">
                        </div>

                        <button type="submit" class="btn btn-primary">Set Transaction PIN</button>
                    </form>
                </c:when>
                <c:otherwise>
                    <form action="${pageContext.request.contextPath}/profile" method="post" style="max-width: 450px;">
                        <input type="hidden" name="csrfToken" value="${csrfToken}">
                        <input type="hidden" name="action" value="changePin">

                        <div class="form-group" style="margin-bottom: 15px;">
                            <label>Current Transaction PIN</label>
                            <input type="password" name="currentPin" class="form-control" required pattern="\d{4,6}">
                        </div>

                        <div class="form-group" style="margin-bottom: 15px;">
                            <label>New Transaction PIN (4–6 digits)</label>
                            <input type="password" name="newPin" class="form-control" required pattern="\d{4,6}">
                        </div>

                        <div class="form-group" style="margin-bottom: 20px;">
                            <label>Confirm New Transaction PIN</label>
                            <input type="password" name="confirmNewPin" class="form-control" required pattern="\d{4,6}">
                        </div>

                        <button type="submit" class="btn btn-secondary">Change Transaction PIN</button>
                    </form>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
</body>
</html>

