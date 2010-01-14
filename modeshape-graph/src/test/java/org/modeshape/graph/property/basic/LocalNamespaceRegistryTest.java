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
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.graph.property.NamespaceRegistry;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class LocalNamespaceRegistryTest {

    private NamespaceRegistry local;
    private NamespaceRegistry delegate;
    private String uri;

    @Before
    public void beforeEach() {
        delegate = new SimpleNamespaceRegistry();
        local = new LocalNamespaceRegistry(delegate);
        uri = "http://www.example.com";
    }

    @Test
    public void shouldMirrorDelegateIfLocalIsEmpty() {
        assertNamespaces(delegate, "");
        assertNamespaces(local, "");
    }

    @Test
    public void shouldAllowLocalRegistryToChangeDefaultNamespace() {
        local.register("", uri);
        assertNamespaces(delegate, "");
        assertNamespaces(local, "=" + uri);
    }

    protected void assertNamespaces( NamespaceRegistry registry,
                                     String... namespaces ) {
        // Create the list of expected namespaces ...
        Map<String, String> expected = new HashMap<String, String>();
        for (String str : namespaces) {
            String[] parts = str.length() != 0 ? str.split("=") : new String[] {"", ""};
            assertThat("invalid namespace string \"" + str + "\"", parts.length, is(2));
            expected.put(parts[0], parts[1]);
        }
        // Now compare the actual to expected ...
        for (NamespaceRegistry.Namespace actual : registry.getNamespaces()) {
            String expectedUri = expected.remove(actual.getPrefix());
            assertThat("namespace URIs differ", actual.getNamespaceUri(), is(expectedUri));
            String prefix = actual.getPrefix();
            String uri = actual.getNamespaceUri();
            assertThat(registry.isRegisteredNamespaceUri(uri), is(true));
            assertThat(registry.getPrefixForNamespaceUri(uri, false), is(prefix));
            assertThat(registry.getNamespaceForPrefix(prefix), is(uri));
        }
        // Check that there are no more expected ...
        if (!expected.isEmpty()) {
            StringBuilder msg = new StringBuilder("actual is missing namespaces ");
            boolean first = true;
            for (Map.Entry<String, String> entry : expected.entrySet()) {
                if (first) first = false;
                else msg.append(", ");
                msg.append(entry.getKey()).append("=").append(entry.getValue());
            }
            assertThat(msg.toString(), registry.getNamespaces().isEmpty(), is(true));
        }
    }
}
