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
package org.modeshape.sequencer.teiid.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.common.collection.ArrayListMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.sequencer.teiid.xmi.XmiAttribute;
import org.modeshape.sequencer.teiid.xmi.XmiElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class ReferenceResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceResolver.class);
    private static final String MMUUID = "mmuuid/";

    public static final Map<String, String> STANDARD_DATA_TYPE_URLS_TO_NAMES;
    public static final Map<String, String> STANDARD_DATA_TYPE_URLS_BY_UUID;
    public static final Map<String, String> STANDARD_DATA_TYPE_UUIDS_BY_NAMES;

    static {
        Map<String, String> dataTypes = new HashMap<String, String>();
        // Really old models have simple data types hrefs that contain UUIDs ...
        String sdtUrl = "http://www.metamatrix.com/metamodels/SimpleDatatypes-instance#mmuuid:";
        dataTypes.put(sdtUrl + "4ca2ae00-3a95-1e20-921b-eeee28353879", "NMTOKEN");
        dataTypes.put(sdtUrl + "4df43700-3b13-1e20-921b-eeee28353879", "normalizedString");
        dataTypes.put(sdtUrl + "3425cb80-d844-1e20-9027-be6d2c3b8b3a", "token");
        dataTypes.put(sdtUrl + "d4d980c0-e623-1e20-8c26-a038c6ed7576", "language");
        dataTypes.put(sdtUrl + "e66c4600-e65b-1e20-8c26-a038c6ed7576", "Name");
        dataTypes.put(sdtUrl + "ac00e000-e676-1e20-8c26-a038c6ed7576", "NCName");
        dataTypes.put(sdtUrl + "4b0f8500-e6a6-1e20-8c26-a038c6ed7576", "NMTOKENS");
        dataTypes.put(sdtUrl + "dd33ff40-e6df-1e20-8c26-a038c6ed7576", "IDREF");
        dataTypes.put(sdtUrl + "88b13dc0-e702-1e20-8c26-a038c6ed7576", "ID");
        dataTypes.put(sdtUrl + "9fece300-e71a-1e20-8c26-a038c6ed7576", "ENTITY");
        dataTypes.put(sdtUrl + "3c99f780-e72d-1e20-8c26-a038c6ed7576", "IDREFS");
        dataTypes.put(sdtUrl + "20360100-e742-1e20-8c26-a038c6ed7576", "ENTITIES");
        dataTypes.put(sdtUrl + "45da3500-e78f-1e20-8c26-a038c6ed7576", "integer");
        dataTypes.put(sdtUrl + "cbdd6e40-b9d2-1e21-8c26-a038c6ed7576", "nonPositiveInteger");
        dataTypes.put(sdtUrl + "0e081200-b8a4-1e21-b812-969c8fc8b016", "nonNegativeInteger");
        dataTypes.put(sdtUrl + "86d29280-b8d3-1e21-b812-969c8fc8b016", "negativeInteger");
        dataTypes.put(sdtUrl + "8cdee840-b900-1e21-b812-969c8fc8b016", "long");
        dataTypes.put(sdtUrl + "33add3c0-b98d-1e21-b812-969c8fc8b016", "int");
        dataTypes.put(sdtUrl + "5bbcf140-b9ae-1e21-b812-969c8fc8b016", "short");
        dataTypes.put(sdtUrl + "26dc1cc0-b9c8-1e21-b812-969c8fc8b016", "byte");
        dataTypes.put(sdtUrl + "1cbbd380-b9ea-1e21-b812-969c8fc8b016", "positiveInteger");
        dataTypes.put(sdtUrl + "54b98780-ba14-1e21-b812-969c8fc8b016", "unsignedLong");
        dataTypes.put(sdtUrl + "badcbd80-ba63-1e21-b812-969c8fc8b016", "unsignedInt");
        dataTypes.put(sdtUrl + "327093c0-ba88-1e21-b812-969c8fc8b016", "unsignedShort");
        dataTypes.put(sdtUrl + "cff745c0-baa2-1e21-b812-969c8fc8b016", "unsignedByte");
        dataTypes.put(sdtUrl + "bf6c34c0-c442-1e24-9b01-c8207cd53eb7", "string");
        dataTypes.put(sdtUrl + "dc476100-c483-1e24-9b01-c8207cd53eb7", "boolean");
        dataTypes.put(sdtUrl + "569dfa00-c456-1e24-9b01-c8207cd53eb7", "decimal");
        dataTypes.put(sdtUrl + "d86b0d00-c48a-1e24-9b01-c8207cd53eb7", "float");
        dataTypes.put(sdtUrl + "1f18b140-c4a3-1e24-9b01-c8207cd53eb7", "double");
        dataTypes.put(sdtUrl + "3b892180-c4a7-1e24-9b01-c8207cd53eb7", "time");
        dataTypes.put(sdtUrl + "65dcde00-c4ab-1e24-9b01-c8207cd53eb7", "date");
        dataTypes.put(sdtUrl + "62472700-a064-1e26-9b08-d6079ebe1f0d", "char");
        dataTypes.put(sdtUrl + "822b9a40-a066-1e26-9b08-d6079ebe1f0d", "biginteger");
        dataTypes.put(sdtUrl + "f2249740-a078-1e26-9b08-d6079ebe1f0d", "bigdecimal");
        dataTypes.put(sdtUrl + "6d9809c0-a07e-1e26-9b08-d6079ebe1f0d", "timestamp");
        dataTypes.put(sdtUrl + "051a0640-b4e8-1e26-9f33-b76fd9d5fa79", "object");
        dataTypes.put(sdtUrl + "559646c0-4941-1ece-b22b-f49159d22ad3", "clob");
        dataTypes.put(sdtUrl + "5a793100-1836-1ed0-ba0f-f2334f5fbf95", "blob");
        dataTypes.put(sdtUrl + "43f5274e-55e1-1f87-ba1c-eea49143eb32", "XMLLiteral");
        dataTypes.put(sdtUrl + "28d98540-b3e7-1e2a-9a03-beb8638ffd21", "duration");
        dataTypes.put(sdtUrl + "5c69dec0-b3ea-1e2a-9a03-beb8638ffd21", "dateTime");
        dataTypes.put(sdtUrl + "17d08040-b3ed-1e2a-9a03-beb8638ffd21", "gYearMonth");
        dataTypes.put(sdtUrl + "b02c7600-b3f2-1e2a-9a03-beb8638ffd21", "gYear");
        dataTypes.put(sdtUrl + "6e604140-b3f5-1e2a-9a03-beb8638ffd21", "gMonthDay");
        dataTypes.put(sdtUrl + "860b7dc0-b3f8-1e2a-9a03-beb8638ffd21", "gDay");
        dataTypes.put(sdtUrl + "187f5580-b3fb-1e2a-9a03-beb8638ffd21", "gMonth");
        dataTypes.put(sdtUrl + "6247ec80-e8a4-1e2a-b433-fb67ea35c07e", "anyURI");
        dataTypes.put(sdtUrl + "eeb5d780-e8c3-1e2a-b433-fb67ea35c07e", "QName");
        dataTypes.put(sdtUrl + "3dcaf900-e8dc-1e2a-b433-fb67ea35c07e", "NOTATION");
        dataTypes.put(sdtUrl + "d9998500-ebba-1e2a-9319-8eaa9b2276c7", "hexBinary");
        dataTypes.put(sdtUrl + "b4c99380-ebc6-1e2a-9319-8eaa9b2276c7", "base64Binary");

        // Populate the name-to-UUID mapping ...
        Map<String, String> dataTypesByUuid = new HashMap<String, String>();
        Map<String, String> dataTypeUuidsByName = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : dataTypes.entrySet()) {
            String url = entry.getKey();
            String name = entry.getValue();
            try {
                String uuid = UUID.fromString(url.substring(sdtUrl.length())).toString();
                dataTypesByUuid.put(uuid, name);
                dataTypeUuidsByName.put(name, uuid);
            } catch (IllegalArgumentException e) {
                LOGGER.error("UUID not valid", e);
            }
        }

        // Newer models have simple data types hrefs that contain names ...
        String xsdUrl = "http://www.w3.org/2001/XMLSchema#";
        for (String value : new HashSet<String>(dataTypes.values())) {
            dataTypes.put(xsdUrl + value, value);
        }
        sdtUrl = "http://www.metamatrix.com/metamodels/SimpleDatatypes-instance#";
        for (String value : new HashSet<String>(dataTypes.values())) {
            dataTypes.put(sdtUrl + value, value);
        }

        STANDARD_DATA_TYPE_URLS_TO_NAMES = Collections.unmodifiableMap(dataTypes);
        STANDARD_DATA_TYPE_URLS_BY_UUID = Collections.unmodifiableMap(dataTypesByUuid);
        STANDARD_DATA_TYPE_UUIDS_BY_NAMES = Collections.unmodifiableMap(dataTypeUuidsByName);
    }

    private final Multimap<Node, XmiAttribute> unresolved = ArrayListMultimap.create();
    private final Map<String, Node> uuidToNode = new HashMap<String, Node>();
    // private final Map<String, String> mmuuidToNodeUuid = new HashMap<String, String>();
    private final Map<String, XmiElement> uuidToXmiElement = new HashMap<String, XmiElement>();

    // private ValueFactory valueFactory;
    // private final Context context;

    // protected ReferenceResolver( Context context ) {
    // this.valueFactory = context.valueFactory();
    // this.context = context;
    // }

    public void clear() {
        this.uuidToNode.clear();
        // mmuuidToNodeUuid.clear();
        this.unresolved.clear();
        this.uuidToXmiElement.clear();
    }

    public void record( String xmiUuid,
                        final Node node ) {
        final String prefix = "mmuuid:";

        if (xmiUuid.startsWith(prefix)) {
            xmiUuid = xmiUuid.substring(prefix.length() + 1);
        }

        this.uuidToNode.put(xmiUuid, node);
    }

    void recordXmiUuid( final String xmiUuid,
                        final XmiElement xmiElement ) {
        this.uuidToXmiElement.put(xmiUuid, xmiElement);
    }

    Map<String, XmiElement> getUuidMappings() {
        return this.uuidToXmiElement;
    }

    XmiElement getElement( final String uuid ) {
        return this.uuidToXmiElement.get(uuid);
    }

    Node getNode( final String uuid ) {
        return this.uuidToNode.get(uuid);
    }

    /**
     * Extracts the "mmuuid" values from the property if the property is indeed an XMI reference to local objects.
     * 
     * @param property the property
     * @return the list of mmuuid values, or null if this property does not contain any references; never empty
     * @throws RepositoryException if error access property value(s)
     */
    protected List<String> references( Property property ) throws RepositoryException {
        List<String> result = new LinkedList<String>();
        for (Value value : property.getValues()) {
            String str = value.getString();

            if (str.startsWith("mmuuid/")) {
                // It is a local reference ...
                String[] references = str.split("\\s");

                for (String reference : references) {
                    result.add(reference);

                    if (!property.isMultiple() && references.length == 1) {
                        // This is the only property value, and only one reference in it ...
                        return result;
                    }
                }
            } else {
                assert result.isEmpty();
                return null;
            }
        }

        return result;
    }

    //
    // public void recordXmiUuidToJcrUuid( String xmiUuid,
    // String jcrUuid ) {
    // if (!StringUtil.isBlank(xmiUuid)) {
    // mmuuidToNodeUuid.put(xmiUuid, jcrUuid);
    // }
    // }

    /**
     * @return unresolved the unresolved references (never <code>null</code>)
     */
    public Multimap<Node, XmiAttribute> getUnresolved() {
        return unresolved;
    }

    public String resolveInternalReference( String href ) {
        if (href == null) {
            return null;
        }

        String mmuuid = null;

        if (href.startsWith(MMUUID)) {
            // It's a local reference ...
            try {
                mmuuid = UUID.fromString(href.substring(MMUUID.length())).toString();
            } catch (IllegalArgumentException e) {
                // ignore
            }
        } else {
            try {
                mmuuid = UUID.fromString(href).toString();
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }

        return mmuuid;
    }
//
//    public ResolvedReference resolve( Node ownerNode,
//                                      String attributeName,
//                                      String href ) throws Exception {
//        if (href == null) {
//            return null;
//        }
//
//        // First check the standard data types ...
//        String name = STANDARD_DATA_TYPE_URLS_TO_NAMES.get(href);
//
//        if (name != null) {
//            // If the href contains a UUID, then extract it ...
//            int index = href.indexOf(MMUUID);
//            String id = null;
//            if (index != -1) {
//                id = href.substring(index + MMUUID.length());
//            } else {
//                id = STANDARD_DATA_TYPE_UUIDS_BY_NAMES.get(name);
//            }
//            return new ResolvedReference(href, name, id);
//        }
//
//        String mmuuid = resolveInternalReference(href);
//
//        if (mmuuid == null) {
//            // Not an internal reference ...
//            int index = href.indexOf(MMUUID);
//            String id = null;
//
//            if (index != -1) {
//                id = href.substring(index + MMUUID.length());
//            }
//
//            mmuuid = id;
//            ResolvedReference result = resolve(ownerNode, attributeName, href, mmuuid);
//
//            if (result == null) {
//                return new ResolvedReference(href, null, null, id, null);
//            }
//        }
//
//        return resolve(ownerNode, attributeName, href, mmuuid);
//    }
//
//    public ResolvedReference resolve( Node ownerNode,
//                                      String attributeName,
//                                      String href,
//                                      String mmuuid ) throws Exception {
//        if (mmuuid == null) {
//            return null;
//        }
//
//        Node path = getNode(mmuuid);
//        String weakReference = path.getIdentifier(); // get JCR uuid
//
//        if ((path == null) && (ownerNode != null) && (attributeName != null)) {
//            // Record this unresolved reference for later resolution ...
//            String propPath = pathFactory.create(ownerPath, attributeName);
//            this.unresolved.put(propPath, mmuuid);
//        }
//
//        if ((path != null) || (weakReference != null)) {
//            String resolvedName = path != null ? stringFactory.create(path.getLastSegment()) : null;
//            return new ResolvedReference(href, resolvedName, path, mmuuid, weakReference);
//        }
//
//        return null;
//    }

    public static class ResolvedReference {

        private final String href;
        private final String reference;
        private final String name;
        private final Node path;
        private final String id;
        private final boolean standardDataType;

        public ResolvedReference( String href,
                                  String standardDataTypeName,
                                  String id ) {
            this.href = href;
            this.standardDataType = true;
            this.name = standardDataTypeName;
            this.id = id;
            this.reference = null;
            this.path = null;
        }

        public ResolvedReference( String href,
                                  String name,
                                  Node path,
                                  String id,
                                  String reference ) {
            this.href = href;
            this.standardDataType = false;
            this.name = name;
            this.reference = reference;
            this.path = path;
            this.id = id;
        }

        public boolean isStandardDataType() {
            return standardDataType;
        }

        public String getHref() {
            return href;
        }

        public String getName() {
            return name;
        }

        public Node getPath() {
            return path;
        }

        public String getId() {
            return id;
        }

        public String getWeakReferenceValue() {
            return reference;
        }
    }

}
