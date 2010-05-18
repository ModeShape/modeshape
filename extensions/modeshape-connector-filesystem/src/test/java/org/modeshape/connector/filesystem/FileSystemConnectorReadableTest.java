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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.List;
import org.junit.Test;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.ReadableConnectorTest;
import org.modeshape.graph.property.PathNotFoundException;

/**
 * @author Randall Hauch
 */
public class FileSystemConnectorReadableTest extends ReadableConnectorTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        // Set the connection properties to be use the content of "./src/test/resources/repositories" as a repository ...
        String path = "./src/test/resources/repositories/";
        String[] predefinedWorkspaceNames = new String[] {path + "airplanes", path + "cars"};
        FileSystemSource source = new FileSystemSource();
        source.setName("Test Repository");
        source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        source.setCreatingWorkspacesAllowed(true);

        return source;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#initializeContent(org.modeshape.graph.Graph)
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
        Node commercial = graph.getNodeAt("/commercial");
        assertThatNodeIsFolder(commercial);

        Node readme = graph.getNodeAt("/commercial/Boeing_777.jpg");
        assertThatNodeIsFile(readme, "image/jpeg", null);
    }

    @Test
    public void shouldBeAbleToExcludePattern() {
        ((FileSystemSource)source).setExclusionPattern("emptyfile.txt");

        try {
            graph.getNodeAt("/emptyfile.txt");
            fail("Should not exist");
        } catch (PathNotFoundException pnfe) {
            // expected
        }

        ((FileSystemSource)source).setExclusionPattern(null);
    }

}
