/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
