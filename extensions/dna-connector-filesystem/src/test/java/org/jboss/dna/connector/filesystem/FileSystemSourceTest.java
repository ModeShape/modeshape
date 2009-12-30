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
package org.jboss.dna.connector.filesystem;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.observe.Observer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class FileSystemSourceTest {

    private FileSystemSource source;
    private RepositoryConnection connection;

    @Before
    public void beforeEach() throws Exception {
        final ExecutionContext context = new ExecutionContext();
        this.source = new FileSystemSource();
        // Set the mandatory properties ...
        this.source.setName("Test Repository");
        this.source.initialize(new RepositoryContext() {

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
                return new RepositoryConnectionFactory() {

                    @SuppressWarnings( "synthetic-access" )
                    public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                        return source.getConnection();
                    }

                };
            }

        });
    }

    @After
    public void afterEach() throws Exception {
        if (this.connection != null) {
            this.connection.close();
        }
    }

    @Test
    public void shouldReturnNonNullCapabilities() {
        assertThat(source.getCapabilities(), is(notNullValue()));
    }

    @Test
    public void shouldSupportSameNameSiblings() {
        assertThat(source.getCapabilities().supportsSameNameSiblings(), is(true));
    }

    @Test
    public void shouldNotSupportUpdates() {
        assertThat(source.getCapabilities().supportsUpdates(), is(false));
    }

    @Test
    public void shouldHaveNullSourceNameUponConstruction() {
        source = new FileSystemSource();
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingName() {
        source.setName("Something");
        assertThat(source.getName(), is("Something"));
        source.setName("another name");
        assertThat(source.getName(), is("another name"));
    }

    @Test
    public void shouldAllowSettingNameToNull() {
        source.setName("some name");
        source.setName(null);
        assertThat(source.getName(), is(nullValue()));
    }

    @Test
    public void shouldHaveDefaultRetryLimit() {
        assertThat(source.getRetryLimit(), is(FileSystemSource.DEFAULT_RETRY_LIMIT));
    }

    @Test
    public void shouldSetRetryLimitToZeroWhenSetWithNonPositiveValue() {
        source.setRetryLimit(0);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-1);
        assertThat(source.getRetryLimit(), is(0));
        source.setRetryLimit(-100);
        assertThat(source.getRetryLimit(), is(0));
    }

    @Test
    public void shouldAllowRetryLimitToBeSet() {
        for (int i = 0; i != 100; ++i) {
            source.setRetryLimit(i);
            assertThat(source.getRetryLimit(), is(i));
        }
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldFailToCreateConnectionIfSourceHasNoName() {
        source.setName(null);
        source.getConnection();
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
}
