package com.course.app.web;

import com.course.app.ApiController;
import com.course.app.security.CurrentUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentUsernameArgumentResolverTest {

    private final CurrentUsernameArgumentResolver resolver = new CurrentUsernameArgumentResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String username) {
        CurrentUser principal = new CurrentUser(username, List.of("USER"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private static MethodParameter param(String method, Class<?> type) throws Exception {
        return new MethodParameter(ApiController.class.getMethod(method, type), 0);
    }

    @Test
    void supportsParameter_annotatedStringParameter_returnsTrue() throws Exception {
        assertTrue(resolver.supportsParameter(param("username", String.class)));
    }

    @Test
    void supportsParameter_parameterWithoutAnnotation_returnsFalse() throws Exception {
        assertFalse(resolver.supportsParameter(param("me", CurrentUser.class)));
    }

    @Test
    void resolveArgument_authenticatedUser_returnsUsernameAsString() throws Exception {
        authenticateAs("alice");

        Object result = resolver.resolveArgument(param("username", String.class), null,
                new ServletWebRequest(new MockHttpServletRequest()), null);

        assertEquals("alice", result);
    }

    @Test
    void resolveArgument_noAuthentication_returnsNull() throws Exception {
        Object result = resolver.resolveArgument(param("username", String.class), null,
                new ServletWebRequest(new MockHttpServletRequest()), null);

        assertNull(result);
    }

    @Test
    void request_secureUsername_resolvedValueIsUsedInResponseBody() throws Exception {
        authenticateAs("bob");
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ApiController())
                .setCustomArgumentResolvers(resolver)
                .build();

        mvc.perform(get("/api/secure/username"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, bob"));
    }
}
