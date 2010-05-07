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
package org.modeshape.connector.infinispan;

import javax.naming.Context;
import org.infinispan.manager.CacheManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;

/**
 * 
 */
public class PersistentInfinispanConnectorTest {

    protected static final String JNDI_NAME = "java/MyCacheManager";

    private ExecutionContext context;
    private InfinispanSource source;
    private CacheManager cacheManager;
    private Context mockJndi;
    private Graph graph;

    @Before
    public void beforeEach() throws Exception {
        context = new ExecutionContext();

        // // Create the cache manager ...
        // cacheManager = new DefaultCacheManager("infinispan_persistent_config.xml"); // looks on classpath first
        // // Set up the mock JNDI ...
        // mockJndi = mock(Context.class);
        // when(mockJndi.lookup(anyString())).thenReturn(null);
        // when(mockJndi.lookup(JNDI_NAME)).thenReturn(cacheManager);
        //
        // String[] predefinedWorkspaceNames = new String[] {"default"};
        // source = new InfinispanSource();
        // source.setName("Test Repository");
        // source.setPredefinedWorkspaceNames(predefinedWorkspaceNames);
        // source.setDefaultWorkspaceName(predefinedWorkspaceNames[0]);
        // source.setCreatingWorkspacesAllowed(true);
        // source.setContext(mockJndi);
        // source.setCacheManagerJndiName(JNDI_NAME);
        //
        // graph = Graph.create(source, context);
        // graph.useWorkspace("default");
    }

    @After
    public void afterEach() throws Exception {
        // try {
        // cacheManager.stop();
        // } finally {
        // // Delete all of the content stored on the file system ...
        // File store = new File("target/infinispan/jcr");
        // FileUtil.delete(store);
        // }
    }

    @Test
    public void placeholder() {

    }

    // @Test
    // public void shouldStartUp() {
    // assertThat(graph.getNodeAt("/"), is(notNullValue()));
    // }
    //
    // @Test
    // public void shouldAllowCreatingAndReReadingNodes() {
    // graph.create("/a").with("prop1", "value1").and();
    //
    // }
}
