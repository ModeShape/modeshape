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
package org.jboss.dna.connector.federation.contribution;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.basic.JodaDateTime;
import org.jboss.dna.graph.property.basic.RootPath;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class MultiChildContributionTest {

    private static final long TWENTY_FOUR_HOURS_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
    public static final DateTime NOW = new JodaDateTime(System.currentTimeMillis()).toUtcTimeZone();
    public static final DateTime YESTERDAY = new JodaDateTime(NOW.getMilliseconds() - TWENTY_FOUR_HOURS_IN_MILLISECONDS).toUtcTimeZone();
    public static final DateTime TOMORROW = new JodaDateTime(NOW.getMilliseconds() + TWENTY_FOUR_HOURS_IN_MILLISECONDS).toUtcTimeZone();

    private String sourceName;
    private String workspaceName;
    private Path pathInSource;
    private MultiChildContribution contribution;
    private DateTime expiration;
    private List<Location> children;
    private Location child1;
    private Location child2;
    private Location child3;

    @Before
    public void beforeEach() throws Exception {
        sourceName = "some source";
        workspaceName = "some workspace";
        pathInSource = RootPath.INSTANCE;
        expiration = TOMORROW;
        child1 = mock(Location.class);
        child2 = mock(Location.class);
        child3 = mock(Location.class);
        children = Arrays.asList(child1, child2, child3);
        contribution = new MultiChildContribution(sourceName, workspaceName, Location.create(pathInSource), expiration, children);
    }

    @Test
    public void shouldAllowNullExpiration() {
        expiration = null;
        contribution = new MultiChildContribution(sourceName, workspaceName, Location.create(pathInSource), expiration, children);
        assertThat(contribution.getExpirationTimeInUtc(), is(nullValue()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowExpirationTimeIfNotInUtcTime() {
        expiration = new JodaDateTime(System.currentTimeMillis(), "CST");
        contribution = new MultiChildContribution(sourceName, workspaceName, Location.create(pathInSource), expiration, children);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullChildren() {
        children = null;
        contribution = new MultiChildContribution(sourceName, workspaceName, Location.create(pathInSource), expiration, children);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowEmptyChildren() {
        children = Collections.emptyList();
        contribution = new MultiChildContribution(sourceName, workspaceName, Location.create(pathInSource), expiration, children);
    }

    @Test
    public void shouldHaveSameExpirationTimeSetInConstructor() {
        assertThat(contribution.getExpirationTimeInUtc(), is(sameInstance(expiration)));
    }

    @Test
    public void shouldHaveSameSourceNameSetInConstructor() {
        assertThat(contribution.getSourceName(), is(sourceName));
    }

    @Test
    public void shouldNotBeExpiredIfExpirationIsInTheFuture() {
        contribution = new MultiChildContribution(sourceName, workspaceName, Location.create(pathInSource), NOW, children);
        assertThat(contribution.isExpired(YESTERDAY), is(false));
        assertThat(contribution.isExpired(TOMORROW), is(true));
    }

    @Test
    public void shouldHaveChildren() {
        assertThat(contribution.getChildrenCount(), is(3));
        Iterator<Location> iter = contribution.getChildren();
        assertThat(iter.next(), is(child1));
        assertThat(iter.next(), is(child2));
        assertThat(iter.next(), is(child3));
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldHaveNoProperties() {
        assertThat(contribution.getPropertyCount(), is(0));
        assertThat(contribution.getProperties().hasNext(), is(false));
        Name propertyName = mock(Name.class); // doesn't matter what the name instance is
        assertThat(contribution.getProperty(propertyName), is(nullValue()));
    }
}
