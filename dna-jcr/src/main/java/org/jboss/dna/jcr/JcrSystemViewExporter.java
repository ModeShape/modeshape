package org.jboss.dna.jcr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.Base64;
import org.jboss.dna.common.xml.XmlCharacters;
import org.jboss.dna.graph.property.Name;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Implementation of {@link AbstractJcrExporter} that implements the system view mapping described in section 6.4.1 of the JCR 1.0
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

        // start the sv:node element for this JCR node
        AttributesImpl atts = new AttributesImpl();
        atts.addAttribute(JcrSvLexicon.NAME.getNamespaceUri(),
                          JcrSvLexicon.NAME.getLocalName(),
                          getPrefixedName(JcrSvLexicon.NAME),
                          PropertyType.nameFromValue(PropertyType.STRING),
                          node.getName());

        startElement(contentHandler, JcrSvLexicon.NODE, atts);

        // Output any special properties first (see Javadoc for SPECIAL_PROPERTY_NAMES for more context)
        for (Name specialPropertyName : SPECIAL_PROPERTY_NAMES) {
            Property specialProperty = ((AbstractJcrNode)node).getProperty(specialPropertyName);

            if (specialProperty != null) {
                emitProperty(specialProperty, contentHandler);
            }
        }

        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            exportProperty(properties.nextProperty(), contentHandler, skipBinary);
        }

        if (!noRecurse) {
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                exportNode(nodes.nextNode(), contentHandler, skipBinary, noRecurse);
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
                                                         + " on non-DNA property";

        AbstractJcrProperty prop = (AbstractJcrProperty)property;

        Name propertyName = prop.name();
        if (SPECIAL_PROPERTY_NAMES.contains(propertyName)) {
            return;
        }

        if (skipBinary && PropertyType.BINARY == prop.getType()) {
            return;
        }

        emitProperty(property, contentHandler);
    }

    /**
     * Fires the appropriate SAX events on the content handler to build the XML elements for the property.
     * 
     * @param property the property to be exported
     * @param contentHandler the SAX content handler for which SAX events will be invoked as the XML document is created.
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    private void emitProperty( Property property,
                               ContentHandler contentHandler ) throws RepositoryException, SAXException {
        assert property instanceof AbstractJcrProperty : "Illegal attempt to use " + getClass().getName()
                                                         + " on non-DNA property";

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

                emitValue(values[i], contentHandler, property.getType());
            }
        } else {
            emitValue(property.getValue(), contentHandler, property.getType());
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
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    private void emitValue( Value value,
                            ContentHandler contentHandler,
                            int propertyType ) throws RepositoryException, SAXException {

        if (PropertyType.BINARY == propertyType) {
            startElement(contentHandler, JcrSvLexicon.VALUE, null);

            byte[] bytes = new byte[BASE_64_BUFFER_SIZE];
            int len;

            try {
                InputStream stream = new Base64.InputStream(value.getStream(), Base64.ENCODE | Base64.URL_SAFE
                                                                               | Base64.DONT_BREAK_LINES);

                while (-1 != (len = stream.read(bytes))) {
                    contentHandler.characters(new String(bytes, 0, len).toCharArray(), 0, len);
                }
            } catch (IOException ioe) {
                throw new RepositoryException(ioe);
            }

            endElement(contentHandler, JcrSvLexicon.VALUE);
        } else {
            String s = value.getString();

            // Per 6.4.1.2 Rule #7 of the JCR 1.0 spec, need to check invalid XML characters

            char[] chars = s.toCharArray();

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
                chars = Base64.encodeBytes(s.getBytes(), Base64.URL_SAFE).toCharArray();
                contentHandler.characters(chars, 0, chars.length);
                endElement(contentHandler, JcrSvLexicon.VALUE);
            }
        }

    }

}
