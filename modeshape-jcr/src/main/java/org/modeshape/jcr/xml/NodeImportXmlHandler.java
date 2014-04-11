/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.jcr.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.LinkedHashMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.basic.LocalNamespaceRegistry;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * A simplified version of the graph xml import handler (from ModeShape 2.x) which is used for importing initial content into
 * workspaces.
 * 
 * @author Randall Hauch
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@NotThreadSafe
public class NodeImportXmlHandler extends DefaultHandler2 {

    /**
     * The choices for how attributes that have no namespace prefix should be assigned a namespace.
     * 
     * @author Randall Hauch
     */
    public enum AttributeScoping {
        /**
         * The attribute's namespace is the default namespace
         */
        USE_DEFAULT_NAMESPACE,
        /**
         * The attribute's namespace is the same namespace as the containing element
         */
        INHERIT_ELEMENT_NAMESPACE
    }

    /**
     * The default multi-valued delimiter string.
     */
    protected static final String DEFAULT_MULTI_VALUE_SEPARATOR = ",";

    /**
     * Decoder for XML names, to turn '_xHHHH_' sequences in the XML element and attribute names into the corresponding UTF-16
     * characters.
     */
    private static final TextDecoder XML_DECODER = new XmlNameEncoder();

    /**
     * The default {@link AttributeScoping}.
     */
    private static final AttributeScoping DEFAULT_ATTRIBUTE_SCOPING = AttributeScoping.USE_DEFAULT_NAMESPACE;

    /**
     * The mandatory name of the xml root element
     */
    private static final String ROOT_ELEMENT_NAME = JcrConstants.JCR_ROOT;

    /**
     * The name of the XML attribute which defines a custom multi value separator.
     */
    private static final String MULTI_VALUE_SEPARATOR_ATTRIBUTE = "multi-value-separator";

    /**
     * The name of the XML attribute whose value should be used for the name of the node. For example, "jcr:name".
     */
    private final String nameAttribute;

    /**
     * The name of the property that is to be set with the type of the XML element. For example, "jcr:primaryType".
     */
    private final String typeAttribute;

    /**
     * The name of the XML attribute which represents the type of a property. By default, this is "type".
     */
    private final String propertyTypeAttribute;

    /**
     * The value of the node type property, if the node's name is set with the {@link #nameAttribute}.
     */
    private final String typeAttributeValue;

    /**
     * The reference to the {@link org.modeshape.jcr.value.NameFactory}
     */
    private final NameFactory nameFactory;

    /**
     * The reference to the {@link org.modeshape.jcr.value.PathFactory}
     */
    protected final PathFactory pathFactory;

    /**
     * The cached reference to the graph's namespace registry.
     */
    private final NamespaceRegistry namespaceRegistry;

    /**
     * The attribute scoping indicating how the namespaces are resolved for elements
     */
    private final AttributeScoping attributeScoping;

    /**
     * The stack of prefixes for each namespace, which is used to keep the {@link #namespaceRegistry local namespace registry} in
     * sync with the namespaces in the XML document.
     */
    private final Map<String, LinkedList<String>> prefixStackByUri = new HashMap<>();

    /**
     * The import destination.
     */
    private final NodeImportDestination destination;

    /**
     * Character buffer to aggregate nested character data
     */
    private final StringBuilder characterDataBuffer = new StringBuilder();

    /**
     * The separator string used for determining multi-valued properties
     */
    protected String multiValueSeparator;

    private final Stack<ImportElement> elementsStack = new Stack<>();
    private final List<ImportElement> parsedElements = new ArrayList<>();

    private LinkedHashMap<Path, ImportElement> parsedElementsByPath;
    private boolean validateRootElement;

    /**
     * Creates a new handler instance, using only an execution context and some default values.
     * 
     * @param destination a non-null {@link NodeImportDestination}
     */
    public NodeImportXmlHandler( NodeImportDestination destination ) {
        this(destination, JcrConstants.JCR_NAME, JcrConstants.JCR_PRIMARY_TYPE, JcrConstants.NT_UNSTRUCTURED,
             DEFAULT_MULTI_VALUE_SEPARATOR, DEFAULT_ATTRIBUTE_SCOPING);
    }

    /**
     * Create a handler that parses an xml file.
     * 
     * @param destination a non-null {@link org.modeshape.jcr.xml.NodeImportDestination} which is expected to provide a valid
     *        context and to handle the results of the import process.
     * @param nameAttribute the name of the property whose value should be used for the names of the nodes (typically, this is
     *        "jcr:name" or something equivalent); or null if the XML element name should always be used as the node name
     * @param typeAttribute the name of the property that should be set with the type of the XML element, or null if there is no
     *        such property
     * @param typeAttributeValue the value of the type property that should be used if the node has no <code>nameAttribute</code>,
     *        or null if the value should be set to the type of the XML element
     * @param multiValueSeparator the string that should be used a separator for creating multi-valued properties.
     * @param scoping defines how to choose the namespace of attributes that do not have a namespace prefix; if null, the
     *        {@link #DEFAULT_ATTRIBUTE_SCOPING} value is used @throws IllegalArgumentException if the destination reference is
     *        null
     */
    public NodeImportXmlHandler( NodeImportDestination destination,
                                 String nameAttribute,
                                 String typeAttribute,
                                 String typeAttributeValue,
                                 String multiValueSeparator,
                                 AttributeScoping scoping ) {
        this.nameAttribute = nameAttribute;
        this.typeAttribute = typeAttribute;
        this.typeAttributeValue = typeAttributeValue;
        this.attributeScoping = scoping != null ? scoping : DEFAULT_ATTRIBUTE_SCOPING;
        this.destination = destination;
        this.multiValueSeparator = !StringUtil.isBlank(multiValueSeparator) ? multiValueSeparator : DEFAULT_MULTI_VALUE_SEPARATOR;

        // Set up a local namespace registry that is kept in sync with the namespaces found in this XML document ...
        ExecutionContext context = destination.getExecutionContext();
        NamespaceRegistry namespaceRegistry = new LocalNamespaceRegistry(context.getNamespaceRegistry());
        final ExecutionContext localContext = context.with(namespaceRegistry);

        // Set up references to frequently-used objects in the context ...
        this.nameFactory = localContext.getValueFactories().getNameFactory();
        this.pathFactory = localContext.getValueFactories().getPathFactory();
        this.namespaceRegistry = localContext.getNamespaceRegistry();

        this.propertyTypeAttribute = createName(null, "type");

        assert this.nameFactory != null;
        assert this.namespaceRegistry != null;
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
            prefixStack = new LinkedList<>();
            this.prefixStackByUri.put(uri, prefixStack);
        }
        prefixStack.addFirst(prefix);

        // If the namespace is already registered, then we'll have to register it in the context's registry, too.
        if (!namespaceRegistry.isRegisteredNamespaceUri(uri)) {
            ExecutionContext destinationContext = destination.getExecutionContext();
            // The namespace is not already registered (locally or in the context's registry), so we have to
            // register it with the context's registry (which the local register then inherits).
            NamespaceRegistry contextRegistry = destinationContext.getNamespaceRegistry();
            if (contextRegistry.getNamespaceForPrefix(prefix) != null) {
                // The prefix is already bound, so register and generate a unique prefix
                destinationContext.getNamespaceRegistry().getPrefixForNamespaceUri(uri, true);
                // Now register locally with the supplied prefix ...
                namespaceRegistry.register(prefix, uri);
            } else {
                destinationContext.getNamespaceRegistry().register(prefix, uri);
            }
        } else {
            // It is already registered, but re-register it locally using the supplied prefix ...
            namespaceRegistry.register(prefix, uri);
        }
    }

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

    @Override
    public void startElement( String uri,
                              String localName,
                              String name,
                              Attributes attributes ) throws SAXException {
        // the root element should only be validated and not taken into account
        if (validateRootElement) {
            if (!name.equalsIgnoreCase(ROOT_ELEMENT_NAME)) {
                throw new SAXException(JcrI18n.errorDuringInitialImport.text("Root xml element must be " + ROOT_ELEMENT_NAME));
            }
            parseCustomSettings(attributes);

            validateRootElement = false;
            return;
        }

        assert localName != null;
        String nodeName = null;

        ImportElement parent = elementsStack.isEmpty() ? null : elementsStack.peek();
        ImportElement element = new ImportElement(parent);
        elementsStack.push(element);

        String typePropertyValue = null;
        // Convert each of the attributes to a property ...
        for (int i = 0, len = attributes.getLength(); i != len; ++i) {
            String attributeLocalName = attributes.getLocalName(i);
            String attributeUri = attributes.getURI(i);
            String attributeName = null;
            if ((attributeUri == null || attributeUri.length() == 0) && attributes.getQName(i).indexOf(':') == -1) {
                switch (this.attributeScoping) {
                    case INHERIT_ELEMENT_NAMESPACE:
                        attributeName = createName(uri, attributeLocalName);
                        break;
                    case USE_DEFAULT_NAMESPACE:
                        attributeName = createName(null, attributeLocalName);
                        break;
                }
            } else {
                attributeName = createName(attributeUri, attributeLocalName);
            }
            assert attributeName != null;
            // Check to see if this is an attribute that represents the node name (which may be null) ...
            if (nodeName == null && attributeName.equalsIgnoreCase(nameAttribute)) {
                nodeName = createName(null, attributes.getValue(i));
                element.setName(nodeName);
                continue;
            }
            if (typePropertyValue == null && attributeName.equalsIgnoreCase(typeAttribute)) {
                typePropertyValue = createName(null, attributes.getValue(i)); // don't use a decoder
                element.setType(typePropertyValue);
                continue;
            }
            // Create a property for this attribute ...
            element.addProperty(attributeName, attributes.getValue(i), org.modeshape.jcr.value.PropertyType.STRING);
        }

        // Create the default node name if no explicit name has been configured
        if (nodeName == null) {
            // No attribute defines the node name ...
            nodeName = createName(uri, localName);
            element.setName(nodeName);
        }

        // Create the default node type if no explicit type has been configured
        if (typePropertyValue == null) {
            if (typeAttributeValue != null) {
                // there is a preconfigured type to use, so use that
                element.setType(typeAttributeValue);
            } else {
                // there is no default value for the type, so use nt:unstructured
                element.setType(JcrConstants.NT_UNSTRUCTURED);
            }
        }
    }

    private void parseCustomSettings( Attributes attributes ) {
        // parse any custom attributes of the root element, which would indicate some custom settings
        for (int i = 0; i < attributes.getLength(); i++) {
            String attributeLocalName = attributes.getLocalName(i);
            if (attributeLocalName.equalsIgnoreCase(MULTI_VALUE_SEPARATOR_ATTRIBUTE)) {
                this.multiValueSeparator = attributes.getValue(i);
            }
        }
    }

    protected String createName( String uri,
                                 String localName ) {
        return !StringUtil.isBlank(uri) ? nameFactory.create(uri, localName, XML_DECODER).getString(NoOpEncoder.getInstance()) : nameFactory.create(localName,
                                                                                                                                                    XML_DECODER)
                                                                                                                                            .getString(NoOpEncoder.getInstance());
    }

    @Override
    public void endElement( String uri,
                            String localName,
                            String name ) {
        if (name.equalsIgnoreCase(ROOT_ELEMENT_NAME)) {
            return;
        }
        ImportElement entry = elementsStack.pop();

        String s = characterDataBuffer.toString().trim();
        if (s.length() > 0) {
            // there is char data
            if (entry.looksLikeProperty()) {
                // This is just a child element that is really a property ...
                entry.setPropertyOnParent(s);
            } else {
                // This is actually a child node that fits the JCR 'jcr:xmlcharacters' pattern ...
                entry.addProperty(JcrLexicon.XMLCHARACTERS.toString(), s, org.modeshape.jcr.value.PropertyType.STRING);
                parsedElements.add(entry);
            }
        } else {
            parsedElements.add(entry);
        }

        characterDataBuffer.delete(0, characterDataBuffer.length());
    }

    @Override
    public void characters( char[] ch,
                            int start,
                            int length ) {
        if (validateRootElement) {
            return;
        }
        // Have to add this to a buffer as one logical set of character data can cause this method to fire multiple times
        characterDataBuffer.append(ch, start, length);
    }

    @SuppressWarnings( "unused" )
    @Override
    public void startDocument() throws SAXException {
        this.validateRootElement = true;
    }

    @Override
    public void endDocument() throws SAXException {
        this.validateRootElement = false;
        try {
            this.destination.submit(getParsedElementByPath());
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
        this.parsedElements.clear();
    }

    private LinkedHashMap<Path, ImportElement> getParsedElementByPath() {
        if (parsedElementsByPath == null) {
            parsedElementsByPath = new LinkedHashMap<>();
            int depthLevel = 1;
            // convert the list of parsed elements to a map making sure that the parents come before the children and the children
            // order is preserved
            while (!parsedElements.isEmpty()) {
                for (Iterator<ImportElement> elementIterator = parsedElements.iterator(); elementIterator.hasNext();) {
                    ImportElement element = elementIterator.next();
                    if (element.getPath().size() == depthLevel) {
                        parsedElementsByPath.put(element.getPath(), element);
                        elementIterator.remove();
                    }
                }
                ++depthLevel;
            }
        }
        return parsedElementsByPath;
    }

    /**
     * Element entries represent in-memory representations of the xml elements (either nodes or properties) encountered between a
     * {@link NodeImportXmlHandler#startElement(String, String, String, Attributes)} and a
     * {@link NodeImportXmlHandler#endElement(String, String, String)} event.
     */
    @SuppressWarnings( "synthetic-access" )
    public class ImportElement {

        private final Multimap<String, String> properties = LinkedHashMultimap.create();
        private final Map<String, org.modeshape.jcr.value.PropertyType> propertyTypes = new HashMap<>();
        private final List<String> mixins = new ArrayList<>();
        private final Map<String, AtomicInteger> childSnsIndexes = new HashMap<>();

        private String name;
        private String type;
        private Path path;
        private ImportElement parent;
        private String propertyType;

        protected ImportElement( ImportElement parent ) {
            this.parent = parent;
        }

        /**
         * Returns whether this element entry looks (at this point) like a property element: either it has no properties or has
         * just one {@link org.modeshape.jcr.xml.NodeImportXmlHandler#propertyTypeAttribute} type attribute.
         * 
         * @return true if this looks like a property element, or false otherwise
         */
        protected final boolean looksLikeProperty() {
            return properties.size() == 0 || (properties.size() == 1 && properties.containsKey(propertyTypeAttribute));
        }

        private int getNextSnsForChildNamed( String childName ) {
            AtomicInteger snsIndex = childSnsIndexes.get(childName);
            if (snsIndex == null) {
                childSnsIndexes.put(childName, new AtomicInteger(0));
            }
            return childSnsIndexes.get(childName).incrementAndGet();
        }

        protected void setName( String name ) {
            this.name = name;
            int snsIndex = 1;
            if (parent != null) {
                snsIndex = parent.getNextSnsForChildNamed(name);
                this.path = pathFactory.create(parent.getPath(), name, snsIndex);
            } else {
                this.path = pathFactory.create("/" + name);
            }
        }

        protected void setType( String type ) {
            this.type = type;
        }

        protected void addProperty( String propertyName,
                                    String propertyValue,
                                    org.modeshape.jcr.value.PropertyType jcrPropertyType ) {
            if (propertyName.equalsIgnoreCase(propertyTypeAttribute)) {
                propertyType = propertyValue;
            }
            String[] values = propertyValue.split(multiValueSeparator);
            for (String value : values) {
                if (jcrPropertyType != org.modeshape.jcr.value.PropertyType.STRING) {
                    // for everything but strings we trim the value
                    value = value.trim();
                }
                if (propertyName.equals(JcrConstants.JCR_MIXIN_TYPES)) {
                    mixins.add(createName(null, value.trim()));
                } else {
                    properties.put(propertyName, value);
                }
            }
            propertyTypes.put(propertyName, jcrPropertyType);
        }

        protected void setPropertyOnParent( String value ) {
            parent.addPropertyWithType(name, value, propertyType);
        }

        private void addPropertyWithType( String propertyName,
                                          String propertyValue,
                                          String propertyType ) {

            if (propertyType == null) {
                // there is no specified type, so add it as a simple string
                propertyType = "string";
            }
            switch (propertyType.toLowerCase()) {
                case "simplereference": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.SIMPLEREFERENCE);
                    break;
                }
                case "weakreference": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.WEAKREFERENCE);
                    break;
                }
                case "reference": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.REFERENCE);
                    break;
                }
                case "long": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.LONG);
                    break;
                }
                case "double": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.DOUBLE);
                    break;
                }
                case "date": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.DATE);
                    break;
                }
                case "decimal": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.DECIMAL);
                    break;
                }
                case "boolean": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.BOOLEAN);
                    break;
                }
                case "name": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.NAME);
                    break;
                }
                case "path": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.PATH);
                    break;
                }
                case "uri": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.URI);
                    break;
                }
                case "binary": {
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.BINARY);
                    break;
                }
                default: {
                    // by default we treat it a string
                    addProperty(propertyName, propertyValue, org.modeshape.jcr.value.PropertyType.STRING);
                }
            }
        }

        /**
         * Returns the name of the import element, which should translate to the name of a jcr node.
         * 
         * @return a non-null {@link String}
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the name of the import element, which should translate to the type of a jcr node.
         * 
         * @return a non-null {@link String}
         */
        public String getType() {
            return type;
        }

        /**
         * Returns the list of mixins of the import element, which should translate to the mixins of a jcr node.
         * 
         * @return a non-null {@link List}
         */

        public List<String> getMixins() {
            return mixins;
        }

        /**
         * Returns the imported element's properties.
         * 
         * @return a non-null {@link Multimap}
         */
        public Multimap<String, String> getProperties() {
            return properties;
        }

        /**
         * Returns the JCR property type of the property with the given name.
         * 
         * @param propertyName a {@link String} the name of a property; never null
         * @return either a {@link org.modeshape.jcr.value.PropertyType} representing the JCR type of the property or {@code null}
         *         if the no property with the given name was parsed.
         */
        public org.modeshape.jcr.value.PropertyType getPropertyType( String propertyName ) {
            return propertyTypes.get(propertyName);
        }

        /**
         * Returns the path of this import element, which translates to the path of the jcr node.
         * 
         * @return a non-null {@link Path}
         */
        public Path getPath() {
            return path;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("ImportElement");
            sb.append("{name='").append(name).append('\'');
            sb.append(", path=").append(path);
            sb.append(", type='").append(type).append('\'');
            if (!properties.isEmpty()) {
                sb.append(", properties=").append(properties);
                sb.append(", types=").append(propertyTypes);
            }
            if (!mixins.isEmpty()) {
                sb.append(", mixins=").append(mixins);
            }
            sb.append('}');
            return sb.toString();
        }
    }
}
