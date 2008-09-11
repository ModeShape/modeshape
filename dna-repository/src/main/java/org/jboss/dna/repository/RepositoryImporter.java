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

import java.io.File;
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
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.monitor.SimpleProgressMonitor;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.repository.sequencers.xml.DnaXmlLexicon;
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

    public interface ImportSpecification {
        /**
         * Specify the location where the content is to be imported, and then perform the import. This is equivalent to calling
         * <code>{@link #into(String, Path) into(sourceName,rootPath)}</code>.
         * 
         * @param sourceName the name of the source into which the content is to be imported
         * @throws IllegalArgumentException if the <code>uri</code> or path are null
         * @throws IOException if there is a problem reading the content
         * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource
         *         repository source}
         */
        void into( String sourceName ) throws IOException, RepositorySourceException;

        /**
         * Specify the location where the content is to be imported, and then perform the import.
         * 
         * @param sourceName the name of the source into which the content is to be imported
         * @param pathInSource the path in the {@link RepositorySource repository source} named <code>sourceName</code> where the
         *        content is to be written; may not be null
         * @throws IllegalArgumentException if the <code>uri</code> or path are null
         * @throws IOException if there is a problem reading the content
         * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource
         *         repository source}
         */
        void into( String sourceName,
                   Path pathInSource ) throws IOException, RepositorySourceException;
    }

    @Immutable
    protected abstract class ImportedContentUsingSequencer implements ImportSpecification {
        private final StreamSequencer sequencer;

        protected ImportedContentUsingSequencer( StreamSequencer sequencer ) {
            this.sequencer = sequencer;
        }

        protected StreamSequencer getSequencer() {
            return this.sequencer;
        }

        protected NodeConflictBehavior getConflictBehavior() {
            return NodeConflictBehavior.UPDATE;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.RepositoryImporter.ImportSpecification#into(java.lang.String)
         */
        public void into( String sourceName ) throws IOException, RepositorySourceException {
            Path root = getContext().getValueFactories().getPathFactory().createRootPath();
            into(sourceName, root);
        }
    }

    @Immutable
    protected class UriImportedContent extends ImportedContentUsingSequencer {
        private final URI uri;
        private final String mimeType;

        protected UriImportedContent( StreamSequencer sequencer,
                                      URI uri,
                                      String mimeType ) {
            super(sequencer);
            this.uri = uri;
            this.mimeType = mimeType;
        }

        /**
         * @return mimeType
         */
        public String getMimeType() {
            return mimeType;
        }

        /**
         * @return uri
         */
        public URI getUri() {
            return uri;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.repository.RepositoryImporter.ImportSpecification#into(java.lang.String,
         *      org.jboss.dna.spi.graph.Path)
         */
        public void into( String sourceName,
                          Path pathInSource ) throws IOException, RepositorySourceException {
            ArgCheck.isNotNull(sourceName, "sourceName");
            ArgCheck.isNotNull(pathInSource, "pathInSource");
            importWithSequencer(getSequencer(), uri, mimeType, sourceName, pathInSource, getConflictBehavior());
        }
    }

    private final RepositoryConnectionFactory sources;
    private final ExecutionContext context;

    public RepositoryImporter( RepositoryConnectionFactory sources,
                               ExecutionContext context ) {
        ArgCheck.isNotNull(sources, "sources");
        ArgCheck.isNotNull(context, "context");
        this.sources = sources;
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
     * Import the content from the XML file at the supplied URI, specifying on the returned {@link ImportSpecification} where the
     * content is to be imported.
     * 
     * @param uri the URI where the importer can read the content that is to be imported
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     */
    public ImportSpecification importXml( URI uri ) {
        ArgCheck.isNotNull(uri, "uri");

        // Create the sequencer ...
        StreamSequencer sequencer = new XmlSequencer();
        return new UriImportedContent(sequencer, uri, "text/xml");
    }

    /**
     * Import the content from the XML file at the supplied file location, specifying on the returned {@link ImportSpecification}
     * where the content is to be imported.
     * 
     * @param pathToFile the path to the XML file that should be imported.
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     */
    public ImportSpecification importXml( String pathToFile ) {
        ArgCheck.isNotNull(pathToFile, "pathToFile");
        return importXml(new File(pathToFile).toURI());
    }

    /**
     * Import the content from the supplied XML file, specifying on the returned {@link ImportSpecification} where the content is
     * to be imported.
     * 
     * @param file the XML file that should be imported.
     * @return the object that should be used to specify into which the content is to be imported
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     */
    public ImportSpecification importXml( File file ) {
        ArgCheck.isNotNull(file, "file");
        return importXml(file.toURI());
    }

    /**
     * Read the content from the supplied URI and import into the repository at the supplied location.
     * 
     * @param uri the URI where the importer can read the content that is to be imported
     * @param sourceName the name of the source into which the content is to be imported
     * @param destinationPathInSource the path in the {@link RepositorySource repository source} where the content is to be
     *        written; may not be null
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     * @throws IOException if there is a problem reading the content
     * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource repository
     *         source}
     */
    public void importXml( URI uri,
                           String sourceName,
                           Path destinationPathInSource ) throws IOException, RepositorySourceException {
        ArgCheck.isNotNull(uri, "uri");
        ArgCheck.isNotNull(destinationPathInSource, "destinationPathInSource");

        // Create the sequencer ...
        StreamSequencer sequencer = new XmlSequencer();
        importWithSequencer(sequencer, uri, "text/xml", sourceName, destinationPathInSource, NodeConflictBehavior.UPDATE);
    }

    /**
     * Use the supplied sequencer to read the content at the given URI (with the specified MIME type) and write that content to
     * the {@link RepositorySource repository source} into the specified location.
     * 
     * @param sequencer the sequencer that should be used; may not be null
     * @param contentUri the URI where the content can be found; may not be null
     * @param mimeType the MIME type for the content; may not be null
     * @param sourceName the name of the source into which the content is to be imported
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
                                        String sourceName,
                                        Path destinationPathInSource,
                                        NodeConflictBehavior conflictBehavior ) throws IOException, RepositorySourceException {
        assert sequencer != null;
        assert contentUri != null;
        assert mimeType != null;
        assert sourceName != null;
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
            connection = sources.createConnection(sourceName);
            if (connection == null) {
                I18n msg = RepositoryI18n.unableToFindRepositorySourceWithName;
                throw new RepositorySourceException(msg.text(sourceName));
            }
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
        private final NameFactory nameFactory;
        private final Name primaryTypeName;

        protected ImporterCommands( Path destinationPath,
                                    NodeConflictBehavior conflictBehavior ) {
            ArgCheck.isNotNull(destinationPath, "destinationPath");
            ArgCheck.isNotNull(conflictBehavior, "conflictBehavior");
            this.conflictBehavior = conflictBehavior;
            this.destinationPath = destinationPath;
            this.nameFactory = getContext().getValueFactories().getNameFactory();
            this.primaryTypeName = this.nameFactory.create("jcr:primaryType");
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
            // Ignore the property value if the "jcr:primaryType" is "dnaxml:document" ...
            if (this.primaryTypeName.equals(propertyName) && values.length == 1) {
                Name typeName = this.nameFactory.create(values[0]);
                if (DnaXmlLexicon.DOCUMENT.equals(typeName)) return;
            }
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
