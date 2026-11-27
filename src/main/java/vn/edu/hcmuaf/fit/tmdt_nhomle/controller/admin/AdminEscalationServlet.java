package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.EscalationDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import java.io.IOException;
import java.util.Map;

@WebServlet(name = "adminEscalationServlet", value = "/admin/escalations")
public class AdminEscalationServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        User admin = AuthFilter.currentUser(req);
        if (admin == null || !admin.isAdmin()) { res.sendError(403); return; }

        req.setAttribute("escalations", EscalationDao.listForAdmin(req.getParameter("status"), req.getParameter("q")));

        long id = parseId(req.getParameter("id"));
        if (id > 0) {
            var esc = EscalationDao.findById(id);
            if (esc == null) { res.sendError(404); return; }
            req.setAttribute("escalation", esc);
            long orderId = ((Number) esc.get("orderId")).longValue();
            long shopId = ((Number) esc.get("shopId")).longValue();
            req.setAttribute("threadMessages", EscalationDao.threadForAdmin(orderId, shopId, id));
        }

        req.getRequestDispatcher("/WEB-INF/admin/escalations.jsp").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        User admin = AuthFilter.currentUser(req);
        if (admin == null || !admin.isAdmin()) { res.sendError(403); return; }

        long id = parseId(req.getParameter("escalationId"));
        if (id < 1) { res.sendError(400); return; }

        String action = req.getParameter("action");
        try {
            if ("assign".equals(action)) {
                EscalationDao.assign(id, admin.getId());
                res.sendRedirect(req.getContextPath() + "/admin/escalations?id=" + id + "&assigned=1");
            } else if ("reply".equals(action)) {
                var esc = EscalationDao.findById(id);
                if (esc == null) { res.sendError(404); return; }
                long orderId = ((Number) esc.get("orderId")).longValue();
                long shopId = ((Number) esc.get("shopId")).longValue();
                EscalationDao.sendFromAdmin(orderId, shopId, admin.getId(), id, req.getParameter("message"));
                res.sendRedirect(req.getContextPath() + "/admin/escalations?id=" + id + "&sent=1");
            } else if ("resolve".equals(action)) {
                String status = "CLOSED".equals(req.getParameter("resolveStatus")) ? "CLOSED" : "RESOLVED";
                EscalationDao.resolve(id, admin.getId(), req.getParameter("verdict"), status);
                res.sendRedirect(req.getContextPath() + "/admin/escalations?id=" + id + "&resolved=1");
            } else {
                throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException | DataAccessException e) {
            req.setAttribute("error", e.getMessage() == null ? "Lỗi xử lý escalation." : e.getMessage());
            req.setAttribute("escalations", EscalationDao.listForAdmin(req.getParameter("status"), req.getParameter("q")));
            if (id > 0) {
                var esc = EscalationDao.findById(id);
                req.setAttribute("escalation", esc);
                if (esc != null) {
                    long orderId = ((Number) esc.get("orderId")).longValue();
                    long shopId = ((Number) esc.get("shopId")).longValue();
                    req.setAttribute("threadMessages", EscalationDao.threadForAdmin(orderId, shopId, id));
                }
            }
            req.getRequestDispatcher("/WEB-INF/admin/escalations.jsp").forward(req, res);
        }
    }

    private static long parseId(String raw) {
        try { return Long.parseLong(raw); } catch (Exception e) { return -1; }
    }
}
