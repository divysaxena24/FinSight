package com.expense.controller;

import java.io.IOException;
import java.sql.Date;

import com.expense.dao.TransactionDAO;
import com.expense.model.Transaction;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/updateTransaction")
public class UpdateTransactionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Transaction transaction = new Transaction();

        transaction.setId(Integer.parseInt(request.getParameter("id")));
        transaction.setType(request.getParameter("type"));
        transaction.setCategory(request.getParameter("category"));
        transaction.setAmount(Double.parseDouble(request.getParameter("amount")));
        transaction.setDescription(request.getParameter("description"));
        transaction.setTransactionDate(Date.valueOf(request.getParameter("transactionDate")));

        TransactionDAO dao = new TransactionDAO();

        if (dao.updateTransaction(transaction)) {

            response.sendRedirect("viewTransaction.jsp");

        } else {

            response.sendRedirect("editTransaction.jsp");

        }

    }
}