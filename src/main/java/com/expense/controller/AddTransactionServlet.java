package com.expense.controller;

import java.io.IOException;

import com.expense.model.User;
import com.expense.service.TransactionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/addTransaction")
public class AddTransactionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final TransactionService transactionService = new TransactionService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        String type = request.getParameter("type");
        String category = request.getParameter("category");
        String amountStr = request.getParameter("amount");
        String description = request.getParameter("description");
        String transactionDate = request.getParameter("transactionDate");

        if (type == null || category == null || amountStr == null || transactionDate == null) {
            session.setAttribute("errorMessage", "All required fields must be filled.");
            response.sendRedirect("addTransactionPage");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.trim());
            if (amount <= 0) {
                session.setAttribute("errorMessage", "Amount must be greater than zero.");
                response.sendRedirect("addTransactionPage");
                return;
            }
        } catch (NumberFormatException e) {
            session.setAttribute("errorMessage", "Invalid amount entered. Please provide a valid number.");
            response.sendRedirect("addTransactionPage");
            return;
        }

        boolean created = transactionService.createTransaction(
                user.getId(), type, category, amount, description, transactionDate);

        if (created) {
            session.setAttribute("successMessage", "Transaction added successfully!");
            response.sendRedirect("dashboard");
        } else {
            session.setAttribute("errorMessage", "Failed to save transaction. Please check your inputs.");
            response.sendRedirect("addTransactionPage");
        }
    }
}
