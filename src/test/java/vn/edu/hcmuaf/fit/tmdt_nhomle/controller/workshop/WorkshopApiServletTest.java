package vn.edu.hcmuaf.fit.tmdt_nhomle.controller.workshop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkshopApiServletTest {
    @Test
    void treatsAllAndBlankStatusAsNoFilter() {
        assertNull(WorkshopApiServlet.normalizeStatusFilter(null));
        assertNull(WorkshopApiServlet.normalizeStatusFilter(""));
        assertNull(WorkshopApiServlet.normalizeStatusFilter("   "));
        assertNull(WorkshopApiServlet.normalizeStatusFilter("ALL"));
        assertNull(WorkshopApiServlet.normalizeStatusFilter(" all "));
    }

    @Test
    void normalizesSpecificStatusValues() {
        assertEquals("PENDING", WorkshopApiServlet.normalizeStatusFilter(" pending "));
        assertEquals("ACCEPTED", WorkshopApiServlet.normalizeStatusFilter("Accepted"));
        assertEquals("REJECTED", WorkshopApiServlet.normalizeStatusFilter("rejected"));
    }
}
