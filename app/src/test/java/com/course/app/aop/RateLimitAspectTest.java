package com.course.app.aop;

import com.course.app.error.TooManyRequestsException;
import com.course.app.security.CurrentUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitAspectTest {

    static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T10:00:00Z");

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    static class Target {
        @RateLimited(maxCalls = 2, perSeconds = 60)
        void limited() {
        }
    }

    private final MutableClock clock = new MutableClock();
    private final RateLimitAspect aspect = new RateLimitAspect(clock);
    private RateLimited annotation;
    private ProceedingJoinPoint pjp;

    @BeforeEach
    void setUp() throws Throwable {
        annotation = Target.class.getDeclaredMethod("limited").getAnnotation(RateLimited.class);
        pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("BookingService.create(..)");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.proceed()).thenReturn("result");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser(username, List.of("USER")), null, List.of()));
    }

    @Test
    void limit_withinLimit_proceedsAndReturnsMethodResult() throws Throwable {
        assertEquals("result", aspect.limit(pjp, annotation));
        assertEquals("result", aspect.limit(pjp, annotation));

        verify(pjp, times(2)).proceed();
    }

    @Test
    void limit_overLimit_throwsAndDoesNotProceed() throws Throwable {
        aspect.limit(pjp, annotation);
        aspect.limit(pjp, annotation);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> aspect.limit(pjp, annotation));

        verify(pjp, times(2)).proceed();
        assertTrue(ex.getMessage().contains("max 2 calls per 60 s"));
        assertTrue(ex.getMessage().contains("BookingService.create(..)"));
    }

    @Test
    void limit_afterWindowPasses_allowsAgain() throws Throwable {
        aspect.limit(pjp, annotation);
        aspect.limit(pjp, annotation);
        assertThrows(TooManyRequestsException.class, () -> aspect.limit(pjp, annotation));

        clock.advance(Duration.ofSeconds(61));

        assertEquals("result", aspect.limit(pjp, annotation));
        verify(pjp, times(3)).proceed();
    }

    @Test
    void limit_retryAfterInMessageReflectsRemainingWindow() throws Throwable {
        aspect.limit(pjp, annotation);
        clock.advance(Duration.ofSeconds(10));
        aspect.limit(pjp, annotation);

        TooManyRequestsException ex = assertThrows(TooManyRequestsException.class,
                () -> aspect.limit(pjp, annotation));

        assertTrue(ex.getMessage().contains("retry in 50 s"), ex.getMessage());
    }

    @Test
    void limit_differentUsers_haveIndependentLimits() throws Throwable {
        authenticateAs("alice");
        aspect.limit(pjp, annotation);
        aspect.limit(pjp, annotation);
        assertThrows(TooManyRequestsException.class, () -> aspect.limit(pjp, annotation));

        authenticateAs("bob");

        assertEquals("result", aspect.limit(pjp, annotation));
    }

    @Test
    void limit_methodThrows_exceptionPropagatesUnchanged() throws Throwable {
        IllegalStateException boom = new IllegalStateException("boom");
        when(pjp.proceed()).thenThrow(boom);

        assertEquals(boom, assertThrows(IllegalStateException.class, () -> aspect.limit(pjp, annotation)));
    }

    @Test
    void limit_overLimit_neverTouchesProceed() throws Throwable {
        RateLimited zero = ZeroLimit.class.getDeclaredMethod("m").getAnnotation(RateLimited.class);

        assertThrows(TooManyRequestsException.class, () -> aspect.limit(pjp, zero));

        verify(pjp, never()).proceed();
    }

    static class ZeroLimit {
        @RateLimited(maxCalls = 0, perSeconds = 60)
        void m() {
        }
    }
}
