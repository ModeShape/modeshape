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
package org.jboss.dna.graph;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.connectors.RepositorySourceException;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.xml.XmlHandler;
import org.jboss.dna.graph.xml.XmlHandler.Destination;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author Randall Hauch
 * @author John Verhaeg
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
     * @throws SAXException if there is a problem with the SAX Parser
     * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource repository
     *         source}
     */
    public Graph.Batch importXml( URI uri,
                                  Location location ) throws IOException, SAXException, RepositorySourceException {
        return importXml(uri, location, false);
    }

    /**
     * Read the content from the supplied URI and import into the repository at the supplied location.
     * 
     * @param uri the URI where the importer can read the content that is to be imported
     * @param location the location in the {@link RepositorySource repository source} where the content is to be written; may not
     *        be null
     * @param skip true if the root element should be skipped, or false if a node should be created for the root XML element
     * @return the batch of requests for creating the graph content that represents the imported content
     * @throws IllegalArgumentException if the <code>uri</code> or destination path are null
     * @throws IOException if there is a problem reading the content
     * @throws SAXException if there is a problem with the SAX Parser
     * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource repository
     *         source}
     */
    public Graph.Batch importXml( URI uri,
                                  Location location,
                                  boolean skip ) throws IOException, SAXException, RepositorySourceException {
        CheckArg.isNotNull(uri, "uri");
        CheckArg.isNotNull(location, "location");
        CheckArg.isNotNull(location.getPath(), "location.getPath()");

        // Create the destination for the XmlHandler ...
        Graph.Batch batch = graph.batch();
        XmlHandler.Destination destination = new CreateOnGraphInBatch(batch);

        // Determine where the content is to be placed ...
        Path parentPath = location.getPath();
        InputStream stream = null;
        Name nameAttribute = JcrLexicon.NAME;
        Name typeAttribute = JcrLexicon.PRIMARY_TYPE;
        Name typeAttributeValue = null;
        NamespaceRegistry reg = graph.getContext().getNamespaceRegistry();
        if (reg.isRegisteredNamespaceUri(JcrNtLexicon.Namespace.URI)) {
            typeAttributeValue = JcrNtLexicon.UNSTRUCTURED;
        }

        TextDecoder decoder = null;
        XmlHandler.AttributeScoping scoping = XmlHandler.AttributeScoping.USE_DEFAULT_NAMESPACE;
        XmlHandler handler = new XmlHandler(destination, skip, parentPath, decoder, nameAttribute, typeAttribute,
                                            typeAttributeValue, scoping);
        try {
            stream = uri.toURL().openStream();
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(new InputSource(stream));
        } finally {
            if (stream != null) stream.close();
        }
        return batch;
    }

    @NotThreadSafe
    protected final static class CreateOnGraphInBatch implements Destination {
        private final Graph.Batch batch;

        protected CreateOnGraphInBatch( Graph.Batch batch ) {
            assert batch != null;
            this.batch = batch;
        }

        public ExecutionContext getExecutionContext() {
            return batch.getGraph().getContext();
        }

        public void create( Path path,
                            List<Property> properties ) {
            assert properties != null;
            if (properties.isEmpty()) {
                batch.create(path).and();
            } else {
                batch.create(path, properties).and();
            }
        }

        public void create( Path path,
                            Property firstProperty,
                            Property... additionalProperties ) {
            if (firstProperty == null) {
                batch.create(path).and();
            } else {
                batch.create(path, firstProperty, additionalProperties);
            }
        }

        public void submit() {
        }
    }

}
