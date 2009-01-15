/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.federation.merge;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.BasicEmptyProperty;
import org.jboss.dna.graph.property.basic.BasicName;
import org.jboss.dna.graph.property.basic.JodaDateTime;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class MergePlanTest {

    private MergePlan plan;
    private List<Contribution> contributions;

    @Before
    public void beforeEach() throws Exception {
        contributions = new ArrayList<Contribution>();
    }

    protected void addContributions( int number ) {
        for (int i = 0; i != number; ++i) {
            Contribution contribution = mock(Contribution.class);
            stub(contribution.getSourceName()).toReturn("source " + i);
            contributions.add(contribution);
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateMergePlanFromArrayWithNoContributions() {
        MergePlan.create(contributions.toArray(new Contribution[contributions.size()]));
    }

    @Test
    public void shouldCreateMergePlanFromArrayWithOneContribution() {
        addContributions(1);
        plan = MergePlan.create(contributions.toArray(new Contribution[contributions.size()]));
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(OneContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(1));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromArrayWithTwoContributions() {
        addContributions(2);
        plan = MergePlan.create(contributions.toArray(new Contribution[contributions.size()]));
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(TwoContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(2));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromArrayWithThreeContributions() {
        addContributions(3);
        plan = MergePlan.create(contributions.toArray(new Contribution[contributions.size()]));
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(ThreeContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(3));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromArrayWithFourContributions() {
        addContributions(4);
        plan = MergePlan.create(contributions.toArray(new Contribution[contributions.size()]));
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(FourContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(4));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromArrayWithFiveContributions() {
        addContributions(5);
        plan = MergePlan.create(contributions.toArray(new Contribution[contributions.size()]));
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(FiveContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(5));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromArrayWithSixContributions() {
        addContributions(6);
        plan = MergePlan.create(contributions.toArray(new Contribution[contributions.size()]));
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(MultipleContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(6));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromArrayWithManyContributions() {
        addContributions(100);
        plan = MergePlan.create(contributions.toArray(new Contribution[contributions.size()]));
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(MultipleContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(100));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToCreateMergePlanFromListWithNoContributions() {
        MergePlan.create(contributions);
    }

    @Test
    public void shouldCreateMergePlanFromListWithOneContribution() {
        addContributions(1);
        plan = MergePlan.create(contributions);
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(OneContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(1));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromListWithTwoContributions() {
        addContributions(2);
        plan = MergePlan.create(contributions);
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(TwoContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(2));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromListWithThreeContributions() {
        addContributions(3);
        plan = MergePlan.create(contributions);
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(ThreeContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(3));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromListWithFourContributions() {
        addContributions(4);
        plan = MergePlan.create(contributions);
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(FourContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(4));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromListWithFiveContributions() {
        addContributions(5);
        plan = MergePlan.create(contributions);
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(FiveContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(5));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromListWithSixContributions() {
        addContributions(6);
        plan = MergePlan.create(contributions);
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(MultipleContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(6));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldCreateMergePlanFromListWithManyContributions() {
        addContributions(100);
        plan = MergePlan.create(contributions);
        assertThat(plan, is(instanceOf(MergePlan.class)));
        assertThat(plan, is(instanceOf(MultipleContributionMergePlan.class)));
        assertThat(plan.getContributionCount(), is(100));
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldComputeOnlyOnceTheExpirationTimeFromTheContributions() {
        DateTime nowInUtc = new JodaDateTime().toUtcTimeZone();
        DateTime nowPlus100InUtc = nowInUtc.plusSeconds(100);
        DateTime nowPlus200InUtc = nowInUtc.plusSeconds(200);

        addContributions(2);
        stub(contributions.get(0).getExpirationTimeInUtc()).toReturn(nowPlus100InUtc);
        stub(contributions.get(1).getExpirationTimeInUtc()).toReturn(nowPlus200InUtc);

        plan = MergePlan.create(contributions);
        DateTime expires = plan.getExpirationTimeInUtc();
        assertThat(expires, is(nowPlus100InUtc));
        verify(contributions.get(0), times(1)).getSourceName();
        verify(contributions.get(1), times(1)).getSourceName();
        verify(contributions.get(0), times(1)).getExpirationTimeInUtc();
        verify(contributions.get(1), times(1)).getExpirationTimeInUtc();

        DateTime expires2 = plan.getExpirationTimeInUtc();
        assertThat(expires2, is(nowPlus100InUtc));
        verifyNoMoreInteractions(contributions.get(0));
        verifyNoMoreInteractions(contributions.get(1));
    }

    @Test
    public void shouldDetermineIfExpired() {
        DateTime nowInUtc = new JodaDateTime().toUtcTimeZone();
        DateTime nowPlus100InUtc = nowInUtc.plusSeconds(100);
        DateTime nowPlus200InUtc = nowInUtc.plusSeconds(200);
        DateTime nowPlus300InUtc = nowInUtc.plusSeconds(300);

        addContributions(2);
        stub(contributions.get(0).getExpirationTimeInUtc()).toReturn(nowPlus100InUtc);
        stub(contributions.get(1).getExpirationTimeInUtc()).toReturn(nowPlus200InUtc);

        plan = MergePlan.create(contributions);
        DateTime expires = plan.getExpirationTimeInUtc();
        assertThat(expires, is(nowPlus100InUtc));
        verify(contributions.get(0), times(1)).getExpirationTimeInUtc();
        verify(contributions.get(1), times(1)).getExpirationTimeInUtc();
        assertThat(plan.isExpired(nowInUtc), is(false));
        assertThat(plan.isExpired(nowPlus100InUtc), is(false));
        assertThat(plan.isExpired(nowPlus200InUtc), is(true));
        assertThat(plan.isExpired(nowPlus300InUtc), is(true));
    }

    @Test
    public void shouldHaveNoAnnotationsUponConstruction() {
        addContributions(2);
        plan = MergePlan.create(contributions);
        assertThat(plan.getAnnotationCount(), is(0));
    }

    @Test
    public void shouldAllowSettingAnnotationAndShouldReturnNullWhenSettingNullAnnotation() {
        addContributions(2);
        plan = MergePlan.create(contributions);
        assertThat(plan.setAnnotation(null), is(nullValue()));
    }

    @Test
    public void shouldAllowSettingAnnotationAndShouldReturnPreviousPropertyWhenSettingAnnotation() {
        Property property = new BasicEmptyProperty(new BasicName("uri", "name"));
        Property property2 = new BasicEmptyProperty(property.getName());

        addContributions(2);
        plan = MergePlan.create(contributions);
        assertThat(plan.setAnnotation(property), is(nullValue()));
        assertThat(plan.setAnnotation(property2), is(property));
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldSetAnnotationsMapToNullIfPassedNullOrEmptyMap() {
        addContributions(2);
        plan = MergePlan.create(contributions);
        plan.setAnnotations(null);
        assertThat(plan.getAnnotationCount(), is(0));

        Map<Name, Property> newAnnotations = mock(Map.class);
        plan.setAnnotations(newAnnotations);
        assertThat(plan.getAnnotationCount(), is(0));
        verify(newAnnotations).size();
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldSetAnnotationsMapToSameInstancePassedIn() {
        addContributions(2);
        plan = MergePlan.create(contributions);
        Map<Name, Property> newAnnotations = mock(Map.class);
        stub(newAnnotations.size()).toReturn(3);
        plan.setAnnotations(newAnnotations);
        assertThat(plan.getAnnotationCount(), is(3));
        verify(newAnnotations).size();
    }

    @Test
    public void shouldReturnCopyOfAnnotationsMapFromGetAnnotations() {
        Property property = new BasicEmptyProperty(new BasicName("uri", "name"));
        Map<Name, Property> annotations = new HashMap<Name, Property>();
        annotations.put(property.getName(), property);

        addContributions(2);
        plan = MergePlan.create(contributions);
        plan.setAnnotations(annotations);
        Map<Name, Property> annotationsCopy = plan.getAnnotations();
        assertThat(annotationsCopy, is(not(sameInstance(annotations))));
    }
}
