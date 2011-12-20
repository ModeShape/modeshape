/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
