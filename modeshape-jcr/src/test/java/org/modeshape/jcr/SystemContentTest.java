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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.nodetype.NodeTypeDefinition;
import org.junit.After;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;

public class SystemContentTest {

    private RepositoryConfiguration config;
    private JcrRepository repository;
    private SystemContent system;

    @Before
    public void beforeEach() throws Exception {
        config = new RepositoryConfiguration("repoName");
        repository = new JcrRepository(config);
        repository.start();
        SessionCache systemCache = repository.createSystemSession(repository.runningState().context(), false);
        system = new SystemContent(systemCache);
    }

    @After
    public void afterEach() throws Exception {
        try {
            repository.shutdown().get(3L, TimeUnit.SECONDS);
        } finally {
            repository = null;
            config = null;
        }
    }

    private final Name name( String name ) {
        return repository.runningState().context().getValueFactories().getNameFactory().create(name);
    }

    @Test
    public void shouldReadNodeTypeDefinitionsFromSystemCatalog() {
        NodeTypes nodeTypes = repository.nodeTypeManager().getNodeTypes();
        Set<Name> builtInNodeTypes = new HashSet<Name>(nodeTypes.getAllNodeTypeNames());
        for (NodeTypeDefinition type : system.readAllNodeTypes()) {
            Name name = name(type.getName());
            JcrNodeType actual = nodeTypes.getNodeType(name);
            assertThat("Did not find actual node type for name \"" + type.getName() + "\"", actual, is(notNullValue()));
            assertThat(builtInNodeTypes.remove(name), is(true));
        }
        assertThat(builtInNodeTypes.isEmpty(), is(true));
    }

    @Test
    public void shouldStoreNodeTypeDefinitionsInSystemCatalog() {
        Collection<JcrNodeType> nodeTypes = repository.nodeTypeManager().getNodeTypes().getAllNodeTypes();
        for (int i = 0; i != 3; ++i) {
            system.store(nodeTypes, true);
            assertThat(repository.nodeTypeManager().refreshFromSystem(), is(true));
        }
    }

    @Test
    @FixFor("MODE-1408")
    public void shouldRegisterNewNamespace() {
        Map<String, String> urisByPrefix = new HashMap<String, String>();
        String uri = "http://foo.bar";
        String prefix = "foobar";
        urisByPrefix.put(prefix, uri);
        system.registerNamespaces(urisByPrefix);
        assertEquals(prefix, system.readNamespacePrefix(uri, false));
    }

    @Test
    public void shouldUnregisterNamespace() {
        Map<String, String> urisByPrefix = new HashMap<String, String>();
        String uri = "http://foo.bar";
        String prefix = "foobar";
        urisByPrefix.put(prefix, uri);

        system.registerNamespaces(urisByPrefix);
        assertTrue(system.unregisterNamespace(uri));
        assertNull(system.readNamespacePrefix(uri, false));
    }
}
