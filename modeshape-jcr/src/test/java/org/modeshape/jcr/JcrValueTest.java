/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import javax.jcr.PropertyType;
import javax.jcr.ValueFormatException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.basic.StandardValueFactories;

/**
 * @author jverhaeg
 */
public class JcrValueTest {

    private ValueFactories factories;
    private JcrValue value;
    @Mock
    private SessionCache cache;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        factories = new StandardValueFactories(Mockito.mock(NamespaceRegistry.class));
        value = new JcrValue(factories, cache, PropertyType.BOOLEAN, true);
    }

    @Test
    public void shouldProvideType() throws Exception {
        assertThat(value.getType(), is(PropertyType.BOOLEAN));
    }

    @Test
    public void shouldAllowConsumingInputStreamAfterConsumingNonInputStream() throws Exception {
        value.getBoolean();
        value.getStream();
    }

    @Test
    public void shouldAllowConsumingNonInputStreamAfterConsumingInputStream() throws Exception {
        value.getBoolean();
        value.getStream();
    }

    @Test
    public void shouldProvideBooleanForBoolean() throws Exception {
        assertThat(value.getBoolean(), is(true));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDateForBoolean() throws Exception {
        value.getDate();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDoubleForBoolean() throws Exception {
        value.getDouble();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLongForBoolean() throws Exception {
        value.getLong();
    }

    @Test
    public void shouldProvideStreamForBoolean() throws Exception {
        testProvidesStream(value);
    }

    @Test
    public void shouldProvideStringForBoolean() throws Exception {
        assertThat(value.getString(), is("true"));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideBooleanForDate() throws Exception {
        new JcrValue(factories, cache, PropertyType.DATE, new Date()).getBoolean();
    }

    @Test
    public void shouldProvideDateForDate() throws Exception {
        Date date = new Date();
        assertThat(new JcrValue(factories, cache, PropertyType.DATE, date).getDate().getTime(), is(date));
    }

    @Test
    public void shouldProvideDoubleForDate() throws Exception {
        Date date = new Date();
        assertThat(new JcrValue(factories, cache, PropertyType.DATE, date).getDouble(), is((double)date.getTime()));
    }

    @Test
    public void shouldProvideLongForDate() throws Exception {
        Date date = new Date();
        assertThat(new JcrValue(factories, cache, PropertyType.DATE, date).getLong(), is(date.getTime()));
    }

    @Test
    public void shouldProvideStreamForDate() throws Exception {
        testProvidesStream(new JcrValue(factories, cache, PropertyType.DATE, new Date()));
    }

    @Test
    public void shouldProvideStringForDate() throws Exception {
        Calendar date = Calendar.getInstance();
        date.set(2008, 7, 18, 12, 0, 0);
        date.set(Calendar.MILLISECOND, 0);
        String expectedValue = "2008-08-18T12:00:00.000";
        assertThat(new JcrValue(factories, cache, PropertyType.DATE, date).getString().substring(0, expectedValue.length()),
                   is(expectedValue));
        assertThat(new JcrValue(factories, cache, PropertyType.DATE, date.getTime()).getString()
                                                                                    .substring(0, expectedValue.length()),
                   is(expectedValue));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideBooleanForDouble() throws Exception {
        new JcrValue(factories, cache, PropertyType.DOUBLE, 0.0).getBoolean();
    }

    @Test
    public void shouldProvideDateForDouble() throws Exception {
        Calendar expectedValue = Calendar.getInstance();
        expectedValue.setTime(new Date(0L));
        assertThat(new JcrValue(factories, cache, PropertyType.DOUBLE, 0.0).getDate().getTimeInMillis(),
                   is(expectedValue.getTimeInMillis()));
    }

    @Test
    public void shouldProvideDoubleForDouble() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.DOUBLE, 1.2).getDouble(), is(1.2));
    }

    @Test
    public void shouldProvideLongForDouble() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.DOUBLE, 1.0).getLong(), is(1L));
        assertThat(new JcrValue(factories, cache, PropertyType.DOUBLE, Double.MAX_VALUE).getLong(), is(Long.MAX_VALUE));
    }

    @Test
    public void shouldProvideStreamForDouble() throws Exception {
        testProvidesStream(new JcrValue(factories, cache, PropertyType.DOUBLE, 1.0));
    }

    @Test
    public void shouldProvideStringForDouble() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.DOUBLE, 1.0).getString(), is("1.0"));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideBooleanForLong() throws Exception {
        new JcrValue(factories, cache, PropertyType.LONG, 0L).getBoolean();
    }

    @Test
    public void shouldProvideDateForLong() throws Exception {
        Calendar expectedValue = Calendar.getInstance();
        expectedValue.setTime(new Date(0L));
        assertThat(new JcrValue(factories, cache, PropertyType.LONG, 0L).getDate().getTimeInMillis(),
                   is(expectedValue.getTimeInMillis()));
    }

    @Test
    public void shouldProvideDoubleForLong() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.LONG, 1L).getDouble(), is(1.0));
    }

    @Test
    public void shouldProvideLongForLong() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.LONG, 1L).getLong(), is(1L));
    }

    @Test
    public void shouldProvideStreamForLong() throws Exception {
        testProvidesStream(new JcrValue(factories, cache, PropertyType.LONG, 1L));
    }

    @Test
    public void shouldProvideStringForLong() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.LONG, 1L).getString(), is("1"));
    }

    @Test
    public void shouldProvideBooleanForString() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.STRING, "true").getBoolean(), is(true));
        assertThat(new JcrValue(factories, cache, PropertyType.STRING, "yes").getBoolean(), is(false));
    }

    @Test
    public void shouldProvideDateForString() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.STRING, "2008").getDate(), notNullValue());
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDateForInvalidString() throws Exception {
        new JcrValue(factories, cache, PropertyType.STRING, "true").getDate();
    }

    @Test
    public void shouldProvideDoubleForString() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.STRING, "1").getDouble(), is(1.0));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDoubleForInvalidString() throws Exception {
        new JcrValue(factories, cache, PropertyType.STRING, "true").getDouble();
    }

    @Test
    public void shouldProvideLongForString() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.STRING, "1").getLong(), is(1L));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLongForInvalidString() throws Exception {
        new JcrValue(factories, cache, PropertyType.STRING, "true").getLong();
    }

    @Test
    public void shouldProvideStreamForString() throws Exception {
        testProvidesStream(new JcrValue(factories, cache, PropertyType.STRING, "true"));
    }

    @Test
    public void shouldProvideStringForString() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.STRING, "true").getString(), is("true"));
    }

    @Test
    public void shouldProvideBooleanForUuid() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.STRING, UUID.randomUUID()).getBoolean(), is(false));
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDateForUuid() throws Exception {
        new JcrValue(factories, cache, PropertyType.STRING, UUID.randomUUID()).getDate();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideDoubleForUuid() throws Exception {
        new JcrValue(factories, cache, PropertyType.STRING, UUID.randomUUID()).getDouble();
    }

    @Test( expected = ValueFormatException.class )
    public void shouldNotProvideLongForUuid() throws Exception {
        new JcrValue(factories, cache, PropertyType.STRING, UUID.randomUUID()).getLong();
    }

    @Test
    public void shouldProvideStreamForUuid() throws Exception {
        testProvidesStream(new JcrValue(factories, cache, PropertyType.STRING, UUID.randomUUID()));
    }

    @Test
    public void shouldProvideStringForUuid() throws Exception {
        String expectedValue = "40d373f7-75ad-4d84-900e-c72ebd98abb9";
        assertThat(new JcrValue(factories, cache, PropertyType.STRING, UUID.fromString(expectedValue)).getString(),
                   is(expectedValue));
    }

    @Test
    public void shouldProvideLength() throws Exception {
        assertThat(new JcrValue(factories, cache, PropertyType.STRING, "test").getLength(), is(4L));
        assertThat(new JcrValue(factories, cache, PropertyType.BINARY, "test").getLength(), is(4L));
    }

    private void testProvidesStream( JcrValue value ) throws Exception {
        InputStream stream = value.getStream();
        assertThat(stream, notNullValue());
        stream.close();
    }
}
