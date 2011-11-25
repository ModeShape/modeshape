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
package com.modeshape.jcr.perftests;

import static java.util.concurrent.TimeUnit.*;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;
import org.modeshape.jcr.perftests.util.DurationsConverter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for the {@link org.modeshape.jcr.perftests.util.DurationsConverter} class.
 *
 * @author Horia Chiorean
 */
public class DurationsConverterTest {

    @Test
    public void nanosToNanos() {
        List<Double> expectedValues = Arrays.asList(1d, 2d);
        assertEquals(expectedValues, DurationsConverter.convertFromNanos(Arrays.asList(1l, 2l), NANOSECONDS));
    }

    @Test
    public void nanosToMillis() {
        List<Double> expectedValues = Arrays.asList(0.000101, 0.000102);
        assertEquals(expectedValues, DurationsConverter.convertFromNanos(Arrays.asList(101l, 102l), MILLISECONDS));
    }

    @Test
    public void nanosToSeconds() {
        List<Double> expectedValues = Arrays.asList(1d, 2d);
        List<Long> input = Arrays.asList((long) Math.pow(10, 9), (long) (2 * Math.pow(10, 9)));
        assertEquals(expectedValues, DurationsConverter.convertFromNanos(input, SECONDS));
    }

    @Test
    public void nanosToMinutes() {
        List<Double> expectedValues = Arrays.asList(0.2d, 0.4d);
        List<Long> input = Arrays.asList((long) (12 * Math.pow(10, 9)), (long) (24 * Math.pow(10, 9)));
        assertEquals(expectedValues, DurationsConverter.convertFromNanos(input, MINUTES));
    }

    @Test
    public void nanosToHours() {
        long nano1 = 1000;
        long nano2 = 500;
        List<Double> expectedValues = Arrays.asList((double) HOURS.convert(nano1, NANOSECONDS),
                (double) HOURS.convert(nano2, NANOSECONDS));
        assertEquals(expectedValues, DurationsConverter.convertFromNanos(Arrays.asList(nano1, nano2), TimeUnit.HOURS));
    }

    @Test
    public void nanosToDays() {
        long nano1 = 1000;
        long nano2 = 2500;
        List<Double> expectedValues = Arrays.asList((double) DAYS.convert(nano1, NANOSECONDS),
                (double) HOURS.convert(nano2, NANOSECONDS));
        assertEquals(expectedValues, DurationsConverter.convertFromNanos(Arrays.asList(nano1, nano2), TimeUnit.DAYS));
    }
}
