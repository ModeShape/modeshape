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
package org.modeshape.connector.disk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.util.Base64;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.graph.connector.base.MapWorkspace;
import org.modeshape.graph.connector.base.NodeCachingWorkspace;
import org.modeshape.graph.connector.base.cache.NodeCache;
import org.modeshape.graph.connector.base.cache.NodeCachePolicy;
import org.modeshape.graph.connector.base.cache.NodeCachePolicyChangedEvent;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.property.basic.FileSystemBinary;

/**
 * Workspace implementation for disk connector
 */
public class DiskWorkspace extends MapWorkspace<DiskNode> implements NodeCachingWorkspace<UUID, DiskNode> {

    /** A version number describing the on-disk format used to store this node */
    private static final byte CURRENT_VERSION = 1;

    private static final String DATA_EXTENSION = ".dat";
    private static final String BACK_REFERENCE_EXTENSION = ".ref";

    private final File workspaceRoot;
    private final DiskRepository repository;
    private final BinaryFactory binFactory;
    private final PropertyFactory propFactory;
    private NodeCache<UUID, DiskNode> cache;
    private NodeCachePolicy<UUID, DiskNode> policy;

    /**
     * Create a new workspace instance.
     * 
     * @param name the name of the workspace; may not be null
     * @param workspaceRoot a pointer to the root of the workspace on disk; may not be null
     * @param rootNode the root node for the workspace; may not be null
     * @param repository the repository to which this workspace belongs; may not be null
     */
    public DiskWorkspace( String name,
                          File workspaceRoot,
                          DiskNode rootNode,
                          DiskRepository repository ) {
        super(name, rootNode);

        assert repository != null;
        assert rootNode != null;
        assert workspaceRoot != null;

        this.workspaceRoot = workspaceRoot;
        this.repository = repository;
        this.propFactory = repository.getContext().getPropertyFactory();
        this.binFactory = repository.getContext().getValueFactories().getBinaryFactory();

        this.policy = repository.diskSource().getNodeCachePolicy();
        this.cache = policy.newCache();

        File rootNodeFile = fileFor(rootNode.getUuid());
        if (!rootNodeFile.exists()) {
            putNode(rootNode);
        }

        repository.diskSource().addNodeCachePolicyChangedListener(this);
    }

    /**
     * Create a new workspace instance.
     * 
     * @param name the name of the workspace
     * @param workspaceRoot a pointer to the root of the workspace on disk
     * @param originalToClone the workspace that is to be cloned
     */
    public DiskWorkspace( String name,
                          File workspaceRoot,
                          DiskWorkspace originalToClone ) {
        super(name, originalToClone);

        assert originalToClone != null;
        assert workspaceRoot != null;

        this.workspaceRoot = workspaceRoot;

        this.repository = originalToClone.repository;
        this.policy = repository.diskSource().getNodeCachePolicy();
        this.cache = policy.newCache();

        this.propFactory = repository.getContext().getPropertyFactory();
        this.binFactory = repository.getContext().getValueFactories().getBinaryFactory();

        repository.diskSource().addNodeCachePolicyChangedListener(this);
    }


    public void destroy() {
        this.workspaceRoot.delete();
        repository.diskSource().removeNodeCachePolicyChangedListener(this);
    }

    /**
     * This method shuts down the workspace and makes it no longer usable. This method should also only be called once.
     */
    public void shutdown() {
        cache.close();
    }

    /**
     * Notifies this workspace that the cache policy has changed and the cache should be reset.
     * 
     * @param event the cache policy changed event; may not be null
     */
    @Override
    public void cachePolicyChanged( NodeCachePolicyChangedEvent<UUID, DiskNode> event ) {
        this.policy = event.getNewPolicy();
        this.cache = policy.newCache();

    }

    @Override
    public NodeCache<UUID, DiskNode> getCache() {
        return cache;
    }

    @Override
    public DiskNode getNode( UUID uuid ) {
        DiskNode node = cache.get(uuid);
        if (node != null) return node;

        File nodeFile = fileFor(uuid);
        if (!nodeFile.exists()) return null;

        node = nodeFor(nodeFile);

        if (node != null) cache.put(uuid, node);
        return node;
    }

    @Override
    public DiskNode putNode( DiskNode node ) {
        writeNode(node);
        cache.put(node.getUuid(), node);
        return node;
    }

    @Override
    public void removeAll() {
        this.workspaceRoot.delete();
        this.workspaceRoot.mkdir();

        cache = policy.newCache();
        // This won't clean up any back-references from this workspace
    }

    @Override
    public DiskNode removeNode( UUID uuid ) {
        File nodeFile = fileFor(uuid);
        if (!nodeFile.exists()) return null;

        DiskNode node = nodeFor(nodeFile);

        nodeFile.delete();
        cache.remove(uuid);
        return node;
    }

    private File fileFor( UUID uuid ) {
        String uuidAsString = uuid.toString();

        File firstLevel = new File(workspaceRoot, uuidAsString.substring(0, 2));
        File secondLevel = new File(firstLevel, uuidAsString.substring(2, 4));
        File file = new File(secondLevel, uuidAsString);

        if (!file.exists()) {
            if (!secondLevel.exists()) {
                if (!firstLevel.exists()) firstLevel.mkdir();

                secondLevel.mkdir();
            }
        }

        return file;
    }

    private DiskNode nodeFor( File nodeFile ) {
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(nodeFile));

            byte version = ois.readByte();
            assert version == CURRENT_VERSION;

            UUID uuid = (UUID)ois.readObject();
            Segment name = (Segment)ois.readObject();
            UUID parent = (UUID)ois.readObject();
            @SuppressWarnings( "unchecked" )
            List<UUID> children = (List<UUID>)ois.readObject();
            @SuppressWarnings( "unchecked" )
            Map<Name, Property> rawProps = (Map<Name, Property>)ois.readObject();

            return new DiskNode(uuid, name, parent, rawProps, children);
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException(cnfe);

        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } finally {
            try {
                if (ois != null) ois.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Inspects the given value to determine if it is a BINARY value that exceeds the {@code largeValueThreshold}.
     * <p/>
     * If this value is a BINARY value that exceeds the threshold, this method creates or reuses a large value in the large value
     * area for this value and replaces the value with a reference to this large value. If this value somehow refers to the large
     * value area but is no longer big enough to belong in the large value area (perhaps because the threshold was changed), this
     * method will "downgrade" the value to an in-memory {@link Binary} implementation.
     * 
     * @param value the value to inspect; may be null
     * @param largeValueThreshold the threshold above which values should be stored in the large value area
     * @param largeValueKeysInUse an aggregation of all of the keys for all large values that this node is currently using; may
     *        not be null
     * @param nodeUuid the UUID of the current node; may not be null
     * @return if {@code value} is not a {@link Binary} or is an in-memory {@code Binary} implementation with size less than
     *         {@code largeValueThreshold}, then {@code value} is returned; otherwise the return value is determined in accordance
     *         with the method description above
     * @throws IOException if the state of the large value area cannot be updated due to a file system issue
     * @throws ClassNotFoundException if the state of the large value area cannot be updated due to a classloading issue
     */
    private Object adjustValueForLargeValueThreshold( Object value,
                                                      long largeValueThreshold,
                                                      Collection<String> largeValueKeysInUse,
                                                      UUID nodeUuid ) throws IOException, ClassNotFoundException {
        if (value != null && value instanceof Binary) {
            Binary binValue = (Binary)value;
            if (value instanceof FileSystemBinary) {
                String key = keyFor(binValue);

                if (binValue.getSize() <= largeValueThreshold) {
                    // Downgrade to a regular binary
                    Object newValue = binFactory.create(binValue.getStream(), binValue.getSize());

                    return newValue;
                }

                // It was already a FileSystemBinary and didn't need to get downgraded
                largeValueKeysInUse.add(key);

            } else { // Not yet a FileSystemBinary
                if (binValue.getSize() > largeValueThreshold) {
                    File largeValueFile = addOrUpdateLargeValueAreaFor(binValue, nodeUuid);

                    largeValueKeysInUse.add(keyFor(binValue));

                    return new FileSystemBinary(largeValueFile);
                }
            }
        }

        return value;
    }

    /**
     * Modifies the state of the large value area to include the payload of the given binary object (if it is not already in the
     * large value area) and a reference from the large value area back to the current node.
     * <p/>
     * If there is not already a data file in the large value area that contains the payload of this {@link Binary} instance, a
     * data file will be created and a corresponding reference file that contains a {@link ValueReference} to the containing node
     * will be created as well.
     * <p/>
     * If there is already a data file in the large value area that contains the payload of this {@link Binary} instance, then the
     * corresponding reference file will be updated to include a reference to this node.
     * 
     * @param binary the binary object for which the payload should be stored in the large value area; may not be null
     * @param nodeUuid the UUID of the node to which the property containing {@code binary} belongs; may not be null
     * @return the file in the large value area that contains the payload for this binary; never null
     * @throws IOException if the state of the large value area cannot be updated due to a file system issue
     * @throws ClassNotFoundException if the state of the large value area cannot be updated due to a classloading issue
     */
    private File addOrUpdateLargeValueAreaFor( Binary binary,
                                               UUID nodeUuid ) throws IOException, ClassNotFoundException {
        String key = keyFor(binary);

        File largeValueFile = new File(repository.largeValuesRoot(), key + DATA_EXTENSION);
        File largeValueFileRefs = new File(repository.largeValuesRoot(), key + BACK_REFERENCE_EXTENSION);

        if (largeValueFile.exists()) {
            // Add reference
            assert largeValueFileRefs.exists();

            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(largeValueFileRefs));
            ValueReferences valueRefs = (ValueReferences)ois.readObject();
            ois.close();

            assert valueRefs != null;
            ValueReference valueRef = new ValueReference(getName(), nodeUuid);

            if (!valueRefs.hasReference(valueRef)) {
                valueRefs = valueRefs.withReference(valueRef);

                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(largeValueFileRefs));
                oos.writeObject(valueRefs);
                oos.close();
            }
        } else {
            // Create file and reference
            FileOutputStream fos = new FileOutputStream(largeValueFile);
            IoUtil.write(binary.getStream(), fos);
            fos.close();

            ValueReference valueRef = new ValueReference(getName(), nodeUuid);
            ValueReferences valueRefs = new ValueReferences(valueRef);

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(largeValueFileRefs));
            oos.writeObject(valueRefs);
            oos.close();

        }

        return largeValueFile;
    }

    private String keyFor( Binary value ) {
        try {
            String key = Base64.encodeBytes(value.getHash(), Base64.URL_SAFE);
            assert key.indexOf('/') == -1 : "Bad hash " + key + " for value " + new String(value.getBytes());
            return key;
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        } // end catch
    }

    /**
     * Iterates over each property in the node, correcting the property if it has a {@link Binary} values that exceeds the
     * {@link DiskSource#setLargeValueSizeInBytes(long) large value threshold} for the {@link DiskSource} and updating the large
     * value area accordingly.
     * <p/>
     * This method will also remove any out-of-date references from the node to large values and clean up large values as needed.
     * 
     * @param node the node to inspect; may not be null
     * @return a modified property map for the node with the properties adjusted to properly use the large value area as noted in
     *         the description above; never null
     * @throws IOException if the state of the large value area cannot be updated due to a file system issue
     * @throws ClassNotFoundException if the state of the large value area cannot be updated due to a classloading issue
     */
    private Map<Name, Property> adjustedPropertiesFor( DiskNode node ) throws IOException, ClassNotFoundException {
        Set<String> largeValueKeysInUse = new HashSet<String>();
        Map<Name, Property> adjustedProps;

        long largeValueThreshold = this.repository.diskSource().getLargeValueSizeInBytes();
        if (largeValueThreshold > 0) {
            adjustedProps = new HashMap<Name, Property>(node.getProperties().size());

            // Iterate over the node's properties
            for (Map.Entry<Name, Property> entry : node.getProperties().entrySet()) {
                Property prop = entry.getValue();
                List<Object> newValues = new ArrayList<Object>(prop.size());

                // Start by assuming that the given property won't require a change
                boolean requiresChange = false;

                // Now iterate over each of the properties values
                for (Object value : prop) {
                    // This method will adjust the value to reference the large value area IFF needed, otherwise it returns the
                    // original value
                    Object newValue = adjustValueForLargeValueThreshold(value,
                                                                        largeValueThreshold,
                                                                        largeValueKeysInUse,
                                                                        node.getUuid());

                    // If the value changed, then an adjustment took place
                    if (newValue != value) {
                        // Note that we'll need to replace this property with an adjusted version
                        requiresChange = true;
                        newValues.add(newValue);
                    } else {
                        // Add the old value into a temp list of values in case a later value gets adjusted and we need to rebuild
                        // the values
                        newValues.add(value);
                    }
                }

                // If at least one value changed, make a new property that references the adjusted values
                if (requiresChange) {
                    adjustedProps.put(entry.getKey(), propFactory.create(entry.getKey(), newValues));
                } else {
                    // Otherwise, just reuse the old property
                    adjustedProps.put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            adjustedProps = node.getProperties();
        }

        // Check if there are any references from this node to large values that are no longer in use
        Set<String> removedKeys = new HashSet<String>(node.largeValueHashesInUse());
        removedKeys.removeAll(largeValueKeysInUse);

        for (String key : removedKeys) {
            File largeValueFileRefs = new File(repository.largeValuesRoot(), key + BACK_REFERENCE_EXTENSION);
            assert largeValueFileRefs.exists();

            ValueReferences valueRefs = null;
            try {
                // Load up the back-references for this large value
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(largeValueFileRefs));
                valueRefs = (ValueReferences)ois.readObject();
                ois.close();
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalStateException(cnfe);
            }

            assert valueRefs != null;
            ValueReference valueRef = new ValueReference(getName(), node.getUuid());
            valueRefs = valueRefs.withoutReference(valueRef);

            if (valueRefs.hasRemainingReferences()) {
                // If there are other valid references to this node, update the reference file
                ObjectOutputStream roos = new ObjectOutputStream(new FileOutputStream(largeValueFileRefs));
                roos.writeObject(valueRefs);
                roos.close();
            } else {
                // Otherwise, clean up the data file and reference file
                File largeValueFile = new File(repository.largeValuesRoot(), key + DATA_EXTENSION);

                largeValueFileRefs.delete();
                largeValueFile.delete();
            }
        }

        return adjustedProps;
    }

    private File writeNode( DiskNode node ) {
        ObjectOutputStream oos = null;

        try {
            Map<Name, Property> adjustedProps = adjustedPropertiesFor(node);

            File nodeFile = fileFor(node.getUuid());
            oos = new ObjectOutputStream(new FileOutputStream(nodeFile));

            oos.writeByte(CURRENT_VERSION);
            oos.writeObject(node.getUuid());
            oos.writeObject(node.getName());
            oos.writeObject(node.getParent());
            oos.writeObject(node.getChildren());
            oos.writeObject(adjustedProps);

            return nodeFile;
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException(cnfe);
        } finally {
            try {
                if (oos != null) oos.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * A record of a reference from a node in a workspace to a value in the large value area.
     */
    @Immutable
    private static class ValueReference implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String workspaceName;
        private final UUID owningNodeUuid;

        ValueReference( String workspaceName,
                        UUID owningNodeUuid ) {
            super();
            this.workspaceName = workspaceName;
            this.owningNodeUuid = owningNodeUuid;
        }

        @Override
        public int hashCode() {
            return owningNodeUuid.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (this == obj) return true;
            if (obj instanceof ValueReference) {
                ValueReference other = (ValueReference)obj;
                return ObjectUtil.isEqualWithNulls(this.owningNodeUuid, other.owningNodeUuid)
                       && ObjectUtil.isEqualWithNulls(this.workspaceName, other.workspaceName);
            }
            return false;
        }
    }

    /**
     * A set of {@link ValueReference} instances.
     */
    @NotThreadSafe
    private static class ValueReferences implements Serializable {
        private static final long serialVersionUID = 1L;

        private final Set<ValueReference> references;

        ValueReferences( ValueReference reference ) {
            super();
            this.references = new HashSet<ValueReference>();

            this.references.add(reference);
        }

        ValueReferences( Set<ValueReference> references ) {
            super();
            this.references = references;
        }

        ValueReferences withoutReference( ValueReference reference ) {
            if (!references.contains(reference)) return this;

            Set<ValueReference> newRefs = new HashSet<ValueReference>(this.references);
            newRefs.remove(reference);

            return new ValueReferences(newRefs);
        }

        ValueReferences withReference( ValueReference reference ) {
            if (references.contains(reference)) return this;

            Set<ValueReference> newRefs = new HashSet<ValueReference>(this.references);
            newRefs.add(reference);

            return new ValueReferences(newRefs);
        }

        boolean hasRemainingReferences() {
            return !this.references.isEmpty();
        }

        boolean hasReference( ValueReference ref ) {
            return this.references.contains(ref);
        }
    }

}
