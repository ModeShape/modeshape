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
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.Session;
import javax.jcr.Workspace;
import org.jboss.dna.graph.connector.SimpleRepository;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

/**
 * @author jverhaeg
 */
public class JcrWorkspaceTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        JcrSessionTest.executionContext = TestUtil.getExecutionContext();
        JcrSessionTest.simpleRepository = SimpleRepository.get(JcrSessionTest.WORKSPACE_NAME);
        JcrSessionTest.connectionFactory = TestUtil.createJackRabbitConnectionFactory(JcrSessionTest.simpleRepository,
                                                                                      JcrSessionTest.executionContext);
        JcrSessionTest.repository = new JcrRepository(JcrSessionTest.executionContext, JcrSessionTest.connectionFactory);
    }

    @AfterClass
    public static void afterClass() {
        SimpleRepository.shutdownAll();
    }

    private Session session;
    private Workspace workspace;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        session = JcrSessionTest.repository.login();
        workspace = session.getWorkspace();
    }

    @After
    public void after() throws Exception {
        if (session.isLive()) {
            session.logout();
        }
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoSession() throws Exception {
        new JcrWorkspace(null, JcrSessionTest.WORKSPACE_NAME);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoWorkspaceName() throws Exception {
        new JcrWorkspace((JcrSession)session, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowClone() throws Exception {
        workspace.clone(null, null, null, false);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowCopy() throws Exception {
        workspace.copy(null, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowCopyFromOtherWorkspace() throws Exception {
        workspace.copy(null, null, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowGetAccessibleWorkspaceNames() throws Exception {
        workspace.getAccessibleWorkspaceNames();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowImportContentHandler() throws Exception {
        workspace.getImportContentHandler(null, 0);
    }

    @Test
    public void shouldProvideName() throws Exception {
        assertThat(workspace.getName(), is(JcrSessionTest.WORKSPACE_NAME));
    }

    @Test
    public void shouldProvideNamespaceRegistry() throws Exception {
        assertThat(workspace.getNamespaceRegistry(), notNullValue());
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowGetNodeTypeManager() throws Exception {
        workspace.getNodeTypeManager();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowGetObservationManager() throws Exception {
        workspace.getObservationManager();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowGetQueryManager() throws Exception {
        workspace.getQueryManager();
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat(workspace.getSession(), is(session));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowImportXml() throws Exception {
        workspace.importXML(null, null, 0);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowMove() throws Exception {
        workspace.move(null, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRestore() throws Exception {
        workspace.restore(null, false);
    }
}
