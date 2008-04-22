/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.repository.sequencers;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.Node;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.CoreI18n;
import org.jboss.dna.common.monitor.ProgressMonitor;
import org.jboss.dna.repository.observation.NodeChange;
import org.jboss.dna.repository.sequencers.Sequencer;
import org.jboss.dna.repository.sequencers.SequencerConfig;
import org.jboss.dna.repository.util.ExecutionContext;
import org.jboss.dna.repository.util.RepositoryNodePath;

/**
 * A sequencer that can be used for basic unit testing.
 * @author Randall Hauch
 */
@ThreadSafe
public class MockSequencerA implements Sequencer {

    private SequencerConfig config;
    private AtomicInteger counter = new AtomicInteger();
    private CountDownLatch latch = new CountDownLatch(0);

    public void setExpectedCount( int numExpected ) {
        this.latch = new CountDownLatch(numExpected);
    }

    public boolean awaitExecution( long timeout, TimeUnit unit ) throws InterruptedException {
        return this.latch.await(timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    public void setConfiguration( SequencerConfig sequencerConfiguration ) {
        this.config = sequencerConfiguration;
    }

    /**
     * {@inheritDoc}
     */
    public void execute( Node input, String sequencedPropertyName, NodeChange changes, Set<RepositoryNodePath> outputPaths, ExecutionContext context, ProgressMonitor progress ) {
        try {
            progress.beginTask(1, CoreI18n.passthrough, "Incrementing counter");
            // increment the counter and record the progress ...
            this.counter.incrementAndGet();
            this.latch.countDown();
            progress.worked(1);
        } finally {
            progress.done();
        }
    }

    public int getCounter() {
        return this.counter.get();
    }

    public boolean isConfigured() {
        return this.config != null;
    }

    /**
     * @return config
     */
    public SequencerConfig getConfiguration() {
        return this.config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (this.config != null ? this.config.getName() : "SampleSequencer") + " [" + this.getCounter() + "]";
    }
}
