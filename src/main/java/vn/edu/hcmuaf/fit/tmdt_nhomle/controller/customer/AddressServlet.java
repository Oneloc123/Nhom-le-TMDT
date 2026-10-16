package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.edu.hcmuaf.fit.tmdt_nhomle.dao.AddressDao;
import vn.edu.hcmuaf.fit.tmdt_nhomle.db.DataAccessException;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.Address;
import vn.edu.hcmuaf.fit.tmdt_nhomle.model.User;

import java.io.IOException;

@WebServlet(name = "addressServlet", value = "/addresses")
public class AddressServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = current(request);
        request.setAttribute("addresses", AddressDao.findByUser(user.getId()));
        request.getRequestDispatcher("/customer/addresses.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        User user = current(request);
        request.setCharacterEncoding("UTF-8");
        String action = nvl(request.getParameter("action"), "add");

        try {
            switch (action) {
                case "add" -> add(request, response, user);
                case "update" -> update(request, response, user);
                case "delete" -> delete(request, response, user);
                case "setDefault" -> setDefault(request, response, user);
                default -> response.sendRedirect(request.getContextPath() + "/addresses");
            }
        } catch (DataAccessException e) {
            request.setAttribute("error", e.getMessage());
            request.setAttribute("addresses", AddressDao.findByUser(user.getId()));
            request.getRequestDispatcher("/customer/addresses.jsp").forward(request, response);
        }
    }

    private void add(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        Address address = readForm(request, user.getId());
        String error = validate(address);
        if (error != null) {
            redirectWithError(request, response, error);
            return;
        }
        boolean makeDefault = "on".equals(request.getParameter("defaultAddress"))
                || "true".equals(request.getParameter("defaultAddress"));
        AddressDao.insert(address, makeDefault);
        response.sendRedirect(request.getContextPath() + "/addresses?saved=1");
    }

    private void update(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        long addressId = parseLong(request.getParameter("addressId"), -1);
        Address existing = AddressDao.findByIdForUser(addressId, user.getId()).orElse(null);
        if (existing == null) {
            redirectWithError(request, response, "Địa chỉ không tồn tại hoặc bạn không có quyền sửa.");
            return;
        }
        Address address = readForm(request, user.getId());
        address.setAddressId(addressId);
        String error = validate(address);
        if (error != null) {
            redirectWithError(request, response, error);
            return;
        }
        boolean makeDefault = "on".equals(request.getParameter("defaultAddress"))
                || "true".equals(request.getParameter("defaultAddress"));
        AddressDao.update(address, makeDefault);
        response.sendRedirect(request.getContextPath() + "/addresses?saved=1");
    }

    private void delete(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        long addressId = parseLong(request.getParameter("addressId"), -1);
        boolean deleted = AddressDao.delete(addressId, user.getId());
        response.sendRedirect(request.getContextPath() + "/addresses"
                + (deleted ? "?deleted=1" : "?error=" + urlEncode("Địa chỉ không tồn tại.")));
    }

    private void setDefault(HttpServletRequest request, HttpServletResponse response, User user)
            throws IOException {
        long addressId = parseLong(request.getParameter("addressId"), -1);
        AddressDao.setDefault(addressId, user.getId());
        response.sendRedirect(request.getContextPath() + "/addresses?saved=1");
    }

    private Address readForm(HttpServletRequest request, long userId) {
        Address address = new Address();
        address.setUserId(userId);
        address.setRecipientName(nvl(request.getParameter("recipientName")).trim());
        address.setRecipientPhone(nvl(request.getParameter("recipientPhone")).trim());
        address.setProvince(nvl(request.getParameter("province")).trim());
        address.setDistrict(nvl(request.getParameter("district")).trim());
        address.setAddressLine(nvl(request.getParameter("addressLine")).trim());
        return address;
    }

    private String validate(Address a) {
        if (a.getRecipientName().isEmpty()) {
            return "Tên người nhận không được để trống.";
        }
        if (a.getRecipientPhone().isEmpty() || !a.getRecipientPhone().matches("\\d{9,12}")) {
            return "Số điện thoại không hợp lệ (9–12 chữ số).";
        }
        if (a.getProvince().isEmpty() || a.getDistrict().isEmpty() || a.getAddressLine().isEmpty()) {
            return "Vui lòng nhập đủ tỉnh/thành, quận/huyện và địa chỉ.";
        }
        return null;
    }

    private void redirectWithError(HttpServletRequest request, HttpServletResponse response,
                                  String message) throws IOException {
        response.sendRedirect(request.getContextPath() + "/addresses?error=" + urlEncode(message));
    }

    private User current(HttpServletRequest request) {
        User user = vn.edu.hcmuaf.fit.tmdt_nhomle.security.AuthFilter.currentUser(request);
        if (user == null) {
            throw new IllegalStateException("Chưa đăng nhập.");
        }
        return user;
    }

    private static String nvl(String s, String def) {
        return s == null ? def : s;
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static long parseLong(String s, long fallback) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String urlEncode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
