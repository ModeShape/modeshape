package org.modeshape.jcr.api;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface RepositoryFactory {

    public javax.jcr.Repository getRepository( Map<String, String> parameters );

    /**
     * Begin the shutdown process for all the {@code JcrEngine JcrEngines} created by calls to {@link #getRepository(Map)}.
     * <p>
     * Calling {@code #getRepository(Map)} with a file-based URL parameter causes a new {@code JcrEngine} to be instantiated and
     * started. Any {@code JcrEngine} created in this manner must be stored by the {@code RepositoryFactory} implementation.
     * Invoking this method iteratively invokes the {@code shutdown()} method on each {@code JcrEngine}.
     * </p>
     * <p>
     * This method merely initiates the shutdown process for each {@code JcrEngine}. There is no guarantee that the shutdown
     * process will have completed prior to this method returning. The {@link #shutdown(long, TimeUnit)} method provides the
     * ability to wait until all engines are shutdown or the given time elapses.
     * </p>
     * <p>
     * Invoking this method does not preclude creating new {@code JcrEngines} with future calls to {@link #getRepository(Map)}.
     * Any caller using this method as part of an application shutdown process should take care to cease invocations of
     * {@link #getRepository(Map)} prior to invoking this method.
     * </p>
     */
    public void shutdown();

    /**
     * Begin the shutdown process for all the {@code JcrEngine JcrEngines} created by calls to {@link #getRepository(Map)}.
     * <p>
     * Calling {@code #getRepository(Map)} with a file-based URL parameter causes a new {@code JcrEngine} to be instantiated and
     * started. Any {@code JcrEngine} created in this manner must be stored by the {@code RepositoryFactory} implementation.
     * Invoking this method iteratively invokes the {@code shutdown()} method on each {@code JcrEngine} and then iteratively
     * invokes the {@code awaitTermination(long, TimeUnit)} method to await termination.
     * </p>
     * <p>
     * Although this method initiates the shutdown process for each {@code JcrEngine} and invokes the {@code awaitTermination}
     * method, there is still no guarantee that the shutdown process will have completed prior to this method returning. It is
     * possible for the time required to shutdown one or more of the engines to exceed the provided time window.
     * </p>
     * <p>
     * Invoking this method does not preclude creating new {@code JcrEngines} with future calls to {@link #getRepository(Map)}.
     * Any caller using this method as part of an application shutdown process should take care to cease invocations of
     * {@link #getRepository(Map)} prior to invoking this method.
     * </p>
     * 
     * @param timeout the maximum time per engine to allow for shutdown
     * @param unit the time unit of the timeout argument
     * @return <tt>true</tt> if all engines completely shut down and <tt>false</tt> if the timeout elapsed before it was shut down
     *         completely
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean shutdown( long timeout,
                          TimeUnit unit ) throws InterruptedException;

}
