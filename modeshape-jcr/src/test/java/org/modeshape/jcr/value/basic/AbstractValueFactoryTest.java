/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.IoException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;

public class AbstractValueFactoryTest {

    public static final TextDecoder CUSTOM_DECODER = new NoOpEncoder();

    private static class MockFactory extends AbstractValueFactory<String> {

        protected MockFactory( TextDecoder decoder,
                               ValueFactories valueFactories ) {
            super(PropertyType.STRING, decoder, valueFactories);
        }

        @Override
        public ValueFactory<String> with( ValueFactories valueFactories ) {
            return new MockFactory(getDecoder(), valueFactories);
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
        public String create( BinaryValue value ) throws ValueFormatException, IoException {
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

        @Override
        public String[] createEmptyArray( int length ) {
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
        if (factory.getPropertyType() != PropertyType.STRING) {
            assertThat(factory.getStringValueFactory(), is(nullValue()));
        }
    }

    @Test
    public void shouldHaveExpectedPropertyType() {
        assertThat(factory.getPropertyType(), is(sameInstance(PropertyType.STRING)));
    }

}
