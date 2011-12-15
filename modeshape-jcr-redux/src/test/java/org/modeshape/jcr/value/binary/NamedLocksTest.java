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
