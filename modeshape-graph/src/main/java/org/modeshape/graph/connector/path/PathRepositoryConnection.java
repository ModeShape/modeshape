package org.modeshape.graph.connector.path;

import java.util.concurrent.TimeUnit;
import javax.transaction.xa.XAResource;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.request.Request;

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
        return source.getCachePolicy();
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
     * @see org.modeshape.graph.connector.RepositoryConnection#execute(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.request.Request)
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
        PathRequestProcessor processor = repository.createRequestProcessor(context, source);
        PathRepositoryTransaction txn = processor.getTransaction();

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
