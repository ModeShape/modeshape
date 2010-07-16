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
package org.modeshape.test.integration.svn;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.connector.svn.SvnRepositorySource;
import org.modeshape.graph.SecurityContext;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrSecurityContextCredentials;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;

public class JcrAndLocalSvnRepositoryTest {
    private boolean print;
    private JcrEngine engine;
    private Session session;

    @Before
    public void beforeEach() throws Exception {
        print = false;

        // Copy our repository into the target area ...
        File source = new File("src/test/resources/svn/local_repos/dummy_svn_repos");
        File target = new File("target/local_svn_repos");
        FileUtil.delete(target);
        int num = FileUtil.copy(source, target);
        print("Copied " + num + " files and directories");

        // Create the engine that uses the local SVN repository ...
        final String repositoryUrl = svnUrlFor(target);
        final String[] predefinedWorkspaceNames = new String[] {"trunk", "tags"};
        final String svnRepositorySource = "svnRepositorySource";
        final String repositoryName = "svnRepository";
        final JcrConfiguration configuration = new JcrConfiguration();
        configuration.repositorySource(svnRepositorySource).usingClass(SvnRepositorySource.class).setProperty("password", "").setProperty("username",
                                                                                                                                          "anonymous").setProperty("repositoryRootUrl",
                                                                                                                                                                   repositoryUrl).setProperty("predefinedWorkspaceNames",
                                                                                                                                                                                              predefinedWorkspaceNames).setProperty("defaultWorkspaceName",
                                                                                                                                                                                                                                    predefinedWorkspaceNames[0]).setProperty("creatingWorkspacesAllowed",
                                                                                                                                                                                                                                                                             false).setProperty("updatesAllowed",
                                                                                                                                                                                                                                                                                                true).and().repository(repositoryName).setDescription("The JCR repository backed by a local SVN").setSource(svnRepositorySource);
        this.engine = configuration.save().and().build();
        this.engine.start();

        print("Using local SVN repository " + repositoryUrl);

        this.session = this.engine.getRepository(repositoryName).login(new JcrSecurityContextCredentials(
                                                                                                         new MyCustomSecurityContext()));

    }

    @After
    public void afterEach() throws Exception {
        if (this.session != null) {
            try {
                this.session.logout();
            } finally {
                this.session = null;
                if (this.engine != null) {
                    this.engine.shutdown();
                }
            }
        }
    }

    @Test
    public void shouldIterateOverChildrenOfRoot() throws Exception {
        print("Getting the root node and it's children (to a maximum depth of 4)...");
        Node root = session.getRootNode();
        printSubgraph(root, 4, "  ");

        Node h = root.getNode("root/c/h");
        assertThatNodeIsFolder(h);
        Node dnaSubmission = root.getNode("root/c/h/JBoss DNA Submission Receipt for JBoss World 2009.pdf");
        assertThatNodeIsFile(dnaSubmission, "application/octet-stream", null);
    }

    @Test
    public void shouldAllowingAddingFile() throws Exception {
        Node rootNode = session.getRootNode();

        File file = new File("src/test/resources/log4j.properties");
        String fileContent = IoUtil.read(file);
        assertThat(file.exists() && file.isFile(), is(true));
        assertThat(fileContent, is(notNullValue()));

        Node fileNode = rootNode.addNode(file.getName(), "nt:file");
        Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
        contentNode.setProperty("jcr:mimeType", "text/plain");
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        contentNode.setProperty("jcr:data", fileContent);

        print("## STARTING TO SAVE ##");
        rootNode.getSession().save();
        print("## SAVED ##");
    }

    protected void print( String str ) {
        if (print) System.out.println(str);
    }

    protected void printSubgraph( Node node,
                                  int depth,
                                  String prefix ) throws RepositoryException {
        NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            print((prefix != null ? prefix : " ") + child);
            if (depth > 1) printSubgraph(child, depth - 1, prefix);
        }
    }

    public void assertThatNodeIsFolder( Node node ) throws RepositoryException {
        assertThat(node, is(notNullValue()));
        assertThat(node.getProperty("jcr:primaryType").getString(), is("nt:folder"));
    }

    public void assertThatNodeIsFile( Node node,
                                      String mimeType,
                                      String contents ) throws RepositoryException {
        assertThat(node, is(notNullValue()));
        assertThat(node.getProperty("jcr:primaryType").getString(), is("nt:file"));

        // Check that there is one child, and that the child is "jcr:content" ...
        NodeIterator nodeIterator = node.getNodes();
        assertThat(nodeIterator.getSize() >= 1L, is(true));

        // Check that the "jcr:content" node is correct ...
        Node jcrContent = node.getNode("jcr:content");
        assertThat(jcrContent.getProperty("jcr:mimeType").getString(), is(mimeType));
        if (contents != null) {
            assertThat(jcrContent.getProperty("jcr:data").getString(), is(contents));
        }
    }

    protected class MyCustomSecurityContext implements SecurityContext {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.SecurityContext#getUserName()
         */
        public String getUserName() {
            return "Fred";
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.SecurityContext#hasRole(java.lang.String)
         */
        public boolean hasRole( String roleName ) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.SecurityContext#logout()
         */
        public void logout() {
            // do something
        }
    }

    /**
     * Create an OS-independent URL for a local SVN repository.
     * 
     * @param directory the directory representing the root of the local SVN repository
     * @return the URL as a string
     * @throws IOException if there is a problem obtaining the canonical URI for the supplied directory
     * @throws SVNException if there is a problem parsing or decoding the URL
     */
    protected static String svnUrlFor( File directory ) throws IOException, SVNException {
        String url = directory.getCanonicalFile().toURI().toURL().toExternalForm();

        url = url.replaceFirst("file:/", "file://localhost/");

        // Have to decode the URL ...
        SVNURL encodedUrl = SVNURL.parseURIEncoded(url);
        url = encodedUrl.toDecodedString();

        if (!url.endsWith("/")) url = url + "/";
        return url;
    }
}
