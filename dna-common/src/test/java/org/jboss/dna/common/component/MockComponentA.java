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

package org.jboss.dna.common.component;

import java.util.concurrent.atomic.AtomicInteger;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.i18n.MockI18n;
import org.jboss.dna.common.monitor.ActivityMonitor;

/**
 * A sequencer that can be used for basic unit testing.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@ThreadSafe
public class MockComponentA implements SampleComponent {

    private SampleComponentConfig config;
    private AtomicInteger counter = new AtomicInteger();

    /**
     * {@inheritDoc}
     */
    public void setConfiguration( SampleComponentConfig config ) {
        this.config = config;
    }

    /**
     * {@inheritDoc}
     */
    public void doSomething( ActivityMonitor activityMonitor ) {
        try {
            activityMonitor.beginTask(1, MockI18n.passthrough, "Incrementing counter");
            // increment the counter and record the progress ...
            this.counter.incrementAndGet();
            activityMonitor.worked(1);
        } finally {
            activityMonitor.done();
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
    public SampleComponentConfig getConfiguration() {
        return this.config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (this.config != null ? this.config.getName() : "SampleComponent") + " [" + this.getCounter() + "]";
    }
}
