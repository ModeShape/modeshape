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

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class TimeBasedKeysTest {

    protected TimeBasedKeys counter = TimeBasedKeys.create();
    private boolean print;

    @Before
    public void beforeEach() {
        this.print = false;
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor1BitCounter() {
        TimeBasedKeys counter = TimeBasedKeys.create(1);
        assertEquals(0L, counter.getCounterStartingAt(0L));
        assertEquals(1L, counter.getCounterEndingAt(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor2Bit() {
        TimeBasedKeys counter = TimeBasedKeys.create(2);
        assertEquals(0L, counter.getCounterStartingAt(0L));
        assertEquals(3L, counter.getCounterEndingAt(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor3Bit() {
        TimeBasedKeys counter = TimeBasedKeys.create(3);
        assertEquals(0L, counter.getCounterStartingAt(0L));
        assertEquals(7L, counter.getCounterEndingAt(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor4Bit() {
        TimeBasedKeys counter = TimeBasedKeys.create(4);
        assertEquals(0L, counter.getCounterStartingAt(0L));
        assertEquals(15L, counter.getCounterEndingAt(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor5Bit() {
        TimeBasedKeys counter = TimeBasedKeys.create(5);
        assertEquals(0L, counter.getCounterStartingAt(0L));
        assertEquals(31L, counter.getCounterEndingAt(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor6Bit() {
        TimeBasedKeys counter = TimeBasedKeys.create(6);
        assertEquals(0L, counter.getCounterStartingAt(0L));
        assertEquals(63L, counter.getCounterEndingAt(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor7Bit() {
        TimeBasedKeys counter = TimeBasedKeys.create(7);
        assertEquals(0L, counter.getCounterStartingAt(0L));
        assertEquals(127L, counter.getCounterEndingAt(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor8Bit() {
        TimeBasedKeys counter = TimeBasedKeys.create(8);
        assertEquals(0L, counter.getCounterStartingAt(0L));
        assertEquals(255L, counter.getCounterEndingAt(0L));
    }

    @Test
    public void shouldCorrectlyCalculateFirstAndLastCounterFor16Bit() {
        long maxValue = ((long)Math.pow(2, 16)) - 1;
        TimeBasedKeys counter = TimeBasedKeys.create(16);
        assertEquals(0L, counter.getCounterStartingAt(0L));
        assertEquals(maxValue, counter.getCounterEndingAt(0L));
    }

    @Test
    public void shouldObtain10MillionCountersThreadSafe() {
        print(counter.nextKey());
        for (int i = 0; i != 10000000; ++i) {
            counter.nextKey();
        }
        print(counter.nextKey());
    }

    @Test
    public void shouldObtain10MillionCountersFromThreadSafeUsingMultipleThreads() {
        print(counter.nextKey());
        for (int j = 0; j != 100; ++j) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i != 100000; ++i) {
                        counter.nextKey();
                    }
                }
            }).run();
        }
        print(counter.nextKey());
    }

    protected void print( String str ) {
        if (print) System.out.println(str);
    }

    protected void print( long value ) {
        if (print) System.out.println(value);
    }
}
