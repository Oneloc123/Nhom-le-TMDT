package vn.edu.hcmuaf.fit.tmdt_nhomle.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthFilterCheckoutTest {

    @Test
    void everyCheckoutMutationRequiresCsrf() throws Exception {
        Method requiresCsrf = AuthFilter.class.getDeclaredMethod("requiresCsrf", String.class);
        requiresCsrf.setAccessible(true);

        assertEquals(true, requiresCsrf.invoke(null, "/cart"));
        assertEquals(true, requiresCsrf.invoke(null, "/order"));
        assertEquals(true, requiresCsrf.invoke(null, "/payment"));
        assertEquals(true, requiresCsrf.invoke(null, "/order-detail"));
    }
}
