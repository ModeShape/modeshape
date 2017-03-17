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

import static org.modeshape.jboss.subsystem.ModelAttributes.DB_COMPRESS;
import static org.modeshape.jboss.subsystem.ModelAttributes.CONNECTION_URL;
import static org.modeshape.jboss.subsystem.ModelAttributes.CREATE_ON_START;
import static org.modeshape.jboss.subsystem.ModelAttributes.DRIVER;
import static org.modeshape.jboss.subsystem.ModelAttributes.DROP_ON_EXIT;
import static org.modeshape.jboss.subsystem.ModelAttributes.FETCH_SIZE;
import static org.modeshape.jboss.subsystem.ModelAttributes.PASSWORD;
import static org.modeshape.jboss.subsystem.ModelAttributes.PERSISTENCE_DS_JNDI_NAME;
import static org.modeshape.jboss.subsystem.ModelAttributes.POOL_SIZE;
import static org.modeshape.jboss.subsystem.ModelAttributes.TABLE_NAME;
import static org.modeshape.jboss.subsystem.ModelAttributes.USERNAME;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.PersistenceService;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.persistence.relational.RelationalDbConfig;
import org.modeshape.schematic.DocumentFactory;
import org.modeshape.schematic.document.EditableDocument;

public class AddDatabasePersistence extends AbstractAddStepHandler {

    public static final AddDatabasePersistence INSTANCE = new AddDatabasePersistence();

    private AddDatabasePersistence() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        for (AttributeDefinition attribute : ModelAttributes.PERSISTENCE_DB_ATTRIBUTES) {
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
        persistenceConfig.setString(FieldName.TYPE, RelationalDbConfig.ALIAS1);
        ModelNode tableName = TABLE_NAME.resolveModelAttribute(context, operation);
        if (tableName.isDefined()) {
            persistenceConfig.setString(TABLE_NAME.getFieldName(), tableName.asString());
        }
        ModelNode createOnStart = CREATE_ON_START.resolveModelAttribute(context, operation);
        if (createOnStart.isDefined()) {
            persistenceConfig.setBoolean(CREATE_ON_START.getFieldName(), createOnStart.asBoolean());
        }
        ModelNode dropOnExit = DROP_ON_EXIT.resolveModelAttribute(context, operation);
        if (dropOnExit.isDefined()) {
            persistenceConfig.setBoolean(DROP_ON_EXIT.getFieldName(), dropOnExit.asBoolean());
        }
        ModelNode fetchSize = FETCH_SIZE.resolveModelAttribute(context, operation);
        if (fetchSize.isDefined()) {
            persistenceConfig.setNumber(FETCH_SIZE.getFieldName(), fetchSize.asInt());
        }
        ModelNode url = CONNECTION_URL.resolveModelAttribute(context, operation);
        if (url.isDefined()) {
            persistenceConfig.setString(CONNECTION_URL.getFieldName(), url.asString());
        }
        ModelNode driver = DRIVER.resolveModelAttribute(context, operation);
        if (driver.isDefined()) {
            persistenceConfig.setString(DRIVER.getFieldName(), driver.asString());
        }
        ModelNode username = USERNAME.resolveModelAttribute(context, operation);
        if (username.isDefined()) {
            persistenceConfig.setString(USERNAME.getFieldName(), username.asString());
        }
        ModelNode password = PASSWORD.resolveModelAttribute(context, operation);
        if (password.isDefined()) {
            persistenceConfig.setString(PASSWORD.getFieldName(), password.asString());
        }  
        ModelNode jndi = PERSISTENCE_DS_JNDI_NAME.resolveModelAttribute(context, operation);
        if (jndi.isDefined()) {
            persistenceConfig.setString(PERSISTENCE_DS_JNDI_NAME.getFieldName(), jndi.asString());
        }
        ModelNode compress = DB_COMPRESS.resolveModelAttribute(context, operation);
        if (compress.isDefined()) {
            persistenceConfig.setBoolean(DB_COMPRESS.getFieldName(), compress.asBoolean());
        }
        ModelNode poolSize = POOL_SIZE.resolveModelAttribute(context, operation);
        if (poolSize.isDefined()) {
            persistenceConfig.setNumber(POOL_SIZE.getFieldName(), poolSize.asInt());
        }
        if (operation.hasDefined(ModelKeys.PROPERTIES)) {
            for (Property property : operation.get(ModelKeys.PROPERTIES).asPropertyList()) {
                persistenceConfig.set(property.getName(), property.getValue().asString());
            }
        }

        PersistenceService persistenceService = new PersistenceService(repositoryName, persistenceConfig);

        ServiceBuilder<JcrRepository> serviceBuilder = target.addService(ModeShapeServiceNames.persistenceDBServiceName(
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
