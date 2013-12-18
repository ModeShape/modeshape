/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

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
        LOGGER.debug("Passivation? " + config.loaders().passivation());
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
