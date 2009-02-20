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
import static org.junit.matchers.JUnitMatchers.hasItems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.basic.BasicName;
import org.jboss.dna.graph.property.basic.BasicSingleValueProperty;
import org.jboss.dna.graph.property.basic.JodaDateTime;
import org.jboss.dna.graph.property.basic.RootPath;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class MultiPropertyContributionTest {

    private static final long TWENTY_FOUR_HOURS_IN_MILLISECONDS = 24 * 60 * 60 * 1000;
    public static final DateTime NOW = new JodaDateTime(System.currentTimeMillis()).toUtcTimeZone();
    public static final DateTime YESTERDAY = new JodaDateTime(NOW.getMilliseconds() - TWENTY_FOUR_HOURS_IN_MILLISECONDS).toUtcTimeZone();
    public static final DateTime TOMORROW = new JodaDateTime(NOW.getMilliseconds() + TWENTY_FOUR_HOURS_IN_MILLISECONDS).toUtcTimeZone();

    private String sourceName;
    private String workspaceName;
    private Path pathInSource;
    private MultiPropertyContribution contribution;
    private DateTime expiration;
    private List<Property> properties;
    private Property property1;
    private Property property2;
    private Property property3;

    @Before
    public void beforeEach() throws Exception {
        sourceName = "some source";
        workspaceName = "some workspace";
        pathInSource = RootPath.INSTANCE;
        expiration = TOMORROW;
        String nsUri = "http://www.jboss.org/default";
        property1 = new BasicSingleValueProperty(new BasicName(nsUri, "property1"), "value1");
        property2 = new BasicSingleValueProperty(new BasicName(nsUri, "property2"), "value2");
        property3 = new BasicSingleValueProperty(new BasicName(nsUri, "property3"), "value3");
        properties = Arrays.asList(property1, property2, property3);
        contribution = new MultiPropertyContribution(sourceName, workspaceName, Location.create(pathInSource), expiration,
                                                     properties);
    }

    @Test
    public void shouldAllowNullExpiration() {
        expiration = null;
        contribution = new MultiPropertyContribution(sourceName, workspaceName, Location.create(pathInSource), expiration,
                                                     properties);
        assertThat(contribution.getExpirationTimeInUtc(), is(nullValue()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowExpirationTimeIfNotInUtcTime() {
        expiration = new JodaDateTime(System.currentTimeMillis(), "CST");
        contribution = new MultiPropertyContribution(sourceName, workspaceName, Location.create(pathInSource), expiration,
                                                     properties);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowNullProperties() {
        properties = null;
        contribution = new MultiPropertyContribution(sourceName, workspaceName, Location.create(pathInSource), expiration,
                                                     properties);
    }

    @Test( expected = AssertionError.class )
    public void shouldNotAllowEmptyProperties() {
        properties = Collections.emptyList();
        contribution = new MultiPropertyContribution(sourceName, workspaceName, Location.create(pathInSource), expiration,
                                                     properties);
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
        contribution = new MultiPropertyContribution(sourceName, workspaceName, Location.create(pathInSource), NOW, properties);
        assertThat(contribution.isExpired(YESTERDAY), is(false));
        assertThat(contribution.isExpired(TOMORROW), is(true));
    }

    @Test
    public void shouldHaveNoChildren() {
        assertThat(contribution.getChildrenCount(), is(0));
        assertThat(contribution.getChildren().hasNext(), is(false));
    }

    @Test
    public void shouldHaveProperties() {
        assertThat(contribution.getPropertyCount(), is(3));
        assertThat(contribution.getProperty(property1.getName()), is(sameInstance(property1)));
        assertThat(contribution.getProperty(property2.getName()), is(sameInstance(property2)));
        assertThat(contribution.getProperty(property3.getName()), is(sameInstance(property3)));
        assertThat(contribution.getProperty(new BasicName("http://www.jboss.org/x", "propertyX")), is(nullValue()));
        List<Property> properties = new ArrayList<Property>();
        for (Iterator<Property> iter = contribution.getProperties(); iter.hasNext();) {
            properties.add(iter.next());
        }
        assertThat(properties, hasItems(property1, property2, property3));
    }

}
