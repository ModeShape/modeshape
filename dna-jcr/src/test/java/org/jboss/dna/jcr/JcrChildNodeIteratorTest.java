/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.jboss.dna.graph.property.basic.BasicName;
import org.jboss.dna.graph.property.basic.BasicPathSegment;
import org.jboss.dna.jcr.cache.ChildNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

/**
 * 
 */
public class JcrChildNodeIteratorTest {

    private List<ChildNode> children;
    private List<Node> childNodes;
    private NodeIterator iter;
    @Mock
    private SessionCache cache;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        children = new ArrayList<ChildNode>();
        childNodes = new ArrayList<Node>();
        for (int i = 0; i != 10; ++i) {
            UUID uuid = UUID.randomUUID();
            ChildNode child = new ChildNode(uuid, new BasicPathSegment(new BasicName("", "name"), i + 1));
            AbstractJcrNode node = mock(AbstractJcrNode.class);
            stub(cache.findJcrNode(uuid)).toReturn(node);
            children.add(child);
            childNodes.add(node);
        }
        iter = new JcrChildNodeIterator(cache, children, children.size());
    }

    @Test
    public void shouldProperlyDetermineHasNext() {
        Iterator<Node> nodeIter = childNodes.iterator();
        long position = 0L;
        assertThat(iter.getPosition(), is(position));
        while (iter.hasNext()) {
            assertThat(nodeIter.hasNext(), is(true));
            Node actual = (Node)iter.next();
            Node expected = nodeIter.next();
            assertThat(iter.getPosition(), is(++position));
            assertThat(iter.getPosition(), is(position)); // call twice
            assertThat(actual, is(sameInstance(expected)));
        }
        assertThat(iter.hasNext(), is(false));
        assertThat(nodeIter.hasNext(), is(false));
    }

    @Test
    public void shouldStartWithPositionOfZero() {
        assertThat(iter.getPosition(), is(0L));
    }

    @Test
    public void shouldHaveCorrectSize() {
        assertThat(iter.getSize(), is((long)childNodes.size()));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRemove() {
        iter.remove();
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailWhenNextIsCalled() {
        while (iter.hasNext()) {
            iter.next();
        }
        iter.next();
    }

}
