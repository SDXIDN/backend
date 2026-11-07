package com.course.app.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
public class AuditAspect {

    private final AuditLog auditLog;
    private final Clock clock;

    public AuditAspect(AuditLog auditLog) {
        this(auditLog, Clock.systemUTC());
    }

    AuditAspect(AuditLog auditLog, Clock clock) {
        this.auditLog = auditLog;
        this.clock = clock;
    }

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        String target = pjp.getSignature().toShortString();
        String ids = Arrays.stream(pjp.getArgs())
                .filter(Number.class::isInstance)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        try {
            Object result = pjp.proceed();
            record(audited, target, "SUCCESS", "ids=[" + ids + "]");
            return result;
        } catch (Throwable ex) {
            record(audited, target, "FAILURE",
                    "ids=[" + ids + "] error=" + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw ex;
        }
    }

    private void record(Audited audited, String target, String outcome, String details) {
        auditLog.record(new AuditEntry(clock.instant(), CurrentActor.name(),
                audited.action(), target, outcome, details));
    }
}
