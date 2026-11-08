package com.expense.controller;

import java.io.IOException;

import com.expense.dao.UserDAO;
import com.expense.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/register", "/register.jsp"})
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final UserDAO userDAO = new UserDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("user") != null) {
            response.sendRedirect("dashboard");
            return;
        }

        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");
        String pin = request.getParameter("transactionPin");

        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {

            request.setAttribute("errorMessage", "All required fields must be filled.");
            request.setAttribute("enteredName", name);
            request.setAttribute("enteredEmail", email);
            request.setAttribute("enteredPhone", phone);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        if (confirmPassword != null && !password.equals(confirmPassword)) {
            request.setAttribute("errorMessage", "Password and Confirm Password do not match.");
            request.setAttribute("enteredName", name);
            request.setAttribute("enteredEmail", email);
            request.setAttribute("enteredPhone", phone);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        if (password.length() < 6) {
            request.setAttribute("errorMessage", "Password must be at least 6 characters long.");
            request.setAttribute("enteredName", name);
            request.setAttribute("enteredEmail", email);
            request.setAttribute("enteredPhone", phone);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        if (pin != null && !pin.trim().isEmpty() && !pin.trim().matches("^\\d{4,6}$")) {
            request.setAttribute("errorMessage", "Transaction PIN must be 4 to 6 numeric digits.");
            request.setAttribute("enteredName", name);
            request.setAttribute("enteredEmail", email);
            request.setAttribute("enteredPhone", phone);
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
            return;
        }

        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.trim());
        user.setPhone(phone != null ? phone.trim() : null);
        user.setPassword(password);
        if (pin != null && !pin.trim().isEmpty()) {
            user.setTransactionPin(pin.trim());
        }

        boolean registered = userDAO.registerUser(user);

        if (registered) {
            HttpSession session = request.getSession(true);
            session.setAttribute("successMessage", "Registration successful! Please login with your credentials.");
            response.sendRedirect("login");
        } else {
            request.setAttribute("errorMessage", "An account with this email already exists or registration failed.");
            request.setAttribute("enteredName", name.trim());
            request.setAttribute("enteredEmail", email.trim());
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }
}
