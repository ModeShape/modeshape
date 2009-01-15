/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.store.jpa.model.basic;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.IsCollectionContaining.hasItems;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.hibernate.ejb.Ejb3Configuration;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.common.util.SecureHash;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.connector.store.jpa.model.basic.BasicModel;
import org.jboss.dna.connector.store.jpa.model.basic.ChildEntity;
import org.jboss.dna.connector.store.jpa.model.basic.ChildId;
import org.jboss.dna.connector.store.jpa.model.basic.LargeValueEntity;
import org.jboss.dna.connector.store.jpa.model.basic.LargeValueId;
import org.jboss.dna.connector.store.jpa.model.basic.NodeId;
import org.jboss.dna.connector.store.jpa.model.basic.PropertiesEntity;
import org.jboss.dna.connector.store.jpa.model.basic.ReferenceEntity;
import org.jboss.dna.connector.store.jpa.model.basic.ReferenceId;
import org.jboss.dna.connector.store.jpa.model.basic.SubgraphQuery;
import org.jboss.dna.connector.store.jpa.model.common.NamespaceEntity;
import org.jboss.dna.connector.store.jpa.util.Namespaces;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PropertyType;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class SubgraphQueryTest {

    private EntityManagerFactory factory;
    private EntityManager manager;
    private BasicModel model;
    private ExecutionContext context;
    private Map<Path, UUID> uuidByPath;
    private Namespaces namespaces;
    private List<Location> locations;
    private String[] validLargeValues;
    private SubgraphQuery query;

    @BeforeClass
    public static void beforeAll() throws Exception {
    }

    @Before
    public void beforeEach() throws Exception {
        model = new BasicModel();
        context = new ExecutionContext();

        // Load in the large value ...
        validLargeValues = new String[] {IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum1.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum2.txt")),
            IoUtil.read(getClass().getClassLoader().getResourceAsStream("LoremIpsum3.txt"))};

        // Connect to the database ...
        Ejb3Configuration configurator = new Ejb3Configuration();
        model.configure(configurator);
        configurator.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        configurator.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        configurator.setProperty("hibernate.connection.username", "sa");
        configurator.setProperty("hibernate.connection.password", "");
        configurator.setProperty("hibernate.connection.url", "jdbc:hsqldb:.");
        configurator.setProperty("hibernate.show_sql", "false");
        configurator.setProperty("hibernate.format_sql", "true");
        configurator.setProperty("hibernate.use_sql_comments", "true");
        configurator.setProperty("hibernate.hbm2ddl.auto", "create");
        factory = configurator.buildEntityManagerFactory();
        manager = factory.createEntityManager();
        namespaces = new Namespaces(manager);

        manager.getTransaction().begin();

        // Now populate a graph of nodes ...
        uuidByPath = new HashMap<Path, UUID>();
        uuidByPath.put(path("/"), UUID.randomUUID());
        create("/a");
        create("/a/a1");
        create("/a/a1/a1");
        create("/a/a1/a2");
        create("/a/a1/a3");
        create("/a/a2");
        create("/a/a2/a1");
        create("/a/a2/a1/a1");
        create("/a/a2/a1/a1/a1");
        create("/a/a2/a1/a1/a2");
        create("/a/a2/a1/a2");
        create("/a/a2/a2");
        create("/a/a2/a3");
        create("/a/a2/a4");
        setLargeValue("/a/a1", "prop1", validLargeValues[0]);
        setLargeValue("/a/a1", "prop1", validLargeValues[1]); // the only node that uses #1
        setLargeValue("/a/a2", "prop1", validLargeValues[0]);
        setLargeValue("/a/a2", "prop2", validLargeValues[2]);
        setLargeValue("/a/a2/a1", "prop2", validLargeValues[0]);
        setLargeValue("/a/a2/a1", "prop3", validLargeValues[2]);
        manager.getTransaction().commit();
        manager.getTransaction().begin();
    }

    @After
    public void afterEach() throws Exception {
        if (query != null) {
            try {
                query.close();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        try {
            manager.close();
        } finally {
            factory.close();
        }
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected void create( String pathStr ) {
        Path path = path(pathStr);
        if (uuidByPath.containsKey(path)) return;
        if (path.isRoot()) return;
        Path parent = path.getParent();
        // Look up the parent ...
        UUID parentUuid = uuidByPath.get(parent);
        assert parentUuid != null;
        // Calculate the child index by walking the existing nodes ...
        int numChildren = 0;
        for (Path existing : uuidByPath.keySet()) {
            if (parent.equals(existing.getParent())) {
                ++numChildren;
            }
        }

        // Create the child entity ...
        Name childName = path.getLastSegment().getName();
        int snsIndex = path.getLastSegment().getIndex();
        NamespaceEntity namespace = namespaces.get(childName.getNamespaceUri(), true);
        UUID childUuid = UUID.randomUUID();
        ChildId id = new ChildId(parentUuid.toString(), childUuid.toString());
        ChildEntity entity = new ChildEntity(id, ++numChildren, namespace, childName.getLocalName(), snsIndex);
        manager.persist(entity);

        // Create the properties ...
        NodeId nodeId = new NodeId(childUuid.toString());
        PropertiesEntity props = new PropertiesEntity(nodeId);
        props.setData("bogus data".getBytes());
        props.setPropertyCount(1);
        props.setCompressed(false);
        manager.persist(props);

        uuidByPath.put(path, childUuid);
    }

    protected ReferenceEntity createReferenceBetween( String fromPathStr,
                                                      String toPathStr ) {
        Path fromPath = path(fromPathStr);
        Path toPath = path(toPathStr);

        // Look up the UUIDs ...
        UUID fromUuid = uuidByPath.get(fromPath);
        UUID toUuid = uuidByPath.get(toPath);
        assert fromUuid != null;
        assert toUuid != null;

        // Now create a reference entity ...
        ReferenceEntity entity = new ReferenceEntity(new ReferenceId(fromUuid.toString(), toUuid.toString()));
        manager.persist(entity);
        return entity;
    }

    protected UUID uuidForPath( String pathStr ) {
        Path path = path(pathStr);
        return uuidByPath.get(path);
    }

    protected void setLargeValue( String pathStr,
                                  String propertyName,
                                  String largeValue ) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Path path = path(pathStr);
        UUID nodeUuid = uuidByPath.get(path);
        assertThat(nodeUuid, is(notNullValue()));

        // Find or create the large value object ...
        LargeValueId id = largeValueId(largeValue);
        LargeValueEntity entity = manager.find(LargeValueEntity.class, id);
        if (entity == null) {
            entity = new LargeValueEntity();
            entity.setId(id);
            entity.setLength(largeValue.length());
            entity.setCompressed(false);
            entity.setData(largeValue.getBytes());
            entity.setType(PropertyType.STRING);
            manager.persist(entity);
        }

        // Load the PropertiesEntity ...
        NodeId nodeId = new NodeId(nodeUuid.toString());
        PropertiesEntity props = manager.find(PropertiesEntity.class, nodeId);
        assertThat(props, is(notNullValue()));

        // Add the large value ...
        props.getLargeValues().add(id);
    }

    protected LargeValueId largeValueId( String value ) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return new LargeValueId(StringUtil.getHexString(SecureHash.getHash(SecureHash.Algorithm.SHA_1, value.getBytes())));
    }

    protected PropertiesEntity getProperties( String pathStr ) {
        Path path = path(pathStr);
        UUID nodeUuid = uuidByPath.get(path);
        assertThat(nodeUuid, is(notNullValue()));

        NodeId nodeId = new NodeId(nodeUuid.toString());
        return manager.find(PropertiesEntity.class, nodeId);
    }

    protected void verifyNextLocationIs( String path ) {
        Path pathObj = path(path);
        UUID uuid = uuidByPath.get(pathObj);
        Location next = locations.remove(0);
        assertThat(next, is(notNullValue()));
        assertThat(next.getPath(), is(pathObj));
        assertThat(next.getUuid(), is(uuid));
    }

    protected void verifyNoMoreLocations() {
        assertThat(locations.isEmpty(), is(true));
    }

    @SuppressWarnings( "unchecked" )
    protected void verifyNodesHaveLargeValues( String... paths ) {
        if (paths == null || paths.length == 0) return;
        // Build the set of UUIDs for the nodes that should have large values ...
        String[] expectedNodeUuids = new String[paths.length];
        for (int i = 0; i != paths.length; ++i) {
            String pathStr = paths[i];
            expectedNodeUuids[i] = uuidForPath(pathStr).toString();
        }
        // Load the PropertiesEntity for the nodes that have large properties ...
        Query queryProps = manager.createQuery("select prop from PropertiesEntity as prop where size(prop.largeValues) > 0");
        Set<String> actualNodeUuids = new HashSet<String>();
        List<PropertiesEntity> propsWithLargeValues = queryProps.getResultList();
        for (PropertiesEntity entity : propsWithLargeValues) {
            String uuidStr = entity.getId().getUuidString();
            actualNodeUuids.add(uuidStr);
        }
        assertThat(actualNodeUuids, hasItems(expectedNodeUuids));
    }

    @Test
    public void shouldFindLargeValueContentFromFile() {
        for (int i = 0; i != validLargeValues.length; ++i) {
            assertThat(validLargeValues[i].startsWith((i + 1) + ". Lorem ipsum dolor sit amet"), is(true));
        }
    }

    @Test
    public void shouldPerformSubgraphQueryOfNodeWithChildrenAndNoGrandchildren() {
        Path path = path("/a/a1");
        UUID uuid = uuidByPath.get(path);
        query = SubgraphQuery.create(context, manager, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNoMoreLocations();
        query.close();
    }

    @Test
    public void shouldPerformSubgraphQueryOfNodeWithChildrenAndGrandchildren() {
        Path path = path("/a/a2");
        UUID uuid = uuidByPath.get(path);
        query = SubgraphQuery.create(context, manager, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a/a2");
        verifyNextLocationIs("/a/a2/a1");
        verifyNextLocationIs("/a/a2/a2");
        verifyNextLocationIs("/a/a2/a3");
        verifyNextLocationIs("/a/a2/a4");
        verifyNextLocationIs("/a/a2/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a2");
        verifyNextLocationIs("/a/a2/a1/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a1/a2");
        verifyNoMoreLocations();
        query.close();
    }

    @Test
    public void shouldPerformSubgraphQueryOfNodeWithChildrenAndGrandchildrenAndGreatGranchildren() {
        Path path = path("/a");
        UUID uuid = uuidByPath.get(path);
        query = SubgraphQuery.create(context, manager, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a");
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a2");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNextLocationIs("/a/a2/a1");
        verifyNextLocationIs("/a/a2/a2");
        verifyNextLocationIs("/a/a2/a3");
        verifyNextLocationIs("/a/a2/a4");
        verifyNextLocationIs("/a/a2/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a2");
        verifyNextLocationIs("/a/a2/a1/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a1/a2");
        verifyNoMoreLocations();
        query.close();
    }

    @Test
    public void shouldPerformMaxDepthSubgraphQueryOfNodeWithChildrenAndGrandchildrenAndGreatGranchildren() {
        Path path = path("/a");
        UUID uuid = uuidByPath.get(path);
        query = SubgraphQuery.create(context, manager, uuid, path, 4);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a");
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a2");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNextLocationIs("/a/a2/a1");
        verifyNextLocationIs("/a/a2/a2");
        verifyNextLocationIs("/a/a2/a3");
        verifyNextLocationIs("/a/a2/a4");
        verifyNextLocationIs("/a/a2/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a2");
        verifyNextLocationIs("/a/a2/a1/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a1/a2");
        verifyNoMoreLocations();

        locations = query.getNodeLocations(true, false);
        verifyNextLocationIs("/a");
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a2");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNextLocationIs("/a/a2/a1");
        verifyNextLocationIs("/a/a2/a2");
        verifyNextLocationIs("/a/a2/a3");
        verifyNextLocationIs("/a/a2/a4");
        verifyNextLocationIs("/a/a2/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a2");
        verifyNoMoreLocations();
        query.close();

        query = SubgraphQuery.create(context, manager, uuid, path, 2);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a");
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a2");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNextLocationIs("/a/a2/a1");
        verifyNextLocationIs("/a/a2/a2");
        verifyNextLocationIs("/a/a2/a3");
        verifyNextLocationIs("/a/a2/a4");
        verifyNoMoreLocations();

        locations = query.getNodeLocations(true, false);
        verifyNextLocationIs("/a");
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a2");
        verifyNoMoreLocations();

        query.close();

        query = SubgraphQuery.create(context, manager, uuid, path, 3);
        locations = query.getNodeLocations(true, false);
        verifyNextLocationIs("/a");
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a2");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNextLocationIs("/a/a2/a1");
        verifyNextLocationIs("/a/a2/a2");
        verifyNextLocationIs("/a/a2/a3");
        verifyNextLocationIs("/a/a2/a4");
        verifyNoMoreLocations();

        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a");
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a2");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNextLocationIs("/a/a2/a1");
        verifyNextLocationIs("/a/a2/a2");
        verifyNextLocationIs("/a/a2/a3");
        verifyNextLocationIs("/a/a2/a4");
        verifyNextLocationIs("/a/a2/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a2");
        verifyNoMoreLocations();
        query.close();
    }

    @Test
    public void shouldDeleteSubgraph() throws Exception {
        // Verify that all the nodes with large values do indeed have them ...
        verifyNodesHaveLargeValues("/a/a1", "/a/a2", "/a/a2/a1");

        // Count the number of objects ...
        assertThat((Long)manager.createQuery("select count(*) from LargeValueEntity").getSingleResult(), is(3L));
        assertThat((Long)manager.createQuery("select count(*) from PropertiesEntity").getSingleResult(), is(14L));
        assertThat((Long)manager.createQuery("select count(*) from ChildEntity").getSingleResult(), is(14L));

        // Delete "/a/a1". Note that "/a/a1" has a large value that is shared by "/a/a2", but it's also the only
        // user of large value #1.
        Path path = path("/a/a1");
        UUID uuid = uuidByPath.get(path);

        query = SubgraphQuery.create(context, manager, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNoMoreLocations();
        query.deleteSubgraph(true);
        assertThat(query.getInwardReferences().isEmpty(), is(true));
        query.close();

        // Commit the transaction, and start another ...
        manager.getTransaction().commit();
        manager.getTransaction().begin();
        manager.flush();

        // Count the number of objects ...
        assertThat((Long)manager.createQuery("select count(*) from LargeValueEntity").getSingleResult(), is(2L));
        assertThat((Long)manager.createQuery("select count(*) from PropertiesEntity").getSingleResult(), is(10L));
        assertThat((Long)manager.createQuery("select count(*) from ChildEntity").getSingleResult(), is(10L));

        // Verify the graph structure is correct ...
        path = path("/a");
        uuid = uuidByPath.get(path);
        query = SubgraphQuery.create(context, manager, uuid, path, 4);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a");
        verifyNextLocationIs("/a/a2");
        verifyNextLocationIs("/a/a2/a1");
        verifyNextLocationIs("/a/a2/a2");
        verifyNextLocationIs("/a/a2/a3");
        verifyNextLocationIs("/a/a2/a4");
        verifyNextLocationIs("/a/a2/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a2");
        verifyNextLocationIs("/a/a2/a1/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a1/a2");
        verifyNoMoreLocations();

        locations = query.getNodeLocations(true, false);
        verifyNextLocationIs("/a");
        verifyNextLocationIs("/a/a2");
        verifyNextLocationIs("/a/a2/a1");
        verifyNextLocationIs("/a/a2/a2");
        verifyNextLocationIs("/a/a2/a3");
        verifyNextLocationIs("/a/a2/a4");
        verifyNextLocationIs("/a/a2/a1/a1");
        verifyNextLocationIs("/a/a2/a1/a2");
        verifyNoMoreLocations();
        query.close();

        // Verify that all the nodes with large values do indeed have them ...
        verifyNodesHaveLargeValues("/a/a2", "/a/a2/a1"); // "/a/a1" was deleted

        // Now, load the one node remaining with
    }

    @Test
    public void shouldNotDeleteSubgraphThatHasNodesReferencedByOtherNodesNotBeingDeleted() throws Exception {
        // Verify that all the nodes with large values do indeed have them ...
        verifyNodesHaveLargeValues("/a/a1", "/a/a2", "/a/a2/a1");

        // Count the number of objects ...
        assertThat((Long)manager.createQuery("select count(*) from LargeValueEntity").getSingleResult(), is(3L));
        assertThat((Long)manager.createQuery("select count(*) from PropertiesEntity").getSingleResult(), is(14L));
        assertThat((Long)manager.createQuery("select count(*) from ChildEntity").getSingleResult(), is(14L));

        // Create references from the "/a/a2" (not being deleted) branch, to the branch being deleted...
        List<ReferenceEntity> expectedInvalidRefs = new ArrayList<ReferenceEntity>();
        expectedInvalidRefs.add(createReferenceBetween("/a/a2", "/a/a1"));
        expectedInvalidRefs.add(createReferenceBetween("/a/a2/a1", "/a/a1/a1"));
        expectedInvalidRefs.add(createReferenceBetween("/a/a2/a2", "/a/a1/a2"));

        // Create references between nodes in the branch being deleted (these shouldn't matter) ...
        createReferenceBetween("/a/a1", "/a/a1/a1");
        createReferenceBetween("/a/a1/a2", "/a/a1/a3");

        // Delete "/a/a1". Note that "/a/a1" has a large value that is shared by "/a/a2", but it's also the only
        // user of large value #1.
        Path path = path("/a/a1");
        UUID uuid = uuidByPath.get(path);

        query = SubgraphQuery.create(context, manager, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNoMoreLocations();
        query.deleteSubgraph(true);

        // Now there should be invalid references ...
        List<ReferenceEntity> invalidReferences = query.getInwardReferences();
        assertThat(invalidReferences.size(), is(3));
        invalidReferences.removeAll(invalidReferences);
        assertThat(invalidReferences.size(), is(0));
        query.close();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldDeleteSubgraphThatHasInternalReferences() throws Exception {
        // Verify that all the nodes with large values do indeed have them ...
        verifyNodesHaveLargeValues("/a/a1", "/a/a2", "/a/a2/a1");

        // Count the number of objects ...
        assertThat((Long)manager.createQuery("select count(*) from LargeValueEntity").getSingleResult(), is(3L));
        assertThat((Long)manager.createQuery("select count(*) from PropertiesEntity").getSingleResult(), is(14L));
        assertThat((Long)manager.createQuery("select count(*) from ChildEntity").getSingleResult(), is(14L));

        // Create references from the nodes that aren't being deleted (these won't matter, but will remain)...
        List<ReferenceEntity> expectedValidRefs = new ArrayList<ReferenceEntity>();
        expectedValidRefs.add(createReferenceBetween("/a/a2", "/a/a2/a1"));

        // Create references between nodes in the branch being deleted (these shouldn't matter) ...
        createReferenceBetween("/a/a1", "/a/a1/a1");
        createReferenceBetween("/a/a1/a2", "/a/a1/a3");

        // Delete "/a/a1". Note that "/a/a1" has a large value that is shared by "/a/a2", but it's also the only
        // user of large value #1.
        Path path = path("/a/a1");
        UUID uuid = uuidByPath.get(path);

        query = SubgraphQuery.create(context, manager, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs("/a/a1");
        verifyNextLocationIs("/a/a1/a1");
        verifyNextLocationIs("/a/a1/a2");
        verifyNextLocationIs("/a/a1/a3");
        verifyNoMoreLocations();
        query.deleteSubgraph(true);

        // Now there should be invalid references ...
        List<ReferenceEntity> invalidReferences = query.getInwardReferences();
        assertThat(invalidReferences.size(), is(0));
        query.close();

        // There should be no references any more ...
        Query refQuery = manager.createQuery("select ref from ReferenceEntity as ref");
        List<ReferenceEntity> remainingReferences = refQuery.getResultList();
        assertThat(remainingReferences.size(), is(1));
        remainingReferences.removeAll(expectedValidRefs);
        assertThat(remainingReferences.size(), is(0));
    }

    @Test
    public void shouldGetVariousReferencesRelatedToSubgraph() throws Exception {
        // Verify that all the nodes with large values do indeed have them ...
        verifyNodesHaveLargeValues("/a/a1", "/a/a2", "/a/a2/a1");

        // Count the number of objects ...
        assertThat((Long)manager.createQuery("select count(*) from LargeValueEntity").getSingleResult(), is(3L));
        assertThat((Long)manager.createQuery("select count(*) from PropertiesEntity").getSingleResult(), is(14L));
        assertThat((Long)manager.createQuery("select count(*) from ChildEntity").getSingleResult(), is(14L));

        // Create references from the nodes that aren't even part of the subgraph ...
        List<ReferenceEntity> otherRefs = new ArrayList<ReferenceEntity>();
        otherRefs.add(createReferenceBetween("/a/a2", "/a/a2/a1"));
        otherRefs.add(createReferenceBetween("/a/a2/a1", "/a/a2/a2"));

        // Create references between nodes in the subgraph ...
        List<ReferenceEntity> internalRefs = new ArrayList<ReferenceEntity>();
        internalRefs.add(createReferenceBetween("/a/a1", "/a/a1/a1"));
        internalRefs.add(createReferenceBetween("/a/a1/a2", "/a/a1/a3"));

        // Create references from nodes outside of the subgraph to nodes inside of the subgraph ...
        List<ReferenceEntity> inwardRefs = new ArrayList<ReferenceEntity>();
        inwardRefs.add(createReferenceBetween("/a/a2", "/a/a1/a1"));
        inwardRefs.add(createReferenceBetween("/a/a2/a1", "/a/a1/a3"));

        // Create references from nodes inside of the subgraph to nodes outside of the subgraph ...
        List<ReferenceEntity> outwardRefs = new ArrayList<ReferenceEntity>();
        outwardRefs.add(createReferenceBetween("/a/a1", "/a/a2"));
        outwardRefs.add(createReferenceBetween("/a/a1/a1", "/a/a2/a1"));

        // Create the query ...
        Path path = path("/a/a1");
        UUID uuid = uuidByPath.get(path);
        query = SubgraphQuery.create(context, manager, uuid, path, Integer.MAX_VALUE);

        // Check the various kinds of references ...
        List<ReferenceEntity> actualInternal = query.getInternalReferences();
        List<ReferenceEntity> actualInward = query.getInwardReferences();
        List<ReferenceEntity> actualOutward = query.getOutwardReferences();

        assertThat(actualInternal.size(), is(internalRefs.size()));
        actualInternal.removeAll(internalRefs);
        assertThat(actualInternal.size(), is(0));

        assertThat(actualInward.size(), is(inwardRefs.size()));
        actualInward.removeAll(inwardRefs);
        assertThat(actualInward.size(), is(0));

        assertThat(actualOutward.size(), is(outwardRefs.size()));
        actualOutward.removeAll(outwardRefs);
        assertThat(actualOutward.size(), is(0));

        query.close();
    }

}
