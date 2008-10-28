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
package org.jboss.dna.graph.properties.basic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;
import org.jboss.dna.common.text.Jsr283Encoder;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 * @author John Verhaeg
 */
public class BasicNameTest {

    private BasicNamespaceRegistry namespaceRegistry;
    private Name name;
    private String validNamespaceUri;
    private String validLocalName;
    private TextEncoder encoder;
    private TextEncoder delimiterEncoder;
    private String validNamespacePrefix;

    @Before
    public void beforeEach() {
        this.validNamespacePrefix = DnaLexicon.Namespace.PREFIX;
        this.validNamespaceUri = DnaLexicon.Namespace.URI;
        this.validLocalName = "localPart";
        this.name = new BasicName(validNamespaceUri, validLocalName);
        this.encoder = Path.URL_ENCODER;
        this.namespaceRegistry = new BasicNamespaceRegistry();
        this.namespaceRegistry.register(validNamespacePrefix, validNamespaceUri);
        this.delimiterEncoder = new TextEncoder() {
            public String encode( String text ) {
                if (":".equals(text)) return "\\:";
                if ("{".equals(text)) return "\\{";
                if ("}".equals(text)) return "\\}";
                return text;
            }
        };
    }

    @Test
    public void shouldAllowNullNamespaceUriInConstructorAndConvertToEmptyString() {
        name = new BasicName(null, validLocalName);
        assertThat(name.getNamespaceUri(), is(""));
    }

    @Test
    public void shouldAllowEmptyNamespaceUriInConstructor() {
        name = new BasicName("", validLocalName);
        assertThat(name.getNamespaceUri(), is(""));
    }

    @Test
    public void shouldTrimNamespaceUriInConstructor() {
        name = new BasicName("  " + validNamespaceUri + "\t \t", validLocalName);
        assertThat(name.getNamespaceUri(), is(validNamespaceUri.trim()));

        name = new BasicName("  \t  \t", validLocalName);
        assertThat(name.getNamespaceUri(), is(""));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowNullLocalNameInConstructor() {
        new BasicName(validNamespaceUri, null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldAllowEmptyLocalNameInConstructor() {
        new BasicName(validNamespaceUri, "");
    }

    @Test
    public void shouldAcceptLocalNameWithColon() {
        validLocalName = "some:name:with:colons";
        name = new BasicName(validNamespaceUri, validLocalName);
        assertThat(name.getLocalName(), is(validLocalName));
    }

    @Test
    public void shouldReturnSameHashCodeForNamesWithSameNamespaceUriAndLocalPart() {
        Name other = new BasicName(name.getNamespaceUri(), name.getLocalName());
        assertThat(name.hashCode(), is(other.hashCode()));
    }

    @Test
    public void shouldConsiderNamesEqualIfTheyHaveTheSameNamespaceUriAndLocalPart() {
        Name other = new BasicName(name.getNamespaceUri(), name.getLocalName());
        assertThat(name.equals(other), is(true));
        assertThat(name.compareTo(other), is(0));
    }

    @Test
    public void shouldConsiderSameInstanceEqualToItself() {
        assertThat(name.equals(name), is(true));
        assertThat(name.compareTo(name), is(0));
    }

    @Test( expected = NullPointerException.class )
    public void shouldNotSupportNullInCompareTo() {
        name.compareTo(null);
    }

    @Test
    public void shouldSupportNullInEquals() {
        assertThat(name.equals(null), is(false));
    }

    @Test
    public void shouldUseFullNamespaceUriInResultFromGetStringWithoutNamespaceRegistry() {
        String encodedNamespaceUri = Path.DEFAULT_ENCODER.encode(validNamespaceUri);
        String encodedLocalName = Path.DEFAULT_ENCODER.encode(validLocalName);
        String result = name.getString();
        assertThat(result, containsString(encodedNamespaceUri));
        assertThat(result, containsString(encodedLocalName));
        assertThat(result, is("{" + encodedNamespaceUri + "}" + encodedLocalName));
    }

    @Test
    public void shouldEncodeColonInLocalNameAndNamespaceUriInResultFromGetStringWithoutNamespaceRegistry() {
        validLocalName = "some:name:with:colons";
        name = new BasicName(validNamespaceUri, validLocalName);
        String encodedNamespaceUri = encoder.encode(validNamespaceUri);
        String encodedLocalName = encoder.encode(validLocalName);
        String result = name.getString(encoder);
        assertThat(result, is("{" + encodedNamespaceUri + "}" + encodedLocalName));
        assertThat(encodedNamespaceUri, is("http%3a%2f%2fwww.jboss.org%2fdna"));
        assertThat(encodedLocalName, is("some%3aname%3awith%3acolons"));
    }

    @Test
    public void shouldUseNamespacePrefixInResultFromGetStringWithNamespaceRegistry() {
        String result = name.getString(namespaceRegistry, encoder);
        assertThat(result, is("dna:" + validLocalName));

        validLocalName = "some:name:with:colons";
        name = new BasicName(validNamespaceUri, validLocalName);
        result = name.getString(namespaceRegistry, encoder);
        assertThat(result, is("dna:some%3aname%3awith%3acolons"));
    }

    @Test
    public void shouldNotIncludeNamespacePrefixOrColonInResultFromGetStringWithNamespaceRegistry() {
        validNamespaceUri = namespaceRegistry.getDefaultNamespaceUri();
        name = new BasicName(validNamespaceUri, validLocalName);
        String result = name.getString(namespaceRegistry, encoder);
        assertThat(result, is(validLocalName));
        result = name.getString(namespaceRegistry); // default encoder
        assertThat(result, is(validLocalName));

        validLocalName = "some:name:with:colons";
        name = new BasicName(validNamespaceUri, validLocalName);
        result = name.getString(namespaceRegistry, encoder);
        assertThat(result, is("some:name:with:colons"));
    }

    @Test
    public void shouldUseDelimiterEncoderToEncodeDelimiterBetweenPrefixAndLocalPart() {
        encoder = new Jsr283Encoder();
        name = new BasicName(DnaLexicon.Namespace.URI, "some:name:with:colons");
        assertThat(name.getString(namespaceRegistry, encoder, delimiterEncoder), is("dna\\:some\uf03aname\uf03awith\uf03acolons"));
        assertThat(name.getString(null, encoder, delimiterEncoder), is("\\{" + encoder.encode(DnaLexicon.Namespace.URI)
                                                                       + "\\}some\uf03aname\uf03awith\uf03acolons"));
    }
}
