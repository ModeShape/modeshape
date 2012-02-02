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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.xml;

import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A {@link org.xml.sax.ext.DefaultHandler2} implementation that is used by the sequencer.
 */
public class XmlSequencerHandler extends DefaultHandler2 {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlSequencerHandler.class);

    /**
     * Decoder for XML names, to turn '_xHHHH_' sequences in the XML element and attribute names into the corresponding UTF-16
     * characters.
     */
    public static TextDecoder DEFAULT_DECODER = new XmlNameEncoder();

    /**
     * The default {@link XmlSequencer.AttributeScoping}.
     */
    public static XmlSequencer.AttributeScoping DEFAULT_ATTRIBUTE_SCOPING = XmlSequencer.AttributeScoping.USE_DEFAULT_NAMESPACE;

    /**
     * The TextDecoder that is used to decode the names.
     */
    protected final TextDecoder decoder;

    /**
     * The stack of prefixes for each namespace, which is used to keep the {@link NamespaceRegistry namespace registry} in
     * sync with the namespaces in the XML document.
     */
    private final Map<String, LinkedList<String>> prefixStackByUri = new HashMap<String, LinkedList<String>>();

    private final XmlSequencer.AttributeScoping attributeScoping;

    private Node currentNode;
    private Session session;

    private String currentEntityName;
    private StringBuilder cDataContent;
    private StringBuilder contentBuilder;

    private final Map<String, String> entityValues = new HashMap<String, String>();

    XmlSequencerHandler( Node rootNode,
                         XmlSequencer.AttributeScoping scoping ) throws RepositoryException {
        CheckArg.isNotNull(rootNode, "outputNode");
        this.currentNode = rootNode;

        this.session = currentNode.getSession();
        this.decoder = DEFAULT_DECODER;
        this.attributeScoping = scoping != null ? scoping : DEFAULT_ATTRIBUTE_SCOPING;
    }

    private void startNode( String name,
                            String primaryType ) throws RepositoryException {
        // Check if content still needs to be output
        if (contentBuilder != null) endContent();
        currentNode = currentNode.addNode(name, primaryType);
    }

    private void endNode() throws RepositoryException {
        // Recover parent's path, namespace, and indexedName map, clearing the ended element's map to free memory
        currentNode = currentNode.getParent();
    }

    /**
     * See if there is any element content that needs to be completed.
     */
    protected void endContent() throws RepositoryException {
        // Process the content of the element ...
        String content = StringUtil.normalize(contentBuilder.toString());
        // Null-out builder to setup for subsequent content.
        // Must be done before call to startElement below to prevent infinite loop.
        contentBuilder = null;
        // Skip if nothing in content but whitespace
        if (content.length() > 0) {
            // Create separate node for each content entry since entries can be interspersed amongst child elements
            startNode(XmlLexicon.ELEMENT_CONTENT, XmlLexicon.ELEMENT_CONTENT);
            currentNode.setProperty(XmlLexicon.ELEMENT_CONTENT, content);
            endNode();
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @see org.xml.sax.helpers.DefaultHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException {
        try {
            currentNode.setPrimaryType(XmlLexicon.DOCUMENT);
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @see org.xml.sax.ext.DefaultHandler2#startDTD(String, String, String)
     */
    @Override
    public void startDTD( String name,
                          String publicId,
                          String systemId ) throws SAXException {
        try {
            currentNode.setProperty(DtdLexicon.NAME, name);
            currentNode.setProperty(DtdLexicon.PUBLIC_ID, publicId);
            currentNode.setProperty(DtdLexicon.SYSTEM_ID, systemId);
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @see org.xml.sax.ext.DefaultHandler2#externalEntityDecl(String, String, String)
     */
    @Override
    public void externalEntityDecl( String name,
                                    String publicId,
                                    String systemId ) throws SAXException {
        // Add "synthetic" entity container to path to help prevent name collisions with XML elements
        try {
            startNode(DtdLexicon.ENTITY, DtdLexicon.ENTITY);
            currentNode.setProperty(DtdLexicon.NAME, name);
            if (publicId != null) {
                currentNode.setProperty(DtdLexicon.PUBLIC_ID, publicId);
            }
            if (systemId != null) {
                currentNode.setProperty(DtdLexicon.SYSTEM_ID, systemId);
            }
            endNode();
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xml.sax.ext.DefaultHandler2#internalEntityDecl(String, String)
     */
    @Override
    public void internalEntityDecl( String name,
                                    String value ) throws SAXException {
        // Add "synthetic" entity container to path to help prevent name collisions with XML elements
        try {
            startNode(DtdLexicon.ENTITY, DtdLexicon.ENTITY);
            currentNode.setProperty(DtdLexicon.NAME, name);
            currentNode.setProperty(DtdLexicon.VALUE, value);
            // Record the name/value pair ...
            entityValues.put(name, value);
            endNode();
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(String, String)
     */
    @Override
    public void processingInstruction( String target,
                                       String data ) throws SAXException {
        // Output separate nodes for each instruction since multiple are allowed
        try {
            startNode(XmlLexicon.PROCESSING_INSTRUCTION, XmlLexicon.PROCESSING_INSTRUCTION);
            currentNode.setProperty(XmlLexicon.TARGET, target.trim());
            if (data != null) {
                currentNode.setProperty(XmlLexicon.PROCESSING_INSTRUCTION_CONTENT, data.trim());
            }
            endNode();
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method ensures that the namespace is registered with the {@link NamespaceRegistry registry}, using the supplied prefix
     * to register the namespace if required. Note that because this class does not really use the namespace prefixes to create
     * names, no attempt is made to match the XML namespace prefixes.
     * </p>
     *
     * @see org.xml.sax.helpers.DefaultHandler#startPrefixMapping(String, String)
     */
    @Override
    public void startPrefixMapping( String prefix,
                                    String uri ) throws SAXException {
        if (StringUtil.isBlank(prefix)) {
            return;
        }

        // Add the prefix to the stack ...
        LinkedList<String> prefixStack = this.prefixStackByUri.get(uri);
        if (prefixStack == null) {
            prefixStack = new LinkedList<String>();
            this.prefixStackByUri.put(uri, prefixStack);
        }
        prefixStack.addFirst(prefix);

        try {
            if (isUriRegistered(uri)) {
                // It is already registered, but re-register it locally using the supplied prefix ...
                session.setNamespacePrefix(prefix, uri);
            } else {
                // The namespace is not already registered so we have to register it with the ws namespace registry.
                // This should also make the prefix available to the current session
                NamespaceRegistry namespaceRegistry = session.getWorkspace().getNamespaceRegistry();
                namespaceRegistry.registerNamespace(prefix, uri);
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    private boolean isUriRegistered( String uri ) throws RepositoryException {
        try {
            session.getNamespacePrefix(uri);
            return true;
        } catch (NamespaceException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xml.sax.helpers.DefaultHandler#endPrefixMapping(String)
     */
    @Override
    public void endPrefixMapping( String prefix ) throws SAXException {
        CheckArg.isNotNull(prefix, "prefix");
        if (StringUtil.isBlank(prefix)) {
            return;
        }
        try {
            // Get the current URI for this prefix ...
            String uri = session.getNamespaceURI(prefix);

            // Get the previous prefix from the stack ...
            LinkedList<String> prefixStack = this.prefixStackByUri.get(uri);
            assert prefixStack != null;
            assert !prefixStack.isEmpty();
            String existingPrefix = prefixStack.removeFirst();
            assert prefix.equals(existingPrefix);

            // If there are no previous prefixes, then remove the mapping ...
            if (prefixStack.isEmpty()) {
                prefixStackByUri.remove(uri);
            } else {
                String previous = prefixStack.getFirst();
                session.setNamespacePrefix(previous, uri);
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @see org.xml.sax.ext.DefaultHandler2#startEntity(String)
     */
    @Override
    public void startEntity( String name ) {
        // Record that we've started an entity by capturing the name of the entity ...
        currentEntityName = name;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @see org.xml.sax.ext.DefaultHandler2#endEntity(String)
     */
    @Override
    public void endEntity( String name ) {
        // currentEntityName is nulled in 'characters(...)', not here.
        // See ModeShape-231 for an issue related to this
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @see org.xml.sax.ext.DefaultHandler2#startCDATA()
     */
    @Override
    public void startCDATA() throws SAXException {
        // CDATA sections can start in the middle of element content, so there may already be some
        // element content already processed ...
        try {
            if (contentBuilder != null) endContent();
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }

        // Prepare builder for concatenating consecutive lines of CDATA
        cDataContent = new StringBuilder();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xml.sax.ext.DefaultHandler2#endCDATA()
     */
    @Override
    public void endCDATA() throws SAXException {
        // Output CDATA built in characters() method
        try {
            startNode(XmlLexicon.CDATA, XmlLexicon.CDATA);
            currentNode.setProperty(XmlLexicon.CDATA_CONTENT, cDataContent.toString());
            endNode();
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
        // Null-out builder to free memory
        cDataContent = null;
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
        String content = String.valueOf(ch, start, length);
        if (cDataContent != null) {
            // Processing the characters in the CDATA, so add to the builder
            cDataContent.append(ch, start, length);
            // Text within builder will be output at the end of CDATA
        } else {
            if (contentBuilder == null) {
                // This is the first line of content, so we have to create the StringBuilder ...
                contentBuilder = new StringBuilder();
            }
            if (currentEntityName != null) {
                // This is an entity reference, so rather than use the entity value characters (the content passed
                // into this method), we want to keep the entity reference ...
                contentBuilder.append('&').append(currentEntityName).append(';');

                // Normally, 'characters' is called with just the entity replacement characters,
                // and is called between 'startEntity' and 'endEntity'. However, per ModeShape-231, some JVMs
                // use an incorrect ordering: 'startEntity', 'endEntity' and then 'characters', and the
                // content passed to the 'characters' call not only includes the entity replacement characters
                // followed by other content. Look for this condition ...
                String entityValue = entityValues.get(currentEntityName);
                if (!content.equals(entityValue) && entityValue != null && entityValue.length() < content.length()) {
                    // Per ModeShape-231, there's extra content after the entity value. So replace the entity value in the
                    // content with the entity reference (not the replacement characters), and add the extra content ...
                    String extraContent = content.substring(entityValue.length());
                    contentBuilder.append(extraContent);
                }
                // We're done reading the entity characters, so null it out
                currentEntityName = null;
            } else {
                // Just append the content normally ...
                contentBuilder.append(content);
            }
            // Text within builder will be output when another element or CDATA is encountered
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xml.sax.ext.DefaultHandler2#comment(char[], int, int)
     */
    @Override
    public void comment( char[] ch,
                         int start,
                         int length ) throws SAXException {
        // Output separate nodes for each comment since multiple are allowed
        try {
            startNode(XmlLexicon.COMMENT, XmlLexicon.COMMENT);
            currentNode.setProperty(XmlLexicon.COMMENT_CONTENT, String.valueOf(ch, start, length).trim());
            endNode();
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xml.sax.helpers.DefaultHandler#startElement(String, String, String,
     *      org.xml.sax.Attributes)
     */
    @Override
    public void startElement( String uri,
                              String localName,
                              String name,
                              Attributes attributes ) throws SAXException {
        assert localName != null;

        try {

            // Create the node with the name built from the element's name ...
            String nodeName = createAttributeName(uri, localName);;
            startNode(nodeName, XmlLexicon.ELEMENT);

            // Now, set each attribute as a property ...
            for (int i = 0, len = attributes.getLength(); i != len; ++i) {
                String attributeLocalName = attributes.getLocalName(i);
                String attributeUri = attributes.getURI(i);
                String attributeName = null;
                if ((attributeUri == null || attributeUri.length() == 0) && attributes.getQName(i).indexOf(':') == -1) {
                    switch (this.attributeScoping) {
                        case INHERIT_ELEMENT_NAMESPACE:
                            attributeName = createAttributeName(uri, attributeLocalName);
                            break;
                        case USE_DEFAULT_NAMESPACE:
                            attributeName = createAttributeName(null, attributeLocalName);
                            break;
                    }
                } else {
                    attributeName = createAttributeName(attributeUri, attributeLocalName);
                }
                assert attributeName != null;
                if (JcrConstants.JCR_NAME.equals(attributeName)) {
                    // We don't want to record the "jcr:name" attribute since it won't match the node name ...
                    continue;
                }
                currentNode.setProperty(attributeName, attributes.getValue(i));
            }
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    private String createAttributeName( String uri,
                                        String localName ) throws RepositoryException {
        if (StringUtil.isBlank(uri)) {
            return decoder.decode(localName.trim());
        } else {
            String prefix = session.getNamespacePrefix(uri);
            assert prefix != null;
            return prefix + ":" + decoder.decode(localName.trim());
        }
    }

    @Override
    public void endElement( String uri,
                            String localName,
                            String name ) throws SAXException {
        try {
            // Check if content still needs to be output
            if (contentBuilder != null) endContent();
            // End the current node ...
            endNode();
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @see org.xml.sax.helpers.DefaultHandler#warning(org.xml.sax.SAXParseException)
     */
    @Override
    public void warning( SAXParseException warning ) {
        LOGGER.warn("SAX warning:", warning);
    }

    /**
     * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error( SAXParseException error ) {
        LOGGER.error("SAX error:", error);
    }
}
