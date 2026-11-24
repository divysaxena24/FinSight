package com.expense.controller;

import java.io.IOException;

import com.expense.dao.PasswordResetDAO;
import com.expense.dao.UserDAO;
import com.expense.model.User;
import com.expense.service.PasswordResetEmailSender;
import com.finsight.security.CsrfTokenManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetDAO passwordResetDAO = new PasswordResetDAO();
    private final PasswordResetEmailSender emailSender = new PasswordResetEmailSender();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(true);
        request.setAttribute("csrfToken", CsrfTokenManager.getOrCreateToken(session));
        request.getRequestDispatcher("/WEB-INF/views/forgot_password.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!CsrfTokenManager.isValidToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF security token.");
            return;
        }

        String email = request.getParameter("email");
        if (email != null && !email.trim().isEmpty()) {
            User user = userDAO.getUserByEmail(email);
            if (user != null) {
                String rawToken = passwordResetDAO.createToken(user.getId());
                if (rawToken != null) {
                    try {
                        emailSender.sendResetEmail(user.getEmail(), buildResetUrl(request, rawToken));
                    } catch (Exception e) {
                        System.err.println("[ForgotPasswordServlet] Password reset email failed: "
                                + e.getClass().getSimpleName());
                    }
                }
            }
        }

        request.getSession(true).setAttribute(
                "successMessage",
                "If an account exists for that email, a password reset link has been sent.");
        response.sendRedirect("login");
    }

    private String buildResetUrl(HttpServletRequest request, String rawToken) {
        String configuredBaseUrl = System.getenv("FINSIGHT_APP_BASE_URL");
        String baseUrl;
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
            baseUrl = configuredBaseUrl.replaceAll("/+$", "");
        } else {
            int port = request.getServerPort();
            boolean omitPort = (request.isSecure() && port == 443)
                    || (!request.isSecure() && port == 80);
            baseUrl = request.getScheme() + "://" + request.getServerName()
                    + (omitPort ? "" : ":" + port)
                    + request.getContextPath();
        }
        return baseUrl + "/reset-password?token=" + rawToken;
    }
}
