/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Node;
import org.jboss.dna.graph.connectors.RepositorySource;
import org.jboss.dna.graph.connectors.test.ReadableConnectorTest;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class FileSystemConnectorReadingTest extends ReadableConnectorTest {

    private String[] pathsInFileSystemToTopLevelNodes;

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() throws IOException {
        // Find the current location of the project ...
        File project = new File(".");
        String absolutePathToProject = project.getCanonicalPath();

        // Set the connection properties to be use the folders in the "./src/test/resources/repositories" as a repository ...
        FileSystemSource source = new FileSystemSource();
        source.setName("Test Repository");

        pathsInFileSystemToTopLevelNodes = new String[] {absolutePathToProject + "/src/test/resources/repositories/airplanes",
            absolutePathToProject + "/src/test/resources/repositories/cars",
            absolutePathToProject + "/src/test/resources/repositories/readme.txt"};
        source.setFileSystemPaths(pathsInFileSystemToTopLevelNodes);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connectors.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
        // No need to initialize any content ...
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

    public void assertThatNodeIsFolder( Node node ) {
        assertThat(node, is(notNullValue()));
        assertThat(node.getProperty(JcrLexicon.PRIMARY_TYPE).getFirstValue(), is((Object)JcrNtLexicon.FOLDER));
    }

    @Test
    public void shouldFindFolderSpecifiedInPathsAsNodesBelowRoot() {
        Node readme = graph.getNodeAt("/readme.txt");
        assertThatNodeIsFile(readme, "text/plain", "This directory contains files and folders that are used in test cases.");

        Node airplanes = graph.getNodeAt("/airplanes");
        assertThatNodeIsFolder(airplanes);

        Node commercial = graph.getNodeAt("/airplanes/commercial");
        assertThatNodeIsFolder(commercial);
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotFindOtherFileThatIsSiblingOfFileOrFoldersSpecifiedToBeTopLevelNodes() {
        // Should not find these, since they're not included in the paths to top-level nodes ...
        assertThat(graph.getNodeAt("/trains"), is(nullValue()));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotFindOtherFolderThatIsSiblingOfFileOrFoldersSpecifiedToBeTopLevelNodes() {
        // Should not find these, since they're not included in the paths to top-level nodes ...
        assertThat(graph.getNodeAt("/DNA icon"), is(nullValue()));
    }
}
