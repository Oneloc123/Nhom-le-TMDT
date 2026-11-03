package vn.edu.hcmuaf.fit.tmdt_nhomle.security;

import jakarta.servlet.http.HttpSession;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class UserSessionRegistry {
    private static final ConcurrentHashMap<Long, Set<HttpSession>> SESSIONS = new ConcurrentHashMap<>();
    private UserSessionRegistry() { }

    public static void register(long userId, HttpSession session) {
        if (session != null) SESSIONS.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public static void unregister(long userId, HttpSession session) {
        if (session == null) return;
        Set<HttpSession> sessions = SESSIONS.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) SESSIONS.remove(userId, sessions);
        }
    }

    public static void invalidateOthers(long userId, HttpSession keep) {
        Set<HttpSession> sessions = SESSIONS.get(userId);
        if (sessions == null) return;
        for (HttpSession session : Set.copyOf(sessions)) {
            if (keep != null && keep.getId().equals(session.getId())) continue;
            try { session.invalidate(); } catch (IllegalStateException ignored) { }
            sessions.remove(session);
        }
    }
}
