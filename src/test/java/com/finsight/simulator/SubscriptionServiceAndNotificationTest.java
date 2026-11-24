package com.finsight.simulator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.expense.dao.UserDAO;
import com.expense.model.User;
import com.expense.util.DBConnection;
import com.finsight.simulator.dao.SubscriptionDAO;
import com.finsight.simulator.subscription.SubscriptionCatalog;
import com.finsight.simulator.subscription.SubscriptionOffer;
import com.finsight.simulator.subscription.SubscriptionRenewal;
import com.finsight.simulator.subscription.UserSubscription;

public class SubscriptionServiceAndNotificationTest {

    private Path tempDb;
    private SubscriptionDAO subscriptionDAO;
    private UserDAO userDAO;
    private int testUserId;

    @BeforeEach
    void setUp() throws Exception {
        tempDb = Files.createTempFile("finsight-subs-test-", ".db");
        DBConnection.setThreadDatabasePath(tempDb.toString());
        subscriptionDAO = new SubscriptionDAO();
        userDAO = new UserDAO();

        User user = new User(0, "Test Subscriber", "subscriber@example.com", "Password123!");
        userDAO.registerUserWithResult(user);
        User dbUser = userDAO.getUserByEmail("subscriber@example.com");
        testUserId = dbUser.getId();
    }

    @AfterEach
    void tearDown() throws Exception {
        DBConnection.clearThreadDatabasePath();
        if (tempDb != null) {
            Files.deleteIfExists(tempDb);
        }
    }

    @Test
    void testCatalogHasTop10SubscriptionsWithPlans() {
        List<SubscriptionOffer> offers = SubscriptionCatalog.offers();
        assertEquals(10, offers.size(), "Catalog must contain exactly 10 popular subscription services");

        String[] expectedCodes = {
            "netflix", "amazon-prime", "disney-hotstar", "spotify", "youtube-premium",
            "chatgpt", "jiocinema", "zee5", "sonyliv", "apple-music"
        };

        for (String code : expectedCodes) {
            SubscriptionOffer offer = SubscriptionCatalog.find(code);
            assertNotNull(offer, "Expected catalog offer: " + code);
            assertFalse(offer.getPlans().isEmpty(), "Offer " + code + " must have selectable plans");
            for (var plan : offer.getPlans()) {
                assertTrue(plan.getPriceRupees() > 0, "Plan price must be positive");
                assertTrue(plan.getCycleMonths() > 0 || plan.getCycleDays() > 0, "Plan cycle must be positive");
                assertNotNull(plan.getOptionLabel(), "Option label must not be null");
                assertNotNull(plan.getLastVerified(), "Last verified must not be null");
            }
        }
    }

    @Test
    void testSubscriptionRenewalCalculationAndAlertThresholds() {
        LocalDate today = LocalDate.of(2026, 11, 24);

        // Scenario 1: Red Alert (<= 7 days): started 2026-10-27 (1-month cycle) -> next renewal 2026-11-27 (3 days)
        UserSubscription sub1 = new UserSubscription();
        sub1.setDisplayName("Netflix");
        sub1.setPlanName("Standard");
        sub1.setPricePaise(49900);
        sub1.setCycleMonths(1);
        sub1.setStartDate("2026-10-27");

        SubscriptionRenewal.apply(sub1, today);

        assertEquals(3, sub1.getDaysRemaining(), "Should have 3 days remaining until renewal");
        assertTrue(sub1.isReminderDue(), "Deadline is <= 7 days, reminder must be due");
        assertTrue(sub1.isRenewingWithin7Days(), "Within 7 days flag must be true");
        assertEquals("red", sub1.getAlertLevel());
        assertEquals("#dc2626", sub1.getAlertColor());
        assertNotNull(sub1.getReminderMessage(), "Reminder message should be set");
        assertTrue(sub1.getReminderMessage().contains("Netflix Standard (₹499) renews in 3 days on 27 Nov 2026"),
                "Message must match required template: " + sub1.getReminderMessage());

        // Scenario 2: Orange Alert (8 - 10 days): renewal in 10 days
        UserSubscription sub2 = new UserSubscription();
        sub2.setDisplayName("ChatGPT");
        sub2.setPlanName("Plus");
        sub2.setPricePaise(199900);
        sub2.setCycleMonths(1);
        sub2.setStartDate("2026-11-04");

        SubscriptionRenewal.apply(sub2, today);

        assertEquals(10, sub2.getDaysRemaining(), "Should have 10 days remaining");
        assertTrue(sub2.isReminderDue(), "Deadline is 10 days, reminder should be due");
        assertFalse(sub2.isRenewingWithin7Days(), "10 days is not within 7 days");
        assertEquals("orange", sub2.getAlertLevel());
        assertEquals("#d97706", sub2.getAlertColor());
        assertTrue(sub2.getReminderMessage().contains("ChatGPT Plus (₹1999) renews in 10 days on 04 Dec 2026"));

        // Scenario 3: Blue Alert (11 - 30 days): renewal in 20 days
        UserSubscription sub3 = new UserSubscription();
        sub3.setDisplayName("Amazon Prime");
        sub3.setPlanName("Prime Annual");
        sub3.setPricePaise(149900);
        sub3.setCycleMonths(12);
        sub3.setStartDate("2025-12-14");

        SubscriptionRenewal.apply(sub3, today);

        assertEquals(20, sub3.getDaysRemaining(), "Should have 20 days remaining");
        assertTrue(sub3.isReminderDue(), "Deadline is 20 days (<= 30), reminder should be due");
        assertEquals("blue", sub3.getAlertLevel());
        assertEquals("#2563eb", sub3.getAlertColor());

        // Scenario 4: > 30 days -> no reminder
        UserSubscription sub4 = new UserSubscription();
        sub4.setDisplayName("Apple Music");
        sub4.setPlanName("Individual");
        sub4.setPricePaise(11900);
        sub4.setCycleMonths(12);
        sub4.setStartDate("2026-06-01");

        SubscriptionRenewal.apply(sub4, today);

        assertTrue(sub4.getDaysRemaining() > 30, "Should have > 30 days remaining");
        assertFalse(sub4.isReminderDue(), "Deadline is > 30 days, reminder should not be due yet");
    }

    @Test
    void testSubscriptionDAOCatalogAndCustomPersistence() {
        // Track a catalog subscription (e.g. JioHotstar)
        UserSubscription catalogSub = new UserSubscription();
        catalogSub.setUserId(testUserId);
        catalogSub.setCatalogCode("jiohotstar");
        catalogSub.setDisplayName("JioHotstar");
        catalogSub.setPlanCode("jiohotstar-super-m");
        catalogSub.setPlanName("Super");
        catalogSub.setPricePaise(14900);
        catalogSub.setCycleMonths(1);
        catalogSub.setCycleDays(0);
        catalogSub.setStartDate("2026-10-25");

        int id1 = subscriptionDAO.insert(catalogSub);
        assertTrue(id1 > 0, "Catalog subscription should be inserted successfully");

        // Track a custom subscription (e.g. Gym Membership)
        UserSubscription customSub = new UserSubscription();
        customSub.setUserId(testUserId);
        customSub.setCatalogCode(null);
        customSub.setDisplayName("Cult.fit Gym");
        customSub.setPlanName("Annual Pass");
        customSub.setPricePaise(1200000); // ₹12,000
        customSub.setCycleMonths(12);
        customSub.setCycleDays(0);
        customSub.setStartDate("2025-12-01");

        int id2 = subscriptionDAO.insert(customSub);
        assertTrue(id2 > 0, "Custom subscription should be inserted successfully");

        List<UserSubscription> userSubs = subscriptionDAO.listForUser(testUserId);
        assertEquals(2, userSubs.size(), "User should have 2 tracked subscriptions");

        // Verify finding by catalog code
        UserSubscription found = subscriptionDAO.findCatalog(testUserId, "jiohotstar");
        assertNotNull(found);
        assertEquals("Super", found.getPlanName());

        // Clean up
        assertTrue(subscriptionDAO.delete(id1, testUserId));
        assertTrue(subscriptionDAO.delete(id2, testUserId));
        assertEquals(0, subscriptionDAO.listForUser(testUserId).size(), "All test subscriptions deleted");
    }
}
