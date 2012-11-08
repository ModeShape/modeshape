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

        /**
         * JCR identifiers relating to VDB manifest data role permissions.
         */
        public interface Permission {
            String ALLOW_ALTER = PREFIX + ":allowAlter";
            String ALLOW_CREATE = PREFIX + ":allowCreate";
            String ALLOW_DELETE = PREFIX + ":allowDelete";
            String ALLOW_EXECUTE = PREFIX + ":allowExecute";
            String ALLOW_READ = PREFIX + ":allowRead";
            String ALLOW_UPDATE = PREFIX + ":allowUpdate";
            String PERMISSION = PREFIX + ":permission";
        }
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
        String ALLOW_READ = "allow-read";
        String ALLOW_UPDATE = "allow-update";
        String ANY_AUTHENTICATED = "any-authenticated";
        String BUILT_IN = "builtIn";
        String CHECKSUM = "checksum";
        String DATA_ROLE = "data-role";
        String DESCRIPTION = "description";
        String ENTRY = "entry";
        String IMPORTS = "imports";
        String IMPORT_DATA_POLICIES = "import-data-policies";
        String IMPORT_VDB = "import-vdb";
        String INDEX_NAME = "indexName";
        String JNDI_NAME = "connection-jndi-name";
        String MAPPED_ROLE_NAME = "mapped-role-name";
        String METADATA = "metadata";
        String MODEL = "model";
        String NAME = "name";
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
        String DATA_ROLES = PREFIX + ":dataRoles";
        String DECLARATIVE_MODEL = PREFIX + ":declarativeModel";
        String DESCRIPTION = PREFIX + ":description";
        String ENTRIES = PREFIX + ":entries";
        String IMPORT_VDBS = PREFIX + ":importVdbs";
        String MODEL = PREFIX + ":model";
        String ORIGINAL_FILE = PREFIX + ":originalFile";
        String PREVIEW = PREFIX + ":preview";
        String TRANSLATORS = PREFIX + ":translators";
        String VERSION = PREFIX + ":version";
        String VIRTUAL_DATABASE = PREFIX + ":virtualDatabase";
    }
}
