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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.graph.JcrLexicon;
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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * 
 */
@NotThreadSafe
public class XmiModelReader extends XmiGraphReader {

    public static final Map<String, String> STANDARD_DATA_TYPE_URLS_TO_NAMES;
    protected static final TextEncoder ENCODER = new Jsr283Encoder();
    static {
        Map<String, String> dataTypes = new HashMap<String, String>();
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
        String xsdUrl = "http://www.w3.org/2001/XMLSchema#";
        for (String value : new HashSet<String>(dataTypes.values())) {
            dataTypes.put(xsdUrl + value, value);
        }
        STANDARD_DATA_TYPE_URLS_TO_NAMES = Collections.unmodifiableMap(dataTypes);
    }

    private final Name modelName;
    private final Path modelPath;
    private final ReferenceFactory referenceFactory;
    private final Map<UUID, UUID> mmuuidToNodeUuid = new HashMap<UUID, UUID>();
    private final Map<UUID, Path> mmuuidToNodePath = new HashMap<UUID, Path>();
    private final Map<Name, ModelObjectHandler> handlers = new HashMap<Name, ModelObjectHandler>();
    private final Multimap<Path, UUID> unresolved = ArrayListMultimap.create();
    private final ModelObjectHandler defaultHandler;

    /**
     * @param modelPath
     * @param subgraph
     * @param generateShortTypeNames
     */
    public XmiModelReader( Path modelPath,
                           Subgraph subgraph,
                           boolean generateShortTypeNames ) {
        super(subgraph, generateShortTypeNames);
        this.referenceFactory = valueFactories.getWeakReferenceFactory();
        this.modelPath = modelPath;
        Path pathWithModelName = modelPath.getLastSegment().getName().equals(JcrLexicon.CONTENT) ? modelPath.getParent() : modelPath;
        this.modelName = pathWithModelName.getLastSegment().getName();
        this.defaultHandler = new DefaultModelObjectHandler();
        registerHandlers();
    }

    protected void registerHandlers() {
        registerHandler("modelImports", new ModelImportHandler());
        registerHandler("mmcore:AnnotationContainer", new SkipBranchHandler());
        registerHandler("diagram:DiagramContainer", new SkipBranchHandler());
    }

    protected void registerHandler( String name,
                                    ModelObjectHandler handler ) {
        registerHandler(nameFactory.create(name), handler);
    }

    protected void registerHandler( Name name,
                                    ModelObjectHandler handler ) {
        handlers.put(name, handler);
    }

    public void writeTo( SequencerOutput output ) {
        System.out.println(subgraph);

        // Walk the subgraph and accumulate the map from mmuuid to generated node UUID ...
        mmuuidToNodeUuid.clear();
        for (SubgraphNode node : subgraph) {
            UUID nodeUuid = uuidFor(node);
            UUID mmUuid = xmiUuidFor(node);
            if (mmUuid != null) mmuuidToNodeUuid.put(mmUuid, nodeUuid);
        }

        // Create the root node for this XMI model ...
        SubgraphNode xmi = subgraph.getRoot();
        Path modelPath = relativePathFrom(modelName);
        output.setProperty(modelPath, JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED);
        output.setProperty(modelPath, JcrLexicon.MIXIN_TYPES, XmiLexicon.MODEL, CoreLexicon.MODEL);
        output.setProperty(modelPath, XmiLexicon.VERSION, firstValue(xmi, "xmi:version", 2.0));

        // Process the model annotation (if present) ...
        SubgraphNode modelAnnotation = xmi.getNode("mmcore:ModelAnnotation");
        Location modelAnnotationLocation = null;
        if (modelAnnotation != null) {
            modelAnnotationLocation = modelAnnotation.getLocation();
            UUID xmiUuid = xmiUuidFor(modelAnnotation);
            mmuuidToNodePath.put(xmiUuid, modelPath);
            setCurrentNamespaceUri(firstValue(modelAnnotation, "primaryMetamodelUri"));
            output.setProperty(modelPath, CoreLexicon.PRIMARY_METAMODEL_URI, getCurrentNamespaceUri());
            output.setProperty(modelPath, CoreLexicon.MODEL_TYPE, firstValue(modelAnnotation, "modelType"));
            output.setProperty(modelPath, XmiLexicon.UUID, xmiUuid);
            output.setProperty(modelPath, JcrLexicon.UUID, uuidFor(modelAnnotation));

            // Process the model imports ...
            for (Location modelImportLocation : modelAnnotation.getChildren()) {
                SubgraphNode modelImport = subgraph.getNode(modelImportLocation);
                processObject(modelPath, modelImport, output);
            }
        }

        // Process the other top-level model objects ...
        for (Location objectLocation : xmi.getChildren()) {
            if (objectLocation.equals(modelAnnotationLocation)) continue;
            SubgraphNode modelObject = subgraph.getNode(objectLocation);
            processObject(modelPath, modelObject, output);
        }

        // Now attempt to resolve any references that were previously unresolved ...
        for (Path propPath : unresolved.keySet()) {
            Path path = propPath.getParent();
            Name propName = propPath.getLastSegment().getName();
            Collection<UUID> mmuuids = unresolved.get(propPath);
            Object[] names = new String[mmuuids.size()];
            int i = 0;
            for (UUID mmuuid : mmuuids) {
                ResolvedReference ref = resolve(null, null, mmuuid);
                if (ref.getName() == null) {
                    names = null;
                    break;
                }
                names[i++] = ref.getName();
            }
            if (names != null) {
                Name refNameName = nameForResolvedName(propName);
                output.setProperty(path, refNameName, names);
            }
        }

        // Now after all of the model objects have been read in, process the annotations ...
        SubgraphNode annotations = xmi.getNode("mmcore:AnnotationContainer");
        if (annotations != null) {
            for (Location annotationLocation : annotations.getChildren()) {
                SubgraphNode annotation = subgraph.getNode(annotationLocation);
                String hrefToAnnotatedObject = firstValue(annotation, "annotatedObject");
                if (hrefToAnnotatedObject != null) {
                    ResolvedReference resolvedRef = resolve(null, null, hrefToAnnotatedObject);
                    if (resolvedRef.getPath() != null) {
                        Path annotatedPath = resolvedRef.getPath();
                        // Process the description ...
                        output.setProperty(annotatedPath, CoreLexicon.DESCRIPTION, firstValue(annotation, "description"));
                        // Process the tags ...
                        for (Location tagLocation : annotation.getChildren()) {
                            Name childName = tagLocation.getPath().getLastSegment().getName();
                            if (childName.getLocalName().equals("tags")) {
                                SubgraphNode tag = subgraph.getNode(tagLocation);
                                String key = firstValue(tag, "key");
                                String value = firstValue(tag, "value");
                                // Keys often contain a ':', but these are almost never what we think of as namespace prefixes
                                Name propertyName = nameFromKey(key);
                                output.setProperty(annotatedPath, propertyName, value);
                            }
                        }
                    }
                }
            }
        }

        // And process the transformations ...
        SubgraphNode transformations = xmi.getNode("transform:TransformationContainer");
        if (transformations != null) {
            for (Location transformationLocation : transformations.getChildren()) {
                SubgraphNode transformation = subgraph.getNode(transformationLocation);
                String hrefToTransformedObject = firstValue(transformation, "targets");
                if (hrefToTransformedObject != null) {
                    ResolvedReference resolvedRef = resolve(null, null, hrefToTransformedObject);
                    if (resolvedRef.getPath() != null) {
                        Path annotatedPath = resolvedRef.getPath();
                        // Get the transformation details ...
                        Path helperNestedNodePath = pathFactory.create(transformationLocation.getPath(), "helper/nested");
                        SubgraphNode helperNested = subgraph.getNode(helperNestedNodePath);
                        // output.setProperty(annotatedPath, TransformLexicon.DESCRIPTION, firstValue(annotation, "description"));
                    }
                }
            }
        }
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
            // Register the newly created object ...
            UUID xmiUuid = xmiUuidFor(node);
            mmuuidToNodePath.put(xmiUuid, actualPath);

            // Process the nested objects ...
            for (Location childLocation : node.getChildren()) {
                SubgraphNode childObject = subgraph.getNode(childLocation);
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

    protected ResolvedReference resolve( Path ownerPath,
                                         Name attributeName,
                                         String href ) {
        if (href == null) {
            return null;
        }
        // First check the standard data types ...
        String name = STANDARD_DATA_TYPE_URLS_TO_NAMES.get(href);
        if (name != null) {
            return new ResolvedReference(name);
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
        return resolve(ownerPath, attributeName, mmuuid);
    }

    protected ResolvedReference resolve( Path ownerPath,
                                         Name attributeName,
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
            return new ResolvedReference(resolvedName, path, weakReference);
        }
        return null;
    }

    public static class ResolvedReference {
        private final Reference reference;
        private final String name;
        private final Path path;
        private final boolean standardDataType;

        public ResolvedReference( String standardDataTypeName ) {
            this.standardDataType = true;
            this.name = standardDataTypeName;
            this.reference = null;
            this.path = null;
        }

        public ResolvedReference( String name,
                                  Path path,
                                  Reference reference ) {
            this.standardDataType = false;
            this.name = name;
            this.reference = reference;
            this.path = path;
        }

        public boolean isStandardDataType() {
            return standardDataType;
        }

        public String getName() {
            return name;
        }

        public Path getPath() {
            return path;
        }

        public Reference getWeakReferenceValue() {
            return reference;
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
            Property primaryType = node.getProperty(JcrLexicon.PRIMARY_TYPE);
            Property mixinTypes = node.getProperty(JcrLexicon.MIXIN_TYPES);
            Name pt = reader.typeNameFrom(reader.nameFrom(reader.stringFrom(primaryType.getFirstValue())));
            if (JcrNtLexicon.UNSTRUCTURED.equals(pt)) {
                if (mixinTypes != null) {
                    output.setProperty(path, JcrLexicon.MIXIN_TYPES, mixinTypes.getValuesAsArray());
                } else {
                    // There are no mixin types, so let's assume that the object had no 'name' attribute, that
                    // the type was placed in the name (and thus no mixin types), and that the name is also
                    // the mixin type ...
                    output.setProperty(path, JcrLexicon.MIXIN_TYPES, reader.typeNameFrom(path.getLastSegment().getName()));
                }
            } else {
                if (mixinTypes == null) {
                    output.setProperty(path, JcrLexicon.MIXIN_TYPES, pt);
                } else {
                    List<Object> values = new ArrayList<Object>(mixinTypes.size() + 1);
                    values.add(pt);
                    for (Object value : mixinTypes) {
                        Name type = reader.nameFrom(reader.stringFrom(value));
                        values.add(type);
                    }
                    output.setProperty(path, JcrLexicon.MIXIN_TYPES, values);
                }
            }
            for (Property property : node.getProperties()) {
                Name name = property.getName();
                if (name.equals(JcrLexicon.PRIMARY_TYPE) || name.equals(JcrLexicon.MIXIN_TYPES)) {
                    continue;
                } else if (name.equals(ModeShapeLexicon.UUID)) {
                    name = JcrLexicon.UUID;
                } else if (name.getNamespaceUri().isEmpty()) {
                    name = reader.nameFrom(name.getLocalName());
                }
                List<UUID> references = reader.references(property);
                if (references != null) {
                    Multimap<Name, Object> valuesByName = ArrayListMultimap.create();
                    for (UUID uuid : references) {
                        ResolvedReference resolved = reader.resolve(path, name, uuid);
                        if (resolved != null) {
                            // And record the reference value ...
                            Name hrefName = reader.nameForHref(name);
                            valuesByName.put(hrefName, "mmuuid/" + uuid);

                            // Record the resolved reference value ...
                            Reference weakReference = resolved.getWeakReferenceValue();
                            if (weakReference != null) {
                                Name refName = reader.nameForResolvedReference(name);
                                valuesByName.put(refName, weakReference);
                            }

                            // Record the name of the resolved object ...
                            String resolvedName = resolved.getName();
                            if (resolvedName != null) {
                                Name refNameName = reader.nameForResolvedName(name);
                                valuesByName.put(refNameName, resolvedName);
                            }
                        }
                    }
                    for (Name propName : valuesByName.keySet()) {
                        Collection<Object> values = valuesByName.get(propName);
                        Object[] valueArray = values.toArray(new Object[values.size()]);
                        output.setProperty(path, propName, valueArray);
                    }
                } else {
                    // No references; just regular values ...
                    output.setProperty(path, name, property.getValuesAsArray());
                }
            }
            // Look at the children to see if there are any references ...
            Multimap<Name, Object> valuesByName = ArrayListMultimap.create();
            for (Location childLocation : node.getChildren()) {
                SubgraphNode child = subgraph.getNode(childLocation);
                if (child.getProperty("href") != null) {
                    // The child node is a reference, so figure out the EObject reference name ...
                    Name attributeName = childLocation.getPath().getLastSegment().getName();

                    // And record the reference value ...
                    String href = reader.firstValue(child, "href");
                    Name hrefName = reader.nameForHref(attributeName);
                    valuesByName.put(hrefName, href);

                    // Now try resolving the 'href' value ...
                    ResolvedReference resolved = reader.resolve(path, attributeName, href);
                    if (resolved != null) {
                        // Record the resolved reference value ...
                        Reference weakReference = resolved.getWeakReferenceValue();
                        if (weakReference != null) {
                            Name refName = reader.nameForResolvedReference(attributeName);
                            valuesByName.put(refName, weakReference);
                        }

                        // Record the name of the resolved object ...
                        String resolvedName = resolved.getName();
                        if (resolvedName != null) {
                            Name refNameName = reader.nameForResolvedName(attributeName);
                            valuesByName.put(refNameName, resolvedName);
                        }
                    }
                }
            }
            for (Name name : valuesByName.keySet()) {
                Collection<Object> values = valuesByName.get(name);
                Object[] valueArray = values.toArray(new Object[values.size()]);
                output.setProperty(path, name, valueArray);
            }
            return path;
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
            output.setProperty(path, JcrLexicon.MIXIN_TYPES, CoreLexicon.IMPORT);
            output.setProperty(path, CoreLexicon.PRIMARY_METAMODEL_URI, primaryMetamodelUri);
            output.setProperty(path, CoreLexicon.MODEL_TYPE, reader.firstValue(node, "modelType"));
            output.setProperty(path, CoreLexicon.PATH, reader.firstValue(node, "path"));
            output.setProperty(path, XmiLexicon.UUID, reader.firstValue(node, "xmi:uuid"));
            output.setProperty(path, JcrLexicon.UUID, reader.uuidFor(node));
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
