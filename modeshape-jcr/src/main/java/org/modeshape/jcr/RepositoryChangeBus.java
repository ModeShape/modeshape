package org.modeshape.jcr;

import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A standard {@link ChangeBus} implementation.
 * 
 * @author Horia Chiorean
 */
@ThreadSafe
public class RepositoryChangeBus implements ChangeBus {

    private static final String NULL_WORKSPACE_NAME = "null_workspace_name";

    private final Executor executor;
    private final ConcurrentHashMap<String, BlockingQueue<ChangeSet>> eventQueues;
    private final ConcurrentHashMap<String, EventsDispatcher> eventDispatchers;
    private final Set<ChangeSetListener> listeners;
    private final RepositoryStatistics statistics;

    private final ReadWriteLock listenersLock = new ReentrantReadWriteLock(true);

    //TODO author=Horia Chiorean date=2/29/12 description=Those 2 members and dependent code should be removed,
    //once multi-threaded changes and the system workspace are fixed
    private final String systemWorkspaceName;
    private final boolean separateThreadForSystemWorkspace;

    RepositoryChangeBus( Executor executor,
                         RepositoryStatistics statistics,
                         String systemWorkspaceName,
                         boolean separateThreadForSystemWorkspace ) {
        this.systemWorkspaceName = systemWorkspaceName;
        this.separateThreadForSystemWorkspace = separateThreadForSystemWorkspace;

        this.executor = executor;
        this.eventQueues = new ConcurrentHashMap<String, BlockingQueue<ChangeSet>>();
        this.eventDispatchers = new ConcurrentHashMap<String, EventsDispatcher>();
        this.listeners = new HashSet<ChangeSetListener>();
        this.statistics = statistics;
    }

    RepositoryChangeBus( Executor executor,
                         RepositoryStatistics statistics ) {
        this(executor, statistics, null, false);                
    }

    void shutdown() {
        try {
            eventQueues.clear();
            eventDispatchers.clear();
            listenersLock.writeLock().lock();
            listeners.clear();
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        String workspaceName = changeSet.getWorkspaceName() != null ? changeSet.getWorkspaceName() : NULL_WORKSPACE_NAME;

        if (!separateThreadForSystemWorkspace && workspaceName.equalsIgnoreCase(systemWorkspaceName)) {
            dispatchChanges(changeSet);
            return;
        }
        
        BlockingQueue<ChangeSet> processQue = eventQueues.get(workspaceName);
        if (processQue == null) {
            processQue = new LinkedBlockingQueue<ChangeSet>();
            eventQueues.putIfAbsent(workspaceName, processQue);

            eventDispatchers.putIfAbsent(workspaceName, new EventsDispatcher(processQue));
            executor.execute(eventDispatchers.get(workspaceName));
        }
        processQue.add(changeSet);
        statistics.increment(ValueMetric.EVENT_QUEUE_SIZE);
    }

    @GuardedBy("listenersLock")
    @Override
    public boolean register( ChangeSetListener listener ) {
        if (listener == null) {
            return false;
        }
        try {
            listenersLock.writeLock().lock();
            return listeners.add(listener);
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    @GuardedBy("listenersLock")
    @Override
    public boolean unregister( ChangeSetListener listener ) {
        if (listener == null) {
            return false;
        }
        try {
            listenersLock.writeLock().lock();
            return listeners.remove(listener);
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    private void dispatchChanges( ChangeSet changeSet ) {
        statistics.decrement(ValueMetric.EVENT_QUEUE_SIZE);
        try {
            listenersLock.readLock().lock();
            for (ChangeSetListener listener : listeners) {
                listener.notify(changeSet);
            }
        } finally {
            listenersLock.readLock().unlock();
        }
    }

    private class EventsDispatcher implements Runnable {

        private final BlockingQueue<ChangeSet> eventsQue;

        private EventsDispatcher( BlockingQueue<ChangeSet> eventsQue ) {
            this.eventsQue = eventsQue;
        }

        @Override
        public void run() {
            ChangeSet changeSet;
            try {
                while ((changeSet = eventsQue.take()) != null) {
                    dispatchChanges(changeSet);
                }
            } catch (InterruptedException e) {
                //ignore   
            }
        }
    }

}
