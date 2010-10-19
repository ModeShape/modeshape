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
package org.modeshape.connector.svn;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrMixLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Graph.Batch;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.test.AbstractConnectorTest;
import org.modeshape.graph.property.PathNotFoundException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.io.SVNRepository;

/**
 * @author Serge Pagop
 */
public class SvnRepositoryConnectorWritableTest extends AbstractConnectorTest {

    protected static final String EMPTY_CONTENT = "";
    protected static final String TEST_CONTENT = "Test content";
    protected SVNRepository remoteRepos = null;
    protected String url;
    protected SVNNodeKind kind = null;
    protected SVNProperties fileProperties = null;
    protected ByteArrayOutputStream baos = null;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws Exception {
        url = SvnConnectorTestUtil.createURL("src/test/resources/dummy_svn_repos", "target/copy_of dummy_svn_repos");
        String[] predefinedWorkspaceNames = new String[] {"trunk", "tags"};
        SvnRepositorySource source = new SvnRepositorySource();
        source.setName("Test Repository");
        source.setUsername("sp");
        source.setPassword("");
        source.setRepositoryRootUrl(url);
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(Boolean.TRUE);
        source.setUpdatesAllowed(true);

        remoteRepos = SvnConnectorTestUtil.createRepository(url + "/trunk", "sp", "");

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) throws Exception {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#afterEach()
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
        graph.create("/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();
        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.create("/testFile1").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFile1/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();
        kind = remoteRepos.checkPath("testFile1", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile1", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        // LEVEL 1
        graph.create("/root/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/root/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();
        kind = remoteRepos.checkPath("root/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("root/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        // LEVEL 2
        graph.create("/root/a/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/root/a/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();
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
        graph.create("/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();

        graph.create("/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, "Should not overwrite".getBytes())
             .ifAbsent()
             .and();

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
        graph.create("/testFolder/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();

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
        graph.create("/root/testFolder/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();

        kind = remoteRepos.checkPath("root/testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("root/testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);
    }

    @Test
    public void shouldBeAbleToCopyFile() throws Exception {
        graph.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();

        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.copy("/testFile").to("/copiedFile");
        kind = remoteRepos.checkPath("copiedFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("copiedFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);
    }

    @Test
    public void shouldBeAbleToCopyFolder() throws Exception {
        graph.create("/testFolder").orReplace().and();
        graph.create("/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFolder/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();

        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.copy("/testFolder").to("/copiedFolder");
        kind = remoteRepos.checkPath("copiedFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("copiedFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);
    }

    @Test
    public void shouldBeAbleToMoveFile() throws Exception {
        graph.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();

        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.move("/testFile").as("newFile").into("/").and();
        kind = remoteRepos.checkPath("newFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("newFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        try {
            graph.getNodeAt("/testFile");
            fail("Old copy of file still exists at source location");
        } catch (PathNotFoundException expected) {

        }
    }

    @Test
    public void shouldBeAbleToMoveFolder() throws Exception {
        graph.create("/testFolder").orReplace().and();
        graph.create("/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFolder/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();

        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind == SVNNodeKind.FILE, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.move("/testFolder").as("newFolder").into("/").and();
        kind = remoteRepos.checkPath("newFolder", -1);
        assertThat(kind == SVNNodeKind.DIR, is(Boolean.TRUE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("newFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);
        try {
            graph.getNodeAt("/testFolder");
            fail("Old copy of file still exists at source location");
        } catch (PathNotFoundException expected) {

        }

    }

    @Test
    public void shouldBeAbleToDeleteFolder() throws Exception {
        graph.create("/testFolder").orReplace().and();
        graph.create("/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFolder/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();
        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind, is(SVNNodeKind.FILE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.getNodeAt("/testFolder");

        graph.delete("/testFolder").and();

        try {
            graph.getNodeAt("/testFolder");
        } catch (PathNotFoundException expected) {
            // Expected
        }
    }

    @Test
    public void shouldBeAbleToDeleteFile() throws Exception {
        graph.create("/testFolder").orReplace().and();
        graph.create("/testFolder/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFolder/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();
        kind = remoteRepos.checkPath("testFolder/testFile", -1);
        assertThat(kind, is(SVNNodeKind.FILE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFolder/testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.getNodeAt("/testFolder/testFile");

        graph.delete("/testFolder/testFile").and();

        try {
            graph.getNodeAt("/testFolder/testFile");
        } catch (PathNotFoundException expected) {
            // Expected
        }
    }

    @Test
    public void shouldBeAbleToClearFileByRemovingDataProperty() throws Exception {
        graph.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        graph.create("/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();
        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind, is(SVNNodeKind.FILE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);
        assertContents(baos, TEST_CONTENT);

        graph.remove("jcr:data").on("/testFile/jcr:content").and();

        kind = remoteRepos.checkPath("testFile", -1);
        assertThat(kind, is(SVNNodeKind.FILE));
        fileProperties = new SVNProperties();
        baos = new ByteArrayOutputStream();
        remoteRepos.getFile("testFile", -1, fileProperties, baos);
        assertContents(baos, "");
    }

    @Test
    public void shouldBeAllOrNothing() {
        String badTestFileName = "/missingDirectory/someFile";

        Batch batch = graph.batch();

        batch.create("/testFile").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FILE).orReplace().and();
        batch.create("/testFile/jcr:content")
             .with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.RESOURCE)
             .and(JcrLexicon.DATA, TEST_CONTENT.getBytes())
             .orReplace()
             .and();
        batch.create(badTestFileName).and();

        try {
            batch.execute();
            fail("The invalid test file name (" + badTestFileName + ") did not fail");
        } catch (PathNotFoundException rse) {
            // Expected
        }

        try {
            graph.getNodeAt("/testFile");
            fail("Got node at /testFile -- whole transaction should have failed");
        } catch (PathNotFoundException expected) {
            // Expected
        }
    }

    @Test
    public void runItTwice() {
        shouldBeAllOrNothing();
        shouldBeAllOrNothing();
        shouldBeAllOrNothing();
    }

    protected void assertContents( ByteArrayOutputStream baos,
                                   String contents ) {
        assertThat(baos, notNullValue());
        assertThat(baos.toString(), is(contents));
    }
}
