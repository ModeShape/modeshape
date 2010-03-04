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
package org.modeshape.common.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.XmlValueEncoder;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class that adapts an arbitrary, open {@link OutputStream} to the {@link ContentHandler} interface. SAX events invoked on this
 * object will be translated into their corresponding XML text and written to the output stream.
 */
public class StreamingContentHandler extends DefaultHandler {

    /** Debug setting that allows all output to be written to {@link System#out}. */
    private static final boolean LOG_TO_CONSOLE = false;

    /**
     * Encoder to properly escape XML attribute values
     * 
     * @see XmlValueEncoder
     */
    private final TextEncoder VALUE_ENCODER = new XmlValueEncoder();

    /**
     * The list of XML namespaces that are predefined and should not be exported by the content handler.
     */
    private final Collection<String> unexportableNamespaces;

    /**
     * The output stream to which the XML will be written
     */
    private final OutputStream os;

    /**
     * The XML namespace prefixes that are currently mapped
     */
    private final Map<String, String> mappedPrefixes;

    public StreamingContentHandler( OutputStream os ) {
        this(os, Collections.<String>emptyList());
    }

    public StreamingContentHandler( OutputStream os,
                                    Collection<String> unexportableNamespaces ) {
        this.os = os;
        this.unexportableNamespaces = unexportableNamespaces;
        mappedPrefixes = new HashMap<String, String>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters( char[] ch,
                            int start,
                            int length ) throws SAXException {
        emit(VALUE_ENCODER.encode(new String(ch, start, length)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException {
        emit("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
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
                              Attributes attributes ) throws SAXException {
        emit("<");
        emit(name);

        for (Map.Entry<String, String> mapping : mappedPrefixes.entrySet()) {
            emit(" xmlns:");
            emit(mapping.getKey());
            emit("=\"");
            emit(mapping.getValue());
            emit("\"");
        }

        mappedPrefixes.clear();

        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                emit(" ");
                emit(attributes.getQName(i));
                emit("=\"");
                emit(VALUE_ENCODER.encode(attributes.getValue(i)));
                emit("\"");
            }
        }

        emit(">");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement( String uri,
                            String localName,
                            String name ) throws SAXException {
        emit("</");
        emit(name);
        emit(">");
        if (LOG_TO_CONSOLE) System.out.println();

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    @Override
    public void startPrefixMapping( String prefix,
                                    String uri ) {
        if (!unexportableNamespaces.contains(prefix)) {
            mappedPrefixes.put(prefix, uri);
        }
    }

    /**
     * Writes the given text to the output stream for this {@link StreamingContentHandler}.
     * 
     * @param text the text to output
     * @throws SAXException if there is an error writing to the stream
     * @see StreamingContentHandler#os
     */
    private void emit( String text ) throws SAXException {

        try {
            if (LOG_TO_CONSOLE) {
                System.out.print(text);
            }

            os.write(text.getBytes());
        } catch (IOException ioe) {
            throw new SAXException(ioe);
        }
    }
}
