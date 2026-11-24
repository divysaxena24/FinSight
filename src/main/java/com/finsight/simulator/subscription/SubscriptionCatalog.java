package com.finsight.simulator.subscription;

import java.util.List;

/**
 * Official 10 popular subscription services in India with published plans and verified pricing.
 * Last verified: November 2026.
 */
public final class SubscriptionCatalog {

    private static final List<SubscriptionOffer> OFFERS = List.of(
            netflix(),
            amazonPrime(),
            disneyHotstar(),
            spotify(),
            youtube(),
            chatGpt(),
            jioCinema(),
            zee5(),
            sonyLiv(),
            appleMusic()
    );

    private SubscriptionCatalog() {
    }

    public static List<SubscriptionOffer> offers() {
        return OFFERS;
    }

    public static SubscriptionOffer find(String code) {
        if (code == null) {
            return null;
        }
        for (SubscriptionOffer offer : OFFERS) {
            if (offer.getCode().equalsIgnoreCase(code)) {
                return offer;
            }
        }
        // Aliases
        if ("jiohotstar".equalsIgnoreCase(code)) {
            return find("disney-hotstar");
        }
        return null;
    }

    private static SubscriptionPlan plan(String code, String name, int rupees, int months, String details) {
        return new SubscriptionPlan(code, name, rupees, months, 0, details, "November 2026");
    }

    private static SubscriptionPlan planDays(String code, String name, int rupees, int days, String details) {
        return new SubscriptionPlan(code, name, rupees, 0, days, details, "November 2026");
    }

    private static SubscriptionOffer netflix() {
        return new SubscriptionOffer(
                "netflix",
                "Netflix",
                "Streaming",
                "Ad-free movies and television series. All plans in India are billed monthly.",
                List.of(
                        plan("netflix-mobile", "Mobile", 149, 1, "1 phone or tablet, 480p, downloads on 1 device"),
                        plan("netflix-basic", "Basic", 199, 1, "1 screen at a time, 720p HD, downloads on 1 device"),
                        plan("netflix-standard", "Standard", 499, 1, "2 screens at a time, 1080p Full HD, downloads on 2 devices"),
                        plan("netflix-premium", "Premium", 649, 1, "4 screens at a time, 4K Ultra HD + HDR, spatial audio, downloads on 6 devices")
                ));
    }

    private static SubscriptionOffer amazonPrime() {
        return new SubscriptionOffer(
                "amazon-prime",
                "Amazon Prime",
                "Shopping & Video",
                "Fast free delivery, Prime Video, Prime Music, Prime Reading, and exclusive member deals.",
                List.of(
                        plan("amazon-prime-m", "Prime Monthly", 299, 1, "Full Prime benefits with ad-supported Prime Video for 1 month"),
                        plan("amazon-prime-q", "Prime Quarterly", 599, 3, "Full Prime benefits for 3 months"),
                        plan("amazon-prime-y", "Prime Annual", 1499, 12, "Full Prime benefits for 1 year (Best Value)"),
                        plan("amazon-lite-y", "Prime Lite Annual", 799, 12, "Free delivery and Prime Video in HD on 1 device for 1 year"),
                        plan("amazon-shopping-y", "Prime Shopping Edition", 399, 12, "Free fast delivery benefits for a full year without Prime Video")
                ));
    }

    private static SubscriptionOffer disneyHotstar() {
        return new SubscriptionOffer(
                "disney-hotstar",
                "Disney+ Hotstar",
                "Streaming & Sports",
                "Live cricket, Star serials, Disney+ Originals, Marvel, and global blockbusters.",
                List.of(
                        plan("hotstar-mobile-m", "Mobile Monthly", 79, 1, "1 mobile phone, 720p HD, with ads"),
                        plan("hotstar-mobile-q", "Mobile Quarterly", 149, 3, "1 mobile phone for 3 months, 720p HD, with ads"),
                        plan("hotstar-mobile-y", "Mobile Annual", 499, 12, "1 mobile phone for 1 year, 720p HD, with ads"),
                        plan("hotstar-super-m", "Super Monthly", 149, 1, "2 devices at a time (TV/Laptop), 1080p Full HD, with ads"),
                        plan("hotstar-super-q", "Super Quarterly", 349, 3, "2 devices for 3 months, 1080p Full HD, with ads"),
                        plan("hotstar-super-y", "Super Annual", 1099, 12, "2 devices for 1 year, 1080p Full HD, with ads"),
                        plan("hotstar-premium-m", "Premium Monthly", 299, 1, "4 devices at a time, 4K Ultra HD, Dolby Vision, ad-free (except live sports)"),
                        plan("hotstar-premium-q", "Premium Quarterly", 699, 3, "4 devices for 3 months, 4K Ultra HD, ad-free"),
                        plan("hotstar-premium-y", "Premium Annual", 2199, 12, "4 devices for 1 year, 4K Ultra HD, ad-free (except live sports)")
                ));
    }

    private static SubscriptionOffer spotify() {
        return new SubscriptionOffer(
                "spotify",
                "Spotify",
                "Music",
                "Over 100 million songs and podcasts with offline listening and zero ad interruptions.",
                List.of(
                        planDays("spotify-mini", "Mini Weekly", 25, 7, "1 mobile account, 30 song downloads, ad-free music for 1 week"),
                        plan("spotify-student", "Student Monthly", 59, 1, "Discounted individual plan for accredited university students"),
                        plan("spotify-individual-m", "Individual Monthly", 119, 1, "1 Premium account, ad-free music, unlimited skips, group sessions"),
                        plan("spotify-duo-m", "Duo Monthly", 149, 1, "2 Premium accounts for couples under one roof, Duo Mix playlist"),
                        plan("spotify-family-m", "Family Monthly", 179, 1, "Up to 6 accounts for family members under one roof with explicit filter"),
                        plan("spotify-individual-y", "Individual Annual", 1189, 12, "Prepaid 1-year individual membership (saves ₹239)")
                ));
    }

    private static SubscriptionOffer youtube() {
        return new SubscriptionOffer(
                "youtube-premium",
                "YouTube Premium",
                "Video & Music",
                "Ad-free YouTube playback, background play, downloads, and full YouTube Music Premium access.",
                List.of(
                        plan("youtube-student", "Student Monthly", 89, 1, "Eligible university students with YouTube and YouTube Music ad-free"),
                        plan("youtube-individual-m", "Individual Monthly", 149, 1, "Ad-free videos, background play, and YouTube Music for 1 user"),
                        plan("youtube-individual-y", "Individual Annual", 1490, 12, "12-month prepaid plan with 2 months free"),
                        plan("youtube-family-m", "Family Monthly", 299, 1, "Up to 5 family members (ages 13+) in the same household")
                ));
    }

    private static SubscriptionOffer chatGpt() {
        return new SubscriptionOffer(
                "chatgpt",
                "ChatGPT Plus",
                "AI Productivity",
                "Advanced OpenAI models including GPT-4o, o1 reasoning, DALL-E image generation, and custom GPTs.",
                List.of(
                        plan("chatgpt-plus", "Plus Monthly", 1999, 1, "Access to GPT-4o, o1 reasoning, web browsing, data analysis, and voice"),
                        plan("chatgpt-pro", "Pro Monthly", 19990, 1, "Unlimited access to all models, OpenAI o1 pro mode, and compute priority")
                ));
    }

    private static SubscriptionOffer jioCinema() {
        return new SubscriptionOffer(
                "jiocinema",
                "JioCinema",
                "Streaming",
                "HBO, Max Originals, Peacock, NBCUniversal, Paramount, and blockbuster regional cinema.",
                List.of(
                        plan("jiocinema-premium-m", "Premium Monthly", 29, 1, "Ad-free international movies and shows in up to 4K on 1 device"),
                        plan("jiocinema-family-m", "Family Monthly", 89, 1, "Ad-free international content on up to 4 devices simultaneously")
                ));
    }

    private static SubscriptionOffer zee5() {
        return new SubscriptionOffer(
                "zee5",
                "Zee5",
                "Streaming",
                "Original web series, mega-hit Indian movies across 12 languages, and 90+ live channels.",
                List.of(
                        plan("zee5-premium-hd", "Premium HD Annual", 699, 12, "Watch on 2 devices simultaneously in 1080p HD for 1 full year"),
                        plan("zee5-premium-4k", "Premium 4K Annual", 899, 12, "Watch on 4 devices simultaneously in 4K with Dolby Atmos audio")
                ));
    }

    private static SubscriptionOffer sonyLiv() {
        return new SubscriptionOffer(
                "sonyliv",
                "SonyLIV",
                "Streaming",
                "Live European football (Champions League), international cricket, tennis, and premium Sony Originals.",
                List.of(
                        plan("sonyliv-mobile-y", "Mobile Only Annual", 599, 12, "1 mobile screen, 720p HD, live sports and originals for 1 year"),
                        plan("sonyliv-premium-m", "Premium Monthly", 399, 1, "Up to 2 screens at a time, Full HD + 4K, all content ad-free"),
                        plan("sonyliv-premium-6m", "Premium 6 Months", 699, 6, "Up to 2 screens at a time, Full HD + 4K for 6 months"),
                        plan("sonyliv-premium-y", "Premium Annual", 1499, 12, "Up to 2 screens at a time, Full HD + 4K for 1 full year")
                ));
    }

    private static SubscriptionOffer appleMusic() {
        return new SubscriptionOffer(
                "apple-music",
                "Apple Music",
                "Music",
                "Lossless audio, Spatial Audio with Dolby Atmos, Apple Music Sing, and over 100 million songs.",
                List.of(
                        plan("apple-music-student", "Student Monthly", 59, 1, "Lossless and spatial audio for accredited students"),
                        plan("apple-music-individual", "Individual Monthly", 119, 1, "Full Apple Music access across all your Apple & Android devices"),
                        plan("apple-music-family", "Family Monthly", 179, 1, "Unlimited access for up to 6 family members with personal music libraries")
                ));
    }
}
