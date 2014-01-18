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
