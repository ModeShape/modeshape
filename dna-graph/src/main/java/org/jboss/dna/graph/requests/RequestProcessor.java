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
package org.jboss.dna.graph.requests;

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
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.basic.BasicEmptyProperty;

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

    protected RequestProcessor( String sourceName,
                                ExecutionContext context ) {
        CheckArg.isNotEmpty(sourceName, "sourceName");
        CheckArg.isNotNull(context, "context");
        this.context = context;
        this.sourceName = sourceName;
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
     * {@link ReadBlockOfChildrenRequest#startingAt() starting index} and a {@link ReadBlockOfChildrenRequest#count() maximum
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
        if (allChildren.size() < request.startingAt()) return;

        // Now, find the children in the block ...
        int endIndex = Math.min(request.endingBefore(), allChildren.size());
        for (int i = request.startingAt(); i != endIndex; ++i) {
            request.addChild(allChildren.get(i));
        }
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
        while (locationsToRead.peek() != null) {
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
            request.setProperties(read.location, readNode.getProperties());

            // Read the children for this node, and add them to the list of locations to be read ...
            ReadAllChildrenRequest readChildren = new ReadAllChildrenRequest(read.location);
            process(readChildren);
            request.setChildren(read.location, readChildren.getChildren());

            // Add each of the children to the list of locations that we need to read ...
            for (Location child : readChildren) {
                locationsToRead.add(new LocationWithDepth(child, read.depth + 1));
            }
        }
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
        // Read the children ...
        ReadAllChildrenRequest readChildren = new ReadAllChildrenRequest(request.at());
        process(readChildren);
        if (readChildren.hasError()) {
            request.setError(readChildren.getError());
            return;
        }
        // Now, copy all of the results into the submitted request ...
        for (Property property : readProperties) {
            request.addProperty(property);
        }
        for (Location child : readChildren) {
            request.addChild(child);
        }
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
        ReadNodeRequest readNode = new ReadNodeRequest(request.on());
        process(readNode);
        if (readNode.hasError()) {
            request.setError(readNode.getError());
            return;
        }
        Property property = readNode.getPropertiesByName().get(request.named());
        request.setProperty(property);
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
    }

    /**
     * A class that represents a location at a known depth
     * 
     * @author Randall Hauch
     */
    protected static class LocationWithDepth {
        protected final Location location;
        protected final int depth;

        protected LocationWithDepth( Location location,
                                     int depth ) {
            this.location = location;
            this.depth = depth;
        }

        @Override
        public String toString() {
            return location.toString() + " at depth " + depth;
        }
    }

}
