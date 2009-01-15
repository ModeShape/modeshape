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
public class ThreeContributionMergePlanTest {

    private ThreeContributionMergePlan plan;
    @Mock
    private Contribution contribution1;
    @Mock
    private Contribution contribution2;
    @Mock
    private Contribution contribution3;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        stub(contribution1.getSourceName()).toReturn("source1");
        stub(contribution2.getSourceName()).toReturn("source2");
        stub(contribution3.getSourceName()).toReturn("source3");
        plan = new ThreeContributionMergePlan(contribution1, contribution2, contribution3);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullFirstContribution() {
        contribution1 = null;
        plan = new ThreeContributionMergePlan(contribution1, contribution2, contribution3);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullSecondContribution() {
        contribution2 = null;
        plan = new ThreeContributionMergePlan(contribution1, contribution2, contribution3);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullThirdContribution() {
        contribution3 = null;
        plan = new ThreeContributionMergePlan(contribution1, contribution2, contribution3);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithMultipleContributionsFromTheSameSource() {
        plan = new ThreeContributionMergePlan(contribution1, contribution2, contribution1);
    }

    @Test
    public void shouldReturnIteratorOverContributions() {
        assertThat(plan, hasItems(contribution1, contribution2, contribution3));
    }

    @Test
    public void shouldHaveContributionCountOfThree() {
        assertThat(plan.getContributionCount(), is(3));
    }

    @Test
    public void shouldReturnContributionWhenSuppliedNameMatchesContributionsSourceName() {
        assertThat(plan.getContributionFrom(contribution1.getSourceName()), is(sameInstance(contribution1)));
        assertThat(plan.getContributionFrom(contribution2.getSourceName()), is(sameInstance(contribution2)));
        assertThat(plan.getContributionFrom(contribution3.getSourceName()), is(sameInstance(contribution3)));
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
        assertThat(plan.isSource("other source"), is(false));
    }
}
