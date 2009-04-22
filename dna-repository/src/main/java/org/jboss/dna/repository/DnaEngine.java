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
package org.jboss.dna.repository;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.repository.DnaConfiguration.DnaMimeTypeDetectorDetails;
import org.jboss.dna.repository.DnaConfiguration.DnaRepositoryDetails;
import org.jboss.dna.repository.DnaConfiguration.DnaSequencerDetails;
import org.jboss.dna.repository.observation.ObservationService;
import org.jboss.dna.repository.sequencer.SequencingService;
import org.jboss.dna.repository.service.AdministeredService;

/**
 * @author Randall Hauch
 */
@Immutable
public class DnaEngine {

    public static final String CONFIGURATION_REPOSITORY_NAME = "dna:configuration";

    private final ExecutionContext context;
    private final List<AdministeredService> services;

    private final RepositoryService repositoryService;
    private final ObservationService observationService;
    private final SequencingService sequencingService;
    private final ExecutorService executorService;

    private final RepositoryConnectionFactory connectionFactory;

    DnaEngine( DnaConfiguration configuration ) {
        this.context = new ExecutionContext();

        DnaRepositoryDetails configSource = configuration.repositories.get(CONFIGURATION_REPOSITORY_NAME);
        assert configSource != null : "Must specify a repository source named " + CONFIGURATION_REPOSITORY_NAME;

        // Use a magic number for now
        executorService = new ScheduledThreadPoolExecutor(10);

        observationService = null; // new ObservationService(null);

        RepositoryLibrary library = new RepositoryLibrary();
        for (DnaRepositoryDetails details : configuration.repositories.values()) {
            // Adding configuration source to the library until proven wrong!
            library.addSource(details.getRepositorySource());
        }

        for (DnaMimeTypeDetectorDetails details : configuration.mimeTypeDetectors.values()) {
            library.getMimeTypeDetectors().addDetector(details.getMimeTypeDetectorConfig());
        }
        repositoryService = new RepositoryService(library, configSource.getRepositorySource().getName(), "", context);

        sequencingService = new SequencingService();
        sequencingService.setExecutorService(executorService);
        for (DnaSequencerDetails details : configuration.sequencers.values()) {
            sequencingService.addSequencer(details.getSequencerConfig());
        }

        this.services = Arrays.asList(new AdministeredService[] { /* observationService, */repositoryService, sequencingService,});

        connectionFactory = new RepositoryConnectionFactory() {
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                RepositorySource source = DnaEngine.this.getRepositorySource(sourceName);
                if (sourceName == null) {
                    throw new RepositorySourceException(sourceName);
                }

                return source.getConnection();
            }
        };
    }

    /*
     * Lookup methods
     */
    public final ExecutionContext getExecutionContext() {
        return context;
    }

    public final RepositorySource getRepositorySource( String repositoryName ) {
        return repositoryService.getRepositorySourceManager().getSource(repositoryName);
    }

    public final RepositoryConnectionFactory getRepositoryConnectionFactory() {
        return connectionFactory;
    }

    public final RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public final ObservationService getObservationService() {
        return observationService;
    }

    public final SequencingService getSequencingService() {
        return sequencingService;
    }

    /*
     * Lifecycle methods
     */

    public void start() {
        for (AdministeredService service : services) {
            service.getAdministrator().start();
        }
    }

    public void shutdown() {
        for (AdministeredService service : services) {
            service.getAdministrator().shutdown();
        }

        try {
            executorService.awaitTermination(10 * 60, TimeUnit.SECONDS); // No TimeUnit.MINUTES in JDK 5
        } catch (InterruptedException ie) {
            // Reset the thread's status and continue this method ...
            Thread.interrupted();
        }
        executorService.shutdown();
    }
}
