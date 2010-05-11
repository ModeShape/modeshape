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
package org.modeshape.graph.connector.base;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;

public class StandardMapWorkspaceTest {

    private ExecutionContext context;
    private Map<UUID, MapNode> store;
    private MapWorkspace<MapNode> workspace;
    private UUID rootUuid;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();
        store = new HashMap<UUID, MapNode>();
        rootUuid = UUID.randomUUID();
        workspace = new StandardMapWorkspace<MapNode>("workspace", store, new MapNode(rootUuid));
    }

    @Test
    public void shouldHaveName() {
        assertThat(workspace.getName(), is("workspace"));
    }

    @Test
    public void shouldHaveRootNode() {
        assertThat(workspace.getRootNode(), is(notNullValue()));
        assertThat(workspace.getRootNode().getUuid(), is(rootUuid));
    }

    @Test
    public void shouldPlaceRootNodeIntoStoreIfNoExistingNode() {
        assertThat(store.get(rootUuid), is(notNullValue()));
        assertThat(store.get(rootUuid).getUuid(), is(rootUuid));
        assertThat(store.get(rootUuid), is(sameInstance(workspace.getRootNode())));
    }

    @Test
    public void shouldNotReplaceRootNodeInStoreIfExistingNode() {
        MapNode root = new MapNode(rootUuid);
        store.clear();
        store.put(rootUuid, root);
        workspace = new StandardMapWorkspace<MapNode>("workspace", store, new MapNode(rootUuid));
        assertStored(root);
    }

    @Test
    public void shouldAddNodes() {
        assertStored(addNodes(10));
    }

    @Test
    public void shouldReplaceExistingNodes() {
        MapNode[] original = addNodes(10);
        assertStored(original);
        MapNode[] modified = addProperty(original, "foo", "value1");
        assertStored(modified);
    }

    @Test
    public void shouldRemoveNodes() {
        for (MapNode node : addNodes(10)) {
            assertThat(workspace.removeNode(node.getUuid()), is(sameInstance(node)));
        }
        assertThat(store.size(), is(1));
        assertThat(store.get(rootUuid), is(sameInstance(workspace.getRootNode())));
    }

    @Test
    public void shouldNotRemoveRootNode() {
        assertThat(workspace.removeNode(rootUuid), is(nullValue()));
        assertThat(store.containsKey(rootUuid), is(true));
        assertThat(workspace.getRootNode(), is(sameInstance(store.get(rootUuid))));
    }

    @Test
    public void shouldRemoveAllNodesExceptTheRootNode() {
        MapNode[] nodes = addProperty(addNodes(10), "foo", "value1");
        MapNode root = addProperty(workspace.getRootNode(), "bar", "value2");
        assertThat(store.size(), is(11));
        assertStored(root);
        assertStored(nodes);
        // Now remove all nodes ...
        workspace.removeAll();
        // Verify there is only one node left, and it has no children or (non-UUID) properties ...
        assertThat(store.size(), is(1));
        assertThat(workspace.getRootNode(), is(sameInstance(store.get(rootUuid))));
        assertThat(workspace.getRootNode(), is(not(sameInstance(root))));
        assertThat(workspace.getRootNode().getChildren().isEmpty(), is(true));
        assertThat(workspace.getRootNode().getProperties().isEmpty(), is(true));
    }

    protected MapNode[] addNodes( int number ) {
        MapNode[] results = new MapNode[number];
        for (int i = 0; i != number; ++i) {
            results[i] = new MapNode(UUID.randomUUID());
            workspace.putNode(results[i]);
            assertThat(store.get(results[i].getUuid()), is(sameInstance(results[i])));
        }
        return results;
    }

    protected void assertStored( MapNode... nodes ) {
        for (MapNode node : nodes) {
            assertThat(store.get(node.getUuid()), is(sameInstance(node)));
            assertThat(store.get(node.getUuid()), is(sameInstance(workspace.getNode(node.getUuid()))));
        }
    }

    protected MapNode addProperty( MapNode original,
                                      String name,
                                      Object... values ) {
        MapNode newNode = original.withProperty(property(name, values));
        assertThat(workspace.putNode(newNode), is(sameInstance(original)));
        return newNode;
    }

    protected MapNode[] addProperty( MapNode[] nodes,
                                        String name,
                                        Object... values ) {
        MapNode[] results = new MapNode[nodes.length];
        int i = 0;
        for (MapNode original : nodes) {
            results[i++] = addProperty(original, name, values);
        }
        return results;
    }

    protected Property property( String name,
                                 Object... values ) {
        return context.getPropertyFactory().create(name(name), values);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

}
