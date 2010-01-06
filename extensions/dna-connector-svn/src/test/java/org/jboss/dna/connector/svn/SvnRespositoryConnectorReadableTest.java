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
import static org.junit.Assert.assertThat;
import java.util.List;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.ReadableConnectorTest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Serge Pagop
 *
 */
public class SvnRespositoryConnectorReadableTest extends ReadableConnectorTest {

    private static String url;

    @BeforeClass
    public static void beforeAny() throws Exception {
        url = SvnConnectorTestUtil.createURL("src/test/resources/dummy_svn_repos", "target/copy_of dummy_svn_repos");

    }

    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws Exception {
        String[] predefinedWorkspaceNames = new String[] {"trunk", "tags"};
        SvnRepositorySource source = new SvnRepositorySource();
        source.setName("Test Repository");
        source.setUsername("sp");
        source.setPassword("");
        source.setRepositoryRootUrl(url);
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(false);
        
        return source;
    }


    /**
     * {@inheritDoc}
     *
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws Exception {
     // No need to initialize any content ...
    }
    
    @Test
    public void shouldFindFolderSpecifiedInPathsAsNodesBelowRoot() {
        Node root = graph.getNodeAt("/root");
        assertThatNodeIsFolder(root);
        Node dnaSubmission = graph.getNodeAt("/root/c/h/JBoss DNA Submission Receipt for JBoss World 2009.pdf");
        assertThatNodeIsFile(dnaSubmission, "application/octet-stream", null);
    }
    
    public void assertThatNodeIsFolder( Node node ) {
        assertThat(node, is(notNullValue()));
        assertThat(node.getProperty(JcrLexicon.PRIMARY_TYPE).getFirstValue(), is((Object)JcrNtLexicon.FOLDER));
    }
    
    public void assertThatNodeIsFile( Node node,
                                      String mimeType,
                                      String contents ) {
        assertThat(node, is(notNullValue()));
        assertThat(node.getProperty(JcrLexicon.PRIMARY_TYPE).getFirstValue(), is((Object)JcrNtLexicon.FILE));

        // Check that there is one child, and that the child is "jcr:content" ...
        List<Location> children = node.getChildren();
        assertThat(children.size(), is(1));
        Location jcrContentLocation = children.get(0);
        assertThat(jcrContentLocation.getPath().getLastSegment().getName(), is(JcrLexicon.CONTENT));

        // Check that the "jcr:content" node is correct ...
        Node jcrContent = graph.getNodeAt(jcrContentLocation);
        assertThat(string(jcrContent.getProperty(JcrLexicon.MIMETYPE).getFirstValue()), is(mimeType));
        if (contents != null) {
            assertThat(string(jcrContent.getProperty(JcrLexicon.DATA).getFirstValue()), is(contents));
        }

    }

}
