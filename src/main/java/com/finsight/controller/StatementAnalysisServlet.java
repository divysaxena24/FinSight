package com.finsight.controller;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import com.expense.model.User;
import com.finsight.engine.StatementAnalysisEngine;
import com.finsight.engine.StatementAnalysisResult;
import com.finsight.engine.model.ParsedTransaction;
import com.finsight.engine.service.HistoricalTransactionAdapter;
import com.finsight.engine.service.StatementImportService;
import com.finsight.engine.service.StatementImportService.ImportResult;
import com.finsight.engine.staging.StagingManager;
import com.finsight.engine.staging.StagingSession;

/**
 * Controller handling end-to-end statement analysis workflow:
 * 1. Upload & Analysis
 * 2. Staging review & transaction updates
 * 3. Final approved database import
 * 4. Interactive report rendering
 */
@WebServlet("/statement-analysis")
@MultipartConfig(
    fileSizeThreshold = 1024 * 1024,        // 1 MB
    maxFileSize = 10 * 1024 * 1024,          // 10 MB
    maxRequestSize = 12 * 1024 * 1024        // 12 MB
)
public class StatementAnalysisServlet extends HttpServlet {

    private final StatementAnalysisEngine analysisEngine;
    private final StagingManager stagingManager;
    private final HistoricalTransactionAdapter historicalAdapter;
    private final StatementImportService importService;

    public StatementAnalysisServlet() {
        this.analysisEngine = new StatementAnalysisEngine();
        this.stagingManager = StagingManager.getInstance();
        this.historicalAdapter = new HistoricalTransactionAdapter();
        this.importService = new StatementImportService();
    }

    public StatementAnalysisServlet(StatementAnalysisEngine analysisEngine,
                                   StagingManager stagingManager,
                                   HistoricalTransactionAdapter historicalAdapter,
                                   StatementImportService importService) {
        this.analysisEngine = analysisEngine;
        this.stagingManager = stagingManager;
        this.historicalAdapter = historicalAdapter;
        this.importService = importService;
    }

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
        // Ensure CSRF token is initialized for the session
        String csrfToken = com.finsight.security.CsrfTokenManager.getOrCreateToken(session);
        request.setAttribute("csrfToken", csrfToken);

        if ("review".equals(action)) {
            handleReviewGet(request, response, user.getId());
        } else if ("report".equals(action)) {
            handleReportGet(request, response, user.getId());
        } else {
            // Default view: Upload Page
            request.setAttribute("user", user);
            request.getRequestDispatcher("/WEB-INF/views/statement_upload.jsp").forward(request, response);
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

        String action = request.getParameter("action");
        if ("upload".equals(action)) {
            if (!com.finsight.security.CsrfTokenManager.isValidToken(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF security token.");
                return;
            }
            handleUploadPost(request, response, user.getId());
        } else if ("update-tx".equals(action)) {
            if (!com.finsight.security.CsrfTokenManager.isValidToken(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF security token.");
                return;
            }
            handleUpdateTxPost(request, response, user.getId());
        } else if ("import".equals(action)) {
            if (!com.finsight.security.CsrfTokenManager.isValidToken(request)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid or missing CSRF security token.");
                return;
            }
            handleImportPost(request, response, user.getId());
        } else {
            response.sendRedirect("statement-analysis");
        }
    }

    private void handleUploadPost(HttpServletRequest request, HttpServletResponse response, int userId)
            throws ServletException, IOException {

        Part filePart = null;
        try {
            filePart = request.getPart("statementPdf");
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Unable to read uploaded file: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/statement_upload.jsp").forward(request, response);
            return;
        }

        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("errorMessage", "Please select a bank statement PDF file to upload.");
            request.getRequestDispatcher("/WEB-INF/views/statement_upload.jsp").forward(request, response);
            return;
        }

        // Parse optional declared boundaries
        LocalDate startDate = parseDate(request.getParameter("startDate"));
        LocalDate endDate = parseDate(request.getParameter("endDate"));

        // Retrieve historical records for multi-month context
        List<ParsedTransaction> priorHistory = historicalAdapter.getHistoricalParsedTransactions(userId);

        StatementAnalysisResult result;
        try (InputStream is = filePart.getInputStream()) {
            result = analysisEngine.analyze(is, startDate, endDate, priorHistory);
        } catch (Exception e) {
            request.setAttribute("errorMessage", "Error during statement processing: " + e.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/statement_upload.jsp").forward(request, response);
            return;
        }

        if (!result.isSuccessful() || result.getParsedTransactionCount() == 0) {
            request.setAttribute("analysisResult", result);
            String errorMsg = result.getFailureMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = (result.getParsedTransactionCount() == 0)
                        ? "No valid transactions could be parsed from the statement."
                        : "Analysis failed with status: " + result.getStatus();
            }
            request.setAttribute("errorMessage", errorMsg);
            request.getRequestDispatcher("/WEB-INF/views/statement_upload.jsp").forward(request, response);
            return;
        }

        // Store into secure server-side staging
        StagingSession stagingSession = stagingManager.createSession(userId, result);
        if (stagingSession.getTotalCount() == 0) {
            request.setAttribute("errorMessage", "No valid transactions were eligible for staging.");
            request.getRequestDispatcher("/WEB-INF/views/statement_upload.jsp").forward(request, response);
            return;
        }

        // Redirect to protected review page
        response.sendRedirect("statement-analysis?action=review&stagingId=" + stagingSession.getStagingId());
    }

    private void handleReviewGet(HttpServletRequest request, HttpServletResponse response, int userId)
            throws ServletException, IOException {
        String stagingId = request.getParameter("stagingId");
        StagingSession stagingSession = stagingManager.getSession(stagingId, userId);

        if (stagingSession == null) {
            request.setAttribute("errorMessage", "Review session expired or invalid. Please upload your statement again.");
            request.getRequestDispatcher("/WEB-INF/views/statement_upload.jsp").forward(request, response);
            return;
        }

        request.setAttribute("staging", stagingSession);
        request.setAttribute("result", stagingSession.getAnalysisResult());
        request.setAttribute("report", stagingSession.getAnalysisResult().getFinancialReport());
        request.getRequestDispatcher("/WEB-INF/views/statement_review.jsp").forward(request, response);
    }

    private void handleUpdateTxPost(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {
        String stagingId = request.getParameter("stagingId");
        String stagingTxId = request.getParameter("stagingTxId");

        StagingSession session = stagingManager.getSession(stagingId, userId);
        if (session == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Session expired or not found\"}");
            return;
        }

        LocalDate newDate = parseDate(request.getParameter("date"));
        String newDesc = request.getParameter("description");
        String newMerchant = request.getParameter("merchant");
        BigDecimal newAmount = parseAmount(request.getParameter("amount"));
        String newCategory = request.getParameter("category");
        Boolean selected = request.getParameter("selected") != null
                ? Boolean.valueOf(request.getParameter("selected")) : null;

        boolean updated = session.updateTransaction(stagingTxId, newDate, newDesc, newMerchant, newAmount, newCategory, selected);

        response.setContentType("application/json");
        if (updated) {
            response.getWriter().write(String.format("{\"success\": true, \"selectedCount\": %d}", session.getSelectedCount()));
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{\"error\": \"Transaction not found\"}");
        }
    }

    private void handleImportPost(HttpServletRequest request, HttpServletResponse response, int userId)
            throws ServletException, IOException {
        String stagingId = request.getParameter("stagingId");
        StagingSession session = stagingManager.getSession(stagingId, userId);

        if (session == null) {
            request.setAttribute("errorMessage", "Staging session expired. Please re-upload your statement.");
            request.getRequestDispatcher("/WEB-INF/views/statement_upload.jsp").forward(request, response);
            return;
        }

        ImportResult importResult = importService.importStagedTransactions(session, userId);

        if (importResult.isSuccess()) {
            // Keep report visible after import
            request.getSession().setAttribute("successMessage", importResult.getMessage());
            response.sendRedirect("statement-analysis?action=report&stagingId=" + stagingId);
        } else {
            request.setAttribute("staging", session);
            request.setAttribute("errorMessage", importResult.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/statement_review.jsp").forward(request, response);
        }
    }

    private void handleReportGet(HttpServletRequest request, HttpServletResponse response, int userId)
            throws ServletException, IOException {
        String stagingId = request.getParameter("stagingId");
        StagingSession session = stagingManager.getSession(stagingId, userId);

        if (session == null) {
            response.sendRedirect("dashboard");
            return;
        }

        request.setAttribute("staging", session);
        request.setAttribute("result", session.getAnalysisResult());
        request.setAttribute("report", session.getAnalysisResult().getFinancialReport());
        request.getRequestDispatcher("/WEB-INF/views/statement_report.jsp").forward(request, response);
    }

    private LocalDate parseDate(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(str.trim());
        } catch (Exception e) {
            return null;
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
}

