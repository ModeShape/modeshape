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

package org.modeshape.common.collection.ring;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * A {@link Pointer} that will always stay behind a number of other {@link Pointer} instances, which can be safely and dynamically
 * added or removed.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class TrailingPointer extends Pointer implements DependentOnPointers {

    private static final Pointer[] EMPTY_ARRAY = new Pointer[0];

    private static final AtomicReferenceFieldUpdater<TrailingPointer, Pointer[]> STAY_BEHIND_UPDATER = AtomicReferenceFieldUpdater.newUpdater(TrailingPointer.class,
                                                                                                                                              Pointer[].class,
                                                                                                                                              "stayBehinds");

    private volatile Pointer[] stayBehinds;

    public TrailingPointer( Pointer... stayBehinds ) {
        this(INITIAL_VALUE, stayBehinds);
    }

    public TrailingPointer( long initialValue,
                            Pointer... stayBehinds ) {
        super(initialValue);
        this.stayBehinds = stayBehinds == null ? EMPTY_ARRAY : stayBehinds;
    }

    @Override
    public long get() {
        return Math.max(INITIAL_VALUE, Pointers.getMinimum(stayBehinds, Long.MAX_VALUE) - 1L);
    }

    @Override
    public void stayBehind( Pointer... pointers ) {
        Pointers.add(this, STAY_BEHIND_UPDATER, null, pointers);
    }

    @Override
    public boolean ignore( Pointer pointer ) {
        return Pointers.remove(this, STAY_BEHIND_UPDATER, pointer);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Pointer pointer : stayBehinds) {
            sb.append(pointer).append(",");
        }
        sb.append(']');
        return sb.toString();
    }
}
