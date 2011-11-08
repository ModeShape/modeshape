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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class AbstractJcrItemTest extends MultiUseAbstractTest {

    private static Node rootNode;
    private static Node savedNode;
    private static Node newNode;
    private static Property propertyOnNewNode;
    private static Node nodeWithModifiedProperty;
    private static Property newPropertyOnModifiedNode;
    private static Property modifiedPropertyOnModifiedNode;
    private static Property unmodifiedPropertyOnModifiedNode;
    private static Node nodeWithModifiedChildren;
    private static Node nodeOfDepth2;

    @BeforeClass
    public static void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();
        rootNode = session.getRootNode();
        savedNode = rootNode.addNode("savedNode");
        savedNode.setProperty("p1", "value1");
        savedNode.setProperty("p2", true);
        nodeWithModifiedProperty = rootNode.addNode("nodeWithModifiedProperty");
        nodeWithModifiedProperty.setProperty("p1", "value1");
        nodeWithModifiedProperty.setProperty("p2", true);
        nodeWithModifiedChildren = rootNode.addNode("nodeWithModifiedChildren");
        session.save();

        // Now create the transient changes, but do NOT save ...
        newPropertyOnModifiedNode = nodeWithModifiedProperty.setProperty("p3", "value3");
        modifiedPropertyOnModifiedNode = nodeWithModifiedProperty.setProperty("p1", "modified value1");
        unmodifiedPropertyOnModifiedNode = nodeWithModifiedProperty.getProperty("p2");

        newNode = rootNode.addNode("newUnsavedNode");
        propertyOnNewNode = newNode.setProperty("p4", "value4");

        nodeOfDepth2 = nodeWithModifiedChildren.addNode("newChild");
        assert rootNode != null;
        assert savedNode != null;
        assert newNode != null;
        assert propertyOnNewNode != null;
        assert nodeWithModifiedProperty != null;
        assert newPropertyOnModifiedNode != null;
        assert modifiedPropertyOnModifiedNode != null;
        assert unmodifiedPropertyOnModifiedNode != null;
        assert nodeWithModifiedChildren != null;
    }

    @AfterClass
    public static void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Test
    public void newPropertyOnModifiedNodeShouldNotBeModified() {
        assertThat(newPropertyOnModifiedNode.isModified(), is(false));
    }

    @Test
    public void newPropertyOnModifiedNodeShouldBeNew() {
        assertThat(newPropertyOnModifiedNode.isNew(), is(true));
    }

    @Test
    public void changedPropertyOnModifiedNodeShouldBeModified() {
        assertThat(modifiedPropertyOnModifiedNode.isModified(), is(true));
    }

    @Test
    public void changedPropertyOnModifiedNodeShouldNotBeNew() {
        assertThat(modifiedPropertyOnModifiedNode.isNew(), is(false));
    }

    @Test
    public void unchangedPropertyOnModifiedNodeShouldNotBeModifiedOrNew() {
        assertThat(unmodifiedPropertyOnModifiedNode.isModified(), is(false));
        assertThat(unmodifiedPropertyOnModifiedNode.isNew(), is(false));
    }

    @Test
    public void newNodeShouldHaveAllNewProperties() throws Exception {
        assertThat(newNode.isNew(), is(true));
        assertThat(newNode.isModified(), is(false));
        for (PropertyIterator iter = newNode.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            assertThat(prop.isNew(), is(true));
            assertThat(prop.isModified(), is(false));
        }
    }

    @Test
    public void savedNodeShouldHaveNoNewOrModifiedProperties() throws Exception {
        assertThat(savedNode.isNew(), is(false));
        assertThat(savedNode.isModified(), is(false));
        assertNoModifiedOrNewProperties(savedNode);
        assertNoModifiedOrNewOrRemovedChildren(savedNode);
    }

    @Test
    public void nodeWithOnlyChangedPropertiesShouldBeConsideredModified() throws Exception {
        assertThat(nodeWithModifiedProperty.isNew(), is(false));
        assertThat(nodeWithModifiedProperty.isModified(), is(true));
        assertNoModifiedOrNewOrRemovedChildren(nodeWithModifiedProperty);
        assertNumberOfModifiedProperties(nodeWithModifiedProperty, 1);
        assertNumberOfNewProperties(nodeWithModifiedProperty, 1);
    }

    @Test
    public void nodeWithOnlyChangedChildrenShouldBeConsideredModified() throws Exception {
        assertThat(nodeWithModifiedProperty.isNew(), is(false));
        assertThat(nodeWithModifiedProperty.isModified(), is(true));
        assertNoModifiedOrNewProperties(nodeWithModifiedChildren);
        assertNumberOfModifiedOrNewChildren(nodeWithModifiedChildren, 1);
    }

    @Test
    public void shouldReportCorrectDepthForNodes() throws Exception {
        assertThat(rootNode.getDepth(), is(0));
        assertThat(savedNode.getDepth(), is(1));
        assertThat(newNode.getDepth(), is(1));
        assertThat(nodeWithModifiedProperty.getDepth(), is(1));
        assertThat(nodeWithModifiedChildren.getDepth(), is(1));
        assertThat(nodeOfDepth2.getDepth(), is(2));
    }

    @Test
    public void parentOfPropertyShouldBeNode() throws Exception {
        assertThat(newPropertyOnModifiedNode.getParent(), is(nodeWithModifiedProperty));
        assertThat(modifiedPropertyOnModifiedNode.getParent(), is(nodeWithModifiedProperty));
        assertThat(unmodifiedPropertyOnModifiedNode.getParent(), is(nodeWithModifiedProperty));
    }

    @Test
    public void zerothAncestorOfPropertyShouldBeRootNode() throws Exception {
        assertThat(newPropertyOnModifiedNode.getAncestor(0), is((Item)rootNode));
        assertThat(modifiedPropertyOnModifiedNode.getAncestor(0), is((Item)rootNode));
        assertThat(unmodifiedPropertyOnModifiedNode.getAncestor(0), is((Item)rootNode));
    }

    @Test
    public void firstAncestorOfPropertyShouldBeParentNode() throws Exception {
        assertThat(newPropertyOnModifiedNode.getAncestor(1), is((Item)nodeWithModifiedProperty));
        assertThat(modifiedPropertyOnModifiedNode.getAncestor(1), is((Item)nodeWithModifiedProperty));
        assertThat(unmodifiedPropertyOnModifiedNode.getAncestor(1), is((Item)nodeWithModifiedProperty));
    }

    @Test
    public void secondAncestorOfPropertyShouldBeParentOfParentNode() throws Exception {
        assertThat(newPropertyOnModifiedNode.getAncestor(2), is((Item)newPropertyOnModifiedNode));
        assertThat(modifiedPropertyOnModifiedNode.getAncestor(2), is((Item)modifiedPropertyOnModifiedNode));
        assertThat(unmodifiedPropertyOnModifiedNode.getAncestor(2), is((Item)unmodifiedPropertyOnModifiedNode));
    }

    @Test
    public void parentOfNodeShouldBeParentNode() throws Exception {
        assertThat(savedNode.getParent(), is(rootNode));
        assertThat(newNode.getParent(), is(rootNode));
        assertThat(nodeWithModifiedProperty.getParent(), is(rootNode));
        assertThat(nodeWithModifiedChildren.getParent(), is(rootNode));
        assertThat(nodeOfDepth2.getParent(), is(nodeWithModifiedChildren));
    }

    @Test( expected = ItemNotFoundException.class )
    public void parentOfRootNodeShouldFail() throws Exception {
        rootNode.getParent();
    }

    protected void assertNoModifiedOrNewProperties( Node node ) throws Exception {
        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            assertThat(prop.isNew(), is(false));
            assertThat(prop.isModified(), is(false));
        }
    }

    protected void assertNoModifiedOrNewOrRemovedChildren( Node node ) throws Exception {
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            assertThat(child.isNew(), is(false));
            assertThat(child.isModified(), is(false));
        }
    }

    protected void assertNumberOfNewOrModifiedProperties( Node node,
                                                          int expected ) throws Exception {
        int numModifiedOrNewProperties = 0;
        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if (prop.isNew() || prop.isModified()) ++numModifiedOrNewProperties;
        }
        assertThat(numModifiedOrNewProperties, is(expected));
    }

    protected void assertNumberOfNewProperties( Node node,
                                                int expected ) throws Exception {
        int numNewProperties = 0;
        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if (prop.isNew()) ++numNewProperties;
        }
        assertThat(numNewProperties, is(expected));
    }

    protected void assertNumberOfModifiedProperties( Node node,
                                                     int expected ) throws Exception {
        int numModifiedProperties = 0;
        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            if (prop.isNew()) ++numModifiedProperties;
        }
        assertThat(numModifiedProperties, is(expected));
    }

    protected void assertNumberOfModifiedOrNewChildren( Node node,
                                                        int expected ) throws Exception {
        int numModifiedOrNewChildren = 0;
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            if (child.isNew() || child.isModified()) ++numModifiedOrNewChildren;
        }
        assertThat(numModifiedOrNewChildren, is(expected));
    }
}
