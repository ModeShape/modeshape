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
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import javax.jcr.NamespaceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.ExecutionContext;

public class JcrNamespaceRegistryTest {

    private ExecutionContext executionContext;
    private JcrNamespaceRegistry registry;
    @Mock
    private JcrSession session;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        executionContext = new ExecutionContext();
        when(session.isLive()).thenReturn(true);
        registry = new JcrNamespaceRegistry(executionContext.getNamespaceRegistry(), session);
    }

    protected void assertThatNamespaceIsRegistered( String prefix,
                                                    String uri ) throws Exception {
        assertThat(registry.getURI(prefix), is(uri));
        assertThat(registry.getPrefix(uri), is(prefix));

        boolean foundPrefix = false;
        for (String existingPrefix : registry.getPrefixes()) {
            if (existingPrefix.equals(prefix)) foundPrefix = true;
        }
        assertThat(foundPrefix, is(true));

        boolean foundUri = false;
        for (String existingUri : registry.getURIs()) {
            if (existingUri.equals(uri)) foundUri = true;
        }
        assertThat(foundUri, is(true));
    }

    protected void assertThatNamespacePrefixIsNotRegistered( String prefix ) throws Exception {
        try {
            registry.getURI(prefix);
            fail("Should not have found namespace mapping with prefix \"" + prefix + "\"");
        } catch (NamespaceException e) {
            // good
        }
        for (String existingPrefix : registry.getPrefixes()) {
            assertThat(existingPrefix, is(not(prefix)));
        }
    }

    protected void assertThatNamespaceUriIsNotRegistered( String uri ) throws Exception {
        try {
            registry.getPrefix(uri);
            fail("Should not have found namespace mapping with URI \"" + uri + "\"");
        } catch (NamespaceException e) {
            // good
        }
        for (String existingUri : registry.getURIs()) {
            assertThat(existingUri, is(not(uri)));
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowRegisteringNullPrefix() throws Exception {
        registry.registerNamespace("foo", null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowRegisteringNullUri() throws Exception {
        registry.registerNamespace(null, "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringPrefixThatStartsWithLowercaseXml() throws Exception {
        registry.registerNamespace("xmlw", "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringPrefixThatStartsWithUppercaseXml() throws Exception {
        registry.registerNamespace("XMLw", "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringPrefixThatStartsWithMixedcaseXml() throws Exception {
        registry.registerNamespace("XmLw", "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringEmptyPrefix() throws Exception {
        registry.registerNamespace("", "http://www.jcp.org/jcr/1.0");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringEmptyUri() throws Exception {
        registry.registerNamespace("foo", "");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingJcrPrefix() throws Exception {
        registry.registerNamespace("jcr", "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingNtPrefix() throws Exception {
        registry.registerNamespace("nt", "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingMixPrefix() throws Exception {
        registry.registerNamespace("mix", "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingSvPrefix() throws Exception {
        registry.registerNamespace("sv", "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingXmlPrefix() throws Exception {
        registry.registerNamespace("xml", "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingDnaPrefix() throws Exception {
        registry.registerNamespace("mode", "http://example.com");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingJcrUri() throws Exception {
        registry.registerNamespace("foo", "http://www.jcp.org/jcr/1.0");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingNtUri() throws Exception {
        registry.registerNamespace("foo", "http://www.jcp.org/jcr/nt/1.0");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingMixUri() throws Exception {
        registry.registerNamespace("foo", "http://www.jcp.org/jcr/mix/1.0");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingSvUri() throws Exception {
        registry.registerNamespace("foo", "http://www.jcp.org/jcr/sv/1.0");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingXmlUri() throws Exception {
        registry.registerNamespace("foo", "http://www.w3.org/XML/1998/namespace");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingXmlnsUri() throws Exception {
        registry.registerNamespace("foo", "http://www.w3.org/2000/xmlns/");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringUsingModeUri() throws Exception {
        registry.registerNamespace("foo", "http://www.modeshape.org/1.0");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringPrefixThatIsNotValidXmlNCName() throws Exception {
        registry.registerNamespace("1foot&in<door", "http://example.com");
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowUnregisteringNullPrefix() throws Exception {
        registry.unregisterNamespace(null);
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteringBlankPrefix() throws Exception {
        registry.unregisterNamespace("");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteringJcrPrefix() throws Exception {
        registry.unregisterNamespace("jcr");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteringNtPrefix() throws Exception {
        registry.unregisterNamespace("nt");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteringMixPrefix() throws Exception {
        registry.unregisterNamespace("mix");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteringSvPrefix() throws Exception {
        registry.unregisterNamespace("sv");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteringXmlPrefix() throws Exception {
        registry.unregisterNamespace("xml");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteringModePrefix() throws Exception {
        registry.unregisterNamespace("mode");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteringPrefixThatIsNotUsed() throws Exception {
        String prefix = "bar";
        assertThatNamespacePrefixIsNotRegistered(prefix);
        registry.unregisterNamespace(prefix);
    }

    @Test
    public void shouldRegisterNewPrefixWithNewUri() throws Exception {
        String prefix = "foo";
        String uri = "http://example.com";
        assertThatNamespacePrefixIsNotRegistered(prefix);
        assertThatNamespaceUriIsNotRegistered(uri);
        registry.registerNamespace(prefix, uri);
        assertThatNamespaceIsRegistered(prefix, uri);
    }

    @Test
    public void shouldRegisterRemoveExistingMappingWhenUsingNewPrefixWithPreviouslyUsedUri() throws Exception {
        String prefix1 = "foo1";
        String prefix2 = "foo2";
        String uri = "http://example.com";
        assertThatNamespacePrefixIsNotRegistered(prefix1);
        assertThatNamespacePrefixIsNotRegistered(prefix2);
        assertThatNamespaceUriIsNotRegistered(uri);
        // Register the URI with the first prefix.
        registry.registerNamespace(prefix1, uri);
        assertThatNamespaceIsRegistered(prefix1, uri);
        // Register the same URI with a different prefix. This should remove the mapping with 'prefix1'
        registry.registerNamespace(prefix2, uri);
        assertThatNamespaceIsRegistered(prefix2, uri);
        assertThatNamespacePrefixIsNotRegistered(prefix1);
    }

    @Test
    public void shouldRegisterOverwriteExistingMappingWhenUsingPreviouslyUsedPrefixWithNewUri() throws Exception {
        String prefix = "foo1";
        String uri1 = "http://example.com";
        String uri2 = "http://acme.com";
        assertThatNamespacePrefixIsNotRegistered(prefix);
        assertThatNamespaceUriIsNotRegistered(uri1);
        assertThatNamespaceUriIsNotRegistered(uri2);
        // Register the first URI with the prefix.
        registry.registerNamespace(prefix, uri1);
        assertThatNamespaceIsRegistered(prefix, uri1);
        // Register the second URI with the same prefix. 'uri1' should no longer be used
        registry.registerNamespace(prefix, uri2);
        assertThatNamespaceIsRegistered(prefix, uri2);
        assertThatNamespaceUriIsNotRegistered(uri1);
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteredPrefix() throws Exception {
        registry.getURI("bogus");
    }

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowUnregisteredUri() throws Exception {
        registry.getPrefix("bogus");
    }
}
