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
package org.modeshape.sequencer.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.property.basic.LocalNamespaceRegistry;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.DefaultHandler2;

/**
 * A {@link DefaultHandler2} implementation that is used by the sequencer.
 */
public class XmlSequencerHandler extends DefaultHandler2 {

    private final SequencerOutput output;
    private final StreamSequencerContext context;

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
     * The name of the attribute that should be used for the node name.
     */
    protected final Name nameAttribute;

    /**
     * The default primary type.
     */
    protected final Name defaultPrimaryType;

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

    private final XmlSequencer.AttributeScoping attributeScoping;

    /**
     * The path for the node representing the current element. This starts out as the path supplied by the constructor, and never
     * is shorter than that initial path.
     */
    protected Path currentPath;

    // Recursive map used to track the number of occurrences of names for elements under a particular path
    private Map<Name, List<IndexedName>> nameToIndexedNamesMap = new HashMap<Name, List<IndexedName>>();

    // The stack of recursive maps being processed, with the head entry being the map for the current path
    private final LinkedList<Map<Name, List<IndexedName>>> nameToIndexedNamesMapStack = new LinkedList<Map<Name, List<IndexedName>>>();

    private String currentEntityName;
    private StringBuilder cDataContent;
    private StringBuilder contentBuilder;
    private final Problems problems;
    private final Map<String, String> entityValues = new HashMap<String, String>();

    /**
     * @param output
     * @param context
     * @param nameAttribute
     * @param defaultPrimaryType
     * @param textDecoder
     * @param scoping
     */
    XmlSequencerHandler( SequencerOutput output,
                         StreamSequencerContext context,
                         Name nameAttribute,
                         Name defaultPrimaryType,
                         TextDecoder textDecoder,
                         XmlSequencer.AttributeScoping scoping ) {
        CheckArg.isNotNull(output, "output");
        CheckArg.isNotNull(context, "context");

        // Use the execution context ...
        this.output = output;
        this.context = context;
        this.problems = context.getProblems();
        assert this.problems != null;

        this.nameAttribute = nameAttribute;
        this.defaultPrimaryType = defaultPrimaryType;
        this.decoder = textDecoder != null ? textDecoder : DEFAULT_DECODER;
        this.attributeScoping = scoping != null ? scoping : DEFAULT_ATTRIBUTE_SCOPING;

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
        Path inputPath = context.getInputPath();
        if (!inputPath.isRoot() && inputPath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
            inputPath = inputPath.getParent();
        }
        this.currentPath = inputPath.isRoot() ? this.pathFactory.createRelativePath() : this.pathFactory.createRelativePath(inputPath.getLastSegment()
                                                                                                                                     .getName());
        assert this.currentPath != null;
    }

    private void startNode( Name name ) {
        // Check if content still needs to be output
        if (contentBuilder != null) endContent();
        // Add name to list of indexed names for this element to ensure we use the correct index (which is the size of the
        // list)
        List<IndexedName> indexedNames = nameToIndexedNamesMap.get(name);
        if (indexedNames == null) {
            indexedNames = new ArrayList<IndexedName>();
            nameToIndexedNamesMap.put(name, indexedNames);
        }
        IndexedName indexedName = new IndexedName();
        indexedNames.add(indexedName);
        // Add element name and the appropriate index to the path.
        // Per the JCR spec, the index must be relative to same-name sibling nodes
        currentPath = pathFactory.create(currentPath, name, indexedNames.size()).getNormalizedPath();
        // currentPath = currentPath.getNormalizedPath();
        // Add the indexed name map to the stack and set the current map to the new element's map
        nameToIndexedNamesMapStack.addFirst(nameToIndexedNamesMap);
        nameToIndexedNamesMap = indexedName.nameToIndexedNamesMap;
    }

    private void endNode() {
        // Recover parent's path, namespace, and indexedName map, clearing the ended element's map to free memory
        currentPath = currentPath.getParent();
        currentPath = currentPath.getNormalizedPath();
        nameToIndexedNamesMap.clear();
        nameToIndexedNamesMap = nameToIndexedNamesMapStack.removeFirst();
    }

    /**
     * See if there is any element content that needs to be completed.
     */
    protected void endContent() {
        // Process the content of the element ...
        String content = StringUtil.normalize(contentBuilder.toString());
        // Null-out builder to setup for subsequent content.
        // Must be done before call to startElement below to prevent infinite loop.
        contentBuilder = null;
        // Skip if nothing in content but whitespace
        if (content.length() > 0) {
            // Create separate node for each content entry since entries can be interspersed amongst child elements
            startNode(ModeShapeXmlLexicon.ELEMENT_CONTENT);
            output.setProperty(currentPath, JcrLexicon.PRIMARY_TYPE, ModeShapeXmlLexicon.ELEMENT_CONTENT);
            output.setProperty(currentPath, ModeShapeXmlLexicon.ELEMENT_CONTENT, content);
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
    public void startDocument() {
        output.setProperty(currentPath, JcrLexicon.PRIMARY_TYPE, ModeShapeXmlLexicon.DOCUMENT);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.xml.sax.ext.DefaultHandler2#startDTD(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void startDTD( String name,
                          String publicId,
                          String systemId ) {
        output.setProperty(currentPath, ModeShapeDtdLexicon.NAME, name);
        output.setProperty(currentPath, ModeShapeDtdLexicon.PUBLIC_ID, publicId);
        output.setProperty(currentPath, ModeShapeDtdLexicon.SYSTEM_ID, systemId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.xml.sax.ext.DefaultHandler2#externalEntityDecl(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void externalEntityDecl( String name,
                                    String publicId,
                                    String systemId ) {
        // Add "synthetic" entity container to path to help prevent name collisions with XML elements
        startNode(ModeShapeDtdLexicon.ENTITY);
        output.setProperty(currentPath, JcrLexicon.PRIMARY_TYPE, ModeShapeDtdLexicon.ENTITY);
        output.setProperty(currentPath, ModeShapeDtdLexicon.NAME, name);
        if (publicId != null) output.setProperty(currentPath, ModeShapeDtdLexicon.PUBLIC_ID, publicId);
        if (systemId != null) output.setProperty(currentPath, ModeShapeDtdLexicon.SYSTEM_ID, systemId);
        endNode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ext.DefaultHandler2#internalEntityDecl(java.lang.String, java.lang.String)
     */
    @Override
    public void internalEntityDecl( String name,
                                    String value ) {
        // Add "synthetic" entity container to path to help prevent name collisions with XML elements
        startNode(ModeShapeDtdLexicon.ENTITY);
        output.setProperty(currentPath, JcrLexicon.PRIMARY_TYPE, ModeShapeDtdLexicon.ENTITY);
        output.setProperty(currentPath, ModeShapeDtdLexicon.NAME, name);
        output.setProperty(currentPath, ModeShapeDtdLexicon.VALUE, value);
        // Record the name/value pair ...
        entityValues.put(name, value);
        endNode();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.xml.sax.helpers.DefaultHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    @Override
    public void processingInstruction( String target,
                                       String data ) {
        // Output separate nodes for each instruction since multiple are allowed
        startNode(ModeShapeXmlLexicon.PROCESSING_INSTRUCTION);
        output.setProperty(currentPath, JcrLexicon.PRIMARY_TYPE, ModeShapeXmlLexicon.PROCESSING_INSTRUCTION);
        output.setProperty(currentPath, ModeShapeXmlLexicon.TARGET, target.trim());
        if (data != null) {
            output.setProperty(currentPath, ModeShapeXmlLexicon.PROCESSING_INSTRUCTION_CONTENT, data.trim());
        }
        endNode();
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
     * <p>
     * {@inheritDoc}
     * </p>
     * 
     * @see org.xml.sax.ext.DefaultHandler2#startEntity(java.lang.String)
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
     * @see org.xml.sax.ext.DefaultHandler2#endEntity(java.lang.String)
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
    public void startCDATA() {
        // CDATA sections can start in the middle of element content, so there may already be some
        // element content already processed ...
        if (contentBuilder != null) endContent();

        // Prepare builder for concatenating consecutive lines of CDATA
        cDataContent = new StringBuilder();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ext.DefaultHandler2#endCDATA()
     */
    @Override
    public void endCDATA() {
        // Output CDATA built in characters() method
        startNode(ModeShapeXmlLexicon.CDATA);
        output.setProperty(currentPath, JcrLexicon.PRIMARY_TYPE, ModeShapeXmlLexicon.CDATA);
        output.setProperty(currentPath, ModeShapeXmlLexicon.CDATA_CONTENT, cDataContent.toString());
        endNode();
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
                         int length ) {
        // Output separate nodes for each comment since multiple are allowed
        startNode(ModeShapeXmlLexicon.COMMENT);
        output.setProperty(currentPath, JcrLexicon.PRIMARY_TYPE, ModeShapeXmlLexicon.COMMENT);
        output.setProperty(currentPath, ModeShapeXmlLexicon.COMMENT_CONTENT, String.valueOf(ch, start, length).trim());
        endNode();
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
        assert localName != null;

        // Create the node with the name built from the element's name ...
        Name nodeName = null;
        if (nameAttribute != null) {
            try {
                String jcrNameValue = attributes.getValue(nameAttribute.getNamespaceUri(), nameAttribute.getLocalName());
                nodeName = nameFactory.create(jcrNameValue);
            } catch (ValueFormatException e) {
            }
        }
        if (nodeName == null) nodeName = nameFactory.create(uri, localName, decoder);
        startNode(nodeName);

        // Set the type of the node ...
        if (defaultPrimaryType != null) {
            output.setProperty(currentPath, JcrLexicon.PRIMARY_TYPE, ModeShapeXmlLexicon.ELEMENT);
        }

        // Now, set each attribute as a property ...
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
            if (JcrLexicon.NAME.equals(attributeName)) {
                // We don't want to record the "jcr:name" attribute since it won't match the node name ...
                continue;
            }
            Object value = attributes.getValue(i);
            if (JcrLexicon.PRIMARY_TYPE.equals(attributeName)) {
                // Convert it to a name ...
                value = nameFactory.create(value);
            }
            output.setProperty(currentPath, attributeName, attributes.getValue(i));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.xml.XmlHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement( String uri,
                            String localName,
                            String name ) {
        // Check if content still needs to be output
        if (contentBuilder != null) endContent();

        // End the current node ...
        endNode();
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
        problems.addWarning(warning, XmlSequencerI18n.warningSequencingXmlDocument, warning);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
     */
    @Override
    public void error( SAXParseException error ) {
        problems.addError(error, XmlSequencerI18n.errorSequencingXmlDocument, error);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
     */
    @Override
    public void fatalError( SAXParseException error ) {
        problems.addError(error, XmlSequencerI18n.errorSequencingXmlDocument, error);
    }

    private class IndexedName {

        Map<Name, List<IndexedName>> nameToIndexedNamesMap = new HashMap<Name, List<IndexedName>>();

        IndexedName() {
        }
    }
}
