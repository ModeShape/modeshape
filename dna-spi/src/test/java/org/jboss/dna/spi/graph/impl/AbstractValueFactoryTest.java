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
package org.jboss.dna.spi.graph.impl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import org.jboss.dna.common.text.NoOpEncoder;
import org.jboss.dna.common.text.TextDecoder;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.Reference;
import org.jboss.dna.spi.graph.ValueFactory;
import org.junit.Before;
import org.junit.Test;

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

        public String create( String value ) {
            return null;
        }

        public String create( String value,
                              TextDecoder decoder ) {
            return null;
        }

        public String create( int value ) {
            return null;
        }

        public String create( long value ) {
            return null;
        }

        public String create( boolean value ) {
            return null;
        }

        public String create( float value ) {
            return null;
        }

        public String create( double value ) {
            return null;
        }

        public String create( BigDecimal value ) {
            return null;
        }

        public String create( Calendar value ) {
            return null;
        }

        public String create( Date value ) {
            return null;
        }

        public String create( Name value ) {
            return null;
        }

        public String create( Path value ) {
            return null;
        }

        public String create( Reference value ) {
            return null;
        }

        public String create( URI value ) {
            return null;
        }

        public String create( byte[] value ) {
            return null;
        }

        public String create( InputStream stream,
                              int approximateLength ) {
            return null;
        }

        public String create( Reader reader,
                              int approximateLength ) {
            return null;
        }
    }

    private AbstractValueFactory factory;

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
