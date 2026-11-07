package com.expense.controller;

import java.io.IOException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/logout")
public class LogoutServlet extends HttpServlet
{
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request,HttpServletResponse response)throws IOException
	{

        HttpSession session = request.getSession();

        session.invalidate();

        response.sendRedirect("login.jsp");
    }
}