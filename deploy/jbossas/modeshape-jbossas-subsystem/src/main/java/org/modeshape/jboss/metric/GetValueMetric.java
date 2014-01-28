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
package org.modeshape.jboss.metric;

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.monitor.History;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.api.monitor.Window;

/**
 * Represents a ModeShape value metric operation.
 */
final class GetValueMetric extends ModeShapeMetricHandler {

    /**
     * The ModeShape metric (never <code>null</code>)
     */
    private final ValueMetric metric;

    /**
     * @param metric the value metric whose operation is being constructed (cannot be <code>null</code>)
     * @param window the metric window (cannot be <code>null</code>)
     */
    public GetValueMetric( final ValueMetric metric,
                           final Window window ) {
        super(window);

        CheckArg.isNotNull(metric, "metric");
        this.metric = metric;
    }

    /**
     * @see org.modeshape.jboss.metric.ModeShapeMetricHandler#history(org.modeshape.jcr.api.monitor.RepositoryMonitor)
     */
    @Override
    protected History history( final RepositoryMonitor repoStats ) throws Exception {
        CheckArg.isNotNull(repoStats, "repoStats");
        return repoStats.getHistory(this.metric, window());
    }

}
