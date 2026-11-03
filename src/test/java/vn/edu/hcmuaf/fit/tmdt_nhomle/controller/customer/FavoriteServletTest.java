package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.customer;

import org.junit.jupiter.api.Test;
import vn.edu.hcmuaf.fit.tmdt_nhomle.util.Redirects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FavoriteServletTest {

    @Test
    void acceptsOnlyLocalReturnPaths() {
        assertEquals("/product-detail?id=12",
                Redirects.safeLocalPath("/product-detail?id=12"));
        assertNull(Redirects.safeLocalPath("https://evil.example"));
        assertNull(Redirects.safeLocalPath("//evil.example/path"));
        assertNull(Redirects.safeLocalPath("/safe\r\nLocation: https://evil.example"));
    }
}
