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
package org.jboss.dna.jcr.cache;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ChangedChildrenTest extends AbstractChildrenTest<ChangedChildren> {

    private ImmutableChildren original;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();

        original = new ImmutableChildren(parentUuid);
        firstChild = original.add(firstChildName, firstChildUuid, pathFactory);

        children = new ChangedChildren(original);
    }

    @Test
    public void shouldHaveCorrectSize() {
        assertThat(children.size(), is(1));
    }

    @Test
    public void shouldHaveSameContentsAsOriginal() {
        assertSameContent(children, original);
    }

    @Test
    public void shouldFindChildrenByName() {
        Iterator<ChildNode> iter = children.getChildren(firstChildName);
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(sameInstance(firstChild)));
        assertThat(iter.hasNext(), is(false));
        try {
            iter.next();
            fail("Failed to throw exception");
        } catch (NoSuchElementException e) {
            // expected
        }
    }

    @Test
    public void shouldReturnSameInstanceFromWithoutIfSuppliedChildIsNotFound() {
        ChildNode nonExistant = new ChildNode(UUID.randomUUID(), pathFactory.createSegment("some segment"));
        assertThat(children.without(nonExistant.getUuid(), pathFactory), is(sameInstance((Children)children)));
    }

    @Test
    public void shouldReturnEmptyChildrenFromWithoutIfOnlyChildIsRemoved() {
        Children newChildren = children.without(firstChild.getUuid(), pathFactory);
        assertThat(newChildren.size(), is(0));
    }

    @Test
    public void shouldReturnSameInstanceFromWithIfSuppliedChildThatIsFoundInContainer() {
        assertThat(children.with(firstChildName, firstChildUuid, pathFactory), is(sameInstance((Children)children)));
    }

    @Test( expected = UnsupportedOperationException.class )
    public void shouldReturnIteratorThatDoesNotSupportRemoving() {
        Iterator<ChildNode> iter = children.getChildren(firstChildName);
        assertThat(iter.hasNext(), is(true));
        assertThat(iter.next(), is(sameInstance(firstChild)));
        iter.remove();
    }

    @Test
    public void shouldReturnChangedChildrenFromWithoutIfSuppliedChildIsFound() {
        ChildNode child2 = children.add(name("childB"), UUID.randomUUID(), pathFactory);
        ChildNode child3 = children.add(name("childC"), UUID.randomUUID(), pathFactory);
        ChildNode child4 = children.add(name("childA"), UUID.randomUUID(), pathFactory);
        ChildNode child5 = children.add(name("childA"), UUID.randomUUID(), pathFactory);
        ChildNode child6 = children.add(name("childD"), UUID.randomUUID(), pathFactory);

        // Check that the children contains what we expect ...
        assertChildNodes(children, firstChild, child2, child3, child4, child5, child6);
        assertChildNodesWithName(children, "childA", firstChild, child4, child5);

        // Remove 'child4' ...
        Children result = children.without(child4.getUuid(), pathFactory);

        // but the result should not have child4 ...
        assertChildNodesWithName(result, "childA", firstChild, "childA");

        // Now check that all the child nodes are in the result, in the expected order ...
        assertChildNodes(result, firstChild, child2, child3, "childA", child6);
    }

    @Test
    public void shouldReturnChangedChildrenFromWithIfSuppliedChildIsNotFound() {
        // Make sure that children contains what we expect ...
        assertChildNodes(children, firstChild);
        assertChildNodesWithName(children, "childA", firstChild);

        // Add a node ...
        Children result = children.with(name("childB"), UUID.randomUUID(), pathFactory);
        assertThat(result, is(sameInstance((Children)children)));
        assertChildNodes(children, firstChild, "childB");
        assertChildNodesWithName(children, "childA", firstChild);

        // Add another node ...
        result = children.with(name("childC"), UUID.randomUUID(), pathFactory);
        assertThat(result, is(sameInstance((Children)children)));
        assertChildNodes(children, firstChild, "childB", "childC");
        assertChildNodesWithName(children, "childA", firstChild);

        // Add another node ...
        result = children.with(name("childA"), UUID.randomUUID(), pathFactory);
        assertThat(result, is(sameInstance((Children)children)));
        assertChildNodes(children, firstChild, "childB", "childC", "childA");
        assertChildNodesWithName(children, "childA", firstChild, "childA");
    }

}
