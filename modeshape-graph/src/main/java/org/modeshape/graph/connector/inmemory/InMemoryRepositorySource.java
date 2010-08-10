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
package org.modeshape.graph.connector.inmemory;

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
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.annotation.ReadOnly;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.BaseRepositorySource;
import org.modeshape.graph.connector.base.Connection;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

/**
 * A {@link RepositorySource} for an in-memory repository. Each {@link InMemoryRepositorySource} instance contains its own
 * repository, and the lifetime of the source dictates the lifetime of the repository and its content.
 */
@ThreadSafe
public class InMemoryRepositorySource implements BaseRepositorySource, ObjectFactory {

    /**
     * The initial version is 1
     */
    private static final long serialVersionUID = 1L;

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    /**
     * The default name for the workspace used by this source, which is a blank string.
     */
    public static final String DEFAULT_WORKSPACE_NAME = "";

    protected static final RepositorySourceCapabilities CAPABILITIES = new RepositorySourceCapabilities(true, true, false, true,
                                                                                                        true);

    protected static final String ROOT_NODE_UUID_ATTR = "rootNodeUuid";
    protected static final String SOURCE_NAME_ATTR = "sourceName";
    protected static final String PREDEFINED_WORKSPACE_NAMES = "predefinedWorkspaceNames";
    protected static final String DEFAULT_WORKSPACE_NAME_ATTR = "defaultWorkspaceName";
    protected static final String DEFAULT_CACHE_POLICY_ATTR = "defaultCachePolicy";
    protected static final String JNDI_NAME_ATTR = "jndiName";
    protected static final String RETRY_LIMIT_ATTR = "retryLimit";

    @Description( i18n = GraphI18n.class, value = "namePropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "namePropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "namePropertyCategory" )
    @GuardedBy( "sourcesLock" )
    private String name;

    @Description( i18n = GraphI18n.class, value = "jndiNamePropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "jndiNamePropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "jndiNamePropertyCategory" )
    @GuardedBy( "this" )
    private String jndiName;

    @Description( i18n = GraphI18n.class, value = "defaultWorkspaceNamePropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "defaultWorkspaceNamePropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "defaultWorkspaceNamePropertyCategory" )
    private String defaultWorkspaceName = DEFAULT_WORKSPACE_NAME;

    @Description( i18n = GraphI18n.class, value = "rootNodeUuidPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "rootNodeUuidPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "rootNodeUuidPropertyCategory" )
    private UUID rootNodeUuid = UUID.randomUUID();

    @Description( i18n = GraphI18n.class, value = "predefinedWorkspacesPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "predefinedWorkspacesPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "predefinedWorkspacesPropertyCategory" )
    private volatile String[] predefinedWorkspaces = new String[] {};

    @Description( i18n = GraphI18n.class, value = "retryLimitPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "retryLimitPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "retryLimitPropertyCategory" )
    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);

    private CachePolicy defaultCachePolicy;
    private transient InMemoryRepository repository;
    private transient ExecutionContext defaultContext = new ExecutionContext();
    private transient RepositoryContext repositoryContext = new DefaultRepositoryContext();

    protected class DefaultRepositoryContext implements RepositoryContext {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getExecutionContext()
         */
        @SuppressWarnings( "synthetic-access" )
        public ExecutionContext getExecutionContext() {
            return defaultContext;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getConfiguration(int)
         */
        public Subgraph getConfiguration( int depth ) {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getObserver()
         */
        public Observer getObserver() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.connector.RepositoryContext#getRepositoryConnectionFactory()
         */
        public RepositoryConnectionFactory getRepositoryConnectionFactory() {
            return null;
        }
    }

    /**
     * Create a repository source instance.
     */
    public InMemoryRepositorySource() {
        super();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context != null ? context : new DefaultRepositoryContext();
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
     * @see org.modeshape.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#setRetryLimit(int)
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
     * Get the name of the workspace that should be used by default.
     * 
     * @return the name of the default workspace
     */
    public String getDefaultWorkspaceName() {
        return defaultWorkspaceName;
    }

    /**
     * Set the default workspace name.
     * 
     * @param defaultWorkspaceName the name of the workspace that should be used by default, or null if "" should be used
     */
    public void setDefaultWorkspaceName( String defaultWorkspaceName ) {
        this.defaultWorkspaceName = defaultWorkspaceName != null ? defaultWorkspaceName : DEFAULT_WORKSPACE_NAME;
    }

    /**
     * Get the string representation of the UUID for the root node.
     * 
     * @return the root node's UUID as a string; may not be null
     */
    public String getRootNodeUuid() {
        return this.rootNodeUuid.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.base.BaseRepositorySource#getRootNodeUuidObject()
     */
    public UUID getRootNodeUuidObject() {
        return this.rootNodeUuid;
    }

    /**
     * @param rootNodeUuid Sets rootNodeUuid to the specified value.
     */
    public void setRootNodeUuid( UUID rootNodeUuid ) {
        this.rootNodeUuid = rootNodeUuid != null ? rootNodeUuid : UUID.randomUUID();
    }

    /**
     * @param rootNodeUuid Sets rootNodeUuid to the specified value.
     */
    public void setRootNodeUuid( String rootNodeUuid ) {
        this.rootNodeUuid = rootNodeUuid != null ? UUID.fromString(rootNodeUuid) : UUID.randomUUID();
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
        CheckArg.isNotNull(name, "name");
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
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public synchronized RepositoryConnection getConnection() throws RepositorySourceException {
        if (repository == null) {
            repository = new InMemoryRepository(this);

            ExecutionContext context = repositoryContext != null ? repositoryContext.getExecutionContext() : defaultContext;
            InMemoryTransaction txn = repository.startTransaction(context, false);
            try {
                // Create the set of initial workspaces ...
                for (String initialName : getPredefinedWorkspaceNames()) {
                    repository.createWorkspace(txn, initialName, CreateConflictBehavior.DO_NOT_CREATE, null);
                }
            } finally {
                txn.commit();
            }

        }
        return new Connection<InMemoryNode, InMemoryWorkspace>(this, repository);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public synchronized void close() {
        // Null the reference to the in-memory repository; open connections still reference it and can continue to work ...
        this.repository = null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        if (getName() != null) {
            ref.add(new StringRefAddr(SOURCE_NAME_ATTR, getName()));
        }
        if (getRootNodeUuid() != null) {
            ref.add(new StringRefAddr(ROOT_NODE_UUID_ATTR, getRootNodeUuid().toString()));
        }
        if (getJndiName() != null) {
            ref.add(new StringRefAddr(JNDI_NAME_ATTR, getJndiName()));
        }
        if (getDefaultWorkspaceName() != null) {
            ref.add(new StringRefAddr(DEFAULT_WORKSPACE_NAME_ATTR, getDefaultWorkspaceName()));
        }
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
                ref.add(new BinaryRefAddr(DEFAULT_CACHE_POLICY_ATTR, baos.toByteArray()));
            } catch (IOException e) {
                I18n msg = GraphI18n.errorSerializingInMemoryCachePolicyInSource;
                throw new RepositorySourceException(getName(), msg.text(policy.getClass().getName(), getName()), e);
            }
        }
        ref.add(new StringRefAddr(RETRY_LIMIT_ATTR, Integer.toString(getRetryLimit())));
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
            String sourceName = (String)values.get(SOURCE_NAME_ATTR);
            String rootNodeUuidString = (String)values.get(ROOT_NODE_UUID_ATTR);
            String jndiName = (String)values.get(JNDI_NAME_ATTR);
            String defaultWorkspaceName = (String)values.get(DEFAULT_WORKSPACE_NAME_ATTR);
            Object defaultCachePolicy = values.get(DEFAULT_CACHE_POLICY_ATTR);
            String retryLimit = (String)values.get(RETRY_LIMIT_ATTR);

            String combinedWorkspaceNames = (String)values.get(PREDEFINED_WORKSPACE_NAMES);
            String[] workspaceNames = null;
            if (combinedWorkspaceNames != null) {
                List<String> paths = StringUtil.splitLines(combinedWorkspaceNames);
                workspaceNames = paths.toArray(new String[paths.size()]);
            }

            // Create the source instance ...
            InMemoryRepositorySource source = new InMemoryRepositorySource();
            if (sourceName != null) source.setName(sourceName);
            if (rootNodeUuidString != null) source.setRootNodeUuid(UUID.fromString(rootNodeUuidString));
            if (defaultWorkspaceName != null) source.setDefaultWorkspaceName(defaultWorkspaceName);
            if (jndiName != null) source.setJndiName(jndiName);
            if (defaultCachePolicy instanceof CachePolicy) {
                source.setDefaultCachePolicy((CachePolicy)defaultCachePolicy);
            }
            if (workspaceNames != null && workspaceNames.length != 0) source.setPredefinedWorkspaceNames(workspaceNames);
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            return source;
        }
        return null;
    }

    /**
     * Gets the names of the workspaces that are available when this source is created.
     * 
     * @return the names of the workspaces that this source starts with, or null if there are no such workspaces
     * @see #setPredefinedWorkspaceNames(String[])
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
     * @see #getPredefinedWorkspaceNames()
     */
    public synchronized void setPredefinedWorkspaceNames( String[] predefinedWorkspaceNames ) {
        this.predefinedWorkspaces = predefinedWorkspaceNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Description( i18n = GraphI18n.class, value = "updatesAllowedPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "updatesAllowedPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "updatesAllowedPropertyCategory" )
    @ReadOnly
    public boolean areUpdatesAllowed() {
        return true;
    }

    /**
     * In-memory connectors aren't shared and cannot be loaded from external sources if updates are not allowed. Therefore, in
     * order to avoid setting up an in-memory connector that is permanently empty (presumably, not a desired outcome), all
     * in-memory connectors must allow updates.
     * 
     * @param updatesAllowed must be true
     * @throws RepositorySourceException if {@code updatesAllowed != true}.
     */
    public void setUpdatesAllowed( boolean updatesAllowed ) {
        if (updatesAllowed == false) {
            throw new RepositorySourceException(GraphI18n.inMemoryConnectorMustAllowUpdates.text(this.name));
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "The \"" + name + "\" in-memory repository";
    }
}
