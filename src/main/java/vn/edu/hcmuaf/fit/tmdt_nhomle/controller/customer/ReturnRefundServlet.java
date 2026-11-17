package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.ReturnRefundDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;
import vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ReturnRefundEvidenceStore;
import vn.edu.hcmuaf.fit.tmdt_nhomle.service.ReturnRefundEvidenceStore.SavedEvidence;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Csrf;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@WebServlet(name = "customerReturnRefundServlet",
        urlPatterns = {"/return-refunds", "/return-refunds/evidence"})
@MultipartConfig(fileSizeThreshold = 1024 * 1024,
        maxFileSize = 5L * 1024 * 1024,
        maxRequestSize = 24L * 1024 * 1024)
public class ReturnRefundServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login?redirect="
                    + URLEncoder.encode("/return-refunds", StandardCharsets.UTF_8));
            return;
        }
        if ("/return-refunds/evidence".equals(request.getServletPath())) {
            streamEvidence(request, response, user.getId());
            return;
        }
        renderPage(request, response, user.getId(), null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        User user = AuthFilter.currentUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String action = trim(request.getParameter("action"));
        long requestId = positiveLong(request.getParameter("requestId"), 0);
        List<SavedEvidence> saved = new ArrayList<>();
        try {
            switch (action) {
                case "create" -> {
                    saved = saveUploads(request);
                    long createdId = ReturnRefundDao.createRequest(user.getId(),
                            positiveLong(request.getParameter("orderItemId"), -1),
                            trim(request.getParameter("requestType")),
                            trim(request.getParameter("reasonCode")),
                            integer(request.getParameter("quantity"), 0),
                            request.getParameter("customerNote"), uploadRecords(saved));
                    response.sendRedirect(request.getContextPath() + "/return-refunds?id=" + createdId + "&created=1");
                    return;
                }
                case "addEvidence" -> {
                    saved = saveUploads(request);
                    ReturnRefundDao.addEvidenceForUser(requestId, user.getId(), uploadRecords(saved));
                    response.sendRedirect(request.getContextPath() + "/return-refunds?id=" + requestId + "&evidenceAdded=1");
                    return;
                }
                case "withdraw" -> {
                    ReturnRefundDao.withdraw(requestId, user.getId());
                    response.sendRedirect(request.getContextPath() + "/return-refunds?id=" + requestId + "&withdrawn=1");
                    return;
                }
                case "markShipped" -> {
                    ReturnRefundDao.markReturnShipped(requestId, user.getId(),
                            request.getParameter("returnCarrier"), request.getParameter("returnTrackingNumber"));
                    response.sendRedirect(request.getContextPath() + "/return-refunds?id=" + requestId + "&shipped=1");
                    return;
                }
                default -> throw new IllegalArgumentException("Thao tác không hợp lệ.");
            }
        } catch (IllegalArgumentException | DataAccessException e) {
            deleteFiles(saved);
            renderPage(request, response, user.getId(), e.getMessage() == null
                    ? "Không thể xử lý yêu cầu. Vui lòng kiểm tra dữ liệu." : e.getMessage());
        } catch (IOException e) {
            deleteFiles(saved);
            throw e;
        } catch (ServletException e) {
            deleteFiles(saved);
            throw e;
        }
    }

    private void renderPage(HttpServletRequest request, HttpServletResponse response, long userId, String error)
            throws ServletException, IOException {
        request.setAttribute("csrfToken", Csrf.ensureToken(request));
        request.setAttribute("eligibleItems", ReturnRefundDao.listEligibleItems(userId));
        request.setAttribute("returnRefundRequests", ReturnRefundDao.listForUser(userId));
        if (error != null && !error.isBlank()) request.setAttribute("error", error);
        long id = positiveLong(request.getParameter("id"), 0);
        if (id > 0) {
            Map<String, Object> detail = ReturnRefundDao.detailForUser(id, userId);
            if (detail == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("selectedRequest", detail);
            request.setAttribute("requestEvents", ReturnRefundDao.eventsForUser(id, userId));
            request.setAttribute("requestEvidence", ReturnRefundDao.evidenceListForUser(id, userId));
        }
        request.getRequestDispatcher("/WEB-INF/views/user/return-refunds.jsp").forward(request, response);
    }

    private void streamEvidence(HttpServletRequest request, HttpServletResponse response, long userId)
            throws IOException {
        long evidenceId = positiveLong(request.getParameter("evidenceId"), 0);
        Map<String, Object> evidence = evidenceId > 0
                ? ReturnRefundDao.evidenceForUser(evidenceId, userId) : null;
        if (evidence == null) { response.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
        Path file;
        try {
            file = ReturnRefundEvidenceStore.resolve(String.valueOf(evidence.get("storageKey")), getServletContext());
        } catch (IllegalArgumentException | IllegalStateException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(file)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String type = String.valueOf(evidence.get("contentType"));
        if (!"image/jpeg".equals(type) && !"image/png".equals(type)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType(type);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("Content-Disposition", "inline; filename=\"return-evidence\"");
        response.setContentLengthLong(Files.size(file));
        try (OutputStream out = response.getOutputStream()) { Files.copy(file, out); }
    }

    private List<SavedEvidence> saveUploads(HttpServletRequest request) throws IOException, ServletException {
        List<SavedEvidence> saved = new ArrayList<>();
        try {
            for (Part part : request.getParts()) {
                if (!"evidence".equals(part.getName()) || part.getSize() <= 0) continue;
                if (saved.size() >= ReturnRefundEvidenceStore.MAX_INITIAL_FILES) {
                    throw new IllegalArgumentException("Mỗi lần chỉ được tải tối đa 4 ảnh.");
                }
                saved.add(ReturnRefundEvidenceStore.save(part, getServletContext()));
            }
            return saved;
        } catch (IOException | ServletException | RuntimeException e) {
            deleteFiles(saved);
            throw e;
        }
    }

    private static List<ReturnRefundDao.EvidenceUpload> uploadRecords(List<SavedEvidence> saved) {
        return saved.stream().map(SavedEvidence::record).toList();
    }

    private static void deleteFiles(List<SavedEvidence> saved) {
        for (SavedEvidence file : saved) ReturnRefundEvidenceStore.delete(file);
    }

    private static String trim(String value) { return value == null ? "" : value.trim(); }

    private static long positiveLong(String raw, long fallback) {
        try {
            long value = Long.parseLong(raw);
            return value > 0 ? value : fallback;
        } catch (RuntimeException e) { return fallback; }
    }

    private static int integer(String raw, int fallback) {
        try { return Integer.parseInt(raw); }
        catch (RuntimeException e) { return fallback; }
    }
}
