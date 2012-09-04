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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.jcr.Node;
import org.modeshape.common.collection.ArrayListMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.teiid.TeiidI18n;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.xmi.XmiElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Records nodes and unresolved references.
 */
public class ReferenceResolver {

    private static final boolean DEBUG = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceResolver.class);

    public static final Map<String, String> STANDARD_DATA_TYPE_URLS_BY_UUID;
    public static final Map<String, String> STANDARD_DATA_TYPE_URLS_TO_NAMES;
    public static final Map<String, String> STANDARD_DATA_TYPE_UUIDS_BY_NAMES;

    static {
        final Map<String, String> dataTypes = new HashMap<String, String>();
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
        final Map<String, String> dataTypesByUuid = new HashMap<String, String>();
        final Map<String, String> dataTypeUuidsByName = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry : dataTypes.entrySet()) {
            final String url = entry.getKey();
            final String name = entry.getValue();
            try {
                final String uuid = UUID.fromString(url.substring(sdtUrl.length())).toString();
                dataTypesByUuid.put(uuid, name);
                dataTypeUuidsByName.put(name, uuid);
            } catch (final IllegalArgumentException e) {
                LOGGER.error("UUID not valid", e);
            }
        }

        // Newer models have simple data types hrefs that contain names ...
        final String xsdUrl = "http://www.w3.org/2001/XMLSchema#";
        for (final String value : new HashSet<String>(dataTypes.values())) {
            dataTypes.put(xsdUrl + value, value);
        }
        sdtUrl = "http://www.metamatrix.com/metamodels/SimpleDatatypes-instance#";
        for (final String value : new HashSet<String>(dataTypes.values())) {
            dataTypes.put(sdtUrl + value, value);
        }

        STANDARD_DATA_TYPE_URLS_TO_NAMES = Collections.unmodifiableMap(dataTypes);
        STANDARD_DATA_TYPE_URLS_BY_UUID = Collections.unmodifiableMap(dataTypesByUuid);
        STANDARD_DATA_TYPE_UUIDS_BY_NAMES = Collections.unmodifiableMap(dataTypeUuidsByName);
    }

    static void debug( final String message ) {
        System.err.println(message);
    }

    // key = uuid, value = UnresolvedReference
    private final Map<String, UnresolvedReference> unresolved = new ConcurrentHashMap<String, ReferenceResolver.UnresolvedReference>();

    // key = uuid, value = Node
    private final Map<String, Node> uuidToNode = new HashMap<String, Node>();

    // key = uuid, value = XmiElement
    private final Map<String, XmiElement> uuidToXmiElement = new HashMap<String, XmiElement>();

    /**
     * @param xmiUuid the UUID of the model object whose node has not been created (cannot be <code>null</code>)
     * @return the unresolved reference (never <code>null</code>)
     * @throws Exception if a node for the specified UUID does exist
     */
    public UnresolvedReference addUnresolvedReference( String xmiUuid ) throws Exception {
        CheckArg.isNotEmpty(xmiUuid, "xmiUuid");

        xmiUuid = resolveInternalReference(xmiUuid);

        if (this.uuidToNode.containsKey(xmiUuid)) {
            throw new Exception(TeiidI18n.illegalUnresolvedReference.text(xmiUuid));
        }

        // see if already unresolved
        UnresolvedReference unresolved = this.unresolved.get(xmiUuid);

        // create unresolved if necessary
        if (unresolved == null) {
            unresolved = new UnresolvedReference(xmiUuid);
            this.unresolved.put(xmiUuid, unresolved);

            if (DEBUG) {
                debug("added " + xmiUuid + " to the list of unresolved references");
            }
        }

        return unresolved;
    }

    /**
     * @param xmiUuid the UUID of the node being requested (cannot be <code>null</code> or empty)
     * @return the node or <code>null</code> if not found
     */
    Node getNode( final String xmiUuid ) {
        CheckArg.isNotEmpty(xmiUuid, "xmiUuid");
        return this.uuidToNode.get(xmiUuid);
    }

    /**
     * @return the unresolved references (never <code>null</code>)
     */
    public Map<String, UnresolvedReference> getUnresolved() {
        return this.unresolved;
    }

    /**
     * @return a map of the registered XMI elements keyed by UUID (never <code>null</code>)
     */
    Map<String, XmiElement> getUuidMappings() {
        return this.uuidToXmiElement;
    }

    /**
     * @param value the value being checked to see if it is a reference (cannot be <code>null</code> or empty)
     * @return <code>true</code> if value is a reference
     */
    public boolean isReference( final String value ) {
        CheckArg.isNotEmpty(value, "value");
        return (value.startsWith(CoreLexicon.ModelId.MM_HREF_PREFIX));
    }

    /**
     * @param xmiUuid the UUID associated with the node being registered (cannot be <code>null</code> or empty)
     * @param node the node being registered (cannot be <code>null</code>)
     */
    public void record( String xmiUuid,
                        final Node node ) {
        CheckArg.isNotEmpty(xmiUuid, "xmiUuid");
        CheckArg.isNotNull(node, "node");

        if (xmiUuid.startsWith(CoreLexicon.ModelId.MM_UUID_PREFIX)) {
            xmiUuid = xmiUuid.substring(CoreLexicon.ModelId.MM_UUID_PREFIX.length() + 1);
        }

        this.uuidToNode.put(xmiUuid, node);
    }

    /**
     * @param xmiUuid the UUID associated with the XMI element being registered (cannot be <code>null</code> or empty)
     * @param xmiElement the XMI element being registered (cannot be <code>null</code>)
     */
    void record( final String xmiUuid,
                 final XmiElement xmiElement ) {
        CheckArg.isNotEmpty(xmiUuid, "xmiUuid");
        CheckArg.isNotNull(xmiElement, "xmiElement");
        this.uuidToXmiElement.put(xmiUuid, xmiElement);
    }

    //
    // /**
    // * Extracts the "mmuuid" values from the property if the property is indeed an XMI reference to local objects.
    // *
    // * @param property the property
    // * @return the list of mmuuid values, or null if this property does not contain any references; never empty
    // * @throws RepositoryException if error access property value(s)
    // */
    // List<String> references( final Property property ) throws RepositoryException {
    // final List<String> result = new LinkedList<String>();
    // for (final Value value : property.getValues()) {
    // final String str = value.getString();
    //
    // if (str.startsWith(CoreLexicon.ModelId.MM_HREF_PREFIX)) {
    // // It is a local reference ...
    // final String[] references = str.split("\\s");
    //
    // for (final String reference : references) {
    // result.add(reference);
    //
    // if (!property.isMultiple() && (references.length == 1)) {
    // // This is the only property value, and only one reference in it ...
    // return result;
    // }
    // }
    // } else {
    // assert result.isEmpty();
    // return null;
    // }
    // }
    //
    // return result;
    // }

    /**
     * @param unresolved the unresolved reference being marked as resolved (cannot be <code>null</code>)
     */
    public void resolved( final UnresolvedReference unresolved ) {
        CheckArg.isNotNull(unresolved, "unresolved");
        final UnresolvedReference resolved = this.unresolved.remove(unresolved.getUuid());
        assert (unresolved == resolved);

        if (DEBUG) {
            debug("UUID " + unresolved.getUuid() + " has been resolved");
        }
    }

    /**
     * @param proposedUuid the value whose UUID prefix is being removed (cannot be <code>null</code> or empty)
     * @return the UUID or <code>null</code> if the proposedUuid is not a UUID
     */
    public String resolveInternalReference( final String proposedUuid ) {
        CheckArg.isNotNull(proposedUuid, "proposedUuid");
        String mmuuid = null;
        final int index = proposedUuid.indexOf(CoreLexicon.ModelId.MM_HREF_PREFIX);

        if (index != -1) {
            // It's a local reference ...
            try {
                mmuuid = UUID.fromString(proposedUuid.substring(index + CoreLexicon.ModelId.MM_HREF_PREFIX.length())).toString();
            } catch (final IllegalArgumentException e) {
                // ignore
            }
        } else {
            try {
                mmuuid = UUID.fromString(proposedUuid).toString();
            } catch (final IllegalArgumentException e) {
                // ignore
            }
        }

        return mmuuid;
    }

    final class UnresolvedProperty {

        private final boolean multi;
        private final String name;
        private final List<String> values;

        protected UnresolvedProperty( final String name,
                                      final String value,
                                      final boolean multi ) {
            this.name = name;
            this.values = new ArrayList<String>();
            this.values.add(value);
            this.multi = multi;
        }

        protected void addValue( final String newValue ) {
            if (this.multi) {
                this.values.add(newValue);
            }
        }

        public String getName() {
            return this.name;
        }

        public String getValue() {
            if (this.multi) {
                throw new IllegalArgumentException();
            }

            return (this.values.isEmpty() ? null : this.values.get(0));
        }

        public List<String> getValues() {
            if (this.multi) {
                return this.values;
            }

            throw new IllegalArgumentException();
        }

        public boolean isMulti() {
            return this.multi;
        }
    }

    /**
     * A referenced UUID that did not have a node associated with it at the time the reference was found.
     */
    class UnresolvedReference {

        private final Set<String> mixins = new HashSet<String>(2);

        /**
         * Once resolved, the node specified node properties will be set with the values. The key is the name of the property and
         * the value is a collection of values to set.
         */
        private final Map<String, UnresolvedProperty> properties = new HashMap<String, ReferenceResolver.UnresolvedProperty>();

        /**
         * The unresolved node is the node whose name will be used to set a referencer property.
         * <p>
         * The key is the name of the referencer node property that will be set with the resolved node name. The value is a
         * collection of referencer node UUIDs.
         */
        private final Multimap<String, String> refNames = ArrayListMultimap.create();

        /**
         * The unresolved reference is the node that is the reference.
         * <p>
         * Once resolved, the specified referencer property will be set with the weak reference of the resolved node. The key is
         * the referencer property and the value is a collection of referencer UUIDs.
         */
        private final Multimap<String, String> refRefs = ArrayListMultimap.create();

        /**
         * The unresolved reference is the node whose reference property needs to be set.
         * <p>
         * Once resolved, a weak reference value will be created from each of the referenced node UUID values and the specified
         * property will be set. The key is the name of the referencer property. The value is a collection of referenced UUIDs.
         */
        private final Multimap<String, String> refs = ArrayListMultimap.create();

        private final String uuid;

        /**
         * <p>
         * <strong>Should only be called by the reference resolver.</strong>
         * 
         * @param uuid the UUID of the unresolved reference (cannot be <code>null</code> or empty)
         */
        UnresolvedReference( final String uuid ) {
            CheckArg.isNotEmpty(uuid, "uuid");
            this.uuid = uuid;
        }

        /**
         * @param newMixin the mixin to add to the unresolved reference (cannot be <code>null</code> or empty)
         * @return <code>true</code> if the mixin was successfully added
         */
        public boolean addMixin( final String newMixin ) {
            CheckArg.isNotEmpty(newMixin, "newMixin");
            final boolean added = this.mixins.add(newMixin);

            if (added) {
                if (DEBUG) {
                    debug("added mixin '" + newMixin + "' to the unresolved reference " + this.uuid);
                }
            }

            return added;
        }

        /**
         * @param propertyName the property name (cannot be <code>null</code> or empty)
         * @param propertyValue the property value (can be <code>null</code> or empty)
         * @param multiValued <code>true</code> if property is multi-valued
         */
        public void addProperty( final String propertyName,
                                 final String propertyValue,
                                 final boolean multiValued ) {
            CheckArg.isNotEmpty(propertyName, "propertyName");

            if (!StringUtil.isBlank(propertyValue)) {
                if (multiValued) {
                    UnresolvedProperty unresolvedProperty = this.properties.get(propertyName);

                    if (unresolvedProperty == null) {
                        unresolvedProperty = new UnresolvedProperty(propertyName, propertyValue, true);
                    } else {
                        unresolvedProperty.addValue(propertyValue);
                    }

                    if (DEBUG) {
                        debug("added multi-valued property '" + propertyName + "' with value '" + propertyValue
                              + "' to the unresolved reference " + this.uuid);
                    }
                } else {
                    this.properties.put(propertyName, new UnresolvedProperty(propertyName, propertyValue, false));

                    if (DEBUG) {
                        debug("added property '" + propertyName + "' with value '" + propertyValue
                              + "' to the unresolved reference " + this.uuid);
                    }
                }
            }
        }

        /**
         * @param propertyName the name of the referencer property to set once the reference is resolved (cannot be
         *        <code>null</code> or empty)
         * @param referencedUuid the UUID of the referenced node (cannot be <code>null</code> or empty)
         */
        public void addReference( final String propertyName,
                                  final String referencedUuid ) {
            CheckArg.isNotEmpty(propertyName, "propertyName");
            CheckArg.isNotEmpty(referencedUuid, "referencerUuid");
            this.refs.put(propertyName, referencedUuid);
        }

        /**
         * @param referencerUuid the UUID of the referencer whose node property will be set with the weak reference of the
         *        resolved node (cannot be <code>null</code> or empty)
         * @param referencerPropertyName the name of the referencer property to set with the weak reference (cannot be
         *        <code>null</code> or empty)
         */
        public void addReferencerReference( final String referencerUuid,
                                            final String referencerPropertyName ) {
            CheckArg.isNotEmpty(referencerUuid, "referencerUuid");
            CheckArg.isNotEmpty(referencerPropertyName, "referencerPropertyName");
            this.refRefs.put(referencerPropertyName, referencerUuid);
        }

        /**
         * @param referencerUuid the UUID of the node whose property needs to be set with the name of the resolved node (cannot be
         *        <code>null</code> or empty)
         * @param referencerPropertyName the name of the referencer property being set (cannot be <code>null</code> or empty)
         */
        public void addResolvedName( final String referencerUuid,
                                     final String referencerPropertyName ) {
            CheckArg.isNotEmpty(referencerUuid, "referencerUuid");
            CheckArg.isNotEmpty(referencerPropertyName, "referencerPropertyName");
            this.refNames.put(referencerPropertyName, referencerUuid);
        }

        /**
         * @return the mixins to add to the resolved node (never <code>null</code>)
         */
        protected Set<String> getMixins() {
            return this.mixins;
        }

        /**
         * Key is property name. Value is a collection of unresolved properties.
         * 
         * @return the unresolved properties that have to be added to the resolved node (never <code>null</code>)
         */
        protected Map<String, UnresolvedProperty> getProperties() {
            return this.properties;
        }

        /**
         * Key is property name that needs to be set with the resolved node's name. Value is the UUID of the node whose property
         * is being set.
         * 
         * @return the UUIDs of the nodes that need to be set with the resolved node name (never <code>null</code>)
         */
        protected Multimap<String, String> getReferenceNames() {
            return this.refNames;
        }

        /**
         * The unresolved reference is the node that the weak reference will be created from and set on the referencer node.
         * 
         * @return the referencers that need to have weak references set on (never <code>null</code>)
         */
        protected Multimap<String, String> getReferencerReferences() {
            return this.refRefs;
        }

        /**
         * Key is property name. Value is a collection of one or more UUID values. If multiple values then property is
         * multi-valued.
         * 
         * @return the references that need to have weak references created (never <code>null</code>)
         */
        protected Multimap<String, String> getReferences() {
            return this.refs;
        }

        /**
         * @return the UUID (never <code>null</code> or empty)
         */
        public String getUuid() {
            return this.uuid;
        }
    }
}
