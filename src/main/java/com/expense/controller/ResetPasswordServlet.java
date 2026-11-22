package com.expense.controller;

import java.io.IOException;

import com.expense.dao.PasswordResetDAO;
import com.finsight.security.CsrfTokenManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/reset-password")
public class ResetPasswordServlet extends HttpServlet {

    private final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String token = request.getParameter("token");
        if (!passwordResetDAO.isTokenValid(token)) {
            request.setAttribute("invalidToken", true);
        } else {
            HttpSession session = request.getSession(true);
            request.setAttribute("csrfToken", CsrfTokenManager.getOrCreateToken(session));
            request.setAttribute("token", token);
        }
        request.getRequestDispatcher("/WEB-INF/views/reset_password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfTokenManager.isValidToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF security token.");
            return;
        }

        String token = request.getParameter("token");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        if (password == null || password.length() < 6) {
            request.setAttribute("errorMessage", "Password must be at least 6 characters long.");
            request.setAttribute("token", token);
            request.setAttribute("csrfToken",
                    CsrfTokenManager.getOrCreateToken(request.getSession(true)));
            request.getRequestDispatcher("/WEB-INF/views/reset_password.jsp").forward(request, response);
            return;
        }

        if (!password.equals(confirmPassword)) {
            request.setAttribute("errorMessage", "Passwords do not match.");
            request.setAttribute("token", token);
            request.setAttribute("csrfToken",
                    CsrfTokenManager.getOrCreateToken(request.getSession(true)));
            request.getRequestDispatcher("/WEB-INF/views/reset_password.jsp").forward(request, response);
            return;
        }

        PasswordResetDAO.ResetResult result = passwordResetDAO.resetPassword(token, password);
        if (result == PasswordResetDAO.ResetResult.SUCCESS) {
            request.getSession(true).setAttribute(
                    "successMessage", "Password reset successful. Please sign in.");
            response.sendRedirect("login");
        } else if (result == PasswordResetDAO.ResetResult.INVALID_OR_EXPIRED) {
            request.setAttribute("invalidToken", true);
            request.getRequestDispatcher("/WEB-INF/views/reset_password.jsp").forward(request, response);
        } else {
            request.setAttribute("errorMessage", "Password reset failed. Please request a new reset link.");
            request.setAttribute("invalidToken", true);
            request.getRequestDispatcher("/WEB-INF/views/reset_password.jsp").forward(request, response);
        }
    }
}
