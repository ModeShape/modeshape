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
package org.modeshape.jcr;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;

/**
 * Unit test for {@link javax.jcr.NamespaceRegistry}
 */
public class JcrNamespaceRegistryTest extends MultiUseAbstractTest {

    private NamespaceRegistry registry;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        registry = session.getWorkspace().getNamespaceRegistry();
    }

    @Override
    @After
    public void afterEach() throws Exception {
        // Unregister all of the namespaces ...
        for (String existingPrefix : registry.getPrefixes()) {
            try {
                registry.unregisterNamespace(existingPrefix);
                // Make sure the prefix is not in the list of built-ins ...
                assertThat(JcrNamespaceRegistry.STANDARD_BUILT_IN_PREFIXES.contains(existingPrefix), is(false));
            } catch (NamespaceException e) {
                // built in namespace ...
            }
        }
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

    @Test( expected = NamespaceException.class )
    public void shouldNotAllowRegisteringNamespaceWithPrefixThatIsNotValidName() throws Exception {
        registry.registerNamespace("foo:", "http://example.com");
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

    @Test
    @FixFor( "MODE-2141" )
    public void shouldNotAllowUnregisteringUsedNamespaces() throws Exception {
        String prefix = "admb";
        String uri = "http://www.admb.be/modeshape/admb/1.0";

        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

        // First create a namespace for the nodeType which is going to be added
        registry.registerNamespace(prefix, uri);
        assertThatNamespaceIsRegistered(prefix, uri);

        // Start creating a nodeTypeTemplate, keep it basic.
        NodeTypeTemplate nodeTypeTemplate = nodeTypeManager.createNodeTypeTemplate();
        String nodeTypeName = prefix + ":test";
        nodeTypeTemplate.setName(nodeTypeName);
        nodeTypeManager.registerNodeType(nodeTypeTemplate, false);

        try {
            registry.unregisterNamespace(prefix);
            fail("Should not allow the unregistration of a namespace used by a node type");
        } catch (NamespaceException e) {
            //expected
        }
        nodeTypeManager.unregisterNodeType(nodeTypeName);
        registry.unregisterNamespace(prefix);
        assertThatNamespacePrefixIsNotRegistered(prefix);
    }

    @Test
    @FixFor( "MODE-2142" )
    public void shouldNotAllowChangingThePrefixOfUsedNamespaces() throws Exception {
        String prefix = "admb";
        String uri = "http://www.admb.be/modeshape/admb/1.0";

        NodeTypeManager nodeTypeManager = session.getWorkspace().getNodeTypeManager();

        // First create a namespace for the nodeType which is going to be added
        registry.registerNamespace(prefix, uri);
        assertThatNamespaceIsRegistered(prefix, uri);

        // Start creating a nodeTypeTemplate, keep it basic.
        NodeTypeTemplate nodeTypeTemplate = nodeTypeManager.createNodeTypeTemplate();
        String nodeTypeName = prefix + ":test";
        nodeTypeTemplate.setName(nodeTypeName);
        nodeTypeManager.registerNodeType(nodeTypeTemplate, false);

        try {
            registry.registerNamespace("newPrefix", uri);
            fail("Should not allow changing the prefix of a namespace used by a node type");
        } catch (NamespaceException e) {
            //expected
        }
        nodeTypeManager.unregisterNodeType(nodeTypeName);

        registry.registerNamespace("newPrefix", uri);
        assertThatNamespacePrefixIsNotRegistered(prefix);
        assertThatNamespaceIsRegistered("newPrefix", uri);
    }

    @FixFor("MODE-2278")
    @Test(expected = NamespaceException.class)
    public void shouldNotAllowColonInPrefix() throws Exception {
        registry.registerNamespace("invalid", "http://invalid");
        //as per http://www.w3.org/TR/REC-xml-names, the prefix has to be a valid NCName
        registry.registerNamespace("invalid:prefix", "http://invalid/prefix");
    }
}
