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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.UUID;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.ThreadSafe;
import org.infinispan.manager.CacheContainer;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.BaseRepositorySource;
import org.modeshape.graph.connector.base.Connection;
import org.modeshape.graph.observe.Observer;

/**
 * A repository source that uses an Infinispan instance to manage the content. This source is capable of using an existing
 * {@link CacheContainer} or creating a new cache container. This process is controlled entirely by the JavaBean properties of the
 * InfinispanSource instance.
 */
@ThreadSafe
public abstract class BaseInfinispanSource implements BaseRepositorySource, ObjectFactory {

    private static final long serialVersionUID = 1L;
    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    /**
     * The default limit is {@value} for the root node's UUID.
     */
    public static final String DEFAULT_ROOT_NODE_UUID = "cafebabe-cafe-babe-cafe-babecafebabe";

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
    protected static final String RETRY_LIMIT = "retryLimit";
    protected static final String DEFAULT_WORKSPACE = "defaultWorkspace";
    protected static final String PREDEFINED_WORKSPACE_NAMES = "predefinedWorkspaceNames";
    protected static final String ALLOW_CREATING_WORKSPACES = "allowCreatingWorkspaces";
    protected static final String UPDATES_ALLOWED = "updatesAllowed";

    @Description( i18n = InfinispanConnectorI18n.class, value = "namePropertyDescription" )
    @Label( i18n = InfinispanConnectorI18n.class, value = "namePropertyLabel" )
    @Category( i18n = InfinispanConnectorI18n.class, value = "namePropertyCategory" )
    private volatile String name;

    @Description( i18n = InfinispanConnectorI18n.class, value = "rootNodeUuidPropertyDescription" )
    @Label( i18n = InfinispanConnectorI18n.class, value = "rootNodeUuidPropertyLabel" )
    @Category( i18n = InfinispanConnectorI18n.class, value = "rootNodeUuidPropertyCategory" )
    private volatile UUID rootNodeUuid = UUID.fromString(DEFAULT_ROOT_NODE_UUID);

    @Description( i18n = InfinispanConnectorI18n.class, value = "retryLimitPropertyDescription" )
    @Label( i18n = InfinispanConnectorI18n.class, value = "retryLimitPropertyLabel" )
    @Category( i18n = InfinispanConnectorI18n.class, value = "retryLimitPropertyCategory" )
    private volatile int retryLimit = DEFAULT_RETRY_LIMIT;

    @Description( i18n = InfinispanConnectorI18n.class, value = "defaultWorkspaceNamePropertyDescription" )
    @Label( i18n = InfinispanConnectorI18n.class, value = "defaultWorkspaceNamePropertyLabel" )
    @Category( i18n = InfinispanConnectorI18n.class, value = "defaultWorkspaceNamePropertyCategory" )
    private volatile String defaultWorkspace;

    @Description( i18n = InfinispanConnectorI18n.class, value = "predefinedWorkspacesPropertyDescription" )
    @Label( i18n = InfinispanConnectorI18n.class, value = "predefinedWorkspacesPropertyLabel" )
    @Category( i18n = InfinispanConnectorI18n.class, value = "predefinedWorkspacesPropertyCategory" )
    private volatile String[] predefinedWorkspaces = new String[] {};

    @Description( i18n = InfinispanConnectorI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = InfinispanConnectorI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = InfinispanConnectorI18n.class, value = "updatesAllowedPropertyCategory" )
    private volatile boolean updatesAllowed = DEFAULT_UPDATES_ALLOWED;

    private volatile CachePolicy defaultCachePolicy;
    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(true, true, false, true, false);
    private transient InfinispanRepository repository;
    private transient Context jndiContext;
    private transient RepositoryContext repositoryContext;

    /**
     * Create a repository source instance.
     */
    public BaseInfinispanSource() {
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
        if (predefinedWorkspaceNames != null && predefinedWorkspaceNames.length == 1) {
            predefinedWorkspaceNames = predefinedWorkspaceNames[0].split("\\s*,\\s*");
        }
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
     * Method controls how a cache container gets created.
     * 
     * @return an appropriate CacheContainer per implementation.
     */
    protected abstract CacheContainer createCacheContainer();

    final CacheContainer cacheContainer() {
        return repository.getCacheContainer();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public synchronized void close() {
        if (this.repository != null) {
            try {
                this.repository.shutdown();
            } finally {
                this.repository = null;
            }
        }
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

    protected synchronized Context getContext() {
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
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {
        if (getName() == null) {
            I18n msg = InfinispanConnectorI18n.propertyIsRequired;
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
            CacheContainer cacheContainer = createCacheContainer();

            // Now create the repository ...
            this.repository = new InfinispanRepository(this, cacheContainer);
        }

        return new Connection<InfinispanNode, InfinispanWorkspace>(this, repository);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Reference getReference() {
        String className = getClass().getName();
        String managerClassName = this.getClass().getName();
        Reference ref = new Reference(className, managerClassName, null);

        ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        ref.add(new StringRefAddr(ROOT_NODE_UUID, getRootNodeUuid().toString()));
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
                I18n msg = InfinispanConnectorI18n.errorSerializingCachePolicyInSource;
                throw new RepositorySourceException(getName(), msg.text(policy.getClass().getName(), getName()), e);
            }
        }
        return ref;
    }
}
