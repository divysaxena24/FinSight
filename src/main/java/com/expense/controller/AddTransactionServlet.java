package com.expense.controller;

import java.io.IOException;
import java.sql.Date;

import com.expense.dao.TransactionDAO;
import com.expense.model.Transaction;
import com.expense.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/addTransaction")
public class AddTransactionServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException, IOException 
	{

        HttpSession session = request.getSession();

        User user = (User) session.getAttribute("user");

        Transaction transaction = new Transaction();

        transaction.setUserId(user.getId());
        transaction.setType(request.getParameter("type"));
        transaction.setCategory(request.getParameter("category"));
        transaction.setAmount(Double.parseDouble(request.getParameter("amount")));
        transaction.setDescription(request.getParameter("description"));
        transaction.setTransactionDate(Date.valueOf(request.getParameter("transactionDate")));

        TransactionDAO dao = new TransactionDAO();

        if (dao.addTransaction(transaction)) 
        {

            response.sendRedirect("dashboard.jsp");

        } 
        else 
        {

            response.sendRedirect("addTransaction.jsp");

        }
    }
}