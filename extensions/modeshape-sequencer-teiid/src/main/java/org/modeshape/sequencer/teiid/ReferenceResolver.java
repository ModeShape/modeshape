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
package org.modeshape.sequencer.teiid;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ReferenceFactory;
import org.modeshape.graph.property.UuidFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 */
public class ReferenceResolver {

    public static final Map<String, String> STANDARD_DATA_TYPE_URLS_TO_NAMES;
    public static final Map<UUID, String> STANDARD_DATA_TYPE_URLS_BY_UUID;
    public static final Map<String, UUID> STANDARD_DATA_TYPE_UUIDS_BY_NAMES;
    protected static final TextEncoder ENCODER = new Jsr283Encoder();
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
        Map<UUID, String> dataTypesByUuid = new HashMap<UUID, String>();
        Map<String, UUID> dataTypeUuidsByName = new HashMap<String, UUID>();
        for (Map.Entry<String, String> entry : dataTypes.entrySet()) {
            String url = entry.getKey();
            String name = entry.getValue();
            try {
                UUID uuid = UUID.fromString(url.substring(sdtUrl.length()));
                dataTypesByUuid.put(uuid, name);
                dataTypeUuidsByName.put(name, uuid);
            } catch (Throwable e) {
                e.printStackTrace();
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

    private final Multimap<Path, UUID> unresolved = ArrayListMultimap.create();
    private final Map<UUID, Path> mmuuidToNodePath = new HashMap<UUID, Path>();
    private final Map<UUID, UUID> mmuuidToNodeUuid = new HashMap<UUID, UUID>();
    private final ValueFactory<String> stringFactory;
    private final ReferenceFactory referenceFactory;
    private final PathFactory pathFactory;
    private final UuidFactory uuidFactory;

    public ReferenceResolver( ExecutionContext context ) {
        ValueFactories valueFactories = context.getValueFactories();
        this.referenceFactory = valueFactories.getWeakReferenceFactory();
        this.uuidFactory = valueFactories.getUuidFactory();
        this.pathFactory = valueFactories.getPathFactory();
        this.stringFactory = valueFactories.getStringFactory();
    }

    public void clear() {
        mmuuidToNodePath.clear();
        mmuuidToNodeUuid.clear();
        unresolved.clear();
    }

    public void recordXmiUuid( UUID xmiUuid,
                               Path actualPath ) {
        mmuuidToNodePath.put(xmiUuid, actualPath);
    }

    public void recordXmiUuidToJcrUuid( UUID xmiUuid,
                                        UUID jcrUuid ) {
        if (xmiUuid != null) mmuuidToNodeUuid.put(xmiUuid, jcrUuid);
    }

    /**
     * @return unresolved
     */
    public Multimap<Path, UUID> getUnresolved() {
        return unresolved;
    }

    public UUID resolveInternalReference( String href ) {
        if (href == null) {
            return null;
        }
        UUID mmuuid = null;
        if (href.startsWith("mmuuid/")) {
            // It's a local reference ...
            try {
                mmuuid = uuidFactory.create(href.substring(7));
            } catch (ValueFormatException e) {
                // ignore
            }
        } else {
            try {
                mmuuid = uuidFactory.create(href);
            } catch (ValueFormatException e) {
                // ignore
            }
        }
        return mmuuid;
    }

    public ResolvedReference resolve( Path ownerPath,
                                      Name attributeName,
                                      String href ) {
        if (href == null) {
            return null;
        }
        // First check the standard data types ...
        String name = STANDARD_DATA_TYPE_URLS_TO_NAMES.get(href);
        if (name != null) {
            // If the href contains a UUID, then extract it ...
            int index = href.indexOf("mmuuid/");
            String id = null;
            if (index != -1) {
                id = href.substring(index + "mmuuid/".length());
            } else {
                UUID uuid = STANDARD_DATA_TYPE_UUIDS_BY_NAMES.get(name);
                if (uuid != null) id = uuid.toString();
            }
            return new ResolvedReference(href, name, id);
        }
        UUID mmuuid = resolveInternalReference(href);
        if (mmuuid == null) {
            // Not an internal reference ...
            int index = href.indexOf("mmuuid/");
            String id = null;
            if (index != -1) {
                id = href.substring(index + "mmuuid/".length());
            }
            mmuuid = this.uuidFactory.create(id);
            ResolvedReference result = resolve(ownerPath, attributeName, href, mmuuid);
            if (result == null) {
                return new ResolvedReference(href, null, null, id, null);
            }
        }
        return resolve(ownerPath, attributeName, href, mmuuid);
    }

    public ResolvedReference resolve( Path ownerPath,
                                      Name attributeName,
                                      String href,
                                      UUID mmuuid ) {
        if (mmuuid == null) {
            return null;
        }
        Path path = mmuuidToNodePath.get(mmuuid);
        Reference weakReference = referenceFactory.create(mmuuidToNodeUuid.get(mmuuid));
        if (path == null && ownerPath != null && attributeName != null) {
            // Record this unresolved reference for later resolution ...
            Path propPath = pathFactory.create(ownerPath, attributeName);
            unresolved.put(propPath, mmuuid);
        }
        if (path != null || weakReference != null) {
            String resolvedName = path != null ? stringFactory.create(path.getLastSegment()) : null;
            return new ResolvedReference(href, resolvedName, path, mmuuid.toString(), weakReference);
        }
        return null;
    }

    public static class ResolvedReference {
        private final String href;
        private final Reference reference;
        private final String name;
        private final Path path;
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
                                  Path path,
                                  String id,
                                  Reference reference ) {
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

        public Path getPath() {
            return path;
        }

        public String getId() {
            return id;
        }

        public Reference getWeakReferenceValue() {
            return reference;
        }
    }

}
