/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.jcr.JcrRepository.Options;
import org.jboss.dna.repository.DnaEngine;
import org.jboss.dna.repository.RepositoryService;
import org.jboss.dna.repository.observation.ObservationService;
import org.jboss.dna.repository.sequencer.SequencingService;

/**
 * The basic component that encapsulates the JBoss DNA services, including the {@link Repository} instances.
 */
public class JcrEngine {

    private final DnaEngine dnaEngine;
    private final Map<String, JcrRepository> repositories;
    private final Lock repositoriesLock;

    JcrEngine( DnaEngine dnaEngine ) {
        this.dnaEngine = dnaEngine;
        this.repositories = new HashMap<String, JcrRepository>();
        this.repositoriesLock = new ReentrantLock();
    }

    /**
     * Get the problems that were encountered when setting up this engine from the configuration.
     * 
     * @return the problems, which may be empty but will never be null
     */
    public Problems getProblems() {
        return dnaEngine.getProblems();
    }

    /**
     * Get the execution context for this engine. This context can be used to create additional (perhaps narrowed) contexts.
     * 
     * @return the engine's execution context; never null
     */
    public final ExecutionContext getExecutionContext() {
        return dnaEngine.getExecutionContext();
    }

    /**
     * Get the RepositorySource with the supplied name.
     * 
     * @param repositoryName the name of the repository (or repository source)
     * @return the named repository source, or null if there is no such repository
     */
    protected final RepositorySource getRepositorySource( String repositoryName ) {
        return dnaEngine.getRepositorySource(repositoryName);
    }

    protected final RepositoryConnectionFactory getRepositoryConnectionFactory() {
        return dnaEngine.getRepositoryConnectionFactory();
    }

    protected final RepositoryService getRepositoryService() {
        return dnaEngine.getRepositoryService();
    }

    protected final ObservationService getObservationService() {
        return dnaEngine.getObservationService();
    }

    protected final SequencingService getSequencingService() {
        return dnaEngine.getSequencingService();
    }

    /**
     * Get the {@link Repository} implementation for the named repository.
     * 
     * @param repositoryName the name of the repository, which corresponds to the name of a configured {@link RepositorySource}
     * @return the named repository instance
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws RepositoryException if there is no repository with the specified name
     */
    public final Repository getRepository( String repositoryName ) throws RepositoryException {
        CheckArg.isNotEmpty(repositoryName, "repositoryName");
        try {
            repositoriesLock.lock();
            JcrRepository repository = repositories.get(repositoryName);
            if (repository == null) {
                if (getRepositorySource(repositoryName) == null) {
                    // The repository name is not a valid repository ...
                    String msg = JcrI18n.repositoryDoesNotExist.text(repositoryName);
                    throw new RepositoryException(msg);
                }
                repository = doCreateJcrRepository(repositoryName);
                repositories.put(repositoryName, repository);
            }
            return repository;
        } finally {
            repositoriesLock.unlock();
        }
    }

    protected JcrRepository doCreateJcrRepository( String repositoryName ) {
        RepositoryConnectionFactory connectionFactory = getRepositoryConnectionFactory();
        Map<String, String> descriptors = null;
        Map<Options, String> options = Collections.singletonMap(Options.PROJECT_NODE_TYPES, "false");
        return new JcrRepository(getExecutionContext(), connectionFactory, repositoryName, descriptors, options);
    }

    /*
     * Lifecycle methods
     */

    public void start() {
        dnaEngine.start();
    }

    public void shutdown() {
        dnaEngine.shutdown();
    }
}
