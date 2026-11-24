package com.finsight.simulator.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.expense.model.User;
import com.finsight.security.CsrfTokenManager;
import com.finsight.simulator.dao.SubscriptionDAO;
import com.finsight.simulator.subscription.SubscriptionCard;
import com.finsight.simulator.subscription.SubscriptionCatalog;
import com.finsight.simulator.subscription.SubscriptionOffer;
import com.finsight.simulator.subscription.SubscriptionPlan;
import com.finsight.simulator.subscription.SubscriptionRenewal;
import com.finsight.simulator.subscription.UserSubscription;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet(urlPatterns = {"/subscriptions", "/subscriptions.jsp"})
public class SubscriptionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final LocalDate EARLIEST_START = LocalDate.of(2010, 1, 1);
    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000");

    private final SubscriptionDAO subscriptionDAO = new SubscriptionDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        User user = (session != null) ? (User) session.getAttribute("user") : null;
        if (user == null) {
            response.sendRedirect("login");
            return;
        }

        LocalDate today = LocalDate.now();
        List<UserSubscription> tracked = loadTracked(user.getId(), today);
        List<SubscriptionCard> cards = new ArrayList<>();
        for (SubscriptionOffer offer : SubscriptionCatalog.offers()) {
            UserSubscription match = null;
            for (UserSubscription row : tracked) {
                if (offer.getCode().equals(row.getCatalogCode())) {
                    match = row;
                    break;
                }
            }
            cards.add(new SubscriptionCard(offer, match));
        }

        List<UserSubscription> reminders = reminders(tracked);
        int renewingWithin7 = 0;
        for (UserSubscription row : tracked) {
            if (row.isRenewingWithin7Days()) {
                renewingWithin7++;
            }
        }

        request.setAttribute("csrfToken", CsrfTokenManager.getOrCreateToken(session));
        request.setAttribute("user", user);
        request.setAttribute("catalogCards", cards);
        request.setAttribute("trackedSubscriptions", tracked);
        request.setAttribute("subscriptionReminders", reminders);
        request.setAttribute("trackedCount", tracked.size());
        request.setAttribute("renewingWithin7Count", renewingWithin7);
        request.setAttribute("remindersCount", reminders.size());
        request.setAttribute("today", today.toString());
        request.getRequestDispatcher("/WEB-INF/views/subscriptions.jsp").forward(request, response);
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
        try {
            if ("track".equals(action)) {
                trackCatalog(session, user, request);
            } else if ("custom".equals(action)) {
                addCustom(session, user, request);
            } else if ("updateCustom".equals(action)) {
                updateCustom(session, user, request);
            } else if ("stop".equals(action)) {
                stop(session, user, request);
            } else {
                session.setAttribute("errorMessage", "That subscription action is not recognised.");
            }
        } catch (IllegalArgumentException ex) {
            session.setAttribute("errorMessage", ex.getMessage());
        }
        response.sendRedirect("subscriptions");
    }

    private void trackCatalog(HttpSession session, User user, HttpServletRequest request) {
        SubscriptionOffer offer = SubscriptionCatalog.find(request.getParameter("catalogCode"));
        if (offer == null) {
            throw new IllegalArgumentException("Choose one of the listed subscriptions.");
        }
        SubscriptionPlan plan = offer.findPlan(request.getParameter("planCode"));
        if (plan == null) {
            throw new IllegalArgumentException("Choose a plan for " + offer.getName() + ".");
        }
        LocalDate start = parseStartDate(request.getParameter("startDate"));

        UserSubscription row = subscriptionDAO.findCatalog(user.getId(), offer.getCode());
        boolean created = row == null;
        if (created) {
            row = new UserSubscription();
            row.setUserId(user.getId());
            row.setCatalogCode(offer.getCode());
        }
        row.setDisplayName(offer.getName());
        row.setPlanCode(plan.getCode());
        row.setPlanName(plan.getName());
        row.setPricePaise(plan.getPriceRupees() * 100L);
        row.setCycleMonths(plan.getCycleMonths());
        row.setCycleDays(plan.getCycleDays());
        row.setStartDate(start.toString());

        boolean saved = created ? subscriptionDAO.insert(row) > 0 : subscriptionDAO.update(row);
        if (!saved) {
            throw new IllegalArgumentException("Could not save " + offer.getName() + ". Please try again.");
        }
        session.setAttribute("successMessage", created
                ? offer.getName() + " is now tracked. You will get a dashboard reminder in the last 7 days before it renews."
                : offer.getName() + " plan updated. The reminder follows this plan and start date.");
    }

    private void addCustom(HttpSession session, User user, HttpServletRequest request) {
        UserSubscription row = customFromRequest(request);
        row.setUserId(user.getId());
        if (subscriptionDAO.insert(row) <= 0) {
            throw new IllegalArgumentException("Could not add that subscription. Please try again.");
        }
        session.setAttribute("successMessage",
                row.getDisplayName() + " is now tracked. You will get a dashboard reminder in the last 7 days before it renews.");
    }

    private void updateCustom(HttpSession session, User user, HttpServletRequest request) {
        int id = parseId(request.getParameter("subscriptionId"));
        UserSubscription existing = subscriptionDAO.findById(id, user.getId());
        if (existing == null || !existing.isCustom()) {
            throw new IllegalArgumentException("That subscription could not be updated.");
        }
        UserSubscription row = customFromRequest(request);
        row.setId(existing.getId());
        row.setUserId(user.getId());
        if (!subscriptionDAO.update(row)) {
            throw new IllegalArgumentException("Could not update that subscription. Please try again.");
        }
        session.setAttribute("successMessage", row.getDisplayName() + " was updated.");
    }

    private void stop(HttpSession session, User user, HttpServletRequest request) {
        int id = parseId(request.getParameter("subscriptionId"));
        UserSubscription existing = subscriptionDAO.findById(id, user.getId());
        if (existing == null || !subscriptionDAO.delete(id, user.getId())) {
            throw new IllegalArgumentException("That subscription is not being tracked.");
        }
        session.setAttribute("successMessage",
                existing.getDisplayName() + " is no longer tracked, so renewal reminders for it have stopped.");
    }

    private UserSubscription customFromRequest(HttpServletRequest request) {
        String name = cleanName(request.getParameter("displayName"), "Subscription name", 2, 60);
        String planName = cleanName(request.getParameter("planName"), "Plan name", 2, 40);
        long paise = parsePricePaise(request.getParameter("price"));
        int cycleMonths = 0;
        int cycleDays = 0;
        String cycle = request.getParameter("cycle");
        if ("days".equals(cycle)) {
            cycleDays = parseCycleDays(request.getParameter("cycleDays"));
        } else if ("1".equals(cycle) || "3".equals(cycle) || "6".equals(cycle) || "12".equals(cycle)) {
            cycleMonths = Integer.parseInt(cycle);
        } else {
            throw new IllegalArgumentException("Choose how often this subscription renews.");
        }
        LocalDate start = parseStartDate(request.getParameter("startDate"));

        UserSubscription row = new UserSubscription();
        row.setDisplayName(name);
        row.setPlanName(planName);
        row.setPricePaise(paise);
        row.setCycleMonths(cycleMonths);
        row.setCycleDays(cycleDays);
        row.setStartDate(start.toString());
        return row;
    }

    private static String cleanName(String raw, String label, int min, int max) {
        if (raw == null) {
            throw new IllegalArgumentException(label + " is required.");
        }
        String cleaned = raw.replaceAll("\\s+", " ").trim();
        if (cleaned.length() < min || cleaned.length() > max) {
            throw new IllegalArgumentException(label + " must be between " + min + " and " + max + " characters.");
        }
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (Character.isISOControl(c)) {
                throw new IllegalArgumentException(label + " contains a character that is not allowed.");
            }
        }
        return cleaned;
    }

    private static long parsePricePaise(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Enter the price you pay for one billing cycle.");
        }
        BigDecimal price;
        try {
            price = new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Enter the price as a number, such as 99 or 99.50.");
        }
        if (price.scale() > 2) {
            throw new IllegalArgumentException("Price can have at most 2 decimal places.");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0 || price.compareTo(MAX_PRICE) > 0) {
            throw new IllegalArgumentException("Price must be greater than 0 and at most ₹10,00,000.");
        }
        return price.movePointRight(2).longValueExact();
    }

    private static int parseCycleDays(String raw) {
        try {
            int days = Integer.parseInt(raw == null ? "" : raw.trim());
            if (days < 1 || days > 3650) {
                throw new NumberFormatException();
            }
            return days;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Custom renewal length must be between 1 and 3650 days.");
        }
    }

    private static LocalDate parseStartDate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Enter the date you started this subscription.");
        }
        LocalDate start;
        try {
            start = LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Enter a valid start date.");
        }
        LocalDate today = LocalDate.now();
        if (start.isAfter(today)) {
            throw new IllegalArgumentException("The start date cannot be in the future.");
        }
        if (start.isBefore(EARLIEST_START)) {
            throw new IllegalArgumentException("The start date must be 1 Jan 2010 or later.");
        }
        return start;
    }

    private static int parseId(String raw) {
        try {
            return Integer.parseInt(raw == null ? "" : raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("That subscription could not be found.");
        }
    }

    public static List<UserSubscription> loadTracked(int userId, LocalDate today) {
        SubscriptionDAO dao = new SubscriptionDAO();
        List<UserSubscription> tracked = dao.listForUser(userId);
        for (UserSubscription row : tracked) {
            SubscriptionRenewal.apply(row, today);
        }
        tracked.sort(Comparator
                .comparing(UserSubscription::isReminderDue).reversed()
                .thenComparingInt(UserSubscription::getDaysRemaining)
                .thenComparing(UserSubscription::getDisplayName, String.CASE_INSENSITIVE_ORDER));
        return tracked;
    }

    public static List<UserSubscription> reminders(List<UserSubscription> tracked) {
        List<UserSubscription> due = new ArrayList<>();
        for (UserSubscription row : tracked) {
            if (row.isReminderDue()) {
                due.add(row);
            }
        }
        return due;
    }
}
