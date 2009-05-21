/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connector.federation;

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
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.SubgraphNode;
import org.jboss.dna.graph.cache.BasicCachePolicy;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceCapabilities;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;

/**
 * 
 */
public class FederatedRepositorySource implements RepositorySource, ObjectFactory {

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    /**
     * The default path to the node defining the configuration for a source.
     */
    public static final String DEFAULT_CONFIGURATION_PATH = "/";

    protected static final String SOURCE_NAME = "sourceName";
    protected static final String RETRY_LIMIT = "retryLimit";
    protected static final String CONFIGURATION_SOURCE_NAME = "configurationSourceName";
    protected static final String CONFIGURATION_PATH = "configurationPath";
    protected static final String CONFIGURATION_WORKSPACE_NAME = "configurationWorkspaceName";

    private static final long serialVersionUID = 1L;

    private volatile String name;
    private volatile int retryLimit;
    private volatile String configurationSourceName;
    private volatile String configurationWorkspaceName;
    private volatile String configurationPath = DEFAULT_CONFIGURATION_PATH;
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
     * @see org.jboss.dna.graph.connector.RepositorySource#getName()
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
     * @see org.jboss.dna.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public synchronized void setRetryLimit( int limit ) {
        retryLimit = limit < 0 ? 0 : limit;
        changeConfiguration();
    }

    /**
     * Get the name of the {@link RepositorySource} that should be used to create the {@link FederatedRepository federated
     * repository configuration} as the configuration repository.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @return the name of the {@link RepositorySource} instance that should be used for the configuration
     * @see #setConfigurationSourceName(String)
     * @see #getConfigurationWorkspaceName()
     * @see #getConfigurationPath()
     */
    public String getConfigurationSourceName() {
        return configurationSourceName;
    }

    /**
     * Set the name of the {@link RepositorySource} that should be used to create the {@link FederatedRepository federated
     * repository configuration} as the configuration repository. The instance will be retrieved from the
     * {@link RepositoryConnectionFactory} instance inside the {@link RepositoryContext#getRepositoryConnectionFactory()
     * repository context} supplied during {@link RepositorySource#initialize(RepositoryContext) initialization}.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @param sourceName the name of the {@link RepositorySource} instance that should be used for the configuration, or null if
     *        the federated repository instance is to be found in JNDI
     * @see #getConfigurationSourceName()
     * @see #setConfigurationPath(String)
     * @see #setConfigurationWorkspaceName(String)
     */
    public synchronized void setConfigurationSourceName( String sourceName ) {
        if (this.configurationSourceName == sourceName || this.configurationSourceName != null
            && this.configurationSourceName.equals(sourceName)) return; // unchanged
        this.configurationSourceName = sourceName;
        changeConfiguration();
    }

    /**
     * Get the name of the workspace in the {@link #getConfigurationSourceName() source} containing the configuration content for
     * this source. If this workspace name is null, the default workspace as defined by that source will be used.
     * 
     * @return the name of the configuration workspace, or null if the default workspace for the
     *         {@link #getConfigurationSourceName() configuration source} should be used
     * @see #getConfigurationSourceName()
     * @see #setConfigurationWorkspaceName(String)
     * @see #getConfigurationPath()
     */
    public String getConfigurationWorkspaceName() {
        return configurationWorkspaceName;
    }

    /**
     * Set the name of the workspace in the {@link #getConfigurationSourceName() source} containing the configuration content for
     * this source. If this workspace name is null, the default workspace as defined by that source will be used.
     * 
     * @param workspaceName the name of the configuration workspace, or null if the default workspace for the
     *        {@link #getConfigurationSourceName() configuration source} should be used
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationPath(String)
     * @see #getConfigurationWorkspaceName()
     */
    public synchronized void setConfigurationWorkspaceName( String workspaceName ) {
        if (this.configurationWorkspaceName == workspaceName || this.configurationWorkspaceName != null
            && this.configurationWorkspaceName.equals(workspaceName)) return; // unchanged
        this.configurationWorkspaceName = workspaceName;
        changeConfiguration();
    }

    /**
     * Get the path in the {@link #getConfigurationWorkspaceName() workspace} of the {@link #getConfigurationSourceName()
     * configuration source} where this source can find the content defining its configuration.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @return the string array of projection rules, or null if the projection rules haven't yet been set or if the federated
     *         repository instance is to be found in JNDI
     * @see #getConfigurationSourceName()
     * @see #getConfigurationWorkspaceName()
     * @see #setConfigurationPath(String)
     */
    public String getConfigurationPath() {
        return configurationPath;
    }

    /**
     * Set the path in the {@link #getConfigurationWorkspaceName() workspace} of the {@link #getConfigurationSourceName()
     * configuration source} where this source can find the content defining its configuration.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @param pathInSourceToConfigurationRoot the path within the configuration source to the node that should be the root of the
     *        configuration information, or null if the {@link #DEFAULT_CONFIGURATION_PATH default path} should be used
     * @see #getConfigurationPath()
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationWorkspaceName(String)
     */
    public void setConfigurationPath( String pathInSourceToConfigurationRoot ) {
        if (this.configurationPath == pathInSourceToConfigurationRoot || this.configurationPath != null
            && this.configurationPath.equals(pathInSourceToConfigurationRoot)) return;
        String path = pathInSourceToConfigurationRoot != null ? pathInSourceToConfigurationRoot : DEFAULT_CONFIGURATION_PATH;
        // Ensure one leading slash and one trailing slashes ...
        this.configurationPath = path = ("/" + path).replaceAll("^/+", "/").replaceAll("/+$", "") + "/";
        changeConfiguration();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return capabilities;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#initialize(org.jboss.dna.graph.connector.RepositoryContext)
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
     * @see org.jboss.dna.graph.connector.RepositorySource#getConnection()
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
                    String configSource = getConfigurationSourceName();
                    String configWorkspace = getConfigurationWorkspaceName();
                    String configPath = getConfigurationPath();
                    if (configSource == null) {
                        I18n msg = GraphI18n.propertyIsRequiredForFederatedRepositorySource;
                        throw new RepositorySourceException(getName(), msg.text("configuration source name", name));
                    }

                    // Load the configuration ...
                    this.configuration = loadRepository(name, repositoryContext, configSource, configWorkspace, configPath);
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
     * @see javax.naming.Referenceable#getReference()
     */
    public Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        ref.add(new StringRefAddr(CONFIGURATION_SOURCE_NAME, getConfigurationSourceName()));
        ref.add(new StringRefAddr(CONFIGURATION_WORKSPACE_NAME, getConfigurationWorkspaceName()));
        ref.add(new StringRefAddr(CONFIGURATION_PATH, getConfigurationPath()));
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
            String configSourceName = values.get(CONFIGURATION_SOURCE_NAME);
            String configSourceWorkspace = values.get(CONFIGURATION_WORKSPACE_NAME);
            String configSourcePath = values.get(CONFIGURATION_PATH);

            // Create the source instance ...
            FederatedRepositorySource source = new FederatedRepositorySource();
            if (sourceName != null) source.setName(sourceName);
            if (configSourceName != null) source.setConfigurationSourceName(configSourceName);
            if (configSourceWorkspace != null) source.setConfigurationWorkspaceName(configSourceWorkspace);
            if (configSourcePath != null) source.setConfigurationPath(configSourcePath);
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
     * Utility to load the current configuration for this source from the {@link #getConfigurationSourceName() configuration
     * repository}. This method may only be called after the source is {@link #initialize(RepositoryContext) initialized}.
     * 
     * @param name the name of the source; may not be null
     * @param repositoryContext the repository context; may not be null
     * @param configSource the name of the configuration source; may not be null
     * @param configWorkspace the name of the workspace in the configuration source, or null if the configuration source's default
     *        workspace should be used
     * @param configPath the path to the node in the configuration workspace that defines the source; may not be null
     * @return the configuration; never null
     * @throws RepositorySourceException if there is a problem with the configuration
     */
    protected FederatedRepository loadRepository( String name,
                                                  RepositoryContext repositoryContext,
                                                  String configSource,
                                                  String configWorkspace,
                                                  String configPath ) throws RepositorySourceException {
        // All the required properties have been set ...
        ExecutionContext executionContext = repositoryContext.getExecutionContext();
        RepositoryConnectionFactory connectionFactory = repositoryContext.getRepositoryConnectionFactory();
        ValueFactories valueFactories = executionContext.getValueFactories();
        ValueFactory<String> strings = valueFactories.getStringFactory();
        ValueFactory<Long> longs = valueFactories.getLongFactory();
        ProjectionParser projectionParser = ProjectionParser.getInstance();
        NamespaceRegistry registry = executionContext.getNamespaceRegistry();

        try {
            Graph config = Graph.create(configSource, connectionFactory, executionContext);
            if (configWorkspace != null) {
                configWorkspace = config.useWorkspace(configWorkspace).getName();
            } else {
                configWorkspace = config.getCurrentWorkspaceName();
            }

            // Read the configuration for the federated repository:
            // Level 1: the node representing the federated repository
            // Level 2: the "dna:workspaces" node
            // Level 3: a node for each workspace in the federated repository
            // Level 4: the "dna:projections" nodes
            // Level 5: a node below "dna:projections" for each projection, with properties for the source name,
            // workspace name, cache expiration time, and projection rules
            Subgraph repositories = config.getSubgraphOfDepth(5).at(configPath);

            // Get the name of the default workspace ...
            String defaultWorkspaceName = null;
            Property defaultWorkspaceNameProperty = repositories.getRoot().getProperty(DnaLexicon.DEFAULT_WORKSPACE_NAME);
            if (defaultWorkspaceNameProperty != null) {
                // Set the name using the property if there is one ...
                defaultWorkspaceName = strings.create(defaultWorkspaceNameProperty.getFirstValue());
            }

            // Get the default expiration time for the repository ...
            CachePolicy defaultCachePolicy = null;
            Property timeToExpire = repositories.getRoot().getProperty(DnaLexicon.TIME_TO_EXPIRE);
            if (timeToExpire != null && !timeToExpire.isEmpty()) {
                long timeToCacheInMillis = longs.create(timeToExpire.getFirstValue());
                defaultCachePolicy = new BasicCachePolicy(timeToCacheInMillis, TimeUnit.MILLISECONDS).getUnmodifiable();
            }

            // Level 2: The "dna:workspaces" node ...
            Node workspacesNode = repositories.getNode(DnaLexicon.WORKSPACES);
            if (workspacesNode == null) {
                I18n msg = GraphI18n.requiredNodeDoesNotExistRelativeToNode;
                throw new RepositorySourceException(msg.text(DnaLexicon.WORKSPACES.getString(registry),
                                                             repositories.getLocation().getPath().getString(registry),
                                                             configWorkspace,
                                                             configSource));
            }

            // Level 3: The workspace nodes ...
            LinkedList<FederatedWorkspace> workspaces = new LinkedList<FederatedWorkspace>();
            for (Location workspace : workspacesNode) {

                // Get the name of the workspace ...
                String workspaceName = null;
                SubgraphNode workspaceNode = repositories.getNode(workspace);
                Property workspaceNameProperty = workspaceNode.getProperty(DnaLexicon.WORKSPACE_NAME);
                if (workspaceNameProperty != null) {
                    // Set the name using the property if there is one ...
                    workspaceName = strings.create(workspaceNameProperty.getFirstValue());
                }
                if (workspaceName == null) {
                    // Otherwise, set the name using the local name of the workspace node ...
                    workspaceName = workspace.getPath().getLastSegment().getName().getLocalName();
                }

                // Level 4: the "dna:projections" node ...
                Node projectionsNode = workspaceNode.getNode(DnaLexicon.PROJECTIONS);
                if (projectionsNode == null) {
                    I18n msg = GraphI18n.requiredNodeDoesNotExistRelativeToNode;
                    throw new RepositorySourceException(getName(), msg.text(DnaLexicon.PROJECTIONS.getString(registry),
                                                                            workspaceNode.getLocation()
                                                                                         .getPath()
                                                                                         .getString(registry),
                                                                            configWorkspace,
                                                                            configSource));
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
            ExecutorService executor = Executors.newCachedThreadPool();

            return new FederatedRepository(name, connectionFactory, workspaces, defaultCachePolicy, executor);
        } catch (RepositorySourceException t) {
            throw t; // rethrow
        } catch (Throwable t) {
            I18n msg = GraphI18n.errorReadingConfigurationForFederatedRepositorySource;
            throw new RepositorySourceException(getName(), msg.text(name, configSource, configWorkspace, configPath), t);
        }
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
        Property sourceNameProperty = node.getProperty(DnaLexicon.SOURCE_NAME);
        if (sourceNameProperty != null && !sourceNameProperty.isEmpty()) {
            // There is a "dna:sourceName" property, so use this instead ...
            sourceName = strings.create(sourceNameProperty.getFirstValue());
        }
        assert sourceName != null;

        // Get the workspace name ...
        String workspaceName = null;
        Property workspaceNameProperty = node.getProperty(DnaLexicon.WORKSPACE_NAME);
        if (workspaceNameProperty != null && !workspaceNameProperty.isEmpty()) {
            // There is a "dna:workspaceName" property, so use this instead ...
            workspaceName = strings.create(workspaceNameProperty.getFirstValue());
        }

        // Get the projection rules ...
        Projection.Rule[] projectionRules = null;
        Property projectionRulesProperty = node.getProperty(DnaLexicon.PROJECTION_RULES);
        if (projectionRulesProperty != null && !projectionRulesProperty.isEmpty()) {
            String[] projectionRuleStrs = strings.create(projectionRulesProperty.getValuesAsArray());
            if (projectionRuleStrs != null && projectionRuleStrs.length != 0) {
                projectionRules = projectionParser.rulesFromStrings(context, projectionRuleStrs);
            }
        }

        // Is this projection read-only?
        boolean readOnly = false;
        Property readOnlyProperty = node.getProperty(DnaLexicon.READ_ONLY);
        if (readOnlyProperty != null && !readOnlyProperty.isEmpty()) {
            readOnly = context.getValueFactories().getBooleanFactory().create(readOnlyProperty.getFirstValue());
        }

        return new Projection(sourceName, workspaceName, readOnly, projectionRules);
    }

}
