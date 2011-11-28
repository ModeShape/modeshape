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
package org.modeshape.jcr.perftests;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class which provides various statistical information based on a number of numerical values.
 *
 * @author Horia Chiorean
 */
public final class StatisticalData {

    private final double[] values;
    private final SummaryStatistics summaryStatistics;

    public StatisticalData( List<? extends Number> valuesList ) {
       this(valuesList.toArray(valuesList.toArray(new Number[valuesList.size()])));
    }

    public StatisticalData( Number... values ) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("The set of values for the statistical data cannot be empty or null");
        }
        this.summaryStatistics = new SummaryStatistics();
        for (Number value : values) {
            this.summaryStatistics.addValue(value.doubleValue());
        }
        this.values = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            this.values[i] = values[i].doubleValue();
        }
        //store the values in ascending order
        Arrays.sort(this.values);
    }

    public long count() {
        return summaryStatistics.getN();
    }

    public double min() {
        return summaryStatistics.getMin();
    }

    public double max() {
        return summaryStatistics.getMax();
    }

    public double standardDeviation() {
        return summaryStatistics.getStandardDeviation();
    }

    public double lowerQuartile() {
        if (values.length == 1) {
            return Double.NaN;
        }
        if (values.length == 2) {
            return values[0];
        }
        int middleIdx = values.length / 2;
        return median(Arrays.copyOfRange(values,0, middleIdx));
    }

    public double median() {
        if (values.length == 1) {
            return values[0];
        }
        int middleIdx = values.length / 2;
        return (values.length % 2 == 1) ? median(values[middleIdx]) : median(values[middleIdx - 1], values[middleIdx]);
    }

    public double upperQuartile() {
        if (values.length == 1) {
            return Double.NaN;
        }
        if (values.length == 2) {
            return values[1];
        }
        int middleIdx = values.length / 2;
        return (values.length % 2 == 0) ? median(Arrays.copyOfRange(values, middleIdx, values.length)) :
                                          median(Arrays.copyOfRange(values, middleIdx + 1, values.length));
    }

    public double[] fiveNumberSummary() {
        return new double[] {min(), lowerQuartile(), median(), upperQuartile(), max()};
    }

    public List<Double> valuesList() {
        List<Double> result = new ArrayList<Double>(values.length);
        for (double value : values) {
            result.add(value);
        }
        return result;
    }

    private double median( double... values ) {
        if (values.length == 1) {
            return values[0];
        }
        int middleIdx = values.length / 2;
        if (values.length % 2 == 0) {
            return avg(values[middleIdx - 1], values[middleIdx]);
        } else {
            return values[middleIdx];
        }
    }

    private double avg( double... values ) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }
}
