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
package org.modeshape.common.util;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.annotation.Immutable;

/**
 * A {@link Future} implementation that is immediately done.
 * 
 * @param <V> the type of result
 */
@Immutable
public final class ImmediateFuture<V> implements Future<V> {

    /**
     * Factory method to more easily create an immediate future
     * 
     * @param <T> the value type
     * @param value the value that the future should return
     * @return the new future; never null
     */
    public static <T> ImmediateFuture<T> create( T value ) {
        return new ImmediateFuture<T>(value);
    }

    private final V result;

    /**
     * Create a new {@link Future} instance that is completed immediately and returns the supplied result.
     * 
     * @param value the value that the future should return
     */
    public ImmediateFuture( V value ) {
        this.result = value;
    }

    @Override
    public boolean cancel( boolean mayInterruptIfRunning ) {
        return false; // completed
    }

    @Override
    public V get() {
        return result;
    }

    @Override
    public V get( long timeout,
                  TimeUnit unit ) {
        return result;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

}
