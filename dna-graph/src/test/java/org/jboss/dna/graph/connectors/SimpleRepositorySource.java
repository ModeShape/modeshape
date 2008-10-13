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
package org.jboss.dna.graph.connectors;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.naming.Reference;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.commands.ActsOnPath;
import org.jboss.dna.graph.commands.CreateNodeCommand;
import org.jboss.dna.graph.commands.DeleteBranchCommand;
import org.jboss.dna.graph.commands.GetChildrenCommand;
import org.jboss.dna.graph.commands.GetPropertiesCommand;
import org.jboss.dna.graph.commands.GraphCommand;
import org.jboss.dna.graph.commands.SetPropertiesCommand;
import org.jboss.dna.graph.commands.executor.AbstractCommandExecutor;
import org.jboss.dna.graph.commands.executor.CommandExecutor;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.basic.BasicSingleValueProperty;
import org.jboss.dna.graph.requests.Request;

/**
 * A {@link RepositorySource} for a {@link SimpleRepository simple repository}.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class SimpleRepositorySource implements RepositorySource {

    private static final long serialVersionUID = 1L;

    /**
     * The default limit is {@value} for retrying {@link RepositoryConnection connection} calls to the underlying source.
     */
    public static final int DEFAULT_RETRY_LIMIT = 0;
    private String repositoryName;
    private String name;
    private final AtomicInteger retryLimit = new AtomicInteger(DEFAULT_RETRY_LIMIT);
    private CachePolicy defaultCachePolicy;
    private transient RepositoryContext repositoryContext;

    public SimpleRepositorySource() {
        super();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#initialize(org.jboss.dna.graph.connectors.RepositoryContext)
     */
    public void initialize( RepositoryContext context ) throws RepositorySourceException {
        this.repositoryContext = context;
    }

    /**
     * @return repositoryContext
     */
    public RepositoryContext getRepositoryContext() {
        return repositoryContext;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getName()
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return repositoryName
     */
    public String getRepositoryName() {
        return repositoryName;
    }

    /**
     * @param repositoryName Sets repositoryName to the specified value.
     */
    public void setRepositoryName( String repositoryName ) {
        this.repositoryName = repositoryName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getRetryLimit()
     */
    public int getRetryLimit() {
        return retryLimit.get();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#setRetryLimit(int)
     */
    public void setRetryLimit( int limit ) {
        retryLimit.set(limit < 0 ? 0 : limit);
    }

    /**
     * @return defaultCachePolicy
     */
    public CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
    }

    /**
     * @param defaultCachePolicy Sets defaultCachePolicy to the specified value.
     */
    public void setDefaultCachePolicy( CachePolicy defaultCachePolicy ) {
        this.defaultCachePolicy = defaultCachePolicy;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.naming.Referenceable#getReference()
     */
    public Reference getReference() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SimpleRepositorySource) {
            SimpleRepositorySource that = (SimpleRepositorySource)obj;
            if (!this.getName().equals(that.getName())) return false;
            if (!this.getRepositoryName().equals(that.getRepositoryName())) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getCapabilities()
     */
    public RepositorySourceCapabilities getCapabilities() {
        return new Capabilities();
    }

    protected class Capabilities implements RepositorySourceCapabilities {
        public boolean supportsSameNameSiblings() {
            return true;
        }

        public boolean supportsUpdates() {
            return true;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.RepositorySource#getConnection()
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        String reposName = this.getRepositoryName();
        if (reposName == null) throw new RepositorySourceException("Invalid repository source: missing repository name");
        SimpleRepository repository = SimpleRepository.get(reposName);
        if (repository == null) {
            throw new RepositorySourceException(this.getName(), "Unable to find repository \"" + reposName + "\"");
        }
        return new Connection(repository, this.getDefaultCachePolicy());
    }

    protected class Connection implements RepositoryConnection {

        private RepositorySourceListener listener;
        private final SimpleRepository repository;
        private final CachePolicy defaultCachePolicy;

        protected Connection( SimpleRepository repository,
                              CachePolicy defaultCachePolicy ) {
            assert repository != null;
            this.repository = repository;
            this.defaultCachePolicy = defaultCachePolicy;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connectors.RepositoryConnection#close()
         */
        public void close() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connectors.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
         *      org.jboss.dna.graph.commands.GraphCommand[])
         */
        public void execute( ExecutionContext context,
                             GraphCommand... commands ) throws RepositorySourceException {
            assert context != null;
            if (repository.isShutdown()) {
                throw new RepositorySourceException(getName(), "The repository \"" + repository.getRepositoryName()
                                                               + "\" is no longer available");
            }
            // Now execute the commands ...
            CommandExecutor executor = new Executor(this.repository, context, this.getSourceName());
            for (GraphCommand command : commands) {
                executor.execute(command);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connectors.RepositoryConnection#execute(org.jboss.dna.graph.ExecutionContext,
         *      org.jboss.dna.graph.requests.Request)
         */
        public void execute( ExecutionContext context,
                             Request request ) throws RepositorySourceException {
            // TODO
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connectors.RepositoryConnection#getDefaultCachePolicy()
         */
        public CachePolicy getDefaultCachePolicy() {
            return defaultCachePolicy;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connectors.RepositoryConnection#getSourceName()
         */
        public String getSourceName() {
            return SimpleRepositorySource.this.getName();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connectors.RepositoryConnection#getXAResource()
         */
        public XAResource getXAResource() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connectors.RepositoryConnection#ping(long, java.util.concurrent.TimeUnit)
         */
        public boolean ping( long time,
                             TimeUnit unit ) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.connectors.RepositoryConnection#setListener(org.jboss.dna.graph.connectors.RepositorySourceListener)
         */
        public void setListener( RepositorySourceListener listener ) {
            this.listener = listener;
        }

        /**
         * @return listener
         */
        public RepositorySourceListener getListener() {
            return listener;
        }

    }

    protected class Executor extends AbstractCommandExecutor {
        private final SimpleRepository repository;
        private final Name uuidPropertyName;

        protected Executor( SimpleRepository repository,
                            ExecutionContext context,
                            String sourceName ) {
            super(context, sourceName);
            this.repository = repository;
            this.uuidPropertyName = context.getValueFactories().getNameFactory().create(this.repository.getUuidPropertyName());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.GetChildrenCommand)
         */
        @Override
        public void execute( GetChildrenCommand command ) throws RepositorySourceException {
            Path targetPath = command.getPath();
            Map<Name, Property> properties = getProperties(command);
            if (properties == null) return;
            // Iterate through all of the properties, looking for any paths that are children of the path ...
            Map<Path, Map<Name, Property>> data = repository.getData();
            List<Path.Segment> childSegments = new LinkedList<Path.Segment>();
            for (Path path : data.keySet()) {
                if (!path.isRoot() && path.getParent().equals(targetPath)) {
                    childSegments.add(path.getLastSegment());
                }
            }
            // This does not store children order, so sort ...
            Collections.sort(childSegments);
            for (Path.Segment childSegment : childSegments) {
                Map<Name, Property> childProperties = repository.getData().get(targetPath);
                Property uuidProperty = childProperties.get(uuidPropertyName);
                command.addChild(childSegment,
                                 uuidProperty == null ? new BasicSingleValueProperty(uuidPropertyName, UUID.randomUUID()) : uuidProperty);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.GetPropertiesCommand)
         */
        @Override
        public void execute( GetPropertiesCommand command ) throws RepositorySourceException {
            Map<Name, Property> properties = getProperties(command);
            if (properties == null) return;
            for (Property property : properties.values()) {
                if (!property.getName().equals(this.uuidPropertyName)) {
                    command.setProperty(property);
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.CreateNodeCommand)
         */
        @Override
        public void execute( CreateNodeCommand command ) throws RepositorySourceException {
            Path targetPath = command.getPath();
            ExecutionContext context = getExecutionContext();
            repository.create(context, targetPath.getString(context.getNamespaceRegistry()));
            Map<Name, Property> properties = repository.getData().get(targetPath);
            assert properties != null;
            for (Property property : command.getProperties()) {
                if (!property.getName().equals(this.uuidPropertyName)) {
                    properties.put(property.getName(), property);
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.SetPropertiesCommand)
         */
        @Override
        public void execute( SetPropertiesCommand command ) throws RepositorySourceException {
            Map<Name, Property> properties = getProperties(command);
            if (properties == null) return;
            for (Property property : command.getProperties()) {
                if (!property.getName().equals(this.uuidPropertyName)) {
                    properties.put(property.getName(), property);
                }
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.commands.executor.AbstractCommandExecutor#execute(org.jboss.dna.graph.commands.DeleteBranchCommand)
         */
        @Override
        public void execute( DeleteBranchCommand command ) throws RepositorySourceException {
            // Iterate through all of the dataq, looking for any paths that are children of the path ...
            Path targetPath = command.getPath();
            Map<Path, Map<Name, Property>> data = repository.getData();
            for (Path path : data.keySet()) {
                if (!path.isRoot() && path.isAtOrBelow(targetPath)) {
                    data.remove(path);
                }
            }
        }

        protected <T extends ActsOnPath & GraphCommand> Map<Name, Property> getProperties( T command ) {
            Path targetPath = command.getPath();
            Map<Name, Property> properties = repository.getData().get(targetPath);
            if (properties == null) {
                Path ancestor = targetPath.getParent();
                while (ancestor != null) {
                    if (repository.getData().get(targetPath) != null) break;
                    ancestor = ancestor.getParent();
                }
                if (ancestor == null) ancestor = getExecutionContext().getValueFactories().getPathFactory().createRootPath();
                command.setError(new PathNotFoundException(targetPath, ancestor));
                return null;
            }
            return properties;
        }
    }

}
