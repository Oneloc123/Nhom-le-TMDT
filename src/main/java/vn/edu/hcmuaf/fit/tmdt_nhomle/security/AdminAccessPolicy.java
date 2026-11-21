package vn.edu.hcmuaf.fit.tmdt_nhomle.security;

import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

/** Single authorization predicate used by the filter and unit tests. */
public final class AdminAccessPolicy {
    private AdminAccessPolicy() { }

    public static boolean canAccess(User user) {
        return user != null && user.isActive() && user.isAdmin() && !user.isDeleted();
    }
}
