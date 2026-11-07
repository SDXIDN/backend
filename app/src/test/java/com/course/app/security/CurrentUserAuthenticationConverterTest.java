package com.course.app.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CurrentUserAuthenticationConverterTest {

    private final CurrentUserAuthenticationConverter converter = new CurrentUserAuthenticationConverter();

    @Test
    void convert_jwtWithRealmRoles_buildsPrincipalAndRolePrefixedAuthorities() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        CurrentUser principal = assertInstanceOf(CurrentUser.class, token.getPrincipal());
        assertEquals("alice", principal.username());
        assertEquals(List.of("USER", "ADMIN"), principal.roles());
        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void convert_jwtWithoutRealmAccess_returnsNoRoles() {
        Jwt jwt = jwtBuilder().build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        CurrentUser principal = (CurrentUser) token.getPrincipal();
        assertThat(principal.roles()).isEmpty();
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void convert_realmAccessWithoutRolesKey_returnsNoRoles() {
        Jwt jwt = jwtBuilder().claim("realm_access", Map.of("other", "value")).build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(((CurrentUser) token.getPrincipal()).roles()).isEmpty();
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("preferred_username", "alice");
    }
}
