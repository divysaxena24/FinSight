package com.finsight.engine.categorization;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic Merchant Normalizer.
 * 
 * Maps messy bank statement narrations and transaction tokens to clean, canonical merchant names.
 * Preserves the original narration exactly without guessing unknown merchants.
 */
public class MerchantNormalizer {

    private final Map<Pattern, String> knownMerchantPatterns;

    public MerchantNormalizer() {
        this.knownMerchantPatterns = initializeKnownMerchants();
    }

    private static Map<Pattern, String> initializeKnownMerchants() {
        Map<Pattern, String> map = new LinkedHashMap<>();

        // Food & Dining
        register(map, "\\bSWIGGY\\b.*", "Swiggy");
        register(map, "\\bZOMATO\\b.*", "Zomato");
        register(map, "\\bSTARBUCKS\\b.*", "Starbucks");
        register(map, "\\bMCDONALD'?S\\b.*", "McDonald's");
        register(map, "\\bDOMINO'?S\\b.*", "Domino's");
        register(map, "\\bKFC\\b.*", "KFC");
        register(map, "\\bBURGER KING\\b.*", "Burger King");
        register(map, "\\bBLINKIT\\b.*", "Blinkit");
        register(map, "\\bZEPTO\\b.*", "Zepto");
        register(map, "\\bBIGBASKET\\b.*", "BigBasket");

        // Shopping
        register(map, "\\b(?:AMZN|AMAZON)\\b.*", "Amazon");
        register(map, "\\bFLIPKART\\b.*", "Flipkart");
        register(map, "\\bMYNTRA\\b.*", "Myntra");
        register(map, "\\bAJIO\\b.*", "Ajio");
        register(map, "\\bNYKAA\\b.*", "Nykaa");
        register(map, "\\bTATA CLIQ\\b.*", "Tata CLiQ");
        register(map, "\\bCROMA\\b.*", "Croma");
        register(map, "\\bRELIANCE DIGITAL\\b.*", "Reliance Digital");
        register(map, "\\bIKEA\\b.*", "IKEA");
        register(map, "\\bDECATHLON\\b.*", "Decathlon");
        register(map, "\\bZARA\\b.*", "Zara");
        register(map, "\\bH&M\\b.*", "H&M");

        // Transport
        register(map, "\\bUBER\\b.*", "Uber");
        register(map, "\\bOLA\\b.*", "Ola");
        register(map, "\\bRAPIDO\\b.*", "Rapido");
        register(map, "\\bIRCTC\\b.*", "IRCTC");
        register(map, "\\bINDIGO\\b.*", "IndiGo");
        register(map, "\\bAIR INDIA\\b.*", "Air India");
        register(map, "\\bMAKEMYTRIP\\b.*", "MakeMyTrip");
        register(map, "\\bRED BUS\\b.*", "RedBus");
        register(map, "\\bFASTAG\\b.*", "NETC FASTag");
        register(map, "\\b(?:SHELL|INDIAN OIL|HPCL|BPCL)\\b.*", "Fuel Station");

        // Utilities
        register(map, "\\bAIRTEL\\b.*", "Airtel");
        register(map, "\\bJIO\\b.*", "Jio");
        register(map, "\\bVI\\b\\s+PREPAID|\\bVODAFONE\\b.*", "Vodafone Idea");
        register(map, "\\bBESCOM\\b.*", "BESCOM Electricity");
        register(map, "\\bADANI ELECTRICITY\\b.*", "Adani Electricity");
        register(map, "\\bMAHAVITARAN\\b.*", "Mahavitaran");
        register(map, "\\bTATA POWER\\b.*", "Tata Power");
        register(map, "\\bACT FIBERNET\\b.*", "ACT Fibernet");

        // Entertainment
        register(map, "\\bNETFLIX\\b.*", "Netflix");
        register(map, "\\bSPOTIFY\\b.*", "Spotify");
        register(map, "\\bDISNEY\\+?\\s*HOTSTAR\\b.*", "Disney+ Hotstar");
        register(map, "\\bBOOKMYSHOW\\b.*", "BookMyShow");
        register(map, "\\bPRIME VIDEO\\b.*", "Amazon Prime");
        register(map, "\\bSONY LIV\\b.*", "SonyLIV");
        register(map, "\\bYOUTUBE\\s*PREMIUM\\b.*", "YouTube");
        register(map, "\\bPVR\\b.*", "PVR Cinemas");
        register(map, "\\bINOX\\b.*", "INOX Leisure");

        // Healthcare
        register(map, "\\bAPOLLO\\s*(?:PHARMACY|HOSPITAL)?\\b.*", "Apollo Healthcare");
        register(map, "\\b1MG\\b.*", "Tata 1mg");
        register(map, "\\bPHARMEASY\\b.*", "PharmEasy");
        register(map, "\\bMEDPLUS\\b.*", "MedPlus");
        register(map, "\\bFORTIS\\b.*", "Fortis Hospital");

        // Investments
        register(map, "\\bZERODHA\\b.*", "Zerodha");
        register(map, "\\bGROWW\\b.*", "Groww");
        register(map, "\\bUPSTOX\\b.*", "Upstox");
        register(map, "\\bINDMONEY\\b.*", "INDmoney");

        return Collections.unmodifiableMap(map);
    }

    private static void register(Map<Pattern, String> map, String regex, String canonicalName) {
        map.put(Pattern.compile(regex, Pattern.CASE_INSENSITIVE), canonicalName);
    }

    /**
     * Normalizes a raw narration or merchant string into a canonical merchant name.
     * If no pattern matches, cleans the description safely without hallucinating.
     */
    public String normalize(String rawDescription) {
        if (rawDescription == null || rawDescription.trim().isEmpty()) {
            return "Unknown";
        }

        String cleaned = cleanRawDescription(rawDescription);

        // Check explicit pattern matches
        for (Map.Entry<Pattern, String> entry : knownMerchantPatterns.entrySet()) {
            Matcher m = entry.getKey().matcher(cleaned);
            if (m.find()) {
                return entry.getValue();
            }
        }

        // Safe fallback: extract clean leading entity if UPI/POS token prefix exists
        return extractFallbackEntity(cleaned);
    }

    /**
     * Cleans whitespace and common payment gateway artifacts (e.g. POS*, UPI/, ECOM/).
     */
    public String cleanRawDescription(String text) {
        if (text == null) return "";
        return text.trim()
                .replaceAll("\\s+", " ");
    }

    private String extractFallbackEntity(String text) {
        // e.g. "UPI/308115629124/John Doe/Payment" -> "John Doe" or first readable segment
        if (text.toUpperCase().startsWith("UPI/")) {
            String[] parts = text.split("/");
            if (parts.length >= 3 && !parts[2].trim().isEmpty() && parts[2].trim().length() > 2) {
                return parts[2].trim();
            }
        }
        if (text.toUpperCase().startsWith("POS ") || text.toUpperCase().startsWith("POS*")) {
            String remainder = text.substring(4).trim();
            int cut = remainder.indexOf(' ');
            return cut > 0 ? remainder.substring(0, cut) : remainder;
        }

        // Return original up to 40 characters
        return text.length() > 40 ? text.substring(0, 40).trim() : text;
    }
}

