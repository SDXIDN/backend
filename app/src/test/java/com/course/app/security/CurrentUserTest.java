package com.course.app.security;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUserTest {

    @Test
    void hasRole_roleIsPresent_returnsTrue() {
        CurrentUser user = new CurrentUser("alice", List.of("USER", "ADMIN"));

        assertTrue(user.hasRole("ADMIN"));
    }

    @Test
    void hasRole_roleIsAbsent_returnsFalse() {
        CurrentUser user = new CurrentUser("bob", List.of("USER"));

        assertFalse(user.hasRole("ADMIN"));
    }

    @Test
    void hasRole_emptyRoles_returnsFalse() {
        CurrentUser user = new CurrentUser("nobody", List.of());

        assertFalse(user.hasRole("USER"));
    }
}
