package com.finsight.simulator.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/**
 * A subscription the user explicitly chose to track.
 */
public class UserSubscription {

    private int id;
    private int userId;
    private String catalogCode;
    private String displayName;
    private String planCode;
    private String planName;
    private long pricePaise;
    private int cycleMonths;
    private int cycleDays;
    private String startDate;

    private boolean reminderDue;
    private String reminderMessage;
    private String renewalLabel;
    private int daysRemaining = -1;
    private String alertLevel; // "red", "orange", "blue"
    private String alertColor; // CSS hex
    private String alertBgColor;
    private String alertBorderColor;
    private boolean renewingWithin7Days;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getCatalogCode() {
        return catalogCode;
    }

    public void setCatalogCode(String catalogCode) {
        this.catalogCode = catalogCode;
    }

    public boolean isCustom() {
        return catalogCode == null || catalogCode.isBlank();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public long getPricePaise() {
        return pricePaise;
    }

    public void setPricePaise(long pricePaise) {
        this.pricePaise = pricePaise;
    }

    public BigDecimal getPriceRupees() {
        return BigDecimal.valueOf(pricePaise, 2);
    }

    public int getPriceRupeesInt() {
        return (int) (pricePaise / 100);
    }

    public int getCycleMonths() {
        return cycleMonths;
    }

    public void setCycleMonths(int cycleMonths) {
        this.cycleMonths = cycleMonths;
    }

    public int getCycleDays() {
        return cycleDays;
    }

    public void setCycleDays(int cycleDays) {
        this.cycleDays = cycleDays;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public LocalDate startAsDate() {
        return LocalDate.parse(startDate);
    }

    public boolean isReminderDue() {
        return reminderDue;
    }

    public void setReminderDue(boolean reminderDue) {
        this.reminderDue = reminderDue;
    }

    public String getReminderMessage() {
        return reminderMessage;
    }

    public void setReminderMessage(String reminderMessage) {
        this.reminderMessage = reminderMessage;
    }

    public String getRenewalLabel() {
        return renewalLabel;
    }

    public void setRenewalLabel(String renewalLabel) {
        this.renewalLabel = renewalLabel;
    }

    public int getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(int daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getAlertColor() {
        return alertColor;
    }

    public void setAlertColor(String alertColor) {
        this.alertColor = alertColor;
    }

    public String getAlertBgColor() {
        return alertBgColor;
    }

    public void setAlertBgColor(String alertBgColor) {
        this.alertBgColor = alertBgColor;
    }

    public String getAlertBorderColor() {
        return alertBorderColor;
    }

    public void setAlertBorderColor(String alertBorderColor) {
        this.alertBorderColor = alertBorderColor;
    }

    public boolean isRenewingWithin7Days() {
        return renewingWithin7Days;
    }

    public void setRenewingWithin7Days(boolean renewingWithin7Days) {
        this.renewingWithin7Days = renewingWithin7Days;
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
        if (cycleMonths == 1) {
            return "month";
        }
        if (cycleMonths == 3) {
            return "quarter";
        }
        if (cycleMonths == 6) {
            return "6 months";
        }
        if (cycleMonths == 12) {
            return "year";
        }
        if (cycleMonths > 0) {
            return cycleMonths + " months";
        }
        return "cycle";
    }

    public String getPriceLabel() {
        BigDecimal rupees = getPriceRupees();
        String amount = rupees.stripTrailingZeros().scale() <= 0
                ? String.format(Locale.ENGLISH, "%,d", rupees.longValue())
                : String.format(Locale.ENGLISH, "%,.2f", rupees);
        return "₹" + amount + " / " + getCycleLabel();
    }
}
