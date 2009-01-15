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
package org.jboss.dna.graph.request.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ReferentialIntegrityException;
import org.jboss.dna.graph.property.basic.BasicEmptyProperty;
import org.jboss.dna.graph.request.CacheableRequest;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.ReadPropertyRequest;
import org.jboss.dna.graph.request.RemovePropertiesRequest;
import org.jboss.dna.graph.request.RenameNodeRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;

/**
 * A component that is used to process and execute {@link Request}s. This class is intended to be subclassed and methods
 * overwritten to define the behavior for executing the different kinds of requests. Abstract methods must be overridden, but
 * non-abstract methods all have meaningful default implementations.
 * 
 * @author Randall Hauch
 */
@Immutable
public abstract class RequestProcessor {

    private final ExecutionContext context;
    private final String sourceName;
    private final DateTime nowInUtc;
    private final CachePolicy defaultCachePolicy;

    protected RequestProcessor( String sourceName,
                                ExecutionContext context ) {
        this(sourceName, context, null, null);
    }

    protected RequestProcessor( String sourceName,
                                ExecutionContext context,
                                DateTime now ) {
        this(sourceName, context, now, null);
    }

    protected RequestProcessor( String sourceName,
                                ExecutionContext context,
                                DateTime now,
                                CachePolicy defaultCachePolicy ) {
        CheckArg.isNotEmpty(sourceName, "sourceName");
        CheckArg.isNotNull(context, "context");
        this.context = context;
        this.sourceName = sourceName;
        this.nowInUtc = now != null ? now : context.getValueFactories().getDateFactory().createUtc();
        this.defaultCachePolicy = defaultCachePolicy;
    }

    /**
     * Get the name of the source against which this processor is executing.
     * 
     * @return the repository source name; never null or empty
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * The execution context that this process is operating within.
     * 
     * @return the execution context; never null
     */
    public ExecutionContext getExecutionContext() {
        return this.context;
    }

    /**
     * Get the 'current time' for this processor, which is usually a constant during its lifetime.
     * 
     * @return the current time in UTC; never null
     */
    protected DateTime getNowInUtc() {
        return this.nowInUtc;
    }

    /**
     * Set the supplied request to have the default cache policy and the {@link #getNowInUtc() current time in UTC}.
     * 
     * @param request the cacheable request
     */
    protected void setCacheableInfo( CacheableRequest request ) {
        request.setCachePolicy(defaultCachePolicy);
        request.setTimeLoaded(nowInUtc);
    }

    /**
     * Set the supplied request to have the supplied cache policy and the {@link #getNowInUtc() current time in UTC}.
     * 
     * @param request the cacheable request
     * @param cachePolicy the cache policy for the request; may be null if there is to be no cache policy
     */
    protected void setCacheableInfo( CacheableRequest request,
                                     CachePolicy cachePolicy ) {
        request.setCachePolicy(cachePolicy);
        request.setTimeLoaded(nowInUtc);
    }

    /**
     * Process a request by determining the type of request and delegating to the appropriate <code>process</code> method for that
     * type.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the general request
     */
    public void process( Request request ) {
        if (request == null) return;
        if (request.isCancelled()) return;
        if (request instanceof CompositeRequest) {
            process((CompositeRequest)request);
        } else if (request instanceof CopyBranchRequest) {
            process((CopyBranchRequest)request);
        } else if (request instanceof CreateNodeRequest) {
            process((CreateNodeRequest)request);
        } else if (request instanceof DeleteBranchRequest) {
            process((DeleteBranchRequest)request);
        } else if (request instanceof MoveBranchRequest) {
            process((MoveBranchRequest)request);
        } else if (request instanceof ReadAllChildrenRequest) {
            process((ReadAllChildrenRequest)request);
        } else if (request instanceof ReadNextBlockOfChildrenRequest) {
            process((ReadNextBlockOfChildrenRequest)request);
        } else if (request instanceof ReadBlockOfChildrenRequest) {
            process((ReadBlockOfChildrenRequest)request);
        } else if (request instanceof ReadBranchRequest) {
            process((ReadBranchRequest)request);
        } else if (request instanceof ReadNodeRequest) {
            process((ReadNodeRequest)request);
        } else if (request instanceof ReadAllPropertiesRequest) {
            process((ReadAllPropertiesRequest)request);
        } else if (request instanceof ReadPropertyRequest) {
            process((ReadPropertyRequest)request);
        } else if (request instanceof RemovePropertiesRequest) {
            process((RemovePropertiesRequest)request);
        } else if (request instanceof RenameNodeRequest) {
            process((RenameNodeRequest)request);
        } else if (request instanceof UpdatePropertiesRequest) {
            process((UpdatePropertiesRequest)request);
        }
    }

    /**
     * Process a request that is composed of multiple other (non-composite) requests. If any of the embedded requests
     * {@link Request#hasError() has an error} after it is processed, the submitted request will be marked with an error.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the composite request
     */
    public void process( CompositeRequest request ) {
        if (request == null) return;
        int numberOfErrors = 0;
        Throwable firstError = null;
        for (Request embedded : request) {
            assert embedded != null;
            if (embedded.isCancelled()) return;
            process(embedded);
            if (embedded.hasError()) {
                if (numberOfErrors == 0) firstError = embedded.getError();
                ++numberOfErrors;
            }
        }
        if (firstError == null) return;
        if (numberOfErrors == 1) {
            request.setError(firstError);
        } else {
            String msg = GraphI18n.multipleErrorsWhileExecutingRequests.text(numberOfErrors, request.size());
            request.setError(new RepositorySourceException(getSourceName(), msg));
        }
    }

    /**
     * Process a request to copy a branch into another location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the copy request
     */
    public abstract void process( CopyBranchRequest request );

    /**
     * Process a request to create a node at a specified location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the create request
     */
    public abstract void process( CreateNodeRequest request );

    /**
     * Process a request to delete a branch at a specified location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the delete request
     * @throws ReferentialIntegrityException if the delete could not be performed because some references to deleted nodes would
     *         have remained after the delete operation completed
     */
    public abstract void process( DeleteBranchRequest request );

    /**
     * Process a request to move a branch at a specified location into a different location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the move request
     */
    public abstract void process( MoveBranchRequest request );

    /**
     * Process a request to read all of the children of a node.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the read request
     */
    public abstract void process( ReadAllChildrenRequest request );

    /**
     * Process a request to read a block of the children of a node. The block is defined by a
     * {@link ReadBlockOfChildrenRequest#startingAtIndex() starting index} and a {@link ReadBlockOfChildrenRequest#count() maximum
     * number of children to include in the block}.
     * <p>
     * This method does nothing if the request is null. The default implementation converts the command to a
     * {@link ReadAllChildrenRequest}, and then finds the children within the block. Obviously for large numbers of children, this
     * implementation may not be efficient and may need to be overridden.
     * </p>
     * 
     * @param request the read request
     */
    public void process( ReadBlockOfChildrenRequest request ) {
        if (request == null) return;
        // Convert the request to a ReadAllChildrenRequest and execute it ...
        ReadAllChildrenRequest readAll = new ReadAllChildrenRequest(request.of());
        process(readAll);
        if (readAll.hasError()) {
            request.setError(readAll.getError());
            return;
        }
        List<Location> allChildren = readAll.getChildren();

        // If there aren't enough children for the block's range ...
        if (allChildren.size() < request.startingAtIndex()) return;

        // Now, find the children in the block ...
        int endIndex = Math.min(request.endingBefore(), allChildren.size());
        for (int i = request.startingAtIndex(); i != endIndex; ++i) {
            request.addChild(allChildren.get(i));
        }
        // Set the actual location ...
        request.setActualLocationOfNode(readAll.getActualLocationOfNode());
        setCacheableInfo(request);
    }

    /**
     * Process a request to read the next block of the children of a node, starting after a previously-retrieved child.
     * <p>
     * This method does nothing if the request is null. The default implementation converts the command to a
     * {@link ReadAllChildrenRequest}, and then finds the children within the block. Obviously for large numbers of children, this
     * implementation may not be efficient and may need to be overridden.
     * </p>
     * 
     * @param request the read request
     */
    public void process( ReadNextBlockOfChildrenRequest request ) {
        if (request == null) return;

        // Get the parent path ...
        Path path = request.startingAfter().getPath();
        Location actualSiblingLocation = request.startingAfter();
        Path parentPath = null;
        if (path != null) parentPath = path.getParent();
        if (parentPath == null) {
            ReadAllPropertiesRequest readPropertiesOfSibling = new ReadAllPropertiesRequest(request.startingAfter());
            process(readPropertiesOfSibling);
            actualSiblingLocation = readPropertiesOfSibling.getActualLocationOfNode();
            parentPath = actualSiblingLocation.getPath().getParent();
        }
        assert parentPath != null;

        // Convert the request to a ReadAllChildrenRequest and execute it ...
        ReadAllChildrenRequest readAll = new ReadAllChildrenRequest(new Location(parentPath));
        process(readAll);
        if (readAll.hasError()) {
            request.setError(readAll.getError());
            return;
        }
        List<Location> allChildren = readAll.getChildren();

        // Iterate through the children, looking for the 'startingAfter' child ...
        boolean found = false;
        int count = 0;
        for (Location child : allChildren) {
            if (count > request.count()) break;
            if (!found) {
                // Set to true if we find the child we're looking for ...
                found = child.equals(request.startingAfter());
            } else {
                // Add the child to the block ...
                ++count;
                request.addChild(child);
            }
        }

        // Set the actual location ...
        request.setActualLocationOfStartingAfterNode(actualSiblingLocation);
        setCacheableInfo(request);
    }

    /**
     * Process a request to read a branch or subgraph that's below a node at a specified location.
     * <p>
     * This method does nothing if the request is null. The default implementation processes the branch by submitting the
     * equivalent requests to {@link ReadNodeRequest read the nodes} and the {@link ReadAllChildrenRequest children}. It starts by
     * doing this for the top-level node, then proceeds for each of the children of that node, and so forth.
     * </p>
     * 
     * @param request the request to read the branch
     */
    public void process( ReadBranchRequest request ) {
        if (request == null) return;
        // Create a queue for locations that need to be read ...
        Queue<LocationWithDepth> locationsToRead = new LinkedList<LocationWithDepth>();
        locationsToRead.add(new LocationWithDepth(request.at(), 1));

        // Now read the locations ...
        boolean first = true;
        while (locationsToRead.peek() != null) {
            if (request.isCancelled()) return;
            LocationWithDepth read = locationsToRead.poll();

            // Check the depth ...
            if (read.depth > request.maximumDepth()) break;

            // Read the properties ...
            ReadNodeRequest readNode = new ReadNodeRequest(read.location);
            process(readNode);
            if (readNode.hasError()) {
                request.setError(readNode.getError());
                return;
            }
            Location actualLocation = readNode.getActualLocationOfNode();
            if (first) {
                // Set the actual location on the original request
                request.setActualLocationOfNode(actualLocation);
                first = false;
            }

            // Record in the request the children and properties that were read on this node ...
            request.setChildren(actualLocation, readNode.getChildren());
            request.setProperties(actualLocation, readNode.getProperties());

            // Add each of the children to the list of locations that we need to read ...
            for (Location child : readNode.getChildren()) {
                locationsToRead.add(new LocationWithDepth(child, read.depth + 1));
            }
        }
        setCacheableInfo(request);
    }

    /**
     * Process a request to read the properties of a node at the supplied location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the read request
     */
    public abstract void process( ReadAllPropertiesRequest request );

    /**
     * Process a request to read the properties and children of a node at the supplied location.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts the single request into a
     * {@link ReadAllChildrenRequest} and a {@link ReadAllPropertiesRequest}.
     * </p>
     * 
     * @param request the read request
     */
    public void process( ReadNodeRequest request ) {
        if (request == null) return;
        // Read the properties ...
        ReadAllPropertiesRequest readProperties = new ReadAllPropertiesRequest(request.at());
        process(readProperties);
        if (readProperties.hasError()) {
            request.setError(readProperties.getError());
            return;
        }
        // Set the actual location ...
        request.setActualLocationOfNode(readProperties.getActualLocationOfNode());

        // Read the children ...
        ReadAllChildrenRequest readChildren = new ReadAllChildrenRequest(request.at());
        process(readChildren);
        if (readChildren.hasError()) {
            request.setError(readChildren.getError());
            return;
        }
        if (request.isCancelled()) return;
        // Now, copy all of the results into the submitted request ...
        for (Property property : readProperties) {
            request.addProperty(property);
        }
        for (Location child : readChildren) {
            request.addChild(child);
        }
        setCacheableInfo(request);
    }

    /**
     * Process a request to read a single property of a node at the supplied location.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts the request that
     * {@link ReadNodeRequest reads the node} and simply returns the one property.
     * </p>
     * 
     * @param request the read request
     */
    public void process( ReadPropertyRequest request ) {
        if (request == null) return;
        ReadAllPropertiesRequest readNode = new ReadAllPropertiesRequest(request.on());
        process(readNode);
        if (readNode.hasError()) {
            request.setError(readNode.getError());
            return;
        }
        Property property = readNode.getPropertiesByName().get(request.named());
        request.setProperty(property);
        // Set the actual location ...
        request.setActualLocationOfNode(readNode.getActualLocationOfNode());
        setCacheableInfo(request);
    }

    /**
     * Process a request to remove the specified properties from a node.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts this request into a
     * {@link UpdatePropertiesRequest}.
     * </p>
     * 
     * @param request the request to remove the properties with certain names
     */
    public void process( RemovePropertiesRequest request ) {
        if (request == null) return;
        Collection<Name> names = request.propertyNames();
        if (names.isEmpty()) return;
        List<Property> emptyProperties = new ArrayList<Property>(names.size());
        for (Name propertyName : names) {
            emptyProperties.add(new BasicEmptyProperty(propertyName));
        }
        UpdatePropertiesRequest update = new UpdatePropertiesRequest(request.from(), emptyProperties);
        process(update);
        if (update.hasError()) {
            request.setError(update.getError());
        }
        // Set the actual location ...
        request.setActualLocationOfNode(update.getActualLocationOfNode());
    }

    /**
     * Process a request to remove the specified properties from a node.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the remove request
     */
    public abstract void process( UpdatePropertiesRequest request );

    /**
     * Process a request to rename a node specified location into a different location.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts the rename into a
     * {@link MoveBranchRequest move}. However, this only works if the <code>request</code> has a {@link Location#hasPath() path}
     * for its {@link RenameNodeRequest#at() location}. (If not, this method throws an {@link UnsupportedOperationException} and
     * must be overriddent.)
     * </p>
     * 
     * @param request the rename request
     */
    public void process( RenameNodeRequest request ) {
        if (request == null) return;
        Location from = request.at();
        if (!from.hasPath()) {
            throw new UnsupportedOperationException();
        }
        Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(from.getPath(), request.toName());
        Location to = new Location(newPath);
        MoveBranchRequest move = new MoveBranchRequest(from, to);
        process(move);
        // Set the actual locations ...
        request.setActualLocations(move.getActualLocationBefore(), move.getActualLocationAfter());
    }

    /**
     * Close this processor, allowing it to clean up any open resources.
     */
    public void close() {
        // do nothing
    }

    /**
     * A class that represents a location at a known depth
     * 
     * @author Randall Hauch
     */
    @Immutable
    protected static class LocationWithDepth {
        protected final Location location;
        protected final int depth;

        protected LocationWithDepth( Location location,
                                     int depth ) {
            this.location = location;
            this.depth = depth;
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }

        @Override
        public String toString() {
            return location.toString() + " at depth " + depth;
        }
    }

}
