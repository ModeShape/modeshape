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
package org.modeshape.common.statistic;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;

public class StopwatchTest {

    private Stopwatch stopwatch;
    private long totalPauseTimeInMillis;

    @Before
    public void beforeEach() {
        this.stopwatch = new Stopwatch();
    }

    private void pause( int numberOfMilliseconds ) {
        try {
            Thread.sleep(numberOfMilliseconds);
            this.totalPauseTimeInMillis += numberOfMilliseconds;
        } catch (InterruptedException e) {
            fail("Error while sleeping for " + numberOfMilliseconds + " milliseconds");
        }
    }

    @Test
    public void shouldAllowStartingAndStoppingOnce() {
        stopwatch.start();
        pause(100);
        stopwatch.stop();
        assertEquals(1, stopwatch.getCount());
    }

    @Test
    public void shouldAllowStartingAndStoppingMultipleTimes() {
        for (int i = 0; i != 3; ++i) {
            assertEquals(false, stopwatch.isRunning());
            stopwatch.start();
            assertEquals(true, stopwatch.isRunning());
            pause(100);
            stopwatch.stop();
            assertEquals(false, stopwatch.isRunning());
        }
    }

    @Test
    public void shouldKnowWhenItsRunning() {
        assertEquals(false, stopwatch.isRunning());
        stopwatch.start();
        assertEquals(true, stopwatch.isRunning());
        stopwatch.stop();
        assertEquals(false, stopwatch.isRunning());
    }

    @Test
    public void shouldAllowStopToBeCalledWhenNotRunning() {
        assertEquals(false, stopwatch.isRunning());
        stopwatch.stop();
        stopwatch.stop();
        assertEquals(false, stopwatch.isRunning());
    }

    @Test
    public void shouldAllowStartToBeCalledWhenAlreadyRunning() {
        assertEquals(false, stopwatch.isRunning());
        stopwatch.start();
        assertEquals(true, stopwatch.isRunning());
        stopwatch.start();
        assertEquals(true, stopwatch.isRunning());
    }

    @Test
    public void shouldReportNumberOfTimesStartedAndStopped() {
        for (int i = 0; i != 3; ++i) {
            stopwatch.start();
            pause(10);
            stopwatch.stop();
        }
        assertEquals(3, stopwatch.getCount());
    }

    @Test
    public void shouldReportTotalTime() {
        for (int i = 0; i != 4; ++i) {
            stopwatch.start();
            pause(100);
            stopwatch.stop();
        }
        assertThat((double)stopwatch.getTotalDuration().getDuration(TimeUnit.MILLISECONDS), is(closeTo(400, 200)));
    }

    @Test
    public void shouldReportAverageTime() {
        for (int i = 0; i != 4; ++i) {
            stopwatch.start();
            pause(100);
            stopwatch.stop();
        }
        assertThat((double)stopwatch.getAverageDuration().getDuration(TimeUnit.MILLISECONDS), is(closeTo(100, 50)));
    }

    @Test
    public void shouldReportMinimumTime() {
        for (int i = 0; i != 3; ++i) {
            stopwatch.start();
            pause(50 * (i + 1));
            stopwatch.stop();
        }
        assertThat((double)stopwatch.getMinimumDuration().getDuration(TimeUnit.MILLISECONDS), is(closeTo(50, 20)));
    }

    @Test
    public void shouldReportMaximumTime() {
        for (int i = 0; i != 3; ++i) {
            stopwatch.start();
            pause(50 * (i + 1));
            stopwatch.stop();
        }
        assertThat((double)stopwatch.getMaximumDuration().getDuration(TimeUnit.MILLISECONDS), is(closeTo(150, 50)));
    }

    @Test
    public void shouldReportValidStatisticsEvenBeforeBeingUsed() {
        assertEquals(0, stopwatch.getCount());

        assertEquals(0.0d, stopwatch.getTotalDuration().getDuration(TimeUnit.SECONDS), 0.00001);
        assertEquals(0.0d, stopwatch.getAverageDuration().getDuration(TimeUnit.SECONDS), 0.00001);
        assertEquals(0.0d, stopwatch.getMinimumDuration().getDuration(TimeUnit.SECONDS), 0.00001);
        assertEquals(0.0d, stopwatch.getMaximumDuration().getDuration(TimeUnit.SECONDS), 0.00001);
    }

    @Test
    public void shouldReportValidStatisticsAfterBeingReset() {
        for (int i = 0; i != 3; ++i) {
            stopwatch.start();
            pause(10 * (i + 1));
            stopwatch.stop();
        }

        stopwatch.reset();

        assertEquals(0, stopwatch.getCount());

        assertEquals(0.0d, stopwatch.getTotalDuration().getDuration(TimeUnit.SECONDS), 0.00001);
        assertEquals(0.0d, stopwatch.getAverageDuration().getDuration(TimeUnit.SECONDS), 0.00001);
        assertEquals(0.0d, stopwatch.getMinimumDuration().getDuration(TimeUnit.SECONDS), 0.00001);
        assertEquals(0.0d, stopwatch.getMaximumDuration().getDuration(TimeUnit.SECONDS), 0.00001);
    }

    @Test
    public void shouldHaveStringRepresentationWithoutStatisticsForSingleSample() {
        stopwatch.start();
        pause(12);
        stopwatch.stop();
        String str = stopwatch.toString();
        System.out.println(str);
        assertTrue(str.matches("^\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
        assertFalse(str.matches(".*1 sample.*"));
        assertFalse(str.matches(".*min=\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
        assertFalse(str.matches(".*max=\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
        assertFalse(str.matches(".*avg=\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
        assertFalse(str.matches(".*median=\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
    }

    @Test
    public void shouldHaveStringRepresentationWithStatisticsForMultipleSample() {
        for (int i = 0; i != 3; ++i) {
            stopwatch.start();
            pause(12);
            stopwatch.stop();
        }
        String str = stopwatch.toString();
        System.out.println(str);
        assertTrue(str.matches("^\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
        assertTrue(str.matches(".*3 samples.*"));
        assertTrue(str.matches(".*min=\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
        assertTrue(str.matches(".*max=\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
        assertTrue(str.matches(".*avg=\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
        assertTrue(str.matches(".*median=\\d{2,}:\\d{2}:\\d{2}\\.\\d{3}(,\\d{1,3})?.*"));
    }

    @Test
    public void shouldHaveAHistogramWithZeroSigma() {
        for (int i = 0; i != 3; ++i) {
            stopwatch.start();
            pause(12);
            stopwatch.stop();
        }
        assertNotNull(stopwatch.getHistogram(0));
    }

    @Test
    public void shouldHaveAHistogramWithOneSigma() {
        for (int i = 0; i != 3; ++i) {
            stopwatch.start();
            pause(12);
            stopwatch.stop();
        }
        assertNotNull(stopwatch.getHistogram(1));
    }

}
