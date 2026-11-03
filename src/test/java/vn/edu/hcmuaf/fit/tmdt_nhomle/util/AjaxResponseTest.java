package vn.edu.hcmuaf.fit.tmdt_nhomle.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AjaxResponseTest {
    @Test
    void quoteProducesValidJsonStringEscapes() {
        assertEquals("\"Đã thêm \\\"áo\\\"\\nOK\"",
                AjaxResponse.quote("Đã thêm \"áo\"\nOK"));
        assertEquals("null", AjaxResponse.quote(null));
    }
}
