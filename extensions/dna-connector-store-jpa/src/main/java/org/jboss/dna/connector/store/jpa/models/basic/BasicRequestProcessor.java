/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.store.jpa.models.basic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.connector.store.jpa.JpaConnectorI18n;
import org.jboss.dna.connector.store.jpa.models.common.NamespaceEntity;
import org.jboss.dna.connector.store.jpa.util.Serializer;
import org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.properties.Binary;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.NameFactory;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyType;
import org.jboss.dna.graph.properties.ValueFactories;
import org.jboss.dna.graph.properties.ValueFactory;
import org.jboss.dna.graph.requests.CopyBranchRequest;
import org.jboss.dna.graph.requests.CreateNodeRequest;
import org.jboss.dna.graph.requests.DeleteBranchRequest;
import org.jboss.dna.graph.requests.MoveBranchRequest;
import org.jboss.dna.graph.requests.ReadAllChildrenRequest;
import org.jboss.dna.graph.requests.ReadAllPropertiesRequest;
import org.jboss.dna.graph.requests.UpdatePropertiesRequest;
import org.jboss.dna.graph.requests.processor.RequestProcessor;

/**
 * @author Randall Hauch
 */
public class BasicRequestProcessor extends RequestProcessor implements LargeValues {

    private final EntityManager entities;
    private final ValueFactory<String> stringFactory;
    private final PathFactory pathFactory;
    private final NameFactory nameFactory;
    private final Namespaces namespaces;
    private final UUID rootNodeUuid;
    private final Serializer serializer;
    private final long largeValueMinimumSizeInBytes;

    /**
     * @param sourceName
     * @param context
     * @param entityManager
     * @param rootNodeUuid
     * @param largeValueMinimumSizeInBytes
     */
    public BasicRequestProcessor( String sourceName,
                                  ExecutionContext context,
                                  EntityManager entityManager,
                                  UUID rootNodeUuid,
                                  long largeValueMinimumSizeInBytes ) {
        super(sourceName, context);
        assert entityManager != null;
        assert rootNodeUuid != null;
        this.entities = entityManager;
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.nameFactory = context.getValueFactories().getNameFactory();
        this.namespaces = new Namespaces(entityManager);
        this.rootNodeUuid = rootNodeUuid;
        this.serializer = new Serializer(context, this);
        this.largeValueMinimumSizeInBytes = largeValueMinimumSizeInBytes;
        // Start the transaction ...
        this.entities.getTransaction().begin();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        // Location actualLocation = null;
        // try {
        // // Create nodes have to be defined via a path ...
        // Location desiredLocation = request.at();
        // Path desiredPath = desiredLocation.getPath();
        // assert desiredPath != null;
        //
        // // // Get the parent
        // // String parentUuidString = getUuidOf(parentLocation);
        // // Location parentActualLocation = getActualLocation(parentLocation, parentUuidString);
        // // Path parentPath = parentActualLocation.getPath();
        // //
        // // // Now see where the child is to be created ...
        // // request.
        //
        // } catch (Throwable e) { // Includes PathNotFoundException
        // request.setError(e);
        // return;
        // }
        // if (actualLocation != null) request.setActualLocationOfNode(actualLocation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllChildrenRequest)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public void process( ReadAllChildrenRequest request ) {
        Location actualLocation = null;
        try {
            Location location = request.of();
            String parentUuidString = getUuidOf(location);
            actualLocation = getActualLocation(location, parentUuidString);
            Path path = actualLocation.getPath();

            // Find the children of the supplied node ...
            Query query = entities.createNamedQuery("ChildEntity.findAllUnderParent");
            query.setParameter("uuid", parentUuidString);
            List<ChildEntity> children = query.getResultList();
            for (ChildEntity child : children) {
                String namespaceUri = child.getChildNamespace().getUri();
                String localName = child.getChildName();
                Name childName = nameFactory.create(namespaceUri, localName);
                Integer sns = child.getSameNameSiblingIndex();
                if (sns == null) sns = new Integer(1);
                Path childPath = pathFactory.create(path, childName, sns);
                String childUuidString = child.getId().getChildUuidString();
                Location childLocation = new Location(childPath, UUID.fromString(childUuidString));
                request.addChild(childLocation);
            }
        } catch (NoResultException e) {
            // there are no properties (probably not expected, but still okay) ...
        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        if (actualLocation != null) request.setActualLocationOfNode(actualLocation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllPropertiesRequest)
     */
    @Override
    public void process( ReadAllPropertiesRequest request ) {
        Location actualLocation = null;
        try {
            Location location = request.at();
            String uuidString = getUuidOf(location);
            actualLocation = getActualLocation(location, uuidString);

            // Find the properties entity for this node ...
            Query query = entities.createNamedQuery("PropertiesEntity.findByUuid");
            query.setParameter("uuid", uuidString);
            PropertiesEntity entity = (PropertiesEntity)query.getSingleResult();

            // Deserialize the properties ...
            int propertyCount = entity.getPropertyCount();
            Collection<Property> properties = new ArrayList<Property>(propertyCount);
            byte[] data = entity.getData();
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            ObjectInputStream ois = new ObjectInputStream(bais);
            serializer.deserializeProperties(ois, properties);
            for (Property property : properties) {
                request.addProperty(property);
            }
        } catch (NoResultException e) {
            // there are no properties (probably not expected, but still okay) ...
        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        if (actualLocation != null) request.setActualLocationOfNode(actualLocation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#close()
     */
    @Override
    public void close() {
        EntityTransaction txn = entities.getTransaction();
        if (txn != null) txn.commit();
        super.close();
    }

    protected Location getActualLocation( Location original,
                                          String uuidString ) {
        // If the original has a path and a UUID, it is complete already ...
        Path path = original.getPath();
        if (path != null) {
            if (original.getIdProperty(DnaLexicon.UUID) != null) return original;
            return original.with(UUID.fromString(uuidString));
        }
        // There is no path, so find it by UUID ...
        path = getPathForUuid(uuidString);
        return new Location(path, UUID.fromString(uuidString));
    }

    protected String getUuidOf( Location location ) throws PathNotFoundException {
        String uuidString = null;
        if (location.hasIdProperties()) {
            // Look for the UUID ...
            Property uuidProperty = location.getIdProperty(DnaLexicon.UUID);
            if (uuidProperty != null && !uuidProperty.isEmpty()) {
                uuidString = stringFactory.create(uuidProperty.iterator().next());
            }
        }
        if (uuidString == null) {
            // Look up the node by using the path to walk down the children, starting at the root ...
            Path path = location.getPath();
            if (path == null) {
                // Location does not have path or DnaLexicon.UUID id property
            }
            assert path != null;
            if (path.isRoot()) {
                uuidString = rootNodeUuid.toString();
            } else {
                String parentUuid = this.rootNodeUuid.toString();
                ChildEntity child = null;
                for (Path.Segment segment : path) {
                    child = findByPathSegment(parentUuid, segment);
                    if (child == null) {
                        // Determine the lowest path that exists ...
                        Path lowest = path;
                        while (lowest.getLastSegment() != segment) {
                            lowest = lowest.getParent();
                        }
                        lowest = lowest.getParent();
                        throw new PathNotFoundException(location, lowest);
                    }
                }
                assert child != null;
                uuidString = child.getId().getChildUuidString();
            }
        }
        assert uuidString != null;
        return uuidString;
    }

    /**
     * Find the node with the supplied path segment that is a child of the supplied parent.
     * 
     * @param parentUuid the UUID of the parent node, in string form
     * @param pathSegment the path segment of the child
     * @return the existing namespace, or null if one does not exist
     * @throws IllegalArgumentException if the manager or URI are null
     */
    protected ChildEntity findByPathSegment( String parentUuid,
                                             Path.Segment pathSegment ) {
        assert namespaces != null;
        assert parentUuid != null;
        assert pathSegment != null;
        Name name = pathSegment.getName();
        String localName = name.getLocalName();
        String nsUri = name.getNamespaceUri();
        Integer nsId = namespaces.getId(nsUri, false);
        if (nsId == null) {
            // The namespace can't be found, then certainly the node won't be found ...
            return null;
        }
        Query query = entities.createNamedQuery("ChildEntity.findByPathSegment");
        query.setParameter("parentUuidString", parentUuid);
        query.setParameter("ns", nsId);
        query.setParameter("childName", localName);
        if (pathSegment.hasIndex()) {
            query.setParameter("sns", localName);
        } else {
            query.setParameter("sns", null);
        }
        try {
            return (ChildEntity)query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Build up the path for the node with the supplied UUID.
     * 
     * @param uuidString the UUID of the node
     * @return the path to the node
     */
    protected Path getPathForUuid( String uuidString ) {
        ChildEntity entity = null;
        String childUuid = uuidString;
        LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
        do {
            // Find the parent of the UUID ...
            Query query = entities.createNamedQuery("ChildEntity.findParentByUuid");
            query.setParameter("childUuidString", childUuid);
            try {
                entity = (ChildEntity)query.getSingleResult();
                String localName = entity.getChildName();
                String uri = entity.getChildNamespace().getUri();
                Integer sns = entity.getSameNameSiblingIndex();
                Name name = nameFactory.create(uri, localName);
                if (sns != null) {
                    segments.addFirst(pathFactory.createSegment(name, sns));
                } else {
                    segments.addFirst(pathFactory.createSegment(name));
                }
            } catch (NoResultException e) {
                entity = null;
            }
        } while (entity != null);
        return pathFactory.createAbsolutePath(segments);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#getMinimumSize()
     */
    public long getMinimumSize() {
        return largeValueMinimumSizeInBytes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#read(org.jboss.dna.graph.properties.ValueFactories,
     *      byte[], long)
     */
    public Object read( ValueFactories valueFactories,
                        byte[] hash,
                        long length ) throws IOException {
        String hashStr = StringUtil.getHexString(hash);
        LargeValueEntity entity = entities.find(LargeValueEntity.class, hashStr);
        if (entity == null) {
            throw new IOException(JpaConnectorI18n.unableToReadLargeValue.text(getSourceName(), hashStr));
        }
        byte[] data = entity.getData();
        return valueFactories.getValueFactory(entity.getType()).create(data);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#write(byte[], long,
     *      org.jboss.dna.graph.properties.PropertyType, java.lang.Object)
     */
    public void write( byte[] hash,
                       long length,
                       PropertyType type,
                       Object value ) throws IOException {
        if (value == null) return;
        String hashStr = StringUtil.getHexString(hash);
        LargeValueEntity entity = entities.find(LargeValueEntity.class, hashStr);
        if (entity == null) {
            entity = new LargeValueEntity();
            entity.setCompressed(true);
            entity.setHash(hashStr);
            entity.setLength(length);
            entity.setType(type);
            ValueFactories factories = getExecutionContext().getValueFactories();
            switch (type) {
                case BINARY:
                    Binary binary = factories.getBinaryFactory().create(value);
                    try {
                        binary.acquire();
                        entity.setData(binary.getBytes());
                    } finally {
                        binary.release();
                    }
                    break;
                default:
                    String str = factories.getStringFactory().create(value);
                    entity.setData(str.getBytes());
                    break;
            }
            entities.persist(entity);
        } else {
            // There is already an existing value, so we'll reuse it and increment the usage count ...
            entity.incrementUsageCount();
        }
    }

    protected static class Namespaces {

        private final EntityManager entityManager;
        private final Map<String, Integer> cache = new HashMap<String, Integer>();

        public Namespaces( EntityManager manager ) {
            this.entityManager = manager;
        }

        public Integer getId( String namespaceUri,
                              boolean createIfRequired ) {
            Integer id = cache.get(namespaceUri);
            if (id == null) {
                NamespaceEntity entity = NamespaceEntity.findByUri(entityManager, namespaceUri, createIfRequired);
                if (entity == null) return null;
                id = entity.getId();
                cache.put(namespaceUri, id);
            }
            assert id != null;
            return id;
        }
    }
}
