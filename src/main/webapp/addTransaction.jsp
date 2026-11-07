<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Add Transaction</title>

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
    width:450px;
    margin:40px auto;
    background:#fff;
    padding:30px;
    border-radius:10px;
    box-shadow:0 0 10px rgba(0,0,0,0.2);
}

h2{
    text-align:center;
    color:#333;
    margin-bottom:25px;
}

label{
    display:block;
    margin-top:15px;
    margin-bottom:5px;
    font-weight:bold;
}

input[type=text],
input[type=number],
input[type=date],
select,
textarea{
    width:100%;
    padding:10px;
    border:1px solid #ccc;
    border-radius:5px;
    font-size:15px;
}

textarea{
    resize:none;
    height:80px;
}

button{
    width:100%;
    margin-top:25px;
    padding:12px;
    background:#28a745;
    color:white;
    border:none;
    border-radius:5px;
    font-size:16px;
    cursor:pointer;
}

button:hover{
    background:#218838;
}

.back{
    text-align:center;
    margin-top:15px;
}

.back a{
    text-decoration:none;
    color:#007bff;
}

.back a:hover{
    text-decoration:underline;
}

</style>

</head>
<body>

<div class="container">

<h2>Add Transaction</h2>

<form action="addTransaction" method="post">

    <label>Type</label>
    <select name="type" required>
        <option value="">--Select Type--</option>
        <option value="Income">Income</option>
        <option value="Expense">Expense</option>
    </select>

    <label>Category</label>
    <select name="category" required>
        <option value="">--Select Category--</option>
        <option>Food</option>
        <option>Travel</option>
        <option>Shopping</option>
        <option>Medical</option>
        <option>Education</option>
        <option>Bills</option>
        <option>Salary</option>
        <option>Other</option>
    </select>

    <label>Amount</label>
    <input type="number" name="amount" step="0.01" required>

    <label>Description</label>
    <textarea name="description" placeholder="Enter description"></textarea>

    <label>Date</label>
    <input type="date" name="transactionDate" required>

    <button type="submit">Save Transaction</button>

</form>

<div class="back">
    <a href="dashboard.jsp">← Back to Dashboard</a>
</div>

</div>

</body>
</html>