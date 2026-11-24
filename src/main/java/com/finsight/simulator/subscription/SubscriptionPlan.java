package com.finsight.simulator.subscription;

import java.util.Locale;

/**
 * One published billing option for a catalog subscription with verified India pricing.
 */
public class SubscriptionPlan {

    private final String code;
    private final String name;
    private final int priceRupees;
    private final int cycleMonths;
    private final int cycleDays;
    private final String details;
    private final String lastVerified;

    public SubscriptionPlan(String code, String name, int priceRupees, int cycleMonths, String details) {
        this(code, name, priceRupees, cycleMonths, 0, details, "November 2026");
    }

    public SubscriptionPlan(String code, String name, int priceRupees, int cycleMonths, int cycleDays, String details, String lastVerified) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Plan code and name are required");
        }
        if (priceRupees <= 0) {
            throw new IllegalArgumentException("Plan price must be positive");
        }
        if (cycleMonths <= 0 && cycleDays <= 0) {
            throw new IllegalArgumentException("Plan cycle must be positive");
        }
        this.code = code;
        this.name = name;
        this.priceRupees = priceRupees;
        this.cycleMonths = cycleMonths;
        this.cycleDays = cycleDays;
        this.details = details == null ? "" : details;
        this.lastVerified = lastVerified == null ? "November 2026" : lastVerified;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public int getPriceRupees() {
        return priceRupees;
    }

    public int getCycleMonths() {
        return cycleMonths;
    }

    public int getCycleDays() {
        return cycleDays;
    }

    public String getDetails() {
        return details;
    }

    public String getLastVerified() {
        return lastVerified;
    }

    public String getCycleLabel() {
        if (cycleDays == 7) {
            return "week";
        }
        if (cycleDays == 1) {
            return "day";
        }
        if (cycleDays > 0) {
            return cycleDays + " days";
        }
        return switch (cycleMonths) {
            case 1 -> "month";
            case 3 -> "quarter";
            case 6 -> "6 months";
            case 12 -> "year";
            default -> cycleMonths + " months";
        };
    }

    public String getOptionLabel() {
        return name + " · ₹" + String.format(Locale.ENGLISH, "%,d", priceRupees) + " / " + getCycleLabel();
    }
}
