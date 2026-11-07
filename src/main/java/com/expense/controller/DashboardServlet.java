package com.expense.controller;

import java.io.IOException;

import com.expense.model.User;
import com.expense.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/dashboard", "/dashboard.jsp"})
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final TransactionService transactionService = new TransactionService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        String rangeParam = request.getParameter("range");
        String customStart = request.getParameter("startDate");
        String customEnd = request.getParameter("endDate");

        // 1. Filtered financial summary using resolved date range
        TransactionService.DashboardSummary summary = transactionService.getFilteredDashboardSummary(
                user.getId(), rangeParam, customStart, customEnd);

        TransactionService.DateRangeResolution dateRange = summary.getDateRange();

        // 2. Continuous monthly trend chart data using the EXACT same resolved date range
        TransactionService.MonthlyTrendChartData chartData = transactionService.getMonthlyTrendChartData(
                user.getId(), dateRange);

        // 3. Serialize chart data to JSON safely (Jackson) and escape HTML/script meta-characters
        String rawJson = objectMapper.writeValueAsString(chartData);
        String safeJson = escapeJsonForHtmlScript(rawJson);

        request.setAttribute("summary", summary);
        request.setAttribute("range", dateRange);
        request.setAttribute("chartDataJson", safeJson);
        request.setAttribute("hasChartData", chartData.isHasData());
        request.setAttribute("user", user);

        request.getRequestDispatcher("/WEB-INF/views/dashboard.jsp").forward(request, response);
    }

    /**
     * Escapes characters that could terminate an HTML <script> block or trigger XSS
     */
    private String escapeJsonForHtmlScript(String json) {
        if (json == null) {
            return "{}";
        }
        return json.replace("&", "\\u0026")
                   .replace("<", "\\u003c")
                   .replace(">", "\\u003e")
                   .replace("'", "\\u0027");
    }
}
