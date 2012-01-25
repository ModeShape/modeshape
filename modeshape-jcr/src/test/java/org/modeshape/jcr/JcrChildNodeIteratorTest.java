/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.modeshape.jcr.JcrChildNodeIterator.NodeResolver;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.basic.BasicName;

/**
 * 
 */
public class JcrChildNodeIteratorTest {

    private Map<ChildReference, AbstractJcrNode> childNodesByRef;
    private List<AbstractJcrNode> children;
    private List<ChildReference> refs;
    private NodeIterator iter;
    private NodeKey keyTemplate;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        keyTemplate = new NodeKey("source1", "workspa", "1");
        children = new ArrayList<AbstractJcrNode>();
        refs = new ArrayList<ChildReference>();
        childNodesByRef = new HashMap<ChildReference, AbstractJcrNode>();
        for (int i = 0; i != 10; ++i) {
            // Create a child reference ...
            String name = "node" + (i + 1);
            NodeKey key = keyTemplate.withId(name);
            ChildReference ref = new ChildReference(key, new BasicName("http://foo", name), 1);
            refs.add(ref);
            // Create a mock child node ...
            AbstractJcrNode childJcrNode = mock(AbstractJcrNode.class);
            children.add(childJcrNode);
            childNodesByRef.put(ref, childJcrNode);
        }
        NodeResolver resolver = new NodeResolver() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public Node nodeFrom( ChildReference ref ) {
                return childNodesByRef.get(ref);
            }
        };
        iter = new JcrChildNodeIterator(resolver, refs.iterator());
    }

    @Test
    public void shouldProperlyDetermineHasNext() {
        Iterator<AbstractJcrNode> nodeIter = children.iterator();
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
        assertThat(iter.getSize(), is((long)children.size()));
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
