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
import java.util.LinkedList;
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
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode.ReferenceType;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryEnvironment;
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
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.basic.NodeKeyReference;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.EmptyBinaryValue;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.jcr.value.binary.InMemoryBinaryValue;

/**
 * A utility class that encapsulates all the logic for reading from and writing to {@link Document} instances.
 */
public class DocumentTranslator implements DocumentConstants {

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
    private final ReferenceFactory simplerefs;
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
        this.simplerefs = this.factories.getSimpleReferenceFactory();
        this.strings = this.factories.getStringFactory();
        assert this.largeStringSize.get() >= 0;
    }

    public DocumentTranslator withLargeStringSize( long largeStringSize ) {
        return new DocumentTranslator(context, documentStore, largeStringSize);
    }

    public final ValueFactory<String> getStringFactory() {
        return strings;
    }

    public final NameFactory getNameFactory() {
        return names;
    }

    public final ReferenceFactory getReferenceFactory() {
        return this.refs;
    }

    public final PropertyFactory getPropertyFactory() {
        return propertyFactory;
    }

    void setMinimumStringLengthForBinaryStorage( long largeValueSize ) {
        assert largeValueSize > -1;
        this.largeStringSize.set(largeValueSize);
    }

    /**
     * Obtain the preferred {@link NodeKey key} for the parent of this node. Because a node can be used in more than once place,
     * it may technically have more than one parent. Therefore, in such cases this method prefers the parent that is in the
     * {@code primaryWorkspaceKey} and, if there is no such parent, the parent that is in the {@code secondaryWorkspaceKey}.
     * 
     * @param document the document for the node; may not be null
     * @param primaryWorkspaceKey the key for the workspace in which the parent should preferably exist; may be null
     * @param secondaryWorkspaceKey the key for the workspace in which the parent should exist if not in the primary workspace;
     *        may be null
     * @return the key representing the preferred parent, or null if the document contains no parent reference or if the parent
     *         reference(s) do not have the specified workspace keys
     */
    public NodeKey getParentKey( Document document,
                                 String primaryWorkspaceKey,
                                 String secondaryWorkspaceKey ) {
        Object value = document.get(PARENT);
        return keyFrom(value, primaryWorkspaceKey, secondaryWorkspaceKey);
    }

    public Set<NodeKey> getAdditionalParentKeys( Document document ) {
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
                keys.add(new NodeKey(key));
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
                                 String propertyName ) {
        return getProperty(document, names.create(propertyName));
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

    public Name getPrimaryType( Document document ) {
        Property primaryType = getProperty(document, JcrLexicon.PRIMARY_TYPE);
        return primaryType != null ? names.create(primaryType.getFirstValue()) : null;
    }

    public String getPrimaryTypeName( Document document ) {
        return strings.create(getProperty(document, JcrLexicon.PRIMARY_TYPE).getFirstValue());
    }

    public Set<Name> getMixinTypes( Document document ) {
        Property prop = getProperty(document, JcrLexicon.MIXIN_TYPES);
        if (prop == null || prop.size() == 0) return Collections.emptySet();

        if (prop.size() == 1) {
            Name name = names.create(prop.getFirstValue());
            return Collections.singleton(name);
        }
        Set<Name> result = new HashSet<Name>();
        for (Object value : prop) {
            Name name = names.create(value);
            result.add(name);
        }
        return result;
    }

    public Set<String> getMixinTypeNames( Document document ) {
        Property prop = getProperty(document, JcrLexicon.MIXIN_TYPES);
        if (prop == null || prop.size() == 0) return Collections.emptySet();

        if (prop.size() == 1) {
            String name = strings.create(prop.getFirstValue());
            return Collections.singleton(name);
        }
        Set<String> result = new HashSet<String>();
        for (Object value : prop) {
            String name = strings.create(value);
            result.add(name);
        }
        return result;
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
                             Set<BinaryKey> unusedBinaryKeys,
                             Set<BinaryKey> usedBinaryKeys) {
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
        decrementBinaryReferenceCount(oldValue, unusedBinaryKeys, usedBinaryKeys);

        // Now set the property ...
        if (property.isEmpty()) {
            urlProps.setArray(localName);
        } else if (property.isMultiple()) {
            EditableArray values = Schematic.newArray(property.size());
            for (Object v : property) {
                values.add(valueToDocument(v, unusedBinaryKeys, usedBinaryKeys));
            }
            urlProps.setArray(localName, values);
        } else {
            assert property.isSingle();
            Object value = valueToDocument(property.getFirstValue(), unusedBinaryKeys, usedBinaryKeys);
            if (value == null) {
                urlProps.remove(localName);
            } else {
                urlProps.set(localName, value);
            }
        }
    }

    public Property removeProperty( EditableDocument document,
                                    Name propertyName,
                                    Set<BinaryKey> unusedBinaryKeys,
                                    Set<BinaryKey> usedBinaryKeys) {
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
        decrementBinaryReferenceCount(fieldValue, unusedBinaryKeys, usedBinaryKeys);

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
                                   Set<BinaryKey> unusedBinaryKeys,
                                   Set<BinaryKey> usedBinaryKeys) {
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
                    array.addValue(valueToDocument(value, unusedBinaryKeys, usedBinaryKeys));
                }
                urlProps.setArray(localName, array);
            } else {
                urlProps.set(localName, valueToDocument(values.iterator().next(), unusedBinaryKeys, usedBinaryKeys));
            }
        } else if (propValue instanceof List<?>) {
            // Decrement the reference count of any binary references ...
            decrementBinaryReferenceCount(propValue, unusedBinaryKeys, usedBinaryKeys);

            // There's an existing property with multiple values ...
            EditableArray array = urlProps.getArray(localName);
            for (Object value : values) {
                value = valueToDocument(value, unusedBinaryKeys, usedBinaryKeys);
                array.addValueIfAbsent(value);
            }
        } else {
            // Decrement the reference count of any binary references ...
            decrementBinaryReferenceCount(propValue, unusedBinaryKeys, usedBinaryKeys);

            // There's just a single value ...
            if (numValues == 1) {
                Object value = valueToDocument(values.iterator().next(), unusedBinaryKeys, usedBinaryKeys);
                if (!value.equals(propValue)) {
                    // But the existing value is different, so we have to change to an array ...
                    EditableArray array = Schematic.newArray(value, propValue);
                    urlProps.setArray(localName, array);
                }
            } else {
                EditableArray array = Schematic.newArray(numValues);
                for (Object value : values) {
                    value = valueToDocument(value, unusedBinaryKeys, usedBinaryKeys);
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
                                      Set<BinaryKey> unusedBinaryKeys,
                                      Set<BinaryKey> usedBinaryKeys) {
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
        decrementBinaryReferenceCount(propValue, unusedBinaryKeys, usedBinaryKeys);

        if (propValue instanceof List<?>) {
            // There's an existing property with multiple values ...
            EditableArray array = urlProps.getArray(localName);
            for (Object value : values) {
                value = valueToDocument(value, null, null);
                array.remove(value);
            }
        } else if (propValue != null) {
            // There's just a single value ...
            for (Object value : values) {
                value = valueToDocument(value, unusedBinaryKeys, usedBinaryKeys);
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
                    parents.remove((Object)oldParent.toString());
                }
            }
            if (additionalParents != null) {
                for (NodeKey removedParent : additionalParents.getRemovals()) {
                    // When the primary parent is removed and changed to one of the additional parents, then the additional
                    // parent is removed. Therefore, we only want to remove it if it does not equal the new parent ...
                    if (!removedParent.equals(parent)) {
                        parents.remove((Object)removedParent.toString()); // remove by value (not by name)
                    }
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
                    parents.remove((Object)removed.toString());
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

    public void setKey( EditableDocument document,
                        String key ) {
        assert key != null;
        document.setString(KEY, key);
    }

    public String getKey( Document document ) {
        return document.getString(KEY);
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
                // we need to clean up projections
                if (isFederatedDocument(doc) && !removals.isEmpty()) {
                    Set<String> removalsStrings = new HashSet<String>();
                    for (NodeKey key : removals) {
                        // only when we're dealing with a foreign key do we need to do this
                        if (!key.toString().startsWith(documentStore.getLocalSourceKey())) {
                            removalsStrings.add(key.toString());
                        }
                    }
                    removeFederatedSegments(doc, removalsStrings);
                }

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
                    assert docInfo != null && docInfo.nextKey != null;
                    // There is more than one block, so update the block size ...
                    doc.getDocument(CHILDREN_INFO).setNumber(BLOCK_SIZE, blockCount);

                    doc = documentStore.edit(docInfo.nextKey, true);
                    lastDoc = doc;
                    assert docInfo != null;
                    lastDocKey = docInfo.nextKey;
                } else {
                    if (doc == document && doc.containsField(CHILDREN_INFO)) {
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
                lastDoc = documentStore.edit(lastKey, true);
            } else {
                lastKey = null;
            }
            // Just append the new children to the end of the last document; we can use an asynchronous process
            // to adjust/optimize the number of children in each block ...
            EditableArray lastChildren = lastDoc.getOrCreateArray(CHILDREN);
            for (ChildReference ref : appended) {
                lastChildren.add(fromChildReference(ref));
            }

            if (lastDoc != document) {
                // We've written to at least one other document, so update the block size ...
                EditableDocument lastDocInfo = lastDoc.getOrCreateDocument(CHILDREN_INFO);
                lastDocInfo.setNumber(BLOCK_SIZE, lastChildren.size());
            }

            // And update the total size and last block on the starting document ...
            EditableDocument childInfo = document.getOrCreateDocument(CHILDREN_INFO);
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
        LinkedHashSet<ChildReference> newChildren = new LinkedHashSet<ChildReference>();
        if (children != null) {
            // process existing children
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
                        newChildren.add(inserted);
                    }
                }
                if (removals.remove(childKey)) {
                    // The node is removed ...
                } else {
                    // The node remains ...
                    Name newName = newNames.get(childKey);
                    if (newName != null) {
                        // But has been renamed ...
                        ref = ref.with(newName, 1);
                    }
                    newChildren.add(ref);
                }
            }
        }

        if (!insertionsByBeforeKey.isEmpty()) {
            // there are transient insertions (due to reordering of transient nodes) that have to be inserted in a correct order
            // if any reorderings involved existing children, they would have already been removed by the previous block
            // note that these insertions have to be added as child because *they do not appear* in the appended list
            LinkedList<ChildReference> toBeInsertedInOrder = new LinkedList<ChildReference>();
            for (Insertions insertion : insertionsByBeforeKey.values()) {
                // process the remaining insertions-before, which indicate transient & reordered children (reordering removes
                // children
                // from the appended list
                for (ChildReference activeReference : insertion.inserted()) {
                    if (toBeInsertedInOrder.contains(activeReference)) {
                        // the current reference is already in the list
                        continue;
                    }
                    Insertions insertionsBeforeActive = insertionsByBeforeKey.get(activeReference.getKey());
                    if (insertionsBeforeActive == null) {
                        toBeInsertedInOrder.addFirst(activeReference);
                        continue;
                    }
                    for (ChildReference referenceBeforeActive : insertionsBeforeActive.inserted()) {
                        if (!toBeInsertedInOrder.contains(referenceBeforeActive)) {
                            toBeInsertedInOrder.add(referenceBeforeActive);
                        }
                    }
                    toBeInsertedInOrder.add(activeReference);
                }
            }
            newChildren.addAll(toBeInsertedInOrder);
        }

        EditableArray newChildrenArray = Schematic.newArray(newChildren.size());
        for (ChildReference childReference : newChildren) {
            newChildrenArray.add(fromChildReference(childReference));
        }
        document.set(CHILDREN, newChildrenArray);
        return newChildren.size();
    }

    public ChildReferences getChildReferences( WorkspaceCache cache,
                                               Document document ) {
        Name primaryType = getPrimaryType(document);
        Set<Name> mixinTypes = getMixinTypes(document);
        NodeTypes nodeTypes = getNodeTypes(cache);

        boolean isUnorderedCollection = nodeTypes != null && nodeTypes.isUnorderedCollection(primaryType, mixinTypes);
        if (isUnorderedCollection) {
            return new BucketedChildReferences(document, this);
        }

        boolean hasChildren = document.containsField(CHILDREN);
        boolean hasFederatedSegments = document.containsField(FEDERATED_SEGMENTS);
        if (!hasChildren && !hasFederatedSegments) {
            return ImmutableChildReferences.EMPTY_CHILD_REFERENCES;
        }

        boolean allowsSNS = nodeTypes == null || nodeTypes.allowsNameSiblings(primaryType, mixinTypes);
        ChildReferences internalChildRefs = hasChildren ? ImmutableChildReferences.create(this, document, CHILDREN, allowsSNS) : ImmutableChildReferences.EMPTY_CHILD_REFERENCES;
        ChildReferences externalChildRefs = hasFederatedSegments ? ImmutableChildReferences.create(this, document,
                                                                                                   FEDERATED_SEGMENTS, allowsSNS) : ImmutableChildReferences.EMPTY_CHILD_REFERENCES;

        // Now look at the 'childrenInfo' document for info about the next block of children ...
        ChildReferencesInfo info = getChildReferencesInfo(document);
        if (!hasChildren) {
            return ImmutableChildReferences.create(externalChildRefs, info, cache, allowsSNS);
        } else if (!hasFederatedSegments) {
            return ImmutableChildReferences.create(internalChildRefs, info, cache, allowsSNS);
        } else {
            return ImmutableChildReferences.create(internalChildRefs, info, externalChildRefs, cache, allowsSNS);
        }
    }

    protected NodeTypes getNodeTypes( WorkspaceCache cache ) {
        RepositoryEnvironment repositoryEnvironment = cache.repositoryEnvironment();
        if (repositoryEnvironment == null) {
            return null;
        }
        return repositoryEnvironment.nodeTypes();
    }

    /**
     * Reads the children of the given block and returns a {@link ChildReferences} instance.
     * 
     * @param block a {@code non-null} {@link Document} representing a block of children
     * @param allowsSNS {@code true} if the child references instance should be SNS aware, {@code false} otherwise
     * @return a {@code non-null} child references instance
     */
    protected ChildReferences getChildReferencesFromBlock( Document block, boolean allowsSNS ) {
        if (!block.containsField(CHILDREN)) {
            return ImmutableChildReferences.EMPTY_CHILD_REFERENCES;
        }
        return ImmutableChildReferences.create(this, block, CHILDREN, allowsSNS);
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
        return Schematic.newDocument(KEY, valueToDocument(ref.getKey(), null, null), NAME, strings.create(ref.getName()));
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

    public Map<NodeKey, Integer> getReferrerCounts( Document document,
                                                    ReferenceType type ) {
        // Get the properties container ...
        Document referrers = document.getDocument(REFERRERS);
        if (referrers == null) {
            return Collections.emptyMap();
        }

        // Get the NodeKeys in the respective arrays ...
        Map<NodeKey, Integer> result = new HashMap<>();
        if (type == ReferenceType.STRONG || type == ReferenceType.BOTH) {
            Document strong = referrers.getDocument(STRONG);
            if (strong != null) {
                for (String keyString : strong.keySet()) {
                    result.put(new NodeKey(keyString), strong.getInteger(keyString));
                }
            }
        }
        if (type == ReferenceType.WEAK || type == ReferenceType.BOTH) {
            Document weak = referrers.getDocument(WEAK);
            if (weak != null) {
                for (String keyString : weak.keySet()) {
                    result.put(new NodeKey(keyString), weak.getInteger(keyString));
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
            int referrersCount = Collections.frequency(addedReferrers, addedReferrer)
                                 - Collections.frequency(removedReferrers, addedReferrer);
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
                                      Set<BinaryKey> unusedBinaryKeys,
                                      Set<BinaryKey> usedBinaryKeys) {
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
            String key = null;
            if (ref.isSimple()) {
                key = SIMPLE_REFERENCE_FIELD;
            } else {
                key = ref.isWeak() ? WEAK_REFERENCE_FIELD : REFERENCE_FIELD;
            }

            String refString = ref instanceof NodeKeyReference ? ((NodeKeyReference)ref).getNodeKey().toString() : this.strings.create(ref);
            boolean isForeign = ref.isForeign();
            return Schematic.newDocument(key, refString, "$foreign", isForeign);
        }
        if (value instanceof URI) {
            return Schematic.newDocument("$uri", this.strings.create((URI)value));
        }
        if (value instanceof ExternalBinaryValue) {
            ExternalBinaryValue externalBinaryValue = (ExternalBinaryValue)value;
            return Schematic.newDocument(EXTERNAL_BINARY_ID_FIELD, externalBinaryValue.getId(), SOURCE_NAME_FIELD,
                                         externalBinaryValue.getSourceName());
        }
        if (value instanceof org.modeshape.jcr.value.BinaryValue) {
            org.modeshape.jcr.value.BinaryValue binary = (org.modeshape.jcr.value.BinaryValue)value;
            if (binary instanceof InMemoryBinaryValue) {
                return new Binary(((InMemoryBinaryValue)binary).getBytes());
            }
            // This is a large value ...
            String sha1 = binary.getHexHash();
            long size = binary.getSize();
            Document ref = Schematic.newDocument(SHA1_FIELD, sha1, LENGTH_FIELD, size);

            // Find the document metadata and increment the usage count ...
            incrementBinaryReferenceCount(binary.getKey(), unusedBinaryKeys, usedBinaryKeys);

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
     * @param usedBinaryKeys the set of binary keys that are considered used; may be null
     */
    protected void incrementBinaryReferenceCount( BinaryKey binaryKey,
                                                  Set<BinaryKey> unusedBinaryKeys,
                                                  Set<BinaryKey> usedBinaryKeys ) {
        // Find the document metadata and increment the usage count ...
        String sha1 = binaryKey.toString();
        String key = keyForBinaryReferenceDocument(sha1);
        // don't acquire a lock since we've already done this at the beginning of the #save
        EditableDocument entry = documentStore.edit(key, false, false);
        if (entry == null) {
            // The document doesn't yet exist, so create it ...
            Document content = Schematic.newDocument(SHA1, sha1, REFERENCE_COUNT, 1L);
            documentStore.localStore().put(key, content);
        } else {
            Long countValue = entry.getLong(REFERENCE_COUNT);
            entry.setNumber(REFERENCE_COUNT, countValue != null ? countValue + 1 : 1L);
        }
        // We're using the sha1, so remove it if its in the set of unused binary keys ...
        if (unusedBinaryKeys != null) {
            unusedBinaryKeys.remove(binaryKey);
        }
        if (usedBinaryKeys != null) {
            usedBinaryKeys.add(binaryKey);
        }
    }

    /**
     * Decrement the reference count for the binary value.
     * 
     * @param fieldValue the value in the document that may contain a binary value reference; may be null
     * @param unusedBinaryKeys the set of binary keys that are considered unused; may be null
     * @param usedBinaryKeys the set of binary keys that are considered used; may be null
     */
    protected void decrementBinaryReferenceCount( Object fieldValue,
                                                  Set<BinaryKey> unusedBinaryKeys,
                                                  Set<BinaryKey> usedBinaryKeys) {
        if (fieldValue instanceof List<?>) {
            for (Object value : (List<?>)fieldValue) {
                decrementBinaryReferenceCount(value, unusedBinaryKeys, usedBinaryKeys);
            }
        } else if (fieldValue instanceof Object[]) {
            for (Object value : (Object[])fieldValue) {
                decrementBinaryReferenceCount(value, unusedBinaryKeys, usedBinaryKeys);
            }
        } else {
            String sha1 = null;
            if (fieldValue instanceof Document) {
                Document docValue = (Document)fieldValue;
                sha1 = docValue.getString(SHA1_FIELD);
            } else if (fieldValue instanceof BinaryKey) {
                sha1 = fieldValue.toString();
            } else if (fieldValue instanceof org.modeshape.jcr.api.Binary && !(fieldValue instanceof InMemoryBinaryValue)) {
                sha1 = ((org.modeshape.jcr.api.Binary)fieldValue).getHexHash();
            }

            if (sha1 != null) {
                BinaryKey binaryKey = new BinaryKey(sha1);
                // Find the document metadata and decrement the usage count ...
                // Don't acquire a lock since we should've done so at the beginning of the #save method
                EditableDocument sha1Usage = documentStore.edit(keyForBinaryReferenceDocument(sha1), false, false);
                if (sha1Usage != null) {
                    Long countValue = sha1Usage.getLong(REFERENCE_COUNT);
                    assert countValue != null;

                    long count = countValue - 1;
                    assert count >= 0;

                    if (count == 0) {
                        // We're not using the binary value anymore ...
                        if (unusedBinaryKeys != null) {
                            unusedBinaryKeys.add(binaryKey);
                        }
                        if (usedBinaryKeys != null) {
                            usedBinaryKeys.remove(binaryKey);
                        }
                    }
                    sha1Usage.setNumber(REFERENCE_COUNT, count);
                } else {
                    // The documentStore doesn't contain the binary ref count doc, so we're no longer using the binary value ...
                    if (unusedBinaryKeys != null) {
                        unusedBinaryKeys.add(binaryKey);
                    }
                    if (usedBinaryKeys != null) {
                        usedBinaryKeys.remove(binaryKey);
                    }
                }
            }
        }
    }

    public Object valueFromDocument( Object value ) {
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
            if (!Null.matches(valueStr = doc.getString(REFERENCE_FIELD))) {
                return createReferenceFromString(refs, doc, valueStr);
            }
            if (!Null.matches(valueStr = doc.getString(WEAK_REFERENCE_FIELD))) {
                return createReferenceFromString(weakrefs, doc, valueStr);
            }
            if (!Null.matches(valueStr = doc.getString(SIMPLE_REFERENCE_FIELD))) {
                return createReferenceFromString(simplerefs, doc, valueStr);
            }
            if (!Null.matches(valueStr = doc.getString("$uuid"))) {
                return UUID.fromString(valueStr);
            }
            if (!Null.matches(valueStr = doc.getString("$uri"))) {
                return uris.create(valueStr);
            }
            if (!Null.matches(valueStr = doc.getString(EXTERNAL_BINARY_ID_FIELD))) {
                String sourceName = doc.getString(SOURCE_NAME_FIELD);
                ExternalBinaryValue externalBinaryValue = documentStore.getExternalBinary(sourceName, valueStr);
                return externalBinaryValue != null ? externalBinaryValue : EmptyBinaryValue.INSTANCE;
            }
            if (!Null.matches(valueStr = doc.getString(SHA1_FIELD))) {
                long size = doc.getLong(LENGTH_FIELD);
                try {
                    return binaries.find(new BinaryKey(valueStr), size);
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
        if (!NodeKey.isValidFormat(valueStr)) {
            throw new IllegalStateException("The reference " + valueStr + " is corrupt: expected a node-key reference");
        }
        return referenceFactory.create(new NodeKey(valueStr), isForeign);
    }

    protected List<Segment> segmentsFrom( List<?> segmentValues ) {
        List<Segment> segments = new ArrayList<Segment>(segmentValues.size());
        for (Object value : segmentValues) {
            Segment segment = paths.createSegment(value.toString(), decoder);
            segments.add(segment);
        }
        return segments;
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

    protected boolean isFederatedDocument( Document document ) {
        return document.containsField(FEDERATED_SEGMENTS);
    }

    protected void removeFederatedSegments( EditableDocument federatedDocument,
                                            Set<String> externalNodeKeys ) {
        if (!federatedDocument.containsField(FEDERATED_SEGMENTS)) {
            return;
        }
        EditableArray federatedSegments = federatedDocument.getArray(FEDERATED_SEGMENTS);
        for (int i = 0; i < federatedSegments.size(); i++) {
            Object federatedSegment = federatedSegments.get(i);
            assert federatedSegment instanceof Document;
            String segmentKey = getKey((Document)federatedSegment);
            if (externalNodeKeys.contains(segmentKey)) {
                federatedSegments.remove(i);
            }
        }
        if (federatedSegments.isEmpty()) {
            federatedDocument.remove(FEDERATED_SEGMENTS);
        }
    }

    protected boolean isQueryable( Document document ) {
        // all documents are considered queryable by default
        return document.getBoolean(QUERYABLE_FIELD, true);
    }

    /**
     * Marks the given document as queryable, by setting a flag.
     * 
     * @param document a {@link EditableDocument} instance; never null
     * @param queryable a boolean which indicates whether the document should be queryable or not.
     */
    public void setQueryable( EditableDocument document,
                              boolean queryable ) {
        document.set(QUERYABLE_FIELD, queryable);
    }

    protected void addFederatedSegment( EditableDocument document,
                                        String externalNodeKey,
                                        String name ) {
        EditableArray federatedSegmentsArray = document.getArray(FEDERATED_SEGMENTS);
        if (federatedSegmentsArray == null) {
            federatedSegmentsArray = Schematic.newArray();
            document.set(FEDERATED_SEGMENTS, federatedSegmentsArray);
        }

        if (!StringUtil.isBlank(externalNodeKey)) {
            EditableDocument federatedSegment = DocumentFactory.newDocument(KEY, externalNodeKey, NAME, name);
            federatedSegmentsArray.add(federatedSegment);
        }
    }

    /**
     * Returns the value of the {@link org.modeshape.jcr.cache.document.DocumentTranslator#CACHE_TTL_SECONDS} field, if such a
     * value exists.
     * 
     * @param document a {@code non-null} document
     * @return either the value of the above field, or {@code null} if such a value doesn't exist.
     */
    protected Integer getCacheTtlSeconds( Document document ) {
        return document.getInteger(CACHE_TTL_SECONDS);
    }
    
    protected String bucketKey( String parentKey, String bucketId ) {
        return parentKey + "/" + bucketId;
    }
    
    protected BucketedChildReferences.Bucket loadBucket( String parentKey, BucketId bucketId ) {
        String bucketKey = bucketKey(parentKey, bucketId.toString());
        SchematicEntry schematicEntry = documentStore.get(bucketKey);
        if (schematicEntry == null) {
            return null;
        }
        return new BucketedChildReferences.Bucket(bucketId, schematicEntry.getContent(), this);
    }
    
    protected void addChildrenToBuckets( EditableDocument parentDoc,
                                         ChildReferences appended ) {
        assert appended != null;
        
        long totalAdditions = 0;
        Integer bucketIdLength = parentDoc.getInteger(BUCKET_ID_LENGTH);     
        assert bucketIdLength != null;
        String parentKey = getKey(parentDoc);

        // add all the new children into their corresponding buckets, creating each bucket if it does not exist
        // we don't need to worry about locking here because the parent document should've already been locked at the beginning
        // of the transaction ensuring (theoretically ;) linearizability 
        Map<BucketId, Set<ChildReference>> additionsPerBucket = new HashMap<>((int)appended.size());

        // first collect all the new references into buckets...
        for (ChildReference inserted : appended) {
            Name insertedName = inserted.getName();
            BucketId bucketId = new BucketId(insertedName, bucketIdLength);
            Set<ChildReference> additions = additionsPerBucket.get(bucketId);
            if (additions == null) {
                additions = new HashSet<>();
                additionsPerBucket.put(bucketId, additions);
            }
            additions.add(inserted);
            ++totalAdditions;
        }

        //then insert them into each bucket
        for (Map.Entry<BucketId, Set<ChildReference>> entry : additionsPerBucket.entrySet()) {
            BucketId bucketId = entry.getKey();
            String bucketKey = bucketKey(parentKey, bucketId.toString());
            boolean newBucket = !documentStore.containsKey(bucketKey);
            EditableDocument bucketDoc = documentStore.edit(bucketKey, true, false);
            assert bucketDoc != null;
            for (ChildReference ref : entry.getValue()) {
                // we store each key,name pair directly in the bucket
                String key = ref.getKey().toString();
                String name = strings.create(ref.getName());
                bucketDoc.setString(key, name);
            }

            if (newBucket) {
                // store the bucket id into the parent
                parentDoc.getOrCreateArray(BUCKETS).add(bucketId.toString());
            }
        }

        Long currentSize = parentDoc.getLong(SIZE);
        if (currentSize == null) {
            parentDoc.setNumber(SIZE, totalAdditions);
        } else {
            parentDoc.setNumber(SIZE, currentSize + totalAdditions);
        }
    }
    
    protected Map<BucketId, Set<NodeKey>> preRemoveChildrenFromBuckets( WorkspaceCache wsCache,
                                                                        EditableDocument parentDoc,
                                                                        Set<NodeKey> removals )  {
        assert removals != null && !removals.isEmpty();
        Integer bucketIdLength = parentDoc.getInteger(BUCKET_ID_LENGTH);
        assert bucketIdLength != null;

        long totalRemovals = 0;

        // figure out the bucket where each removed node belongs
        Map<BucketId, Set<NodeKey>> removalsPerBucket = new HashMap<>(removals.size());
        for (NodeKey removedKey : removals) {
            Name removedName = wsCache.getNode(removedKey).getName(wsCache);
            BucketId bucketId = new BucketId(removedName, bucketIdLength);
            Set<NodeKey> bucketRemovals = removalsPerBucket.get(bucketId);
            if (bucketRemovals == null) {
                bucketRemovals = new HashSet<>();
                removalsPerBucket.put(bucketId, bucketRemovals);
            }
            bucketRemovals.add(removedKey);
            ++totalRemovals;
        }

        Long currentSize = parentDoc.getLong(SIZE);
        if (totalRemovals > 0 && currentSize != null) {
            parentDoc.setNumber(SIZE, currentSize - totalRemovals);
        }

        return removalsPerBucket;
    }

    protected void persistBucketRemovalChanges( NodeKey parentKey,
                                                Map<BucketId, Set<NodeKey>> removalsPerBucket ) {
        EditableDocument parentDoc = documentStore.edit(parentKey.toString(), false, false);
        // for each bucket, get the corresponding document (locking it) and make the children changes
        for (Map.Entry<BucketId, Set<NodeKey>> entry : removalsPerBucket.entrySet()) {
            BucketId bucketId = entry.getKey();
            Set<NodeKey> removalsFromBucket = entry.getValue();
            String bucketIdString = bucketId.toString();
            String bucketKey = bucketKey(parentKey.toString(), bucketIdString);
            EditableDocument bucketDoc = documentStore.edit(bucketKey, false, true);
            assert bucketDoc != null;
            for (NodeKey toRemove : removalsFromBucket) {
                // keys are stored directly in the bucket
                bucketDoc.remove(toRemove.toString());
            }
            if (bucketDoc.isEmpty()) {
                documentStore.remove(bucketKey);
                parentDoc.getArray(BUCKETS).remove((Object)bucketIdString);
            } 
        }
    }
    
    protected void removeAllBucketsFromUnorderedCollection( NodeKey parentDocKey ) {
        // should already have been loaded into the cache
        EditableDocument parentDoc = documentStore.edit(parentDocKey.toString(), false, false);
        assert parentDoc != null;
        EditableArray bucketsIds = parentDoc.getArray(BUCKETS);
        if (bucketsIds == null || bucketsIds.isEmpty()) {
            return;
        }
        for (Object bucketId : bucketsIds) {
            String bucketKey = bucketKey(parentDocKey.toString(), bucketId.toString());
            documentStore.remove(bucketKey);
        }
    }
    
    protected void addInternalProperties(EditableDocument doc, Map<String, Object> properties) {
        if (properties.isEmpty()) {
            return;
        }
        doc.putAll(properties);
    }

    protected void removeInteralProperties(EditableDocument doc, Set<String> properties) {
        if (properties.isEmpty()) {
            return;
        }
        for (String propertyName : properties) {
            doc.remove(propertyName);
        }
    }
}
