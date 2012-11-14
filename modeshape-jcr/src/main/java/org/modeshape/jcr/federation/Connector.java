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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.cache.DocumentAlreadyExistsException;
import org.modeshape.jcr.cache.DocumentNotFoundException;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.federation.paging.PagingWriter;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
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
     * The name of this connector, set via reflection immediately after instantiation.
     */
    private String name;

    /**
     * The name of the repository that owns this connector, set via reflection immediately after instantiation.
     */
    private String repositoryName;

    /**
     * The execution context, set via reflection before ModeShape calls {@link #initialize(NamespaceRegistry, NodeTypeManager)}.
     */
    private ExecutionContext context;

    /**
     * The MIME type detector, set via reflection before ModeShape calls {@link #initialize(NamespaceRegistry, NodeTypeManager)}.
     */
    private MimeTypeDetector mimeTypeDetector;

    /**
     * The default maximum number of seconds that a document returned by this connector should be stored in the workspace cache.
     * This can be overwritten, on a per-document-basis.
     * <p>
     * The field is assigned via reflection based upon the configuration of the external source represented by this connector
     * before ModeShape calls {@link #initialize(NamespaceRegistry, NodeTypeManager)}.
     * </p>
     */
    private Integer cacheTtlSeconds;

    private boolean initialized = false;

    /**
     * A document translator that is used within the DocumentReader implementation, but which has no DocumentStore reference and
     * thus is not fully-functional.
     * <p>
     * The field is assigned via reflection before ModeShape calls {@link #initialize(NamespaceRegistry, NodeTypeManager)}.
     * </p>
     */
    private DocumentTranslator translator;

    /**
     * A property store that the connector can use to persist "extra" properties that cannot be stored in the external system. The
     * use of this store is optional, and connectors should store as much information as possible in the external system.
     * Connectors are also responsible for removing the extra properties for a node when it is removed.
     * <p>
     * The field is assigned via reflection before ModeShape calls {@link #initialize(NamespaceRegistry, NodeTypeManager)}.
     * </p>
     */
    private ExtraPropertiesStore extraPropertiesStore;

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
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * Get the MIME type detector for this connector instance. This is available for use in or after the
     * {@link #initialize(NamespaceRegistry, NodeTypeManager)} method.
     * 
     * @return the MIME type detector; never null
     */
    public MimeTypeDetector getMimeTypeDetector() {
        return mimeTypeDetector;
    }

    /**
     * Returns the default value, for this connector, of the maximum number of seconds an external document should be stored in
     * the workspace cache.
     * 
     * @return an {@link Integer} value. If {@code null}, it means that no special value is configured and the default workspace
     *         cache configuration will be used. If negative, it means an entry will be cached forever.
     */
    public Integer getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    protected ExtraProperties extraPropertiesFor( String id,
                                                  boolean update ) {
        return new ExtraProperties(id, update);
    }

    /**
     * Get the "extra" properties store. Connectors can directly use this, although it's probably easier to
     * {@link #extraPropertiesFor(String,boolean) create} an {@link ExtraProperties} object for each node and use it to
     * {@link ExtraProperties#add(Property) add}, {@link ExtraProperties#remove(Name) remove} and then
     * {@link ExtraProperties#save() store or update} the extra properties in the extra properties store.
     * 
     * @return the storage for extra properties; never null
     */
    protected ExtraPropertiesStore extraPropertiesStore() {
        return extraPropertiesStore;
    }

    /**
     * Method that can be called by a connector during {@link #initialize(NamespaceRegistry, NodeTypeManager) initialization} if
     * it wants to provide its own implementation of an "extra" properties store.
     * 
     * @param customExtraPropertiesStore the custom implementation of the ExtraPropertiesStore; may not be null
     */
    protected void setExtraPropertiesStore( ExtraPropertiesStore customExtraPropertiesStore ) {
        CheckArg.isNotNull(customExtraPropertiesStore, "customExtraPropertiesStore");
        this.extraPropertiesStore = customExtraPropertiesStore;
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
     * @return true if the document was removed, or false if there was no document with the given id
     */
    public abstract boolean removeDocument( String id );

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
     * @throws DocumentAlreadyExistsException if there is already a new document with the same identifier
     * @throws DocumentNotFoundException if one of the modified documents was removed by another session
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

    protected DocumentTranslator translator() {
        return translator;
    }

    protected DocumentWriter newDocument( String id ) {
        return new FederatedDocumentWriter(translator).setId(id);
    }

    protected DocumentWriter newDocument( Document document ) {
        return new FederatedDocumentWriter(translator, document);
    }

    protected PagingWriter newPagedDocument( Document document ) {
        return new PagingWriter(translator, document);
    }

    protected PagingWriter newPagedDocument() {
        return new PagingWriter(translator);
    }

    protected DocumentReader readDocument( Document document ) {
        return new FederatedDocumentReader(translator, document);
    }

    /**
     * Get the set of value factory objects that the connector can use to create property value objects.
     * 
     * @return the collection of factories; never null
     */
    protected final ValueFactories factories() {
        return context.getValueFactories();
    }

    protected final PropertyFactory propertyFactory() {
        return context.getPropertyFactory();
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

        public DocumentWriter addProperty( String name,
                                           Object[] values );

        public DocumentWriter addProperty( Name name,
                                           Object[] values );

        public DocumentWriter addProperty( String name,
                                           Object firstValue,
                                           Object... additionalValues );

        public DocumentWriter addProperty( Name name,
                                           Object firstValue,
                                           Object... additionalValues );

        public DocumentWriter addProperties( Map<Name, Property> properties );

        public DocumentWriter addChild( String id,
                                        String name );

        public DocumentWriter addChild( String id,
                                        Name name );

        public DocumentWriter addChild( EditableDocument child );

        public DocumentWriter setParents( String... parentIds );

        public DocumentWriter setParent( String parentId );

        public DocumentWriter setParents( List<String> parentIds );

        public DocumentWriter merge( Document document );

        public EditableDocument document();

        FederatedDocumentWriter setChildren( List<? extends Document> children );

        FederatedDocumentWriter setCacheTtlSeconds( int seconds );
    }

    public static interface DocumentReader {
        public String getDocumentId();

        public List<String> getParentIds();

        List<EditableDocument> getChildren();

        Document document();

        Integer getCacheTtlSeconds();

        Name getPrimaryType();

        String getPrimaryTypeName();

        Set<Name> getMixinTypes();

        Set<String> getMixinTypeNames();

        Property getProperty( Name name );

        Property getProperty( String name );

        Map<Name, Property> getProperties();
    }

    public final class ExtraProperties {
        private Map<Name, Property> properties = new HashMap<Name, Property>();
        private final boolean update;
        private final String id;

        protected ExtraProperties( String id,
                                   boolean update ) {
            this.id = id;
            this.update = update;
        }

        public ExtraProperties add( Property property ) {
            this.properties.put(property.getName(), property);
            return this;
        }

        public ExtraProperties addAll( Map<Name, Property> properties ) {
            this.properties.putAll(properties);
            return this;
        }

        public ExtraProperties remove( Name propertyName ) {
            this.properties.put(propertyName, null);
            return this;
        }

        public ExtraProperties remove( String propertyName ) {
            this.properties.put(nameFrom(propertyName), null);
            return this;
        }

        public ExtraProperties except( Name... names ) {
            for (Name name : names) {
                this.properties.remove(name);
            }
            return this;
        }

        public ExtraProperties except( String... names ) {
            for (String name : names) {
                this.properties.remove(nameFrom(name));
            }
            return this;
        }

        public ExtraProperties exceptPrimaryType() {
            this.properties.remove(JcrLexicon.PRIMARY_TYPE);
            return this;
        }

        public void save() {
            if (update) {
                extraPropertiesStore().updateProperties(id, properties);
            } else {
                extraPropertiesStore().storeProperties(id, properties);
            }
            properties.clear();
        }
    }
}
