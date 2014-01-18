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
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * @author jverhaeg
 */
public class JcrPropertyIteratorTest {

    private List<Property> properties;
    private PropertyIterator iter;

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        properties = new ArrayList<Property>();
        properties.add(Mockito.mock(Property.class));
        properties.add(Mockito.mock(Property.class));
        properties.add(Mockito.mock(Property.class));
        properties.add(Mockito.mock(Property.class));
        iter = new JcrPropertyIterator(properties);
    }

    @Test
    public void shouldProvidePropertyIterator() throws Exception {
        assertThat(iter, notNullValue());
        assertThat(iter.getSize(), is(4L));
        assertThat(iter.getPosition(), is(0L));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.nextProperty(), is(sameInstance(properties.get(0))));
        assertThat(iter.getPosition(), is(1L));
        assertThat(iter.hasNext(), is(true));
        iter.skip(2);
        assertThat(iter.getPosition(), is(3L));
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.nextProperty(), is(sameInstance(properties.get(3))));
        assertThat(iter.getPosition(), is(4L));
        assertThat(iter.hasNext(), is(false));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldNotAllowPropertyIteratorRemove() throws Exception {
        new JcrPropertyIterator(properties).remove();
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowPropertyIteratorNegativeSkip() throws Exception {
        new JcrPropertyIterator(properties).skip(-1);
    }

    @Test
    public void shouldAllowPropertyIteratorPositiveSkip() throws Exception {
        JcrPropertyIterator iter = new JcrPropertyIterator(properties);
        iter.skip(3);
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.nextProperty(), is(properties.get(3)));
        assertThat(iter.hasNext(), is(false));
    }
}
