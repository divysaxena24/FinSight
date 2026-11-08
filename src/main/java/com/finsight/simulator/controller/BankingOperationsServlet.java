package com.finsight.simulator.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import com.expense.dao.UserDAO;
import com.expense.dao.UserDAO.PinVerificationResult;
import com.expense.model.User;
import com.finsight.security.CsrfTokenManager;
import com.finsight.simulator.dao.AccountDAO;
import com.finsight.simulator.dao.LedgerDAO;
import com.finsight.simulator.dao.LedgerDAO.TransactionExecutionResult;
import com.finsight.simulator.model.Account;
import com.finsight.simulator.service.FinancialIntelligenceService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/banking")
public class BankingOperationsServlet extends HttpServlet {

    private final AccountDAO accountDAO = new AccountDAO();
    private final LedgerDAO ledgerDAO = new LedgerDAO();
    private final UserDAO userDAO = new UserDAO();
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

        String action = request.getParameter("action");
        if (action == null || action.trim().isEmpty()) {
            response.sendRedirect("dashboard");
            return;
        }

        String csrfToken = CsrfTokenManager.getOrCreateToken(session);
        request.setAttribute("csrfToken", csrfToken);

        List<Account> accounts = accountDAO.getAccountsByUserId(user.getId());
        request.setAttribute("accounts", accounts);
        request.setAttribute("user", user);

        if ("deposit".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/deposit.jsp").forward(request, response);
        } else if ("withdraw".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/withdraw.jsp").forward(request, response);
        } else if ("transfer".equals(action)) {
            request.getRequestDispatcher("/WEB-INF/views/transfer.jsp").forward(request, response);
        } else if ("receipt".equals(action)) {
            String ref = request.getParameter("ref");
            if (ref != null && !ref.trim().isEmpty()) {
                com.finsight.simulator.model.SimulatorTransaction tx = ledgerDAO.getTransactionByReference(ref.trim());
                if (tx != null) {
                    // Enforce ownership: Transaction must involve at least one account belonging to authenticated user
                    boolean authorized = false;
                    for (Account acc : accounts) {
                        if ((tx.getSourceAccountId() != null && tx.getSourceAccountId().equals(acc.getId()))
                                || (tx.getDestinationAccountId() != null && tx.getDestinationAccountId().equals(acc.getId()))) {
                            authorized = true;
                            break;
                        }
                    }
                    if (authorized) {
                        request.setAttribute("transaction", tx);
                    } else {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: You are not authorized to view this transaction receipt.");
                        return;
                    }
                }
            }
            request.getRequestDispatcher("/WEB-INF/views/receipt.jsp").forward(request, response);
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

        String action = request.getParameter("action");
        String csrfToken = CsrfTokenManager.getOrCreateToken(session);
        request.setAttribute("csrfToken", csrfToken);
        request.setAttribute("accounts", accountDAO.getAccountsByUserId(user.getId()));
        request.setAttribute("user", user);

        if ("deposit".equals(action)) {
            handleDeposit(request, response, user);
        } else if ("withdraw".equals(action)) {
            handleWithdrawal(request, response, user);
        } else if ("transfer".equals(action)) {
            handleTransfer(request, response, user);
        } else {
            response.sendRedirect("dashboard");
        }
    }

    private void handleDeposit(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String accountIdStr = request.getParameter("accountId");
        String amountStr = request.getParameter("amount");
        String desc = request.getParameter("description");
        String cat = request.getParameter("category");
        String idempotencyKey = request.getParameter("idempotencyKey");

        BigDecimal amount = parseAmount(amountStr);
        int accountId = parseAccountId(accountIdStr);

        if (accountId <= 0 || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            request.setAttribute("errorMessage", "Please select an account and specify a valid positive amount.");
            request.getRequestDispatcher("/WEB-INF/views/deposit.jsp").forward(request, response);
            return;
        }

        TransactionExecutionResult result = ledgerDAO.executeDeposit(
                user.getId(), accountId, amount, desc, cat != null ? cat : "Salary", idempotencyKey);

        if (result.isSuccess()) {
            response.sendRedirect("banking?action=receipt&ref=" + result.getTransaction().getReferenceNumber());
        } else {
            request.setAttribute("errorMessage", result.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/deposit.jsp").forward(request, response);
        }
    }

    private void handleWithdrawal(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String accountIdStr = request.getParameter("accountId");
        String amountStr = request.getParameter("amount");
        String desc = request.getParameter("description");
        String cat = request.getParameter("category");
        String pin = request.getParameter("transactionPin");
        String idempotencyKey = request.getParameter("idempotencyKey");

        BigDecimal amount = parseAmount(amountStr);
        int accountId = parseAccountId(accountIdStr);

        // 1. PIN Verification
        if (pin == null || pin.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Transaction PIN is required to authorize withdrawal.");
            request.getRequestDispatcher("/WEB-INF/views/withdraw.jsp").forward(request, response);
            return;
        }

        PinVerificationResult pinResult = userDAO.verifyTransactionPin(user.getId(), pin.trim());
        if (!pinResult.isSuccess()) {
            request.setAttribute("errorMessage", pinResult.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/withdraw.jsp").forward(request, response);
            return;
        }

        if (accountId <= 0 || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            request.setAttribute("errorMessage", "Please select an account and specify a valid positive amount.");
            request.getRequestDispatcher("/WEB-INF/views/withdraw.jsp").forward(request, response);
            return;
        }

        TransactionExecutionResult result = ledgerDAO.executeWithdrawal(
                user.getId(), accountId, amount, desc, cat != null ? cat : "Shopping", idempotencyKey);

        if (result.isSuccess()) {
            // Anomaly evaluation hook
            intelligenceService.evaluateAndRecordAnomaly(user.getId(), result.getTransaction());
            response.sendRedirect("banking?action=receipt&ref=" + result.getTransaction().getReferenceNumber());
        } else {
            request.setAttribute("errorMessage", result.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/withdraw.jsp").forward(request, response);
        }
    }

    private void handleTransfer(HttpServletRequest request, HttpServletResponse response, User user)
            throws ServletException, IOException {
        String sourceAccountIdStr = request.getParameter("sourceAccountId");
        String recipientAccountNo = request.getParameter("recipientAccountNumber");
        String amountStr = request.getParameter("amount");
        String desc = request.getParameter("description");
        String pin = request.getParameter("transactionPin");
        String idempotencyKey = request.getParameter("idempotencyKey");

        BigDecimal amount = parseAmount(amountStr);
        int sourceAccountId = parseAccountId(sourceAccountIdStr);

        // 1. PIN Verification
        if (pin == null || pin.trim().isEmpty()) {
            request.setAttribute("errorMessage", "Transaction PIN is required to authorize transfer.");
            request.getRequestDispatcher("/WEB-INF/views/transfer.jsp").forward(request, response);
            return;
        }

        PinVerificationResult pinResult = userDAO.verifyTransactionPin(user.getId(), pin.trim());
        if (!pinResult.isSuccess()) {
            request.setAttribute("errorMessage", pinResult.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/transfer.jsp").forward(request, response);
            return;
        }

        if (sourceAccountId <= 0 || recipientAccountNo == null || recipientAccountNo.trim().isEmpty()
                || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            request.setAttribute("errorMessage", "Please provide complete source account, recipient account, and positive transfer amount.");
            request.getRequestDispatcher("/WEB-INF/views/transfer.jsp").forward(request, response);
            return;
        }

        TransactionExecutionResult result = ledgerDAO.executeTransfer(
                user.getId(), sourceAccountId, recipientAccountNo.trim(), amount, desc, "Transfer", idempotencyKey);

        if (result.isSuccess()) {
            intelligenceService.evaluateAndRecordAnomaly(user.getId(), result.getTransaction());
            response.sendRedirect("banking?action=receipt&ref=" + result.getTransaction().getReferenceNumber());
        } else {
            request.setAttribute("errorMessage", result.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/transfer.jsp").forward(request, response);
        }
    }

    private BigDecimal parseAmount(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            return new BigDecimal(str.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int parseAccountId(String str) {
        if (str == null || str.trim().isEmpty()) return -1;
        try {
            return Integer.parseInt(str.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}

