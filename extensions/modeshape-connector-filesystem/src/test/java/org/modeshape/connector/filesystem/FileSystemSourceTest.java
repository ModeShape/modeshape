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
package org.modeshape.connector.filesystem;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.MockRepositoryContext;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositorySourceException;

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
        this.source.setWorkspaceRootPath("target");
        this.source.initialize(new MockRepositoryContext(context, source));
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
