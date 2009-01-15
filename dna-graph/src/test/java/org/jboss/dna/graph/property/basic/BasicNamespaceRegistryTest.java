/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph.property.basic;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import org.jboss.dna.graph.property.basic.BasicNamespaceRegistry;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class BasicNamespaceRegistryTest {

    private String validNamespaceUri1;
    private String validNamespaceUri2;
    private String validNamespaceUri3;
    private String validPrefix1;
    private String validPrefix2;
    private BasicNamespaceRegistry namespaceRegistry;

    @Before
    public void setUp() {
        namespaceRegistry = new BasicNamespaceRegistry();
        validNamespaceUri1 = "http://www.jboss.org/dna/2";
        validNamespaceUri2 = "http://acme.com/something";
        validNamespaceUri3 = "http://www.redhat.com";
        validPrefix1 = "dna";
        validPrefix2 = "acme";
    }

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
    public void shouldGeneratePrefixIfNamespaceUriIsNotRegistered() {
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri3, false), is(nullValue()));
        // Now get the generated prefix ...
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri3, true), is("ns001"));
        // Change the template ...
        namespaceRegistry.setGeneratedPrefixTemplate("xyz0000abc");
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri2, true), is("xyz0002abc"));
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri2, true), is("xyz0002abc"));
        // Change the template again ...
        namespaceRegistry.setGeneratedPrefixTemplate("xyz####abc");
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri1, true), is("xyz3abc"));
        assertThat(namespaceRegistry.getPrefixForNamespaceUri(validNamespaceUri1, true), is("xyz3abc"));
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

        BasicNamespaceRegistry newRegistry = new BasicNamespaceRegistry();
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
