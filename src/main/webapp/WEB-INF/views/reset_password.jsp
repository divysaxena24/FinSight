<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Reset Password</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="auth-wrapper">
        <div class="auth-sidebar">
            <h1>FinSight</h1>
            <p>Create a strong new password for your FinSight account.</p>
        </div>
        <div class="auth-content">
            <div class="auth-box">
                <h2>Reset Password</h2>

                <c:choose>
                    <c:when test="${invalidToken}">
                        <div class="alert alert-danger" role="alert">
                            This reset link is invalid or has expired. Please request a new link.
                        </div>
                        <p class="auth-register">
                            <a href="${pageContext.request.contextPath}/forgot-password" class="auth-register-link">Request another link</a>
                        </p>
                    </c:when>
                    <c:otherwise>
                        <c:if test="${not empty errorMessage}">
                            <div class="alert alert-danger" role="alert">
                                <c:out value="${errorMessage}"/>
                            </div>
                        </c:if>
                        <form action="${pageContext.request.contextPath}/reset-password" method="post">
                            <input type="hidden" name="csrfToken" value="${csrfToken}">
                            <input type="hidden" name="token" value="${token}">
                            <div class="form-group">
                                <label for="password">New Password</label>
                                <input type="password" id="password" name="password" class="form-control"
                                       required minlength="6" autocomplete="new-password" autofocus>
                            </div>
                            <div class="form-group">
                                <label for="confirmPassword">Confirm New Password</label>
                                <input type="password" id="confirmPassword" name="confirmPassword"
                                       class="form-control" required minlength="6" autocomplete="new-password">
                            </div>
                            <button type="submit" class="btn btn-primary auth-submit">Update Password</button>
                        </form>
                    </c:otherwise>
                </c:choose>
            </div>
        </div>
    </div>
</body>
</html>
