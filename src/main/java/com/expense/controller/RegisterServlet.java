package com.expense.controller;

import java.io.IOException;

import com.expense.dao.UserDAO;
import com.expense.model.User;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request,HttpServletResponse response)throws ServletException, IOException
	{

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        User user = new User();

        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);

        UserDAO dao = new UserDAO();

        if(dao.registerUser(user)) 
        {

            response.sendRedirect("login.jsp");

        } 
        else 
        {

            response.sendRedirect("register.jsp");

        }
    }
}
