package com.finsight.simulator.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.expense.model.User;
import com.finsight.security.CsrfTokenManager;
import com.finsight.simulator.dao.AccountDAO;
import com.finsight.simulator.dao.LedgerDAO;
import com.finsight.simulator.model.Account;
import com.finsight.simulator.model.Account.AccountType;
import com.finsight.simulator.model.SimulatorTransaction;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/accounts")
public class AccountsServlet extends HttpServlet {

    private final AccountDAO accountDAO = new AccountDAO();
    private final LedgerDAO ledgerDAO = new LedgerDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        String csrfToken = CsrfTokenManager.getOrCreateToken(session);
        request.setAttribute("csrfToken", csrfToken);

        List<Account> accounts = accountDAO.getAccountsByUserId(user.getId());
        BigDecimal totalBalance = accountDAO.getTotalBalanceForUser(user.getId());

        String viewAccountIdStr = request.getParameter("id");
        if (viewAccountIdStr != null && !viewAccountIdStr.trim().isEmpty()) {
            try {
                int accId = Integer.parseInt(viewAccountIdStr.trim());
                Account selectedAcc = accountDAO.getAccountById(accId);
                if (selectedAcc != null && selectedAcc.getUserId() == user.getId()) {
                    List<SimulatorTransaction> history = ledgerDAO.getTransactionsForAccount(accId, 25);
                    BigDecimal reconciled = ledgerDAO.calculateReconciledBalance(accId);
                    request.setAttribute("selectedAccount", selectedAcc);
                    request.setAttribute("accountHistory", history);
                    request.setAttribute("reconciledBalance", reconciled);
                }
            } catch (NumberFormatException ignored) {}
        }

        request.setAttribute("accounts", accounts);
        request.setAttribute("totalBalance", totalBalance);
        request.setAttribute("user", user);

        request.getRequestDispatcher("/WEB-INF/views/accounts.jsp").forward(request, response);
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

        String action = request.getParameter("action");
        if ("create".equals(action)) {
            String typeStr = request.getParameter("accountType");
            AccountType type = "CURRENT".equalsIgnoreCase(typeStr) ? AccountType.CURRENT : AccountType.SAVINGS;

            Account created = accountDAO.createAccount(user.getId(), type, BigDecimal.ZERO);
            if (created != null) {
                session.setAttribute("successMessage", "Account created successfully! Account No: " + created.getAccountNumber());
            } else {
                session.setAttribute("errorMessage", "Failed to create account. Please try again.");
            }
        }

        response.sendRedirect("accounts");
    }
}

