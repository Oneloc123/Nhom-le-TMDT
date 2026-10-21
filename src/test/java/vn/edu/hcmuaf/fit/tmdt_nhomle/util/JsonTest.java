package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {
    @Test
    void escapesQuotesControlsAndScriptMarkup() {
        assertEquals("{\"name\":\"\\u003c/script\\u003e\\u0026\\\"\\n\"}",
                Json.stringify(Map.of("name", "</script>&\"\n")));
    }
}
