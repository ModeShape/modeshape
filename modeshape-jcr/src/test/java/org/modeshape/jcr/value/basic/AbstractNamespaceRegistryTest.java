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
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.value.NamespaceRegistry;

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
