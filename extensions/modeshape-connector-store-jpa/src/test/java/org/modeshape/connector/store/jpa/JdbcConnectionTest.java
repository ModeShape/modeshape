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
package org.modeshape.connector.store.jpa;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.modeshape.connector.store.jpa.model.basic.BasicModel;
import org.modeshape.connector.store.jpa.model.basic.NodeId;
import org.modeshape.connector.store.jpa.model.basic.PropertiesEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Simple unit test that simply verifies the database connection.
 */
public class JdbcConnectionTest {

    private EntityManagerFactory factory;
    private EntityManager manager;
    private BasicModel model;

    @Before
    public void beforeEach() throws Exception {
        model = new BasicModel();
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
            // Set the connection properties using the environment defined in the POM files ...
            JpaSource source = TestEnvironment.configureJpaSource("Test Repository", this);

            // Connect to the database ...
            Ejb3Configuration configurator = new Ejb3Configuration();
            model.configure(configurator);
            configurator.setProperty("hibernate.dialect", source.getDialect());
            configurator.setProperty("hibernate.connection.driver_class", source.getDriverClassName());
            configurator.setProperty("hibernate.connection.username", source.getUsername());
            configurator.setProperty("hibernate.connection.password", source.getPassword());
            configurator.setProperty("hibernate.connection.url", source.getUrl());
            configurator.setProperty("hibernate.show_sql", "false");
            configurator.setProperty("hibernate.format_sql", "true");
            configurator.setProperty("hibernate.use_sql_comments", "true");
            configurator.setProperty("hibernate.hbm2ddl.auto", "create");
            factory = configurator.buildEntityManagerFactory();
            manager = factory.createEntityManager();
        }
        return manager;
    }

    @Test
    public void shouldConnectToDatabaseAndPersistBasicProperty() {
        startEntityManager();

        Long workspaceId = 10L;
        NodeId nodeId = new NodeId(workspaceId, UUID.randomUUID().toString());
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
}
