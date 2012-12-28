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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import java.util.List;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

public class ModeShapeExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "modeshape"; //$NON-NLS-1$
    static final String DATA_DIR_VARIABLE = "jboss.server.data.dir"; //$NON-NLS-1$

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;

    private static final String RESOURCE_NAME = ModeShapeExtension.class.getPackage().getName() + ".LocalDescriptions";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    static final PathElement REPOSITORY_PATH = PathElement.pathElement(ModelKeys.REPOSITORY);
    static final PathElement SEQUENCER_PATH = PathElement.pathElement(ModelKeys.SEQUENCER);
    static final PathElement SOURCE_PATH = PathElement.pathElement(ModelKeys.SOURCE);
    static final PathElement TEXT_EXTRACTOR_PATH = PathElement.pathElement(ModelKeys.TEXT_EXTRACTOR);
    static final PathElement AUTHENTICATOR_PATH = PathElement.pathElement(ModelKeys.AUTHENTICATOR);

    // Index storage PathElements
    static final PathElement INDEX_STORAGE_PATH = PathElement.pathElement(ModelKeys.CONFIGURATION, ModelKeys.INDEX_STORAGE);
    static final PathElement RAM_INDEX_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE, ModelKeys.RAM_INDEX_STORAGE);
    static final PathElement LOCAL_FILE_INDEX_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE,
                                                                                     ModelKeys.LOCAL_FILE_INDEX_STORAGE);
    static final PathElement MASTER_FILE_INDEX_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE,
                                                                                      ModelKeys.MASTER_FILE_INDEX_STORAGE);
    static final PathElement SLAVE_FILE_INDEX_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE,
                                                                                     ModelKeys.SLAVE_FILE_INDEX_STORAGE);
    static final PathElement CACHE_INDEX_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE,
                                                                                ModelKeys.CACHE_INDEX_STORAGE);
    static final PathElement CUSTOM_INDEX_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE,
                                                                                 ModelKeys.CUSTOM_INDEX_STORAGE);

    // Binary storage PathElements
    static final PathElement BINARY_STORAGE_PATH = PathElement.pathElement(ModelKeys.CONFIGURATION, ModelKeys.BINARY_STORAGE);
    static final PathElement FILE_BINARY_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE,
                                                                                ModelKeys.FILE_BINARY_STORAGE);
    static final PathElement CACHE_BINARY_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE,
                                                                                 ModelKeys.CACHE_BINARY_STORAGE);
    static final PathElement DB_BINARY_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE, ModelKeys.DB_BINARY_STORAGE);
    static final PathElement CUSTOM_BINARY_STORAGE_PATH = PathElement.pathElement(ModelKeys.STORAGE_TYPE,
                                                                                  ModelKeys.CUSTOM_BINARY_STORAGE);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver( final String... keyPrefix ) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME,
                                                       ModeShapeExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize( ExtensionContext context ) {
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME,
                                                                             MANAGEMENT_API_MAJOR_VERSION,
                                                                             MANAGEMENT_API_MINOR_VERSION);

        registration.registerXMLElementWriter(new ModeShapeSubsystemXMLWriter());
        // ModeShape system, with children repositories.
        final ManagementResourceRegistration modeShapeSubsystem = registration.registerSubsystemModel(ModeShapeRootResource.INSTANCE);
        // Repository submodel
        final ManagementResourceRegistration repositorySubmodel = modeShapeSubsystem.registerSubModel(ModeShapeRepositoryResource.INSTANCE);

        // Sequencer submodel
        repositorySubmodel.registerSubModel(ModeShapeSequencerResource.INSTANCE);

        // External sources submodel
        repositorySubmodel.registerSubModel(ModeShapeSourceResource.INSTANCE);

        // Text extractor submodel
        repositorySubmodel.registerSubModel(ModeShapeTextExtractorResource.INSTANCE);

        // Authenticator submodel
        repositorySubmodel.registerSubModel(ModeShapeAuthenticatorResource.INSTANCE);

        // Index storage submodel
        final ManagementResourceRegistration indexStorageSubmodel = repositorySubmodel.registerSubModel(ModeShapeIndexStorageResource.INSTANCE);
        indexStorageSubmodel.registerSubModel(ModeShapeRamIndexStorageResource.INSTANCE);
        indexStorageSubmodel.registerSubModel(ModeShapeMasterFileIndexStorageResource.INSTANCE);
        indexStorageSubmodel.registerSubModel(ModeShapeSlaveFileIndexStorageResource.INSTANCE);
        indexStorageSubmodel.registerSubModel(ModeShapeCacheIndexStorageResource.INSTANCE);
        indexStorageSubmodel.registerSubModel(ModeShapeCustomIndexStorageResource.INSTANCE);
        indexStorageSubmodel.registerSubModel(ModeShapeLocalFileIndexStorageResource.INSTANCE);

        // Binary storage submodel and type submodels
        final ManagementResourceRegistration binaryStorageSubmodel = repositorySubmodel.registerSubModel(ModeShapeBinaryStorageResource.INSTANCE);
        binaryStorageSubmodel.registerSubModel(ModeShapeFileBinaryStorageResource.INSTANCE);
        binaryStorageSubmodel.registerSubModel(ModeShapeCacheBinaryStorageResource.INSTANCE);
        binaryStorageSubmodel.registerSubModel(ModeShapeDatabaseBinaryStorageResource.INSTANCE);
        binaryStorageSubmodel.registerSubModel(ModeShapeCustomBinaryStorageResource.INSTANCE);

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
