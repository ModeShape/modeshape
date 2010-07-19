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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.HashMap;
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
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinitionTemplate;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeTemplate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;

/**
 * 
 */
public class AbstractJcrNodeTest extends AbstractJcrTest {

    private static final String ID_PATH = "[" + UUID.randomUUID() + "]";

    private AbstractJcrNode rootNode;
    private AbstractJcrNode cars;
    private AbstractJcrNode hybrid;
    private AbstractJcrNode prius;
    private AbstractJcrNode highlander;
    private AbstractJcrNode altima;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        rootNode = cache.findJcrRootNode();
        cars = cache.findJcrNode(null, path("/Cars"));
        hybrid = cache.findJcrNode(null, path("/Cars/Hybrid"));
        prius = cache.findJcrNode(null, path("/Cars/Hybrid/Toyota Prius"));
        highlander = cache.findJcrNode(null, path("/Cars/Hybrid/Toyota Highlander"));
        altima = cache.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
    }

    /*
     * Visitors
     */

    @Test
    public void shouldAllowVisitation() throws Exception {
        ItemVisitor visitor = Mockito.mock(ItemVisitor.class);
        hybrid.accept(visitor);
        Mockito.verify(visitor).visit(hybrid);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowVisitationIfNoVisitor() throws Exception {
        hybrid.accept(null);
    }

    /*
     * Ancestors
     */

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowNegativeAncestorDepth() throws Exception {
        hybrid.getAncestor(-1);
    }

    @Test
    public void shouldReturnRootForAncestorOfDepthZero() throws Exception {
        assertThat(hybrid.getAncestor(0), is((Item)rootNode));
    }

    @Test
    public void shouldReturnAncestorAtLevelOneForAncestorOfDepthOne() throws Exception {
        assertThat(hybrid.getAncestor(1), is((Item)cars));
    }

    @Test
    public void shouldReturnSelfForAncestorOfDepthEqualToDepthOfNode() throws Exception {
        assertThat(hybrid.getAncestor(hybrid.getDepth()), is((Item)hybrid));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldFailToReturnAncestorWhenDepthIsGreaterThanNodeDepth() throws Exception {
        hybrid.getAncestor(hybrid.getDepth() + 1);
    }

    @Test
    public void shouldIndicateIsNode() {
        assertThat(prius.isNode(), is(true));
    }

    /*
     * Property-related methods
     */

    @Test
    public void shouldReturnPropertyFromGetPropertyWithValidName() throws Exception {
        javax.jcr.Property property = prius.getProperty("vehix:model");
        assertThat(property, is(notNullValue()));
        assertThat(property.getName(), is("vehix:model"));
        assertThat(property.getString(), is("Prius"));
    }

    @Test
    public void shouldReturnPropertyFromGetPropertyWithValidRelativePath() throws Exception {
        javax.jcr.Property property = prius.getProperty("./vehix:model");
        assertThat(property, is(notNullValue()));
        assertThat(property.getName(), is("vehix:model"));
        assertThat(property.getString(), is("Prius"));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnPropertyFromGetPropertyWithNameOfPropertyThatDoesNotExist() throws Exception {
        prius.getProperty("nonExistantProperty");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToReturnPropertyFromGetPropertyWithAbsolutePath() throws Exception {
        prius.getProperty("/test");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnPropertyFromGetPropertyWithRelativePathToNonExistantItem() throws Exception {
        prius.getProperty("../bogus/path");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToReturnPropertyFromGetPropertyWithIdentifierPath() throws Exception {
        prius.getProperty(ID_PATH);
    }

    @Test
    public void shouldReturnPropertyFromGetPropertyWithRelativePathToPropertyOnOtherNode() throws Exception {
        javax.jcr.Property property = prius.getProperty("../Nissan Altima/vehix:model");
        assertThat(property, is(notNullValue()));
        assertThat(property.getName(), is("vehix:model"));
        assertThat(property.getString(), is("Altima"));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldReturnPropertyFromGetPropertyWithRelativePathToOtherNode() throws Exception {
        prius.getProperty("../Nissan Altima");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetPropertyWithNullPath() throws Exception {
        prius.getProperty((String)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetPropertyWithEmptyPath() throws Exception {
        prius.getProperty("");
    }

    @Test
    public void shouldReturnTrueFromHasPropertyWithValidName() throws Exception {
        assertThat(prius.hasProperty("vehix:model"), is(true));
    }

    @Test
    public void shouldReturnTrueFromHasPropertyWithValidRelativePath() throws Exception {
        assertThat(prius.hasProperty("./vehix:model"), is(true));
    }

    @Test
    public void shouldReturnFalseFromHasPropertyWithNameOfPropertyThatDoesNotExist() throws Exception {
        assertThat(prius.hasProperty("non-existant"), is(false));
    }

    @Test
    public void shouldReturnFalseFromHasPropertyWithRelativePathToNonExistantItem() throws Exception {
        assertThat(prius.hasProperty("../Nissan Altima/non-existant"), is(false));
    }

    @Test
    public void shouldReturnTrueFromHasPropertyWithRelativePathToPropertyOnOtherNode() throws Exception {
        assertThat(prius.hasProperty("../Nissan Altima/vehix:model"), is(true));
    }

    @Test
    public void shouldReturnFalseFromHasPropertyWithRelativePathToOtherNode() throws Exception {
        assertThat(prius.hasProperty("../Nissan Altima"), is(false));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasNodeWithIdentifierPath() throws Exception {
        prius.hasNode(ID_PATH);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasNodeWithAbsolutePath() throws Exception {
        prius.hasNode("/something");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasPropertyWithAbsolutePath() throws Exception {
        prius.hasProperty("/Cars/Hybrid/Toyota Prius/vehix:model");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasPropertyWithNullPath() throws Exception {
        prius.hasProperty((String)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasPropertyWithEmptyPath() throws Exception {
        prius.hasProperty("");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasPropertyWithIdentifierPath() throws Exception {
        prius.hasProperty(ID_PATH);
    }

    @Test
    public void shouldReturnFalseFromHasPropertyIfPathIsParentPath() throws Exception {
        assertThat(prius.hasProperty(".."), is(false));
    }

    @Test
    public void shouldReturnFalseFromHasPropertyIfPathIsSelfPath() throws Exception {
        assertThat(prius.hasProperty("."), is(false));
    }

    @Test
    public void shouldReturnTrueFromHasPropertiesIfNodeHasAtLeastOneProperty() throws Exception {
        assertThat(prius.hasProperties(), is(true));
    }

    @Test
    public void shouldReturnFalseFromHasPropertiesIfNodeHasNoProperties() throws Exception {
        // can't really test this, since all nodes have at least one property ...
        // assertThat(child.hasProperties(), is(false));
    }

    /*
     *  getNode() and related methods
     */

    @Test
    public void shouldReturnChildNodeFromGetNodeWithValidName() throws Exception {
        assertThat(hybrid.getNode("Toyota Prius"), is((javax.jcr.Node)prius));
        assertThat(hybrid.getNode("Toyota Prius"), is(sameInstance((javax.jcr.Node)prius)));
    }

    @Test
    public void shouldReturnNonChildNodeFromGetNodeWithValidRelativePath() throws Exception {
        assertThat(altima.getNode("../Toyota Prius"), is((javax.jcr.Node)prius));
        assertThat(altima.getNode("../Toyota Prius"), is(sameInstance((javax.jcr.Node)prius)));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnNodeFromGetNodeWithValidRelativePathToProperty() throws Exception {
        altima.getNode("../Toyota Prius/vehix:model");
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnNodeFromGetNodeWithValidRelativePathToNoNodeOrProperty() throws Exception {
        altima.getNode("../../nonExistant");
    }

    @Test
    public void shouldReturnSelfFromGetNodeWithRelativePathContainingOnlySelfReference() throws Exception {
        assertThat(hybrid.getNode("."), is((javax.jcr.Node)hybrid));
        assertThat(hybrid.getNode("."), is(sameInstance((javax.jcr.Node)hybrid)));
    }

    @Test
    public void shouldReturnSelfFromGetNodeWithRelativePathResolvingToSelf() throws Exception {
        assertThat(hybrid.getNode("Toyota Prius/.."), is((javax.jcr.Node)hybrid));
        assertThat(hybrid.getNode("Toyota Prius/.."), is(sameInstance((javax.jcr.Node)hybrid)));
    }

    @Test
    public void shouldReturnParentFromGetNodeWithRelativePathContainingOnlyParentReference() throws Exception {
        assertThat(prius.getNode("../.."), is((javax.jcr.Node)cars));
        assertThat(prius.getNode("../.."), is(sameInstance((javax.jcr.Node)cars)));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToReturnChildNodeFromGetNodeWithNameOfChildThatDoesNotExist() throws Exception {
        altima.getNode("nonExistant");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetNodeWithNoPath() throws Exception {
        prius.getNode((String)null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetNodeWithIdentifierPath() throws Exception {
        prius.getNode(ID_PATH);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowGetNodeWithAbsolutePath() throws Exception {
        prius.getNode("/something");
    }

    @Test
    public void shouldProvideNodeIterator() throws Exception {
        NodeIterator iter = hybrid.getNodes();
        assertThat(iter, notNullValue());
        assertThat(iter.getSize(), is(3L));
        assertThat(iter.next(), is((Object)prius));
        assertThat(iter.next(), is((Object)highlander));
        assertThat(iter.next(), is((Object)altima));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldProvideFilteredNodeIteratorForPattern() throws Exception {
        NodeIterator iter = hybrid.getNodes(" Toyota P*|*lander ");
        assertThat(iter, notNullValue());
        assertThat(iter.getSize(), is(2L));
        assertThat(iter.next(), is((Object)prius));
        assertThat(iter.next(), is((Object)highlander));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldProvideFilteredNodeIteratorForPatternArray() throws Exception {
        NodeIterator iter = hybrid.getNodes(new String[] {" Toyota P*", "*lander"});
        assertThat(iter, notNullValue());
        assertThat(iter.getSize(), is(2L));
        assertThat(iter.next(), is((Object)prius));
        assertThat(iter.next(), is((Object)highlander));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldReturnEmptyIteratorForEmptyPattern() throws Exception {
        NodeIterator iter = hybrid.getNodes("");
        assertThat(iter, notNullValue());
        assertThat(iter.getSize(), is(0L));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldReturnEmptyIteratorForEmptyPatternArray() throws Exception {
        NodeIterator iter = hybrid.getNodes(new String[] {""});
        assertThat(iter, notNullValue());
        assertThat(iter.getSize(), is(0L));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldProvidePropertyIterator() throws Exception {
        assertThat(prius.getProperties(), notNullValue());

        Map<String, Property> properties = new HashMap<String, Property>();
        for (PropertyIterator iter = prius.getProperties(); iter.hasNext();) {
            Property prop = iter.nextProperty();
            properties.put(prop.getName(), prop);
        }

        assertThat(properties.size(), is(9));
        assertThat(properties.get("vehix:maker").getString(), is("Toyota"));
        assertThat(properties.get("vehix:model").getString(), is("Prius"));
        assertThat(properties.get("vehix:year").getString(), is("2008"));
        assertThat(properties.get("vehix:msrp").getString(), is("$21,500"));
        assertThat(properties.get("vehix:userRating").getString(), is("4.2"));
        assertThat(properties.get("vehix:valueRating").getString(), is("5"));
        assertThat(properties.get("vehix:mpgCity").getString(), is("48"));
        assertThat(properties.get("vehix:mpgHighway").getString(), is("45"));
        assertThat(properties.get("jcr:primaryType").getString(), is("vehix:car"));
        assertThat(prius.isNodeType(JcrMixLexicon.REFERENCEABLE), is(false));
        // If the node was referenceable, the UUID below should be returned
        // assertThat(properties.get("jcr:uuid").getString(), is("3b9e4a2f-040e-4d65-9778-abe70ae48324"));
    }

    @Test
    public void shouldProvidePropertyIteratorForPattern() throws Exception {
        String pattern = "vehix:ma*|vehix:mo*";
        assertThat(prius.getProperties(pattern), notNullValue());

        Map<String, Property> properties = new HashMap<String, Property>();
        for (PropertyIterator iter = prius.getProperties(pattern); iter.hasNext();) {
            Property prop = iter.nextProperty();
            properties.put(prop.getName(), prop);
        }

        assertThat(properties.size(), is(2));
        assertThat(properties.get("vehix:maker").getString(), is("Toyota"));
        assertThat(properties.get("vehix:model").getString(), is("Prius"));
    }

    @Test
    public void shouldProvideEmptyPropertyIteratorForEmptyPattern() throws Exception {
        String pattern = "";
        assertThat(prius.getProperties(pattern), notNullValue());

        Map<String, Property> properties = new HashMap<String, Property>();
        for (PropertyIterator iter = prius.getProperties(pattern); iter.hasNext();) {
            Property prop = iter.nextProperty();
            properties.put(prop.getName(), prop);
        }

        assertThat(properties.size(), is(0));
    }

    @Test
    public void shouldProvidePropertyIteratorForPatternArray() throws Exception {
        String[] pattern = new String[] {"vehix:ma*", "vehix:mo*"};
        assertThat(prius.getProperties(pattern), notNullValue());

        Map<String, Property> properties = new HashMap<String, Property>();
        for (PropertyIterator iter = prius.getProperties(pattern); iter.hasNext();) {
            Property prop = iter.nextProperty();
            properties.put(prop.getName(), prop);
        }

        assertThat(properties.size(), is(2));
        assertThat(properties.get("vehix:maker").getString(), is("Toyota"));
        assertThat(properties.get("vehix:model").getString(), is("Prius"));
    }

    @Test
    public void shouldProvideEmptyPropertyIteratorForEmptyPatternArray() throws Exception {
        String[] pattern = new String[] {""};
        assertThat(prius.getProperties(pattern), notNullValue());

        Map<String, Property> properties = new HashMap<String, Property>();
        for (PropertyIterator iter = prius.getProperties(pattern); iter.hasNext();) {
            Property prop = iter.nextProperty();
            properties.put(prop.getName(), prop);
        }

        assertThat(properties.size(), is(0));
    }

    @Test
    public void shouldReturnTrueFromHasNodeWithValidName() throws Exception {
        assertThat(hybrid.hasNode("Toyota Prius"), is(true));
    }

    @Test
    public void shouldReturnTrueFromHasNodeWithValidRelativePath() throws Exception {
        assertThat(altima.hasNode("../Toyota Prius"), is(true));
    }

    @Test
    public void shouldReturnFalseFromHasNodeWithValidRelativePathToProperty() throws Exception {
        assertThat(altima.hasNode("../Toyota Prius/vehix:model"), is(false));
    }

    @Test
    public void shouldReturnFalseFromHasNodeWithWithValidRelativePathToNoNodeOrProperty() throws Exception {
        assertThat(altima.hasNode("../../nonExistant"), is(false));
        assertThat(altima.hasNode("../nonExistant"), is(false));
        assertThat(altima.hasNode("nonExistant"), is(false));
    }

    @Test
    public void shouldReturnTrueFromHasNodeWithRelativePathContainingOnlySelfReference() throws Exception {
        assertThat(hybrid.hasNode("."), is(true));
    }

    @Test
    public void shouldReturnTrueFromHasNodeWithRelativePathResolvingToSelf() throws Exception {
        assertThat(hybrid.hasNode("Toyota Prius/.."), is(true));
    }

    @Test
    public void shouldReturnTrueFromHasNodeWithRelativePathContainingOnlyParentReference() throws Exception {
        assertThat(prius.hasNode("../.."), is(true));
    }

    @Test
    public void shouldReturnFalseFromHasNodeWithNameOfChildThatDoesNotExist() throws Exception {
        assertThat(altima.hasNode("nonExistant"), is(false));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowHasNodeWithNoPath() throws Exception {
        prius.hasNode(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAbsolutePathInHasNode() throws Exception {
        cars.hasNode("/a/b/c");
    }

    @Test
    public void shouldReturnTrueFromHasNodesIfThereIsAtLeastOneChild() throws Exception {
        assertThat(hybrid.hasNodes(), is(true));
    }

    @Test
    public void shouldReturnFalseFromHasNodesIfThereAreNoChildren() throws Exception {
        assertThat(prius.hasNodes(), is(false));
    }

    /*
     * More comprehensive tests of addMixin, removeMixin, and canAddMixin require additional setup 
     * and are in MixinTest
     */

    @Test( expected = LockException.class )
    public void shouldNotAllowGetLockIfNoLock() throws Exception {
        hybrid.getLock();
    }

    @Test
    public void shouldTreatNonVersionableNodesAsCheckedOut() throws Exception {
        assertThat(hybrid.isCheckedOut(), is(true));
    }

    @Test
    public void shouldNotAllowIsLocked() throws Exception {
        assertThat(hybrid.isLocked(), is(false));
    }

    @Test( expected = NullPointerException.class )
    public void shouldNotAllowOrderBeforeWithNullArgs() throws Exception {
        hybrid.orderBefore(null, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowOrderBeforeWithIdentifierPathAsFirstParameter() throws Exception {
        hybrid.orderBefore(ID_PATH, "/something");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowOrderBeforeWithIdentifierPathAsSecondParameter() throws Exception {
        hybrid.orderBefore("/something", ID_PATH);
    }

    /*
     * Primary-type and -item methods
     */

    @Test
    public void shouldReturnItemFromGetPrimaryNodeType() throws Exception {
        NodeType carType = nodeTypes.getNodeType("vehix:car");
        assertThat(carType, is(notNullValue()));
        assertThat(prius.getPrimaryNodeType(), is(carType));
    }

    @Test
    public void shouldReturnItemFromGetPrimaryItemIfItExists() throws Exception {
        NodeType carType = nodeTypes.getNodeType("vehix:car");
        assertThat(carType, is(notNullValue()));
        assertThat(carType.getPrimaryItemName(), is("vehix:model"));
        assertThat(prius.getPrimaryItem(), is(sameInstance((Item)prius.getProperty("vehix:model"))));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldFailToReturnItemFromGetPrimaryItemIfPrimaryTypeDoesNotHavePrimaryItemName() throws Exception {
        // Get nt:unstructured and verify it has no primary item name ...
        NodeType ntUnstructured = nodeTypes.getNodeType("nt:unstructured");
        assertThat(ntUnstructured, is(notNullValue()));
        assertThat(ntUnstructured.getPrimaryItemName(), is(nullValue()));

        // Now check using a node that has the "nt:unstructured" primary type ...
        cars.getPrimaryItem();
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldFailToReturnItemFromGetPrimaryItemIfTheNodeHasNoItemMatchingThatSpecifiedByThePrimaryType()
        throws Exception {
        // Find the "vehix:car" type ...
        NodeType carType = nodeTypes.getNodeType("vehix:car");
        assertThat(carType, is(notNullValue()));
        assertThat(carType.getPrimaryItemName(), is("vehix:model"));
        assertThat(prius.getPrimaryItem(), is(sameInstance((Item)prius.getProperty("vehix:model"))));

        // Now remove the "vehix:model" property so there is no such item ...
        prius.getProperty("vehix:model").remove();
        prius.getPrimaryItem();
    }

    /*
     * Miscellaneous methods
     */

    @Test
    public void shouldProvideSession() throws Exception {
        assertThat((JcrSession)prius.getSession(), is(jcrSession));
    }

    /*
     * Same-ness
     */

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowIsSameWithNoItem() throws Exception {
        hybrid.isSame(null);
    }

    @Test
    public void shouldReturnFalseFromIsSameIfTheRepositoryInstanceIsDifferent() throws Exception {
        // Set up the store ...
        InMemoryRepositorySource source2 = new InMemoryRepositorySource();
        source2.setName("store");
        Graph store2 = Graph.create(source2, context);
        store2.importXmlFrom(AbstractJcrTest.class.getClassLoader().getResourceAsStream("cars.xml")).into("/");
        JcrSession jcrSession2 = mock(JcrSession.class);
        when(jcrSession2.nodeTypeManager()).thenReturn(nodeTypes);
        when(jcrSession2.isLive()).thenReturn(true);
        SessionCache cache2 = new SessionCache(jcrSession2, store2.getCurrentWorkspaceName(), context, nodeTypes, store2);

        Workspace workspace2 = mock(Workspace.class);
        Repository repository2 = mock(Repository.class);
        when(jcrSession2.getWorkspace()).thenReturn(workspace2);
        when(jcrSession2.getRepository()).thenReturn(repository2);
        when(workspace2.getName()).thenReturn("workspace1");

        // Use the same id and location ...
        javax.jcr.Node prius2 = cache2.findJcrNode(null, path("/Cars/Hybrid/Toyota Prius"));
        assertThat(prius2.isSame(prius), is(false));
    }

    @Test
    public void shouldReturnFalseFromIsSameIfTheWorkspaceNameIsDifferent() throws Exception {
        // Set up the store ...
        InMemoryRepositorySource source2 = new InMemoryRepositorySource();
        source2.setName("store");
        Graph store2 = Graph.create(source2, context);
        store2.importXmlFrom(AbstractJcrTest.class.getClassLoader().getResourceAsStream("cars.xml")).into("/");
        JcrSession jcrSession2 = mock(JcrSession.class);
        when(jcrSession2.nodeTypeManager()).thenReturn(nodeTypes);
        when(jcrSession2.isLive()).thenReturn(true);
        SessionCache cache2 = new SessionCache(jcrSession2, store2.getCurrentWorkspaceName(), context, nodeTypes, store2);

        Workspace workspace2 = mock(Workspace.class);
        JcrRepository repository2 = mock(JcrRepository.class);
        RepositoryLockManager repoLockManager2 = mock(RepositoryLockManager.class);
        when(jcrSession2.getWorkspace()).thenReturn(workspace2);
        when(jcrSession2.getRepository()).thenReturn(repository2);
        when(workspace2.getName()).thenReturn("workspace2");

        WorkspaceLockManager lockManager = new WorkspaceLockManager(context, repoLockManager2, "workspace2", null);
        JcrLockManager jcrLockManager = new JcrLockManager(jcrSession2, lockManager);
        when(jcrSession2.lockManager()).thenReturn(jcrLockManager);

        // Use the same id and location; use 'Toyota Prius'
        // since the UUID is defined in 'cars.xml' and therefore will be the same
        javax.jcr.Node prius2 = cache2.findJcrNode(null, path("/Cars/Hybrid/Toyota Prius"));
        prius2.addMixin("mix:referenceable");
        prius.addMixin("mix:referenceable");
        String priusUuid2 = prius2.getIdentifier();
        String priusUuid = prius.getIdentifier();
        assertThat(priusUuid, is(priusUuid2));
        assertThat(prius2.isSame(prius), is(false));
    }

    @Test
    public void shouldReturnFalseFromIsSameIfTheNodeUuidIsDifferent() throws Exception {
        // Set up the store ...
        InMemoryRepositorySource source2 = new InMemoryRepositorySource();
        source2.setName("store");
        Graph store2 = Graph.create(source2, context);
        store2.importXmlFrom(AbstractJcrTest.class.getClassLoader().getResourceAsStream("cars.xml")).into("/");
        JcrSession jcrSession2 = mock(JcrSession.class);
        when(jcrSession2.nodeTypeManager()).thenReturn(nodeTypes);
        when(jcrSession2.isLive()).thenReturn(true);
        SessionCache cache2 = new SessionCache(jcrSession2, store2.getCurrentWorkspaceName(), context, nodeTypes, store2);

        Workspace workspace2 = mock(Workspace.class);
        JcrRepository repository2 = mock(JcrRepository.class);
        RepositoryLockManager repoLockManager2 = mock(RepositoryLockManager.class);
        when(jcrSession2.getWorkspace()).thenReturn(workspace2);
        when(jcrSession2.getRepository()).thenReturn(repository2);
        when(workspace2.getName()).thenReturn("workspace1");

        WorkspaceLockManager lockManager = new WorkspaceLockManager(context, repoLockManager2, "workspace2", null);
        JcrLockManager jcrLockManager = new JcrLockManager(jcrSession2, lockManager);
        when(jcrSession2.lockManager()).thenReturn(jcrLockManager);

        // Use the same id and location; use 'Nissan Altima'
        // since the UUIDs will be different (cars.xml doesn't define on this node) ...
        javax.jcr.Node altima2 = cache2.findJcrNode(null, path("/Cars/Hybrid/Nissan Altima"));
        altima2.addMixin("mix:referenceable");
        altima.addMixin("mix:referenceable");
        String altimaUuid = altima.getIdentifier();
        String altimaUuid2 = altima2.getIdentifier();
        assertThat(altimaUuid, is(not(altimaUuid2)));
        assertThat(altima2.isSame(altima), is(false));
    }

    @Test
    public void shouldReturnTrueFromIsSameIfTheNodeUuidAndWorkspaceNameAndRepositoryInstanceAreSame() throws Exception {
        // Set up the store ...
        InMemoryRepositorySource source2 = new InMemoryRepositorySource();
        source2.setName("store");
        Graph store2 = Graph.create(source2, context);
        store2.importXmlFrom(AbstractJcrTest.class.getClassLoader().getResourceAsStream("cars.xml")).into("/");
        JcrSession jcrSession2 = mock(JcrSession.class);
        when(jcrSession2.nodeTypeManager()).thenReturn(nodeTypes);
        when(jcrSession2.isLive()).thenReturn(true);
        SessionCache cache2 = new SessionCache(jcrSession2, store2.getCurrentWorkspaceName(), context, nodeTypes, store2);

        Workspace workspace2 = mock(Workspace.class);
        when(jcrSession2.getWorkspace()).thenReturn(workspace2);
        when(jcrSession2.getRepository()).thenReturn(repository);
        when(workspace2.getName()).thenReturn("workspace1");

        WorkspaceLockManager lockManager = new WorkspaceLockManager(context, repository.getRepositoryLockManager(), "workspace1",
                                                                    null);
        JcrLockManager jcrLockManager = new JcrLockManager(jcrSession2, lockManager);
        when(jcrSession2.lockManager()).thenReturn(jcrLockManager);

        // Use the same id and location ...
        javax.jcr.Node prius2 = cache2.findJcrNode(null, path("/Cars/Hybrid/Toyota Prius"));
        prius2.addMixin("mix:referenceable");
        prius.addMixin("mix:referenceable");
        String priusUuid = prius.getIdentifier();
        String priusUuid2 = prius2.getIdentifier();
        assertThat(priusUuid, is(priusUuid2));
        assertThat(prius2.isSame(prius), is(true));
    }

    @Test
    public void shouldAlwaysHaveCorrespondenceIdForRootNodeThatContainsSelfPath() throws Exception {
        CorrespondenceId id = rootNode.getCorrespondenceId();
        assertThat(id.getReferenceableId(), is(rootNode.getIdentifier()));
        assertThat(id.getRelativePath().size(), is(1));
        assertThat(id.getRelativePath().getLastSegment().isSelfReference(), is(true));
    }

    @Test
    public void shouldAlwaysHaveCorrespondenceId() throws Exception {
        assertThat(cars.isReferenceable(), is(false));
        CorrespondenceId id = cars.getCorrespondenceId();
        assertThat(id.getReferenceableId(), is(rootNode.getIdentifier()));
        assertThat(id.getRelativePath().size(), is(1));
        assertThat(id.getRelativePath(), is(path("Cars")));

        assertThat(hybrid.isReferenceable(), is(false));
        id = hybrid.getCorrespondenceId();
        assertThat(id.getReferenceableId(), is(rootNode.getIdentifier()));
        assertThat(id.getRelativePath().size(), is(2));
        assertThat(id.getRelativePath(), is(path("Cars/Hybrid")));

        altima.addMixin("mix:referenceable");
        assertThat(altima.isReferenceable(), is(true));
        id = altima.getCorrespondenceId();
        assertThat(id.getReferenceableId(), is(altima.getIdentifier()));
        assertThat(id.getRelativePath().size(), is(1));
        assertThat(id.getRelativePath(), is(path(".")));
    }

    @Test
    public void shouldAddNodeWhenIntermediateNodesDoExist() throws Exception {
        javax.jcr.Node newNode = rootNode.addNode("Cars/Hybrid/CreateThis", "nt:unstructured");
        assertThat(newNode.getName(), is("CreateThis"));
        assertThat(newNode.getParent(), is(sameInstance((javax.jcr.Node)hybrid)));
    }

    @Test( expected = PathNotFoundException.class )
    public void shouldFailToAddNodeWhenIntermediateNodesDoNotExist() throws Exception {
        rootNode.addNode("Cars/nonExistant/CreateThis", "nt:unstructured");
    }

    @SuppressWarnings( "unchecked" )
    private void registerTestNodeType() throws Exception {
        try {
            nodeTypes.getNodeType("autocreateTest");
            return;
        } catch (NoSuchNodeTypeException nsnte) {
        }

        NodeTypeTemplate ntt = nodeTypes.createNodeTypeTemplate();
        JcrPropertyDefinitionTemplate pdt = (JcrPropertyDefinitionTemplate)nodeTypes.createPropertyDefinitionTemplate();
        NodeDefinitionTemplate ndt = nodeTypes.createNodeDefinitionTemplate();

        pdt.setName("autoProp");
        pdt.setRequiredType(PropertyType.STRING);
        pdt.setDefaultValues(new String[] {"default"});
        pdt.setAutoCreated(true);

        ndt.setName("autoChild");
        ndt.setRequiredPrimaryTypeNames(new String[] {"nt:base"});
        ndt.setDefaultPrimaryTypeName("nt:unstructured");
        ndt.setAutoCreated(true);

        ntt.setName("autocreateTest");
        ntt.getNodeDefinitionTemplates().add(ndt);
        ntt.getPropertyDefinitionTemplates().add(pdt);

        nodeTypes.registerNodeType(ntt, false);

    }

    @Test
    public void shouldAddAutocreatedPropertiesForNode() throws Exception {
        registerTestNodeType();

        Node testNode = rootNode.addNode("autoPropTest", "autocreateTest");
        assertThat(testNode.hasProperty("autoProp"), is(true));

        Property autoProp = testNode.getProperty("autoProp");
        assertThat(autoProp.getString(), is("default"));
    }

    @Test
    public void shouldAddAutocreatedChildNodesForNode() throws Exception {
        registerTestNodeType();

        Node testNode = rootNode.addNode("autoChildTest", "autocreateTest");
        assertThat(testNode.hasNode("autoChild"), is(true));

        Node autoProp = testNode.getNode("autoChild");
        assertThat(autoProp.getPrimaryNodeType().getName(), is("nt:unstructured"));
    }
}
