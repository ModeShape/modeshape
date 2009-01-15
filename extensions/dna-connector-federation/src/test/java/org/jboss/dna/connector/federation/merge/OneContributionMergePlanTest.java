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
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Mockito.stub;
import java.util.Iterator;
import org.jboss.dna.connector.federation.contribution.Contribution;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class OneContributionMergePlanTest {

    private OneContributionMergePlan plan;
    @Mock
    private Contribution contribution1;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        plan = new OneContributionMergePlan(contribution1);
        stub(contribution1.getSourceName()).toReturn("source");
    }

    @Test( expected = AssertionError.class )
    public void shouldFailWhenCreatingMergePlanWithNullFirstContribution() {
        contribution1 = null;
        plan = new OneContributionMergePlan(contribution1);
    }

    @Test
    public void shouldReturnIteratorOverContributions() {
        Iterator<Contribution> iter = plan.iterator();
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(sameInstance(contribution1)));
        assertThat(iter.hasNext(), is(false));
        assertThat(plan, hasItem(contribution1));
    }

    @Test
    public void shouldHaveContributionCountOfOne() {
        assertThat(plan.getContributionCount(), is(1));
    }

    @Test
    public void shouldReturnContributionWhenSuppliedNameMatchesContributionsSourceName() {
        assertThat(plan.getContributionFrom(contribution1.getSourceName()), is(sameInstance(contribution1)));
    }

    @Test
    public void shouldReturnNullContributionWhenSuppliedNameDoesNotMatchContributionsSourceName() {
        assertThat(plan.getContributionFrom("other source"), is(nullValue()));
    }

    @Test
    public void shouldCompareSourceNameOfContributionsWhenCallingIsSource() {
        assertThat(plan.isSource(contribution1.getSourceName()), is(true));
        assertThat(plan.isSource("other source"), is(false));
    }
}
