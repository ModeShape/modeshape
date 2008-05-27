/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.sequencers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.ComponentLibrary;
import org.jboss.dna.common.component.StandardClassLoaderFactory;
import org.jboss.dna.common.monitor.LoggingProgressMonitor;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.monitor.SimpleProgressMonitor;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.repository.observation.NodeChange;
import org.jboss.dna.repository.observation.NodeChangeListener;
import org.jboss.dna.repository.observation.NodeChanges;
import org.jboss.dna.repository.services.AbstractServiceAdministrator;
import org.jboss.dna.repository.services.AdministeredService;
import org.jboss.dna.repository.services.ServiceAdministrator;
import org.jboss.dna.repository.util.ExecutionContext;
import org.jboss.dna.repository.util.JcrTools;
import org.jboss.dna.repository.util.RepositoryNodePath;
import org.jboss.dna.repository.util.SessionFactory;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.ValueFactories;

/**
 * A sequencing system is used to monitor changes in the content of {@link Repository JCR repositories} and to sequence the
 * content to extract or to generate structured information.
 * @author Randall Hauch
 */
public class SequencingService implements AdministeredService, NodeChangeListener {

    /**
     * Interface used to select the set of {@link Sequencer} instances that should be run.
     * @author Randall Hauch
     */
    public static interface Selector {

        /**
         * Select the sequencers that should be used to sequence the supplied node.
         * @param sequencers the list of all sequencers available at the moment; never null
         * @param node the node to be sequenced; never null
         * @param nodeChange the set of node changes; never null
         * @return the list of sequencers that should be used; may not be null
         */
        List<Sequencer> selectSequencers( List<Sequencer> sequencers, Node node, NodeChange nodeChange );
    }

    /**
     * The default {@link Selector} implementation that selects every sequencer every time it's called, regardless of the node (or
     * logger) supplied.
     * @author Randall Hauch
     */
    protected static class DefaultSelector implements Selector {

        public List<Sequencer> selectSequencers( List<Sequencer> sequencers, Node node, NodeChange nodeChange ) {
            return sequencers;
        }
    }

    /**
     * Interface used to determine whether a {@link NodeChange} should be processed.
     * @author Randall Hauch
     */
    public static interface NodeFilter {

        /**
         * Determine whether the node represented by the supplied change should be submitted for sequencing.
         * @param nodeChange the node change event
         * @return true if the node should be submitted for sequencing, or false if the change should be ignored
         */
        boolean accept( NodeChange nodeChange );
    }

    /**
     * The default filter implementation, which accepts only new nodes or nodes that have new or changed properties.
     * @author Randall Hauch
     */
    protected static class DefaultNodeFilter implements NodeFilter {

        public boolean accept( NodeChange nodeChange ) {
            // Only care about new nodes or nodes that have new/changed properies ...
            return nodeChange.includesEventTypes(Event.NODE_ADDED, Event.PROPERTY_ADDED, Event.PROPERTY_CHANGED);
        }
    }

    /**
     * The default {@link Selector} that considers every {@link Sequencer} to be used for every node.
     * @see SequencingService#setSequencerSelector(org.jboss.dna.repository.sequencers.SequencingService.Selector)
     */
    public static final Selector DEFAULT_SEQUENCER_SELECTOR = new DefaultSelector();
    /**
     * The default {@link NodeFilter} that accepts new nodes or nodes that have new/changed properties.
     * @see SequencingService#setSequencerSelector(org.jboss.dna.repository.sequencers.SequencingService.Selector)
     */
    public static final NodeFilter DEFAULT_NODE_FILTER = new DefaultNodeFilter();

    /**
     * Class loader factory instance that always returns the
     * {@link Thread#getContextClassLoader() current thread's context class loader} (if not null) or component library's class
     * loader.
     */
    protected static final ClassLoaderFactory DEFAULT_CLASSLOADER_FACTORY = new StandardClassLoaderFactory(SequencingService.class.getClassLoader());

    /**
     * The administrative component for this service.
     * @author Randall Hauch
     */
    protected class Administrator extends AbstractServiceAdministrator {

        protected Administrator() {
            super(State.PAUSED);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String serviceName() {
            return RepositoryI18n.sequencingServiceName.text();
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
        public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
            return doAwaitTermination(timeout, unit);
        }

    }

    private ExecutionContext executionContext;
    private SequencerLibrary sequencerLibrary = new SequencerLibrary();
    private Selector sequencerSelector = DEFAULT_SEQUENCER_SELECTOR;
    private NodeFilter nodeFilter = DEFAULT_NODE_FILTER;
    private ExecutorService executorService;
    private Logger logger = Logger.getLogger(this.getClass());
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
     * @return the administrative component; never null
     */
    public ServiceAdministrator getAdministrator() {
        return this.administrator;
    }

    /**
     * Get the statistics for this system.
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
     * Get the class loader factory that should be used to load sequencers. By default, this service uses a factory that will
     * return either the {@link Thread#getContextClassLoader() current thread's context class loader} (if not null) or the class
     * loader that loaded this class.
     * @return the class loader factory; never null
     * @see #setClassLoaderFactory(ClassLoaderFactory)
     */
    public ClassLoaderFactory getClassLoaderFactory() {
        return this.sequencerLibrary.getClassLoaderFactory();
    }

    /**
     * Set the Maven Repository that should be used to load the sequencer classes. By default, this service uses a class loader
     * factory that will return either the {@link Thread#getContextClassLoader() current thread's context class loader} (if not
     * null) or the class loader that loaded this class.
     * @param classLoaderFactory the class loader factory reference, or null if the default class loader factory should be used.
     * @see #getClassLoaderFactory()
     */
    public void setClassLoaderFactory( ClassLoaderFactory classLoaderFactory ) {
        this.sequencerLibrary.setClassLoaderFactory(classLoaderFactory != null ? classLoaderFactory : DEFAULT_CLASSLOADER_FACTORY);
    }

    /**
     * Add the configuration for a sequencer, or update any existing one that represents the
     * {@link SequencerConfig#equals(Object) same configuration}
     * @param config the new configuration
     * @return true if the sequencer was added, or false if there already was an existing and
     * {@link SequencerConfig#hasChanged(SequencerConfig) unchanged} sequencer configuration
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #updateSequencer(SequencerConfig)
     * @see #removeSequencer(SequencerConfig)
     */
    public boolean addSequencer( SequencerConfig config ) {
        return this.sequencerLibrary.add(config);
    }

    /**
     * Update the configuration for a sequencer, or add it if there is no
     * {@link SequencerConfig#equals(Object) matching configuration}.
     * @param config the updated (or new) configuration
     * @return true if the sequencer was updated, or false if there already was an existing and
     * {@link SequencerConfig#hasChanged(SequencerConfig) unchanged} sequencer configuration
     * @throws IllegalArgumentException if <code>config</code> is null
     * @see #addSequencer(SequencerConfig)
     * @see #removeSequencer(SequencerConfig)
     */
    public boolean updateSequencer( SequencerConfig config ) {
        return this.sequencerLibrary.update(config);
    }

    /**
     * Remove the configuration for a sequencer.
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
     * Get the logger for this system
     * @return the logger
     */
    public Logger getLogger() {
        return this.logger;
    }

    /**
     * Set the logger for this system.
     * @param logger the logger, or null if the standard logging should be used
     */
    public void setLogger( Logger logger ) {
        this.logger = logger != null ? logger : Logger.getLogger(this.getClass());
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
        ArgCheck.isNotNull(executionContext, "execution context");
        if (this.getAdministrator().isStarted()) {
            throw new IllegalStateException(RepositoryI18n.unableToChangeExecutionContextWhileRunning.text());
        }
        this.executionContext = executionContext;
    }

    /**
     * Get the executor service used to run the sequencers.
     * @return the executor service
     * @see #setExecutorService(ExecutorService)
     */
    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    /**
     * Set the executor service that should be used by this system. By default, the system is set up with a
     * {@link Executors#newSingleThreadExecutor() executor that uses a single thread}.
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
        ArgCheck.isNotNull(executorService, "executor service");
        if (this.getAdministrator().isStarted()) {
            throw new IllegalStateException(RepositoryI18n.unableToChangeExecutionContextWhileRunning.text());
        }
        this.executorService = executorService;
    }

    /**
     * Override this method to creates a different kind of default executor service. This method is called when the system is
     * {@link #start() started} without an executor service being {@link #setExecutorService(ExecutorService) set}.
     * <p>
     * This method creates a {@link Executors#newSingleThreadExecutor() single-threaded executor}.
     * </p>
     * @return
     */
    protected ExecutorService createDefaultExecutorService() {
        return Executors.newSingleThreadExecutor();
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
        assert this.nodeFilter != null;
        assert this.sequencerLibrary != null;
    }

    protected void shutdownService() {
        if (this.executorService != null) {
            this.executorService.shutdown();
        }
    }

    protected boolean doAwaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
        if (this.executorService == null || this.executorService.isTerminated()) return true;
        return this.executorService.awaitTermination(timeout, unit);
    }

    /**
     * Get the sequencing selector used by this system.
     * @return the sequencing selector
     */
    public Selector getSequencerSelector() {
        return this.sequencerSelector;
    }

    /**
     * Set the sequencer selector, or null if the {@link #DEFAULT_SEQUENCER_SELECTOR default sequencer selector} should be used.
     * @param sequencerSelector the selector
     */
    public void setSequencerSelector( Selector sequencerSelector ) {
        this.sequencerSelector = sequencerSelector != null ? sequencerSelector : DEFAULT_SEQUENCER_SELECTOR;
    }

    /**
     * Get the node filter used by this system.
     * @return the node filter
     */
    public NodeFilter getNodeFilter() {
        return this.nodeFilter;
    }

    /**
     * Set the filter that checks which nodes are to be sequenced, or null if the {@link #DEFAULT_NODE_FILTER default node filter}
     * should be used.
     * @param nodeFilter the new node filter
     */
    public void setNodeFilter( NodeFilter nodeFilter ) {
        this.nodeFilter = nodeFilter != null ? nodeFilter : DEFAULT_NODE_FILTER;
    }

    /**
     * {@inheritDoc}
     */
    public void onNodeChanges( NodeChanges changes ) {
        NodeFilter filter = this.getNodeFilter();
        for (final NodeChange changedNode : changes) {
            // Only care about new nodes or nodes that have new/changed properies ...
            if (filter.accept(changedNode)) {
                try {
                    this.executorService.execute(new Runnable() {

                        public void run() {
                            processChangedNode(changedNode);
                        }
                    });
                } catch (RejectedExecutionException e) {
                    // The executor service has been shut down, so do nothing with this set of changes
                }
            }
        }
    }

    /**
     * Do the work of processing by sequencing the node. This method is called by the {@link #executorService executor service}
     * when it performs it's work on the enqueued {@link ChangedNode ChangedNode runnable objects}.
     * @param node the node to be processed.
     */
    protected void processChangedNode( NodeChange changedNode ) {
        try {
            final String repositoryWorkspaceName = changedNode.getRepositoryWorkspaceName();
            Session session = null;
            try {
                // Figure out which sequencers accept this path,
                // and track which output nodes should be passed to each sequencer...
                final String nodePath = changedNode.getAbsolutePath();
                Map<SequencerCall, Set<RepositoryNodePath>> sequencerCalls = new HashMap<SequencerCall, Set<RepositoryNodePath>>();
                List<Sequencer> allSequencers = this.sequencerLibrary.getInstances();
                List<Sequencer> sequencers = new ArrayList<Sequencer>(allSequencers.size());
                for (Sequencer sequencer : allSequencers) {
                    final SequencerConfig config = sequencer.getConfiguration();
                    for (SequencerPathExpression pathExpression : config.getPathExpressions()) {
                        for (String propertyName : changedNode.getModifiedProperties()) {
                            String path = nodePath + "/@" + propertyName;
                            SequencerPathExpression.Matcher matcher = pathExpression.matcher(path);
                            if (matcher.matches()) {
                                // String selectedPath = matcher.getSelectedPath();
                                RepositoryNodePath outputPath = RepositoryNodePath.parse(matcher.getOutputPath(), repositoryWorkspaceName);
                                SequencerCall call = new SequencerCall(sequencer, propertyName);
                                // Record the output path ...
                                Set<RepositoryNodePath> outputPaths = sequencerCalls.get(call);
                                if (outputPaths == null) {
                                    outputPaths = new HashSet<RepositoryNodePath>();
                                    sequencerCalls.put(call, outputPaths);
                                }
                                outputPaths.add(outputPath);
                                sequencers.add(sequencer);
                                break;
                            }
                        }
                    }
                }

                Node node = null;
                if (!sequencers.isEmpty()) {
                    // Create a session that we'll use for all sequencing ...
                    session = this.getExecutionContext().getSessionFactory().createSession(repositoryWorkspaceName);

                    // Find the changed node ...
                    String relPath = changedNode.getAbsolutePath().replaceAll("^/+", "");
                    node = session.getRootNode().getNode(relPath);

                    // Figure out which sequencers should run ...
                    sequencers = this.sequencerSelector.selectSequencers(sequencers, node, changedNode);
                }
                if (sequencers.isEmpty()) {
                    this.statistics.recordNodeSkipped();
                    if (this.logger.isDebugEnabled()) {
                        this.logger.trace("Skipping '{0}': no sequencers matched this condition", changedNode);
                    }
                } else {
                    // Run each of those sequencers ...
                    ProgressMonitor progressMonitor = new SimpleProgressMonitor(RepositoryI18n.sequencerTask.text(changedNode));
                    if (this.logger.isTraceEnabled()) {
                        progressMonitor = new LoggingProgressMonitor(progressMonitor, this.logger, Logger.Level.TRACE);
                    }
                    try {
                        progressMonitor.beginTask(sequencerCalls.size(), RepositoryI18n.sequencerTask, changedNode);
                        for (Map.Entry<SequencerCall, Set<RepositoryNodePath>> entry : sequencerCalls.entrySet()) {
                            final SequencerCall sequencerCall = entry.getKey();
                            final Set<RepositoryNodePath> outputPaths = entry.getValue();
                            final Sequencer sequencer = sequencerCall.getSequencer();
                            final String sequencerName = sequencer.getConfiguration().getName();
                            final String propertyName = sequencerCall.getSequencedPropertyName();

                            // Get the paths to the nodes where the sequencer should write it's output ...
                            assert outputPaths != null && outputPaths.size() != 0;

                            // Create a new execution context for each sequencer
                            final Context executionContext = new Context();
                            final ProgressMonitor sequenceMonitor = progressMonitor.createSubtask(1);
                            try {
                                sequenceMonitor.beginTask(100, RepositoryI18n.sequencerSubtask, sequencerName);
                                sequencer.execute(node, propertyName, changedNode, outputPaths, executionContext, sequenceMonitor.createSubtask(80)); // 80%
                            } catch (RepositoryException e) {
                                this.logger.error(e, RepositoryI18n.errorInRepositoryWhileSequencingNode, sequencerName, changedNode);
                            } catch (SequencerException e) {
                                this.logger.error(e, RepositoryI18n.errorWhileSequencingNode, sequencerName, changedNode);
                            } finally {
                                try {
                                    // Save the changes made by each sequencer ...
                                    if (session != null) session.save();
                                    sequenceMonitor.worked(10); // 90% of sequenceMonitor
                                } finally {
                                    try {
                                        // And always close the context.
                                        // This closes all sessions that may have been created by the sequencer.
                                        executionContext.close();
                                    } finally {
                                        sequenceMonitor.done(); // 100% of sequenceMonitor
                                    }
                                }
                            }
                        }
                        this.statistics.recordNodeSequenced();
                    } finally {
                        progressMonitor.done();
                    }
                }
            } finally {
                if (session != null) session.logout();
            }
        } catch (RepositoryException e) {
            this.logger.error(e, RepositoryI18n.errorInRepositoryWhileFindingSequencersToRunAgainstNode, changedNode);
        } catch (Throwable e) {
            this.logger.error(e, RepositoryI18n.errorFindingSequencersToRunAgainstNode, changedNode);
        }
    }

    protected class Context implements ExecutionContext {

        protected final ExecutionContext delegate;
        protected final SessionFactory factory;
        private final Set<Session> sessions = new HashSet<Session>();
        protected final AtomicBoolean closed = new AtomicBoolean(false);

        protected Context() {
            this.delegate = SequencingService.this.getExecutionContext();
            final SessionFactory delegateSessionFactory = this.delegate.getSessionFactory();
            this.factory = new SessionFactory() {

                public Session createSession( String name ) throws RepositoryException {
                    if (closed.get()) throw new IllegalStateException(RepositoryI18n.executionContextHasBeenClosed.text());
                    Session session = delegateSessionFactory.createSession(name);
                    recordSession(session);
                    return session;
                }
            };
        }

        protected synchronized void recordSession( Session session ) {
            if (session != null) sessions.add(session);
        }

        /**
         * {@inheritDoc}
         */
        public NamespaceRegistry getNamespaceRegistry() {
            return this.delegate.getNamespaceRegistry();
        }

        /**
         * {@inheritDoc}
         */
        public ValueFactories getValueFactories() {
            return this.delegate.getValueFactories();
        }

        /**
         * {@inheritDoc}
         */
        public SessionFactory getSessionFactory() {
            return this.factory;
        }

        /**
         * {@inheritDoc}
         */
        public JcrTools getTools() {
            return SequencingService.this.getExecutionContext().getTools();
        }

        public synchronized void close() {
            if (this.closed.get()) return;
            this.closed.set(true);
            for (Session session : sessions) {
                if (session != null) session.logout();
            }
        }
    }

    /**
     * The statistics for the system. Each sequencing system has an instance of this class that is updated.
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

        protected SequencerCall( Sequencer sequencer, String sequencedPropertyName ) {
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
    }
}
