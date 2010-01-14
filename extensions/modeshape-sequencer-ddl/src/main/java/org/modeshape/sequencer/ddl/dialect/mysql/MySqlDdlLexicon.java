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

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;
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
    
    public static final Name TYPE_CREATE_DATABASE_STATEMENT 		= new BasicName(Namespace.URI, "createDatabaseStatement");
    public static final Name TYPE_CREATE_DEFINER_STATEMENT 			= new BasicName(Namespace.URI, "createDefinerStatement");
    public static final Name TYPE_CREATE_EVENT_STATEMENT 			= new BasicName(Namespace.URI, "createEventStatement");
    public static final Name TYPE_CREATE_FUNCTION_STATEMENT 		= new BasicName(Namespace.URI, "createFunctionStatement");
    public static final Name TYPE_CREATE_INDEX_STATEMENT 			= new BasicName(Namespace.URI, "createIndexStatement");
    public static final Name TYPE_CREATE_LOGFILE_GROUP_STATEMENT 	= new BasicName(Namespace.URI, "createFunctionStatement");
    public static final Name TYPE_CREATE_PROCEDURE_STATEMENT 		= new BasicName(Namespace.URI, "createProcedureStatement");
    public static final Name TYPE_CREATE_SERVER_STATEMENT 			= new BasicName(Namespace.URI, "createFunctionStatement");
    public static final Name TYPE_CREATE_TABLESPACE_STATEMENT 		= new BasicName(Namespace.URI, "createTablespaceStatement");
    public static final Name TYPE_CREATE_TRIGGER_STATEMENT 			= new BasicName(Namespace.URI, "createTriggerStatement");
    
    
    public static final Name TYPE_DROP_DATABASE_STATEMENT 		= new BasicName(Namespace.URI, "dropDatabaseStatement");
    public static final Name TYPE_DROP_EVENT_STATEMENT			= new BasicName(Namespace.URI, "dropEventStatement");
    public static final Name TYPE_DROP_FUNCTION_STATEMENT		= new BasicName(Namespace.URI, "dropFunctionStatement");
    public static final Name TYPE_DROP_INDEX_STATEMENT 			= new BasicName(Namespace.URI, "dropIndexStatement");
    public static final Name TYPE_DROP_LOGFILE_GROUP_STATEMENT	= new BasicName(Namespace.URI, "dropLogfileGroupStatement");
    public static final Name TYPE_DROP_PROCEDURE_STATEMENT 		= new BasicName(Namespace.URI, "dropProcedureStatement");
    public static final Name TYPE_DROP_SERVER_STATEMENT			= new BasicName(Namespace.URI, "dropServerStatement");
    public static final Name TYPE_DROP_TABLESPACE_STATEMENT 	= new BasicName(Namespace.URI, "dropTablespaceStatement");
    public static final Name TYPE_DROP_TRIGGER_STATEMENT 		= new BasicName(Namespace.URI, "dropTriggerStatement");
    
    public static final Name TYPE_ALTER_ALGORITHM_STATEMENT		= new BasicName(Namespace.URI, "alterAlgorithmStatement");
    public static final Name TYPE_ALTER_DATABASE_STATEMENT		= new BasicName(Namespace.URI, "alterDatabaseStatement");
    public static final Name TYPE_ALTER_DEFINER_STATEMENT		= new BasicName(Namespace.URI, "alterDefinerStatement");
    public static final Name TYPE_ALTER_EVENT_STATEMENT			= new BasicName(Namespace.URI, "alterEventStatement");
    public static final Name TYPE_ALTER_FUNCTION_STATEMENT 		= new BasicName(Namespace.URI, "alterFunctionStatement");
    public static final Name TYPE_ALTER_INDEX_STATEMENT 		= new BasicName(Namespace.URI, "alterIndexStatement");
    public static final Name TYPE_ALTER_LOGFILE_GROUP_STATEMENT	= new BasicName(Namespace.URI, "alterLogfileGroupStatement");
    public static final Name TYPE_ALTER_PROCEDURE_STATEMENT 	= new BasicName(Namespace.URI, "alterProcedureStatement");
    public static final Name TYPE_ALTER_SERVER_STATEMENT		= new BasicName(Namespace.URI, "alterServerStatement");
    public static final Name TYPE_ALTER_SCHEMA_STATEMENT		= new BasicName(Namespace.URI, "alterSchemaStatement");
    public static final Name TYPE_ALTER_TABLESPACE_STATEMENT 	= new BasicName(Namespace.URI, "alterTablespaceStatement");
    public static final Name TYPE_ALTER_TRIGGER_STATEMENT 		= new BasicName(Namespace.URI, "alterTriggerStatement");
    public static final Name TYPE_ALTER_VIEW_STATEMENT 			= new BasicName(Namespace.URI, "alterViewStatement");
    
    public static final Name TYPE_RENAME_DATABASE_STATEMENT 	= new BasicName(Namespace.URI, "renameDatabaseStatement");
    public static final Name TYPE_RENAME_SCHEMA_STATEMENT 		= new BasicName(Namespace.URI, "renameSchemaStatement");
    public static final Name TYPE_RENAME_TABLE_STATEMENT 		= new BasicName(Namespace.URI, "renameTableStatement");

}
