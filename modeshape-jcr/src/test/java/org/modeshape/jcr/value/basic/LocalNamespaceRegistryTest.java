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
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.modeshape.jcr.value.NamespaceRegistry;
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
