/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.store.jpa.model.simple;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.connector.store.jpa.JpaConnectorI18n;
import org.jboss.dna.connector.store.jpa.model.common.WorkspaceEntity;
import org.jboss.dna.connector.store.jpa.util.Namespaces;
import org.jboss.dna.connector.store.jpa.util.Serializer;
import org.jboss.dna.connector.store.jpa.util.Workspaces;
import org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.LockFailedException;
import org.jboss.dna.graph.connector.map.AbstractMapWorkspace;
import org.jboss.dna.graph.connector.map.MapNode;
import org.jboss.dna.graph.connector.map.MapRepository;
import org.jboss.dna.graph.connector.map.MapWorkspace;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.request.CompositeRequest;
import org.jboss.dna.graph.request.LockBranchRequest.LockScope;

/**
 * Implementation of {@link MapRepository} for the {@link SimpleModel Simple JPA connector model}. This class exposes a map of
 * workspace names to {@link Workspace workspaces} and each workspace provides a logical mapping of node UUIDs to {@link JpaNode
 * nodes}. The {@code JpaNode} class functions as an adapter between the {@link NodeEntity persistent entity for nodes} and the
 * {@link MapNode map repository interface for nodes}.
 * <p>
 * This class differs slightly from the other {@link MapRepository} implementations in that it exists only within the lifetime of
 * a single {@link EntityManager} (which itself is opened and closed within the lifetime of a single {@link SimpleJpaConnection}.
 * The other map repository implementations all outlive any particular connection and generally survive for the lifetime of the
 * DNA server.
 * </p>
 */
public class SimpleJpaRepository extends MapRepository {

    protected final EntityManager entityManager;
    protected final Workspaces workspaceEntities;
    protected final Namespaces namespaceEntities;
    protected final ExecutionContext context;
    protected final PathFactory pathFactory;
    protected final NameFactory nameFactory;
    private final List<String> predefinedWorkspaceNames;
    protected final boolean compressData;
    protected final boolean creatingWorkspacesAllowed;
    protected final long minimumSizeOfLargeValuesInBytes;

    // private final boolean referentialIntegrityEnforced;

    public SimpleJpaRepository( String sourceName,
                                UUID rootNodeUuid,
                                String defaultWorkspaceName,
                                String[] predefinedWorkspaceNames,
                                EntityManager entityManager,
                                ExecutionContext context,
                                boolean compressData,
                                boolean creatingWorkspacesAllowed,
                                boolean referentialIntegrityEnforced,
                                long minimumSizeOfLargeValuesInBytes ) {
        super(sourceName, rootNodeUuid, defaultWorkspaceName);

        this.context = context;
        ValueFactories valueFactories = context.getValueFactories();
        this.nameFactory = valueFactories.getNameFactory();
        this.pathFactory = valueFactories.getPathFactory();
        this.predefinedWorkspaceNames = Arrays.asList(predefinedWorkspaceNames);
        this.compressData = compressData;
        this.creatingWorkspacesAllowed = creatingWorkspacesAllowed;
        // this.referentialIntegrityEnforced = referentialIntegrityEnforced;
        this.minimumSizeOfLargeValuesInBytes = minimumSizeOfLargeValuesInBytes;

        this.entityManager = entityManager;
        workspaceEntities = new Workspaces(entityManager);
        namespaceEntities = new Namespaces(entityManager);
        super.initialize();
    }

    public SimpleJpaRepository( String sourceName,
                                UUID rootNodeUuid,
                                EntityManager entityManager,
                                ExecutionContext context,
                                boolean compressData,
                                boolean creatingWorkspacesAllowed,
                                boolean referentialIntegrityEnforced,
                                long minimumSizeOfLargeValuesInBytes ) {
        super(sourceName, rootNodeUuid);

        this.context = context;
        ValueFactories valueFactories = context.getValueFactories();
        this.nameFactory = valueFactories.getNameFactory();
        this.pathFactory = valueFactories.getPathFactory();
        this.predefinedWorkspaceNames = Collections.emptyList();
        this.compressData = compressData;
        this.creatingWorkspacesAllowed = creatingWorkspacesAllowed;
        // this.referentialIntegrityEnforced = referentialIntegrityEnforced;
        this.minimumSizeOfLargeValuesInBytes = minimumSizeOfLargeValuesInBytes;

        this.entityManager = entityManager;
        workspaceEntities = new Workspaces(entityManager);
        namespaceEntities = new Namespaces(entityManager);
        super.initialize();
    }

    final EntityManager entityManager() {
        return entityManager;
    }

    /**
     * Determine whether creating workspaces is allowed.
     * 
     * @return true if creating workspace is allowed, or false otherwise
     * @see org.jboss.dna.connector.store.jpa.JpaSource#isCreatingWorkspacesAllowed()
     */
    final boolean creatingWorkspacesAllowed() {
        return this.creatingWorkspacesAllowed;
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.dna.graph.connector.map.MapRepository#createWorkspace(org.jboss.dna.graph.ExecutionContext, java.lang.String)
     */
    @Override
    protected MapWorkspace createWorkspace( ExecutionContext context,
                                            String name ) {

        WorkspaceEntity entity = workspaceEntities.get(name, false);

        if (entity != null) {
            return new Workspace(this, name, entity.getId().intValue());
        }

        entity = workspaceEntities.create(name);

        // Flush to ensure that the entity ID is set
        entityManager.flush();

        Workspace workspace = new Workspace(this, name, entity.getId().intValue());
        workspace.createRootNode();

        return workspace;
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.dna.graph.connector.map.MapRepository#getWorkspace(java.lang.String)
     */
    @Override
    public MapWorkspace getWorkspace( String name ) {
        MapWorkspace workspace = super.getWorkspace(name);
        if (workspace != null) return workspace;

        // There's no such workspace in the local cache, check if one exists in the DB
        if (name == null) name = getDefaultWorkspaceName();
        WorkspaceEntity entity = workspaceEntities.get(name, false);
        if (entity == null) {
            if (this.predefinedWorkspaceNames.contains(name)) {
                return createWorkspace(context, name);
            }

            return null;
        }

        return new Workspace(this, name, entity.getId());
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.dna.graph.connector.map.MapRepository#getWorkspaceNames()
     */
    @Override
    public Set<String> getWorkspaceNames() {
        Set<String> workspaceNames = new HashSet<String>(super.getWorkspaceNames());
        workspaceNames.addAll(predefinedWorkspaceNames);

        return workspaceNames;
    }

    /**
     * This class provides a logical mapping of UUIDs to {@link JpaNode nodes} within a named workspace.
     * <p>
     * Like its enclosing class, this class only survives for the lifetime of a single request (which may be a
     * {@link CompositeRequest}).
     * </p>
     */
    @SuppressWarnings( "synthetic-access" )
    protected class Workspace extends AbstractMapWorkspace {
        private final long workspaceId;
        private final Map<Path, MapNode> nodesByPath = new HashMap<Path, MapNode>();

        public Workspace( MapRepository repository,
                          String name,
                          long workspaceId ) {
            super(repository, name);

            this.workspaceId = workspaceId;

            // This gets called from the repository for this connector since the repository
            // already knows whether this workspace existed in the database before this call.
            // initialize();
        }

        void createRootNode() {
            initialize();
        }

        /*
         * (non-Javadoc)
         * @see org.jboss.dna.graph.connector.map.AbstractMapWorkspace#correctSameNameSiblingIndexes(org.jboss.dna.graph.ExecutionContext, org.jboss.dna.graph.connector.map.MapNode, org.jboss.dna.graph.property.Name)
         */
        @Override
        protected void correctSameNameSiblingIndexes( ExecutionContext context,
                                                      MapNode parentNode,
                                                      Name name ) {
            int snsIndex = 1;
            int parentIndex = 0;
            List<MapNode> children = parentNode.getChildren();

            for (MapNode child : children) {
                NodeEntity childNode = ((JpaNode)child).entity;
                if (parentIndex != childNode.getIndexInParent()) {
                    childNode.setIndexInParent(parentIndex);
                }

                if (name.equals(child.getName().getName())) {
                    if (snsIndex != childNode.getSameNameSiblingIndex()) {
                        childNode.setSameNameSiblingIndex(snsIndex);
                    }
                    snsIndex++;

                }
                parentIndex++;
            }

        }

        /**
         * Adds the given node to the persistent store, replacing any node already in the persistent store with the same UUID.
         * <p>
         * Invoking this method causes a database INSERT statement to execute immediately.
         * </p>
         * 
         * @param node the node to add to the persistent store; may not be null
         */
        @Override
        protected void addNodeToMap( MapNode node ) {
            assert node != null;

            NodeEntity nodeEntity = ((JpaNode)node).entity;
            nodeEntity.setWorkspaceId(this.workspaceId);

            entityManager.persist(nodeEntity);
        }

        @Override
        protected MapNode removeNodeFromMap( UUID nodeUuid ) {
            throw new IllegalStateException("This code should be unreachable");
        }

        /**
         * Removes the given node and its children from the persistent store using the
         * {@link SubgraphQuery#deleteSubgraph(boolean) subgraph bulk delete method}.
         * 
         * @param node the root of the branch to be removed
         */
        @Override
        protected void removeUuidReference( MapNode node ) {
            SubgraphQuery branch = SubgraphQuery.create(context, entityManager, workspaceId, node.getUuid(), null, 0);
            branch.deleteSubgraph(true);
            branch.close();
        }

        /*
         * (non-Javadoc)
         * @see org.jboss.dna.graph.connector.map.AbstractMapWorkspace#createMapNode(java.util.UUID)
         */
        @Override
        protected MapNode createMapNode( UUID uuid ) {
            return new JpaNode(uuid);
        }

        /**
         * Removes all of the nodes in this workspace from the persistent store with a single query.
         */
        @Override
        protected void removeAllNodesFromMap() {
            Query query = entityManager.createQuery("NodeEntity.deleteAllInWorkspace");
            query.setParameter("workspaceId", workspaceId);
            query.executeUpdate();
        }

        /*
         * (non-Javadoc)
         * @see org.jboss.dna.graph.connector.map.AbstractMapWorkspace#getNode(java.util.UUID)
         */
        @Override
        public JpaNode getNode( UUID nodeUuid ) {
            assert nodeUuid != null;

            Query query = entityManager.createNamedQuery("NodeEntity.findByNodeUuid");
            query.setParameter("workspaceId", workspaceId);
            query.setParameter("nodeUuidString", nodeUuid.toString());
            try {
                // Find the parent of the UUID ...
                NodeEntity result = (NodeEntity)query.getSingleResult();
                return new JpaNode(result);
            } catch (NoResultException e) {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see org.jboss.dna.graph.connector.map.AbstractMapWorkspace#getNode(org.jboss.dna.graph.property.Path)
         */
        @Override
        public MapNode getNode( Path path ) {
            MapNode node = nodesByPath.get(path);
            if (node != null) return node;

            node = super.getNode(path);
            nodesByPath.put(path, node);
            return node;
        }

        /**
         * Retrieves the branch of nodes rooted at the given location using the {@link SubgraphQuery#getNodes(boolean, boolean)
         * subgraph bulk accessor method}.
         * 
         * @param rootLocation the root of the branch of nodes to retrieve
         * @param maximumDepth the maximum depth to retrieve; a negative number indicates that the entire branch should be
         *        retrieved
         * @return the list of nodes in the branch rooted at {@code rootLocation}
         */
        public List<MapNode> getBranch( Location rootLocation,
                                        int maximumDepth ) {
            assert rootLocation.getUuid() != null || rootLocation.getPath() != null;
            UUID subgraphRootUuid = rootLocation.getUuid();

            if (subgraphRootUuid == null) {
                MapNode rootNode = getNode(rootLocation.getPath());
                subgraphRootUuid = rootNode.getUuid();
                assert subgraphRootUuid != null;
            }

            Path subgraphRootPath = null; // Don't need the path for this
            SubgraphQuery subgraph = SubgraphQuery.create(context,
                                                          entityManager,
                                                          workspaceId,
                                                          subgraphRootUuid,
                                                          subgraphRootPath,
                                                          maximumDepth);

            List<NodeEntity> entities = subgraph.getNodes(true, true);
            List<MapNode> nodes = new ArrayList<MapNode>(entities.size());

            for (NodeEntity entity : entities) {
                nodes.add(new JpaNode(entity));
            }

            subgraph.close();

            return nodes;
        }

        /**
         * This connector does not support connector-level, persistent locking of nodes.
         * 
         * @param node
         * @param lockScope
         * @param lockTimeoutInMillis
         * @throws LockFailedException
         */
        public void lockNode( MapNode node,
                              LockScope lockScope,
                              long lockTimeoutInMillis ) throws LockFailedException {
            // Locking is not supported by this connector
        }

        /**
         * This connector does not support connector-level, persistent locking of nodes.
         * 
         * @param node the node to be unlocked
         */
        public void unlockNode( MapNode node ) {
            // Locking is not supported by this connector
        }

    }

    /**
     * Adapter between the {@link NodeEntity persistent entity for nodes} and the {@link MapNode map repository interface for
     * nodes}.
     */
    @SuppressWarnings( "synthetic-access" )
    @NotThreadSafe
    protected class JpaNode implements MapNode {
        private final NodeEntity entity;
        private Map<Name, Property> properties = null;

        protected JpaNode( NodeEntity entity ) {
            this.entity = entity;
        }

        public JpaNode( UUID uuid ) {
            this.entity = new NodeEntity();
            entity.setNodeUuidString(uuid.toString());
        }

        private final JpaNode jpaNodeFor( MapNode node ) {
            if (!(node instanceof JpaNode)) {
                throw new IllegalStateException();
            }
            return (JpaNode)node;
        }

        public void addChild( int index,
                              MapNode child ) {
            entity.addChild(index, jpaNodeFor(child).entity);
        }

        public void addChild( MapNode child ) {
            entity.addChild(jpaNodeFor(child).entity);
        }

        public List<MapNode> getChildren() {
            List<MapNode> children = new ArrayList<MapNode>(entity.getChildren().size());

            for (NodeEntity child : entity.getChildren()) {
                children.add(new JpaNode(child));
            }

            return Collections.unmodifiableList(children);
        }

        public Segment getName() {
            return pathFactory.createSegment(nameFactory.create(entity.getChildNamespace().getUri(), entity.getChildName()),
                                             entity.getSameNameSiblingIndex());
        }

        public MapNode getParent() {
            if (entity.getParent() == null) return null;
            return new JpaNode(entity.getParent());
        }

        private void ensurePropertiesLoaded() {
            if (properties != null) return;

            Collection<Property> propsCollection = new LinkedList<Property>();

            if (entity.getData() != null) {
                Serializer serializer = new Serializer(context, true);
                ObjectInputStream ois = null;

                try {
                    LargeValueSerializer largeValues = new LargeValueSerializer(entity);
                    ois = new ObjectInputStream(new ByteArrayInputStream(entity.getData()));
                    serializer.deserializeAllProperties(ois, propsCollection, largeValues);

                } catch (IOException ioe) {
                    throw new IllegalStateException(ioe);
                } catch (ClassNotFoundException cnfe) {
                    throw new IllegalStateException(cnfe);
                } finally {
                    try {
                        if (ois != null) ois.close();
                    } catch (Exception ex) {
                    }
                }
            }

            PropertyFactory propertyFactory = context.getPropertyFactory();
            Map<Name, Property> properties = new HashMap<Name, Property>();
            properties.put(DnaLexicon.UUID, propertyFactory.create(DnaLexicon.UUID, getUuid()));
            for (Property prop : propsCollection) {
                properties.put(prop.getName(), prop);
            }

            this.properties = properties;
        }

        private void serializeProperties() {
            Serializer serializer = new Serializer(context, true);
            ObjectOutputStream oos = null;

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(baos);

                LargeValueSerializer largeValues = new LargeValueSerializer(entity);
                // dna:uuid prop is in collection but won't be serialized
                int numberOfPropertiesToSerialize = properties.size() - 1;
                serializer.serializeProperties(oos,
                                               numberOfPropertiesToSerialize,
                                               properties.values(),
                                               largeValues,
                                               Serializer.NO_REFERENCES_VALUES);
                oos.flush();
                entity.setData(baos.toByteArray());
                entity.setPropertyCount(properties.size());
            } catch (IOException ioe) {
                throw new IllegalStateException(ioe);
            } finally {
                try {
                    if (oos != null) oos.close();
                } catch (Exception ignore) {
                }
            }
        }

        public MapNode removeProperty( Name propertyName ) {
            ensurePropertiesLoaded();

            if (properties.containsKey(propertyName)) {
                properties.remove(propertyName);
                serializeProperties();
            }
            return this;
        }

        public Map<Name, Property> getProperties() {
            ensurePropertiesLoaded();
            return properties;
        }

        public Property getProperty( ExecutionContext context,
                                     String name ) {
            return getProperty(context.getValueFactories().getNameFactory().create(name));
        }

        public Property getProperty( Name name ) {
            ensurePropertiesLoaded();
            return properties.get(name);
        }

        public Set<Name> getUniqueChildNames() {
            List<NodeEntity> children = entity.getChildren();
            Set<Name> uniqueNames = new HashSet<Name>(children.size());

            for (NodeEntity child : children) {
                uniqueNames.add(nameFactory.create(child.getChildNamespace().getUri(), child.getChildName()));
            }

            return uniqueNames;
        }

        public UUID getUuid() {
            if (entity.getNodeUuidString() == null) return null;
            return UUID.fromString(entity.getNodeUuidString());
        }

        public boolean removeChild( MapNode child ) {

            /*
             * The NodeEntity.equals method compares on the Hibernate identifier to avoid
             * confusing Hibernate.  However, different nodes can be loaded in the same 
             * session for the same UUID in the same workspace, forcing us to roll our own
             * implementation of indexOf that tests for the equality of the NodeEntity UUIDs, 
             * rather than their Hibernate identifiers.
             */
            List<NodeEntity> children = entity.getChildren();

            int index = -1;
            String childUuidString = jpaNodeFor(child).entity.getNodeUuidString();
            for (int i = 0; i < children.size(); i++) {
                if (childUuidString.equals(children.get(i).getNodeUuidString())) {
                    index = i;
                    break;
                }
            }

            // int index = entity.getChildren().indexOf(jpaNodeFor(child).entity);
            // assert entity.getChildren().contains(jpaNodeFor(child).entity);
            if (index < 0) return false;

            entity.removeChild(index);

            assert !entity.getChildren().contains(child);
            assert child.getParent() == null;

            return true;
        }

        public void clearChildren() {
            entity.getChildren().clear();
        }

        public void setName( Segment name ) {
            entity.setChildNamespace(namespaceEntities.get(name.getName().getNamespaceUri(), true));
            // entity.setChildNamespace(NamespaceEntity.findByUri(entityManager, name.getName().getNamespaceUri(), true));
            entity.setChildName(name.getName().getLocalName());
            entity.setSameNameSiblingIndex(name.getIndex());
        }

        public void setParent( MapNode parent ) {
            if (parent == null) {
                entity.setParent(null);
            } else {
                entity.setParent(jpaNodeFor(parent).entity);
            }
        }

        public MapNode setProperty( ExecutionContext context,
                                    String name,
                                    Object... values ) {
            PropertyFactory propertyFactory = context.getPropertyFactory();

            return this.setProperty(propertyFactory.create(nameFactory.create(name), values));
        }

        public MapNode setProperty( Property property ) {
            ensurePropertiesLoaded();

            properties.put(property.getName(), property);
            serializeProperties();

            return this;
        }

        public MapNode setProperties( Iterable<Property> properties ) {
            ensurePropertiesLoaded();

            for (Property property : properties) {
                this.properties.put(property.getName(), property);
            }

            serializeProperties();

            return this;
        }

        @Override
        public String toString() {
            if (entity.getNodeUuidString().equals(rootNodeUuid.toString())) return "<root>";
            return getName().getString() + " (" + entity.getNodeUuidString() + ")";
        }

        @Override
        public boolean equals( Object obj ) {
            if (!(obj instanceof JpaNode)) return false;

            JpaNode other = (JpaNode)obj;
            return entity.getNodeUuidString().equals(other.entity.getNodeUuidString());
        }

        @Override
        public int hashCode() {
            return entity.getNodeUuidString().hashCode();
        }

    }

    protected class LargeValueSerializer implements LargeValues {
        private final NodeEntity node;
        private final Set<String> written;

        public LargeValueSerializer( NodeEntity entity ) {
            this.node = entity;
            this.written = null;
        }

        public LargeValueSerializer( NodeEntity entity,
                                     Set<String> written ) {
            this.node = entity;
            this.written = written;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#getMinimumSize()
         */
        public long getMinimumSize() {
            return minimumSizeOfLargeValuesInBytes;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#read(org.jboss.dna.graph.property.ValueFactories,
         *      byte[], long)
         */
        public Object read( ValueFactories valueFactories,
                            byte[] hash,
                            long length ) throws IOException {
            String hashStr = StringUtil.getHexString(hash);
            // Find the large value ...
            LargeValueEntity entity = entityManager.find(LargeValueEntity.class, hashStr);
            if (entity != null) {
                // Find the large value from the existing property entity ...
                byte[] data = entity.getData();
                if (entity.isCompressed()) {
                    InputStream stream = new GZIPInputStream(new ByteArrayInputStream(data));
                    try {
                        data = IoUtil.readBytes(stream);
                    } finally {
                        stream.close();
                    }
                }
                return valueFactories.getValueFactory(entity.getType()).create(data);
            }
            throw new IOException(JpaConnectorI18n.unableToReadLargeValue.text(getSourceName(), hashStr));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#write(byte[], long,
         *      org.jboss.dna.graph.property.PropertyType, java.lang.Object)
         */
        public void write( byte[] hash,
                           long length,
                           PropertyType type,
                           Object value ) throws IOException {
            if (value == null) return;
            String hashStr = StringUtil.getHexString(hash);
            if (written != null) written.add(hashStr);

            // Look for an existing value in the collection ...
            for (LargeValueEntity existing : node.getLargeValues()) {
                if (existing.getHash().equals(hashStr)) {
                    // Already associated with this properties entity
                    return;
                }
            }
            LargeValueEntity entity = entityManager.find(LargeValueEntity.class, hashStr);
            if (entity == null) {
                // We have to create the large value entity ...
                entity = new LargeValueEntity();
                entity.setCompressed(compressData);
                entity.setHash(hashStr);
                entity.setLength(length);
                entity.setType(type);
                ValueFactories factories = context.getValueFactories();
                byte[] bytes = null;
                switch (type) {
                    case BINARY:
                        Binary binary = factories.getBinaryFactory().create(value);
                        InputStream stream = null;
                        try {
                            binary.acquire();
                            stream = binary.getStream();
                            if (compressData) stream = new GZIPInputStream(stream);
                            bytes = IoUtil.readBytes(stream);
                        } finally {
                            try {
                                if (stream != null) stream.close();
                            } finally {
                                binary.release();
                            }
                        }
                        break;
                    case URI:
                        // This will be treated as a string ...
                    default:
                        String str = factories.getStringFactory().create(value);
                        if (compressData) {
                            ByteArrayOutputStream bs = new ByteArrayOutputStream();
                            OutputStream strStream = new GZIPOutputStream(bs);
                            try {
                                IoUtil.write(str, strStream);
                            } finally {
                                strStream.close();
                            }
                            bytes = bs.toByteArray();
                        } else {
                            bytes = str.getBytes();
                        }
                        break;
                }
                entity.setData(bytes);
                entityManager.persist(entity);
            }
            // Now associate the large value with the properties entity ...
            assert entity.getHash() != null;
            node.getLargeValues().add(entity);
        }

    }

}
