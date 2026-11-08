package com.finsight.simulator.controller;

import java.io.IOException;

import com.expense.dao.UserDAO;
import com.expense.dao.UserDAO.PinVerificationResult;
import com.expense.model.User;
import com.finsight.security.CsrfTokenManager;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/profile")
public class ProfileServlet extends HttpServlet {

    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;
        if (sessionUser == null) {
            response.sendRedirect("login");
            return;
        }

        // Fetch fresh state from database
        User user = userDAO.getUserById(sessionUser.getId());
        if (user == null) {
            response.sendRedirect("logout");
            return;
        }

        String csrfToken = CsrfTokenManager.getOrCreateToken(session);
        request.setAttribute("csrfToken", csrfToken);
        request.setAttribute("user", user);

        request.getRequestDispatcher("/WEB-INF/views/profile.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User sessionUser = (session != null) ? (User) session.getAttribute("user") : null;
        if (sessionUser == null) {
            response.sendRedirect("login");
            return;
        }

        if (!CsrfTokenManager.isValidToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF security token.");
            return;
        }

        String action = request.getParameter("action");
        if ("updateProfile".equals(action)) {
            String name = request.getParameter("name");
            String phone = request.getParameter("phone");
            if (name != null && !name.trim().isEmpty()) {
                boolean ok = userDAO.updateProfile(sessionUser.getId(), name.trim(), phone != null ? phone.trim() : null);
                if (ok) {
                    sessionUser.setName(name.trim());
                    sessionUser.setPhone(phone);
                    session.setAttribute("successMessage", "Profile details updated successfully.");
                } else {
                    session.setAttribute("errorMessage", "Failed to update profile details.");
                }
            }
        } else if ("updatePassword".equals(action)) {
            String oldPass = request.getParameter("oldPassword");
            String newPass = request.getParameter("newPassword");
            String confirmPass = request.getParameter("confirmPassword");

            if (newPass == null || newPass.length() < 6) {
                session.setAttribute("errorMessage", "New password must be at least 6 characters long.");
            } else if (!newPass.equals(confirmPass)) {
                session.setAttribute("errorMessage", "New passwords do not match.");
            } else {
                boolean ok = userDAO.updatePassword(sessionUser.getId(), oldPass, newPass);
                if (ok) {
                    session.setAttribute("successMessage", "Password updated successfully.");
                } else {
                    session.setAttribute("errorMessage", "Current password is incorrect.");
                }
            }
        } else if ("setupPin".equals(action)) {
            String pin = request.getParameter("pin");
            String confirmPin = request.getParameter("confirmPin");

            if (pin == null || !pin.matches("^\\d{4,6}$")) {
                session.setAttribute("errorMessage", "PIN must consist of 4 to 6 numeric digits.");
            } else if (!pin.equals(confirmPin)) {
                session.setAttribute("errorMessage", "PINs do not match.");
            } else {
                boolean ok = userDAO.setTransactionPin(sessionUser.getId(), pin);
                if (ok) {
                    session.setAttribute("successMessage", "Transaction PIN configured successfully.");
                } else {
                    session.setAttribute("errorMessage", "Failed to save Transaction PIN.");
                }
            }
        } else if ("changePin".equals(action)) {
            String currentPin = request.getParameter("currentPin");
            String newPin = request.getParameter("newPin");
            String confirmNewPin = request.getParameter("confirmNewPin");

            if (newPin == null || !newPin.matches("^\\d{4,6}$")) {
                session.setAttribute("errorMessage", "New PIN must consist of 4 to 6 numeric digits.");
            } else if (!newPin.equals(confirmNewPin)) {
                session.setAttribute("errorMessage", "New PINs do not match.");
            } else {
                boolean ok = userDAO.changeTransactionPin(sessionUser.getId(), currentPin, newPin);
                if (ok) {
                    session.setAttribute("successMessage", "Transaction PIN changed successfully.");
                } else {
                    session.setAttribute("errorMessage", "Current PIN is incorrect or verification is locked.");
                }
            }
        }

        response.sendRedirect("profile");
    }
}

