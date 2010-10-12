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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.Base64;
import org.modeshape.common.xml.XmlCharacters;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.ValueFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Implementation of {@link AbstractJcrExporter} that implements the system view mapping described in section 7.2 of the JCR 2.0
 * specification.
 * 
 * @see JcrSession#exportSystemView(String, ContentHandler, boolean, boolean)
 * @see JcrSession#exportSystemView(String, OutputStream, boolean, boolean)
 */
@NotThreadSafe
class JcrSystemViewExporter extends AbstractJcrExporter {

    /**
     * Buffer size for reading Base64-encoded binary streams for export.
     */
    private static final int BASE_64_BUFFER_SIZE = 1024;

    /**
     * The list of the special JCR properties that must be exported first for each node. These properties must be exported in list
     * order if they are present on the node as per section 6.4.1 rule 11.
     */
    private static final List<Name> SPECIAL_PROPERTY_NAMES = Arrays.asList(new Name[] {JcrLexicon.PRIMARY_TYPE,
        JcrLexicon.MIXIN_TYPES, JcrLexicon.UUID});

    JcrSystemViewExporter( JcrSession session ) {
        super(session, Arrays.asList(new String[] {"xml"}));
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
        exportNode(node, contentHandler, skipBinary, noRecurse, node.getDepth() == 0);
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
     * @param isRoot true if the supplied node is the root node (supplied as an efficiency)
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    protected void exportNode( Node node,
                               ContentHandler contentHandler,
                               boolean skipBinary,
                               boolean noRecurse,
                               boolean isRoot ) throws RepositoryException, SAXException {

        // start the sv:node element for this JCR node
        AttributesImpl atts = new AttributesImpl();
        String nodeName = node.getName();
        if (isRoot && node.getDepth() == 0) {
            // This is the root node ...
            nodeName = "jcr:root";
        }
        atts.addAttribute(JcrSvLexicon.NAME.getNamespaceUri(),
                          JcrSvLexicon.NAME.getLocalName(),
                          getPrefixedName(JcrSvLexicon.NAME),
                          PropertyType.nameFromValue(PropertyType.STRING),
                          nodeName);

        startElement(contentHandler, JcrSvLexicon.NODE, atts);

        if (node instanceof JcrSharedNode) {
            // This is a shared node, and per Section 14.7 of the JCR 2.0 specification, they have to be written out
            // in a special way ...

            // jcr:primaryType = nt:share ...
            emitProperty(JcrLexicon.PRIMARY_TYPE, PropertyType.NAME, JcrNtLexicon.SHARE, contentHandler, skipBinary);

            // jcr:uuid = UUID of shared node ...
            emitProperty(JcrLexicon.UUID, PropertyType.STRING, node.getIdentifier(), contentHandler, skipBinary);
        } else {

            // Output any special properties first (see Javadoc for SPECIAL_PROPERTY_NAMES for more context)
            for (Name specialPropertyName : SPECIAL_PROPERTY_NAMES) {
                Property specialProperty = ((AbstractJcrNode)node).getProperty(specialPropertyName);

                if (specialProperty != null) {
                    emitProperty(specialProperty, contentHandler, skipBinary);
                }
            }

            PropertyIterator properties = node.getProperties();
            while (properties.hasNext()) {
                exportProperty(properties.nextProperty(), contentHandler, skipBinary);
            }

            if (!noRecurse) {
                NodeIterator nodes = node.getNodes();
                while (nodes.hasNext()) {
                    exportNode(nodes.nextNode(), contentHandler, skipBinary, noRecurse, false);
                }
            }
        }

        endElement(contentHandler, JcrSvLexicon.NODE);
    }

    /**
     * @param property
     * @param contentHandler the SAX content handler for which SAX events will be invoked as the XML document is created.
     * @param skipBinary if <code>true</code>, indicates that binary properties should not be exported
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    private void exportProperty( Property property,
                                 ContentHandler contentHandler,
                                 boolean skipBinary ) throws RepositoryException, SAXException {
        assert property instanceof AbstractJcrProperty : "Illegal attempt to use " + getClass().getName()
                                                         + " on non-ModeShape property";

        AbstractJcrProperty prop = (AbstractJcrProperty)property;

        Name propertyName = prop.name();
        if (SPECIAL_PROPERTY_NAMES.contains(propertyName)) {
            return;
        }

        emitProperty(property, contentHandler, skipBinary);
    }

    /**
     * Fires the appropriate SAX events on the content handler to build the XML elements for the property.
     * 
     * @param property the property to be exported
     * @param contentHandler the SAX content handler for which SAX events will be invoked as the XML document is created.
     * @param skipBinary if <code>true</code>, indicates that binary properties should not be exported
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    private void emitProperty( Property property,
                               ContentHandler contentHandler,
                               boolean skipBinary ) throws RepositoryException, SAXException {
        assert property instanceof AbstractJcrProperty : "Illegal attempt to use " + getClass().getName()
                                                         + " on non-ModeShape property";

        AbstractJcrProperty prop = (AbstractJcrProperty)property;

        // first set the property sv:name attribute
        AttributesImpl propAtts = new AttributesImpl();
        propAtts.addAttribute(JcrSvLexicon.NAME.getNamespaceUri(),
                              JcrSvLexicon.NAME.getLocalName(),
                              getPrefixedName(JcrSvLexicon.NAME),
                              PropertyType.nameFromValue(PropertyType.STRING),
                              prop.getName());

        // and it's sv:type attribute
        propAtts.addAttribute(JcrSvLexicon.TYPE.getNamespaceUri(),
                              JcrSvLexicon.TYPE.getLocalName(),
                              getPrefixedName(JcrSvLexicon.TYPE),
                              PropertyType.nameFromValue(PropertyType.STRING),
                              PropertyType.nameFromValue(prop.getType()));

        // output the sv:property element
        startElement(contentHandler, JcrSvLexicon.PROPERTY, propAtts);

        // then output a sv:value element for each of its values
        if (prop instanceof JcrMultiValueProperty) {
            Value[] values = prop.getValues();
            for (int i = 0; i < values.length; i++) {

                emitValue(values[i], contentHandler, property.getType(), skipBinary);
            }
        } else {
            emitValue(property.getValue(), contentHandler, property.getType(), skipBinary);
        }

        // end the sv:property element
        endElement(contentHandler, JcrSvLexicon.PROPERTY);
    }

    /**
     * Fires the appropriate SAX events on the content handler to build the XML elements for the value.
     * 
     * @param value the value to be exported
     * @param contentHandler the SAX content handler for which SAX events will be invoked as the XML document is created.
     * @param propertyType the {@link PropertyType} for the given value
     * @param skipBinary if <code>true</code>, indicates that binary properties should not be exported
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    private void emitValue( Value value,
                            ContentHandler contentHandler,
                            int propertyType,
                            boolean skipBinary ) throws RepositoryException, SAXException {

        if (PropertyType.BINARY == propertyType) {
            startElement(contentHandler, JcrSvLexicon.VALUE, null);

            // Per section 6.5 of the 1.0.1 spec, we need to emit one empty-value tag for each value if the property is
            // multi-valued and skipBinary is true
            if (!skipBinary) {
                byte[] bytes = new byte[BASE_64_BUFFER_SIZE];
                int len;

                Binary binary = value.getBinary();
                try {
                    InputStream stream = new Base64.InputStream(binary.getStream(), Base64.ENCODE);
                    while (-1 != (len = stream.read(bytes))) {
                        contentHandler.characters(new String(bytes, 0, len).toCharArray(), 0, len);
                    }
                } catch (IOException ioe) {
                    throw new RepositoryException(ioe);
                } finally {
                    binary.dispose();
                }
            }
            endElement(contentHandler, JcrSvLexicon.VALUE);
        } else {
            emitValue(value.getString(), contentHandler);
        }
    }

    private void emitValue( String value,
                            ContentHandler contentHandler ) throws RepositoryException, SAXException {

        // Per Section 7.2 Rule 11a of the JCR 2.0 spec, need to check invalid XML characters

        char[] chars = value.toCharArray();

        boolean allCharsAreValidXml = true;
        for (int i = 0; i < chars.length; i++) {
            if (!XmlCharacters.isValid(chars[i])) {
                allCharsAreValidXml = false;
                break;
            }
        }

        if (allCharsAreValidXml) {

            startElement(contentHandler, JcrSvLexicon.VALUE, null);
            contentHandler.characters(chars, 0, chars.length);
            endElement(contentHandler, JcrSvLexicon.VALUE);
        } else {
            AttributesImpl valueAtts = new AttributesImpl();
            valueAtts.addAttribute("xsi", "type", "xsi:type", "STRING", "xsd:base64Binary");

            startElement(contentHandler, JcrSvLexicon.VALUE, valueAtts);
            try {
                chars = Base64.encodeBytes(value.getBytes("UTF-8")).toCharArray();
            } catch (IOException ioe) {
                throw new RepositoryException(ioe);
            }
            contentHandler.characters(chars, 0, chars.length);
            endElement(contentHandler, JcrSvLexicon.VALUE);
        }
    }

    /**
     * Fires the appropriate SAX events on the content handler to build the XML elements for the property.
     * 
     * @param propertyName the name of the property to be exported
     * @param propertyType the type of the property to be exported
     * @param value the value of the single-valued property to be exported
     * @param contentHandler the SAX content handler for which SAX events will be invoked as the XML document is created.
     * @param skipBinary if <code>true</code>, indicates that binary properties should not be exported
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    private void emitProperty( Name propertyName,
                               int propertyType,
                               Object value,
                               ContentHandler contentHandler,
                               boolean skipBinary ) throws RepositoryException, SAXException {
        ValueFactory<String> strings = session.getExecutionContext().getValueFactories().getStringFactory();

        // first set the property sv:name attribute
        AttributesImpl propAtts = new AttributesImpl();
        propAtts.addAttribute(JcrSvLexicon.NAME.getNamespaceUri(),
                              JcrSvLexicon.NAME.getLocalName(),
                              getPrefixedName(JcrSvLexicon.NAME),
                              PropertyType.nameFromValue(PropertyType.STRING),
                              strings.create(propertyName));

        // and it's sv:type attribute
        propAtts.addAttribute(JcrSvLexicon.TYPE.getNamespaceUri(),
                              JcrSvLexicon.TYPE.getLocalName(),
                              getPrefixedName(JcrSvLexicon.TYPE),
                              PropertyType.nameFromValue(PropertyType.STRING),
                              PropertyType.nameFromValue(propertyType));

        // output the sv:property element
        startElement(contentHandler, JcrSvLexicon.PROPERTY, propAtts);

        // then output a sv:value element for each of its values
        emitValue(strings.create(value), contentHandler);

        // end the sv:property element
        endElement(contentHandler, JcrSvLexicon.PROPERTY);
    }
}
