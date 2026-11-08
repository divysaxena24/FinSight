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
import com.finsight.simulator.service.FinancialIntelligenceService;

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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect("login");
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
        List<BudgetRecommendation> recommendations = intelligenceService.getSmartBudgetRecommendations(user.getId());

        request.setAttribute("acceptedBudgets", acceptedBudgets);
        request.setAttribute("recommendations", recommendations);
        request.setAttribute("currentMonthYear", currentMonthYear);
        request.getRequestDispatcher("/WEB-INF/views/budgets.jsp").forward(request, response);
    }

    private void handleAlertsView(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        List<AnomalyAlert> alerts = intelligenceDAO.getAlertsForUser(user.getId(), 50);
        request.setAttribute("alerts", alerts);
        request.getRequestDispatcher("/WEB-INF/views/alerts.jsp").forward(request, response);
    }

    private void handleHealthView(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        FinancialHealthReport report = intelligenceService.calculateFinancialHealth(user.getId());
        request.setAttribute("healthReport", report);
        request.getRequestDispatcher("/WEB-INF/views/health.jsp").forward(request, response);
    }
}

