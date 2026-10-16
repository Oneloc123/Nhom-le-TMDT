package vn.edu.hcmuaf.fit.tmdt_nhomle.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MailService {

    private static final Map<String, String> emailBox = new ConcurrentHashMap<>();

    private MailService() {
    }

    public static String sendVerificationCode(String email, String fullName) {
        String code = String.format("%06d", (int) (Math.random() * 1_000_000));
        String body = "Xin chao " + fullName + ",\n"
                + "Ma xac thuc tai khoan PrintKraft 3D cua ban la: " + code + "\n"
                + "Ma co hieu luc trong 15 phut.";
        emailBox.put(email, body);
        System.out.println("[MAIL -> " + email + "] " + body.replace('\n', ' '));
        return code;
    }

    public static String sendPasswordReset(String email, String fullName) {
        String otp = String.format("%06d", (int) (Math.random() * 1_000_000));
        String body = "Xin chao " + fullName + ",\n"
                + "Ban da yeu cau khoi phuc mat khau PrintKraft 3D. OTP: " + otp + "\n"
                + "Neu ban khong yeu cau, hay bo qua email nay.";
        emailBox.put(email, body);
        System.out.println("[MAIL -> " + email + "] " + body.replace('\n', ' '));
        return otp;
    }

    public static String peek(String email) {
        return emailBox.get(email);
    }
}
