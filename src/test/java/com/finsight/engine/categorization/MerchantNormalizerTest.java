package com.finsight.engine.categorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MerchantNormalizer Unit Tests")
public class MerchantNormalizerTest {

    private MerchantNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new MerchantNormalizer();
    }

    @Test
    @DisplayName("1. Known food & dining merchant normalization")
    void testFoodMerchants() {
        assertEquals("Swiggy", normalizer.normalize("SWIGGY*ORDER12345 BANGALORE"));
        assertEquals("Zomato", normalizer.normalize("ZOMATO LIMITED GURGAON"));
        assertEquals("Starbucks", normalizer.normalize("STARBUCKS COFFEE KORAMANGALA"));
        assertEquals("McDonald's", normalizer.normalize("MCDONALD'S FAMILY RESTAURANT"));
        assertEquals("Domino's", normalizer.normalize("DOMINO'S PIZZA INDIRANAGAR"));
        assertEquals("Blinkit", normalizer.normalize("BLINKIT COMMERCE PRIVATE LIMITED"));
        assertEquals("Zepto", normalizer.normalize("ZEPTO GROCERY STORE"));
    }

    @Test
    @DisplayName("2. Known shopping merchant normalization")
    void testShoppingMerchants() {
        assertEquals("Amazon", normalizer.normalize("AMZN PAYMENTS INDIA PVT LTD"));
        assertEquals("Amazon", normalizer.normalize("AMAZON RETAIL INDIA"));
        assertEquals("Flipkart", normalizer.normalize("FLIPKART INTERNET PRIVATE"));
        assertEquals("Myntra", normalizer.normalize("MYNTRA DESIGNS BANGALORE"));
        assertEquals("Croma", normalizer.normalize("CROMA INFOTEC STORE 402"));
        assertEquals("Zara", normalizer.normalize("ZARA RETAIL STORE PHOENIX"));
        assertEquals("IKEA", normalizer.normalize("IKEA INDIA NAGASANDRA"));
    }

    @Test
    @DisplayName("3. Known transport merchant normalization")
    void testTransportMerchants() {
        assertEquals("Uber", normalizer.normalize("UBER INDIA RIDES"));
        assertEquals("Ola", normalizer.normalize("OLA CABS TRIP 9912"));
        assertEquals("IRCTC", normalizer.normalize("IRCTC RAIL TICKETING NEW DELHI"));
        assertEquals("IndiGo", normalizer.normalize("INDIGO AIRLINES 6E-201"));
        assertEquals("NETC FASTag", normalizer.normalize("FASTAG TOLL PLAZA BANGALORE"));
        assertEquals("Fuel Station", normalizer.normalize("INDIAN OIL PETROL PUMP WHITEFIELD"));
    }

    @Test
    @DisplayName("4. Known utilities merchant normalization")
    void testUtilitiesMerchants() {
        assertEquals("Airtel", normalizer.normalize("AIRTEL PREPAID RECHARGE"));
        assertEquals("Jio", normalizer.normalize("JIO POSTPAID BILL PAYMENT"));
        assertEquals("BESCOM Electricity", normalizer.normalize("BESCOM BILL PAYMENT KPTCL"));
        assertEquals("ACT Fibernet", normalizer.normalize("ACT FIBERNET BROADBAND"));
    }

    @Test
    @DisplayName("5. Known entertainment merchant normalization")
    void testEntertainmentMerchants() {
        assertEquals("Netflix", normalizer.normalize("NETFLIX ENTERTAINMENT SERVICES"));
        assertEquals("Spotify", normalizer.normalize("SPOTIFY INDIA SUBSCRIPTION"));
        assertEquals("BookMyShow", normalizer.normalize("BOOKMYSHOW CINEMA TICKETS"));
        assertEquals("PVR Cinemas", normalizer.normalize("PVR CINEMAS VEGA CITY"));
    }

    @Test
    @DisplayName("6. Known healthcare & investment merchant normalization")
    void testHealthAndInvestmentMerchants() {
        assertEquals("Apollo Healthcare", normalizer.normalize("APOLLO PHARMACY INDIRANAGAR"));
        assertEquals("Tata 1mg", normalizer.normalize("1MG HEALTHCARE TECHNOLOGIES"));
        assertEquals("Zerodha", normalizer.normalize("ZERODHA BROKING LTD"));
        assertEquals("Groww", normalizer.normalize("GROWW INVESTMENTS NEXTBILLION"));
    }

    @Test
    @DisplayName("7. Case and whitespace normalization")
    void testCaseAndWhitespace() {
        assertEquals("Swiggy", normalizer.normalize("   swiggy   order   "));
        assertEquals("Uber", normalizer.normalize("uber    india  "));
        assertEquals("Netflix", normalizer.normalize("netflix"));
    }

    @Test
    @DisplayName("8. Unknown merchant fallback preserves clean entity token")
    void testUnknownMerchantFallback() {
        // Safe extraction from UPI/Ref/Entity/Note format
        String entity = normalizer.normalize("UPI/308115629124/Local Tailor Shop/Payment");
        assertEquals("Local Tailor Shop", entity);

        // Safe extraction from POS format
        String pos = normalizer.normalize("POS Corner Bakery Store");
        assertEquals("Corner", pos);

        // Plain raw string up to length
        String raw = normalizer.normalize("Unregistered Neighborhood Grocer");
        assertEquals("Unregistered Neighborhood Grocer", raw);
    }

    @Test
    @DisplayName("9. UPI and POS prefix normalization with order IDs and reference numbers")
    void testUpiAndPosPrefixesWithOrderIds() {
        // UPI with reference numbers and known merchant
        assertEquals("Zomato", normalizer.normalize("UPI/308115629124/ZOMATO/01"));
        assertEquals("Swiggy", normalizer.normalize("UPI/982711200192/SWIGGY/ORDER_991823"));

        // POS with store identifier
        assertEquals("Croma", normalizer.normalize("POS*CROMA STORE"));
        assertEquals("Starbucks", normalizer.normalize("POS 4912 STARBUCKS INDIRANAGAR"));
    }

    @Test
    @DisplayName("10. Partial and misleading match prevention")
    void testPartialAndMisleadingMatches() {
        // "SUBWAY" contains "UB" or "WAY" but must not match "UBER"
        assertEquals("SUBWAY RESTAURANT", normalizer.normalize("SUBWAY RESTAURANT"));

        // "AMAZING DEALS" must not match "AMAZON" or "AMZN"
        assertEquals("AMAZING DEALS HYPERMARKET", normalizer.normalize("AMAZING DEALS HYPERMARKET"));

        // "FLIP" without "FLIPKART" must not match Flipkart
        assertEquals("FLIP FLOP SHOE STORE", normalizer.normalize("FLIP FLOP SHOE STORE"));
    }

    @Test
    @DisplayName("11. Extra-long narrations beyond 100 characters are safely bounded")
    void testExtraLongNarrations() {
        String longNarration = "VERY LONG UNKNOWN TRANSACTION NARRATION OVER A HUNDRED CHARACTERS REPEATED FOR BOUND TESTING PURPOSE 1234567890 1234567890";
        String normalized = normalizer.normalize(longNarration);
        assertNotNull(normalized);
        assertTrue(normalized.length() <= 40);
    }

    @Test
    @DisplayName("12. Null and empty input returns Unknown")
    void testNullAndEmpty() {
        assertEquals("Unknown", normalizer.normalize(null));
        assertEquals("Unknown", normalizer.normalize("   "));
    }
}

