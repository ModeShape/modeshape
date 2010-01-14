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
package org.modeshape.graph.query.plan;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class PlanNodeTest {

    private PlanNode node;
    private PlanNode parent;

    @Before
    public void beforeEach() {
        node = new PlanNode(Type.GROUP);
    }

    @After
    public void afterEach() {
        this.node = null;
        this.parent = null;
    }

    @Test
    public void shouldFindTypeGivenSymbolWithSameCase() {
        for (Type type : Type.values()) {
            assertThat(Type.forSymbol(type.getSymbol()), is(sameInstance(type)));
        }
    }

    @Test
    public void shouldFindTypeGivenSymbolWithLowerCase() {
        for (Type type : Type.values()) {
            assertThat(Type.forSymbol(type.getSymbol().toLowerCase()), is(sameInstance(type)));
        }
    }

    @Test
    public void shouldFindTypeGivenSymbolWithUpperCase() {
        for (Type type : Type.values()) {
            assertThat(Type.forSymbol(type.getSymbol().toUpperCase()), is(sameInstance(type)));
        }
    }

    @Test
    public void shouldFindTypeGivenSymbolWithLeadingAndTrailingWhitespace() {
        for (Type type : Type.values()) {
            assertThat(Type.forSymbol(" \t " + type.getSymbol() + " \t \n"), is(sameInstance(type)));
        }
    }

    @Test
    public void shouldCreatePlanNodeWithTypeAndNoParent() {
        for (Type type : Type.values()) {
            node = new PlanNode(type);
            assertThat(node.getType(), is(type));
            assertThat(node.getParent(), is(nullValue()));
        }
    }

    @Test
    public void shouldCreatePlanNodeWithTypeAndParent() {
        for (Type type : Type.values()) {
            parent = new PlanNode(Type.JOIN);
            node = new PlanNode(type, parent);
            assertThat(node.getType(), is(type));
            assertThat(node.getParent(), is(sameInstance(parent)));
            assertThat(parent.getFirstChild(), is(sameInstance(node)));
            assertThat(parent.getChildCount(), is(1));
        }
    }

    @Test
    public void shouldAddNodeToParentWhenConstructingChildNodeWithTypeAndParent() {
        parent = new PlanNode(Type.JOIN);
        int counter = 0;
        for (Type type : Type.values()) {
            node = new PlanNode(type, parent);
            ++counter;
            assertThat(node.getType(), is(type));
            assertThat(node.getParent(), is(sameInstance(parent)));
            assertThat(parent.getLastChild(), is(sameInstance(node)));
            assertThat(parent.getChildCount(), is(counter));
        }
    }

    @Test
    public void shouldSetType() {
        node = new PlanNode(Type.JOIN);
        for (Type type : Type.values()) {
            node.setType(type);
            assertThat(node.getType(), is(type));
        }
    }

    @Test
    public void shouldGetFirstChildAndLastChildWithOneChild() {
        parent = new PlanNode(Type.JOIN);
        node = new PlanNode(Type.ACCESS, parent);
        assertThat(parent.getFirstChild(), is(sameInstance(node)));
        assertThat(parent.getLastChild(), is(sameInstance(node)));
    }

    @Test
    public void shouldGetFirstChildAndLastChildWithTwoChildren() {
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.ACCESS, parent);
        assertThat(parent.getFirstChild(), is(sameInstance(child1)));
        assertThat(parent.getLastChild(), is(sameInstance(child2)));
    }

    @Test
    public void shouldGetFirstChildAndLastChildWithMoreThanTwoChildren() {
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        new PlanNode(Type.ACCESS, parent);
        PlanNode child3 = new PlanNode(Type.ACCESS, parent);
        assertThat(parent.getFirstChild(), is(sameInstance(child1)));
        assertThat(parent.getLastChild(), is(sameInstance(child3)));
    }

    @Test
    public void shouldGetFirstChildAndLastChildWithNoChildren() {
        parent = new PlanNode(Type.JOIN);
        assertThat(parent.getFirstChild(), is(nullValue()));
        assertThat(parent.getLastChild(), is(nullValue()));
    }

    @Test
    public void shouldRemoveNodeFromExistingParentWhenSettingParentToNull() {
        parent = new PlanNode(Type.JOIN);
        node = new PlanNode(Type.ACCESS, parent);
        assertThat(parent.getFirstChild(), is(sameInstance(node)));
        assertThat(parent.getChildCount(), is(1));
        node.setParent(null);
        assertThat(parent.getChildCount(), is(0));
        assertThat(node.getParent(), is(nullValue()));
    }

    @Test
    public void shouldInsertNewParentNodeInBetweenExistingParentAndChild() {
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
        assertThat(parent.getFirstChild(), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getLastChild(), is(sameInstance(child3)));
        assertThat(parent.getChildCount(), is(3));
        node = new PlanNode(Type.GROUP);
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
        PlanNode child1 = new PlanNode(Type.ACCESS);
        node = new PlanNode(Type.GROUP);
        PlanNode nodeChild = new PlanNode(Type.JOIN, node);
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
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
        PlanNode grandChild21 = new PlanNode(Type.LIMIT, child2);
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
        node = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, node);
        assertThat(node.getFirstChild(), is(sameInstance(child1)));
        assertThat(node.getChildCount(), is(1));
        // Perform the removeFromParent ...
        assertThat(node.removeFromParent(), is(nullValue()));
        assertThat(node.getFirstChild(), is(sameInstance(child1)));
        assertThat(node.getChildCount(), is(1));
    }

    @Test
    public void shouldReturnListOfChildren() {
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
        List<PlanNode> children = parent.getChildren();
        assertThat(children.get(0), is(sameInstance(child1)));
        assertThat(children.get(1), is(sameInstance(child2)));
        assertThat(children.get(2), is(sameInstance(child3)));
        assertThat(children.size(), is(3));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnImmutableListOfChildren() {
        parent = new PlanNode(Type.JOIN);
        new PlanNode(Type.ACCESS, parent);
        new PlanNode(Type.DUP_REMOVE, parent);
        new PlanNode(Type.GROUP, parent);
        parent.getChildren().clear();
    }

    @Test
    public void shouldReturnIteratorOfChildren() {
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
        Iterator<PlanNode> children = parent.iterator();
        assertThat(children.next(), is(sameInstance(child1)));
        assertThat(children.next(), is(sameInstance(child2)));
        assertThat(children.next(), is(sameInstance(child3)));
        assertThat(children.hasNext(), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnImmutableIteratorOfChildren() {
        parent = new PlanNode(Type.JOIN);
        new PlanNode(Type.ACCESS, parent);
        new PlanNode(Type.DUP_REMOVE, parent);
        new PlanNode(Type.GROUP, parent);
        Iterator<PlanNode> iter = parent.iterator();
        iter.next();
        iter.remove();
    }

    @Test
    public void shouldRemoveAllChildrenOfParentWithNoChildrenByReturningEmptyList() {
        parent = new PlanNode(Type.JOIN);
        // Perform the remove, and verify the list has all the children ...
        List<PlanNode> children = parent.removeAllChildren();
        assertThat(children.size(), is(0));
        assertThat(parent.getChildCount(), is(0));
        // Add a new child to the parent ...
        PlanNode child1a = new PlanNode(Type.ACCESS, parent);
        assertThat(parent.getFirstChild(), is(sameInstance(child1a)));
        // The returned copy should not be modified ...
        assertThat(children.size(), is(0));
    }

    @Test
    public void shouldRemoveAllChildrenAndReturnCopyOfListOfChildren() {
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
        // Perform the remove, and verify the list has all the children ...
        List<PlanNode> children = parent.removeAllChildren();
        assertThat(children.get(0), is(sameInstance(child1)));
        assertThat(children.get(1), is(sameInstance(child2)));
        assertThat(children.get(2), is(sameInstance(child3)));
        assertThat(children.size(), is(3));
        assertThat(parent.getChildCount(), is(0));
        // Add a new child to the parent ...
        PlanNode child1a = new PlanNode(Type.ACCESS, parent);
        assertThat(parent.getFirstChild(), is(sameInstance(child1a)));
        // The returned copy should not be modified ...
        assertThat(children.get(0), is(sameInstance(child1)));
        assertThat(children.get(1), is(sameInstance(child2)));
        assertThat(children.get(2), is(sameInstance(child3)));
        assertThat(children.size(), is(3));
    }

    @Test
    public void shouldReturnCorrectChildCount() {
        parent = new PlanNode(Type.JOIN);
        assertThat(parent.getChildCount(), is(0));
        for (int i = 0; i != 10; ++i) {
            new PlanNode(Type.ACCESS, parent);
            assertThat(parent.getChildCount(), is(i + 1));
        }
    }

    @Test
    public void shouldAddChildrenAtEnd() {
        parent = new PlanNode(Type.JOIN);
        List<PlanNode> children = new ArrayList<PlanNode>();
        children.add(new PlanNode(Type.ACCESS, parent));
        children.add(new PlanNode(Type.GROUP, parent));
        children.add(new PlanNode(Type.NULL, parent));
        parent.addChildren(children);
        int index = 0;
        for (PlanNode child : children) {
            assertThat(parent.getChild(index++), is(sameInstance(child)));
        }
    }

    @Test
    public void shouldRemoveChild() {
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
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
        node = new PlanNode(Type.PROJECT);
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
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
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
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
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
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
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
        PlanNode grandChild1 = new PlanNode(Type.SELECT, child2);
        PlanNode grandChild2 = new PlanNode(Type.SET_OPERATION, child2);
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
        PlanNode parentOfReplacement = new PlanNode(Type.SORT);
        PlanNode replacement = new PlanNode(Type.SELECT, parentOfReplacement);
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
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
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
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
        PlanNode nonChild = new PlanNode(Type.PROJECT);
        PlanNode replacement = new PlanNode(Type.SELECT);
        parent = new PlanNode(Type.JOIN);
        PlanNode child1 = new PlanNode(Type.ACCESS, parent);
        PlanNode child2 = new PlanNode(Type.DUP_REMOVE, parent);
        PlanNode child3 = new PlanNode(Type.GROUP, parent);
        assertThat(parent.getChild(0), is(sameInstance(child1)));
        assertThat(parent.getChild(1), is(sameInstance(child2)));
        assertThat(parent.getChild(2), is(sameInstance(child3)));
        assertThat(parent.replaceChild(nonChild, replacement), is(false));
    }

    @Test
    public void shouldGetPathThatIncludesStartAndEndNodes() {
        PlanNode root = new PlanNode(Type.JOIN);
        PlanNode node1 = new PlanNode(Type.ACCESS, root);
        PlanNode node2 = new PlanNode(Type.DUP_REMOVE, node1);
        PlanNode node3 = new PlanNode(Type.GROUP, node2);
        PlanNode node4 = new PlanNode(Type.SELECT, node3);
        PlanNode node5 = new PlanNode(Type.SET_OPERATION, node4);
        assertThat(root.getPathTo(root), is(path(root)));
        assertThat(root.getPathTo(node1), is(path(root, node1)));
        assertThat(root.getPathTo(node2), is(path(root, node1, node2)));
        assertThat(root.getPathTo(node3), is(path(root, node1, node2, node3)));
        assertThat(root.getPathTo(node4), is(path(root, node1, node2, node3, node4)));
        assertThat(root.getPathTo(node5), is(path(root, node1, node2, node3, node4, node5)));

        assertThat(node1.getPathTo(node1), is(path(node1)));
        assertThat(node1.getPathTo(node2), is(path(node1, node2)));
        assertThat(node1.getPathTo(node3), is(path(node1, node2, node3)));
        assertThat(node1.getPathTo(node4), is(path(node1, node2, node3, node4)));
        assertThat(node1.getPathTo(node5), is(path(node1, node2, node3, node4, node5)));

        assertThat(node2.getPathTo(node2), is(path(node2)));
        assertThat(node2.getPathTo(node3), is(path(node2, node3)));
        assertThat(node2.getPathTo(node4), is(path(node2, node3, node4)));
        assertThat(node2.getPathTo(node5), is(path(node2, node3, node4, node5)));
    }

    protected LinkedList<PlanNode> path( PlanNode... expectedNodes ) {
        LinkedList<PlanNode> result = new LinkedList<PlanNode>();
        for (PlanNode node : expectedNodes) {
            result.add(node);
        }
        return result;
    }

    @Test
    public void shouldNotAddNullSelectorNames() {
        Collection<SelectorName> names = Collections.singletonList(null);
        node.addSelectors(names);
        assertThat(node.getSelectors().isEmpty(), is(true));
        node.addSelector(null);
        assertThat(node.getSelectors().isEmpty(), is(true));
        SelectorName name = new SelectorName("something");
        node.addSelector(name, null);
        assertThat(node.getSelectors().size(), is(1));
        assertThat(node.getSelectors().contains(name), is(true));
    }

    @Test
    public void shouldCorrectlyDetermineIfAncestorHasType() {
        PlanNode root = new PlanNode(Type.JOIN);
        PlanNode node1 = new PlanNode(Type.ACCESS, root);
        PlanNode node2 = new PlanNode(Type.DUP_REMOVE, node1);
        PlanNode node3 = new PlanNode(Type.GROUP, node2);
        PlanNode node4 = new PlanNode(Type.SELECT, node3);
        PlanNode node5 = new PlanNode(Type.SET_OPERATION, node4);
        assertThat(node5.hasAncestorOfType(Type.SET_OPERATION), is(false)); // no ancestor, just self
        assertThat(node5.hasAncestorOfType(Type.SOURCE), is(false));
        assertThat(node5.hasAncestorOfType(Type.DUP_REMOVE), is(true));
        assertThat(node5.hasAncestorOfType(Type.DUP_REMOVE, Type.SELECT), is(true));
        assertThat(node5.hasAncestorOfType(Type.DUP_REMOVE, Type.SELECT, Type.SOURCE), is(true));
    }
}
