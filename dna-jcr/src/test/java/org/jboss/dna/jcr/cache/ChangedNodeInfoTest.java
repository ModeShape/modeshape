/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.jcr.cache;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.jboss.dna.jcr.cache.IsNodeInfoWithChildrenHavingNames.hasChild;
import static org.jboss.dna.jcr.cache.IsNodeInfoWithChildrenHavingNames.hasChildren;
import static org.jboss.dna.jcr.cache.IsNodeInfoWithChildrenHavingUuids.hasChild;
import static org.jboss.dna.jcr.cache.IsNodeInfoWithChildrenHavingUuids.hasChildren;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.jcr.NodeDefinitionId;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ChangedNodeInfoTest {

    private ExecutionContext context;
    private PathFactory pathFactory;
    private NodeInfo original;
    private UUID uuid;
    private Name primaryTypeName;
    private Name[] requiredPrimaryTypes;
    private Location location;
    private NodeDefinitionId definitionId;
    private ChangedChildren children;
    private Map<Name, PropertyInfo> properties;
    private List<Name> mixinTypeNames;
    private ChangedNodeInfo changes;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        context.getNamespaceRegistry().register("acme", "http://example.com/acme");
        pathFactory = context.getValueFactories().getPathFactory();

        // Set up the original ...
        uuid = UUID.randomUUID();
        location = Location.create(uuid);
        primaryTypeName = name("acme:geniusType");
        requiredPrimaryTypes = new Name[] {name("acme:requiredTypeA"), name("acme:requiredTypeB")};
        definitionId = new NodeDefinitionId(name("acme:geniusContainerType"), name("acme:geniuses"), requiredPrimaryTypes);
        children = new ChangedChildren(uuid);
        properties = new HashMap<Name, PropertyInfo>();
        mixinTypeNames = new LinkedList<Name>();
        original = new ImmutableNodeInfo(location, primaryTypeName, mixinTypeNames, definitionId, uuid, children, properties);

        // Create the changed node representation ...
        changes = new ChangedNodeInfo(original);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Segment segment( String segment ) {
        return pathFactory.createSegment(segment);
    }

    /**
     * Utility to set a property to the original node representation. This will replace any existing property with the same name.
     * 
     * @param name the name of the property; may not be null
     * @return the new property representation; never null
     */
    protected PropertyInfo makePropertyInOriginal( String name ) {
        Name propName = name(name);
        PropertyInfo propertyInfo = mock(PropertyInfo.class);
        stub(propertyInfo.getPropertyName()).toReturn(propName);
        properties.put(propName, propertyInfo);
        return propertyInfo;
    }

    /**
     * Utility to change a property in the changed representation.
     * 
     * @param name the name of the property to change; may not be null
     * @return the new property; never null
     */
    protected PropertyInfo setPropertyInChanged( String name ) {
        Name propName = name(name);
        PropertyInfo propertyInfo = mock(PropertyInfo.class);
        stub(propertyInfo.getPropertyName()).toReturn(propName);
        changes.setProperty(propertyInfo, context.getValueFactories());
        return propertyInfo;
    }

    protected ChildNode makeChildInOriginal( String childName ) {
        ChildNode newChild = children.add(name(childName), UUID.randomUUID(), pathFactory);
        return newChild;
    }

    protected ChildNode addChildInChanged( String childName ) {
        ChildNode newChild = changes.addChild(name(childName), UUID.randomUUID(), pathFactory);
        // Make sure that the 'changes' object no longer returns the original object's children object
        assertThat(changes.getChildren(), is(not(sameInstance(original.getChildren()))));
        return newChild;
    }

    protected void removeChildFromChanged( ChildNode child ) {
        // Verify that a child node with the supplied UUID is contained.
        // Note that it may not be the same ChildNode instance if another SNS node with smaller index was removed
        assertThat(changes.getChildren().getChild(child.getUuid()), is(notNullValue()));
        // Now remove the child, making sure the result is the same as the next 'getChildren()' call ...
        assertThat(changes.removeChild(child.getUuid(), pathFactory), is(notNullValue()));
        // Verify it no longer exists ...
        assertThat(changes.getChildren().getChild(child.getUuid()), is(nullValue()));
    }

    @Test
    public void shouldHaveLocationFromOriginal() {
        assertThat(changes.getOriginalLocation(), is(sameInstance(location)));
    }

    @Test
    public void shouldHaveParentUuidFromOriginal() {
        assertThat(changes.getParent(), is(sameInstance(uuid)));
    }

    @Test
    public void shouldHavePrimaryTypeNameFromOriginal() {
        assertThat(changes.getPrimaryTypeName(), is(sameInstance(primaryTypeName)));
    }

    @Test
    public void shouldHaveMixinTypeNamesFromOriginal() {
        assertThat(changes.getMixinTypeNames(), is(sameInstance(mixinTypeNames)));
    }

    @Test
    public void shouldUpdateMixinTypeNamesWhenSettingJcrMixinTypeProperty() {
        // Create the DNA property ...
        Property mixinTypes = context.getPropertyFactory().create(JcrLexicon.MIXIN_TYPES, "dna:type1", "dna:type2");

        // Modify the property ...
        PropertyInfo newPropertyInfo = mock(PropertyInfo.class);
        stub(newPropertyInfo.getPropertyName()).toReturn(mixinTypes.getName());
        stub(newPropertyInfo.getProperty()).toReturn(mixinTypes);
        PropertyInfo previous = changes.setProperty(newPropertyInfo, context.getValueFactories());
        assertThat(previous, is(nullValue()));

        // Verify that the mixin types were updated ...
        assertThat(changes.getProperty(name("jcr:mixinTypes")), is(sameInstance(newPropertyInfo)));
        assertThat(changes.getMixinTypeNames(), hasItems(name("dna:type2"), name("dna:type1")));
    }

    @Test
    public void shouldHaveNodeDefinitionIdFromOriginal() {
        assertThat(changes.getDefinitionId(), is(sameInstance(definitionId)));
    }

    @Test
    public void shouldHaveChildrenFromOriginal() {
        assertThat(changes.getChildren(), is(sameInstance((Children)children)));

        ChildNode childA = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(1));
        assertThat(changes.getChildren().getChild(childA.getUuid()), is(sameInstance(childA)));
        assertThat(changes.getChildren(), hasChild(segment("childA[1]")));
        assertThat(changes.getChildren(), hasChild(childA.getUuid()));
    }

    @Test
    public void shouldHaveChildrenAfterAddingChild() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Add child ...
        ChildNode childA3 = addChildInChanged("childA");

        // Verify that all children are there in the proper order ...
        assertThat(changes.getChildren().size(), is(4));
        assertThat(changes.getChildren(), hasChildren(segment("childA[1]"),
                                                      segment("childB[1]"),
                                                      segment("childA[2]"),
                                                      segment("childA[3]")));
        assertThat(changes.getChildren(), hasChildren(childA1.getUuid(), childB1.getUuid(), childA2.getUuid(), childA3.getUuid()));
        assertThat(changes.getChildren(), hasItems(childA1, childB1, childA2, childA3));
    }

    @Test
    public void shouldHaveChildrenAfterAddingMultipleChildren() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Add some children in the changed representation ...
        ChildNode childA3 = addChildInChanged("childA");
        ChildNode childC1 = addChildInChanged("childC");
        ChildNode childC2 = addChildInChanged("childC");

        // Verify that all children are there in the proper order ...
        assertThat(changes.getChildren().size(), is(6));
        assertThat(changes.getChildren(), hasChildren(segment("childA[1]"),
                                                      segment("childB[1]"),
                                                      segment("childA[2]"),
                                                      segment("childA[3]"),
                                                      segment("childC[1]"),
                                                      segment("childC[2]")));
        assertThat(changes.getChildren(), hasChildren(childA1.getUuid(),
                                                      childB1.getUuid(),
                                                      childA2.getUuid(),
                                                      childA3.getUuid(),
                                                      childC1.getUuid(),
                                                      childC2.getUuid()));
        assertThat(changes.getChildren(), hasItems(childA1, childB1, childA2, childA3, childC1, childC2));
    }

    @Test
    public void shouldHaveChildrenAfterAddingMultipleChildrenAndRemovingOthers() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Add some children in the changed representation ...
        ChildNode childA3 = addChildInChanged("childA");
        ChildNode childC1 = addChildInChanged("childC");
        ChildNode childC2 = addChildInChanged("childC");

        // Delete a child that was added and another that was an original ...
        removeChildFromChanged(childC1);
        removeChildFromChanged(childA2);

        // Verify that all children are there in the proper order ...
        assertThat(changes.getChildren().size(), is(4));
        assertThat(changes.getChildren(), hasChildren(segment("childA[1]"),
                                                      segment("childB[1]"),
                                                      segment("childA[2]"),
                                                      segment("childC[1]")));
        assertThat(changes.getChildren(), hasChildren(childA1.getUuid(), childB1.getUuid(), childA3.getUuid(), childC2.getUuid()));
    }

    @Test
    public void shouldHaveChildrenAfterAddingMultipleChildrenAndThenRemovingThoseJustAdded() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Add some children in the changed representation ...
        ChildNode childA3 = addChildInChanged("childA");
        ChildNode childC1 = addChildInChanged("childC");
        ChildNode childC2 = addChildInChanged("childC");

        // Delete a child that was added and another that was an original...
        removeChildFromChanged(childA3);
        removeChildFromChanged(childC1); // causes replacement of 'childC2' with lower SNS index
        removeChildFromChanged(childC2);

        // Verify that all children are there in the proper order ...
        assertThat(changes.getChildren().size(), is(3));
        assertThat(changes.getChildren(), hasChildren(segment("childA[1]"), segment("childB[1]"), segment("childA[2]")));
        assertThat(changes.getChildren(), hasChildren(childA1.getUuid(), childB1.getUuid(), childA2.getUuid()));

        // Do it again, but change the order of delete to delete from the back ...

        // Add some children in the changed representation ...
        childA3 = addChildInChanged("childA");
        childC1 = addChildInChanged("childC");
        childC2 = addChildInChanged("childC");

        // Delete a child that was added and another that was an original ...
        removeChildFromChanged(childC2);
        removeChildFromChanged(childC1);
        removeChildFromChanged(childA3);

        // Verify that all children are there in the proper order ...
        assertThat(changes.getChildren().size(), is(3));
        assertThat(changes.getChildren(), hasChildren(segment("childA[1]"), segment("childB[1]"), segment("childA[2]")));
        assertThat(changes.getChildren(), hasChildren(childA1.getUuid(), childB1.getUuid(), childA2.getUuid()));

    }

    @Test
    public void shouldHaveChildrenAfterDeletingChild() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Delete a child that was added and another that was an original ...
        removeChildFromChanged(childA1);

        // Verify that all children are there in the proper order ...
        assertThat(changes.getChildren().size(), is(2));
        assertThat(changes.getChildren(), hasChildren(segment("childB[1]"), segment("childA[1]")));
        assertThat(changes.getChildren(), hasChildren(childB1.getUuid(), childA2.getUuid()));
    }

    @Test
    public void shouldHaveChildrenAfterDeletingMultipleChildren() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Delete a child that was added and another that was an original ...
        removeChildFromChanged(childA1);
        removeChildFromChanged(childA2);

        // Verify that all children are there in the proper order ...
        assertThat(changes.getChildren().size(), is(1));
        assertThat(changes.getChildren(), hasChildren(segment("childB[1]")));
        assertThat(changes.getChildren(), hasChildren(childB1.getUuid()));
    }

    @Test
    public void shouldHaveChildrenAfterDeletingAllChildrenFromTheFirsttChildToTheLast() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Delete all children, from the front to the back ...
        removeChildFromChanged(childA1); // causes replacement of 'childA2' with lower SNS index
        removeChildFromChanged(childA2);
        removeChildFromChanged(childB1);

        // Verify that all children have been removed ...
        assertThat(changes.getChildren().size(), is(0));
    }

    @Test
    public void shouldHaveChildrenAfterDeletingAllChildrenFromTheLastChildToTheFirst() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Delete all children, from the back to the front ...
        removeChildFromChanged(childA2);
        removeChildFromChanged(childB1);
        removeChildFromChanged(childA1);

        // Verify that all children have been removed ...
        assertThat(changes.getChildren().size(), is(0));
    }

    @Test
    public void shouldHaveChildrenAfterAddingSomeChildrenThenDeletingAllChildrenFromTheFirstChildToTheLast() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Add some children in the changed representation ...
        ChildNode childA3 = addChildInChanged("childA");
        ChildNode childC1 = addChildInChanged("childC");
        ChildNode childC2 = addChildInChanged("childC");

        // Delete all children, from the front to the back ...
        removeChildFromChanged(childA3);
        removeChildFromChanged(childC1); // causes replacement of 'childC2' with lower SNS index
        removeChildFromChanged(childC2);
        removeChildFromChanged(childA1); // causes replacement of 'childA2' with lower SNS index
        removeChildFromChanged(childB1);
        removeChildFromChanged(childA2);

        // Verify that all children have been removed ...
        assertThat(changes.getChildren().size(), is(0));
    }

    @Test
    public void shouldHaveChildrenAfterAddingSomeChildrenThenDeletingAllChildrenFromTheLastChildToTheFirst() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Add some children in the changed representation ...
        ChildNode childA3 = addChildInChanged("childA");
        ChildNode childC1 = addChildInChanged("childC");
        ChildNode childC2 = addChildInChanged("childC");

        // Delete all children, from the back to the front ...
        removeChildFromChanged(childC2);
        removeChildFromChanged(childC1);
        removeChildFromChanged(childA3);
        removeChildFromChanged(childA2);
        removeChildFromChanged(childB1);
        removeChildFromChanged(childA1);

        // Verify that all children have been removed ...
        assertThat(changes.getChildren().size(), is(0));
    }

    @Test
    public void shouldHaveChildrenAfterReorderingChildren() {

    }

    @Test
    @SuppressWarnings( "unused" )
    public void shouldNotRemoveFromNodeInfoWithNoChildChangesAChildThatMatchesSegmentButNotUuid() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Create a bogus node that has a new UUID but with the same segment as 'childA3' ...
        Children before = changes.getChildren();
        int beforeSize = before.size();
        assertThat(changes.removeChild(UUID.randomUUID(), pathFactory), is(nullValue()));
        Children after = changes.getChildren();
        assertThat(after.size(), is(beforeSize));
        assertThat(after, is(sameInstance(before)));
        assertThat(after, is(sameInstance(changes.getChildren())));
    }

    @Test
    @SuppressWarnings( "unused" )
    public void shouldNotRemoveFromNodeInfoWithSomeChildChangesAChildThatMatchesSegmentButNotUuid() {
        // Set up the original ...
        ChildNode childA1 = makeChildInOriginal("childA");
        ChildNode childB1 = makeChildInOriginal("childB");
        ChildNode childA2 = makeChildInOriginal("childA");
        assertThat(changes.getChildren().size(), is(3));

        // Add some children in the changed representation ...
        ChildNode childA3 = addChildInChanged("childA");
        ChildNode childC1 = addChildInChanged("childC");
        ChildNode childC2 = addChildInChanged("childC");

        // Create a bogus node that has a new UUID but with the same segment as 'childA3' ...
        Children before = changes.getChildren();
        int beforeSize = before.size();
        assertThat(changes.removeChild(UUID.randomUUID(), pathFactory), is(nullValue()));
        Children after = changes.getChildren();
        assertThat(after.size(), is(beforeSize));
        assertThat(after, is(sameInstance(before)));
        assertThat(after, is(sameInstance(changes.getChildren())));
    }

    @Test
    public void shouldFindPropertyThatHasNotBeenModifiedButIsInOriginal() {
        PropertyInfo propertyInfo = makePropertyInOriginal("test");
        assertThat(changes.getProperty(name("test")), is(sameInstance(propertyInfo)));
    }

    @Test
    public void shouldFindPropertyThatHasBeenModifiedFromOriginal() {
        PropertyInfo propertyInfo = makePropertyInOriginal("test");
        assertThat(changes.getProperty(name("test")), is(sameInstance(propertyInfo)));

        // Modify the property ...
        PropertyInfo newPropertyInfo = mock(PropertyInfo.class);
        stub(newPropertyInfo.getPropertyName()).toReturn(name("test"));
        PropertyInfo previous = changes.setProperty(newPropertyInfo, context.getValueFactories());
        assertThat(previous, is(sameInstance(propertyInfo)));

        // Verify we can find the new property ...
        assertThat(changes.getProperty(name("test")), is(sameInstance(newPropertyInfo)));
    }

    @Test
    public void shouldNotFindPropertyThatHasBeenDeletedFromOriginal() {
        PropertyInfo propertyInfo = makePropertyInOriginal("test");
        assertThat(changes.getProperty(name("test")), is(sameInstance(propertyInfo)));

        // Delete the property ...
        PropertyInfo previous = changes.removeProperty(name("test"));
        assertThat(previous, is(sameInstance(propertyInfo)));

        // Verify we can not find the new property ...
        assertThat(changes.getProperty(name("test")), is(nullValue()));
    }

    @Test
    public void shouldNotFindPropertyThatIsNotInOriginal() {
        assertThat(changes.getProperty(name("test")), is(nullValue()));

        makePropertyInOriginal("test");
        assertThat(changes.getProperty(name("nonExistant")), is(nullValue()));
    }

    @Test
    public void shouldFindAllPropertyNamesWhenChangedNodeHasNoChangedProperties() {
        PropertyInfo propA = makePropertyInOriginal("propA");
        PropertyInfo propB = makePropertyInOriginal("propB");
        PropertyInfo propC = makePropertyInOriginal("propC");
        PropertyInfo propD = makePropertyInOriginal("propD");
        Set<Name> names = changes.getPropertyNames();
        assertThat(names.size(), is(4));
        assertThat(names, hasItems(propA.getPropertyName(),
                                   propB.getPropertyName(),
                                   propC.getPropertyName(),
                                   propD.getPropertyName()));
    }

    @Test
    public void shouldFindAllPropertyNamesWhenChangedNodeHasDeletedAllProperties() {
        PropertyInfo propA = makePropertyInOriginal("propA");
        PropertyInfo propB = makePropertyInOriginal("propB");
        PropertyInfo propC = makePropertyInOriginal("propC");
        PropertyInfo propD = makePropertyInOriginal("propD");
        // Remove all properties ...
        assertThat(changes.removeProperty(propA.getPropertyName()), is(sameInstance(propA)));
        assertThat(changes.removeProperty(propB.getPropertyName()), is(sameInstance(propB)));
        assertThat(changes.removeProperty(propC.getPropertyName()), is(sameInstance(propC)));
        assertThat(changes.removeProperty(propD.getPropertyName()), is(sameInstance(propD)));
        // Now ask for names
        Set<Name> names = changes.getPropertyNames();
        assertThat(names.size(), is(0));
    }

    @Test
    public void shouldFindAllPropertyNamesWhenChangedNodeHasDeletedSomeProperties() {
        PropertyInfo propA = makePropertyInOriginal("propA");
        PropertyInfo propB = makePropertyInOriginal("propB");
        PropertyInfo propC = makePropertyInOriginal("propC");
        PropertyInfo propD = makePropertyInOriginal("propD");
        // Remove some properties ...
        assertThat(changes.removeProperty(propB.getPropertyName()), is(sameInstance(propB)));
        assertThat(changes.removeProperty(propD.getPropertyName()), is(sameInstance(propD)));
        // Now ask for names
        Set<Name> names = changes.getPropertyNames();
        assertThat(names.size(), is(2));
        assertThat(names, hasItems(propA.getPropertyName(), propC.getPropertyName()));
    }

    @Test
    @SuppressWarnings( "unused" )
    public void shouldFindAllPropertyNamesWhenChangedNodeHasChangedSomeProperties() {
        PropertyInfo propA = makePropertyInOriginal("propA");
        PropertyInfo propB = makePropertyInOriginal("propB");
        PropertyInfo propC = makePropertyInOriginal("propC");
        PropertyInfo propD = makePropertyInOriginal("propD");
        // Change some properties ...
        PropertyInfo propB2 = setPropertyInChanged("propB");
        PropertyInfo propC2 = setPropertyInChanged("propC");
        // Now ask for names
        Set<Name> names = changes.getPropertyNames();
        assertThat(names.size(), is(4));
        assertThat(names, hasItems(propA.getPropertyName(),
                                   propB2.getPropertyName(),
                                   propC2.getPropertyName(),
                                   propD.getPropertyName()));
    }

    @Test
    @SuppressWarnings( "unused" )
    public void shouldFindAllPropertyNamesWhenChangedNodeHasAddedSomeProperties() {
        PropertyInfo propA = makePropertyInOriginal("propA");
        PropertyInfo propB = makePropertyInOriginal("propB");
        PropertyInfo propC = makePropertyInOriginal("propC");
        PropertyInfo propD = makePropertyInOriginal("propD");
        // Add some properties ...
        PropertyInfo propE = setPropertyInChanged("propE");
        PropertyInfo propF = setPropertyInChanged("propF");
        PropertyInfo propG = setPropertyInChanged("propG");
        // Now ask for names
        Set<Name> names = changes.getPropertyNames();
        assertThat(names.size(), is(7));
        assertThat(names, hasItems(propA.getPropertyName(),
                                   propB.getPropertyName(),
                                   propC.getPropertyName(),
                                   propD.getPropertyName(),
                                   propE.getPropertyName(),
                                   propF.getPropertyName()));
    }

    @Test
    @SuppressWarnings( "unused" )
    public void shouldFindAllPropertyNamesWhenChangedNodeHasChangedAndDeletedAndAddedSomeProperties() {
        PropertyInfo propA = makePropertyInOriginal("propA");
        PropertyInfo propB = makePropertyInOriginal("propB");
        PropertyInfo propC = makePropertyInOriginal("propC");
        PropertyInfo propD = makePropertyInOriginal("propD");
        // Change some properties ...
        PropertyInfo propB2 = setPropertyInChanged("propB");
        PropertyInfo propC2 = setPropertyInChanged("propC");
        // Add some properties ...
        PropertyInfo propE = setPropertyInChanged("propE");
        // Remove some properties ...
        assertThat(changes.removeProperty(propB2.getPropertyName()), is(sameInstance(propB2)));
        assertThat(changes.removeProperty(propD.getPropertyName()), is(sameInstance(propD)));
        // Now ask for names
        Set<Name> names = changes.getPropertyNames();
        assertThat(names.size(), is(3));
        assertThat(names, hasItems(propA.getPropertyName(), propC2.getPropertyName(), propE.getPropertyName()));
    }

    @Test
    @SuppressWarnings( "unused" )
    public void shouldFindAllPropertyNamesWhenChangedNodeHasChangedAndDeletedAllProperties() {
        PropertyInfo propA = makePropertyInOriginal("propA");
        PropertyInfo propB = makePropertyInOriginal("propB");
        PropertyInfo propC = makePropertyInOriginal("propC");
        PropertyInfo propD = makePropertyInOriginal("propD");
        // Change some properties ...
        PropertyInfo propB2 = setPropertyInChanged("propB");
        PropertyInfo propC2 = setPropertyInChanged("propC");
        // Remove all properties ...
        assertThat(changes.removeProperty(propA.getPropertyName()), is(sameInstance(propA)));
        assertThat(changes.removeProperty(propB2.getPropertyName()), is(sameInstance(propB2)));
        assertThat(changes.removeProperty(propC2.getPropertyName()), is(sameInstance(propC2)));
        assertThat(changes.removeProperty(propD.getPropertyName()), is(sameInstance(propD)));
        // Now ask for names
        Set<Name> names = changes.getPropertyNames();
        assertThat(names.size(), is(0));
    }

}
