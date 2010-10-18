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
package org.modeshape.connector.infinispan;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import net.jcip.annotations.ThreadSafe;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;

/**
 * A repository source that uses an Infinispan instance to manage the content. This source is capable of using an existing
 * {@link CacheContainer} or creating a new cache container. This process is controlled entirely by the JavaBean properties of the
 * InfinispanSource instance.
 * <p>
 * This source first attempts to find an existing cache manager found in {@link #getCacheContainerJndiName() JNDI} (or the
 * {@link DefaultCacheManager} if no such manager is available) and the {@link #getCacheConfigurationName() cache configuration
 * name} if supplied or the default configuration if not set.
 * </p>
 * <p>
 * Like other {@link RepositorySource} classes, instances of InfinispanCacheSource can be placed into JNDI and do support the
 * creation of {@link Referenceable JNDI referenceable} objects and resolution of references into JBossCacheSource.
 * </p>
 */
@ThreadSafe
public class InfinispanSource extends BaseInfinispanSource {

    private static final long serialVersionUID = 2L;
    protected static final String CACHE_CONFIGURATION_NAME = "cacheConfigurationName";
    protected static final String CACHE_FACTORY_JNDI_NAME = "cacheContainerJndiName";

    @Description( i18n = InfinispanConnectorI18n.class, value = "cacheConfigurationNamePropertyDescription" )
    @Label( i18n = InfinispanConnectorI18n.class, value = "cacheConfigurationNamePropertyLabel" )
    @Category( i18n = InfinispanConnectorI18n.class, value = "cacheConfigurationNamePropertyCategory" )
    private volatile String cacheConfigurationName;

    @Description( i18n = InfinispanConnectorI18n.class, value = "cacheContainerJndiNamePropertyDescription" )
    @Label( i18n = InfinispanConnectorI18n.class, value = "cacheContainerJndiNamePropertyLabel" )
    @Category( i18n = InfinispanConnectorI18n.class, value = "cacheContainerJndiNamePropertyCategory" )
    private volatile String cacheContainerJndiName;

    /**
     * Create a repository source instance.
     */
    public InfinispanSource() {
    }

    /**
     * Get the name in JNDI of a {@link CacheContainer} instance that should be used to create the cache for this source.
     * <p>
     * This source first attempts to find a cache instance using the {@link CacheContainer} found in
     * {@link #getCacheContainerJndiName() JNDI} (or the {@link DefaultCacheManager} if no such manager is available) and the
     * {@link #getCacheConfigurationName() cache configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @return the JNDI name of the {@link CacheContainer} instance that should be used, or null if the
     *         {@link DefaultCacheManager} should be used if a cache is to be created
     * @see #setCacheContainerJndiName(String)
     * @see #getCacheConfigurationName()
     */
    public String getCacheContainerJndiName() {
        return cacheContainerJndiName;
    }

    /**
     * Set the name in JNDI of a {@link CacheContainer} instance that should be used to obtain the {@link Cache} instance used by
     * this source.
     * <p>
     * This source first attempts to find a cache instance using the {@link CacheContainer} found in
     * {@link #getCacheContainerJndiName() JNDI} (or the {@link DefaultCacheManager} if no such manager is available) and the
     * {@link #getCacheConfigurationName() cache configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @param jndiName the JNDI name of the {@link CacheContainer} instance that should be used, or null if the
     *        {@link DefaultCacheManager} should be used if a cache is to be created
     * @see #setCacheContainerJndiName(String)
     * @see #getCacheConfigurationName()
     */
    public synchronized void setCacheContainerJndiName( String jndiName ) {
        if (this.cacheContainerJndiName == jndiName || this.cacheContainerJndiName != null
            && this.cacheContainerJndiName.equals(jndiName)) return; // unchanged
        this.cacheContainerJndiName = jndiName;
    }

    @Deprecated
    /**
     * This method may be removed at any time, and is kept for backwards compatibility.
     * Now invokes getCacheContainerJndiName()
     */
    public String getCacheManagerJndiName() {
        return this.getCacheContainerJndiName();
    }

    @Deprecated
    /**
     * Set the name in JNDI of a {@link cacheContainer} instance.
     * This method is now deprecated, and calls setCacheContainerJndiName(String)
     */
    public synchronized void setCacheManagerJndiName( String jndiName ) {
        this.setCacheContainerJndiName(jndiName);
    }

    /**
     * Get the name of the configuration that should be used if a {@link Cache cache} is to be created using the
     * {@link CacheContainer} found in JNDI or the {@link DefaultCacheManager} if needed.
     * <p>
     * This source first attempts to find a cache instance using the {@link CacheContainer} found in
     * {@link #getCacheContainerJndiName() JNDI} (or the {@link DefaultCacheManager} if no such manager is available) and the
     * {@link #getCacheConfigurationName() cache configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @return the name of the configuration that should be passed to the {@link CacheContainer}, or null if the default
     *         configuration should be used
     * @see #setCacheConfigurationName(String)
     * @see #getCacheContainerJndiName()
     */
    public String getCacheConfigurationName() {
        return cacheConfigurationName;
    }

    /**
     * Get the name of the configuration that should be used if a {@link Cache cache} is to be created using the
     * {@link CacheContainer} found in JNDI or the {@link DefaultCacheManager} if needed.
     * <p>
     * This source first attempts to find a cache instance using the {@link CacheContainer} found in
     * {@link #getCacheContainerJndiName() JNDI} (or the {@link DefaultCacheManager} if no such manager is available) and the
     * {@link #getCacheConfigurationName() cache configuration name} if supplied or the default configuration if not set.
     * </p>
     * 
     * @param cacheConfigurationName the name of the configuration that should be passed to the {@link CacheContainer}, or null if
     *        the default configuration should be used
     * @see #getCacheConfigurationName()
     * @see #getCacheContainerJndiName()
     */
    public synchronized void setCacheConfigurationName( String cacheConfigurationName ) {
        if (this.cacheConfigurationName == cacheConfigurationName || this.cacheConfigurationName != null
            && this.cacheConfigurationName.equals(cacheConfigurationName)) return; // unchanged
        this.cacheConfigurationName = cacheConfigurationName;
    }

    @Override
    protected CacheContainer createCacheContainer() {
        CacheContainer cacheContainer = null;
        String jndiName = getCacheContainerJndiName();
        if (jndiName != null && jndiName.trim().length() != 0) {
            Object object = null;
            try {
                object = super.getContext().lookup(jndiName);
                if (object != null) cacheContainer = (CacheContainer)object;
            } catch (ClassCastException err) {
                I18n msg = InfinispanConnectorI18n.objectFoundInJndiWasNotCacheContainer;
                String className = object != null ? object.getClass().getName() : "null";
                throw new RepositorySourceException(getName(), msg.text(jndiName, this.getName(), className), err);
            } catch (Throwable err) {
                if (err instanceof RuntimeException) throw (RuntimeException)err;
                throw new RepositorySourceException(getName(), err);
            }
        } else {
            String configName = getCacheConfigurationName();
            if (configName == null) {
                cacheContainer = new DefaultCacheManager();
            } else {
                /*
                * First try treating the config name as a classpath resource, then as a file name.
                */
                InputStream configStream = getClass().getResourceAsStream(configName);
                try {
                    if (configStream == null) {
                        configStream = new FileInputStream(configName);
                    }
                } catch (IOException ioe) {
                    I18n msg = InfinispanConnectorI18n.configFileNotFound;
                    throw new RepositorySourceException(super.getName(), msg.text(configName), ioe);
                }

                try {
                    cacheContainer = new DefaultCacheManager(configStream);
                } catch (IOException ioe) {
                    I18n msg = InfinispanConnectorI18n.configFileNotValid;
                    throw new RepositorySourceException(super.getName(), msg.text(configName), ioe);
                } finally {
                    try {
                        configStream.close();
                    } catch (IOException ioe) {
                    }
                }
            }
        }

        return cacheContainer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof InfinispanSource) {
            InfinispanSource that = (InfinispanSource)obj;
            if (this.getName() == null) {
                if (that.getName() != null) return false;
            } else {
                if (!this.getName().equals(that.getName())) return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return HashCode.compute(getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Reference getReference() {
        Reference ref = super.getReference();
        ref.add(new StringRefAddr(CACHE_FACTORY_JNDI_NAME, getCacheContainerJndiName()));
        ref.add(new StringRefAddr(CACHE_CONFIGURATION_NAME, getCacheConfigurationName()));
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
            String cacheContainerJndiName = (String)values.get(CACHE_FACTORY_JNDI_NAME);
            String cacheConfigurationName = (String)values.get(CACHE_CONFIGURATION_NAME);
            Object defaultCachePolicy = values.get(DEFAULT_CACHE_POLICY);
            String retryLimit = (String)values.get(RETRY_LIMIT);
            String defaultWorkspace = (String)values.get(DEFAULT_WORKSPACE);
            String createWorkspaces = (String)values.get(ALLOW_CREATING_WORKSPACES);
            String updatesAllowed = (String)values.get(UPDATES_ALLOWED);

            String combinedWorkspaceNames = (String)values.get(PREDEFINED_WORKSPACE_NAMES);
            String[] workspaceNames = null;
            if (combinedWorkspaceNames != null) {
                List<String> paths = StringUtil.splitLines(combinedWorkspaceNames);
                workspaceNames = paths.toArray(new String[paths.size()]);
            }

            // Create the source instance ...
            InfinispanSource source = new InfinispanSource();
            if (sourceName != null) source.setName(sourceName);
            if (rootNodeUuidString != null) source.setRootNodeUuid(rootNodeUuidString);
            if (cacheContainerJndiName != null) source.setCacheContainerJndiName(cacheContainerJndiName);
            if (cacheConfigurationName != null) source.setCacheConfigurationName(cacheConfigurationName);
            if (defaultCachePolicy instanceof CachePolicy) {
                source.setDefaultCachePolicy((CachePolicy)defaultCachePolicy);
            }
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            if (defaultWorkspace != null) source.setDefaultWorkspaceName(defaultWorkspace);
            if (createWorkspaces != null) source.setCreatingWorkspacesAllowed(Boolean.parseBoolean(createWorkspaces));
            if (workspaceNames != null && workspaceNames.length != 0) source.setPredefinedWorkspaceNames(workspaceNames);
            if (updatesAllowed != null) source.setUpdatesAllowed(Boolean.valueOf(updatesAllowed));
            return source;
        }
        return null;
    }
}
