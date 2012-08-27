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
 * Constants used during sequencing the VDB manifest.
 */
public interface VdbLexicon {

    public interface DataRole {
        String DATA_ROLE = PREFIX + ":dataRole";
        String DESCRIPTION = PREFIX + ":description";
        String ANY_AUTHENTICATED = PREFIX + ":anyAuthenticated";
        String ALLOW_CREATE_TEMP_TABLES = PREFIX + ":allowCreateTemporaryTables";
        String MAPPED_ROLE_NAMES = PREFIX + ":mappedRoleNames";
        String PERMISSIONS = PREFIX + ":permissions";

        public interface Permission {
            String PERMISSION = PREFIX + ":permission";
            String ALLOW_ALTER = PREFIX + ":allowAlter";
            String ALLOW_CREATE = PREFIX + ":allowCreate";
            String ALLOW_DELETE = PREFIX + ":allowDelete";
            String ALLOW_EXECUTE = PREFIX + ":allowExecute";
            String ALLOW_READ = PREFIX + ":allowRead";
            String ALLOW_UPDATE = PREFIX + ":allowUpdate";
        }
    }

    public interface Entry {
        String ENTRY = PREFIX + ":entry";
        String PATH = PREFIX + ":path";
        String DESCRIPTION = PREFIX + ":description";
    }

    public interface ManifestIds {
        String ANY_AUTHENTICATED = "any-authenticated";
        String ALLOW_ALTER = "allow-alter";
        String ALLOW_CREATE = "allow-create";
        String ALLOW_CREATE_TEMP_TABLES = "allow-create-temporary-tables";
        String ALLOW_DELETE = "allow-delete";
        String ALLOW_EXECUTE = "allow-execute";
        String ALLOW_READ = "allow-read";
        String ALLOW_UPDATE = "allow-update";
        String BUILT_IN = "builtIn";
        String CHECKSUM = "checksum";
        String DATA_ROLE = "data-role";
        String DESCRIPTION = "description";
        String ENTRY = "entry";
        String IMPORTS = "imports";
        String JNDI_NAME = "connection-jndi-name";
        String MAPPED_ROLE_NAME = "mapped-role-name";
        String MODEL = "model";
        String NAME = "name";
        String PATH = "path";
        String PERMISSION = "permission";
        String RESOURCE_NAME = "resource-name";
        String PREVIEW = "preview";
        String PROPERTY = "property";
        String SEVERITY = "severity";
        String SOURCE = "source";
        String TRANSLATOR = "translator";
        String TRANSLATOR_NAME = "translator-name";
        String TYPE = "type";
        String VDB = "vdb";
        String VALIDATION_ERROR = "validation-error";
        String VALUE = "value";
        String VERSION = "version";
        String VISIBLE = "visible";
    }

    public interface Model {
        String BUILT_IN = PREFIX + ":builtIn";
        String CHECKSUM = PREFIX + ":checksum";
        String DESCRIPTION = PREFIX + ":description";
        String MARKERS = PREFIX + ":markers";
        String MODEL = PREFIX + ":model";
        String PATH_IN_VDB = PREFIX + ":pathInVdb";
        String SOURCE_TRANSLATOR = PREFIX + ":sourceTranslator";
        String SOURCE_JNDI_NAME = PREFIX + ":sourceJndiName";
        String SOURCE_NAME = PREFIX + ":sourceName";
        String TYPE = PREFIX + ":type";
        String VISIBLE = PREFIX + ":visible";

        public interface Marker {
            String MARKER = PREFIX + ":marker";
            String SEVERITY = PREFIX + ":severity";
            String PATH = PREFIX + ":path";
            String MESSAGE = PREFIX + ":message";
        }
    }

    public interface Namespace {
        String URI = "http://www.metamatrix.com/metamodels/VirtualDatabase";
        String PREFIX = "vdb";
    }

    public interface Translator {
        String TRANSLATOR = PREFIX + ":translator";
        String TYPE = PREFIX + ":type";
        String DESCRIPTION = PREFIX + ":description";
    }

    public interface Vdb {
        String VIRTUAL_DATABASE = PREFIX + ":virtualDatabase";
        String DESCRIPTION = PREFIX + ":description";
        String VERSION = PREFIX + ":version";
        String PREVIEW = PREFIX + ":preview";
        String ORIGINAL_FILE = PREFIX + ":originalFile";
        String TRANSLATORS = PREFIX + ":translators";
        String DATA_ROLES = PREFIX + ":dataRoles";
        String ENTRIES = PREFIX + ":entries";
        String MODEL = PREFIX + ":model";
    }
}
