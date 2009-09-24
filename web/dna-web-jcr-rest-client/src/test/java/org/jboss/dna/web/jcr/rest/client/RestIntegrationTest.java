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
package org.jboss.dna.web.jcr.rest.client;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import org.jboss.dna.web.jcr.rest.client.domain.Repository;
import org.jboss.dna.web.jcr.rest.client.domain.Server;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;
import org.jboss.dna.web.jcr.rest.client.json.JsonRestClient;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public final class RestIntegrationTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    // user and password configured in pom
    private static final String PSWD = "password"; //$NON-NLS-1$
    private static final String USER = "dnauser"; //$NON-NLS-1$

    private static final Server SERVER = new Server("http://localhost:8080", USER, PSWD, false); //$NON-NLS-1$
    private static final String REPOSITORY_NAME = "dna:repository"; //$NON-NLS-1$
    private static final Repository REPOSITORY = new Repository(REPOSITORY_NAME, SERVER);
    private static final String WORKSPACE_NAME = "default"; //$NON-NLS-1$
    private static final Workspace WORKSPACE = new Workspace(WORKSPACE_NAME, REPOSITORY);

    private static final String WORKSPACE_PATH = "/myproject/myfolder/"; //$NON-NLS-1$
    private static final String FILE_PATH = WORKSPACE_PATH + "document.txt"; //$NON-NLS-1$

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    private JsonRestClient restClient;

    private ServerManager serverManager;

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    @Before
    public void beforeEach() {
        this.restClient = new JsonRestClient();
        this.serverManager = new ServerManager(null, this.restClient);
        this.serverManager.addServer(SERVER);
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    @Test
    public void shouldAllowServerPropertiesToChange() {
        // TODO implement shouldAllowServerPropertiesToChange()
    }

//    @Test
    public void shouldGetRepositories() throws Exception {
        Collection<Repository> repositories = this.serverManager.getRepositories(SERVER);
        assertThat(repositories.size(), is(1));
    }

//    @Test
    public void shouldGetWorkspaces() throws Exception {
        Collection<Workspace> workspaces = this.serverManager.getWorkspaces(REPOSITORY);
        assertThat(workspaces.size(), is(1));
    }

//    @Test
    public void shouldPublish() throws Exception {
        URL textFile = getClass().getResource(FILE_PATH);
        assertThat(textFile, is(notNullValue()));

        // publish
        File file = new File(textFile.toURI());
        assertThat(this.serverManager.publish(WORKSPACE, WORKSPACE_PATH, file).isOk(), is(true));

        // confirm it exists in repository
        assertThat((this.restClient).pathExists(WORKSPACE, WORKSPACE_PATH, file), is(true));
    }

    @Test
    public void shouldUnpublish() {
        // TODO implement shouldUnpublish()
    }

}
