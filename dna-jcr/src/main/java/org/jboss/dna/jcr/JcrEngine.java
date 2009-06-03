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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.GraphNamespaceRegistry;
import org.jboss.dna.jcr.JcrRepository.Option;
import org.jboss.dna.repository.DnaConfiguration;
import org.jboss.dna.repository.DnaEngine;

/**
 * The basic component that encapsulates the JBoss DNA services, including the {@link Repository} instances.
 */
public class JcrEngine extends DnaEngine {

    private final Map<String, JcrRepository> repositories;
    private final Lock repositoriesLock;

    JcrEngine( ExecutionContext context,
               DnaConfiguration.ConfigurationDefinition configuration ) {
        super(context, configuration);
        this.repositories = new HashMap<String, JcrRepository>();
        this.repositoriesLock = new ReentrantLock();
    }

    /**
     * Get the {@link Repository} implementation for the named repository.
     * 
     * @param repositoryName the name of the repository, which corresponds to the name of a configured {@link RepositorySource}
     * @return the named repository instance
     * @throws IllegalArgumentException if the repository name is null, blank or invalid
     * @throws RepositoryException if there is no repository with the specified name
     * @throws IllegalStateException if this engine was not {@link #start() started}
     */
    public final JcrRepository getRepository( String repositoryName ) throws RepositoryException {
        CheckArg.isNotEmpty(repositoryName, "repositoryName");
        checkRunning();
        try {
            repositoriesLock.lock();
            JcrRepository repository = repositories.get(repositoryName);
            if (repository == null) {
                try {
                    repository = doCreateJcrRepository(repositoryName);
                } catch (PathNotFoundException e) {
                    // The repository name is not a valid repository ...
                    String msg = JcrI18n.repositoryDoesNotExist.text(repositoryName);
                    throw new RepositoryException(msg);
                }
                repositories.put(repositoryName, repository);
            }
            return repository;
        } finally {
            repositoriesLock.unlock();
        }
    }

    protected JcrRepository doCreateJcrRepository( String repositoryName ) throws RepositoryException, PathNotFoundException {
        RepositoryConnectionFactory connectionFactory = getRepositoryConnectionFactory();
        Map<String, String> descriptors = null;
        Map<Option, String> options = new HashMap<Option, String>();

        // Read the subgraph that represents the repository ...
        PathFactory pathFactory = getExecutionContext().getValueFactories().getPathFactory();
        Path repositoriesPath = pathFactory.create(configuration.getPath(), DnaLexicon.REPOSITORIES);
        Path repositoryPath = pathFactory.create(repositoriesPath, repositoryName);
        Graph configuration = getConfigurationGraph();
        Subgraph subgraph = configuration.getSubgraphOfDepth(3).at(repositoryPath);

        // Read the options ...
        Node optionsNode = subgraph.getNode(DnaLexicon.OPTIONS);
        if (optionsNode != null) {
            for (Location optionLocation : optionsNode.getChildren()) {
                Node optionNode = configuration.getNodeAt(optionLocation);
                Path.Segment segment = optionLocation.getPath().getLastSegment();
                Property valueProperty = optionNode.getProperty(DnaLexicon.VALUE);
                if (valueProperty == null) continue;
                Option option = Option.findOption(segment.getName().getLocalName());
                if (option == null) continue;
                options.put(option, valueProperty.getFirstValue().toString());
            }
        }

        // Read the namespaces ...
        ExecutionContext context = getExecutionContext();
        Node namespacesNode = subgraph.getNode(DnaLexicon.NAMESPACES);
        if (namespacesNode != null) {
            GraphNamespaceRegistry registry = new GraphNamespaceRegistry(configuration, namespacesNode.getLocation().getPath(),
                                                                         DnaLexicon.NAMESPACE_URI);
            context = context.with(registry);
        }

        // Get the name of the source ...
        Property property = subgraph.getRoot().getProperty(DnaLexicon.SOURCE_NAME);
        if (property == null || property.isEmpty()) {
            String readableName = readable(DnaLexicon.SOURCE_NAME);
            String readablePath = readable(subgraph.getLocation());
            String msg = JcrI18n.propertyNotFoundOnNode.text(readableName, readablePath, configuration.getCurrentWorkspaceName());
            throw new RepositoryException(msg);
        }
        String sourceName = context.getValueFactories().getStringFactory().create(property.getFirstValue());

        // Create the repository ...
        JcrRepository repository = new JcrRepository(context, connectionFactory, sourceName, descriptors, options);

        // Register all the the node types ...
        Node nodeTypesNode = subgraph.getNode(DnaLexicon.NODE_TYPES);
        if (nodeTypesNode != null) {
            try {
                repository.getRepositoryTypeManager().registerNodeTypes(subgraph, nodeTypesNode.getLocation());
            } catch (RepositoryException e) {
                // Error registering the node types ...
                getProblems().addError(e, JcrI18n.errorRegisteringNodeTypes, repositoryName);
            }
        }

        return repository;
    }

    protected final String readable( Name name ) {
        return name.getString(context.getNamespaceRegistry());
    }

    protected final String readable( Path path ) {
        return path.getString(context.getNamespaceRegistry());
    }

    protected final String readable( Location location ) {
        return location.getString(context.getNamespaceRegistry());
    }
}
