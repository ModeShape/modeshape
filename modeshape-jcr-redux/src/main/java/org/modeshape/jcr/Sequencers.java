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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.infinispan.schematic.document.Null;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.api.sequencer.InvalidSequencerPathExpression;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.jcr.api.sequencer.SequencerContext;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.PropertyAdded;
import org.modeshape.jcr.cache.change.PropertyChanged;
import org.modeshape.jcr.cache.change.WorkspaceAdded;
import org.modeshape.jcr.cache.change.WorkspaceRemoved;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.sequencer.SequencerPathExpression;
import org.modeshape.jcr.sequencer.SequencerPathExpression.Matcher;
import org.modeshape.jcr.value.DateTime;
import org.modeshape.jcr.value.DateTimeFactory;
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
public class Sequencers implements ChangeSetListener {

    private final JcrRepository.RunningState repository;
    private final Map<String, Sequencer> sequencersByName;
    private final Collection<Component> components;
    private final Lock configChangeLock = new ReentrantLock();
    private final Map<String, Collection<SequencerPathExpression>> pathExpressionsBySequencerName;
    private volatile Map<String, Collection<SequencingConfiguration>> configByWorkspaceName;
    private final String systemWorkspaceKey;
    private final String processId;
    private final ValueFactory<String> stringFactory;
    private final DateTimeFactory dateFactory;
    private final WorkQueue workQueue;
    private final Logger logger;

    public Sequencers( JcrRepository.RunningState repository,
                       Collection<Component> components,
                       Iterable<String> workspaceNames,
                       WorkQueue workQueue ) {
        this.repository = repository;
        this.components = components;
        this.workQueue = workQueue;
        this.systemWorkspaceKey = repository.repositoryCache().getSystemKey().getWorkspaceKey();
        this.logger = Logger.getLogger(getClass());
        if (components.isEmpty()) {
            this.processId = null;
            this.stringFactory = null;
            this.dateFactory = null;
            this.configByWorkspaceName = null;
            this.sequencersByName = null;
            this.pathExpressionsBySequencerName = null;
        } else {
            assert workQueue != null;
            this.processId = repository.context().getProcessId();
            ExecutionContext context = this.repository.context();
            this.stringFactory = context.getValueFactories().getStringFactory();
            this.dateFactory = context.getValueFactories().getDateFactory();
            this.sequencersByName = new HashMap<String, Sequencer>();
            this.configByWorkspaceName = new HashMap<String, Collection<SequencingConfiguration>>();
            this.pathExpressionsBySequencerName = new HashMap<String, Collection<SequencerPathExpression>>();

            ClassLoader defaultClassLoader = getClass().getClassLoader();
            String repoName = repository.name();
            for (Component component : components) {
                String name = component.getName();
                try {
                    ClassLoader cl = context.getClassLoader(component.getClasspath());
                    if (cl == null) cl = defaultClassLoader;
                    Sequencer sequencer = component.createInstance(Sequencer.class, cl);
                    if (sequencer != null) {
                        sequencer.initialize(name, repoName);
                        // For each sequencer, figure out which workspaces apply ...
                        sequencersByName.put(name, sequencer);
                        // For each sequencer, create the path expressions ...
                        Set<SequencerPathExpression> pathExpressions = buildPathExpressionSet(sequencer);
                        pathExpressionsBySequencerName.put(name, pathExpressions);
                    } else {
                        logger.error(JcrI18n.unableToInitializeSequencer, name, repoName, "");
                    }
                } catch (Throwable t) {
                    logger.error(JcrI18n.unableToInitializeSequencer, name, repoName, t.getMessage());
                }
            }
            // Now process each workspace ...
            for (String workspaceName : workspaceNames) {
                workspaceAdded(workspaceName);
            }
        }
    }

    private Sequencers( Sequencers original,
                        JcrRepository.RunningState repository ) {
        this.repository = repository;
        this.workQueue = original.workQueue;
        this.systemWorkspaceKey = original.systemWorkspaceKey;
        this.processId = original.processId;
        this.stringFactory = repository.context().getValueFactories().getStringFactory();
        this.dateFactory = repository.context().getValueFactories().getDateFactory();
        this.components = original.components;
        this.sequencersByName = original.sequencersByName;
        this.configByWorkspaceName = original.configByWorkspaceName;
        this.pathExpressionsBySequencerName = original.pathExpressionsBySequencerName;
        this.logger = original.logger;
    }

    protected Sequencers with( JcrRepository.RunningState repository ) {
        return repository == this.repository ? this : new Sequencers(this, repository);
    }

    protected DateTime now() {
        return dateFactory.create();
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
        for (Sequencer sequencer : sequencersByName.values()) {
            for (SequencerPathExpression expression : pathExpressionsBySequencerName.get(sequencer.getName())) {
                if (expression.appliesToWorkspace(workspaceName)) {
                    configs.add(new SequencingConfiguration(expression, sequencer));
                }
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

    protected void submitWork( SequencingConfiguration sequencingConfig,
                               Matcher matcher,
                               String inputWorkspaceName ) {
        // Conver the input path (which has a '@' to denote a property) to a standard JCR path ...
        SequencingWorkItem workItem = new SequencingWorkItem(sequencingConfig.getSequencer().getName(), inputWorkspaceName,
                                                             matcher.getJcrInputPath(), matcher.getOutputPath(),
                                                             matcher.getOutputWorkspaceName());
        workQueue.submit(workItem);
    }

    public void perform( SequencingWorkItem workItem,
                         Session inputSession,
                         Session outputSession ) throws RepositoryException {
        // Get the sequencer ...
        Sequencer sequencer = sequencersByName.get(workItem.getSequencerName());
        if (sequencer != null) {

            // Find the input that has changed and is to be sequenced ...
            Item inputItem = inputSession.getItem(workItem.getInputPath());
            Property changedProperty = null;
            Node changedNode = null;
            if (inputItem instanceof Property) {
                changedProperty = (Property)inputItem;
                changedNode = changedProperty.getParent();
            } else {
                changedNode = (Node)inputItem;
            }

            // Find the parent of the output ...
            Node parentOfOutput = outputSession.getNode(workItem.getOutputPath());

            // Execute the sequencer ...
            String repositoryName = repository.name();
            SequencerContext context = new SequencingContext(repositoryName, now());
            sequencer.execute(changedNode, changedProperty, parentOfOutput, context);
        }
    }

    protected Set<SequencerPathExpression> buildPathExpressionSet( Sequencer sequencer ) throws InvalidSequencerPathExpression {

        String pathExpression = sequencer.getPathExpression();
        Object[] pathExpressions = sequencer.getPathExpressions();

        if (pathExpression != null) {
            return buildPathExpressionSet(pathExpression);
        } else if (pathExpressions != null) {
            String[] strings = new String[pathExpressions.length];
            int i = 0;
            for (Object value : pathExpressions) {
                if (Null.matches(value)) continue;
                strings[i++] = value.toString();
            }
            return buildPathExpressionSet(strings);
        }
        String msg = RepositoryI18n.atLeastOneSequencerPathExpressionMustBeSpecified.text(repository.name(), sequencer.getName());
        throw new InvalidSequencerPathExpression(msg);
    }

    private Set<SequencerPathExpression> buildPathExpressionSet( String... pathExpressions )
        throws InvalidSequencerPathExpression {

        Set<SequencerPathExpression> result = null;
        if (pathExpressions != null) {
            result = new LinkedHashSet<SequencerPathExpression>();
            for (String pathExpression : pathExpressions) {
                if (pathExpression == null) continue;
                pathExpression = pathExpression.trim();
                if (pathExpression.length() == 0) continue;
                SequencerPathExpression expression = SequencerPathExpression.compile(pathExpression);
                result.add(expression);
            }
            result = Collections.unmodifiableSet(result);
        } else {
            result = Collections.emptySet(); // already immutable
        }
        return result;
    }

    protected static final class SequencingContext implements SequencerContext {
        private final String repositoryName;
        private final DateTime now;

        protected SequencingContext( String repositoryName,
                                     DateTime now ) {
            this.repositoryName = repositoryName;
            this.now = now;
        }

        @Override
        public String getRepositoryName() {
            return repositoryName;
        }

        @Override
        public Calendar getTimestamp() {
            return now.toCalendar();
        }

        @Override
        public String toString() {
            return repositoryName + " @ " + now.toString();
        }
    }

    /**
     * This method is called when changes are persisted to the repository. This method quickly looks at the changes and decides
     * which (if any) sequencers should be called, and enqueues any sequencing work in the supplied work queue for
     * {@link #perform processing} on one or more separate thread.
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
                        if (!matcher.matches()) continue;
                        // The property should be sequenced ...
                        submitWork(config, matcher, workspaceName);
                    }
                } else if (change instanceof PropertyChanged) {
                    PropertyChanged changed = (PropertyChanged)change;
                    Path nodePath = changed.getPathToNode();
                    String strPath = stringFactory.create(nodePath);
                    Name propName = changed.getNewProperty().getName();
                    // Check if the property is sequencable ...
                    for (SequencingConfiguration config : configs) {
                        Matcher matcher = config.matches(strPath, propName);
                        if (!matcher.matches()) continue;
                        // The property should be sequenced ...
                        submitWork(config, matcher, workspaceName);
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
            logger.error(e, JcrI18n.errorCleaningUpLocks, repository.name());
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

        private final String sequencerName;
        private final String inputWorkspaceName;
        private final String inputPath;
        private final String outputPath;
        private final String outputWorkspaceName;
        private final int hc;

        protected SequencingWorkItem( String sequencerName,
                                      String inputWorkspaceName,
                                      String inputPath,
                                      String outputPath,
                                      String outputWorkspaceName ) {
            this.sequencerName = sequencerName;
            this.inputWorkspaceName = inputWorkspaceName;
            this.inputPath = inputPath;
            this.outputPath = outputPath;
            this.outputWorkspaceName = outputWorkspaceName;
            this.hc = HashCode.compute(this.sequencerName,
                                       this.inputWorkspaceName,
                                       this.inputPath,
                                       this.outputPath,
                                       this.outputWorkspaceName);
            assert this.sequencerName != null;
            assert this.inputPath != null;
            assert this.outputPath != null;
        }

        /**
         * Get the name of the sequencer.
         * 
         * @return the sequencer name; never null
         */
        public String getSequencerName() {
            return sequencerName;
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
                if (!this.sequencerName.equals(that.sequencerName)) return false;
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
            return sequencerName + " @ " + inputPath + " -> " + outputPath
                   + (outputWorkspaceName != null ? (" in workspace '" + outputWorkspaceName + "'") : "");
        }
    }

}
