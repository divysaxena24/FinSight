package com.expense.controller;

import java.io.IOException;

import com.expense.dao.TransactionDAO;
import com.expense.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/deleteTransaction")
public class DeleteTransactionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        String idParam = request.getParameter("id");

        if (idParam == null || idParam.isEmpty()) {
            response.sendRedirect("viewTransaction.jsp");
            return;
        }

        try {
            int id = Integer.parseInt(idParam);
            TransactionDAO dao = new TransactionDAO();

            // Secure deletion: verifies that transaction belongs to the logged-in user
            dao.deleteTransaction(id, user.getId());

            response.sendRedirect("viewTransaction.jsp");

        } catch (NumberFormatException e) {
            e.printStackTrace();
            response.sendRedirect("viewTransaction.jsp");
        }
    }
}
