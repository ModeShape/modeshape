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

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class TimeBasedCounterTest {

    protected TimeBasedCounter counter = TimeBasedCounter.createCounter();
    private boolean print;

    @Before
    public void beforeEach() {
        this.print = false;
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor1BitCounter() {
        TimeBasedCounter counter = TimeBasedCounter.createCounter(1);
        assertEquals(0L, counter.getEarliestCounterForTime(0L));
        assertEquals(1L, counter.getLatestInclusiveCounterForTime(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor2Bit() {
        TimeBasedCounter counter = TimeBasedCounter.createCounter(2);
        assertEquals(0L, counter.getEarliestCounterForTime(0L));
        assertEquals(3L, counter.getLatestInclusiveCounterForTime(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor3Bit() {
        TimeBasedCounter counter = TimeBasedCounter.createCounter(3);
        assertEquals(0L, counter.getEarliestCounterForTime(0L));
        assertEquals(7L, counter.getLatestInclusiveCounterForTime(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor4Bit() {
        TimeBasedCounter counter = TimeBasedCounter.createCounter(4);
        assertEquals(0L, counter.getEarliestCounterForTime(0L));
        assertEquals(15L, counter.getLatestInclusiveCounterForTime(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor5Bit() {
        TimeBasedCounter counter = TimeBasedCounter.createCounter(5);
        assertEquals(0L, counter.getEarliestCounterForTime(0L));
        assertEquals(31L, counter.getLatestInclusiveCounterForTime(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor6Bit() {
        TimeBasedCounter counter = TimeBasedCounter.createCounter(6);
        assertEquals(0L, counter.getEarliestCounterForTime(0L));
        assertEquals(63L, counter.getLatestInclusiveCounterForTime(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor7Bit() {
        TimeBasedCounter counter = TimeBasedCounter.createCounter(7);
        assertEquals(0L, counter.getEarliestCounterForTime(0L));
        assertEquals(127L, counter.getLatestInclusiveCounterForTime(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor8Bit() {
        TimeBasedCounter counter = TimeBasedCounter.createCounter(8);
        assertEquals(0L, counter.getEarliestCounterForTime(0L));
        assertEquals(255L, counter.getLatestInclusiveCounterForTime(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor16Bit() {
        long maxValue = ((long)Math.pow(2, 16)) - 1;
        TimeBasedCounter counter = TimeBasedCounter.createCounter(16);
        assertEquals(0L, counter.getEarliestCounterForTime(0L));
        assertEquals(maxValue, counter.getLatestInclusiveCounterForTime(0L));
    }

    @Test
    public void shouldObtain10MillionCountersThreadSafe() {
        print(counter.next());
        for (int i = 0; i != 10000000; ++i) {
            counter.next();
        }
        print(counter.next());
    }

    @Test
    public void shouldObtain10MillionCountersFromThreadSafeUsingMultipleThreads() {
        print(counter.next());
        for (int j = 0; j != 100; ++j) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i != 100000; ++i) {
                        counter.next();
                    }
                }
            }).run();
        }
        print(counter.next());
    }

    protected void print( String str ) {
        if (print) System.out.println(str);
    }

    protected void print( long value ) {
        if (print) System.out.println(value);
    }
}
