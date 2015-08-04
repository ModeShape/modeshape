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
package org.modeshape.jcr;

import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.Base64;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Implementation of {@link AbstractJcrExporter} that implements the document view mapping described in section 6.4.2 of the JCR
 * 1.0 specification.
 * 
 * @see JcrSession#exportDocumentView(String, ContentHandler, boolean, boolean)
 * @see JcrSession#exportDocumentView(String, java.io.OutputStream, boolean, boolean)
 */
@NotThreadSafe
class JcrDocumentViewExporter extends AbstractJcrExporter {

    /** The name encoder needs to encode spaces plus the standard slash characters */
    static final TextEncoder NAME_ENCODER = new JcrDocumentViewExporter.JcrDocumentViewPropertyEncoder(' ', '\r', '\n', '\t');
    static final TextEncoder VALUE_ENCODER = NAME_ENCODER;
    static final TextDecoder NAME_DECODER = (TextDecoder)NAME_ENCODER;
    static final TextDecoder VALUE_DECODER = (TextDecoder)NAME_ENCODER;

    private final ValueFactory<String> stringFactory;

    JcrDocumentViewExporter( JcrSession session ) {
        super(session, Collections.<String>emptyList());
        stringFactory = session.stringFactory();
    }

    /**
     * Exports <code>node</code> (or the subtree rooted at <code>node</code>) into an XML document by invoking SAX events on
     * <code>contentHandler</code>.
     * 
     * @param node the node which should be exported. If <code>noRecursion</code> was set to <code>false</code> in the
     *        constructor, the entire subtree rooted at <code>node</code> will be exported.
     * @param contentHandler the SAX content handler for which SAX events will be invoked as the XML document is created.
     * @param skipBinary if <code>true</code>, indicates that binary properties should not be exported
     * @param noRecurse if<code>true</code>, indicates that only the given node should be exported, otherwise a recursive export
     *        and not any of its child nodes.
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    @Override
    public void exportNode( Node node,
                            ContentHandler contentHandler,
                            boolean skipBinary,
                            boolean noRecurse ) throws RepositoryException, SAXException {
        ExecutionContext executionContext = session.context();

        JcrSharedNode sharedNode = asSharedNode(node);
        if (sharedNode != null) {
            // This is a shared node, and per Section 14.7 of the JCR 2.0 specification, they have to be written out
            // in a special way ...
            AttributesImpl atts = new AttributesImpl();

            // jcr:primaryType = nt:share ...
            addAttribute(atts, JcrLexicon.PRIMARY_TYPE, PropertyType.NAME, JcrNtLexicon.SHARE);

            // jcr:uuid = UUID of shared node ...
            addAttribute(atts, JcrLexicon.UUID, PropertyType.STRING, node.getIdentifier());

            // Write out the element ...
            Name name = sharedNode.segment().getName();
            startElement(contentHandler, name, atts);
            endElement(contentHandler, name);
            return;
        }
        exporting(node);

        // If this node is a special xmltext node, output it as raw content (see JCR 1.0 spec - section 6.4.2.3)
        if (node.getDepth() > 0 && isXmlTextNode(node)) {
            String xmlCharacters = getXmlCharacters(node);
            contentHandler.characters(xmlCharacters.toCharArray(), 0, xmlCharacters.length());
            return;
        }

        // Build the attributes for this node's element, but add the primary type first ...
        AttributesImpl atts = new AttributesImpl();
        Property primaryType = ((AbstractJcrNode)node).getProperty(JcrLexicon.PRIMARY_TYPE);
        if (primaryType != null) {
            addAttribute(atts, primaryType, skipBinary, false);
        }

        // And add the remaining properties next ...
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property prop = properties.nextProperty();
            if (prop == primaryType) continue;
            addAttribute(atts, prop, skipBinary, true);
        }

        // Special case to stub in name for root node as per JCR 1.0 Spec - 6.4.2.2
        Name name = null;
        ValueFactories valueFactories = executionContext.getValueFactories();
        if (node.getDepth() == 0) {
            name = JcrLexicon.ROOT;
        } else {
            name = valueFactories.getNameFactory().create(node.getName());
        }

        // Write out the element ...
        startElement(contentHandler, name, atts);
        if (!noRecurse) {
            //the node iterator should check permissions and return only those nodes on which there is READ permission
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                Node child = nodes.nextNode();
                //MODE-2171 Ignore any ACL nodes
                if (!child.isNodeType(ModeShapeLexicon.ACCESS_LIST_NODE_TYPE_STRING)) {
                    exportNode(child, contentHandler, skipBinary, noRecurse);
                }
            }
        }
        endElement(contentHandler, name);
    }

    protected void addAttribute( AttributesImpl atts,
                                 Name propertyName,
                                 int propertyType,
                                 Object value ) {
        String valueAsString = VALUE_ENCODER.encode(stringFactory.create(value));
        String localPropName = getPrefixedName(propertyName);
        atts.addAttribute(propertyName.getNamespaceUri(),
                          propertyName.getLocalName(),
                          localPropName,
                          org.modeshape.jcr.api.PropertyType.nameFromValue(propertyType),
                          valueAsString);

    }

    protected void addAttribute( AttributesImpl atts,
                                 Property prop,
                                 boolean skipBinary,
                                 boolean skipPrimaryType ) throws RepositoryException {

        Name propName = ((AbstractJcrProperty)prop).name();
        if (skipPrimaryType && JcrLexicon.PRIMARY_TYPE.equals(propName)) return;

        String localPropName = getPrefixedName(propName);

        if (skipBinary && PropertyType.BINARY == prop.getType()) {
            atts.addAttribute(propName.getNamespaceUri(),
                              propName.getLocalName(),
                              localPropName,
                              org.modeshape.jcr.api.PropertyType.nameFromValue(prop.getType()),
                              "");
            return;
        }

        
        String valueAsString = "";
        if (prop instanceof JcrSingleValueProperty) {
            valueAsString = jcrValueToString(prop.getValue(), prop.getType());
        } else {
            Value[] values = prop.getValues();
            if (values.length > 0) {
                StringBuilder multiValuedString = new StringBuilder(valueAsString);
                for (Iterator<Value> valuesIterator = Arrays.asList(values).iterator(); valuesIterator.hasNext();) {
                    String valueString = jcrValueToString(valuesIterator.next(), prop.getType());
                    multiValuedString.append(valueString);
                    if (valuesIterator.hasNext()) {
                        multiValuedString.append(" ");
                    }
                }
                valueAsString = multiValuedString.toString();
            }
        }

        atts.addAttribute(propName.getNamespaceUri(),
                          NAME_ENCODER.encode(propName.getLocalName()),
                          NAME_ENCODER.encode(localPropName),
                          org.modeshape.jcr.api.PropertyType.nameFromValue(prop.getType()),
                          valueAsString);
    }

    private String jcrValueToString( Value value, int propertyType ) throws RepositoryException {
        if (value == null) {
            return "";
        }

        if (PropertyType.BINARY == propertyType) {
            try {
                Base64.InputStream is = new Base64.InputStream(value.getBinary().getStream(), Base64.ENCODE);
                return IoUtil.read(is);
            } catch (IOException ioe) {
                throw new RepositoryException(ioe);
            }
        } else {
            return VALUE_ENCODER.encode(value.getString());
        }
    }   

    /**
     * Indicates whether the current node is an XML text node as per section 6.4.2.3 of the JCR 1.0 specification. XML text nodes
     * are nodes that have the name &quot;jcr:xmltext&quot; and only one property (besides the mandatory
     * &quot;jcr:primaryType&quot;). The property must have a property name of &quot;jcr:xmlcharacters&quot;, a type of
     * <code>String</code>, and does not have multiple values.
     * <p/>
     * In practice, this is handled in ModeShape by making XML text nodes have a type of &quot;dna:xmltext&quot;, which enforces
     * these property characteristics.
     * 
     * @param node the node to test
     * @return whether this node is a special xml text node
     * @throws RepositoryException if there is an error accessing the repository
     */
    private boolean isXmlTextNode( Node node ) throws RepositoryException {
        // ./xmltext/xmlcharacters exception (see JSR-170 Spec 6.4.2.3)

        if (getPrefixedName(JcrLexicon.XMLTEXT).equals(node.getName())) {
            if (node.getNodes().getSize() == 0) {

                PropertyIterator properties = node.getProperties();
                boolean xmlCharactersFound = false;

                while (properties.hasNext()) {
                    Property property = properties.nextProperty();

                    if (getPrefixedName(JcrLexicon.PRIMARY_TYPE).equals(property.getName())) {
                        continue;
                    }

                    if (getPrefixedName(JcrLexicon.XMLCHARACTERS).equals(property.getName())) {
                        xmlCharactersFound = true;
                        continue;
                    }

                    // If the xmltext node has any properties other than primaryType or xmlcharacters, return false;
                    return false;
                }

                return xmlCharactersFound;
            }
        }

        return false;

    }

    /**
     * Returns the XML characters for the given node. The node must be an XML text node, as defined in
     * {@link #isXmlTextNode(Node)}.
     * 
     * @param node the node for which XML characters will be retrieved.
     * @return the xml characters for this node
     * @throws RepositoryException if there is an error accessing this node
     */
    private String getXmlCharacters( Node node ) throws RepositoryException {
        // ./xmltext/xmlcharacters exception (see JSR-170 Spec 6.4.2.3)

        assert isXmlTextNode(node);

        Property xmlCharacters = node.getProperty(getPrefixedName(JcrLexicon.XMLCHARACTERS));

        assert xmlCharacters != null;

        if (xmlCharacters.getDefinition().isMultiple()) {
            StringBuilder bf = new StringBuilder();
            for (Value value : xmlCharacters.getValues()) {
                bf.append(value.getString());
            }
            return bf.toString();
        }

        return xmlCharacters.getValue().getString();
    }

    /**
     * Special {@link TextEncoder} that implements the subset of XML name encoding suggested by section 6.4.4 of the JCR 1.0.1
     * specification. This encoder only encodes space (0x20), carriage return (0x0D), new line (0x0A), tab (0x09), and any
     * underscore characters that might otherwise suggest an encoding, as defined in {@link XmlNameEncoder}.
     */
    protected static class JcrDocumentViewPropertyEncoder extends XmlNameEncoder {

        private final Set<Character> mappedCharacters;

        protected JcrDocumentViewPropertyEncoder( char... chars ) {
            mappedCharacters = new HashSet<Character>();
            for (char c : chars) {
                mappedCharacters.add(c);
            }
        }

        // See section 6.4.4 of the JCR 1.0.1 spec for why these hoops must be jumped through
        @Override
        public String encode( String text ) {
            if (text == null) return null;
            if (text.length() == 0) return text;
            StringBuilder sb = new StringBuilder();
            String hex = null;
            CharacterIterator iter = new StringCharacterIterator(text);
            char first = iter.first();
            boolean isDigit = Character.isDigit(first);
            for (char c = first; c != CharacterIterator.DONE; c = iter.next()) {
                if (c != first && isDigit && !Character.isDigit(c)) {
                    isDigit = false;
                }

                if (c == '_') {
                    // Read the next character (if there is one) ...
                    char next = iter.next();
                    if (next == CharacterIterator.DONE) {
                        sb.append(c);
                        break;
                    }
                    // If the next character is not 'x', then these are just regular characters ...
                    if (next != 'x') {
                        sb.append(c).append(next);
                        continue;
                    }
                    // The next character is 'x', so write out the '_' character in encoded form ...
                    sb.append("_x005f_");
                    // And then write out the next character ...
                    sb.append(next);
                } else if (!mappedCharacters.contains(c) && !isDigit) {
                    // Legal characters for an XML Name ...
                    sb.append(c);
                } else {
                    // All other characters must be escaped with '_xHHHH_' where 'HHHH' is the hex string for the code point
                    hex = Integer.toHexString(c);
                    // The hex string excludes the leading '0's, so check the character values so we know how many to prepend
                    if (c >= '\u0000' && c <= '\u000f') {
                        sb.append("_x000").append(hex);
                    } else if (c >= '\u0010' && c <= '\u00ff') {
                        sb.append("_x00").append(hex);
                    } else if (c >= '\u0100' && c <= '\u0fff') {
                        sb.append("_x0").append(hex);
                    } else {
                        sb.append("_x").append(hex);
                    }
                    sb.append('_');
                }
            }
            return sb.toString();
        }

        @Override
        public String decode( String encodedText ) {
            return super.decode(encodedText);
        }
    }

}
