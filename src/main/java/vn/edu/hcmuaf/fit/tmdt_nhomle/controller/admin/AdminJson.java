package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.admin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small JSON codec for the admin API, avoiding a new runtime dependency. */
public final class AdminJson {
    private AdminJson() { }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    public static Map<String, Object> parseObject(String json) {
        Object parsed = new Parser(json).parse();
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("JSON body must be an object.");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String || value instanceof Character) {
            quote(String.valueOf(value), out);
        } else if (value instanceof Number || value instanceof Boolean) {
            out.append(value);
        } else if (value instanceof Map<?, ?> map) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) out.append(',');
                quote(String.valueOf(entry.getKey()), out);
                out.append(':');
                write(entry.getValue(), out);
                first = false;
            }
            out.append('}');
        } else if (value instanceof Iterable<?> values) {
            out.append('[');
            boolean first = true;
            for (Object item : values) {
                if (!first) out.append(',');
                write(item, out);
                first = false;
            }
            out.append(']');
        } else {
            quote(String.valueOf(value), out);
        }
    }

    private static void quote(String value, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) out.append(String.format("\\u%04x", (int) ch));
                    else out.append(ch);
                }
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String source;
        private int position;

        private Parser(String source) {
            this.source = source == null ? "" : source;
        }

        private Object parse() {
            Object value = value();
            whitespace();
            if (position != source.length()) fail("Unexpected content after JSON value.");
            return value;
        }

        private Object value() {
            whitespace();
            if (position >= source.length()) return fail("JSON body is empty.");
            return switch (source.charAt(position)) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> number();
            };
        }

        private Map<String, Object> object() {
            position++;
            Map<String, Object> result = new LinkedHashMap<>();
            whitespace();
            if (take('}')) return result;
            do {
                whitespace();
                if (position >= source.length() || source.charAt(position) != '"') fail("Expected an object key.");
                String key = string();
                whitespace();
                expect(':');
                result.put(key, value());
                whitespace();
                if (take('}')) return result;
                expect(',');
            } while (true);
        }

        private List<Object> array() {
            position++;
            List<Object> result = new ArrayList<>();
            whitespace();
            if (take(']')) return result;
            do {
                result.add(value());
                whitespace();
                if (take(']')) return result;
                expect(',');
            } while (true);
        }

        private String string() {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (position < source.length()) {
                char ch = source.charAt(position++);
                if (ch == '"') return result.toString();
                if (ch != '\\') {
                    if (ch < 0x20) fail("Unescaped control character.");
                    result.append(ch);
                    continue;
                }
                if (position >= source.length()) fail("Incomplete escape sequence.");
                char escaped = source.charAt(position++);
                switch (escaped) {
                    case '"', '\\', '/' -> result.append(escaped);
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> {
                        if (position + 4 > source.length()) fail("Incomplete unicode escape.");
                        try {
                            result.append((char) Integer.parseInt(source.substring(position, position + 4), 16));
                        } catch (NumberFormatException e) {
                            fail("Invalid unicode escape.");
                        }
                        position += 4;
                    }
                    default -> fail("Invalid escape sequence.");
                }
            }
            return fail("Unterminated string.");
        }

        private Object number() {
            int start = position;
            if (take('-')) { /* sign consumed */ }
            while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
            if (take('.')) while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
            if (take('e') || take('E')) {
                if (!take('+')) take('-');
                while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
            }
            if (start == position) return fail("Invalid JSON value.");
            try {
                return new BigDecimal(source.substring(start, position));
            } catch (NumberFormatException e) {
                return fail("Invalid number.");
            }
        }

        private Object literal(String token, Object value) {
            if (!source.startsWith(token, position)) return fail("Invalid JSON value.");
            position += token.length();
            return value;
        }

        private void whitespace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++;
        }

        private boolean take(char expected) {
            if (position < source.length() && source.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            if (!take(expected)) fail("Expected '" + expected + "'.");
        }

        private <T> T fail(String message) {
            throw new IllegalArgumentException(message + " At character " + position + ".");
        }
    }
}
