package vn.edu.hcmuaf.fit.tmdt_nhomle.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthFilterWorkshopSecurityTest {
    @Test
    void workshopPagesAndApiRequireCsrfOnPost() throws Exception {
        assertTrue(invokeBoolean("requiresCsrf", "/workshop/messages"));
        assertTrue(invokeBoolean("requiresCsrf", "/workshop/change-requests"));
        assertTrue(invokeBoolean("requiresCsrf", "/workshop/api/messages/42"));
        assertTrue(invokeBoolean("requiresCsrf", "/workshop/api/change-requests/7/respond"));
        assertFalse(invokeBoolean("requiresCsrf", "/public/catalog"));
    }

    @Test
    void workshopApiPathsAreRecognizedByTheAccessFilter() throws Exception {
        assertTrue(invokeBoolean("isWorkshopPath", "/workshop/dashboard"));
        assertTrue(invokeBoolean("isWorkshopPath", "/workshop/api/messages/42"));
        assertFalse(invokeBoolean("isWorkshopPath", "/workshop-other"));
    }

    private static boolean invokeBoolean(String methodName, String path) throws Exception {
        Method method = AuthFilter.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, path);
    }
}
