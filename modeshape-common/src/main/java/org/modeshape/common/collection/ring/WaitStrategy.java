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

import java.util.concurrent.TimeoutException;

/**
 * Strategy that encapsulates how to wait for a {@link Pointer} to make the supplied position available.
 */
public interface WaitStrategy {

    /**
     * Wait for the given position to be available. It is possible for this method to return a value less than the supplied
     * position supplied on the implementation of the {@link WaitStrategy}. A common use for this is to signal a timeout.
     * 
     * @param position the desired position
     * @param pointer the main pointer from ringbuffer. Wait/notify strategies will need this as it's the only sequence that is
     *        also notified upon update.
     * @param dependentPointer on which to wait.
     * @param barrier the processor is waiting on.
     * @return the position that is available, and that may be greater than the requested position.
     * @throws InterruptedException if the thread is interrupted.
     * @throws TimeoutException if this blocking method times out
     */
    long waitFor( long position,
                  Pointer pointer,
                  Pointer dependentPointer,
                  PointerBarrier barrier ) throws InterruptedException, TimeoutException;

    /**
     * Signal all components that want to be notified that the pointer has advanced.
     */
    void signalAllWhenBlocking();
}
