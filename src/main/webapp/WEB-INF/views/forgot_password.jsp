<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Forgot Password</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="auth-wrapper">
        <div class="auth-sidebar">
            <h1>FinSight</h1>
            <p>Reset access to your simulated banking and personal finance account securely.</p>
        </div>
        <div class="auth-content">
            <div class="auth-box">
                <h2>Forgot Password</h2>
                <p class="subtitle">Enter your registered email address</p>

                <form action="${pageContext.request.contextPath}/forgot-password" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <div class="form-group">
                        <label for="email">Email Address</label>
                        <input type="email" id="email" name="email" class="form-control"
                               required autocomplete="email" autofocus placeholder="name@example.com">
                    </div>
                    <button type="submit" class="btn btn-primary auth-submit">Send Reset Link</button>
                </form>

                <p class="auth-register">
                    <a href="${pageContext.request.contextPath}/login" class="auth-register-link">Back to Sign In</a>
                </p>
            </div>
        </div>
    </div>
</body>
</html>
