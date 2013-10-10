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

import java.util.HashSet;
import java.util.Set;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.cache.CachedNode;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

        // check all strong references
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

        // check all weak references
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
    @FixFor( "MODE-1489" )
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

    @Test
    @FixFor( "MODE-2034" )
    public void shouldReorderChildrenWithChanges() throws Exception {
        Node parent = session.getRootNode().addNode("parent", "nt:unstructured");
        Node child1 = parent.addNode("child1");
        Node child2 = parent.addNode("child2");
        session.save();

        parent.orderBefore("child2", "child1");
        child1.setProperty("prop", "value");
        child2.setProperty("prop", "value");
        session.save();

        NodeIterator nodeIterator = parent.getNodes();
        long childIdx = nodeIterator.getSize();
        while (nodeIterator.hasNext()) {
            Node child = nodeIterator.nextNode();
            assertEquals("child" + childIdx, child.getName());
            childIdx--;
        }
    }

    @Test
    @FixFor( "MODE-1663" )
    public void shouldMakeReferenceableNodesUsingCustomTypes() throws Exception {
        Node cars = session.getNode("/Cars");
        cars.addNode("referenceableCar1", "car:referenceableCar");
        Node refCar = cars.addNode("referenceableCar2");
        refCar.setPrimaryType("car:referenceableCar");

        session.save();

        Node referenceableCar1 = session.getNode("/Cars/referenceableCar1");
        String uuid = referenceableCar1.getProperty(JcrLexicon.UUID.getString()).getString();
        assertEquals(referenceableCar1.getIdentifier(), uuid);

        Node referenceableCar2 = session.getNode("/Cars/referenceableCar2");
        uuid = referenceableCar2.getProperty(JcrLexicon.UUID.getString()).getString();
        assertEquals(referenceableCar2.getIdentifier(), uuid);
    }

    @Test
    @FixFor( "MODE-1751" )
    public void shouldNotCauseReferentialIntegrityExceptionWhenSameReferrerUpdatedMultipleTimes() throws Exception {
        Node nodeA = session.getRootNode().addNode("nodeA");
        nodeA.addMixin("mix:referenceable");
        Node nodeB = session.getRootNode().addNode("nodeB");
        nodeB.setProperty("nodeA", session.getValueFactory().createValue(nodeA, false));
        session.save();

        nodeB.setProperty("nodeA", session.getValueFactory().createValue(nodeA, false));
        session.save();

        nodeB.remove();
        session.save();

        nodeA.remove();
        session.save();
    }

    @Test
    public void shouldReturnEmptyIterator() throws RepositoryException {
        Node jcrRootNode = session.getRootNode();
        Node rootNode = jcrRootNode.addNode("mapSuperclassTest");
        // session.save();
        Node newNode = rootNode.addNode("newNode");
        assertNotNull(newNode);
        NodeIterator nodeIterator = rootNode.getNodes("myMap");
        assertFalse(nodeIterator.hasNext());
        session.save();
        nodeIterator = rootNode.getNodes("myMap");
        assertFalse(nodeIterator.hasNext());
    }

    @Test
    @FixFor("MODE-1969")
    public void shouldAllowSimpleReferences() throws Exception {
        registerNodeTypes("cnd/simple-references.cnd");

        JcrRootNode rootNode = session.getRootNode();
        AbstractJcrNode a = rootNode.addNode("A");
        a.addMixin("mix:referenceable");
        AbstractJcrNode b = rootNode.addNode("B");
        b.addMixin("mix:referenceable");
        AbstractJcrNode c = rootNode.addNode("C");
        c.addMixin("mix:referenceable");

        org.modeshape.jcr.api.ValueFactory valueFactory = session.getValueFactory();
        Node testNode = rootNode.addNode("test", "test:node");
        testNode.setProperty("test:singleRef", valueFactory.createSimpleReference(a));
        testNode.setProperty("test:multiRef", new Value[]{valueFactory.createSimpleReference(b),
                valueFactory.createSimpleReference(c)});
        session.save();

        Property singleRef = testNode.getProperty("test:singleRef");
        assertEquals(a.getIdentifier(), singleRef.getNode().getIdentifier());
        assertNoBackReferences(a);

        Property multiRef = testNode.getProperty("test:multiRef");
        assertTrue(multiRef.isMultiple());
        Value[] actualValues = multiRef.getValues();
        assertEquals(2, actualValues.length);
        assertArrayEquals(new String[] { b.getIdentifier(), c.getIdentifier() },
                          new String[] { actualValues[0].getString(), actualValues[1].getString() });
        assertNoBackReferences(b);
        assertNoBackReferences(c);

        a.remove();
        b.remove();
        c.remove();

        session.save();

        try {
            testNode.getProperty("test:singleRef").getNode();
            fail("Target node for simple reference property should not be found");
        } catch (javax.jcr.ItemNotFoundException e) {
            //expected
        }
    }

    private void assertNoBackReferences( AbstractJcrNode node ) throws RepositoryException {
        assertEquals(0, node.getReferences().getSize());
        assertEquals(0, node.getWeakReferences().getSize());
        assertFalse(node.referringNodes(CachedNode.ReferenceType.BOTH).hasNext());
    }

    @Test
    @FixFor("MODE-1969")
    public void shouldNotAllowSimpleReferencesWithoutMixReferenceableMixin() throws Exception {
        registerNodeTypes("cnd/simple-references.cnd");

        JcrRootNode rootNode = session.getRootNode();
        AbstractJcrNode a = rootNode.addNode("A");
        org.modeshape.jcr.api.ValueFactory valueFactory = session.getValueFactory();
        Node testNode = rootNode.addNode("test", "test:node");
        try {
            testNode.setProperty("test:singleRef", valueFactory.createSimpleReference(a));
            fail("Simple references should not be allowed if the target node doesn't have the mix:referenceable mixin");
        } catch (RepositoryException e) {
            //expected
        }
    }

    @Test
    @FixFor("MODE-2069")
    public void shouldAllowSearchingForSNSViaRegex() throws Exception {
        JcrRootNode rootNode = session.getRootNode();
        rootNode.addNode("child");
        rootNode.addNode("child");
        session.save();

        assertEquals(0, rootNode.getNodes("child[2]").getSize());
        assertEquals(0, rootNode.getNodes("*[2]").getSize());
        assertEquals(0, rootNode.getNodes("*[1]|*[2]").getSize());
        assertEquals(0, rootNode.getNodes(new String[] { "*[2]" }).getSize());
        assertEquals(0, rootNode.getNodes(new String[] { "*[1]", "*[2]" }).getSize());

        assertEquals(2, rootNode.getNodes(new String[] { "child", "child" }).getSize());
        assertEquals(2, rootNode.getNodes(new String[] { "*child"}).getSize());
        assertEquals(2, rootNode.getNodes(new String[] { "child*"}).getSize());
        assertEquals(2, rootNode.getNodes(new String[] { "child"}).getSize());
    }

    @Test
    @FixFor("MODE-2069")
    public void shouldEscapeSpecialCharactersWhenSearchingNodesViaRegex() throws Exception {
        JcrRootNode rootNode = session.getRootNode();
        rootNode.addNode("special\t\r\n()\\?!^${}.\"");
        session.save();

        assertEquals(1, rootNode.getNodes("*\t*").getSize());
        assertEquals(1, rootNode.getNodes("*\r*").getSize());
        assertEquals(1, rootNode.getNodes("*\n*").getSize());
        assertEquals(1, rootNode.getNodes("*(*").getSize());
        assertEquals(1, rootNode.getNodes("*)*").getSize());
        assertEquals(1, rootNode.getNodes("*()*").getSize());
        assertEquals(1, rootNode.getNodes("*\\*").getSize());
        assertEquals(1, rootNode.getNodes("*?*").getSize());
        assertEquals(1, rootNode.getNodes("*!*").getSize());
        assertEquals(1, rootNode.getNodes("*^*").getSize());
        assertEquals(1, rootNode.getNodes("*$*").getSize());
        assertEquals(1, rootNode.getNodes("*{*").getSize());
        assertEquals(1, rootNode.getNodes("*}*").getSize());
        assertEquals(1, rootNode.getNodes("*{}*").getSize());
        assertEquals(1, rootNode.getNodes("*.*").getSize());
        assertEquals(1, rootNode.getNodes("*\"*").getSize());

        assertEquals(0, rootNode.getNodes("*[*").getSize());
        assertEquals(0, rootNode.getNodes("*]*").getSize());
        assertEquals(0, rootNode.getNodes("*[]*").getSize());
        assertEquals(1, rootNode.getNodes("*:*").getSize()); //jcr:system
        assertEquals(0, rootNode.getNodes("*/*").getSize());
    }

    @Test
    @FixFor("MODE-2069")
    public void shouldOnlyTrimLeadingAndTrailingSpacesWhenSearchingViaRegex() throws Exception {
        JcrRootNode rootNode = session.getRootNode();
        rootNode.addNode(" A ");
        rootNode.addNode("B ");
        rootNode.addNode(" C");
        rootNode.addNode("D E");
        session.save();

        assertEquals(1, rootNode.getNodes(" A ").getSize());
        assertEquals(1, rootNode.getNodes("B ").getSize());
        assertEquals(1, rootNode.getNodes(" C").getSize());
        assertEquals(2, rootNode.getNodes(" A |B ").getSize());
        assertEquals(3, rootNode.getNodes(" A | B | C ").getSize());
        assertEquals(1, rootNode.getNodes("D E").getSize());
        assertEquals(1, rootNode.getNodes(" D E ").getSize());
        assertEquals(2, rootNode.getNodes(" A |D E").getSize());
    }
}
