/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.connector.federation;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JaasSecurityContext;
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
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class FederatedRepositorySource implements RepositorySource, ObjectFactory {

    /**
     */
    private static final long serialVersionUID = 7587346948013486977L;

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;

    public static final String DEFAULT_CONFIGURATION_SOURCE_PATH = "/";

    protected static final RepositorySourceCapabilities CAPABILITIES = new RepositorySourceCapabilities(true, true);

    protected static final String REPOSITORY_NAME = "repositoryName";
    protected static final String SOURCE_NAME = "sourceName";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String CONFIGURATION_SOURCE_NAME = "configurationSourceName";
    protected static final String CONFIGURATION_SOURCE_PATH = "configurationSourcePath";
    protected static final String SECURITY_DOMAIN = "securityDomain";
    protected static final String RETRY_LIMIT = "retryLimit";

    private String repositoryName;
    private String sourceName;
    private String username;
    private String password;
    private String configurationSourceName;
    private String configurationWorkspaceName;
    private String configurationSourcePath = DEFAULT_CONFIGURATION_SOURCE_PATH;
    private String securityDomain;
    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);
    private transient FederatedRepository repository;
    private transient RepositoryContext repositoryContext;

    /**
     * Create a new instance of the source, which must still be properly initialized with a {@link #setRepositoryName(String)
     * repository name}.
     */
    public FederatedRepositorySource() {
        super();
    }

    /**
     * Create a new instance of the source with the required repository name and federation service.
     * 
     * @param repositoryName the repository name
     * @throws IllegalArgumentException if the federation service is null or the repository name is null or blank
     */
    public FederatedRepositorySource( String repositoryName ) {
        super();
        CheckArg.isNotNull(repositoryName, "repositoryName");
        this.repositoryName = repositoryName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#initialize(org.jboss.dna.graph.connector.RepositoryContext)
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
     */
    public synchronized String getName() {
        return sourceName;
    }

    /**
     * Set the name of this source.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @param sourceName the name of this repository source
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationSourcePath(String)
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setName(String)
     */
    public synchronized void setName( String sourceName ) {
        if (this.sourceName == sourceName || this.sourceName != null && this.sourceName.equals(sourceName)) return; // unchanged
        this.sourceName = sourceName;
        changeRepositoryConfig();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        retryLimit.set(limit < 0 ? 0 : limit);
    }

    /**
     * Get the name in JNDI of a {@link RepositorySource} instance that should be used by the {@link FederatedRepository federated
     * repository} as the configuration repository.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @return the JNDI name of the {@link RepositorySource} instance that should be used for the configuration, or null if the
     *         federated repository instance is to be found in JNDI
     * @see #setConfigurationSourceName(String)
     */
    public String getConfigurationSourceName() {
        return configurationSourceName;
    }

    /**
     * Get the name of a {@link RepositorySource} instance that should be used by the {@link FederatedRepository federated
     * repository} as the configuration repository. The instance will be retrieved from the {@link RepositoryConnectionFactory}
     * instance from the {@link RepositoryContext#getRepositoryConnectionFactory() repository context} supplied during
     * {@link RepositorySource#initialize(RepositoryContext) initialization}.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @param sourceName the name of the {@link RepositorySource} instance that should be used for the configuration, or null if
     *        the federated repository instance is to be found in JNDI
     * @see #getConfigurationSourceName()
     * @see #setConfigurationSourcePath(String)
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setName(String)
     */
    public void setConfigurationSourceName( String sourceName ) {
        if (this.configurationSourceName == sourceName || this.configurationSourceName != null
            && this.configurationSourceName.equals(sourceName)) return; // unchanged
        this.configurationSourceName = sourceName;
        changeRepositoryConfig();
    }

    /**
     * Set the name of the workspace in the {@link #getConfigurationSourceName() source} used by the {@link FederatedRepository
     * federated repository} as the configuration repository. If this workspace name is null, the default workspace as defined by
     * that source will be used.
     * 
     * @return the name of the configuration workspace, or null if the default workspace for the
     *         {@link #getConfigurationSourceName() configuration source} should be used
     */
    public String getConfigurationWorkspaceName() {
        return configurationWorkspaceName;
    }

    /**
     * Set the name of the workspace in the {@link #getConfigurationSourceName() source} used by the {@link FederatedRepository
     * federated repository} as the configuration repository. If this workspace name is null, the default workspace as defined by
     * that source will be used.
     * 
     * @param workspaceName the name of the configuration workspace, or null if the default workspace for the
     *        {@link #getConfigurationSourceName() configuration source} should be used
     */
    public void setConfigurationWorkspaceName( String workspaceName ) {
        if (this.configurationWorkspaceName == workspaceName || this.configurationWorkspaceName != null
            && this.configurationWorkspaceName.equals(workspaceName)) return; // unchanged
        this.configurationWorkspaceName = workspaceName;
        changeRepositoryConfig();
    }

    /**
     * Get the path in the source that will be subgraph below the <code>/dna:system</code> branch of the repository.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @return the string array of projection rules, or null if the projection rules haven't yet been set or if the federated
     *         repository instance is to be found in JNDI
     * @see #setConfigurationSourcePath(String)
     */
    public String getConfigurationSourcePath() {
        return configurationSourcePath;
    }

    /**
     * Set the path in the source that will be subgraph below the <code>/dna:system</code> branch of the repository.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @param pathInSourceToConfigurationRoot the path within the configuration source to the node that should be the root of the
     *        configuration information, or null if the path hasn't yet been set or if the federated repository instance is to be
     *        found in JNDI
     * @see #setConfigurationSourcePath(String)
     * @see #setConfigurationSourceName(String)
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setName(String)
     */
    public void setConfigurationSourcePath( String pathInSourceToConfigurationRoot ) {
        if (this.configurationSourcePath == pathInSourceToConfigurationRoot || this.configurationSourcePath != null
            && this.configurationSourcePath.equals(pathInSourceToConfigurationRoot)) return;
        String path = pathInSourceToConfigurationRoot != null ? pathInSourceToConfigurationRoot : DEFAULT_CONFIGURATION_SOURCE_PATH;
        // Ensure one leading slash and one trailing slashes ...
        this.configurationSourcePath = path = ("/" + path).replaceAll("^/+", "/").replaceAll("/+$", "") + "/";
        changeRepositoryConfig();
    }

    /**
     * Get the name of the security domain that should be used by JAAS to identify the application or security context. This
     * should correspond to the JAAS login configuration located within the JAAS login configuration file.
     * 
     * @return securityDomain
     */
    public String getSecurityDomain() {
        return securityDomain;
    }

    /**
     * Set the name of the security domain that should be used by JAAS to identify the application or security context. This
     * should correspond to the JAAS login configuration located within the JAAS login configuration file.
     * 
     * @param securityDomain Sets securityDomain to the specified value.
     */
    public void setSecurityDomain( String securityDomain ) {
        if (this.securityDomain != null && this.securityDomain.equals(securityDomain)) return; // unchanged
        this.securityDomain = securityDomain;
        changeRepositoryConfig();
    }

    /**
     * Get the name of the federated repository.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @return the name of the repository
     * @see #setRepositoryName(String)
     */
    public synchronized String getRepositoryName() {
        return this.repositoryName;
    }

    /**
     * Get the name of the federated repository.
     * <p>
     * This is a required property.
     * </p>
     * 
     * @param repositoryName the new name of the repository
     * @throws IllegalArgumentException if the repository name is null, empty or blank
     * @see #getRepositoryName()
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationSourcePath(String)
     * @see #setPassword(String)
     * @see #setUsername(String)
     * @see #setName(String)
     */
    public synchronized void setRepositoryName( String repositoryName ) {
        CheckArg.isNotEmpty(repositoryName, "repositoryName");
        if (this.repositoryName != null && this.repositoryName.equals(repositoryName)) return; // unchanged
        this.repositoryName = repositoryName;
        changeRepositoryConfig();
    }

    /**
     * Get the username that should be used when authenticating and {@link #getConnection() creating connections}.
     * <p>
     * This is an optional property, required only when authentication is to be used.
     * </p>
     * 
     * @return the username, or null if no username has been set or are not to be used
     * @see #setUsername(String)
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * Set the username that should be used when authenticating and {@link #getConnection() creating connections}.
     * <p>
     * This is an optional property, required only when authentication is to be used.
     * </p>
     * 
     * @param username the username, or null if no username has been set or are not to be used
     * @see #getUsername()
     * @see #setPassword(String)
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationSourcePath(String)
     * @see #setPassword(String)
     * @see #setRepositoryName(String)
     * @see #setName(String)
     */
    public void setUsername( String username ) {
        if (this.username != null && this.username.equals(username)) return; // unchanged
        this.username = username;
        changeRepositoryConfig();
    }

    /**
     * Get the password that should be used when authenticating and {@link #getConnection() creating connections}.
     * <p>
     * This is an optional property, required only when authentication is to be used.
     * </p>
     * 
     * @return the password, or null if no password have been set or are not to be used
     * @see #setPassword(String)
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Get the password that should be used when authenticating and {@link #getConnection() creating connections}.
     * <p>
     * This is an optional property, required only when authentication is to be used.
     * </p>
     * 
     * @param password the password, or null if no password have been set or are not to be used
     * @see #getPassword()
     * @see #setConfigurationSourceName(String)
     * @see #setConfigurationSourcePath(String)
     * @see #setUsername(String)
     * @see #setRepositoryName(String)
     * @see #setName(String)
     */
    public void setPassword( String password ) {
        if (this.password != null && this.password.equals(password)) return; // unchanged
        this.password = password;
        changeRepositoryConfig();
    }

    /**
     * This method is called to signal that some aspect of the configuration has changed. If a {@link #getRepository() repository}
     * instance has been created, it's configuration is
     * {@link #getWorkspaceConfigurations(ExecutionContext, RepositoryConnectionFactory) rebuilt} and updated. Nothing is done,
     * however, if there is currently no {@link #getRepository() repository}.
     */
    protected synchronized void changeRepositoryConfig() {
        if (this.repository != null) {
            RepositoryContext repositoryContext = getRepositoryContext();
            if (repositoryContext != null) {
                this.repository = getRepository();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        if (getName() == null) {
            I18n msg = FederationI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("name"));
        }
        if (getRepositoryContext() == null) {
            I18n msg = FederationI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("repository context"));
        }
        if (getUsername() != null && getSecurityDomain() == null) {
            I18n msg = FederationI18n.propertyIsRequired;
            throw new RepositorySourceException(getName(), msg.text("security domain"));
        }
        // Find the repository ...
        FederatedRepository repository = getRepository();
        // Authenticate the user ...
        String username = this.username;
        Object credentials = this.password;
        RepositoryConnection connection = repository.createConnection(this, username, credentials);
        if (connection == null) {
            I18n msg = FederationI18n.unableToAuthenticateConnectionToFederatedRepository;
            throw new RepositorySourceException(msg.text(this.repositoryName, username));
        }
        // Return the new connection ...
        return connection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#close()
     */
    public void close() {
        // do nothing ...
    }

    /**
     * Get the {@link FederatedRepository} instance that this source is using. This method uses the following logic:
     * <ol>
     * <li>If a {@link FederatedRepository} already was obtained from a prior call, the same instance is returned.</li>
     * <li>A {@link FederatedRepository} is created using a {@link FederatedWorkspace} is created from this instance's properties
     * and {@link ExecutionContext} and {@link RepositoryConnectionFactory} instances obtained from JNDI.</li>
     * <li></li>
     * <li></li>
     * </ol>
     * 
     * @return the federated repository instance
     * @throws RepositorySourceException
     */
    protected synchronized FederatedRepository getRepository() throws RepositorySourceException {
        if (repository == null) {
            ExecutionContext context = getExecutionContext();
            RepositoryConnectionFactory connectionFactory = getRepositoryContext().getRepositoryConnectionFactory();
            // And create the configuration and the repository ...
            List<FederatedWorkspace> configs = getWorkspaceConfigurations(context, connectionFactory);
            repository = new FederatedRepository(repositoryName, context, connectionFactory, configs);
        }
        return repository;
    }

    protected ExecutionContext getExecutionContext() {
        ExecutionContext factory = getRepositoryContext().getExecutionContext();
        CallbackHandler handler = createCallbackHandler();
        try {
            String securityDomain = getSecurityDomain();
            if (securityDomain != null || getUsername() != null) {
                return factory.with(new JaasSecurityContext(securityDomain, handler));
            }
            return factory;
        } catch (LoginException e) {
            I18n msg = FederationI18n.unableToCreateExecutionContext;
            throw new RepositorySourceException(getName(), msg.text(this.sourceName, securityDomain), e);
        }
    }

    protected CallbackHandler createCallbackHandler() {
        return new CallbackHandler() {
            public void handle( Callback[] callbacks ) {
                for (Callback callback : callbacks) {
                    if (callback instanceof NameCallback) {
                        NameCallback nameCallback = (NameCallback)callback;
                        nameCallback.setName(FederatedRepositorySource.this.getUsername());
                    }
                    if (callback instanceof PasswordCallback) {
                        PasswordCallback passwordCallback = (PasswordCallback)callback;
                        passwordCallback.setPassword(FederatedRepositorySource.this.getPassword().toCharArray());
                    }
                }
            }
        };
    }

    /**
     * Create a {@link FederatedWorkspace} instances from the current properties of this instance. This method does <i>not</i>
     * modify the state of this instance.
     * 
     * @param context the execution context that should be used to read the configuration; may not be null
     * @param connectionFactory the factory for {@link RepositoryConnection}s can be obtained; may not be null
     * @return the collection of configurations reflecting the workspaces as currently defined, order such that the default
     *         workspace is first; never null
     */
    protected synchronized List<FederatedWorkspace> getWorkspaceConfigurations( ExecutionContext context,
                                                                                RepositoryConnectionFactory connectionFactory ) {
        Problems problems = new SimpleProblems();
        ValueFactories valueFactories = context.getValueFactories();
        ValueFactory<String> strings = valueFactories.getStringFactory();
        ValueFactory<Long> longs = valueFactories.getLongFactory();
        ProjectionParser projectionParser = ProjectionParser.getInstance();

        // Create a graph to access the configuration ...
        Graph config = Graph.create(this.configurationSourceName, connectionFactory, context);
        if (this.configurationWorkspaceName != null) config.useWorkspace(this.configurationWorkspaceName);
        String configurationWorkspaceName = config.getCurrentWorkspaceName();

        // Read the federated repositories subgraph, of max depth 6:
        // Level 1: the node representing the federated repository
        // Level 2: the "dna:workspaces" node
        // Level 3: a node for each workspace in the federated repository
        // Level 4: the "dna:cache" project node, or the "dna:projections" nodes
        // Level 5: a node below "dna:projections" for each projection, with properties for the source name,
        // workspace name, cache expiration time, and projection rules
        Subgraph repositories = config.getSubgraphOfDepth(5).at(getConfigurationSourcePath());

        // Get the name of the default workspace ...
        String defaultWorkspaceName = null;
        Property defaultWorkspaceNameProperty = repositories.getRoot().getProperty(FederatedLexicon.DEFAULT_WORKSPACE_NAME);
        if (defaultWorkspaceNameProperty != null) {
            // Set the name using the property if there is one ...
            defaultWorkspaceName = strings.create(defaultWorkspaceNameProperty.getFirstValue());
        }

        Node workspacesNode = repositories.getNode(FederatedLexicon.WORKSPACES);
        if (workspacesNode == null) {
            I18n msg = FederationI18n.requiredNodeDoesNotExistRelativeToNode;
            String name = FederatedLexicon.WORKSPACES.getString(context.getNamespaceRegistry());
            String relativeTo = repositories.getLocation().getPath().getString(context.getNamespaceRegistry());
            throw new FederationException(msg.text(name, relativeTo, configurationWorkspaceName, configurationSourceName));
        }
        LinkedList<FederatedWorkspace> workspaces = new LinkedList<FederatedWorkspace>();
        for (Location workspace : workspacesNode) {
            // Get the name of the workspace ...
            String workspaceName = null;
            SubgraphNode workspaceNode = repositories.getNode(workspace);
            Property workspaceNameProperty = workspaceNode.getProperty(FederatedLexicon.WORKSPACE_NAME);
            if (workspaceNameProperty != null) {
                // Set the name using the property if there is one ...
                workspaceName = strings.create(workspaceNameProperty.getFirstValue());
            }
            if (workspaceName == null) {
                // Otherwise, set the name using the local name of the workspace node ...
                workspaceName = workspace.getPath().getLastSegment().getName().getLocalName();
            }

            // Get the cache projection ...
            Projection cacheProjection = null;
            CachePolicy cachePolicy = null;
            Node cacheNode = workspaceNode.getNode(FederatedLexicon.CACHE);
            if (cacheNode != null) {
                // Create the projection from the "dna:cache" node ...
                cacheProjection = createProjection(context, projectionParser, cacheNode, problems);

                // Get the cache expiration time for the cache ...
                Property timeToExpire = cacheNode.getProperty(FederatedLexicon.TIME_TO_EXPIRE);
                if (timeToExpire != null && !timeToExpire.isEmpty()) {
                    long timeToCacheInMillis = longs.create(timeToExpire.getFirstValue());
                    cachePolicy = new BasicCachePolicy(timeToCacheInMillis, TimeUnit.MILLISECONDS).getUnmodifiable();
                }
            }

            // Get the source projections ...
            Node projectionsNode = workspaceNode.getNode(FederatedLexicon.PROJECTIONS);
            if (projectionsNode == null) {
                I18n msg = FederationI18n.requiredNodeDoesNotExistRelativeToNode;
                String name = FederatedLexicon.PROJECTIONS.getString(context.getNamespaceRegistry());
                String relativeTo = workspaceNode.getLocation().getPath().getString(context.getNamespaceRegistry());
                throw new FederationException(msg.text(name, relativeTo, configurationWorkspaceName, configurationSourceName));
            }
            List<Projection> sourceProjections = new LinkedList<Projection>();
            for (Location projection : projectionsNode) {
                Node projectionNode = repositories.getNode(projection);

                // Create the projection ...
                sourceProjections.add(createProjection(context, projectionParser, projectionNode, problems));
            }

            // Create the federated workspace configuration ...
            FederatedWorkspace space = new FederatedWorkspace(workspaceName, cacheProjection, sourceProjections, cachePolicy);
            if (workspaceName.equals(defaultWorkspaceName)) {
                workspaces.addFirst(space);
            } else {
                workspaces.add(space);
            }
        }

        return workspaces;
    }

    /**
     * Instantiate the {@link Projection} described by the supplied properties.
     * 
     * @param context the execution context that should be used to read the configuration; may not be null
     * @param projectionParser the projection rule parser that should be used; may not be null
     * @param node the node where these properties were found; never null
     * @param problems the problems container in which any problems should be reported; never null
     * @return the region instance, or null if it could not be created
     */
    protected Projection createProjection( ExecutionContext context,
                                           ProjectionParser projectionParser,
                                           Node node,
                                           Problems problems ) {
        ValueFactory<String> strings = context.getValueFactories().getStringFactory();

        Path path = node.getLocation().getPath();

        // Get the source name from the local name of the node ...
        String sourceName = path.getLastSegment().getName().getLocalName();
        Property sourceNameProperty = node.getProperty(FederatedLexicon.SOURCE_NAME);
        if (sourceNameProperty != null && !sourceNameProperty.isEmpty()) {
            // There is a "dna:sourceName" property, so use this instead ...
            sourceName = strings.create(sourceNameProperty.getFirstValue());
        }
        assert sourceName != null;

        // Get the workspace name ...
        String workspaceName = null;
        Property workspaceNameProperty = node.getProperty(FederatedLexicon.WORKSPACE_NAME);
        if (workspaceNameProperty != null && !workspaceNameProperty.isEmpty()) {
            // There is a "dna:workspaceName" property, so use this instead ...
            workspaceName = strings.create(workspaceNameProperty.getFirstValue());
        }

        // Get the projection rules ...
        Projection.Rule[] projectionRules = null;
        Property projectionRulesProperty = node.getProperty(FederatedLexicon.PROJECTION_RULES);
        if (projectionRulesProperty != null && !projectionRulesProperty.isEmpty()) {
            String[] projectionRuleStrs = strings.create(projectionRulesProperty.getValuesAsArray());
            if (projectionRuleStrs != null && projectionRuleStrs.length != 0) {
                projectionRules = projectionParser.rulesFromStrings(context, projectionRuleStrs);
            }
        }
        if (problems.hasErrors()) return null;

        return new Projection(sourceName, workspaceName, projectionRules);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = this.getClass().getName();
        Reference ref = new Reference(className, factoryClassName, null);

        if (getRepositoryName() != null) {
            ref.add(new StringRefAddr(REPOSITORY_NAME, getRepositoryName()));
        }
        if (getName() != null) {
            ref.add(new StringRefAddr(SOURCE_NAME, getName()));
        }
        if (getUsername() != null) {
            ref.add(new StringRefAddr(USERNAME, getUsername()));
        }
        if (getPassword() != null) {
            ref.add(new StringRefAddr(PASSWORD, getPassword()));
        }
        if (getConfigurationSourceName() != null) {
            ref.add(new StringRefAddr(CONFIGURATION_SOURCE_NAME, getConfigurationSourceName()));
        }
        if (getConfigurationSourcePath() != null) {
            ref.add(new StringRefAddr(CONFIGURATION_SOURCE_PATH, getConfigurationSourcePath()));
        }
        if (getSecurityDomain() != null) {
            ref.add(new StringRefAddr(SECURITY_DOMAIN, getSecurityDomain()));
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
            String repositoryName = values.get(FederatedRepositorySource.REPOSITORY_NAME);
            String sourceName = values.get(FederatedRepositorySource.SOURCE_NAME);
            String username = values.get(FederatedRepositorySource.USERNAME);
            String password = values.get(FederatedRepositorySource.PASSWORD);
            String configurationSourceName = values.get(FederatedRepositorySource.CONFIGURATION_SOURCE_NAME);
            String configurationSourcePath = values.get(FederatedRepositorySource.CONFIGURATION_SOURCE_PATH);
            String securityDomain = values.get(FederatedRepositorySource.SECURITY_DOMAIN);
            String retryLimit = values.get(FederatedRepositorySource.RETRY_LIMIT);

            // Create the source instance ...
            FederatedRepositorySource source = new FederatedRepositorySource();
            if (repositoryName != null) source.setRepositoryName(repositoryName);
            if (sourceName != null) source.setName(sourceName);
            if (username != null) source.setUsername(username);
            if (password != null) source.setPassword(password);
            if (configurationSourceName != null) source.setConfigurationSourceName(configurationSourceName);
            if (configurationSourcePath != null) source.setConfigurationSourcePath(configurationSourcePath);
            if (securityDomain != null) source.setSecurityDomain(securityDomain);
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
        return repositoryName.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof FederatedRepositorySource) {
            FederatedRepositorySource that = (FederatedRepositorySource)obj;
            // The repository name, source name, and federation service must all match
            if (!this.getRepositoryName().equals(that.getRepositoryName())) return false;
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
     * 
     * @see org.jboss.dna.graph.connector.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return CAPABILITIES;
    }
}
