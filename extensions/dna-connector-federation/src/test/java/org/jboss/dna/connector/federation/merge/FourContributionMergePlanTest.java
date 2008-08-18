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
package org.jboss.dna.connector.federation.merge;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.mockito.Mockito.stub;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class FourContributionMergePlanTest {

    private FourContributionMergePlan plan;
    @Mock
    private Contribution contribution1;
    @Mock
    private Contribution contribution2;
    @Mock
    private Contribution contribution3;
    @Mock
    private Contribution contribution4;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        stub(contribution1.getSourceName()).toReturn("source1");
        stub(contribution2.getSourceName()).toReturn("source2");
        stub(contribution3.getSourceName()).toReturn("source3");
        stub(contribution4.getSourceName()).toReturn("source4");
        plan = new FourContributionMergePlan(contribution1, contribution2, contribution3, contribution4);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullFirstContribution() {
        contribution1 = null;
        plan = new FourContributionMergePlan(contribution1, contribution2, contribution3, contribution4);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullSecondContribution() {
        contribution2 = null;
        plan = new FourContributionMergePlan(contribution1, contribution2, contribution3, contribution4);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullThirdContribution() {
        contribution3 = null;
        plan = new FourContributionMergePlan(contribution1, contribution2, contribution3, contribution4);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullFourthContribution() {
        contribution4 = null;
        plan = new FourContributionMergePlan(contribution1, contribution2, contribution3, contribution4);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithMultipleContributionsFromTheSameSource() {
        plan = new FourContributionMergePlan(contribution1, contribution1, contribution3, contribution4);
    }

    @Test
    public void shouldReturnIteratorOverContributions() {
        assertThat(plan, hasItems(contribution1, contribution2, contribution3, contribution4));
    }

    @Test
    public void shouldHaveContributionCountOfFour() {
        assertThat(plan.getContributionCount(), is(4));
    }

    @Test
    public void shouldReturnContributionWhenSuppliedNameMatchesContributionsSourceName() {
        assertThat(plan.getContributionFrom(contribution1.getSourceName()), is(sameInstance(contribution1)));
        assertThat(plan.getContributionFrom(contribution2.getSourceName()), is(sameInstance(contribution2)));
        assertThat(plan.getContributionFrom(contribution3.getSourceName()), is(sameInstance(contribution3)));
        assertThat(plan.getContributionFrom(contribution4.getSourceName()), is(sameInstance(contribution4)));
    }

    @Test
    public void shouldReturnNullContributionWhenSuppliedNameDoesNotMatchContributionsSourceName() {
        assertThat(plan.getContributionFrom("other source"), is(nullValue()));
    }

    @Test
    public void shouldCompareSourceNameOfContributionsWhenCallingIsSource() {
        assertThat(plan.isSource(contribution1.getSourceName()), is(true));
        assertThat(plan.isSource(contribution2.getSourceName()), is(true));
        assertThat(plan.isSource(contribution3.getSourceName()), is(true));
        assertThat(plan.isSource(contribution4.getSourceName()), is(true));
        assertThat(plan.isSource("other source"), is(false));
    }
}
