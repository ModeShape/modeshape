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
package org.modeshape.sequencer.teiid.xmi;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * A XMI file reader.
 */
public class XmiReader {

    static final Logger LOGGER = Logger.getLogger(XmiReader.class);

    private static String getIndent( final int stackSize ) {
        final StringBuilder indent = new StringBuilder();

        for (int i = 0; i < stackSize; ++i) {
            if (i == 0) {
                continue;
            }

            indent.append("  ");
        }

        return indent.toString();
    }

    private final List<XmiElement> elements = new ArrayList<XmiElement>();
    private boolean keepReading = true;
    private final Map<String, String> namespaces = new HashMap<String, String>(); // namespaces declared in file
    private final String path; // never empty
    private final Stack<XmiElement> stack = new Stack<XmiElement>();

    /**
     * @param path the path to the XMI file being read (cannot be <code>null</code> or empty)
     */
    protected XmiReader( final String path ) {
        CheckArg.isNotEmpty(path, "path");
        this.path = path;
    }

    /**
     * @param element the element the attribute is being added to (cannot be <code>null</code>)
     * @param attribute the attribute being added (cannot be <code>null</code>)
     */
    protected void addAttribute( final XmiElement element,
                                 final XmiAttribute attribute ) {
        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(attribute, "attribute");
        element.addAttribute(attribute);
    }

    /**
     * @param parent the element the child element is being added to (cannot be <code>null</code>)
     * @param newChild the element being added as a child (cannot be <code>null</code>)
     */
    protected void addChild( final XmiElement parent,
                             final XmiElement newChild ) {
        CheckArg.isNotNull(parent, "parent");
        CheckArg.isNotNull(newChild, "newChild");
        parent.addChild(newChild);
    }

    /**
     * @param newElement the root level element being added (cannot be <code>null</code>)
     */
    protected void addElement( final XmiElement newElement ) {
        CheckArg.isNotNull(newElement, "newElement");
        this.elements.add(newElement);
    }

    /**
     * @param streamReader the stream reader (cannot be <code>null</code>)
     * @param element the element whose attributes are being created from the stream (cannot be <code>null</code>)
     */
    protected void createAttributes( final XMLStreamReader streamReader,
                                     final XmiElement element ) {
        CheckArg.isNotNull(streamReader, "streamReader");
        CheckArg.isNotNull(element, "element");

        for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
            final XmiAttribute newAttribute = new XmiAttribute(streamReader.getAttributeName(i).getLocalPart());
            newAttribute.setNamespacePrefix(streamReader.getAttributePrefix(i));
            newAttribute.setNamespaceUri(streamReader.getAttributeNamespace(i));
            newAttribute.setValue(streamReader.getAttributeValue(i));
            addAttribute(element, newAttribute);

            LOGGER.debug("{0}  added attribute: '{1}'", getIndent(this.stack.size()), newAttribute);
        }
    }

    /**
     * @return the root-level child elements found in the model (never <code>null</code>)
     */
    public List<XmiElement> getElements() {
        return this.elements;
    }

    /**
     * @param namespaceUri the namespace URI of the elements being requested (cannot be <code>null</code> or empty)
     * @return the child elements with the specified namespace URI (never <code>null</code>)
     */
    protected List<XmiElement> getElements( final String namespaceUri ) {
        CheckArg.isNotEmpty(namespaceUri, "namespaceUri");
        final List<XmiElement> namespacedElements = new ArrayList<XmiElement>();

        for (final XmiElement element : getElements()) {
            if (namespaceUri.equals(element.getNamespaceUri())) {
                namespacedElements.add(element);
            }
        }

        return namespacedElements;
    }

    /**
     * Map has keys of namespaces prefixes and values of namespace URIs.
     *
     * @return the namespaces declared in the file (never <code>null</code>)
     */
    protected Map<String, String> getNamespaces() {
        return this.namespaces;
    }

    /**
     * @return the path to the resource (can be <code>null</code> or empty)
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @return the resource name (never <code>null</code>)
     */
    public String getResourceName() {
        final int index = this.path.lastIndexOf(File.separator);

        if (index == -1) {
            return this.path;
        }

        return this.path.substring(index + 1);
    }

    /**
     * @return the number of elements currently in the stack
     */
    protected int getStackSize() {
        return this.stack.size();
    }

    /**
     * Handles a stream {@link javax.xml.stream.XMLStreamConstants#CHARACTERS} event.
     *
     * @param streamReader the stream reader (cannot be <code>null</code>)
     */
    protected void handleCharacters( final XMLStreamReader streamReader ) {
        CheckArg.isNotNull(streamReader, "streamReader");
        final String value = streamReader.getText();

        if (!StringUtil.isBlank(value) && !this.stack.isEmpty()) {
            this.stack.peek().setValue(value);
        } else if (!StringUtil.isBlank(value)) {
            LOGGER.debug("**** unhandled XmiReader CHARACTERS event type. Character={0}", streamReader.getText());
        }
    }

    /**
     * Handles a stream {@link javax.xml.stream.XMLStreamConstants#END_ELEMENT} event.
     *
     * @param streamReader the stream reader (cannot be <code>null</code>)
     * @return the XMI element popped off the stack (never <code>null</code>)
     */
    protected XmiElement handleEndElement( final XMLStreamReader streamReader ) {
        CheckArg.isNotNull(streamReader, "streamReader");
        final XmiElement popped = pop(streamReader);

        LOGGER.debug("{0}end:elementName={1}, popped={2}", getIndent(this.stack.size() + 1), streamReader.getLocalName(), popped);
        return popped;
    }

    /**
     * Handles a stream events not covered by other methods.
     *
     * @param streamReader the stream reader (cannot be <code>null</code>)
     */
    protected void handleOtherEvents( final XMLStreamReader streamReader ) {
        CheckArg.isNotNull(streamReader, "streamReader");
        LOGGER.debug("**** unhandled XmiReader event of type {0}", streamReader.getEventType());
    }

    /**
     * Handles a stream {@link javax.xml.stream.XMLStreamConstants#START_ELEMENT} event.
     *
     * @param streamReader the stream reader (cannot be <code>null</code>)
     * @return the new XMI element pushed onto the stack (never <code>null</code>)
     * @throws Exception if there is a problem reading from the stream
     */
    protected XmiElement handleStartElement( final XMLStreamReader streamReader ) throws Exception {
        CheckArg.isNotNull(streamReader, "streamReader");

        final XmiElement element = new XmiElement(streamReader.getLocalName());
        element.setNamespaceUri(streamReader.getNamespaceURI());
        element.setNamespacePrefix(streamReader.getPrefix());

        if (streamReader.hasText()) {
            element.setValue(streamReader.getElementText());
        }

        push(element);

        LOGGER.debug("{0}startElement: {1}", getIndent(this.stack.size()), element);

        // create attributes
        createAttributes(streamReader, element);

        if (XmiLexicon.ModelId.XMI_TAG.equals(streamReader.getLocalName())) {
            for (int i = 0, size = streamReader.getNamespaceCount(); i < size; ++i) {
                final String nsPrefix = streamReader.getNamespacePrefix(i);
                final String nsUri = streamReader.getNamespaceURI(i);
                this.namespaces.put(nsPrefix, nsUri);

                LOGGER.debug("registered namespace {0}={1} to model", nsPrefix, nsUri);
            }
        }

        return element;
    }

    /**
     * @param streamReader the stream reader (cannot be <code>null</code>)
     * @return the XMI element poppoed off the stack (never <code>null</code>)
     */
    protected XmiElement pop( final XMLStreamReader streamReader ) {
        CheckArg.isNotNull(streamReader, "streamReader");
        return this.stack.pop();
    }

    /**
     * @param element the XMI element being pushed onto the stack (cannot be <code>null</code>)
     */
    protected void push( final XmiElement element ) {
        CheckArg.isNotNull(element, "element");

        // add as root element
        if (this.stack.isEmpty()) {
            addElement(element);
        } else { // add to parent
            addChild(this.stack.peek(), element);
        }

        this.stack.push(element);
    }

    /**
     * @param stream the input stream (cannot be <code>null</code>)
     * @return the root elements found (never <code>null</code>)
     * @throws Exception if there is a problem reading the stream
     */
    protected final List<XmiElement> read( final InputStream stream ) throws Exception {
        CheckArg.isNotNull(stream, "stream");

        final XMLInputFactory factory = XMLInputFactory.newInstance();
        final XMLStreamReader streamReader = factory.createXMLStreamReader(stream);

        while (this.keepReading && streamReader.hasNext()) {
            streamReader.next();

            if (streamReader.isStartElement()) {
                handleStartElement(streamReader);
            } else if (streamReader.isEndElement()) {
                handleEndElement(streamReader);
            } else if (streamReader.isCharacters()) {
                handleCharacters(streamReader);
            } else {
                handleOtherEvents(streamReader);
            }
        }

        return this.elements;
    }

    protected void stop() {
        this.keepReading = false;
    }
}
