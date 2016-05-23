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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.modeshape.common.util.IoUtil;
import org.modeshape.common.xml.SimpleNamespaceContext;
import org.modeshape.sequencer.epub.EpubMetadataProperty.AlternateScript;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility for extracting Metadata from EPUB format.
 * 
 * @since 5.1
 */
public class EpubMetadata {

    static final String[] MIME_TYPE_STRINGS = { "application/epub+zip" };

    // The XML namespace for the Dublin Core schema.
    static final String DUBLIN_CORE_PREFIX = "dc";
    static final String DUBLIN_CORE_URI = "http://purl.org/dc/elements/1.1/";

    private List<EpubMetadataProperty> title = new ArrayList<>();
    private List<EpubMetadataProperty> creator = new ArrayList<>();
    private List<EpubMetadataProperty> contributor = new ArrayList<>();
    private List<EpubMetadataProperty> language = new ArrayList<>();
    private List<EpubMetadataProperty> identifier = new ArrayList<>();
    private List<EpubMetadataProperty> description = new ArrayList<>();
    private List<EpubMetadataProperty> publisher = new ArrayList<>();
    private List<EpubMetadataProperty> rights = new ArrayList<>();
    private List<EpubMetadataProperty> date = new ArrayList<>();
    private List<EpubMetadataProperty> alternateScript = new ArrayList<>();

    private InputStream in;

    public EpubMetadata( InputStream inputStream ) {
        this.in = inputStream;
    }

    /*
     * Check that given file is supported by this sequencer.
     * The sequencer first examines the container to get the list of all rootfiles.
     * Then it process each rootfile and extracts the metadata.
     */
    public boolean check() throws Exception {
        // we need to create a copy of the file, because the container file
        // might be at the end of the stream.
        File fileCopy = File.createTempFile("modeshape-sequencer-epub", ".tmp");
        IoUtil.write(in, new BufferedOutputStream(new FileOutputStream(fileCopy)));

        List<String> rootfiles = new ArrayList<>();
        try (ZipInputStream zipStream =
                new ZipInputStream(new FileInputStream(fileCopy))) {
            rootfiles = getRootfiles(zipStream);
        }

        if (!rootfiles.isEmpty()) {
            try (ZipInputStream zipStream =
                    new ZipInputStream(new FileInputStream(fileCopy))) {
                ZipEntry entry = null;
                while ((entry = zipStream.getNextEntry()) != null) {
                    String entryName = entry.getName();
                    if (rootfiles.contains(entryName)) {
                        checkRootfile(zipStream, entry);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("No rootfile package found in given EPUB file.");
        }

        // try to delete the file immediately or on JVM exit
        boolean deleted = false;
        try {
            deleted = fileCopy.delete();
        } catch (SecurityException e) {
            // ignore
        }
        if (!deleted) {
            fileCopy.deleteOnExit();
        }

        return true;
    }

    /**
     * Process the EPUB package and extract the metadata fields.
     */
    private boolean checkRootfile( ZipInputStream zipStream,
                                   ZipEntry entry ) throws Exception {
        List<EpubMetadataProperty> properties = new ArrayList<>();
        Map<String, EpubMetadataProperty> propertiesWithId = new HashMap<>();

        ByteArrayOutputStream content = getZipEntryContent(zipStream, entry);

        // get metadata elements
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(content.toByteArray()));

        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        xpath.setNamespaceContext(new SimpleNamespaceContext().setNamespace(DUBLIN_CORE_PREFIX, DUBLIN_CORE_URI));
        XPathExpression expr = xpath.compile("//" + DUBLIN_CORE_PREFIX + ":*");
        NodeList metadata = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

        for (int i = 0; i < metadata.getLength(); i++) {
            EpubMetadataProperty property = new EpubMetadataProperty();

            Node node = metadata.item(i);
            property.setName(node.getLocalName());
            property.setValue(node.getTextContent());

            Node nodeId = node.getAttributes().getNamedItem("id");
            if (nodeId != null) {
                String id = nodeId.getTextContent();
                if (!propertiesWithId.containsKey(id)) {
                    propertiesWithId.put(id, property);
                }
            } else {
                properties.add(property);
            }
        }

        // read <meta> elements which can refine the properties
        NodeList metadataRefines = doc.getElementsByTagName("meta");
        for (int i = 0; i < metadataRefines.getLength(); i++) {
            Node node = metadataRefines.item(i);
            Node refines = node.getAttributes().getNamedItem("refines");
            if (refines != null) {
                String refinesId = refines.getTextContent().substring(1);
                if (propertiesWithId.containsKey(refinesId)) {
                    EpubMetadataProperty property = propertiesWithId.get(refinesId);

                    Node schemeAttribute = node.getAttributes().getNamedItem("scheme");
                    if (schemeAttribute != null) {
                        property.setScheme(schemeAttribute.getTextContent());
                    }

                    String propertyName = node.getAttributes().getNamedItem("property").getTextContent();
                    if (propertyName.equals("title-type")) {
                        property.setTitleType(node.getTextContent());
                    } else if (propertyName.equals("identifier-type")) {
                        property.setIdentifierType(node.getTextContent());
                    } else if (propertyName.equals("metadata-authority")) {
                        property.setMetadataAuthority(node.getTextContent());
                    } else if (propertyName.equals("role")) {
                        property.setRole(node.getTextContent());
                    } else if (propertyName.equals("display-seq")) {
                        property.setDisplaySeq(Long.parseLong(node.getTextContent()));
                    } else if (propertyName.equals("group-position")) {
                        property.setGroupPosition(Long.parseLong(node.getTextContent()));
                    } else if (propertyName.equals("file-as")) {
                        property.setFileAs(node.getTextContent());
                    } else if (propertyName.equals("alternate-script")) {
                        Node languageAttribute = node.getAttributes().getNamedItem("xml:lang");
                        if (languageAttribute != null) {
                            property.setAlternateScript(new AlternateScript(node.getTextContent(), languageAttribute.getTextContent()));
                        }
                    }
                }
            }
        }

        properties.addAll(propertiesWithId.values());
        for (EpubMetadataProperty prop : properties) {
            String propertyName = prop.getName();
            if (propertyName.equals("identifier")) {
                getIdentifier().add(prop);
            } else if (propertyName.equals("title")) {
                getTitle().add(prop);
            } else if (propertyName.equals("language")) {
                getLanguage().add(prop);
            } else if (propertyName.equals("contributor")) {
                getContributor().add(prop);
            } else if (propertyName.equals("creator")) {
                getCreator().add(prop);
            } else if (propertyName.equals("description")) {
                getDescription().add(prop);
            } else if (propertyName.equals("publisher")) {
                getPublisher().add(prop);
            } else if (propertyName.equals("rights")) {
                getRights().add(prop);
            } else if (propertyName.equals("date")) {
                getDate().add(prop);
            }
        }

        return true;
    }

    /**
     * Parse the container file to get the list of all rootfile packages.
     */
    private List<String> getRootfiles( ZipInputStream zipStream ) throws Exception {
        List<String> rootfiles = new ArrayList<>();
        ZipEntry entry = null;
        while ((entry = zipStream.getNextEntry()) != null) {
            String entryName = entry.getName();
            if (entryName.endsWith("META-INF/container.xml")) {
                ByteArrayOutputStream content = getZipEntryContent(zipStream, entry);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(content.toByteArray()));

                XPathFactory xPathfactory = XPathFactory.newInstance();
                XPath xpath = xPathfactory.newXPath();
                XPathExpression expr = xpath.compile("/container/rootfiles/rootfile");
                NodeList rootfileNodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

                for (int i = 0; i < rootfileNodes.getLength(); i++) {
                    Node node = rootfileNodes.item(i);
                    rootfiles.add(node.getAttributes().getNamedItem("full-path").getNodeValue());
                }
                break;
            }
        }
        return rootfiles;
    }

    /**
     * Read the content of the ZipEntry without closing the stream.
     */
    private ByteArrayOutputStream getZipEntryContent(
            ZipInputStream zipStream,
            ZipEntry entry ) throws IOException {
        try (ByteArrayOutputStream content =
                new ByteArrayOutputStream()) {
            byte[] bytes = new byte[(int) entry.getSize()];
            int read;
            while ((read = zipStream.read(bytes, 0, bytes.length)) != -1) {
                content.write(bytes, 0, read);
            }
            return content;
        }
    }

    public List<EpubMetadataProperty> getTitle() {
        return title;
    }

    public List<EpubMetadataProperty> getCreator() {
        return creator;
    }

    public List<EpubMetadataProperty> getContributor() {
        return contributor;
    }

    public List<EpubMetadataProperty> getLanguage() {
        return language;
    }

    public List<EpubMetadataProperty> getIdentifier() {
        return identifier;
    }

    public List<EpubMetadataProperty> getDescription() {
        return description;
    }

    public List<EpubMetadataProperty> getPublisher() {
        return publisher;
    }

    public List<EpubMetadataProperty> getRights() {
        return rights;
    }

    public List<EpubMetadataProperty> getDate() {
        return date;
    }

    public List<EpubMetadataProperty> getAlternateScript() {
        return alternateScript;
    }

}
