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
package org.modeshape.connector.store.jpa.model.basic;

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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.StringUtil;
import org.modeshape.connector.store.jpa.model.common.NamespaceEntity;
import org.modeshape.connector.store.jpa.util.Namespaces;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;

/**
 * @author Randall Hauch
 */
public class SubgraphQueryTest {

    private EntityManagerFactory factory;
    private EntityManager manager;
    private BasicModel model;
    private ExecutionContext context;
    private Long workspaceId;
    private Map<Long, Map<Path, UUID>> uuidByPathByWorkspace;
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
        configurator.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:subgraphQueryTest");
        configurator.setProperty("hibernate.show_sql", "false");
        configurator.setProperty("hibernate.format_sql", "true");
        configurator.setProperty("hibernate.use_sql_comments", "true");
        configurator.setProperty("hibernate.hbm2ddl.auto", "create");
        factory = configurator.buildEntityManagerFactory();
        manager = factory.createEntityManager();
        namespaces = new Namespaces(manager);

        uuidByPathByWorkspace = new HashMap<Long, Map<Path, UUID>>();

        manager.getTransaction().begin();

        // Determine the UUID for all root nodes ...
        UUID rootUuid = UUID.randomUUID();

        // Now populate a graph of nodes ...
        workspaceId = 10L;
        uuidByPath(workspaceId).put(path("/"), rootUuid);
        create(workspaceId, "/a");
        create(workspaceId, "/a/a1");
        create(workspaceId, "/a/a1/a1");
        create(workspaceId, "/a/a1/a2");
        create(workspaceId, "/a/a1/a3");
        create(workspaceId, "/a/a2");
        create(workspaceId, "/a/a2/a1");
        create(workspaceId, "/a/a2/a1/a1");
        create(workspaceId, "/a/a2/a1/a1/a1");
        create(workspaceId, "/a/a2/a1/a1/a2");
        create(workspaceId, "/a/a2/a1/a2");
        create(workspaceId, "/a/a2/a2");
        create(workspaceId, "/a/a2/a3");
        create(workspaceId, "/a/a2/a4");
        setLargeValue(workspaceId, "/a/a1", "prop1", validLargeValues[0]);
        setLargeValue(workspaceId, "/a/a1", "prop1", validLargeValues[1]); // the only node that uses #1
        setLargeValue(workspaceId, "/a/a2", "prop1", validLargeValues[0]);
        setLargeValue(workspaceId, "/a/a2", "prop2", validLargeValues[2]);
        setLargeValue(workspaceId, "/a/a2/a1", "prop2", validLargeValues[0]);
        setLargeValue(workspaceId, "/a/a2/a1", "prop3", validLargeValues[2]);
        manager.getTransaction().commit();
        manager.getTransaction().begin();

        // Create some content in other workspaces ...
        Long otherWorkspace = 3254L;
        uuidByPath(otherWorkspace).put(path("/"), rootUuid);
        create(otherWorkspace, "/a");
        create(otherWorkspace, "/a/a1");
        create(otherWorkspace, "/a/a1/a1");
        create(otherWorkspace, "/a/a1/a2");
        create(otherWorkspace, "/a/a1/a3");
        create(otherWorkspace, "/a/a2");
        create(otherWorkspace, "/a/a2/a1");
        create(otherWorkspace, "/a/a2/a1/a1");
        create(otherWorkspace, "/a/a2/a1/a1/a1");
        create(otherWorkspace, "/a/a2/a1/a1/a2");
        create(otherWorkspace, "/a/a2/a1/a2");
        create(otherWorkspace, "/a/a2/a2");
        create(otherWorkspace, "/a/a2/a3");
        create(otherWorkspace, "/a/a2/a4");
        // Add some large values that were already used in the other workspace
        setLargeValue(workspaceId, "/a/a2/a1", "prop2", validLargeValues[0]);
        setLargeValue(workspaceId, "/a/a2/a1", "prop2", validLargeValues[2]);
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
            manager = null;
            try {
                factory.close();
            } finally {
                factory = null;
            }
        }
    }

    protected Map<Path, UUID> uuidByPath( Long workspaceId ) {
        Map<Path, UUID> map = uuidByPathByWorkspace.get(workspaceId);
        if (map == null) {
            map = new HashMap<Path, UUID>();
            uuidByPathByWorkspace.put(workspaceId, map);
        }
        return map;
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected void create( Long workspaceId,
                           String pathStr ) {
        Path path = path(pathStr);
        if (uuidByPath(workspaceId).containsKey(path)) return;
        if (path.isRoot()) return;
        Path parent = path.getParent();
        // Look up the parent ...
        UUID parentUuid = uuidByPath(workspaceId).get(parent);
        assert parentUuid != null;
        // Calculate the child index by walking the existing nodes ...
        int numChildren = 0;
        for (Path existing : uuidByPath(workspaceId).keySet()) {
            if (parent.equals(existing.getParent())) {
                ++numChildren;
            }
        }

        // Create the child entity ...
        Name childName = path.getLastSegment().getName();
        int snsIndex = path.getLastSegment().getIndex();
        NamespaceEntity namespace = namespaces.get(childName.getNamespaceUri(), true);
        UUID childUuid = UUID.randomUUID();
        ChildId id = new ChildId(workspaceId, childUuid.toString());
        ChildEntity entity = new ChildEntity(id, parentUuid.toString(), ++numChildren, namespace, childName.getLocalName(),
                                             snsIndex);
        manager.persist(entity);

        // Create the properties ...
        NodeId nodeId = new NodeId(workspaceId, childUuid.toString());
        PropertiesEntity props = new PropertiesEntity(nodeId);
        props.setData("bogus data".getBytes());
        props.setPropertyCount(1);
        props.setCompressed(false);
        manager.persist(props);

        uuidByPath(workspaceId).put(path, childUuid);
    }

    protected ReferenceEntity createReferenceBetween( Long workspaceId,
                                                      String fromPathStr,
                                                      String toPathStr ) {
        Path fromPath = path(fromPathStr);
        Path toPath = path(toPathStr);

        // Look up the UUIDs ...
        UUID fromUuid = uuidByPath(workspaceId).get(fromPath);
        UUID toUuid = uuidByPath(workspaceId).get(toPath);
        assert fromUuid != null;
        assert toUuid != null;

        // Now create a reference entity ...
        ReferenceEntity entity = new ReferenceEntity(new ReferenceId(workspaceId, fromUuid.toString(), toUuid.toString()));
        manager.persist(entity);
        return entity;
    }

    protected UUID uuidForPath( Long workspaceId,
                                String pathStr ) {
        Path path = path(pathStr);
        return uuidByPath(workspaceId).get(path);
    }

    protected void setLargeValue( Long workspaceId,
                                  String pathStr,
                                  String propertyName,
                                  String largeValue ) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        Path path = path(pathStr);
        UUID nodeUuid = uuidByPath(workspaceId).get(path);
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
        NodeId nodeId = new NodeId(workspaceId, nodeUuid.toString());
        PropertiesEntity props = manager.find(PropertiesEntity.class, nodeId);
        assertThat(props, is(notNullValue()));

        // Add the large value ...
        props.getLargeValues().add(id);
    }

    protected LargeValueId largeValueId( String value ) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return new LargeValueId(StringUtil.getHexString(SecureHash.getHash(SecureHash.Algorithm.SHA_1, value.getBytes())));
    }

    protected PropertiesEntity getProperties( Long workspaceId,
                                              String pathStr ) {
        Path path = path(pathStr);
        UUID nodeUuid = uuidByPath(workspaceId).get(path);
        assertThat(nodeUuid, is(notNullValue()));

        NodeId nodeId = new NodeId(workspaceId, nodeUuid.toString());
        return manager.find(PropertiesEntity.class, nodeId);
    }

    protected void verifyNextLocationIs( Long workspaceId,
                                         String path ) {
        Path pathObj = path(path);
        UUID uuid = uuidByPath(workspaceId).get(pathObj);
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
            expectedNodeUuids[i] = uuidForPath(workspaceId, pathStr).toString();
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

    protected void assertNumberOfLargeValueEntities( long count ) throws Exception {
        Query query = manager.createQuery("select count(*) from LargeValueEntity");
        assertThat((Long)query.getSingleResult(), is(count));
    }

    protected void assertNumberOfPropertyEntitiesInWorkspace( Long workspaceId,
                                                              long count ) throws Exception {
        Query query = manager.createQuery("select count(*) from PropertiesEntity as prop where prop.id.workspaceId = :workspaceId");
        query.setParameter("workspaceId", workspaceId);
        assertThat((Long)query.getSingleResult(), is(count));
    }

    protected void assertNumberOfChildEntitiesInWorkspace( Long workspaceId,
                                                           long count ) throws Exception {
        Query query = manager.createQuery("select count(*) from ChildEntity as child where child.id.workspaceId = :workspaceId");
        query.setParameter("workspaceId", workspaceId);
        assertThat((Long)query.getSingleResult(), is(count));
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
        UUID uuid = uuidByPath(workspaceId).get(path);
        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
        verifyNoMoreLocations();
        query.close();
    }

    @Test
    public void shouldPerformSubgraphQueryOfNodeWithChildrenAndGrandchildren() {
        Path path = path("/a/a2");
        UUID uuid = uuidByPath(workspaceId).get(path);
        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a4");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1/a2");
        verifyNoMoreLocations();
        query.close();
    }

    @Test
    public void shouldPerformSubgraphQueryOfNodeWithChildrenAndGrandchildrenAndGreatGranchildren() {
        Path path = path("/a");
        UUID uuid = uuidByPath(workspaceId).get(path);
        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a");
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a4");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1/a2");
        verifyNoMoreLocations();
        query.close();
    }

    @Test
    public void shouldPerformMaxDepthSubgraphQueryOfNodeWithChildrenAndGrandchildrenAndGreatGranchildren() {
        Path path = path("/a");
        UUID uuid = uuidByPath(workspaceId).get(path);
        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, 4);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a");
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a4");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1/a2");
        verifyNoMoreLocations();

        locations = query.getNodeLocations(true, false);
        verifyNextLocationIs(workspaceId, "/a");
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a4");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a2");
        verifyNoMoreLocations();
        query.close();

        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, 2);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a");
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a4");
        verifyNoMoreLocations();

        locations = query.getNodeLocations(true, false);
        verifyNextLocationIs(workspaceId, "/a");
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNoMoreLocations();

        query.close();

        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, 3);
        locations = query.getNodeLocations(true, false);
        verifyNextLocationIs(workspaceId, "/a");
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a4");
        verifyNoMoreLocations();

        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a");
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a4");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a2");
        verifyNoMoreLocations();
        query.close();
    }

    @Test
    public void shouldDeleteSubgraph() throws Exception {
        // Verify that all the nodes with large values do indeed have them ...
        verifyNodesHaveLargeValues("/a/a1", "/a/a2", "/a/a2/a1");

        // Count the number of objects ...
        assertNumberOfLargeValueEntities(3L);
        assertNumberOfPropertyEntitiesInWorkspace(workspaceId, 14L);
        assertNumberOfChildEntitiesInWorkspace(workspaceId, 14L);

        // Delete "/a/a1". Note that "/a/a1" has a large value that is shared by "/a/a2", but it's also the only
        // user of large value #1.
        Path path = path("/a/a1");
        UUID uuid = uuidByPath(workspaceId).get(path);

        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
        verifyNoMoreLocations();
        query.deleteSubgraph(true);
        assertThat(query.getInwardReferences().isEmpty(), is(true));
        query.close();

        // Commit the transaction, and start another ...
        manager.getTransaction().commit();
        manager.getTransaction().begin();
        manager.flush();

        // Count the number of objects ...
        assertNumberOfLargeValueEntities(2L);
        assertNumberOfPropertyEntitiesInWorkspace(workspaceId, 10L);
        assertNumberOfChildEntitiesInWorkspace(workspaceId, 10L);

        // Verify the graph structure is correct ...
        path = path("/a");
        uuid = uuidByPath(workspaceId).get(path);
        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, 4);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a");
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a4");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1/a2");
        verifyNoMoreLocations();

        locations = query.getNodeLocations(true, false);
        verifyNextLocationIs(workspaceId, "/a");
        verifyNextLocationIs(workspaceId, "/a/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a2");
        verifyNextLocationIs(workspaceId, "/a/a2/a3");
        verifyNextLocationIs(workspaceId, "/a/a2/a4");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a2/a1/a2");
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
        assertNumberOfLargeValueEntities(3L);
        assertNumberOfPropertyEntitiesInWorkspace(workspaceId, 14L);
        assertNumberOfChildEntitiesInWorkspace(workspaceId, 14L);

        // Create references from the "/a/a2" (not being deleted) branch, to the branch being deleted...
        List<ReferenceEntity> expectedInvalidRefs = new ArrayList<ReferenceEntity>();
        expectedInvalidRefs.add(createReferenceBetween(workspaceId, "/a/a2", "/a/a1"));
        expectedInvalidRefs.add(createReferenceBetween(workspaceId, "/a/a2/a1", "/a/a1/a1"));
        expectedInvalidRefs.add(createReferenceBetween(workspaceId, "/a/a2/a2", "/a/a1/a2"));

        // Create references between nodes in the branch being deleted (these shouldn't matter) ...
        createReferenceBetween(workspaceId, "/a/a1", "/a/a1/a1");
        createReferenceBetween(workspaceId, "/a/a1/a2", "/a/a1/a3");

        // Delete "/a/a1". Note that "/a/a1" has a large value that is shared by "/a/a2", but it's also the only
        // user of large value #1.
        Path path = path("/a/a1");
        UUID uuid = uuidByPath(workspaceId).get(path);

        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
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
        assertNumberOfLargeValueEntities(3L);
        assertNumberOfPropertyEntitiesInWorkspace(workspaceId, 14L);
        assertNumberOfChildEntitiesInWorkspace(workspaceId, 14L);

        // Create references from the nodes that aren't being deleted (these won't matter, but will remain)...
        List<ReferenceEntity> expectedValidRefs = new ArrayList<ReferenceEntity>();
        expectedValidRefs.add(createReferenceBetween(workspaceId, "/a/a2", "/a/a2/a1"));

        // Create references between nodes in the branch being deleted (these shouldn't matter) ...
        createReferenceBetween(workspaceId, "/a/a1", "/a/a1/a1");
        createReferenceBetween(workspaceId, "/a/a1/a2", "/a/a1/a3");

        // Delete "/a/a1". Note that "/a/a1" has a large value that is shared by "/a/a2", but it's also the only
        // user of large value #1.
        Path path = path("/a/a1");
        UUID uuid = uuidByPath(workspaceId).get(path);

        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, Integer.MAX_VALUE);
        locations = query.getNodeLocations(true, true);
        verifyNextLocationIs(workspaceId, "/a/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a1");
        verifyNextLocationIs(workspaceId, "/a/a1/a2");
        verifyNextLocationIs(workspaceId, "/a/a1/a3");
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
        assertNumberOfLargeValueEntities(3L);
        assertNumberOfPropertyEntitiesInWorkspace(workspaceId, 14L);
        assertNumberOfChildEntitiesInWorkspace(workspaceId, 14L);

        // Create references from the nodes that aren't even part of the subgraph ...
        List<ReferenceEntity> otherRefs = new ArrayList<ReferenceEntity>();
        otherRefs.add(createReferenceBetween(workspaceId, "/a/a2", "/a/a2/a1"));
        otherRefs.add(createReferenceBetween(workspaceId, "/a/a2/a1", "/a/a2/a2"));

        // Create references between nodes in the subgraph ...
        List<ReferenceEntity> internalRefs = new ArrayList<ReferenceEntity>();
        internalRefs.add(createReferenceBetween(workspaceId, "/a/a1", "/a/a1/a1"));
        internalRefs.add(createReferenceBetween(workspaceId, "/a/a1/a2", "/a/a1/a3"));

        // Create references from nodes outside of the subgraph to nodes inside of the subgraph ...
        List<ReferenceEntity> inwardRefs = new ArrayList<ReferenceEntity>();
        inwardRefs.add(createReferenceBetween(workspaceId, "/a/a2", "/a/a1/a1"));
        inwardRefs.add(createReferenceBetween(workspaceId, "/a/a2/a1", "/a/a1/a3"));

        // Create references from nodes inside of the subgraph to nodes outside of the subgraph ...
        List<ReferenceEntity> outwardRefs = new ArrayList<ReferenceEntity>();
        outwardRefs.add(createReferenceBetween(workspaceId, "/a/a1", "/a/a2"));
        outwardRefs.add(createReferenceBetween(workspaceId, "/a/a1/a1", "/a/a2/a1"));

        // Create the query ...
        Path path = path("/a/a1");
        UUID uuid = uuidByPath(workspaceId).get(path);
        query = SubgraphQuery.create(context, manager, workspaceId, uuid, path, Integer.MAX_VALUE);

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
