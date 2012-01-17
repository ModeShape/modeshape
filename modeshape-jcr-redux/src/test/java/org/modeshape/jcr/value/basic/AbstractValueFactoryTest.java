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
package org.modeshape.jcr.value.basic;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Binary;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

/**
 * @author Randall Hauch
 */
public class AbstractValueFactoryTest {

    public static final TextDecoder CUSTOM_DECODER = new NoOpEncoder();

    private static class MockFactory extends AbstractValueFactory<String> {

        protected MockFactory( TextDecoder decoder,
                               StringValueFactory stringValueFactory ) {
            super(PropertyType.STRING, decoder, stringValueFactory);
        }

        @Override
        public String create( String value ) {
            return null;
        }

        @Override
        public String create( String value,
                              TextDecoder decoder ) {
            return null;
        }

        @Override
        public String create( int value ) {
            return null;
        }

        @Override
        public String create( long value ) {
            return null;
        }

        @Override
        public String create( boolean value ) {
            return null;
        }

        @Override
        public String create( float value ) {
            return null;
        }

        @Override
        public String create( double value ) {
            return null;
        }

        @Override
        public String create( BigDecimal value ) {
            return null;
        }

        @Override
        public String create( Calendar value ) {
            return null;
        }

        @Override
        public String create( Date value ) {
            return null;
        }

        @Override
        public String create( DateTime value ) throws ValueFormatException {
            return null;
        }

        @Override
        public String create( Name value ) {
            return null;
        }

        @Override
        public String create( Path value ) {
            return null;
        }

        @Override
        public String create( Path.Segment value ) {
            return null;
        }

        @Override
        public String create( Reference value ) {
            return null;
        }

        @Override
        public String create( URI value ) {
            return null;
        }

        @Override
        public String create( UUID value ) {
            return null;
        }

        @Override
        public String create( byte[] value ) {
            return null;
        }

        @Override
        public String create( Binary value ) throws ValueFormatException, IoException {
            return null;
        }

        @Override
        public String create( NodeKey value ) throws ValueFormatException {
            return null;
        }

        @Override
        public String create( InputStream stream ) {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String[] createEmptyArray( int length ) {
            return new String[length];
        }
    }

    private AbstractValueFactory<String> factory;

    @Before
    public void beforeEach() {
        factory = new MockFactory(null, null);
    }

    @Test
    public void shouldHaveDefaultEncoderIfNullPassedIntoConstructor() {
        assertThat(factory.getDecoder(), is(notNullValue()));
        assertThat(factory.getDecoder(), is(sameInstance(ValueFactory.DEFAULT_DECODER)));
    }

    @Test
    public void shouldReturnTextEncoderPassedIntoConstructor() {
        factory = new MockFactory(CUSTOM_DECODER, null);
        assertThat(factory.getDecoder(), is(notNullValue()));
        assertThat(factory.getDecoder(), is(sameInstance(CUSTOM_DECODER)));
    }

    @Test
    public void shouldReturnDefaultTextEncoderWhenNullPassedToGetEncoder() {
        assertThat(factory.getDecoder(), is(sameInstance(ValueFactory.DEFAULT_DECODER)));
        assertThat(factory.getDecoder(null), is(sameInstance(ValueFactory.DEFAULT_DECODER)));
        assertThat(factory.getDecoder(CUSTOM_DECODER), is(sameInstance(CUSTOM_DECODER)));
    }

    @Test
    public void shouldReturnSuppliedTextEncoderWhenNonNullPassedToGetEncoder() {
        assertThat(factory.getDecoder(), is(sameInstance(ValueFactory.DEFAULT_DECODER)));
        assertThat(factory.getDecoder(null), is(sameInstance(ValueFactory.DEFAULT_DECODER)));
        assertThat(factory.getDecoder(CUSTOM_DECODER), is(sameInstance(CUSTOM_DECODER)));
    }

    @Test
    public void shouldHaveNullStringValueFactoryIfNullPassedIntoConstructor() {
        assertThat(factory.getStringValueFactory(), is(nullValue()));
    }

    @Test
    public void shouldHaveExpectedPropertyType() {
        assertThat(factory.getPropertyType(), is(sameInstance(PropertyType.STRING)));
    }

}
