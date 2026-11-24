package com.finsight.simulator.subscription;

import java.util.List;

/**
 * A service most people in India subscribe to, with the plans they can choose.
 */
public class SubscriptionOffer {

    private final String code;
    private final String name;
    private final String category;
    private final String summary;
    private final List<SubscriptionPlan> plans;

    public SubscriptionOffer(String code, String name, String category, String summary, List<SubscriptionPlan> plans) {
        this.code = code;
        this.name = name;
        this.category = category;
        this.summary = summary;
        this.plans = List.copyOf(plans);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getSummary() {
        return summary;
    }

    public List<SubscriptionPlan> getPlans() {
        return plans;
    }

    public SubscriptionPlan findPlan(String planCode) {
        if (planCode == null) {
            return null;
        }
        for (SubscriptionPlan plan : plans) {
            if (plan.getCode().equals(planCode)) {
                return plan;
            }
        }
        return null;
    }
}
