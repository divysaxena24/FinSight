package com.finsight.simulator.subscription;

/**
 * A catalog service plus the plan this user is tracking, if they chose one.
 */
public class SubscriptionCard {

    private final SubscriptionOffer offer;
    private final UserSubscription tracked;

    public SubscriptionCard(SubscriptionOffer offer, UserSubscription tracked) {
        this.offer = offer;
        this.tracked = tracked;
    }

    public SubscriptionOffer getOffer() {
        return offer;
    }

    public UserSubscription getUserSubscription() {
        return tracked;
    }

    public boolean isTracked() {
        return tracked != null;
    }
}
