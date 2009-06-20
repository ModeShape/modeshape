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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.hibernate.ejb.Ejb3Configuration;
import org.jboss.dna.connector.store.jpa.model.common.NamespaceEntity;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ChildEntityTest {

    private EntityManagerFactory factory;
    private EntityManager manager;
    private BasicModel model;
    private ExecutionContext context;

    @Before
    public void beforeEach() throws Exception {
        model = new BasicModel();
        context = new ExecutionContext();
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

        // Always create a bunch of nodes in a workspace that is not used, so we're sure that these
        // tests work only on the workspace used in the test
        NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");
        Long workspaceId = 1202L;
        for (int i = 0; i != 10; ++i) {
            createChildren(workspaceId, UUID.randomUUID(), ns, 1, 10, "child", false);
        }
    }

    @After
    public void afterEach() throws Exception {
        try {
            if (manager != null) manager.close();
        } finally {
            manager = null;
            if (factory != null) {
                try {
                    factory.close();
                } finally {
                    factory = null;
                }
            }
        }
    }

    protected ChildId[] createChildren( Long workspaceId,
                                        UUID parentUuid,
                                        NamespaceEntity ns,
                                        int startingIndex,
                                        int numChildren,
                                        String localName,
                                        boolean useSns ) {

        ChildId[] result = new ChildId[numChildren];
        manager.getTransaction().begin();
        try {
            // Create the child entities ...
            for (int i = 0; i != numChildren; ++i) {
                int indexInParent = i + startingIndex;
                ChildId id = new ChildId(workspaceId, UUID.randomUUID().toString());
                ChildEntity child = null;
                if (useSns) {
                    child = new ChildEntity(id, parentUuid.toString(), indexInParent, ns, localName, i + 1);
                } else {
                    String name = numChildren == 1 ? localName : localName + indexInParent;
                    child = new ChildEntity(id, parentUuid.toString(), indexInParent, ns, name);
                }
                result[i] = id;
                manager.persist(child);
            }
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            // manager.getTransaction().rollback();
            throw t;
        }
        return result;
    }

    protected ChildId[] createMixtureOfChildren( Long workspaceId,
                                                 UUID parentUuid,
                                                 NamespaceEntity ns ) {
        ChildId[] ids1 = createChildren(workspaceId, parentUuid, ns, 1, 10, "child", false);
        ChildId[] ids2 = createChildren(workspaceId, parentUuid, ns, 11, 10, "childWithSameName", true);
        ChildId[] ids3 = createChildren(workspaceId, parentUuid, ns, 21, 1, "anotherChild", false);
        ChildId[] ids4 = createChildren(workspaceId, parentUuid, ns, 22, 1, "nextToLastChild", false);
        ChildId[] ids5 = createChildren(workspaceId, parentUuid, ns, 23, 1, "lastChild", false);
        ChildId[][] ids = new ChildId[][] {ids1, ids2, ids3, ids4, ids5};
        ChildId[] results = new ChildId[ids1.length + ids2.length + ids3.length + ids4.length + ids5.length];
        int i = 0;
        for (ChildId[] idArray : ids) {
            for (ChildId id : idArray)
                results[i++] = id;
        }
        return results;
    }

    protected ChildEntity getChild( Long workspaceId,
                                    String childUuid ) {
        Query query = manager.createNamedQuery("ChildEntity.findByChildUuid");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("childUuidString", childUuid);
        return (ChildEntity)query.getSingleResult();
    }

    @Test
    public void shouldCreateChildrenWithDifferentNames() {
        Long workspaceId = 1L;
        UUID parentUuid = UUID.randomUUID();
        NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");
        ChildId[] ids = createChildren(workspaceId, parentUuid, ns, 1, 10, "child", false);

        // Look up the object ...
        manager.getTransaction().begin();
        try {
            int index = 1;
            for (ChildId id : ids) {
                ChildEntity child = manager.find(ChildEntity.class, id);
                assertThat(child.getId(), is(id));
                assertThat(child.getIndexInParent(), is(index));
                assertThat(child.getChildName(), is("child" + index));
                assertThat(child.getChildNamespace(), is(ns));
                assertThat(child.getSameNameSiblingIndex(), is(1));
                ++index;
            }
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @Test
    public void shouldCreateChildrenWithSameNameSiblingIndex() {
        Long workspaceId = 1L;
        UUID parentUuid = UUID.randomUUID();
        NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");
        ChildId[] ids = createChildren(workspaceId, parentUuid, ns, 1, 10, "child", true);

        // Look up the object ...
        manager.getTransaction().begin();
        try {
            int index = 1;
            for (ChildId id : ids) {
                ChildEntity child = manager.find(ChildEntity.class, id);
                assertThat(child.getId(), is(id));
                assertThat(child.getIndexInParent(), is(index));
                assertThat(child.getChildName(), is("child"));
                assertThat(child.getChildNamespace(), is(ns));
                assertThat(child.getSameNameSiblingIndex(), is(index));
                ++index;
            }
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldCreateMixtureOfChildrenWithDifferentNamesAndSameNameSiblingIndexes() {
        Long workspaceId = 1L;
        UUID parentUuid = UUID.randomUUID();
        NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");
        createChildren(workspaceId, parentUuid, ns, 1, 10, "child", false);
        createChildren(workspaceId, parentUuid, ns, 11, 10, "childWithSameName", true);
        createChildren(workspaceId, parentUuid, ns, 21, 1, "anotherChild", false);
        createChildren(workspaceId, parentUuid, ns, 22, 1, "nextToLastChild", false);
        createChildren(workspaceId, parentUuid, ns, 23, 1, "lastChild", false);

        // Look up the object ...
        manager.getTransaction().begin();
        try {
            Query query = manager.createNamedQuery("ChildEntity.findAllUnderParent");
            query.setParameter("workspaceId", workspaceId);
            query.setParameter("parentUuidString", parentUuid.toString());
            List<ChildEntity> children = query.getResultList();
            int index = 1;
            assertThat(children.size(), is(23));
            for (ChildEntity child : children) {
                assertThat(child.getIndexInParent(), is(index++));
            }
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldCreateMixtureOfChildrenWithDifferentNamesAndSameNameSiblingIndexesMethod2() {
        Long workspaceId = 1L;
        UUID parentUuid = UUID.randomUUID();
        NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");
        ChildId[] ids = createMixtureOfChildren(workspaceId, parentUuid, ns);
        assertThat(ids.length, is(23));

        // Look up the object ...
        manager.getTransaction().begin();
        try {
            Query query = manager.createNamedQuery("ChildEntity.findAllUnderParent");
            query.setParameter("workspaceId", workspaceId);
            query.setParameter("parentUuidString", parentUuid.toString());
            List<ChildEntity> children = query.getResultList();
            int index = 1;
            assertThat(children.size(), is(23));
            for (ChildEntity child : children) {
                assertThat(child.getIndexInParent(), is(index++));
            }

            index = 1;
            for (ChildId id : ids) {
                ChildEntity entity = getChild(workspaceId, id.getChildUuidString());
                assertThat(entity.getIndexInParent(), is(index++));
            }
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldFindEntitiesInIndexRange() {
        Long workspaceId = 1L;
        UUID parentUuid = UUID.randomUUID();
        NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");
        ChildId[] ids = createMixtureOfChildren(workspaceId, parentUuid, ns);
        assertThat(ids.length, is(23));

        // Look up the objects ...
        manager.getTransaction().begin();
        try {
            Query query = manager.createNamedQuery("ChildEntity.findAllUnderParent");
            query.setParameter("workspaceId", workspaceId);
            query.setParameter("parentUuidString", parentUuid.toString());
            List<ChildEntity> children = query.getResultList();
            int index = 1;
            assertThat(children.size(), is(23));
            for (ChildEntity child : children) {
                assertThat(child.getIndexInParent(), is(index++));
            }

            query = manager.createNamedQuery("ChildEntity.findRangeUnderParent");
            query.setParameter("workspaceId", workspaceId);
            query.setParameter("parentUuidString", parentUuid.toString());
            query.setParameter("firstIndex", 3);
            query.setParameter("afterIndex", 6);
            children = query.getResultList();
            assertThat(children.size(), is(3));
            assertThat(children.get(0).getIndexInParent(), is(3));
            assertThat(children.get(1).getIndexInParent(), is(4));
            assertThat(children.get(2).getIndexInParent(), is(5));

        } finally {
            manager.getTransaction().rollback();
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldFindEntitiesAfterIndex() {
        Long workspaceId = 1L;
        UUID parentUuid = UUID.randomUUID();
        NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");
        ChildId[] ids = createMixtureOfChildren(workspaceId, parentUuid, ns);
        assertThat(ids.length, is(23));

        // Look up the objects ...
        manager.getTransaction().begin();
        try {
            Query query = manager.createNamedQuery("ChildEntity.findAllUnderParent");
            query.setParameter("parentUuidString", parentUuid.toString());
            query.setParameter("workspaceId", workspaceId);
            List<ChildEntity> children = query.getResultList();
            int index = 1;
            assertThat(children.size(), is(23));
            for (ChildEntity child : children) {
                assertThat(child.getIndexInParent(), is(index++));
            }

            query = manager.createNamedQuery("ChildEntity.findChildrenAfterIndexUnderParent");
            query.setParameter("parentUuidString", parentUuid.toString());
            query.setParameter("afterIndex", 18);
            query.setParameter("workspaceId", workspaceId);
            children = query.getResultList();
            assertThat(children.size(), is(6));
            assertThat(children.get(0).getIndexInParent(), is(18));
            assertThat(children.get(1).getIndexInParent(), is(19));
            assertThat(children.get(2).getIndexInParent(), is(20));
            assertThat(children.get(3).getIndexInParent(), is(21));
            assertThat(children.get(4).getIndexInParent(), is(22));
            assertThat(children.get(5).getIndexInParent(), is(23));

        } finally {
            manager.getTransaction().rollback();
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldFindAdjustChildIndexesAfterRemovalOfFirstSibling() {
        Long workspaceId = 1L;
        UUID parentUuid = UUID.randomUUID();
        NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");
        ChildId[] ids = createMixtureOfChildren(workspaceId, parentUuid, ns);
        assertThat(ids.length, is(23));

        // Look up the objects ...
        manager.getTransaction().begin();
        try {
            Query query = manager.createNamedQuery("ChildEntity.findAllUnderParent");
            query.setParameter("parentUuidString", parentUuid.toString());
            query.setParameter("workspaceId", workspaceId);
            List<ChildEntity> children = query.getResultList();
            int index = 1;
            assertThat(children.size(), is(23));
            for (ChildEntity child : children) {
                assertThat(child.getIndexInParent(), is(index++));
            }

            // Remove the first child ...
            ChildEntity child = getChild(workspaceId, ids[0].getChildUuidString());
            assertThat(child, is(notNullValue()));
            String childName = child.getChildName();
            manager.remove(child);

            ChildEntity.adjustSnsIndexesAndIndexesAfterRemoving(manager,
                                                                workspaceId,
                                                                parentUuid.toString(),
                                                                childName,
                                                                ns.getId(),
                                                                0);

            assertChildren(workspaceId, parentUuid.toString(),
            // "child1",
                           "child2",
                           "child3",
                           "child4",
                           "child5",
                           "child6",
                           "child7",
                           "child8",
                           "child9",
                           "child10",
                           "childWithSameName[1]",
                           "childWithSameName[2]",
                           "childWithSameName[3]",
                           "childWithSameName[4]",
                           "childWithSameName[5]",
                           "childWithSameName[6]",
                           "childWithSameName[7]",
                           "childWithSameName[8]",
                           "childWithSameName[9]",
                           "childWithSameName[10]",
                           "anotherChild",
                           "nextToLastChild",
                           "lastChild");

        } finally {
            manager.getTransaction().rollback();
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void assertChildren( Long workspaceId,
                                   String parentUuid,
                                   String... childNames ) {
        Query query = manager.createNamedQuery("ChildEntity.findAllUnderParent");
        query.setParameter("parentUuidString", parentUuid.toString());
        query.setParameter("workspaceId", workspaceId);
        List<ChildEntity> children = query.getResultList();
        int index = 0;
        for (ChildEntity child : children) {
            // System.out.println("found " + child);
            String childName = childNames[index++];
            Path.Segment segment = context.getValueFactories().getPathFactory().createSegment(childName);
            assertThat(child.getChildName(), is(segment.getName().getLocalName()));
            assertThat(child.getSameNameSiblingIndex(), is(segment.hasIndex() ? segment.getIndex() : 1));
            assertThat(child.getIndexInParent(), is(index)); // index is incremented
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void printChildren( Long workspaceId,
                                  String parentUuid ) {
        Query query = manager.createNamedQuery("ChildEntity.findAllUnderParent");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("parentUuidString", parentUuid.toString());
        List<ChildEntity> children = query.getResultList();
        for (ChildEntity child : children) {
            System.out.println("found " + child);
        }

    }
}
