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

import java.util.concurrent.atomic.AtomicLong;

/**
 * A sequence of long values that are updated atomically.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class Pointer {

    public static long INITIAL_VALUE = -1L;

    private final AtomicLong value;

    public Pointer() {
        value = new AtomicLong(INITIAL_VALUE);
    }

    public Pointer( long initialValue ) {
        value = new AtomicLong(initialValue);
    }

    public long get() {
        return value.get();
    }

    public void set( long newValue ) {
        this.value.set(newValue);
    }

    public long incrementAndGet() {
        return this.value.incrementAndGet();
    }

    @Override
    public String toString() {
        return super.toString() + "=" + value.get();
    }

}
