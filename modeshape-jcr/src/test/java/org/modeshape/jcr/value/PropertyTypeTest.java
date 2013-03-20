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
package org.modeshape.jcr.value;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.UUID;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.value.DateTime;

public class PropertyTypeTest {

    private static ExecutionContext context;

    @BeforeClass
    public static void beforeAll() {
        context = new ExecutionContext();
    }

    @Test
    public void shouldDiscoverPropertyTypeForValue() throws Exception {
        assertThat(PropertyType.discoverType(""), is(PropertyType.STRING));
        assertThat(PropertyType.discoverType("something"), is(PropertyType.STRING));
        assertThat(PropertyType.discoverType(binary("something")), is(PropertyType.BINARY));
        assertThat(PropertyType.discoverType(5), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType((short)5), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(5L), is(PropertyType.LONG));
        assertThat(PropertyType.discoverType(true), is(PropertyType.BOOLEAN));
        assertThat(PropertyType.discoverType(5.0f), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(5.0d), is(PropertyType.DOUBLE));
        assertThat(PropertyType.discoverType(decimal(5.0d)), is(PropertyType.DECIMAL));
        assertThat(PropertyType.discoverType(uri("http://x.com")), is(PropertyType.URI));
        assertThat(PropertyType.discoverType(path("/jcr:system/a/b/c")), is(PropertyType.PATH));
        assertThat(PropertyType.discoverType(path("/jcr:system")), is(PropertyType.PATH));
        assertThat(PropertyType.discoverType(path("/")), is(PropertyType.PATH));
        assertThat(PropertyType.discoverType(path("[idnetifier]")), is(PropertyType.PATH));
        assertThat(PropertyType.discoverType(name("jcr:system")), is(PropertyType.NAME));
        assertThat(PropertyType.discoverType(name("system")), is(PropertyType.NAME));
        assertThat(PropertyType.discoverType(date()), is(PropertyType.DATE));
        assertThat(PropertyType.discoverType(calendar()), is(PropertyType.DATE));
        assertThat(PropertyType.discoverType(reference(uuid())), is(PropertyType.REFERENCE));
        assertThat(PropertyType.discoverType(weakReference(uuid())), is(PropertyType.WEAKREFERENCE));
    }

    protected static Path path( Object path ) throws ValueFormatException {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected static Name name( Object name ) throws ValueFormatException {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected static Binary binary( Object value ) throws ValueFormatException {
        return context.getValueFactories().getBinaryFactory().create(value);
    }

    protected static URI uri( Object value ) throws ValueFormatException {
        return context.getValueFactories().getUriFactory().create(value);
    }

    protected static UUID uuid( Object value ) throws ValueFormatException {
        return context.getValueFactories().getUuidFactory().create(value);
    }

    protected static UUID uuid() throws ValueFormatException {
        return context.getValueFactories().getUuidFactory().create();
    }

    protected static BigDecimal decimal( Object value ) throws ValueFormatException {
        return context.getValueFactories().getDecimalFactory().create(value);
    }

    protected static DateTime date( Object value ) throws ValueFormatException {
        return context.getValueFactories().getDateFactory().create(value);
    }

    protected static DateTime date() throws ValueFormatException {
        return context.getValueFactories().getDateFactory().create();
    }

    protected static Calendar calendar( Object value ) throws ValueFormatException {
        return date(value).toCalendar();
    }

    protected static Calendar calendar() throws ValueFormatException {
        return date().toCalendar();
    }

    protected static Reference reference( Object value ) throws ValueFormatException {
        return context.getValueFactories().getReferenceFactory().create(value);
    }

    protected static Reference weakReference( Object value ) throws ValueFormatException {
        return context.getValueFactories().getWeakReferenceFactory().create(value);
    }

}
