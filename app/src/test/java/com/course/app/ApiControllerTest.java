package com.course.app;

import com.course.app.security.CurrentUser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ApiControllerTest {

    private final ApiController controller = new ApiController();

    @Test
    void publicPing_anyCaller_returnsPong() {
        assertEquals("pong (no auth required)", controller.publicPing());
    }

    @Test
    void me_authenticatedUser_returnsSamePrincipal() {
        CurrentUser user = new CurrentUser("alice", List.of("USER"));

        assertSame(user, controller.me(user));
    }

    @Test
    void me_noPrincipal_returnsNull() {
        assertNull(controller.me(null));
    }

    @Test
    void reports_called_returnsAdminReportText() {
        assertEquals("Sensitive report data - ADMIN only", controller.reports());
    }
}
