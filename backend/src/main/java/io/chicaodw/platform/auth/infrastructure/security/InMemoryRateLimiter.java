package io.chicaodw.platform.auth.infrastructure.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window request counter, in memory (no Redis this sprint — Sprint 11B.6A,
 * SEC-AUTH-03/DT-011B.5 §9 HARD-01). One bucket per key; a key that exceeds its
 * capacity within the current window is denied until the window rolls over.
 *
 * Deliberately not a token bucket / sliding window — the DT called for "solução em
 * memória... configurável", not a specific algorithm; fixed-window is the simplest
 * correct option and is trivial to reason about and test.
 */
@Component
public class InMemoryRateLimiter {

    private final Clock clock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public record Decision(boolean allowed, long retryAfterSeconds) {}

    public Decision tryAcquire(String key, int capacity, long windowSeconds) {
        long now = clock.instant().getEpochSecond();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket());
        return bucket.tryAcquire(now, capacity, windowSeconds);
    }

    private static final class Bucket {

        private long windowStartEpochSecond;
        private int count;

        synchronized Decision tryAcquire(long now, int capacity, long windowSeconds) {
            if (now - windowStartEpochSecond >= windowSeconds) {
                windowStartEpochSecond = now;
                count = 0;
            }
            count++;
            if (count > capacity) {
                long retryAfter = windowSeconds - (now - windowStartEpochSecond);
                return new Decision(false, Math.max(retryAfter, 1));
            }
            return new Decision(true, 0);
        }
    }
}
