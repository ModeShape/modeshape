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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jboss.subsystem.ModeShapeServiceNames;
import org.modeshape.jcr.api.monitor.History;
import org.modeshape.jcr.api.monitor.RepositoryMonitor;
import org.modeshape.jcr.api.monitor.Statistics;
import org.modeshape.jcr.api.monitor.Window;

/**
 * Represents either a ModeShape value or duration metric operation.
 */
public abstract class ModeShapeMetricHandler extends AbstractRuntimeOnlyHandler {

    /**
     * Will be <code>null</code> until {@link #logger} is called.
     */
    private Logger logger;

    private final Window window;

    /**
     * @param metricWindow the metric window (cannot be <code>null</code>)
     */
    protected ModeShapeMetricHandler( final Window metricWindow ) {
        CheckArg.isNotNull(metricWindow, "metricWindow");
        this.window = metricWindow;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.as.controller.AbstractRuntimeOnlyHandler#executeRuntimeStep(org.jboss.as.controller.OperationContext,
     *      org.jboss.dmr.ModelNode)
     */
    @Override
    protected void executeRuntimeStep( final OperationContext context,
                                       final ModelNode operation ) throws OperationFailedException {
        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);
        final String repositoryName = pathAddress.getLastElement().getValue();
        final ServiceController<?> sc = context.getServiceRegistry(false).getRequiredService(ModeShapeServiceNames.monitorServiceName(repositoryName));
        final RepositoryMonitor repoStats = (RepositoryMonitor)sc.getValue();

        try {
            final History history = history(repoStats);
            final Statistics[] stats = history.getStats();

            if ((stats.length != 0) && (stats[stats.length - 1] != null)) {
                final ModelNode result = context.getResult();

                for (final Statistics sample : stats) {
                    // sample can be null if the window is larger than the repository uptime
                    if (sample != null) {
                        result.add(sample.getMaximum());
                    }
                }
            }

            context.stepCompleted();
        } catch (final Exception e) {
            throw new OperationFailedException(e);
        }
    }

    /**
     * @param repoStats the repository statistics used to obtain the metric history (cannot be <code>null</code>)
     * @return the requested metric history (never <code>null</code> but can be empty)
     * @throws Exception if there is a problem obtaining the repository statistics history
     */
    protected abstract History history( final RepositoryMonitor repoStats ) throws Exception;

    /**
     * @return the logger (never <code>null</code>)
     */
    protected final Logger logger() {
        if (this.logger == null) {
            this.logger = Logger.getLogger(getClass().getName());
        }

        return this.logger;
    }

    /**
     * @return the metrics window (never <code>null</code>)
     */
    protected Window window() {
        return this.window;
    }

}
