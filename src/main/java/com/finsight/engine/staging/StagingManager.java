package com.finsight.engine.staging;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.finsight.engine.StatementAnalysisResult;

/**
 * Thread-safe In-Memory Staging Manager with automatic expiration and ownership validation.
 */
public class StagingManager {

    private static final long DEFAULT_TTL_SECONDS = 3600; // 1 hour
    private static final StagingManager INSTANCE = new StagingManager();

    private final Map<String, StagingSession> sessionStore = new ConcurrentHashMap<>();

    private StagingManager() {}

    public static StagingManager getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new staging session for a user.
     */
    public StagingSession createSession(int userId, StatementAnalysisResult result) {
        cleanupExpiredSessions();
        StagingSession session = new StagingSession(userId, result, DEFAULT_TTL_SECONDS);
        sessionStore.put(session.getStagingId(), session);
        return session;
    }

    /**
     * Retrieves a staging session if it belongs to the given user and has not expired.
     */
    public StagingSession getSession(String stagingId, int userId) {
        cleanupExpiredSessions();
        if (stagingId == null) return null;
        StagingSession session = sessionStore.get(stagingId);
        if (session == null) return null;

        if (session.getUserId() != userId || session.isExpired()) {
            return null;
        }

        return session;
    }

    /**
     * Removes a session after import or discard.
     */
    public void removeSession(String stagingId) {
        if (stagingId != null) {
            sessionStore.remove(stagingId);
        }
    }

    public synchronized void cleanupExpiredSessions() {
        Iterator<Map.Entry<String, StagingSession>> iterator = sessionStore.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, StagingSession> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
            }
        }
    }

    public void clearAll() {
        sessionStore.clear();
    }
}

