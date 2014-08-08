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

package org.modeshape.jcr.cache;

import org.modeshape.jcr.value.Name;

/**
 * A function interface that determines (potentially lazily) the number of siblings with a given name.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class SiblingCounter {
    /**
     * Get the number of existing siblings that all have the supplied name. If the implementation is expensive, it should cache
     * the result so subsequent calls with the same parameter are fast.
     *
     * @param childName the name for the siblings; may not be null
     * @return the number of existing siblings with the new child's name
     */
    public abstract int countSiblingsNamed( Name childName );

    /**
     * Create a sibling counter that always returns '0' for the number of same name siblings.
     *
     * @return the counter that always returns 0; never null
     */
    public static SiblingCounter noSiblings() {
        return constant(0);
    }

    /**
     * Create a sibling counter that always returns '1' for the number of same name siblings.
     *
     * @return the counter that always returns 0; never null
     */
    public static SiblingCounter oneSibling() {
        return constant(1);
    }

    /**
     * Create a sibling counter that always return the supplied count, regardless of the name or node.
     *
     * @param count the count to be returned; may not be negative
     * @return the counter that always returns {@code count}; never null
     */
    public static SiblingCounter constant( final int count ) {
        assert count > -1;
        return new SiblingCounter() {
            @Override
            public int countSiblingsNamed( Name childName ) {
                return count;
            }
        };
    }

    /**
     * Creates a sibling counter that uses the supplied {@link ChildReferences}.
     *
     * @param childRefs the child references; may not be null
     * @return the sibling counter; never null
     */
    public static SiblingCounter create( final ChildReferences childRefs ) {
        assert childRefs != null;
        return new SiblingCounter() {
            @Override
            public int countSiblingsNamed( Name childName ) {
                return childRefs.getChildCount(childName);
            }
        };
    }

    /**
     * Creates a sibling counter that lazily obtains a {@link ChildReferences}.
     *
     * @param node the node; may not be null
     * @param cache the cache; may not be null
     * @return the sibling counter; never null
     */
    public static SiblingCounter create( final CachedNode node,
                                         final NodeCache cache ) {
        assert node != null;
        assert cache != null;
        return new SiblingCounter() {
            @Override
            public int countSiblingsNamed( Name childName ) {
                return node.getChildReferences(cache).getChildCount(childName);
            }
        };
    }

    /**
     * Creates a sibling counter that alters another counter by a constant value.
     *
     * @param counter the sibling counter; may not be null
     * @param delta the positive or negative amount by which the {@code counter}'s value is altered
     * @return the sibling counter; never null
     */
    public static SiblingCounter alter( final SiblingCounter counter,
                                        final int delta ) {
        assert counter != null;
        return new SiblingCounter() {
            @Override
            public int countSiblingsNamed( Name childName ) {
                int count = counter.countSiblingsNamed(childName) + delta;
                return count > 0 ? count : 0; // never negative
            }
        };
    }
}
