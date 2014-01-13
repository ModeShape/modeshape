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
package org.modeshape.common.xml;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.common.SystemFailureException;
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

    public static final String DEFAULT_ENCODING = "UTF-8";

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
    private final OutputStreamWriter writer;

    /**
     * The XML namespace prefixes that are currently mapped
     */
    private final Map<String, String> mappedPrefixes;

    /**
     * The XML declaration information written to the output.
     */
    private final String declaration;

    public StreamingContentHandler( OutputStream os ) {
        this(os, Collections.<String>emptyList());
    }

    public StreamingContentHandler( OutputStream os,
                                    Collection<String> unexportableNamespaces ) {
        try {
            this.writer = new OutputStreamWriter(os, DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // This should never happen ...
            throw new SystemFailureException(e);
        }
        this.unexportableNamespaces = unexportableNamespaces;
        this.mappedPrefixes = new HashMap<String, String>();
        this.declaration = "version=\"1.0\" encoding=\"" + DEFAULT_ENCODING + "\"";
    }

    public StreamingContentHandler( OutputStream os,
                                    Collection<String> unexportableNamespaces,
                                    String encoding ) throws UnsupportedEncodingException {
        this.writer = new OutputStreamWriter(os, encoding);
        this.unexportableNamespaces = unexportableNamespaces;
        this.mappedPrefixes = new HashMap<String, String>();
        if (encoding == null || encoding.length() == 0) encoding = "UTF-8";
        this.declaration = "version=\"1.0\" encoding=\"" + encoding + "\"";
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
        emit("<?xml " + declaration + "?>");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new SAXException(e);
        }
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
     */
    private void emit( String text ) throws SAXException {

        try {
            if (LOG_TO_CONSOLE) {
                System.out.print(text);
            }

            writer.write(text);
        } catch (IOException ioe) {
            throw new SAXException(ioe);
        }
    }
}
