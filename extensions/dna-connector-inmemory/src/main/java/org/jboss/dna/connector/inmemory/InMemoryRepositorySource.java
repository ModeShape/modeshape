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
package org.jboss.dna.connector.inmemory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.GuardedBy;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositoryContext;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.connectors.RepositorySourceCapabilities;
import org.jboss.dna.graph.connectors.RepositorySourceException;

/**
 * @author Randall Hauch
 */
public class InMemoryRepositorySource implements RepositorySource, ObjectFactory {

    /**
     * The initial version is 1
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    protected static final String ROOT_NODE_UUID = "rootNodeUuid";
    protected static final String SOURCE_NAME = "sourceName";
    protected static final String DEFAULT_CACHE_POLICY = "defaultCachePolicy";
    protected static final String JNDI_NAME = "jndiName";
    protected static final String RETRY_LIMIT = "retryLimit";

    @GuardedBy( "sourcesLock" )
    private String name;
    @GuardedBy( "this" )
    private String jndiName;
    private UUID rootNodeUuid = UUID.randomUUID();
    private CachePolicy defaultCachePolicy;
    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);
    private transient InMemoryRepository repository;
    private transient RepositoryContext repositoryContext;

    /**
     * Create a repository source instance.
     */
    public InMemoryRepositorySource() {
        super();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#initialize(org.jboss.dna.graph.connectors.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context;
    }

    /**
     * @return repositoryContext
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        retryLimit.set(limit < 0 ? 0 : limit);
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
        return this.name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        if (repository == null) {
            repository = new InMemoryRepository(name, rootNodeUuid);
        }
        return new InMemoryRepositoryConnection(this, repository);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        if (getName() != null) {
            ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        }
        if (getRootNodeUuid() != null) {
            ref.add(new StringRefAddr(ROOT_NODE_UUID, getRootNodeUuid().toString()));
        }
        if (getJndiName() != null) {
            ref.add(new StringRefAddr(JNDI_NAME, getJndiName()));
        }
        if (getDefaultCachePolicy() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CachePolicy policy = getDefaultCachePolicy();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(policy);
                ref.add(new BinaryRefAddr(DEFAULT_CACHE_POLICY, baos.toByteArray()));
            } catch (IOException e) {
                I18n msg = InMemoryConnectorI18n.errorSerializingCachePolicyInSource;
                throw new RepositorySourceException(getName(), msg.text(policy.getClass().getName(), getName()), e);
            }
        }
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        return ref;
    }

    /**
     * {@inheritDoc}
     */
    public Object getObjectInstance( Object obj,
                                     javax.naming.Name name,
                                     Context nameCtx,
                                     Hashtable<?, ?> environment ) throws Exception {
        if (obj instanceof Reference) {
            Map<String, Object> values = new HashMap<String, Object>();
            Reference ref = (Reference)obj;
            Enumeration<?> en = ref.getAll();
            while (en.hasMoreElements()) {
                RefAddr subref = (RefAddr)en.nextElement();
                if (subref instanceof StringRefAddr) {
                    String key = subref.getType();
                    Object value = subref.getContent();
                    if (value != null) values.put(key, value.toString());
                } else if (subref instanceof BinaryRefAddr) {
                    String key = subref.getType();
                    Object value = subref.getContent();
                    if (value instanceof byte[]) {
                        // Deserialize ...
                        ByteArrayInputStream bais = new ByteArrayInputStream((byte[])value);
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        value = ois.readObject();
                        values.put(key, value);
                    }
                }
            }
            String sourceName = (String)values.get(SOURCE_NAME);
            String rootNodeUuidString = (String)values.get(ROOT_NODE_UUID);
            String jndiName = (String)values.get(JNDI_NAME);
            Object defaultCachePolicy = values.get(DEFAULT_CACHE_POLICY);
            String retryLimit = (String)values.get(RETRY_LIMIT);

            // Create the source instance ...
            InMemoryRepositorySource source = new InMemoryRepositorySource();
            if (sourceName != null) source.setName(sourceName);
            if (rootNodeUuidString != null) source.setRootNodeUuid(UUID.fromString(rootNodeUuidString));
            if (jndiName != null) source.setJndiName(jndiName);
            if (defaultCachePolicy instanceof CachePolicy) {
                source.setDefaultCachePolicy((CachePolicy)defaultCachePolicy);
            }
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            return source;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getCapabilities()
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
