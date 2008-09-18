/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.properties.NamespaceRegistry;
import org.jboss.dna.graph.properties.Path.Segment;
import org.jboss.dna.graph.properties.basic.BasicName;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class AbstractJcrNodeTest {

    static MockAbstractJcrNode createChild( JcrSession session,
                                            String name,
                                            int index,
                                            List<Segment> children,
                                            Node parent ) throws Exception {
        MockAbstractJcrNode child = new MockAbstractJcrNode(session, name, parent);
        Segment seg = Mockito.mock(Segment.class);
        stub(seg.getName()).toReturn(new BasicName(null, name));
        children.add(seg);
        stub(session.getItem(parent.getPath() + "/" + name + '[' + index + ']')).toReturn(child);
        return child;
    }

    static class MockAbstractJcrNode extends AbstractJcrNode {

        String name;
        Node parent;

        MockAbstractJcrNode( JcrSession session,
                             String name,
                             Node parent ) {
            super(session);
            this.name = name;
            this.parent = parent;
        }

        public int getDepth() {
            return 0;
        }

        public int getIndex() {
            return 0;
        }

        public String getName() {
            return name;
        }

        public Node getParent() {
            return parent;
        }

        public String getPath() throws RepositoryException {
            return (parent == null ? '/' + getName() : parent.getPath() + '/' + getName());
        }
    }

    private AbstractJcrNode node;
    @Mock
    private JcrSession session;
    private List<Segment> children;
    private Set<Property> properties;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        NamespaceRegistry registry = Mockito.mock(NamespaceRegistry.class);
        ExecutionContext context = Mockito.mock(ExecutionContext.class);
        stub(context.getNamespaceRegistry()).toReturn(registry);
        stub(session.getExecutionContext()).toReturn(context);
        children = new ArrayList<Segment>();
        properties = new HashSet<Property>();
        node = new MockAbstractJcrNode(session, "node", null);
        node.setProperties(properties);
    }

    @Test
    public void shouldAllowVisitation() throws Exception {
        ItemVisitor visitor = Mockito.mock(ItemVisitor.class);
        node.accept(visitor);
        Mockito.verify(visitor).visit(node);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowVisitationIfNoVisitor() throws Exception {
        node.accept(null);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNoSession() throws Exception {
        new MockAbstractJcrNode(null, null, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNegativeAncestorDepth() throws Exception {
        node.getAncestor(-1);
    }

    @Test
    public void shouldProvideAncestor() throws Exception {
        assertThat(node.getAncestor(0), is((Item)node));
    }

    @Test
    public void shouldProvideInternalUuid() throws Exception {
        UUID uuid = UUID.randomUUID();
        node.setInternalUuid(uuid);
        assertThat(node.getInternalUuid(), is(uuid));
    }

    @Test
    public void shouldProvideNamedProperty() throws Exception {
        Property property = Mockito.mock(Property.class);
        stub(property.getName()).toReturn("test");
        properties.add(property);
        assertThat(node.getProperty("test"), is(property));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shoudNotAllowAddMixin() throws Exception {
        node.addMixin(null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shoudNotAllowAddNode() throws Exception {
        node.addNode(null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shoudNotAllowAddNodeWithType() throws Exception {
        node.addNode(null, null);
    }

    @Test
    public void shoudNotAllowCanAddMixin() throws Exception {
        assertThat(node.canAddMixin(null), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shoudNotAllowCancelMerge() throws Exception {
        node.cancelMerge(null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shoudNotAllowCheckin() throws Exception {
        node.checkin();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shoudNotAllowCheckout() throws Exception {
        node.checkout();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shoudNotAllowDoneMerge() throws Exception {
        node.doneMerge(null);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shoudNotAllowGetBaseVersion() throws Exception {
        node.getBaseVersion();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowGetCorrespondingNodePath() throws Exception {
        node.getCorrespondingNodePath(null);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shoudNotAllowGetLock() throws Exception {
        node.getLock();
    }

    @Test
    public void shouldProvideNode() throws Exception {
        Node child = createChild(session, "child", 1, children, node);
        node.setChildren(children);
        stub(session.getItem("/node/child")).toReturn(child);
        assertThat(node.getNode("child"), is(child));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetNodeWithNoPath() throws Exception {
        node.getNode(null);
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotProvideNodeIfPathNotFound() throws Exception {
        node.getNode("bogus");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotProvideNodeIfPathIsProperty() throws Exception {
        Property property = Mockito.mock(Property.class);
        stub(session.getItem("/property")).toReturn(property);
        node.getNode("property");
    }

    @Test
    public void shouldProvideNodeIterator() throws Exception {
        assertThat(node.getNodes(), notNullValue());
    }

    @Test
    public void shoudProvidePrimaryItem() throws Exception {
        Property property = Mockito.mock(Property.class);
        stub(property.getName()).toReturn("jcr:primaryItemName");
        stub(property.getString()).toReturn("primaryItem");
        properties.add(property);
        Item primaryItem = Mockito.mock(Item.class);
        stub(session.getItem("/node/primaryItem")).toReturn(primaryItem);
        assertThat(node.getPrimaryItem(), is(primaryItem));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shoudNotProvidePrimaryItemIfUnavailable() throws Exception {
        node.getPrimaryItem();
    }

    @Test
    public void shouldProvideProperty() throws Exception {
        Property prop1 = Mockito.mock(Property.class);
        stub(prop1.getName()).toReturn("prop1");
        properties.add(prop1);
        assertThat(node.getProperty("prop1"), is(prop1));
        MockAbstractJcrNode child = createChild(session, "child", 1, children, node);
        Set<Property> properties = new HashSet<Property>();
        child.setProperties(properties);
        Property prop2 = Mockito.mock(Property.class);
        stub(prop2.getName()).toReturn("prop2");
        stub(session.getItem("/node/child/prop2")).toReturn(prop2);
        properties.add(prop2);
        MockAbstractJcrNode prop3Node = createChild(session, "prop3", 1, children, child);
        node.setChildren(children);
        assertThat(node.getProperty("child/prop2"), is(prop2));
        // Ensure we return a property even when a child exists with the same name
        Property prop3 = Mockito.mock(Property.class);
        stub(prop3.getName()).toReturn("prop3");
        properties.add(prop3);
        stub(session.getItem("/node/child/prop3")).toReturn(prop3Node);
        assertThat(node.getProperty("child/prop3"), is(prop3));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetPropertyWithNullPath() throws Exception {
        node.getProperty(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetPropertyWithEmptyPath() throws Exception {
        node.getProperty("");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotProvideChildPropertyIfNotAvailable() throws Exception {
        node.getProperty("prop1");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldNotProvideDescendentPropertyIfNotAvailable() throws Exception {
        MockAbstractJcrNode child = createChild(session, "child", 1, children, node);
        Set<Property> properties = new HashSet<Property>();
        child.setProperties(properties);
        MockAbstractJcrNode propNode = createChild(session, "prop", 1, children, child);
        node.setChildren(children);
        stub(session.getItem("/node/child/prop")).toReturn(propNode);
        node.getProperty("child/prop");
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowGetReferences() throws Exception {
        node.getReferences();
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat((JcrSession)node.getSession(), is(session));
    }

    @Test
    public void shouldProvideUuidIfReferenceable() throws Exception {
        String uuid = "uuid";
        Property mixinProp = Mockito.mock(Property.class);
        stub(mixinProp.getName()).toReturn("jcr:mixinTypes");
        Value value = Mockito.mock(Value.class);
        stub(value.getString()).toReturn("mix:referenceable");
        stub(mixinProp.getValues()).toReturn(new Value[] {value});
        properties.add(mixinProp);
        Property uuidProp = Mockito.mock(Property.class);
        stub(uuidProp.getName()).toReturn("jcr:uuid");
        stub(uuidProp.getString()).toReturn(uuid);
        properties.add(uuidProp);
        assertThat(node.getUUID(), is(uuid));
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotProvideUuidIfNotReferenceable() throws Exception {
        String uuid = "uuid";
        Property mixinProp = Mockito.mock(Property.class);
        stub(mixinProp.getName()).toReturn("jcr:mixinTypes");
        Value value = Mockito.mock(Value.class);
        stub(mixinProp.getValues()).toReturn(new Value[] {value});
        properties.add(mixinProp);
        Property uuidProp = Mockito.mock(Property.class);
        stub(uuidProp.getName()).toReturn("jcr:uuid");
        stub(uuidProp.getString()).toReturn(uuid);
        properties.add(uuidProp);
        node.getUUID();
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotProvideUuidIfNoMixinTypes() throws Exception {
        String uuid = "uuid";
        Property uuidProp = Mockito.mock(Property.class);
        stub(uuidProp.getName()).toReturn("jcr:uuid");
        stub(uuidProp.getString()).toReturn(uuid);
        properties.add(uuidProp);
        node.getUUID();
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowGetVersionHistory() throws Exception {
        node.getVersionHistory();
    }

    @Test
    public void shouldProvideHasNode() throws Exception {
        assertThat(node.hasNode("{}child"), is(false));
        Property prop = Mockito.mock(Property.class);
        stub(prop.getName()).toReturn("prop");
        properties.add(prop);
        assertThat(node.hasNode("prop"), is(false));
        Node child = createChild(session, "child", 1, children, node);
        Node child2 = createChild(session, "child2", 1, children, child);
        node.setChildren(children);
        assertThat(node.hasNode("child"), is(true));
        stub(session.getItem("/node/child/{}child2")).toReturn(child2);
        assertThat(node.hasNode("child/{}child2"), is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasNodeWithNoPath() throws Exception {
        node.hasNode(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasNodeWithEmptyPath() throws Exception {
        node.hasNode("");
    }

    @Test
    public void shouldProvideHasNodes() throws Exception {
        assertThat(node.hasNodes(), is(false));
        createChild(session, "child", 1, children, node);
        node.setChildren(children);
        assertThat(node.hasNodes(), is(true));
    }

    @Test
    public void shouldProvideHasProperties() throws Exception {
        assertThat(node.hasProperties(), is(false));
        properties.add(Mockito.mock(Property.class));
        assertThat(node.hasProperties(), is(true));
    }

    @Test
    public void shouldIndicateHasProperty() throws Exception {
        assertThat(node.hasProperty("prop"), is(false));
        MockAbstractJcrNode child = createChild(session, "child", 1, children, node);
        node.setChildren(children);
        assertThat(node.hasProperty("child"), is(false));
        Property prop = Mockito.mock(Property.class);
        stub(prop.getName()).toReturn("prop");
        properties.add(prop);
        assertThat(node.hasProperty("prop"), is(true));
        Set<Property> properties = new HashSet<Property>();
        child.setProperties(properties);
        Property prop2 = Mockito.mock(Property.class);
        stub(prop2.getName()).toReturn("prop2");
        properties.add(prop2);
        stub(session.getItem("/node/child/prop2")).toReturn(prop2);
        assertThat(node.hasProperty("child/prop2"), is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasPropertyWithNoPath() throws Exception {
        node.hasProperty(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasPropertyWithEmptyPath() throws Exception {
        node.hasProperty("");
    }

    @Test
    public void shouldNotAllowHoldsLock() throws Exception {
        assertThat(node.holdsLock(), is(false));
    }

    @Test
    public void shouldNotAllowIsCheckedOut() throws Exception {
        assertThat(node.isCheckedOut(), is(false));
    }

    @Test
    public void shouldNotAllowIsLocked() throws Exception {
        assertThat(node.isLocked(), is(false));
    }

    @Test
    public void shouldIndicateIsNode() {
        assertThat(node.isNode(), is(true));
    }

    @Test
    public void shouldProvideIsSame() throws Exception {
        stub(session.getWorkspace()).toReturn(Mockito.mock(Workspace.class));
        JcrSession session2 = Mockito.mock(JcrSession.class);
        Node node2 = new MockAbstractJcrNode(session2, node.getName(), node.getParent());
        assertThat(node.isSame(node2), is(false));
        Property prop = Mockito.mock(Property.class);
        stub(prop.getSession()).toReturn(session);
        assertThat(node.isSame(prop), is(false));
        node2 = Mockito.mock(Node.class);
        stub(node2.getSession()).toReturn(session);
        assertThat(node.isSame(node2), is(false));
        node2 = new MockAbstractJcrNode(session, node.getName(), node.getParent());
        UUID uuid = UUID.randomUUID();
        node.setInternalUuid(uuid);
        ((MockAbstractJcrNode)node2).setInternalUuid(UUID.randomUUID());
        assertThat(node.isSame(node2), is(false));
        ((MockAbstractJcrNode)node2).setInternalUuid(uuid);
        assertThat(node.isSame(node2), is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowIsSameWithNoItem() throws Exception {
        node.isSame(null);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowLock() throws Exception {
        node.lock(false, false);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowMerge() throws Exception {
        node.merge(null, false);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowOrderBefore() throws Exception {
        node.orderBefore(null, null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRemoveMixin() throws Exception {
        node.removeMixin(null);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowRestoreVersionName() throws Exception {
        node.restore((String)null, false);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowRestoreVersion() throws Exception {
        node.restore((Version)null, false);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowRestoreVersionAtPath() throws Exception {
        node.restore(null, null, false);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowRestoreByLabel() throws Exception {
        node.restoreByLabel(null, false);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetBooleanProperty() throws Exception {
        node.setProperty(null, false);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetCalendarProperty() throws Exception {
        node.setProperty(null, (Calendar)null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetDoubleProperty() throws Exception {
        node.setProperty(null, 0.0);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetInputStreamProperty() throws Exception {
        node.setProperty(null, (InputStream)null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetLongProperty() throws Exception {
        node.setProperty(null, 0L);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetNodeProperty() throws Exception {
        node.setProperty(null, (Node)null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetStringProperty() throws Exception {
        node.setProperty(null, (String)null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetStringsProperty() throws Exception {
        node.setProperty(null, (String[])null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetValueProperty() throws Exception {
        node.setProperty(null, (Value)null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetValuesProperty() throws Exception {
        node.setProperty(null, (Value[])null);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetStringPropertyWithType() throws Exception {
        node.setProperty(null, (String)null, 0);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetStringsPropertyWithType() throws Exception {
        node.setProperty(null, (String[])null, 0);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetValuePropertyWithType() throws Exception {
        node.setProperty(null, (Value)null, 0);
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowSetValuesPropertyWithType() throws Exception {
        node.setProperty(null, (Value[])null, 0);
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowUnlock() throws Exception {
        node.unlock();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowUpdate() throws Exception {
        node.update(null);
    }
}
