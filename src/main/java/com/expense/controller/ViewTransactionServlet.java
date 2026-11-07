package com.expense.controller;

import java.io.IOException;
import java.util.List;

import com.expense.model.Transaction;
import com.expense.model.User;
import com.expense.service.TransactionService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/viewTransaction", "/viewTransaction.jsp"})
public class ViewTransactionServlet extends HttpServlet {

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

        List<Transaction> transactions = transactionService.getUserTransactions(user.getId());
        request.setAttribute("transactions", transactions);
        request.setAttribute("user", user);

        request.getRequestDispatcher("/WEB-INF/views/viewTransaction.jsp").forward(request, response);
    }
}

