/*
 * JBoss, Home of Professional Open Source. Copyright 2008, Red Hat Middleware LLC, and individual contributors as indicated by
 * the @author tags. See the copyright.txt file in the distribution for a full listing of individual contributors. This is free
 * software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your option) any later version. This software is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details. You should have received a
 * copy of the GNU Lesser General Public License along with this software; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.stats;

import static org.junit.Assert.*;
import java.util.concurrent.TimeUnit;
import org.jboss.dna.common.stats.Stopwatch;
import org.junit.Before;
import org.junit.Test;

public class StopwatchTest {

    private Stopwatch stopwatch;
    private int totalPauseTimeInMillis;

    @Before
    public void beforeEach() throws Exception {
        this.stopwatch = new Stopwatch();
    }

    private double getTotalPauseTime( TimeUnit unit ) {
        switch (unit) {
            case MILLISECONDS:
                return this.totalPauseTimeInMillis;
            case MICROSECONDS:
                return this.totalPauseTimeInMillis * 1000;
            case NANOSECONDS:
                return this.totalPauseTimeInMillis * 1000 * 1000;
            case SECONDS:
                double time = this.totalPauseTimeInMillis;
                return time / 1000.0d;
        }
        fail("Unexpected time unit -- this should not be possible");
        return 0;
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
            pause(10);
            stopwatch.stop();
        }
        assertEquals(this.getTotalPauseTime(TimeUnit.MILLISECONDS), stopwatch.getTotalDuration().getDuration(TimeUnit.MILLISECONDS), 0.000001f);
    }

    @Test
    public void shouldReportAverageTime() {
        for (int i = 0; i != 4; ++i) {
            stopwatch.start();
            pause(10);
            stopwatch.stop();
        }
        assertEquals(10, stopwatch.getAverageDuration().getDuration(TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldReportMinimumTime() {
        for (int i = 0; i != 3; ++i) {
            stopwatch.start();
            pause(10 * (i + 1));
            stopwatch.stop();
        }
        assertEquals(10, stopwatch.getMinimumDuration().getDuration(TimeUnit.MILLISECONDS));
    }

    @Test
    public void shouldReportMaximumTime() {
        for (int i = 0; i != 3; ++i) {
            stopwatch.start();
            pause(10 * (i + 1));
            stopwatch.stop();
        }
        assertEquals(30, stopwatch.getMaximumDuration().getDuration(TimeUnit.MILLISECONDS));
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
