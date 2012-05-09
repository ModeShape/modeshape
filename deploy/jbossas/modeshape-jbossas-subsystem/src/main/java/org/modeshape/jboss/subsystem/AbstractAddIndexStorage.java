/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.modeshape.jboss.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import java.util.List;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.modeshape.jboss.service.IndexStorageService;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.jcr.RepositoryConfiguration.FieldValue;

public abstract class AbstractAddIndexStorage extends AbstractAddStepHandler {

    protected static final String DATA_DIR_VARIABLE = ModeShapeExtension.DATA_DIR_VARIABLE;

    protected AbstractAddIndexStorage() {
    }

    @Override
    protected void populateModel( ModelNode operation,
                                  ModelNode model ) throws OperationFailedException {
        String opName = operation.get(OP).asString();
        if (ModelKeys.ADD_RAM_INDEX_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.RAM_INDEX_STORAGE, ModelAttributes.RAM_INDEX_STORAGE_ATTRIBUTES);
        } else if (ModelKeys.ADD_LOCAL_FILE_INDEX_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.LOCAL_FILE_INDEX_STORAGE, ModelAttributes.LOCAL_FILE_INDEX_STORAGE_ATTRIBUTES);
        } else if (ModelKeys.ADD_MASTER_FILE_INDEX_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.MASTER_FILE_INDEX_STORAGE, ModelAttributes.MASTER_FILE_INDEX_STORAGE_ATTRIBUTES);
        } else if (ModelKeys.ADD_SLAVE_FILE_INDEX_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.SLAVE_FILE_INDEX_STORAGE, ModelAttributes.SLAVE_FILE_INDEX_STORAGE_ATTRIBUTES);
        } else if (ModelKeys.ADD_CACHE_INDEX_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.CACHE_INDEX_STORAGE, ModelAttributes.CACHE_INDEX_STORAGE_ATTRIBUTES);
        } else if (ModelKeys.ADD_CUSTOM_INDEX_STORAGE.equals(opName)) {
            populate(operation, model, ModelKeys.CUSTOM_INDEX_STORAGE, ModelAttributes.CUSTOM_INDEX_STORAGE_ATTRIBUTES);
        }
    }

    static void populate( ModelNode operation,
                          ModelNode model,
                          String modelName,
                          AttributeDefinition[] attributes ) throws OperationFailedException {
        for (AttributeDefinition attribute : attributes) {
            attribute.validateAndSet(operation, model);
        }
        // Set the binary storage type last (overwriting any value that they've manually added) ...
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
        // final String storageName = pathAddress.getLastElement().getValue();

        // Get the type of the storage (this is required) ...
        // final String storageType = storage.get(ModelKeys.INDEX_STORAGE_TYPE).asString();

        // Build the 'query' document (except for the extractors) ...
        EditableDocument query = Schematic.newDocument();
        String rebuild = ModelAttributes.REBUILD_INDEXES_UPON_STARTUP.resolveModelAttribute(context, storage).asString();
        query.set(FieldName.REBUILD_UPON_STARTUP, rebuild);

        // Build the 'query/indexing' nested document ...
        EditableDocument indexing = query.getOrCreateDocument(FieldName.INDEXING);
        writeIndexingConfiguration(context, storage, indexing);

        // Build the 'query/indexingStorage' nested document ...
        EditableDocument indexStorage = query.getOrCreateDocument(FieldName.INDEX_STORAGE);
        writeIndexStorageConfiguration(context, storage, indexStorage);

        // Build the 'query/indexing/backend' nested document ...
        EditableDocument backend = indexing.getOrCreateDocument(FieldName.INDEXING_BACKEND);
        writeIndexingBackendConfiguration(context, storage, backend);

        IndexStorageService service = new IndexStorageService(repositoryName, query);

        addControllersAndDependencies(repositoryName, service, newControllers, target);

    }

    protected abstract void writeIndexStorageConfiguration( final OperationContext context,
                                                            final ModelNode storage,
                                                            EditableDocument indexStorage ) throws OperationFailedException;

    protected void writeIndexingConfiguration( final OperationContext context,
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
                                                  ServiceTarget target ) throws OperationFailedException {
    }
}
