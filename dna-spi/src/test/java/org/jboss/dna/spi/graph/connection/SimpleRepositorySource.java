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
package org.jboss.dna.spi.graph.connection;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.naming.Reference;
import javax.transaction.xa.XAResource;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.spi.cache.CachePolicy;
import org.jboss.dna.spi.graph.InvalidPathException;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.commands.CompositeCommand;
import org.jboss.dna.spi.graph.commands.GetChildrenCommand;
import org.jboss.dna.spi.graph.commands.GetPropertiesCommand;
import org.jboss.dna.spi.graph.commands.GraphCommand;

/**
 * A {@link RepositorySource} for a {@link SimpleRepository simple repository}.
 * 
 * @author Randall Hauch
 */
@ThreadSafe
public class SimpleRepositorySource extends AbstractRepositorySource {

    private static final long serialVersionUID = 1L;

    private String repositoryName;
    private String name;

    public SimpleRepositorySource() {
        super();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.spi.graph.connection.RepositoryConnectionFactory#getName()
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
     * @see org.jboss.dna.spi.graph.connection.AbstractRepositorySource#createConnection()
     */
    @Override
    protected synchronized RepositoryConnection createConnection() throws RepositorySourceException {
        String reposName = this.getRepositoryName();
        SimpleRepository repository = SimpleRepository.get(reposName);
        if (repository == null) {
            throw new RepositorySourceException(this.getName(), "Unable to find repository \"" + reposName + "\"");
        }
        return new Connection(repository);
    }

    protected class Connection implements RepositoryConnection {

        private RepositorySourceListener listener;
        private final SimpleRepository repository;

        protected Connection( SimpleRepository repository ) {
            assert repository != null;
            this.repository = repository;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.RepositoryConnection#close()
         */
        public void close() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.RepositoryConnection#execute(org.jboss.dna.spi.graph.connection.ExecutionEnvironment,
         *      org.jboss.dna.spi.graph.commands.GraphCommand[])
         */
        public void execute( ExecutionEnvironment env,
                             GraphCommand... commands ) throws RepositorySourceException {
            assert env != null;
            if (repository.isShutdown()) {
                throw new RepositorySourceException(getName(), "The repository \"" + repository.getRepositoryName()
                                                               + "\" is no longer available");
            }
            for (GraphCommand command : commands) {
                executeCommand(env, command);
            }
        }

        protected void executeCommand( ExecutionEnvironment env,
                                       GraphCommand command ) {
            if (command == null) return;
            if (command instanceof CompositeCommand) {
                CompositeCommand composite = (CompositeCommand)command;
                for (GraphCommand nestedCommand : composite) {
                    executeCommand(env, nestedCommand);
                }
            }
            Map<Path, Map<Name, Property>> data = repository.getData();
            // Only commands we recognize are the following; everything else is ignored ...
            if (command instanceof GetPropertiesCommand) { // super of GetNodeCommand
                GetPropertiesCommand getProperties = (GetPropertiesCommand)command;
                Path targetPath = getProperties.getPath();
                Map<Name, Property> properties = data.get(targetPath);
                if (properties == null) {
                    getProperties.setError(new InvalidPathException("Non-existant node: " + targetPath));
                    return;
                }
                for (Property property : properties.values()) {
                    getProperties.setProperty(property);
                }
            }
            if (command instanceof GetChildrenCommand) { // super of GetNodeCommand
                GetChildrenCommand getChildren = (GetChildrenCommand)command;
                Path targetPath = getChildren.getPath();
                if (data.get(targetPath) == null) {
                    getChildren.setError(new InvalidPathException("Non-existant node: " + targetPath));
                    return;
                }
                // Iterate through all of the properties, looking for any paths that are children of the path ...
                List<Path.Segment> childSegments = new LinkedList<Path.Segment>();
                for (Path path : data.keySet()) {
                    if (path.getAncestor().equals(targetPath)) {
                        childSegments.add(path.getLastSegment());
                    }
                }
                // This does not store children order, so sort ...
                Collections.sort(childSegments);
                getChildren.setChildren(childSegments);
            }

        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.RepositoryConnection#getDefaultCachePolicy()
         */
        public CachePolicy getDefaultCachePolicy() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.RepositoryConnection#getSourceName()
         */
        public String getSourceName() {
            return SimpleRepositorySource.this.getName();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.RepositoryConnection#getXAResource()
         */
        public XAResource getXAResource() {
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.RepositoryConnection#ping(long, java.util.concurrent.TimeUnit)
         */
        public boolean ping( long time,
                             TimeUnit unit ) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.graph.connection.RepositoryConnection#setListener(org.jboss.dna.spi.graph.connection.RepositorySourceListener)
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

}
