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
package org.jboss.dna.connector.federation.executor;

import java.util.Set;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.connector.federation.Projection;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.connectors.RepositoryConnectionFactory;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.properties.DateTime;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.requests.CopyBranchRequest;
import org.jboss.dna.graph.requests.CreateNodeRequest;
import org.jboss.dna.graph.requests.DeleteBranchRequest;
import org.jboss.dna.graph.requests.MoveBranchRequest;
import org.jboss.dna.graph.requests.ReadAllChildrenRequest;
import org.jboss.dna.graph.requests.ReadAllPropertiesRequest;
import org.jboss.dna.graph.requests.ReadNodeRequest;
import org.jboss.dna.graph.requests.Request;
import org.jboss.dna.graph.requests.UpdatePropertiesRequest;
import org.jboss.dna.graph.requests.processor.RequestProcessor;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class SingleProjectionCommandExecutor extends RequestProcessor {

    private final Projection projection;
    private final PathFactory pathFactory;
    private final RepositoryConnectionFactory connectionFactory;
    private RepositoryConnection connection;

    /**
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param projection the projection used for the cached information; may not be null and must have exactly one
     *        {@link Projection#getRules() rule}
     * @param connectionFactory the factory for {@link RepositoryConnection} instances
     */
    public SingleProjectionCommandExecutor( ExecutionContext context,
                                            String sourceName,
                                            Projection projection,
                                            RepositoryConnectionFactory connectionFactory ) {
        this(context, sourceName, null, projection, connectionFactory);
    }

    /**
     * @param context the execution context in which the executor will be run; may not be null
     * @param sourceName the name of the {@link RepositorySource} that is making use of this executor; may not be null or empty
     * @param now the current time; may be null if the system time is to be used
     * @param projection the projection used for the cached information; may not be null and must have exactly one
     *        {@link Projection#getRules() rule}
     * @param connectionFactory the factory for {@link RepositoryConnection} instances
     */
    public SingleProjectionCommandExecutor( ExecutionContext context,
                                            String sourceName,
                                            DateTime now,
                                            Projection projection,
                                            RepositoryConnectionFactory connectionFactory ) {
        super(sourceName, context, now);
        assert connectionFactory != null;
        assert projection != null;
        assert projection.getRules().size() == 1;
        this.projection = projection;
        this.connectionFactory = connectionFactory;
        this.pathFactory = context.getValueFactories().getPathFactory();
        assert this.pathFactory != null;
    }

    protected RepositoryConnection getConnection() throws RepositorySourceException {
        if (connection == null) {
            // Create a connection ...
            connection = this.connectionFactory.createConnection(this.projection.getSourceName());
        }
        return connection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see RequestProcessor#close()
     */
    @Override
    public void close() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } finally {
                this.connection = null;
            }
        }
        super.close();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        Location locationInSource = projectIntoSource(request.of());
        ReadAllChildrenRequest projected = new ReadAllChildrenRequest(locationInSource);
        getConnection().execute(this.getExecutionContext(), projected);
        if (projected.hasError()) {
            return;
        }
        for (Location child : projected.getChildren()) {
            request.addChild(child);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        Location locationInSource = projectIntoSource(request.at());
        ReadAllPropertiesRequest projected = new ReadAllPropertiesRequest(locationInSource);
        getConnection().execute(this.getExecutionContext(), projected);
        if (projected.hasError()) {
            projectError(projected, request.at(), request);
            return;
        }
        for (Property property : projected.getProperties()) {
            request.addProperty(property);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        Location locationInSource = projectIntoSource(request.at());
        ReadNodeRequest projected = new ReadNodeRequest(locationInSource);
        getConnection().execute(this.getExecutionContext(), projected);
        if (projected.hasError()) {
            projectError(projected, request.at(), request);
            return;
        }
        for (Property property : projected.getProperties()) {
            request.addProperty(property);
        }
        for (Location child : projected.getChildren()) {
            request.addChild(child);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        Location locationInSource = projectIntoSource(request.under());
        Name child = request.named();
        Integer desiredIndex = request.desiredIndex();
        int index = desiredIndex != null ? desiredIndex.intValue() : 0;
        CreateNodeRequest projected = new CreateNodeRequest(locationInSource, child, index, request.properties());
        getConnection().execute(this.getExecutionContext(), projected);
        if (projected.hasError()) {
            projectError(projected, request.under(), request);
            return;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        Location locationInSource = projectIntoSource(request.on());
        UpdatePropertiesRequest projected = new UpdatePropertiesRequest(locationInSource, request.properties());
        getConnection().execute(this.getExecutionContext(), projected);
        if (projected.hasError()) {
            projectError(projected, request.on(), request);
            return;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        Location locationInSource = projectIntoSource(request.at());
        DeleteBranchRequest projected = new DeleteBranchRequest(locationInSource);
        getConnection().execute(this.getExecutionContext(), projected);
        if (projected.hasError()) {
            projectError(projected, request.at(), request);
            return;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        Location fromLocationInSource = projectIntoSource(request.from());
        Location intoLocationInSource = projectIntoSource(request.into());
        MoveBranchRequest projected = new MoveBranchRequest(fromLocationInSource, intoLocationInSource);
        getConnection().execute(this.getExecutionContext(), projected);
        if (projected.hasError()) {
            projectError(projected, null, request);
            return;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        Location fromLocationInSource = projectIntoSource(request.from());
        Location intoLocationInSource = projectIntoSource(request.into());
        CopyBranchRequest projected = new CopyBranchRequest(fromLocationInSource, intoLocationInSource);
        getConnection().execute(this.getExecutionContext(), projected);
        if (projected.hasError()) {
            projectError(projected, null, request);
            return;
        }
    }

    protected Location projectIntoSource( Location pathInRepository ) {
        Path path = pathInRepository.getPath();
        CheckArg.isNotNull(path, "pathInRepository.getPath()");
        Set<Path> paths = this.projection.getPathsInSource(path, pathFactory);
        if (paths.isEmpty()) return null;
        Path projectedPath = paths.iterator().next();
        Location location = null;
        if (pathInRepository.hasIdProperties()) {
            location = new Location(projectedPath, pathInRepository.getIdProperties());
        } else {
            new Location(projectedPath);
        }
        return location;
    }

    protected Location projectIntoRepository( Location pathInSource ) {
        Path path = pathInSource.getPath();
        CheckArg.isNotNull(path, "pathInSource.getPath()");
        Path projectedPath = this.projection.getPathsInRepository(path, pathFactory).iterator().next();
        Location location = null;
        if (pathInSource.hasIdProperties()) {
            location = new Location(projectedPath, pathInSource.getIdProperties());
        } else {
            new Location(projectedPath);
        }
        return location;
    }

    protected void projectError( Request original,
                                 Location originalLocation,
                                 Request projected ) {
        Throwable error = original.getError();
        if (error instanceof PathNotFoundException) {
            PathNotFoundException pnf = (PathNotFoundException)error;
            Path lowestExisting = pnf.getLowestAncestorThatDoesExist();
            if (lowestExisting != null) lowestExisting = projectIntoRepository(new Location(lowestExisting)).getPath();
            if (originalLocation == null) originalLocation = projectIntoRepository(pnf.getLocation());
            error = new PathNotFoundException(originalLocation, lowestExisting, pnf.getMessage());
        }
        projected.setError(error);
    }

}
