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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import javax.jcr.ImportUUIDBehavior;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import java.util.HashSet;
import java.util.Set;

public class JcrNodeTest extends MultiUseAbstractTest {

    private AbstractJcrNode hybrid;
    private AbstractJcrNode altima;

    @BeforeClass
    public static final void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

        // Import the node types and the data ...
        registerNodeTypes("cars.cnd");
        importContent("/", "io/cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
    }

    @AfterClass
    public static final void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        hybrid = session.getNode("/Cars/Hybrid");
        altima = session.getNode("/Cars/Hybrid/Nissan Altima");
        assertThat(hybrid, is(notNullValue()));
        assertThat(altima, is(notNullValue()));
    }

    @Test
    public void shouldHavePath() throws Exception {
        assertThat(altima.getPath(), is("/Cars/Hybrid/Nissan Altima"));

        javax.jcr.Node altima2 = hybrid.addNode("Nissan Altima");
        try {
            assertThat(altima2, is(notNullValue()));
            assertThat(altima2.getPath(), is("/Cars/Hybrid/Nissan Altima[2]"));
        } finally {
            altima2.remove(); // remove the node we added in this test to not interfere with other tests
        }
    }

    @Test
    public void shouldHaveSameNameSiblingIndex() throws Exception {
        assertThat(altima.getIndex(), is(1));

        javax.jcr.Node altima2 = hybrid.addNode("Nissan Altima");
        try {
            assertThat(altima2, is(notNullValue()));
            assertThat(altima2.getIndex(), is(2));
        } finally {
            altima2.remove(); // remove the node we added in this test to not interfere with other tests
        }
    }

    @Test
    public void shouldHaveNameThatExcludesSameNameSiblingIndex() throws Exception {
        assertThat(altima.getName(), is("Nissan Altima"));
        javax.jcr.Node altima2 = hybrid.addNode("Nissan Altima");
        try {
            assertThat(altima2, is(notNullValue()));
            assertThat(altima2.getPath(), is("/Cars/Hybrid/Nissan Altima[2]"));
            assertThat(altima2.getName(), is("Nissan Altima"));
        } finally {
            altima2.remove(); // remove the node we added in this test to not interfere with other tests
        }
    }

    @Test
    public void shouldRetrieveReferenceProperties() throws Exception {
        Node referenceableNode = session.getRootNode().addNode("referenceable");
        referenceableNode.addMixin(JcrMixLexicon.REFERENCEABLE.toString());

        Node node1 = session.getRootNode().addNode("node1");
        Property prop1 = node1.setProperty("prop1", session.getValueFactory().createValue(referenceableNode, false));
        Property prop2 = node1.setProperty("prop2", session.getValueFactory().createValue(referenceableNode, true));

        Node node2 = session.getRootNode().addNode("node2");
        Property prop3 = node2.setProperty("prop3", session.getValueFactory().createValue(referenceableNode, false));
        Property prop4 = node2.setProperty("prop4", session.getValueFactory().createValue(referenceableNode, true));

        session.save();

        //check all strong references
        PropertyIterator propertyIterator = referenceableNode.getReferences();
        assertEquals(2, propertyIterator.getSize());
        Set<String> propertyNames = new HashSet<String>(2);
        while (propertyIterator.hasNext()) {
            propertyNames.add(propertyIterator.nextProperty().getName());
        }
        assertTrue(propertyNames.contains(prop1.getName()) && propertyNames.contains(prop3.getName()));

        propertyIterator = referenceableNode.getReferences("prop1");
        assertEquals(1, propertyIterator.getSize());
        assertEquals(prop1.getName(), propertyIterator.nextProperty().getName());

        propertyIterator = referenceableNode.getReferences("unknown");
        assertEquals(0, propertyIterator.getSize());

        //check all weak references
        propertyIterator = referenceableNode.getWeakReferences();
        assertEquals(2, propertyIterator.getSize());
        propertyNames = new HashSet<String>(2);
        while (propertyIterator.hasNext()) {
            propertyNames.add(propertyIterator.nextProperty().getName());
        }
        assertTrue(propertyNames.contains(prop2.getName()) && propertyNames.contains(prop4.getName()));

        propertyIterator = referenceableNode.getWeakReferences("prop4");
        assertEquals(1, propertyIterator.getSize());
        assertEquals(prop4.getName(), propertyIterator.nextProperty().getName());

        propertyIterator = referenceableNode.getWeakReferences("unknown");
        assertEquals(0, propertyIterator.getSize());
    }

    @Test
    @FixFor("MODE-1489")
    public void shouldAllowMultipleOrderBeforeWithoutSave() throws Exception {
        int childCount = 2;

        Node parent = session.getRootNode().addNode("parent", "nt:unstructured");
        for (int i = 0; i < childCount; i++) {
            parent.addNode("Child " + i, "nt:unstructured");
        }

        session.save();

        long childIdx = 0;
        NodeIterator nodeIterator = parent.getNodes();
        while (nodeIterator.hasNext()) {
            parent.orderBefore("Child " + childIdx, "Child 0");
            childIdx++;
            nodeIterator.nextNode();
        }

        session.save();

        nodeIterator = parent.getNodes();
        childIdx = nodeIterator.getSize() - 1;
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            assertEquals("Child " + childIdx, child.getName());
            childIdx--;
        }
    }
}
