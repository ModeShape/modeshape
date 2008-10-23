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
package org.jboss.dna.graph.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.monitor.SimpleProgressMonitor;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.NodeConflictBehavior;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.Reference;
import org.jboss.dna.graph.properties.ValueFactories;
import org.jboss.dna.graph.properties.ValueFactory;
import org.jboss.dna.graph.properties.ValueFormatException;
import org.jboss.dna.graph.sequencers.SequencerContext;
import org.jboss.dna.graph.sequencers.SequencerOutput;
import org.jboss.dna.graph.sequencers.StreamSequencer;
import org.jboss.dna.graph.xml.DnaXmlLexicon;
import org.jboss.dna.graph.xml.XmlSequencer;

/**
 * @author Randall Hauch
 */
public class GraphImporter {

    private final Graph graph;

    public GraphImporter( Graph graph ) {
        CheckArg.isNotNull(graph, "graph");
        this.graph = graph;
    }

    /**
     * Get the context in which the importer will be executed.
     * 
     * @return the execution context; never null
     */
    public ExecutionContext getContext() {
        return this.graph.getContext();
    }

    /**
     * The graph that this importer uses.
     * 
     * @return the graph; never null
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * Read the content from the supplied URI and import into the repository at the supplied location.
     * 
     * @param uri the URI where the importer can read the content that is to be imported
     * @param location the location in the {@link RepositorySource repository source} where the content is to be written; may not
     *        be null
     * @return the batch of requests for creating the graph content that represents the imported content
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     * @throws IOException if there is a problem reading the content
     * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource repository
     *         source}
     */
    public Graph.Batch importXml( URI uri,
                                  Location location ) throws IOException, RepositorySourceException {
        CheckArg.isNotNull(uri, "uri");
        CheckArg.isNotNull(location, "location");

        // Create the sequencer ...
        StreamSequencer sequencer = new XmlSequencer();
        return importWithSequencer(sequencer, uri, "text/xml", location, NodeConflictBehavior.UPDATE);
    }

    /**
     * Use the supplied sequencer to read the content at the given URI (with the specified MIME type) and write that content to
     * the {@link RepositorySource repository source} into the specified location.
     * 
     * @param sequencer the sequencer that should be used; may not be null
     * @param contentUri the URI where the content can be found; may not be null
     * @param mimeType the MIME type for the content; may not be null
     * @param location the location in the {@link RepositorySource repository source} where the content is to be written; may not
     *        be null
     * @param conflictBehavior the behavior when a node is to be created when an existing node already exists; defaults to
     *        {@link NodeConflictBehavior#UPDATE} if null
     * @return the batch of requests for creating the graph content that represents the imported content
     * @throws IOException if there is a problem reading the content
     * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource repository
     *         source}
     */
    protected Graph.Batch importWithSequencer( StreamSequencer sequencer,
                                               URI contentUri,
                                               String mimeType,
                                               Location location,
                                               NodeConflictBehavior conflictBehavior )
        throws IOException, RepositorySourceException {
        assert sequencer != null;
        assert contentUri != null;
        assert mimeType != null;
        assert location != null;
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
        String activity = GraphI18n.errorImportingContent.text(location.getPath(), contentUri);
        ProgressMonitor progressMonitor = new SimpleProgressMonitor(activity);
        Graph.Batch batch = getGraph().batch();
        ImporterOutput importedContent = new ImporterOutput(batch, location.getPath());
        InputStream stream = null;
        try {
            stream = contentUri.toURL().openStream();
            sequencer.sequence(stream, importedContent, importerContext, progressMonitor);
        } catch (MalformedURLException err) {
            throw new IOException(err.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    I18n msg = GraphI18n.errorImportingContent;
                    getContext().getLogger(getClass()).error(e, msg, mimeType, contentUri);
                }
            }
        }
        // Finish any leftovers ...
        importedContent.process();

        // Now execute the commands against the repository ...
        return batch;
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

    protected class ImporterOutput implements SequencerOutput {
        private final Graph.Batch batch;
        private Path latestPath;
        private final LinkedList<Property> latestProperties = new LinkedList<Property>();
        private final Path destinationPath;
        private final NameFactory nameFactory;
        private final Name primaryTypeName;

        protected ImporterOutput( Graph.Batch batch,
                                  Path destinationPath ) {
            CheckArg.isNotNull(batch, "batch");
            CheckArg.isNotNull(destinationPath, "destinationPath");
            this.batch = batch;
            this.destinationPath = destinationPath;
            this.nameFactory = getContext().getValueFactories().getNameFactory();
            this.primaryTypeName = this.nameFactory.create("jcr:primaryType");
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerOutput#getFactories()
         */
        public ValueFactories getFactories() {
            return getContext().getValueFactories();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerOutput#getNamespaceRegistry()
         */
        public NamespaceRegistry getNamespaceRegistry() {
            return getContext().getNamespaceRegistry();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerOutput#setProperty(java.lang.String, java.lang.String, java.lang.Object[])
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
         * @see org.jboss.dna.graph.sequencers.SequencerOutput#setProperty(org.jboss.dna.graph.properties.Path,
         *      org.jboss.dna.graph.properties.Name, java.lang.Object[])
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

            // Set the latest information ...
            if (!nodePath.equals(latestPath)) process();
            latestPath = nodePath;
            latestProperties.add(property);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerOutput#setReference(java.lang.String, java.lang.String,
         *      java.lang.String[])
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

        protected void process() {
            if (latestPath != null) {
                if (latestProperties.isEmpty()) {
                    batch.create(latestPath).and();
                } else {
                    Property firstProp = latestProperties.removeFirst();
                    if (latestProperties.size() != 0) {
                        Property[] props = latestProperties.toArray(new Property[latestProperties.size()]);
                        batch.create(latestPath, firstProp, props).and();
                    } else {
                        batch.create(latestPath, firstProp).and();
                    }
                    latestProperties.clear();
                }
            }
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
         * @see org.jboss.dna.graph.sequencers.SequencerContext#getFactories()
         */
        public ValueFactories getFactories() {
            return getContext().getValueFactories();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerContext#getInputPath()
         */
        public Path getInputPath() {
            return inputPath;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerContext#getInputProperties()
         */
        public Set<Property> getInputProperties() {
            return inputProperties;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerContext#getInputProperty(org.jboss.dna.graph.properties.Name)
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
         * @see org.jboss.dna.graph.sequencers.SequencerContext#getLogger(java.lang.Class)
         */
        public Logger getLogger( Class<?> clazz ) {
            return getContext().getLogger(clazz);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerContext#getLogger(java.lang.String)
         */
        public Logger getLogger( String name ) {
            return getContext().getLogger(name);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerContext#getMimeType()
         */
        public String getMimeType() {
            return mimeType;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.sequencers.SequencerContext#getNamespaceRegistry()
         */
        public NamespaceRegistry getNamespaceRegistry() {
            return getContext().getNamespaceRegistry();
        }

    }

}
