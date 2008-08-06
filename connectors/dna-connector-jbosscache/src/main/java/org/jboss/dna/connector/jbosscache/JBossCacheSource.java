/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.jbosscache;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.DnaLexicon;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.connection.AbstractRepositorySource;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositorySourceCapabilities;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class JBossCacheSource extends AbstractRepositorySource implements ObjectFactory {

    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_UUID_PROPERTY_NAME = DnaLexicon.PropertyNames.UUID;

    private static final ConcurrentMap<String, JBossCacheSource> sources = new ConcurrentHashMap<String, JBossCacheSource>();
    private static final ReadWriteLock sourcesLock = new ReentrantReadWriteLock();

    /**
     * Get the names of the in-memory repository sources that are currently registered
     * 
     * @return the unmodifiable set of names
     */
    public static Set<String> getSourceNames() {
        Lock lock = sourcesLock.readLock();
        try {
            lock.lock();
            return Collections.unmodifiableSet(sources.keySet());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the source with the supplied name.
     * 
     * @param name the name
     * @return the source, or null if there is no source with the supplied name
     */
    public static JBossCacheSource getSource( String name ) {
        Lock lock = sourcesLock.readLock();
        try {
            lock.lock();
            return sources.get(name);
        } finally {
            lock.unlock();
        }
    }

    @GuardedBy( "sourcesLock" )
    private String name;
    @GuardedBy( "this" )
    private String jndiName;
    private UUID rootNodeUuid = UUID.randomUUID();
    private CachePolicy defaultCachePolicy;
    private String cacheConfigurationName;
    private String uuidPropertyName = DEFAULT_UUID_PROPERTY_NAME;
    private transient Cache<Name, Object> cache;

    /**
     * Create a repository source instance.
     */
    public JBossCacheSource() {
    }

    /**
     * Get the default cache policy for this source, or null if the global default cache policy should be used
     * 
     * @return the default cache policy, or null if this source has no explicit default cache policy
     */
    public CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
    }

    /**
     * @param defaultCachePolicy Sets defaultCachePolicy to the specified value.
     */
    public void setDefaultCachePolicy( CachePolicy defaultCachePolicy ) {
        this.defaultCachePolicy = defaultCachePolicy;
    }

    /**
     * @return rootNodeUuid
     */
    public UUID getRootNodeUuid() {
        return this.rootNodeUuid;
    }

    /**
     * @param rootNodeUuid Sets rootNodeUuid to the specified value.
     */
    public void setRootNodeUuid( UUID rootNodeUuid ) {
        this.rootNodeUuid = rootNodeUuid != null ? rootNodeUuid : UUID.randomUUID();
    }

    /**
     * @return uuidPropertyName
     */
    public String getUuidPropertyName() {
        return this.uuidPropertyName;
    }

    /**
     * @param uuidPropertyName Sets uuidPropertyName to the specified value.
     */
    public synchronized void setUuidPropertyName( String uuidPropertyName ) {
        this.uuidPropertyName = uuidPropertyName != null ? uuidPropertyName.trim() : DEFAULT_UUID_PROPERTY_NAME;
    }

    /**
     * If you use this to set a JNDI name, this source will be bound to that name using the default {@link InitialContext}. You
     * can also do this manually if you have additional requirements.
     * 
     * @param name the JNDI name
     * @throws NamingException if there is a problem registering this object
     * @see #getJndiName()
     */
    public void setJndiName( String name ) throws NamingException {
        setJndiName(name, null);
    }

    /**
     * Register this source in JNDI under the supplied name using the supplied context. to set a JNDI name, this source will be
     * bound to that name using the default {@link InitialContext}. You can also do this manually if you have additional
     * requirements.
     * 
     * @param name the JNDI name, or null if this object is to no longer be registered
     * @param context the JNDI context, or null if the {@link InitialContext} should be used
     * @throws NamingException if there is a problem registering this object
     * @see #getJndiName()
     */
    public synchronized void setJndiName( String name,
                                          Context context ) throws NamingException {
        ArgCheck.isNotNull(name, "name");
        if (context == null) context = new InitialContext();

        // First register in JNDI under the new name ...
        if (name != null) {
            context.bind(name, this);
        }
        // Second, unregister from JNDI if there is already a name ...
        if (jndiName != null && !jndiName.equals(name)) {
            context.unbind(jndiName);
        }
        // Record the new name ...
        this.jndiName = name;
    }

    /**
     * Gets the JNDI name this source is bound to. Only valid if you used setJNDIName to bind it.
     * 
     * @return the JNDI name, or null if it is not bound in JNDI
     * @see #setJndiName(String)
     */
    public synchronized String getJndiName() {
        return jndiName;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        Lock lock = sourcesLock.readLock();
        try {
            lock.lock();
            return this.name;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param name Sets name to the specified value.
     * @return true if the name was changed, or false if an existing instance already exists with that name
     */
    public boolean setName( String name ) {
        Lock lock = sourcesLock.writeLock();
        try {
            lock.lock();
            // Determine if this name is allowed ...
            if (name != null && sources.containsKey(name)) return false;

            // Remove this object under its current name
            if (this.name != null) {
                sources.remove(this.name);
            }
            // Register this object under the new name
            this.name = name;
            if (this.name != null) {
                sources.put(this.name, this);
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected synchronized RepositoryConnection createConnection() {
        if (this.cache == null) {
            CacheFactory<Name, Object> factory = new DefaultCacheFactory<Name, Object>();
            cache = factory.createCache(cacheConfigurationName);
        }
        return new JBossCacheConnection(this, this.cache);
    }

    /**
     * {@inheritDoc}
     */
    public Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = className;
        return new Reference(className, new StringRefAddr("DnaConnectorJBossCacheSource", getName()), factoryClassName, null);
    }

    /**
     * {@inheritDoc}
     */
    public Object getObjectInstance( Object obj,
                                     javax.naming.Name name,
                                     Context nameCtx,
                                     Hashtable<?, ?> environment ) {
        if (obj instanceof Reference) {
            Reference ref = (Reference)obj;
            if (ref.getClassName().equals(getClass().getName())) {
                RefAddr addr = ref.get("DnaConnectorJBossCacheSource");
                return JBossCacheSource.getSource((String)addr.getContent());
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return new Capabilities();
    }

    protected class Capabilities implements RepositorySourceCapabilities {
        public boolean supportsSameNameSiblings() {
            return true;
        }

        public boolean supportsUpdates() {
            return true;
        }
    }
}
