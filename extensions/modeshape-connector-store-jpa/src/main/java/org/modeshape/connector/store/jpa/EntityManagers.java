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
package org.modeshape.connector.store.jpa;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import net.jcip.annotations.ThreadSafe;
import org.hibernate.ejb.Ejb3Configuration;
import org.modeshape.graph.connector.RepositoryConnection;

/**
 * Utility class that owns an {@link EntityManagerFactory} instance and that provides references to {@link EntityManager}
 * instances while providing the ability to properly clean up all resources by closing the EntityManager and EntityManagerFactory
 * objects when no longer needed.
 * <p>
 * This class is instantiated by the {@link JpaSource} and passed to the {@link RepositoryConnection} objects, which use this
 * class to obtain an EntityManager. When the JPA connection object is {@link RepositoryConnection#close() closed}, it returns the
 * EntityManager to this object. Because this class maintains a count of the EntityManager references handed out, the last
 * EntityManager to be returned will cause the EntityManagerFactory to be closed.
 * </p>
 * <p>
 * This class does put the EntityManager implementations inside a HashMap, and therefore does expect that the EntityManager uses
 * object equality for <code>equals</code>.
 * </p>
 */
@ThreadSafe
public class EntityManagers {

    private final Ejb3Configuration configuration;
    private final Map<EntityManager, AtomicInteger> referenceCounts = new HashMap<EntityManager, AtomicInteger>();
    private EntityManagerFactory factory;
    private boolean canClose;

    EntityManagers( Ejb3Configuration configuration ) {
        this.configuration = configuration;
    }

    /**
     * Check out an EntityManager instance. The resulting manager <i>must</i> be returned with {@link #checkin(EntityManager)}. In
     * fact, for every {@link #checkout()} call, there must be a corresponding {@link #checkin(EntityManager)} call with the
     * checked out EntityManager.
     * <p>
     * Note that this class may return the same EntityManager for multiple calls, but this should not matter to the caller.
     * </p>
     * 
     * @return the entity manager; never null
     */
    public synchronized EntityManager checkout() {
        if (factory == null) {
            // Create the factory ...
            factory = configuration.buildEntityManagerFactory();
            assert referenceCounts.isEmpty();
            canClose = false;
        }
        // Create the entity manager and increment the reference count ...
        EntityManager manager = factory.createEntityManager();
        if (referenceCounts.containsKey(manager)) {
            referenceCounts.get(manager).incrementAndGet();
        } else {
            referenceCounts.put(manager, new AtomicInteger(1));
        }
        return manager;
    }

    /**
     * Return an EntityManager when it is no longer needed. This method should be called once supplying the same EntityManager
     * instance returned from a {@link #checkout()} call. In fact, for every {@link #checkout()} call, there must be a
     * corresponding {@link #checkin(EntityManager)} call with the checked out EntityManager.
     * <p>
     * If this is the last reference to the EntityManager instance, it will be closed.
     * </p>
     * 
     * @param manager the entity manager; may not be null and must be the result of a prior {@link #checkout()} call
     */
    public synchronized void checkin( EntityManager manager ) {
        if (manager == null) return;
        // Decrement the reference count ...
        AtomicInteger count = referenceCounts.get(manager);
        assert count != null;
        count.decrementAndGet();
        assert count.get() >= 0;
        if (count.get() == 0) {
            // Need to remove this manager ...
            if (referenceCounts.remove(manager) != null) {
                // And close the manager ...
                manager.close();
            }
        }
        // If there are no more entity managers checkout out, close the factory ...
        closeFactoryIfNoManagersCheckedOut();
    }

    public synchronized void close() {
        canClose = true;
        closeFactoryIfNoManagersCheckedOut();
    }

    /**
     * For all opened EntityManager instances to be closed immediately, even if they are checked out. This should be called with
     * caution, since all EntityManager and EntityManagerFactory instances will be closed when no longer used - as long as the
     * {@link #checkout() checkout}/{@link #checkin(EntityManager) checkin} pattern is always used.
     */
    public synchronized void closeNow() {
        canClose = true;
        try {
            for (EntityManager manager : referenceCounts.keySet()) {
                manager.close();
            }
            referenceCounts.clear();
        } finally {
            try {
                factory.close(); // This implicitly closes all EntityManagers that are still opened
            } finally {
                factory = null;
            }
        }
    }

    private void closeFactoryIfNoManagersCheckedOut() {
        if (referenceCounts.isEmpty() && canClose && factory != null) {
            try {
                factory.close();
            } finally {
                factory = null;
            }
        }
    }
}
