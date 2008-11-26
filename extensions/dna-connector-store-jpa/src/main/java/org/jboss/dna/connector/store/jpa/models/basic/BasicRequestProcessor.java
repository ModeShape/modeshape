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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.connector.store.jpa.JpaConnectorI18n;
import org.jboss.dna.connector.store.jpa.models.common.NamespaceEntity;
import org.jboss.dna.connector.store.jpa.models.common.NodeId;
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
import org.jboss.dna.graph.requests.InvalidRequestException;
import org.jboss.dna.graph.requests.MoveBranchRequest;
import org.jboss.dna.graph.requests.ReadAllChildrenRequest;
import org.jboss.dna.graph.requests.ReadAllPropertiesRequest;
import org.jboss.dna.graph.requests.ReadNodeRequest;
import org.jboss.dna.graph.requests.ReadPropertyRequest;
import org.jboss.dna.graph.requests.UpdatePropertiesRequest;
import org.jboss.dna.graph.requests.processor.RequestProcessor;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicRequestProcessor extends RequestProcessor implements LargeValues {

    private final EntityManager entities;
    private final ValueFactory<String> stringFactory;
    private final PathFactory pathFactory;
    private final NameFactory nameFactory;
    private final Namespaces namespaces;
    private final UUID rootNodeUuid;
    private final String rootNodeUuidString;
    private final Serializer serializer;
    private final long largeValueMinimumSizeInBytes;
    private final boolean compressData;
    protected final Logger logger;

    /**
     * @param sourceName
     * @param context
     * @param entityManager
     * @param rootNodeUuid
     * @param largeValueMinimumSizeInBytes
     * @param compressData
     */
    public BasicRequestProcessor( String sourceName,
                                  ExecutionContext context,
                                  EntityManager entityManager,
                                  UUID rootNodeUuid,
                                  long largeValueMinimumSizeInBytes,
                                  boolean compressData ) {
        super(sourceName, context);
        assert entityManager != null;
        assert rootNodeUuid != null;
        this.entities = entityManager;
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.nameFactory = context.getValueFactories().getNameFactory();
        this.namespaces = new Namespaces(entityManager);
        this.rootNodeUuid = rootNodeUuid;
        this.rootNodeUuidString = this.rootNodeUuid.toString();
        this.serializer = new Serializer(context, true);
        this.largeValueMinimumSizeInBytes = largeValueMinimumSizeInBytes;
        this.compressData = compressData;
        this.logger = getExecutionContext().getLogger(getClass());

        // Start the transaction ...
        this.entities.getTransaction().begin();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        String childUuidString = null;
        try {
            // Create nodes have to be defined via a path ...
            Location parentLocation = request.under();
            ActualLocation actual = getActualLocation(parentLocation);
            String parentUuidString = actual.uuid;
            assert parentUuidString != null;

            // We need to look for an existing UUID property in the request,
            // so since we have to iterate through the properties, go ahead an serialize them right away ...
            childUuidString = createProperties(null, request.properties());

            // Find or create the namespace for the child ...
            Name childName = request.named();
            String childNsUri = childName.getNamespaceUri();
            Integer nsId = namespaces.getId(childNsUri, true);
            assert nsId != null;

            // Find the largest SNS index in the existing ChildEntity objects with the same name ...
            String childLocalName = childName.getLocalName();
            Query query = entities.createNamedQuery("ChildEntity.findMaximumSnsIndex");
            query.setParameter("parentUuid", parentUuidString);
            query.setParameter("ns", nsId);
            query.setParameter("childName", childLocalName);
            int nextSnsIndex = 1;
            try {
                Integer result = (Integer)query.getSingleResult();
                nextSnsIndex = result != null ? result + 1 : 1;
            } catch (NoResultException e) {
            }

            // Find the largest child index in the existing ChildEntity objects ...
            query = entities.createNamedQuery("ChildEntity.findMaximumChildIndex");
            query.setParameter("parentUuid", parentUuidString);
            int nextIndexInParent = 1;
            try {
                Integer result = (Integer)query.getSingleResult();
                nextIndexInParent = result != null ? result + 1 : 1;
            } catch (NoResultException e) {
            }

            // Create the new ChildEntity ...
            NamespaceEntity ns = entities.find(NamespaceEntity.class, nsId);
            assert ns != null;
            ChildId id = new ChildId(parentUuidString, childUuidString);
            ChildEntity entity = new ChildEntity(id, nextIndexInParent, ns, childLocalName, nextSnsIndex);
            entities.persist(entity);

            // Look up the actual path, regardless of the supplied path...
            assert childUuidString != null;
            assert actual.location.getPath() != null;
            Path path = pathFactory.create(actual.location.getPath(), childName, nextSnsIndex);
            actualLocation = new Location(path, UUID.fromString(childUuidString));

        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            logger.trace(e, "Problem " + request);
            return;
        }
        request.setActualLocationOfNode(actualLocation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadNodeRequest)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public void process( ReadNodeRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        try {
            Location location = request.at();
            ActualLocation actual = getActualLocation(location);
            String parentUuidString = actual.uuid;
            actualLocation = actual.location;
            Path path = actualLocation.getPath();

            // Record the UUID as a property, since it's not stored in the serialized properties...
            request.addProperty(actualLocation.getIdProperty(DnaLexicon.UUID));

            // Find the properties entity for this node ...
            Query query = entities.createNamedQuery("PropertiesEntity.findByUuid");
            query.setParameter("uuid", parentUuidString);
            try {
                PropertiesEntity entity = (PropertiesEntity)query.getSingleResult();

                // Deserialize the properties ...
                boolean compressed = entity.isCompressed();
                Collection<Property> properties = new LinkedList<Property>();
                byte[] data = entity.getData();
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                InputStream is = compressed ? new ZipInputStream(bais) : bais;
                ObjectInputStream ois = new ObjectInputStream(is);
                try {
                    serializer.deserializeAllProperties(ois, properties, this);
                    for (Property property : properties) {
                        request.addProperty(property);
                    }
                } finally {
                    ois.close();
                }

            } catch (NoResultException e) {
                // No properties, but that's okay...
            }
            // Find the children of the supplied node ...
            query = entities.createNamedQuery("ChildEntity.findAllUnderParent");
            query.setParameter("parentUuidString", parentUuidString);
            List<ChildEntity> children = query.getResultList();
            for (ChildEntity child : children) {
                String namespaceUri = child.getChildNamespace().getUri();
                String localName = child.getChildName();
                Name childName = nameFactory.create(namespaceUri, localName);
                int sns = child.getSameNameSiblingIndex();
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
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadAllChildrenRequest)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public void process( ReadAllChildrenRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        try {
            Location location = request.of();
            ActualLocation actual = getActualLocation(location);
            String parentUuidString = actual.uuid;
            actualLocation = actual.location;
            Path path = actualLocation.getPath();

            // Find the children of the supplied node ...
            Query query = entities.createNamedQuery("ChildEntity.findAllUnderParent");
            query.setParameter("parentUuidString", parentUuidString);
            List<ChildEntity> children = query.getResultList();
            for (ChildEntity child : children) {
                String namespaceUri = child.getChildNamespace().getUri();
                String localName = child.getChildName();
                Name childName = nameFactory.create(namespaceUri, localName);
                int sns = child.getSameNameSiblingIndex();
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
        logger.trace(request.toString());
        Location actualLocation = null;
        try {
            Location location = request.at();
            ActualLocation actual = getActualLocation(location);
            String uuidString = actual.uuid;
            actualLocation = actual.location;

            // Record the UUID as a property, since it's not stored in the serialized properties...
            request.addProperty(actualLocation.getIdProperty(DnaLexicon.UUID));

            // Find the properties entity for this node ...
            Query query = entities.createNamedQuery("PropertiesEntity.findByUuid");
            query.setParameter("uuid", uuidString);
            PropertiesEntity entity = (PropertiesEntity)query.getSingleResult();

            // Deserialize the properties ...
            boolean compressed = entity.isCompressed();
            int propertyCount = entity.getPropertyCount();
            Collection<Property> properties = new ArrayList<Property>(propertyCount);
            byte[] data = entity.getData();
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            InputStream is = compressed ? new ZipInputStream(bais) : bais;
            ObjectInputStream ois = new ObjectInputStream(is);
            try {
                serializer.deserializeAllProperties(ois, properties, this);
                for (Property property : properties) {
                    request.addProperty(property);
                }
            } finally {
                ois.close();
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
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.ReadPropertyRequest)
     */
    @Override
    public void process( ReadPropertyRequest request ) {
        logger.trace(request.toString());
        // Small optimization ...
        final Name propertyName = request.named();
        if (DnaLexicon.UUID.equals(propertyName)) {
            try {
                // Just get the UUID ...
                Location location = request.on();
                ActualLocation actualLocation = getActualLocation(location);
                request.setActualLocationOfNode(actualLocation.location);
            } catch (Throwable e) { // Includes PathNotFoundException
                request.setError(e);
            }
            return;
        }
        // Process the one property that's requested ...
        Location actualLocation = null;
        try {
            Location location = request.on();
            ActualLocation actual = getActualLocation(location);
            String uuidString = actual.uuid;
            actualLocation = actual.location;

            // Find the properties entity for this node ...
            Query query = entities.createNamedQuery("PropertiesEntity.findByUuid");
            query.setParameter("uuid", uuidString);
            PropertiesEntity entity = (PropertiesEntity)query.getSingleResult();

            // Deserialize the stream of properties, but only materialize the one property ...
            boolean compressed = entity.isCompressed();
            int propertyCount = entity.getPropertyCount();
            Collection<Property> properties = new ArrayList<Property>(propertyCount);
            byte[] data = entity.getData();
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            InputStream is = compressed ? new ZipInputStream(bais) : bais;
            ObjectInputStream ois = new ObjectInputStream(is);
            try {
                Serializer.LargeValues skippedLargeValues = Serializer.NO_LARGE_VALUES;
                serializer.deserializeSomeProperties(ois, properties, this, skippedLargeValues, propertyName);
                for (Property property : properties) {
                    request.setProperty(property); // should be only one property
                }
            } finally {
                ois.close();
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
        logger.trace(request.toString());
        Location actualLocation = null;
        try {
            Location location = request.on();
            ActualLocation actual = getActualLocation(location);
            actualLocation = actual.location;

            // Find the properties entity for this node ...
            Query query = entities.createNamedQuery("PropertiesEntity.findByUuid");
            query.setParameter("uuid", actual.uuid);
            PropertiesEntity entity = null;
            try {
                entity = (PropertiesEntity)query.getSingleResult();

                // Determine which large values are referenced ...
                Collection<String> hexKeys = null;
                String largeValueHexKeys = entity.getLargeValueKeys();
                if (largeValueHexKeys != null) {
                    hexKeys = createHexValues(largeValueHexKeys);
                }

                // Prepare the streams so we can deserialize all existing properties and reserialize the old and updated
                // properties ...
                boolean compressed = entity.isCompressed();
                ByteArrayInputStream bais = new ByteArrayInputStream(entity.getData());
                InputStream is = compressed ? new ZipInputStream(bais) : bais;
                ObjectInputStream ois = new ObjectInputStream(is);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStream os = compressed ? new ZipOutputStream(baos) : baos;
                ObjectOutputStream oos = new ObjectOutputStream(os);
                int numProperties = 0;
                SkippedLargeValues skipped = new SkippedLargeValues();
                RecordingLargeValues largeValues = new RecordingLargeValues();
                try {
                    numProperties = serializer.reserializeProperties(ois, oos, request.properties(), largeValues, skipped);
                } finally {
                    try {
                        ois.close();
                    } finally {
                        oos.close();
                    }
                }
                largeValueHexKeys = createHexValuesString(largeValues.writtenKeys);
                entity.setPropertyCount(numProperties);
                entity.setData(baos.toByteArray());
                entity.setCompressed(compressData);
                entity.setLargeValueKeys(largeValueHexKeys);

                // Update the large values that used to be reference but no longer are ...
                if (hexKeys != null) {
                    for (String oldHexKey : skipped.skippedKeys) {
                        LargeValueEntity largeValue = entities.find(LargeValueEntity.class, oldHexKey);
                        if (largeValue != null) {
                            if (largeValue.decrementUsageCount() == 0) {
                                entities.remove(largeValue);
                            }
                        }
                    }
                }
            } catch (NoResultException e) {
                // there are no properties yet ...
                createProperties(actual.uuid, request.properties());
            }

        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        if (actualLocation != null) request.setActualLocationOfNode(actualLocation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        logger.trace(request.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        logger.trace(request.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.requests.processor.RequestProcessor#process(org.jboss.dna.graph.requests.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        logger.trace(request.toString());
        Location actualOldLocation = null;
        Location actualNewLocation = null;
        try {
            Location fromLocation = request.from();
            ActualLocation actualLocation = getActualLocation(fromLocation);
            String fromUuidString = actualLocation.uuid;
            actualOldLocation = actualLocation.location;

            // It's not possible to move the root node
            if (actualOldLocation.getPath().isRoot()) {
                String msg = JpaConnectorI18n.unableToMoveRootNode.text(getSourceName());
                throw new InvalidRequestException(msg);
            }

            // Find the ChildEntity of the existing 'from' node ...
            ChildEntity fromEntity = actualLocation.childEntity;
            final String oldParentUuid = fromEntity.getId().getParentUuidString();

            // Find the actual new location ...
            Location toLocation = request.into();
            String toUuidString = null;
            if (request.hasNoEffect()) {
                actualNewLocation = actualOldLocation;
            } else {
                // We have to proceed as normal ...
                ActualLocation actualIntoLocation = getActualLocation(toLocation);
                toUuidString = actualIntoLocation.uuid;
                if (!toUuidString.equals(oldParentUuid)) {
                    // Now we know that the new parent is not the existing parent ...
                    final int oldSnsIndex = fromEntity.getSameNameSiblingIndex();
                    final int oldIndex = fromEntity.getIndexInParent();

                    // Find the largest SNS index in the existing ChildEntity objects with the same name ...
                    String childLocalName = fromEntity.getChildName();
                    NamespaceEntity ns = fromEntity.getChildNamespace();
                    Query query = entities.createNamedQuery("ChildEntity.findMaximumSnsIndex");
                    query.setParameter("parentUuidString", toUuidString);
                    query.setParameter("ns", ns.getId());
                    query.setParameter("childName", childLocalName);
                    int nextSnsIndex = 1;
                    try {
                        nextSnsIndex = (Integer)query.getSingleResult();
                    } catch (NoResultException e) {
                    }

                    // Find the largest child index in the existing ChildEntity objects ...
                    query = entities.createNamedQuery("ChildEntity.findMaximumChildIndex");
                    query.setParameter("parentUuidString", toUuidString);
                    int nextIndexInParent = 1;
                    try {
                        nextIndexInParent = (Integer)query.getSingleResult() + 1;
                    } catch (NoResultException e) {
                    }

                    // Move the child entity to be under the new parent ...
                    fromEntity.setId(new ChildId(toUuidString, fromUuidString));
                    fromEntity.setIndexInParent(nextIndexInParent);
                    fromEntity.setSameNameSiblingIndex(nextSnsIndex);

                    // And adjust the SNS index and indexes ...
                    adjustSnsIndexesAndIndexesAfterRemoving(oldParentUuid, childLocalName, ns.getId(), oldIndex, oldSnsIndex);
                }
            }

        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        request.setActualLocations(actualOldLocation, actualNewLocation);
    }

    protected void adjustSnsIndexesAndIndexesAfterRemoving( String uuidParent,
                                                            String childName,
                                                            int childNamespaceIndex,
                                                            int childIndex,
                                                            int childSnsIndex ) {

    }

    protected String createProperties( String uuidString,
                                       Collection<Property> properties ) throws IOException {
        RecordingLargeValues largeValues = new RecordingLargeValues();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream os = compressData ? new ZipOutputStream(baos) : baos;
        ObjectOutputStream oos = new ObjectOutputStream(os);
        int numProperties = properties.size();
        try {
            oos.writeInt(numProperties);
            for (Property property : properties) {
                if (uuidString == null && property.getName().equals(DnaLexicon.UUID)) {
                    uuidString = stringFactory.create(property.getFirstValue());
                }
                if (serializer.serializeProperty(oos, property, largeValues)) ++numProperties;
            }
        } finally {
            oos.close();
        }
        String largeValueHexHashesString = createHexValuesString(largeValues.writtenKeys);
        if (uuidString == null) uuidString = stringFactory.create(UUID.randomUUID());

        // Create the PropertiesEntity ...
        NodeId nodeId = new NodeId(uuidString);
        PropertiesEntity props = new PropertiesEntity(nodeId);
        props.setData(baos.toByteArray());
        props.setCompressed(compressData);
        props.setPropertyCount(numProperties);
        props.setLargeValueKeys(largeValueHexHashesString);
        entities.persist(props);
        return uuidString;
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

    /**
     * Utility method to look up the actual information given a supplied location. This method verifies that the location actually
     * represents an existing node, or it throws a {@link PathNotFoundException}. In all cases, the resulting information contains
     * the correct path and the correct UUID.
     * <p>
     * Note that this method sometimes performs "unnecessary" work when the location contains both a path to a node and the node's
     * corresponding UUID. Strictly speaking, this method would need to do very little. However, in such cases, this method does
     * verify that the information is still correct (ensuring that calls to use the {@link ChildEntity} will be correct). So,
     * while this work <i>may</i> be unnecessary, it does ensure that the location is consistent and correct (something that is
     * not unnecessary).
     * </p>
     * <p>
     * There are cases when a request containing a Path and a UUID are no longer correct. The node may have been just moved by
     * another request (perhaps from a different client), or there may be an error in the component making the request. In these
     * cases, this method assumes that the path is incorrect (since paths may change) and finds the <i>correct path</i> given the
     * UUID.
     * </p>
     * <p>
     * This method will also find the path when the location contains just the UUID.
     * </p>
     * 
     * @param original the original location; may not be null
     * @return the actual location, which includes the verified location and additional information needed by this method that may
     *         be usable after this method is called
     * @throws PathNotFoundException if the location does not represent a location that could be found
     */
    protected ActualLocation getActualLocation( Location original ) throws PathNotFoundException {
        assert original != null;

        // Look for the UUID in the original ...
        Property uuidProperty = original.getIdProperty(DnaLexicon.UUID);
        String uuidString = uuidProperty != null && !uuidProperty.isEmpty() ? stringFactory.create(uuidProperty.getFirstValue()) : null;

        // If the original location has a UUID, then use that to find the child entity that represents the location ...
        if (uuidString != null) {
            // The original has a UUID, so use that to find the child entity.
            // Then walk up the ancestors and build the path.
            String nodeUuidString = uuidString;
            LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
            // while (uuidString != null && !uuidString.equals(this.rootNodeUuidString)) {
            // // Find the parent of the child, along with the child's name and SNS index ...
            // Query query = entities.createNamedQuery("ChildEntity.findValuesByChildUuid");
            // query.setParameter("childUuidString", uuidString);
            // try {
            // Object[] record = (Object[])query.getSingleResult();
            // String parentUuidString = (String)record[0];
            // String uri = (String)record[1];
            // String localName = (String)record[2];
            // int sns = (Integer)record[3];
            // // Now create the path segment and set the next child UUID as the parent of this child ...
            // Name name = nameFactory.create(uri, localName);
            // segments.addFirst(pathFactory.createSegment(name, sns));
            // uuidString = parentUuidString;
            // } catch (NoResultException e) {
            // uuidString = null;
            // }
            // }
            // Path fullPath = pathFactory.createAbsolutePath(segments);
            // return new ActualLocation(new Location(fullPath, uuidProperty), nodeUuidString, null);
            ChildEntity entity = null;
            while (uuidString != null && !uuidString.equals(this.rootNodeUuidString)) {
                Query query = entities.createNamedQuery("ChildEntity.findByChildUuid");
                query.setParameter("childUuidString", uuidString);
                try {
                    // Find the parent of the UUID ...
                    entity = (ChildEntity)query.getSingleResult();
                    String localName = entity.getChildName();
                    String uri = entity.getChildNamespace().getUri();
                    int sns = entity.getSameNameSiblingIndex();
                    Name name = nameFactory.create(uri, localName);
                    segments.addFirst(pathFactory.createSegment(name, sns));
                    uuidString = entity.getId().getParentUuidString();
                } catch (NoResultException e) {
                    uuidString = null;
                }
            }
            Path fullPath = pathFactory.createAbsolutePath(segments);
            return new ActualLocation(new Location(fullPath, uuidProperty), nodeUuidString, entity);
        }

        // There is no UUID, so look for a path ...
        Path path = original.getPath();
        if (path == null) {
            String propName = DnaLexicon.UUID.getString(getExecutionContext().getNamespaceRegistry());
            String msg = JpaConnectorI18n.locationShouldHavePathAndOrProperty.text(getSourceName(), propName);
            throw new PathNotFoundException(original, pathFactory.createRootPath(), msg);
        }

        // Walk the child entities, starting at the root, down the to the path ...
        if (path.isRoot()) {
            return new ActualLocation(original.with(rootNodeUuid), rootNodeUuidString, null);
        }
        String parentUuid = this.rootNodeUuidString;
        // String childUuid = null;
        // for (Path.Segment segment : path) {
        // Name name = segment.getName();
        // String localName = name.getLocalName();
        // String nsUri = name.getNamespaceUri();
        // int snsIndex = segment.hasIndex() ? segment.getIndex() : 1;
        //
        // Query query = entities.createNamedQuery("ChildEntity.findChildUuidByPathSegment");
        // query.setParameter("parentUuidString", parentUuid);
        // query.setParameter("nsUri", nsUri);
        // query.setParameter("childName", localName);
        // query.setParameter("sns", snsIndex);
        // try {
        // childUuid = (String)query.getSingleResult();
        // } catch (NoResultException e) {
        // // Unable to complete the path, so prepare the exception by determining the lowest path that exists ...
        // Path lowest = path;
        // while (lowest.getLastSegment() != segment) {
        // lowest = lowest.getParent();
        // }
        // lowest = lowest.getParent();
        // throw new PathNotFoundException(original, lowest);
        // }
        // parentUuid = childUuid;
        // }
        // return new ActualLocation(original.with(UUID.fromString(childUuid)), childUuid, null);

        ChildEntity child = null;
        for (Path.Segment segment : path) {
            child = findByPathSegment(parentUuid, segment);
            if (child == null) {
                // Unable to complete the path, so prepare the exception by determining the lowest path that exists ...
                Path lowest = path;
                while (lowest.getLastSegment() != segment) {
                    lowest = lowest.getParent();
                }
                lowest = lowest.getParent();
                throw new PathNotFoundException(original, lowest);
            }
            parentUuid = child.getId().getChildUuidString();
        }
        assert child != null;
        uuidString = child.getId().getChildUuidString();
        return new ActualLocation(original.with(UUID.fromString(uuidString)), uuidString, child);
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
        int snsIndex = pathSegment.hasIndex() ? pathSegment.getIndex() : 1;
        if (nsId == null) {
            // The namespace can't be found, then certainly the node won't be found ...
            return null;
        }
        Query query = entities.createNamedQuery("ChildEntity.findByPathSegment");
        query.setParameter("parentUuidString", parentUuid);
        query.setParameter("ns", nsId);
        query.setParameter("childName", localName);
        query.setParameter("sns", snsIndex);
        try {
            return (ChildEntity)query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    protected String createHexValuesString( Collection<String> hexValues ) {
        if (hexValues == null || hexValues.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String hexValue : hexValues) {
            if (first) {
                first = false;
            } else {
                sb.append(',');
            }
            sb.append(hexValue);
        }
        return sb.toString();
    }

    protected Collection<String> createHexValues( String hexValuesString ) {
        return Arrays.asList(hexValuesString.split(","));
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
            byte[] bytes = null;
            switch (type) {
                case BINARY:
                    Binary binary = factories.getBinaryFactory().create(value);
                    InputStream stream = null;
                    try {
                        binary.acquire();
                        stream = binary.getStream();
                        if (compressData) stream = new ZipInputStream(stream);
                        bytes = IoUtil.readBytes(stream);
                    } finally {
                        try {
                            if (stream != null) stream.close();
                        } finally {
                            binary.release();
                        }
                    }
                    break;
                default:
                    String str = factories.getStringFactory().create(value);
                    bytes = str.getBytes();
                    if (compressData) {
                        InputStream strStream = new ZipInputStream(new ByteArrayInputStream(bytes));
                        try {
                            bytes = IoUtil.readBytes(strStream);
                        } finally {
                            strStream.close();
                        }
                    }
                    break;
            }
            entity.setData(bytes);
            entities.persist(entity);
        } else {
            // There is already an existing value, so we'll reuse it and increment the usage count ...
            entity.incrementUsageCount();
        }
    }

    protected class RecordingLargeValues implements LargeValues {
        protected Collection<String> readKeys = new HashSet<String>();
        protected Collection<String> writtenKeys = new HashSet<String>();

        RecordingLargeValues() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#getMinimumSize()
         */
        public long getMinimumSize() {
            return BasicRequestProcessor.this.getMinimumSize();
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
            String key = StringUtil.getHexString(hash);
            readKeys.add(key);
            return BasicRequestProcessor.this.read(valueFactories, hash, length);
        }

        public void write( byte[] hash,
                           long length,
                           PropertyType type,
                           Object value ) throws IOException {
            String key = StringUtil.getHexString(hash);
            writtenKeys.add(key);
            BasicRequestProcessor.this.write(hash, length, type, value);
        }
    }

    protected class SkippedLargeValues implements LargeValues {
        protected Collection<String> skippedKeys = new HashSet<String>();

        SkippedLargeValues() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#getMinimumSize()
         */
        public long getMinimumSize() {
            return BasicRequestProcessor.this.getMinimumSize();
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
            String key = StringUtil.getHexString(hash);
            skippedKeys.add(key);
            return null;
        }

        public void write( byte[] hash,
                           long length,
                           PropertyType type,
                           Object value ) {
            throw new UnsupportedOperationException();
        }
    }

    @Immutable
    protected static class ActualLocation {
        /** The actual location */
        protected final Location location;
        /** The string-form of the UUID, supplied as a convenience. */
        protected final String uuid;
        /** The ChildEntity that represents the location, which may be null if the location represents the root node */
        protected final ChildEntity childEntity;

        protected ActualLocation( Location location,
                                  String uuid,
                                  ChildEntity childEntity ) {
            assert location != null;
            assert uuid != null;
            this.location = location;
            this.uuid = uuid;
            this.childEntity = childEntity;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return this.location.toString() + " (uuid=" + uuid + ") " + childEntity;
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
