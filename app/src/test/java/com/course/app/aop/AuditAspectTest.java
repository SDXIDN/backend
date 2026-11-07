package com.course.app.aop;

import com.course.app.security.CurrentUser;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuditAspectTest {

    static class Target {
        @Audited(action = "UPDATE_BOOKING")
        void update() {
        }
    }

    private final AuditLog auditLog = new AuditLog();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
    private final AuditAspect aspect = new AuditAspect(auditLog, clock);
    private Audited annotation;
    private ProceedingJoinPoint pjp;

    @BeforeEach
    void setUp() throws Exception {
        annotation = Target.class.getDeclaredMethod("update").getAnnotation(Audited.class);
        pjp = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn("BookingService.update(..)");
        when(pjp.getSignature()).thenReturn(signature);
        when(pjp.getArgs()).thenReturn(new Object[]{7L, "secret@email.com"});
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new CurrentUser("admin", List.of("ADMIN")), null, List.of()));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void audit_success_proceedsReturnsResultAndRecordsSuccess() throws Throwable {
        Object methodResult = new Object();
        when(pjp.proceed()).thenReturn(methodResult);

        Object result = aspect.audit(pjp, annotation);

        verify(pjp, times(1)).proceed();
        assertSame(methodResult, result);
        assertEquals(1, auditLog.entries().size());
        AuditEntry e = auditLog.entries().get(0);
        assertEquals("UPDATE_BOOKING", e.action());
        assertEquals("admin", e.actor());
        assertEquals("BookingService.update(..)", e.target());
        assertEquals("SUCCESS", e.outcome());
        assertEquals("ids=[7]", e.details());
        assertEquals(Instant.parse("2026-01-01T10:00:00Z"), e.at());
    }

    @Test
    void audit_methodThrows_recordsFailureAndRethrowsSameException() throws Throwable {
        IllegalStateException boom = new IllegalStateException("not found");
        when(pjp.proceed()).thenThrow(boom);

        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> aspect.audit(pjp, annotation));

        assertSame(boom, thrown);
        AuditEntry e = auditLog.entries().get(0);
        assertEquals("FAILURE", e.outcome());
        assertTrue(e.details().contains("IllegalStateException: not found"));
    }

    @Test
    void audit_nonNumericArguments_areNotWrittenToLog() throws Throwable {
        when(pjp.proceed()).thenReturn(null);

        aspect.audit(pjp, annotation);

        assertTrue(!auditLog.entries().get(0).details().contains("secret@email.com"));
    }

    @Test
    void audit_noAuthentication_actorIsAnonymous() throws Throwable {
        SecurityContextHolder.clearContext();
        when(pjp.proceed()).thenReturn(null);

        aspect.audit(pjp, annotation);

        assertEquals("anonymous", auditLog.entries().get(0).actor());
    }

    @Test
    void auditLog_keepsOnlyLastEntries() {
        for (int i = 0; i < AuditLog.MAX_ENTRIES + 5; i++) {
            auditLog.record(new AuditEntry(Instant.EPOCH, "u", "A", "t", "SUCCESS", "n=" + i));
        }

        assertEquals(AuditLog.MAX_ENTRIES, auditLog.entries().size());
        assertEquals("n=5", auditLog.entries().get(0).details());
    }
}
