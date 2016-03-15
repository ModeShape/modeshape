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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.PersistenceService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.persistence.file.FileDbProvider;
import org.modeshape.schematic.DocumentFactory;
import org.modeshape.schematic.document.EditableDocument;

public class AddFilePersistence extends AbstractAddStepHandler {

    public static final AddFilePersistence INSTANCE = new AddFilePersistence();

    private AddFilePersistence() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        for (AttributeDefinition attribute : ModelAttributes.PERSISTENCE_FS_ATTRIBUTES) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, 
                                  ModelNode operation,
                                  Resource resource) throws OperationFailedException {
        ServiceTarget target = context.getServiceTarget();


        final AddressContext addressContext = AddressContext.forOperation(operation);
        final String repositoryName = addressContext.repositoryName();
        final String type = addressContext.lastPathElementValue();

        // Record the properties ...
        EditableDocument persistenceConfig = DocumentFactory.newDocument();
        persistenceConfig.setString(FieldName.TYPE, FileDbProvider.TYPE_FILE);
        
        ModelNode path = ModelAttributes.FS_PATH.resolveModelAttribute(context, operation);
        String pathString = path.isDefined() ? 
                            path.asString() : 
                            "${" + ModeShapeExtension.JBOSS_DATA_DIR_VARIABLE + "}/modeshape/" + repositoryName; 
        persistenceConfig.setString(ModelAttributes.FS_PATH.getFieldName(), pathString);
        
        ModelNode compress = ModelAttributes.FS_COMPRESS.resolveModelAttribute(context, operation);
        if (compress.isDefined()) {
            persistenceConfig.setBoolean(ModelAttributes.FS_COMPRESS.getFieldName(), compress.asBoolean());
        }

        PersistenceService persistenceService = new PersistenceService(repositoryName, persistenceConfig);

        ServiceBuilder<JcrRepository> serviceBuilder = target.addService(ModeShapeServiceNames.persistenceFSServiceName(
                                                                                 repositoryName,
                                                                                 type),
                                                                         persistenceService);
        // add the engine and the repository as dependencies to make sure they are started first
        serviceBuilder.addDependency(ModeShapeServiceNames.ENGINE, ModeShapeEngine.class,
                                     persistenceService.getModeShapeEngineInjector());
        serviceBuilder.addDependency(ModeShapeServiceNames.repositoryServiceName(repositoryName),
                                     JcrRepository.class,
                                     persistenceService.getJcrRepositoryInjector());
        serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        serviceBuilder.install();
    }
}
