package com.expense.controller;

import java.io.IOException;

import com.expense.dao.TransactionDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/deleteTransaction")
public class DeleteTransactionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String idParam = request.getParameter("id");

        if (idParam == null || idParam.isEmpty()) {
            response.sendRedirect("viewTransaction.jsp");
            return;
        }

        try {

            int id = Integer.parseInt(idParam);

            TransactionDAO dao = new TransactionDAO();

            dao.deleteTransaction(id);

            // After deleting, go back to transactions page
            response.sendRedirect("viewTransaction.jsp");

        } catch (NumberFormatException e) {

            e.printStackTrace();

            response.sendRedirect("viewTransaction.jsp");
        }
    }
}