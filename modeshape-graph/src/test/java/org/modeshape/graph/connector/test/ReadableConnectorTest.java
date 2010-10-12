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
package org.modeshape.graph.connector.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.CacheableRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.graph.request.ReadNextBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.ReadPropertyRequest;

/**
 * A class that provides standard reading verification tests for connectors. This class is designed to be extended for each
 * connector, and in each subclass the {@link #setUpSource()} method is defined to provide a valid {@link RepositorySource} for
 * the connector to be tested.
 * <p>
 * Since these tests only use methods that never modify repository content, the repository is set up only once (before the first
 * test) and is shut down after all tests have completed.
 * </p>
 */
public abstract class ReadableConnectorTest extends AbstractConnectorTest {

    /**
     * Method that is executed after each test. This method does nothing, since the repository is set up once for all of the tests
     * and then shutdown after all tests have completed.
     * 
     * @throws Exception
     */
    @Override
    @After
    public void afterEach() throws Exception {
        // Don't shut down the repository
    }

    @Test
    public void shouldAlwaysBeAbleToReadRootNode() {
        Node root = graph.getNodeAt("/");
        assertThat("Connector must always have a root node", root, is(notNullValue()));
        assertThat("Root node must always have a path", root.getLocation().hasPath(), is(true));
        assertThat("Root node must never have a null path", root.getLocation().getPath(), is(notNullValue()));
        assertThat("Root node's path must be the root path", root.getLocation().getPath().isRoot(), is(true));
        List<Property> idProperties = root.getLocation().getIdProperties();
        if (idProperties == null) {
            assertThat(root.getLocation().hasIdProperties(), is(false));
        } else {
            assertThat(root.getLocation().hasIdProperties(), is(true));
        }
    }

    @Test
    public void shouldReturnEquivalentLocationForRootUponRepeatedCalls() {
        Node root = graph.getNodeAt("/");
        for (int i = 0; i != 10; ++i) {
            Node anotherRoot = graph.getNodeAt("/");
            assertThat(anotherRoot.getLocation().isSame(root.getLocation()), is(true));
            assertThat(anotherRoot.getLocation().getPath(), is(root.getLocation().getPath()));
            assertThat(anotherRoot.getLocation().getIdProperties(), is(root.getLocation().getIdProperties()));
        }
    }

    @Test
    public void shouldFindRootByIdentificationProperties() {
        Node root = graph.getNodeAt("/");
        Location rootLocation = root.getLocation();
        if (rootLocation.hasIdProperties()) {
            List<Property> idProperties = rootLocation.getIdProperties();
            assertThat("Root node's ID properties was null when there were supposed to be properties",
                       idProperties,
                       is(notNullValue()));
            Property firstProperty = idProperties.get(0);
            Property[] additionalProperties = new Property[] {};
            if (idProperties.size() > 1) {
                List<Property> morePropertiesList = idProperties.subList(1, idProperties.size());
                assertThat(morePropertiesList.isEmpty(), is(false));
                additionalProperties = morePropertiesList.toArray(new Property[morePropertiesList.size()]);
            }
            // Find the root node using the identification properties ...
            Node anotherRoot = graph.getNodeAt(firstProperty, additionalProperties);
            assertThat(anotherRoot.getLocation().isSame(root.getLocation()), is(true));
            assertThat(anotherRoot.getLocation().getPath(), is(root.getLocation().getPath()));
            assertThat(anotherRoot.getLocation().getIdProperties(), is(root.getLocation().getIdProperties()));
        }
    }

    @Test
    public void shouldFindRootByUUID() {
        Node root = graph.getNodeAt("/");
        Location rootLocation = root.getLocation();
        UUID uuid = rootLocation.getUuid();
        if (uuid != null) {
            // Find the root node using the identification properties ...
            Node anotherRoot = graph.getNodeAt(uuid);
            assertThat(anotherRoot.getLocation().isSame(root.getLocation()), is(true));
            assertThat(anotherRoot.getLocation().getPath(), is(root.getLocation().getPath()));
            assertThat(anotherRoot.getLocation().getIdProperties(), is(root.getLocation().getIdProperties()));
            assertThat(anotherRoot.getLocation().getUuid(), is(root.getLocation().getUuid()));
        }
    }

    @Test
    public void shouldReadTheChildrenOfTheRootNode() {
        List<Location> children = graph.getChildren().of("/");
        assertThat(children, is(notNullValue()));
        for (Location child : children) {
            // Check the location has a path that has the root as a parent ...
            assertThat(child.hasPath(), is(true));
            assertThat(child.getPath().getParent().isRoot(), is(true));

            // Verify that each node can be read multiple ways ...
            readNodeThoroughly(child);
        }
    }

    @Ignore
    @Test
    public void shouldReadSubgraphStartingAtRootAndWithMaximumDepthOfThree() {
        Subgraph subgraph = graph.getSubgraphOfDepth(3).at("/");
        assertThat(subgraph, is(notNullValue()));

        // Verify that the root node is the same as getting it directly ...
        Node root = subgraph.getRoot();
        assertSameNode(root, graph.getNodeAt("/"));

        // Verify the first-level children ...
        List<Location> children = graph.getChildren().of("/");
        assertThat(children, is(notNullValue()));
        for (Location childLocation : children) {
            // Verify the child in the subgraph matches the same node obtained directly from the graph ...
            Node child = subgraph.getNode(childLocation);
            assertSameNode(child, graph.getNodeAt(childLocation));

            // Now get the second-level children ...
            List<Location> grandChildren = graph.getChildren().of(childLocation);
            assertThat(grandChildren, is(notNullValue()));
            for (Location grandchildLocation : grandChildren) {
                // Verify the grandchild in the subgraph matches the same node obtained directly from the graph ...
                Node grandchild = subgraph.getNode(grandchildLocation);
                assertSameNode(grandchild, graph.getNodeAt(grandchildLocation));

                // The subgraph should contain the children locations and properties for the grandchildren.
                // However, the subgraph should not a node for the children of the grandchildren ...
                for (Location greatGrandchild : grandchild.getChildren()) {
                    assertThat(subgraph.getNode(greatGrandchild), is(nullValue()));
                }
            }
        }
    }

    @Test
    public void shouldReadIndividualPropertiesOfNodes() {
        // Read each node that is a child of the root...
        for (Location childLocation : graph.getChildren().of("/")) {
            // For each node ...
            Node child = graph.getNodeAt(childLocation);
            for (Property property : child.getProperties()) {
                Name name = property.getName();
                // Re-read the property and verify the value(s) match the value(s) in 'property'
                Property singleProperty = graph.getProperty(name).on(childLocation);
                assertThat(singleProperty, is(notNullValue()));
                assertThat(singleProperty, is(property));
            }
        }
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReadNodeThatDoesNotExist() {
        // Look up the child that should not exist, and this should throw an exception ...
        Path nonExistantChildName = findPathToNonExistentNodeUnder("/");
        Object node = graph.getNodeAt(nonExistantChildName);
        print(node.toString());
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReadPropertyOnNodeThatDoesNotExist() {
        // Look up the child that should not exist, and this should throw an exception ...
        Path nonExistantChildName = findPathToNonExistentNodeUnder("/");
        graph.getProperty("jcr:uuid").on(nonExistantChildName);
    }

    @Test
    public void shouldFailToReadPropertyThatDoesNotExistOnExistingNode() {
        // Read each node that is a child of the root...
        for (Location childLocation : graph.getChildren().of("/")) {
            // Find a name for a non-existing property ...
            String nonExistentPropertyName = "ab39dbyfg739_adf7bg";
            // For each node ...
            Property property = graph.getProperty(nonExistentPropertyName).on(childLocation); // will throw exception
            assertThat(property, is(nullValue()));
        }
    }

    @Test
    public void shouldIncludeTimeLoadedInReadNodeRequests() {
        String workspaceName = graph.getCurrentWorkspaceName();
        // Don't use the graph so that we can obtain and interrogate the request ...
        CacheableRequest request = new ReadNodeRequest(location("/"), workspaceName);
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadAllPropertiesRequests() {
        String workspaceName = graph.getCurrentWorkspaceName();
        // Don't use the graph so that we can obtain and interrogate the request ...
        CacheableRequest request = new ReadAllPropertiesRequest(location("/"), workspaceName);
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadAllChildrenRequests() {
        String workspaceName = graph.getCurrentWorkspaceName();
        // Don't use the graph so that we can obtain and interrogate the request ...
        CacheableRequest request = new ReadAllChildrenRequest(location("/"), workspaceName);
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadBlockOfChildrenRequests() {
        String workspaceName = graph.getCurrentWorkspaceName();
        // Don't use the graph so that we can obtain and interrogate the request ...
        CacheableRequest request = new ReadBlockOfChildrenRequest(location("/"), workspaceName, 0, 100);
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadNextBlockOfChildrenRequests() {
        // Get the first child ...
        String workspaceName = graph.getCurrentWorkspaceName();
        Location firstChild = graph.getChildren().of("/").get(0);
        // Don't use the graph so that we can obtain and interrogate the request ...
        CacheableRequest request = new ReadNextBlockOfChildrenRequest(firstChild, workspaceName, 100);
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldIncludeTimeLoadedInReadPropertyRequests() {
        // Get each of the properties on the first child ...
        String workspaceName = graph.getCurrentWorkspaceName();
        Location firstChildLocation = graph.getChildren().of("/").get(0);
        Node firstChild = graph.getNodeAt(firstChildLocation);
        // Don't use the graph so that we can obtain and interrogate the request ...
        for (Property property : firstChild.getProperties()) {
            CacheableRequest request = new ReadPropertyRequest(firstChildLocation, workspaceName, property.getName());
            execute(request);
            assertThat(request.getTimeLoaded(), is(notNullValue()));
        }
    }

    @Test
    public void shouldIncludeTimeLoadedInReadBranchRequests() {
        String workspaceName = graph.getCurrentWorkspaceName();
        // Don't use the graph so that we can obtain and interrogate the request ...
        CacheableRequest request = new ReadBranchRequest(location("/"), workspaceName, 2);
        execute(request);
        assertThat(request.getTimeLoaded(), is(notNullValue()));
    }

    @Test
    public void shouldReturnSameStructureForRepeatedReadBranchRequests() {
        // Verify that the content doesn't change
        Location root = graph.getCurrentWorkspace().getRoot();
        Subgraph subgraph1 = graph.getSubgraphOfDepth(10).at(root);
        for (int i = 0; i != 4; ++i) {
            Subgraph subgraph2 = graph.getSubgraphOfDepth(10).at(root);
            assertEquivalentSubgraphs(subgraph1, subgraph2, true, true);
        }
    }
}
