/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.xml.XmlHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * An importer of graph content. This class can be used directly, or the import can be done via the {@link Graph} using the
 * {@link Graph#importXmlFrom(java.io.File)} (and similar) methods.
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
        InputStream stream = null;
        try {
            stream = uri.toURL().openStream();
            return importXml(stream, location, skip);
        } finally {
            if (stream != null) stream.close();
        }
    }

    /**
     * Read the content from the supplied URI and import into the repository at the supplied location. This method does <i>not</i>
     * close the stream.
     * 
     * @param stream the stream containing the content to be imported
     * @param location the location in the {@link RepositorySource repository source} where the content is to be written; may not
     *        be null
     * @return the batch of requests for creating the graph content that represents the imported content
     * @throws IllegalArgumentException if the <code>stream</code> or destination path are null
     * @throws IOException if there is a problem reading the content
     * @throws SAXException if there is a problem with the SAX Parser
     * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource repository
     *         source}
     */
    public Graph.Batch importXml( InputStream stream,
                                  Location location ) throws IOException, SAXException, RepositorySourceException {
        return importXml(stream, location, false);
    }

    /**
     * Read the content from the supplied URI and import into the repository at the supplied location. This method does <i>not</i>
     * close the stream.
     * 
     * @param stream the stream containing the content to be imported
     * @param location the location in the {@link RepositorySource repository source} where the content is to be written; may not
     *        be null
     * @param skip true if the root element should be skipped, or false if a node should be created for the root XML element
     * @return the batch of requests for creating the graph content that represents the imported content
     * @throws IllegalArgumentException if the <code>stream</code> or destination path are null
     * @throws IOException if there is a problem reading the content
     * @throws SAXException if there is a problem with the SAX Parser
     * @throws RepositorySourceException if there is a problem while writing the content to the {@link RepositorySource repository
     *         source}
     */
    public Graph.Batch importXml( InputStream stream,
                                  Location location,
                                  boolean skip ) throws IOException, SAXException, RepositorySourceException {
        CheckArg.isNotNull(stream, "uri");
        CheckArg.isNotNull(location, "location");
        CheckArg.isNotNull(location.getPath(), "location.getPath()");

        // Create the destination for the XmlHandler ...
        Graph.Batch batch = graph.batch();
        Destination destination = new GraphBatchDestination(batch, true);

        // Determine where the content is to be placed ...
        Path parentPath = location.getPath();
        Name nameAttribute = getNameAttribute();
        Name typeAttribute = getTypeAttribute();
        Name typeAttributeValue = null;
        NamespaceRegistry reg = graph.getContext().getNamespaceRegistry();
        if (reg.isRegisteredNamespaceUri(JcrNtLexicon.Namespace.URI)) {
            typeAttributeValue = JcrNtLexicon.UNSTRUCTURED;
        }

        TextDecoder decoder = null;
        XmlHandler.AttributeScoping scoping = XmlHandler.AttributeScoping.USE_DEFAULT_NAMESPACE;
        XmlHandler handler = new XmlHandler(destination, skip, parentPath, decoder, nameAttribute, typeAttribute,
                                            typeAttributeValue, scoping);
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);
        reader.parse(new InputSource(stream));
        if (stream != null) stream.close();
        return batch;
    }

    /**
     * Return an {@link XmlHandler} that can be used to import content directly into the supplied location. The operations
     * resulting from the {@link XmlHandler} operations are batched until the {@link XmlHandler#endDocument()} is called, at which
     * point all enqueued operations are submitted to the graph.
     * 
     * @param location the location in the {@link RepositorySource repository source} where the content is to be written; may not
     *        be null
     * @param skip true if the root element should be skipped, or false if a node should be created for the root XML element
     * @return the {@link XmlHandler} that can be used to import content
     * @throws IllegalArgumentException if the <code>stream</code> or destination path are null
     */
    public XmlHandler getHandlerForImportingXml( Location location,
                                                 boolean skip ) {
        CheckArg.isNotNull(location, "location");
        CheckArg.isNotNull(location.getPath(), "location.getPath()");

        // Create the destination for the XmlHandler ...
        Graph.Batch batch = graph.batch();
        Destination destination = new GraphBatchDestination(batch, false);

        // Determine where the content is to be placed ...
        Path parentPath = location.getPath();
        Name nameAttribute = getNameAttribute();
        Name typeAttribute = getTypeAttribute();
        Name typeAttributeValue = null;
        NamespaceRegistry reg = graph.getContext().getNamespaceRegistry();
        if (reg.isRegisteredNamespaceUri(JcrNtLexicon.Namespace.URI)) {
            typeAttributeValue = JcrNtLexicon.UNSTRUCTURED;
        }

        TextDecoder decoder = null;
        XmlHandler.AttributeScoping scoping = XmlHandler.AttributeScoping.USE_DEFAULT_NAMESPACE;
        XmlHandler handler = new XmlHandler(destination, skip, parentPath, decoder, nameAttribute, typeAttribute,
                                            typeAttributeValue, scoping);
        return handler;
    }

    protected Name getNameAttribute() {
        return JcrLexicon.NAME;
    }

    protected Name getTypeAttribute() {
        return JcrLexicon.PRIMARY_TYPE;
    }

}
