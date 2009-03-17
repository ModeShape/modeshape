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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Path.Segment;
import org.junit.Before;
import org.junit.Test;

/**
 * @param <ChildrenType> the type for the {@link #children} variable
 */
public abstract class AbstractChildrenTest<ChildrenType extends Children> {

    protected ChildrenType children;
    protected ExecutionContext context;
    protected PathFactory pathFactory;
    protected NameFactory nameFactory;
    protected UUID parentUuid;
    protected UUID firstChildUuid;
    protected Name firstChildName;
    protected ChildNode firstChild;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        pathFactory = context.getValueFactories().getPathFactory();
        nameFactory = context.getValueFactories().getNameFactory();

        parentUuid = UUID.randomUUID();
        firstChildUuid = UUID.randomUUID();
        firstChildName = nameFactory.create("childA");
    }

    protected Name name( String name ) {
        return nameFactory.create(name);
    }

    protected void assertChildNodesWithName( Children children,
                                             String childName,
                                             Object... childNodes ) {
        Name name = name(childName);
        Iterator<ChildNode> iter = children.getChildren(name);
        int expectedSnsIndex = 1;
        for (Object expectedChild : childNodes) {
            assertThat(iter.hasNext(), is(true));
            ChildNode next = iter.next();
            assertThat(next.getName(), is(name));
            assertThat(next.getSnsIndex(), is(expectedSnsIndex++));
            if (expectedChild instanceof ChildNode) {
                assertThat(next, is(sameInstance(expectedChild)));
            } else if (expectedChild instanceof Name) {
                Name expectedName = (Name)expectedChild;
                assertThat(next.getName(), is(expectedName));
            } else if (expectedChild instanceof String) {
                Name expectedName = name((String)expectedChild);
                assertThat(next.getName(), is(expectedName));
            }
        }
        assertThat(iter.hasNext(), is(false));
    }

    protected void assertChildNodes( Children children,
                                     Object... childNodes ) {
        Iterator<ChildNode> iter = children.iterator();
        Map<Name, AtomicInteger> expectedSnsIndexes = new HashMap<Name, AtomicInteger>();
        for (Object expectedChild : childNodes) {
            assertThat(iter.hasNext(), is(true));
            ChildNode next = iter.next();
            Name actualName = next.getName();
            // Check the name ...
            if (expectedChild instanceof ChildNode) {
                assertThat(next, is(sameInstance(expectedChild)));
            } else if (expectedChild instanceof Name) {
                Name expectedName = (Name)expectedChild;
                assertThat(actualName, is(expectedName));
            } else if (expectedChild instanceof String) {
                Name expectedName = name((String)expectedChild);
                assertThat(actualName, is(expectedName));
            }
            // Check the SNS ...
            AtomicInteger expectedSns = expectedSnsIndexes.get(actualName);
            if (expectedSns == null) {
                expectedSns = new AtomicInteger(1);
                expectedSnsIndexes.put(actualName, expectedSns);
            }
            assertThat(next.getSnsIndex(), is(expectedSns.getAndIncrement()));
        }
        assertThat(iter.hasNext(), is(false));
    }

    protected void assertSameContent( Children children,
                                      Children other ) {
        Iterator<ChildNode> iter = children.iterator();
        Iterator<ChildNode> otherIter = other.iterator();
        while (iter.hasNext()) {
            assertThat(otherIter.hasNext(), is(true));
            ChildNode next = iter.next();
            ChildNode otherNext = otherIter.next();
            assertThat(next, is(otherNext));
        }
        assertThat(iter.hasNext(), is(false));
        assertThat(otherIter.hasNext(), is(false));
    }

    @Test
    public void shouldFindFirstChildByUuid() {
        ChildNode firstChild = children.getChild(firstChildUuid);
        assertThat(firstChild, is(notNullValue()));
        assertThat(firstChild, is(sameInstance(this.firstChild)));
        assertThat(firstChild.getUuid(), is(firstChildUuid));
        assertThat(firstChild.getName(), is(firstChildName));
        assertThat(firstChild.getSnsIndex(), is(1));
        assertThat(firstChild.getSegment().getIndex(), is(1));
        assertThat(firstChild.getSegment().getName(), is(firstChildName));
    }

    @Test
    public void shouldFindFirstChildByName() {
        Segment segment = pathFactory.createSegment(firstChildName, 1);
        ChildNode firstChild = children.getChild(segment);
        assertThat(firstChild, is(notNullValue()));
        assertThat(firstChild, is(sameInstance(this.firstChild)));
        assertThat(firstChild.getUuid(), is(firstChildUuid));
        assertThat(firstChild.getName(), is(firstChildName));
        assertThat(firstChild.getSnsIndex(), is(1));
        assertThat(firstChild.getSegment().getIndex(), is(1));
        assertThat(firstChild.getSegment().getName(), is(firstChildName));
    }

    @Test
    public void shouldImplementToString() {
        children.toString();
    }
}
