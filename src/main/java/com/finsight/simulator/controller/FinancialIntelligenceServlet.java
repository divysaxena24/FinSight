package com.finsight.simulator.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.expense.model.User;
import com.finsight.engine.insights.BudgetRecommendation;
import com.finsight.security.CsrfTokenManager;
import com.finsight.simulator.dao.IntelligenceDAO;
import com.finsight.simulator.model.AnomalyAlert;
import com.finsight.simulator.model.CategoryBudget;
import com.finsight.simulator.model.FinancialHealthReport;
import com.finsight.simulator.service.BudgetLimitService;
import com.finsight.simulator.service.FinancialIntelligenceService;
import com.finsight.simulator.subscription.UserSubscription;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/budgets", "/alerts", "/health"})
public class FinancialIntelligenceServlet extends HttpServlet {

    private final FinancialIntelligenceService intelligenceService = new FinancialIntelligenceService();
    private final IntelligenceDAO intelligenceDAO = new IntelligenceDAO();
    private final com.finsight.simulator.service.BudgetLimitService budgetLimitService = new com.finsight.simulator.service.BudgetLimitService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        String action = request.getParameter("action");
        if ("checkBudget".equals(action)) {
            handleBudgetCheck(request, response, user);
            return;
        }

        String servletPath = request.getServletPath();
        String csrfToken = CsrfTokenManager.getOrCreateToken(session);
        request.setAttribute("csrfToken", csrfToken);
        request.setAttribute("user", user);

        if ("/budgets".equals(servletPath)) {
            handleBudgetsView(request, response, user);
        } else if ("/alerts".equals(servletPath)) {
            handleAlertsView(request, response, user);
        } else if ("/health".equals(servletPath)) {
            handleHealthView(request, response, user);
        } else {
            response.sendRedirect("dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        if (!CsrfTokenManager.isValidToken(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF security token.");
            return;
        }

        String servletPath = request.getServletPath();
        if ("/budgets".equals(servletPath)) {
            String category = request.getParameter("category");
            String limitStr = request.getParameter("monthlyLimit");
            String monthYear = request.getParameter("monthYear");
            if (monthYear == null || monthYear.trim().isEmpty()) {
                monthYear = java.time.YearMonth.now().toString();
            }

            try {
                BigDecimal limit = new BigDecimal(limitStr.trim());
                if (limit.compareTo(BigDecimal.ZERO) > 0 && category != null && !category.trim().isEmpty()) {
                    intelligenceDAO.saveOrUpdateBudget(user.getId(), category.trim(), limit, monthYear);
                    session.setAttribute("successMessage", "Budget for " + category + " updated to ₹" + limit.toPlainString());
                }
            } catch (Exception e) {
                session.setAttribute("errorMessage", "Invalid budget amount.");
            }
            response.sendRedirect("budgets");

        } else if ("/alerts".equals(servletPath)) {
            String alertIdStr = request.getParameter("alertId");
            try {
                int alertId = Integer.parseInt(alertIdStr.trim());
                intelligenceDAO.acknowledgeAlert(alertId, user.getId());
                session.setAttribute("successMessage", "Alert acknowledged.");
            } catch (Exception ignored) {}
            response.sendRedirect("alerts");
        } else {
            response.sendRedirect("dashboard");
        }
    }

    private void handleBudgetsView(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String currentMonthYear = java.time.YearMonth.now().toString();
        List<CategoryBudget> acceptedBudgets = intelligenceDAO.getBudgetsForMonth(user.getId(), currentMonthYear);
        for (CategoryBudget b : acceptedBudgets) {
            BigDecimal spent = intelligenceDAO.getMonthlyCategorySpend(user.getId(), b.getCategory(), currentMonthYear);
            b.setCurrentSpent(spent);
            BigDecimal rem = b.getMonthlyLimit().subtract(spent);
            b.setRemaining(rem.compareTo(BigDecimal.ZERO) > 0 ? rem : BigDecimal.ZERO);
            if (b.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0) {
                double pct = spent.divide(b.getMonthlyLimit(), 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
                b.setPercentage(Math.round(pct * 10.0) / 10.0);
            }
            if (spent.compareTo(b.getMonthlyLimit()) > 0) {
                b.setExceeded(true);
                b.setExcessAmount(spent.subtract(b.getMonthlyLimit()));
            }
        }
        List<BudgetRecommendation> recommendations = intelligenceService.getSmartBudgetRecommendations(user.getId());

        request.setAttribute("acceptedBudgets", acceptedBudgets);
        request.setAttribute("recommendations", recommendations);
        request.setAttribute("currentMonthYear", currentMonthYear);
        request.getRequestDispatcher("/WEB-INF/views/budgets.jsp").forward(request, response);
    }

    private void handleBudgetCheck(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String category = request.getParameter("category");
        String amountStr = request.getParameter("amount");

        BigDecimal amount = BigDecimal.ZERO;
        try {
            if (amountStr != null && !amountStr.trim().isEmpty()) {
                amount = new BigDecimal(amountStr.trim());
            }
        } catch (Exception ignored) {}

        BudgetLimitService.BudgetSummary summary = budgetLimitService.getBudgetSummary(user.getId(), category, java.time.LocalDate.now());
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("hasBudget", summary.isHasBudget());
        data.put("category", summary.getCategory());
        data.put("limit", summary.getLimit());
        data.put("spent", summary.getSpent());
        data.put("remaining", summary.getRemaining());
        data.put("requestedAmount", amount);

        if (summary.isHasBudget() && amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal newTotal = summary.getSpent().add(amount);
            boolean exceeds = newTotal.compareTo(summary.getLimit()) > 0;
            data.put("exceeds", exceeds);
            if (exceeds) {
                BigDecimal excess = newTotal.subtract(summary.getLimit());
                data.put("excessAmount", excess);
                data.put("message", "You are crossing your " + summary.getCategory()
                        + " budget limit! Monthly limit: ₹" + summary.getLimit()
                        + ", Current spent: ₹" + summary.getSpent()
                        + ", Remaining: ₹" + summary.getRemaining()
                        + ". This payment of ₹" + amount + " exceeds your budget by ₹" + excess + ".");
            } else {
                data.put("exceeds", false);
                data.put("excessAmount", BigDecimal.ZERO);
            }
        } else {
            data.put("exceeds", false);
            data.put("excessAmount", BigDecimal.ZERO);
        }

        response.getWriter().write(mapper.writeValueAsString(data));
    }

    private void handleAlertsView(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        List<AnomalyAlert> alerts = intelligenceDAO.getAlertsForUser(user.getId(), 50);
        List<UserSubscription> tracked = SubscriptionServlet.loadTracked(user.getId(), java.time.LocalDate.now());
        List<UserSubscription> subscriptionReminders = SubscriptionServlet.reminders(tracked);
        request.setAttribute("alerts", alerts);
        request.setAttribute("subscriptionReminders", subscriptionReminders);
        request.getRequestDispatcher("/WEB-INF/views/alerts.jsp").forward(request, response);
    }

    private void handleHealthView(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        FinancialHealthReport report = intelligenceService.calculateFinancialHealth(user.getId());
        request.setAttribute("healthReport", report);
        request.getRequestDispatcher("/WEB-INF/views/health.jsp").forward(request, response);
    }
}

