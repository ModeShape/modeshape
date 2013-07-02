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
package org.modeshape.jboss.metric;

import java.util.ArrayList;
import java.util.List;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;

/**
 * Attributes that are metrics in ModeShape subsystem. These are runtime-only and default to a value of zero.
 */
public final class ModelMetrics {

    /**
     * All duration and value metric attribute definitions.
     */
    public static final List<MetricAttributeDefinition> ALL_METRICS;

    static {
        final Window[] windows = Window.values();
        final ValueMetric[] valueMetrics = ValueMetric.values();
        final DurationMetric[] durationMetrics = DurationMetric.values();
        ALL_METRICS = new ArrayList<MetricAttributeDefinition>(windows.length * (valueMetrics.length + durationMetrics.length));

        for (final Window window : windows) {
            for (final DurationMetric metric : durationMetrics) {
                final MetricAttributeDefinition attrDefn = new DurationMetricAttributeDefinition(metric, window);
                ALL_METRICS.add(attrDefn);
            }

            for (final ValueMetric metric : valueMetrics) {
                final MetricAttributeDefinition attrDefn = new ValueMetricAttributeDefinition(metric, window);
                ALL_METRICS.add(attrDefn);
            }
        }
    }

    /**
     * Don't allow construction outside this class.
     */
    private ModelMetrics() {
        // nothing to do
    }

    /**
     * Represents an attribute definition of a duration metric.
     */
    private static class DurationMetricAttributeDefinition extends MetricAttributeDefinition {

        final OperationStepHandler handler;

        /**
         * @param metric the duration metric (cannot be <code>null</code>)
         * @param window the metrics window (cannot be <code>null</code>)
         */
        DurationMetricAttributeDefinition( final DurationMetric metric,
                                           final Window window ) {
            super(attributeName(metric.getLiteral(), window));
            this.handler = new GetDurationMetric(metric, window);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jboss.metric.ModelMetrics.MetricAttributeDefinition#metricHandler()
         */
        @Override
        public OperationStepHandler metricHandler() {
            return this.handler;
        }

    }

    /**
     * Represents an attribute definition of a ModeShape metric.
     */
    public abstract static class MetricAttributeDefinition extends SimpleAttributeDefinition {

        /**
         * @param metricName the metric name (cannot be <code>null</code> or empty)
         * @param window the metrics window (cannot be <code>null</code>)
         * @return the attribute definition name (never <code>null</code> or empty)
         */
        public static final String attributeName( final String metricName,
                                                  final Window window ) {
            assert !StringUtil.isBlank(metricName);
            assert (window != null);
            return (metricName + '-' + window.getLiteral());
        }

        /**
         * @param name the attribute name (cannot be <code>null</code> or empty)
         */
        protected MetricAttributeDefinition( final String name ) {
            super(name, new ModelNode(0), ModelType.DOUBLE, false);
        }

        /**
         * @return the operation used to obtain the metric value (never <code>null</code>)
         */
        public abstract OperationStepHandler metricHandler();

    }

    /**
     * Represents an attribute definition of a value metric.
     */
    private static class ValueMetricAttributeDefinition extends MetricAttributeDefinition {

        final OperationStepHandler handler;

        /**
         * @param metric the value metric (cannot be <code>null</code>)
         * @param window the metrics window (cannot be <code>null</code>)
         */
        ValueMetricAttributeDefinition( final ValueMetric metric,
                                        final Window window ) {
            super(attributeName(metric.getLiteral(), window));
            this.handler = new GetValueMetric(metric, window);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jboss.metric.ModelMetrics.MetricAttributeDefinition#metricHandler()
         */
        @Override
        public OperationStepHandler metricHandler() {
            return this.handler;
        }

    }

}
