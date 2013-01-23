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
import org.infinispan.interceptors.IsMarshallableInterceptor;
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
    private final boolean deltaConsistsOfChanges;
    private final boolean clustered;

    CacheContext( AdvancedCache<String, SchematicEntry> cache ) {
        this.cache = cache;
        final Configuration config = cache.getCacheConfiguration();

        // We're clustered if the cache mode is not local ...
        this.clustered = config.clustering().cacheMode() != CacheMode.LOCAL;

        // ********************************************************************************************************
        // TODO 3.2 Remove all the version-specific code in our 3.2 release, since that will be upgrading ISPN 5.2.x
        // See MODE-1771
        // ********************************************************************************************************

        // At this point, we're always going to create a Delta object that ships the entire document, because
        // of problems we're having in ISPN 5.1.x. See MODE-1733 for details.
        boolean deltaConsistsOfChanges = false;

        Flag deltaWriteFlag = null;
        short ispnVersionActual = Version.getVersionShort();
        short ispnVersion520 = Version.encodeVersion(5, 2, 0);
        if (ispnVersionActual >= ispnVersion520) {
            // This flag was introduced in Infinispan 5.1.6.FINAL (see ISPN-2094), so we can only use this flag
            // if we're using 5.1.6 or later. However, the state transfer functionality of DeltaAware doesn't seem
            // to have been fixed until 5.2.0. See MODE-1733, MODE-1746, and MODE-1745 for details.
            try {
                deltaWriteFlag = Flag.valueOf("DELTA_WRITE");
                deltaConsistsOfChanges = true;
                LOGGER.debug("Found DELTA_WRITE flag");
            } catch (IllegalArgumentException e) {
                // okay, must not be able to find this flag leave deltaConsistsOfChanges as 'false'
                LOGGER.debug("Failed to find DELTA_WRITE flag");
            }
        } else {
            // This is before Infinispan 5.2.0, so we can't actually use deltas. See MODE-1733 for details.
            deltaConsistsOfChanges = false;
            LOGGER.debug("No DELTA_WRITE flag available");
        }

        boolean pre520Final = ispnVersionActual < ispnVersion520
                              || (ispnVersionActual == ispnVersion520 && !Version.VERSION.toLowerCase().endsWith("final"));
        if (pre520Final && config.loaders().usingAsyncStore()) {
            // This is one of the non-final releases of 5.2.0 or an earlier release, which has a bug when using an async cache
            // store (see MODE-1733 and ISPN-2748). The workaround is to remove the IsMarshallableInterceptor ...
            this.cache.removeInterceptor(IsMarshallableInterceptor.class);
            LOGGER.debug("Removing IsMarshallableInterceptor from interceptor stack in pre-5.2.0.Final Infinispan cache '"
                         + cache.getName() + "'");
        }
        LOGGER.trace("ispnVersionActual = " + ispnVersionActual);
        LOGGER.trace("ispnVersion520    = " + ispnVersion520);
        LOGGER.trace("Version.VERSION   = " + Version.VERSION);
        LOGGER.trace("Version.VERSION.toLowerCase().endsWith('final') = " + Version.VERSION.toLowerCase().endsWith("final"));
        LOGGER.trace("is pre-5.2.0.Final = " + pre520Final);
        LOGGER.trace("config.loaders().usingAsyncStore()    = " + config.loaders().usingAsyncStore());

        EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
        flags.add(Flag.SKIP_REMOTE_LOOKUP);
        if (deltaWriteFlag != null) {
            // When passivation is enabled, cache loader needs to attempt to load
            // the previous value in order to merge it if necessary, so mark atomic
            // hash map writes as delta writes
            LOGGER.debug("Passivation? " + config.loaders().passivation());
            LOGGER.debug("Eviction? " + config.eviction().strategy());
            LOGGER.debug("Clustering mode? " + config.clustering().cacheMode());
            if (config.loaders().passivation() || config.eviction().strategy() != EvictionStrategy.NONE
                || config.clustering().cacheMode() != CacheMode.LOCAL) {
                // If we're passivating, evicting, or clustering, we need to use the DELTA_WRITE flag ...
                flags.add(deltaWriteFlag);
            } else {
                flags.add(Flag.SKIP_CACHE_LOAD);
            }
        }
        LOGGER.debug("Using cache with flags " + flags + " during SchematicEntry updates");
        this.cacheForWriting = this.cache.withFlags(flags.toArray(new Flag[flags.size()]));

        this.deltaConsistsOfChanges = deltaConsistsOfChanges;
        if (this.isDeltaContainingChangesEnabled()) {
            LOGGER.debug("Deltas will be used to serializing changes to documents in '" + cache.getName() + "'.");
        } else {
            LOGGER.debug("Deltas will NOT be used to serializing changes to documents in '" + cache.getName() + "'.");
        }
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
