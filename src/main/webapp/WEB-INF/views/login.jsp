<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>FinSight | Login</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
    <div class="auth-wrapper">
        <div class="auth-sidebar">
            <h1>FinSight</h1>
            <p>Smart Personal Expense Analytics Platform. Take control of your financial health with intuitive tracking, automated insights, and clear reporting.</p>
        </div>
        <div class="auth-content">
            <div class="auth-box">
                <h2>Sign In</h2>
                <p class="subtitle">Access your financial dashboard</p>

                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger">
                        <span><c:out value="${errorMessage}"/></span>
                    </div>
                </c:if>

                <c:if test="${not empty sessionScope.errorMessage}">
                    <div class="alert alert-danger">
                        <span><c:out value="${sessionScope.errorMessage}"/></span>
                    </div>
                    <c:remove var="errorMessage" scope="session"/>
                </c:if>

                <c:if test="${not empty sessionScope.successMessage}">
                    <div class="alert alert-success">
                        <span><c:out value="${sessionScope.successMessage}"/></span>
                    </div>
                    <c:remove var="successMessage" scope="session"/>
                </c:if>

                <form action="${pageContext.request.contextPath}/login" method="post">
                    <div class="form-group">
                        <label for="email">Email Address</label>
                        <input type="email" id="email" name="email" class="form-control" 
                               value="<c:out value="${enteredEmail}"/>" required placeholder="name@example.com" autofocus>
                    </div>

                    <div class="form-group">
                        <label for="password">Password</label>
                        <input type="password" id="password" name="password" class="form-control" 
                               required placeholder="••••••••">
                    </div>

                    <button type="submit" class="btn btn-primary" style="width: 100%; margin-top: 1rem;">
                        Sign In to FinSight
                    </button>
                </form>

                <p style="margin-top: 1.75rem; text-align: center; color: var(--text-muted); font-size: 0.95rem;">
                    Don't have an account? 
                    <a href="${pageContext.request.contextPath}/register" style="color: var(--primary); font-weight: 600; text-decoration: none;">Create one here</a>
                </p>
            </div>
        </div>
    </div>
</body>
</html>

