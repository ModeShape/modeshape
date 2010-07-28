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
package org.modeshape.jcr;

import java.io.IOException;
import java.io.OutputStream;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.Base64;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Implementation of {@link AbstractJcrExporter} that implements the document view mapping described in section 6.4.2 of the JCR
 * 1.0 specification.
 * 
 * @see JcrSession#exportDocumentView(String, ContentHandler, boolean, boolean)
 * @see JcrSession#exportDocumentView(String, OutputStream, boolean, boolean)
 */
@NotThreadSafe
class JcrDocumentViewExporter extends AbstractJcrExporter {

    private static final int ENCODE_BUFFER_SIZE = 2 << 15;

    private static final TextEncoder VALUE_ENCODER = new JcrDocumentViewExporter.JcrDocumentViewPropertyEncoder();
    private final ValueFactory<String> stringFactory;

    JcrDocumentViewExporter( JcrSession session ) {
        super(session, Collections.<String>emptyList());
        stringFactory = session.getExecutionContext().getValueFactories().getStringFactory();
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
        ExecutionContext executionContext = session.getExecutionContext();

        if (node instanceof JcrSharedNode) {
            // This is a shared node, and per Section 14.7 of the JCR 2.0 specification, they have to be written out
            // in a special way ...
            AbstractJcrNode sharedNode = ((JcrSharedNode)node).proxyNode();
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
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                exportNode(nodes.nextNode(), contentHandler, skipBinary, noRecurse);
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
                          PropertyType.nameFromValue(propertyType),
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
                              PropertyType.nameFromValue(prop.getType()),
                              "");
            return;
        }

        Value value;
        if (prop instanceof JcrSingleValueProperty) {
            value = prop.getValue();
        } else {
            // Only output the first value of the multi-valued property.
            // This is acceptable as per JCR 1.0 Spec (section 6.4.2.5)
            value = prop.getValues()[0];
        }

        String valueAsString;
        if (PropertyType.BINARY == prop.getType()) {
            StringBuffer buff = new StringBuffer(ENCODE_BUFFER_SIZE);
            try {
                Base64.InputStream is = new Base64.InputStream(value.getBinary().getStream(), Base64.ENCODE);

                byte[] bytes = new byte[ENCODE_BUFFER_SIZE];
                int len;
                while (-1 != (len = is.read(bytes, 0, ENCODE_BUFFER_SIZE))) {
                    buff.append(new String(bytes, 0, len));
                }
            } catch (IOException ioe) {
                throw new RepositoryException(ioe);
            }
            valueAsString = buff.toString();
        } else {
            valueAsString = VALUE_ENCODER.encode(value.getString());
        }

        atts.addAttribute(propName.getNamespaceUri(),
                          propName.getLocalName(),
                          localPropName,
                          PropertyType.nameFromValue(prop.getType()),
                          valueAsString);
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
            StringBuffer buff = new StringBuffer();

            for (Value value : xmlCharacters.getValues()) {
                buff.append(value.getString());
            }

            return buff.toString();
        }

        return xmlCharacters.getValue().getString();
    }

    /**
     * Special {@link TextEncoder} that implements the subset of XML name encoding suggested by section 6.4.4 of the JCR 1.0.1
     * specification. This encoder only encodes space (0x20), carriage return (0x0D), new line (0x0A), tab (0x09), and any
     * underscore characters that might otherwise suggest an encoding, as defined in {@link XmlNameEncoder}.
     */
    protected static class JcrDocumentViewPropertyEncoder extends XmlNameEncoder {

        private static final Set<Character> MAPPED_CHARACTERS;

        static {
            MAPPED_CHARACTERS = new HashSet<Character>();
            MAPPED_CHARACTERS.add(' ');
            MAPPED_CHARACTERS.add('\r');
            MAPPED_CHARACTERS.add('\n');
            MAPPED_CHARACTERS.add('\t');

        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.common.text.TextEncoder#encode(java.lang.String)
         */
        // See section 6.4.4 of the JCR 1.0.1 spec for why these hoops must be jumped through
        @Override
        public String encode( String text ) {
            if (text == null) return null;
            if (text.length() == 0) return text;
            StringBuilder sb = new StringBuilder();
            String hex = null;
            CharacterIterator iter = new StringCharacterIterator(text);
            for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
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
                } else if (!MAPPED_CHARACTERS.contains(c)) {
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
    }
}
