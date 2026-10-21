package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

/**
 * Kiểm tra cú pháp JSON phía server (recursive descent, không dùng thư viện ngoài)
 * cho cấu hình thiết kế custom. Mục đích: chặn dữ liệu rác/phục vụ inject trước khi
 * ghi vào cột JSON, đồng thời đặt giới hạn kích thước/độ sâu.
 *
 * <p>Không parse giá trị nghiệp vụ — chỉ xác nhận đây là JSON object hợp lệ.</p>
 */
public final class JsonValidator {

    /** Kích thước tối đa của chuỗi cấu hình (ký tự). */
    public static final int MAX_LENGTH = 8_000;
    /** Độ sâu lồng nhau tối đa. */
    public static final int MAX_DEPTH = 8;

    private final String input;
    private int pos;
    private int depth;
    private boolean failed;

    private JsonValidator(String input) {
        this.input = input;
    }

    /**
     * @return {@code null} nếu hợp lệ, ngược lại là thông báo lỗi (tiếng Việt).
     */
    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null; // optional — không bắt buộc có cấu hình
        }
        if (raw.length() > MAX_LENGTH) {
            return "Cấu hình JSON tối đa " + MAX_LENGTH + " ký tự.";
        }
        JsonValidator validator = new JsonValidator(raw.trim());
        if (!validator.parseObject()) {
            return "Cấu hình JSON không hợp lệ.";
        }
        validator.skipWhitespace();
        if (validator.pos < validator.input.length()) {
            return "Cấu hình JSON có dữ liệu thừa sau dấu }.";
        }
        return null;
    }

    /** Convenience: true nếu rỗng hoặc là JSON object hợp lệ. */
    public static boolean isValid(String raw) {
        return validate(raw) == null;
    }

    // ------------------------------------------------------------------
    private boolean parseObject() {
        skipWhitespace();
        if (!expect('{')) return false;
        depth++;
        if (depth > MAX_DEPTH) { failed = true; return false; }
        skipWhitespace();
        if (peek() == '}') { pos++; depth--; return true; }
        while (true) {
            skipWhitespace();
            if (!parseString()) return false;
            skipWhitespace();
            if (!expect(':')) return false;
            if (!parseValue()) return false;
            skipWhitespace();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == '}') { pos++; depth--; return true; }
            return false;
        }
    }

    private boolean parseValue() {
        skipWhitespace();
        char c = peek();
        switch (c) {
            case '{' -> { return parseObject(); }
            case '[' -> { return parseArray(); }
            case '"' -> { return parseString(); }
            case 't' -> { return literal("true"); }
            case 'f' -> { return literal("false"); }
            case 'n' -> { return literal("null"); }
            default -> { return parseNumber(); }
        }
    }

    private boolean parseArray() {
        skipWhitespace();
        if (!expect('[')) return false;
        depth++;
        if (depth > MAX_DEPTH) { failed = true; return false; }
        skipWhitespace();
        if (peek() == ']') { pos++; depth--; return true; }
        while (true) {
            if (!parseValue()) return false;
            skipWhitespace();
            char c = peek();
            if (c == ',') { pos++; continue; }
            if (c == ']') { pos++; depth--; return true; }
            return false;
        }
    }

    private boolean parseString() {
        skipWhitespace();
        if (!expect('"')) return false;
        while (pos < input.length()) {
            char c = input.charAt(pos++);
            if (c == '"') return true;
            if (c == '\\') {
                if (pos >= input.length()) return false;
                char escape = input.charAt(pos++);
                if (escape == 'u') {
                    if (pos + 4 > input.length()) return false;
                    for (int i = 0; i < 4; i++) {
                        char hex = input.charAt(pos++);
                        boolean hexDigit = (hex >= '0' && hex <= '9')
                                || (hex >= 'a' && hex <= 'f') || (hex >= 'A' && hex <= 'F');
                        if (!hexDigit) return false;
                    }
                } else if ("\"\\/bfnrt".indexOf(escape) < 0) {
                    return false;
                }
            } else if (c < 0x20) {
                return false;
            }
        }
        return false;
    }

    private boolean parseNumber() {
        skipWhitespace();
        int start = pos;
        if (peek() == '-') pos++;
        if (!digits()) return false;
        if (peek() == '.') { pos++; if (!digits()) return false; }
        char sign = peek();
        if (sign == 'e' || sign == 'E') {
            pos++;
            char plus = peek();
            if (plus == '+' || plus == '-') pos++;
            if (!digits()) return false;
        }
        return pos > start;
    }

    private boolean literal(String word) {
        if (input.regionMatches(pos, word, 0, word.length())) {
            pos += word.length();
            return true;
        }
        return false;
    }

    private boolean digits() {
        int start = pos;
        while (pos < input.length() && Character.isDigit(input.charAt(pos))) pos++;
        return pos > start;
    }

    private boolean expect(char expected) {
        if (pos < input.length() && input.charAt(pos) == expected) {
            pos++;
            return true;
        }
        return false;
    }

    private char peek() {
        return pos < input.length() ? input.charAt(pos) : '\0';
    }

    private void skipWhitespace() {
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') pos++;
            else break;
        }
    }
}
