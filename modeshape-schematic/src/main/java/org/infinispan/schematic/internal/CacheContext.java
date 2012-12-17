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

import javax.transaction.TransactionManager;
import org.infinispan.AdvancedCache;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionTable;

/**
 * Encapsulation of some runtime and configuration-related information for a particular advanced cache.
 */
final class CacheContext {

    private final AdvancedCache<String, SchematicEntry> cache;
    private final TransactionManager txnMgr;
    private final TransactionTable transactionTable;
    private final boolean explicitLockingEnabled;

    CacheContext( AdvancedCache<String, SchematicEntry> cache ) {
        this.cache = cache;
        this.txnMgr = cache.getTransactionManager();
        this.transactionTable = cache.getComponentRegistry().getComponent(TransactionTable.class);
        LockingMode lockingMode = cache.getCacheConfiguration().transaction().lockingMode();
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
}
