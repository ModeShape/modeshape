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
package org.modeshape.sequencer.epub;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility for extracting Metadata from EPUB format.
 */
public class EpubMetadata {

    static final String[] MIME_TYPE_STRINGS = { "application/epub+zip" };

    // The XML namespace for the Dublin Core schema.
    static final String DUBLIN_CORE_PREFIX = "dc";
    static final String DUBLIN_CORE_URI = "http://purl.org/dc/elements/1.1/";

    private String title;
    private String creator;
    private String contributor;
    private String language;
    private String identifier;
    private String subject;
    private String description;
    private String publisher;
    private String date;

    private InputStream in;
    private DataInput din;

    /*
     * Check that given file is supported by this sequencer.
     */
    public boolean check() {
        try {
            ZipInputStream zip = new ZipInputStream(in);
            ZipEntry entry = null;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith("/content.opf")) {
                    // get content file
                    ByteArrayOutputStream content = new ByteArrayOutputStream();
                    byte[] bytes = new byte[(int) entry.getSize()];
                    int read;
                    while ((read = zip.read(bytes, 0, bytes.length)) != -1) {
                        content.write(bytes, 0, read);
                      }
                    content.flush();

                    // get metadata elements
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    factory.setNamespaceAware(true);
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(new ByteArrayInputStream(content.toByteArray()));

                    XPathFactory xPathfactory = XPathFactory.newInstance();
                    XPath xpath = xPathfactory.newXPath();
                    xpath.setNamespaceContext(new SimpleNSC(DUBLIN_CORE_PREFIX, DUBLIN_CORE_URI));
                    XPathExpression expr = xpath.compile("//" + DUBLIN_CORE_PREFIX + ":*");
                    NodeList metadata = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

                    for (int i = 0; i < metadata.getLength(); i++) {
                        Node node = metadata.item(i);
                        if (node.getLocalName().equals("title")) {
                            title = node.getTextContent();
                        } else if (node.getLocalName().equals("creator")) {
                            creator = node.getTextContent();
                        } else if (node.getLocalName().equals("contributor")) {
                            contributor = node.getTextContent();
                        } else if (node.getLocalName().equals("language")) {
                            language = node.getTextContent();
                        } else if (node.getLocalName().equals("identifier")) {
                            identifier = node.getTextContent();
                        } else if (node.getLocalName().equals("subject")) {
                            subject = node.getTextContent();
                        } else if (node.getLocalName().equals("description")) {
                            description = node.getTextContent();
                        } else if (node.getLocalName().equals("publisher")) {
                            publisher = node.getTextContent();
                        } else if (node.getLocalName().equals("date")) {
                            date = node.getTextContent();
                        }
                    }
                }
            }
            zip.close();

            return true;
        } catch (Exception e) {
            // log
        }

        return false;
    }

    public String getTitle() {
        return title;
    }

    public String getCreator() {
        return creator;
    }

    public String getContributor() {
        return contributor;
    }

    public String getLanguage() {
        return language;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getSubject() {
        return subject;
    }

    public String getDescription() {
        return description;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getDate() {
        return date;
    }

    private int read( byte[] a ) throws IOException {
        if (in != null) {
            return in.read(a);
        }
        din.readFully(a);
        return a.length;
    }


    /**
     * Set the input stream to the argument stream (or file). Note that {@link java.io.RandomAccessFile} implements
     * {@link java.io.DataInput}.
     *
     * @param dataInput the input stream to read from
     */
    public void setInput( DataInput dataInput ) {
        din = dataInput;
        in = null;
    }

    /**
     * Set the input stream to the argument stream (or file).
     *
     * @param inputStream the input stream to read from
     */
    public void setInput( InputStream inputStream ) {
        in = inputStream;
        din = null;
    }

    /**
     * Simple NamespaceContext which can resolve single namespace.
     */
    private class SimpleNSC implements NamespaceContext {

        private final String prefix;
        private final String uri;

        public SimpleNSC(String prefix, String uri) {
            this.prefix = prefix;
            this.uri = uri;
        }

        public String getNamespaceURI(String prefix) {
          if (prefix.equals(prefix)) {
            return uri;
          } else {
            return null;
          }
        }

        @Override
        public String getPrefix(String arg0) {
            return prefix;
        }

        @Override
        public Iterator<String> getPrefixes(String arg0) {
            return Arrays.asList(prefix).iterator();
        }
      }
}
