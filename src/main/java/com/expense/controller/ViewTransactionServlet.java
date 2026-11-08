package com.expense.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.expense.model.Transaction;
import com.expense.model.UnifiedTransactionItem;
import com.expense.model.User;
import com.expense.service.TransactionService;
import com.finsight.simulator.dao.LedgerDAO;
import com.finsight.simulator.model.SimulatorTransaction;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/viewTransaction", "/viewTransaction.jsp"})
public class ViewTransactionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final TransactionService transactionService = new TransactionService();
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

        List<Transaction> manualTransactions = transactionService.getUserTransactions(user.getId());
        List<SimulatorTransaction> simTransactions = ledgerDAO.getTransactionsForUser(user.getId(), 200);

        List<UnifiedTransactionItem> unifiedList = new ArrayList<>();

        if (manualTransactions != null) {
            for (Transaction tx : manualTransactions) {
                UnifiedTransactionItem item = new UnifiedTransactionItem();
                item.setManualId(tx.getId());
                item.setReference("TXN-" + tx.getId());
                item.setDate(tx.getTransactionDate() != null ? tx.getTransactionDate().toString() : "");
                item.setType(tx.getType()); // "Income" or "Expense"
                item.setCategory(tx.getCategory() != null ? tx.getCategory() : "Other");
                item.setDescription(tx.getDescription() != null ? tx.getDescription() : "");
                item.setAmount(BigDecimal.valueOf(tx.getAmount()));
                item.setCredit("Income".equalsIgnoreCase(tx.getType()));
                item.setSource(UnifiedTransactionItem.SourceType.MANUAL);
                item.setStatus("COMPLETED");
                unifiedList.add(item);
            }
        }

        if (simTransactions != null) {
            for (SimulatorTransaction stx : simTransactions) {
                UnifiedTransactionItem item = new UnifiedTransactionItem();
                item.setReference(stx.getReferenceNumber());
                String dateStr = stx.getCreatedAt();
                if (dateStr != null && dateStr.length() >= 10) {
                    dateStr = dateStr.substring(0, 10);
                }
                item.setDate(dateStr != null ? dateStr : "");
                
                String typeStr;
                boolean isCredit;
                if (stx.getTransactionType() == SimulatorTransaction.TransactionType.DEPOSIT) {
                    typeStr = "Deposit";
                    isCredit = true;
                } else if (stx.getTransactionType() == SimulatorTransaction.TransactionType.TRANSFER) {
                    typeStr = "Transfer";
                    isCredit = false; // outgoing transfer from perspective of sender
                } else {
                    typeStr = "Withdrawal";
                    isCredit = false;
                }
                item.setType(typeStr);
                item.setCategory(stx.getCategory() != null ? stx.getCategory() : "Banking");
                item.setDescription(stx.getDescription() != null ? stx.getDescription() : "");
                item.setAmount(stx.getAmount() != null ? stx.getAmount() : BigDecimal.ZERO);
                item.setCredit(isCredit);
                item.setSource(UnifiedTransactionItem.SourceType.SIMULATOR);
                item.setStatus(stx.getStatus() != null ? stx.getStatus().name() : "COMPLETED");
                unifiedList.add(item);
            }
        }

        // Sort descending by date, then by reference/id
        unifiedList.sort(Comparator.comparing(UnifiedTransactionItem::getDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(UnifiedTransactionItem::getReference, Comparator.nullsLast(Comparator.reverseOrder())));

        request.setAttribute("unifiedTransactions", unifiedList);
        request.setAttribute("user", user);

        request.getRequestDispatcher("/WEB-INF/views/viewTransaction.jsp").forward(request, response);
    }
}
