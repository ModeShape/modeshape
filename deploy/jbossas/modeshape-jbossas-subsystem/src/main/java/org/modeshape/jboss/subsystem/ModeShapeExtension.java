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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import java.util.List;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

public class ModeShapeExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "modeshape"; //$NON-NLS-1$
    static final String DATA_DIR_VARIABLE = "jboss.server.data.dir"; //$NON-NLS-1$

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;

    private static final PathElement repositoryPath = PathElement.pathElement(ModelKeys.REPOSITORY);
    private static final PathElement sequencerPath = PathElement.pathElement(ModelKeys.SEQUENCER);
    private static final PathElement indexStoragePath = PathElement.pathElement(ModelKeys.INDEX_STORAGE,
                                                                                ModelKeys.INDEX_STORAGE_NAME);
    private static final PathElement binaryStoragePath = PathElement.pathElement(ModelKeys.BINARY_STORAGE,
                                                                                 ModelKeys.BINARY_STORAGE_NAME);

    @Override
    public void initialize( ExtensionContext context ) {
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME,
                                                                             MANAGEMENT_API_MAJOR_VERSION,
                                                                             MANAGEMENT_API_MINOR_VERSION);

        // LogManager.setLogListener(new Log4jListener());

        registration.registerXMLElementWriter(new ModeShapeSubsystemXMLWriter());

        // ModeShape system, with children repositories.
        final ManagementResourceRegistration modeShapeSubsystem = registration.registerSubsystemModel(ModeShapeSubsystemProviders.SUBSYSTEM);
        modeShapeSubsystem.registerOperationHandler(ADD,
                                                    AddModeShapeSubsystem.INSTANCE,
                                                    ModeShapeSubsystemProviders.SUBSYSTEM_ADD,
                                                    false);
        modeShapeSubsystem.registerOperationHandler(DESCRIBE,
                                                    DescribeModeShapeSubsystem.INSTANCE,
                                                    ModeShapeSubsystemProviders.SUBSYSTEM_DESCRIBE,
                                                    false);

        // Repository submodel
        final ManagementResourceRegistration repositorySubmodel = modeShapeSubsystem.registerSubModel(repositoryPath,
                                                                                                      ModeShapeSubsystemProviders.REPOSITORY);
        repositorySubmodel.registerOperationHandler(ADD,
                                                    AddRepository.INSTANCE,
                                                    ModeShapeSubsystemProviders.REPOSITORY_ADD,
                                                    false);
        repositorySubmodel.registerOperationHandler(REMOVE,
                                                    RemoveRepository.INSTANCE,
                                                    ModeShapeSubsystemProviders.REPOSITORY_REMOVE,
                                                    false);
        RepositoryWriteAttributeHandler.INSTANCE.registerAttributes(repositorySubmodel);

        // Sequencer submodel
        final ManagementResourceRegistration sequencerSubmodel = repositorySubmodel.registerSubModel(sequencerPath,
                                                                                                     ModeShapeSubsystemProviders.SEQUENCER);
        sequencerSubmodel.registerOperationHandler(ADD, AddSequencer.INSTANCE, ModeShapeSubsystemProviders.SEQUENCER_ADD, false);
        sequencerSubmodel.registerOperationHandler(REMOVE,
                                                   RemoveSequencer.INSTANCE,
                                                   ModeShapeSubsystemProviders.SEQUENCER_REMOVE,
                                                   false);
        SequencerWriteAttributeHandler.INSTANCE.registerAttributes(sequencerSubmodel);

        // Index storage submodel
        final ManagementResourceRegistration indexStorageSubmodel = repositorySubmodel.registerSubModel(indexStoragePath,
                                                                                                        ModeShapeSubsystemProviders.INDEX_STORAGE);
        indexStorageSubmodel.registerOperationHandler(ModelKeys.ADD_RAM_INDEX_STORAGE,
                                                      AddRamIndexStorage.INSTANCE,
                                                      ModeShapeSubsystemProviders.RAM_INDEX_STORAGE_ADD,
                                                      false);
        indexStorageSubmodel.registerOperationHandler(ModelKeys.ADD_LOCAL_FILE_INDEX_STORAGE,
                                                      AddLocalFileSystemIndexStorage.INSTANCE,
                                                      ModeShapeSubsystemProviders.LOCAL_FILE_INDEX_STORAGE_ADD,
                                                      false);
        indexStorageSubmodel.registerOperationHandler(ModelKeys.ADD_MASTER_FILE_INDEX_STORAGE,
                                                      AddMasterFileSystemIndexStorage.INSTANCE,
                                                      ModeShapeSubsystemProviders.MASTER_FILE_INDEX_STORAGE_ADD,
                                                      false);
        indexStorageSubmodel.registerOperationHandler(ModelKeys.ADD_SLAVE_FILE_INDEX_STORAGE,
                                                      AddSlaveFileSystemIndexStorage.INSTANCE,
                                                      ModeShapeSubsystemProviders.SLAVE_FILE_INDEX_STORAGE_ADD,
                                                      false);
        indexStorageSubmodel.registerOperationHandler(ModelKeys.ADD_CACHE_INDEX_STORAGE,
                                                      AddCacheIndexStorage.INSTANCE,
                                                      ModeShapeSubsystemProviders.CACHE_INDEX_STORAGE_ADD,
                                                      false);
        indexStorageSubmodel.registerOperationHandler(ModelKeys.ADD_CUSTOM_INDEX_STORAGE,
                                                      AddCustomIndexStorage.INSTANCE,
                                                      ModeShapeSubsystemProviders.CUSTOM_INDEX_STORAGE_ADD,
                                                      false);
        indexStorageSubmodel.registerOperationHandler(ModelKeys.REMOVE_INDEX_STORAGE,
                                                      RemoveIndexStorage.INSTANCE,
                                                      ModeShapeSubsystemProviders.INDEX_STORAGE_REMOVE,
                                                      false);
        IndexStorageWriteAttributeHandler.INSTANCE.registerAttributes(indexStorageSubmodel);

        // Binary storage submodel
        final ManagementResourceRegistration binaryStorageSubmodel = repositorySubmodel.registerSubModel(binaryStoragePath,
                                                                                                         ModeShapeSubsystemProviders.BINARY_STORAGE);
        binaryStorageSubmodel.registerOperationHandler(ModelKeys.ADD_FILE_BINARY_STORAGE,
                                                       AddFileBinaryStorage.INSTANCE,
                                                       ModeShapeSubsystemProviders.FILE_BINARY_STORAGE_ADD,
                                                       false);
        binaryStorageSubmodel.registerOperationHandler(ModelKeys.ADD_DB_BINARY_STORAGE,
                                                       AddDatabaseBinaryStorage.INSTANCE,
                                                       ModeShapeSubsystemProviders.DB_BINARY_STORAGE_ADD,
                                                       false);
        binaryStorageSubmodel.registerOperationHandler(ModelKeys.ADD_CACHE_BINARY_STORAGE,
                                                       AddCacheBinaryStorage.INSTANCE,
                                                       ModeShapeSubsystemProviders.CACHE_BINARY_STORAGE_ADD,
                                                       false);
        binaryStorageSubmodel.registerOperationHandler(ModelKeys.ADD_CUSTOM_BINARY_STORAGE,
                                                       AddCustomBinaryStorage.INSTANCE,
                                                       ModeShapeSubsystemProviders.CUSTOM_BINARY_STORAGE_ADD,
                                                       false);
        binaryStorageSubmodel.registerOperationHandler(ModelKeys.REMOVE_BINARY_STORAGE,
                                                       RemoveBinaryStorage.INSTANCE,
                                                       ModeShapeSubsystemProviders.BINARY_STORAGE_REMOVE,
                                                       false);
        BinaryStorageWriteAttributeHandler.INSTANCE.registerAttributes(binaryStorageSubmodel);

    }

    @Override
    public void initializeParsers( ExtensionParsingContext context ) {
        // Register all of the available parsers ...
        for (Namespace namespace : Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), reader);
            }
        }
    }

}
