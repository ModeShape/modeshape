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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.stub;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path.Segment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * @author jverhaeg
 */
public class JcrNodeTest {

    private JcrNode node;
    private Node root;
    @Mock
    private JcrSession session;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        root = new JcrRootNode(session);
        Segment segment = Mockito.mock(Segment.class);
        Name name = Mockito.mock(Name.class);
        stub(name.getString((NamespaceRegistry)anyObject())).toReturn("name");
        stub(segment.getName()).toReturn(name);
        stub(segment.getIndex()).toReturn(2);
        UUID uuid = UUID.randomUUID();
        node = new JcrNode(session, uuid, segment);
        ExecutionContext context = Mockito.mock(ExecutionContext.class);
        stub(session.getExecutionContext()).toReturn(context);
        stub(session.getNode(uuid)).toReturn(root);
        node.setProperties(new HashMap<Name, Property>());
        node.setChildren(new ArrayList<Segment>());
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowAncestorDepthGreaterThanNodeDepth() throws Exception {
        node.getAncestor(2);
    }

    @Test
    public void shouldProvideDepth() throws Exception {
        assertThat(node.getDepth(), is(1));
    }

    @Test
    public void shouldProvideIndex() throws Exception {
        assertThat(node.getIndex(), is(2));
    }

    @Test
    public void shouldProvideName() throws Exception {
        assertThat(node.getName(), is("name"));
    }

    @Test
    public void shouldProvidePath() throws Exception {
        assertThat(node.getPath(), is("/name[2]"));
    }
}
