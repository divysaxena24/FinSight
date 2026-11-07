package com.expense.controller;

import java.io.IOException;

import com.expense.model.Transaction;
import com.expense.model.User;
import com.expense.service.TransactionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/editTransaction", "/editTransaction.jsp"})
public class EditTransactionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final TransactionService transactionService = new TransactionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        String idStr = request.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            session.setAttribute("errorMessage", "Missing transaction ID.");
            response.sendRedirect("viewTransaction");
            return;
        }

        try {
            int id = Integer.parseInt(idStr.trim());
            Transaction transaction = transactionService.getTransaction(id, user.getId());

            if (transaction == null) {
                session.setAttribute("errorMessage", "Transaction not found or you do not have permission to edit it.");
                response.sendRedirect("viewTransaction");
                return;
            }

            request.setAttribute("transaction", transaction);
            request.getRequestDispatcher("/WEB-INF/views/editTransaction.jsp").forward(request, response);

        } catch (NumberFormatException e) {
            session.setAttribute("errorMessage", "Invalid transaction ID format.");
            response.sendRedirect("viewTransaction");
        }
    }
}
