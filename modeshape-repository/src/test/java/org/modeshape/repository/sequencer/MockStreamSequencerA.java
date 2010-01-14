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

package org.modeshape.repository.sequencer;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * A mock stream sequencer that can be used for basic unit testing.
 */
@ThreadSafe
public class MockStreamSequencerA implements StreamSequencer {

    private AtomicInteger counter = new AtomicInteger();
    private volatile CountDownLatch latch = new CountDownLatch(0);

    public void setExpectedCount( int numExpected ) {
        this.latch = new CountDownLatch(numExpected);
    }

    public boolean awaitExecution( long timeout,
                                   TimeUnit unit ) throws InterruptedException {
        return this.latch.await(timeout, unit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.StreamSequencer#sequence(java.io.InputStream,
     *      org.modeshape.graph.sequencer.SequencerOutput, org.modeshape.graph.sequencer.StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        // increment the counter and record the progress ...
        this.counter.incrementAndGet();
        this.latch.countDown();
    }

    public int getCounter() {
        return this.counter.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "MockStreamSequencerA [" + this.getCounter() + "]";
    }
}
