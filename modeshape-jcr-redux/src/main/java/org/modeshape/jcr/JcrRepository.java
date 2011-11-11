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
package org.modeshape.jcr;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessControlContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.Credentials;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.security.auth.login.LoginContext;
import javax.transaction.TransactionManager;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Array;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.Editor;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.document.Paths;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.jcr.JcrEngine.State;
import org.modeshape.jcr.JcrRepository.ConfigurationChange;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.JcrRepository.SessionMonitor;
import org.modeshape.jcr.RepositoryConfiguration.AnonymousSecurity;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.JaasSecurity;
import org.modeshape.jcr.RepositoryConfiguration.Security;
import org.modeshape.jcr.RepositoryStatistics.ValueMetric;
import org.modeshape.jcr.api.AnonymousCredentials;
import org.modeshape.jcr.api.Repository;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.SessionCacheMonitor;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.WorkspaceAdded;
import org.modeshape.jcr.cache.change.WorkspaceRemoved;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.security.AnonymousProvider;
import org.modeshape.jcr.security.AuthenticationProvider;
import org.modeshape.jcr.security.AuthenticationProviders;
import org.modeshape.jcr.security.JaasProvider;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.ValueFactories;

/**
 * 
 */
public class JcrRepository implements org.modeshape.jcr.api.Repository {

    /**
     * The set of supported query language string constants.
     * 
     * @see javax.jcr.query.QueryManager#getSupportedQueryLanguages()
     * @see javax.jcr.query.QueryManager#createQuery(String, String)
     */
    public static final class QueryLanguage {
        /**
         * The standard JCR 1.0 XPath query language.
         */
        @SuppressWarnings( "deprecation" )
        public static final String XPATH = Query.XPATH;

        /**
         * The SQL dialect that is based upon an enhanced version of the JCR-SQL query language defined by the JCR 1.0.1
         * specification.
         */
        @SuppressWarnings( "deprecation" )
        public static final String JCR_SQL = Query.SQL;

        /**
         * The SQL dialect that is based upon an enhanced version of the JCR-SQL2 query language defined by the JCR 2.0
         * specification.
         */
        public static final String JCR_SQL2 = Query.JCR_SQL2;

        /**
         * The enhanced Query Object Model language defined by the JCR 2.0 specification.
         */
        public static final String JCR_JQOM = Query.JCR_JQOM;
        /**
         * The full-text search language defined as part of the abstract query model, in Section 6.7.19 of the JCR 2.0
         * specification.
         */
        // TODO : Query
        // public static final String SEARCH = FullTextSearchParser.LANGUAGE;
    }

    protected static final Set<String> MISSING_JAAS_POLICIES = new CopyOnWriteArraySet<String>();

    private static final boolean AUTO_START_REPO_UPON_LOGIN = true;

    protected final Logger logger;
    private final AtomicReference<RepositoryConfiguration> config = new AtomicReference<RepositoryConfiguration>();
    private final AtomicReference<String> repositoryName = new AtomicReference<String>();
    private final Map<String, Object> descriptors;
    private final AtomicReference<RunningState> runningState = new AtomicReference<RunningState>();
    private final AtomicReference<State> state = new AtomicReference<State>(State.NOT_RUNNING);
    private final Lock stateLock = new ReentrantLock();
    private final AtomicBoolean allowAutoStartDuringLogin = new AtomicBoolean(AUTO_START_REPO_UPON_LOGIN);

    /**
     * Create a Repository instance given the {@link RepositoryConfiguration configuration}.
     * 
     * @param configuration the repository configuration; may not be null
     * @throws ConfigurationException if there is a problem with the configuration
     */
    protected JcrRepository( RepositoryConfiguration configuration ) throws ConfigurationException {
        this.config.set(configuration);
        RepositoryConfiguration config = this.config.get();

        // Validate the configuration to make sure there are no errors ...
        Results results = configuration.validate();
        if (results.hasErrors()) {
            String msg = JcrI18n.errorsInRepositoryConfiguration.text(this.repositoryName,
                                                                      results.errorCount(),
                                                                      results.toString());
            throw new ConfigurationException(results, msg);
        }

        this.repositoryName.set(config.getName());
        assert this.config != null;
        assert this.repositoryName != null;
        this.logger = Logger.getLogger(getClass());
        this.logger.debug("Initializing '{0}' repository", this.repositoryName);

        // Set up the descriptors ...
        this.descriptors = new HashMap<String, Object>();
        initializeDescriptors();
    }

    RepositoryConfiguration repositoryConfiguration() {
        return config.get();
    }

    /**
     * Get the state of this JCR repository instance.
     * 
     * @return the state; never null
     */
    public State getState() {
        return state.get();
    }

    /**
     * Get the name of this JCR repository instance.
     * 
     * @return the name; never null
     */
    public String getName() {
        return repositoryName.get();
    }

    /**
     * Get the component that can be used to obtain statistics for this repository.
     * 
     * @return the statistics component; never null
     * @throws IllegalStateException if the repository is not {@link #getState() running}
     */
    public RepositoryStatistics getRepositoryStatistics() {
        return statistics();
    }

    /**
     * Start this repository instance.
     * 
     * @throws FileNotFoundException if the Infinispan configuration file is specified but could not be found
     * @throws IOException if there is a problem with the specified Infinispan configuration file
     * @throws NamingException if there is a problem looking in JNDI for the Infinispan CacheContainer
     */
    void start() throws IOException, NamingException {
        doStart();
    }

    /**
     * Terminate all active sessions.
     * 
     * @return a future representing the asynchronous session termination process.
     */
    Future<Boolean> shutdown() {
        // Create a simple executor that will do the backgrounding for us ...
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Submit a runnable to terminate all sessions ...
            Future<Boolean> future = executor.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return doShutdown();
                }
            });
            return future;
        } finally {
            // Now shutdown the executor and return the future ...
            executor.shutdown();
        }
    }

    /**
     * Apply the supplied changes to this repository's configuration, and if running change the services to reflect the updated
     * configuration. Note that this method assumes the proposed changes have already been validated; see
     * {@link RepositoryConfiguration#validate(Changes)}.
     * 
     * @param changes the changes for the configuration
     * @throws FileNotFoundException if the Infinispan configuration file is changed but could not be found
     * @throws IOException if there is a problem with the specified Infinispan configuration file
     * @throws NamingException if there is a problem looking in JNDI for the Infinispan CacheContainer
     * @see JcrEngine#update(String, Changes)
     */
    void apply( Changes changes ) throws IOException, NamingException {
        final RepositoryConfiguration oldConfiguration = this.config.get();
        Editor copy = oldConfiguration.edit();
        ConfigurationChange configChanges = new ConfigurationChange();
        copy.apply(changes, configChanges);
        try {
            stateLock.lock();
            // Always update the configuration ...
            RunningState oldState = this.runningState.get();
            this.config.set(new RepositoryConfiguration(copy, copy.getString(FieldName.NAME)));
            if (oldState != null) {
                assert state.get() == State.RUNNING;
                // Repository is running, so create a new running state ...
                this.runningState.set(new RunningState(oldState, configChanges));

                // Handle a few special cases that the running state doesn't really handle itself ...
                if (!configChanges.storageChanged && configChanges.predefinedWorkspacesChanged) workspacesChanged();
                if (configChanges.nameChanged) repositoryNameChanged();
            }
        } finally {
            stateLock.unlock();
        }
    }

    protected final RunningState doStart() throws IOException, NamingException {
        try {
            stateLock.lock();
            RunningState state = this.runningState.get();
            if (state == null) {
                // start the repository by creating the running state ...
                this.state.set(State.STARTING);
                state = new RunningState();
                this.runningState.compareAndSet(null, state);
                workspacesChanged();
                this.state.set(State.RUNNING);
            }
            return state;
        } catch (IOException e) {
            // Only way to get exception is because we tried to create the running state (e.g., it was not running when we
            // entered)
            this.state.set(State.NOT_RUNNING);
            throw e;
        } catch (NamingException e) {
            // Only way to get exception is because we tried to create the running state (e.g., it was not running when we
            // entered)
            this.state.set(State.NOT_RUNNING);
            throw e;
        } catch (RuntimeException e) {
            // Only way to get exception is because we tried to create the running state (e.g., it was not running when we
            // entered)
            this.state.set(State.NOT_RUNNING);
            throw e;
        } finally {
            stateLock.unlock();
        }
    }

    protected final boolean doShutdown() {
        if (this.state.get() == State.NOT_RUNNING) return true;
        try {
            stateLock.lock();
            RunningState running = this.runningState.get();
            if (running != null) {
                // Prevent future 'login(...)' calls from restarting the repository ...
                this.allowAutoStartDuringLogin.set(false);

                this.state.set(State.STOPPING);

                // Terminate each of the still-open sessions ...
                running.terminateSessions();

                // Now shutdown the running state ...
                running.shutdown();

                // Null out the running state ...
                this.runningState.set(null);
            }
            this.state.set(State.NOT_RUNNING);
        } finally {
            stateLock.unlock();
        }
        return true;
    }

    private final Cache<String, SchematicEntry> infinispanCache() {
        return database().getCache();
    }

    protected final SchematicDb database() {
        return runningState().database();
    }

    protected final String repositoryName() {
        return repositoryName.get();
    }

    protected final RepositoryCache repositoryCache() {
        return runningState().repositoryCache();
    }

    protected final RepositoryStatistics statistics() {
        return runningState().statistics();
    }

    protected final TransactionManager txnManager() {
        return infinispanCache().getAdvancedCache().getTransactionManager();
    }

    protected final RepositoryNodeTypeManager nodeTypeManager() {
        return runningState().nodeTypeManager();
    }

    protected final RepositoryLockManager lockManager() {
        return runningState().lockManager();
    }

    protected final NamespaceRegistry persistentRegistry() {
        return runningState().persistentRegistry();
    }

    protected final String systemWorkspaceName() {
        return runningState().systemWorkspaceName();
    }

    protected final String systemWorkspaceKey() {
        return runningState().systemWorkspaceKey();
    }

    protected final JcrRepository.RunningState runningState() {
        RunningState running = runningState.get();
        if (running == null) {
            throw new IllegalStateException(JcrI18n.repositoryIsNotRunningOrHasBeenShutDown.text(repositoryName()));
        }
        return running;
    }

    final SessionCache createSystemSession( ExecutionContext context,
                                            boolean readOnly ) {
        return repositoryCache().createSession(context, systemWorkspaceName(), readOnly);
    }

    /**
     * Get the immutable configuration for this repository.
     * 
     * @return the configuration; never null
     */
    public RepositoryConfiguration getConfiguration() {
        return this.config.get();
    }

    @Override
    public String getDescriptor( String key ) {
        if (key == null) return null;
        if (!isSingleValueDescriptor(key)) return null;

        JcrValue value = (JcrValue)descriptors.get(key);
        try {
            return value.getString();
        } catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }

    @Override
    public JcrValue getDescriptorValue( String key ) {
        if (key == null) return null;
        if (!isSingleValueDescriptor(key)) return null;
        return (JcrValue)descriptors.get(key);
    }

    @Override
    public JcrValue[] getDescriptorValues( String key ) {
        Object value = descriptors.get(key);
        if (value instanceof JcrValue[]) {
            // Make a defensive copy of the array; the elements are immutable ...
            JcrValue[] values = (JcrValue[])value;
            JcrValue[] newValues = new JcrValue[values.length];
            System.arraycopy(values, 0, newValues, 0, values.length);
            return newValues;
        }
        if (value instanceof JcrValue) {
            return new JcrValue[] {(JcrValue)value};
        }
        return null;
    }

    @Override
    public boolean isSingleValueDescriptor( String key ) {
        if (key == null) return true;
        return descriptors.get(key) instanceof JcrValue;
    }

    @Override
    public boolean isStandardDescriptor( String key ) {
        return STANDARD_DESCRIPTORS.contains(key);
    }

    @Override
    public String[] getDescriptorKeys() {
        return descriptors.keySet().toArray(new String[descriptors.size()]);
    }

    @Override
    public synchronized JcrSession login() throws RepositoryException {
        return login(null, null);
    }

    @Override
    public synchronized JcrSession login( Credentials credentials ) throws RepositoryException {
        return login(credentials, null);
    }

    @Override
    public synchronized JcrSession login( String workspaceName ) throws RepositoryException {
        return login(null, workspaceName);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>credentials</code> is not <code>null</code> but:
     *         <ul>
     *         <li>provides neither a <code>getLoginContext()</code> nor a <code>getAccessControlContext()</code> method and is
     *         not an instance of {@code SimpleCredentials}.</li>
     *         <li>provides a <code>getLoginContext()</code> method that doesn't return a {@link LoginContext}.
     *         <li>provides a <code>getLoginContext()</code> method that returns a <code>
     *         null</code> {@link LoginContext}.
     *         <li>does not provide a <code>getLoginContext()</code> method, but provides a <code>getAccessControlContext()</code>
     *         method that doesn't return an {@link AccessControlContext}.
     *         <li>does not provide a <code>getLoginContext()</code> method, but provides a <code>getAccessControlContext()</code>
     *         method that returns a <code>null</code> {@link AccessControlContext}.
     *         </ul>
     * @see javax.jcr.Repository#login(javax.jcr.Credentials, java.lang.String)
     */
    @Override
    public synchronized JcrSession login( final Credentials credentials,
                                          String workspaceName ) throws RepositoryException {
        final String repoName = this.repositoryName();

        // Get the running state ...
        RunningState running = this.runningState.get();
        if (running == null) {
            if (this.allowAutoStartDuringLogin.get()) {
                // Try starting ...
                try {
                    running = doStart();
                } catch (Throwable t) {
                    throw new RepositoryException(JcrI18n.errorStartingRepository.text(repoName, t.getMessage()));
                }
                if (running == null) {
                    throw new RepositoryException(JcrI18n.repositoryIsNotRunningOrHasBeenShutDown.text(repoName));
                }
            } else {
                throw new RepositoryException(JcrI18n.repositoryIsNotRunningOrHasBeenShutDown.text(repoName));
            }
        }

        workspaceName = validateWorkspaceName(running, workspaceName);
        final AuthenticationProviders authenticators = running.authenticators();
        final Credentials anonCredentials = running.anonymousCredentials();
        final Map<String, Object> attributes = new HashMap<String, Object>();

        // Try to authenticate with the provider(s) ...
        ExecutionContext context = running.context();
        ExecutionContext sessionContext = authenticators.authenticate(credentials, repoName, workspaceName, context, attributes);

        if (sessionContext == null && credentials != null && anonCredentials != null) {
            // Failed non-anonymous authentication, so try anonymous authentication ...
            if (logger.isDebugEnabled()) logger.debug(JcrI18n.usingAnonymousUser.text());
            attributes.clear();
            sessionContext = authenticators.authenticate(anonCredentials, repoName, workspaceName, context, attributes);
        }

        if (sessionContext == null) {
            // Failed authentication ...
            throw new javax.jcr.LoginException(JcrI18n.loginFailed.text(repoName, workspaceName));
        }

        // We have successfully authenticated ...
        boolean readOnly = false; // assume not
        JcrSession session = new JcrSession(this, workspaceName, sessionContext, attributes, readOnly);

        running.addSession(session);
        return session;
    }

    private String validateWorkspaceName( RunningState runningState,
                                          String workspaceName ) throws RepositoryException {
        if (workspaceName == null) {
            return runningState.defaultWorkspaceName();
        }
        if (runningState.systemWorkspaceName().equals(workspaceName)) {
            throw new NoSuchWorkspaceException(JcrI18n.workspaceNameIsInvalid.text(repositoryName(), workspaceName));
        }
        return workspaceName;
    }

    protected static class ConfigurationChange implements org.infinispan.schematic.document.Editor.Observer {

        private final Path SECURITY_PATH = Paths.path(FieldName.SECURITY);
        private final Path QUERY_PATH = Paths.path(FieldName.QUERY);
        private final Path SEQUENCING_PATH = Paths.path(FieldName.SEQUENCING);
        private final Path EXTRACTORS_PATH = Paths.path(FieldName.QUERY, FieldName.EXTRACTORS);
        private final Path STORAGE_PATH = Paths.path(FieldName.STORAGE);
        private final Path WORKSPACES_PATH = Paths.path(FieldName.WORKSPACES);
        private final Path PREDEFINED_PATH = Paths.path(FieldName.WORKSPACES, FieldName.PREDEFINED);
        private final Path JNDI_PATH = Paths.path(FieldName.JNDI_NAME);
        private final Path LARGE_VALUE_PATH = Paths.path(FieldName.LARGE_VALUE_SIZE_IN_BYTES);
        private final Path NAME_PATH = Paths.path(FieldName.NAME);
        private final Path MONITORING_PATH = Paths.path(FieldName.MONITORING);

        protected boolean securityChanged = false;
        protected boolean sequencingChanged = false;
        protected boolean extractorsChanged = false;
        protected boolean storageChanged = false;
        protected boolean indexingChanged = false;
        protected boolean workspacesChanged = false;
        protected boolean predefinedWorkspacesChanged = false;
        protected boolean jndiChanged = false;
        protected boolean largeValueChanged = false;
        protected boolean nameChanged = false;
        protected boolean monitoringChanged = false;

        @Override
        public void setArrayValue( Path path,
                                   Array.Entry entry ) {
            checkForChanges(path);
        }

        @Override
        public void addArrayValue( Path path,
                                   Array.Entry entry ) {
            checkForChanges(path);
        }

        @Override
        public void removeArrayValue( Path path,
                                      Array.Entry entry ) {
            checkForChanges(path);
        }

        @Override
        public void clear( Path path ) {
            checkForChanges(path);
        }

        @Override
        public void put( Path parentPath,
                         String field,
                         Object newValue ) {
            checkForChanges(parentPath.with(field));
        }

        @Override
        public void remove( Path path,
                            String field ) {
            checkForChanges(path.with(field));
        }

        private void checkForChanges( Path path ) {
            if (!storageChanged && path.startsWith(STORAGE_PATH)) storageChanged = true;
            if (!sequencingChanged && path.startsWith(SEQUENCING_PATH)) sequencingChanged = true;
            if (!extractorsChanged && path.startsWith(EXTRACTORS_PATH)) extractorsChanged = true;
            if (!securityChanged && path.startsWith(SECURITY_PATH)) securityChanged = true;
            if (!workspacesChanged && path.startsWith(WORKSPACES_PATH) && !path.startsWith(PREDEFINED_PATH)) workspacesChanged = true;
            if (!predefinedWorkspacesChanged && path.startsWith(PREDEFINED_PATH)) predefinedWorkspacesChanged = true;
            if (!indexingChanged && path.startsWith(QUERY_PATH) && !path.startsWith(EXTRACTORS_PATH)) indexingChanged = true;
            if (!jndiChanged && path.equals(JNDI_PATH)) jndiChanged = true;
            if (!largeValueChanged && path.equals(LARGE_VALUE_PATH)) largeValueChanged = true;
            if (!nameChanged && path.equals(NAME_PATH)) nameChanged = true;
            if (!monitoringChanged && path.equals(MONITORING_PATH)) monitoringChanged = true;
        }
    }

    @SuppressWarnings( "deprecation" )
    private void initializeDescriptors() {
        ValueFactories factories = new ExecutionContext().getValueFactories();

        descriptors.put(Repository.LEVEL_1_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.LEVEL_2_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.OPTION_LOCKING_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.OPTION_OBSERVATION_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.OPTION_QUERY_SQL_SUPPORTED, valueFor(factories, true));
        // TODO: Transactions
        descriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_VERSIONING_SUPPORTED, valueFor(factories, true));
        // TODO: Query
        // I think the value should be 'true', since MODE-613 is closed
        descriptors.put(Repository.QUERY_XPATH_DOC_ORDER, valueFor(factories, false)); // see MODE-613
        descriptors.put(Repository.QUERY_XPATH_POS_INDEX, valueFor(factories, true));

        descriptors.put(Repository.WRITE_SUPPORTED, valueFor(factories, true));
        // TODO: Change in 3.0
        descriptors.put(Repository.IDENTIFIER_STABILITY, valueFor(factories, Repository.IDENTIFIER_STABILITY_METHOD_DURATION));
        descriptors.put(Repository.OPTION_XML_IMPORT_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.OPTION_XML_EXPORT_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.OPTION_UNFILED_CONTENT_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_SIMPLE_VERSIONING_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_ACTIVITIES_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_BASELINES_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_ACCESS_CONTROL_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_JOURNALED_OBSERVATION_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_RETENTION_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_LIFECYCLE_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_NODE_AND_PROPERTY_WITH_SAME_NAME_SUPPORTED, valueFor(factories, true));
        // TODO: Change in 3.0
        descriptors.put(Repository.OPTION_UPDATE_PRIMARY_NODE_TYPE_SUPPORTED, valueFor(factories, false));
        descriptors.put(Repository.OPTION_UPDATE_MIXIN_NODE_TYPES_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.OPTION_SHAREABLE_NODES_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.OPTION_NODE_TYPE_MANAGEMENT_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_INHERITANCE,
                        valueFor(factories, Repository.NODE_TYPE_MANAGEMENT_INHERITANCE_MULTIPLE));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_PRIMARY_ITEM_NAME_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_ORDERABLE_CHILD_NODES_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_RESIDUAL_DEFINITIONS_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_AUTOCREATED_DEFINITIONS_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_SAME_NAME_SIBLINGS_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_PROPERTY_TYPES, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_OVERRIDES_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_MULTIVALUED_PROPERTIES_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_MULTIPLE_BINARY_PROPERTIES_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_VALUE_CONSTRAINTS_SUPPORTED, valueFor(factories, true));
        // TODO: Change in 3.0
        descriptors.put(Repository.NODE_TYPE_MANAGEMENT_UPDATE_IN_USE_SUPORTED, valueFor(factories, false));
        descriptors.put(Repository.QUERY_LANGUAGES,
                        new JcrValue[] {valueFor(factories, Query.XPATH), valueFor(factories, Query.JCR_SQL2),
                            valueFor(factories, Query.SQL), valueFor(factories, Query.JCR_JQOM)});
        descriptors.put(Repository.QUERY_STORED_QUERIES_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.QUERY_FULL_TEXT_SEARCH_SUPPORTED, valueFor(factories, true));
        descriptors.put(Repository.QUERY_JOINS, valueFor(factories, Repository.QUERY_JOINS_INNER_OUTER));
        descriptors.put(Repository.SPEC_NAME_DESC, valueFor(factories, JcrI18n.SPEC_NAME_DESC.text()));
        descriptors.put(Repository.SPEC_VERSION_DESC, valueFor(factories, "2.0"));

        descriptors.put(Repository.REP_NAME_DESC, valueFor(factories, ModeShape.getName()));
        descriptors.put(Repository.REP_VENDOR_DESC, valueFor(factories, ModeShape.getVendor()));
        descriptors.put(Repository.REP_VENDOR_URL_DESC, valueFor(factories, ModeShape.getUrl()));
        descriptors.put(Repository.REP_VERSION_DESC, valueFor(factories, ModeShape.getVersion()));
        descriptors.put(Repository.OPTION_WORKSPACE_MANAGEMENT_SUPPORTED, valueFor(factories, true));

        descriptors.put(Repository.REPOSITORY_NAME, valueFor(factories, repositoryName()));
    }

    private static JcrValue valueFor( ValueFactories valueFactories,
                                      int type,
                                      Object value ) {
        return new JcrValue(valueFactories, type, value);
    }

    private static JcrValue valueFor( ValueFactories valueFactories,
                                      String value ) {
        return valueFor(valueFactories, PropertyType.STRING, value);
    }

    private static JcrValue valueFor( ValueFactories valueFactories,
                                      boolean value ) {
        return valueFor(valueFactories, PropertyType.BOOLEAN, value);
    }

    protected void workspacesChanged() {
        RunningState running = runningState();
        if (running != null) {
            Set<String> workspaceNames = running.repositoryCache().getWorkspaceNames();
            ValueFactories factories = running.context().getValueFactories();
            JcrValue[] values = new JcrValue[workspaceNames.size()];
            int i = 0;
            for (String workspaceName : workspaceNames) {
                values[i++] = valueFor(factories, workspaceName);
            }
            descriptors.put(Repository.REPOSITORY_WORKSPACES, values);
        }
    }

    private void repositoryNameChanged() {
        descriptors.put(Repository.REPOSITORY_NAME, repositoryName());
    }

    /**
     * Clean up the repository content's garbage.
     */
    void cleanUp() {
        RunningState running = runningState.get();
        if (running != null) {
            running.cleanUpLocks();
        }
    }

    protected class WorkspaceListener implements ChangeSetListener {
        @Override
        public void notify( ChangeSet changeSet ) {

            if (changeSet == null || !repositoryCache().getKey().equals(changeSet.getRepositoryKey())) return;
            String workspaceKey = changeSet.getWorkspaceKey();
            if (workspaceKey == null) {
                // Look for changes to the workspaces ...
                boolean changed = false;
                for (Change change : changeSet) {
                    if (change instanceof WorkspaceAdded) {
                        changed = true;
                    } else if (change instanceof WorkspaceRemoved) {
                        changed = true;
                    }
                }
                if (changed) workspacesChanged();
            }
        }
    }

    @Immutable
    protected class RunningState {
        private final RepositoryConfiguration config;
        private final SchematicDb database;
        private final RepositoryCache cache;
        private final AuthenticationProviders authenticators;
        private final Credentials anonymousCredentialsIfSuppliedCredentialsFail;
        private final String defaultWorkspaceName;
        private final String systemWorkspaceName;
        private final String systemWorkspaceKey;
        private final RepositoryNodeTypeManager nodeTypes;
        private final RepositoryLockManager lockManager;
        private final TransactionManager txnMgr;
        private final String jndiName;
        private final SystemNamespaceRegistry persistentRegistry;
        private final ExecutionContext context;
        private final ReadWriteLock activeSessionLock = new ReentrantReadWriteLock();
        private final WeakHashMap<JcrSession, Object> activeSessions = new WeakHashMap<JcrSession, Object>();
        private final RepositoryStatistics statistics;
        private final ScheduledExecutorService statsRollupService;

        protected RunningState() throws IOException, NamingException {
            this(null, null);
        }

        protected RunningState( JcrRepository.RunningState other,
                                JcrRepository.ConfigurationChange change ) throws IOException, NamingException {
            this.config = repositoryConfiguration();
            ExecutionContext tempContext = new ExecutionContext();

            // Set up monitoring (doing this early in the process so it is avialable to other components to use) ...
            if (other != null && !change.monitoringChanged) {
                this.statistics = other.statistics;
                this.statsRollupService = other.statsRollupService;
            } else {
                this.statistics = other != null ? other.statistics : new RepositoryStatistics(tempContext);
                if (this.config.getMonitoring().enabled()) {
                    // Start the Cron service, with a minimum of a single thread ...
                    ThreadFactory cronThreadFactory = new NamedThreadFactory("modeshape-stats");
                    this.statsRollupService = new ScheduledThreadPoolExecutor(1, cronThreadFactory);
                    this.statistics.start(this.statsRollupService);
                } else {
                    this.statsRollupService = null;
                }
            }

            logger.debug("Starting '{0}' repository", repositoryName());
            this.systemWorkspaceName = RepositoryConfiguration.SYSTEM_WORKSPACE_NAME;
            this.systemWorkspaceKey = NodeKey.keyForWorkspaceName(this.systemWorkspaceName);

            if (other != null && !change.workspacesChanged) {
                this.defaultWorkspaceName = other.defaultWorkspaceName;
            } else {
                // Set up some of the defaults ...
                this.defaultWorkspaceName = config.getDefaultWorkspaceName();
            }

            if (other != null) {
                if (change.storageChanged) {
                    // Can't change where we're storing the content while we're running, so take effect upon next startup
                    logger.warn(JcrI18n.storageRelatedConfigurationChangesWillTakeEffectAfterShutdown, getName());
                }
                // reuse the existing storage-related components ...
                this.database = other.database;
                this.txnMgr = other.txnMgr;
                this.cache = other.cache;
                this.nodeTypes = other.nodeTypes;
                this.lockManager = other.lockManager;
                this.persistentRegistry = other.persistentRegistry;
                this.context = other.context;
                if (change.largeValueChanged) {
                    // We can update the value used in the repository cache dynamically ...
                    this.cache.setLargeValueSizeInBytes(config.getLargeValueSizeInBytes());
                }
                if (change.workspacesChanged) {
                    // Make sure that all the predefined workspaces are available ...
                    for (String workspaceName : config.getPredefinedWorkspaceNames()) {
                        this.cache.createWorkspace(workspaceName);
                    }
                }
            } else {
                // find the Schematic database and Infinispan Cache ...
                CacheContainer container = config.getCacheContainer();
                String cacheName = config.getCacheName();
                this.database = Schematic.get(container, cacheName);
                assert this.database != null;
                this.txnMgr = this.database.getCache().getAdvancedCache().getTransactionManager();

                // Now create the registry implementation and the execution context that uses it ...
                this.persistentRegistry = new SystemNamespaceRegistry(this);
                this.context = tempContext.with(persistentRegistry);
                this.persistentRegistry.setContext(this.context);

                // Set up the repository cache ...
                SessionCacheMonitor monitor = statsRollupService != null ? new SessionMonitor(this.statistics) : null;
                this.cache = new RepositoryCache(context, database, config, txnMgr, new SystemContentInitializer(), monitor);
                assert this.cache != null;

                // Set up the node type manager ...
                this.nodeTypes = new RepositoryNodeTypeManager(this, true, true);
                this.cache.register(this.nodeTypes);

                // Set up the lock manager ...
                this.lockManager = new RepositoryLockManager(this);
                this.cache.register(this.lockManager);

                // Refresh several of the components information from the repository cache ...
                this.persistentRegistry.refreshFromSystem();
                this.lockManager.refreshFromSystem();
                if (!this.nodeTypes.refreshFromSystem()) {
                    try {
                        // Read in the built-in node types ...
                        CndImporter importer = new CndImporter(context, true);
                        importer.importBuiltIns(new SimpleProblems());
                        this.nodeTypes.registerNodeTypes(importer.getNodeTypeDefinitions(), false, true, true);
                    } catch (RepositoryException re) {
                        throw new IllegalStateException("Could not load node type definition files", re);
                    } catch (IOException ioe) {
                        throw new IllegalStateException("Could not access node type definition files", ioe);
                    }
                }
                // Add the built-ins, ensuring we overwrite any badly-initialized values ...
                this.persistentRegistry.register(JcrNamespaceRegistry.STANDARD_BUILT_IN_NAMESPACES_BY_PREFIX);

                // Record the number of workspaces that are available/predefined ...
                this.statistics.set(ValueMetric.WORKSPACE_COUNT, cache.getWorkspaceNames().size());
            }

            if (other != null && !change.securityChanged) {
                this.authenticators = other.authenticators;
                this.anonymousCredentialsIfSuppliedCredentialsFail = other.anonymousCredentialsIfSuppliedCredentialsFail;
            } else {
                // Set up the security ...
                AtomicBoolean useAnonymouOnFailedLogins = new AtomicBoolean();
                this.authenticators = createAuthenticationProviders(useAnonymouOnFailedLogins);
                this.anonymousCredentialsIfSuppliedCredentialsFail = useAnonymouOnFailedLogins.get() ? new AnonymousCredentials() : null;
            }

            if (other != null && !change.indexingChanged) {

            } else {

            }

            if (other != null && !change.extractorsChanged) {

            } else {

            }

            if (other != null && !change.sequencingChanged) {

            } else {

            }

            if (other != null && !change.jndiChanged) {
                // The repository is already registered (or not registered)
                this.jndiName = other.jndiName;
            } else {
                // The JNDI location has changed, so register the new one ...
                this.jndiName = config.getJndiName();
                bindIntoJndi();

                // And unregister the old name ...
                if (other != null) {
                    other.unbindFromJndi();
                }
            }
        }

        private final ClassLoader classLoader() {
            return database.getCache().getConfiguration().getClassLoader();
        }

        final String name() {
            return repositoryName();
        }

        final ExecutionContext context() {
            return context;
        }

        final RepositoryCache repositoryCache() {
            return cache;
        }

        private final Cache<String, SchematicEntry> infinispanCache() {
            return database().getCache();
        }

        protected final SchematicDb database() {
            return database;
        }

        protected final TransactionManager txnManager() {
            return infinispanCache().getAdvancedCache().getTransactionManager();
        }

        protected final RepositoryNodeTypeManager nodeTypeManager() {
            return nodeTypes;
        }

        protected final RepositoryLockManager lockManager() {
            return lockManager;
        }

        protected final String systemWorkspaceName() {
            return systemWorkspaceName;
        }

        protected final String systemWorkspaceKey() {
            return systemWorkspaceKey;
        }

        protected final String defaultWorkspaceName() {
            return defaultWorkspaceName;
        }

        protected final NamespaceRegistry persistentRegistry() {
            return persistentRegistry;
        }

        protected final AuthenticationProviders authenticators() {
            return authenticators;
        }

        protected final RepositoryStatistics statistics() {
            return statistics;
        }

        protected final Credentials anonymousCredentials() {
            return anonymousCredentialsIfSuppliedCredentialsFail;
        }

        private AuthenticationProviders createAuthenticationProviders( AtomicBoolean useAnonymouOnFailedLogins ) {
            // Prepare to create the authenticators and authorizers ...
            AuthenticationProviders authenticators = new AuthenticationProviders();
            Security securityConfig = config.getSecurity();

            // Set up the JAAS providers ...
            JaasSecurity jaasSecurity = securityConfig.getJaas();
            if (jaasSecurity != null) {
                String policyName = jaasSecurity.getPolicyName();
                if (policyName != null && policyName.trim().length() != 0) {
                    try {
                        JaasProvider jaasProvider = new JaasProvider(policyName);
                        authenticators = authenticators.with(jaasProvider);
                    } catch (java.lang.SecurityException e) {
                        if (MISSING_JAAS_POLICIES.add(policyName)) {
                            logger.warn(JcrI18n.loginConfigNotFound,
                                        policyName,
                                        RepositoryConfiguration.FieldName.SECURITY + "/"
                                        + RepositoryConfiguration.FieldName.JAAS_POLICY_NAME,
                                        repositoryName());
                        }
                    } catch (javax.security.auth.login.LoginException e) {
                        if (MISSING_JAAS_POLICIES.add(policyName)) {
                            logger.warn(JcrI18n.loginConfigNotFound,
                                        policyName,
                                        RepositoryConfiguration.FieldName.SECURITY + "/"
                                        + RepositoryConfiguration.FieldName.JAAS_POLICY_NAME,
                                        repositoryName());
                        }
                    }
                }
            }

            // Set up any custom AuthenticationProvider classes ...
            Problems problems = new SimpleProblems();
            ClassLoader cl = classLoader();
            for (Component component : securityConfig.getCustomProviders(problems)) {
                try {
                    AuthenticationProvider provider = component.createInstance(AuthenticationProvider.class, cl);
                    if (provider != null) {
                        authenticators = authenticators.with(provider);
                        if (provider instanceof AnonymousProvider) {
                            Object value = component.getFields().get(FieldName.USE_ANONYMOUS_ON_FAILED_LOGINS);
                            if (Boolean.TRUE.equals(value)) useAnonymouOnFailedLogins.set(true);
                        }
                    }
                } catch (Throwable t) {
                    logger.error(JcrI18n.unableToInitializeAuthenticationProvider,
                                 component.getName(),
                                 repositoryName(),
                                 t.getMessage());
                }
            }

            // And last set up the anonymous provider ...
            AnonymousSecurity anonSecurity = securityConfig.getAnonymous();
            if (anonSecurity != null) {
                // Set up the anonymous provider (if appropriate) ...
                Set<String> anonRoles = anonSecurity.getAnonymousRoles();
                if (!anonRoles.isEmpty()) {
                    String anonUsername = anonSecurity.getAnonymousUsername();
                    AnonymousProvider anonProvider = new AnonymousProvider(anonUsername, anonRoles);
                    authenticators = authenticators.with(anonProvider);
                    logger.debug("Enabling anonymous authentication and authorization.");
                }
                if (anonSecurity.useAnonymousOnFailedLogings()) {
                    useAnonymouOnFailedLogins.set(true);
                }
            }

            return authenticators;
        }

        final SessionCache createSystemSession( ExecutionContext context,
                                                boolean readOnly ) {
            return cache.createSession(context, systemWorkspaceName(), readOnly);
        }

        protected void shutdown() {
            // Unregister from JNDI ...
            unbindFromJndi();
        }

        protected void bindIntoJndi() {
            if (jndiName != null && jndiName.trim().length() != 0) {
                try {
                    InitialContext ic = new InitialContext();
                    ic.bind(jndiName, JcrRepository.this);
                } catch (NoInitialContextException e) {
                    // No JNDI here ...
                    logger.debug("No JNDI found, so not registering '{0}' repository", getName());
                } catch (NamingException e) {
                    logger.error(e, JcrI18n.unableToBindToJndi, config.getName(), jndiName, e.getMessage());
                }
            }
        }

        protected void unbindFromJndi() {
            if (jndiName != null && jndiName.trim().length() != 0) {
                try {
                    InitialContext ic = new InitialContext();
                    ic.unbind(jndiName);
                } catch (NoInitialContextException e) {
                    // No JNDI here ...
                    logger.debug("No JNDI found, so not registering '{0}' repository", getName());
                } catch (NamingException e) {
                    // Maybe it wasn't registered ??
                    logger.debug(e,
                                 "Error while unregistering the '{0}' repository from the '{1}' name in JNDI",
                                 config.getName(),
                                 jndiName);
                }
            }
        }

        void addSession( JcrSession session ) {
            Lock lock = this.activeSessionLock.writeLock();
            try {
                lock.lock();
                activeSessions.put(session, null);
            } finally {
                lock.unlock();
            }
        }

        void removeSession( JcrSession session ) {
            Lock lock = this.activeSessionLock.writeLock();
            try {
                lock.lock();
                activeSessions.remove(session);
            } finally {
                lock.unlock();
            }
        }

        void terminateSessions() {
            Lock lock = this.activeSessionLock.writeLock();
            try {
                lock.lock();
                for (JcrSession session : this.activeSessions.keySet()) {
                    session.terminate(false); // don't remove from active sessions, as we're blocked and iterating on it ...
                }
                this.activeSessions.clear();
            } finally {
                lock.unlock();
            }
        }

        void cleanUpLocks() {
            if (logger.isDebugEnabled()) {
                logger.trace("Starting lock cleanup in the '{0}' repository", repositoryName());
            }

            // Get the IDs for the active sessions ...
            Set<String> activeSessionIds = new HashSet<String>();
            Lock lock = this.activeSessionLock.writeLock();
            try {
                lock.lock();
                Iterator<Map.Entry<JcrSession, Object>> iter = this.activeSessions.entrySet().iterator();
                while (iter.hasNext()) {
                    JcrSession session = iter.next().getKey();
                    if (session.isLive()) activeSessionIds.add(session.sessionId());
                }
            } finally {
                lock.unlock();
            }

            // Create a system session and delegate to its logic ...
            SessionCache systemSession = createSystemSession(context(), false);
            SystemContent system = new SystemContent(systemSession);
            system.cleanUpLocks(activeSessionIds);
            system.save();

            if (logger.isDebugEnabled()) {
                logger.trace("Finishing lock cleanup in the '{0}' repository", repositoryName());
            }
        }
    }

    protected static class SessionMonitor implements SessionCacheMonitor {
        private final RepositoryStatistics statistics;

        protected SessionMonitor( RepositoryStatistics statistics ) {
            this.statistics = statistics;
        }

        @Override
        public void performChange( long changedNodesCount ) {
            // ValueMetric.SESSION_SAVES are tracked in JcrSession.save() ...
            this.statistics.increment(ValueMetric.NODE_CHANGES, changedNodesCount);
        }
    }

}
