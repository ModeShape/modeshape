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
 * 
 */
public class VdbLexicon {

    public static class Namespace {
        public static final String URI = "http://www.metamatrix.com/metamodels/VirtualDatabase";
        public static final String PREFIX = "vdb";
    }

    public static final String VIRTUAL_DATABASE = PREFIX + ":virtualDatabase";
    public static final String DESCRIPTION = PREFIX + ":description";
    public static final String VERSION = PREFIX + ":version";
    public static final String ORIGINAL_FILE = PREFIX + ":originalFile";
    public static final String PROPERTIES = PREFIX + ":properties";

    public interface Model {
        String MODEL = PREFIX + ":model";
        String VISIBLE = PREFIX + ":visible";
        String PATH_IN_VDB = PREFIX + ":pathInVdb";
        String SOURCE_TRANSLATOR = PREFIX + ":sourceTranslator";
        String SOURCE_JNDI_NAME = PREFIX + ":sourceJndiName";
        String SOURCE_NAME = PREFIX + ":sourceName";
        String PROPERTIES = PREFIX + ":properties";
    
        public interface Marker {
            String MARKERS = PREFIX + ":markers";
            String MARKER = PREFIX + ":marker";
            String SEVERITY = PREFIX + ":severity";
            String PATH = PREFIX + ":path";
            String MESSAGE = PREFIX + ":message";
        }
    }

    public interface DataRole {
        String DATA_ROLES = PREFIX + ":dataRoles";
        String DATA_ROLE = PREFIX + ":dataRole";
        String NAME = PREFIX + ":name";
        String DESCRIPTION = PREFIX + ":description";
        String ANY_AUTHENTICATED = PREFIX + ":anyAuthenticated";
        String ALLOW_CREATE_TEMP_TABLES = PREFIX + ":allowCreateTemporaryTables";
        String MAPPED_ROLE_NAMES = PREFIX + ":mappedRoleNames";

        public interface Permission {
            String PERMISSIONS = PREFIX + ":permissions";
            String PERMISSION = PREFIX + ":permission";
            String RESOURCE_NAME = PREFIX + ":resourceName";
            String ALLOW_ALTER = PREFIX + ":allowAlter";
            String ALLOW_CREATE = PREFIX + ":allowCreate";
            String ALLOW_DELETE = PREFIX + ":allowDelete";
            String ALLOW_EXECUTE = PREFIX + ":allowExecute";
            String ALLOW_READ = PREFIX + ":allowRead";
            String ALLOW_UPDATE = PREFIX + ":allowUpdate";
        }
    }

    public interface Translator {
        String TRANSLATORS = PREFIX + ":translators";
        String TRANSLATOR = PREFIX + ":translator";
        String NAME = PREFIX + ":name";
        String TYPE = PREFIX + ":type";
        String DESCRIPTION = PREFIX + ":description";
        String PROPERTIES = PREFIX + ":properties";
    }

    public interface Entry {
        String ENTRIES = PREFIX + ":entries";
        String ENTRY = PREFIX + ":entry";
        String PATH = PREFIX + ":path";
        String DESCRIPTION = PREFIX + ":description";
        String PROPERTIES = PREFIX + ":properties";
    }
}
