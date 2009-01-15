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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class MultipleContributionMergePlanTest {

    private MultipleContributionMergePlan plan;
    private List<Contribution> contributions;

    @Before
    public void beforeEach() throws Exception {
        contributions = new LinkedList<Contribution>();
        addContributions(10);
        plan = new MultipleContributionMergePlan(contributions);
    }

    protected void addContributions( int number ) {
        for (int i = 0; i != number; ++i) {
            Contribution contribution = mock(Contribution.class);
            stub(contribution.getSourceName()).toReturn("source " + i);
            contributions.add(contribution);
        }
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullFirstContribution() {
        contributions.add(0, null);
        plan = new MultipleContributionMergePlan(contributions);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullSecondContribution() {
        contributions.add(1, null);
        plan = new MultipleContributionMergePlan(contributions);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullThirdContribution() {
        contributions.add(2, null);
        plan = new MultipleContributionMergePlan(contributions);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullFourthContribution() {
        contributions.add(3, null);
        plan = new MultipleContributionMergePlan(contributions);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullFifthContribution() {
        contributions.add(4, null);
        plan = new MultipleContributionMergePlan(contributions);
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithMultipleContributionsFromTheSameSource() {
        contributions.add(contributions.get(0));
        plan = new MultipleContributionMergePlan(contributions);
    }

    @Test
    public void shouldReturnIteratorOverContributions() {
        assertThat(plan, hasItems(contributions.toArray(new Contribution[contributions.size()])));
    }

    @Test
    public void shouldHaveContributionCountOfFive() {
        assertThat(plan.getContributionCount(), is(contributions.size()));
    }

    @Test
    public void shouldReturnContributionWhenSuppliedNameMatchesContributionsSourceName() {
        for (Contribution contribution : contributions) {
            assertThat(plan.getContributionFrom(contribution.getSourceName()), is(sameInstance(contribution)));
        }
    }

    @Test
    public void shouldReturnNullContributionWhenSuppliedNameDoesNotMatchContributionsSourceName() {
        assertThat(plan.getContributionFrom("other source"), is(nullValue()));
    }

    @Test
    public void shouldCompareSourceNameOfContributionsWhenCallingIsSource() {
        for (Contribution contribution : contributions) {
            assertThat(plan.isSource(contribution.getSourceName()), is(true));
        }
        assertThat(plan.isSource("other source"), is(false));
    }
}
