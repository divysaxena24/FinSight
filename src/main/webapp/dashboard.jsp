<%@ page import="com.expense.model.User"%>
<%@ page import="com.expense.dao.TransactionDAO"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.expense.model.User" %>

<%
    User user = (User) session.getAttribute("user");

    if (user == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    // Temporary values (will be replaced with database values later)
   TransactionDAO dao = new TransactionDAO();

	double totalIncome = dao.getTotalIncome(user.getId());
	double totalExpense = dao.getTotalExpense(user.getId());
	double balance = totalIncome - totalExpense;
%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Dashboard</title>

<style>
*{
    margin:0;
    padding:0;
    box-sizing:border-box;
    font-family:Arial, sans-serif;
}

body{
    background:#f4f6f9;
}

.container{
    width:80%;
    margin:40px auto;
}

h1{
    text-align:center;
    color:#2c3e50;
    margin-bottom:10px;
}

h2{
    text-align:center;
    color:#555;
    margin-bottom:30px;
}

.cards{
    display:flex;
    justify-content:space-between;
    gap:20px;
    margin-bottom:40px;
}

.card{
    flex:1;
    background:white;
    padding:25px;
    border-radius:10px;
    text-align:center;
    box-shadow:0 0 10px rgba(0,0,0,0.1);
}

.card h3{
    color:#555;
    margin-bottom:15px;
}

.card p{
    font-size:28px;
    font-weight:bold;
    color:#27ae60;
}

.buttons{
    display:flex;
    justify-content:center;
    gap:20px;
    flex-wrap:wrap;
}

.buttons a{
    text-decoration:none;
    background:#007bff;
    color:white;
    padding:12px 25px;
    border-radius:5px;
    font-size:16px;
}

.buttons a:hover{
    background:#0056b3;
}
</style>

</head>

<body>

<div class="container">

    <h1>Expense Management System</h1>

    <h2>Welcome <%= user.getName() %></h2>

    <div class="cards">

        <div class="card">
            <h3>Total Income</h3>
            <p>₹<%= totalIncome %></p>
        </div>

        <div class="card">
            <h3>Total Expense</h3>
            <p>₹<%= totalExpense %></p>
        </div>

        <div class="card">
            <h3>Balance</h3>
            <p>₹<%= balance %></p>
        </div>

    </div>

   
    <div class="buttons">
    	<a href="addTransaction.jsp">Add Transaction</a>
    	<a href="viewTransaction.jsp">View Transactions</a>
    	<a href="logout">Logout</a>
    </div>

</div>

</body>
</html>