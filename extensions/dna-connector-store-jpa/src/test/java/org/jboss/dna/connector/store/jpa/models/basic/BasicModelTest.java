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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import org.hibernate.ejb.Ejb3Configuration;
import org.jboss.dna.common.util.SecureHash;
import org.jboss.dna.common.util.StringUtil;
import org.jboss.dna.connector.store.jpa.JpaConnectorI18n;
import org.jboss.dna.connector.store.jpa.models.common.NamespaceEntity;
import org.jboss.dna.connector.store.jpa.models.common.NodeId;
import org.jboss.dna.graph.BasicExecutionContext;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.properties.PropertyType;
import org.jboss.dna.graph.requests.processor.RequestProcessor;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test not only verifies the (minimal) functionality of the {@link BasicModel} class, but it also verifies that the entity
 * classes used by the {@link BasicModel#configure(Ejb3Configuration) configuration} are consistent and error-free. In other
 * words, if there are any problems with any of the entity annotations, they will be found when the {@link EntityManager} is
 * {@link #startEntityManager() started}.
 * 
 * @author Randall Hauch
 */
public class BasicModelTest {

    private EntityManagerFactory factory;
    private EntityManager manager;
    private BasicModel model;
    private ExecutionContext context;

    @BeforeClass
    public static void beforeAll() throws Exception {
    }

    @Before
    public void beforeEach() throws Exception {
        model = new BasicModel();
        context = new BasicExecutionContext();
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

    protected EntityManager startEntityManager() {
        if (manager == null) {
            // Connect to the database ...
            Ejb3Configuration configurator = new Ejb3Configuration();
            model.configure(configurator);
            configurator.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
            configurator.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
            configurator.setProperty("hibernate.connection.username", "sa");
            configurator.setProperty("hibernate.connection.password", "");
            configurator.setProperty("hibernate.connection.url", "jdbc:hsqldb:.");
            configurator.setProperty("hibernate.show_sql", "true");
            configurator.setProperty("hibernate.format_sql", "true");
            configurator.setProperty("hibernate.use_sql_comments", "true");
            configurator.setProperty("hibernate.hbm2ddl.auto", "create");
            factory = configurator.buildEntityManagerFactory();
            manager = factory.createEntityManager();
        }
        return manager;
    }

    @Test
    public void shouldHaveName() {
        assertThat(model.getName(), is("Basic"));
    }

    @Test
    public void shouldHaveDescription() {
        assertThat(model.getDescription(), is(JpaConnectorI18n.basicModelDescription.text()));
        assertThat(model.getDescription(Locale.US), is(JpaConnectorI18n.basicModelDescription.text()));
    }

    @Test
    public void shouldCreateRequestProcessor() {
        EntityManager manager = mock(EntityManager.class);
        EntityTransaction txn = mock(EntityTransaction.class);
        stub(manager.getTransaction()).toReturn(txn);
        RequestProcessor proc = model.createRequestProcessor("test source", context, manager, UUID.randomUUID(), 100, false);
        assertThat(proc, is(notNullValue()));
    }

    @Test
    public void shouldPersistPropertyEntityWithCompressedFlagAndNoChildren() {
        startEntityManager();
        NodeId nodeId = new NodeId(UUID.randomUUID());
        PropertiesEntity prop = new PropertiesEntity();
        prop.setCompressed(true);
        prop.setData("Hello, World".getBytes());
        prop.setId(nodeId);
        manager.getTransaction().begin();
        try {
            // Save a properties entity (with compressed data) ...
            manager.persist(prop);
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
        // Look up the object ...
        manager.getTransaction().begin();
        try {
            PropertiesEntity prop2 = manager.find(PropertiesEntity.class, nodeId);
            assertThat(prop2.isCompressed(), is(prop.isCompressed()));
            assertThat(prop2.getId(), is(prop.getId()));
            assertThat(prop2.getData(), is(prop.getData()));
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @Test
    public void shouldPersistPropertyEntityWithUncompressedFlagAndNoChildren() {
        startEntityManager();
        NodeId nodeId = new NodeId(UUID.randomUUID());
        PropertiesEntity prop = new PropertiesEntity();
        prop.setData("Hello, World".getBytes());
        prop.setId(nodeId);
        manager.getTransaction().begin();
        try {
            // Save a properties entity (with compressed data) ...
            manager.persist(prop);
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
        // Look up the object ...
        manager.getTransaction().begin();
        try {
            PropertiesEntity prop2 = manager.find(PropertiesEntity.class, nodeId);
            assertThat(prop2.isCompressed(), is(prop.isCompressed()));
            assertThat(prop2.getId(), is(prop.getId()));
            assertThat(prop2.getData(), is(prop.getData()));
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @Test
    public void shouldPersistLargeValueEntityWithCompressedFlag() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        startEntityManager();
        byte[] content = "Jack and Jill went up the hill to grab a pail of water.".getBytes();
        String hash = StringUtil.getHexString(SecureHash.getHash(SecureHash.Algorithm.SHA_1, content));
        LargeValueEntity entity = new LargeValueEntity();
        entity.setCompressed(true);
        entity.setHash(hash);
        entity.setLength(content.length);
        entity.setData(content);
        entity.setType(PropertyType.STRING);
        manager.getTransaction().begin();
        try {
            // Save the entity ...
            manager.persist(entity);
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
        // Look up the object ...
        manager.getTransaction().begin();
        try {
            LargeValueEntity entity2 = manager.find(LargeValueEntity.class, hash);
            assertThat(entity2.isCompressed(), is(entity.isCompressed()));
            assertThat(entity2.getHash(), is(entity.getHash()));
            assertThat(entity2.getData(), is(entity.getData()));
            assertThat(entity2.getLength(), is(entity.getLength()));
            assertThat(entity2.getType(), is(entity.getType()));
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @Test
    public void shouldPersistLargeValueEntityWithUncompressedFlag() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        startEntityManager();
        byte[] content = "Jack and Jill went up the hill to grab a pail of water.".getBytes();
        String hash = StringUtil.getHexString(SecureHash.getHash(SecureHash.Algorithm.SHA_1, content));
        LargeValueEntity entity = new LargeValueEntity();
        // entity.setCompressed(false);
        entity.setHash(hash);
        entity.setLength(content.length);
        entity.setData(content);
        entity.setType(PropertyType.STRING);
        manager.getTransaction().begin();
        try {
            // Save the entity ...
            manager.persist(entity);
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
        // Look up the object ...
        manager.getTransaction().begin();
        try {
            LargeValueEntity entity2 = manager.find(LargeValueEntity.class, hash);
            assertThat(entity2.isCompressed(), is(entity.isCompressed()));
            assertThat(entity2.getHash(), is(entity.getHash()));
            assertThat(entity2.getData(), is(entity.getData()));
            assertThat(entity2.getLength(), is(entity.getLength()));
            assertThat(entity2.getType(), is(entity.getType()));
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @Test
    public void shouldPersistNamespaceEntity() {
        startEntityManager();
        String uri = "http://www.example.com";
        NamespaceEntity namespace = new NamespaceEntity(uri);
        manager.getTransaction().begin();
        try {
            // Save a namespace entity ...
            manager.persist(namespace);
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
        // Look up the object ...
        manager.getTransaction().begin();
        try {
            NamespaceEntity ns2 = manager.find(NamespaceEntity.class, namespace.getId());
            assertThat(ns2.getUri(), is(namespace.getUri()));
            assertThat(ns2.getId(), is(namespace.getId()));
        } finally {
            manager.getTransaction().rollback();
        }
        // Look up by namespace ...
        manager.getTransaction().begin();
        try {
            NamespaceEntity ns2 = NamespaceEntity.findByUri(manager, uri);
            assertThat(ns2.getUri(), is(namespace.getUri()));
            assertThat(ns2.getId(), is(namespace.getId()));
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @Test
    public void shouldPersistChildEntity() {
        startEntityManager();
        UUID parentId = UUID.randomUUID();

        // Create UUIDs for several children ...
        ChildId childId1 = new ChildId(parentId, UUID.randomUUID());
        ChildId childId2 = new ChildId(parentId, UUID.randomUUID());
        ChildId childId3 = new ChildId(parentId, UUID.randomUUID());
        assertThat(childId1, is(not(childId2)));
        assertThat(childId1, is(not(childId3)));
        assertThat(childId2, is(not(childId3)));

        manager.getTransaction().begin();
        try {
            NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");

            // Create the child entities ...
            ChildEntity child1 = new ChildEntity(childId1, 1, ns, "child1");
            ChildEntity child2 = new ChildEntity(childId2, 2, ns, "child2");
            ChildEntity child3 = new ChildEntity(childId3, 3, ns, "child3", 1);

            // Save a properties entities ...
            manager.persist(child1);
            manager.persist(child2);
            manager.persist(child3);
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            // manager.getTransaction().rollback();
            throw t;
        }
        // Look up the object ...
        manager.getTransaction().begin();
        try {
            NamespaceEntity ns = NamespaceEntity.findByUri(manager, "http://www.example.com");

            ChildEntity child1a = manager.find(ChildEntity.class, childId1);
            ChildEntity child2a = manager.find(ChildEntity.class, childId2);
            ChildEntity child3a = manager.find(ChildEntity.class, childId3);

            assertThat(child1a.getId(), is(childId1));
            assertThat(child1a.getIndexInParent(), is(1));
            assertThat(child1a.getChildName(), is("child1"));
            assertThat(child1a.getChildNamespace(), is(ns));
            assertThat(child1a.getSameNameSiblingIndex(), is(nullValue()));

            assertThat(child2a.getId(), is(childId2));
            assertThat(child2a.getIndexInParent(), is(2));
            assertThat(child2a.getChildName(), is("child2"));
            assertThat(child2a.getChildNamespace(), is(ns));
            assertThat(child2a.getSameNameSiblingIndex(), is(nullValue()));

            assertThat(child3a.getId(), is(childId3));
            assertThat(child3a.getIndexInParent(), is(3));
            assertThat(child3a.getChildName(), is("child3"));
            assertThat(child3a.getChildNamespace(), is(ns));
            assertThat(child3a.getSameNameSiblingIndex(), is(1));
        } finally {
            manager.getTransaction().rollback();
        }
    }
}
