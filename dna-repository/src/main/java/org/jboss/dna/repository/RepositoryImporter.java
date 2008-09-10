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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.monitor.SimpleProgressMonitor;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.repository.sequencers.xml.XmlSequencer;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.connector.RepositoryConnectionFactory;
import org.jboss.dna.spi.connector.RepositorySource;
import org.jboss.dna.spi.connector.RepositorySourceException;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.NamespaceRegistry;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.Property;
import org.jboss.dna.spi.graph.PropertyFactory;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.ValueFactory;
import org.jboss.dna.spi.graph.ValueFormatException;
import org.jboss.dna.spi.graph.commands.CompositeCommand;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.NodeConflictBehavior;
import org.jboss.dna.spi.graph.commands.impl.BasicCreateNodeCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGraphCommand;
import org.jboss.dna.spi.sequencers.SequencerContext;
import org.jboss.dna.spi.sequencers.SequencerOutput;
import org.jboss.dna.spi.sequencers.StreamSequencer;

/**
 * @author Randall Hauch
 */
public class RepositoryImporter {

    private final RepositoryConnectionFactory sources;
    private final String sourceName;
    private final ExecutionContext context;

    public RepositoryImporter( RepositorySource source,
                               ExecutionContext context ) {
        ArgCheck.isNotNull(source, "source");
        ArgCheck.isNotNull(context, "context");
        this.sources = new SingleRepositorySourceConnectionFactory(source);
        this.sourceName = source.getName();
        this.context = context;
    }

    public RepositoryImporter( RepositoryConnectionFactory sources,
                               String sourceName,
                               ExecutionContext context ) {
        ArgCheck.isNotNull(sources, "sources");
        ArgCheck.isNotEmpty(sourceName, "sourceName");
        ArgCheck.isNotNull(context, "context");
        this.sources = sources;
        this.sourceName = sourceName;
        this.context = context;
    }

    /**
     * Get the context in which the importer will be executed.
     * 
     * @return the execution context; never null
     */
    public ExecutionContext getContext() {
        return this.context;
    }

    /**
     * Read the content from the supplied URI and import into the repository at the supplied location.
     * 
     * @param uri the URI where the importer can read the content that is to be imported
     * @param destinationPathInSource
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     * @throws IOException if there is a problem reading the content
     * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource repository
     *         source}
     */
    public void importXml( URI uri,
                           Path destinationPathInSource ) throws IOException, RepositorySourceException {
        ArgCheck.isNotNull(uri, "uri");
        ArgCheck.isNotNull(destinationPathInSource, "destinationPathInSource");

        // Create the sequencer ...
        StreamSequencer sequencer = new XmlSequencer();
        importWithSequencer(sequencer, uri, "text/xml", destinationPathInSource, NodeConflictBehavior.UPDATE);
    }

    /**
     * Use the supplied sequencer to read the content at the given URI (with the specified MIME type) and write that content to
     * the {@link RepositorySource repository source} into the specified location.
     * 
     * @param sequencer the sequencer that should be used; may not be null
     * @param contentUri the URI where the content can be found; may not be null
     * @param mimeType the MIME type for the content; may not be null
     * @param destinationPathInSource the path in the {@link RepositorySource repository source} where the content is to be
     *        written; may not be null
     * @param conflictBehavior the behavior when a node is to be created when an existing node already exists; defaults to
     *        {@link NodeConflictBehavior#UPDATE} if null
     * @throws IOException if there is a problem reading the content
     * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource repository
     *         source}
     */
    protected void importWithSequencer( StreamSequencer sequencer,
                                        URI contentUri,
                                        String mimeType,
                                        Path destinationPathInSource,
                                        NodeConflictBehavior conflictBehavior ) throws IOException, RepositorySourceException {
        assert sequencer != null;
        assert contentUri != null;
        assert mimeType != null;
        assert destinationPathInSource != null;
        conflictBehavior = conflictBehavior != null ? conflictBehavior : NodeConflictBehavior.UPDATE;

        // Get the input path by creating from the URI, in case the URI is a valid path ...
        Path inputPath = extractInputPathFrom(contentUri);
        assert inputPath != null;

        // Now create the importer context ...
        PropertyFactory propertyFactory = getContext().getPropertyFactory();
        NameFactory nameFactory = getContext().getValueFactories().getNameFactory();
        Set<Property> inputProperties = new HashSet<Property>();
        inputProperties.add(propertyFactory.create(nameFactory.create("jcr:mimeType"), mimeType));
        ImporterContext importerContext = new ImporterContext(inputPath, inputProperties, "text/xml");

        // Now run the sequencer ...
        String activity = RepositoryI18n.errorImportingContent.text(destinationPathInSource, contentUri);
        ProgressMonitor progressMonitor = new SimpleProgressMonitor(activity);
        ImporterCommands commands = new ImporterCommands(destinationPathInSource, conflictBehavior);
        InputStream stream = null;
        try {
            stream = contentUri.toURL().openStream();
            sequencer.sequence(stream, commands, importerContext, progressMonitor);
        } catch (MalformedURLException err) {
            throw new IOException(err.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    I18n msg = RepositoryI18n.errorImportingContent;
                    context.getLogger(getClass()).error(e, msg, mimeType, contentUri);
                }
            }
        }

        // Now execute the commands against the repository ...
        RepositoryConnection connection = null;
        try {
            connection = sources.createConnection(this.sourceName);
            connection.execute(context, commands);
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (RepositorySourceException e) {
                    I18n msg = RepositoryI18n.errorImportingContent;
                    context.getLogger(getClass()).error(e, msg, mimeType, contentUri);
                }
            }
        }
    }

    /**
     * @param contentUri
     * @return the input path
     */
    protected Path extractInputPathFrom( URI contentUri ) {
        try {
            return getContext().getValueFactories().getPathFactory().create(contentUri);
        } catch (ValueFormatException e) {
            // Get the last component of the URI, and use it to create the input path ...
            String path = contentUri.getPath();
            return getContext().getValueFactories().getPathFactory().create(path);
        }
    }

    protected class SingleRepositorySourceConnectionFactory implements RepositoryConnectionFactory {
        private final RepositorySource source;

        protected SingleRepositorySourceConnectionFactory( RepositorySource source ) {
            ArgCheck.isNotNull(source, "source");
            this.source = source;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
         */
        public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
            if (source.getName().equals(sourceName)) {
                return source.getConnection();
            }
            return null;
        }
    }

    protected class ImporterCommands extends BasicGraphCommand implements SequencerOutput, CompositeCommand {
        private final List<GraphCommand> commands = new ArrayList<GraphCommand>();
        private final Map<Path, BasicCreateNodeCommand> createNodeCommands = new HashMap<Path, BasicCreateNodeCommand>();
        private final NodeConflictBehavior conflictBehavior;
        private final Path destinationPath;

        protected ImporterCommands( Path destinationPath,
                                    NodeConflictBehavior conflictBehavior ) {
            ArgCheck.isNotNull(destinationPath, "destinationPath");
            ArgCheck.isNotNull(conflictBehavior, "conflictBehavior");
            this.conflictBehavior = conflictBehavior;
            this.destinationPath = destinationPath;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerOutput#getFactories()
         */
        public ValueFactories getFactories() {
            return getContext().getValueFactories();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerOutput#getNamespaceRegistry()
         */
        public NamespaceRegistry getNamespaceRegistry() {
            return getContext().getNamespaceRegistry();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerOutput#setProperty(java.lang.String, java.lang.String, java.lang.Object[])
         */
        public void setProperty( String nodePath,
                                 String propertyName,
                                 Object... values ) {
            // Create a command that sets the property ...
            Path path = getFactories().getPathFactory().create(nodePath);
            Name name = getFactories().getNameFactory().create(propertyName);
            setProperty(path, name, values);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerOutput#setProperty(org.jboss.dna.spi.graph.Path,
         *      org.jboss.dna.spi.graph.Name, java.lang.Object[])
         */
        public void setProperty( Path nodePath,
                                 Name propertyName,
                                 Object... values ) {
            PathFactory pathFactory = getFactories().getPathFactory();
            if (nodePath.isAbsolute()) nodePath.relativeTo(pathFactory.createRootPath());
            nodePath = pathFactory.create(destinationPath, nodePath).getNormalizedPath();
            Property property = getContext().getPropertyFactory().create(propertyName, values);
            BasicCreateNodeCommand command = createNodeCommands.get(nodePath);
            if (command != null) {
                // We've already created the node, so find that command and add to it.
                Collection<Property> properties = command.getProperties();
                // See if the property was already added and remove it if so
                Iterator<Property> iter = properties.iterator();
                while (iter.hasNext()) {
                    Property existingProperty = iter.next();
                    if (existingProperty.getName().equals(propertyName)) {
                        iter.remove();
                        break;
                    }
                }
                command.getProperties().add(property);
            } else {
                // We haven't created the node yet (and we're assuming that we need to), so create the node
                List<Property> properties = new ArrayList<Property>();
                properties.add(property);
                command = new BasicCreateNodeCommand(nodePath, properties, conflictBehavior);
                createNodeCommands.put(nodePath, command);
                commands.add(command);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerOutput#setReference(java.lang.String, java.lang.String, java.lang.String[])
         */
        public void setReference( String nodePath,
                                  String propertyName,
                                  String... paths ) {
            Path path = getFactories().getPathFactory().create(nodePath);
            Name name = getFactories().getNameFactory().create(propertyName);
            // Create an array of reference values ...
            ValueFactory<Reference> factory = getFactories().getReferenceFactory();
            Object[] values = new Object[paths.length];
            int i = 0;
            for (String referencedPath : paths) {
                values[i++] = factory.create(referencedPath);
            }
            setProperty(path, name, values);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<GraphCommand> iterator() {
            return this.commands.iterator();
        }

    }

    protected class ImporterContext implements SequencerContext {

        private final Path inputPath;
        private final Set<Property> inputProperties;
        private final String mimeType;

        protected ImporterContext( Path inputPath,
                                   Set<Property> inputProperties,
                                   String mimeType ) {
            this.inputPath = inputPath;
            this.inputProperties = inputProperties;
            this.mimeType = mimeType;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerContext#getFactories()
         */
        public ValueFactories getFactories() {
            return getContext().getValueFactories();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerContext#getInputPath()
         */
        public Path getInputPath() {
            return inputPath;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerContext#getInputProperties()
         */
        public Set<Property> getInputProperties() {
            return inputProperties;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerContext#getInputProperty(org.jboss.dna.spi.graph.Name)
         */
        public Property getInputProperty( Name name ) {
            for (Property property : inputProperties) {
                if (property.getName().equals(name)) return property;
            }
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerContext#getLogger(java.lang.Class)
         */
        public Logger getLogger( Class<?> clazz ) {
            return getContext().getLogger(clazz);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerContext#getLogger(java.lang.String)
         */
        public Logger getLogger( String name ) {
            return getContext().getLogger(name);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerContext#getMimeType()
         */
        public String getMimeType() {
            return mimeType;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.spi.sequencers.SequencerContext#getNamespaceRegistry()
         */
        public NamespaceRegistry getNamespaceRegistry() {
            return getContext().getNamespaceRegistry();
        }

    }

}
