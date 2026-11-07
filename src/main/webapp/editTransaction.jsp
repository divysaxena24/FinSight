<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<%@ page import="com.expense.dao.TransactionDAO"%>
<%@ page import="com.expense.model.Transaction"%>
<%@ page import="com.expense.model.User"%>

<%
User user = (User) session.getAttribute("user");
if (user == null) {
    response.sendRedirect("login.jsp");
    return;
}

String idStr = request.getParameter("id");
if (idStr == null || idStr.trim().isEmpty()) {
    response.sendRedirect("viewTransaction.jsp");
    return;
}

int id = Integer.parseInt(idStr);
TransactionDAO dao = new TransactionDAO();

// Secure retrieval: only allow fetching if it belongs to current user
Transaction t = dao.getTransactionById(id, user.getId());

if (t == null) {
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

.form-group{
    margin-bottom:15px;
}

label{
    display:block;
    margin-bottom:5px;
    font-weight:bold;
}

input, select, textarea{
    width:100%;
    padding:10px;
    border:1px solid #ccc;
    border-radius:5px;
}

button{
    width:100%;
    padding:10px;
    background:#28a745;
    border:none;
    color:white;
    font-size:16px;
    border-radius:5px;
    cursor:pointer;
}

button:hover{
    background:#218838;
}

.back-link{
    display:block;
    text-align:center;
    margin-top:15px;
}

</style>

</head>
<body>

<div class="container">

<h2>Edit Transaction</h2>

<form action="updateTransaction" method="post">

    <input type="hidden" name="id" value="<%=t.getId()%>">

    <div class="form-group">
        <label>Type</label>
        <select name="type" required>
            <option value="Income" <%= "Income".equals(t.getType()) ? "selected" : "" %>>Income</option>
            <option value="Expense" <%= "Expense".equals(t.getType()) ? "selected" : "" %>>Expense</option>
        </select>
    </div>

    <div class="form-group">
        <label>Category</label>
        <input type="text" name="category" value="<%=t.getCategory()%>" required>
    </div>

    <div class="form-group">
        <label>Amount</label>
        <input type="number" step="0.01" name="amount" value="<%=t.getAmount()%>" required>
    </div>

    <div class="form-group">
        <label>Description</label>
        <textarea name="description"><%=t.getDescription()%></textarea>
    </div>

    <div class="form-group">
        <label>Date</label>
        <input type="date" name="transactionDate" value="<%=t.getTransactionDate()%>" required>
    </div>

    <button type="submit">Update Transaction</button>

</form>

<a href="viewTransaction.jsp" class="back-link">Back to Transactions</a>

</div>

</body>
</html>
