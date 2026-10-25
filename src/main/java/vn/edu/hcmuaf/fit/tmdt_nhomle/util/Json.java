package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

/** Small JSON writer for Admin API payloads; no values are concatenated without escaping. */
public final class Json {
    private Json() { }

    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    private static void append(StringBuilder out, Object value) {
        if (value == null) { out.append("null"); return; }
        if (value instanceof String || value instanceof Character || value instanceof Enum<?>) {
            string(out, value.toString()); return;
        }
        if (value instanceof Number number) {
            if (number instanceof Double d && !Double.isFinite(d)
                    || number instanceof Float f && !Float.isFinite(f)) out.append("null");
            else out.append(number);
            return;
        }
        if (value instanceof Boolean) { out.append(value); return; }
        if (value instanceof Map<?, ?> map) {
            out.append('{');
            Iterator<? extends Map.Entry<?, ?>> iterator = map.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<?, ?> entry = iterator.next();
                string(out, String.valueOf(entry.getKey())); out.append(':'); append(out, entry.getValue());
                if (iterator.hasNext()) out.append(',');
            }
            out.append('}'); return;
        }
        if (value instanceof Iterable<?> iterable) {
            out.append('['); Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) { append(out, iterator.next()); if (iterator.hasNext()) out.append(','); }
            out.append(']'); return;
        }
        if (value.getClass().isArray()) {
            out.append('[');
            for (int i = 0; i < Array.getLength(value); i++) {
                if (i > 0) out.append(','); append(out, Array.get(value, i));
            }
            out.append(']'); return;
        }
        string(out, value.toString());
    }

    private static void string(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '<' -> out.append("\\u003c");
                case '>' -> out.append("\\u003e");
                case '&' -> out.append("\\u0026");
                case '\u2028' -> out.append("\\u2028");
                case '\u2029' -> out.append("\\u2029");
                default -> {
                    if (c < 0x20) out.append("\\u%04x".formatted((int) c));
                    else out.append(c);
                }
            }
        }
        out.append('"');
    }
}
