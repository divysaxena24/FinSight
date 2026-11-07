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

@WebServlet("/updateTransaction")
public class UpdateTransactionServlet extends HttpServlet {

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

        String idStr = request.getParameter("id");
        String type = request.getParameter("type");
        String category = request.getParameter("category");
        String amountStr = request.getParameter("amount");
        String description = request.getParameter("description");
        String transactionDate = request.getParameter("transactionDate");

        if (idStr == null || type == null || category == null || amountStr == null || transactionDate == null) {
            session.setAttribute("errorMessage", "All required fields must be provided.");
            response.sendRedirect("viewTransaction");
            return;
        }

        int id;
        double amount;
        try {
            id = Integer.parseInt(idStr.trim());
            amount = Double.parseDouble(amountStr.trim());
            if (amount <= 0) {
                session.setAttribute("errorMessage", "Amount must be greater than zero.");
                response.sendRedirect("editTransaction?id=" + id);
                return;
            }
        } catch (NumberFormatException e) {
            session.setAttribute("errorMessage", "Invalid numeric values provided.");
            response.sendRedirect("viewTransaction");
            return;
        }

        boolean updated = transactionService.updateTransaction(
                id, user.getId(), type, category, amount, description, transactionDate);

        if (updated) {
            session.setAttribute("successMessage", "Transaction updated successfully!");
            response.sendRedirect("viewTransaction");
        } else {
            session.setAttribute("errorMessage", "Failed to update transaction or access denied.");
            response.sendRedirect("editTransaction?id=" + id);
        }
    }
}
