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
package org.modeshape.repository.sequencer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.component.ClassLoaderFactory;
import org.modeshape.common.component.ComponentLibrary;
import org.modeshape.common.component.StandardClassLoaderFactory;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Node;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.observe.ChangeObserver;
import org.modeshape.graph.observe.NetChangeObserver;
import org.modeshape.graph.observe.NetChangeObserver.ChangeType;
import org.modeshape.graph.observe.NetChangeObserver.NetChange;
import org.modeshape.graph.observe.NetChangeObserver.NetChanges;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.repository.RepositoryI18n;
import org.modeshape.repository.RepositoryLibrary;
import org.modeshape.repository.service.AbstractServiceAdministrator;
import org.modeshape.repository.service.AdministeredService;
import org.modeshape.repository.service.ServiceAdministrator;
import org.modeshape.repository.util.RepositoryNodePath;

/**
 * A sequencing system is used to monitor changes in the content of ModeShape repositories and to sequence the content to extract
 * or to generate structured information.
 */
public class SequencingService implements AdministeredService {

    /**
     * Interface used to select the set of {@link Sequencer} instances that should be run.
     * 
     * @author Randall Hauch
     */
    public static interface Selector {

        /**
         * Select the sequencers that should be used to sequence the supplied node.
         * 
         * @param sequencers the list of all sequencers available at the moment; never null
         * @param node the node to be sequenced; never null
         * @param nodeChange the set of node changes; never null
         * @return the list of sequencers that should be used; may not be null
         */
        List<Sequencer> selectSequencers( List<Sequencer> sequencers,
                                          Node node,
                                          NetChange nodeChange );
    }

    /**
     * The default {@link Selector} implementation that selects every sequencer every time it's called, regardless of the node (or
     * logger) supplied.
     * 
     * @author Randall Hauch
     */
    protected static class DefaultSelector implements Selector {

        public List<Sequencer> selectSequencers( List<Sequencer> sequencers,
                                                 Node node,
                                                 NetChange nodeChange ) {
            return sequencers;
        }
    }

    /**
     * The default {@link Selector} that considers every {@link Sequencer} to be used for every node.
     * 
     * @see SequencingService#setSequencerSelector(org.modeshape.repository.sequencer.SequencingService.Selector)
     */
    public static final Selector DEFAULT_SEQUENCER_SELECTOR = new DefaultSelector();

    /**
     * Class loader factory instance that always returns the {@link Thread#getContextClassLoader() current thread's context class
     * loader} (if not null) or component library's class loader.
     */
    protected static final ClassLoaderFactory DEFAULT_CLASSLOADER_FACTORY = new StandardClassLoaderFactory(
                                                                                                           SequencingService.class.getClassLoader());

    /**
     * The administrative component for this service.
     * 
     * @author Randall Hauch
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(RepositoryI18n.sequencingServiceName, State.PAUSED);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doStart( State fromState ) {
            super.doStart(fromState);
            startService();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void doShutdown( State fromState ) {
            super.doShutdown(fromState);
            shutdownService();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean doCheckIsTerminated() {
            return isServiceTerminated();
        }

        /**
         * {@inheritDoc}
         */
        public boolean awaitTermination( long timeout,
                                         TimeUnit unit ) throws InterruptedException {
            return doAwaitTermination(timeout, unit);
        }

    }

    private ExecutionContext executionContext;
    private SequencerLibrary sequencerLibrary = new SequencerLibrary();
    private List<Sequencer> sequencersList = new ArrayList<Sequencer>();
    private Selector sequencerSelector = DEFAULT_SEQUENCER_SELECTOR;
    private ExecutorService executorService;
    private RepositoryLibrary repositoryLibrary;
    private ChangeObserver repositoryObserver;
    private final Statistics statistics = new Statistics();
    private final Administrator administrator = new Administrator();

    /**
     * Create a new sequencing system, configured with no sequencers and not monitoring any workspaces. Upon construction, the
     * system is {@link ServiceAdministrator#isPaused() paused} and must be configured and then
     * {@link ServiceAdministrator#start() started}.
     */
    public SequencingService() {
        this.sequencerLibrary.setClassLoaderFactory(DEFAULT_CLASSLOADER_FACTORY);
    }

    /**
     * Return the administrative component for this service.
     * 
     * @return the administrative component; never null
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * Get the statistics for this system.
     * 
     * @return statistics
     */
    public Statistics getStatistics() {
        return this.statistics;
    }

    /**
     * @return sequencerLibrary
     */
    protected ComponentLibrary<Sequencer, SequencerConfig> getSequencerLibrary() {
        return this.sequencerLibrary;
    }

    /**
     * Add the configuration for a sequencer, or update any existing one that represents the
     * {@link SequencerConfig#equals(Object) same configuration}
     * 
     * @param config the new configuration
     * @return true if the sequencer was added, or false if there already was an existing and
     *         {@link SequencerConfig#hasChanged(SequencerConfig) unchanged} sequencer configuration
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #updateSequencer(SequencerConfig)
     * @see #removeSequencer(SequencerConfig)
     */
    public boolean addSequencer( SequencerConfig config ) {
        return this.sequencerLibrary.add(config);
    }

    /**
     * Get configurations for all known sequencers
     * 
     * @return List of {@link SequencerConfig}s
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #updateSequencer(SequencerConfig)
     * @see #removeSequencer(SequencerConfig)
     */
    public List<SequencerConfig> getSequencers() {
        return this.sequencerLibrary.getSequenceConfigs();
    }

    /**
     * Update the configuration for a sequencer, or add it if there is no {@link SequencerConfig#equals(Object) matching
     * configuration}.
     * 
     * @param config the updated (or new) configuration
     * @return true if the sequencer was updated, or false if there already was an existing and
     *         {@link SequencerConfig#hasChanged(SequencerConfig) unchanged} sequencer configuration
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #addSequencer(SequencerConfig)
     * @see #removeSequencer(SequencerConfig)
     */
    public boolean updateSequencer( SequencerConfig config ) {
        return this.sequencerLibrary.update(config);
    }

    /**
     * Remove the configuration for a sequencer.
     * 
     * @param config the configuration to be removed
     * @return true if the sequencer was removed, or false if there was no existing sequencer
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #addSequencer(SequencerConfig)
     * @see #updateSequencer(SequencerConfig)
     */
    public boolean removeSequencer( SequencerConfig config ) {
        return this.sequencerLibrary.remove(config);
    }

    /**
     * @return executionContext
     */
    public ExecutionContext getExecutionContext() {
        return this.executionContext;
    }

    /**
     * @param executionContext Sets executionContext to the specified value.
     */
    public void setExecutionContext( ExecutionContext executionContext ) {
        CheckArg.isNotNull(executionContext, "execution context");
        if (this.getAdministrator().isStarted()) {
            throw new IllegalStateException(RepositoryI18n.unableToChangeExecutionContextWhileRunning.text());
        }
        this.executionContext = executionContext;
        this.sequencerLibrary.setClassLoaderFactory(executionContext);
    }

    /**
     * Get the repository library to be used for repository lookup
     * 
     * @return the repository library
     */
    public RepositoryLibrary getRepositoryLibrary() {
        return this.repositoryLibrary;
    }

    public void setRepositoryLibrary( RepositoryLibrary repositoryLibrary ) {
        this.repositoryLibrary = repositoryLibrary;
    }

    /**
     * Get the executor service used to run the sequencers.
     * 
     * @return the executor service
     * @see #setExecutorService(ExecutorService)
     */
    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    /**
     * Set the executor service that should be used by this system. By default, the system is set up with a
     * {@link Executors#newSingleThreadExecutor() executor that uses a single thread}.
     * 
     * @param executorService the executor service
     * @see #getExecutorService()
     * @see Executors#newCachedThreadPool()
     * @see Executors#newCachedThreadPool(java.util.concurrent.ThreadFactory)
     * @see Executors#newFixedThreadPool(int)
     * @see Executors#newFixedThreadPool(int, java.util.concurrent.ThreadFactory)
     * @see Executors#newScheduledThreadPool(int)
     * @see Executors#newScheduledThreadPool(int, java.util.concurrent.ThreadFactory)
     * @see Executors#newSingleThreadExecutor()
     * @see Executors#newSingleThreadExecutor(java.util.concurrent.ThreadFactory)
     * @see Executors#newSingleThreadScheduledExecutor()
     * @see Executors#newSingleThreadScheduledExecutor(java.util.concurrent.ThreadFactory)
     */
    public void setExecutorService( ExecutorService executorService ) {
        CheckArg.isNotNull(executorService, "executor service");
        if (this.getAdministrator().isStarted()) {
            throw new IllegalStateException(RepositoryI18n.unableToChangeExecutionContextWhileRunning.text());
        }
        this.executorService = executorService;
    }

    /**
     * Override this method to creates a different kind of default executor service. This method is called when the system is
     * {@link #startService() started} without an executor service being {@link #setExecutorService(ExecutorService) set}.
     * <p>
     * This method creates a {@link Executors#newSingleThreadExecutor() single-threaded executor}.
     * </p>
     * 
     * @return the executor service
     */
    protected ExecutorService createDefaultExecutorService() {
        return Executors.newSingleThreadExecutor(new NamedThreadFactory("sequencing"));
    }

    protected void startService() {
        if (this.getExecutionContext() == null) {
            throw new IllegalStateException(RepositoryI18n.unableToStartSequencingServiceWithoutExecutionContext.text());
        }
        if (this.executorService == null) {
            this.executorService = createDefaultExecutorService();
        }
        assert this.executorService != null;
        assert this.sequencerSelector != null;
        assert this.sequencerLibrary != null;
        assert this.repositoryLibrary != null;
        this.repositoryObserver = new RepositoryObserver();
        // Register the observer ...
        this.repositoryLibrary.register(this.repositoryObserver);
    }

    protected void shutdownService() {
        // Unregister our observer ...
        if (this.repositoryObserver != null) this.repositoryObserver.unregister();
        // And shut down the executor service ..
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
    }

    protected boolean isServiceTerminated() {
        if (this.executorService != null) {
            return this.executorService.isTerminated();
        }
        return true;
    }

    protected boolean doAwaitTermination( long timeout,
                                          TimeUnit unit ) throws InterruptedException {
        if (this.executorService == null || this.executorService.isTerminated()) return true;
        return this.executorService.awaitTermination(timeout, unit);
    }

    /**
     * Get the sequencing selector used by this system.
     * 
     * @return the sequencing selector
     */
    public Selector getSequencerSelector() {
        return this.sequencerSelector;
    }

    /**
     * Set the sequencer selector, or null if the {@link #DEFAULT_SEQUENCER_SELECTOR default sequencer selector} should be used.
     * 
     * @param sequencerSelector the selector
     */
    public void setSequencerSelector( Selector sequencerSelector ) {
        this.sequencerSelector = sequencerSelector != null ? sequencerSelector : DEFAULT_SEQUENCER_SELECTOR;
    }

    /**
     * Do the work of processing by sequencing the node. This method is called by the {@link #executorService executor service}
     * when it performs it's work on the enqueued {@link NetChange NetChange runnable objects}.
     * 
     * @param changes the change describing the node to be processed.
     */
    protected void processChange( NetChanges changes ) {
        final ExecutionContext context = this.getExecutionContext();
        final Logger logger = context.getLogger(getClass());
        assert logger != null;

        try {
            List<Sequencer> allSequencers = null;
            final String repositorySourceName = changes.getSourceName();
            for (NetChange change : changes.getNetChanges()) {
                // Go through each net change, and only process node/property adds and property changes ...
                if (change.includes(ChangeType.NODE_ADDED, ChangeType.PROPERTY_ADDED, ChangeType.PROPERTY_CHANGED)) {
                    final String repositoryWorkspaceName = change.getRepositoryWorkspaceName();

                    // Figure out which sequencers accept this path,
                    // and track which output nodes should be passed to each sequencer...
                    final Path nodePath = change.getPath();
                    final String nodePathStr = context.getValueFactories().getStringFactory().create(nodePath);
                    SequencerCalls sequencerCalls = new SequencerCalls();
                    if (allSequencers == null) {
                        allSequencers = this.sequencerLibrary.getInstances();
                    }
                    List<Sequencer> sequencers = new LinkedList<Sequencer>();
                    for (Sequencer sequencer : allSequencers) {
                        final SequencerConfig config = sequencer.getConfiguration();
                        for (SequencerPathExpression pathExpression : config.getPathExpressions()) {
                            for (Property property : change.getAddedOrModifiedProperties()) {
                                Name propertyName = property.getName();
                                String propertyNameStr = context.getValueFactories().getStringFactory().create(propertyName);
                                String path = repositorySourceName + ":" + repositoryWorkspaceName + ":" + nodePathStr + "/@"
                                              + propertyNameStr;
                                SequencerPathExpression.Matcher matcher = pathExpression.matcher(path);
                                if (matcher.matches()) {
                                    // String selectedPath = matcher.getSelectedPath();
                                    RepositoryNodePath outputPath = RepositoryNodePath.parse(matcher.getOutputPath(),
                                                                                             matcher.getOutputRepositoryName(),
                                                                                             matcher.getOutputWorkspaceName());
                                    SequencerCall call = new SequencerCall(sequencer, propertyNameStr);
                                    // Record the output path ...
                                    sequencerCalls.record(call, outputPath);
                                    sequencers.add(sequencer);
                                    break;
                                }
                            }
                        }
                    }

                    RepositorySource source = repositoryLibrary.getSource(repositorySourceName);
                    Graph sourceGraph = Graph.create(source, context);
                    Node node = null;
                    if (!sequencers.isEmpty()) {

                        // Find the changed node ...
                        node = sourceGraph.getNodeAt(nodePath);

                        // Figure out which sequencers should run ...
                        sequencers = this.sequencerSelector.selectSequencers(sequencers, node, change);
                    }
                    if (sequencers.isEmpty()) {
                        this.statistics.recordNodeSkipped();
                        if (logger.isDebugEnabled()) {
                            logger.trace("Skipping '{0}': no sequencers matched this condition", change);
                        }
                    } else {
                        // Run each of those sequencers ...
                        for (SequencerCall sequencerCall : sequencerCalls) {
                            final Sequencer sequencer = sequencerCall.getSequencer();
                            final String sequencerName = sequencer.getConfiguration().getName();
                            final String propertyName = sequencerCall.getSequencedPropertyName();

                            // Figure out the different output paths for each output source ...
                            Map<String, Set<RepositoryNodePath>> outputPathsBySourceName = sequencerCalls.getOutputPathsFor(sequencerCall);
                            assert !outputPathsBySourceName.isEmpty();

                            // Create a new execution context for the output paths in each output source ...
                            for (Map.Entry<String, Set<RepositoryNodePath>> outputEntry : outputPathsBySourceName.entrySet()) {
                                String sourceName = outputEntry.getKey();
                                Set<RepositoryNodePath> outputPathsInSource = outputEntry.getValue();
                                RepositorySource outputSource = repositoryLibrary.getSource(sourceName);
                                Graph outputGraph = Graph.create(outputSource, context);

                                final SimpleProblems problems = new SimpleProblems();
                                SequencerContext sequencerContext = new SequencerContext(context, sourceGraph, outputGraph,
                                                                                         changes.getTimestamp());
                                try {
                                    sequencer.execute(node, propertyName, change, outputPathsInSource, sequencerContext, problems);
                                    sequencerContext.getDestination().submit();
                                } catch (SequencerException e) {
                                    logger.error(e, RepositoryI18n.errorWhileSequencingNode, sequencerName, change);
                                }
                            }
                        }
                        this.statistics.recordNodeSequenced();
                    }
                }
            }
        } catch (Throwable e) {
            logger.error(e, RepositoryI18n.errorFindingSequencersToRunAgainstNode, changes);
        }
    }

    /**
     * @param sequencersList Sets sequencersList to the specified value.
     */
    public void setSequencersList( List<Sequencer> sequencersList ) {
        this.sequencersList = sequencersList;
    }

    /**
     * @return sequencersList
     */
    public List<Sequencer> getSequencersList() {
        return sequencersList;
    }

    /**
     * The statistics for the system. Each sequencing system has an instance of this class that is updated.
     * 
     * @author Randall Hauch
     */
    @ThreadSafe
    public class Statistics {

        private final AtomicLong numberOfNodesSequenced = new AtomicLong(0);
        private final AtomicLong numberOfNodesSkipped = new AtomicLong(0);
        private final AtomicLong startTime;

        protected Statistics() {
            startTime = new AtomicLong(System.currentTimeMillis());
        }

        public Statistics reset() {
            this.startTime.set(System.currentTimeMillis());
            this.numberOfNodesSequenced.set(0);
            this.numberOfNodesSkipped.set(0);
            return this;
        }

        /**
         * @return the system time when the statistics were started
         */
        public long getStartTime() {
            return this.startTime.get();
        }

        /**
         * @return the number of nodes that were sequenced
         */
        public long getNumberOfNodesSequenced() {
            return this.numberOfNodesSequenced.get();
        }

        /**
         * @return the number of nodes that were skipped because no sequencers applied
         */
        public long getNumberOfNodesSkipped() {
            return this.numberOfNodesSkipped.get();
        }

        protected void recordNodeSequenced() {
            this.numberOfNodesSequenced.incrementAndGet();
        }

        protected void recordNodeSkipped() {
            this.numberOfNodesSkipped.incrementAndGet();
        }
    }

    @Immutable
    protected class SequencerCall {

        private final Sequencer sequencer;
        private final String sequencerName;
        private final String sequencedPropertyName;
        private final int hc;

        protected SequencerCall( Sequencer sequencer,
                                 String sequencedPropertyName ) {
            this.sequencer = sequencer;
            this.sequencerName = sequencer.getConfiguration().getName();
            this.sequencedPropertyName = sequencedPropertyName;
            this.hc = HashCode.compute(this.sequencerName, this.sequencedPropertyName);
        }

        /**
         * @return sequencer
         */
        public Sequencer getSequencer() {
            return this.sequencer;
        }

        /**
         * @return sequencedPropertyName
         */
        public String getSequencedPropertyName() {
            return this.sequencedPropertyName;
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
            if (obj instanceof SequencerCall) {
                SequencerCall that = (SequencerCall)obj;
                if (!this.sequencerName.equals(that.sequencerName)) return false;
                if (!this.sequencedPropertyName.equals(that.sequencedPropertyName)) return false;
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
            return sequencerName + " (" + sequencedPropertyName;
        }
    }

    protected class RepositoryObserver extends NetChangeObserver {
        protected RepositoryObserver() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.NetChangeObserver#notify(org.modeshape.graph.observe.NetChangeObserver.NetChanges)
         */
        @Override
        protected void notify( final NetChanges netChanges ) {
            try {
                getExecutorService().execute(new Runnable() {

                    public void run() {
                        processChange(netChanges);
                    }
                });
            } catch (RejectedExecutionException e) {
                // The executor service has been shut down, so do nothing with this set of changes
            }
        }
    }

    protected class SequencerCalls implements Iterable<SequencerCall> {
        private final Map<SequencerCall, Map<String, Set<RepositoryNodePath>>> sequencerCalls = new HashMap<SequencerCall, Map<String, Set<RepositoryNodePath>>>();

        protected void record( SequencerCall call,
                               RepositoryNodePath outputPath ) {
            assert outputPath != null;
            String sourceName = outputPath.getRepositorySourceName();
            assert sourceName != null;

            // Record the output path ...
            Map<String, Set<RepositoryNodePath>> outputPathsBySourceName = sequencerCalls.get(call);
            if (outputPathsBySourceName == null) {
                outputPathsBySourceName = new HashMap<String, Set<RepositoryNodePath>>();
                sequencerCalls.put(call, outputPathsBySourceName);
            }
            Set<RepositoryNodePath> outputPaths = outputPathsBySourceName.get(sourceName);
            if (outputPaths == null) {
                outputPaths = new HashSet<RepositoryNodePath>();
                outputPathsBySourceName.put(sourceName, outputPaths);
            }
            outputPaths.add(outputPath);
        }

        protected Iterable<SequencerCall> getCalls() {
            return sequencerCalls.keySet();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Iterable#iterator()
         */
        @Override
        public Iterator<SequencerCall> iterator() {
            return sequencerCalls.keySet().iterator();
        }

        protected Map<String, Set<RepositoryNodePath>> getOutputPathsFor( SequencerCall call ) {
            return sequencerCalls.get(call);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return sequencerCalls.toString();
        }
    }
}
