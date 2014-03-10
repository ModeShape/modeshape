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
import org.modeshape.jcr.cache.NodeKey;

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
        String key = UUID.randomUUID().toString();
        assertThat(PropertyType.discoverType(reference(key)), is(PropertyType.REFERENCE));
        assertThat(PropertyType.discoverType(weakReference(key)), is(PropertyType.WEAKREFERENCE));
        assertThat(PropertyType.discoverType(simpleReference(key)), is(PropertyType.SIMPLEREFERENCE));
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
        return context.getValueFactories().getReferenceFactory().create(new NodeKey(value.toString()));
    }

    protected static Reference weakReference( Object value ) throws ValueFormatException {
        return context.getValueFactories().getWeakReferenceFactory().create(new NodeKey(value.toString()));
    }

    protected static Reference simpleReference( Object value ) throws ValueFormatException {
        return context.getValueFactories().getSimpleReferenceFactory().create(new NodeKey(value.toString()));
    }

}
