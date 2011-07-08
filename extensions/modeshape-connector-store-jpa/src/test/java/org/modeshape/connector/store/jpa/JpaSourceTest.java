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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySourceException;

/**
 * @author Randall Hauch
 */
public class JpaSourceTest {

    private JpaSource source;
    private RepositoryConnection connection;

    @Before
    public void beforeEach() throws Exception {
        // Set the connection properties using the environment defined in the POM files ...
        this.source = TestEnvironment.configureJpaSource("Test Repository", this);

        // Override the inherited properties ...
        this.source.setDefaultWorkspaceName("default");
        this.source.setCreatingWorkspacesAllowed(true);
    }

    @After
    public void afterEach() throws Exception {
        if (this.connection != null) {
            this.connection.close();
        }
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldFailToCreateConnectionIfSourceHasNoName() {
        source.setName(null);
        source.getConnection();
    }

    @Test
    public void shouldHaveNoDefaultModelUponConstruction() {
        JpaSource source = new JpaSource();
        assertThat(source.getModel(), is(nullValue()));
    }

    @Test
    public void shouldCreateConnection() throws Exception {
        connection = source.getConnection();
        assertThat(connection, is(notNullValue()));
    }

    @Test
    public void shouldAllowMultipleConnectionsToBeOpenAtTheSameTime() throws Exception {
        List<RepositoryConnection> connections = new ArrayList<RepositoryConnection>();
        try {
            for (int i = 0; i != 10; ++i) {
                RepositoryConnection conn = source.getConnection();
                assertThat(conn, is(notNullValue()));
                connections.add(conn);
            }
        } finally {
            // Close all open connections ...
            for (RepositoryConnection conn : connections) {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }

    @FixFor( "MODE-1102" )
    @Test
    public void shouldDefaultAutoGenerateSchemaValueToValidate() {
        source = new JpaSource();
        source.setName("Some name");
        assertThat(source.getAutoGenerateSchema(), is(JpaSource.DEFAULT_AUTO_GENERATE_SCHEMA));

        // Verify it is set correctly on the Hibernate configuration ...
        JpaAdapter adapter = new HibernateAdapter();
        Properties config = adapter.getProperties(source);
        assertThat(config.get("hibernate.hbm2ddl.auto").toString(), is(JpaSource.DEFAULT_AUTO_GENERATE_SCHEMA));
    }

    @FixFor( "MODE-1102" )
    @Test
    public void shouldTreatNullValueForAutoGenerateSchemaAsDefault() {
        source = new JpaSource();
        source.setName("Some name");
        source.setAutoGenerateSchema(null);
        assertThat(source.getAutoGenerateSchema(), is(JpaSource.DEFAULT_AUTO_GENERATE_SCHEMA));

        // Verify it is set correctly on the Hibernate configuration ...
        JpaAdapter adapter = new HibernateAdapter();
        Properties config = adapter.getProperties(source);
        assertThat(config.get("hibernate.hbm2ddl.auto").toString(), is(JpaSource.DEFAULT_AUTO_GENERATE_SCHEMA));
    }

    @FixFor( "MODE-1102" )
    @Test
    public void shouldValidStringValueForAutoGenerateSchemaAsDisable() {
        String[] values = {"create", "create-drop", "update", "validate"};
        for (String value : values) {
            source = new JpaSource();
            source.setName("Some name");
            source.setAutoGenerateSchema(value);
            assertThat(source.getAutoGenerateSchema(), is(value));

            // Verify it is set correctly on the Hibernate configuration ...
            JpaAdapter adapter = new HibernateAdapter();
            Properties config = adapter.getProperties(source);
            assertThat(config.get("hibernate.hbm2ddl.auto").toString(), is(value));
        }
    }

    @FixFor( "MODE-1102" )
    @Test
    public void shouldTreatEmptyStringValueForAutoGenerateSchemaAsDefault() {
        source = new JpaSource();
        source.setName("Some name");
        source.setAutoGenerateSchema("");
        assertThat(source.getAutoGenerateSchema(), is(JpaSource.DEFAULT_AUTO_GENERATE_SCHEMA));

        // Verify it is set correctly on the Hibernate configuration ...
        JpaAdapter adapter = new HibernateAdapter();
        Properties config = adapter.getProperties(source);
        assertThat(config.get("hibernate.hbm2ddl.auto").toString(), is(JpaSource.DEFAULT_AUTO_GENERATE_SCHEMA));
    }

    @FixFor( "MODE-1102" )
    @Test
    public void shouldTreatUpperCaseDisableStringValueForAutoGenerateSchemaAsDisable() {
        source = new JpaSource();
        source.setName("Some name");
        source.setAutoGenerateSchema(JpaSource.AUTO_GENERATE_SCHEMA_DISABLE.toUpperCase());
        assertThat(source.getAutoGenerateSchema(), is(JpaSource.AUTO_GENERATE_SCHEMA_DISABLE));

        // Verify it is set correctly on the Hibernate configuration ...
        JpaAdapter adapter = new HibernateAdapter();
        Properties config = adapter.getProperties(source);
        assertThat(config.get("hibernate.hbm2ddl.auto"), is(nullValue()));
    }
}
