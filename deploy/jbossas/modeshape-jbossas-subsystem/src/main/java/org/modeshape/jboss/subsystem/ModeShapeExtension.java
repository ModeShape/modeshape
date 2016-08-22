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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import java.util.List;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

public class ModeShapeExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "modeshape"; //$NON-NLS-1$
    public static final String JBOSS_DATA_DIR_VARIABLE = "jboss.server.data.dir"; //$NON-NLS-1$
    public static final String JBOSS_CONFIG_DIR_VARIABLE = "jboss.server.config.dir";

    public static final class ModuleID {
        public static final String MAIN = "org.modeshape"; //$NON-NLS-1$
    }

    protected static final int MANAGEMENT_API_MAJOR_VERSION = 3;
    protected static final int MANAGEMENT_API_MINOR_VERSION = 0;

    private static final String RESOURCE_NAME = ModeShapeExtension.class.getPackage().getName() + ".LocalDescriptions";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    static final PathElement WEBAPP_PATH = PathElement.pathElement(ModelKeys.WEBAPP);
    static final PathElement REPOSITORY_PATH = PathElement.pathElement(ModelKeys.REPOSITORY);
    static final PathElement SEQUENCER_PATH = PathElement.pathElement(ModelKeys.SEQUENCER);
    static final PathElement INDEX_PROVIDER_PATH = PathElement.pathElement(ModelKeys.INDEX_PROVIDER);
    static final PathElement DB_PERSISTENCE_PATH = PathElement.pathElement(Attribute.DB_PERSISTENCE.getLocalName());
    static final PathElement FS_PERSISTENCE_PATH = PathElement.pathElement(Attribute.FS_PERSISTENCE.getLocalName());
    static final PathElement INDEX_DEFINITION_PATH = PathElement.pathElement(ModelKeys.INDEX);
    static final PathElement SOURCE_PATH = PathElement.pathElement(ModelKeys.SOURCE);
    static final PathElement TEXT_EXTRACTOR_PATH = PathElement.pathElement(ModelKeys.TEXT_EXTRACTOR);
    static final PathElement AUTHENTICATOR_PATH = PathElement.pathElement(ModelKeys.AUTHENTICATOR);

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
                                                                             ModelVersion.create(MANAGEMENT_API_MAJOR_VERSION,
                                                                                                 MANAGEMENT_API_MINOR_VERSION));

        registration.registerXMLElementWriter(new ModeShapeSubsystemXMLWriter());
        // ModeShape system, with children repositories.
        final ManagementResourceRegistration modeShapeSubsystem = registration.registerSubsystemModel(ModeShapeRootResource.INSTANCE);

        // Webapp submodel
        modeShapeSubsystem.registerSubModel(ModeShapeWebAppResource.INSTANCE);

        // Repository submodel
        final ManagementResourceRegistration repositorySubmodel = modeShapeSubsystem.registerSubModel(ModeShapeRepositoryResource.INSTANCE);
        
        // Persistence submodel
        repositorySubmodel.registerSubModel(ModeShapeDbPersistenceResource.INSTANCE);
        repositorySubmodel.registerSubModel(ModeShapeFilePersistenceResource.INSTANCE);

        // Sequencer submodel
        repositorySubmodel.registerSubModel(ModeShapeSequencerResource.INSTANCE);

        // Index provider submodel
        repositorySubmodel.registerSubModel(ModeShapeIndexProviderResource.INSTANCE);

        // Index definition submodel
        repositorySubmodel.registerSubModel(ModeShapeIndexDefinitionResource.INSTANCE);

        // External sources submodel
        repositorySubmodel.registerSubModel(ModeShapeSourceResource.INSTANCE);

        // Text extractor submodel
        repositorySubmodel.registerSubModel(ModeShapeTextExtractorResource.INSTANCE);

        // Authenticator submodel
        repositorySubmodel.registerSubModel(ModeShapeAuthenticatorResource.INSTANCE);

        // Binary storage submodel and type submodels
        final ManagementResourceRegistration binaryStorageSubmodel = repositorySubmodel.registerSubModel(ModeShapeBinaryStorageResource.INSTANCE);
        binaryStorageSubmodel.registerSubModel(ModeShapeFileBinaryStorageResource.DEFAULT);
        binaryStorageSubmodel.registerSubModel(ModeShapeDatabaseBinaryStorageResource.DEFAULT);
        binaryStorageSubmodel.registerSubModel(ModeShapeCustomBinaryStorageResource.DEFAULT);
        binaryStorageSubmodel.registerSubModel(ModeShapeTransientBinaryStorageResource.DEFAULT);
        binaryStorageSubmodel.registerSubModel(ModeShapeCassandraBinaryStorageResource.DEFAULT);
        binaryStorageSubmodel.registerSubModel(ModeShapeMongoBinaryStorageResource.DEFAULT);
        binaryStorageSubmodel.registerSubModel(ModeShapeS3BinaryStorageResource.DEFAULT);

        ManagementResourceRegistration compositeStorageSubmodel = binaryStorageSubmodel.registerSubModel(ModeShapeCompositeBinaryStorageResource.INSTANCE);
        compositeStorageSubmodel.registerSubModel(ModeShapeFileBinaryStorageResource.NESTED);
        compositeStorageSubmodel.registerSubModel(ModeShapeDatabaseBinaryStorageResource.NESTED);
        compositeStorageSubmodel.registerSubModel(ModeShapeCustomBinaryStorageResource.NESTED);
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
