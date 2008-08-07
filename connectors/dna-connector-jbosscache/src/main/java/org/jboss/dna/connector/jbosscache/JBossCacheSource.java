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
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.ThreadSafe;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.spi.DnaLexicon;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.connector.AbstractRepositorySource;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.connector.RepositorySource;
import org.jboss.dna.spi.connector.RepositorySourceCapabilities;
import org.jboss.dna.spi.connector.RepositorySourceException;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Property;

/**
 * A repository source that uses a JBoss Cache instance to manage the content. This source is capable of using an existing
 * {@link Cache} instance or creating a new instance. This process is controlled entirely by the JavaBean properties of the
 * JBossCacheSource instance.
 * <p>
 * This source first attempts to find an existing cache in {@link #getCacheJndiName() JNDI}. If none is found, then it attempts to
 * create a cache instance using the {@link CacheFactory} found in {@link #getCacheFactoryJndiName() JNDI} (or the
 * {@link DefaultCacheFactory} if no such factory is available) and the {@link #getCacheConfigurationName() cache configuration
 * name} if supplied or the default configuration if not set.
 * </p>
 * <p>
 * Like other {@link RepositorySource} classes, instances of JBossCacheSource can be placed into JNDI and do support the creation
 * of {@link Referenceable JNDI referenceable} objects and resolution of references into JBossCacheSource.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class JBossCacheSource extends AbstractRepositorySource implements ObjectFactory {

    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_UUID_PROPERTY_NAME = DnaLexicon.PropertyNames.UUID;

    protected static final String ROOT_NODE_UUID = "rootNodeUuid";
    protected static final String SOURCE_NAME = "sourceName";
    protected static final String DEFAULT_CACHE_POLICY = "defaultCachePolicy";
    protected static final String CACHE_CONFIGURATION_NAME = "cacheConfigurationName";
    protected static final String CACHE_FACTORY_JNDI_NAME = "cacheFactoryJndiName";
    protected static final String CACHE_JNDI_NAME = "cacheJndiName";
    protected static final String UUID_PROPERTY_NAME = "uuidPropertyName";
    protected static final String RETRY_LIMIT = "retryLimit";

    private String name;
    private UUID rootNodeUuid = UUID.randomUUID();
    private CachePolicy defaultCachePolicy;
    private String cacheConfigurationName;
    private String cacheFactoryJndiName;
    private String cacheJndiName;
    private String uuidPropertyName = DEFAULT_UUID_PROPERTY_NAME;
    private transient Cache<Name, Object> cache;
    private transient Context jndiContext;

    /**
     * Create a repository source instance.
     */
    public JBossCacheSource() {
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the name of this source
     * 
     * @param name the name for this source
     */
    public synchronized void setName( String name ) {
        if (this.name == name || this.name != null && this.name.equals(name)) return; // unchanged
        this.name = name;
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
    public synchronized void setDefaultCachePolicy( CachePolicy defaultCachePolicy ) {
        if (this.defaultCachePolicy == defaultCachePolicy || this.defaultCachePolicy != null
            && this.defaultCachePolicy.equals(defaultCachePolicy)) return; // unchanged
        this.defaultCachePolicy = defaultCachePolicy;
    }

    /**
     * Get the name in JNDI of a {@link Cache} instance that should be used by this source.
     * <p>
     * This source first attempts to find an existing cache in {@link #getCacheJndiName() JNDI}. If none is found, then it
     * attempts to create a cache instance using the {@link CacheFactory} found in {@link #getCacheFactoryJndiName() JNDI} (or the
     * {@link DefaultCacheFactory} if no such factory is available) and the {@link #getCacheConfigurationName() cache
     * configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @return the JNDI name of the {@link Cache} instance that should be used, or null if the cache is to be created with a cache
     *         factory {@link #getCacheFactoryJndiName() found in JNDI} using the specified {@link #getCacheConfigurationName()
     *         cache configuration name}.
     * @see #setCacheJndiName(String)
     * @see #getCacheConfigurationName()
     * @see #getCacheFactoryJndiName()
     */
    public String getCacheJndiName() {
        return cacheJndiName;
    }

    /**
     * Set the name in JNDI of a {@link Cache} instance that should be used by this source.
     * <p>
     * This source first attempts to find an existing cache in {@link #getCacheJndiName() JNDI}. If none is found, then it
     * attempts to create a cache instance using the {@link CacheFactory} found in {@link #getCacheFactoryJndiName() JNDI} (or the
     * {@link DefaultCacheFactory} if no such factory is available) and the {@link #getCacheConfigurationName() cache
     * configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @param cacheJndiName the JNDI name of the {@link Cache} instance that should be used, or null if the cache is to be created
     *        with a cache factory {@link #getCacheFactoryJndiName() found in JNDI} using the specified
     *        {@link #getCacheConfigurationName() cache configuration name}.
     * @see #getCacheJndiName()
     * @see #getCacheConfigurationName()
     * @see #getCacheFactoryJndiName()
     */
    public synchronized void setCacheJndiName( String cacheJndiName ) {
        if (this.cacheJndiName == cacheJndiName || this.cacheJndiName != null && this.cacheJndiName.equals(cacheJndiName)) return; // unchanged
        this.cacheJndiName = cacheJndiName;
    }

    /**
     * Get the name in JNDI of a {@link CacheFactory} instance that should be used to create the cache for this source.
     * <p>
     * This source first attempts to find an existing cache in {@link #getCacheJndiName() JNDI}. If none is found, then it
     * attempts to create a cache instance using the {@link CacheFactory} found in {@link #getCacheFactoryJndiName() JNDI} (or the
     * {@link DefaultCacheFactory} if no such factory is available) and the {@link #getCacheConfigurationName() cache
     * configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @return the JNDI name of the {@link CacheFactory} instance that should be used, or null if the {@link DefaultCacheFactory}
     *         should be used if a cache is to be created
     * @see #setCacheFactoryJndiName(String)
     * @see #getCacheConfigurationName()
     * @see #getCacheJndiName()
     */
    public String getCacheFactoryJndiName() {
        return cacheFactoryJndiName;
    }

    /**
     * Set the name in JNDI of a {@link CacheFactory} instance that should be used to obtain the {@link Cache} instance used by
     * this source.
     * <p>
     * This source first attempts to find an existing cache in {@link #getCacheJndiName() JNDI}. If none is found, then it
     * attempts to create a cache instance using the {@link CacheFactory} found in {@link #getCacheFactoryJndiName() JNDI} (or the
     * {@link DefaultCacheFactory} if no such factory is available) and the {@link #getCacheConfigurationName() cache
     * configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @param jndiName the JNDI name of the {@link CacheFactory} instance that should be used, or null if the
     *        {@link DefaultCacheFactory} should be used if a cache is to be created
     * @see #setCacheFactoryJndiName(String)
     * @see #getCacheConfigurationName()
     * @see #getCacheJndiName()
     */
    public synchronized void setCacheFactoryJndiName( String jndiName ) {
        if (this.cacheFactoryJndiName == jndiName || this.cacheFactoryJndiName != null
            && this.cacheFactoryJndiName.equals(jndiName)) return; // unchanged
        this.cacheFactoryJndiName = jndiName;
    }

    /**
     * Get the name of the configuration that should be used if a {@link Cache cache} is to be created using the
     * {@link CacheFactory} found in JNDI or the {@link DefaultCacheFactory} if needed.
     * <p>
     * This source first attempts to find an existing cache in {@link #getCacheJndiName() JNDI}. If none is found, then it
     * attempts to create a cache instance using the {@link CacheFactory} found in {@link #getCacheFactoryJndiName() JNDI} (or the
     * {@link DefaultCacheFactory} if no such factory is available) and the {@link #getCacheConfigurationName() cache
     * configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @return the name of the configuration that should be passed to the {@link CacheFactory}, or null if the default
     *         configuration should be used
     * @see #setCacheConfigurationName(String)
     * @see #getCacheFactoryJndiName()
     * @see #getCacheJndiName()
     */
    public String getCacheConfigurationName() {
        return cacheConfigurationName;
    }

    /**
     * Get the name of the configuration that should be used if a {@link Cache cache} is to be created using the
     * {@link CacheFactory} found in JNDI or the {@link DefaultCacheFactory} if needed.
     * <p>
     * This source first attempts to find an existing cache in {@link #getCacheJndiName() JNDI}. If none is found, then it
     * attempts to create a cache instance using the {@link CacheFactory} found in {@link #getCacheFactoryJndiName() JNDI} (or the
     * {@link DefaultCacheFactory} if no such factory is available) and the {@link #getCacheConfigurationName() cache
     * configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @param cacheConfigurationName the name of the configuration that should be passed to the {@link CacheFactory}, or null if
     *        the default configuration should be used
     * @see #getCacheConfigurationName()
     * @see #getCacheFactoryJndiName()
     * @see #getCacheJndiName()
     */
    public synchronized void setCacheConfigurationName( String cacheConfigurationName ) {
        if (this.cacheConfigurationName == cacheConfigurationName || this.cacheConfigurationName != null
            && this.cacheConfigurationName.equals(cacheConfigurationName)) return; // unchanged
        this.cacheConfigurationName = cacheConfigurationName;
    }

    /**
     * Get the UUID of the root node for the cache. If the cache exists, this UUID is not used but is instead set to the UUID of
     * the existing root node.
     * 
     * @return the UUID of the root node for the cache.
     */
    public String getRootNodeUuid() {
        return this.rootNodeUuid.toString();
    }

    /**
     * Get the UUID of the root node for the cache. If the cache exists, this UUID is not used but is instead set to the UUID of
     * the existing root node.
     * 
     * @return the UUID of the root node for the cache.
     */
    public UUID getRootNodeUuidObject() {
        return this.rootNodeUuid;
    }

    /**
     * Set the UUID of the root node in this repository. If the cache exists, this UUID is not used but is instead set to the UUID
     * of the existing root node.
     * 
     * @param rootNodeUuid the UUID of the root node for the cache, or null if the UUID should be randomly generated
     */
    public synchronized void setRootNodeUuid( String rootNodeUuid ) {
        UUID uuid = null;
        if (rootNodeUuid == null) uuid = UUID.randomUUID();
        else uuid = UUID.fromString(rootNodeUuid);
        if (this.rootNodeUuid.equals(uuid)) return; // unchanged
        this.rootNodeUuid = uuid;
    }

    /**
     * Get the {@link Property#getName() property name} where the UUID is stored for each node.
     * 
     * @return the name of the UUID property; never null
     */
    public String getUuidPropertyName() {
        return this.uuidPropertyName;
    }

    /**
     * Set the {@link Property#getName() property name} where the UUID is stored for each node.
     * 
     * @param uuidPropertyName the name of the UUID property, or null if the {@link #DEFAULT_UUID_PROPERTY_NAME default name}
     *        should be used
     */
    public synchronized void setUuidPropertyName( String uuidPropertyName ) {
        if (uuidPropertyName == null || uuidPropertyName.trim().length() == 0) uuidPropertyName = DEFAULT_UUID_PROPERTY_NAME;
        if (this.uuidPropertyName.equals(uuidPropertyName)) return; // unchanged
        this.uuidPropertyName = uuidPropertyName;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "unchecked" )
    @Override
    protected synchronized RepositoryConnection createConnection() {
        if (getName() == null) {
            I18n msg = JBossCacheConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }
        if (getUuidPropertyName() == null) {
            I18n msg = JBossCacheConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("uuidPropertyName"));
        }
        if (this.cache == null) {
            // First look for an existing cache instance in JNDI ...
            Context context = getContext();
            String jndiName = this.getCacheJndiName();
            if (jndiName != null && jndiName.trim().length() != 0) {
                Object object = null;
                try {
                    if (context == null) context = new InitialContext();
                    object = context.lookup(jndiName);
                    if (object != null) cache = (Cache<Name, Object>)object;
                } catch (ClassCastException err) {
                    I18n msg = JBossCacheConnectorI18n.objectFoundInJndiWasNotCache;
                    String className = object != null ? object.getClass().getName() : "null";
                    throw new RepositorySourceException(getName(), msg.text(jndiName, this.getName(), className), err);
                } catch (Throwable err) {
                    // try loading
                }
            }
            if (cache == null) {
                // Then look for a cache factory in JNDI ...
                CacheFactory<Name, Object> cacheFactory = null;
                jndiName = getCacheFactoryJndiName();
                if (jndiName != null && jndiName.trim().length() != 0) {
                    Object object = null;
                    try {
                        if (context == null) context = new InitialContext();
                        object = context.lookup(jndiName);
                        if (object != null) cacheFactory = (CacheFactory<Name, Object>)object;
                    } catch (ClassCastException err) {
                        I18n msg = JBossCacheConnectorI18n.objectFoundInJndiWasNotCacheFactory;
                        String className = object != null ? object.getClass().getName() : "null";
                        throw new RepositorySourceException(getName(), msg.text(jndiName, this.getName(), className), err);
                    } catch (Throwable err) {
                        // try loading
                    }
                }
                if (cacheFactory == null) cacheFactory = new DefaultCacheFactory<Name, Object>();

                // Now, get the configuration name ...
                String configName = this.getCacheConfigurationName();
                if (configName != null) {
                    cache = cacheFactory.createCache(configName);
                } else {
                    cache = cacheFactory.createCache();
                }
            }
        }
        return new JBossCacheConnection(this, this.cache);
    }

    protected Context getContext() {
        return this.jndiContext;
    }

    protected synchronized void setContext( Context context ) {
        this.jndiContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof JBossCacheSource) {
            JBossCacheSource that = (JBossCacheSource)obj;
            if (this.getName() == null) {
                if (that.getName() != null) return false;
            } else {
                if (!this.getName().equals(that.getName())) return false;
            }
            return true;
        }
        return false;
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
        if (getUuidPropertyName() != null) {
            ref.add(new StringRefAddr(UUID_PROPERTY_NAME, getUuidPropertyName()));
        }
        if (getCacheJndiName() != null) {
            ref.add(new StringRefAddr(CACHE_JNDI_NAME, getCacheJndiName()));
        }
        if (getCacheFactoryJndiName() != null) {
            ref.add(new StringRefAddr(CACHE_FACTORY_JNDI_NAME, getCacheFactoryJndiName()));
        }
        if (getCacheConfigurationName() != null) {
            ref.add(new StringRefAddr(CACHE_CONFIGURATION_NAME, getCacheConfigurationName()));
        }
        if (getDefaultCachePolicy() != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            CachePolicy policy = getDefaultCachePolicy();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(policy);
                ref.add(new BinaryRefAddr(DEFAULT_CACHE_POLICY, baos.toByteArray()));
            } catch (IOException e) {
                I18n msg = JBossCacheConnectorI18n.errorSerializingCachePolicyInSource;
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
            String uuidPropertyName = (String)values.get(UUID_PROPERTY_NAME);
            String cacheJndiName = (String)values.get(CACHE_JNDI_NAME);
            String cacheFactoryJndiName = (String)values.get(CACHE_FACTORY_JNDI_NAME);
            String cacheConfigurationName = (String)values.get(CACHE_CONFIGURATION_NAME);
            Object defaultCachePolicy = values.get(DEFAULT_CACHE_POLICY);
            String retryLimit = (String)values.get(RETRY_LIMIT);

            // Create the source instance ...
            JBossCacheSource source = new JBossCacheSource();
            if (sourceName != null) source.setName(sourceName);
            if (rootNodeUuidString != null) source.setRootNodeUuid(rootNodeUuidString);
            if (uuidPropertyName != null) source.setUuidPropertyName(uuidPropertyName);
            if (cacheJndiName != null) source.setCacheJndiName(cacheJndiName);
            if (cacheFactoryJndiName != null) source.setCacheFactoryJndiName(cacheFactoryJndiName);
            if (cacheConfigurationName != null) source.setCacheConfigurationName(cacheConfigurationName);
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
     * @see org.jboss.dna.spi.connector.RepositorySource#getCapabilities()
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
