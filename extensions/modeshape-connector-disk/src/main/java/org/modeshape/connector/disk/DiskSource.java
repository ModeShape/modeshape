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
package org.modeshape.connector.disk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.StringUtil;
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
 * A repository source that uses a uses a disk to store arbitrary content. Unlike the {@code FileSystemSource}, this connector can
 * store arbitrary content and is not limited to storing nodes of type {@code nt:folder, nt:file, and nt:resource}. However,
 * content stored by this connector is not intended to be accessible to other applications unless they integrate with ModeShape to
 * read the data.
 * <p>
 * Nodes created by this source are assigned a UUID and mapped to a system of folders and subfolders based on this UUID.
 * </p>
 * <p>
 * Like other {@link RepositorySource} classes, instances of DiskSource can be placed into JNDI and do support the creation of
 * {@link Referenceable JNDI referenceable} objects and resolution of references into DiskSource.
 * </p>
 */
@ThreadSafe
public class DiskSource implements BaseRepositorySource, ObjectFactory {

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

    /**
     * The initial value for where content should be stored on disk is "{@value} ", unless otherwise specified.
     */
    public static final String DEFAULT_REPOSITORY_ROOT_PATH = "/tmp";

    private static final String ROOT_NODE_UUID = "rootNodeUuid";
    private static final String SOURCE_NAME = "sourceName";
    private static final String DEFAULT_CACHE_POLICY = "defaultCachePolicy";
    private static final String RETRY_LIMIT = "retryLimit";
    private static final String DEFAULT_WORKSPACE = "defaultWorkspace";
    private static final String PREDEFINED_WORKSPACE_NAMES = "predefinedWorkspaceNames";
    private static final String ALLOW_CREATING_WORKSPACES = "allowCreatingWorkspaces";
    private static final String UPDATES_ALLOWED = "updatesAllowed";
    private static final String REPOSITORY_ROOT_PATH = "repositoryRootPath";

    @Description( i18n = DiskConnectorI18n.class, value = "namePropertyDescription" )
    @Label( i18n = DiskConnectorI18n.class, value = "namePropertyLabel" )
    @Category( i18n = DiskConnectorI18n.class, value = "namePropertyCategory" )
    private volatile String name;

    @Description( i18n = DiskConnectorI18n.class, value = "rootNodeUuidPropertyDescription" )
    @Label( i18n = DiskConnectorI18n.class, value = "rootNodeUuidPropertyLabel" )
    @Category( i18n = DiskConnectorI18n.class, value = "rootNodeUuidPropertyCategory" )
    private volatile UUID rootNodeUuid = UUID.fromString(DEFAULT_ROOT_NODE_UUID);

    @Description( i18n = DiskConnectorI18n.class, value = "retryLimitPropertyDescription" )
    @Label( i18n = DiskConnectorI18n.class, value = "retryLimitPropertyLabel" )
    @Category( i18n = DiskConnectorI18n.class, value = "retryLimitPropertyCategory" )
    private volatile int retryLimit = DEFAULT_RETRY_LIMIT;

    @Description( i18n = DiskConnectorI18n.class, value = "defaultWorkspaceNamePropertyDescription" )
    @Label( i18n = DiskConnectorI18n.class, value = "defaultWorkspaceNamePropertyLabel" )
    @Category( i18n = DiskConnectorI18n.class, value = "defaultWorkspaceNamePropertyCategory" )
    private volatile String defaultWorkspace;

    @Description( i18n = DiskConnectorI18n.class, value = "predefinedWorkspaceNamesPropertyDescription" )
    @Label( i18n = DiskConnectorI18n.class, value = "predefinedWorkspaceNamesPropertyLabel" )
    @Category( i18n = DiskConnectorI18n.class, value = "predefinedWorkspaceNamesPropertyCategory" )
    private volatile String[] predefinedWorkspaces = new String[] {};

    @Description( i18n = DiskConnectorI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = DiskConnectorI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = DiskConnectorI18n.class, value = "updatesAllowedPropertyCategory" )
    private volatile boolean updatesAllowed = DEFAULT_UPDATES_ALLOWED;

    @Description( i18n = DiskConnectorI18n.class, value = "repositoryRootPathPropertyDescription" )
    @Label( i18n = DiskConnectorI18n.class, value = "repositoryRootPathPropertyLabel" )
    @Category( i18n = DiskConnectorI18n.class, value = "repositoryRootPathPropertyCategory" )
    private volatile String repositoryRootPath = DEFAULT_REPOSITORY_ROOT_PATH;

    private volatile CachePolicy defaultCachePolicy;
    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(true, true, false, true, true);
    private transient DiskRepository repository;
    private transient Context jndiContext;
    private transient RepositoryContext repositoryContext;

    /**
     * Create a repository source instance.
     */
    public DiskSource() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    @Override
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @return the path to the root of the repository on disk. This path must be (at least) readable.
     */
    public String getRepositoryRootPath() {
        return this.repositoryRootPath;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getCapabilities()
     */
    @Override
    public RepositorySourceCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * Get the default cache policy for this source, or null if the global default cache policy should be used
     * 
     * @return the default cache policy, or null if this source has no explicit default cache policy
     */
    @Override
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
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    @Override
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int)
     */
    @Override
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
     * Set the the path to the root of the repository on disk. This path must be (at least) readable.
     * 
     * @param repositoryRootPath the path to the root of the repository on disk. This path must be (at least) readable.
     */
    public synchronized void setRepositoryRootPath( String repositoryRootPath ) {
        if (repositoryRootPath == null) repositoryRootPath = DEFAULT_REPOSITORY_ROOT_PATH;
        this.repositoryRootPath = repositoryRootPath;
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
    @Override
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
    @Override
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
    @Override
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
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    @Override
    public synchronized void close() {
    }

    /**
     * @return repositoryContext
     */
    @Override
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

    @Override
    public boolean areUpdatesAllowed() {
        return this.updatesAllowed;
    }

    @Override
    public void setUpdatesAllowed( boolean updatesAllowed ) {
        this.updatesAllowed = updatesAllowed;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    @Override
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {
        if (getName() == null) {
            I18n msg = DiskConnectorI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }

        if (this.repository == null) {
            this.repository = new DiskRepository(this);
        }

        return new Connection<DiskNode, DiskWorkspace>(this, this.repository);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Reference getReference() {
        String className = getClass().getName();
        String managerClassName = this.getClass().getName();
        Reference ref = new Reference(className, managerClassName, null);

        ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        ref.add(new StringRefAddr(ROOT_NODE_UUID, getRootNodeUuid().toString()));
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        ref.add(new StringRefAddr(DEFAULT_WORKSPACE, getDefaultWorkspaceName()));
        ref.add(new StringRefAddr(UPDATES_ALLOWED, String.valueOf(areUpdatesAllowed())));
        ref.add(new StringRefAddr(REPOSITORY_ROOT_PATH, String.valueOf(getRepositoryRootPath())));
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
                I18n msg = DiskConnectorI18n.errorSerializingCachePolicyInSource;
                throw new RepositorySourceException(getName(), msg.text(policy.getClass().getName(), getName()), e);
            }
        }
        return ref;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof DiskSource) {
            DiskSource that = (DiskSource)obj;
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
            Object defaultCachePolicy = values.get(DEFAULT_CACHE_POLICY);
            String retryLimit = (String)values.get(RETRY_LIMIT);
            String defaultWorkspace = (String)values.get(DEFAULT_WORKSPACE);
            String createWorkspaces = (String)values.get(ALLOW_CREATING_WORKSPACES);
            String updatesAllowed = (String)values.get(UPDATES_ALLOWED);
            String repositoryRootPath = (String)values.get(REPOSITORY_ROOT_PATH);

            String combinedWorkspaceNames = (String)values.get(PREDEFINED_WORKSPACE_NAMES);
            String[] workspaceNames = null;
            if (combinedWorkspaceNames != null) {
                List<String> paths = StringUtil.splitLines(combinedWorkspaceNames);
                workspaceNames = paths.toArray(new String[paths.size()]);
            }

            // Create the source instance ...
            DiskSource source = new DiskSource();
            if (sourceName != null) source.setName(sourceName);
            if (rootNodeUuidString != null) source.setRootNodeUuid(rootNodeUuidString);
            if (defaultCachePolicy instanceof CachePolicy) source.setDefaultCachePolicy((CachePolicy)defaultCachePolicy);
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            if (defaultWorkspace != null) source.setDefaultWorkspaceName(defaultWorkspace);
            if (createWorkspaces != null) source.setCreatingWorkspacesAllowed(Boolean.parseBoolean(createWorkspaces));
            if (workspaceNames != null && workspaceNames.length != 0) source.setPredefinedWorkspaceNames(workspaceNames);
            if (updatesAllowed != null) source.setUpdatesAllowed(Boolean.valueOf(updatesAllowed));
            if (repositoryRootPath != null) source.setRepositoryRootPath(repositoryRootPath);
            return source;
        }
        return null;
    }
}
