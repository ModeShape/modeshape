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
package org.modeshape.jcr.value.binary;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.StringUtil;

public class NamedLocksTest {

    private NamedLocks namedLocks;

    @Before
    public void beforeEach() {
        namedLocks = new NamedLocks();
    }

    @Test
    public void shouldHaveNoLocksToStart() {
        assertThat(namedLocks.size(), is(0));
    }

    @Test
    public void shouldAllowLockingAndUnlocking() {
        assertThat(namedLocks.size(), is(0));
        Lock lock = namedLocks.writeLock("some name");
        assertThat(namedLocks.size(), is(1));
        lock.unlock();
        assertThat(namedLocks.size(), is(0));
    }

    @Test
    public void shouldAllowMultipleThreadsToConcurrentlyAccessState() throws Exception {
        final int numThreads = 10;
        final int runsPerThread = 20;
        final boolean print = false;
        final String lockName = "counter";
        final AtomicBoolean value = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(numThreads);
        final NamedLocks locks = this.namedLocks;
        for (int i = 0; i != numThreads; ++i) {
            // Create a thread that repeatedly attempts to lock to modifiy the value.
            // The 'value' should always be false when the lock is obtained,
            // then changed to true while the lock is held, and then set back to false
            // before the lock is released.
            final String threadName = StringUtil.justifyRight("" + (i + 1), 3, ' ');
            Runnable runner = new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i != runsPerThread; ++i) {
                            Lock lock = locks.writeLock(lockName);
                            try {
                                // We have the exclusive lock, so try toggling the value and verify it's what we expect ...
                                assertThat(value.compareAndSet(false, true), is(true));
                                // Now set the value back to 'false' for the other threads ...
                                assertThat(value.compareAndSet(true, false), is(true));
                                if (print) System.out.println("Thread " + threadName + " iteration " + i);
                            } finally {
                                lock.unlock();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            };
            Thread thread = new Thread(runner);
            thread.start();
        }
        // Now wait for the threads to complete ...
        latch.await(10, TimeUnit.SECONDS);
        assertThat(namedLocks.size(), is(0));
    }
}
