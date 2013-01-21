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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeManager;
import org.infinispan.util.ReflectionUtil;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.HashCode;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.PropertyAdded;
import org.modeshape.jcr.cache.change.PropertyChanged;
import org.modeshape.jcr.cache.change.WorkspaceAdded;
import org.modeshape.jcr.cache.change.WorkspaceRemoved;
import org.modeshape.jcr.sequencer.InvalidSequencerPathExpression;
import org.modeshape.jcr.sequencer.SequencerPathExpression;
import org.modeshape.jcr.sequencer.SequencerPathExpression.Matcher;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.ValueFactory;

/**
 * Component that manages the library of sequencers configured for a repository. Simply instantiate, and register as a
 * {@link ChangeSetListener listener} of cache changes.
 * <p>
 * This class keeps a cache of the {@link SequencerPathExpression} instances (and the corresponding {@link Sequencer}
 * implementation) for each workspace. This is so that it's much easier and more efficient to process the events, which happens
 * very frequently. Also, that structure is a bit backward compared to how the sequencers are defined in the configuration.
 * </p>
 */
@Immutable
public class Sequencers implements ChangeSetListener {

    /** We don't use the standard logging convention here; we want clients to easily configure logging for sequencing */
    private static final Logger LOGGER = Logger.getLogger("org.modeshape.jcr.sequencing.sequencers");
    private static final boolean TRACE = LOGGER.isTraceEnabled();
    private static final boolean DEBUG = LOGGER.isDebugEnabled();

    private final JcrRepository.RunningState repository;
    private final Map<UUID, Sequencer> sequencersById;
    private final Collection<Component> components;
    private final Lock configChangeLock = new ReentrantLock();
    private final Map<UUID, Collection<SequencerPathExpression>> pathExpressionsBySequencerId;
    private volatile Map<String, Collection<SequencingConfiguration>> configByWorkspaceName;
    private final String systemWorkspaceKey;
    private final String processId;
    private final ValueFactory<String> stringFactory;
    private final WorkQueue workQueue;
    private boolean initialized;
    private volatile boolean shutdown = false;

    public Sequencers( JcrRepository.RunningState repository,
                       Collection<Component> components,
                       Iterable<String> workspaceNames,
                       WorkQueue workQueue ) {
        this.repository = repository;
        this.components = components;
        this.workQueue = workQueue;
        this.systemWorkspaceKey = repository.repositoryCache().getSystemKey().getWorkspaceKey();
        if (components.isEmpty()) {
            this.processId = null;
            this.stringFactory = null;
            this.configByWorkspaceName = null;
            this.sequencersById = null;
            this.pathExpressionsBySequencerId = null;
            this.initialized = true;
        } else {
            assert workQueue != null;
            this.processId = repository.context().getProcessId();
            ExecutionContext context = this.repository.context();
            this.stringFactory = context.getValueFactories().getStringFactory();
            this.sequencersById = new HashMap<UUID, Sequencer>();
            this.configByWorkspaceName = new HashMap<String, Collection<SequencingConfiguration>>();
            this.pathExpressionsBySequencerId = new HashMap<UUID, Collection<SequencerPathExpression>>();

            String repoName = repository.name();
            for (Component component : components) {
                try {
                    Sequencer sequencer = component.createInstance(getClass().getClassLoader());
                    // Set the repository name field ...
                    ReflectionUtil.setValue(sequencer, "repositoryName", repoName);

                    // Set the logger instance
                    ReflectionUtil.setValue(sequencer, "logger", ExtensionLogger.getLogger(sequencer.getClass()));
                    // We'll initialize it later in #intialize() ...

                    // For each sequencer, figure out which workspaces apply ...
                    UUID uuid = sequencer.getUniqueId();
                    sequencersById.put(sequencer.getUniqueId(), sequencer);
                    // For each sequencer, create the path expressions ...
                    Set<SequencerPathExpression> pathExpressions = buildPathExpressionSet(sequencer);
                    pathExpressionsBySequencerId.put(uuid, pathExpressions);
                    if (DEBUG) {
                        LOGGER.debug("Created sequencer '{0}' in repository '{1}' with valid path expressions: {2}",
                                     sequencer.getName(),
                                     repository.name(),
                                     pathExpressions);
                    }
                } catch (Throwable t) {
                    if (t.getCause() != null) {
                        t = t.getCause();
                    }
                    LOGGER.error(t, JcrI18n.unableToInitializeSequencer, component, repoName, t.getMessage());
                }
            }
            // Now process each workspace ...
            for (String workspaceName : workspaceNames) {
                workspaceAdded(workspaceName);
            }
            this.initialized = false;
        }
    }

    private Sequencers( Sequencers original,
                        JcrRepository.RunningState repository ) {
        this.repository = repository;
        this.workQueue = original.workQueue;
        this.systemWorkspaceKey = original.systemWorkspaceKey;
        this.processId = original.processId;
        this.stringFactory = repository.context().getValueFactories().getStringFactory();
        this.components = original.components;
        this.sequencersById = original.sequencersById;
        this.configByWorkspaceName = original.configByWorkspaceName;
        this.pathExpressionsBySequencerId = original.pathExpressionsBySequencerId;
    }

    protected Sequencers with( JcrRepository.RunningState repository ) {
        return repository == this.repository ? this : new Sequencers(this, repository);
    }

    protected void initialize() {
        if (initialized) {
            // nothing to do ...
            return;
        }

        // Get a session that we'll pass to the sequencers to use for registering namespaces and node types
        Session session = null;
        try {
            // Get a session that we'll pass to the sequencers to use for registering namespaces and node types
            session = repository.loginInternalSession();
            NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();
            NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

            if (!(nodeTypeManager instanceof org.modeshape.jcr.api.nodetype.NodeTypeManager)) {
                throw new IllegalStateException("Invalid node type manager (expected modeshape NodeTypeManager): "
                                                + nodeTypeManager.getClass().getName());
            }

            // Initialize each sequencer using the supplied session ...
            for (Iterator<Map.Entry<UUID, Sequencer>> sequencersIterator = sequencersById.entrySet().iterator(); sequencersIterator.hasNext();) {
                Sequencer sequencer = sequencersIterator.next().getValue();
                try {
                    sequencer.initialize(registry, (org.modeshape.jcr.api.nodetype.NodeTypeManager)nodeTypeManager);

                    // If successful, call the 'postInitialize' method reflectively (due to inability to call directly) ...
                    Method postInitialize = ReflectionUtil.findMethod(Sequencer.class, "postInitialize");
                    ReflectionUtil.invokeAccessibly(sequencer, postInitialize, new Object[] {});
                    if (DEBUG) {
                        LOGGER.debug("Successfully initialized sequencer '{0}' in repository '{1}'",
                                     sequencer.getName(),
                                     repository.name());
                    }
                } catch (Throwable t) {
                    LOGGER.error(JcrI18n.unableToInitializeSequencer, sequencer, repository.name(), t.getMessage());
                    sequencersIterator.remove();
                }
            }
        } catch (RepositoryException e) {
            throw new SystemFailureException(e);
        } finally {
            if (session != null) {
                session.logout();
            }
        }

    }

    /**
     * Determine if there are no sequencers configured.
     * 
     * @return true if there are no sequencers, or false if there is at least one.
     */
    public boolean isEmpty() {
        return this.components.size() == 0;
    }

    /**
     * Signal that a new workspace was added.
     * 
     * @param workspaceName the workspace name; may not be null
     */
    protected void workspaceAdded( String workspaceName ) {
        String workspaceKey = NodeKey.keyForWorkspaceName(workspaceName);
        if (systemWorkspaceKey.equals(workspaceKey)) {
            // No sequencers for the system workspace!
            return;
        }
        Collection<SequencingConfiguration> configs = new LinkedList<SequencingConfiguration>();
        // Go through the sequencers to see which apply to this workspace ...
        for (Sequencer sequencer : sequencersById.values()) {
            boolean updated = false;
            for (SequencerPathExpression expression : pathExpressionsBySequencerId.get(sequencer.getUniqueId())) {
                if (expression.appliesToWorkspace(workspaceName)) {
                    updated = true;
                    configs.add(new SequencingConfiguration(expression, sequencer));
                }
            }
            if (DEBUG && updated) {
                LOGGER.debug("Updated sequencer '{0}' (id={1}) configuration due to new workspace '{2}' in repository '{3}'",
                             sequencer.getName(),
                             sequencer.getUniqueId(),
                             workspaceName,
                             repository.name());
            }
        }
        if (configs.isEmpty()) return;
        // Otherwise, update the configs by workspace key ...
        try {
            configChangeLock.lock();
            // Make a copy of the existing map ...
            Map<String, Collection<SequencingConfiguration>> configByWorkspaceName = new HashMap<String, Collection<SequencingConfiguration>>(
                                                                                                                                              this.configByWorkspaceName);
            // Insert the new information ...
            configByWorkspaceName.put(workspaceName, configs);
            // Replace the exisiting map (which is used without a lock) ...
            this.configByWorkspaceName = configByWorkspaceName;
        } finally {
            configChangeLock.unlock();
        }
    }

    /**
     * Signal that a new workspace was removed.
     * 
     * @param workspaceName the workspace name; may not be null
     */
    protected void workspaceRemoved( String workspaceName ) {
        // Otherwise, update the configs by workspace key ...
        try {
            configChangeLock.lock();
            // Make a copy of the existing map ...
            Map<String, Collection<SequencingConfiguration>> configByWorkspaceName = new HashMap<String, Collection<SequencingConfiguration>>(
                                                                                                                                              this.configByWorkspaceName);
            // Insert the new information ...
            if (configByWorkspaceName.remove(workspaceName) != null) {
                // Replace the exisiting map (which is used without a lock) ...
                this.configByWorkspaceName = configByWorkspaceName;
            }
        } finally {
            configChangeLock.unlock();
        }
    }

    /**
     * @return stringFactory
     */
    protected final ValueFactory<String> stringFactory() {
        return stringFactory;
    }

    protected final void shutdown() {
        repository.sequencingQueue().shutdown();
        shutdown = true;
    }

    protected final RepositoryStatistics statistics() {
        return repository.statistics();
    }

    protected void submitWork( SequencingConfiguration sequencingConfig,
                               Matcher matcher,
                               String inputWorkspaceName,
                               String propertyName,
                               String userId ) {
        if (shutdown) return;
        // Convert the input path (which has a '@' to denote a property) to a standard JCR path ...
        SequencingWorkItem workItem = new SequencingWorkItem(sequencingConfig.getSequencer().getUniqueId(), userId,
                                                             inputWorkspaceName, matcher.getSelectedPath(),
                                                             matcher.getJcrInputPath(), matcher.getOutputPath(),
                                                             matcher.getOutputWorkspaceName(), propertyName);
        statistics().increment(ValueMetric.SEQUENCER_QUEUE_SIZE);
        workQueue.submit(workItem);
    }

    public Sequencer getSequencer( UUID id ) {
        return sequencersById.get(id);
    }

    protected Set<SequencerPathExpression> buildPathExpressionSet( Sequencer sequencer ) throws InvalidSequencerPathExpression {
        String[] pathExpressions = sequencer.getPathExpressions();
        if (pathExpressions.length == 0) {
            String msg = RepositoryI18n.atLeastOneSequencerPathExpressionMustBeSpecified.text(repository.name(),
                                                                                              sequencer.getName());
            throw new InvalidSequencerPathExpression(msg);
        }

        // Compile the path expressions ...
        Set<SequencerPathExpression> result = new LinkedHashSet<SequencerPathExpression>();
        for (String pathExpression : pathExpressions) {
            assert pathExpression != null;
            assert pathExpression.length() != 0;
            SequencerPathExpression expression = SequencerPathExpression.compile(pathExpression);
            result.add(expression);
        }
        return Collections.unmodifiableSet(result);
    }

    @Immutable
    protected static final class SequencingContext implements Sequencer.Context {
        private final DateTime now;
        private final org.modeshape.jcr.api.ValueFactory valueFactory;

        protected SequencingContext( DateTime now,
                                     org.modeshape.jcr.api.ValueFactory jcrValueFactory ) {
            this.now = now;
            this.valueFactory = jcrValueFactory;
        }

        @Override
        public Calendar getTimestamp() {
            return now.toCalendar();
        }

        @Override
        public org.modeshape.jcr.api.ValueFactory valueFactory() {
            return valueFactory;
        }
    }

    /**
     * This method is called when changes are persisted to the repository. This method quickly looks at the changes and decides
     * which (if any) sequencers should be called, and enqueues any sequencing work in the supplied work queue for subsequent
     * asynchronous processing.
     * 
     * @param changeSet the changes
     */
    @Override
    public void notify( ChangeSet changeSet ) {
        if (this.processId == null) {
            // No sequencers, so return immediately ...
            return;
        }
        if (!processId.equals(changeSet.getProcessKey())) {
            // We didn't generate these changes, so skip them ...
            return;
        }

        final String workspaceName = changeSet.getWorkspaceName();
        final Collection<SequencingConfiguration> configs = this.configByWorkspaceName.get(workspaceName);
        if (configs == null) {
            // No sequencers apply to this workspace ...
            return;
        }

        try {
            // Now process the changes ...
            for (Change change : changeSet) {
                // Look at property added and removed events.
                if (change instanceof PropertyAdded) {
                    PropertyAdded added = (PropertyAdded)change;
                    Path nodePath = added.getPathToNode();
                    String strPath = stringFactory.create(nodePath);
                    Name propName = added.getProperty().getName();
                    // Check if the property is sequencable ...
                    for (SequencingConfiguration config : configs) {
                        Matcher matcher = config.matches(strPath, propName);
                        if (!matcher.matches()) {
                            if (TRACE) {
                                LOGGER.trace("Added property '{1}:{0}' in repository '{2}' did not match sequencer '{3}' and path expression '{4}'",
                                             added.getPath(),
                                             workspaceName,
                                             repository.name(),
                                             config.getSequencer().getName(),
                                             config.getPathExpression());
                            }
                            continue;
                        }
                        if (TRACE) {
                            LOGGER.trace("Submitting added property '{1}:{0}' in repository '{2}' for sequencing using '{3}' and path expression '{4}'",
                                         added.getPath(),
                                         workspaceName,
                                         repository.name(),
                                         config.getSequencer().getName(),
                                         config.getPathExpression());
                        }
                        // The property should be sequenced ...
                        submitWork(config, matcher, workspaceName, stringFactory.create(propName), changeSet.getUserId());
                    }
                } else if (change instanceof PropertyChanged) {
                    PropertyChanged changed = (PropertyChanged)change;
                    Path nodePath = changed.getPathToNode();
                    String strPath = stringFactory.create(nodePath);
                    Name propName = changed.getNewProperty().getName();
                    // Check if the property is sequencable ...
                    for (SequencingConfiguration config : configs) {
                        Matcher matcher = config.matches(strPath, propName);
                        if (!matcher.matches()) {
                            if (TRACE) {
                                LOGGER.trace("Changed property '{1}:{0}' in repository '{2}' did not match sequencer '{3}' and path expression '{4}'",
                                             changed.getPath(),
                                             workspaceName,
                                             repository.name(),
                                             config.getSequencer().getName(),
                                             config.getPathExpression());
                            }
                            continue;
                        }
                        if (TRACE) {
                            LOGGER.trace("Submitting changed property '{1}:{0}' in repository '{2}' for sequencing using '{3}' and path expression '{4}'",
                                         changed.getPath(),
                                         workspaceName,
                                         repository.name(),
                                         config.getSequencer().getName(),
                                         config.getPathExpression());
                        }
                        // The property should be sequenced ...
                        submitWork(config, matcher, workspaceName, stringFactory.create(propName), changeSet.getUserId());
                    }
                }
                // It's possible we should also be looking at other types of events (like property removed or
                // node added/changed/removed events), but this is consistent with the 2.x behavior.

                // Handle the workspace changes ...
                else if (change instanceof WorkspaceAdded) {
                    WorkspaceAdded added = (WorkspaceAdded)change;
                    workspaceAdded(added.getWorkspaceName());
                } else if (change instanceof WorkspaceRemoved) {
                    WorkspaceRemoved removed = (WorkspaceRemoved)change;
                    workspaceRemoved(removed.getWorkspaceName());
                }
            }
        } catch (Throwable e) {
            LOGGER.error(e, JcrI18n.errorCleaningUpLocks, repository.name());
        }
    }

    public static interface WorkQueue {
        void submit( SequencingWorkItem work );
    }

    /**
     * This class represents a single {@link SequencerPathExpression} and the corresponding {@link Sequencer} implementation that
     * should be used if the path expression matches.
     */
    protected final class SequencingConfiguration {
        private final Sequencer sequencer;
        private final SequencerPathExpression pathExpression;

        protected SequencingConfiguration( SequencerPathExpression expression,
                                           Sequencer sequencer ) {
            this.sequencer = sequencer;
            this.pathExpression = expression;
        }

        /**
         * @return pathExpression
         */
        public SequencerPathExpression getPathExpression() {
            return pathExpression;
        }

        /**
         * @return sequencer
         */
        public Sequencer getSequencer() {
            return sequencer;
        }

        /**
         * Determine if this sequencer configuration matches the supplied changed node and property, meaning the changes should be
         * sequenced by this sequencer.
         * 
         * @param pathToChangedNode the path to the added/changed node; may not be null
         * @param changedPropertyName the name of the changed property; may not be null
         * @return the matcher that defines whether there's a match, and if so where the sequenced output is to be
         */
        public Matcher matches( String pathToChangedNode,
                                Name changedPropertyName ) {
            // Put the path in the right form ...
            String absolutePath = pathToChangedNode + "/@" + stringFactory().create(changedPropertyName);
            return pathExpression.matcher(absolutePath);
        }
    }

    @Immutable
    public static final class SequencingWorkItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private final UUID sequencerId;
        private final String inputWorkspaceName;
        private final String selectedPath;
        private final String inputPath;
        private final String changedPropertyName;
        private final String outputPath;
        private final String outputWorkspaceName;
        private final int hc;
        private final String userId;

        protected SequencingWorkItem( UUID sequencerId,
                                      String userId,
                                      String inputWorkspaceName,
                                      String selectedPath,
                                      String inputPath,
                                      String outputPath,
                                      String outputWorkspaceName,
                                      String changedPropertyName ) {
            this.userId = userId;
            this.sequencerId = sequencerId;
            this.inputWorkspaceName = inputWorkspaceName;
            this.selectedPath = selectedPath;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.outputWorkspaceName = outputWorkspaceName;
            this.changedPropertyName = changedPropertyName;
            this.hc = HashCode.compute(this.sequencerId,
                                       this.inputWorkspaceName,
                                       this.inputPath,
                                       this.changedPropertyName,
                                       this.outputPath,
                                       this.outputWorkspaceName);
            assert this.sequencerId != null;
            assert this.inputPath != null;
            assert this.changedPropertyName != null;
            assert this.outputPath != null;
        }

        /**
         * Get the identifier of the sequencer.
         * 
         * @return the sequencer ID; never null
         */
        public UUID getSequencerId() {
            return sequencerId;
        }

        /**
         * Get the id (username) of the user which triggered the sequencing
         * 
         * @return the user id, never null
         */
        public String getUserId() {
            return userId;
        }

        /**
         * Get the name of the workspace where the input exists.
         * 
         * @return the input workspace name; never null
         */
        public String getInputWorkspaceName() {
            return inputWorkspaceName;
        }

        /**
         * Get the input path of the node/property that is to be sequenced.
         * 
         * @return the input path; never null
         */
        public String getInputPath() {
            return inputPath;
        }

        /**
         * Get the path of the selected node that is to be sequenced.
         * 
         * @return the selected path; never null
         */
        public String getSelectedPath() {
            return selectedPath;
        }

        /**
         * Get the name of the changed property.
         * 
         * @return the name of the property that was changed; never null
         */
        public String getChangedPropertyName() {
            return changedPropertyName;
        }

        /**
         * Get the path for the sequencing output.
         * 
         * @return the output path; never null
         */
        public String getOutputPath() {
            return outputPath;
        }

        /**
         * Get the name of the workspace where the output is to be written.
         * 
         * @return the output workspace name; may be null if the output is to be written to the same workspace as the input
         */
        public String getOutputWorkspaceName() {
            return outputWorkspaceName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.hc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SequencingWorkItem) {
                SequencingWorkItem that = (SequencingWorkItem)obj;
                if (this.hc != that.hc) return false;
                if (!this.sequencerId.equals(that.sequencerId)) return false;
                if (!this.inputWorkspaceName.equals(that.inputWorkspaceName)) return false;
                if (!this.inputPath.equals(that.inputPath)) return false;
                if (!this.outputPath.equals(that.outputWorkspaceName)) return false;
                if (!this.outputWorkspaceName.equals(that.outputWorkspaceName)) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return sequencerId + " @ " + inputPath + " -> " + outputPath
                   + (outputWorkspaceName != null ? (" in workspace '" + outputWorkspaceName + "'") : "");
        }
    }

}
