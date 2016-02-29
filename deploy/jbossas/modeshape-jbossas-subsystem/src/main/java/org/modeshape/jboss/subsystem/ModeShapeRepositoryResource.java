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
package org.modeshape.jboss.subsystem;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.modeshape.jboss.metric.ModelMetrics;
import org.modeshape.jboss.metric.ModelMetrics.MetricAttributeDefinition;

/**
 * 
 */
public class ModeShapeRepositoryResource extends SimpleResourceDefinition {
    protected final static ModeShapeRepositoryResource INSTANCE = new ModeShapeRepositoryResource();

    private ModeShapeRepositoryResource() {
        super(ModeShapeExtension.REPOSITORY_PATH, ModeShapeExtension.getResourceDescriptionResolver(ModelKeys.REPOSITORY),
              AddRepository.INSTANCE, RemoveRepository.INSTANCE);
    }
    
    @Override
    public void registerAttributes( ManagementResourceRegistration resourceRegistration ) {
        super.registerAttributes(resourceRegistration);
        RepositoryWriteAttributeHandler.INSTANCE.registerAttributes(resourceRegistration);
        registerMetrics(resourceRegistration);
    }

    private void registerMetrics( final ManagementResourceRegistration registration ) {
        for (final MetricAttributeDefinition attrDefn : ModelMetrics.ALL_METRICS) {
            registration.registerMetric(attrDefn, attrDefn.metricHandler());
        }
    }

}
