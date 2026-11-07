package com.course.app.aop;

import java.time.Instant;

public record AuditEntry(Instant at, String actor, String action, String target,
                         String outcome, String details) {
}
