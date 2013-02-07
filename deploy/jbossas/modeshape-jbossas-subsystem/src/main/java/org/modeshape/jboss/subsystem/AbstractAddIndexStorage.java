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
package org.modeshape.jboss.subsystem;

import java.util.List;
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.IndexStorage;
import org.modeshape.jboss.service.IndexStorageService;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

public abstract class AbstractAddIndexStorage extends AbstractAddStepHandler {

    protected static final String DATA_DIR_VARIABLE = ModeShapeExtension.DATA_DIR_VARIABLE;

    protected AbstractAddIndexStorage() {
    }

    static void populate( ModelNode operation,
                          ModelNode model,
                          String modelName,
                          AttributeDefinition[] attributes ) throws OperationFailedException {
        for (AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(operation, model);
        }
        // Set the index storage type last (overwriting any value that they've manually added) ...
        model.get(ModelKeys.INDEX_STORAGE_TYPE).set(modelName);
    }

    @Override
    protected void performRuntime( final OperationContext context,
                                   final ModelNode operation,
                                   final ModelNode storage,
                                   final ServiceVerificationHandler verificationHandler,
                                   final List<ServiceController<?>> newControllers ) throws OperationFailedException {

        ServiceTarget target = context.getServiceTarget();

        final ModelNode address = operation.require(OP_ADDR);
        final PathAddress pathAddress = PathAddress.pathAddress(address);
        final String repositoryName = pathAddress.getElement(1).getValue();
        ServiceName indexStorageServiceName = ModeShapeServiceNames.indexStorageServiceName(repositoryName);

        //get the default service registered by "AddRepository
        IndexStorageService existingService = (IndexStorageService)context.getServiceRegistry(false).getService(indexStorageServiceName).getService();
        //get the query instance from the existing service, so that any indexing attributes set via "AddRepository" are not lost
        EditableDocument query = existingService.getValue().getQueryConfiguration();

        // Build the 'query/indexing' nested document ...
        EditableDocument indexing = query.getOrCreateDocument(FieldName.INDEXING);
        writeIndexStorageSpecificIndexingConfiguration(context, storage, indexing);

        // Build the 'query/indexingStorage' nested document from scratch
        EditableDocument indexStorage = query.setDocument(FieldName.INDEX_STORAGE);
        writeIndexStorageConfiguration(context, storage, indexStorage);

        // Build the 'query/indexing/backend' nested document ...
        EditableDocument backend = indexing.getOrCreateDocument(FieldName.INDEXING_BACKEND);
        writeIndexingBackendConfiguration(context, storage, backend);

        IndexStorageService service = new IndexStorageService(query);
        ServiceBuilder<IndexStorage> indexBuilder = target.addService(indexStorageServiceName, service);
        indexBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        addControllersAndDependencies(repositoryName, service, newControllers, indexBuilder, target);

        //remove the default service added by "AddRepository"
        context.removeService(indexStorageServiceName);
        newControllers.add(indexBuilder.install());
    }

    protected abstract void writeIndexStorageConfiguration( final OperationContext context,
                                                            final ModelNode storage,
                                                            EditableDocument indexStorage ) throws OperationFailedException;

    protected void writeIndexStorageSpecificIndexingConfiguration( final OperationContext context,
                                                                   final ModelNode storage,
                                                                   EditableDocument indexing ) throws OperationFailedException {
        String format = ModelAttributes.INDEX_FORMAT.resolveModelAttribute(context, storage).asString();
        indexing.set(FieldName.INDEXING_INDEX_FORMAT, format);
    }

    @SuppressWarnings( "unused" )
    protected void writeIndexingBackendConfiguration( final OperationContext context,
                                                      final ModelNode storage,
                                                      EditableDocument backend ) throws OperationFailedException {
        backend.set(FieldName.TYPE, FieldValue.INDEXING_BACKEND_TYPE_LUCENE);
    }

    @SuppressWarnings( "unused" )
    protected void addControllersAndDependencies( String repositoryName,
                                                  IndexStorageService service,
                                                  List<ServiceController<?>> newControllers,
                                                  ServiceBuilder<IndexStorage> builder,
                                                  ServiceTarget target ) throws OperationFailedException {
    }
}
