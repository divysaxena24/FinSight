<%@ page language="java"
    contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>

<%@ page import="java.util.List"%>
<%@ page import="com.expense.model.User"%>
<%@ page import="com.expense.model.Transaction"%>
<%@ page import="com.expense.dao.TransactionDAO"%>

<%
    User user = (User) session.getAttribute("user");

    if (user == null) {
        response.sendRedirect("login.jsp");
        return;
    }

    TransactionDAO dao = new TransactionDAO();

    List<Transaction> list = dao.getTransactionsByUser(user.getId());
%>

<!DOCTYPE html>
<html>

<head>

    <meta charset="UTF-8">

    <title>View Transactions</title>

    <style>

        body {
            font-family: Arial, sans-serif;
            background: #f4f6f9;
            margin: 30px;
        }

        h2 {
            text-align: center;
            color: #333;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            background: white;
            margin-top: 20px;
        }

        th, td {
            border: 1px solid #ddd;
            padding: 12px;
            text-align: center;
        }

        th {
            background: #007bff;
            color: white;
        }

        tr:nth-child(even) {
            background: #f2f2f2;
        }

        .edit {
            background: #28a745;
            color: white;
            padding: 6px 12px;
            text-decoration: none;
            border-radius: 4px;
            margin-right: 5px;
        }

        .delete {
            background: #dc3545;
            color: white;
            padding: 6px 12px;
            text-decoration: none;
            border-radius: 4px;
        }

        .back {
            display: inline-block;
            margin-top: 20px;
            background: #007bff;
            color: white;
            padding: 10px 20px;
            text-decoration: none;
            border-radius: 5px;
        }

        .edit:hover {
            background: #218838;
        }

        .delete:hover {
            background: #c82333;
        }

        .back:hover {
            background: #0056b3;
        }

    </style>

</head>

<body>

    <h2>My Transactions</h2>

    <table>

        <tr>
            <th>Date</th>
            <th>Type</th>
            <th>Category</th>
            <th>Amount</th>
            <th>Description</th>
            <th>Action</th>
        </tr>

        <%
            for (Transaction t : list) {
        %>

        <tr>

            <td>
                <%= t.getTransactionDate() %>
            </td>

            <td>
                <%= t.getType() %>
            </td>

            <td>
                <%= t.getCategory() %>
            </td>

            <td>
                ₹<%= t.getAmount() %>
            </td>

            <td>
                <%= t.getDescription() %>
            </td>

            <td>

                <!-- Edit -->
                <a class="edit"
                   href="<%= request.getContextPath() %>/editTransaction.jsp?id=<%= t.getId() %>">
                    Edit
                </a>

                <!-- Delete -->
                <a class="delete"
                   href="<%= request.getContextPath() %>/deleteTransaction?id=<%= t.getId() %>"
                   onclick="return confirm('Are you sure you want to delete this transaction?');">
                    Delete
                </a>

            </td>

        </tr>

        <%
            }
        %>

    </table>

    <br>

    <a class="back"
       href="<%= request.getContextPath() %>/dashboard.jsp">
        ← Back to Dashboard
    </a>

</body>

</html>