/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.infinispan.schematic.internal;

import java.util.EnumSet;
import javax.transaction.TransactionManager;
import org.infinispan.AdvancedCache;
import org.infinispan.atomic.Delta;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionTable;
import org.infinispan.commons.logging.Log;
import org.infinispan.commons.logging.LogFactory;

/**
 * Encapsulation of some runtime and configuration-related information for a particular advanced cache.
 */
final class CacheContext {

    private static final Log LOGGER = LogFactory.getLog(CacheContext.class);

    private final AdvancedCache<String, SchematicEntry> cache;
    private final AdvancedCache<String, SchematicEntry> cacheForWriting;
    private final AdvancedCache<String, SchematicEntry> cacheForLocking;
    private final TransactionManager txnMgr;
    private final TransactionTable transactionTable;
    private final boolean explicitLockingEnabled;
    private final boolean clustered;

    CacheContext( AdvancedCache<String, SchematicEntry> cache ) {
        this.cache = cache;
        final Configuration config = cache.getCacheConfiguration();

        // We're clustered if the cache mode is not local ...
        this.clustered = config.clustering().cacheMode() != CacheMode.LOCAL;

        EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
        flags.add(Flag.SKIP_REMOTE_LOOKUP);
        flags.add(Flag.DELTA_WRITE);
        LOGGER.debug("Deltas will be used to serializing changes to documents in '" + cache.getName() + "'.");
        //LOGGER.debug("Passivation? " + config.loaders().passivation()); // hxp 20140112 -- Configuration api no longer provides classLoader() or loaders()
        LOGGER.debug("Eviction? " + config.eviction().strategy());
        LOGGER.debug("Clustering mode? " + config.clustering().cacheMode());

        LOGGER.debug("Using cache with flags " + flags + " during SchematicEntry updates");
        this.cacheForWriting = this.cache.withFlags(flags.toArray(new Flag[flags.size()]));

        this.txnMgr = cache.getTransactionManager();
        this.transactionTable = cache.getComponentRegistry().getComponent(TransactionTable.class);
        LockingMode lockingMode = config.transaction().lockingMode();
        this.explicitLockingEnabled = lockingMode == LockingMode.PESSIMISTIC;
        if (this.isExplicitLockingEnabled()) {
            this.cacheForLocking = this.cache.withFlags(Flag.FAIL_SILENTLY);
            LOGGER.debug("Explicit locks will be used when modifying documents in '" + cache.getName()
                         + "' (Infinispan's locking mode is PESSIMISTIC).");
        } else {
            this.cacheForLocking = this.cache;
            LOGGER.debug("Explicit locks will NOT be used when modifying documents in '" + cache.getName()
                         + "' (Infinispan's locking mode is not PESSIMISTIC).");
        }
    }

    /**
     * Get the advanced cache.
     * 
     * @return the cache; never null
     */
    public AdvancedCache<String, SchematicEntry> getCache() {
        return cache;
    }

    /**
     * Get the advanced cache that should be used for writing.
     * 
     * @return the writing cache; never null
     */
    public AdvancedCache<String, SchematicEntry> getCacheForWriting() {
        return cacheForWriting;
    }

    /**
     * Get the advanced cache that should be used for locking.
     * 
     * @return the locking cache; never null
     */
    public AdvancedCache<String, SchematicEntry> getCacheForLocking() {
        return cacheForLocking;
    }

    /**
     * Get the cache's transaction table.
     * 
     * @return the transaction table; never null
     */
    public TransactionTable getTransactionTable() {
        return transactionTable;
    }

    /**
     * Get the cache's transaction manager.
     * 
     * @return the transaction manager; never null
     */
    public TransactionManager getTransactionManager() {
        return txnMgr;
    }

    /**
     * Return whether explicit locking is enabled.
     * 
     * @return true if explicit locks should be used, or false otherwise
     */
    public boolean isExplicitLockingEnabled() {
        return explicitLockingEnabled;
    }

    /**
     * Return whether the {@link Delta} implementation will contain only the differences.
     * 
     * @return true if the {@link Delta} implementation contains only differences, or false if it contains the whole document.
     */
    public boolean isDeltaContainingChangesEnabled() {
        return false;
    }

    /**
     * Return whether this cache is clustered.
     * 
     * @return true if the cache is clustered, or false otherwise.
     */
    public boolean isClustered() {
        return clustered;
    }
}
