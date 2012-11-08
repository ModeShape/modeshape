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
package org.modeshape.jcr.cache.document;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.infinispan.schematic.DocumentFactory;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Null;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode.ReferenceType;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.SessionNode.ChangedAdditionalParents;
import org.modeshape.jcr.cache.document.SessionNode.ChangedChildren;
import org.modeshape.jcr.cache.document.SessionNode.Insertions;
import org.modeshape.jcr.cache.document.SessionNode.ReferrerChanges;
import org.modeshape.jcr.value.BinaryFactory;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ReferenceFactory;
import org.modeshape.jcr.value.UuidFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.basic.NodeKeyReference;
import org.modeshape.jcr.value.basic.StringReference;
import org.modeshape.jcr.value.basic.UuidReference;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.InMemoryBinaryValue;

/**
 * A utility class that encapsulates all the logic for reading from and writing to {@link Document} instances.
 */
public class DocumentTranslator {

    public static final String SHA1 = "sha1";
    public static final String SHA1_FIELD = "$sha1";
    public static final String LENGTH = "len";
    public static final String LENGTH_FIELD = "$len";
    public static final String PARENT = "parent";
    public static final String LARGE_VALUE = "value";
    public static final String PROPERTIES = "properties";
    public static final String CHILDREN = "children";
    public static final String CHILDREN_INFO = "childrenInfo";
    public static final String FEDERATED_SEGMENTS = "federatedSegments";
    public static final String COUNT = "count";
    public static final String BLOCK_SIZE = "blockSize";
    public static final String NEXT_BLOCK = "nextBlock";
    public static final String LAST_BLOCK = "lastBlock";
    public static final String NAME = "name";
    public static final String KEY = "key";
    public static final String REFERRERS = "referrers";
    public static final String WEAK = "weak";
    public static final String STRONG = "strong";
    public static final String REFERENCE_COUNT = "refCount";
    /**
     * A constant that can be used by a connector implementation as a supplementary document field, that indicates the maximum number
     * of seconds that particular document should be stored in the workspace cache.
     */
    public static final String CACHE_TTL_SECONDS = "cacheTtlSeconds";

    private final DocumentStore documentStore;
    private final AtomicLong largeStringSize = new AtomicLong();
    private final ExecutionContext context;
    private final PropertyFactory propertyFactory;
    private final ValueFactories factories;
    private final PathFactory paths;
    private final NameFactory names;
    private final DateTimeFactory dates;
    private final BinaryFactory binaries;
    private final ValueFactory<Long> longs;
    private final ValueFactory<Double> doubles;
    private final ValueFactory<URI> uris;
    private final ValueFactory<BigDecimal> decimals;
    private final ValueFactory<String> strings;
    private final ReferenceFactory refs;
    private final ReferenceFactory weakrefs;
    private final UuidFactory uuids;
    private final TextEncoder encoder = NoOpEncoder.getInstance();
    private final TextDecoder decoder = NoOpEncoder.getInstance();

    public DocumentTranslator( ExecutionContext context,
                               DocumentStore documentStore,
                               long largeStringSize ) {
        this.documentStore = documentStore;
        this.largeStringSize.set(largeStringSize);
        this.context = context;
        this.propertyFactory = this.context.getPropertyFactory();
        this.factories = this.context.getValueFactories();
        this.paths = this.factories.getPathFactory();
        this.names = this.factories.getNameFactory();
        this.dates = this.factories.getDateFactory();
        this.binaries = this.factories.getBinaryFactory();
        this.longs = this.factories.getLongFactory();
        this.doubles = this.factories.getDoubleFactory();
        this.uris = this.factories.getUriFactory();
        this.decimals = this.factories.getDecimalFactory();
        this.refs = this.factories.getReferenceFactory();
        this.weakrefs = this.factories.getWeakReferenceFactory();
        this.uuids = this.factories.getUuidFactory();
        this.strings = this.factories.getStringFactory();
        assert this.largeStringSize.get() >= 0;
    }

    void setLargeValueSize( long largeValueSize ) {
        assert largeValueSize > -1;
        this.largeStringSize.set(largeValueSize);
    }

    /**
     * Obtain the preferred {@link NodeKey key} for the parent of this node. Because a node can be used in more than once place,
     * it may technically have more than one parent. Therefore, in such cases this method prefers the parent that is in the
     * {@code primaryWorkspaceKey} and, if there is no such parent, the parent that is in the {@code secondaryWorkspaceKey}.
     *
     * @param document the document for the node; may not be null
     * @param primaryWorkspaceKey the key for the workspace in which the parent should preferrably exist; may be null
     * @param secondaryWorkspaceKey the key for the workspace in which the parent should exist if not in the primary workspace;
     * may be null
     * @return the key representing the preferred parent, or null if the document contains no parent reference or if the parent
     *         reference(s) do not have the specified workspace keys
     */
    public NodeKey getParentKey( Document document,
                                 String primaryWorkspaceKey,
                                 String secondaryWorkspaceKey ) {
        Object value = document.get(PARENT);
        return keyFrom(value, primaryWorkspaceKey, secondaryWorkspaceKey);
    }

    public Set<NodeKey> getParentKeys( Document document,
                                       String primaryWorkspaceKey,
                                       String secondaryWorkspaceKey ) {
        Object value = document.get(PARENT);
        if (value instanceof String) {
            // The first key is the primary parent, so there are no more additional parents ...
            return Collections.emptySet();
        }
        if (value instanceof List<?>) {
            List<?> values = (List<?>)value;
            if (values.size() == 1) {
                // The first key is the primary parent, so there are no more additional parents ...
                return Collections.emptySet();
            }
            Set<NodeKey> keys = new LinkedHashSet<NodeKey>();
            Iterator<?> iter = values.iterator();
            // skip the first parent, since it's the primary parent ...
            iter.next();
            while (iter.hasNext()) {
                Object v = iter.next();
                if (v == null) {
                    continue;
                }
                String key = (String)v;
                NodeKey nodeKey = new NodeKey(key);
                String workspaceKey = nodeKey.getWorkspaceKey();
                if (workspaceKey.equals(primaryWorkspaceKey) || workspaceKey.equals(secondaryWorkspaceKey)) {
                    keys.add(nodeKey);
                }
            }
            return keys;
        }
        return Collections.emptySet();
    }

    private final NodeKey keyFrom( Object value,
                                   String primaryWorkspaceKey,
                                   String secondaryWorkspaceKey ) {
        if (value instanceof String) {
            return new NodeKey((String)value);
        }
        if (value instanceof List<?>) {
            List<?> values = (List<?>)value;
            if (values.size() == 1) {
                return keyFrom(values.get(0), primaryWorkspaceKey, secondaryWorkspaceKey);
            }
            NodeKey keyWithSecondaryWorkspaceKey = null;
            for (Object v : values) {
                if (v == null) {
                    continue;
                }
                NodeKey key = new NodeKey((String)v);
                if (key.getWorkspaceKey().equals(primaryWorkspaceKey)) {
                    return key;
                }
                if (keyWithSecondaryWorkspaceKey == null && secondaryWorkspaceKey != null
                    && key.getWorkspaceKey().equals(secondaryWorkspaceKey)) {
                    keyWithSecondaryWorkspaceKey = key;
                }
            }
            return keyWithSecondaryWorkspaceKey;
        }
        return null;
    }

    public void getProperties( Document document,
                               Map<Name, Property> result ) {
        // Get the properties container ...
        Document properties = document.getDocument(PROPERTIES);
        if (properties != null) {

            // For all namespaces ...
            for (Field nsField : properties.fields()) {
                String namespaceUri = nsField.getName();
                Document nsDoc = nsField.getValueAsDocument();

                // Get all the properties in the namespace ...
                for (Field propField : nsDoc.fields()) {
                    String localName = propField.getName();
                    Name propertyName = names.create(namespaceUri, localName);
                    if (!result.containsKey(propertyName)) {
                        Object fieldValue = propField.getValue();
                        Property property = propertyFor(propertyName, fieldValue);
                        result.put(propertyName, property);
                    }
                }
            }
        }
    }

    public int countProperties( Document document ) {
        // Get the properties container ...
        Document properties = document.getDocument(PROPERTIES);
        if (properties == null) {
            return 0;
        }

        int count = 0;
        for (Field nsField : properties.fields()) {
            Document urlProps = nsField.getValueAsDocument();
            if (urlProps != null) {
                for (Field propField : urlProps.fields()) {
                    if (!Null.matches(propField.getValue())) {
                        ++count;
                    }
                }
            }
        }
        return count;
    }

    public boolean hasProperties( Document document ) {
        // Get the properties container ...
        Document properties = document.getDocument(PROPERTIES);
        if (properties == null) {
            return false;
        }

        for (Field nsField : properties.fields()) {
            Document urlProps = nsField.getValueAsDocument();
            if (urlProps != null) {
                for (Field propField : urlProps.fields()) {
                    if (!Null.matches(propField.getValue())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean hasProperty( Document document,
                                Name propertyName ) {
        // Get the properties container ...
        Document properties = document.getDocument(PROPERTIES);
        if (properties == null) {
            return false;
        }

        // Get the namespace container for the property name's namespace ...
        Document urlProps = properties.getDocument(propertyName.getNamespaceUri());
        if (urlProps == null) {
            return false;
        }

        // Get the property ...
        Object fieldValue = urlProps.get(propertyName.getLocalName());
        return !Null.matches(fieldValue);
    }

    public Property getProperty( Document document,
                                 Name propertyName ) {
        // Get the properties container ...
        Document properties = document.getDocument(PROPERTIES);
        if (properties == null) {
            return null;
        }

        // Get the namespace container for the property name's namespace ...
        Document urlProps = properties.getDocument(propertyName.getNamespaceUri());
        if (urlProps == null) {
            return null;
        }

        // Get the property ...
        Object fieldValue = urlProps.get(propertyName.getLocalName());
        return fieldValue == null ? null : propertyFor(propertyName, fieldValue);
    }

    protected Property propertyFor( Name propertyName,
                                    Object fieldValue ) {
        Object value = valueFromDocument(fieldValue);
        if (value instanceof List<?>) {
            List<?> values = (List<?>)value;
            return propertyFactory.create(propertyName, values);
        }
        return propertyFactory.create(propertyName, value);
    }

    public void setProperty( EditableDocument document,
                             Property property,
                             Set<BinaryKey> unusedBinaryKeys ) {
        // Get or create the properties container ...
        EditableDocument properties = document.getDocument(PROPERTIES);
        if (properties == null) {
            properties = document.setDocument(PROPERTIES);
        }

        // Get or create the namespace container for the property name's namespace ...
        Name propertyName = property.getName();
        String namespaceUri = propertyName.getNamespaceUri();
        EditableDocument urlProps = properties.getDocument(namespaceUri);
        if (urlProps == null) {
            urlProps = properties.setDocument(namespaceUri);
        }

        // Get the old value ...
        String localName = propertyName.getLocalName();
        Object oldValue = urlProps.get(localName);
        decrementBinaryReferenceCount(oldValue, unusedBinaryKeys);

        // Now set the property ...
        if (property.isEmpty()) {
            urlProps.setArray(localName);
        } else if (property.isMultiple()) {
            EditableArray values = Schematic.newArray(property.size());
            for (Object v : property) {
                values.add(valueToDocument(v, unusedBinaryKeys));
            }
            urlProps.setArray(localName, values);
        } else {
            assert property.isSingle();
            Object value = valueToDocument(property.getFirstValue(), unusedBinaryKeys);
            if (value == null) {
                urlProps.remove(localName);
            } else {
                urlProps.set(localName, value);
            }
        }
    }

    public Property removeProperty( EditableDocument document,
                                    Name propertyName,
                                    Set<BinaryKey> unusedBinaryKeys ) {
        // Get the properties container if it exists ...
        EditableDocument properties = document.getDocument(PROPERTIES);
        if (properties == null) {
            // Doesn't contain the property ...
            return null;
        }

        // Get the namespace container for the property name's namespace ...
        String namespaceUri = propertyName.getNamespaceUri();
        EditableDocument urlProps = properties.getDocument(namespaceUri);
        if (urlProps == null) {
            // Doesn't contain the property ...
            return null;
        }

        // Now remove the property ...
        String localName = propertyName.getLocalName();
        Object fieldValue = urlProps.remove(localName);

        // We're removing a reference to a binary value, and we need to decrement the reference count ...
        decrementBinaryReferenceCount(fieldValue, unusedBinaryKeys);

        // Now remove the namespace if empty ...
        if (urlProps.isEmpty()) {
            properties.remove(namespaceUri);
        }
        return fieldValue == null ? null : propertyFor(propertyName, fieldValue);
    }

    public void addPropertyValues( EditableDocument document,
                                   Name propertyName,
                                   boolean isMultiple,
                                   Collection<?> values,
                                   Set<BinaryKey> unusedBinaryKeys ) {
        assert values != null;
        int numValues = values.size();
        if (numValues == 0) {
            return;
        }

        // Get or create the properties container ...
        EditableDocument properties = document.getDocument(PROPERTIES);
        if (properties == null) {
            properties = document.setDocument(PROPERTIES);
        }

        // Get or create the namespace container for the property name's namespace ...
        String namespaceUri = propertyName.getNamespaceUri();
        EditableDocument urlProps = properties.getDocument(namespaceUri);
        if (urlProps == null) {
            urlProps = properties.setDocument(namespaceUri);
        }

        // Now add the value to the property ...
        String localName = propertyName.getLocalName();
        Object propValue = urlProps.get(localName);
        if (propValue == null) {
            // We have to create the property ...
            if (isMultiple || numValues > 1) {
                EditableArray array = Schematic.newArray(numValues);
                for (Object value : values) {
                    array.addValue(valueToDocument(value, unusedBinaryKeys));
                }
                urlProps.setArray(localName, array);
            } else {
                urlProps.set(localName, valueToDocument(values.iterator().next(), unusedBinaryKeys));
            }
        } else if (propValue instanceof List<?>) {
            // Decrement the reference count of any binary references ...
            decrementBinaryReferenceCount(propValue, unusedBinaryKeys);

            // There's an existing property with multiple values ...
            EditableArray array = urlProps.getArray(localName);
            for (Object value : values) {
                value = valueToDocument(value, unusedBinaryKeys);
                array.addValueIfAbsent(value);
            }
        } else {
            // Decrement the reference count of any binary references ...
            decrementBinaryReferenceCount(propValue, unusedBinaryKeys);

            // There's just a single value ...
            if (numValues == 1) {
                Object value = valueToDocument(values.iterator().next(), unusedBinaryKeys);
                if (!value.equals(propValue)) {
                    // But the existing value is different, so we have to change to an array ...
                    EditableArray array = Schematic.newArray(value, propValue);
                    urlProps.setArray(localName, array);
                }
            } else {
                EditableArray array = Schematic.newArray(numValues);
                for (Object value : values) {
                    value = valueToDocument(value, unusedBinaryKeys);
                    if (!value.equals(propValue)) {
                        array.addValue(value);
                    }
                }
                assert !array.isEmpty();
                urlProps.setArray(localName, array);
            }
        }
    }

    public void removePropertyValues( EditableDocument document,
                                      Name propertyName,
                                      Collection<?> values,
                                      Set<BinaryKey> unusedBinaryKeys ) {
        assert values != null;
        int numValues = values.size();
        if (numValues == 0) {
            return;
        }

        // Get the properties container if it exists ...
        EditableDocument properties = document.getDocument(PROPERTIES);
        if (properties == null) {
            // Doesn't contain the property ...
            return;
        }

        // Get the namespace container for the property name's namespace ...
        String namespaceUri = propertyName.getNamespaceUri();
        EditableDocument urlProps = properties.getDocument(namespaceUri);
        if (urlProps == null) {
            // Doesn't contain the property ...
            return;
        }

        // Now add the value to the property ...
        String localName = propertyName.getLocalName();
        Object propValue = urlProps.get(localName);
        if (propValue instanceof List<?>) {
            // There's an existing property with multiple values ...
            EditableArray array = urlProps.getArray(localName);
            for (Object value : values) {
                value = valueToDocument(value, unusedBinaryKeys);
                array.remove(value);
            }
        } else if (propValue != null) {
            // There's just a single value ...
            for (Object value : values) {
                value = valueToDocument(value, unusedBinaryKeys);
                if (value.equals(propValue)) {
                    // And the value matches, so remove the field ...
                    urlProps.remove(localName);
                    break;
                }
            }
        }

        // Now remove the namespace if empty ...
        if (urlProps.isEmpty()) {
            properties.remove(namespaceUri);
        }
    }

    public void setParents( EditableDocument document,
                            NodeKey parent,
                            NodeKey oldParent,
                            ChangedAdditionalParents additionalParents ) {
        Object existingParent = document.get(PARENT);
        if (existingParent == null) {
            if (parent != null) {
                if (additionalParents == null || additionalParents.isEmpty()) {
                    document.setString(PARENT, parent.toString());
                } else {
                    EditableArray parents = Schematic.newArray(additionalParents.additionCount() + 1);
                    parents.add(parent.toString());
                    for (NodeKey added : additionalParents.getAdditions()) {
                        parents.add(added.toString());
                    }
                    document.set(PARENT, parents);
                }
            } else if (additionalParents != null && !additionalParents.isEmpty()) {
                EditableArray parents = Schematic.newArray(additionalParents.additionCount());
                for (NodeKey added : additionalParents.getAdditions()) {
                    parents.add(added.toString());
                }
                document.set(PARENT, parents);
            }
            return;
        }
        if (existingParent instanceof List<?>) {
            // Remove the old parent and add the new parent ...
            EditableArray parents = document.getArray(PARENT);
            if (parent != null && !parent.equals(oldParent)) {
                parents.addStringIfAbsent(parent.toString());
                if (oldParent != null) {
                    parents.remove(oldParent.toString());
                }
            }
            if (additionalParents != null) {
                for (NodeKey removed : additionalParents.getRemovals()) {
                    parents.remove((Object)removed.toString()); // remove by value (not by name)
                }
                for (NodeKey added : additionalParents.getAdditions()) {
                    parents.addStringIfAbsent(added.toString());
                }
            }
        } else if (existingParent instanceof String) {
            String existing = (String)existingParent;
            if (parent != null && (additionalParents == null || additionalParents.isEmpty())) {
                String oldParentStr = oldParent != null ? oldParent.toString() : null;
                if (existing.equals(oldParentStr)) {
                    // Just replace the
                    document.set(PARENT, parent.toString());
                } else {
                    // Add it ...
                    EditableArray parents = Schematic.newArray(2);
                    parents.add(existing);
                    parents.add(parent.toString());
                    document.set(PARENT, parents);
                }
            } else {
                // Just replace the existing 'parent' field ...
                int totalNumber = additionalParents.additionCount() - additionalParents.removalCount() + 1;
                assert totalNumber >= 0;
                EditableArray parents = Schematic.newArray(totalNumber);
                if (parent != null && !existingParent.equals(parent.toString())) {
                    parents.add(parent.toString());
                } else {
                    parents.add(existingParent);
                }
                for (NodeKey removed : additionalParents.getRemovals()) {
                    parents.remove(removed.toString());
                }
                for (NodeKey added : additionalParents.getAdditions()) {
                    parents.add(added.toString());
                }
                document.set(PARENT, parents);
            }
        }
    }

    public void setKey( EditableDocument document,
                        NodeKey key ) {
        assert document.getString(KEY) == null;
        document.setString(KEY, key.toString());
    }

    public void changeChildren( EditableDocument document,
                                ChangedChildren changedChildren,
                                ChildReferences appended ) {
        assert !(changedChildren == null && appended == null);

        // Get the total number of children and the number of children in this block ...
        ChildReferencesInfo info = getChildReferencesInfo(document);
        long newTotalSize = 0L;

        EditableDocument doc = document;
        EditableDocument lastDoc = document;
        String lastDocKey = null;
        if (changedChildren != null && !changedChildren.isEmpty()) {
            Map<NodeKey, Insertions> insertionsByBeforeKey = changedChildren.getInsertionsByBeforeKey();

            // Handle removals and renames ...
            Set<NodeKey> removals = changedChildren.getRemovals();
            Map<NodeKey, Name> newNames = changedChildren.getNewNames();
            while (doc != null) {
                // Change the existing children ...
                long blockCount = insertChildren(doc, insertionsByBeforeKey, removals, newNames);
                newTotalSize += blockCount;

                // Look at the 'childrenInfo' document for info about the next block of children ...
                SchematicEntry nextEntry = null;
                ChildReferencesInfo docInfo = doc == document ? info : getChildReferencesInfo(doc);
                if (docInfo != null && docInfo.nextKey != null) {
                    // The children are segmented, so get the next block of children ...
                    nextEntry = documentStore.get(docInfo.nextKey);
                }

                if (nextEntry != null) {
                    // There is more than one block, so update the block size ...
                    doc.getDocument(CHILDREN_INFO).setNumber(BLOCK_SIZE, blockCount);

                    doc = nextEntry.editDocumentContent();
                    lastDoc = doc;
                    assert docInfo != null;
                    lastDocKey = docInfo.nextKey;
                } else {
                    if (doc == document) {
                        // This is still the first document, so there shouldn't be a block size ...
                        EditableDocument childInfo = doc.getDocument(CHILDREN_INFO);
                        childInfo.remove(BLOCK_SIZE);
                        childInfo.set(COUNT, newTotalSize);
                    }
                    doc = null;
                }
            }
        } else {
            // We're not inserting or removing children, so we've not modified the number of children ...
            newTotalSize = info != null ? info.totalSize : 0L;
        }

        if (appended != null && appended.size() != 0) {
            String lastKey = info != null ? info.lastKey : null;
            if (lastKey != null && !lastKey.equals(lastDocKey)) {
                // Find the last document ...
                SchematicEntry lastBlockEntry = documentStore.get(lastKey);
                lastDoc = lastBlockEntry.editDocumentContent();
            } else {
                lastKey = null;
            }
            // Just append the new children to the end of the last document; we can use an asynchronous process
            // to adjust/optimize the number of children in each block ...
            EditableArray lastChildren = lastDoc.getArray(CHILDREN);
            if (lastChildren == null) {
                lastChildren = lastDoc.setArray(CHILDREN);
            }
            for (ChildReference ref : appended) {
                lastChildren.add(fromChildReference(ref));
            }

            if (lastDoc != document) {
                // We've written to at least one other document, so update the block size ...
                EditableDocument lastDocInfo = lastDoc.getDocument(CHILDREN_INFO);
                if (lastDocInfo == null) {
                    lastDocInfo = lastDoc.setDocument(CHILDREN_INFO);
                }
                lastDocInfo.setNumber(BLOCK_SIZE, lastChildren.size());
            }

            // And update the total size and last block on the starting document ...
            EditableDocument childInfo = document.getDocument(CHILDREN_INFO);
            if (childInfo == null) {
                childInfo = document.setDocument(CHILDREN_INFO);
            }
            newTotalSize += appended.size();
            childInfo.setNumber(COUNT, newTotalSize);

            // And if needed the reference to the last block ...
            if (lastKey != null) {
                childInfo.setString(LAST_BLOCK, lastKey);
            }
        }
    }

    protected long insertChildren( EditableDocument document,
                                   Map<NodeKey, Insertions> insertionsByBeforeKey,
                                   Set<NodeKey> removals,
                                   Map<NodeKey, Name> newNames ) {
        List<?> children = document.getArray(CHILDREN);
        assert children != null;
        EditableArray newChildren = Schematic.newArray(children.size());
        for (Object value : children) {
            ChildReference ref = childReferenceFrom(value);
            if (ref == null) {
                continue;
            }
            NodeKey childKey = ref.getKey();
            // Are nodes inserted before this node?
            Insertions insertions = insertionsByBeforeKey.remove(childKey);
            if (insertions != null) {
                for (ChildReference inserted : insertions.inserted()) {
                    newChildren.add(fromChildReference(inserted));
                }
            }
            if (removals.remove(childKey)) {
                // The node is removed ...
            } else {
                // The node remains ...
                Name newName = newNames.get(childKey);
                if (newName != null) {
                    // But has been renamed ...
                    ChildReference newRef = ref.with(newName, 1);
                    value = fromChildReference(newRef);
                }
                newChildren.add(value);
            }
        }
        document.set(CHILDREN, newChildren);
        return newChildren.size();
    }

    public ChildReferences getChildReferences( WorkspaceCache cache,
                                               Document document ) {
        List<?> children = document.getArray(CHILDREN);
        List<?> externalSegments = document.getArray(FEDERATED_SEGMENTS);

        if (children == null && externalSegments == null) {
            return ImmutableChildReferences.EMPTY_CHILD_REFERENCES;
        }

        // Materialize the ChildReference objects in the 'children' document ...
        List<ChildReference> internalChildRefsList = childReferencesListFromArray(children);

        // Materialize the ChildReference objects in the 'federated segments' document ...
        List<ChildReference> externalChildRefsList = childReferencesListFromArray(externalSegments);

        // Now look at the 'childrenInfo' document for info about the next block of children ...
        ChildReferencesInfo info = getChildReferencesInfo(document);
        if (info != null) {
            // The children are segmented ...
            ChildReferences internalChildRefs = ImmutableChildReferences.create(internalChildRefsList);
            ChildReferences externalChildRefs = ImmutableChildReferences.create(externalChildRefsList);

            return ImmutableChildReferences.create(internalChildRefs, info, externalChildRefs, cache);
        }
        if (externalSegments != null) {
            // There is no segmenting, so just add the federated references at the end
            internalChildRefsList.addAll(externalChildRefsList);
        }
        return ImmutableChildReferences.create(internalChildRefsList);
    }

    private List<ChildReference> childReferencesListFromArray( List<?> children ) {
        if (children == null) {
            return new ArrayList<ChildReference>();
        }
        List<ChildReference> childRefsList = new ArrayList<ChildReference>(children.size());
        for (Object value : children) {
            ChildReference ref = childReferenceFrom(value);
            if (ref != null) {
                childRefsList.add(ref);
            }
        }
        return childRefsList;
    }

    public ChildReferencesInfo getChildReferencesInfo( Document document ) {
        // Now look at the 'childrenInfo' document for info about the next block ...
        Document childrenInfo = document.getDocument(CHILDREN_INFO);
        if (childrenInfo != null) {
            long totalSize = childrenInfo.getLong(COUNT, 0L);
            long blockSize = childrenInfo.getLong(BLOCK_SIZE, 0L);
            String nextBlockKey = childrenInfo.getString(NEXT_BLOCK);
            String lastBlockKey = childrenInfo.getString(LAST_BLOCK, nextBlockKey);
            return new ChildReferencesInfo(totalSize, blockSize, nextBlockKey, lastBlockKey);
        }
        return null;
    }

    @Immutable
    public static class ChildReferencesInfo {
        public final long totalSize;
        public final long blockSize;
        public final String nextKey;
        public final String lastKey;

        public ChildReferencesInfo( long totalSize,
                                    long blockSize,
                                    String nextKey,
                                    String lastKey ) {
            this.totalSize = totalSize;
            this.blockSize = blockSize;
            this.nextKey = nextKey;
            this.lastKey = lastKey;
        }

        @Override
        public String toString() {
            return "totalSize: " + totalSize + "; blockSize: " + blockSize + "; nextKey: " + nextKey + "; lastKey: " + lastKey;
        }
    }

    protected ChildReference childReferenceFrom( Object value ) {
        if (value instanceof Document) {
            Document doc = (Document)value;
            String keyStr = doc.getString(KEY);
            NodeKey key = new NodeKey(keyStr);
            String nameStr = doc.getString(NAME);
            Name name = names.create(nameStr, decoder);
            // We always use 1 for the SNS index, since the SNS index is dependent upon SNS nodes before it
            return new ChildReference(key, name, 1);
        }
        return null;
    }

    public EditableDocument fromChildReference( ChildReference ref ) {
        // We don't write the
        return Schematic.newDocument(KEY, valueToDocument(ref.getKey(), null), NAME, strings.create(ref.getName()));
    }

    public Set<NodeKey> getReferrers( Document document,
                                      ReferenceType type ) {
        // Get the properties container ...
        Document referrers = document.getDocument(REFERRERS);
        if (referrers == null) {
            return new HashSet<NodeKey>();
        }

        // Get the NodeKeys in the respective arrays ...
        Set<NodeKey> result = new HashSet<NodeKey>();
        if (type != ReferenceType.WEAK) {
            Document strong = referrers.getDocument(STRONG);
            if (strong != null) {
                for (String keyString : strong.keySet()) {
                    result.add(new NodeKey(keyString));
                }
            }
        }
        if (type != ReferenceType.STRONG) {
            Document weak = referrers.getDocument(WEAK);
            if (weak != null) {
                for (String keyString : weak.keySet()) {
                    result.add(new NodeKey(keyString));
                }
            }
        }
        return result;
    }

    public void changeReferrers( EditableDocument document,
                                 ReferrerChanges changes ) {
        if (changes.isEmpty()) {
            // There are no changes requested ...
            return;
        }

        // Get the properties container ...
        EditableDocument referrers = document.getDocument(REFERRERS);
        List<NodeKey> strongAdded = changes.getAddedReferrers(ReferenceType.STRONG);
        List<NodeKey> weakAdded = changes.getAddedReferrers(ReferenceType.WEAK);

        if (referrers == null) {
            // There are no references in the document, so create it from the added references ...
            referrers = document.setDocument(REFERRERS);
            if (!strongAdded.isEmpty()) {
                Set<NodeKey> strongAddedSet = new HashSet<NodeKey>(strongAdded);
                EditableDocument strong = referrers.setDocument(STRONG);
                for (NodeKey key : strongAddedSet) {
                    strong.set(key.toString(), Collections.frequency(strongAdded, key));
                }
            }
            if (!weakAdded.isEmpty()) {
                Set<NodeKey> weakAddedSet = new HashSet<NodeKey>(weakAdded);
                EditableDocument weak = referrers.setDocument(WEAK);
                for (NodeKey key : weakAddedSet) {
                    weak.set(key.toString(), Collections.frequency(weakAdded, key));
                }
            }
            return;
        }

        // There are already some references, so update them
        List<NodeKey> strongRemoved = changes.getRemovedReferrers(ReferenceType.STRONG);
        Map<NodeKey, Integer> strongCount = computeReferrersCountDelta(strongAdded, strongRemoved);
        if (!strongCount.isEmpty()) {
            EditableDocument strong = referrers.getOrCreateDocument(STRONG);
            updateReferrers(strong, strongCount);
        }

        List<NodeKey> weakRemoved = changes.getRemovedReferrers(ReferenceType.WEAK);
        Map<NodeKey, Integer> weakCount = computeReferrersCountDelta(weakAdded, weakRemoved);
        if (!weakCount.isEmpty()) {
            EditableDocument weak = referrers.getOrCreateDocument(WEAK);
            updateReferrers(weak, weakCount);
        }
    }

    private void updateReferrers( EditableDocument owningDocument,
                                  Map<NodeKey, Integer> referrersCountDelta ) {
        for (NodeKey strongKey : referrersCountDelta.keySet()) {
            int newCount = referrersCountDelta.get(strongKey);
            String keyString = strongKey.toString();
            Integer existingCount = (Integer)owningDocument.get(keyString);

            if (existingCount != null) {
                int actualCount = existingCount + newCount;
                if (actualCount <= 0) {
                    owningDocument.remove(keyString);
                } else {
                    owningDocument.set(keyString, actualCount);
                }
            } else if (newCount > 0) {
                owningDocument.set(keyString, newCount);
            }
        }
    }

    /**
     * Given the lists of added & removed referrers (which may contain duplicates), compute the delta with which the count has to
     * be updated in the document
     *
     * @param addedReferrers the list of referrers that was added
     * @param removedReferrers the list of referrers that was removed
     * @return a map(nodekey, delta) pairs
     */
    private Map<NodeKey, Integer> computeReferrersCountDelta( List<NodeKey> addedReferrers,
                                                              List<NodeKey> removedReferrers ) {
        Map<NodeKey, Integer> referrersCountDelta = new HashMap<NodeKey, Integer>(0);

        Set<NodeKey> addedReferrersUnique = new HashSet<NodeKey>(addedReferrers);
        for (NodeKey addedReferrer : addedReferrersUnique) {
            int referrersCount = Collections.frequency(addedReferrers, addedReferrer) - Collections.frequency(removedReferrers,
                                                                                                              addedReferrer);
            referrersCountDelta.put(addedReferrer, referrersCount);
        }

        Set<NodeKey> removedReferrersUnique = new HashSet<NodeKey>(removedReferrers);
        for (NodeKey removedReferrer : removedReferrersUnique) {
            // process what's left in the removed list, only if not found in the added
            if (!referrersCountDelta.containsKey(removedReferrer)) {
                referrersCountDelta.put(removedReferrer, -1 * Collections.frequency(removedReferrers, removedReferrer));
            }
        }
        return referrersCountDelta;
    }

    protected Object valueToDocument( Object value,
                                      Set<BinaryKey> unusedBinaryKeys ) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String valueStr = (String)value;
            if (valueStr.length() < this.largeStringSize.get()) {
                // It's just a small string ...
                return value;
            }
            // Otherwise, this string is larger than our threshold, and we should treat it as a binary value ...
            value = binaries.create(valueStr);
            // and just continue, where the value will be processed below ...
        }
        if (value instanceof NodeKey) {
            return ((NodeKey)value).toString();
        }
        if (value instanceof UUID) {
            return Schematic.newDocument("$uuid", this.strings.create((UUID)value));
        }
        if (value instanceof Boolean) {
            return value;
        }
        if (value instanceof Long) {
            return value;
        }
        if (value instanceof Integer) {
            return new Long(((Integer)value).intValue());
        }
        if (value instanceof Double) {
            return value;
        }
        if (value instanceof Name) {
            Name name = (Name)value;
            return Schematic.newDocument("$name", name.getString(encoder));
        }
        if (value instanceof Path) {
            Path path = (Path)value;
            List<Object> segments = Schematic.newArray(path.size());
            for (Segment segment : path) {
                String str = segment.getString(encoder);
                segments.add(str);
            }
            boolean relative = !path.isAbsolute();
            return Schematic.newDocument("$path", segments, "$relative", relative);
        }
        if (value instanceof DateTime) {
            return Schematic.newDocument("$date", this.strings.create((DateTime)value));
        }
        if (value instanceof BigDecimal) {
            return Schematic.newDocument("$dec", this.strings.create((BigDecimal)value));
        }
        if (value instanceof Reference) {
            Reference ref = (Reference)value;
            String key = ref.isWeak() ? "$wref" : "$ref";
            String refString =
                    value instanceof NodeKeyReference ? ((NodeKeyReference)value).getNodeKey().toString() : this.strings.create(
                            ref);
            boolean isForeign = (value instanceof NodeKeyReference) && ((NodeKeyReference)value).isForeign();
            return Schematic.newDocument(key, refString, "$foreign", isForeign);
        }
        if (value instanceof URI) {
            return Schematic.newDocument("$uri", this.strings.create((URI)value));
        }
        if (value instanceof org.modeshape.jcr.value.BinaryValue) {
            org.modeshape.jcr.value.BinaryValue binary = (org.modeshape.jcr.value.BinaryValue)value;
            if (binary instanceof InMemoryBinaryValue) {
                return new Binary(((InMemoryBinaryValue)binary).getBytes());
            }
            // This is a large value ...
            String sha1 = binary.getHexHash();
            long size = binary.getSize();
            Document ref = Schematic.newDocument(SHA1_FIELD, sha1, "$len", size);

            // Find the document metadata and increment the usage count ...
            incrementBinaryReferenceCount(binary.getKey(), unusedBinaryKeys);

            // Now return the sha-1 reference ...
            return ref;
        }
        assert false : "Unexpected property value \"" + value + "\" of type " + value.getClass().getSimpleName();
        return null;
    }

    protected final String keyForBinaryReferenceDocument( String sha1 ) {
        return sha1 + "-ref";
    }

    /**
     * Increment the reference count for the stored binary value with the supplied SHA-1 hash.
     *
     * @param binaryKey the key for the binary value; never null
     * @param unusedBinaryKeys the set of binary keys that are considered unused; may be null
     */
    protected void incrementBinaryReferenceCount( BinaryKey binaryKey,
                                                  Set<BinaryKey> unusedBinaryKeys ) {
        // Find the document metadata and increment the usage count ...
        String sha1 = binaryKey.toString();
        String key = keyForBinaryReferenceDocument(sha1);
        SchematicEntry entry = documentStore.get(key);
        if (entry == null) {
            // The document doesn't yet exist, so create it ...
            Document content = Schematic.newDocument(SHA1, sha1, REFERENCE_COUNT, 1L);
            documentStore.localStore().put(key, content);
        } else {
            EditableDocument sha1Usage = entry.editDocumentContent();
            Long countValue = sha1Usage.getLong(REFERENCE_COUNT);
            sha1Usage.setNumber(REFERENCE_COUNT, countValue != null ? countValue + 1 : 1L);
        }
        // We're using the sha1, so remove it if its in the set of unused binary keys ...
        if (unusedBinaryKeys != null) {
            unusedBinaryKeys.remove(binaryKey);
        }
    }

    /**
     * Decrement the reference count for the binary value.
     *
     * @param fieldValue the value in the document that may contain a binary value reference; may be null
     * @param unusedBinaryKeys the set of binary keys that are considered unused; may be null
     * @return true if the binary value is no longer referenced, or false otherwise
     */
    protected boolean decrementBinaryReferenceCount( Object fieldValue,
                                                     Set<BinaryKey> unusedBinaryKeys ) {
        if (fieldValue instanceof List<?>) {
            for (Object value : (List<?>)fieldValue) {
                decrementBinaryReferenceCount(value, unusedBinaryKeys);
            }
        } else if (fieldValue instanceof Document) {
            Document docValue = (Document)fieldValue;
            String sha1 = docValue.getString(SHA1);
            if (sha1 != null) {
                // Find the document metadata and increment the usage count ...
                SchematicEntry entry = documentStore.get(sha1 + "-usage");
                EditableDocument sha1Usage = entry.editDocumentContent();
                Long countValue = sha1Usage.getLong(REFERENCE_COUNT);
                if (countValue == null) {
                    return true;
                }
                long count = countValue - 1;
                if (count < 0) {
                    count = 0;
                    // We're not using the binary value anymore ...
                    if (unusedBinaryKeys != null) {
                        unusedBinaryKeys.add(new BinaryKey(sha1));
                    }
                }
                sha1Usage.setNumber(REFERENCE_COUNT, count);
                return count <= 1;
            }
        }
        return false;
    }

    protected Object valueFromDocument( Object value ) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return value;
        }
        if (value instanceof Boolean) {
            return value;
        }
        if (value instanceof Long) {
            return value;
        }
        if (value instanceof Double) {
            return value;
        }
        if (value instanceof Binary) {
            Binary binary = (Binary)value;
            return binaries.create(binary.getBytes());
        }
        if (value instanceof URI) {
            URI uri = (URI)value;
            return uris.create(uri);
        }
        if (value instanceof List<?>) {
            List<?> values = (List<?>)value;
            int size = values.size();
            if (size == 0) {
                return Collections.emptyList();
            }
            List<Object> result = new ArrayList<Object>(values.size());
            for (Object val : values) {
                result.add(valueFromDocument(val));
            }
            return result;
        }
        if (value instanceof Document) {
            Document doc = (Document)value;
            String valueStr = null;
            List<?> array = null;
            if (!Null.matches(valueStr = doc.getString("$name"))) {
                return names.create(valueStr, decoder);
            }
            if (!Null.matches(array = doc.getArray("$path"))) {
                List<Segment> segments = segmentsFrom(array);
                boolean relative = doc.getBoolean("$relative");
                return relative ? paths.createRelativePath(segments) : paths.createAbsolutePath(segments);
            }
            if (!Null.matches(valueStr = doc.getString("$date"))) {
                return dates.create(valueStr);
            }
            if (!Null.matches(valueStr = doc.getString("$dec"))) {
                return decimals.create(valueStr);
            }
            if (!Null.matches(valueStr = doc.getString("$ref"))) {
                return createReferenceFromString(refs, doc, valueStr);
            }
            if (!Null.matches(valueStr = doc.getString("$wref"))) {
                return createReferenceFromString(weakrefs, doc, valueStr);
            }
            if (!Null.matches(valueStr = doc.getString("$uuid"))) {
                return uuids.create(valueStr);
            }
            if (!Null.matches(valueStr = doc.getString("$uri"))) {
                return uris.create(valueStr);
            }
            if (!Null.matches(valueStr = doc.getString(SHA1_FIELD))) {
                String sha1 = valueStr;
                long size = doc.getLong(LENGTH_FIELD);
                try {
                    return binaries.find(new BinaryKey(sha1), size);
                } catch (BinaryStoreException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        if (value instanceof Integer) {
            return longs.create(((Integer)value).longValue());
        }
        if (value instanceof Float) {
            return doubles.create(((Float)value).doubleValue());
        }
        assert false : "Unexpected document value \"" + value + "\" of type " + value.getClass().getSimpleName();
        return null;
    }

    private Object createReferenceFromString( ReferenceFactory referenceFactory,
                                              Document doc,
                                              String valueStr ) {
        boolean isForeign = doc.getBoolean("$foreign");
        if (NodeKey.isValidFormat(valueStr)) {
            return referenceFactory.create(new NodeKey(valueStr), isForeign);
        }
        try {
            UUID uuid = UUID.fromString(valueStr);
            return refs.create(new UuidReference(uuid));
        } catch (IllegalArgumentException e) {
            return refs.create(new StringReference(valueStr));
        }
    }

    protected List<Segment> segmentsFrom( List<?> segmentValues ) {
        List<Segment> segments = new ArrayList<Segment>(segmentValues.size());
        for (Object value : segmentValues) {
            Segment segment = paths.createSegment(value.toString(), decoder);
            segments.add(segment);
        }
        return segments;
    }

    protected Object resolveLargeValue( String sha1 ) {
        // NOTE: In the future, we may want to delay reading the large value. This could be accomplished
        // by create a Binary implementation that creates reads (and caches) the content. This would definitely
        // work for large Binary values, but would work for large String values only if we're not later
        // attempting to discover the PropertyType based upon the value (as the discovery process would
        // return BINARY for the large String value).

        // Look up the large value in the database ...
        SchematicEntry entry = documentStore.get(sha1);
        if (entry == null) {
            // The large value is no longer there, so return null ...
            return null;
        }
        if (entry.hasBinaryContent()) {
            // It's a large binary value ...
            return binaries.create(entry.getContentAsBinary().getBytes());
        }
        // Otherwise, it's just a large string value ...
        Document largeValueDoc = entry.getContentAsDocument();
        return largeValueDoc.getString(LARGE_VALUE);
    }

    /**
     * <p>
     * Note that this method changes the underlying db as well as the given document, so *it must* be called either from a
     * transactional context or it must be followed by a session.save call, otherwise there might be inconsistencies between what
     * a session sees as "persisted" state and the reality.
     * </p>
     *
     * @param key
     * @param document
     * @param targetCountPerBlock
     * @param tolerance
     */
    protected void optimizeChildrenBlocks( NodeKey key,
                                           EditableDocument document,
                                           int targetCountPerBlock,
                                           int tolerance ) {
        if (document == null) {
            SchematicEntry entry = documentStore.get(key.toString());
            if (entry == null) {
                return;
            }
            document = entry.editDocumentContent();
            if (document == null) {
                return;
            }
        }
        EditableArray children = document.getArray(CHILDREN);
        if (children == null) {
            // There are no children to optimize
            return;
        }

        // Get the children info
        EditableDocument info = document.getDocument(CHILDREN_INFO);
        boolean selfContained = true;
        if (info != null) {
            selfContained = !info.containsField(NEXT_BLOCK);
        }

        if (selfContained) {
            // This is a self-contained block; we only need to do something if the child count is larger than target +/- tolerance
            int total = children.size();
            if (total < targetCountPerBlock + tolerance) {
                // The number of children is small enough ...
                return;
            }
            // Otherwise, there are more children than our target + tolerance, so we need to split the children ...
            splitChildren(key, document, children, targetCountPerBlock, tolerance, true, null);
        } else {
            assert info != null;
            // This is not self-contained; there are already at least two blocks.
            // Go through each block, and either split it, merge it with the previous block, or leave it.
            EditableDocument doc = document;
            NodeKey docKey = key;
            while (doc != null) {
                EditableDocument docInfo = doc.getDocument(CHILDREN_INFO);
                String nextKey = docInfo != null ? docInfo.getString(NEXT_BLOCK) : null;
                children = doc.getArray(CHILDREN);
                int count = children.size();
                boolean isFirst = doc == document;
                if (count > (targetCountPerBlock + tolerance)) {
                    // This block is too big, so we should split it into multiple blocks...
                    splitChildren(docKey, doc, children, targetCountPerBlock, tolerance, isFirst, nextKey);
                } else if (count < (targetCountPerBlock - tolerance) && nextKey != null) {
                    // This block is too small, so always combine it with the next block, if there is one
                    // (even if that makes the next block too big, since it will be split in a later pass).
                    // Note that since we're only splitting if there is a next block, a last block that
                    // is too small will be left untouched. At this time, we think this is okay.
                    nextKey = mergeChildren(docKey, doc, children, isFirst, nextKey);

                    if (nextKey == null) {
                        // We merged the last block into this document, so we need to change the pointer in 'document'
                        // to be this doc ...
                        info.setString(LAST_BLOCK, docKey.toString());
                    }
                }
                // Otherwise, this block is just right

                // Find the next block ...
                if (nextKey != null) {
                    SchematicEntry nextEntry = documentStore.get(nextKey);
                    doc = nextEntry.editDocumentContent();
                    docKey = new NodeKey(nextKey);
                } else {
                    doc = null;
                }
            }
        }
    }

    /**
     * Split the children in the given document (with the given key) into two or more blocks, based upon the specified number of
     * desired children per block and a tolerance. This method will create additional blocks and will modify the supplied document
     * (with the smaller number of children and the pointer to the next block).
     * <p>
     * Note this method returns very quickly if the method determines that there is no work to do.
     * </p>
     * <p>
     * Note that this method changes the underlying db as well as the given document, so *it must* be called either from a
     * transactional context or it must be followed by a session.save call, otherwise there might be inconsistencies between what
     * a session sees as "persisted" state and the reality.
     * </p>
     *
     * @param key the key for the document whose children are to be split; may not be null
     * @param document the document whose children are to be split; may not be null
     * @param children the children that are to be split; may not be null
     * @param targetCountPerBlock the goal for the number of children in each block; must be positive
     * @param tolerance a tolerance that when added to and subtraced from the <code>targetCountPerBlock</code> gives an acceptable
     * range for the number of children; must be positive but smaller than <code>targetCountPerBlock</code>
     * @param isFirst true if the supplied document is the first node document, or false if it is a block document
     * @param nextBlock the key for the next block of children; may be null if the supplied document is the last document and
     * there is no next block
     * @return true if the children were split, or false if no changes were made
     */
    protected boolean splitChildren( NodeKey key,
                                     EditableDocument document,
                                     EditableArray children,
                                     int targetCountPerBlock,
                                     int tolerance,
                                     boolean isFirst,
                                     String nextBlock ) {
        assert 0 < targetCountPerBlock;
        assert 0 < tolerance;
        assert tolerance < targetCountPerBlock;
        // Calculate the number of blocks that we'll create and the size of the last block ...
        int total = children.size();
        int numFullBlocks = total / targetCountPerBlock;

        if (numFullBlocks == 0) {
            // This block doesn't need to be split ...
            return false;
        }

        int sizeOfLastBlock = total % targetCountPerBlock;
        if (sizeOfLastBlock < (targetCountPerBlock - tolerance)) {
            // The last block would be too small to be on its own ...
            if (numFullBlocks == 1) {
                // We would split into one full block and a second too-small block, so there's no point of splitting ...
                return false;
            }
            // We'll split it into multiple blocks, so we'll just include the children in the last too-small block
            // in the previous block ...
            sizeOfLastBlock = 0;
        }

        // The order we do things is important here. The best thing is to create and persist blocks 2...n immediately,
        // and then we can change the first document to have the smaller number of children and to point to the newly-created
        // block 2 (which points to block 3, etc.). This order means that anybody reading the input document never reads an
        // inconsistent set of children.
        int startIndex = targetCountPerBlock;
        int endIndex = 0;
        final String firstNewBlockKey = key.withRandomId().toString();
        String blockKey = firstNewBlockKey;
        for (int n = 1; n != numFullBlocks; ++n) {
            // Create the sublist of children that should be written to a new block ...
            boolean isLast = n == (numFullBlocks - 1);
            endIndex = isLast ? total : (startIndex + targetCountPerBlock);
            EditableArray blockChildren = Schematic.newArray(children.subList(startIndex, endIndex));

            // Create the new block, with a key that contains a UUID for the identifier ...
            String nextBlockKey = (isLast) ? nextBlockKey = nextBlock : key.withRandomId().toString();
            EditableDocument blockDoc = Schematic.newDocument();
            EditableDocument childInfo = blockDoc.setDocument(CHILDREN_INFO);
            childInfo.setNumber(BLOCK_SIZE, blockChildren.size());
            if (nextBlockKey != null) {
                childInfo.setString(NEXT_BLOCK, nextBlockKey);
            }

            // Write the children ...
            blockDoc.setArray(CHILDREN, blockChildren);

            // Now persist the new document ...
            documentStore.localStore().put(blockKey, blockDoc);

            // And get ready for the next block ...
            if (!isLast) {
                blockKey = nextBlockKey;
                startIndex = endIndex;
            }
        }

        // Now we can update the input document's children and nextBlock reference ...
        EditableArray newChildren = Schematic.newArray(children.subList(0, targetCountPerBlock));
        document.setArray(CHILDREN, newChildren);
        EditableDocument childInfo = document.getDocument(CHILDREN_INFO);
        if (childInfo == null) {
            childInfo = document.setDocument(CHILDREN_INFO);
        }
        childInfo.setNumber(BLOCK_SIZE, newChildren.size());
        childInfo.setString(NEXT_BLOCK, firstNewBlockKey);

        if (isFirst && nextBlock == null) {
            // We generated a new last block and we have to update the reference ...
            childInfo.setString(LAST_BLOCK, blockKey);
        }

        // Note we never changed the number of children, so we don't need to update 'count'.
        return true;
    }

    /**
     * Modify the supplied document (with the given key) to merge in all of the children from the next block. If the next block is
     * empty or contains no children, it will be deleted its next block merged. Note that this merging is performed, even if the
     * resulting number of children is considered 'too-large' (as such 'too-large' blocks will be optimized at a subsequent
     * optimization pass).
     * <p>
     * Note that this method changes the underlying db as well as the given document, so *it must* be called either from a
     * transactional context or it must be followed by a session.save call, otherwise there might be inconsistencies between what
     * a session sees as "persisted" state and the reality.
     * </p>
     *
     * @param key the key for the document whose children are to be merged with the next block; may not be null
     * @param document the document to be modified with the next block's children; may not be null
     * @param children the children into which are to be merged the next block's children; may not be null
     * @param isFirst true if the supplied document is the first node document, or false if it is a block document
     * @param nextBlock the key for the next block of children; may be null if the supplied document is the last document and
     * there is no next block
     * @return the key for the block of children that is after blocks that are removed; may be null if the supplied document is
     *         the last block
     */
    protected String mergeChildren( NodeKey key,
                                    EditableDocument document,
                                    EditableArray children,
                                    boolean isFirst,
                                    String nextBlock ) {
        // The children in the next block should be added to the children in this block, even if the size would be too large
        // as any too-large blocks will eventually be optimized later ...
        EditableDocument info = document.getDocument(CHILDREN_INFO);
        if (info == null) {
            info = document.setDocument(CHILDREN_INFO);
        }

        // First, find the next block that we can use ...
        Set<String> toBeDeleted = new HashSet<String>();
        SchematicEntry nextEntry = null;
        String nextBlocksNext = null;
        while (nextBlock != null) {
            nextEntry = documentStore.get(nextBlock);
            Document nextDoc = nextEntry.getContentAsDocument();
            List<?> nextChildren = nextDoc.getArray(CHILDREN);
            Document nextInfo = nextDoc.getDocument(CHILDREN_INFO);

            if (nextChildren == null || nextChildren.isEmpty()) {
                // Delete this empty block ...
                toBeDeleted.add(nextBlock);
                nextEntry = null;

                // And figure out the next block ...
                nextBlock = nextInfo != null ? nextInfo.getString(NEXT_BLOCK) : null;
            } else {
                // We can use this block, so copy the children into it ...
                children.addAll(nextChildren);

                // Figure out the key for the next block ...
                nextBlocksNext = nextInfo != null ? nextInfo.getString(NEXT_BLOCK) : null;

                if (isFirst && nextBlocksNext == null) {
                    // This is the first block and there is no more, so set the count and remove the block-related fields ...
                    info.setNumber(COUNT, children.size());
                    info.remove(NEXT_BLOCK);
                    info.remove(LAST_BLOCK);
                } else {
                    // Just update the block size and the next block ...
                    info.setNumber(BLOCK_SIZE, children.size());
                    info.setString(NEXT_BLOCK, nextBlocksNext);
                }

                // And then mark it for deletion ...
                toBeDeleted.add(nextBlock);
                nextBlock = null;
            }
        }

        // Now that we've updated the input document, delete any entries that are no longer needed ...
        for (String deleteKey : toBeDeleted) {
            documentStore.remove(deleteKey);
        }

        return nextBlocksNext;
    }

    /**
     * Checks if the given document is already locked
     *
     * @param doc the document
     * @return true if the change was made successfully, or false otherwise
     */
    public boolean isLocked( EditableDocument doc ) {
        return hasProperty(doc, JcrLexicon.LOCK_OWNER) || hasProperty(doc, JcrLexicon.LOCK_IS_DEEP);
    }

    /**
     * Given an existing document adds a new federated segment with the given alias pointing to the external document located
     * at {@code externalPath}
     *
     * @param document a {@code non-null} {@link EditableDocument} representing the document of a local node to which the federated
     * segment should be appended.
     * @param sourceName a {@code non-null} string, the name of the source where {@code externalPath} will be resolved
     * @param externalPath a {@code non-null} string the location in the external source which points to an external node
     * @param alias an optional string representing the name under which the federated segment will be linked. In effect, this
     * represents the name of a child reference. If not present, the {@code externalPath} will be used.
     *
     * Note that the name of the federated segment (either coming from {@code externalPath} or {@code alias}) should not
     * contain the "/" character. If it does, this will be removed.
     */
    public void addFederatedSegment( EditableDocument document,
                                     String sourceName,
                                     String externalPath,
                                     String alias ) {
        EditableArray federatedSegmentsArray = document.getArray(FEDERATED_SEGMENTS);
        if (federatedSegmentsArray == null) {
            federatedSegmentsArray = Schematic.newArray();
            document.set(FEDERATED_SEGMENTS, federatedSegmentsArray);
        }

        String parentKey = document.getString(KEY);
        String externalNodeKey = documentStore.createExternalProjection(parentKey, sourceName, externalPath);
        if (!StringUtil.isBlank(externalNodeKey)) {
            String segmentName = !StringUtil.isBlank(alias) ? alias : externalPath;
            if (segmentName.endsWith("/")) {
                segmentName = segmentName.substring(0, segmentName.length() - 1);
            }
            if (segmentName.contains("/")) {
                segmentName = segmentName.substring(segmentName.lastIndexOf("/") + 1);
            }
            EditableDocument federatedSegment = DocumentFactory.newDocument(KEY, externalNodeKey, NAME, segmentName);
            federatedSegmentsArray.add(federatedSegment);
        }
    }

    /**
     * Returns the value of the {@link org.modeshape.jcr.cache.document.DocumentTranslator#CACHE_TTL_SECONDS} field,
     * if such a value exists.
     * @param document a {@code non-null} document
     * @return either the value of the above field, or {@code null} if such a value doesn't exist.
     */
    public Integer getCacheTtlSeconds(Document document) {
        return document.getInteger(CACHE_TTL_SECONDS);
    }
}
