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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.sequencers.SequencerContext;
import org.jboss.dna.graph.sequencers.SequencerOutput;
import org.jboss.dna.graph.sequencers.StreamSequencer;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * @author John Verhaeg
 */
public class XmlSequencer implements StreamSequencer {

    private static final String DEFAULT_PRIMARY_TYPE = "nt:unstructured";
    private static final String DECL_HANDLER_FEATURE = "http://xml.org/sax/properties/declaration-handler";
    private static final String ENTITY_RESOLVER_2_FEATURE = "http://xml.org/sax/features/use-entity-resolver2";
    private static final String LEXICAL_HANDLER_FEATURE = "http://xml.org/sax/properties/lexical-handler";
    private static final String RESOLVE_DTD_URIS_FEATURE = "http://xml.org/sax/features/resolve-dtd-uris";
    private static final String LOAD_EXTERNAL_DTDS_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencers.StreamSequencer#sequence(InputStream, SequencerOutput, SequencerContext,
     *      ProgressMonitor)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          SequencerContext context,
                          ProgressMonitor monitor ) {
        monitor.beginTask(100.0, GraphI18n.sequencingXmlDocument);
        XMLReader reader;
        try {
            reader = XMLReaderFactory.createXMLReader();
            Handler handler = new Handler(output, context, monitor);
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            // Ensure handler acting as entity resolver 2
            reader.setProperty(DECL_HANDLER_FEATURE, handler);
            // Ensure handler acting as lexical handler
            reader.setProperty(LEXICAL_HANDLER_FEATURE, handler);
            // Ensure handler acting as entity resolver 2
            setFeature(reader, ENTITY_RESOLVER_2_FEATURE, true);
            // Prevent loading of external DTDs
            setFeature(reader, LOAD_EXTERNAL_DTDS_FEATURE, false);
            // Prevent the resolving of DTD entities into fully-qualified URIS
            setFeature(reader, RESOLVE_DTD_URIS_FEATURE, false);
            // Parse XML document
            reader.parse(new InputSource(stream));
        } catch (Exception error) {
            context.getLogger(getClass()).error(error, GraphI18n.fatalErrorSequencingXmlDocument, error);
            monitor.getProblems().addError(error, GraphI18n.fatalErrorSequencingXmlDocument, error);
        } finally {
            monitor.done();
        }
    }

    /**
     * Sets the reader's named feature to the supplied value, only if the feature is not already set to that value. This method
     * does nothing if the feature is not known to the reader.
     * 
     * @param reader the reader; may not be null
     * @param featureName the name of the feature; may not be null
     * @param value the value for the feature
     */
    private void setFeature( XMLReader reader,
                             String featureName,
                             boolean value ) {
        try {
            if (reader.getFeature(featureName) != value) {
                reader.setFeature(featureName, value);
            }
        } catch (SAXNotRecognizedException meansFeatureNotRecognized) {
        } catch (SAXNotSupportedException meansFeatureNotSupported) {
        }
    }

    private final class Handler extends DefaultHandler2 {

        private final SequencerOutput output;
        private final SequencerContext context;
        private final ProgressMonitor monitor;

        private double progress;

        private Path path; // The DNA path of the node representing the current XML element

        // Cached instances of the name factory and commonly referenced names
        private final NameFactory nameFactory;
        private Name primaryTypeName;
        private Name defaultPrimaryType;

        // Recursive map used to track the number of occurrences of names for elements under a particular path
        private Map<Name, List<IndexedName>> nameToIndexedNamesMap = new HashMap<Name, List<IndexedName>>();

        // The stack of recursive maps being processed, with the head entry being the map for the current path
        private final LinkedList<Map<Name, List<IndexedName>>> nameToIndexedNamesMapStack = new LinkedList<Map<Name, List<IndexedName>>>();

        // The stack of XML namespace in scope, with the head entry being namespace of the closest ancestor element declaring a
        // namespace.
        private final LinkedList<String> nsStack = new LinkedList<String>();

        // Builder used to concatenate concurrent lines of CDATA into a single value.
        private StringBuilder cDataBuilder;

        // Builder used to concatenate concurrent lines of element content and entity evaluations into a single value.
        private StringBuilder contentBuilder;

        // The entity being processed
        private String entity;

        Handler( SequencerOutput output,
                 SequencerContext context,
                 ProgressMonitor monitor ) {
            assert output != null;
            assert monitor != null;
            assert context != null;
            this.output = output;
            this.context = context;
            this.monitor = monitor;
            // Initialize path to a an empty path relative to the SequencerOutput's target path.
            path = context.getValueFactories().getPathFactory().createRelativePath();
            // Cache name factory since it is frequently used
            nameFactory = context.getValueFactories().getNameFactory();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.ext.DefaultHandler2#attributeDecl(java.lang.String, java.lang.String, java.lang.String,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public void attributeDecl( String name,
                                   String name2,
                                   String type,
                                   String mode,
                                   String value ) throws SAXException {
            stopIfCancelled();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        @Override
        public void characters( char[] ch,
                                int start,
                                int length ) throws SAXException {
            stopIfCancelled();
            String content = String.valueOf(ch, start, length);
            // Check if data should be appended to previously parsed CDATA
            if (cDataBuilder == null) {
                // If content is for an entity, replace with entity reference
                if (entity != null) {
                    content = '&' + entity + ';';
                }
                // Check if first line of content
                if (contentBuilder == null) {
                    contentBuilder = new StringBuilder(content);
                } else {
                    // Append additional lines or entity evaluations to previous content, separated by a space
                    if (entity == null) {
                        contentBuilder.append(' ');
                    }
                    contentBuilder.append(content);
                    // Text within builder will be output when another element or CDATA is encountered
                }
            } else {
                cDataBuilder.append(ch, start, length);
                // Text within builder will be output at the end of CDATA
            }
            updateProgress();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.ext.DefaultHandler2#comment(char[], int, int)
         */
        @Override
        public void comment( char[] ch,
                             int start,
                             int length ) throws SAXException {
            stopIfCancelled();
            // Output separate nodes for each comment since multiple are allowed
            startElement(DnaXmlLexicon.COMMENT);
            output.setProperty(path, getPrimaryTypeName(), DnaXmlLexicon.COMMENT);
            output.setProperty(path, DnaXmlLexicon.COMMENT_CONTENT, String.valueOf(ch, start, length));
            endElement();
            updateProgress();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.ext.DefaultHandler2#elementDecl(java.lang.String, java.lang.String)
         */
        @Override
        public void elementDecl( String name,
                                 String model ) throws SAXException {
            stopIfCancelled();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.ext.DefaultHandler2#endCDATA()
         */
        @Override
        public void endCDATA() throws SAXException {
            stopIfCancelled();
            // Output CDATA built in characters() method
            output.setProperty(path, DnaXmlLexicon.CDATA_CONTENT, cDataBuilder.toString());
            endElement();
            // Null-out builder to free memory
            cDataBuilder = null;
            updateProgress();
        }

        private void endContent() {
            if (contentBuilder != null) {
                // Normalize content
                String content = StringUtil.normalize(contentBuilder.toString());
                // Null-out builder to setup for subsequent content.
                // Must be done before call to startElement below to prevent infinite loop.
                contentBuilder = null;
                // Skip if nothing in content but whitespace
                if (content.length() > 0) {
                    // Create separate node for each content entry since entries can be interspersed amongst child elements
                    startElement(DnaXmlLexicon.ELEMENT_CONTENT);
                    output.setProperty(path, getPrimaryTypeName(), DnaXmlLexicon.ELEMENT_CONTENT);
                    output.setProperty(path, DnaXmlLexicon.ELEMENT_CONTENT, content);
                    endElement();
                }
            }
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#endDocument()
         */
        @Override
        public void endDocument() throws SAXException {
            stopIfCancelled();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.ext.DefaultHandler2#endDTD()
         */
        @Override
        public void endDTD() throws SAXException {
            stopIfCancelled();
        }

        private void endElement() {
            // Recover parent's path, namespace, and indexedName map, clearing the ended element's map to free memory
            path = path.getParent();
            nameToIndexedNamesMap.clear();
            nameToIndexedNamesMap = nameToIndexedNamesMapStack.removeFirst();
            nsStack.removeFirst();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void endElement( String uri,
                                String localName,
                                String name ) throws SAXException {
            stopIfCancelled();
            // Check if content still needs to be output
            endContent();
            endElement();
            updateProgress();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.ext.DefaultHandler2#endEntity(java.lang.String)
         */
        @Override
        public void endEntity( String name ) throws SAXException {
            stopIfCancelled();
            entity = null;
            updateProgress();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#error(org.xml.sax.SAXParseException)
         */
        @Override
        public void error( SAXParseException error ) {
            context.getLogger(XmlSequencer.class).error(error, GraphI18n.errorSequencingXmlDocument, error);
            monitor.getProblems().addError(error, GraphI18n.errorSequencingXmlDocument, error);
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
                                        String systemId ) throws SAXException {
            stopIfCancelled();
            // Add "synthetic" entity container to path to help prevent name collisions with XML elements
            Name entityName = DnaDtdLexicon.ENTITY;
            startElement(entityName);
            output.setProperty(path, getPrimaryTypeName(), entityName);
            output.setProperty(path, nameFactory.create(DnaDtdLexicon.NAME), name);
            output.setProperty(path, nameFactory.create(DnaDtdLexicon.PUBLIC_ID), publicId);
            output.setProperty(path, nameFactory.create(DnaDtdLexicon.SYSTEM_ID), systemId);
            endElement();
            updateProgress();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#fatalError(org.xml.sax.SAXParseException)
         */
        @Override
        public void fatalError( SAXParseException error ) {
            context.getLogger(XmlSequencer.class).error(error, GraphI18n.fatalErrorSequencingXmlDocument, error);
            monitor.getProblems().addError(error, GraphI18n.fatalErrorSequencingXmlDocument, error);
        }

        private Name getPrimaryTypeName() {
            if (primaryTypeName == null) {
                primaryTypeName = nameFactory.create(NameFactory.JCR_PRIMARY_TYPE);
            }
            return primaryTypeName;
        }

        private Name getDefaultPrimaryType() {
            if (defaultPrimaryType == null) {
                defaultPrimaryType = nameFactory.create(DEFAULT_PRIMARY_TYPE);
            }
            return defaultPrimaryType;
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#ignorableWhitespace(char[], int, int)
         */
        @Override
        public void ignorableWhitespace( char[] ch,
                                         int start,
                                         int length ) throws SAXException {
            stopIfCancelled();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.ext.DefaultHandler2#internalEntityDecl(java.lang.String, java.lang.String)
         */
        @Override
        public void internalEntityDecl( String name,
                                        String value ) throws SAXException {
            stopIfCancelled();
            // Add "synthetic" entity container to path to help prevent name collisions with XML elements
            Name entityName = DnaDtdLexicon.ENTITY;
            startElement(entityName);
            output.setProperty(path, getPrimaryTypeName(), entityName);
            output.setProperty(path, DnaDtdLexicon.NAME, name);
            output.setProperty(path, DnaDtdLexicon.VALUE, value);
            endElement();
            updateProgress();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#notationDecl(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void notationDecl( String name,
                                  String publicId,
                                  String systemId ) throws SAXException {
            stopIfCancelled();
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
                                           String data ) throws SAXException {
            stopIfCancelled();
            // Output separate nodes for each instruction since multiple are allowed
            Name name = DnaXmlLexicon.PROCESSING_INSTRUCTION;
            startElement(name);
            output.setProperty(path, getPrimaryTypeName(), name);
            output.setProperty(path, DnaXmlLexicon.TARGET, target);
            output.setProperty(path, DnaXmlLexicon.PROCESSING_INSTRUCTION_CONTENT, data);
            endElement();
            updateProgress();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#skippedEntity(java.lang.String)
         */
        @Override
        public void skippedEntity( String name ) throws SAXException {
            stopIfCancelled();
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
            stopIfCancelled();
            // Output separate nodes for each CDATA since multiple are allowed
            startElement(DnaXmlLexicon.CDATA);
            // Prepare builder for concatenating consecutive lines of CDATA
            cDataBuilder = new StringBuilder();
            updateProgress();
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
            stopIfCancelled();
            output.setProperty(path, getPrimaryTypeName(), DnaXmlLexicon.DOCUMENT);
            updateProgress();
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
                              String systemId ) throws SAXException {
            stopIfCancelled();
            output.setProperty(path, DnaDtdLexicon.NAME, name);
            output.setProperty(path, DnaDtdLexicon.PUBLIC_ID, publicId);
            output.setProperty(path, DnaDtdLexicon.SYSTEM_ID, systemId);
            updateProgress();
        }

        private void startElement( Name name ) {
            // Check if content still needs to be output
            endContent();
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
            path = context.getValueFactories().getPathFactory().create(path, name, indexedNames.size());
            path = path.getNormalizedPath();
            // Add the indexed name map to the stack and set the current map to the new element's map
            nameToIndexedNamesMapStack.addFirst(nameToIndexedNamesMap);
            nameToIndexedNamesMap = indexedName.nameToIndexedNamesMap;
            // Set the current namespace to whatever is declared by this element, or if not declared, to its nearest ancestor that
            // does declare a namespace.
            String ns = name.getNamespaceUri();
            if (ns.length() == 0) {
                nsStack.addFirst(nsStack.isEmpty() ? "" : nsStack.getFirst());
            } else {
                nsStack.addFirst(ns);
            }
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
         *      org.xml.sax.Attributes)
         */
        @Override
        public void startElement( String uri,
                                  String localName,
                                  String name,
                                  Attributes attributes ) throws SAXException {
            stopIfCancelled();
            // Look for the "jcr:name" attribute, and use that if it's there
            Name type = getDefaultPrimaryType();
            Name nameObj = nameFactory.create(name);
            for (int ndx = 0, len = attributes.getLength(); ndx < len; ++ndx) {
                String ns = attributes.getURI(ndx);
                String attrLocalName = attributes.getLocalName(ndx);
                Object value = attributes.getValue(ndx);
                String jcrNsUri = context.getNamespaceRegistry().getNamespaceForPrefix("jcr");
                if (jcrNsUri != null && jcrNsUri.equals(ns) && attrLocalName.equals("name")) {
                    nameObj = nameFactory.create(value);
                    break;
                }
            }
            startElement(nameObj);
            output.setProperty(path, getPrimaryTypeName(), type);
            // Output this element's attributes using the attribute's namespace, if supplied, or the current namespace in scope.
            String inheritedNs = nsStack.getFirst();
            for (int ndx = 0, len = attributes.getLength(); ndx < len; ++ndx) {
                String ns = attributes.getURI(ndx);
                String attrLocalName = attributes.getLocalName(ndx);
                Object value = attributes.getValue(ndx);
                String jcrNsUri = context.getNamespaceRegistry().getNamespaceForPrefix("jcr");
                if (jcrNsUri != null && jcrNsUri.equals(ns) && attrLocalName.equals("primaryType")) {
                    value = nameFactory.create(value);
                }
                if (jcrNsUri != null && jcrNsUri.equals(ns) && attrLocalName.equals("name")) {
                    continue;
                }
                output.setProperty(path, nameFactory.create(ns.length() == 0 ? inheritedNs : ns, attrLocalName), value);
            }
            updateProgress();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.ext.DefaultHandler2#startEntity(java.lang.String)
         */
        @Override
        public void startEntity( String name ) throws SAXException {
            stopIfCancelled();
            entity = name;
            updateProgress();
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#startPrefixMapping(java.lang.String, java.lang.String)
         */
        @Override
        public void startPrefixMapping( String prefix,
                                        String uri ) throws SAXException {
            stopIfCancelled();
            // Register any unregistered namespaces
            NamespaceRegistry registry = context.getNamespaceRegistry();
            if (!registry.isRegisteredNamespaceUri(uri)) {
                registry.register(prefix, uri);
            }
            updateProgress();
        }

        private void stopIfCancelled() throws SAXException {
            if (monitor.isCancelled()) {
                throw new SAXException(GraphI18n.canceledSequencingXmlDocument.text());
            }
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         * 
         * @see org.xml.sax.helpers.DefaultHandler#unparsedEntityDecl(java.lang.String, java.lang.String, java.lang.String,
         *      java.lang.String)
         */
        @Override
        public void unparsedEntityDecl( String name,
                                        String publicId,
                                        String systemId,
                                        String notationName ) throws SAXException {
            stopIfCancelled();
        }

        private void updateProgress() {
            if (progress == 100.0) {
                progress = 1;
            } else {
                progress++;
            }
            monitor.worked(progress);
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
            context.getLogger(XmlSequencer.class).warn(warning, GraphI18n.warningSequencingXmlDocument);
            monitor.getProblems().addWarning(warning, GraphI18n.warningSequencingXmlDocument, warning);
        }
    }

    private class IndexedName {

        Map<Name, List<IndexedName>> nameToIndexedNamesMap = new HashMap<Name, List<IndexedName>>();

        IndexedName() {
        }
    }
}
