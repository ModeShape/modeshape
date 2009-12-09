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
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.InvalidPathException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.request.CompositeRequestChannel;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DeleteChildrenRequest;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;
import org.jboss.dna.graph.search.SearchEngine.Workspaces;

/**
 * The processor that is created by the provider whenever a logical set of activities needs to be performed.
 * 
 * @param <WorkspaceType> the type of workspace
 */
public abstract class SearchEngineProcessor<WorkspaceType extends SearchEngineWorkspace> extends RequestProcessor {

    protected boolean rollback = false;
    protected final Workspaces<WorkspaceType> workspaces;

    /**
     * @param sourceName
     * @param context
     * @param workspaces
     * @param observer
     * @param now
     */
    protected SearchEngineProcessor( String sourceName,
                                     ExecutionContext context,
                                     Workspaces<WorkspaceType> workspaces,
                                     Observer observer,
                                     DateTime now ) {
        super(sourceName, context, observer, now);
        this.workspaces = workspaces;
        assert this.workspaces != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#close()
     */
    @Override
    public void close() {
        try {
            if (rollback) rollback();
            else commit();
        } finally {
            // publish any changes to the observer ...
            super.close();
        }
    }

    /**
     * Subclasses should implement this method to throw away any work that has been done with this processor.
     */
    protected abstract void rollback();

    /**
     * Subclasses should implement this method to commit and save any work that has been done with this processor.
     */
    protected abstract void commit();

    /**
     * Optimize the indexes for all workspaces, if required.
     * 
     * @return true if an optimization was performed, or false if there was no need
     */
    public boolean optimize() {
        // do nothing by default
        return false;
    }

    /**
     * Optimize the indexes for the named workspace, if required.
     * 
     * @param workspaceName the name of the workspace to be optimized; never null
     * @return true if an optimization was performed, or false if there was no need
     */
    public boolean optimize( String workspaceName ) {
        // do nothing by default
        return false;
    }

    /**
     * Utility method to index all of the content at or below the supplied path in the named workspace within the
     * {@link #getSourceName() source}. If the starting point is the root node, then this method will drop the existing index(es)
     * and rebuild from the content in the workspace of the source.
     * <p>
     * This method works by reading the graph and constructing and {@link #process(Request) processing} the corresponding
     * {@link CreateNodeRequest}s ( and possibly a single {@link UpdatePropertiesRequest} for the top-level node) that result in
     * the same subgraph being 'created' in the index.
     * </p>
     * 
     * @param workspaceName the name of the workspace to be crawled; may not be null
     * @param startingPoint the location that represents the content to be indexed; must have a path
     * @param depthPerRead the depth of each subgraph read operation
     * @return the number of nodes that were indexed
     * @throws RepositorySourceException if there is a problem accessing the content
     */
    protected int crawl( String workspaceName,
                         Location startingPoint,
                         int depthPerRead ) {
        CompositeRequestChannel channel = new CompositeRequestChannel(getSourceName());
        ExecutorService service = Executors.newSingleThreadExecutor();
        channel.start(service, getExecutionContext(), workspaces.getRepositoryConnectionFactory());
        try {
            return crawl(workspaceName, startingPoint, depthPerRead, channel);
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
        return 0;
    }

    /**
     * Utility method to index all of the content at or below the supplied path in the named workspace within the
     * {@link #getSourceName() source}. If the starting point is the root node, then this method will drop the existing index(es)
     * and rebuild from the content in the workspace of the source.
     * <p>
     * This method works by reading the graph and constructing and {@link #process(Request) processing} the corresponding
     * {@link CreateNodeRequest}s ( and possibly a single {@link UpdatePropertiesRequest} for the top-level node) that result in
     * the same subgraph being 'created' in the index.
     * </p>
     * 
     * @param workspaceName the name of the workspace to be crawled; may not be null
     * @param startingPoint the location that represents the content to be indexed; must have a path
     * @param depthPerRead the depth of each subgraph read operation
     * @param channel the channel that has been openned (and started) and that should be used to add requests to the underlying
     *        source; may not be null
     * @return the number of nodes that were indexed
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws InterruptedException if the channel thread was interrupted
     */
    protected int crawl( String workspaceName,
                         Location startingPoint,
                         int depthPerRead,
                         CompositeRequestChannel channel ) throws InterruptedException {
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
            int count = 0;

            // Destroy the nodes at the supplied location ...
            if (startingPoint.getPath().isRoot()) {
                // Just delete the whole content ...
                process(new DeleteBranchRequest(startingPoint, workspaceName));
            } else {
                // We can't delete the node, since later same-name-siblings might be changed. So delete the children ...
                process(new DeleteChildrenRequest(startingPoint, workspaceName));
            }

            // Now update all of the properties, removing any that are no longer needed ...
            Location topNode = locationIter.next();
            Map<Name, Property> properties = readSubgraph.getPropertiesFor(topNode);
            boolean removeOtherProperties = true;
            UpdatePropertiesRequest request = new UpdatePropertiesRequest(startingPoint, workspaceName, properties,
                                                                          removeOtherProperties);
            request.setActualLocationOfNode(topNode);
            process(request);
            if (request.isCancelled() || request.hasError()) {
                rollback = true;
                return count;
            }
            ++count;

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
                    process(create);
                    if (create.isCancelled() || create.hasError()) {
                        rollback = true;
                        return count;
                    }
                    ++count;

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
            return count;

        } catch (InvalidPathException e) {
            // The node must no longer exist, so delete it from the indexes ...
            requests.add(new DeleteBranchRequest(startingPoint, workspaceName));
        }
        return 0;
    }
}
