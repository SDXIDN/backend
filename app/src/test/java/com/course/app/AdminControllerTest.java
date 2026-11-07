package com.course.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminControllerTest {

    private final AdminController controller = new AdminController();

    @Test
    void reports_called_returnsUserLevelText() {
        assertEquals("Sensitive report data - USER only", controller.reports());
    }

    @Test
    void moreReports_called_returnsAdminLevelText() {
        assertEquals("Extremely sensitive report data - ADMIN only", controller.moreReports());
    }
}
