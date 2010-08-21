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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrMixLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ReferenceFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.DiagramLexicon;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.lexicon.XsiLexicon;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 */
@NotThreadSafe
public class XmiModelReader extends XmiGraphReader {

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

    private final Name modelName;
    private final Path modelPath;
    private final ReferenceFactory referenceFactory;
    private final Map<UUID, UUID> mmuuidToNodeUuid = new HashMap<UUID, UUID>();
    private final Map<UUID, Path> mmuuidToNodePath = new HashMap<UUID, Path>();
    private final Map<Name, ModelObjectHandler> handlers = new HashMap<Name, ModelObjectHandler>();
    private final Multimap<Path, UUID> unresolved = ArrayListMultimap.create();
    private ModelObjectHandler defaultHandler;
    private final Map<UUID, PropertySet> mmuuidToPropertySet = new HashMap<UUID, PropertySet>();
    protected final boolean useXmiUuidsAsJcrUuids;

    /**
     * @param modelPath
     * @param subgraph
     * @param generateShortTypeNames
     * @param useXmiUuidsAsJcrUuids
     */
    public XmiModelReader( Path modelPath,
                           Subgraph subgraph,
                           boolean generateShortTypeNames,
                           boolean useXmiUuidsAsJcrUuids ) {
        super(subgraph, generateShortTypeNames);
        this.referenceFactory = valueFactories.getWeakReferenceFactory();
        this.modelPath = modelPath;
        Path pathWithModelName = this.modelPath.getLastSegment().getName().equals(JcrLexicon.CONTENT) ? this.modelPath.getParent() : this.modelPath;
        Name modelName = pathWithModelName.getLastSegment().getName();
        // Remove the ".xmi" from the end of the local name (the root will have a 'xmi:model' mixin type, so they can easily be
        // found) ...
        String modelNameWithoutExtension = modelName.getLocalName().replaceAll("\\.xmi$", "");
        this.modelName = nameFactory.create(modelName.getNamespaceUri(), modelNameWithoutExtension);
        this.useXmiUuidsAsJcrUuids = useXmiUuidsAsJcrUuids;
        prepare();
    }

    protected void prepare() {
        // Register some of the namespaces we'll need ...
        namespaces.register(DiagramLexicon.Namespace.PREFIX, DiagramLexicon.Namespace.URI);
        namespaces.register(TransformLexicon.Namespace.PREFIX, TransformLexicon.Namespace.URI);
        namespaces.register(JdbcLexicon.Namespace.PREFIX, JdbcLexicon.Namespace.URI);
        namespaces.register(RelationalLexicon.Namespace.PREFIX, RelationalLexicon.Namespace.URI);
        namespaces.register(XsiLexicon.Namespace.PREFIX, XsiLexicon.Namespace.URI);

        replaceTypeName("relational:importSetting", "jdbcs:importedFrom");
        replaceTypeName("jdbcs:jdbcSource", "jdbcs:source");
    }

    protected void registerHandler( String name,
                                    ModelObjectHandler handler ) {
        registerHandler(nameFactory.create(name), handler);
    }

    protected void registerHandler( Name name,
                                    ModelObjectHandler handler ) {
        handlers.put(name, handler);
    }

    protected void registerDefaultHandler( ModelObjectHandler handler ) {
        defaultHandler = handler;
    }

    protected void clearHandlers() {
        this.handlers.clear();
    }

    protected PropertySet propertiesFor( UUID mmuuid,
                                         boolean createIfMissing ) {
        PropertySet result = mmuuidToPropertySet.get(mmuuid);
        if (result == null && createIfMissing) {
            result = new PropertySet(this);
            mmuuidToPropertySet.put(mmuuid, result);
        }
        return result;
    }

    @Override
    protected UUID uuidFor( Node node ) {
        return this.useXmiUuidsAsJcrUuids ? xmiUuidFor(node) : super.uuidFor(node);
    }

    public boolean isAcceptedPrimaryMetamodel( String uri ) {
        return RelationalLexicon.Namespace.URI.equals(uri);
    }

    public boolean write( SequencerOutput output ) {
        if (!writePhase1(output)) return false;
        if (!writePhase2(output)) return false;
        return writePhase3(output);
    }

    public boolean writePhase1( SequencerOutput output ) {
        clearHandlers();
        registerDefaultHandler(new SkipBranchHandler());
        registerHandler("annotations", new AnnotationHandler());
        registerHandler("transformationMappings", new TransformationHandler());

        // Walk the subgraph and accumulate the map from mmuuid to generated node UUID ...
        mmuuidToNodeUuid.clear();
        for (SubgraphNode node : subgraph) {
            UUID mmUuid = xmiUuidFor(node);
            UUID nodeUuid = uuidFor(node);
            if (mmUuid != null) mmuuidToNodeUuid.put(mmUuid, nodeUuid);
        }

        // Figure out the primary metamodel before we do anything else ...
        SubgraphNode xmi = subgraph.getRoot();
        Path modelRootPath = relativePathFrom(modelName);
        SubgraphNode modelAnnotation = xmi.getNode("mmcore:ModelAnnotation");
        if (modelAnnotation != null) {
            String primaryMetamodelUri = firstValue(modelAnnotation, "primaryMetamodelUri");
            if (!isAcceptedPrimaryMetamodel(primaryMetamodelUri)) return false;
            setCurrentNamespaceUri(primaryMetamodelUri);
        }

        // Process the annotations and transformations, before we write out any nodes.
        // This is because these two steps read in the properties that are projected
        // (via PropertySet objects keyed by the mmuuid) onto the real objects.
        // Since the SequencerOutput does not let us output properties on a node
        // afte we've moved on to other nodes, we need to read these 'projected' properties first.
        SubgraphNode annotationContainer = xmi.getNode("mmcore:AnnotationContainer");
        if (annotationContainer != null) {
            // Process the annotations ...
            for (Location objectLocation : annotationContainer.getChildren()) {
                SubgraphNode modelObject = subgraph.getNode(objectLocation);
                processObject(modelRootPath, modelObject, output);
            }
        }
        SubgraphNode transformationContainer = xmi.getNode("transform:TransformationContainer");
        if (transformationContainer != null) {
            // Process the annotations ...
            for (Location objectLocation : transformationContainer.getChildren()) {
                SubgraphNode modelObject = subgraph.getNode(objectLocation);
                processObject(modelRootPath, modelObject, output);
            }
        }

        // Process the model annotation first (if present) ...
        if (modelAnnotation != null) {
            UUID xmiUuid = xmiUuidFor(modelAnnotation);
            mmuuidToNodePath.put(xmiUuid, modelRootPath);
            PropertySet props = propertiesFor(xmiUuid, true);
            props.add(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
            props.add(JcrLexicon.MIXIN_TYPES,
                      XmiLexicon.MODEL,
                      CoreLexicon.MODEL,
                      JcrMixLexicon.REFERENCEABLE,
                      XmiLexicon.REFERENCEABLE);
            props.add(XmiLexicon.VERSION, firstValue(xmi, "xmi:version", 2.0));
            props.add(CoreLexicon.PRIMARY_METAMODEL_URI, getCurrentNamespaceUri());
            props.add(CoreLexicon.MODEL_TYPE, firstValue(modelAnnotation, "modelType"));
            props.writeTo(output, modelRootPath);
            output.setProperty(modelRootPath, JcrLexicon.UUID, uuidFor(modelAnnotation));
            output.setProperty(modelRootPath, XmiLexicon.UUID, xmiUuid);

            // Process the model imports ...
            for (Location modelImportLocation : modelAnnotation.getChildren()) {
                SubgraphNode modelImport = subgraph.getNode(modelImportLocation);
                processObject(modelRootPath, modelImport, output);
            }
        } else {
            // Create the root node for this XMI model ...
            output.setProperty(modelRootPath, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
            output.setProperty(modelRootPath,
                               JcrLexicon.MIXIN_TYPES,
                               XmiLexicon.MODEL,
                               CoreLexicon.MODEL,
                               JcrMixLexicon.REFERENCEABLE,
                               XmiLexicon.REFERENCEABLE);

            output.setProperty(modelRootPath, XmiLexicon.VERSION, firstValue(xmi, "xmi:version", 2.0));
        }
        output.setProperty(modelRootPath, CoreLexicon.MODEL_FILE, stringFrom(modelPath));
        return true;
    }

    public boolean writePhase2( SequencerOutput output ) {
        clearHandlers();
        registerDefaultHandler(new DefaultModelObjectHandler());
        registerHandler("modelImports", new ModelImportHandler());
        registerHandler("mmcore:ModelAnnotation", new SkipBranchHandler());
        registerHandler("mmcore:AnnotationContainer", new SkipBranchHandler());
        registerHandler("transform:TransformationContainer", new SkipBranchHandler());
        registerHandler("diagram:DiagramContainer", new SkipBranchHandler());
        registerHandler("importSettings", new DefaultModelObjectHandler(JdbcLexicon.IMPORTED_FROM));

        SubgraphNode xmi = subgraph.getRoot();
        Path modelRootPath = relativePathFrom(modelName);
        // Process the other top-level model objects ...
        for (Location objectLocation : xmi.getChildren()) {
            SubgraphNode modelObject = subgraph.getNode(objectLocation);
            processObject(modelRootPath, modelObject, output);
        }
        return true;
    }

    public boolean writePhase3( SequencerOutput output ) {

        // Now attempt to resolve any references that were previously unresolved ...
        for (Path propPath : unresolved.keySet()) {
            Path path = propPath.getParent();
            Name propName = propPath.getLastSegment().getName();
            Collection<UUID> mmuuids = unresolved.get(propPath);
            Object[] names = new String[mmuuids.size()];
            int i = 0;
            for (UUID mmuuid : mmuuids) {
                ResolvedReference ref = resolve(null, null, null, mmuuid);
                if (ref.getName() == null) {
                    names = null;
                    break;
                }
                names[i++] = ref.getName();
            }
            if (names != null && !useXmiUuidsAsJcrUuids) {
                Name refNameName = nameForResolvedName(propName);
                output.setProperty(path, refNameName, names);
            }
        }
        return true;
    }

    protected Name nameFromKey( String keyName ) {
        int index = keyName.indexOf(':');
        if (index != -1) {
            String prefix = keyName.substring(0, index);
            if (namespaces.getNamespaceForPrefix(prefix) != null) {
                return nameFactory.create(keyName);
            }
            // Otherwise, escape all ':' ...
            keyName = ENCODER.encode(prefix);
        }
        return nameFactory.create(keyName);
    }

    protected Path processObject( Path parentPath,
                                  SubgraphNode node,
                                  SequencerOutput output ) {
        Location location = node.getLocation();
        Path path = path(parentPath, location.getPath().getLastSegment());
        ModelObjectHandler handler = findHandler(node);
        Path actualPath = handler.process(path, node, subgraph, this, output);

        if (actualPath != null) {
            // Before we do anything else, write out any PropertySet for this object ...
            UUID mmuuid = xmiUuidFor(node);
            PropertySet props = propertiesFor(mmuuid, false);
            if (props != null) {
                props.writeTo(output, actualPath);
            }

            // Register the newly created object ...
            UUID xmiUuid = xmiUuidFor(node);
            mmuuidToNodePath.put(xmiUuid, actualPath);

            // Process the nested objects ...
            for (Location childLocation : node.getChildren()) {
                SubgraphNode childObject = subgraph.getNode(childLocation);
                if (childObject == null) continue;
                processObject(actualPath, childObject, output);
            }
        }

        return actualPath;
    }

    protected ModelObjectHandler findHandler( Node node ) {
        // Find the handler for the 'jcr:primaryType' as literally specified ...
        String primaryTypeStr = firstValue(node, JcrLexicon.PRIMARY_TYPE);
        Name primaryType = nameFactory.create(primaryTypeStr);
        ModelObjectHandler handler = handlers.get(primaryType);
        if (handler == null && primaryTypeStr.indexOf(':') == -1) {
            // Find the handler for the 'jcr:primaryType' using the current namespace ...
            primaryType = nameFactory.create(currentNamespaceUri, primaryTypeStr);
            handler = handlers.get(primaryType);
            if (handler == null) {
                // Find the handler for the 'jcr:primaryType' using the 'mmcore' namespace ...
                primaryType = nameFactory.create(CoreLexicon.Namespace.URI, primaryTypeStr);
                handler = handlers.get(primaryType);
            }
        }
        if (handler == null) {
            // Try the object name ...
            Location objectLocation = node.getLocation();
            Name name = objectLocation.getPath().getLastSegment().getName();
            handler = handlers.get(name);
        }
        if (handler == null) {
            // Use the default handler ...
            handler = defaultHandler;
        }
        return handler;
    }

    protected UUID resolveInternalReference( String href ) {
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

    protected ResolvedReference resolve( Path ownerPath,
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
            return new ResolvedReference(href, null, null, id, null);
        }
        return resolve(ownerPath, attributeName, href, mmuuid);
    }

    protected ResolvedReference resolve( Path ownerPath,
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
            String resolvedName = path != null ? stringFrom(path.getLastSegment()) : null;
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

    protected PropertySet createPropertySet() {
        return new PropertySet(this);
    }

    public static class PropertySet {
        private final Multimap<Name, Object> refsByName = ArrayListMultimap.create();
        private final Multimap<Name, Object> propsByName = ArrayListMultimap.create();
        private final Map<String, String> tags = new HashMap<String, String>();
        private final XmiModelReader reader;

        protected PropertySet( XmiModelReader reader ) {
            this.reader = reader;
        }

        public void addTag( String name,
                            String value ) {
            if (value != null) {
                tags.put(name, value);
            }
        }

        public void addRef( Name attributeName,
                            ResolvedReference resolved ) {
            if (resolved != null) {
                refsByName.put(attributeName, resolved);
            }
        }

        public void addRef( Name attributeName,
                            String href ) {
            if (href != null) {
                refsByName.put(attributeName, href);
            }
        }

        public void add( Name attributeName,
                         Object value ) {
            if (value != null) {
                propsByName.put(attributeName, value);
            }
        }

        public void add( Name attributeName,
                         Object... values ) {
            if (values != null && values.length != 0) {
                for (Object value : values) {
                    propsByName.put(attributeName, value);
                }
            }
        }

        public void writeTo( SequencerOutput output,
                             Path path ) {
            // Figure out which tags are applied to this object ...
            boolean tagProps = false;
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                String key = tag.getKey();
                String[] parts = key.split(":", 2);
                if (parts.length < 2) {
                    // Just write the tag on the tagged object ...
                    Name tagName = reader.nameFrom(ENCODER.encode(key));
                    add(tagName, tag.getValue());
                    tagProps = true;
                }
            }
            if (tagProps) {
                add(JcrLexicon.MIXIN_TYPES, CoreLexicon.TAGS);
            }

            // Now write out the individual properties ...
            for (Name propName : propsByName.keySet()) {
                Collection<Object> values = propsByName.get(propName);
                Object[] valueArray = values.toArray(new Object[values.size()]);
                output.setProperty(path, propName, valueArray);
            }

            // And write out the reference properties ...
            for (Name propName : refsByName.keySet()) {
                Collection<Object> values = refsByName.get(propName);
                for (Object value : values) {
                    ResolvedReference resolved = null;
                    if (value instanceof String) {
                        String href = (String)value;
                        resolved = reader.resolve(null, null, href);
                    } else if (value instanceof ResolvedReference) {
                        resolved = (ResolvedReference)value;
                    }
                    if (resolved == null) continue;
                    if (!reader.useXmiUuidsAsJcrUuids) {
                        if (resolved.getHref() != null) {
                            // And record the reference value ...
                            Name hrefName = reader.nameForHref(propName);
                            output.setProperty(path, hrefName, resolved.getHref());
                        }

                        // Record the identifier value ...
                        if (resolved.getId() != null) {
                            Name idName = reader.nameForResolvedId(propName);
                            output.setProperty(path, idName, resolved.getId());
                        }

                        // Record the name of the resolved object ...
                        String resolvedName = resolved.getName();
                        if (resolvedName != null) {
                            Name refNameName = reader.nameForResolvedName(propName);
                            output.setProperty(path, refNameName, resolvedName);
                        }
                    }
                    // Record the resolved reference value ...
                    Reference weakReference = resolved.getWeakReferenceValue();
                    if (weakReference != null) {
                        Name refName = reader.nameForResolvedReference(propName);
                        output.setProperty(path, refName, weakReference);
                    }
                }
            }

            // And finally write out the tags that have to be placed onto child nodes ...
            for (Map.Entry<String, String> tag : tags.entrySet()) {
                String key = tag.getKey();
                String[] parts = key.split(":", 2);
                if (parts.length >= 2) {
                    // Use the first part of the tag name as the name of a child node ...
                    Name childName = reader.nameFactory.create(ENCODER.encode(parts[0]));
                    Path childPath = reader.path(path, childName);
                    output.setProperty(childPath, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
                    output.setProperty(childPath, JcrLexicon.MIXIN_TYPES, CoreLexicon.TAGS);
                    // And the rest of the key as the property name ...
                    Name tagName = reader.nameFactory.create(ENCODER.encode(parts[1]));
                    String value = tag.getValue();
                    output.setProperty(childPath, tagName, value);
                }
            }
        }
    }

    public static interface ModelObjectHandler {
        Path process( Path path,
                      SubgraphNode node,
                      Subgraph subgraph,
                      XmiModelReader reader,
                      SequencerOutput output );
    }

    public static class DefaultModelObjectHandler implements ModelObjectHandler {
        private final Set<Name> mixinTypeNames = new HashSet<Name>();

        public DefaultModelObjectHandler( Name... requiredMixinTypeNames ) {
            for (Name name : requiredMixinTypeNames) {
                mixinTypeNames.add(name);
            }
        }

        @Override
        public Path process( Path path,
                             SubgraphNode node,
                             Subgraph subgraph,
                             XmiModelReader reader,
                             SequencerOutput output ) {
            if (node.getProperty("href") != null) {
                // This is a reference that should have been handled in the processing of the parent node
                return null;
            }
            output.setProperty(path, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);

            // Get the propert set for this object ...
            UUID mmuuid = reader.xmiUuidFor(node);
            PropertySet propSet = mmuuid != null ? reader.propertiesFor(mmuuid, true) : reader.createPropertySet();

            // Figure out the mixins, which will include the primary type and any other mixins ...
            Property primaryType = node.getProperty(JcrLexicon.PRIMARY_TYPE);
            Property mixinTypes = node.getProperty(JcrLexicon.MIXIN_TYPES);
            String xsiTypeValue = reader.firstValue(node, "xsi:type");
            Name pt = reader.typeNameFrom(reader.nameFrom(reader.stringFrom(primaryType.getFirstValue())));
            if ("relational:View".equals(xsiTypeValue)) {
                int x = 0;
            }
            Name xsiType = xsiTypeValue != null ? reader.typeNameFrom(reader.nameFrom(xsiTypeValue)) : null;
            if (JcrNtLexicon.UNSTRUCTURED.equals(pt)) {
                if (mixinTypes != null) {
                    for (Object mixinTypeName : mixinTypes) {
                        Name mixinName = reader.nameFrom(reader.stringFrom(mixinTypeName));
                        propSet.add(JcrLexicon.MIXIN_TYPES, reader.typeNameFrom(mixinName));
                    }
                } else {
                    if (xsiType != null) {
                        propSet.add(JcrLexicon.MIXIN_TYPES, xsiType);
                    } else {
                        // There are no mixin types, so let's assume that the object had no 'name' attribute, that
                        // the type was placed in the name (and thus no mixin types), and that the name is also
                        // the mixin type ...
                        propSet.add(JcrLexicon.MIXIN_TYPES, reader.typeNameFrom(path.getLastSegment().getName()));
                    }
                }
            } else {
                if (xsiType != null) {
                    propSet.add(JcrLexicon.MIXIN_TYPES, reader.typeNameFrom(xsiType));
                } else {
                    propSet.add(JcrLexicon.MIXIN_TYPES, pt);
                }
                if (mixinTypes != null) {
                    for (Object mixinTypeName : mixinTypes) {
                        Name mixinName = reader.nameFrom(reader.stringFrom(mixinTypeName));
                        propSet.add(JcrLexicon.MIXIN_TYPES, reader.typeNameFrom(mixinName));
                    }
                }
            }
            propSet.add(JcrLexicon.MIXIN_TYPES, JcrMixLexicon.REFERENCEABLE);
            if (node.getProperty(XmiLexicon.UUID) != null) {
                propSet.add(JcrLexicon.MIXIN_TYPES, XmiLexicon.REFERENCEABLE);
            }

            // Now process the properties ...
            for (Property property : node.getProperties()) {
                Name name = property.getName();
                if (name.equals(JcrLexicon.PRIMARY_TYPE) || name.equals(JcrLexicon.MIXIN_TYPES) || name.equals(XsiLexicon.TYPE)) {
                    continue;
                } else if (name.equals(ModeShapeLexicon.UUID) || name.equals(JcrLexicon.UUID)) {
                    output.setProperty(path, JcrLexicon.UUID, reader.uuidFor(node));
                    continue;
                } else if (name.equals(XmiLexicon.UUID)) {
                    output.setProperty(path, name, reader.xmiUuidFor(node));
                    continue;
                } else if (name.getNamespaceUri().isEmpty()) {
                    name = reader.nameFrom(name.getLocalName());
                }
                List<UUID> references = reader.references(property);
                if (references != null) {
                    PropertySet setter = reader.createPropertySet();
                    for (UUID uuid : references) {
                        ResolvedReference resolved = reader.resolve(path, name, null, uuid);
                        setter.addRef(name, resolved);
                    }
                    setter.writeTo(output, path);
                } else {
                    // No references; just regular values ...
                    output.setProperty(path, name, property.getValuesAsArray());
                }
            }

            // Look at the children to see if there are any references ...
            for (Location childLocation : node.getChildren()) {
                SubgraphNode child = subgraph.getNode(childLocation);
                if (child.getProperty("href") != null) {
                    // The child node is a reference, so figure out the EObject reference name ...
                    Name attributeName = reader.nameFrom(childLocation.getPath().getLastSegment().getName());
                    String href = reader.firstValue(child, "href");
                    propSet.addRef(attributeName, href);
                }
            }
            if (mmuuid == null) {
                propSet.writeTo(output, path);
            }
            return path;
        }

        protected Object[] filterMixinTypeNames( Iterable<Object> names ) {
            Set<Name> result = new HashSet<Name>(this.mixinTypeNames);
            for (Object name : names) {
                result.add((Name)name);
            }
            return result.toArray(new Object[result.size()]);
        }
    }

    public static class ModelImportHandler implements ModelObjectHandler {
        @Override
        public Path process( Path path,
                             SubgraphNode node,
                             Subgraph subgraph,
                             XmiModelReader reader,
                             SequencerOutput output ) {
            String primaryMetamodelUri = reader.firstValue(node, "primaryMetamodelUri");
            output.setProperty(path, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
            output.setProperty(path, CoreLexicon.PRIMARY_METAMODEL_URI, primaryMetamodelUri);
            output.setProperty(path, CoreLexicon.MODEL_TYPE, reader.firstValue(node, "modelType"));
            output.setProperty(path, CoreLexicon.PATH, reader.firstValue(node, "path"));
            output.setProperty(path, JcrLexicon.UUID, reader.uuidFor(node));
            output.setProperty(path, XmiLexicon.UUID, reader.firstValue(node, "xmi:uuid"));
            output.setProperty(path,
                               JcrLexicon.MIXIN_TYPES,
                               CoreLexicon.IMPORT,
                               JcrMixLexicon.REFERENCEABLE,
                               XmiLexicon.REFERENCEABLE);

            return path;
        }
    }

    public static class AnnotationHandler implements ModelObjectHandler {
        @Override
        public Path process( Path path,
                             SubgraphNode annotation,
                             Subgraph subgraph,
                             XmiModelReader reader,
                             SequencerOutput output ) {
            List<UUID> mmuuids = reader.references(annotation.getProperty("annotatedObject"));
            if (mmuuids != null) {
                for (UUID mmuuid : mmuuids) {
                    PropertySet props = reader.propertiesFor(mmuuid, true);
                    // Process the description ...
                    String desc = reader.firstValue(annotation, "description");
                    props.add(CoreLexicon.DESCRIPTION, reader.firstValue(annotation, "description"));
                    props.add(JcrLexicon.MIXIN_TYPES, CoreLexicon.ANNOTATED);
                    // Process the tags ...
                    for (Location tagLocation : annotation.getChildren()) {
                        Name childName = tagLocation.getPath().getLastSegment().getName();
                        if (childName.getLocalName().equals("tags")) {
                            SubgraphNode tag = subgraph.getNode(tagLocation);
                            String key = reader.firstValue(tag, "key");
                            String value = reader.firstValue(tag, "value");
                            props.addTag(key, value);
                        }
                    }
                }
            }
            return null; // don't process any children
        }
    }

    public static class TransformationHandler implements ModelObjectHandler {
        @Override
        public Path process( Path path,
                             SubgraphNode transformation,
                             Subgraph subgraph,
                             XmiModelReader reader,
                             SequencerOutput output ) {
            String hrefToTransformedObject = reader.firstValue(transformation, "target");
            if (hrefToTransformedObject != null) {
                UUID mmuuid = reader.resolveInternalReference(hrefToTransformedObject);
                PropertySet props = reader.propertiesFor(mmuuid, true);

                props.add(JcrLexicon.MIXIN_TYPES, TransformLexicon.TRANSFORMED);

                // Get the transformation details ...
                Path helperNestedNodePath = reader.path(transformation.getLocation().getPath(), "helper/nested");
                SubgraphNode helperNested = subgraph.getNode(helperNestedNodePath);
                if (helperNested != null) {
                    props.add(TransformLexicon.SELECT_SQL, reader.firstValue(helperNested, "selectSql"));
                    props.add(TransformLexicon.INSERT_SQL, reader.firstValue(helperNested, "insertSql"));
                    props.add(TransformLexicon.UPDATE_SQL, reader.firstValue(helperNested, "updateSql"));
                    props.add(TransformLexicon.DELETE_SQL, reader.firstValue(helperNested, "deleteSql"));
                    props.add(TransformLexicon.INSERT_ALLOWED, reader.firstValue(helperNested, "insertAllowed", true));
                    props.add(TransformLexicon.UPDATE_ALLOWED, reader.firstValue(helperNested, "updateAllowed", true));
                    props.add(TransformLexicon.DELETE_ALLOWED, reader.firstValue(helperNested, "deleteAllowed", true));
                    props.add(TransformLexicon.INSERT_SQL_DEFAULT, reader.firstValue(helperNested, "insertSqlDefault", true));
                    props.add(TransformLexicon.UPDATE_SQL_DEFAULT, reader.firstValue(helperNested, "updateSqlDefault", true));
                    props.add(TransformLexicon.DELETE_SQL_DEFAULT, reader.firstValue(helperNested, "deleteSqlDefault", true));
                    // props.add(TransformLexicon.OUTPUT_LOCKED, firstValue(helperNested,"outputLocked",false));
                }

                for (Location childLocation : transformation.getChildren()) {
                    Name childName = childLocation.getPath().getLastSegment().getName();
                    SubgraphNode child = subgraph.getNode(childLocation);
                    if (childName.getLocalName().equals("inputs")) {
                        // Record the inputs to the transformed object ...
                        // Now collect the inputs to the transformation ...
                        String inputHref = reader.firstValue(child, "href");
                        if (inputHref == null) continue;
                        Name name = TransformLexicon.INPUTS;
                        props.addRef(name, inputHref);
                    } else if (childName.getLocalName().equals("nested")) {
                        // Record the nested transformation ...
                        // Find the output (the transformed) object ...
                        String outputHref = reader.firstValue(child, "outputs");
                        UUID nestedMmuuid = reader.resolveInternalReference(outputHref);
                        PropertySet nestedProps = reader.propertiesFor(nestedMmuuid, true);
                        for (Location inputLocation : child.getChildren()) {
                            SubgraphNode inputNode = subgraph.getNode(inputLocation);
                            String inputHref = reader.firstValue(inputNode, "href");
                            if (inputHref == null) continue;
                            Name name = TransformLexicon.INPUTS;
                            nestedProps.addRef(name, inputHref);
                            nestedProps.add(JcrLexicon.MIXIN_TYPES, TransformLexicon.TRANSFORMED);
                        }
                    }
                }
            }
            return null; // don't process any children
        }
    }

    public static class SkipNodeHandler implements ModelObjectHandler {
        @Override
        public Path process( Path path,
                             SubgraphNode node,
                             Subgraph subgraph,
                             XmiModelReader reader,
                             SequencerOutput output ) {
            return path;
        }
    }

    public static class SkipBranchHandler implements ModelObjectHandler {
        @Override
        public Path process( Path path,
                             SubgraphNode node,
                             Subgraph subgraph,
                             XmiModelReader reader,
                             SequencerOutput output ) {
            return null;
        }
    }
}
