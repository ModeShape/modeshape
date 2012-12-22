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
import org.infinispan.Version;
import org.infinispan.atomic.Delta;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.context.Flag;
import org.infinispan.eviction.EvictionStrategy;
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
    private static final boolean TRACE = LOGGER.isTraceEnabled();

    private final AdvancedCache<String, SchematicEntry> cache;
    private final AdvancedCache<String, SchematicEntry> cacheForWriting;
    private final TransactionManager txnMgr;
    private final TransactionTable transactionTable;
    private final boolean explicitLockingEnabled;
    private final boolean deltaConsistsOfChanges;
    private final boolean clustered;

    CacheContext( AdvancedCache<String, SchematicEntry> cache ) {
        this.cache = cache;
        final Configuration config = cache.getCacheConfiguration();

        // We're clustered if the cache mode is not local ...
        this.clustered = config.clustering().cacheMode() != CacheMode.LOCAL;

        // At this point, we're always going to create a Delta object that ships the entire document, because
        // of problems we're having in ISPN 5.1.x. See MODE-1733 for details.
        this.deltaConsistsOfChanges = false;

        Flag deltaWriteFlag = null;
        short ispnVersionActual = Version.getVersionShort();
        short ispnVersion316 = Version.encodeVersion(3, 1, 6);
        if (ispnVersionActual <= ispnVersion316) {
            // This flag was introduced in Infinispan 5.1.6.FINAL (see ISPN-2094), so before this we have to
            // handle deltas differently
            try {
                deltaWriteFlag = Flag.valueOf("DELTA_WRITE");
            } catch (IllegalArgumentException e) {
            }
        }
        EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
        flags.add(Flag.SKIP_REMOTE_LOOKUP);
        if (deltaWriteFlag != null) {
            // When passivation is enabled, cache loader needs to attempt to load
            // the previous value in order to merge it if necessary, so mark atomic
            // hash map writes as delta writes
            if (config.loaders().passivation() || config.eviction().strategy() != EvictionStrategy.NONE) {
                flags.add(deltaWriteFlag);
            } else {
                flags.add(Flag.SKIP_CACHE_LOAD);
            }
        }
        if (TRACE) {
            LOGGER.trace("Using cache with flags " + flags + " during SchematicEntry updates");
        }
        this.cacheForWriting = this.cache.withFlags(flags.toArray(new Flag[flags.size()]));

        this.txnMgr = cache.getTransactionManager();
        this.transactionTable = cache.getComponentRegistry().getComponent(TransactionTable.class);
        LockingMode lockingMode = config.transaction().lockingMode();
        this.explicitLockingEnabled = lockingMode == LockingMode.PESSIMISTIC;
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
        return deltaConsistsOfChanges;
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
