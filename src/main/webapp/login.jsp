<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Expense Tracker | Login</title>

<style>

*{
    margin:0;
    padding:0;
    box-sizing:border-box;
    font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;
}

body{
    height:100vh;
    display:flex;
    justify-content:center;
    align-items:center;
    background:linear-gradient(135deg,#d8ebff,#c4e0ff);
}

/* Logo */

.header{
    position:absolute;
    top:25px;
    left:50px;
    display:flex;
    align-items:center;
    gap:12px;
}

.header .icon{
    font-size:40px;
}

.header .title{
    font-size:34px;
    font-weight:700;
    color:#1e293b;
}

/* Main Container */

.container{
    width:820px;
    height:430px;
    display:flex;
    background:#fff;
    border-radius:18px;
    overflow:hidden;
    box-shadow:0 15px 40px rgba(0,0,0,0.15);
}

/* Left */

.left{
    width:42%;
    display:flex;
    justify-content:center;
    align-items:center;
    background:#fff;
}

.login-box{
    width:280px;
}

.login-box h2{
    text-align:center;
    margin-bottom:28px;
    color:#222;
    font-size:34px;
}

/* Inputs */

.input-box{
    margin-bottom:18px;
}

.input-box input{
    width:100%;
    padding:13px 15px;
    border:1px solid #d0d0d0;
    border-radius:8px;
    font-size:15px;
    transition:.3s;
    outline:none;
}

.input-box input:focus{
    border-color:#0d6efd;
    box-shadow:0 0 5px rgba(13,110,253,.3);
}

/* Button */

button{
    width:100%;
    padding:13px;
    border:none;
    border-radius:8px;
    background:#0d6efd;
    color:#fff;
    font-size:16px;
    cursor:pointer;
    transition:.3s;
}

button:hover{
    background:#0b5ed7;
}

/* Register */

.register-link{
    text-align:center;
    margin-top:20px;
    font-size:15px;
}

.register-link a{
    color:#0d6efd;
    font-weight:600;
    text-decoration:none;
}

.register-link a:hover{
    text-decoration:underline;
}

/* Right */

.right{
    width:58%;
    background:#eef6ff;
    display:flex;
    justify-content:center;
    align-items:center;
    text-align:center;
    padding:40px;
}

.content h1{
    color:#1f2937;
    font-size:42px;
    margin-bottom:25px;
}

.content p{
    color:#555;
    font-size:18px;
    line-height:32px;
}

.content span{
    color:#0d6efd;
    font-weight:bold;
}

</style>

</head>

<body>

<!-- Logo -->

<div class="header">
    <span class="icon">💰</span>
    <span class="title">Expense Tracker</span>
</div>

<!-- Main Container -->

<div class="container">

    <!-- Left Side -->

    <div class="left">

        <div class="login-box">

            <h2>Login</h2>

            <form action="${pageContext.request.contextPath}/login" method="post">

                <div class="input-box">
                    <input
                        type="email"
                        name="email"
                        placeholder="Email Address"
                        required>
                </div>

                <div class="input-box">
                    <input
                        type="password"
                        name="password"
                        placeholder="Password"
                        required>
                </div>

                <button type="submit">Login</button>

            </form>

            <div class="register-link">
                Don't have an account?
                <a href="register.jsp">Create Account</a>
            </div>

        </div>

    </div>

    <!-- Right Side -->

    <div class="right">

        <div class="content">

            <h1>Welcome Back!</h1>

            <p>
                Login to your
                <span>Expense Management System</span>
                and easily manage your income, expenses,
                transactions, and savings.
            </p>

            <br>

            <p>
                Keep track of your finances, monitor your balance,
                and stay organized—all in one secure place.
            </p>

        </div>

    </div>

</div>

</body>
</html>