package com.finsight.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Utility for cryptographically secure CSRF token generation, storage, and constant-time validation.
 */
public final class CsrfTokenManager {

    public static final String CSRF_SESSION_ATTR = "FINSIGHT_CSRF_TOKEN";
    public static final String CSRF_FORM_PARAM = "csrfToken";
    public static final String CSRF_HEADER_NAME = "X-CSRF-TOKEN";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CsrfTokenManager() {}

    /**
     * Retrieves or generates a cryptographically secure CSRF token for the session.
     */
    public static String getOrCreateToken(HttpSession session) {
        if (session == null) {
            return null;
        }

        synchronized (session) {
            String token = (String) session.getAttribute(CSRF_SESSION_ATTR);
            if (token == null || token.trim().isEmpty()) {
                byte[] randomBytes = new byte[32];
                SECURE_RANDOM.nextBytes(randomBytes);
                token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
                session.setAttribute(CSRF_SESSION_ATTR, token);
            }
            return token;
        }
    }

    /**
     * Validates whether the incoming request contains a valid CSRF token matching the session token.
     * Looks in request parameter 'csrfToken' first, then request header 'X-CSRF-TOKEN'.
     * Uses MessageDigest.isEqual for constant-time comparison to prevent timing attacks.
     */
    public static boolean isValidToken(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }

        String sessionToken = (String) session.getAttribute(CSRF_SESSION_ATTR);
        if (sessionToken == null || sessionToken.trim().isEmpty()) {
            return false;
        }

        String incomingToken = request.getParameter(CSRF_FORM_PARAM);
        if (incomingToken == null || incomingToken.trim().isEmpty()) {
            incomingToken = request.getHeader(CSRF_HEADER_NAME);
        }

        if (incomingToken == null || incomingToken.trim().isEmpty()) {
            return false;
        }

        return MessageDigest.isEqual(
                sessionToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                incomingToken.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}

