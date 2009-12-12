/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.graph.search;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.InvalidPathException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.CompositeRequestChannel;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DeleteChildrenRequest;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * A utility class that crawls content and generates {@link Request} objects defining the operations to perform on an index.
 */
@Immutable
public class SearchEngineIndexer {

    private final SearchEngine engine;
    private final String sourceName;
    private final RepositoryConnectionFactory connectionFactory;

    /**
     * Create an indexer that will refresh the indexes in the supplied search engine by crawling content, using the supplied
     * connection factory to obtain connections
     * 
     * @param searchEngine the search engine that is to be updated
     * @param connectionFactory the factory for creating connections to the repository containing the content
     * @throws IllegalArgumentException if the search engine or connection factory references are null
     */
    public SearchEngineIndexer( SearchEngine searchEngine,
                                RepositoryConnectionFactory connectionFactory ) {
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isNotNull(searchEngine, "searchEngine");
        this.engine = searchEngine;
        this.sourceName = engine.getSourceName();
        this.connectionFactory = connectionFactory;
        assert this.sourceName != null;
    }

    /**
     * Get the search engine used by this crawler
     * 
     * @return the search engine; never null
     */
    public SearchEngine getSearchEngine() {
        return engine;
    }

    /**
     * Get the name of the {@link RepositorySource} containing the content that is to be placed inside the search engine.
     * 
     * @return sourceName the name of the repository source; never null
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Utility method to index all of the content at or below the supplied path in the named workspace within the
     * {@link #getSourceName() source}. If the starting point is the root node, then this method will drop the existing index(es)
     * and rebuild from the content in the workspace and source.
     * <p>
     * This method operates synchronously and returns when the requested indexing is completed.
     * </p>
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @param workspaceName the name of the workspace
     * @param startingPoint the location that represents the content to be indexed; must have a path
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the context, workspace name or location are null, or if the depth per read is not
     *         positive
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void index( ExecutionContext context,
                       String workspaceName,
                       Location startingPoint,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(startingPoint, "startingPoint");
        CheckArg.isPositive(depthPerRead, "depthPerRead");
        assert startingPoint.hasPath();
        // Create the request processor that we'll use to update the search engine ...
        RequestProcessor processor = engine.createProcessor(context, null, false);
        try {

            // Verify the workspace with the processor before doing anything else, to prime the processor with a search workspace
            VerifyWorkspaceRequest verify = new VerifyWorkspaceRequest(workspaceName);
            processor.process(verify);
            if (verify.hasError()) {
                Throwable error = verify.getError();
                if (error instanceof RuntimeException) throw (RuntimeException)error;
                throw new InvalidWorkspaceException(error);
            }

            // Create and start the channel to the source ...
            CompositeRequestChannel channel = new CompositeRequestChannel(getSourceName());
            ExecutorService service = Executors.newSingleThreadExecutor();
            channel.start(service, context, connectionFactory);
            try {
                List<Request> requests = new LinkedList<Request>();

                // Read the first subgraph ...
                try {
                    ReadBranchRequest readSubgraph = new ReadBranchRequest(startingPoint, workspaceName, depthPerRead);
                    channel.addAndAwait(readSubgraph);
                    if (readSubgraph.hasError()) {
                        channel.cancel(false);
                        Throwable t = readSubgraph.getError();
                        if (t instanceof RuntimeException) throw (RuntimeException)t;
                        throw new RepositorySourceException(getSourceName(), t);
                    }
                    Iterator<Location> locationIter = readSubgraph.iterator();
                    assert locationIter.hasNext();

                    // Destroy the nodes at the supplied location ...
                    if (startingPoint.getPath().isRoot()) {
                        // Just delete the whole content ...
                        processor.process(new DeleteBranchRequest(startingPoint, workspaceName));
                    } else {
                        // We can't delete the node, since later same-name-siblings might be changed. So delete the children ...
                        processor.process(new DeleteChildrenRequest(startingPoint, workspaceName));
                    }

                    // Now update all of the properties, removing any that are no longer needed ...
                    Location topNode = locationIter.next();
                    Map<Name, Property> properties = readSubgraph.getPropertiesFor(topNode);
                    boolean removeOtherProperties = true;
                    UpdatePropertiesRequest request = new UpdatePropertiesRequest(startingPoint, workspaceName, properties,
                                                                                  removeOtherProperties);
                    request.setActualLocationOfNode(topNode);
                    processor.process(request);
                    if (request.isCancelled() || request.hasError()) return;

                    // Create a queue that we'll use to walk the content ...
                    LinkedList<Location> locationsToRead = new LinkedList<Location>();

                    // Now walk the remaining nodes in the subgraph ...
                    while (true) {
                        while (locationIter.hasNext()) {

                            // Index the node ...
                            Location location = locationIter.next();
                            Path path = location.getPath();
                            Location parent = readSubgraph.getLocationFor(path.getParent());
                            Name childName = path.getLastSegment().getName();
                            Collection<Property> nodePoperties = readSubgraph.getPropertiesFor(location).values();
                            CreateNodeRequest create = new CreateNodeRequest(parent, workspaceName, childName, nodePoperties);
                            create.setActualLocationOfNode(location); // set this so we don't have to figure it out
                            processor.process(create);
                            if (create.isCancelled() || create.hasError()) return;

                            // Process the children ...
                            for (Location child : readSubgraph.getChildren(location)) {
                                if (!readSubgraph.includes(child)) {
                                    // Record this location as needing to be read ...
                                    locationsToRead.add(child);
                                }
                            }
                        }

                        if (locationsToRead.isEmpty()) break;
                        Location location = locationsToRead.poll();
                        assert location != null;

                        readSubgraph = new ReadBranchRequest(location, workspaceName, depthPerRead);
                        channel.addAndAwait(readSubgraph);
                        if (readSubgraph.hasError()) {
                            if (readSubgraph.hasError()) {
                                channel.cancel(false);
                                Throwable t = readSubgraph.getError();
                                if (t instanceof RuntimeException) throw (RuntimeException)t;
                                throw new RepositorySourceException(getSourceName(), t);
                            }
                        }
                    }

                } catch (InvalidPathException e) {
                    // The node must no longer exist, so delete it from the indexes ...
                    requests.add(new DeleteBranchRequest(startingPoint, workspaceName));
                }
            } catch (InterruptedException err) {
                // Clear the interrupted status of the thread ...
                Thread.interrupted();
            } finally {
                // Close the channel ...
                try {
                    channel.close();
                } finally {
                    // And shut down the service ...
                    service.shutdown();
                    try {
                        service.awaitTermination(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        // Clear the interrupted status of the thread ...
                        Thread.interrupted();
                    }
                }
            }
        } finally {
            processor.close();
        }
    }

    /**
     * Index all of the content at or below the supplied path in the named workspace within the {@link #getSourceName() source}.
     * If the starting point is the root node, then this method will drop the existing index(es) and rebuild from the content in
     * the workspace and source.
     * <p>
     * This method operates synchronously and returns when the requested indexing is completed.
     * </p>
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @param workspaceName the name of the workspace
     * @param startingPoint the path that represents the content to be indexed
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the context, workspace name or path are null, or if the depth per read is not positive
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void index( ExecutionContext context,
                       String workspaceName,
                       Path startingPoint,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        index(context, workspaceName, Location.create(startingPoint), depthPerRead);
    }

    /**
     * Index all of the content in the named workspace within the {@link #getSourceName() source}. This method operates
     * synchronously and returns when the requested indexing is completed.
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @param workspaceName the name of the workspace
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the context or workspace name is null, or if the depth per read is not positive
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void index( ExecutionContext context,
                       String workspaceName,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        Path rootPath = context.getValueFactories().getPathFactory().createRootPath();
        index(context, workspaceName, Location.create(rootPath), depthPerRead);
    }

    /**
     * Index (or re-index) all of the content in all of the workspaces within the source. This method operates synchronously and
     * returns when the requested indexing is completed.
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @param depthPerRead the depth of each subgraph read operation
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws IllegalArgumentException if the context is null, or if depth per read is not positive
     */
    public void index( ExecutionContext context,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(context, "context");
        CheckArg.isPositive(depthPerRead, "depthPerRead");
        Path rootPath = context.getValueFactories().getPathFactory().createRootPath();
        Location rootLocation = Location.create(rootPath);
        for (String workspaceName : Graph.create(sourceName, connectionFactory, context).getWorkspaces()) {
            index(context, workspaceName, rootLocation, depthPerRead);
        }
    }
}
