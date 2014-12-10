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
package org.modeshape.sequencer.teiid.lexicon;

import static org.modeshape.sequencer.teiid.lexicon.VdbLexicon.Namespace.PREFIX;

/**
 * Constants associated with the VDB namespace used in reading VDB manifests and writing JCR nodes.
 */
public interface VdbLexicon {

    /**
     * The URI and prefix constants of the VDB namespace.
     */
    public interface Namespace {
        String PREFIX = "vdb";
        String URI = "http://www.metamatrix.com/metamodels/VirtualDatabase";
    }

    /**
     * JCR identifiers relating to VDB manifest data roles.
     */
    public interface DataRole {
        String ALLOW_CREATE_TEMP_TABLES = PREFIX + ":allowCreateTemporaryTables";
        String ANY_AUTHENTICATED = PREFIX + ":anyAuthenticated";
        String DATA_ROLE = PREFIX + ":dataRole";
        String DESCRIPTION = PREFIX + ":description";
        String MAPPED_ROLE_NAMES = PREFIX + ":mappedRoleNames";
        String PERMISSIONS = PREFIX + ":permissions";
        String GRANT_ALL = PREFIX + ":grantAll";

        /**
         * JCR identifiers relating to VDB manifest data role permissions.
         */
        public interface Permission {
            String ALLOW_ALTER = PREFIX + ":allowAlter";
            String ALLOW_CREATE = PREFIX + ":allowCreate";
            String ALLOW_DELETE = PREFIX + ":allowDelete";
            String ALLOW_EXECUTE = PREFIX + ":allowExecute";
            String ALLOW_LANGUAGE = PREFIX + ":allowLanguage";
            String ALLOW_READ = PREFIX + ":allowRead";
            String ALLOW_UPDATE = PREFIX + ":allowUpdate";
            String CONDITIONS = PREFIX + ":conditions";
            String MASKS = PREFIX + ":masks";
            String PERMISSION = PREFIX + ":permission";

            /**
             * JCR identifiers relating to VDB manifest data role permission conditions.
             */
            public interface Condition {
                String CONDITION = PREFIX + ":condition";
                String CONSTRAINT = PREFIX + ":constraint";
            }

            /**
             * JCR identifiers relating to VDB manifest data role permission masks.
             */
            public interface Mask {
                String MASK = PREFIX + ":mask";
                String ORDER = PREFIX + ":order";
            }
        }
    }

    /**
     * JCR identifiers relating to VDB manifest sources.
     */
    public interface Source {
        String NAME = PREFIX + ":sourceName";
        String JNDI_NAME = PREFIX + ":sourceJndiName";
        String SOURCE = PREFIX + ":source";
        String TRANSLATOR = PREFIX + ":sourceTranslator";
    }

    /**
     * JCR identifiers relating to VDB manifest entries.
     */
    public interface Entry {
        String DESCRIPTION = PREFIX + ":description";
        String ENTRY = PREFIX + ":entry";
        String PATH = PREFIX + ":path";
    }

    /**
     * JCR identifiers relating to VDB manifest imported VDBs.
     */
    public interface ImportVdb {
        String IMPORT_DATA_POLICIES = PREFIX + ":importDataPolicies";
        String IMPORT_VDB = PREFIX + ":importVdb";
        String VERSION = PREFIX + ":version";
    }

    /**
     * Constants associated with the VDB namespace that identify VDB manifest identifiers.
     */
    public interface ManifestIds {
        String ALLOW_ALTER = "allow-alter";
        String ALLOW_CREATE = "allow-create";
        String ALLOW_CREATE_TEMP_TABLES = "allow-create-temporary-tables";
        String ALLOW_DELETE = "allow-delete";
        String ALLOW_EXECUTE = "allow-execute";
        String ALLOW_LANGUAGE = "allow-language";
        String ALLOW_READ = "allow-read";
        String ALLOW_UPDATE = "allow-update";
        String ANY_AUTHENTICATED = "any-authenticated";
        String BUILT_IN = "builtIn";
        String CHECKSUM = "checksum";
        String CONDITION = "condition";
        String CONNECTION_TYPE = "connection-type";
        String CONSTRAINT = "constraint";
        String DATA_ROLE = "data-role";
        String DESCRIPTION = "description";
        String ENTRY = "entry";
        String GRANT_ALL = "grant-all";
        String IMPORTS = "imports";
        String IMPORT_DATA_POLICIES = "import-data-policies";
        String IMPORT_VDB = "import-vdb";
        String INDEX_NAME = "indexName";
        String JNDI_NAME = "connection-jndi-name";
        String MAPPED_ROLE_NAME = "mapped-role-name";
        String MASK = "mask";
        String METADATA = "metadata";
        String MODEL = "model";
        String NAME = "name";
        String ORDER = "order";
        String PATH = "path";
        String PERMISSION = "permission";
        String PREVIEW = "preview";
        String PROPERTY = "property";
        String RESOURCE_NAME = "resource-name";
        String SEVERITY = "severity";
        String SOURCE = "source";
        String TRANSLATOR = "translator";
        String TRANSLATOR_NAME = "translator-name";
        String TYPE = "type";
        String VALIDATION_ERROR = "validation-error";
        String VALUE = "value";
        String VDB = "vdb";
        String VERSION = "version";
        String VISIBLE = "visible";
    }

    /**
     * JCR identifiers relating to VDB manifest models.
     */
    public interface Model {
        String BUILT_IN = PREFIX + ":builtIn";
        String CHECKSUM = PREFIX + ":checksum";
        String DESCRIPTION = PREFIX + ":description";
        String INDEX_NAME = PREFIX + ":indexName";
        String MARKERS = PREFIX + ":markers";
        String METADATA_TYPE = PREFIX + ":metadataType";
        String MODEL = PREFIX + ":model";
        String MODEL_DEFINITION = PREFIX + ":modelDefinition";
        String PATH_IN_VDB = PREFIX + ":pathInVdb";
        String SOURCE_JNDI_NAME = PREFIX + ":sourceJndiName";
        String SOURCE_NAME = PREFIX + ":sourceName";
        String SOURCE_TRANSLATOR = PREFIX + ":sourceTranslator";
        String VISIBLE = PREFIX + ":visible";

        /**
         * JCR identifiers relating to VDB manifest model validation error markers.
         */
        public interface Marker {
            String MARKER = PREFIX + ":marker";
            String MESSAGE = PREFIX + ":message";
            String PATH = PREFIX + ":path";
            String SEVERITY = PREFIX + ":severity";
        }
    }

    /**
     * JCR identifiers relating to VDB manifest translators.
     */
    public interface Translator {
        String DESCRIPTION = PREFIX + ":description";
        String TRANSLATOR = PREFIX + ":translator";
        String TYPE = PREFIX + ":type";
    }

    /**
     * JCR identifiers relating to the VDB manifest.
     */
    public interface Vdb {
        String CONNECTION_TYPE = PREFIX + ":connectionType";
        String DATA_ROLES = PREFIX + ":dataRoles";
        String DECLARATIVE_MODEL = PREFIX + ":declarativeModel";
        String DESCRIPTION = PREFIX + ":description";
        String ENTRIES = PREFIX + ":entries";
        String IMPORT_VDBS = PREFIX + ":importVdbs";
        String MODEL = PREFIX + ":model";
        String NAME = PREFIX + ":name";
        String ORIGINAL_FILE = PREFIX + ":originalFile";
        String PREVIEW = PREFIX + ":preview";
        String SOURCES = PREFIX + ":sources";
        String TRANSLATORS = PREFIX + ":translators";
        String VERSION = PREFIX + ":version";
        String VIRTUAL_DATABASE = PREFIX + ":virtualDatabase";
    }
}
