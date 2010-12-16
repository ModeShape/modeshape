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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.graph.JcrLexicon;

/**
 * @author jverhaeg
 */
public class JcrWorkspaceTest extends AbstractSessionTest {

    @BeforeClass
    public static void beforeClass() {
        // Initialize IDTrust
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
    }

    @Override
    protected void initializeContent() {
        graph.create("/a").and().create("/a/b").and().create("/a/b/c").and().create("/b").and();
        graph.set("booleanProperty").on("/a/b").to(true);
        graph.set("jcr:primaryType").on("/a/b").to("nt:unstructured");
        graph.set("stringProperty").on("/a/b/c").to("value");

        // Make sure the path to the namespaces exists ...
        graph.create("/jcr:system").and().create("/jcr:system/mode:namespaces").ifAbsent().and();

    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCloneWithNullWorkspaceName() throws Exception {
        workspace.clone(null, "/src", "/dest", false);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCopyFromNullPathToNullPath() throws Exception {
        workspace.copy(null, null);
    }

    @Ignore( "QueryManager is not initialized correctly, preventing the 'copy' to work properly" )
    @Test
    public void shouldCopyFromPathToAnotherPathInSameWorkspace() throws Exception {
        workspace.copy("/a/b", "/b/b-copy");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowCopyFromOtherWorkspaceWithNullWorkspace() throws Exception {
        workspace.copy(null, null, null);
    }

    @Test
    public void shouldNotAllowGetAccessibleWorkspaceNames() throws Exception {
        String[] names = workspace.getAccessibleWorkspaceNames();
        assertThat(names.length, is(1));
        assertThat(names[0], is(workspaceName));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowImportContentHandlerWithNullPath() throws Exception {
        workspace.getImportContentHandler(null, 0);
    }

    @Test
    public void shouldGetImportContentHandlerWithValidPath() throws Exception {
        assertThat(workspace.getImportContentHandler("/b", 0), is(notNullValue()));
    }

    @Test
    public void shouldProvideName() throws Exception {
        assertThat(workspace.getName(), is(workspaceName));
    }

    @Test
    public void shouldHaveSameContextIdAsSession() {
        assertThat(workspace.context().getId(), is(session.getExecutionContext().getId()));
    }

    @Test
    public void shouldProvideNamespaceRegistry() throws Exception {
        NamespaceRegistry registry = workspace.getNamespaceRegistry();
        assertThat(registry, is(notNullValue()));
        assertThat(registry.getURI(JcrLexicon.Namespace.PREFIX), is(JcrLexicon.Namespace.URI));
    }

    @Test
    public void shouldGetNodeTypeManager() throws Exception {
        assertThat(workspace.getNodeTypeManager(), is(notNullValue()));
    }

    @Test
    public void shouldGetObservationManager() throws Exception {
        assertThat(workspace.getObservationManager(), is(notNullValue()));
    }

    @Test
    public void shouldProvideQueryManager() throws Exception {
        assertThat(workspace.getQueryManager(), notNullValue());
    }

    @Test
    public void shouldCreateQuery() throws Exception {
        String statement = "SELECT * FROM [nt:unstructured]";

        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery(statement, Query.JCR_SQL2);

        assertThat(query, is(notNullValue()));
        assertThat(query.getLanguage(), is(Query.JCR_SQL2));
        assertThat(query.getStatement(), is(statement));
    }

    @Test
    public void shouldStoreQueryAsNode() throws Exception {
        String statement = "SELECT * FROM [nt:unstructured]";

        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery(statement, Query.JCR_SQL2);

        Node node = query.storeAsNode("/storedQuery");
        assertThat(node, is(notNullValue()));
        assertThat(node.getPrimaryNodeType().getName(), is("nt:query"));
        assertThat(node.getProperty("jcr:language").getString(), is(Query.JCR_SQL2));
        assertThat(node.getProperty("jcr:statement").getString(), is(statement));
    }

    @Test
    public void shouldLoadStoredQuery() throws Exception {
        String statement = "SELECT * FROM [nt:unstructured]";

        QueryManager queryManager = workspace.getQueryManager();
        Query query = queryManager.createQuery(statement, Query.JCR_SQL2);

        Node node = query.storeAsNode("/storedQuery");

        Query loaded = queryManager.getQuery(node);

        assertThat(loaded, is(notNullValue()));
        assertThat(loaded.getLanguage(), is(Query.JCR_SQL2));
        assertThat(loaded.getStatement(), is(statement));
        assertThat(loaded.getStoredQueryPath(), is(node.getPath()));
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat(workspace.getSession(), is(notNullValue()));
    }

    @Test
    public void shouldAllowImportXml() throws Exception {
        String inputData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                           + "<sv:node xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:nt=\"http://www.jcp.org/jcr/nt/1.0\" "
                           + "xmlns:sv=\"http://www.jcp.org/jcr/sv/1.0\" sv:name=\"workspaceTestNode\">"
                           + "<sv:property sv:name=\"jcr:primaryType\" sv:type=\"Name\">"
                           + "<sv:value>nt:unstructured</sv:value></sv:property></sv:node>";
        workspace.importXML("/", new ByteArrayInputStream(inputData.getBytes()), 0);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowMoveFromNullPath() throws Exception {
        workspace.move(null, null);
    }

    @Test
    public void shouldAllowMoveFromPathToAnotherPathInSameWorkspace() throws Exception {
        workspace.move("/a/b", "/b/b-copy");
    }
}
