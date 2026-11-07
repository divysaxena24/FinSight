package com.expense.controller;

import java.io.IOException;
import java.sql.Date;

import com.expense.dao.TransactionDAO;
import com.expense.model.Transaction;
import com.expense.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/updateTransaction")
public class UpdateTransactionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        try {
            Transaction transaction = new Transaction();
            transaction.setId(Integer.parseInt(request.getParameter("id")));
            transaction.setUserId(user.getId());
            transaction.setType(request.getParameter("type"));
            transaction.setCategory(request.getParameter("category"));
            transaction.setAmount(Double.parseDouble(request.getParameter("amount")));
            transaction.setDescription(request.getParameter("description"));
            transaction.setTransactionDate(Date.valueOf(request.getParameter("transactionDate")));

            TransactionDAO dao = new TransactionDAO();

            // Secure update: verifies that transaction belongs to the logged-in user
            if (dao.updateTransaction(transaction, user.getId())) {
                response.sendRedirect("viewTransaction.jsp");
            } else {
                response.sendRedirect("editTransaction.jsp?id=" + transaction.getId());
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("editTransaction.jsp");
        }
    }
}
