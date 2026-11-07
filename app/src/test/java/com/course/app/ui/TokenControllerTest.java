package com.course.app.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenControllerTest {

    private final TokenController controller = new TokenController();

    @Mock
    private OAuth2AuthorizedClient client;

    @Test
    void token_clientHasAccessToken_returnsItsValue() {
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "abc.def.ghi",
                Instant.parse("2026-11-07T10:00:00Z"), Instant.parse("2026-11-07T11:00:00Z"));
        when(client.getAccessToken()).thenReturn(token);

        Map<String, String> result = controller.token(client);

        assertEquals(Map.of("access_token", "abc.def.ghi"), result);
    }

    @Test
    void token_clientWithoutAccessToken_throwsNullPointerException() {
        when(client.getAccessToken()).thenReturn(null);

        assertThrows(NullPointerException.class, () -> controller.token(client));
    }
}
