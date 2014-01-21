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
import static org.junit.Assert.assertThat;
import java.util.NoSuchElementException;
import javax.jcr.NodeIterator;
import org.junit.Before;
import org.junit.Test;

public class JcrEmptyNodeIteratorTest {

    private NodeIterator iter;

    @Before
    public void beforeEach() {
        iter = JcrEmptyNodeIterator.INSTANCE;
    }

    @Test
    public void shouldNotHaveNext() {
        assertThat(iter.hasNext(), is(false));
    }

    @Test
    public void shouldHavePositionOfZero() {
        assertThat(iter.getPosition(), is(0L));
    }

    @Test
    public void shouldHaveSizeOfZero() {
        assertThat(iter.getSize(), is(0L));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowRemove() {
        iter.remove();
    }

    @Test( expected = NoSuchElementException.class )
    public void shouldFailWhenNextIsCalled() {
        iter.next();
    }

}
