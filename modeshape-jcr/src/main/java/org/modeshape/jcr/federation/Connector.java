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

package org.modeshape.jcr.federation;

import java.io.IOException;
import java.util.List;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * SPI of a generic external connector, representing the interface to an external system integrated with ModeShape. Since it is
 * expected that the documents are well formed (structure-wise), the {@link FederatedDocumentWriter} class should be used.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class Connector {

    /**
     * The logger instance, set via reflection
     */
    private Logger logger;

    /**
     * The name of this connector, set via reflection
     */
    private String name;

    /**
     * The name of the repository that owns this connector, set via reflection
     */
    private String repositoryName;

    /**
     * The execution context, set via reflection
     */
    private ExecutionContext context;

    /**
     * The MIME type detector, set via reflection
     */
    private MimeTypeDetector mimeTypeDetector;

    private boolean initialized = false;

    /**
     * Ever connector is expected to have a no-argument constructor, although the class should never initialize any of the data at
     * this time. Instead, all initialization should be performed in the {@link #initialize} method.
     */
    public Connector() {
    }

    /**
     * Returns the name of the source which this connector interfaces with.
     * 
     * @return a {@code non-null} string.
     */
    public String getSourceName() {
        return name;
    }

    /**
     * Get the name of the repository.
     * 
     * @return the repository name; never null
     */
    public final String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Get the logger for this connector instance. This is available for use in or after the
     * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method.
     * 
     * @return the logger; never null
     */
    protected final Logger getLogger() {
        return logger;
    }

    /**
     * Get the execution context for this connector instance. This is available for use in or after the
     * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method.
     * 
     * @return the context; never null
     */
    protected ExecutionContext getContext() {
        return context;
    }

    /**
     * Get the MIME type detector for this connector instance. This is available for use in or after the
     * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method.
     * 
     * @return the MIME type detector; never null
     */
    protected MimeTypeDetector getMimeTypeDetector() {
        return mimeTypeDetector;
    }

    /**
     * Initialize the connector. This is called automatically by ModeShape once for each Connector instance, and should not be
     * called by the connector. By the time this method is called, ModeShape will hav already set the {@link #context},
     * {@link #logger}, {@link #name}, and {@link #repositoryName} plus any fields that match configuration properties for the
     * connector.
     * <p>
     * By default this method does nothing, so it should be overridden by implementations to do a one-time initialization of any
     * internal components. For example, connectors can use the supplied <code>registry</code> and <code>nodeTypeManager</code>
     * objects to register custom namesapces and node types required by the external content.
     * </p>
     * <p>
     * This is an excellent place for connector to validate the connector-specific fields set by ModeShape via reflection during
     * instantiation.
     * </p>
     * 
     * @param registry the namespace registry that can be used to register custom namespaces; never null
     * @param nodeTypeManager the node type manager that can be used to register custom node types; never null
     * @throws RepositoryException if operations on the {@link NamespaceRegistry} or {@link NodeTypeManager} fail
     * @throws IOException if any stream based operations fail (like importing cnd files)
     */
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        // Subclasses may not necessarily call 'super.initialize(...)', but if they do then we can make this assertion ...
        assert !initialized : "The Connector.initialize(...) method should not be called by subclasses; ModeShape has already (and automatically) initialized the Connector";
    }

    /**
     * Method called by the code calling {@link #initialize} (typically via reflection) to signal that the initialize method is
     * completed. See Sequencers.initialize() for details, and no this method is indeed used.
     */
    @SuppressWarnings( "unused" )
    private void postInitialize() {
        if (!initialized) {
            initialized = true;

            // ------------------------------------------------------------------------------------------------------------
            // Add any code here that needs to run after #initialize(...), which will be overwritten by subclasses
            // ------------------------------------------------------------------------------------------------------------
        }
    }

    /**
     * Shutdown the connector by releasing all resources. This is called automatically by ModeShape when this Connector instance
     * is no longer needed, and should never be called by the connector.
     */
    public abstract void shutdown();

    /**
     * Returns a {@link Document} instance representing the document with a given id. The document should have a "proper"
     * structure for it to be usable by ModeShape.
     * 
     * @param id a {@code non-null} string
     * @return either an {@link Document} instance or {@code null}
     */
    public abstract Document getDocumentById( String id );

    /**
     * Returns the id of an external node located at the given path.
     * 
     * @param path a {@code non-null} string representing an exeternal path.
     * @return either the id of the document or {@code null}
     */
    public abstract String getDocumentId( String path );

    /**
     * Removes the document with the given id.
     * 
     * @param id a {@code non-null} string.
     */
    public abstract void removeDocument( String id );

    /**
     * Checks if a document with the given id exists in the end-source.
     * 
     * @param id a {@code non-null} string.
     * @return {@code true} if such a document exists, {@code false} otherwise.
     */
    public abstract boolean hasDocument( String id );

    /**
     * Stores the given document.
     * 
     * @param document a {@code non-null} {@link org.infinispan.schematic.document.Document} instance.
     */
    public abstract void storeDocument( Document document );

    /**
     * Updates the document with the given id.
     * 
     * @param id a {@code non-null} string representing the id of a document
     * @param document a {@code non-null} {@link org.infinispan.schematic.document.Document} instance.
     */
    public abstract void updateDocument( String id,
                                         Document document );

    /**
     * Utility method that checks whether the field with the supplied name is set.
     * 
     * @param fieldValue the value of the field
     * @param fieldName the name of the field
     * @throws RepositoryException if the field value is null
     */
    protected void checkFieldNotNull( Object fieldValue,
                                      String fieldName ) throws RepositoryException {
        if (fieldValue == null) {
            throw new RepositoryException(JcrI18n.requiredFieldNotSetInConnector.text(getSourceName(), getClass(), fieldName));
        }
    }

    protected DocumentWriter newDocument( String id ) {
        return new FederatedDocumentWriter(context).setId(id);
    }

    protected DocumentWriter newDocument( Document document ) {
        return new FederatedDocumentWriter(context, document);
    }

    protected DocumentReader readDocument(Document document) {
        return new FederatedDocumentReader(document);
    }

    /**
     * Get the set of value factory objects that the connector can use to create property value objects.
     * 
     * @return the collection of factories; never null
     */
    protected final ValueFactories factories() {
        return context.getValueFactories();
    }

    /**
     * Helper method that creates a {@link Name} object from a string, using no decoding. This is equivalent to calling "
     * <code>factories().getNameFactory().create(nameString)</code>", and is simply provided for convenience.
     * 
     * @param nameString the string from which the name is to be created
     * @return the value, or null if the supplied string is null
     * @throws ValueFormatException if the conversion from a string could not be performed
     * @see NameFactory#create(String, TextDecoder)
     * @see NameFactory#create(String, String, TextDecoder)
     * @see NameFactory#create(String, String)
     * @see #nameFrom(String, String)
     * @see #nameFrom(String, String, TextDecoder)
     */
    protected final Name nameFrom( String nameString ) {
        return factories().getNameFactory().create(nameString);
    }

    /**
     * Create a name from the given namespace URI and local name. This is equivalent to calling "
     * <code>factories().getNameFactory().create(namespaceUri,localName)</code>", and is simply provided for convenience.
     * 
     * @param namespaceUri the namespace URI
     * @param localName the local name
     * @return the new name
     * @throws IllegalArgumentException if the local name is <code>null</code> or empty
     * @see NameFactory#create(String, TextDecoder)
     * @see NameFactory#create(String, String, TextDecoder)
     * @see NameFactory#create(String, String)
     * @see #nameFrom(String)
     * @see #nameFrom(String, String, TextDecoder)
     */
    protected final Name nameFrom( String namespaceUri,
                                   String localName ) {
        return factories().getNameFactory().create(namespaceUri, localName);
    }

    /**
     * Create a name from the given namespace URI and local name. This is equivalent to calling "
     * <code>factories().getNameFactory().create(namespaceUri,localName,decoder)</code>", and is simply provided for convenience.
     * 
     * @param namespaceUri the namespace URI
     * @param localName the local name
     * @param decoder the decoder that should be used to decode the qualified name
     * @return the new name
     * @throws IllegalArgumentException if the local name is <code>null</code> or empty
     * @see NameFactory#create(String, String, TextDecoder)
     * @see NameFactory#create(String, TextDecoder)
     * @see NameFactory#create(String, String, TextDecoder)
     * @see NameFactory#create(String, String)
     * @see #nameFrom(String)
     * @see #nameFrom(String, String)
     */
    protected final Name nameFrom( String namespaceUri,
                                   String localName,
                                   TextDecoder decoder ) {
        return factories().getNameFactory().create(namespaceUri, localName, decoder);
    }

    public static interface DocumentWriter {
        public DocumentWriter setId( String id );

        public DocumentWriter addProperty( String name,
                                            Object value );

        public DocumentWriter addProperty( Name name,
                                            Object value );

        public DocumentWriter addChild( String id,
                                         String name );

        public DocumentWriter addChild( String id,
                                         Name name );

        public DocumentWriter setParents( String...parentIds );

        public DocumentWriter setParents( List<String> parentIds );

        public DocumentWriter merge( Document document );

        public EditableDocument document();

        FederatedDocumentWriter addChild( EditableDocument child );

        FederatedDocumentWriter setChildren( List<Document> children );
    }

    public static interface DocumentReader {
        public String getDocumentId();

        public List<String> getParentIds();

        public String getName();

        List<EditableDocument> getChildren();

        Document document();
    }
}
