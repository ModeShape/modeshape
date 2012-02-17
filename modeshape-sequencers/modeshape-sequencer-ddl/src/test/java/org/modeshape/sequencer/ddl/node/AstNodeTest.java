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
package org.modeshape.sequencer.ddl.node;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class AstNodeTest {

    private AstNode node;
    private AstNode parent;

    @Before
    public void beforeEach() {
        node = new AstNode("node1");
    }

    @After
    public void afterEach() {
        this.node = null;
        this.parent = null;
    }

    @Test
    public void shouldCreatePlanNodeWithNameAndNoParent() {
        String name = "something";
        node = new AstNode(name);
        assertThat(node.getName(), is(name));
        assertThat(node.getParent(), is(nullValue()));
    }

    @Test
    public void shouldCreatePlanNodeWithNameAndParent() {
        String name = "something";
        parent = new AstNode("parent");
        node = new AstNode(parent, name);
        assertThat(node.getName(), is(name));
        assertThat(node.getParent(), is(sameInstance(parent)));
        assertThat(parent.getFirstChild(), is(sameInstance(node)));
        assertThat(parent.getChildCount(), is(1));
    }

    @Test
    public void shouldGetFirstChildAndLastChildWithOneChild() {
        parent = new AstNode("parent");
        node = new AstNode(parent, "child");
        assertThat(parent.getFirstChild(), is(sameInstance(node)));
        assertThat(parent.getLastChild(), is(sameInstance(node)));
    }

    @Test
    public void shouldGetFirstChildAndLastChildWithTwoChildren() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        assertThat(parent.getFirstChild(), is(sameInstance(child1)));
        assertThat(parent.getLastChild(), is(sameInstance(child2)));
    }

    @Test
    public void shouldGetFirstChildAndLastChildWithMoreThanTwoChildren() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        assertThat(parent.getFirstChild(), is(sameInstance(child1)));
        assertThat(parent.getLastChild(), is(sameInstance(child3)));
    }

    @Test
    public void shouldGetFirstChildAndLastChildWithNoChildren() {
        parent = new AstNode("parent");
        assertThat(parent.getFirstChild(), is(nullValue()));
        assertThat(parent.getLastChild(), is(nullValue()));
    }

    @Test
    public void shouldRemoveNodeFromExistingParentWhenSettingParentToNull() {
        parent = new AstNode("parent");
        node = new AstNode(parent, "child");
        assertThat(parent.getFirstChild(), is(sameInstance(node)));
        assertThat(parent.getChildCount(), is(1));
        node.setParent(null);
        assertThat(parent.getChildCount(), is(0));
        assertThat(node.getParent(), is(nullValue()));
    }

    @Test
    public void shouldInsertNewParentNodeInBetweenExistingParentAndChild() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        assertThat(parent.getFirstChild(), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getLastChild(), is(sameInstance(child3)));
        assertThat(parent.getChildCount(), is(3));
        node = new AstNode("inserted");
        child2.insertAsParent(node);
        assertThat(parent.getChildCount(), is(3));
        assertThat(parent.getFirstChild(), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(node)));
        assertThat(parent.getLastChild(), is(sameInstance(child3)));
        assertThat(node.getParent(), is(sameInstance(parent)));
        assertThat(child2.getParent(), is(sameInstance(node)));
    }

    @Test
    public void shouldInsertNewParentNodeInAboveNodeWithoutParent() {
        AstNode child1 = new AstNode("childA");
        node = new AstNode("node");
        AstNode nodeChild = new AstNode(node, "child");
        // Perform the insertAsParent ...
        child1.insertAsParent(node);
        assertThat(node.getParent(), is(nullValue()));
        assertThat(node.getChildCount(), is(2));
        assertThat(node.getFirstChild(), is(sameInstance(nodeChild)));
        assertThat(node.getLastChild(), is(sameInstance(child1)));
        assertThat(child1.getParent(), is(sameInstance(node)));
    }

    @Test
    public void shouldRemoveFromParentWhenThereIsAParent() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        AstNode grandChild21 = new AstNode(child2, "grandChild21");
        assertThat(parent.getFirstChild(), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getLastChild(), is(sameInstance(child3)));
        assertThat(parent.getChildCount(), is(3));
        assertThat(child2.getFirstChild(), is(sameInstance(grandChild21)));
        // Perform the removeFromParent ...
        assertThat(child2.removeFromParent(), is(sameInstance(parent)));
        assertThat(parent.getFirstChild(), is(sameInstance(child1)));
        assertThat(parent.getLastChild(), is(sameInstance(child3)));
        assertThat(parent.getChildCount(), is(2));
        // There should still be the child in the removed node ...
        assertThat(child2.getFirstChild(), is(sameInstance(grandChild21)));
    }

    @Test
    public void shouldRemoveFromParentWhenThereIsNoParent() {
        node = new AstNode("node");
        AstNode child1 = new AstNode(node, "child");
        assertThat(node.getFirstChild(), is(sameInstance(child1)));
        assertThat(node.getChildCount(), is(1));
        // Perform the removeFromParent ...
        assertThat(node.removeFromParent(), is(nullValue()));
        assertThat(node.getFirstChild(), is(sameInstance(child1)));
        assertThat(node.getChildCount(), is(1));
    }

    @Test
    public void shouldReturnListOfChildren() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        List<AstNode> children = parent.getChildren();
        assertThat(children.get(0), is(sameInstance(child1)));
        assertThat(children.get(1), is(sameInstance(child2)));
        assertThat(children.get(2), is(sameInstance(child3)));
        assertThat(children.size(), is(3));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnImmutableListOfChildren() {
        parent = new AstNode("parent");
        new AstNode(parent, "childA");
        new AstNode(parent, "childB");
        new AstNode(parent, "childC");
        parent.getChildren().clear();
    }

    @Test
    public void shouldReturnIteratorOfChildren() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        Iterator<AstNode> children = parent.iterator();
        assertThat(children.next(), is(sameInstance(child1)));
        assertThat(children.next(), is(sameInstance(child2)));
        assertThat(children.next(), is(sameInstance(child3)));
        assertThat(children.hasNext(), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnImmutableIteratorOfChildren() {
        parent = new AstNode("parent");
        new AstNode(parent, "childA");
        new AstNode(parent, "childB");
        new AstNode(parent, "childC");
        Iterator<AstNode> iter = parent.iterator();
        iter.next();
        iter.remove();
    }

    @Test
    public void shouldRemoveAllChildrenOfParentWithNoChildrenByReturningEmptyList() {
        parent = new AstNode("parent");
        // Perform the remove, and verify the list has all the children ...
        List<AstNode> children = parent.removeAllChildren();
        assertThat(children.size(), is(0));
        assertThat(parent.getChildCount(), is(0));
        // Add a new child to the parent ...
        AstNode child1a = new AstNode(parent, "child1A");
        assertThat(parent.getFirstChild(), is(sameInstance(child1a)));
        // The returned copy should not be modified ...
        assertThat(children.size(), is(0));
    }

    @Test
    public void shouldRemoveAllChildrenAndReturnCopyOfListOfChildren() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        // Perform the remove, and verify the list has all the children ...
        List<AstNode> children = parent.removeAllChildren();
        assertThat(children.get(0), is(sameInstance(child1)));
        assertThat(children.get(1), is(sameInstance(child2)));
        assertThat(children.get(2), is(sameInstance(child3)));
        assertThat(children.size(), is(3));
        assertThat(parent.getChildCount(), is(0));
        // Add a new child to the parent ...
        AstNode child1a = new AstNode(parent, "child1A");
        assertThat(parent.getFirstChild(), is(sameInstance(child1a)));
        // The returned copy should not be modified ...
        assertThat(children.get(0), is(sameInstance(child1)));
        assertThat(children.get(1), is(sameInstance(child2)));
        assertThat(children.get(2), is(sameInstance(child3)));
        assertThat(children.size(), is(3));
    }

    @Test
    public void shouldReturnCorrectChildCount() {
        parent = new AstNode("parent");
        assertThat(parent.getChildCount(), is(0));
        for (int i = 0; i != 10; ++i) {
            new AstNode(parent, "child");
            assertThat(parent.getChildCount(), is(i + 1));
        }
    }

    @Test
    public void shouldAddChildrenAtEnd() {
        parent = new AstNode("parent");
        List<AstNode> children = new ArrayList<AstNode>();
        children.add(new AstNode(parent, "child"));
        children.add(new AstNode(parent, "child"));
        children.add(new AstNode(parent, "child"));
        parent.addChildren(children);
        int index = 0;
        for (AstNode child : children) {
            assertThat(parent.getChild(index++), is(sameInstance(child)));
        }
    }

    @Test
    public void shouldRemoveChild() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        // Perform the remove, and verify children have changed ...
        assertThat(parent.removeChild(child2), is(true));
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child3)));
    }

    @Test
    public void shouldNotRemoveChildIfNotReallyAChild() {
        node = new AstNode("node");
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        // Try to remove the non-child, and verify the children have no changed ...
        assertThat(parent.removeChild(node), is(false));
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
    }

    @Test
    public void shouldNotRemoveChildIfReferenceIsNull() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        // Try to remove the non-child, and verify the children have no changed ...
        assertThat(parent.removeChild(null), is(false));
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
    }

    @Test
    public void shouldExtractChildByRemovingIfChildHasNoChildren() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        // Perform the extraction ...
        parent.extractChild(child2);
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child3)));
        assertThat(parent.getChildCount(), is(2));
    }

    @Test
    public void shouldExtractChildByReplacingWithFirstGrandchild() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        AstNode grandChild1 = new AstNode(child2, "grandchildA");
        AstNode grandChild2 = new AstNode(child2, "grandchildB");
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        // Perform the extraction ...
        parent.extractChild(child2);
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(grandChild1)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        assertThat(parent.getChildCount(), is(3));
        // The old child should still contain just the remaining child(ren) ...
        assertThat(child2.getFirstChild(), is(sameInstance(grandChild2)));
        assertThat(child2.getParent(), is(nullValue()));
    }

    @Test
    public void shouldReplaceChild() {
        AstNode parentOfReplacement = new AstNode("parentOfReplacement");
        AstNode replacement = new AstNode(parentOfReplacement, "replacement");
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        // Perform the replacement ...
        assertThat(parent.replaceChild(child2, replacement), is(true));
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(replacement)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        assertThat(replacement.getParent(), is(sameInstance(parent)));
        assertThat(child1.getParent(), is(sameInstance(parent)));
        assertThat(child2.getParent(), is(nullValue()));
        assertThat(child3.getParent(), is(sameInstance(parent)));
        // The replacement should no longer be a child of its former parent ...
        assertThat(parentOfReplacement.getChildCount(), is(0));
    }

    @Test
    public void shouldReplaceChildWithAnotherChildToSwapPositions() {
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        // Perform the replacement ...
        assertThat(parent.replaceChild(child2, child3), is(true));
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child3)));
        assertThat(parent.getChild(2), is(sameInstance(child2)));
        assertThat(child1.getParent(), is(sameInstance(parent)));
        assertThat(child2.getParent(), is(sameInstance(parent)));
        assertThat(child3.getParent(), is(sameInstance(parent)));
    }

    @Test
    public void shouldNotReplaceChildIfChildNodeIsNotReallyAChild() {
        AstNode nonChild = new AstNode("nonChild");
        AstNode replacement = new AstNode("replacement");
        parent = new AstNode("parent");
        AstNode child1 = new AstNode(parent, "childA");
        AstNode child2 = new AstNode(parent, "childB");
        AstNode child3 = new AstNode(parent, "childC");
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        assertThat(parent.replaceChild(nonChild, replacement), is(false));
    }

    @Test
    public void shouldReturnPath() {
        AstNode root = new AstNode("root");
        AstNode node1 = new AstNode(root, "node1");
        AstNode node2 = new AstNode(node1, "node2");
        AstNode node3 = new AstNode(node2, "node3");
        AstNode node4 = new AstNode(node3, "node4");
        AstNode node5 = new AstNode(node4, "node5");
        node4.setProperty("prop1", "value1");
        assertThat(root.getAbsolutePath(), is("/root"));
        assertThat(node1.getAbsolutePath(), is("/root/node1"));
        assertThat(node2.getAbsolutePath(), is("/root/node1/node2"));
        assertThat(node3.getAbsolutePath(), is("/root/node1/node2/node3"));
        assertThat(node4.getAbsolutePath(), is("/root/node1/node2/node3/node4"));
        assertThat(node5.getAbsolutePath(), is("/root/node1/node2/node3/node4/node5"));
    }
}
