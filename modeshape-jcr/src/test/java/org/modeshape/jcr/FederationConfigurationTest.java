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

import java.io.InputStream;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class FederationConfigurationTest extends SingleUseAbstractTest {

    protected InputStream resource( String path ) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        assertThat(stream, is(notNullValue()));
        return stream;
    }

    @FixFor( "MODE-1772" )
    @Test
    public void shouldStartRepositoryWithFileSystemConnectorAccessingAncestorOfCacheStoreDirectory() throws Exception {
        // Clean up and create some initial files ...
        FileUtil.delete("target/federation_persistent_repository");

        startRepositoryWithConfiguration(resource("config/repo-config-filesystem-federation-with-persistence.json"));
        Session session = session();
        Node federation = session.getNode("/federation");

        // Get the children under federation ...
        NodeIterator iter = federation.getNodes();
        while (iter.hasNext()) {
            Node child = iter.nextNode();
            assertThat(child, is(notNullValue()));
        }

        // Add a node directly under 'federation' (which is a federated node)...
        for (int i = 0; i != 3; ++i) {
            Node newNode = federation.addNode("Node");
            assertThat(newNode, is(notNullValue()));
            session.save();
        }

        // Add a node directly under 'federation/classes/org' (which is a federated node)...
        Node org = federation.getNode("classes/org");
        for (int i = 0; i != 3; ++i) {
            Node newNode = org.addNode("GeneratedFolder_" + UUID.randomUUID().toString(), "nt:folder");
            assertThat(newNode, is(notNullValue()));
            session.save();
        }

        // Add a node directly under 'federation/generated-sources' (which is a federated node)...
        Node generated = federation.getNode("generated-sources");
        for (int i = 0; i != 3; ++i) {
            Node newNode = generated.addNode("GeneratedFolder_" + UUID.randomUUID().toString(), "nt:folder");
            assertThat(newNode, is(notNullValue()));
            session.save();
        }
    }

    @Test( expected = RepositoryException.class )
    @FixFor( "MODE-1798" )
    public void shouldNotAllowPreconfiguredProjectionIfAliasNotProvided() throws Exception {
        //we expect this to fail because there is already an internal node that is supposed to be used as an alias as well
        startRepositoryWithConfiguration(resource("config/repo-config-filesystem-federation-invalid-alias.json"));
    }

    @Test
    @FixFor( "MODE-1802" )
    public void fileSystemSourceShouldSupportRootProjection() throws Exception {
        // Clean up
        FileUtil.delete("target/classes/test");

        startRepositoryWithConfiguration(resource("config/repo-config-filesystem-federation-root-projection.json"));
        Session session = session();
        Node root = session.getNode("/fs");
        assertNotNull(root);
        Node folder1 = root.addNode("test", "nt:folder");
        session.save();
        Node folder2 = root.getNode("test");
        assertThat(folder1.getIdentifier(), is(folder2.getIdentifier()));
    }

    @Override
    protected boolean startRepositoryAutomatically() {
        return false;
    }
}
