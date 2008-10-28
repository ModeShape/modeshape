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
package org.jboss.dna.graph.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.SAXParser;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.common.text.XmlNameEncoder;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ext.DefaultHandler2;

/**
 * A {@link DefaultHandler2} specialization that responds to XML content events by creating the corresponding content in the
 * supplied graph. This implementation ignores DTD entities, XML contents, and other XML processing instructions. If other
 * behavior is required, the appropriate methods can be overridden. (Which is why this class extends <code>DefaultHandler2</code>,
 * which has support for processing all the different parts of XML.
 * <p>
 * This class can be passed to the {@link SAXParser}'s {@link SAXParser#parse(java.io.File, org.xml.sax.helpers.DefaultHandler)
 * parse(..,DefaultHandler)} methods.
 * </p>
 * 
 * @author Randall Hauch
 */
@NotThreadSafe
public class XmlHandler extends DefaultHandler2 {

    /**
     * The choices for how attributes that have no namespace prefix should be assigned a namespace.
     * 
     * @author Randall Hauch
     */
    public enum AttributeScoping {
        /** The attribute's namespace is the default namespace */
        USE_DEFAULT_NAMESPACE,
        /** The attribute's namespace is the same namespace as the containing element */
        INHERIT_ELEMENT_NAMESPACE;
    }

    /**
     * Decoder for XML names, to turn '_xHHHH_' sequences in the XML element and attribute names into the corresponding UTF-16
     * characters.
     */
    public static TextDecoder DEFAULT_DECODER = new XmlNameEncoder();

    /**
     * The default {@link AttributeScoping}.
     */
    public static AttributeScoping DEFAULT_ATTRIBUTE_SCOPING = AttributeScoping.USE_DEFAULT_NAMESPACE;

    /**
     * The destination where the content should be sent.
     */
    protected final Destination destination;

    /**
     * The name of the XML attribute whose value should be used for the name of the node. For example, "jcr:name".
     */
    protected final Name nameAttribute;

    /**
     * The cached reference to the graph's path factory.
     */
    protected final PathFactory pathFactory;

    /**
     * The cached reference to the graph's name factory.
     */
    protected final NameFactory nameFactory;

    /**
     * The cached reference to the graph's property factory.
     */
    protected final PropertyFactory propertyFactory;

    /**
     * The cached reference to the graph's namespace registry.
     */
    protected final NamespaceRegistry namespaceRegistry;

    /**
     * The TextDecoder that is used to decode the names.
     */
    protected final TextDecoder decoder;

    /**
     * Local set of the namespace URIs that are registered. This is an optimization, rather than relying upon the (thread-safe)
     * {@link #namespaceRegistry}.
     */
    private final Set<String> namespaceUris = new HashSet<String>();

    private final AttributeScoping attributeScoping;

    /**
     * The path for the node representing the current element. This starts out as the path supplied by the constructor, and never
     * is shorter than that initial path.
     */
    protected Path currentPath;

    /**
     * Flag the records whether the first element should be skipped.
     */
    protected boolean skipFirstElement;

    /**
     * A temporary list used to store the properties for a single node. This is cleared, populated, then used to create the node.
     */
    protected final List<Property> properties = new ArrayList<Property>();

    /**
     * A working array that contains a single value object that is used to create Property objects (without having to create an
     * array of values for each property).
     */
    protected final Object[] propertyValues = new Object[1];

    /**
     * Create a handler that creates content in the supplied graph
     * 
     * @param destination the destination where the content should be sent.graph in which the content should be placed
     * @param skipRootElement true if the root element of the document should be skipped, or false if the root element should be
     *        converted to the top-level node of the content
     * @param parent the path to the node in the graph under which the content should be placed; if null, the root node is assumed
     * @param textDecoder the text decoder that should be used to decode the XML element names and XML attribute names, prior to
     *        using those values to create names; or null if the default encoder should be used
     * @param nameAttribute the name of the XML attribute whose value should be used for the names of the nodes (typically, this
     *        is "jcr:name" or something equivalent); or null if the XML element name should always be used as the node name
     * @param scoping defines how to choose the namespace of attributes that do not have a namespace prefix; if null, the
     *        {@link #DEFAULT_ATTRIBUTE_SCOPING} value is used
     * @throws IllegalArgumentException if the destination reference is null
     */
    public XmlHandler( Destination destination,
                       boolean skipRootElement,
                       Path parent,
                       TextDecoder textDecoder,
                       Name nameAttribute,
                       AttributeScoping scoping ) {
        CheckArg.isNotNull(destination, "destination");
        assert destination != null;
        this.destination = destination;
        this.nameAttribute = nameAttribute;
        this.decoder = textDecoder != null ? textDecoder : DEFAULT_DECODER;
        this.skipFirstElement = skipRootElement;
        this.attributeScoping = scoping != null ? scoping : DEFAULT_ATTRIBUTE_SCOPING;

        // Set up references to frequently-used objects in the context ...
        final ExecutionContext context = destination.getExecutionContext();
        assert context != null;
        this.nameFactory = context.getValueFactories().getNameFactory();
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.propertyFactory = context.getPropertyFactory();
        this.namespaceRegistry = context.getNamespaceRegistry();
        assert this.nameFactory != null;
        assert this.pathFactory != null;
        assert this.propertyFactory != null;
        assert this.namespaceRegistry != null;

        // Set up the initial path ...
        this.currentPath = parent != null ? parent : this.pathFactory.createRootPath();
        assert this.currentPath != null;
    }

    /**
     * Create a handler that creates content in the supplied graph
     * 
     * @param graph the graph in which the content should be placed
     * @param useBatch true if all of the actions to create the content in the graph should be submitted to the graph in a single
     *        batch, or false if they should be submitted immediately after each is identified
     * @param skipRootElement true if the root element of the document should be skipped, or false if the root element should be
     *        converted to the top-level node of the content
     * @param parent the path to the node in the graph under which the content should be placed; if null, the root node is assumed
     * @param textDecoder the text decoder that should be used to decode the XML element names and XML attribute names, prior to
     *        using those values to create names; or null if the default encoder should be used
     * @param nameAttribute the name of the XML attribute whose value should be used for the names of the nodes (typically, this
     *        is "jcr:name" or something equivalent); or null if the XML element name should always be used as the node name
     * @param scoping defines how to choose the namespace of attributes that do not have a namespace prefix; if null, the
     *        {@link #DEFAULT_ATTRIBUTE_SCOPING} value is used
     * @throws IllegalArgumentException if the graph reference is null
     */
    public XmlHandler( Graph graph,
                       boolean useBatch,
                       boolean skipRootElement,
                       Path parent,
                       TextDecoder textDecoder,
                       Name nameAttribute,
                       AttributeScoping scoping ) {
        this(createDestination(graph, useBatch), skipRootElement, parent, textDecoder, nameAttribute, scoping);
    }

    protected static Destination createDestination( Graph graph,
                                                    boolean useBatch ) {
        CheckArg.isNotNull(graph, "graph");
        return useBatch ? new CreateOnGraphInBatches(graph.batch()) : new CreateOnGraph(graph);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method ensures that the namespace is registered with the {@link NamespaceRegistry registry}, using the supplied prefix
     * to register the namespace if required. Note that because this class does not really use the namespace prefixes to create
     * {@link Name} objects, no attempt is made to match the XML namespace prefixes.
     * </p>
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    @Override
    public void startPrefixMapping( String prefix,
                                    String uri ) {
        assert uri != null;
        if (namespaceUris.add(uri)) {
            // This is a new namespace for this document ...
            if (!namespaceRegistry.isRegisteredNamespaceUri(uri)) {
                if (prefix != null && prefix.length() == 0) prefix = null;
                namespaceRegistry.register(prefix, uri);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
     *      org.xml.sax.Attributes)
     */
    @Override
    public void startElement( String uri,
                              String localName,
                              String name,
                              Attributes attributes ) {
        // Should this (root) element be skipped?
        if (skipFirstElement) {
            skipFirstElement = false;
            return;
        }
        assert localName != null;
        Name nodeName = null;

        properties.clear();
        // Convert each of the attributes to a property ...
        for (int i = 0; i != attributes.getLength(); ++i) {
            String attributeLocalName = attributes.getLocalName(i);
            String attributeUri = attributes.getURI(i);
            Name attributeName = null;
            if ((attributeUri == null || attributeUri.length() == 0) && attributes.getQName(i).indexOf(':') == -1) {
                switch (this.attributeScoping) {
                    case INHERIT_ELEMENT_NAMESPACE:
                        attributeName = nameFactory.create(uri, attributeLocalName, decoder);
                        break;
                    case USE_DEFAULT_NAMESPACE:
                        attributeName = nameFactory.create(attributeLocalName, decoder);
                        break;
                }
            } else {
                attributeName = nameFactory.create(attributeUri, attributeLocalName, decoder);
            }
            assert attributeName != null;
            // Check to see if this is an attribute that represents the node name (which may be null) ...
            if (nodeName == null && attributeName.equals(nameAttribute)) {
                nodeName = nameFactory.create(attributes.getValue(i)); // don't use a decoder
                continue;
            }
            // Create a property for this attribute ...
            Property property = createProperty(attributeName, attributes.getValue(i));
            properties.add(property);
        }
        // Create the node name if required ...
        if (nodeName == null) nodeName = nameFactory.create(uri, localName, decoder);
        // Update the current path ...
        currentPath = pathFactory.create(currentPath, nodeName);
        // Create the node, and note that we don't care about same-name siblings (as the graph will correct them) ...
        destination.create(currentPath, properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement( String uri,
                            String localName,
                            String name ) {
        // Nothing to do but to change the current path to be the parent ...
        currentPath = currentPath.getParent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() {
        // Submit any outstanding requests (if there are any) ...
        destination.submit();
    }

    /**
     * Create a property with the given name and value, obtained from an attribute name and value in the XML content.
     * <p>
     * By default, this method creates a property by directly using the value as the sole String value of the property.
     * </p>
     * 
     * @param propertyName the name of the property; never null
     * @param value the attribute value
     * @return the property; may not be null
     */
    protected Property createProperty( Name propertyName,
                                       String value ) {
        propertyValues[0] = value;
        return propertyFactory.create(propertyName, propertyValues);
    }

    /**
     * Interface used internally as the destination for the requests. This is used to abstract whether the requests should be
     * submitted immediately or in a single batch.
     * 
     * @author Randall Hauch
     */
    @NotThreadSafe
    public static interface Destination {

        /**
         * Obtain the execution context of the destination.
         * 
         * @return the destination's execution context
         */
        public ExecutionContext getExecutionContext();

        /**
         * Create a node at the supplied path and with the supplied attributes. The path will be absolute.
         * 
         * @param path the absolute path of the node
         * @param properties the properties for the node; never null, but may be empty if there are no properties
         */
        public void create( Path path,
                            List<Property> properties );

        /**
         * Signal to this destination that any enqueued create requests should be submitted. Usually this happens at the end of
         * the document parsing, but an implementer must allow for it to be called multiple times and anytime during parsing.
         */
        public void submit();
    }

    @NotThreadSafe
    protected final static class CreateOnGraph implements Destination {
        private final Graph graph;

        protected CreateOnGraph( final Graph graph ) {
            assert graph != null;
            this.graph = graph;
        }

        public ExecutionContext getExecutionContext() {
            return graph.getContext();
        }

        public final void create( Path path,
                                  List<Property> properties ) {
            assert properties != null;
            if (properties.isEmpty()) {
                graph.create(path);
            } else {
                graph.create(path, properties);
            }
        }

        public void submit() {
            // Nothing to do, since each call to 'create' immediate executes on the graph
        }
    }

    @NotThreadSafe
    protected final static class CreateOnGraphInBatches implements Destination {
        private final Graph.Batch batch;

        protected CreateOnGraphInBatches( Graph.Batch batch ) {
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
                batch.create(path);
            } else {
                batch.create(path, properties);
            }
        }

        public void submit() {
            batch.execute();
        }
    }

}
