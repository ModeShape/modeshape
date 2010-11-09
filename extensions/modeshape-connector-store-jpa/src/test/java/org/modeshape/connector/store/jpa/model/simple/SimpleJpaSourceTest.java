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
package org.modeshape.connector.store.jpa.model.simple;

import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.connector.store.jpa.JpaSource;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.observe.Observer;

public class SimpleJpaSourceTest {

    private JpaSource source;

    @Before
    public void beforeEach() {
        // Set the connection properties using the environment defined in the POM files ...
        source = new JpaSource();

        source.setModel(JpaSource.Models.SIMPLE.getName());
        source.setName("SimpleJpaSource");
        source.setDialect("org.hibernate.dialect.HSQLDialect");
        source.setDriverClassName("org.hsqldb.jdbcDriver");
        source.setUsername("sa");
        source.setPassword("");
        source.setUrl("jdbc:hsqldb:mem:.");
        source.setShowSql(false);
        source.setAutoGenerateSchema("create");

        source.initialize(new RepositoryContext() {

            private final ExecutionContext context = new ExecutionContext();

            public Subgraph getConfiguration( int depth ) {
                return null;
            }

            public ExecutionContext getExecutionContext() {
                return context;
            }

            public Observer getObserver() {
                return null;
            }

            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return null;
            }

        });
    }

    @Test
    public void shouldCreateLiveConnection() throws InterruptedException {
        RepositoryConnection connection = source.getConnection();
        connection.ping(1, TimeUnit.SECONDS);
        connection.close();
    }

    /*    
        The test below helps establish that the hibernate.connection.isolation property is being passed to Hibernate
        correctly and respected by Hibernate, but the test case is very brittle and should not be checked in.
        
        If you wish to reverify this behavior, please uncomment the entityManager() method in SimpleJpaConnection.
        @FixFor("MODE-983")
        @Test
        public void shouldAllowChangingIsolationLevel() throws Exception {
            RepositoryConnection conn;
            SimpleJpaConnection jpaConn;
            EntityManagerImpl emgr;
            int txLevel;

            // txLevel = Connection.TRANSACTION_SERIALIZABLE;

            txLevel = Connection.TRANSACTION_REPEATABLE_READ;
            source.setIsolationLevel(txLevel);
            conn = source.getConnection();
            assertThat(conn, instanceOf(SimpleJpaConnection.class));

            jpaConn = (SimpleJpaConnection)conn;
            emgr = (EntityManagerImpl)jpaConn.entityManager();
            assertTrue(emgr.getSession().connection().getTransactionIsolation() == txLevel);
            jpaConn.close();

        txLevel = Connection.TRANSACTION_SERIALIZABLE;
        source.setIsolationLevel(txLevel);
        conn = source.getConnection();
        assertThat(conn, instanceOf(SimpleJpaConnection.class));

        jpaConn = (SimpleJpaConnection)conn;
        emgr = (EntityManagerImpl)jpaConn.entityManager();
        assertTrue(emgr.getSession().connection().getTransactionIsolation() == txLevel);
        jpaConn.close();

        }
        */
}
