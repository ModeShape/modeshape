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
package org.jboss.dna.connector.svn;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.graph.cache.CachePolicy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * @author Serge Pagop
 */
public class SVNRepositoryConnectionTest {
    private SVNRepositoryConnection connection;
    private SVNRepository selectedRepository;
    private String sourceName;
    private String url;
    String username;
    String password;
    private Set<String> availableWorkspaceNames;

    @Mock
    private CachePolicy policy;


    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        url = SVNConnectorTestUtil.createURL("src/test/resources/dummy_svn_repos", "target/copy_of dummy_svn_repos");
        username = "sp";
        password = "";
        // Set up the appropriate factory for a particular protocol
        selectedRepository = SVNConnectorTestUtil.createRepository(url, username, password);
        sourceName = "the source name";
        availableWorkspaceNames = new HashSet<String>();
        availableWorkspaceNames.add(url + "trunk");
        availableWorkspaceNames.add(url + "tags");
        connection = new SVNRepositoryConnection(sourceName, selectedRepository, availableWorkspaceNames,
                                                 Boolean.FALSE, policy, Boolean.TRUE, new RepositoryAccessData(url, username,password));

    }

    @After
    public void afterEach() {
        try {
            if (connection != null) connection.close();
        } finally {

        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInstantiateIfSourceNameIsNull() {
        sourceName = null;
        connection = new SVNRepositoryConnection(sourceName, selectedRepository, availableWorkspaceNames,
                                                 Boolean.FALSE, policy, Boolean.FALSE, new RepositoryAccessData(url, username,password));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInstantiateIfRepositoryIsNull() {
        selectedRepository = null;
        connection = new SVNRepositoryConnection(sourceName, selectedRepository, availableWorkspaceNames,
                                                 Boolean.FALSE, policy, Boolean.FALSE, new RepositoryAccessData(url, username,password));
    }

    @Test
    public void shouldInstantiateWithValidSourceAndDAVRepositoryReferences() throws Exception {
        assertThat(connection, is(notNullValue()));
    }

    @Test
    public void shouldDelegateToTheSourceForTheConnectionsSourceName() {
        assertThat(connection.getSourceName(), is("the source name"));
    }

    @Test
    public void shouldDelegateToTheSourceForTheConnectionsDefaultCachePolicy() {
        assertThat(connection.getDefaultCachePolicy(), is(sameInstance(policy)));
    }

    @Test
    public void shouldGetTheSVNRepositoryRootFromTheSVNRepositoryWhenPinged() throws Exception {
        CachePolicy policy = mock(CachePolicy.class);
        selectedRepository = SVNConnectorTestUtil.createRepository(url, "sp", "");
        connection = new SVNRepositoryConnection("the source name", selectedRepository, availableWorkspaceNames,
                                                 Boolean.FALSE, policy, Boolean.FALSE, new RepositoryAccessData(url, username,password));
        assertThat(connection.ping(1, TimeUnit.SECONDS), is(true));
    }
}