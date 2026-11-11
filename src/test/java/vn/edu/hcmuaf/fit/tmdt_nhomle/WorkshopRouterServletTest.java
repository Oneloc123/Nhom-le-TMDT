package vn.edu.hcmuaf.fit.tmdt_nhomle;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopRouterServletTest {
    @Test
    void exposesMessagingAndChangeRequestPages() throws Exception {
        Field pagesField = WorkshopRouterServlet.class.getDeclaredField("PAGES");
        pagesField.setAccessible(true);

        @SuppressWarnings("unchecked")
        Set<String> pages = (Set<String>) pagesField.get(null);

        assertTrue(pages.contains("/messages"));
        assertTrue(pages.contains("/change-requests"));
    }
}
