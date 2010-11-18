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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
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
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.sequencer.teiid.ReferenceResolver.ResolvedReference;
import org.modeshape.sequencer.teiid.VdbModel.ValidationMarker;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.DiagramLexicon;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.lexicon.XsiLexicon;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 */
@NotThreadSafe
public class XmiModelReader extends XmiGraphReader {

    protected static final TextEncoder ENCODER = new Jsr283Encoder();

    private final Path modelRootPath;
    private final String originalFile;
    private final Map<Name, ModelObjectHandler> handlers = new HashMap<Name, ModelObjectHandler>();
    private ModelObjectHandler defaultHandler;
    private final Map<UUID, PropertySet> mmuuidToPropertySet = new HashMap<UUID, PropertySet>();
    protected final boolean useXmiUuidsAsJcrUuids;
    protected final DefaultProperties defaults;
    protected ReferenceResolver resolver;
    private VdbModel vdbModel;
    private String sha1;

    /**
     * Create a reader for XMI model files.
     * 
     * @param parentPath the path under which the model node should be created by this reaader; may be null if the path is to be
     *        determined
     * @param modelName the name of the model file
     * @param pathToModelFile the path of the original model file
     * @param subgraph the subgraph containing the model contents
     * @param generateShortTypeNames true if shorter node type and property type names should be used (where the local part of
     *        'shorter' names will not begin with the prefix), or false if the names should be used as is
     * @param useXmiUuidsAsJcrUuids true if the 'xmi:uuid' values are to be used as the 'jcr:uuid' values, or false if new UUIDs
     *        should be generated for 'jcr:uuid'; should only be true if a model can appear in a repository workspace only once
     * @param vdbModel information about the model, or null if the original model file is stand-alone and was not contained in a
     *        VDB
     */
    public XmiModelReader( Path parentPath,
                           Name modelName,
                           String pathToModelFile,
                           Subgraph subgraph,
                           boolean generateShortTypeNames,
                           boolean useXmiUuidsAsJcrUuids,
                           VdbModel vdbModel ) {
        super(subgraph, generateShortTypeNames);
        try {
            this.defaults = DefaultProperties.getDefaults();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.originalFile = pathToModelFile;
        this.modelRootPath = parentPath != null ? pathFactory.create(parentPath, modelName) : pathFactory.createRelativePath(modelName);
        this.useXmiUuidsAsJcrUuids = useXmiUuidsAsJcrUuids;
        this.vdbModel = vdbModel;
        setResolver(null);
        prepare();
    }

    protected void prepare() {
        // Register some of the namespaces we'll need ...
        namespaces.register(DiagramLexicon.Namespace.PREFIX, DiagramLexicon.Namespace.URI);
        namespaces.register(TransformLexicon.Namespace.PREFIX, TransformLexicon.Namespace.URI);
        namespaces.register(JdbcLexicon.Namespace.PREFIX, JdbcLexicon.Namespace.URI);
        namespaces.register(RelationalLexicon.Namespace.PREFIX, RelationalLexicon.Namespace.URI);
        namespaces.register(XsiLexicon.Namespace.PREFIX, XsiLexicon.Namespace.URI);

        replaceTypeName("relational:importSetting", "jdbcs:imported");
        replaceTypeName("relational:result", "relational:procedureResult");
        replaceTypeName("relational:parameter", "relational:procedureParameter");
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

    /**
     * @param resolver Sets resolver to the specified value.
     */
    public void setResolver( ReferenceResolver resolver ) {
        this.resolver = resolver != null ? resolver : new ReferenceResolver(subgraph.getGraph().getContext());
    }

    /**
     * @param sha1 Sets sha1 to the specified value.
     */
    public void setSha1Hash( String sha1 ) {
        this.sha1 = sha1;
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
        if (!writePhase0(output)) return false;
        if (!writePhase1(output)) return false;
        if (!writePhase2(output)) return false;
        return writePhase3(output);
    }

    public boolean writePhase0( SequencerOutput output ) {
        clearHandlers();
        registerDefaultHandler(new SkipBranchHandler());
        registerHandler("annotations", new AnnotationHandler());
        registerHandler("transformationMappings", new TransformationHandler());

        // Walk the subgraph and accumulate the map from mmuuid to generated node UUID ...
        for (SubgraphNode node : subgraph) {
            UUID mmUuid = xmiUuidFor(node);
            UUID nodeUuid = uuidFor(node);
            resolver.recordXmiUuidToJcrUuid(mmUuid, nodeUuid);
        }
        return true;
    }

    public boolean writePhase1( SequencerOutput output ) {

        // Figure out the primary metamodel before we do anything else ...
        SubgraphNode xmi = subgraph.getRoot();
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
        Name primaryType = vdbModel != null ? VdbLexicon.MODEL : XmiLexicon.MODEL;
        if (modelAnnotation != null) {
            UUID xmiUuid = xmiUuidFor(modelAnnotation);
            resolver.recordXmiUuid(xmiUuid, modelRootPath);
            PropertySet props = propertiesFor(xmiUuid, true);
            props.add(JcrLexicon.PRIMARY_TYPE, primaryType);
            props.add(JcrLexicon.MIXIN_TYPES, CoreLexicon.MODEL, JcrMixLexicon.REFERENCEABLE, XmiLexicon.REFERENCEABLE);
            props.add(XmiLexicon.VERSION, firstValue(xmi, "xmi:version", 2.0));
            props.add(CoreLexicon.PRIMARY_METAMODEL_URI, getCurrentNamespaceUri());
            props.add(CoreLexicon.MODEL_TYPE, firstValue(modelAnnotation, "modelType"));
            props.add(CoreLexicon.PRODUCER_NAME, firstValue(modelAnnotation, "ProducerName"));
            props.add(CoreLexicon.PRODUCER_VERSION, firstValue(modelAnnotation, "ProducerVersion"));
            props.writeTo(output, modelRootPath);
            output.setProperty(modelRootPath, JcrLexicon.UUID, uuidFor(modelAnnotation));
            output.setProperty(modelRootPath, XmiLexicon.UUID, xmiUuid);
        } else {
            // Create the root node for this XMI model ...
            output.setProperty(modelRootPath, JcrLexicon.PRIMARY_TYPE, primaryType);
            output.setProperty(modelRootPath,
                               JcrLexicon.MIXIN_TYPES,
                               CoreLexicon.MODEL,
                               JcrMixLexicon.REFERENCEABLE,
                               XmiLexicon.REFERENCEABLE);

            output.setProperty(modelRootPath, XmiLexicon.VERSION, firstValue(xmi, "xmi:version", 2.0));
        }

        if (originalFile != null) {
            output.setProperty(modelRootPath, CoreLexicon.ORIGINAL_FILE, originalFile);
        }
        if (sha1 != null) {
            output.setProperty(modelRootPath, ModeShapeLexicon.SHA1, sha1);
        }

        // Write out the VDB-related information (if applicable) ...
        if (vdbModel != null) {
            output.setProperty(modelRootPath, VdbLexicon.VISIBLE, vdbModel.isVisible());
            output.setProperty(modelRootPath, VdbLexicon.CHECKSUM, vdbModel.getChecksum());
            output.setProperty(modelRootPath, VdbLexicon.BUILT_IN, vdbModel.isBuiltIn());
            output.setProperty(modelRootPath, VdbLexicon.PATH_IN_VDB, vdbModel.getPathInVdb());
            String translator = vdbModel.getSourceTranslator();
            String sourceName = vdbModel.getSourceName();
            String jndiName = vdbModel.getSourceJndiName();
            if (translator != null) output.setProperty(modelRootPath, VdbLexicon.SOURCE_TRANSLATOR, translator);
            if (sourceName != null) output.setProperty(modelRootPath, VdbLexicon.SOURCE_NAME, sourceName);
            if (jndiName != null) output.setProperty(modelRootPath, VdbLexicon.SOURCE_JNDI_NAME, jndiName);
            if (!vdbModel.getProblems().isEmpty()) {
                Path markersPath = path(modelRootPath, VdbLexicon.MARKERS);
                output.setProperty(markersPath, JcrLexicon.PRIMARY_TYPE, VdbLexicon.MARKERS);
                for (ValidationMarker marker : vdbModel.getProblems()) {
                    Path markerPath = path(markersPath, VdbLexicon.MARKER);
                    output.setProperty(markerPath, JcrLexicon.PRIMARY_TYPE, VdbLexicon.MARKER);
                    output.setProperty(markerPath, VdbLexicon.SEVERITY, marker.getSeverity().name());
                    output.setProperty(markerPath, VdbLexicon.PATH, marker.getPath());
                    output.setProperty(markerPath, VdbLexicon.MESSAGE, marker.getMessage());
                }
            }
        }

        if (modelAnnotation != null) {
            registerHandler("modelImports", new ModelImportHandler());
            // Process the model imports ...
            for (Location modelImportLocation : modelAnnotation.getChildren()) {
                SubgraphNode modelImport = subgraph.getNode(modelImportLocation);
                processObject(modelRootPath, modelImport, output);
            }
        }
        return true;
    }

    public boolean writePhase2( SequencerOutput output ) {
        clearHandlers();
        registerDefaultHandler(new DefaultModelObjectHandler());
        registerHandler("mmcore:ModelAnnotation", new SkipBranchHandler());
        registerHandler("mmcore:AnnotationContainer", new SkipBranchHandler());
        registerHandler("transform:TransformationContainer", new SkipBranchHandler());
        registerHandler("diagram:DiagramContainer", new SkipBranchHandler());
        registerHandler("importSettings", new DefaultModelObjectHandler(JdbcLexicon.IMPORTED));

        SubgraphNode xmi = subgraph.getRoot();
        // Process the other top-level model objects ...
        for (Location objectLocation : xmi.getChildren()) {
            SubgraphNode modelObject = subgraph.getNode(objectLocation);
            processObject(modelRootPath, modelObject, output);
        }
        return true;
    }

    public boolean writePhase3( SequencerOutput output ) {

        // Now attempt to resolve any references that were previously unresolved ...
        for (Entry<Path, Collection<UUID>> entry : resolver.getUnresolved().asMap().entrySet()) {
            Path propPath = entry.getKey();
            Collection<UUID> mmuuids = entry.getValue();
            Path path = propPath.getParent();
            Name propName = propPath.getLastSegment().getName();
            Object[] names = new String[mmuuids.size()];
            int i = 0;
            for (UUID mmuuid : mmuuids) {
                ResolvedReference ref = resolver.resolve(null, null, null, mmuuid);
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
            if (!(handler instanceof SkipNodeHandler)) {
                // Before we do anything else, write out any PropertySet for this object ...
                UUID mmuuid = xmiUuidFor(node);
                PropertySet props = propertiesFor(mmuuid, false);
                if (props != null) {
                    props.writeTo(output, actualPath);
                }

                // Register the newly created object ...
                UUID xmiUuid = xmiUuidFor(node);
                resolver.recordXmiUuid(xmiUuid, actualPath);
            }

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

        private void addDefaultsForType( Collection<Object> nodeTypes ) {
            if (nodeTypes == null || nodeTypes.isEmpty()) return;
            for (Object nodeType : nodeTypes) {
                String nodeTypeString = reader.stringFrom(nodeType);
                for (Map.Entry<String, Object> defaultValue : reader.defaults.getDefaultsFor(nodeTypeString).entrySet()) {
                    Name name = reader.nameFrom(defaultValue.getKey());
                    if (propsByName.containsKey(name)) continue;
                    add(name, defaultValue.getValue());
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

            // Add any missing properties that have defaults ...
            addDefaultsForType(propsByName.get(JcrLexicon.PRIMARY_TYPE));
            addDefaultsForType(propsByName.get(JcrLexicon.MIXIN_TYPES));

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
                        resolved = reader.resolver.resolve(null, null, href);
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

            // Get the propert set for this object ...
            UUID mmuuid = reader.xmiUuidFor(node);
            PropertySet propSet = mmuuid != null ? reader.propertiesFor(mmuuid, true) : reader.createPropertySet();

            // Figure out the mixins, which will include the primary type and any other mixins ...
            Property primaryType = node.getProperty(JcrLexicon.PRIMARY_TYPE);
            Property mixinTypes = node.getProperty(JcrLexicon.MIXIN_TYPES);
            String xsiTypeValue = reader.firstValue(node, "xsi:type");
            Name pt = xsiTypeValue != null ? reader.typeNameFrom(reader.nameFrom(xsiTypeValue)) : reader.typeNameFrom(reader.nameFrom(reader.stringFrom(primaryType.getFirstValue())));
            if (JcrNtLexicon.UNSTRUCTURED.equals(pt)) {
                if (mixinTypes != null) {
                    for (Object mixinTypeName : mixinTypes) {
                        Name mixinName = reader.nameFrom(reader.stringFrom(mixinTypeName));
                        propSet.add(JcrLexicon.MIXIN_TYPES, reader.typeNameFrom(mixinName));
                    }
                } else {
                    // There are no mixin types, so let's assume that the object had no 'name' attribute, that
                    // the type was placed in the name (and thus no mixin types), and that the name is also
                    // the mixin type ...
                    propSet.add(JcrLexicon.MIXIN_TYPES, reader.typeNameFrom(path.getLastSegment().getName()));
                }
            } else {
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
            propSet.add(JcrLexicon.PRIMARY_TYPE, pt);

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
                    // PropertySet setter = reader.createPropertySet();
                    for (UUID uuid : references) {
                        ResolvedReference resolved = reader.resolver.resolve(path, name, null, uuid);
                        propSet.addRef(name, resolved);
                    }
                    propSet.writeTo(output, path);
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
            output.setProperty(path, JcrLexicon.PRIMARY_TYPE, CoreLexicon.IMPORT);
            output.setProperty(path, CoreLexicon.PRIMARY_METAMODEL_URI, primaryMetamodelUri);
            output.setProperty(path, CoreLexicon.MODEL_TYPE, reader.firstValue(node, "modelType"));
            output.setProperty(path, CoreLexicon.PATH, reader.firstValue(node, "path"));
            output.setProperty(path, JcrLexicon.UUID, reader.uuidFor(node));
            output.setProperty(path, XmiLexicon.UUID, reader.firstValue(node, "xmi:uuid"));
            output.setProperty(path, JcrLexicon.MIXIN_TYPES, JcrMixLexicon.REFERENCEABLE, XmiLexicon.REFERENCEABLE);

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
                UUID mmuuid = reader.resolver.resolveInternalReference(hrefToTransformedObject);
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
                        UUID nestedMmuuid = reader.resolver.resolveInternalReference(outputHref);
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
