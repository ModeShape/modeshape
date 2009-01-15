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
package org.jboss.dna.connector.store.jpa.model.basic;

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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
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
import org.jboss.dna.connector.store.jpa.model.common.NamespaceEntity;
import org.jboss.dna.connector.store.jpa.util.Namespaces;
import org.jboss.dna.connector.store.jpa.util.RequestProcessorCache;
import org.jboss.dna.connector.store.jpa.util.Serializer;
import org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.Reference;
import org.jboss.dna.graph.property.ReferentialIntegrityException;
import org.jboss.dna.graph.property.UuidFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.InvalidRequestException;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadBranchRequest;
import org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.ReadPropertyRequest;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class BasicRequestProcessor extends RequestProcessor {

    protected final EntityManager entities;
    protected final ValueFactory<String> stringFactory;
    protected final PathFactory pathFactory;
    protected final NameFactory nameFactory;
    protected final UuidFactory uuidFactory;
    protected final Namespaces namespaces;
    protected final UUID rootNodeUuid;
    protected final String rootNodeUuidString;
    protected final Serializer serializer;
    protected final long largeValueMinimumSizeInBytes;
    protected final boolean compressData;
    protected final Logger logger;
    protected final RequestProcessorCache cache;
    protected final boolean enforceReferentialIntegrity;
    private boolean referencesChanged;

    /**
     * @param sourceName
     * @param context
     * @param entityManager
     * @param rootNodeUuid
     * @param largeValueMinimumSizeInBytes
     * @param compressData
     * @param enforceReferentialIntegrity
     */
    public BasicRequestProcessor( String sourceName,
                                  ExecutionContext context,
                                  EntityManager entityManager,
                                  UUID rootNodeUuid,
                                  long largeValueMinimumSizeInBytes,
                                  boolean compressData,
                                  boolean enforceReferentialIntegrity ) {
        super(sourceName, context);
        assert entityManager != null;
        assert rootNodeUuid != null;
        this.entities = entityManager;
        ValueFactories valuesFactory = context.getValueFactories();
        this.stringFactory = valuesFactory.getStringFactory();
        this.pathFactory = valuesFactory.getPathFactory();
        this.nameFactory = valuesFactory.getNameFactory();
        this.uuidFactory = valuesFactory.getUuidFactory();
        this.namespaces = new Namespaces(entityManager);
        this.rootNodeUuid = rootNodeUuid;
        this.rootNodeUuidString = this.rootNodeUuid.toString();
        this.largeValueMinimumSizeInBytes = largeValueMinimumSizeInBytes;
        this.compressData = compressData;
        this.enforceReferentialIntegrity = enforceReferentialIntegrity;
        this.serializer = new Serializer(context, true);
        this.logger = getExecutionContext().getLogger(getClass());
        this.cache = new RequestProcessorCache(this.pathFactory);

        // Start the transaction ...
        this.entities.getTransaction().begin();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        try {
            // Create nodes have to be defined via a path ...
            Location parentLocation = request.under();
            ActualLocation actual = getActualLocation(parentLocation);
            String parentUuidString = actual.uuid;
            assert parentUuidString != null;

            // We need to look for an existing UUID property in the request,
            // so since we have to iterate through the properties, go ahead an serialize them right away ...
            String uuidString = null;
            for (Property property : request.properties()) {
                if (property.getName().equals(DnaLexicon.UUID)) {
                    uuidString = stringFactory.create(property.getFirstValue());
                    break;
                }
            }
            if (uuidString == null) uuidString = UUID.randomUUID().toString();
            assert uuidString != null;
            createProperties(uuidString, request.properties());

            // Find or create the namespace for the child ...
            Name childName = request.named();
            String childNsUri = childName.getNamespaceUri();
            NamespaceEntity ns = namespaces.get(childNsUri, true);
            assert ns != null;
            final Path parentPath = actual.location.getPath();
            assert parentPath != null;

            // Figure out the next SNS index and index-in-parent for this new child ...
            actualLocation = addNewChild(actual, uuidString, childName);

            // Since we've just created this node, we know about all the children (actually, there are none).
            cache.setAllChildren(actualLocation.getPath(), new LinkedList<Location>());

            // Flush the entities ...
            // entities.flush();

        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            logger.trace(e, "Problem " + request);
            return;
        }
        request.setActualLocationOfNode(actualLocation);
    }

    protected Location addNewChild( ActualLocation parent,
                                    String childUuid,
                                    Name childName ) {
        int nextSnsIndex = 1; // SNS index is 1-based
        int nextIndexInParent = 0; // index-in-parent is 0-based
        String childNsUri = childName.getNamespaceUri();
        NamespaceEntity ns = namespaces.get(childNsUri, true);
        assert ns != null;

        final Path parentPath = parent.location.getPath();
        assert parentPath != null;

        // Look in the cache for the children of the parent node.
        LinkedList<Location> childrenOfParent = cache.getAllChildren(parentPath);
        if (childrenOfParent != null) {
            // The cache had the complete list of children for the parent node, which means
            // we know about all of the children and can walk the children to figure out the next indexes.
            nextIndexInParent = childrenOfParent.size();
            if (nextIndexInParent > 1) {
                // Since we want the last indexes, process the list backwards ...
                ListIterator<Location> iter = childrenOfParent.listIterator(childrenOfParent.size());
                while (iter.hasPrevious()) {
                    Location existing = iter.previous();
                    Path.Segment segment = existing.getPath().getLastSegment();
                    if (!segment.getName().equals(childName)) continue;
                    // Otherwise the name matched, so get the indexes ...
                    nextSnsIndex = segment.getIndex() + 1;
                }
            }
        } else {
            // The cache did not have the complete list of children for the parent node,
            // so we need to look the values up by querying the database ...

            // Find the largest SNS index in the existing ChildEntity objects with the same name ...
            String childLocalName = childName.getLocalName();
            Query query = entities.createNamedQuery("ChildEntity.findMaximumSnsIndex");
            query.setParameter("parentUuid", parent.uuid);
            query.setParameter("ns", ns.getId());
            query.setParameter("childName", childLocalName);
            try {
                Integer result = (Integer)query.getSingleResult();
                nextSnsIndex = result != null ? result + 1 : 1; // SNS index is 1-based
            } catch (NoResultException e) {
            }

            // Find the largest child index in the existing ChildEntity objects ...
            query = entities.createNamedQuery("ChildEntity.findMaximumChildIndex");
            query.setParameter("parentUuid", parent.uuid);
            try {
                Integer result = (Integer)query.getSingleResult();
                nextIndexInParent = result != null ? result + 1 : 0; // index-in-parent is 0-based
            } catch (NoResultException e) {
            }
        }

        // Create the new ChildEntity ...
        ChildId id = new ChildId(parent.uuid, childUuid);
        ChildEntity entity = new ChildEntity(id, nextIndexInParent, ns, childName.getLocalName(), nextSnsIndex);
        entities.persist(entity);

        // Set the actual path, regardless of the supplied path...
        Path path = pathFactory.create(parentPath, childName, nextSnsIndex);
        Location actualLocation = new Location(path, UUID.fromString(childUuid));

        // Finally, update the cache with the information we know ...
        if (childrenOfParent != null) {
            // Add to the cached list of children ...
            childrenOfParent.add(actualLocation);
        }
        return actualLocation;
    }

    protected class NextChildIndexes {
        protected final int nextIndexInParent;
        protected final int nextSnsIndex;

        protected NextChildIndexes( int nextIndexInParent,
                                    int nextSnsIndex ) {
            this.nextIndexInParent = nextIndexInParent;
            this.nextSnsIndex = nextSnsIndex;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        try {
            Location location = request.at();
            ActualLocation actual = getActualLocation(location);
            String parentUuidString = actual.uuid;
            actualLocation = actual.location;

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
                if (data != null) {
                    LargeValueSerializer largeValues = new LargeValueSerializer(entity);
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    InputStream is = compressed ? new GZIPInputStream(bais) : bais;
                    ObjectInputStream ois = new ObjectInputStream(is);
                    try {
                        serializer.deserializeAllProperties(ois, properties, largeValues);
                        for (Property property : properties) {
                            request.addProperty(property);
                        }
                    } finally {
                        ois.close();
                    }
                }

            } catch (NoResultException e) {
                // No properties, but that's okay...
            }

            // Get the children for this node ...
            for (Location childLocation : getAllChildren(actual)) {
                request.addChild(childLocation);
            }
        } catch (NoResultException e) {
            // there are no properties (probably not expected, but still okay) ...
        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        if (actualLocation != null) request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        try {
            Location location = request.of();
            ActualLocation actual = getActualLocation(location);
            actualLocation = actual.location;

            // Get the children for this node ...
            for (Location childLocation : getAllChildren(actual)) {
                request.addChild(childLocation);
            }
        } catch (NoResultException e) {
            // there are no properties (probably not expected, but still okay) ...
        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        if (actualLocation != null) request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    /**
     * Utility method to obtain all of the children for a node, either from the cache (if all children are known to this
     * processor) or by querying the database (and caching the list of children).
     * 
     * @param parent the actual location of the parent node; may not be null
     * @return the list of child locations
     */
    protected LinkedList<Location> getAllChildren( ActualLocation parent ) {
        assert parent != null;
        Path parentPath = parent.location.getPath();
        assert parentPath != null;
        LinkedList<Location> cachedChildren = cache.getAllChildren(parentPath);
        if (cachedChildren != null) {
            // The cache has all of the children for the node ...
            return cachedChildren;
        }

        // Not found in the cache, so query the database ...
        Query query = entities.createNamedQuery("ChildEntity.findAllUnderParent");
        query.setParameter("parentUuidString", parent.uuid);
        LinkedList<Location> childLocations = new LinkedList<Location>();
        @SuppressWarnings( "unchecked" )
        List<ChildEntity> children = query.getResultList();
        for (ChildEntity child : children) {
            String namespaceUri = child.getChildNamespace().getUri();
            String localName = child.getChildName();
            Name childName = nameFactory.create(namespaceUri, localName);
            int sns = child.getSameNameSiblingIndex();
            Path childPath = pathFactory.create(parentPath, childName, sns);
            String childUuidString = child.getId().getChildUuidString();
            Location childLocation = new Location(childPath, UUID.fromString(childUuidString));
            childLocations.add(childLocation);
        }
        // Update the cache ...
        cache.setAllChildren(parentPath, childLocations);
        return childLocations;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadBlockOfChildrenRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        final int startingIndex = request.startingAtIndex();
        try {
            Location parentLocation = request.of();
            ActualLocation actualParent = getActualLocation(parentLocation);
            actualLocation = actualParent.location;

            Path parentPath = actualParent.location.getPath();
            assert parentPath != null;
            LinkedList<Location> cachedChildren = cache.getAllChildren(parentPath);
            if (cachedChildren != null) {
                // The cache has all of the children for the node ...
                if (startingIndex < cachedChildren.size()) {
                    ListIterator<Location> iter = cachedChildren.listIterator(startingIndex);
                    for (int i = 0; i != request.count() && iter.hasNext(); ++i) {
                        Location child = iter.next();
                        request.addChild(child);
                    }
                }
            } else {
                // Nothing was cached, so we need to search the database for the children ...
                Query query = entities.createNamedQuery("ChildEntity.findRangeUnderParent");
                query.setParameter("parentUuidString", actualParent.uuid);
                query.setParameter("firstIndex", startingIndex);
                query.setParameter("afterIndex", startingIndex + request.count());
                @SuppressWarnings( "unchecked" )
                List<ChildEntity> children = query.getResultList();
                for (ChildEntity child : children) {
                    String namespaceUri = child.getChildNamespace().getUri();
                    String localName = child.getChildName();
                    Name childName = nameFactory.create(namespaceUri, localName);
                    int sns = child.getSameNameSiblingIndex();
                    Path childPath = pathFactory.create(parentPath, childName, sns);
                    String childUuidString = child.getId().getChildUuidString();
                    Location childLocation = new Location(childPath, UUID.fromString(childUuidString));
                    request.addChild(childLocation);
                }
                // Do not update the cache, since we don't know all of the children.
            }

        } catch (NoResultException e) {
            // there are no properties (probably not expected, but still okay) ...
        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        if (actualLocation != null) request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadNextBlockOfChildrenRequest)
     */
    @Override
    public void process( ReadNextBlockOfChildrenRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        final Location previousSibling = request.startingAfter();
        final int count = request.count();
        try {
            ActualLocation actualSibling = getActualLocation(previousSibling);
            actualLocation = actualSibling.location;
            if (!actualLocation.getPath().isRoot()) {
                // First look in the cache for the children of the parent ...
                Path parentPath = actualSibling.location.getPath().getParent();
                assert parentPath != null;
                LinkedList<Location> cachedChildren = cache.getAllChildren(parentPath);
                if (cachedChildren != null) {
                    // The cache has all of the children for the node.
                    // First find the location of the previous sibling ...
                    boolean accumulate = false;
                    int counter = 0;
                    for (Location child : cachedChildren) {
                        if (accumulate) {
                            // We're accumulating children ...
                            request.addChild(child);
                            ++counter;
                            if (counter <= count) continue;
                            break;
                        }
                        // Haven't found the previous sibling yet ...
                        if (child.isSame(previousSibling)) {
                            accumulate = true;
                        }
                    }
                } else {
                    // The children were not found in the cache, so we have to search the database.
                    // We don't know the UUID of the parent, so find the previous sibling and
                    // then get the starting index and the parent UUID ...
                    ChildEntity previousChild = actualSibling.childEntity;
                    if (previousChild == null) {
                        Query query = entities.createNamedQuery("ChildEntity.findByChildUuid");
                        query.setParameter("childUuidString", actualSibling.uuid);
                        previousChild = (ChildEntity)query.getSingleResult();
                    }
                    int startingIndex = previousChild.getIndexInParent() + 1;
                    String parentUuid = previousChild.getId().getParentUuidString();

                    // Now search the database for the children ...
                    Query query = entities.createNamedQuery("ChildEntity.findRangeUnderParent");
                    query.setParameter("parentUuidString", parentUuid);
                    query.setParameter("firstIndex", startingIndex);
                    query.setParameter("afterIndex", startingIndex + request.count());
                    @SuppressWarnings( "unchecked" )
                    List<ChildEntity> children = query.getResultList();
                    LinkedList<Location> allChildren = null;
                    if (startingIndex == 1 && children.size() < request.count()) {
                        // The previous child was the first sibling, and we got fewer children than
                        // the max count. This means we know all of the children, so accumulate the locations
                        // so they can be cached ...
                        allChildren = new LinkedList<Location>();
                        allChildren.add(actualSibling.location);
                    }
                    for (ChildEntity child : children) {
                        String namespaceUri = child.getChildNamespace().getUri();
                        String localName = child.getChildName();
                        Name childName = nameFactory.create(namespaceUri, localName);
                        int sns = child.getSameNameSiblingIndex();
                        Path childPath = pathFactory.create(parentPath, childName, sns);
                        String childUuidString = child.getId().getChildUuidString();
                        Location childLocation = new Location(childPath, UUID.fromString(childUuidString));
                        request.addChild(childLocation);
                        if (allChildren != null) {
                            // We're going to cache the results, so add this child ...
                            allChildren.add(childLocation);
                        }
                    }

                    if (allChildren != null) {
                        cache.setAllChildren(parentPath, allChildren);
                    }
                }
            }

        } catch (NoResultException e) {
            // there are no properties (probably not expected, but still okay) ...
        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        if (actualLocation != null) request.setActualLocationOfStartingAfterNode(actualLocation);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadAllPropertiesRequest)
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
            if (data != null) {
                LargeValueSerializer largeValues = new LargeValueSerializer(entity);
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                InputStream is = compressed ? new GZIPInputStream(bais) : bais;
                ObjectInputStream ois = new ObjectInputStream(is);
                try {
                    serializer.deserializeAllProperties(ois, properties, largeValues);
                    for (Property property : properties) {
                        request.addProperty(property);
                    }
                } finally {
                    ois.close();
                }
            }
        } catch (NoResultException e) {
            // there are no properties (probably not expected, but still okay) ...
        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        if (actualLocation != null) request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadPropertyRequest)
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
                UUID uuid = actualLocation.location.getUuid();
                Property uuidProperty = getExecutionContext().getPropertyFactory().create(propertyName, uuid);
                request.setProperty(uuidProperty);
                request.setActualLocationOfNode(actualLocation.location);
                setCacheableInfo(request);
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
            if (data != null) {
                LargeValueSerializer largeValues = new LargeValueSerializer(entity);
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                InputStream is = compressed ? new GZIPInputStream(bais) : bais;
                ObjectInputStream ois = new ObjectInputStream(is);
                try {
                    Serializer.LargeValues skippedLargeValues = Serializer.NO_LARGE_VALUES;
                    serializer.deserializeSomeProperties(ois, properties, largeValues, skippedLargeValues, propertyName);
                    for (Property property : properties) {
                        request.setProperty(property); // should be only one property
                    }
                } finally {
                    ois.close();
                }
            }
        } catch (NoResultException e) {
            // there are no properties (probably not expected, but still okay) ...
        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        if (actualLocation != null) request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.UpdatePropertiesRequest)
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

                // Prepare the streams so we can deserialize all existing properties and reserialize the old and updated
                // properties ...
                boolean compressed = entity.isCompressed();
                byte[] originalData = entity.getData();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStream os = compressed ? new GZIPOutputStream(baos) : baos;
                ObjectOutputStream oos = new ObjectOutputStream(os);
                int numProps = 0;
                LargeValueSerializer largeValues = null;
                Collection<Property> props = request.properties();
                References refs = enforceReferentialIntegrity ? new References() : null;
                if (originalData == null) {
                    largeValues = new LargeValueSerializer(entity);
                    numProps = props.size();
                    serializer.serializeProperties(oos, numProps, props, largeValues, refs);
                } else {
                    boolean hadLargeValues = !entity.getLargeValues().isEmpty();
                    Set<String> largeValueHashesWritten = hadLargeValues ? new HashSet<String>() : null;
                    largeValues = new LargeValueSerializer(entity, largeValueHashesWritten);
                    ByteArrayInputStream bais = new ByteArrayInputStream(originalData);
                    InputStream is = compressed ? new GZIPInputStream(bais) : bais;
                    ObjectInputStream ois = new ObjectInputStream(is);
                    SkippedLargeValues removedValues = new SkippedLargeValues(largeValues);
                    try {
                        Serializer.ReferenceValues refValues = refs != null ? refs : Serializer.NO_REFERENCES_VALUES;
                        numProps = serializer.reserializeProperties(ois, oos, props, largeValues, removedValues, refValues);
                    } finally {
                        try {
                            ois.close();
                        } finally {
                            oos.close();
                        }
                    }
                    // The new large values were recorded and associated with the properties entity during reserialization.
                    // However, any values no longer used now need to be removed ...
                    if (hadLargeValues) {
                        // Remove any large value from the 'skipped' list that was also written ...
                        removedValues.skippedKeys.removeAll(largeValueHashesWritten);
                        for (String oldHexKey : removedValues.skippedKeys) {
                            LargeValueId id = new LargeValueId(oldHexKey);
                            entity.getLargeValues().remove(id);
                        }
                    }

                    if (refs != null) {
                        // Remove any existing references ...
                        if (refs.hasRemoved()) {
                            for (Reference reference : refs.getRemoved()) {
                                String toUuid = resolveToUuid(reference);
                                if (toUuid != null) {
                                    ReferenceId id = new ReferenceId(actual.uuid, toUuid);
                                    ReferenceEntity refEntity = entities.find(ReferenceEntity.class, id);
                                    if (refEntity != null) {
                                        entities.remove(refEntity);
                                        referencesChanged = true;
                                    }
                                }
                            }
                        }
                    }
                }
                entity.setPropertyCount(numProps);
                entity.setData(baos.toByteArray());
                entity.setCompressed(compressData);

                if (refs != null && refs.hasWritten()) {
                    // If there were references from the updated node ...
                    Set<Reference> newReferences = refs.getWritten();
                    // Remove any reference that was written (and not removed) ...
                    newReferences.removeAll(refs.getRead());
                    if (newReferences.size() != 0) {
                        // Now save the new references ...
                        for (Reference reference : newReferences) {
                            String toUuid = resolveToUuid(reference);
                            if (toUuid != null) {
                                ReferenceId id = new ReferenceId(actual.uuid, toUuid);
                                ReferenceEntity refEntity = new ReferenceEntity(id);
                                entities.persist(refEntity);
                                referencesChanged = true;
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.ReadBranchRequest)
     */
    @Override
    public void process( ReadBranchRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        try {
            Location location = request.at();
            ActualLocation actual = getActualLocation(location);
            actualLocation = actual.location;
            Path path = actualLocation.getPath();

            // Record the location of each node by its UUID; we'll use this when processing the properties ...
            Map<String, Location> locationsByUuid = new HashMap<String, Location>();
            locationsByUuid.put(actual.uuid, location);

            // Compute the subgraph, including the root ...
            int maxDepth = request.maximumDepth();
            SubgraphQuery query = SubgraphQuery.create(getExecutionContext(), entities, actualLocation.getUuid(), path, maxDepth);

            try {
                // Record all of the children ...
                Path parent = path;
                String parentUuid = actual.uuid;
                Location parentLocation = actualLocation;
                List<Location> children = new LinkedList<Location>();
                Map<Location, List<Location>> childrenByParentLocation = new HashMap<Location, List<Location>>();
                childrenByParentLocation.put(parentLocation, children);
                boolean includeChildrenOfNodesAtMaxDepth = true;
                for (ChildEntity child : query.getNodes(false, includeChildrenOfNodesAtMaxDepth)) {
                    String namespaceUri = child.getChildNamespace().getUri();
                    String localName = child.getChildName();
                    Name childName = nameFactory.create(namespaceUri, localName);
                    int sns = child.getSameNameSiblingIndex();
                    // Figure out who the parent is ...
                    String childParentUuid = child.getId().getParentUuidString();
                    if (!parentUuid.equals(childParentUuid)) {
                        // Find the correct parent ...
                        parentLocation = locationsByUuid.get(childParentUuid);
                        parent = parentLocation.getPath();
                        parentUuid = childParentUuid;
                        // See if there is already a list of children for this parent ...
                        children = childrenByParentLocation.get(parentLocation);
                        if (children == null) {
                            children = new LinkedList<Location>();
                            childrenByParentLocation.put(parentLocation, children);
                        }
                    }
                    assert children != null;
                    Path childPath = pathFactory.create(parent, childName, sns);
                    String childUuidString = child.getId().getChildUuidString();
                    Location childLocation = new Location(childPath, UUID.fromString(childUuidString));
                    locationsByUuid.put(childUuidString, childLocation);
                    children.add(childLocation);
                }
                // Now add the list of children to the results ...
                for (Map.Entry<Location, List<Location>> entry : childrenByParentLocation.entrySet()) {
                    // Don't add if there are no children ...
                    if (!entry.getValue().isEmpty()) {
                        request.setChildren(entry.getKey(), entry.getValue());
                    }
                }

                // Note that we've found children for nodes that are at the maximum depth. This is so that the nodes
                // in the subgraph all have the correct children. However, we don't want to store the properties for
                // any node whose depth is greater than the maximum depth. Therefore, only get the properties that
                // include nodes within the maximum depth...
                includeChildrenOfNodesAtMaxDepth = false;

                // Now record all of the properties ...
                for (PropertiesEntity props : query.getProperties(true, includeChildrenOfNodesAtMaxDepth)) {
                    boolean compressed = props.isCompressed();
                    int propertyCount = props.getPropertyCount();
                    Collection<Property> properties = new ArrayList<Property>(propertyCount);
                    Location nodeLocation = locationsByUuid.get(props.getId().getUuidString());
                    assert nodeLocation != null;
                    // Record the UUID as a property, since it's not stored in the serialized properties...
                    properties.add(actualLocation.getIdProperty(DnaLexicon.UUID));
                    // Deserialize all the properties (except the UUID)...
                    byte[] data = props.getData();
                    if (data != null) {
                        LargeValueSerializer largeValues = new LargeValueSerializer(props);
                        ByteArrayInputStream bais = new ByteArrayInputStream(data);
                        InputStream is = compressed ? new GZIPInputStream(bais) : bais;
                        ObjectInputStream ois = new ObjectInputStream(is);
                        try {
                            serializer.deserializeAllProperties(ois, properties, largeValues);
                            request.setProperties(nodeLocation, properties);
                        } finally {
                            ois.close();
                        }
                    }
                }
            } finally {
                // Close and release the temporary data used for this operation ...
                query.close();
            }

        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        logger.trace(request.toString());
        Location actualFromLocation = null;
        Location actualToLocation = null;
        try {
            Location fromLocation = request.from();
            ActualLocation actualFrom = getActualLocation(fromLocation);
            actualFromLocation = actualFrom.location;
            Path fromPath = actualFromLocation.getPath();

            Location newParentLocation = request.into();
            ActualLocation actualNewParent = getActualLocation(newParentLocation);
            assert actualNewParent != null;

            // Create a map that we'll use to record the new UUID for each of the original nodes ...
            Map<String, String> originalToNewUuid = new HashMap<String, String>();

            // Compute the subgraph, including the top node in the subgraph ...
            SubgraphQuery query = SubgraphQuery.create(getExecutionContext(), entities, actualFromLocation.getUuid(), fromPath, 0);
            try {
                // Walk through the original nodes, creating new ChildEntity object (i.e., copy) for each original ...
                List<ChildEntity> originalNodes = query.getNodes(true, true);
                Iterator<ChildEntity> originalIter = originalNodes.iterator();

                // Start with the original (top-level) node first, since we need to add it to the list of children ...
                if (originalIter.hasNext()) {
                    ChildEntity original = originalIter.next();

                    // Create a new UUID for the copy ...
                    String copyUuid = UUID.randomUUID().toString();
                    originalToNewUuid.put(original.getId().getChildUuidString(), copyUuid);

                    // Now add the new copy of the original ...
                    Name childName = fromPath.getLastSegment().getName();
                    actualToLocation = addNewChild(actualNewParent, copyUuid, childName);
                }

                // Now create copies of all children in the subgraph, assigning new UUIDs to each new child ...
                while (originalIter.hasNext()) {
                    ChildEntity original = originalIter.next();
                    String newParentUuidOfCopy = originalToNewUuid.get(original.getId().getParentUuidString());
                    assert newParentUuidOfCopy != null;

                    // Create a new UUID for the copy ...
                    String copyUuid = UUID.randomUUID().toString();
                    originalToNewUuid.put(original.getId().getChildUuidString(), copyUuid);

                    // Create the copy ...
                    ChildEntity copy = new ChildEntity(new ChildId(newParentUuidOfCopy, copyUuid), original.getIndexInParent(),
                                                       original.getChildNamespace(), original.getChildName(),
                                                       original.getSameNameSiblingIndex());
                    entities.persist(copy);
                }
                entities.flush();

                // Now create copies of all the intra-subgraph references, replacing the UUIDs on both ends ...
                Set<String> newNodesWithReferenceProperties = new HashSet<String>();
                for (ReferenceEntity reference : query.getInternalReferences()) {
                    String newFromUuid = originalToNewUuid.get(reference.getId().getFromUuidString());
                    assert newFromUuid != null;
                    String newToUuid = originalToNewUuid.get(reference.getId().getToUuidString());
                    assert newToUuid != null;
                    ReferenceEntity copy = new ReferenceEntity(new ReferenceId(newFromUuid, newToUuid));
                    entities.persist(copy);
                    newNodesWithReferenceProperties.add(newFromUuid);
                }

                // Now create copies of all the references owned by the subgraph but pointing to non-subgraph nodes,
                // so we only replaced the 'from' UUID ...
                for (ReferenceEntity reference : query.getOutwardReferences()) {
                    String oldToUuid = reference.getId().getToUuidString();
                    String newFromUuid = originalToNewUuid.get(reference.getId().getFromUuidString());
                    assert newFromUuid != null;
                    ReferenceEntity copy = new ReferenceEntity(new ReferenceId(newFromUuid, oldToUuid));
                    entities.persist(copy);
                    newNodesWithReferenceProperties.add(newFromUuid);
                }

                // Now process the properties, creating a copy (note references are not changed) ...
                for (PropertiesEntity original : query.getProperties(true, true)) {
                    // Find the UUID of the copy ...
                    String copyUuid = originalToNewUuid.get(original.getId().getUuidString());
                    assert copyUuid != null;

                    // Create the copy ...
                    boolean compressed = original.isCompressed();
                    byte[] originalData = original.getData();
                    PropertiesEntity copy = new PropertiesEntity(new NodeId(copyUuid));
                    copy.setCompressed(compressed);
                    if (newNodesWithReferenceProperties.contains(copyUuid)) {

                        // This node has internal or outward references that must be adjusted ...
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        OutputStream os = compressed ? new GZIPOutputStream(baos) : baos;
                        ObjectOutputStream oos = new ObjectOutputStream(os);
                        ByteArrayInputStream bais = new ByteArrayInputStream(originalData);
                        InputStream is = compressed ? new GZIPInputStream(bais) : bais;
                        ObjectInputStream ois = new ObjectInputStream(is);
                        try {
                            serializer.adjustReferenceProperties(ois, oos, originalToNewUuid);
                        } finally {
                            try {
                                ois.close();
                            } finally {
                                oos.close();
                            }
                        }
                        copy.setData(baos.toByteArray());
                    } else {
                        // No references to adjust, so just copy the original data ...
                        copy.setData(originalData);
                    }
                    copy.setPropertyCount(original.getPropertyCount());
                    copy.setReferentialIntegrityEnforced(original.isReferentialIntegrityEnforced());
                    entities.persist(copy);
                }
                entities.flush();

            } finally {
                // Close and release the temporary data used for this operation ...
                query.close();
            }

        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        request.setActualLocations(actualFromLocation, actualToLocation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        logger.trace(request.toString());
        Location actualLocation = null;
        try {
            Location location = request.at();
            ActualLocation actual = getActualLocation(location);
            actualLocation = actual.location;
            Path path = actualLocation.getPath();

            // Compute the subgraph, including the top node in the subgraph ...
            SubgraphQuery query = SubgraphQuery.create(getExecutionContext(), entities, actualLocation.getUuid(), path, 0);
            try {
                ChildEntity deleted = query.getNode();
                String parentUuidString = deleted.getId().getParentUuidString();
                String childName = deleted.getChildName();
                long nsId = deleted.getChildNamespace().getId();
                int indexInParent = deleted.getIndexInParent();

                // Get the locations of all deleted nodes, which will be required by events ...
                List<Location> deletedLocations = query.getNodeLocations(true, true);

                // Now delete the subgraph ...
                query.deleteSubgraph(true);

                // Verify referential integrity: that none of the deleted nodes are referenced by nodes not being deleted.
                List<ReferenceEntity> invalidReferences = query.getInwardReferences();
                if (invalidReferences.size() > 0) {
                    // Some of the references that remain will be invalid, since they point to nodes that
                    // have just been deleted. Build up the information necessary to produce a useful exception ...
                    ValueFactory<Reference> refFactory = getExecutionContext().getValueFactories().getReferenceFactory();
                    Map<Location, List<Reference>> invalidRefs = new HashMap<Location, List<Reference>>();
                    for (ReferenceEntity entity : invalidReferences) {
                        UUID fromUuid = UUID.fromString(entity.getId().getFromUuidString());
                        ActualLocation actualFromLocation = getActualLocation(new Location(fromUuid));
                        Location fromLocation = actualFromLocation.location;
                        List<Reference> refs = invalidRefs.get(fromLocation);
                        if (refs == null) {
                            refs = new ArrayList<Reference>();
                            invalidRefs.put(fromLocation, refs);
                        }
                        UUID toUuid = UUID.fromString(entity.getId().getToUuidString());
                        refs.add(refFactory.create(toUuid));
                    }
                    String msg = JpaConnectorI18n.unableToDeleteBecauseOfReferences.text();
                    throw new ReferentialIntegrityException(invalidRefs, msg);
                }

                // And adjust the SNS index and indexes ...
                ChildEntity.adjustSnsIndexesAndIndexesAfterRemoving(entities, parentUuidString, childName, nsId, indexInParent);
                entities.flush();

                // Remove from the cache of children locations all entries for deleted nodes ...
                cache.removeBranch(deletedLocations);
            } finally {
                // Close and release the temporary data used for this operation ...
                query.close();
            }

        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        request.setActualLocationOfNode(actualLocation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.MoveBranchRequest)
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
            Path oldPath = actualOldLocation.getPath();

            // It's not possible to move the root node
            if (oldPath.isRoot()) {
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

                    // Flush the entities to the database ...
                    entities.flush();

                    // Determine the new location ...
                    Path newParentPath = actualIntoLocation.location.getPath();
                    Name childName = oldPath.getLastSegment().getName();
                    Path newPath = pathFactory.create(newParentPath, childName, nextSnsIndex);
                    actualNewLocation = actualOldLocation.with(newPath);

                    // And adjust the SNS index and indexes ...
                    ChildEntity.adjustSnsIndexesAndIndexesAfterRemoving(entities,
                                                                        oldParentUuid,
                                                                        childLocalName,
                                                                        ns.getId(),
                                                                        oldIndex);

                    // Update the cache ...
                    cache.moveNode(actualOldLocation, oldIndex, actualNewLocation);

                }

            }

        } catch (Throwable e) { // Includes PathNotFoundException
            request.setError(e);
            return;
        }
        request.setActualLocations(actualOldLocation, actualNewLocation);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#close()
     */
    @Override
    public void close() {
        // Verify that the references are valid so far ...
        verifyReferences();

        // Now commit the transaction ...
        EntityTransaction txn = entities.getTransaction();
        if (txn != null) txn.commit();
        super.close();
    }

    /**
     * {@link ReferenceEntity Reference entities} are added and removed in the appropriate <code>process(...)</code> methods.
     * However, this method is typically called in {@link BasicRequestProcessor#close()} and performs the following steps:
     * <ol>
     * <li>Remove all references that have a "from" node that is under the versions branch.</li>
     * <li>Verify that all remaining references have a valid and existing "to" node</li>
     * </ol>
     */
    protected void verifyReferences() {
        if (!enforceReferentialIntegrity) return;
        if (referencesChanged) {

            // Remove all references that have a "from" node that doesn't support referential integrity ...
            ReferenceEntity.deleteUnenforcedReferences(entities);

            // Verify that all references are resolved to existing nodes ...
            int numUnresolved = ReferenceEntity.countAllReferencesResolved(entities);
            if (numUnresolved != 0) {
                List<ReferenceEntity> references = ReferenceEntity.verifyAllReferencesResolved(entities);
                ValueFactory<Reference> refFactory = getExecutionContext().getValueFactories().getReferenceFactory();
                Map<Location, List<Reference>> invalidRefs = new HashMap<Location, List<Reference>>();
                for (ReferenceEntity entity : references) {
                    UUID fromUuid = UUID.fromString(entity.getId().getFromUuidString());
                    Location location = new Location(fromUuid);
                    location = getActualLocation(location).location;
                    List<Reference> refs = invalidRefs.get(location);
                    if (refs == null) {
                        refs = new ArrayList<Reference>();
                        invalidRefs.put(location, refs);
                    }
                    UUID toUuid = UUID.fromString(entity.getId().getToUuidString());
                    refs.add(refFactory.create(toUuid));
                }
                String msg = JpaConnectorI18n.invalidReferences.text(getSourceName());
                throw new ReferentialIntegrityException(invalidRefs, msg);
            }

            referencesChanged = false;
        }
    }

    protected String createProperties( String uuidString,
                                       Collection<Property> properties ) throws IOException {
        assert uuidString != null;

        // Create the PropertiesEntity ...
        NodeId nodeId = new NodeId(uuidString);
        PropertiesEntity props = new PropertiesEntity(nodeId);

        // If there are properties ...
        boolean processProperties = true;
        if (properties.isEmpty()) processProperties = false;
        else if (properties.size() == 1 && properties.iterator().next().getName().equals(JcrLexicon.NAME)) processProperties = false;

        if (processProperties) {
            References refs = enforceReferentialIntegrity ? new References() : null;
            LargeValueSerializer largeValues = new LargeValueSerializer(props);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream os = compressData ? new GZIPOutputStream(baos) : baos;
            ObjectOutputStream oos = new ObjectOutputStream(os);
            int numProperties = properties.size();
            try {
                Serializer.ReferenceValues refValues = refs != null ? refs : Serializer.NO_REFERENCES_VALUES;
                serializer.serializeProperties(oos, numProperties, properties, largeValues, refValues);
            } finally {
                oos.close();
            }

            props.setData(baos.toByteArray());
            props.setPropertyCount(numProperties);

            // Record the changes to the references ...
            if (refs != null && refs.hasWritten()) {
                for (Reference reference : refs.getWritten()) {
                    String toUuid = resolveToUuid(reference);
                    if (toUuid != null) {
                        ReferenceId id = new ReferenceId(uuidString, toUuid);
                        ReferenceEntity refEntity = new ReferenceEntity(id);
                        entities.persist(refEntity);
                        referencesChanged = true;
                    }
                }
            }
        } else {
            props.setData(null);
            props.setPropertyCount(0);
        }
        props.setCompressed(compressData);
        props.setReferentialIntegrityEnforced(true);

        entities.persist(props);

        // References will be persisted in the commit ...
        return uuidString;
    }

    /**
     * Attempt to resolve the reference.
     * 
     * @param reference the reference
     * @return the UUID of the node to which the reference points, or null if the reference could not be resolved
     */
    protected String resolveToUuid( Reference reference ) {
        // See if the reference is by UUID ...
        try {
            UUID uuid = uuidFactory.create(reference);
            ActualLocation actualLocation = getActualLocation(new Location(uuid));
            return actualLocation.uuid;
        } catch (ValueFormatException e) {
            // Unknown kind of reference, which we don't track
        } catch (PathNotFoundException e) {
            // Unable to resolve reference ...
        }
        // Unable to resolve reference ...
        return null;
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

        Path path = original.getPath();
        if (path != null) {
            // See if the location is already in the cache ...
            Location cached = cache.getLocationFor(path);
            if (cached != null) {
                return new ActualLocation(cached, cached.getUuid().toString(), null);
            }
        }

        // If the original location has a UUID, then use that to find the child entity that represents the location ...
        if (uuidString != null) {
            // The original has a UUID, so use that to find the child entity.
            // Then walk up the ancestors and build the path.
            String nodeUuidString = uuidString;
            LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
            ChildEntity entity = null;
            ChildEntity originalEntity = null;
            while (uuidString != null && !uuidString.equals(this.rootNodeUuidString)) {
                Query query = entities.createNamedQuery("ChildEntity.findByChildUuid");
                query.setParameter("childUuidString", uuidString);
                try {
                    // Find the parent of the UUID ...
                    entity = (ChildEntity)query.getSingleResult();
                    if (originalEntity == null) originalEntity = entity;
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
            Location newLocation = new Location(fullPath, uuidProperty);
            cache.addNewNode(newLocation);
            return new ActualLocation(newLocation, nodeUuidString, originalEntity);
        }

        // There is no UUID, so look for a path ...
        if (path == null) {
            String propName = DnaLexicon.UUID.getString(getExecutionContext().getNamespaceRegistry());
            String msg = JpaConnectorI18n.locationShouldHavePathAndOrProperty.text(getSourceName(), propName);
            throw new PathNotFoundException(original, pathFactory.createRootPath(), msg);
        }

        // Walk the child entities, starting at the root, down the to the path ...
        if (path.isRoot()) {
            Location newLocation = original.with(rootNodeUuid);
            cache.addNewNode(newLocation);
            return new ActualLocation(newLocation, rootNodeUuidString, null);
        }
        // See if the parent location is known in the cache ...
        Location cachedParent = cache.getLocationFor(path.getParent());
        if (cachedParent != null) {
            // We know the UUID of the parent, so we can find the child a little faster ...
            ChildEntity child = findByPathSegment(cachedParent.getUuid().toString(), path.getLastSegment());
            uuidString = child.getId().getChildUuidString();
            Location newLocation = original.with(UUID.fromString(uuidString));
            cache.addNewNode(newLocation);
            return new ActualLocation(newLocation, uuidString, child);
        }

        // We couldn't find the parent, so we need to search by path ...
        String parentUuid = this.rootNodeUuidString;
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
        Location newLocation = original.with(UUID.fromString(uuidString));
        cache.addNewNode(newLocation);
        return new ActualLocation(newLocation, uuidString, child);
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
        NamespaceEntity ns = namespaces.get(nsUri, false);
        if (ns == null) {
            // The namespace can't be found, then certainly the node won't be found ...
            return null;
        }
        int snsIndex = pathSegment.hasIndex() ? pathSegment.getIndex() : 1;
        Query query = entities.createNamedQuery("ChildEntity.findByPathSegment");
        query.setParameter("parentUuidString", parentUuid);
        query.setParameter("ns", ns.getId());
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

    protected class LargeValueSerializer implements LargeValues {
        private final PropertiesEntity properties;
        private Set<String> written;

        public LargeValueSerializer( PropertiesEntity entity ) {
            this.properties = entity;
            this.written = null;
        }

        public LargeValueSerializer( PropertiesEntity entity,
                                     Set<String> written ) {
            this.properties = entity;
            this.written = written;
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
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#read(org.jboss.dna.graph.property.ValueFactories,
         *      byte[], long)
         */
        public Object read( ValueFactories valueFactories,
                            byte[] hash,
                            long length ) throws IOException {
            String hashStr = StringUtil.getHexString(hash);
            // Find the large value ...
            LargeValueId largeValueId = new LargeValueId(hashStr);
            LargeValueEntity entity = entities.find(LargeValueEntity.class, largeValueId);
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
            final LargeValueId id = new LargeValueId(hashStr);
            for (LargeValueId existing : properties.getLargeValues()) {
                if (existing.equals(id)) {
                    // Already associated with this properties entity
                    return;
                }
            }
            LargeValueEntity entity = entities.find(LargeValueEntity.class, id);
            if (entity == null) {
                // We have to create the large value entity ...
                entity = new LargeValueEntity();
                entity.setCompressed(true);
                entity.setId(id);
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
                entities.persist(entity);
            }
            // Now associate the large value with the properties entity ...
            assert id.getHash() != null;
            properties.getLargeValues().add(id);
        }

    }

    protected class RecordingLargeValues implements LargeValues {
        protected final Collection<String> readKeys = new HashSet<String>();
        protected final Collection<String> writtenKeys = new HashSet<String>();
        protected final LargeValues delegate;

        RecordingLargeValues( LargeValues delegate ) {
            assert delegate != null;
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#getMinimumSize()
         */
        public long getMinimumSize() {
            return delegate.getMinimumSize();
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
            String key = StringUtil.getHexString(hash);
            readKeys.add(key);
            return delegate.read(valueFactories, hash, length);
        }

        public void write( byte[] hash,
                           long length,
                           PropertyType type,
                           Object value ) throws IOException {
            String key = StringUtil.getHexString(hash);
            writtenKeys.add(key);
            delegate.write(hash, length, type, value);
        }
    }

    protected class SkippedLargeValues implements LargeValues {
        protected Collection<String> skippedKeys = new HashSet<String>();
        protected final LargeValues delegate;

        SkippedLargeValues( LargeValues delegate ) {
            assert delegate != null;
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.LargeValues#getMinimumSize()
         */
        public long getMinimumSize() {
            return delegate.getMinimumSize();
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

    protected class References implements Serializer.ReferenceValues {
        private Set<Reference> read;
        private Set<Reference> removed;
        private Set<Reference> written;

        protected References() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.ReferenceValues#read(org.jboss.dna.graph.property.Reference)
         */
        public void read( Reference reference ) {
            if (read == null) read = new HashSet<Reference>();
            read.add(reference);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.ReferenceValues#remove(org.jboss.dna.graph.property.Reference)
         */
        public void remove( Reference reference ) {
            if (removed == null) removed = new HashSet<Reference>();
            removed.add(reference);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.connector.store.jpa.util.Serializer.ReferenceValues#write(org.jboss.dna.graph.property.Reference)
         */
        public void write( Reference reference ) {
            if (written == null) written = new HashSet<Reference>();
            written.add(reference);
        }

        public boolean hasRead() {
            return read != null;
        }

        public boolean hasRemoved() {
            return removed != null;
        }

        public boolean hasWritten() {
            return written != null;
        }

        /**
         * @return read
         */
        public Set<Reference> getRead() {
            if (read != null) return read;
            return Collections.emptySet();
        }

        /**
         * @return removed
         */
        public Set<Reference> getRemoved() {
            if (removed != null) return removed;
            return Collections.emptySet();
        }

        /**
         * @return written
         */
        public Set<Reference> getWritten() {
            if (written != null) return written;
            return Collections.emptySet();
        }
    }
}
