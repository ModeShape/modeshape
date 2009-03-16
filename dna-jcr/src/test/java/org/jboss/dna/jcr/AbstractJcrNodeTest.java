/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.jcr.SessionCache.Children;
import org.jboss.dna.jcr.SessionCache.NodeInfo;
import org.jboss.dna.jcr.SessionCache.PropertyInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class AbstractJcrNodeTest {

    static class MockAbstractJcrNode extends AbstractJcrNode {

        MockAbstractJcrNode( SessionCache cache,
                             UUID uuid ) {
            super(cache, uuid);
        }

        @Override
        boolean isRoot() {
            return false;
        }

        @Override
        public int getDepth() {
            return 0;
        }

        public int getIndex() {
            return 0;
        }

        public String getName() throws RepositoryException {
            return cache.getPathFor(nodeInfo()).getLastSegment().getString(namespaces());
        }

        public Node getParent() throws RepositoryException {
            return cache.findJcrNode(nodeInfo().getParent());
        }

        public String getPath() throws RepositoryException {
            return cache.getPathFor(nodeInfo()).getString(namespaces());
        }
    }

    private ExecutionContext context;
    private UUID uuid;
    private AbstractJcrNode node;
    private Children children;
    @Mock
    private SessionCache cache;
    @Mock
    private JcrSession session;
    @Mock
    private JcrNodeTypeManager nodeTypes;
    @Mock
    private NodeInfo info;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        context.getNamespaceRegistry().register("acme", "http://www.example.com");
        uuid = UUID.randomUUID();
        stub(cache.session()).toReturn(session);
        stub(cache.context()).toReturn(context);
        stub(cache.workspaceName()).toReturn("my workspace");
        stub(cache.findNodeInfo(uuid)).toReturn(info);
        stub(cache.getPathFor(info)).toReturn(path("/a/b/c/d"));
        stub(session.nodeTypeManager()).toReturn(nodeTypes);
        node = new MockAbstractJcrNode(cache, uuid);

        // Create the children container for the node ...
        children = new Children(uuid);
        stub(info.getChildren()).toReturn(children);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Path relativePath( String relativePath ) {
        return context.getValueFactories().getPathFactory().create(relativePath);
    }

    protected Path path( String absolutePath ) {
        return context.getValueFactories().getPathFactory().create(absolutePath);
    }

    protected Value stringValueFor( Object value ) {
        return new JcrValue(context.getValueFactories(), PropertyType.STRING, value);
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

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowNegativeAncestorDepth() throws Exception {
        node.getAncestor(-1);
    }

    @Test
    public void shouldProvideAncestor() throws Exception {
        assertThat(node.getAncestor(0), is((Item)node));
    }

    @Test
    public void shouldReturnPropertyFromGetPropertyWithValidName() throws Exception {
        PropertyId propertyId = new PropertyId(uuid, name("test"));
        AbstractJcrProperty property = mock(AbstractJcrProperty.class);
        stub(cache.findJcrProperty(propertyId)).toReturn(property);
        assertThat(node.getProperty("test"), is((Property)property));
    }

    @Test
    public void shouldReturnPropertyFromGetPropertyWithValidRelativePath() throws Exception {
        PropertyId propertyId = new PropertyId(uuid, name("test"));
        AbstractJcrProperty property = mock(AbstractJcrProperty.class);
        stub(cache.findJcrProperty(propertyId)).toReturn(property);
        assertThat(node.getProperty("test"), is((Property)property));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnPropertyFromGetPropertyWithNameOfPropertyThatDoesNotExist() throws Exception {
        node.getProperty("nonExistantProperty");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToReturnPropertyFromGetPropertyWithAbsolutePath() throws Exception {
        node.getProperty("/test");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnPropertyFromGetPropertyWithRelativePathToNonExistantItem() throws Exception {
        node.getProperty("../bogus/path");
    }

    @Test
    public void shouldReturnPropertyFromGetPropertyWithRelativePathToPropertyOnOtherNode() throws Exception {
        AbstractJcrProperty property = mock(AbstractJcrProperty.class);
        stub(cache.findJcrItem(uuid, relativePath("../good/path"))).toReturn(property);
        assertThat(node.getProperty("../good/path"), is((Property)property));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldReturnPropertyFromGetPropertyWithRelativePathToOtherNode() throws Exception {
        AbstractJcrNode otherNode = mock(AbstractJcrNode.class);
        stub(cache.findJcrItem(uuid, relativePath("../good/path"))).toReturn(otherNode);
        node.getProperty("../good/path");
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

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetPropertyWithNullPath() throws Exception {
        node.getProperty((String)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetPropertyWithEmptyPath() throws Exception {
        node.getProperty("");
    }

    @Test
    public void shouldReturnChildNodeFromGetNodeWithValidName() throws Exception {
        AbstractJcrNode child = mock(AbstractJcrNode.class);
        UUID childUuid = UUID.randomUUID();
        children.append(cache, name("child"), childUuid);
        stub(cache.findJcrNode(childUuid)).toReturn(child);
        assertThat(node.getNode("child"), is((Node)child));
    }

    @Test
    public void shouldReturnNonChildNodeFromGetNodeWithValidRelativePath() throws Exception {
        AbstractJcrNode otherNode = mock(AbstractJcrNode.class);
        stub(cache.findJcrItem(uuid, path("../other/node"))).toReturn(otherNode);
        assertThat(node.getNode("../other/node"), is((Node)otherNode));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnNodeFromGetNodeWithValidRelativePathToProperty() throws Exception {
        AbstractJcrProperty property = mock(AbstractJcrProperty.class);
        stub(cache.findJcrItem(uuid, path("../other/node"))).toReturn(property);
        node.getNode("../other/node");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnNodeFromGetNodeWithValidRelativePathToNoNodeOrProperty() throws Exception {
        node.getNode("../other/node");
    }

    @Test
    public void shouldReturnSelfFromGetNodeWithRelativePathContainingOnlySelfReference() throws Exception {
        assertThat(node.getNode("."), is((Node)node));
    }

    @Test
    public void shouldReturnParentFromGetNodeWithRelativePathContainingOnlyParentReference() throws Exception {
        AbstractJcrNode parent = mock(AbstractJcrNode.class);
        UUID parentUuid = UUID.randomUUID();
        stub(info.getParent()).toReturn(parentUuid);
        stub(cache.findJcrNode(parentUuid)).toReturn(parent);
        assertThat(node.getNode(".."), is((Node)parent));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnChildNodeFromGetNodeWithNameOfChildThatDoesNotExist() throws Exception {
        node.getNode("something");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetNodeWithNoPath() throws Exception {
        node.getNode(null);
    }

    @Test
    public void shouldProvideNodeIterator() throws Exception {
        AbstractJcrNode child = mock(AbstractJcrNode.class);
        UUID childUuid = UUID.randomUUID();
        children.append(cache, name("child"), childUuid);
        stub(cache.findJcrNode(childUuid)).toReturn(child);
        NodeIterator iter = node.getNodes();
        assertThat(iter, notNullValue());
        assertThat(iter.getSize(), is(1L));
        assertThat(iter.next(), is((Object)child));
    }

    @Test
    public void shoudReturnItemFromGetPrimaryItemIfItExists() throws Exception {
        // Define the node type ...
        Name primaryTypeName = name("thePrimaryType");
        JcrNodeType primaryType = mock(JcrNodeType.class);
        stub(nodeTypes.getNodeType(primaryTypeName)).toReturn(primaryType);
        stub(primaryType.getPrimaryItemName()).toReturn("thePrimaryItemName");
        // Define the node info to use this primary type ...
        stub(info.getPrimaryTypeName()).toReturn(primaryTypeName);
        // Now make an item with the appropriate name ...
        AbstractJcrNode child = mock(AbstractJcrNode.class);
        stub(cache.findJcrItem(uuid, path("thePrimaryItemName"))).toReturn(child);
        // Now call the method ...
        assertThat(node.getPrimaryItem(), is((Item)child));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shoudFailToReturnItemFromGetPrimaryItemIfPrimaryTypeDoesNotHavePrimaryItemName() throws Exception {
        // Define the node type ...
        Name primaryTypeName = name("thePrimaryType");
        JcrNodeType primaryType = mock(JcrNodeType.class);
        stub(nodeTypes.getNodeType(primaryTypeName)).toReturn(primaryType);
        stub(primaryType.getPrimaryItemName()).toReturn(null);
        // Define the node info to use this primary type ...
        stub(info.getPrimaryTypeName()).toReturn(primaryTypeName);
        // Now call the method ...
        node.getPrimaryItem();
    }

    @Test( expected = ItemNotFoundException.class )
    public void shoudFailToReturnItemFromGetPrimaryItemIfThePrimaryTypeAsInvalidPrimaryItemName() throws Exception {
        // Define the node type ...
        Name primaryTypeName = name("thePrimaryType");
        JcrNodeType primaryType = mock(JcrNodeType.class);
        stub(nodeTypes.getNodeType(primaryTypeName)).toReturn(primaryType);
        stub(primaryType.getPrimaryItemName()).toReturn("/this/is/not/valid");
        // Define the node info to use this primary type ...
        stub(info.getPrimaryTypeName()).toReturn(primaryTypeName);
        // Now call the method ...
        node.getPrimaryItem();
    }

    @Test( expected = ItemNotFoundException.class )
    public void shoudFailToReturnItemFromGetPrimaryItemIfTheNodeHasNoItemMatchingThatSpecifiedByThePrimaryType() throws Exception {
        // Define the node type ...
        Name primaryTypeName = name("thePrimaryType");
        JcrNodeType primaryType = mock(JcrNodeType.class);
        stub(nodeTypes.getNodeType(primaryTypeName)).toReturn(primaryType);
        stub(primaryType.getPrimaryItemName()).toReturn("thePrimaryItemName");
        // Define the node info to use this primary type ...
        stub(info.getPrimaryTypeName()).toReturn(primaryTypeName);
        // Now call the method ...
        stub(cache.findJcrItem(uuid, path("thePrimaryItemName"))).toThrow(new ItemNotFoundException());
        node.getPrimaryItem();
    }

    @Test
    public void shouldReturnEmptyIteratorFromGetReferencesWhenThereAreNoProperties() throws Exception {
        // Set up two properties (one containing references, the other not) ...
        List<AbstractJcrProperty> props = Arrays.asList(new AbstractJcrProperty[] {});
        stub(cache.findJcrPropertiesFor(uuid)).toReturn(props);
        // Now call the method ...
        PropertyIterator iter = node.getReferences();
        assertThat(iter.getSize(), is(0L));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldReturnEmptyIteratorFromGetReferencesWhenThereAreNoReferenceProperties() throws Exception {
        // Set up two properties (one containing references, the other not) ...
        AbstractJcrProperty propertyA = mock(AbstractJcrProperty.class);
        AbstractJcrProperty propertyB = mock(AbstractJcrProperty.class);
        List<AbstractJcrProperty> props = Arrays.asList(new AbstractJcrProperty[] {propertyA, propertyB});
        stub(cache.findJcrPropertiesFor(uuid)).toReturn(props);
        stub(propertyA.getType()).toReturn(PropertyType.LONG);
        stub(propertyB.getType()).toReturn(PropertyType.BOOLEAN);
        // Now call the method ...
        PropertyIterator iter = node.getReferences();
        assertThat(iter.getSize(), is(0L));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldReturnIteratorFromGetReferencesWhenThereIsAtLeastOneReferenceProperty() throws Exception {
        // Set up two properties (one containing references, the other not) ...
        AbstractJcrProperty propertyA = mock(AbstractJcrProperty.class);
        AbstractJcrProperty propertyB = mock(AbstractJcrProperty.class);
        List<AbstractJcrProperty> props = Arrays.asList(new AbstractJcrProperty[] {propertyA, propertyB});
        stub(cache.findJcrPropertiesFor(uuid)).toReturn(props);
        stub(propertyA.getType()).toReturn(PropertyType.LONG);
        stub(propertyB.getType()).toReturn(PropertyType.REFERENCE);
        // Now call the method ...
        PropertyIterator iter = node.getReferences();
        assertThat(iter.getSize(), is(1L));
        assertThat(iter.next(), is(sameInstance((Object)propertyB)));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat((JcrSession)node.getSession(), is(session));
    }

    @Test
    public void shouldProvideUuidIfReferenceable() throws Exception {
        // Create the property ...
        PropertyId propertyId = new PropertyId(uuid, name("jcr:mixinTypes"));
        AbstractJcrProperty property = mock(AbstractJcrProperty.class);
        stub(cache.findJcrProperty(propertyId)).toReturn(property);
        stub(property.getValues()).toReturn(new Value[] {stringValueFor("acme:someMixin"), stringValueFor("mix:referenceable")});
        // Call the method ...
        assertThat(node.getUUID(), is(uuid.toString()));
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotProvideUuidIfNotReferenceable() throws Exception {
        // Create the property ...
        PropertyId propertyId = new PropertyId(uuid, name("jcr:mixinTypes"));
        AbstractJcrProperty property = mock(AbstractJcrProperty.class);
        stub(cache.findJcrProperty(propertyId)).toReturn(property);
        stub(property.getValues()).toReturn(new Value[] {stringValueFor("acme:someMixin"), stringValueFor("mix:notReferenceable")});
        // Call the method ...
        node.getUUID();
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotProvideUuidIfNoMixinTypes() throws Exception {
        PropertyId propertyId = new PropertyId(uuid, name("jcr:mixinTypes"));
        stub(cache.findJcrProperty(propertyId)).toReturn(null);
        // Call the method ...
        node.getUUID();
    }

    @Test( expected = UnsupportedRepositoryOperationException.class )
    public void shouldNotAllowGetVersionHistory() throws Exception {
        node.getVersionHistory();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullPathInHasNode() throws Exception {
        node.hasNode((String)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyPathInHasNode() throws Exception {
        node.hasNode("");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAbsolutePathInHasNode() throws Exception {
        node.hasNode("/a/b/c");
    }

    @Test
    public void shouldReturnTrueFromHasNodeWithSelfReference() throws Exception {
        assertThat(node.hasNode("."), is(true));
    }

    @Test
    public void shouldReturnTrueFromHasNodeWithParentReferenceIfNotRootNode() throws Exception {
        assertThat(node.hasNode(".."), is(!node.isRoot()));
    }

    @Test
    public void shouldReturnTrueFromHasNodeIfRelativePathIdentifiesExistingNode() throws Exception {
        AbstractJcrNode otherNode = mock(AbstractJcrNode.class);
        stub(cache.findJcrNode(uuid, path("../other/node"))).toReturn(otherNode);
        assertThat(node.hasNode("../other/node"), is(true));
    }

    @Test
    public void shouldReturnTrueFromHasNodeIfChildWithNameExists() throws Exception {
        children.append(cache, name("child"), UUID.randomUUID());
        assertThat(node.hasNode("child[1]"), is(true));
        assertThat(node.hasNode("child[2]"), is(false));
        children.append(cache, name("child"), UUID.randomUUID());
        assertThat(node.hasNode("child[2]"), is(true));
        assertThat(node.hasNode("child[3]"), is(false));
    }

    @Test
    public void shouldReturnFalseFromHasNodeWithNameOfChildThatDoesNotExist() throws Exception {
        assertThat(node.hasNode("something"), is(false));
    }

    @Test
    public void shouldReturnFalseFromHasNodesIfThereAreNoChildren() throws Exception {
        assertThat(node.hasNodes(), is(false));
    }

    @Test
    public void shouldReturnTrueFromHasNodesIfThereIsAtLeastOneChild() throws Exception {
        children.append(cache, name("child"), UUID.randomUUID());
        assertThat(node.hasNodes(), is(true));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullPathInHasProperty() throws Exception {
        node.hasProperty((String)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowEmptyPathInHasProperty() throws Exception {
        node.hasProperty("");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAbsolutePathInHasProperty() throws Exception {
        node.hasProperty("/a/b/c");
    }

    @Test
    public void shouldReturnTrueFromHasPropertyIfPathIsRelativePathToOtherNodeWithNamedProperty() throws Exception {
        AbstractJcrProperty property = mock(AbstractJcrProperty.class);
        stub(cache.findJcrItem(uuid, relativePath("../good/path"))).toReturn(property);
        assertThat(node.hasProperty("../good/path"), is(true));
    }

    @Test
    public void shouldReturnFalseFromHasPropertyIfPathIsRelativePathToOtherNodeWithoutNamedProperty() throws Exception {
        stub(cache.findJcrItem(uuid, relativePath("../good/path"))).toThrow(new PathNotFoundException());
        assertThat(node.hasProperty("../good/path"), is(false));
    }

    @Test
    public void shouldReturnFalseFromHasPropertyIfPathIsParentPath() throws Exception {
        assertThat(node.hasProperty(".."), is(false));
    }

    @Test
    public void shouldReturnFalseFromHasPropertyIfPathIsSelfPath() throws Exception {
        assertThat(node.hasProperty("."), is(false));
    }

    @Test
    public void shouldReturnTrueFromHasPropertyIfPathIsNameAndNodeHasProperty() throws Exception {
        PropertyInfo propertyInfo = mock(PropertyInfo.class);
        stub(cache.findPropertyInfo(new PropertyId(uuid, name("prop")))).toReturn(propertyInfo);
        assertThat(node.hasProperty("prop"), is(true));
    }

    @Test
    public void shouldReturnFalseFromHasPropertyIfPathIsNameAndNodeDoesNotHaveProperty() throws Exception {
        stub(cache.findPropertyInfo(new PropertyId(uuid, name("prop")))).toReturn(null);
        assertThat(node.hasProperty("prop"), is(false));
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void shouldReturnTrueFromHasPropertiesIfNodeHasAtLeastOneProperty() throws Exception {
        Map<Name, PropertyInfo> properties = mock(Map.class);
        stub(properties.size()).toReturn(5);
        stub(info.getProperties()).toReturn(properties);
        assertThat(node.hasProperties(), is(true));
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void shouldReturnFalseFromHasPropertiesIfNodeHasNoProperties() throws Exception {
        Map<Name, PropertyInfo> properties = mock(Map.class);
        stub(properties.size()).toReturn(0);
        stub(info.getProperties()).toReturn(properties);
        assertThat(node.hasProperties(), is(false));
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
    public void shouldReturnFalseFromIsSameIfTheRepositoryInstanceIsDifferent() throws Exception {
        Workspace workspace1 = mock(Workspace.class);
        Repository repository1 = mock(Repository.class);
        stub(session.getWorkspace()).toReturn(workspace1);
        stub(session.getRepository()).toReturn(repository1);
        stub(workspace1.getName()).toReturn("workspace1");

        Workspace workspace2 = mock(Workspace.class);
        JcrSession session2 = Mockito.mock(JcrSession.class);
        Repository repository2 = mock(Repository.class);
        SessionCache cache2 = mock(SessionCache.class);
        stub(session2.getWorkspace()).toReturn(workspace2);
        stub(session2.getRepository()).toReturn(repository2);
        stub(workspace2.getName()).toReturn("workspace1");
        stub(cache2.session()).toReturn(session2);

        UUID uuid2 = uuid;
        Node node2 = new MockAbstractJcrNode(cache2, uuid2);
        assertThat(node.isSame(node2), is(false));
    }

    @Test
    public void shouldReturnFalseFromIsSameIfTheWorkspaceNameIsDifferent() throws Exception {
        Workspace workspace1 = mock(Workspace.class);
        Repository repository1 = mock(Repository.class);
        stub(session.getWorkspace()).toReturn(workspace1);
        stub(session.getRepository()).toReturn(repository1);
        stub(workspace1.getName()).toReturn("workspace1");

        Workspace workspace2 = mock(Workspace.class);
        JcrSession session2 = Mockito.mock(JcrSession.class);
        SessionCache cache2 = mock(SessionCache.class);
        stub(session2.getWorkspace()).toReturn(workspace2);
        stub(session2.getRepository()).toReturn(repository1);
        stub(workspace2.getName()).toReturn("workspace2");
        stub(cache2.session()).toReturn(session2);

        UUID uuid2 = uuid;
        Node node2 = new MockAbstractJcrNode(cache2, uuid2);
        assertThat(node.isSame(node2), is(false));
    }

    @Test
    public void shouldReturnFalseFromIsSameIfTheNodeUuidIsDifferent() throws Exception {
        Workspace workspace1 = mock(Workspace.class);
        Repository repository1 = mock(Repository.class);
        stub(session.getWorkspace()).toReturn(workspace1);
        stub(session.getRepository()).toReturn(repository1);
        stub(workspace1.getName()).toReturn("workspace1");

        Workspace workspace2 = mock(Workspace.class);
        JcrSession session2 = Mockito.mock(JcrSession.class);
        SessionCache cache2 = mock(SessionCache.class);
        stub(session2.getWorkspace()).toReturn(workspace2);
        stub(session2.getRepository()).toReturn(repository1);
        stub(workspace2.getName()).toReturn("workspace1");
        stub(cache2.session()).toReturn(session2);

        UUID uuid2 = UUID.randomUUID();
        Node node2 = new MockAbstractJcrNode(cache2, uuid2);
        assertThat(node.isSame(node2), is(false));
    }

    @Test
    public void shouldReturnTrueFromIsSameIfTheNodeUuidAndWorkspaceNameAndRepositoryInstanceAreSame() throws Exception {
        Workspace workspace1 = mock(Workspace.class);
        Repository repository1 = mock(Repository.class);
        stub(session.getWorkspace()).toReturn(workspace1);
        stub(session.getRepository()).toReturn(repository1);
        stub(workspace1.getName()).toReturn("workspace1");

        Workspace workspace2 = mock(Workspace.class);
        JcrSession session2 = Mockito.mock(JcrSession.class);
        SessionCache cache2 = mock(SessionCache.class);
        stub(session2.getWorkspace()).toReturn(workspace2);
        stub(session2.getRepository()).toReturn(repository1);
        stub(workspace2.getName()).toReturn("workspace1");
        stub(cache2.session()).toReturn(session2);

        UUID uuid2 = uuid;
        Node node2 = new MockAbstractJcrNode(cache2, uuid2);
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
