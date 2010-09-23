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
package org.modeshape.graph.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.NamedThreadFactory;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.InvalidPathException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.CompositeRequestChannel;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.DeleteChildrenRequest;
import org.modeshape.graph.request.GetWorkspacesRequest;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.UpdatePropertiesRequest;

/**
 * A utility class that can be used to update the indexes of a search engine by crawling parts of the source. Each instance is
 * created to make a series of atomic updates to the search engine using a single connection to the source.
 */
@NotThreadSafe
public class SearchEngineIndexer {

    private final ExecutionContext context;
    private final RepositoryConnectionFactory connectionFactory;
    private final String sourceName;
    private final SearchEngine searchEngine;
    private final int maxDepthPerRead;
    private final ExecutorService service;
    private final CompositeRequestChannel channel;
    private final SearchEngineProcessor processor;
    private boolean closed = false;

    /**
     * Create an indexer that will update the indexes in the supplied search engine by crawling content, using the supplied
     * connection factory to obtain connections.
     * <p>
     * As soon as this indexer is created, it establishes a connection to the underlying source and is ready to being retrieving
     * content from the source and using it to update the indexes. Therefore, <i>the instance <strong>must</strong> be
     * {@link #close() closed} when completed.</i>
     * </p>
     * 
     * @param context the context in which the indexing operations are to be performed
     * @param searchEngine the search engine that is to be updated
     * @param connectionFactory the factory for creating connections to the repository containing the content
     * @param maxDepthPerRead the maximum depth for issuing each read requests when indexing
     * @throws IllegalArgumentException if the search engine or connection factory references are null, or if the maximum depth
     *         per read is not positive
     */
    public SearchEngineIndexer( ExecutionContext context,
                                SearchEngine searchEngine,
                                RepositoryConnectionFactory connectionFactory,
                                int maxDepthPerRead ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(searchEngine, "searchEngine");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isPositive(maxDepthPerRead, "maxDepthPerRead");
        this.context = context;
        this.searchEngine = searchEngine;
        this.sourceName = searchEngine.getSourceName();
        this.connectionFactory = connectionFactory;
        this.channel = new CompositeRequestChannel(this.sourceName);
        this.service = Executors.newSingleThreadExecutor(new NamedThreadFactory("search-" + sourceName));
        // Start the channel and search engine processor right away (this is why this object must be closed)
        this.channel.start(service, this.context, this.connectionFactory);
        this.processor = this.searchEngine.createProcessor(this.context, null, false);
        this.maxDepthPerRead = maxDepthPerRead;
    }

    /**
     * Get the name of the source containing the content.
     * 
     * @return the source name; never null
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Index all of the content in the named workspace within the {@link #getSourceName() source}. This method operates
     * synchronously and returns when the requested indexing is completed.
     * 
     * @param workspaceName the name of the workspace
     * @return this object for convenience in method chaining; never null
     * @throws IllegalArgumentException if the context or workspace name is null, or if the depth per read is not positive
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public SearchEngineIndexer index( String workspaceName ) throws RepositorySourceException, SearchEngineException {
        Path rootPath = context.getValueFactories().getPathFactory().createRootPath();
        index(workspaceName, Location.create(rootPath));
        return this;
    }

    /**
     * Index (or re-index) all of the content in all of the workspaces within the source. This method operates synchronously and
     * returns when the requested indexing is completed.
     * 
     * @return this object for convenience in method chaining; never null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws IllegalArgumentException if the context is null, or if depth per read is not positive
     */
    public SearchEngineIndexer indexAllWorkspaces() throws RepositorySourceException, SearchEngineException {
        // Get the names of all the workspaces ...
        GetWorkspacesRequest getWorkspaces = new GetWorkspacesRequest();
        try {
            channel.addAndAwait(getWorkspaces);
            checkRequestForErrors(getWorkspaces);
        } catch (InterruptedException e) {
            // Clear the interrupted status of the thread and continue ...
            Thread.interrupted();
            return this;
        }
        // Index all of the workspaces ...
        Path rootPath = context.getValueFactories().getPathFactory().createRootPath();
        Location rootLocation = Location.create(rootPath);
        for (String workspaceName : getWorkspaces.getAvailableWorkspaceNames()) {
            index(workspaceName, rootLocation);
        }
        return this;
    }

    /**
     * Crawl and index the full subgraph content starting at the supplied path in the named workspace.
     * 
     * @param workspaceName the name of the workspace
     * @param path the path of the content to be indexed
     * @return this object for convenience in method chaining; never null
     * @throws IllegalArgumentException if the workspace name or location are null, or if the depth is less than 1
     * @throws IllegalStateException if this object has already been {@link #close() closed}
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public SearchEngineIndexer index( String workspaceName,
                                      Path path ) {
        checkNotClosed();
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(path, "path");
        indexSubgraph(workspaceName, Location.create(path), Integer.MAX_VALUE);
        return this;
    }

    /**
     * Crawl and index the content starting at the supplied path in the named workspace, to the designated depth.
     * 
     * @param workspaceName the name of the workspace
     * @param path the path of the content to be indexed
     * @param depth the depth of the content to be indexed
     * @return this object for convenience in method chaining; never null
     * @throws IllegalArgumentException if the workspace name or location are null, or if the depth is less than 1
     * @throws IllegalStateException if this object has already been {@link #close() closed}
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public SearchEngineIndexer index( String workspaceName,
                                      Path path,
                                      int depth ) {
        checkNotClosed();
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(path, "path");
        CheckArg.isPositive(depth, "depth");
        if (depth == 1) {
            indexProperties(workspaceName, Location.create(path));
        } else {
            indexSubgraph(workspaceName, Location.create(path), depth);
        }
        return this;
    }

    /**
     * Crawl and index the full subgraph content starting at the supplied location in the named workspace.
     * 
     * @param workspaceName the name of the workspace
     * @param location the location of the content to be indexed
     * @return this object for convenience in method chaining; never null
     * @throws IllegalArgumentException if the workspace name or location are null, or if the depth is less than 1
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public SearchEngineIndexer index( String workspaceName,
                                      Location location ) {
        checkNotClosed();
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(location, "location");
        indexSubgraph(workspaceName, location, Integer.MAX_VALUE);
        return this;
    }

    /**
     * Crawl and index the content starting at the supplied location in the named workspace, to the designated depth.
     * 
     * @param workspaceName the name of the workspace
     * @param location the location of the content to be indexed
     * @param depth the depth of the content to be indexed
     * @return this object for convenience in method chaining; never null
     * @throws IllegalArgumentException if the workspace name or location are null, or if the depth is less than 1
     * @throws IllegalStateException if this object has already been {@link #close() closed}
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public SearchEngineIndexer index( String workspaceName,
                                      Location location,
                                      int depth ) {
        checkNotClosed();
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(location, "location");
        CheckArg.isPositive(depth, "depth");
        if (depth == 1) {
            indexProperties(workspaceName, location);
        } else {
            indexSubgraph(workspaceName, location, depth);
        }
        return this;
    }

    protected void indexSubgraph( String workspaceName,
                                  Location startingLocation,
                                  int depth ) {
        int depthPerRead = Math.min(maxDepthPerRead, depth);
        // Read the first subgraph ...
        ReadBranchRequest readSubgraph = new ReadBranchRequest(startingLocation, workspaceName, depthPerRead);
        try {
            channel.addAndAwait(readSubgraph);
            checkRequestForErrors(readSubgraph);
        } catch (InterruptedException e) {
            // Clear the interrupted status of the thread and continue ...
            Thread.interrupted();
            return;
        } catch (InvalidPathException e) {
            // The node must no longer exist, so delete it from the indexes ...
            process(new DeleteBranchRequest(startingLocation, workspaceName));
            return;
        }
        Iterator<Location> locationIter = readSubgraph.iterator();
        assert locationIter.hasNext();

        // Destroy the nodes at the supplied location ...
        if (startingLocation.getPath().isRoot()) {
            // Just delete the whole content ...
            process(new DeleteBranchRequest(startingLocation, workspaceName));
        } else {
            // We can't delete the node, since later same-name-siblings might be changed. So delete the children ...
            process(new DeleteChildrenRequest(startingLocation, workspaceName));
        }

        // Now update all of the properties, removing any that are no longer needed ...
        Location topNode = locationIter.next();
        assert topNode.equals(startingLocation);
        Map<Name, Property> properties = readSubgraph.getPropertiesFor(topNode);
        if (properties == null) return;
        if (startingLocation.getPath().isRoot()) {
            // The properties of the root node generally don't include the primary type, but we need to add it here ...
            Property rootPrimaryType = context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.ROOT);
            properties.put(JcrLexicon.PRIMARY_TYPE, rootPrimaryType);
        }
        UpdatePropertiesRequest request = new UpdatePropertiesRequest(topNode, workspaceName, properties, true);
        request.setActualLocationOfNode(topNode);
        process(request);
        checkRequestForErrors(request);

        // Create a map to record the actual locations of the parent nodes of each read request ...
        Map<Path, Location> locationsByPath = new HashMap<Path, Location>();
        locationsByPath.put(startingLocation.getPath(), startingLocation);

        // Create a queue that we'll use to walk the content ...
        LinkedList<Location> locationsToRead = new LinkedList<Location>();

        // Now walk the remaining nodes in the subgraph ...
        while (true) {
            while (locationIter.hasNext()) {

                // Index the node ...
                Location location = locationIter.next();
                Path path = location.getPath();
                Path parentPath = path.getParent();
                Location parent = readSubgraph.getLocationFor(parentPath);
                if (parent == null) {
                    parent = locationsByPath.get(parentPath);
                }
                Name childName = path.getLastSegment().getName();
                Collection<Property> nodePoperties = readSubgraph.getPropertiesFor(location).values();
                CreateNodeRequest create = new CreateNodeRequest(parent, workspaceName, childName, nodePoperties);
                create.setActualLocationOfNode(location); // set this so we don't have to figure it out
                process(create);
                if (create.isCancelled() || create.hasError()) return;

                // Process the children ...
                boolean recordedParentLocation = false;
                for (Location child : readSubgraph.getChildren(location)) {
                    if (!readSubgraph.includes(child)) {
                        if (!recordedParentLocation) {
                            locationsByPath.put(location.getPath(), location);
                            recordedParentLocation = true;
                        }
                        // The subgraph did not contain the child, so record the location as needing to be read ...
                        locationsToRead.add(child);
                    }
                }
            }

            if (locationsToRead.isEmpty()) break;
            Location location = locationsToRead.poll();
            assert location != null;

            // Recompute the depth per read ...
            depthPerRead = Math.min(maxDepthPerRead, depth - location.getPath().size());
            if (depthPerRead < 1) continue;
            readSubgraph = new ReadBranchRequest(location, workspaceName, depthPerRead);
            try {
                channel.addAndAwait(readSubgraph);
            } catch (InterruptedException e) {
                // Clear the interrupted status of the thread and continue ...
                Thread.interrupted();
                return;
            }
            checkRequestForErrors(readSubgraph);
            locationIter = readSubgraph.iterator();
        }
    }

    protected void indexProperties( String workspaceName,
                                    Location location ) {
        ReadAllPropertiesRequest readProps = new ReadAllPropertiesRequest(location, workspaceName);
        try {
            channel.addAndAwait(readProps);
        } catch (InterruptedException e) {
            // Clear the interrupted status of the thread and continue ...
            Thread.interrupted();
        }
        checkRequestForErrors(readProps);

        // Now update the properties in the search engine ...
        location = readProps.getActualLocationOfNode();
        Map<Name, Property> properties = readProps.getPropertiesByName();
        UpdatePropertiesRequest request = new UpdatePropertiesRequest(location, workspaceName, properties, true);
        request.setActualLocationOfNode(location);
        process(request);
        checkRequestForErrors(readProps);
    }

    /**
     * Send the supplied change request directly to the search engine's processor.
     * 
     * @param searchEngineRequest
     */
    public final void process( ChangeRequest searchEngineRequest ) {
        processor.process(searchEngineRequest);
    }

    protected final void checkRequestForErrors( Request request ) throws RepositorySourceException, RuntimeException {
        if (request.hasError()) {
            Throwable t = request.getError();
            if (t instanceof RuntimeException) throw (RuntimeException)t;
            throw new RepositorySourceException(sourceName, t);
        }
    }

    protected final void checkNotClosed() throws IllegalStateException {
        if (closed) {
            throw new IllegalStateException(GraphI18n.searchEngineIndexerForSourceHasAlreadyBeenClosed.text(sourceName));
        }
    }

    /**
     * Return whether this indexer has already been {@link #close() closed}.
     * 
     * @return true if this has been closed, or false if it is still usable
     * @see #close()
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Close this indexer and release all resources. This method has no effect if it is called when this indexer is alread closed.
     * 
     * @see #isClosed()
     */
    public void close() {
        if (closed) return;
        closed = true;
        // Close the channel ...
        try {
            channel.close();
        } finally {
            // And shut down the executor service ...
            service.shutdown();
            try {
                service.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Log this ...
                I18n msg = GraphI18n.errorShuttingDownExecutorServiceInSearchEngineIndexer;
                Logger.getLogger(getClass()).error(msg, sourceName);
                // Clear the interrupted status of the thread ...
                Thread.interrupted();
            } finally {
                // Close the search engine processor ...
                processor.close();
            }
        }
    }
}
