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
package org.modeshape.graph.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.parsers.SAXParser;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.basic.LocalNamespaceRegistry;
import org.xml.sax.Attributes;
import org.xml.sax.ext.DefaultHandler2;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * A {@link DefaultHandler2} specialization that responds to XML content events by creating the corresponding content in the
 * supplied graph. This implementation ignores DTD entities, XML contents, and other XML processing instructions. If other
 * behavior is required, the appropriate methods can be overridden. (Which is why this class extends <code>DefaultHandler2</code>,
 * which has support for processing all the different parts of XML.
 * <p>
 * This class can be passed to the {@link SAXParser}'s {@link SAXParser#parse(java.io.File, org.xml.sax.helpers.DefaultHandler)
 * parse(..,DefaultHandler)} methods.
 * </p>
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

    private final ExecutionContext context;

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
     * The name of the property that is to be set with the type of the XML element. For example, "jcr:name".
     */
    protected final Name typeAttribute;

    /**
     * The value of the node type property, if the node's name is set with the {@link #nameAttribute}.
     */
    protected final Name typeAttributeValue;

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
     * The stack of prefixes for each namespace, which is used to keep the {@link #namespaceRegistry local namespace registry} in
     * sync with the namespaces in the XML document.
     */
    private final Map<String, LinkedList<String>> prefixStackByUri = new HashMap<String, LinkedList<String>>();

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
     * Character buffer to aggregate nested character data
     * 
     * @see ElementEntry
     */
    private StringBuilder characterDataBuffer = new StringBuilder();

    /**
     * Stack of pending {@link ElementEntry element entries} from the root of the imported content to the current node.
     * 
     * @see ElementEntry
     */
    private final LinkedList<ElementEntry> elementStack = new LinkedList<ElementEntry>();

    /**
     * Create a handler that creates content in the supplied graph
     * 
     * @param destination the destination where the content should be sent.graph in which the content should be placed
     * @param skipRootElement true if the root element of the document should be skipped, or false if the root element should be
     *        converted to the top-level node of the content
     * @param parent the path to the node in the graph under which the content should be placed; if null, the root node is assumed
     * @param textDecoder the text decoder that should be used to decode the XML element names and XML attribute names, prior to
     *        using those values to create names; or null if the default encoder should be used
     * @param nameAttribute the name of the property whose value should be used for the names of the nodes (typically, this is
     *        "jcr:name" or something equivalent); or null if the XML element name should always be used as the node name
     * @param typeAttribute the name of the property that should be set with the type of the XML element, or null if there is no
     *        such property
     * @param typeAttributeValue the value of the type property that should be used if the node has no <code>nameAttribute</code>,
     *        or null if the value should be set to the type of the XML element
     * @param scoping defines how to choose the namespace of attributes that do not have a namespace prefix; if null, the
     *        {@link #DEFAULT_ATTRIBUTE_SCOPING} value is used
     * @throws IllegalArgumentException if the destination reference is null
     */
    public XmlHandler( Destination destination,
                       boolean skipRootElement,
                       Path parent,
                       TextDecoder textDecoder,
                       Name nameAttribute,
                       Name typeAttribute,
                       Name typeAttributeValue,
                       AttributeScoping scoping ) {
        CheckArg.isNotNull(destination, "destination");
        assert destination != null;
        this.destination = destination;
        this.nameAttribute = nameAttribute;
        this.typeAttribute = typeAttribute;
        this.typeAttributeValue = typeAttributeValue;
        this.decoder = textDecoder != null ? textDecoder : DEFAULT_DECODER;
        this.skipFirstElement = skipRootElement;
        this.attributeScoping = scoping != null ? scoping : DEFAULT_ATTRIBUTE_SCOPING;

        // Use the execution context ...
        this.context = destination.getExecutionContext();
        assert this.context != null;

        // Set up a local namespace registry that is kept in sync with the namespaces found in this XML document ...
        NamespaceRegistry namespaceRegistry = new LocalNamespaceRegistry(this.context.getNamespaceRegistry());
        final ExecutionContext localContext = this.context.with(namespaceRegistry);

        // Set up references to frequently-used objects in the context ...
        this.nameFactory = localContext.getValueFactories().getNameFactory();
        this.pathFactory = localContext.getValueFactories().getPathFactory();
        this.propertyFactory = localContext.getPropertyFactory();
        this.namespaceRegistry = localContext.getNamespaceRegistry();
        assert this.nameFactory != null;
        assert this.pathFactory != null;
        assert this.propertyFactory != null;
        assert this.namespaceRegistry != null;

        // Set up the initial path ...
        this.currentPath = parent != null ? parent : this.pathFactory.createRootPath();
        assert this.currentPath != null;
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
        // Add the prefix to the stack ...
        LinkedList<String> prefixStack = this.prefixStackByUri.get(uri);
        if (prefixStack == null) {
            prefixStack = new LinkedList<String>();
            this.prefixStackByUri.put(uri, prefixStack);
        }
        prefixStack.addFirst(prefix);

        // If the namespace is already registered, then we'll have to register it in the context's registry, too.
        if (!namespaceRegistry.isRegisteredNamespaceUri(uri)) {
            // The namespace is not already registered (locally or in the context's registry), so we have to
            // register it with the context's registry (which the local register then inherits).
            NamespaceRegistry contextRegistry = context.getNamespaceRegistry();
            if (contextRegistry.getNamespaceForPrefix(prefix) != null) {
                // The prefix is already bound, so register and generate a unique prefix
                context.getNamespaceRegistry().getPrefixForNamespaceUri(uri, true);
                // Now register locally with the supplied prefix ...
                namespaceRegistry.register(prefix, uri);
            } else {
                context.getNamespaceRegistry().register(prefix, uri);
            }
        } else {
            // It is already registered, but re-register it locally using the supplied prefix ...
            namespaceRegistry.register(prefix, uri);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endPrefixMapping(java.lang.String)
     */
    @Override
    public void endPrefixMapping( String prefix ) {
        assert prefix != null;
        // Get the current URI for this prefix ...
        String uri = namespaceRegistry.getNamespaceForPrefix(prefix);
        assert uri != null;

        // Get the previous prefix from the stack ...
        LinkedList<String> prefixStack = this.prefixStackByUri.get(uri);
        assert prefixStack != null;
        assert !prefixStack.isEmpty();
        String existingPrefix = prefixStack.removeFirst();
        assert prefix.equals(existingPrefix);

        // If there are no previous prefixes, then remove the mapping ...
        if (prefixStack.isEmpty()) {
            namespaceRegistry.unregister(uri);
            prefixStackByUri.remove(uri);
        } else {
            String previous = prefixStack.getFirst();
            namespaceRegistry.register(previous, uri);
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

        ElementEntry element;
        if (elementStack.isEmpty()) {
            element = new ElementEntry(null, currentPath, null);
        } else {
            // Add the parent
            elementStack.peek().addAsNode();
            element = new ElementEntry(elementStack.peek(), currentPath, null);
        }
        elementStack.addFirst(element);

        properties.clear();
        Object typePropertyValue = null;
        // Convert each of the attributes to a property ...
        for (int i = 0, len = attributes.getLength(); i != len; ++i) {
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
                element.setName(nodeName);
                continue;
            }
            if (typePropertyValue == null && attributeName.equals(typeAttribute)) {
                typePropertyValue = nameFactory.create(attributes.getValue(i)); // don't use a decoder
                continue;
            }
            // Create a property for this attribute ...
            element.addProperty(attributeName, attributes.getValue(i));
        }
        // Create the node name if required ...
        if (nodeName == null) {
            // No attribute defines the node name ...
            nodeName = nameFactory.create(uri, localName, decoder);
            element.setName(nodeName);
        } else {
            if (typePropertyValue == null) typePropertyValue = nameFactory.create(uri, localName, decoder);
        }
        if (typeAttribute != null) {
            // A attribute defines the node name. Set the type property, if required
            if (typePropertyValue == null) typePropertyValue = typeAttributeValue;
            if (typePropertyValue != null) {
                element.addProperty(typeAttribute, typePropertyValue);
            }
        }

        // Update the current path ...
        currentPath = element.path();
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

        String s = characterDataBuffer.toString().trim();
        if (s.length() > 0) {
            ElementEntry entry = elementStack.removeFirst();
            if (entry.isPropertyElement()) {
                // This is just a child element that is really a property ...
                entry.addAsPropertySetTo(s);
            } else {
                // This is actually a child node that fits the JCR 'jcr:xmlcharacters' pattern ...
                entry.addProperty(JcrLexicon.XMLCHARACTERS, s);
                entry.submit();
            }
        } else if (!elementStack.isEmpty()) {
            elementStack.removeFirst().submit();
        }
        characterDataBuffer = new StringBuilder();

        // Nothing to do but to change the current path to be the parent ...
        currentPath = currentPath.getParent();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters( char[] ch,
                            int start,
                            int length ) {
        // Have to add this to a buffer as one logical set of character data can cause this method to fire multiple times
        characterDataBuffer.append(ch, start, length);
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
     * By default, this method creates a property by directly using the value as the sole value of the property.
     * </p>
     * 
     * @param propertyName the name of the property; never null
     * @param value the attribute value
     * @return the property; may not be null
     */
    protected Property createProperty( Name propertyName,
                                       Object value ) {
        propertyValues[0] = value;
        return propertyFactory.create(propertyName, propertyValues);
    }

    /**
     * Create a property with the given name and values, obtained from an attribute name and value in the XML content.
     * <p>
     * By default, this method creates a property by directly using the values as the values of the property.
     * </p>
     * 
     * @param propertyName the name of the property; never null
     * @param values the attribute values
     * @return the property; may not be null
     */
    protected Property createProperty( Name propertyName,
                                       Collection<Object> values ) {
        return propertyFactory.create(propertyName, values);
    }

    /**
     * Possible states for an {@link ElementEntry} instance. All element entries start in state {@code TBD} and then transition to
     * one of the terminating states, {@code NODE} or {@code PROPERTY} when {@link ElementEntry#addAsNode()} or
     * {@link ElementEntry#addAsPropertySetTo(Object)} is invoked.
     */
    protected enum ElementEntryState {
        NODE,
        PROPERTY,
        TBD
    }

    /**
     * Element entries hold references to the data of "pending" elements. "Pending" elements are elements which have been
     * encountered through a {@link XmlHandler#startElement(String, String, String, Attributes)} event but have not yet been fully
     * committed to the {@link XmlHandler#destination}.
     * <p>
     * As the current import semantics allow elements with nested character data to be imported as properties, it is not always
     * possible to determine whether the element represents a node or a property from within the {@code startElement} method.
     * Therefore, {@code ElementEntries} are initially created in an {@link ElementEntryState#TBD unknown state} and submitted to
     * the {@code destination} when it can be positively determined that the entry represents a property (if nested character data
     * is encountered) or a node (if a child node is detected or the {@link XmlHandler#endElement(String, String, String)} method
     * is invoked prior to encountering nested character data).
     * </p>
     * <p>
     * As ModeShape does not currently support a way to add a value to an existing property through the Graph API, {@code
     * ElementEntries} also contain a {@link Multimap} of property names to values. The node's properties are aggregated and only
     * submitted to the {@code destination} when the {@link XmlHandler#endElement(String, String, String)} event fires.
     * </p>
     */
    private class ElementEntry {

        private ElementEntry parent;
        // Stored separately since the root node has no parent ElementEntry but does have a path
        private Path pathToParent;
        private Path pathToThisNode;
        private Name name;
        private Multimap<Name, Object> properties;
        private ElementEntryState state;
        private Map<Name, AtomicInteger> childSnsIndexes = new HashMap<Name, AtomicInteger>();

        protected ElementEntry( ElementEntry parent,
                                Path pathToParent,
                                Name name ) {
            this.parent = parent;
            this.pathToParent = pathToParent;
            this.name = name;
            this.state = ElementEntryState.TBD;
            properties = LinkedHashMultimap.create();
        }

        /**
         * Returns whether this element entry looks (at this point) like a property element: it has no properties or just a single
         * "jcr:primaryType" property.
         * 
         * @return true if this looks like a property element, or false otherwise
         */
        protected boolean isPropertyElement() {
            if (state == ElementEntryState.PROPERTY) return true;
            int count = properties.size();
            if (count == 0) return true;
            if (count == 1 && properties.containsKey(JcrLexicon.PRIMARY_TYPE)) return true;
            return false;
        }

        private int getNextSnsForChildNamed( Name childName ) {
            AtomicInteger snsIndex = childSnsIndexes.get(childName);
            if (snsIndex == null) {
                snsIndex = new AtomicInteger(0);
                childSnsIndexes.put(childName, snsIndex);
            }
            return snsIndex.addAndGet(1);
        }

        protected void setName( Name name ) {
            this.name = name;
            int snsIndex = 1;
            if (parent != null) {
                snsIndex = parent.getNextSnsForChildNamed(name);
            }
            pathToThisNode = pathFactory.create(pathToParent, name, snsIndex);
        }

        protected void addProperty( Name propertyName,
                                    Object propertyValue ) {
            assert state != ElementEntryState.PROPERTY;
            properties.put(propertyName, propertyValue);
        }

        protected void addAsNode() {
            assert state != ElementEntryState.PROPERTY;
            if (state == ElementEntryState.NODE) return;

            state = ElementEntryState.NODE;
            destination.create(pathToThisNode, Collections.<Property>emptyList());
        }

        protected void addAsPropertySetTo( Object value ) {
            assert state != ElementEntryState.NODE;
            state = ElementEntryState.PROPERTY;
            parent.addProperty(name, value);
        }

        protected final Path path() {
            return pathToThisNode;
        }

        protected void submit() {
            if (state == ElementEntryState.PROPERTY) return;

            if (state == ElementEntryState.NODE && properties.size() == 0) return;
            Property[] propertiesToAdd = new Property[properties.size()];
            int i = 0;
            for (Name name : properties.keySet()) {
                propertiesToAdd[i++] = createProperty(name, properties.get(name));
            }

            if (state == ElementEntryState.TBD) {
                // Merge the add and the create
                destination.create(pathToThisNode, Arrays.asList(propertiesToAdd));
            } else {
                destination.setProperties(pathToThisNode, propertiesToAdd);
            }
        }
    }
}
