/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;

public class FederationConfigurationTest extends SingleUseAbstractTest {

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

        // Add a node directly under 'generated-sources' (which is a federated node)...
        Node sources = federation.getNode("generated-sources");
        for (int i = 0; i != 3; ++i) {
            Node newNode = sources.addNode("GeneratedFolder_" + UUID.randomUUID().toString(), "nt:folder");
            assertThat(newNode, is(notNullValue()));
            session.save();
        }

        // Add a node directly under 'generated-sources/annotations' (which is a federated node)...
        Node annotations = federation.getNode("generated-sources/annotations");
        for (int i = 0; i != 3; ++i) {
            Node newNode = annotations.addNode("GeneratedFolder_" + UUID.randomUUID().toString(), "nt:folder");
            assertThat(newNode, is(notNullValue()));
            session.save();
        }
    }

    @Test
    public void shouldIgnorePreconfiguredProjectionIfProjectedPathPointsTowardsInternalNode() throws Exception {
        startRepositoryWithConfiguration(resource("config/repo-config-filesystem-federation-invalid-alias.json"));
        //check that there only the internal node + 1 child and that the projection has not been created
        Session session = session();
        Node federation = session.getNode("/federation");
        assertEquals(1, federation.getNodes().getSize());
        assertNotNull(session.getNode("/federation/fs"));
    }

    @Override
    protected boolean startRepositoryAutomatically() {
        return false;
    }
}
