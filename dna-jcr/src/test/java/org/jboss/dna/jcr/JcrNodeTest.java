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
package org.jboss.dna.jcr;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.UUID;
import javax.jcr.ItemNotFoundException;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.jcr.SessionCache.NodeInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrNodeTest {

    private UUID uuid;
    private JcrNode node;
    private ExecutionContext context;
    @Mock
    private SessionCache cache;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);

        uuid = UUID.randomUUID();
        node = new JcrNode(cache, uuid);

        context = new ExecutionContext();
        stub(cache.context()).toReturn(context);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowAncestorDepthGreaterThanNodeDepth() throws Exception {
        NodeInfo info = mock(NodeInfo.class);
        stub(cache.findNodeInfo(uuid)).toReturn(info);
        stub(cache.getPathFor(info)).toReturn(path("/a/b/c/name[2]"));
        node.getAncestor(6);
    }

    @Test
    public void shouldProvideDepth() throws Exception {
        NodeInfo info = mock(NodeInfo.class);
        stub(cache.findNodeInfo(uuid)).toReturn(info);
        stub(cache.getPathFor(info)).toReturn(path("/a/b/c/name[2]"));
        assertThat(node.getDepth(), is(4));
    }

    @Test
    public void shouldProvideIndex() throws Exception {
        stub(cache.getSnsIndexOf(uuid)).toReturn(1);
        assertThat(node.getIndex(), is(1));
    }

    @Test
    public void shouldProvideName() throws Exception {
        stub(cache.getNameOf(uuid)).toReturn(name("name"));
        assertThat(node.getName(), is("name"));
    }

    @Test
    public void shouldProvidePath() throws Exception {
        stub(cache.getPathFor(uuid)).toReturn(path("/a/b/c/name[2]"));
        assertThat(node.getPath(), is("/a/b/c/name[2]"));
    }
}
