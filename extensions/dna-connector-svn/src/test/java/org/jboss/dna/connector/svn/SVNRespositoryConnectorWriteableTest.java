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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayOutputStream;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrMixLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.test.AbstractConnectorTest;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * @author Serge Pagop
 */
public class SVNRespositoryConnectorWriteableTest extends AbstractConnectorTest {

    protected static final String EMPTY_CONTENT = "";
    protected static final String TEST_CONTENT = "Test content";
    protected SVNRepository remoteRepos = null;
    protected String repositoryRootURL;
    protected SVNNodeKind kind = null;
    protected SVNProperties fileProperties = null;
    protected ByteArrayOutputStream baos = null;

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws Exception {
        repositoryRootURL = SVNConnectorTestUtil.createURL("src/test/resources/dummy_svn_repos", "target/copy_of dummy_svn_repos");
        String[] predefinedWorkspaceNames = new String[] {repositoryRootURL + "trunk", repositoryRootURL + "tags"};
        SVNRepositorySource source = new SVNRepositorySource();
        source.setName("Test Repository");
        source.setUsername("sp");
        source.setPassword("");
        source.setRepositoryRootURL(repositoryRootURL);
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDirectoryForDefaultWorkspace(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(Boolean.TRUE);
        source.setUpdatesAllowed(true);

        remoteRepos = SVNRepositoryUtil.createRepository(repositoryRootURL + "trunk", "sp", "");

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws Exception {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#afterEach()
     */
    @Override
    public void afterEach() throws Exception {
        remoteRepos = null;
        super.afterEach();
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldNotBeAbleToCreateInvalidTypeForRepository() {
        graph.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED).orReplace().and();
    }

    @Test( expected = RepositorySourceException.class )
    public void shouldNotBeAbleToSetArbitraryProperties() {
        graph.create("/testFile").with(JcrLexicon.MIXIN_TYPES, JcrMixLexicon.LOCKABLE).orReplace().and();
    }

    @Test
    public void shouldBeAbleToCreateNodeFileWithContentLevel1() throws Exception {

        // LEVEL 0
        graph.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                     TEST_CONTENT.getBytes()).orReplace().and();
        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.create("/testFile1").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFile1/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                      TEST_CONTENT.getBytes()).orReplace().and();
        kind = remoteRepos.checkPath("testFile1", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile1", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        // LEVEL 1
        graph.create("/root/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/root/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                          TEST_CONTENT.getBytes()).orReplace().and();
        kind = remoteRepos.checkPath("root/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("root/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        // LEVEL 2
        graph.create("/root/a/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/root/a/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                            TEST_CONTENT.getBytes()).orReplace().and();
        kind = remoteRepos.checkPath("root/a/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("root/a/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);
    }

    @Test
    public void shouldRespectConflictBehaviorOnCreate() throws Exception {
        graph.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                     TEST_CONTENT.getBytes()).orReplace().and();

        graph.create("/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                     "Should not overwrite".getBytes()).ifAbsent().and();

        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);
    }

    @Test
    public void shouldBeAbleToCreateFileWithNoContent() throws Exception {
        graph.create("/testEmptyFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();

        kind = remoteRepos.checkPath("testEmptyFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testEmptyFile", -1, fileProperties, baos);
        assertContents(baos, EMPTY_CONTENT);
    }

    @Test
    public void shouldBeAbleToCreateFolder() throws Exception {
        graph.create("/testFolder").orReplace().and();

        kind = remoteRepos.checkPath("testFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));

        graph.create("/root/testFolder").orReplace().and();

        kind = remoteRepos.checkPath("root/testFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));

        graph.create("/root/a/testFolder").orReplace().and();

        kind = remoteRepos.checkPath("root/a/testFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));
    }

    @Test
    public void shouldBeAbleToAddChildrenToFolder() throws Exception {
        graph.create("/testFolder").orReplace().and();

        kind = remoteRepos.checkPath("testFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));

        graph.create("/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFolder/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                                TEST_CONTENT.getBytes()).orReplace().and();

        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.create("/root/testFolder").orReplace().and();

        kind = remoteRepos.checkPath("root/testFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));

        graph.create("/root/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/root/testFolder/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                                     TEST_CONTENT.getBytes()).orReplace().and();

        kind = remoteRepos.checkPath("root/testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("root/testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);
    }

    // @Test
    // public void shouldBeAbleToCopyFile() throws Exception {
    // graph.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
    // graph.create("/testFile/jcr:content")
    // .with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE)
    // .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
    // .orReplace()
    // .and();
    //
    // kind = remoteRepos.checkPath("testFile", -1);
    // assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
    // fileProperties = new SVNProperties();
    // baos = new ByteArrayOutputStream();
    // remoteRepos.getFile("testFile", -1, fileProperties, baos);
    // assertContents(baos, TEST_CONTENT);
    //        
    // graph.copy("/testFile").to("/copiedFile");
    // kind = remoteRepos.checkPath("copiedFile", -1);
    // assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
    // fileProperties = new SVNProperties();
    // baos = new ByteArrayOutputStream();
    // remoteRepos.getFile("copiedFile", -1, fileProperties, baos);
    // assertContents(baos, TEST_CONTENT);
    // }
    //
    // @Test
    // public void shouldBeAbleToCopyFolder() throws Exception {
    // graph.create("/testFolder").orReplace().and();
    // graph.create("/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
    // graph.create("/testFolder/testFile/jcr:content")
    // .with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE)
    // .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
    // .orReplace()
    // .and();
    //
    // kind = remoteRepos.checkPath("testFolder/testFile", -1);
    // assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
    // fileProperties = new SVNProperties();
    // baos = new ByteArrayOutputStream();
    // remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);
    // assertContents(baos, TEST_CONTENT);
    //        
    //
    // graph.copy("/testFolder").to("/copiedFolder");
    // kind = remoteRepos.checkPath("copiedFolder", -1);
    // assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));
    // fileProperties = new SVNProperties();
    // baos = new ByteArrayOutputStream();
    // remoteRepos.getFile("copiedFolder/testFile", -1, fileProperties, baos);
    // assertContents(baos, TEST_CONTENT);
    // }

    @Test
    public void shouldBeAbleToDeleteFolderWithContents() throws Exception {
        graph.create("/testFolder").orReplace().and();
        graph.create("/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFolder/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                                TEST_CONTENT.getBytes()).orReplace().and();

        kind = remoteRepos.checkPath("testFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));

        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.delete("/testFolder");

        kind = remoteRepos.checkPath("testFolder", -1);
        assertThat(kind == SVNNodeKind.NONE, is(Boolean.TRUE));
        
       
    }

    @Test
    public void shouldBeAbleToDeleteFile() throws Exception {
        graph.create("/testFolder").orReplace().and();
        graph.create("/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFolder/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                                TEST_CONTENT.getBytes()).orReplace().and();

        kind = remoteRepos.checkPath("testFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));

        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.delete("/testFolder/testFile");

        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.NONE, is(Boolean.TRUE));
        
        
        graph.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                                TEST_CONTENT.getBytes()).orReplace().and();
        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.delete("/testFile");

        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind == SVNNodeKind.NONE, is(Boolean.TRUE));
    }
    
    @Test
    public void shouldBeAbleToDeleteOnlyTheFileContent() throws Exception {
        graph.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                     TEST_CONTENT.getBytes()).orReplace().and();
        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.delete("/testFile/jcr:content");

        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));

        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);

        assertEmptyContents(baos, TEST_CONTENT);

        graph.create("/testFolder").orReplace().and();
        graph.create("/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFolder/testFile/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                                TEST_CONTENT.getBytes()).orReplace().and();
        kind = remoteRepos.checkPath("testFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));

        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.delete("/testFolder/testFile/jcr:content");

        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));

        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);

        assertEmptyContents(baos, TEST_CONTENT);
        
        
        graph.create("/testNode1").orReplace().and();
        graph.create("/testNode1/testNode10").orReplace().and();
        graph.create("/testNode1/testNode10/testItem0").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testNode1/testNode10/testItem0/jcr:content").with(JcrLexicon.PRIMARY_TYPE, DnaLexicon.RESOURCE).and(JcrLexicon.DATA,
                                                                                                                TEST_CONTENT.getBytes()).orReplace().and();
        kind = remoteRepos.checkPath("testNode1", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));

        kind = remoteRepos.checkPath("testNode1/testNode10", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));
        
        kind = remoteRepos.checkPath("testNode1/testNode10/testItem0", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testNode1/testNode10/testItem0", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.delete("/testNode1/testNode10/testItem0/jcr:content");

        kind = remoteRepos.checkPath("testNode1/testNode10/testItem0", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testNode1/testNode10/testItem0", -1, fileProperties, baos);
        
        assertEmptyContents(baos, TEST_CONTENT);
    }

    protected void assertContents( ByteArrayOutputStream baos,
                                   String contents ) {
        assertThat(baos, notNullValue());
        assertThat(baos.toString(), is(contents));
    }
    
    protected void assertEmptyContents( ByteArrayOutputStream baos,
                                   String contents ) {
        assertThat(baos, notNullValue());
        assertThat(baos.toString(), not(contents));
    }
}
