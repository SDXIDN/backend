package com.course.app.aop;

import com.course.app.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentActor {

    private CurrentActor() {
    }

    public static String name() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return "anonymous";
        }
        if (auth.getPrincipal() instanceof CurrentUser user && user.username() != null) {
            return user.username();
        }
        return auth.isAuthenticated() && auth.getName() != null ? auth.getName() : "anonymous";
    }
}
