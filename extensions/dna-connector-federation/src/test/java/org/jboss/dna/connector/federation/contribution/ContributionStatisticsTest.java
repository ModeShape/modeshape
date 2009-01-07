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
package org.jboss.dna.connector.federation.contribution;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.List;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ContributionStatisticsTest {

    /**
     * Statistics should be recorded ONLY during testing within an IDE, and should <i>never</i> be committed to SVN with the
     * {@link ContributionStatistics#RECORD} set to <code>true</code>. This test verifies this, and thus will fail only during IDE
     * testing.
     */
    @Test
    public void shouldNotRecordStatisticsUnlessMeasuringStatisticsInLocalTests() {
        assertThat(ContributionStatistics.RECORD, is(false));
    }

    @Test
    public void shouldRecordTopFiveStatistics() {
        if (!ContributionStatistics.RECORD) return;
        for (int j = 0; j != 10; ++j) {
            for (int i = 0; i != j; ++i)
                ContributionStatistics.record(i, 1);
        }
        List<ContributionStatistics.Data> topData = ContributionStatistics.getTop(5);
        for (ContributionStatistics.Data data : topData) {
            System.out.println(data);
        }
        assertThat(topData.size(), is(5));
        assertThat(topData.get(0).getInstanceCount(), is(9l));
        assertThat(topData.get(1).getInstanceCount(), is(8l));
        assertThat(topData.get(2).getInstanceCount(), is(7l));
        assertThat(topData.get(3).getInstanceCount(), is(6l));
        assertThat(topData.get(4).getInstanceCount(), is(5l));
    }

}
