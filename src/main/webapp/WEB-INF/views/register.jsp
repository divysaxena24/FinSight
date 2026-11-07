<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Register</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="auth-wrapper">
        <div class="auth-sidebar">
            <h1>FinSight</h1>
            <p>Smart Personal Expense Analytics Platform. Create your free account to track expenses, manage budgets, and gain clarity over your personal finances.</p>
        </div>
        <div class="auth-content">
            <div class="auth-box">
                <h2>Create Account</h2>
                <p class="subtitle">Join FinSight and start organizing your finances</p>

                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger">
                        <span><c:out value="${errorMessage}"/></span>
                    </div>
                </c:if>

                <form action="${pageContext.request.contextPath}/register" method="post">
                    <div class="form-group">
                        <label for="name">Full Name</label>
                        <input type="text" id="name" name="name" class="form-control" 
                               value="<c:out value="${enteredName}"/>" required placeholder="John Doe" autofocus>
                    </div>

                    <div class="form-group">
                        <label for="email">Email Address</label>
                        <input type="email" id="email" name="email" class="form-control" 
                               value="<c:out value="${enteredEmail}"/>" required placeholder="name@example.com">
                    </div>

                    <div class="form-group">
                        <label for="password">Password</label>
                        <input type="password" id="password" name="password" class="form-control" 
                               required minlength="6" placeholder="At least 6 characters">
                    </div>

                    <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 1rem;">
                        Create My Account
                    </button>
                </form>

                <p style="margin-top: 1.75rem; text-align: center; color: var(--text-muted); font-size: 0.95rem;">
                    Already registered? 
                    <a href="${pageContext.request.contextPath}/login" style="color: var(--primary); font-weight: 600; text-decoration: none;">Sign in here</a>
                </p>
            </div>
        </div>
    </div>
</body>
</html>

