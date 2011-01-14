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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.connector.store.jpa.model.common.WorkspaceEntity;
import org.modeshape.connector.store.jpa.model.simple.SimpleModel;
import org.modeshape.graph.connector.RepositoryConnection;

/**
 * Simple unit test that simply verifies the database connection.
 */
public class JdbcConnectionTest {

    private EntityManagerFactory factory;
    private EntityManager manager;
    private SimpleModel model;

    @Before
    public void beforeEach() throws Exception {
        model = new SimpleModel();
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
    public void shouldConnectToDatabaseAndPersistWorkspace() {
        startEntityManager();

        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setName("connection test");

        manager.getTransaction().begin();
        try {
            // Save a properties entity (with compressed data) ...
            manager.persist(workspace);
            manager.getTransaction().commit();
        } catch (RuntimeException t) {
            manager.getTransaction().rollback();
            throw t;
        }
        // Look up the object ...
        manager.getTransaction().begin();
        try {
            WorkspaceEntity workspace2 = manager.find(WorkspaceEntity.class, workspace.getId());
            assertThat(workspace2.getName(), is(workspace.getName()));
        } finally {
            manager.getTransaction().rollback();
        }
    }

    @Test
    public void shouldAutoDetectDialectAndSetOnJpaSource() {
        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = TestEnvironment.configureJpaSource("Test Repository", this);
        String expectedDialect = source.getDialect();

        source.setDialect(null);
        RepositoryConnection connection = null;
        try {
            connection = source.getConnection();
            assertThat(source.getDialect(), is(notNullValue()));
            if (expectedDialect != null) {
                if (expectedDialect.toLowerCase().contains("mysql")) {
                    // The MySQL auto-detected dialect may be different than the dialect that was explicitly set
                    assertThat(source.getDialect().toLowerCase().contains("mysql"), is(true));
                } else {
                    assertThat(source.getDialect(), is(expectedDialect));
                }
            }
        } finally {
            if (connection != null) connection.close();
        }
    }
}
