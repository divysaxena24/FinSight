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

@WebServlet("/deleteTransaction")
public class DeleteTransactionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final TransactionService transactionService = new TransactionService();

    // Support POST for safe state mutation
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processDelete(request, response);
    }

    // Retain GET for backward compatibility with existing links, while logging/enforcing session verification
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processDelete(request, response);
    }

    private void processDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        String idParam = request.getParameter("id");

        if (idParam == null || idParam.trim().isEmpty()) {
            session.setAttribute("errorMessage", "Missing transaction ID for deletion.");
            response.sendRedirect("viewTransaction");
            return;
        }

        try {
            int id = Integer.parseInt(idParam.trim());
            boolean deleted = transactionService.deleteTransaction(id, user.getId());

            if (deleted) {
                session.setAttribute("successMessage", "Transaction deleted successfully.");
            } else {
                session.setAttribute("errorMessage", "Could not delete transaction. It may not exist or belongs to another user.");
            }

            response.sendRedirect("viewTransaction");

        } catch (NumberFormatException e) {
            session.setAttribute("errorMessage", "Invalid transaction ID format.");
            response.sendRedirect("viewTransaction");
        }
    }
}
