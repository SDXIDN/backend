package com.course.app.aop;

import com.course.app.error.TooManyRequestsException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class RateLimitAspect {

    private final Clock clock;
    private final Map<String, Deque<Instant>> calls = new ConcurrentHashMap<>();

    public RateLimitAspect() {
        this(Clock.systemUTC());
    }

    RateLimitAspect(Clock clock) {
        this.clock = clock;
    }

    @Around("@annotation(rateLimited)")
    public Object limit(ProceedingJoinPoint pjp, RateLimited rateLimited) throws Throwable {
        String method = pjp.getSignature().toShortString();
        String key = method + "|" + CurrentActor.name();
        Instant now = clock.instant();
        Instant windowStart = now.minusSeconds(rateLimited.perSeconds());

        Deque<Instant> window = calls.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && !window.peekFirst().isAfter(windowStart)) {
                window.pollFirst();
            }
            if (window.size() >= rateLimited.maxCalls()) {
                long retryAfter = window.isEmpty()
                        ? rateLimited.perSeconds()
                        : Math.max(1, Duration.between(now,
                                window.peekFirst().plusSeconds(rateLimited.perSeconds())).toSeconds());
                throw new TooManyRequestsException("Rate limit exceeded for " + method + ": max "
                        + rateLimited.maxCalls() + " calls per " + rateLimited.perSeconds()
                        + " s, retry in " + retryAfter + " s");
            }
            window.addLast(now);
        }
        return pjp.proceed();
    }
}
