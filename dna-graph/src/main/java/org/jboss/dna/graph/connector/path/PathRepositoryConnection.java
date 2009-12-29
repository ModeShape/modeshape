package org.jboss.dna.graph.connector.path;

import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.jboss.dna.common.statistic.Stopwatch;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.processor.RequestProcessor;

public class PathRepositoryConnection implements RepositoryConnection {

    private final PathRepositorySource source;
    private final PathRepository repository;

    public PathRepositoryConnection( PathRepositorySource source,
                                     PathRepository repository ) {
        assert source != null;
        assert repository != null;
        this.source = source;
        this.repository = repository;
    }

    /**
     * {@inheritDoc}
     */
    public String getSourceName() {
        return source.getName();
    }

    /**
     * {@inheritDoc}
     */
    public CachePolicy getDefaultCachePolicy() {
        return source.getDefaultCachePolicy();
    }

    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean ping( long time,
                         TimeUnit unit ) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.request.Request)
     */
    public void execute( ExecutionContext context,
                         Request request ) throws RepositorySourceException {
        Logger logger = context.getLogger(getClass());
        Stopwatch sw = null;
        if (logger.isTraceEnabled()) {
            sw = new Stopwatch();
            sw.start();
        }

        // Do any commands update/write?
        PathRepositoryTransaction txn = repository.startTransaction(request.isReadOnly());
        RepositoryContext repositoryContext = this.source.getRepositoryContext();
        Observer observer = repositoryContext != null ? repositoryContext.getObserver() : null;
        RequestProcessor processor = new PathRequestProcessor(context, this.repository, observer, source.areUpdatesAllowed());

        boolean commit = true;
        try {
            // Obtain the lock and execute the commands ...
            processor.process(request);
            if (request.hasError() && !request.isReadOnly()) {
                // The changes failed, so we need to rollback so we have 'all-or-nothing' behavior
                commit = false;
            }
        } catch (Throwable error) {
            commit = false;
        } finally {
            try {
                processor.close();
            } finally {
                // Now commit or rollback ...
                try {
                    if (commit) {
                        txn.commit();
                    } else {
                        // Need to rollback the changes made to the repository ...
                        txn.rollback();
                    }
                } catch (Throwable commitOrRollbackError) {
                    if (commit && !request.hasError() && !request.isFrozen()) {
                        // Record the error on the request ...
                        request.setError(commitOrRollbackError);
                    }
                    commit = false; // couldn't do it
                }
                if (commit) {
                    // Now that we've closed our transaction, we can notify the observer of the committed changes ...
                    processor.notifyObserverOfChanges();
                }
            }
        }

        if (logger.isTraceEnabled()) {
            assert sw != null;
            sw.stop();
            logger.trace(getClass().getSimpleName() + ".execute(...) took " + sw.getTotalDuration());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "Connection to the \"" + getSourceName() + "\" " + repository.getClass().getSimpleName();
    }
}
