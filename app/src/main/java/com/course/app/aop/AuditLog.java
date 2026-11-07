package com.course.app.aop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
public class AuditLog {

    static final int MAX_ENTRIES = 1000;
    private static final Logger LOG = LoggerFactory.getLogger("AUDIT");

    private final ConcurrentLinkedDeque<AuditEntry> entries = new ConcurrentLinkedDeque<>();

    public void record(AuditEntry entry) {
        entries.addLast(entry);
        while (entries.size() > MAX_ENTRIES) {
            entries.pollFirst();
        }
        LOG.info("action={} actor={} target={} outcome={} details={}",
                entry.action(), entry.actor(), entry.target(), entry.outcome(), entry.details());
    }

    public List<AuditEntry> entries() {
        return new ArrayList<>(entries);
    }
}
