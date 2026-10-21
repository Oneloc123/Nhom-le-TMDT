package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.WorkshopDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

/** Chooses the existing portal associated with an authenticated account. */
public final class PortalRedirect {
    private static final System.Logger LOG = System.getLogger(PortalRedirect.class.getName());

    private PortalRedirect() { }

    public static String afterLogin(User user) {
        if (user != null && user.isAdmin()) return "/admin";
        if (user != null) {
            try {
                if (WorkshopDao.hasOwnedShop(user.getId())) return "/workshop/dashboard";
            } catch (DataAccessException e) {
                LOG.log(System.Logger.Level.WARNING,
                        "Could not determine workshop portal for signed-in account", e);
            }
        }
        return "/home";
    }
}
