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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrMixLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathExpression;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.repository.mimetype.MimeTypeDetectorConfig;
import org.jboss.dna.repository.observation.ObservationService;
import org.jboss.dna.repository.sequencer.SequencerConfig;
import org.jboss.dna.repository.sequencer.SequencingService;
import org.jboss.dna.repository.service.AdministeredService;
import org.jboss.dna.repository.util.JcrExecutionContext;
import org.jboss.dna.repository.util.SessionFactory;
import org.jboss.dna.repository.util.SimpleSessionFactory;

/**
 * A single instance of the DNA services, which is obtained after setting up the {@link DnaConfiguration#build() configuration}.
 * 
 * @see DnaConfiguration
 */
@Immutable
public class DnaEngine {

    public static final String CONFIGURATION_REPOSITORY_NAME = "dna:configuration";

    private final DnaConfiguration.ConfigurationRepository configuration;
    private final ConfigurationScanner scanner;
    private final Problems problems;
    private final ExecutionContext context;
    private final List<AdministeredService> services;

    private final RepositoryService repositoryService;
    private final ObservationService observationService;
    private final SequencingService sequencingService;
    private final ExecutorService executorService;

    private final RepositoryConnectionFactory connectionFactory;

    DnaEngine( DnaConfiguration configuration ) {
        this.problems = new SimpleProblems();

        // Use the configuration's context ...
        this.context = configuration.context();

        // And set up the scanner ...
        this.configuration = configuration.configurationSource;
        this.scanner = new ConfigurationScanner(this.problems, this.context, this.configuration);

        // Add the configuration source to the repository library ...
        final RepositorySource configSource = this.configuration.getRepositorySource();
        RepositoryLibrary library = new RepositoryLibrary();
        library.addSource(configSource);

        // Create the RepositoryService, pointing it to the configuration repository ...
        Path pathToConfigurationRoot = this.configuration.getPath();
        repositoryService = new RepositoryService(library, configSource.getName(), "", pathToConfigurationRoot, context);

        for (MimeTypeDetectorConfig config : scanner.getMimeTypeDetectors()) {
            library.getMimeTypeDetectors().addDetector(config);
        }

        // Create the sequencing service ...
        executorService = new ScheduledThreadPoolExecutor(10); // Use a magic number for now
        sequencingService = new SequencingService();
        SessionFactory sessionFactory = new SimpleSessionFactory();
        JcrExecutionContext jcrContext = new JcrExecutionContext(context, sessionFactory, "");
        sequencingService.setExecutionContext(jcrContext);
        sequencingService.setExecutorService(executorService);
        for (SequencerConfig sequencerConfig : scanner.getSequencingConfigurations()) {
            sequencingService.addSequencer(sequencerConfig);
        }

        // Create the observation service ...
        observationService = null; // new ObservationService(null);

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

    /**
     * Get the problems that were encountered when setting up this engine from the configuration.
     * 
     * @return the problems, which may be empty but will never be null
     */
    public Problems getProblems() {
        return problems;
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

    /**
     * The component responsible for reading the configuration repository and (eventually) for propagating changes in the
     * configuration repository into the services.
     */
    protected class ConfigurationScanner {
        private final Problems problems;
        private final ExecutionContext context;
        private final DnaConfiguration.ConfigurationRepository configurationRepository;

        protected ConfigurationScanner( Problems problems,
                                        ExecutionContext context,
                                        DnaConfiguration.ConfigurationRepository configurationRepository ) {
            this.problems = problems;
            this.context = context;
            this.configurationRepository = configurationRepository;
        }

        public List<MimeTypeDetectorConfig> getMimeTypeDetectors() {
            List<MimeTypeDetectorConfig> detectors = new ArrayList<MimeTypeDetectorConfig>();
            Graph graph = Graph.create(configurationRepository.getRepositorySource(), context);
            Path pathToSequencersNode = context.getValueFactories().getPathFactory().create(configurationRepository.getPath(),
                                                                                            DnaLexicon.MIME_TYPE_DETECTORS);
            try {
                Subgraph subgraph = graph.getSubgraphOfDepth(2).at(pathToSequencersNode);

                Set<Name> skipProperties = new HashSet<Name>();
                skipProperties.add(DnaLexicon.READABLE_NAME);
                skipProperties.add(DnaLexicon.DESCRIPTION);
                skipProperties.add(DnaLexicon.CLASSNAME);
                skipProperties.add(DnaLexicon.CLASSPATH);
                skipProperties.add(DnaLexicon.PATH_EXPRESSIONS);
                Set<String> skipNamespaces = new HashSet<String>();
                skipNamespaces.add(JcrLexicon.Namespace.URI);
                skipNamespaces.add(JcrNtLexicon.Namespace.URI);
                skipNamespaces.add(JcrMixLexicon.Namespace.URI);

                for (Location detectorLocation : subgraph.getRoot().getChildren()) {
                    Node node = subgraph.getNode(detectorLocation);
                    String name = stringValueOf(node, DnaLexicon.READABLE_NAME);
                    String desc = stringValueOf(node, DnaLexicon.DESCRIPTION);
                    String classname = stringValueOf(node, DnaLexicon.CLASSNAME);
                    String[] classpath = stringValuesOf(node, DnaLexicon.CLASSPATH);
                    Map<String, Object> properties = new HashMap<String, Object>();
                    for (Property property : node.getProperties()) {
                        Name propertyName = property.getName();
                        if (skipNamespaces.contains(propertyName.getNamespaceUri())) continue;
                        if (skipProperties.contains(propertyName)) continue;
                        if (property.isSingle()) {
                            properties.put(propertyName.getLocalName(), property.getFirstValue());
                        } else {
                            properties.put(propertyName.getLocalName(), property.getValuesAsArray());
                        }
                    }
                    MimeTypeDetectorConfig config = new MimeTypeDetectorConfig(name, desc, properties, classname, classpath);
                    detectors.add(config);
                }
            } catch (PathNotFoundException e) {
                // no detectors registered ...
            }
            return detectors;
        }

        public List<SequencerConfig> getSequencingConfigurations() {
            List<SequencerConfig> configs = new ArrayList<SequencerConfig>();
            Graph graph = Graph.create(configurationRepository.getRepositorySource(), context);
            Path pathToSequencersNode = context.getValueFactories().getPathFactory().create(configurationRepository.getPath(),
                                                                                            DnaLexicon.SEQUENCERS);
            try {
                Subgraph subgraph = graph.getSubgraphOfDepth(2).at(pathToSequencersNode);

                Set<Name> skipProperties = new HashSet<Name>();
                skipProperties.add(DnaLexicon.READABLE_NAME);
                skipProperties.add(DnaLexicon.DESCRIPTION);
                skipProperties.add(DnaLexicon.CLASSNAME);
                skipProperties.add(DnaLexicon.CLASSPATH);
                skipProperties.add(DnaLexicon.PATH_EXPRESSIONS);
                Set<String> skipNamespaces = new HashSet<String>();
                skipNamespaces.add(JcrLexicon.Namespace.URI);
                skipNamespaces.add(JcrNtLexicon.Namespace.URI);
                skipNamespaces.add(JcrMixLexicon.Namespace.URI);

                for (Location sequencerLocation : subgraph.getRoot().getChildren()) {
                    Node sequencerNode = subgraph.getNode(sequencerLocation);
                    String name = stringValueOf(sequencerNode, DnaLexicon.READABLE_NAME);
                    String desc = stringValueOf(sequencerNode, DnaLexicon.DESCRIPTION);
                    String classname = stringValueOf(sequencerNode, DnaLexicon.CLASSNAME);
                    String[] classpath = stringValuesOf(sequencerNode, DnaLexicon.CLASSPATH);
                    String[] expressionStrings = stringValuesOf(sequencerNode, DnaLexicon.PATH_EXPRESSIONS);
                    List<PathExpression> pathExpressions = new ArrayList<PathExpression>();
                    if (expressionStrings != null) {
                        for (String expressionString : expressionStrings) {
                            try {
                                pathExpressions.add(PathExpression.compile(expressionString));
                            } catch (Throwable t) {
                                problems.addError(t,
                                                  RepositoryI18n.pathExpressionIsInvalidOnSequencer,
                                                  expressionString,
                                                  name,
                                                  t.getLocalizedMessage());
                            }
                        }
                    }
                    String[] goodExpressionStrings = new String[pathExpressions.size()];
                    for (int i = 0; i != pathExpressions.size(); ++i) {
                        PathExpression expression = pathExpressions.get(i);
                        goodExpressionStrings[i] = expression.getExpression();
                    }
                    Map<String, Object> properties = new HashMap<String, Object>();
                    for (Property property : sequencerNode.getProperties()) {
                        Name propertyName = property.getName();
                        if (skipNamespaces.contains(propertyName.getNamespaceUri())) continue;
                        if (skipProperties.contains(propertyName)) continue;
                        if (property.isSingle()) {
                            properties.put(propertyName.getLocalName(), property.getFirstValue());
                        } else {
                            properties.put(propertyName.getLocalName(), property.getValuesAsArray());
                        }
                    }
                    SequencerConfig config = new SequencerConfig(name, desc, properties, classname, classpath,
                                                                 goodExpressionStrings);
                    configs.add(config);
                }
            } catch (PathNotFoundException e) {
                // no detectors registered ...
            }
            return configs;
        }

        private String stringValueOf( Node node,
                                      Name propertyName ) {
            Property property = node.getProperty(propertyName);
            if (property == null) return null;
            if (property.isEmpty()) return null;
            return context.getValueFactories().getStringFactory().create(property.getFirstValue());
        }

        private String[] stringValuesOf( Node node,
                                         Name propertyName ) {
            Property property = node.getProperty(propertyName);
            if (property == null) return null;
            return context.getValueFactories().getStringFactory().create(property.getValuesAsArray());
        }

    }
}
