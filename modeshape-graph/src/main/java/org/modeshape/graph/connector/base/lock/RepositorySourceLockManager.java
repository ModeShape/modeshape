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
package org.modeshape.graph.connector.base.lock;

import java.util.Map;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.connector.base.Repository;


/**
 * Encapsulation of all of the functionality that repositories need to support configurable locking.
 */
@NotThreadSafe
public class RepositorySourceLockManager {

    private static final Logger LOG = Logger.getLogger(RepositorySourceLockManager.class);

    public static final String LOCK_PROVIDER_CLASS_NAME = "lockProviderClassName";
    public static final String LOCK_STRATEGY_CLASS_NAME = "lockStrategyClassName";
    
    /**
     * The default lock provider for this class (JVM locking). The value is {@value} .
     */
    public static final String DEFAULT_LOCK_PROVIDER_CLASS_NAME = JvmLockProvider.class.getName();

    /**
     * The default lock strategy for this class (repository-level locking). The value is {@value} .
     */
    public static final String DEFAULT_LOCK_STRATEGY_CLASS_NAME = RepositoryLockStrategy.class.getName();

    protected transient String lockProviderClassName = DEFAULT_LOCK_PROVIDER_CLASS_NAME;
    protected transient String lockStrategyClassName = DEFAULT_LOCK_STRATEGY_CLASS_NAME;

    /**
     * Returns the class name of the current lock provider for this repository.
     * 
     * @return the class name of the current lock provider for this repository; never null.
     */
    public String getLockProviderClassName() {
        return this.lockProviderClassName;
    }

    /**
     * Returns the class name of the current lock strategy for this repository.
     * 
     * @return the class name of the current lock strategy for this repository; never null.
     */
    public String getLockStrategyClassName() {
        return this.lockStrategyClassName;
    }

    /**
     * Sets the {@link LockProvider lock provider class name} for this {@link Repository repository}.
     * 
     * @param lockProviderClassName the class name of the lock provider; null indicates that the default for the repository should
     *        be used. Each repository is free to define its own default lock provider.
     */
    public void setLockProviderClassName( String lockProviderClassName ) {
        if (lockProviderClassName == null || lockProviderClassName.trim().length() == 0) {
            lockProviderClassName = DEFAULT_LOCK_PROVIDER_CLASS_NAME;
        }

        this.lockProviderClassName = lockProviderClassName;

    }

    /**
     * Sets the {@link LockStrategy lock strategy class name} for this {@link Repository repository}.
     * 
     * @param lockStrategyClassName the class name of the lock strategy; null indicates that the default for the repository should
     *        be used. Each repository is free to define its own default lock strategy.
     */
    public void setLockStrategyClassName( String lockStrategyClassName ) {
        if (lockStrategyClassName == null || lockStrategyClassName.trim().length() == 0) {
            lockStrategyClassName = DEFAULT_LOCK_STRATEGY_CLASS_NAME;
        }

        this.lockStrategyClassName = lockStrategyClassName;
    }

    public LockStrategy getLockStrategy( String sourceName ) {
        try {
            Class<? extends LockStrategy> lockStrategyClass = Class.forName(getLockStrategyClassName()).asSubclass(LockStrategy.class);
            return lockStrategyClass.newInstance();
        } catch (ClassNotFoundException cnfe) {
            LOG.error(GraphI18n.lockStrategyClassNotFound,
                      sourceName,
                      getLockStrategyClassName(),
                      RepositoryLockStrategy.class.getName());
            return new RepositoryLockStrategy();
        } catch (InstantiationException e) {
            LOG.error(GraphI18n.lockStrategyClassBadConstructor,
                      sourceName,
                      getLockStrategyClassName(),
                      RepositoryLockStrategy.class.getName());
            return new RepositoryLockStrategy();
        } catch (IllegalAccessException e) {
            LOG.error(GraphI18n.lockStrategyClassBadConstructor,
                      sourceName,
                      getLockStrategyClassName(),
                      RepositoryLockStrategy.class.getName());
            return new RepositoryLockStrategy();
        }
    }
    
    public LockProvider getLockProvider( String sourceName ) {
        try {
            Class<? extends LockProvider> lockProviderClass = Class.forName(getLockProviderClassName()).asSubclass(LockProvider.class);
            return lockProviderClass.newInstance();
        } catch (ClassNotFoundException cnfe) {
            LOG.error(GraphI18n.lockProviderClassNotFound,
                      sourceName,
                      getLockProviderClassName(),
                      JvmLockProvider.class.getName());
            return new JvmLockProvider();
        } catch (InstantiationException e) {
            LOG.error(GraphI18n.lockProviderClassBadConstructor,
                      sourceName,
                      getLockProviderClassName(),
                      JvmLockProvider.class.getName());
            return new JvmLockProvider();
        } catch (IllegalAccessException e) {
            LOG.error(GraphI18n.lockProviderClassBadConstructor,
                      sourceName,
                      getLockProviderClassName(),
                      JvmLockProvider.class.getName());
            return new JvmLockProvider();
        }

    }
    
    public void setPropertiesFromValues( LockManagingRepositorySource source,
                                         Map<String, Object> values ) {
        String lockProviderClassName = String.valueOf(values.get(LOCK_PROVIDER_CLASS_NAME));
        if (lockProviderClassName != null) source.setLockProviderClassName(lockProviderClassName);

        String lockStrategyClassName = String.valueOf(values.get(LOCK_PROVIDER_CLASS_NAME));
        if (lockStrategyClassName != null) source.setLockStrategyClassName(lockStrategyClassName);
    }

    public void setReferenceFromProperties( Reference ref ) {
        ref.add(new StringRefAddr(LOCK_PROVIDER_CLASS_NAME, getLockProviderClassName()));
        ref.add(new StringRefAddr(LOCK_STRATEGY_CLASS_NAME, getLockStrategyClassName()));
    }
}
