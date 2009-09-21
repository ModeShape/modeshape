/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.search;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import org.jboss.dna.common.text.SecureHashTextEncoder;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.util.SecureHash.Algorithm;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.NamespaceRegistry.Namespace;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class EncodingNamespaceRegistryTest {

    private ExecutionContext context;
    private NamespaceRegistry registry;
    private EncodingNamespaceRegistry encodedRegistry;
    private TextEncoder encoder;
    private ExecutionContext encodedContext;

    @Before
    public void beforeEach() {
        this.context = new ExecutionContext();
        this.registry = this.context.getNamespaceRegistry();
        this.encoder = new SecureHashTextEncoder(Algorithm.SHA_1, 10);
        this.encodedRegistry = new EncodingNamespaceRegistry(registry, encoder);
        this.encodedContext = context.with(encodedRegistry);
    }

    @Test
    public void shouldHaveEncodedPrefixesForAllRegisteredNamespacesExceptFixedOnes() {
        Collection<Namespace> namespaces = registry.getNamespaces();
        assertThat(namespaces.size() > 4, is(true));
        for (Namespace namespace : namespaces) {
            String uri = namespace.getNamespaceUri();
            String actualEncodedPrefix = encodedRegistry.getPrefixForNamespaceUri(uri, false);
            if (encodedRegistry.getFixedNamespaceUris().contains(uri)) {
                assertThat(actualEncodedPrefix, is(namespace.getPrefix()));
            } else {
                String expectedEncodedPrefix = encoder.encode(uri);
                assertThat(expectedEncodedPrefix, is(actualEncodedPrefix));
            }
            String actualUri = encodedRegistry.getNamespaceForPrefix(actualEncodedPrefix);
            assertThat(uri, is(actualUri));
        }
    }

    @Test
    public void shouldAllowPathConversionToAndFromString() {
        String uri1 = "http://acme.com/wabbler";
        String uri2 = "http://troublemakers.com/contixity";
        String uri3 = "http://example.com/infinitiy";
        String ns1 = "wab";
        String ns2 = "ctx";
        String ns3 = "inf";
        registry.register(ns1, uri1);
        registry.register(ns2, uri2);
        registry.register(ns3, uri3);
        String pathStr = "/wab:part1/wab:part2/ctx:part3/inf:part4/dna:part5";
        Path actualPath = context.getValueFactories().getPathFactory().create(pathStr);
        String actualPathStr = context.getValueFactories().getStringFactory().create(actualPath);
        assertThat(pathStr, is(actualPathStr));
        String encodedPathStr = encodedContext.getValueFactories().getStringFactory().create(actualPath);
        String encodedPrefix1 = encoder.encode(uri1);
        String encodedPrefix2 = encoder.encode(uri2);
        String encodedPrefix3 = encoder.encode(uri3);
        String expectedPathStr = "/" + encodedPrefix1 + ":part1/" + encodedPrefix1 + ":part2/" + encodedPrefix2 + ":part3/"
                                 + encodedPrefix3 + ":part4/dna:part5";
        assertThat(expectedPathStr, is(encodedPathStr));
        Path actualPath2 = encodedContext.getValueFactories().getPathFactory().create(encodedPathStr);
        assertThat(actualPath, is(actualPath2));
    }
}
