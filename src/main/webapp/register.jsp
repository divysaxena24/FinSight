<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Expense Tracker | Register</title>

<style>

*{
    margin:0;
    padding:0;
    box-sizing:border-box;
    font-family:'Segoe UI',sans-serif;
}

body{
    background:linear-gradient(135deg,#dfefff,#c7e3ff);
    min-height:100vh;
}

/* Header */

.header{
    width:100%;
    height:80px;
    display:flex;
    align-items:center;
    padding:0 60px;
}

.logo{
    font-size:32px;
    font-weight:700;
    color:#0d6efd;
}

/* Main */

.main{
    display:flex;
    justify-content:center;
    align-items:center;
    margin-top:20px;
}

/* Card */

.container{
    width:900px;
    height:470px;
    background:#fff;
    border-radius:18px;
    overflow:hidden;
    display:flex;
    box-shadow:0 15px 35px rgba(0,0,0,.15);
}

/* Left */

.left{
    width:42%;
    display:flex;
    justify-content:center;
    align-items:center;
    background:#fff;
}

.register-box{
    width:280px;
}

.register-box h2{
    text-align:center;
    color:#222;
    margin-bottom:8px;
    font-size:32px;
}

.subtitle{
    text-align:center;
    color:#777;
    font-size:14px;
    margin-bottom:25px;
}

.input-box{
    margin-bottom:16px;
}

.input-box input{
    width:100%;
    padding:12px 15px;
    border:1px solid #d0d0d0;
    border-radius:8px;
    font-size:15px;
    outline:none;
    transition:.3s;
}

.input-box input:focus{
    border-color:#0d6efd;
    box-shadow:0 0 5px rgba(13,110,253,.25);
}

button{
    width:100%;
    padding:12px;
    border:none;
    border-radius:8px;
    background:#0d6efd;
    color:white;
    font-size:16px;
    cursor:pointer;
    transition:.3s;
}

button:hover{
    background:#0b5ed7;
}

.login-link{
    margin-top:18px;
    text-align:center;
    font-size:14px;
}

.login-link a{
    color:#0d6efd;
    text-decoration:none;
    font-weight:600;
}

.login-link a:hover{
    text-decoration:underline;
}

/* Right */

.right{
    width:58%;
    background:#eef6ff;
    display:flex;
    justify-content:center;
    align-items:center;
    padding:45px;
}

.content{
    text-align:center;
}

.content h1{
    font-size:34px;
    color:#222;
    margin-bottom:20px;
}

.content p{
    font-size:16px;
    color:#555;
    line-height:28px;
}

.features{
    margin-top:30px;
}

.feature{
    background:#fff;
    margin-bottom:12px;
    padding:12px 18px;
    border-radius:8px;
    text-align:left;
    box-shadow:0 2px 8px rgba(0,0,0,.05);
    color:#444;
    font-size:15px;
}

</style>

</head>

<body>

<!-- Header -->

<div class="header">
    <div class="logo">💰 Expense Tracker</div>
</div>

<!-- Main -->

<div class="main">

<div class="container">

    <!-- Left -->

    <div class="left">

        <div class="register-box">

            <h2>Create Account</h2>

            <p class="subtitle">
                Register to manage your expenses easily.
            </p>

            <form action="register" method="post">

                <div class="input-box">
                    <input type="text"
                           name="name"
                           placeholder="Full Name"
                           required>
                </div>

                <div class="input-box">
                    <input type="email"
                           name="email"
                           placeholder="Email Address"
                           required>
                </div>

                <div class="input-box">
                    <input type="password"
                           name="password"
                           placeholder="Password"
                           required>
                </div>

                <button type="submit">Create Account</button>

            </form>

            <div class="login-link">
                Already have an account?
                <a href="login.jsp">Login</a>
            </div>

        </div>

    </div>

    <!-- Right -->

    <div class="right">

        <div class="content">

            <h1>Welcome to Expense Tracker</h1>

            <p>
                Create your account and securely manage your income,
                expenses, and financial records from one place.
            </p>

            <div class="features">

                <div class="feature">
                    📈 Track daily income and expenses
                </div>

                <div class="feature">
                    💰 Monitor your account balance instantly
                </div>

                <div class="feature">
                    📋 View and manage all transactions
                </div>

                <div class="feature">
                    🔒 Secure and easy-to-use dashboard
                </div>

            </div>

        </div>

    </div>

</div>

</div>

</body>
</html>