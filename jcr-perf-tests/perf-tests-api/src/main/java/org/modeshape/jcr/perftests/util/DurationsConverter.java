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
package org.modeshape.jcr.perftests.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Class which converts between lists for various durations as {@link java.util.concurrent.TimeUnit}
 *
 * @author Horia Chiorean
 */
public final class DurationsConverter {

    private DurationsConverter() {
    }

    /**
     * Converts a list of durations expresses as nanoseconds, to a desired time unit. This method overcomes the loss of precision
     * when using {@link TimeUnit#convert(long, java.util.concurrent.TimeUnit)}
     *
     * @param nanos a list of nanosecond values
     * @param toUnit a <code>TimeUnit</code> to which the nanoseconds will be converted
     * @return a list of double values, representing the converted units.
     */
    public static List<Double> convertFromNanos( List<Long> nanos, TimeUnit toUnit ) {
        List<Double> convertedDurations = new ArrayList<Double>(nanos.size());
        for (long durationNano : nanos) {
            convertedDurations.add(convertFromNanos(durationNano, toUnit));
        }
        return convertedDurations;
    }

    private static double convertFromNanos( double nanoSeconds, TimeUnit toUnit ) {
        switch (toUnit) {
            case NANOSECONDS: {
                return nanoSeconds;
            }
            case MILLISECONDS: {
                return nanoSeconds / Math.pow(10, 6);
            }
            case SECONDS: {
                return nanoSeconds / Math.pow(10, 9);
            }
            case MINUTES: {
                return (nanoSeconds / Math.pow(10, 9)) / 60;
            }
            default: {
                return toUnit.convert((long)nanoSeconds, TimeUnit.NANOSECONDS);
            }
        }
    }
}
