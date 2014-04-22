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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link WaitStrategy} that blocks the current thread until an entry is available for consumption. This implementation uses
 * Java locks and {@link Condition conditions}.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class BlockingWaitStrategy implements WaitStrategy {

    private final Lock lock = new ReentrantLock();
    private final Condition waitCondition = lock.newCondition();

    @Override
    public long waitFor( long position,
                         Pointer pointer,
                         Pointer dependentPointer,
                         PointerBarrier barrier ) throws InterruptedException {
        // Get the next position that is immediately available ...
        long availablePosition = pointer.get();
        if (availablePosition < position) {
            // The caller wants a position that is farther along than what is available, so we have to block ...
            lock.lock();
            try {
                while (!barrier.isComplete() && (availablePosition = pointer.get()) < position) {
                    waitCondition.await();
                }
            } finally {
                lock.unlock();
            }
        }
        return availablePosition;
    }

    @Override
    public void signalAllWhenBlocking() {
        lock.lock();
        try {
            waitCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
