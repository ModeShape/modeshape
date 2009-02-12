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
package org.jboss.dna.connector.federation.merge.strategy;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matcher;
import org.jboss.dna.common.collection.IsIteratorContaining;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.connector.federation.merge.FederatedNode;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Randall Hauch
 */
public class SimpleMergeStrategyTest {

    private SimpleMergeStrategy strategy;
    private List<Contribution> contributions;
    private ExecutionContext context;
    private FederatedNode node;
    private String workspaceName;
    protected Path parentPath;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        strategy = new SimpleMergeStrategy();
        contributions = new LinkedList<Contribution>();
        context = new ExecutionContext();
        context.getNamespaceRegistry().register("dna", "http://www.jboss.org/dna/something");
        context.getNamespaceRegistry().register("jcr", "http://www.jcr.org");
        parentPath = context.getValueFactories().getPathFactory().create("/a/b/c");
        workspaceName = "some workspace";
        node = new FederatedNode(new Location(parentPath), workspaceName);
    }

    @Test
    public void shouldAddChildrenFromOneContribution() {
        addContribution("source1").addChildren("childA", "childB[1]", "childB[2]");
        strategy.merge(node, contributions, context);
        assertThat(node.getChildren(), hasChildLocations("childA", "childB[1]", "childB[2]"));
    }

    @Test
    public void shouldCombinePropertiesFromOneContribution() {
        addContribution("source1").setProperty("p1", "p1 value");
        strategy.merge(node, contributions, context);
        assertThat(node.getProperties().size(), is(2));
        assertThat(node.getPropertiesByName().get(DnaLexicon.UUID), is(notNullValue()));
        assertThat(node.getPropertiesByName().get(name("p1")), is(property("p1", "p1 value")));
    }

    @Test
    public void shouldAddChildrenFromMultipleContributionsInOrderAndShouldNotChangeSameNameSiblingIndexesWhenChildrenDoNotShareNamesWithChildrenFromDifferentContributions() {
        addContribution("source1").addChildren("childA", "childB[1]", "childB[2]");
        addContribution("source2").addChildren("childX", "childY[1]", "childY[2]");
        strategy.merge(node, contributions, context);
        assertThat(node.getChildren(), hasChildLocations("childA", "childB[1]", "childB[2]", "childX", "childY[1]", "childY[2]"));
    }

    @Test
    public void shouldAddChildrenFromMultipleContributionsInOrderAndShouldChangeSameNameSiblingIndexesWhenChildrenDoShareNamesWithChildrenFromDifferentContributions() {
        addContribution("source1").addChildren("childA", "childB[1]", "childB[2]");
        addContribution("source2").addChildren("childX", "childB", "childY");
        addContribution("source3").addChildren("childX", "childB");
        strategy.merge(node, contributions, context);
        assertThat(node.getChildren(), hasChildLocations("childA",
                                                         "childB[1]",
                                                         "childB[2]",
                                                         "childX[1]",
                                                         "childB[3]",
                                                         "childY",
                                                         "childX[2]",
                                                         "childB[4]"));
    }

    @Test
    public void shouldCombinePropertiesFromMultipleContributionsAndRemoveNoValuesWhenNoContributionsContainSameProperty() {
        addContribution("source1").setProperty("p1", "p1 value");
        addContribution("source2").setProperty("p2", "p2 value");
        strategy.merge(node, contributions, context);
        assertThat(node.getProperties().size(), is(3));
        assertThat(node.getPropertiesByName().get(DnaLexicon.UUID), is(notNullValue()));
        assertThat(node.getPropertiesByName().get(name("p1")), is(property("p1", "p1 value")));
        assertThat(node.getPropertiesByName().get(name("p2")), is(property("p2", "p2 value")));
    }

    @Test
    public void shouldCombinePropertiesFromMultipleContributionsAndRemoveDuplicateValuesWhenContributionsContainSameProperty() {
        addContribution("source1").setProperty("p1", "p1 value").setProperty("p12", "1", "2", "3");
        addContribution("source2").setProperty("p2", "p2 value").setProperty("p12", "3", "4");
        strategy.merge(node, contributions, context);
        assertThat(node.getProperties().size(), is(4));
        assertThat(node.getPropertiesByName().get(DnaLexicon.UUID), is(notNullValue()));
        assertThat(node.getPropertiesByName().get(name("p1")), is(property("p1", "p1 value")));
        assertThat(node.getPropertiesByName().get(name("p2")), is(property("p2", "p2 value")));
        assertThat(node.getPropertiesByName().get(name("p12")), is(property("p12", "1", "2", "3", "4")));
    }

    @Test
    public void shouldCombinePropertiesFromMultipleContributionsAndMaintainAllValuesForEveryProperty() {
        addContribution("source1").setProperty("p1", "p1 value").setProperty("p12", "1", "2", "3");
        addContribution("source2").setProperty("p2", "p2 value").setProperty("p12", "3", "4");
        strategy.merge(node, contributions, context);
        assertThat(node.getProperties().size(), is(4));
        assertThat(node.getPropertiesByName().get(DnaLexicon.UUID), is(notNullValue()));
        for (Contribution contribution : contributions) {
            Iterator<Property> iter = contribution.getProperties();
            while (iter.hasNext()) {
                Property contributionProperty = iter.next();
                Property mergedProperty = node.getPropertiesByName().get(contributionProperty.getName());
                assertThat(mergedProperty, is(notNullValue()));
                // Make sure that the merged property has each value ...
                for (Object contributedValue : contributionProperty.getValuesAsArray()) {
                    boolean foundValue = false;
                    for (Object mergedValue : mergedProperty.getValuesAsArray()) {
                        if (mergedValue.equals(contributedValue)) {
                            foundValue = true;
                            break;
                        }
                    }
                    assertThat(foundValue, is(true));
                }
            }
        }
    }

    @Test
    public void shouldCombinePropertiesFromMultipleContributionsWhenPropertyValuesAreOfDifferentPropertyTypes() {
        addContribution("source1").setProperty("p1", "p1 value").setProperty("p12", "1", "2", "3");
        addContribution("source2").setProperty("p2", "p2 value").setProperty("p12", 3, 4);
        strategy.merge(node, contributions, context);
        assertThat(node.getProperties().size(), is(4));
        assertThat(node.getPropertiesByName().get(DnaLexicon.UUID), is(notNullValue()));
        assertThat(node.getPropertiesByName().get(name("p1")), is(property("p1", "p1 value")));
        assertThat(node.getPropertiesByName().get(name("p2")), is(property("p2", "p2 value")));
        assertThat(node.getPropertiesByName().get(name("p12")), is(property("p12", "1", "2", "3", 4)));
    }

    @Test
    public void shouldCreateMergePlanWhenMergingContributions() {
        addContribution("source1").addChildren("childA", "childB[1]", "childB[2]").setProperty("p1", "p1 value");
        addContribution("source2").addChildren("childX", "childB", "childY").setProperty("p2", "p2 value");
        strategy.merge(node, contributions, context);
        assertThat(node.getMergePlan(), is(notNullValue()));
        assertThat(node.getMergePlan().getContributionCount(), is(2));
        assertThat(node.getMergePlan().getContributionFrom("source1"), is(sameInstance(contributions.get(0))));
        assertThat(node.getMergePlan().getContributionFrom("source2"), is(sameInstance(contributions.get(1))));
    }

    @Test
    public void shouldCorrectlyBuildMockUsingContributionBuilder() {
        assertThat(contributions.size(), is(0));
        addContribution("source1").addChildren("childA", "childB[1]", "childB[2]").setProperty("p1", "p1 value");
        assertThat(contributions.size(), is(1));
        assertThat(contributions.get(0).getChildren(), hasLocationIterator("childA", "childB[1]", "childB[2]"));
    }

    protected Matcher<List<Location>> hasChildLocations( String... childNames ) {
        List<Location> locations = new ArrayList<Location>();
        for (String childName : childNames) {
            locations.add(new Location(context.getValueFactories().getPathFactory().create(parentPath, childName)));
        }
        return equalTo(locations);
    }

    protected Matcher<Iterator<Location>> hasLocationIterator( String... childNames ) {
        Location[] locations = new Location[childNames.length];
        int index = 0;
        for (String childName : childNames) {
            locations[index++] = new Location(context.getValueFactories().getPathFactory().create(parentPath, childName));
        }
        return IsIteratorContaining.hasItems(locations);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Property property( String name,
                                 Object... values ) {
        return context.getPropertyFactory().create(name(name), values);
    }

    protected ContributionBuilder addContribution( String sourceName ) {
        ContributionBuilder builder = new ContributionBuilder(context, sourceName, contributions);
        contributions.add(builder.getMock());
        return builder;
    }

    protected class ContributionBuilder {
        protected final Contribution mockContribution;
        protected final ExecutionContext context;
        protected final Map<Name, Property> properties = new HashMap<Name, Property>();
        protected final List<Location> children = new ArrayList<Location>();

        protected ContributionBuilder( ExecutionContext context,
                                       String name,
                                       List<Contribution> contributions ) {
            this.context = context;
            this.mockContribution = Mockito.mock(Contribution.class);
            stub(mockContribution.getLocationInSource()).toReturn(new Location(parentPath));
            stub(mockContribution.getSourceName()).toReturn(name);
            stub(mockContribution.getChildren()).toAnswer(new Answer<Iterator<Location>>() {
                public Iterator<Location> answer( InvocationOnMock invocation ) throws Throwable {
                    return ContributionBuilder.this.children.iterator();
                }
            });
            stub(mockContribution.getChildrenCount()).toAnswer(new Answer<Integer>() {
                public Integer answer( InvocationOnMock invocation ) throws Throwable {
                    return ContributionBuilder.this.children.size();
                }
            });
            stub(mockContribution.getProperties()).toAnswer(new Answer<Iterator<Property>>() {
                public Iterator<Property> answer( InvocationOnMock invocation ) throws Throwable {
                    return ContributionBuilder.this.properties.values().iterator();
                }
            });
            stub(mockContribution.getPropertyCount()).toAnswer(new Answer<Integer>() {
                public Integer answer( InvocationOnMock invocation ) throws Throwable {
                    return ContributionBuilder.this.properties.size();
                }
            });
        }

        public Contribution getMock() {
            return this.mockContribution;
        }

        public ContributionBuilder addChildren( String... pathsForChildren ) {
            for (String childPath : pathsForChildren) {
                Path path = context.getValueFactories().getPathFactory().create(parentPath, childPath);
                children.add(new Location(path));
            }
            return this;
        }

        public ContributionBuilder setProperty( String name,
                                                Object... values ) {
            Name propertyName = context.getValueFactories().getNameFactory().create(name);
            Property property = context.getPropertyFactory().create(propertyName, values);
            stub(mockContribution.getProperty(propertyName)).toReturn(property);
            if (values != null && values.length > 0) {
                properties.put(propertyName, property);
            } else {
                properties.remove(propertyName);
            }
            return this;
        }
    }

}
