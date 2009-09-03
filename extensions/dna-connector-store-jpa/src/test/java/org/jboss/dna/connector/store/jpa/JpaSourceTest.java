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
package org.jboss.dna.connector.store.jpa;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class JpaSourceTest {

    private JpaSource source;
    private JpaConnection connection;

    @Before
    public void beforeEach() throws Exception {
        this.source = new JpaSource();
        // Set the connection properties to be an in-memory HSQL database ...
        this.source.setName("Test Repository");
        this.source.setDialect("org.hibernate.dialect.HSQLDialect");
        this.source.setDriverClassName("org.hsqldb.jdbcDriver");
        this.source.setUsername("sa");
        this.source.setPassword("");
        this.source.setUrl("jdbc:hsqldb:.");
        this.source.setMaximumConnectionsInPool(3);
        this.source.setMinimumConnectionsInPool(0);
        this.source.setNumberOfConnectionsToAcquireAsNeeded(1);
        this.source.setMaximumSizeOfStatementCache(100);
        this.source.setMaximumConnectionIdleTimeInSeconds(0);
        this.source.setDefaultWorkspaceName("default");
        this.source.setCreatingWorkspacesAllowed(true);
        this.source.setAutoGenerateSchema("create");
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
        assertThat(source.getModel(), is(nullValue()));
    }

    @Test
    public void shouldCreateConnection() throws Exception {
        connection = (JpaConnection)source.getConnection();
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
}
