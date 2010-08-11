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
package org.modeshape.connector.jbosscache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.config.ConfigurationException;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.BaseRepositorySource;
import org.modeshape.graph.connector.base.Connection;
import org.modeshape.graph.observe.Observer;

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
 * </p>
 */
@ThreadSafe
public class JBossCacheSource implements BaseRepositorySource, ObjectFactory {

    private static final long serialVersionUID = 2L;
    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;
    public static final String DEFAULT_UUID_PROPERTY_NAME = ModeShapeLexicon.UUID.getString();

    /**
     * The initial {@link #getDefaultWorkspaceName() name of the default workspace} is "{@value} ", unless otherwise specified.
     */
    public static final String DEFAULT_NAME_OF_DEFAULT_WORKSPACE = "default";

    /**
     * The initial value for whether updates are allowed is "{@value} ", unless otherwise specified.
     */
    public static final boolean DEFAULT_UPDATES_ALLOWED = true;

    protected static final String ROOT_NODE_UUID = "rootNodeUuid";
    protected static final String SOURCE_NAME = "sourceName";
    protected static final String DEFAULT_CACHE_POLICY = "defaultCachePolicy";
    protected static final String CACHE_CONFIGURATION_NAME = "cacheConfigurationName";
    protected static final String CACHE_FACTORY_JNDI_NAME = "cacheFactoryJndiName";
    protected static final String CACHE_JNDI_NAME = "cacheJndiName";
    protected static final String RETRY_LIMIT = "retryLimit";
    protected static final String DEFAULT_WORKSPACE = "defaultWorkspace";
    protected static final String PREDEFINED_WORKSPACE_NAMES = "predefinedWorkspaceNames";
    protected static final String ALLOW_CREATING_WORKSPACES = "allowCreatingWorkspaces";
    protected static final String UPDATES_ALLOWED = "updatesAllowed";

    @Description( i18n = JBossCacheConnectorI18n.class, value = "namePropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "namePropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "namePropertyCategory" )
    private volatile String name;

    @Description( i18n = JBossCacheConnectorI18n.class, value = "rootNodeUuidPropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "rootNodeUuidPropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "rootNodeUuidPropertyCategory" )
    private volatile UUID rootNodeUuid = UUID.randomUUID();

    @Description( i18n = JBossCacheConnectorI18n.class, value = "cacheConfigurationNamePropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "cacheConfigurationNamePropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "cacheConfigurationNamePropertyCategory" )
    private volatile String cacheConfigurationName;

    @Description( i18n = JBossCacheConnectorI18n.class, value = "cacheFactoryJndiNamePropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "cacheFactoryJndiNamePropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "cacheFactoryrJndiNamePropertyCategory" )
    private volatile String cacheFactoryJndiName;

    @Description( i18n = JBossCacheConnectorI18n.class, value = "cacheJndiNamePropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "cacheJndiNamePropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "cacheJndiNamePropertyCategory" )
    private volatile String cacheJndiName;

    @Description( i18n = JBossCacheConnectorI18n.class, value = "retryLimitPropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "retryLimitPropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "retryLimitPropertyCategory" )
    private volatile int retryLimit = DEFAULT_RETRY_LIMIT;

    @Description( i18n = JBossCacheConnectorI18n.class, value = "defaultWorkspaceNamePropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "defaultWorkspaceNamePropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "defaultWorkspaceNamePropertyCategory" )
    private volatile String defaultWorkspace;

    @Description( i18n = JBossCacheConnectorI18n.class, value = "predefinedWorkspacesPropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "predefinedWorkspacesPropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "predefinedWorkspacesPropertyCategory" )
    private volatile String[] predefinedWorkspaces = new String[] {};

    @Description( i18n = JBossCacheConnectorI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "updatesAllowedPropertyCategory" )
    private volatile boolean updatesAllowed = DEFAULT_UPDATES_ALLOWED;

    private volatile CachePolicy defaultCachePolicy;
    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(true, true, false, true, false);
    private transient JBossCacheRepository repository;
    private transient Context jndiContext;
    private transient RepositoryContext repositoryContext;
    private final Set<String> repositoryNamesForConfigurationNameProblems = new HashSet<String>();

    /**
     * Create a repository source instance.
     */
    public JBossCacheSource() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public synchronized void setRetryLimit( int limit ) {
        retryLimit = limit < 0 ? 0 : limit;
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
     * Get the name of the default workspace.
     * 
     * @return the name of the workspace that should be used by default; never null
     */
    public String getDefaultWorkspaceName() {
        return defaultWorkspace;
    }

    /**
     * Set the name of the workspace that should be used when clients don't specify a workspace.
     * 
     * @param nameOfDefaultWorkspace the name of the workspace that should be used by default, or null if the
     *        {@link #DEFAULT_NAME_OF_DEFAULT_WORKSPACE default name} should be used
     */
    public synchronized void setDefaultWorkspaceName( String nameOfDefaultWorkspace ) {
        this.defaultWorkspace = nameOfDefaultWorkspace != null ? nameOfDefaultWorkspace : DEFAULT_NAME_OF_DEFAULT_WORKSPACE;
    }

    /**
     * Gets the names of the workspaces that are available when this source is created.
     * 
     * @return the names of the workspaces that this source starts with, or null if there are no such workspaces
     * @see #setPredefinedWorkspaceNames(String[])
     * @see #setCreatingWorkspacesAllowed(boolean)
     */
    public synchronized String[] getPredefinedWorkspaceNames() {
        String[] copy = new String[predefinedWorkspaces.length];
        System.arraycopy(predefinedWorkspaces, 0, copy, 0, predefinedWorkspaces.length);
        return copy;
    }

    /**
     * Sets the names of the workspaces that are available when this source is created.
     * 
     * @param predefinedWorkspaceNames the names of the workspaces that this source should start with, or null if there are no
     *        such workspaces
     * @see #setCreatingWorkspacesAllowed(boolean)
     * @see #getPredefinedWorkspaceNames()
     */
    public synchronized void setPredefinedWorkspaceNames( String[] predefinedWorkspaceNames ) {
        this.predefinedWorkspaces = predefinedWorkspaceNames;
    }

    /**
     * Get whether this source allows workspaces to be created dynamically.
     * 
     * @return true if this source allows workspaces to be created by clients, or false if the
     *         {@link #getPredefinedWorkspaceNames() set of workspaces} is fixed
     * @see #setPredefinedWorkspaceNames(String[])
     * @see #getPredefinedWorkspaceNames()
     * @see #setCreatingWorkspacesAllowed(boolean)
     */
    @Description( i18n = JBossCacheConnectorI18n.class, value = "creatingWorkspacesAllowedPropertyDescription" )
    @Label( i18n = JBossCacheConnectorI18n.class, value = "creatingWorkspacesAllowedPropertyLabel" )
    @Category( i18n = JBossCacheConnectorI18n.class, value = "creatingWorkspacesAllowedPropertyCategory" )
    public boolean isCreatingWorkspacesAllowed() {
        return capabilities.supportsCreatingWorkspaces();
    }

    /**
     * Set whether this source allows workspaces to be created dynamically.
     * 
     * @param allowWorkspaceCreation true if this source allows workspaces to be created by clients, or false if the
     *        {@link #getPredefinedWorkspaceNames() set of workspaces} is fixed
     * @see #setPredefinedWorkspaceNames(String[])
     * @see #getPredefinedWorkspaceNames()
     * @see #isCreatingWorkspacesAllowed()
     */
    public synchronized void setCreatingWorkspacesAllowed( boolean allowWorkspaceCreation ) {
        capabilities = new RepositorySourceCapabilities(true, capabilities.supportsUpdates(), false, allowWorkspaceCreation,
                                                        capabilities.supportsReferences());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    @SuppressWarnings( "unchecked" )
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {
        if (getName() == null) {
            I18n msg = JBossCacheConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }
        if (this.repository == null) {
            Context context = getContext();
            if (context == null) {
                try {
                    context = new InitialContext();
                } catch (NamingException err) {
                    throw new RepositorySourceException(name, err);
                }
            }

            // Look for a cache manager in JNDI ...
            CacheFactory<UUID, JBossCacheNode> cacheFactory = null;
            String jndiName = getCacheFactoryJndiName();
            if (jndiName != null && jndiName.trim().length() != 0) {
                Object object = null;
                try {
                    object = context.lookup(jndiName);
                    if (object != null) cacheFactory = (CacheFactory<UUID, JBossCacheNode>)object;
                } catch (ClassCastException err) {
                    I18n msg = JBossCacheConnectorI18n.objectFoundInJndiWasNotCacheFactory;
                    String className = object != null ? object.getClass().getName() : "null";
                    throw new RepositorySourceException(getName(), msg.text(jndiName, this.getName(), className), err);
                } catch (Throwable err) {
                    if (err instanceof RuntimeException) throw (RuntimeException)err;
                    throw new RepositorySourceException(getName(), err);
                }
            }
            if (cacheFactory == null) cacheFactory = new DefaultCacheFactory<UUID, JBossCacheNode>();

            // Now create the repository ...
            Cache<UUID, JBossCacheNode> cache = createNewCache(cacheFactory, getName());
            this.repository = new JBossCacheRepository(this, cache);
        }

        return new Connection<JBossCacheNode, JBossCacheWorkspace>(this, repository);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public synchronized void close() {
        // Null the reference to the repository; open connections still reference it and can continue to work ...
        this.repository = null;
    }

    /**
     * Method that is responsible for attempting to create a new cache given the supplied workspace name. Note that this is
     * probably called at most once for each workspace name (except if this method fails to create a cache for a given workspace
     * name).
     * 
     * @param cacheFactory the cache factory
     * @param repositoryName the name of the repository
     * @return the new cache that corresponds to the workspace name
     */
    @GuardedBy( "writeLock" )
    protected Cache<UUID, JBossCacheNode> createNewCache( CacheFactory<UUID, JBossCacheNode> cacheFactory,
                                                          String repositoryName ) {
        assert repositoryName != null;
        if (cacheFactory == null) return null;

        // Try to create the cache using the workspace name as the configuration ...
        try {
            return cacheFactory.createCache(repositoryName);
        } catch (ConfigurationException error) {
            // The workspace name is probably not the name of a configuration ...
            I18n msg = JBossCacheConnectorI18n.workspaceNameWasNotValidConfiguration;
            Logger.getLogger(getClass()).debug(msg.text(repositoryName, error.getMessage()));
        }

        if (this.cacheConfigurationName != null) {
            // Try to create the cache using the default configuration name ...
            try {
                return cacheFactory.createCache(getCacheConfigurationName());
            } catch (ConfigurationException error) {
                // The default configuration name is not valid ...
                if (this.repositoryNamesForConfigurationNameProblems.add(repositoryName)) {
                    // Log this problem only the first time ...
                    I18n msg = JBossCacheConnectorI18n.defaultCacheFactoryConfigurationNameWasNotValidConfiguration;
                    Logger.getLogger(getClass()).debug(msg.text(repositoryName));
                }
            }
        }

        // Just create a new cache with the default configuration ...
        return cacheFactory.createCache();
    }

    /**
     * @return repositoryContext
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    protected Observer getObserver() {
        return repositoryContext != null ? repositoryContext.getObserver() : null;
    }

    protected Context getContext() {
        return this.jndiContext;
    }

    protected synchronized void setContext( Context context ) {
        this.jndiContext = context;
    }

    public boolean areUpdatesAllowed() {
        return this.updatesAllowed;
    }

    public void setUpdatesAllowed( boolean updatesAllowed ) {
        this.updatesAllowed = updatesAllowed;
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

    @Override
    public int hashCode() {
        return HashCode.compute(getName());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        ref.add(new StringRefAddr(ROOT_NODE_UUID, getRootNodeUuid().toString()));
        ref.add(new StringRefAddr(CACHE_JNDI_NAME, getCacheJndiName()));
        ref.add(new StringRefAddr(CACHE_FACTORY_JNDI_NAME, getCacheFactoryJndiName()));
        ref.add(new StringRefAddr(CACHE_CONFIGURATION_NAME, getCacheConfigurationName()));
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        ref.add(new StringRefAddr(DEFAULT_WORKSPACE, getDefaultWorkspaceName()));
        ref.add(new StringRefAddr(UPDATES_ALLOWED, String.valueOf(areUpdatesAllowed())));
        ref.add(new StringRefAddr(ALLOW_CREATING_WORKSPACES, Boolean.toString(isCreatingWorkspacesAllowed())));
        String[] workspaceNames = getPredefinedWorkspaceNames();
        if (workspaceNames != null && workspaceNames.length != 0) {
            ref.add(new StringRefAddr(PREDEFINED_WORKSPACE_NAMES, StringUtil.combineLines(workspaceNames)));
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
            String cacheJndiName = (String)values.get(CACHE_JNDI_NAME);
            String cacheFactoryJndiName = (String)values.get(CACHE_FACTORY_JNDI_NAME);
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
            JBossCacheSource source = new JBossCacheSource();
            if (sourceName != null) source.setName(sourceName);
            if (rootNodeUuidString != null) source.setRootNodeUuid(rootNodeUuidString);
            if (cacheJndiName != null) source.setCacheJndiName(cacheJndiName);
            if (cacheFactoryJndiName != null) source.setCacheFactoryJndiName(cacheFactoryJndiName);
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
