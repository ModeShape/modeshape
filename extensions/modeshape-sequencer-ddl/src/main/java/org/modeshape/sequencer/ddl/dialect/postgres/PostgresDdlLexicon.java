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
package org.modeshape.sequencer.ddl.dialect.postgres;

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 *
 */
public class PostgresDdlLexicon  extends StandardDdlLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/ddl/postgres/1.0";
        public static final String PREFIX = "postgresddl";
    }
    
    public static final Name TYPE_CREATE_AGGREGATE_STATEMENT 			= new BasicName(Namespace.URI, "createAggregateStatement");
    public static final Name TYPE_CREATE_CAST_STATEMENT 				= new BasicName(Namespace.URI, "createCastStatement");
    public static final Name TYPE_CREATE_CONSTRAINT_TRIGGER_STATEMENT 	= new BasicName(Namespace.URI, "createConstraintTriggerStatement");
    public static final Name TYPE_CREATE_CONVERSION_STATEMENT 			= new BasicName(Namespace.URI, "createConversionStatement");
    public static final Name TYPE_CREATE_DATABASE_STATEMENT 			= new BasicName(Namespace.URI, "createDatabaseStatement");
    public static final Name TYPE_CREATE_FOREIGN_DATA_WRAPPER_STATEMENT = new BasicName(Namespace.URI, "createForeignDataWrapperStatement");
    public static final Name TYPE_CREATE_FUNCTION_STATEMENT 			= new BasicName(Namespace.URI, "createFunctionStatement");
    public static final Name TYPE_CREATE_GROUP_STATEMENT 				= new BasicName(Namespace.URI, "createGroupStatement");
    public static final Name TYPE_CREATE_INDEX_STATEMENT 				= new BasicName(Namespace.URI, "createIndexStatement");
    public static final Name TYPE_CREATE_LANGUAGE_STATEMENT 			= new BasicName(Namespace.URI, "createLanguageStatement");
    public static final Name TYPE_CREATE_OPERATOR_STATEMENT 			= new BasicName(Namespace.URI, "createOperatorStatement");
    public static final Name TYPE_CREATE_ROLE_STATEMENT 				= new BasicName(Namespace.URI, "createRoleStatement");
    public static final Name TYPE_CREATE_RULE_STATEMENT 				= new BasicName(Namespace.URI, "createRuleStatement");
    public static final Name TYPE_CREATE_SEQUENCE_STATEMENT 			= new BasicName(Namespace.URI, "createSequenceStatement");
    public static final Name TYPE_CREATE_SERVER_STATEMENT 				= new BasicName(Namespace.URI, "createServerStatement");
    public static final Name TYPE_CREATE_TABLESPACE_STATEMENT 			= new BasicName(Namespace.URI, "createTablespaceStatement");
    public static final Name TYPE_CREATE_TEXT_SEARCH_STATEMENT 			= new BasicName(Namespace.URI, "createTextSearchStatement");
    public static final Name TYPE_CREATE_TRIGGER_STATEMENT 				= new BasicName(Namespace.URI, "createTriggerStatement");
    public static final Name TYPE_CREATE_TYPE_STATEMENT 				= new BasicName(Namespace.URI, "createTypeStatement");
    public static final Name TYPE_CREATE_USER_STATEMENT 				= new BasicName(Namespace.URI, "createUserStatement");
    public static final Name TYPE_CREATE_USER_MAPPING_STATEMENT 		= new BasicName(Namespace.URI, "createUserMappingStatement");

    public static final Name TYPE_DROP_AGGREGATE_STATEMENT 				= new BasicName(Namespace.URI, "dropAggregateStatement");
    public static final Name TYPE_DROP_CAST_STATEMENT 					= new BasicName(Namespace.URI, "dropCastStatement");
    public static final Name TYPE_DROP_CONSTRAINT_TRIGGER_STATEMENT 	= new BasicName(Namespace.URI, "dropConstraintTriggerStatement");
    public static final Name TYPE_DROP_CONVERSION_STATEMENT 			= new BasicName(Namespace.URI, "dropConversionStatement");
    public static final Name TYPE_DROP_DATABASE_STATEMENT 				= new BasicName(Namespace.URI, "dropDatabaseStatement");
    public static final Name TYPE_DROP_FOREIGN_DATA_WRAPPER_STATEMENT 	= new BasicName(Namespace.URI, "dropForeignDataWrapperStatement");
    public static final Name TYPE_DROP_FUNCTION_STATEMENT 				= new BasicName(Namespace.URI, "dropFunctionStatement");
    public static final Name TYPE_DROP_GROUP_STATEMENT 					= new BasicName(Namespace.URI, "dropGroupStatement");
    public static final Name TYPE_DROP_INDEX_STATEMENT 					= new BasicName(Namespace.URI, "dropIndexStatement");
    public static final Name TYPE_DROP_LANGUAGE_STATEMENT 				= new BasicName(Namespace.URI, "dropLanguageStatement");
    public static final Name TYPE_DROP_OPERATOR_STATEMENT 				= new BasicName(Namespace.URI, "dropOperatorStatement");
    public static final Name TYPE_DROP_OWNED_BY_STATEMENT 				= new BasicName(Namespace.URI, "dropOwnedByStatement");
    public static final Name TYPE_DROP_ROLE_STATEMENT 					= new BasicName(Namespace.URI, "dropRoleStatement");
    public static final Name TYPE_DROP_RULE_STATEMENT 					= new BasicName(Namespace.URI, "dropRuleStatement");
    public static final Name TYPE_DROP_SEQUENCE_STATEMENT 				= new BasicName(Namespace.URI, "dropSequenceStatement");
    public static final Name TYPE_DROP_SERVER_STATEMENT 				= new BasicName(Namespace.URI, "dropServerStatement");
    public static final Name TYPE_DROP_TABLESPACE_STATEMENT 			= new BasicName(Namespace.URI, "dropTablespaceStatement");
    public static final Name TYPE_DROP_TEXT_SEARCH_STATEMENT 			= new BasicName(Namespace.URI, "dropTextSearchStatement");
    public static final Name TYPE_DROP_TRIGGER_STATEMENT 				= new BasicName(Namespace.URI, "dropTriggerStatement");
    public static final Name TYPE_DROP_TYPE_STATEMENT 					= new BasicName(Namespace.URI, "dropTypeStatement");
    public static final Name TYPE_DROP_USER_STATEMENT 					= new BasicName(Namespace.URI, "dropUserStatement");
    public static final Name TYPE_DROP_USER_MAPPING_STATEMENT 			= new BasicName(Namespace.URI, "dropUserMappingStatement");
    
    public static final Name TYPE_ALTER_AGGREGATE_STATEMENT 			= new BasicName(Namespace.URI, "alterAggregateStatement");
    public static final Name TYPE_ALTER_CONVERSION_STATEMENT 			= new BasicName(Namespace.URI, "alterConversionStatement");
    public static final Name TYPE_ALTER_DATABASE_STATEMENT 				= new BasicName(Namespace.URI, "alterDatabaseStatement");
    public static final Name TYPE_ALTER_FOREIGN_DATA_WRAPPER_STATEMENT 	= new BasicName(Namespace.URI, "alterForeignDataWrapperStatement");
    public static final Name TYPE_ALTER_FUNCTION_STATEMENT 				= new BasicName(Namespace.URI, "alterFunctionStatement");
    public static final Name TYPE_ALTER_GROUP_STATEMENT 				= new BasicName(Namespace.URI, "alterGroupStatement");
    public static final Name TYPE_ALTER_INDEX_STATEMENT 				= new BasicName(Namespace.URI, "alterIndexStatement");
    public static final Name TYPE_ALTER_LANGUAGE_STATEMENT 				= new BasicName(Namespace.URI, "alterLanguageStatement");
    public static final Name TYPE_ALTER_OPERATOR_STATEMENT 				= new BasicName(Namespace.URI, "alterOperatorStatement");
    public static final Name TYPE_ALTER_ROLE_STATEMENT 					= new BasicName(Namespace.URI, "alterRoleStatement");
    public static final Name TYPE_ALTER_SCHEMA_STATEMENT 				= new BasicName(Namespace.URI, "alterSchemaStatement");
    public static final Name TYPE_ALTER_SEQUENCE_STATEMENT 				= new BasicName(Namespace.URI, "alterSequenceStatement");
    public static final Name TYPE_ALTER_SERVER_STATEMENT 				= new BasicName(Namespace.URI, "alterServerStatement");
    public static final Name TYPE_ALTER_TABLESPACE_STATEMENT 			= new BasicName(Namespace.URI, "alterTablespaceStatement");
    public static final Name TYPE_ALTER_TEXT_SEARCH_STATEMENT 			= new BasicName(Namespace.URI, "alterTextSearchStatement");
    public static final Name TYPE_ALTER_TRIGGER_STATEMENT 				= new BasicName(Namespace.URI, "alterTriggerStatement");
    public static final Name TYPE_ALTER_TYPE_STATEMENT 					= new BasicName(Namespace.URI, "alterTypeStatement");
    public static final Name TYPE_ALTER_USER_STATEMENT 					= new BasicName(Namespace.URI, "alterUserStatement");
    public static final Name TYPE_ALTER_USER_MAPPING_STATEMENT 			= new BasicName(Namespace.URI, "alterUserMappingStatement");
    public static final Name TYPE_ALTER_VIEW_STATEMENT 					= new BasicName(Namespace.URI, "alterViewStatement");

    // This is required to attach additional properties
    public static final Name TYPE_ALTER_TABLE_STATEMENT_POSTGRES 		= new BasicName(Namespace.URI, "alterTableStatement");
    
    
    public static final Name TYPE_ABORT_STATEMENT 					= new BasicName(Namespace.URI, "abortStatement");
    public static final Name TYPE_ANALYZE_STATEMENT 				= new BasicName(Namespace.URI, "analyzeStatement");
    public static final Name TYPE_CLUSTER_STATEMENT 				= new BasicName(Namespace.URI, "clusterStatement");
    public static final Name TYPE_COMMENT_ON_STATEMENT 				= new BasicName(Namespace.URI, "commentOnStatement");
    public static final Name TYPE_COPY_STATEMENT 					= new BasicName(Namespace.URI, "copyStatement");
    public static final Name TYPE_DEALLOCATE_STATEMENT				= new BasicName(Namespace.URI, "deallocateStatement");
    public static final Name TYPE_DECLARE_STATEMENT 				= new BasicName(Namespace.URI, "declareStatement");
    public static final Name TYPE_DISCARD_STATEMENT 				= new BasicName(Namespace.URI, "discardStatement");
    public static final Name TYPE_EXPLAIN_STATEMENT 				= new BasicName(Namespace.URI, "explainStatement");
    public static final Name TYPE_FETCH_STATEMENT 					= new BasicName(Namespace.URI, "fetchStatement");
    public static final Name TYPE_LISTEN_STATEMENT 					= new BasicName(Namespace.URI, "listenStatement");
    public static final Name TYPE_LOAD_STATEMENT 					= new BasicName(Namespace.URI, "loadStatement");
    public static final Name TYPE_LOCK_TABLE_STATEMENT 				= new BasicName(Namespace.URI, "lockStatement");
    public static final Name TYPE_MOVE_STATEMENT 					= new BasicName(Namespace.URI, "moveStatement");
    public static final Name TYPE_NOTIFY_STATEMENT 					= new BasicName(Namespace.URI, "notifyStatement");
    public static final Name TYPE_PREPARE_STATEMENT 				= new BasicName(Namespace.URI, "prepareStatement");
    public static final Name TYPE_REASSIGN_OWNED_STATEMENT 			= new BasicName(Namespace.URI, "reassignOwnedStatement");
    public static final Name TYPE_REINDEX_STATEMENT 				= new BasicName(Namespace.URI, "reindexStatement");
    public static final Name TYPE_RELEASE_SAVEPOINT_STATEMENT 		= new BasicName(Namespace.URI, "releaseSavepointStatement");
    public static final Name TYPE_ROLLBACK_STATEMENT 				= new BasicName(Namespace.URI, "rollbackStatement");
    public static final Name TYPE_SELECT_INTO_STATEMENT 			= new BasicName(Namespace.URI, "selectIntoStatement");
    public static final Name TYPE_SHOW_STATEMENT 					= new BasicName(Namespace.URI, "showStatement");
    public static final Name TYPE_TRUNCATE_STATEMENT 				= new BasicName(Namespace.URI, "truncateStatement");
    public static final Name TYPE_UNLISTEN_STATEMENT 				= new BasicName(Namespace.URI, "unlistenStatement");
    public static final Name TYPE_VACUUM_STATEMENT 					= new BasicName(Namespace.URI, "vacuumStatement");
    
    public static final Name TYPE_GRANT_ON_SEQUENCE_STATEMENT       = new BasicName(Namespace.URI, "grantOnSequenceStatement");
    public static final Name TYPE_GRANT_ON_DATABASE_STATEMENT       = new BasicName(Namespace.URI, "grantOnDatabaseStatement");
    public static final Name TYPE_GRANT_ON_FOREIGN_DATA_WRAPPER_STATEMENT  = new BasicName(Namespace.URI, "grantOnForeignDataWrapperStatement");
    public static final Name TYPE_GRANT_ON_FOREIGN_SERVER_STATEMENT = new BasicName(Namespace.URI, "grantOnForeignServerStatement");
    public static final Name TYPE_GRANT_ON_FUNCTION_STATEMENT       = new BasicName(Namespace.URI, "grantOnFunctionStatement");
    public static final Name TYPE_GRANT_ON_LANGUAGE_STATEMENT       = new BasicName(Namespace.URI, "grantOnLanguageStatement");
    public static final Name TYPE_GRANT_ON_SCHEMA_STATEMENT         = new BasicName(Namespace.URI, "grantOnSchemaStatement");
    public static final Name TYPE_GRANT_ON_TABLESPACE_STATEMENT     = new BasicName(Namespace.URI, "grantOnTablespaceStatement");
    public static final Name TYPE_GRANT_ON_PROCEDURE_STATEMENT      = new BasicName(Namespace.URI, "grantOnProcedureStatement");
    public static final Name TYPE_GRANT_ROLES_STATEMENT             = new BasicName(Namespace.URI, "grantRolesStatement");
    
    public static final Name TYPE_RENAME_COLUMN 					= new BasicName(Namespace.URI, "renamedColumn");
    
    public static final Name SCHEMA_NAME 							= new BasicName(Namespace.URI, "schemaName");
    public static final Name FUNCTION_PARAMETER                     = new BasicName(Namespace.URI, "functionParameter");
    public static final Name FUNCTION_PARAMETER_MODE                = new BasicName(Namespace.URI, "mode");
    public static final Name ROLE                                   = new BasicName(Namespace.URI, "role");
    
    // PROPERTY NAMES
    public static final Name TARGET_OBJECT_TYPE = new BasicName(Namespace.URI, "targetObjectType");
    public static final Name TARGET_OBJECT_NAME = new BasicName(Namespace.URI, "targetObjectName");
    public static final Name COMMENT = new BasicName(Namespace.URI, "comment");
}
