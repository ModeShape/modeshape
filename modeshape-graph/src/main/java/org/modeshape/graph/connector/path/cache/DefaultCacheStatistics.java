package org.modeshape.graph.connector.path.cache;

import java.util.concurrent.atomic.AtomicLong;
import net.jcip.annotations.ThreadSafe;

/**
 * Default, thread-safe implementation of {@link CacheStatistics} that uses {@link AtomicLong AtomicLongs} as counters for the
 * statistics.
 */
@ThreadSafe
public final class DefaultCacheStatistics implements CacheStatistics {
    private final AtomicLong writes = new AtomicLong(0);
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong expirations = new AtomicLong(0);

    public long getWrites() {
        return writes.get();
    }

    public long getHits() {
        return hits.get();
    }

    public long getMisses() {
        return misses.get();
    }

    public long getExpirations() {
        return expirations.get();
    }

    public long incrementWrites() {
        return writes.getAndIncrement();
    }

    public long incrementHits() {
        return hits.getAndIncrement();
    }

    public long incrementMisses() {
        return misses.getAndIncrement();
    }

    public long incrementExpirations() {
        return expirations.getAndIncrement();
    }
}
