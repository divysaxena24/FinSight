package com.finsight.simulator.subscription;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Calculates next renewal date and sets 30-day, 10-day, and 7-day alert thresholds.
 */
public final class SubscriptionRenewal {

    private static final DateTimeFormatter LABEL =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private SubscriptionRenewal() {
    }

    public static void apply(UserSubscription subscription, LocalDate today) {
        if (subscription == null || today == null) {
            return;
        }
        try {
            LocalDate next = nextRenewal(
                    subscription.startAsDate(),
                    subscription.getCycleMonths(),
                    subscription.getCycleDays(),
                    today);
            int days = (int) ChronoUnit.DAYS.between(today, next);
            subscription.setDaysRemaining(days);
            subscription.setRenewalLabel(next.format(LABEL));

            // Within 7 days counter
            boolean within7 = days >= 0 && days <= 7;
            subscription.setRenewingWithin7Days(within7);

            // Alerts on Dashboard & Alerts Page at 30 days, 10 days, 7 days or less
            // Color code: 30 days = blue, 10 days = orange, 7 days or less = red
            if (days >= 0 && days <= 7) {
                subscription.setReminderDue(true);
                subscription.setAlertLevel("red");
                subscription.setAlertColor("#dc2626");
                subscription.setAlertBgColor("#fef2f2");
                subscription.setAlertBorderColor("#fecaca");
                subscription.setReminderMessage(formatMessage(subscription, days));
            } else if (days > 7 && days <= 10) {
                subscription.setReminderDue(true);
                subscription.setAlertLevel("orange");
                subscription.setAlertColor("#d97706");
                subscription.setAlertBgColor("#fffbeb");
                subscription.setAlertBorderColor("#fde68a");
                subscription.setReminderMessage(formatMessage(subscription, days));
            } else if (days > 10 && days <= 30) {
                subscription.setReminderDue(true);
                subscription.setAlertLevel("blue");
                subscription.setAlertColor("#2563eb");
                subscription.setAlertBgColor("#eff6ff");
                subscription.setAlertBorderColor("#bfdbfe");
                subscription.setReminderMessage(formatMessage(subscription, days));
            } else {
                subscription.setReminderDue(false);
                subscription.setAlertLevel(null);
                subscription.setAlertColor(null);
                subscription.setReminderMessage(null);
            }
        } catch (RuntimeException ex) {
            subscription.setReminderDue(false);
            subscription.setAlertLevel(null);
            subscription.setReminderMessage(null);
            subscription.setDaysRemaining(-1);
            subscription.setRenewalLabel("Unavailable");
            subscription.setRenewingWithin7Days(false);
        }
    }

    public static LocalDate nextRenewal(LocalDate start, int cycleMonths, int cycleDays, LocalDate today) {
        if (start == null || today == null) {
            throw new IllegalArgumentException("start and today are required");
        }
        if (cycleMonths > 0) {
            for (int step = 1; step <= 600; step++) {
                LocalDate renewal = start.plusMonths((long) cycleMonths * step);
                if (!renewal.isBefore(today)) {
                    return renewal;
                }
            }
            throw new IllegalArgumentException("Could not resolve renewal date");
        }
        if (cycleDays > 0) {
            long elapsed = ChronoUnit.DAYS.between(start, today);
            if (elapsed < cycleDays) {
                return start.plusDays(cycleDays);
            }
            long completed = elapsed / cycleDays;
            if (elapsed % cycleDays == 0) {
                return today;
            }
            return start.plusDays(cycleDays * (completed + 1));
        }
        throw new IllegalArgumentException("A billing cycle is required");
    }

    private static String formatMessage(UserSubscription subscription, int days) {
        String who = subscription.getDisplayName() + " " + subscription.getPlanName()
                + " (₹" + subscription.getPriceRupeesInt() + ")";
        if (days == 0) {
            return who + " renews today on " + subscription.getRenewalLabel();
        }
        if (days == 1) {
            return who + " renews tomorrow on " + subscription.getRenewalLabel() + " (in 1 day)";
        }
        return who + " renews in " + days + " days on " + subscription.getRenewalLabel();
    }
}
