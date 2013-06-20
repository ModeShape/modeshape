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
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.modeshape.jcr.api.monitor.DurationMetric;
import org.modeshape.jcr.api.monitor.ValueMetric;

/**
 * Attributes that are metrics in ModeShape subsystem. These are runtime-only and default to a value of zero.
 */
public final class ModelMetrics {

    /**
     * A collection of all the ModeShape duration metrics.
     */
    public static final AttributeDefinition[] REPOSITORY_DURATION_METRICS;

    /**
     * A collection of all the ModeShape value metrics.
     */
    public static final AttributeDefinition[] REPOSITORY_VALUE_METRICS;

    static {
        { // duration metrics
            final List<AttributeDefinition> durationMetrics = new ArrayList<AttributeDefinition>(DurationMetric.values().length);

            for (final DurationMetric metric : DurationMetric.values()) {
                final SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(metric.getLiteral(),
                                                                                                      ModelType.DOUBLE);
                builder.setFlags(AttributeAccess.Flag.STORAGE_RUNTIME);
                builder.setDefaultValue(new ModelNode(0));
                durationMetrics.add(builder.build());
            }

            REPOSITORY_DURATION_METRICS = durationMetrics.toArray(new AttributeDefinition[durationMetrics.size()]);
        }

        { // duration metrics
            final List<AttributeDefinition> valueMetrics = new ArrayList<AttributeDefinition>(ValueMetric.values().length);

            for (final ValueMetric metric : ValueMetric.values()) {
                final SimpleAttributeDefinitionBuilder builder = new SimpleAttributeDefinitionBuilder(metric.getLiteral(),
                                                                                                      ModelType.DOUBLE);
                builder.setFlags(AttributeAccess.Flag.STORAGE_RUNTIME);
                builder.setDefaultValue(new ModelNode(0));
                valueMetrics.add(builder.build());
            }

            REPOSITORY_VALUE_METRICS = valueMetrics.toArray(new AttributeDefinition[valueMetrics.size()]);
        }
    }

    /**
     * Don't allow construction outside this class.
     */
    private ModelMetrics() {
        // nothing to do
    }

}
