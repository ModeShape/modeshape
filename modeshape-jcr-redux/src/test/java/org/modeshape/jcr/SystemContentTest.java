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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.nodetype.NodeTypeDefinition;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
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
        Set<Name> builtInNodeTypes = new HashSet<Name>(repository.nodeTypeManager().getAllNodeTypeNames());
        for (NodeTypeDefinition type : system.readAllNodeTypes()) {
            Name name = name(type.getName());
            JcrNodeType actual = repository.nodeTypeManager().getNodeType(name);
            assertThat("Did not find actual node type for name \"" + type.getName() + "\"", actual, is(notNullValue()));
            assertThat(builtInNodeTypes.remove(name), is(true));
        }
        assertThat(builtInNodeTypes.isEmpty(), is(true));
    }

    @Test
    public void shouldStoreNodeTypeDefinitionsInSystemCatalog() {
        Collection<JcrNodeType> nodeTypes = repository.nodeTypeManager().getAllNodeTypes();
        for (int i = 0; i != 3; ++i) {
            system.store(nodeTypes, true);
            assertThat(repository.nodeTypeManager().refreshFromSystem(), is(true));
        }
    }
}
