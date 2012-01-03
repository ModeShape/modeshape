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
package org.modeshape.sequencer.ddl.dialect.mysql;

import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 *
 */
public class MySqlDdlLexicon  extends StandardDdlLexicon {
    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/ddl/mysql/1.0";
        public static final String PREFIX = "mysqlddl";
    }

    // MIXINS
    
    public static final String TYPE_CREATE_DATABASE_STATEMENT 		= Namespace.PREFIX + ":createDatabaseStatement";
    public static final String TYPE_CREATE_DEFINER_STATEMENT 			= Namespace.PREFIX + ":createDefinerStatement";
    public static final String TYPE_CREATE_EVENT_STATEMENT 			= Namespace.PREFIX + ":createEventStatement";
    public static final String TYPE_CREATE_FUNCTION_STATEMENT 		= Namespace.PREFIX + ":createFunctionStatement";
    public static final String TYPE_CREATE_INDEX_STATEMENT 			= Namespace.PREFIX + ":createIndexStatement";
    public static final String TYPE_CREATE_LOGFILE_GROUP_STATEMENT 	= Namespace.PREFIX + ":createFunctionStatement";
    public static final String TYPE_CREATE_PROCEDURE_STATEMENT 		= Namespace.PREFIX + ":createProcedureStatement";
    public static final String TYPE_CREATE_SERVER_STATEMENT 			= Namespace.PREFIX + ":createFunctionStatement";
    public static final String TYPE_CREATE_TABLESPACE_STATEMENT 		= Namespace.PREFIX + ":createTablespaceStatement";
    public static final String TYPE_CREATE_TRIGGER_STATEMENT 			= Namespace.PREFIX + ":createTriggerStatement";

    public static final String TYPE_DROP_DATABASE_STATEMENT 		= Namespace.PREFIX + ":dropDatabaseStatement";
    public static final String TYPE_DROP_EVENT_STATEMENT			= Namespace.PREFIX + ":dropEventStatement";
    public static final String TYPE_DROP_FUNCTION_STATEMENT		= Namespace.PREFIX + ":dropFunctionStatement";
    public static final String TYPE_DROP_INDEX_STATEMENT 			= Namespace.PREFIX + ":dropIndexStatement";
    public static final String TYPE_DROP_LOGFILE_GROUP_STATEMENT	= Namespace.PREFIX + ":dropLogfileGroupStatement";
    public static final String TYPE_DROP_PROCEDURE_STATEMENT 		= Namespace.PREFIX + ":dropProcedureStatement";
    public static final String TYPE_DROP_SERVER_STATEMENT			= Namespace.PREFIX + ":dropServerStatement";
    public static final String TYPE_DROP_TABLESPACE_STATEMENT 	= Namespace.PREFIX + ":dropTablespaceStatement";
    public static final String TYPE_DROP_TRIGGER_STATEMENT 		= Namespace.PREFIX + ":dropTriggerStatement";
    
    public static final String TYPE_ALTER_ALGORITHM_STATEMENT		= Namespace.PREFIX + ":alterAlgorithmStatement";
    public static final String TYPE_ALTER_DATABASE_STATEMENT		= Namespace.PREFIX + ":alterDatabaseStatement";
    public static final String TYPE_ALTER_DEFINER_STATEMENT		= Namespace.PREFIX + ":alterDefinerStatement";
    public static final String TYPE_ALTER_EVENT_STATEMENT			= Namespace.PREFIX + ":alterEventStatement";
    public static final String TYPE_ALTER_FUNCTION_STATEMENT 		= Namespace.PREFIX + ":alterFunctionStatement";
    public static final String TYPE_ALTER_INDEX_STATEMENT 		= Namespace.PREFIX + ":alterIndexStatement";
    public static final String TYPE_ALTER_LOGFILE_GROUP_STATEMENT	= Namespace.PREFIX + ":alterLogfileGroupStatement";
    public static final String TYPE_ALTER_PROCEDURE_STATEMENT 	= Namespace.PREFIX + ":alterProcedureStatement";
    public static final String TYPE_ALTER_SERVER_STATEMENT		= Namespace.PREFIX + ":alterServerStatement";
    public static final String TYPE_ALTER_SCHEMA_STATEMENT		= Namespace.PREFIX + ":alterSchemaStatement";
    public static final String TYPE_ALTER_TABLESPACE_STATEMENT 	= Namespace.PREFIX + ":alterTablespaceStatement";
    public static final String TYPE_ALTER_TRIGGER_STATEMENT 		= Namespace.PREFIX + ":alterTriggerStatement";
    public static final String TYPE_ALTER_VIEW_STATEMENT 			= Namespace.PREFIX + ":alterViewStatement";
    
    public static final String TYPE_RENAME_DATABASE_STATEMENT 	= Namespace.PREFIX + ":renameDatabaseStatement";
    public static final String TYPE_RENAME_SCHEMA_STATEMENT 		= Namespace.PREFIX + ":renameSchemaStatement";
    public static final String TYPE_RENAME_TABLE_STATEMENT 		= Namespace.PREFIX + ":renameTableStatement";

}
