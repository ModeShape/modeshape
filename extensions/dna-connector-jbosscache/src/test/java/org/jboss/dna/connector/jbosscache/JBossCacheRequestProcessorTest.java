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
package org.jboss.dna.connector.jbosscache;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.HashSet;
import java.util.Set;
import javax.naming.Context;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jboss.cache.Fqn;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author Randall Hauch
 */
public class JBossCacheRequestProcessorTest {

    private JBossCacheRequestProcessor processor;
    private JBossCacheWorkspaces workspaces;
    private Set<String> initialWorkspaceNames;
    private String defaultWorkspaceName;
    private String defaultConfigName;
    private CacheFactory<Name, Object> cacheFactory;
    private ExecutionContext context;
    private PathFactory pathFactory;
    @Mock
    private Context jndi;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        context = new ExecutionContext();
        context.getNamespaceRegistry().register(DnaLexicon.Namespace.PREFIX, DnaLexicon.Namespace.URI);
        context.getNamespaceRegistry().register(JBossCacheLexicon.Namespace.PREFIX, JBossCacheLexicon.Namespace.URI);

        cacheFactory = new DefaultCacheFactory<Name, Object>();
        defaultConfigName = null;
        initialWorkspaceNames = new HashSet<String>();
        initialWorkspaceNames.add("workspace1");
        initialWorkspaceNames.add("workspace2");
        defaultWorkspaceName = initialWorkspaceNames.iterator().next();
        workspaces = new JBossCacheWorkspaces("source", cacheFactory, defaultConfigName, initialWorkspaceNames, jndi);
        processor = new JBossCacheRequestProcessor("source", context, null, workspaces, defaultWorkspaceName, true);

        pathFactory = context.getValueFactories().getPathFactory();
    }

    @Test
    public void shouldCreateFullyQualifiedNodeOfPathSegmentsFromPath() {
        Path path = pathFactory.create("/a/b/c/d");
        Fqn<?> fqn = processor.getFullyQualifiedName(path);
        assertThat(fqn.size(), is(4));
        assertThat(fqn.isRoot(), is(false));
        for (int i = 0; i != path.size(); ++i) {
            assertThat((Path.Segment)fqn.get(i), is(path.getSegment(i)));
        }
    }

    @Test
    public void shouldCreateFullyQualifiedNodeOfPathSegmentsFromRootPath() {
        Path path = pathFactory.createRootPath();
        Fqn<?> fqn = processor.getFullyQualifiedName(path);
        assertThat(fqn.size(), is(0));
        assertThat(fqn.isRoot(), is(true));
    }

    @Test
    public void shouldCreateFullyQualifiedNodeFromPathSegment() {
        Path.Segment segment = pathFactory.createSegment("a");
        Fqn<?> fqn = processor.getFullyQualifiedName(segment);
        assertThat(fqn.size(), is(1));
        assertThat(fqn.isRoot(), is(false));
        assertThat((Path.Segment)fqn.get(0), is(segment));
    }

    @Test
    public void shouldCreatePathFromFullyQualifiedNode() {
        Path path = pathFactory.create("/a/b/c/d");
        Fqn<Path.Segment> fqn = processor.getFullyQualifiedName(path);
        assertThat(processor.getPath(pathFactory, fqn), is(path));
    }

    @Test
    public void shouldCreateRootPathFromRootFullyQualifiedNode() {
        Path path = pathFactory.createRootPath();
        Fqn<Path.Segment> fqn = processor.getFullyQualifiedName(path);
        assertThat(processor.getPath(pathFactory, fqn), is(path));
    }
}
