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
package org.modeshape.graph.property.basic;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import org.modeshape.graph.property.NamespaceRegistry;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractNamespaceRegistryTest<NamespaceRegistryType extends NamespaceRegistry> {

    protected String validNamespaceUri1;
    protected String validNamespaceUri2;
    protected String validNamespaceUri3;
    protected String validPrefix1;
    protected String validPrefix2;
    protected NamespaceRegistryType namespaceRegistry;

    @Before
    public void setUp() {
        validNamespaceUri1 = "http://example.com/foo";
        validNamespaceUri2 = "http://acme.com/something";
        validNamespaceUri3 = "http://www.redhat.com";
        validPrefix1 = "foo";
        validPrefix2 = "acme";
        namespaceRegistry = createNamespaceRegistry();
    }

    protected abstract NamespaceRegistryType createNamespaceRegistry();

    @Test
    public void shouldReturnNullForNamespaceUriIfPrefixIsNotRegistered() {
        assertThat(namespaceRegistry.getNamespaceForPrefix("notfound"), is(nullValue()));
        // now register some namespaces ...
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        assertThat(namespaceRegistry.getNamespaceForPrefix("notfound"), is(nullValue()));
    }

    @Test
    public void shouldReturnNamespaceUriForPrefixThatIsRegistered() {
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        assertThat(namespaceRegistry.getNamespaceForPrefix(validPrefix1), is(validNamespaceUri1));
        assertThat(namespaceRegistry.getNamespaceForPrefix(validPrefix2), is(validNamespaceUri2));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullPrefixParameterWhenGettingNamespaceUri() {
        namespaceRegistry.getNamespaceForPrefix(null);
    }

    @Test
    public void shouldReturnNullForPrefixIfNamespaceUriIsNotRegistered() {
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri3, false), is(nullValue()));
        // now register some namespaces ...
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri3, false), is(nullValue()));
    }

    @Test
    public void shouldReturnPrefixForNamespaceUriThatIsRegistered() {
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri1, false), is(validPrefix1));
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri2, false), is(validPrefix2));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullNamespaceUriParameterWhenGettingPrefix() {
        namespaceRegistry.getPrefixForNamespaceUri(null, false);
    }

    @Test
    public void shouldAlwaysHaveDefaultNamespaceRegistered() {
        assertThat(namespaceRegistry.getDefaultNamespaceUri(), is(notNullValue()));
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        assertThat(namespaceRegistry.getDefaultNamespaceUri(), is(notNullValue()));
    }

    @Test
    public void shouldReturnNonNullDefaultNamespaceUriWhenThereAreNamespacesRegisteredIncludineOneWithZeroLengthPrefix() {
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register("", validNamespaceUri2);
        assertThat(namespaceRegistry.getDefaultNamespaceUri(), is(validNamespaceUri2));
        assertThat(namespaceRegistry.getNamespaceForPrefix(""), is(validNamespaceUri2));
        assertThat(namespaceRegistry.getRegisteredNamespaceUris(), hasItem(validNamespaceUri2));
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri2, false), is(""));
    }

    @Test
    public void shouldNotFindRegisteredNamespaceIfNamespaceNotRegistered() {
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri1), is(false));
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri2), is(false));
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri3), is(false));
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri1), is(true));
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri2), is(true));
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri3), is(false));
    }

    @Test
    public void shouldFindRegisteredNamespace() {
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri1), is(true));
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri2), is(true));
    }

    @Test
    public void shouldBeAbleToCopyNamespacesToAnotherRegistry() {
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        namespaceRegistry.register("", validNamespaceUri3);
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri1), is(true));
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri2), is(true));
        assertThat(namespaceRegistry.isRegisteredNamespaceUri(validNamespaceUri3), is(true));
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri1, false), is(validPrefix1));
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri2, false), is(validPrefix2));
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri3, false), is(""));
        assertThat(namespaceRegistry.getNamespaceForPrefix(validPrefix1), is(validNamespaceUri1));
        assertThat(namespaceRegistry.getNamespaceForPrefix(validPrefix2), is(validNamespaceUri2));
        assertThat(namespaceRegistry.getNamespaceForPrefix(""), is(validNamespaceUri3));

        NamespaceRegistry newRegistry = new SimpleNamespaceRegistry();
        for (String uri : this.namespaceRegistry.getRegisteredNamespaceUris()) {
            String prefix = this.namespaceRegistry.getPrefixForNamespaceUri(uri, false);
            newRegistry.register(prefix, uri);
        }
        assertThat(newRegistry.isRegisteredNamespaceUri(validNamespaceUri1), is(true));
        assertThat(newRegistry.isRegisteredNamespaceUri(validNamespaceUri2), is(true));
        assertThat(newRegistry.isRegisteredNamespaceUri(validNamespaceUri3), is(true));
        assertThat(newRegistry.getPrefixForNamespaceUri(validNamespaceUri1, false), is(validPrefix1));
        assertThat(newRegistry.getPrefixForNamespaceUri(validNamespaceUri2, false), is(validPrefix2));
        assertThat(newRegistry.getPrefixForNamespaceUri(validNamespaceUri3, false), is(""));
        assertThat(newRegistry.getNamespaceForPrefix(validPrefix1), is(validNamespaceUri1));
        assertThat(newRegistry.getNamespaceForPrefix(validPrefix2), is(validNamespaceUri2));
        assertThat(newRegistry.getNamespaceForPrefix(""), is(validNamespaceUri3));

    }

}
