package com.expense.controller;

import java.io.IOException;
import java.util.List;

import com.expense.model.User;
import com.expense.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finsight.simulator.dao.AccountDAO;
import com.finsight.simulator.dao.LedgerDAO;
import com.finsight.simulator.model.Account;
import com.finsight.simulator.model.FinancialHealthReport;
import com.finsight.simulator.model.SimulatorTransaction;
import com.finsight.simulator.service.FinancialIntelligenceService;

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
    private final AccountDAO accountDAO = new AccountDAO();
    private final LedgerDAO ledgerDAO = new LedgerDAO();
    private final FinancialIntelligenceService intelligenceService = new FinancialIntelligenceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;

        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        // Simulator accounts and balance
        java.math.BigDecimal totalSimulatorBalance = accountDAO.getTotalBalanceForUser(user.getId());
        List<Account> userAccounts = accountDAO.getAccountsByUserId(user.getId());
        List<SimulatorTransaction> recentSimTransactions = ledgerDAO.getTransactionsForUser(user.getId(), 5);
        FinancialHealthReport healthReport = intelligenceService.calculateFinancialHealth(user.getId());

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

        // 3. Category-wise expense breakdown using the EXACT same resolved date range
        TransactionService.CategoryBreakdownData categoryData = transactionService.getCategoryBreakdownData(
                user.getId(), dateRange);

        // 4. Serialize chart datasets to JSON safely (Jackson) and escape HTML/script meta-characters
        String rawChartJson = objectMapper.writeValueAsString(chartData);
        String safeChartJson = escapeJsonForHtmlScript(rawChartJson);

        String rawCategoryJson = objectMapper.writeValueAsString(categoryData);
        String safeCategoryJson = escapeJsonForHtmlScript(rawCategoryJson);

        request.setAttribute("summary", summary);
        request.setAttribute("range", dateRange);
        request.setAttribute("chartDataJson", safeChartJson);
        request.setAttribute("hasChartData", chartData.isHasData());
        request.setAttribute("categoryData", categoryData);
        request.setAttribute("categoryDataJson", safeCategoryJson);
        request.setAttribute("hasCategoryData", categoryData.isHasData());
        request.setAttribute("user", user);

        // Simulator Attributes
        request.setAttribute("totalSimulatorBalance", totalSimulatorBalance);
        request.setAttribute("userAccounts", userAccounts);
        request.setAttribute("recentSimTransactions", recentSimTransactions);
        request.setAttribute("healthReport", healthReport);

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
