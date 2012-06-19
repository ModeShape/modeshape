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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.Editor;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ImmediateFuture;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.jcr.api.Repositories;

/**
 * A container for repositories.
 */
@ThreadSafe
public class ModeShapeEngine implements Repositories {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, JcrRepository> repositories = new HashMap<String, JcrRepository>();
    private ExecutorService repositoryStarterService;
    private ScheduledExecutorService cron;
    private volatile State state = State.NOT_RUNNING;

    public enum State {
        NOT_RUNNING,
        STARTING,
        RUNNING,
        STOPPING;
    }

    public ModeShapeEngine() {
    }

    protected final boolean checkRunning() {
        if (state == State.RUNNING) return true;
        throw new IllegalStateException(JcrI18n.engineIsNotRunning.text());
    }

    /**
     * Get the running state of this engine.
     * 
     * @return the current state; never null
     */
    public State getState() {
        return state;
    }

    /**
     * Start this engine to make it available for use. This method does nothing if the engine is already running.
     * 
     * @see #shutdown()
     */
    public void start() {
        if (state == State.RUNNING) return;
        final Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            this.state = State.STARTING;

            // Create an executor service that we'll use to start the repositories ...
            ThreadFactory threadFactory = new NamedThreadFactory("modeshape-start-repo");
            repositoryStarterService = Executors.newCachedThreadPool(threadFactory);

            // Start the Cron service, with a minimum of a single thread ...
            ThreadFactory cronThreadFactory = new NamedThreadFactory("modeshape-cron");
            cron = new ScheduledThreadPoolExecutor(1, cronThreadFactory);

            // Add a Cron job that cleans up each repository ...
            cron.scheduleAtFixedRate(new GarbageCollectionTask(),
                                     RepositoryConfiguration.GARBAGE_COLLECTION_SWEEP_PERIOD,
                                     RepositoryConfiguration.GARBAGE_COLLECTION_SWEEP_PERIOD,
                                     TimeUnit.MILLISECONDS);

            state = State.RUNNING;
        } catch (RuntimeException e) {
            state = State.NOT_RUNNING;
            throw e;
        } finally {
            lock.unlock();
        }
    }

    protected class GarbageCollectionTask implements Runnable {
        @Override
        public void run() {
            for (JcrRepository repository : repositories()) {
                // Okay to call, even if not running ...
                repository.cleanUp();
            }
        }
    }

    /**
     * Shutdown this engine to stop all repositories, terminate any ongoing background operations (such as sequencing), and
     * reclaim any resources that were acquired by this engine. This method may be called multiple times, but only the first time
     * has an effect.
     * <p>
     * This is equivalent to calling <code>shutdown(true)</code>
     * 
     * @return a future that allows the caller to block until the engine is shutdown; any error during shutdown will be thrown
     *         when {@link Future#get() getting} the result from the future, where the exception is wrapped in a
     *         {@link ExecutionException}. The value returned from the future will always be true if the engine shutdown (or was
     *         not running), or false if the engine is still running.
     * @see #start()
     */
    public Future<Boolean> shutdown() {
        return shutdown(true);
    }

    /**
     * Shutdown this engine, optionally stopping all still-running repositories.
     * 
     * @param forceShutdownOfAllRepositories true if the engine should be shutdown even if there are currently-running
     *        repositories, or false if the engine should not be shutdown if at least one repository is still running.
     * @return a future that allows the caller to block until the engine is shutdown; any error during shutdown will be thrown
     *         when {@link Future#get() getting} the repository from the future, where the exception is wrapped in a
     *         {@link ExecutionException}. The value returned from the future will always be true if the engine shutdown (or was
     *         not running), or false if the engine is still running.
     * @see #start()
     */
    public Future<Boolean> shutdown( boolean forceShutdownOfAllRepositories ) {
        if (!forceShutdownOfAllRepositories) {
            // Check to see if there are any still running ...
            final Lock lock = this.lock.readLock();
            try {
                lock.lock();
                for (JcrRepository repository : repositories.values()) {
                    switch (repository.getState()) {
                        case NOT_RUNNING:
                        case STOPPING:
                            break;
                        case RUNNING:
                        case STARTING:
                            // This repository is still running, so fail
                            return ImmediateFuture.create(Boolean.FALSE);
                    }
                }
                // If we got to here, there are no more running repositories ...
            } finally {
                lock.unlock();
            }

        }
        // Create a simple executor that will do the backgrounding for us ...
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            // Submit a runnable to shutdown the repositories ...
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
     * Do the work of shutting down this engine and its repositories.
     * 
     * @return true if the engine was shutdown (or was not running), and false if the engine is still running and could not be
     *         shutdown
     */
    protected boolean doShutdown() {
        if (state == State.NOT_RUNNING) return true;
        final Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            state = State.STOPPING;

            if (!repositories.isEmpty()) {
                // Now go through all of the repositories and request they all be shutdown ...
                Queue<Future<Boolean>> repoFutures = new LinkedList<Future<Boolean>>();
                Queue<String> repoNames = new LinkedList<String>();
                for (JcrRepository repository : repositories.values()) {
                    if (repository != null) {
                        repoNames.add(repository.getName());
                        repoFutures.add(repository.shutdown());
                    }
                }

                // Now block while each is shutdown ...
                while (repoFutures.peek() != null) {
                    String repoName = repoNames.poll();
                    try {
                        // Get the results from the future (this will return only when the shutdown has completed) ...
                        repoFutures.poll().get();

                        // We've successfully shut down, so remove it from the map ...
                        repositories.remove(repoName);
                    } catch (ExecutionException e) {
                        Logger.getLogger(getClass()).error(e, JcrI18n.failedToShutdownDeployedRepository, repoName);
                    } catch (InterruptedException e) {
                        Logger.getLogger(getClass()).error(e, JcrI18n.failedToShutdownDeployedRepository, repoName);
                    }
                }
            }

            if (repositories.isEmpty()) {
                // All repositories were properly shutdown, so now stop the service for starting and shutting down the repos ...
                repositoryStarterService.shutdown();

                // Do not clear the set of repositories, so that restarting will work just fine ...
                this.state = State.NOT_RUNNING;
                repositoryStarterService = null;
            } else {
                // Could not shut down all repositories, so keep running ..
                this.state = State.RUNNING;
            }
        } catch (RuntimeException e) {
            this.state = State.RUNNING;
            throw e;
        } finally {
            lock.unlock();
        }
        return true;
    }

    /**
     * Get the deployed {@link Repository} instance with the given the name.
     * 
     * @param repositoryName the name of the deployed repository
     * @return the named repository instance
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws NoSuchRepositoryException if there is no repository with the specified name
     * @throws IllegalStateException if this engine is not {@link #getState() running}
     * @see #deploy(RepositoryConfiguration)
     * @see #undeploy(String)
     */
    @Override
    public final JcrRepository getRepository( String repositoryName ) throws NoSuchRepositoryException {
        CheckArg.isNotEmpty(repositoryName, "repositoryName");
        checkRunning();

        final Lock lock = this.lock.readLock();
        try {
            lock.lock();
            JcrRepository repository = repositories.get(repositoryName);
            if (repository == null) {
                throw new NoSuchRepositoryException(JcrI18n.repositoryDoesNotExist.text(repositoryName));
            }
            return repository;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Set<String> getRepositoryNames() {
        checkRunning();

        final Lock lock = this.lock.readLock();
        try {
            lock.lock();
            Set<String> names = new HashSet<String>();
            for (JcrRepository repository : repositories.values()) {
                names.add(repository.getName());
            }
            return names;
        } finally {
            lock.unlock();
        }
    }

    protected Set<String> getRepositoryKeys() {
        checkRunning();

        final Lock lock = this.lock.readLock();
        try {
            lock.lock();
            return new HashSet<String>(repositories.keySet());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the state of the deployed {@link Repository} instance with the given the name.
     * 
     * @param repositoryName the name of the deployed repository
     * @return the state of the repository instance; never null
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws NoSuchRepositoryException if there is no repository with the specified name
     * @throws IllegalStateException if this engine is not {@link #getState() running}
     * @see #deploy(RepositoryConfiguration)
     * @see #undeploy(String)
     */
    public final State getRepositoryState( String repositoryName ) throws NoSuchRepositoryException {
        return getRepository(repositoryName).getState();
    }

    /**
     * Get the immutable configuration for the repository with the supplied name.
     * 
     * @param repositoryName the name of the deployed repository
     * @return the repository configuration; never null
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws NoSuchRepositoryException if there is no repository with the specified name
     * @throws IllegalStateException if this engine is not {@link #getState() running}
     */
    public final RepositoryConfiguration getRepositoryConfiguration( String repositoryName ) throws NoSuchRepositoryException {
        return getRepository(repositoryName).getConfiguration();
    }

    /**
     * Asynchronously start the deployed {@link Repository} instance with the given the name, and return a future that will return
     * the Repository instance. If the Repository is already running, this method returns a future that returns immediately.
     * <p>
     * Note that the caller does not have to wait for the startup process to complete. However, to do so the caller merely calls
     * {@link Future#get() get()} or {@link Future#get(long, TimeUnit) get(long,TimeUnit)} on the future to return the repository
     * instance. Note that any exceptions thrown during the startup process will be wrapped in an {@link ExecutionException}
     * thrown by the Future's <code>get</code> methods.
     * </p>
     * 
     * @param repositoryName the name of the deployed repository
     * @return the state of the repository instance; never null
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws NoSuchRepositoryException if there is no repository with the specified name
     * @throws IllegalStateException if this engine is not {@link #getState() running}
     * @see #deploy(RepositoryConfiguration)
     * @see #undeploy(String)
     */
    public final Future<JcrRepository> startRepository( String repositoryName ) throws NoSuchRepositoryException {
        final JcrRepository repository = getRepository(repositoryName);
        if (repository.getState() == State.RUNNING) {
            return ImmediateFuture.create(repository);
        }

        // Create an initializer that will start the repository ...
        return repositoryStarterService.submit(new Callable<JcrRepository>() {
            @Override
            public JcrRepository call() throws Exception {
                // Instantiate (and start) the repository ...
                try {
                    repository.start();
                    return repository;
                } catch (Exception e) {
                    // Something went wrong, so undeploy the repository ...
                    throw e;
                }
            }
        });
    }

    /**
     * Asynchronously shutdown the deployed {@link Repository} instance with the given the name, and return a future that will
     * return whether the Repository instance is shutdown. If the Repository is not running, the resulting future will return
     * immediately.
     * <p>
     * Note that the caller does not have to wait for the shutdown to completed. However, to do so the caller merely calls
     * {@link Future#get() get()} or {@link Future#get(long, TimeUnit) get(long,TimeUnit)} on the future to return a boolean flag
     * specifying whether the Repository instance is shutdown (not running). Note that any exceptions thrown during the shutdown
     * will be wrapped in an {@link ExecutionException} thrown by the Future's <code>get</code> methods.
     * </p>
     * 
     * @param repositoryName the name of the deployed repository
     * @return a future wrapping the asynchronous shutdown process; never null, and {@link Future#get()} will return whether the
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws NoSuchRepositoryException if there is no repository with the specified name
     * @throws IllegalStateException if this engine is not {@link #getState() running}
     * @see #deploy(RepositoryConfiguration)
     * @see #undeploy(String)
     */
    public final Future<Boolean> shutdownRepository( String repositoryName ) throws NoSuchRepositoryException {
        return getRepository(repositoryName).shutdown();
    }

    /**
     * Get an instantaneous snapshot of the JCR repositories and their state. Note that the results are accurate only when this
     * methods returns.
     * 
     * @return the immutable map of repository states keyed by repository names; never null
     */
    public Map<String, State> getRepositories() {
        checkRunning();
        Map<String, State> results = new HashMap<String, State>();
        final Lock lock = this.lock.readLock();
        try {
            lock.lock();
            for (JcrRepository repository : repositories.values()) {
                results.put(repository.getName(), repository.getState());
            }
        } finally {
            lock.unlock();
        }
        return Collections.unmodifiableMap(results);
    }

    /**
     * Returns a copy of the repositories. Note that when returned, not all repositories may be active.
     * 
     * @return a copy of the repositories; never null
     */
    protected Collection<JcrRepository> repositories() {
        if (this.state == State.RUNNING) {
            final Lock lock = this.lock.readLock();
            try {
                lock.lock();
                return new ArrayList<JcrRepository>(repositories.values());
            } finally {
                lock.unlock();
            }
        }
        return Collections.emptyList();
    }

    /**
     * Deploy a new repository with the given configuration. This method will fail if this engine already contains a repository
     * with the specified name.
     * 
     * @param repositoryConfiguration the configuration for the repository
     * @return the deployed repository instance, which must be {@link #startRepository(String) started} before it can be used;
     *         never null
     * @throws ConfigurationException if the configuration is not valid
     * @throws RepositoryException if there is already a deployed repository with the specified name, or if there is a problem
     *         deploying the repository
     * @throws IllegalArgumentException if the configuration is null
     * @see #deploy(RepositoryConfiguration)
     * @see #update(String, Changes)
     * @see #undeploy(String)
     */
    public JcrRepository deploy( final RepositoryConfiguration repositoryConfiguration )
        throws ConfigurationException, RepositoryException {
        return deploy(repositoryConfiguration, null);
    }

    /**
     * Deploy a new repository with the given configuration. This method will fail if this engine already contains a repository
     * with the specified name.
     * 
     * @param repositoryConfiguration the configuration for the repository
     * @param repositoryKey the key by which this repository is known to this engine; may be null if the
     *        {@link RepositoryConfiguration#getName() repository's name} should be used
     * @return the deployed repository instance, which must be {@link #startRepository(String) started} before it can be used;
     *         never null
     * @throws ConfigurationException if the configuration is not valid
     * @throws RepositoryException if there is already a deployed repository with the specified name, or if there is a problem
     *         deploying the repository
     * @throws IllegalArgumentException if the configuration is null
     * @see #deploy(RepositoryConfiguration)
     * @see #update(String, Changes)
     * @see #undeploy(String)
     */
    protected JcrRepository deploy( final RepositoryConfiguration repositoryConfiguration,
                                    final String repositoryKey ) throws ConfigurationException, RepositoryException {
        CheckArg.isNotNull(repositoryConfiguration, "repositoryConfiguration");
        checkRunning();

        final String repoName = repositoryKey != null ? repositoryKey : repositoryConfiguration.getName();
        Problems problems = repositoryConfiguration.validate();
        if (problems.hasErrors()) {
            throw new ConfigurationException(problems, JcrI18n.repositoryConfigurationIsNotValid.text(repoName,
                                                                                                      problems.toString()));
        }

        // Now try to deploy the repository ...
        JcrRepository repository = null;
        final Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            if (this.repositories.containsKey(repoName)) {
                throw new RepositoryException(JcrI18n.repositoryIsAlreadyDeployed.text(repoName));
            }

            // Instantiate (but do not start!) the repository, store it in our map, and return it ...
            repository = new JcrRepository(repositoryConfiguration);
            this.repositories.put(repoName, repository);
        } finally {
            lock.unlock();
        }
        return repository;
    }

    /**
     * Update the configuration of a deployed repository by applying the set of changes to that repository's configuration. The
     * changes can be built by obtaining the configuration for the deployed instance, obtaining an editor (which actually contains
     * a copy of the configuration) and using it to make changes, and then getting the changes from the editor. The benefit of
     * this approach is that the changes can be made in an isolated copy of the configuration and all of the changes applied en
     * masse.
     * <p>
     * The basic outline for modifying a repository's configuration is as follows:
     * <ol>
     * <li>Get the current configuration for the repository that is to be changed</li>
     * <li>Get an editor from that configuration</li>
     * <li>Use the editor to capture and validate the changes you want to make</li>
     * <li>Update the repository's configuration with the changes from the editor</li>
     * </ol>
     * Here's some code that shows how this is done:
     * 
     * <pre>
     *   ModeShapeEngine engine = ...
     *   Repository deployed = engine.{@link ModeShapeEngine#getRepository(String) getRepository("repo")};
     *   RepositoryConfiguration deployedConfig = deployed.{@link JcrRepository#getConfiguration() getConfiguration()};
     *   
     *   // Create an editor, which is actually manipulating a copy of the configuration document ...
     *   Editor editor = deployedConfig.{@link RepositoryConfiguration#edit() edit()};
     *   
     *   // Modify the copy of the configuration (we'll do something trivial here) ...
     *   editor.setNumber(FieldName.LARGE_VALUE_SIZE_IN_BYTES,8096);
     *   
     *   // Get our changes and validate them ...
     *   Changes changes = editor.{@link Editor#getChanges() getChanges()};
     *   Results validationResults = deployedConfig.{@link RepositoryConfiguration#validate(Changes) validate(changes)};
     *   if ( validationResults.hasErrors() ) {
     *       // you've done something wrong with your editor
     *   } else {
     *       // Update the deployed repository's configuration with these changes ...
     *       Future&lt;Boolean> future = engine.{@link ModeShapeEngine#update(String, Changes) update("repo",changes)};
     *           
     *       // Optionally block while the repository instance is changed to 
     *       // reflect the new configuration ...
     *       JcrRepository updated = future.get();
     *   }
     * </pre>
     * 
     * </p>
     * <p>
     * Note that this method blocks while the changes to the configuration are validated and applied, but before the repository
     * has changed to reflect the new configuration, which is done asynchronously. The resulting future represents that
     * asynchronous process, and the future can be used to block until that updating is completed.
     * </p>
     * 
     * @param repositoryName the name of the repository
     * @param changes the changes that should be applied to the repository's configuration
     * @return a future that allows the caller to block until the repository has completed all of its changes.
     * @throws ConfigurationException if the configuration is not valid with the supplied changes
     * @throws NoSuchRepositoryException if there is no repository with the specified name
     * @throws RepositoryException if there is a problem updating the repository
     * @throws IllegalArgumentException if any of the parameters are null or invalid
     * @see #deploy(RepositoryConfiguration)
     * @see #undeploy(String)
     */
    public Future<JcrRepository> update( final String repositoryName,
                                         final Changes changes )
        throws ConfigurationException, NoSuchRepositoryException, RepositoryException {

        final Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            // Get the repository ...
            final JcrRepository repository = this.repositories.get(repositoryName);
            if (repository == null) {
                // There is no repository with this name ...
                throw new NoSuchRepositoryException(JcrI18n.repositoryDoesNotExist.text(repositoryName));
            }

            // Determine if the changes would result in a valid repository configuration ...
            RepositoryConfiguration config = repository.getConfiguration();
            Problems problems = config.validate(changes);
            if (problems.hasErrors()) {
                throw new ConfigurationException(problems, JcrI18n.repositoryConfigurationIsNotValid.text(repositoryName,
                                                                                                          problems.toString()));
            }

            // Create an initializer that will start the repository ...
            Future<JcrRepository> future = repositoryStarterService.submit(new Callable<JcrRepository>() {
                @Override
                public JcrRepository call() throws Exception {
                    // Apply the changes to the repository ...
                    repository.apply(changes);
                    return repository;
                }
            });

            // And return the future
            return future;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stop and undeploy the named {@link Repository}.
     * 
     * @param repositoryName the name of the deployed repository
     * @return a future that allows the caller to block until the repository is shutdown; if the repository could not shutdown,
     *         {@link Future#get() getting} the repository from the future will throw the exception wrapped in a
     *         {@link ExecutionException}
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws NoSuchRepositoryException if there is no repository with the specified name
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public Future<Boolean> undeploy( final String repositoryName ) throws NoSuchRepositoryException {
        CheckArg.isNotEmpty(repositoryName, "repositoryName");
        checkRunning();

        // Now try to undeploy the repository ...
        final Lock lock = this.lock.writeLock();
        try {
            lock.lock();
            final JcrRepository repository = this.repositories.remove(repositoryName);
            if (repository == null) {
                // There is no repository with this name ...
                throw new NoSuchRepositoryException(JcrI18n.repositoryDoesNotExist.text(repositoryName));
            }
            // There is an existing repository, so start to shut it down (note that it may fail) ...
            return repository.shutdown();
        } finally {
            lock.unlock();
        }
    }
}
