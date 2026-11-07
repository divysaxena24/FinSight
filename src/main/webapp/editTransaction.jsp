<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<%@ page import="com.expense.dao.TransactionDAO"%>
<%@ page import="com.expense.model.Transaction"%>

<%
int id = Integer.parseInt(request.getParameter("id"));

TransactionDAO dao = new TransactionDAO();

Transaction t = dao.getTransactionById(id);

if(t == null){
    response.sendRedirect("viewTransaction.jsp");
    return;
}
%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Edit Transaction</title>

<style>

*{
    margin:0;
    padding:0;
    box-sizing:border-box;
    font-family:Arial,sans-serif;
}

body{
    background:#f4f6f9;
}

.container{
    width:450px;
    margin:40px auto;
    background:white;
    padding:30px;
    border-radius:10px;
    box-shadow:0 0 10px rgba(0,0,0,.2);
}

h2{
    text-align:center;
    margin-bottom:20px;
}

label{
    display:block;
    margin-top:15px;
    margin-bottom:5px;
    font-weight:bold;
}

input,
select,
textarea{
    width:100%;
    padding:10px;
    border:1px solid #ccc;
    border-radius:5px;
}

textarea{
    height:80px;
    resize:none;
}

button{
    width:100%;
    margin-top:20px;
    padding:12px;
    background:#28a745;
    color:white;
    border:none;
    border-radius:5px;
    cursor:pointer;
}

button:hover{
    background:#218838;
}

a{
    display:block;
    text-align:center;
    margin-top:15px;
    text-decoration:none;
}

</style>

</head>

<body>

<div class="container">

<h2>Edit Transaction</h2>

<form action="updateTransaction" method="post">

<input type="hidden" name="id" value="<%=t.getId()%>">

<label>Type</label>

<select name="type">

<option value="Income"
<%=t.getType().equals("Income")?"selected":""%>>

Income

</option>

<option value="Expense"
<%=t.getType().equals("Expense")?"selected":""%>>

Expense

</option>

</select>

<label>Category</label>

<input type="text"
name="category"
value="<%=t.getCategory()%>">

<label>Amount</label>

<input
type="number"
step="0.01"
name="amount"
value="<%=t.getAmount()%>">

<label>Description</label>

<textarea
name="description"><%=t.getDescription()%></textarea>

<label>Date</label>

<input type="date" name="transactionDate" value="<%=t.getTransactionDate()%>">

<button type="submit">

Update Transaction

</button>

</form>

<a href="viewTransaction.jsp">

Back

</a>

</div>

</body>
</html>