/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.graph.connector.federation;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.cache.BasicCachePolicy;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;

/**
 * A {@link RepositorySource} for a federated repository.
 */
@ThreadSafe
public class FederatedRepositorySource implements RepositorySource, ObjectFactory {

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String RETRY_LIMIT = "retryLimit";

    private static final long serialVersionUID = 1L;

    @Description( i18n = GraphI18n.class, value = "namePropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "namePropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "namePropertyCategory" )
    private volatile String name;

    @Description( i18n = GraphI18n.class, value = "retryLimitPropertyDescription" )
    @Label( i18n = GraphI18n.class, value = "retryLimitPropertyLabel" )
    @Category( i18n = GraphI18n.class, value = "retryLimitPropertyCategory" )
    private volatile int retryLimit;
    private volatile RepositorySourceCapabilities capabilities = new RepositorySourceCapabilities(true, true, false, false, true);
    private volatile transient FederatedRepository configuration;
    private volatile transient RepositoryContext context;

    /**
     * Construct a new instance of a {@link RepositorySource} for a federated repository.
     */
    public FederatedRepositorySource() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public synchronized void setName( String name ) {
        if (this.name == name || this.name != null && this.name.equals(name)) return; // unchanged
        this.name = name;
        changeConfiguration();
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
        changeConfiguration();
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
     * @see org.modeshape.graph.connector.RepositorySource#initialize(org.modeshape.graph.connector.RepositoryContext)
     */
    public synchronized void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.context = context;
        changeConfiguration();
    }

    /**
     * Get the repository context that was used to {@link #initialize(RepositoryContext) initialize} this source.
     * 
     * @return the context, or null if the source was not yet {@link #initialize(RepositoryContext) initialized}
     */
    /*package*/RepositoryContext getRepositoryContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        FederatedRepository config = this.configuration;
        if (config == null) {
            synchronized (this) {
                if (this.configuration == null) {
                    // Check all the properties of this source ...
                    String name = getName();
                    if (name == null) {
                        I18n msg = GraphI18n.namePropertyIsRequiredForFederatedRepositorySource;
                        throw new RepositorySourceException(getName(), msg.text("name"));
                    }
                    RepositoryContext repositoryContext = getRepositoryContext();
                    if (repositoryContext == null) {
                        I18n msg = GraphI18n.federatedRepositorySourceMustBeInitialized;
                        throw new RepositorySourceException(getName(), msg.text("name", name));
                    }

                    // Load the configuration ...
                    this.configuration = loadRepository(name, repositoryContext);
                }
                config = this.configuration;
            }
        }
        Observer observer = this.context != null ? this.context.getObserver() : null;
        return new FederatedRepositoryConnection(config, observer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.RepositorySource#close()
     */
    public void close() {
        synchronized (this) {
            if (this.configuration != null) {
                // Release the configuration ...
                if (this.configuration.getExecutor() != null) {
                    this.configuration.getExecutor().shutdown();
                }
                this.configuration = null;
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.naming.Referenceable#getReference()
     */
    public Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        return ref;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.naming.spi.ObjectFactory#getObjectInstance(java.lang.Object, javax.naming.Name, javax.naming.Context,
     *      java.util.Hashtable)
     */
    public Object getObjectInstance( Object obj,
                                     Name name,
                                     Context nameCtx,
                                     Hashtable<?, ?> environment ) throws Exception {
        if (obj instanceof Reference) {
            Map<String, String> values = new HashMap<String, String>();
            Reference ref = (Reference)obj;
            Enumeration<?> en = ref.getAll();
            while (en.hasMoreElements()) {
                RefAddr subref = (RefAddr)en.nextElement();
                if (subref instanceof StringRefAddr) {
                    String key = subref.getType();
                    Object value = subref.getContent();
                    if (value != null) values.put(key, value.toString());
                }
            }
            String sourceName = values.get(SOURCE_NAME);
            String retryLimit = values.get(RETRY_LIMIT);

            // Create the source instance ...
            FederatedRepositorySource source = new FederatedRepositorySource();
            if (sourceName != null) source.setName(sourceName);
            if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
            return source;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return HashCode.compute(getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof FederatedRepositorySource) {
            FederatedRepositorySource that = (FederatedRepositorySource)obj;
            // The source name must match
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
     * Mark the current configuration (if there is one) as being invalid.
     */
    @GuardedBy( "this" )
    protected void changeConfiguration() {
        this.configuration = null;
    }

    /**
     * Utility to load the current configuration for this source from the {@link RepositoryContext#getConfiguration(int)
     * configuration repository}. This method may only be called after the source is {@link #initialize(RepositoryContext)
     * initialized}.
     * 
     * @param name the name of the source; may not be null
     * @param repositoryContext the repository context; may not be null
     * @return the configuration; never null
     * @throws RepositorySourceException if there is a problem with the configuration
     */
    protected FederatedRepository loadRepository( String name,
                                                  RepositoryContext repositoryContext ) throws RepositorySourceException {
        // All the required properties have been set ...
        ExecutionContext executionContext = repositoryContext.getExecutionContext();
        RepositoryConnectionFactory connectionFactory = repositoryContext.getRepositoryConnectionFactory();
        ValueFactories valueFactories = executionContext.getValueFactories();
        ValueFactory<String> strings = valueFactories.getStringFactory();
        ValueFactory<Long> longs = valueFactories.getLongFactory();
        ProjectionParser projectionParser = ProjectionParser.getInstance();
        NamespaceRegistry registry = executionContext.getNamespaceRegistry();

        try {
            // Read the configuration for the federated repository:
            // Level 1: the node representing the federated repository
            // Level 2: the "dna:workspaces" node
            // Level 3: a node for each workspace in the federated repository
            // Level 4: the "dna:projections" nodes
            // Level 5: a node below "dna:projections" for each projection, with properties for the source name,
            // workspace name, cache expiration time, and projection rules
            Subgraph repositories = repositoryContext.getConfiguration(5);

            // Get the name of the default workspace ...
            String defaultWorkspaceName = null;
            Property defaultWorkspaceNameProperty = repositories.getRoot().getProperty(ModeShapeLexicon.DEFAULT_WORKSPACE_NAME);
            if (defaultWorkspaceNameProperty != null) {
                // Set the name using the property if there is one ...
                defaultWorkspaceName = strings.create(defaultWorkspaceNameProperty.getFirstValue());
            }

            // Get the default expiration time for the repository ...
            CachePolicy defaultCachePolicy = null;
            Property timeToExpire = repositories.getRoot().getProperty(ModeShapeLexicon.TIME_TO_EXPIRE);
            if (timeToExpire != null && !timeToExpire.isEmpty()) {
                long timeToCacheInMillis = longs.create(timeToExpire.getFirstValue());
                defaultCachePolicy = new BasicCachePolicy(timeToCacheInMillis, TimeUnit.MILLISECONDS).getUnmodifiable();
            }

            // Level 2: The "dna:workspaces" node ...
            Node workspacesNode = repositories.getNode(ModeShapeLexicon.WORKSPACES);
            if (workspacesNode == null) {
                I18n msg = GraphI18n.requiredNodeDoesNotExistRelativeToNode;
                throw new RepositorySourceException(msg.text(ModeShapeLexicon.WORKSPACES.getString(registry),
                                                             repositories.getLocation().getPath().getString(registry),
                                                             repositories.getGraph().getCurrentWorkspaceName(),
                                                             repositories.getGraph().getSourceName()));
            }

            // Level 3: The workspace nodes ...
            LinkedList<FederatedWorkspace> workspaces = new LinkedList<FederatedWorkspace>();
            for (Location workspace : workspacesNode) {

                // Get the name of the workspace ...
                String workspaceName = null;
                SubgraphNode workspaceNode = repositories.getNode(workspace);
                Property workspaceNameProperty = workspaceNode.getProperty(ModeShapeLexicon.WORKSPACE_NAME);
                if (workspaceNameProperty != null) {
                    // Set the name using the property if there is one ...
                    workspaceName = strings.create(workspaceNameProperty.getFirstValue());
                }
                if (workspaceName == null) {
                    // Otherwise, set the name using the local name of the workspace node ...
                    workspaceName = workspace.getPath().getLastSegment().getName().getLocalName();
                }

                // Level 4: the "dna:projections" node ...
                Node projectionsNode = workspaceNode.getNode(ModeShapeLexicon.PROJECTIONS);
                if (projectionsNode == null) {
                    I18n msg = GraphI18n.requiredNodeDoesNotExistRelativeToNode;
                    throw new RepositorySourceException(getName(), msg.text(ModeShapeLexicon.PROJECTIONS.getString(registry),
                                                                            workspaceNode.getLocation()
                                                                                         .getPath()
                                                                                         .getString(registry),
                                                                            repositories.getGraph().getCurrentWorkspaceName(),
                                                                            repositories.getGraph().getSourceName()));
                }

                // Level 5: the projection nodes ...
                List<Projection> sourceProjections = new LinkedList<Projection>();
                for (Location projection : projectionsNode) {
                    Node projectionNode = repositories.getNode(projection);
                    sourceProjections.add(createProjection(executionContext, projectionParser, projectionNode));
                }

                // Create the federated workspace configuration ...
                FederatedWorkspace space = new FederatedWorkspace(repositoryContext, name, workspaceName, sourceProjections,
                                                                  defaultCachePolicy);
                if (workspaceName.equals(defaultWorkspaceName)) {
                    workspaces.addFirst(space);
                } else {
                    workspaces.add(space);
                }
            }

            // Create the ExecutorService ...
            ExecutorService executor = Executors.newCachedThreadPool(new NamedThreadFactory(name));

            return new FederatedRepository(name, connectionFactory, workspaces, defaultCachePolicy, executor);
        } catch (RepositorySourceException t) {
            throw t; // rethrow
        } catch (Throwable t) {
            I18n msg = GraphI18n.errorReadingConfigurationForFederatedRepositorySource;
            throw new RepositorySourceException(getName(), msg.text(name), t);
        }
    }

    /**
     * Add a federated workspace to this source. If a workspace with the supplied name already exists, it will be replaced with
     * the new one.
     * 
     * @param workspaceName the name of the new federated workspace
     * @param projections the projections that should be used in the workspace
     * @param isDefault true if this workspace should be used as the default workspace, or false otherwise
     * @return the federated workspace
     * @throws IllegalArgumentException if the workspace name or the projections reference are null
     */
    public synchronized FederatedWorkspace addWorkspace( String workspaceName,
                                                         Iterable<Projection> projections,
                                                         boolean isDefault ) {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(projections, "projections");

        // Check all the properties of this source ...
        String name = getName();
        if (name == null) {
            I18n msg = GraphI18n.namePropertyIsRequiredForFederatedRepositorySource;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }
        RepositoryContext context = getRepositoryContext();
        if (context == null) {
            I18n msg = GraphI18n.federatedRepositorySourceMustBeInitialized;
            throw new RepositorySourceException(getName(), msg.text("name", name));
        }

        // Now set up or get the existing components needed by the workspace ...
        RepositoryConnectionFactory connectionFactory = null;
        ExecutorService executor = null;
        LinkedList<FederatedWorkspace> workspaces = new LinkedList<FederatedWorkspace>();
        CachePolicy defaultCachePolicy = null;
        if (this.configuration != null) {
            connectionFactory = this.configuration.getConnectionFactory();
            executor = this.configuration.getExecutor();
            defaultCachePolicy = this.configuration.getDefaultCachePolicy();
            for (String existingWorkspaceName : this.configuration.getWorkspaceNames()) {
                if (existingWorkspaceName.equals(workspaceName)) continue;
                workspaces.add(this.configuration.getWorkspace(existingWorkspaceName));
            }
        } else {
            connectionFactory = context.getRepositoryConnectionFactory();
            executor = Executors.newCachedThreadPool(new NamedThreadFactory(name));
        }

        // Add the new workspace ...
        FederatedWorkspace newWorkspace = new FederatedWorkspace(context, name, workspaceName, projections, defaultCachePolicy);
        if (isDefault) {
            workspaces.addFirst(newWorkspace);
        } else {
            workspaces.add(newWorkspace);
        }
        // Update the configuration ...
        this.configuration = new FederatedRepository(name, connectionFactory, workspaces, defaultCachePolicy, executor);
        return newWorkspace;
    }

    /**
     * Remove the named workspace from the repository source.
     * 
     * @param workspaceName the name of the workspace to remove
     * @return true if the workspace was removed, or false otherwise
     * @throws IllegalArgumentException if the workspace name is null
     */
    public synchronized boolean removeWorkspace( String workspaceName ) {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        if (this.configuration == null) return false;
        FederatedWorkspace workspace = this.configuration.getWorkspace(workspaceName);
        if (workspace == null) return false;
        List<FederatedWorkspace> workspaces = new LinkedList<FederatedWorkspace>();
        for (String existingWorkspaceName : this.configuration.getWorkspaceNames()) {
            if (existingWorkspaceName.equals(workspaceName)) continue;
            workspaces.add(this.configuration.getWorkspace(existingWorkspaceName));
        }
        RepositoryConnectionFactory connectionFactory = this.configuration.getConnectionFactory();
        ExecutorService executor = this.configuration.getExecutor();
        CachePolicy defaultCachePolicy = this.configuration.getDefaultCachePolicy();
        this.configuration = new FederatedRepository(name, connectionFactory, workspaces, defaultCachePolicy, executor);
        return true;
    }

    public synchronized boolean hasWorkspace( String workspaceName ) {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        return this.configuration != null && this.configuration.getWorkspaceNames().contains(workspaceName);
    }

    /**
     * Instantiate the {@link Projection} described by the supplied properties.
     * 
     * @param context the execution context that should be used to read the configuration; may not be null
     * @param projectionParser the projection rule parser that should be used; may not be null
     * @param node the node where these properties were found; never null
     * @return the region instance, or null if it could not be created
     */
    protected Projection createProjection( ExecutionContext context,
                                           ProjectionParser projectionParser,
                                           Node node ) {
        ValueFactory<String> strings = context.getValueFactories().getStringFactory();

        Path path = node.getLocation().getPath();

        // Get the source name from the local name of the node ...
        String sourceName = path.getLastSegment().getName().getLocalName();
        Property sourceNameProperty = node.getProperty(ModeShapeLexicon.SOURCE_NAME);
        if (sourceNameProperty != null && !sourceNameProperty.isEmpty()) {
            // There is a "dna:sourceName" property, so use this instead ...
            sourceName = strings.create(sourceNameProperty.getFirstValue());
        }
        assert sourceName != null;

        // Get the workspace name ...
        String workspaceName = null;
        Property workspaceNameProperty = node.getProperty(ModeShapeLexicon.WORKSPACE_NAME);
        if (workspaceNameProperty != null && !workspaceNameProperty.isEmpty()) {
            // There is a "dna:workspaceName" property, so use this instead ...
            workspaceName = strings.create(workspaceNameProperty.getFirstValue());
        }

        // Get the projection rules ...
        Projection.Rule[] projectionRules = null;
        Property projectionRulesProperty = node.getProperty(ModeShapeLexicon.PROJECTION_RULES);
        if (projectionRulesProperty != null && !projectionRulesProperty.isEmpty()) {
            String[] projectionRuleStrs = strings.create(projectionRulesProperty.getValuesAsArray());
            if (projectionRuleStrs != null && projectionRuleStrs.length != 0) {
                projectionRules = projectionParser.rulesFromStrings(context, projectionRuleStrs);
            }
        }

        // Is this projection read-only?
        boolean readOnly = false;
        Property readOnlyProperty = node.getProperty(ModeShapeLexicon.READ_ONLY);
        if (readOnlyProperty != null && !readOnlyProperty.isEmpty()) {
            readOnly = context.getValueFactories().getBooleanFactory().create(readOnlyProperty.getFirstValue());
        }

        return new Projection(sourceName, workspaceName, readOnly, projectionRules);
    }

}
