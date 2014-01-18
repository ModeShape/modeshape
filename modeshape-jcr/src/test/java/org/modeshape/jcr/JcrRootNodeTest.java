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
import static org.junit.Assert.assertThat;
import javax.jcr.ItemNotFoundException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author jverhaeg
 */
public class JcrRootNodeTest extends MultiUseAbstractTest {

    private AbstractJcrNode rootNode;

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        rootNode = session.getRootNode();
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldNotAllowAncestorDepthGreaterThanNodeDepth() throws Exception {
        rootNode.getAncestor(1);
    }

    @Test
    public void shouldHaveZeroDepth() throws Exception {
        assertThat(rootNode.getDepth(), is(0));
    }

    @Test
    public void shouldIndicateIndexIsOne() throws Exception {
        assertThat(rootNode.getIndex(), is(1));
    }

    @Test
    public void shouldHaveEmptyName() throws Exception {
        String name = rootNode.getName();
        assertThat(name, notNullValue());
        assertThat(name.length(), is(0));
    }

    @Test( expected = ItemNotFoundException.class )
    public void shouldHaveNoParent() throws Exception {
        rootNode.getParent();
    }

    @Test
    public void shouldProvidePath() throws Exception {
        assertThat(rootNode.getPath(), is("/"));
    }
}
