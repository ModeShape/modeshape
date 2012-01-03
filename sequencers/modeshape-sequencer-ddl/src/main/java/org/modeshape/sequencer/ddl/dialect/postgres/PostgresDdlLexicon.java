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

import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 *
 */
public class PostgresDdlLexicon  extends StandardDdlLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/ddl/postgres/1.0";
        public static final String PREFIX = "postgresddl";
    }
    
    public static final String TYPE_CREATE_AGGREGATE_STATEMENT 			= Namespace.PREFIX + ":createAggregateStatement";
    public static final String TYPE_CREATE_CAST_STATEMENT 				= Namespace.PREFIX + ":createCastStatement";
    public static final String TYPE_CREATE_CONSTRAINT_TRIGGER_STATEMENT 	= Namespace.PREFIX + ":createConstraintTriggerStatement";
    public static final String TYPE_CREATE_CONVERSION_STATEMENT 			= Namespace.PREFIX + ":createConversionStatement";
    public static final String TYPE_CREATE_DATABASE_STATEMENT 			= Namespace.PREFIX + ":createDatabaseStatement";
    public static final String TYPE_CREATE_FOREIGN_DATA_WRAPPER_STATEMENT = Namespace.PREFIX + ":createForeignDataWrapperStatement";
    public static final String TYPE_CREATE_FUNCTION_STATEMENT 			= Namespace.PREFIX + ":createFunctionStatement";
    public static final String TYPE_CREATE_GROUP_STATEMENT 				= Namespace.PREFIX + ":createGroupStatement";
    public static final String TYPE_CREATE_INDEX_STATEMENT 				= Namespace.PREFIX + ":createIndexStatement";
    public static final String TYPE_CREATE_LANGUAGE_STATEMENT 			= Namespace.PREFIX + ":createLanguageStatement";
    public static final String TYPE_CREATE_OPERATOR_STATEMENT 			= Namespace.PREFIX + ":createOperatorStatement";
    public static final String TYPE_CREATE_ROLE_STATEMENT 				= Namespace.PREFIX + ":createRoleStatement";
    public static final String TYPE_CREATE_RULE_STATEMENT 				= Namespace.PREFIX + ":createRuleStatement";
    public static final String TYPE_CREATE_SEQUENCE_STATEMENT 			= Namespace.PREFIX + ":createSequenceStatement";
    public static final String TYPE_CREATE_SERVER_STATEMENT 				= Namespace.PREFIX + ":createServerStatement";
    public static final String TYPE_CREATE_TABLESPACE_STATEMENT 			= Namespace.PREFIX + ":createTablespaceStatement";
    public static final String TYPE_CREATE_TEXT_SEARCH_STATEMENT 			= Namespace.PREFIX + ":createTextSearchStatement";
    public static final String TYPE_CREATE_TRIGGER_STATEMENT 				= Namespace.PREFIX + ":createTriggerStatement";
    public static final String TYPE_CREATE_TYPE_STATEMENT 				= Namespace.PREFIX + ":createTypeStatement";
    public static final String TYPE_CREATE_USER_STATEMENT 				= Namespace.PREFIX + ":createUserStatement";
    public static final String TYPE_CREATE_USER_MAPPING_STATEMENT 		= Namespace.PREFIX + ":createUserMappingStatement";

    public static final String TYPE_DROP_AGGREGATE_STATEMENT 				= Namespace.PREFIX + ":dropAggregateStatement";
    public static final String TYPE_DROP_CAST_STATEMENT 					= Namespace.PREFIX + ":dropCastStatement";
    public static final String TYPE_DROP_CONSTRAINT_TRIGGER_STATEMENT 	= Namespace.PREFIX + ":dropConstraintTriggerStatement";
    public static final String TYPE_DROP_CONVERSION_STATEMENT 			= Namespace.PREFIX + ":dropConversionStatement";
    public static final String TYPE_DROP_DATABASE_STATEMENT 				= Namespace.PREFIX + ":dropDatabaseStatement";
    public static final String TYPE_DROP_FOREIGN_DATA_WRAPPER_STATEMENT 	= Namespace.PREFIX + ":dropForeignDataWrapperStatement";
    public static final String TYPE_DROP_FUNCTION_STATEMENT 				= Namespace.PREFIX + ":dropFunctionStatement";
    public static final String TYPE_DROP_GROUP_STATEMENT 					= Namespace.PREFIX + ":dropGroupStatement";
    public static final String TYPE_DROP_INDEX_STATEMENT 					= Namespace.PREFIX + ":dropIndexStatement";
    public static final String TYPE_DROP_LANGUAGE_STATEMENT 				= Namespace.PREFIX + ":dropLanguageStatement";
    public static final String TYPE_DROP_OPERATOR_STATEMENT 				= Namespace.PREFIX + ":dropOperatorStatement";
    public static final String TYPE_DROP_OWNED_BY_STATEMENT 				= Namespace.PREFIX + ":dropOwnedByStatement";
    public static final String TYPE_DROP_ROLE_STATEMENT 					= Namespace.PREFIX + ":dropRoleStatement";
    public static final String TYPE_DROP_RULE_STATEMENT 					= Namespace.PREFIX + ":dropRuleStatement";
    public static final String TYPE_DROP_SEQUENCE_STATEMENT 				= Namespace.PREFIX + ":dropSequenceStatement";
    public static final String TYPE_DROP_SERVER_STATEMENT 				= Namespace.PREFIX + ":dropServerStatement";
    public static final String TYPE_DROP_TABLESPACE_STATEMENT 			= Namespace.PREFIX + ":dropTablespaceStatement";
    public static final String TYPE_DROP_TEXT_SEARCH_STATEMENT 			= Namespace.PREFIX + ":dropTextSearchStatement";
    public static final String TYPE_DROP_TRIGGER_STATEMENT 				= Namespace.PREFIX + ":dropTriggerStatement";
    public static final String TYPE_DROP_TYPE_STATEMENT 					= Namespace.PREFIX + ":dropTypeStatement";
    public static final String TYPE_DROP_USER_STATEMENT 					= Namespace.PREFIX + ":dropUserStatement";
    public static final String TYPE_DROP_USER_MAPPING_STATEMENT 			= Namespace.PREFIX + ":dropUserMappingStatement";
    
    public static final String TYPE_ALTER_AGGREGATE_STATEMENT 			= Namespace.PREFIX + ":alterAggregateStatement";
    public static final String TYPE_ALTER_CONVERSION_STATEMENT 			= Namespace.PREFIX + ":alterConversionStatement";
    public static final String TYPE_ALTER_DATABASE_STATEMENT 				= Namespace.PREFIX + ":alterDatabaseStatement";
    public static final String TYPE_ALTER_FOREIGN_DATA_WRAPPER_STATEMENT 	= Namespace.PREFIX + ":alterForeignDataWrapperStatement";
    public static final String TYPE_ALTER_FUNCTION_STATEMENT 				= Namespace.PREFIX + ":alterFunctionStatement";
    public static final String TYPE_ALTER_GROUP_STATEMENT 				= Namespace.PREFIX + ":alterGroupStatement";
    public static final String TYPE_ALTER_INDEX_STATEMENT 				= Namespace.PREFIX + ":alterIndexStatement";
    public static final String TYPE_ALTER_LANGUAGE_STATEMENT 				= Namespace.PREFIX + ":alterLanguageStatement";
    public static final String TYPE_ALTER_OPERATOR_STATEMENT 				= Namespace.PREFIX + ":alterOperatorStatement";
    public static final String TYPE_ALTER_ROLE_STATEMENT 					= Namespace.PREFIX + ":alterRoleStatement";
    public static final String TYPE_ALTER_SCHEMA_STATEMENT 				= Namespace.PREFIX + ":alterSchemaStatement";
    public static final String TYPE_ALTER_SEQUENCE_STATEMENT 				= Namespace.PREFIX + ":alterSequenceStatement";
    public static final String TYPE_ALTER_SERVER_STATEMENT 				= Namespace.PREFIX + ":alterServerStatement";
    public static final String TYPE_ALTER_TABLESPACE_STATEMENT 			= Namespace.PREFIX + ":alterTablespaceStatement";
    public static final String TYPE_ALTER_TEXT_SEARCH_STATEMENT 			= Namespace.PREFIX + ":alterTextSearchStatement";
    public static final String TYPE_ALTER_TRIGGER_STATEMENT 				= Namespace.PREFIX + ":alterTriggerStatement";
    public static final String TYPE_ALTER_TYPE_STATEMENT 					= Namespace.PREFIX + ":alterTypeStatement";
    public static final String TYPE_ALTER_USER_STATEMENT 					= Namespace.PREFIX + ":alterUserStatement";
    public static final String TYPE_ALTER_USER_MAPPING_STATEMENT 			= Namespace.PREFIX + ":alterUserMappingStatement";
    public static final String TYPE_ALTER_VIEW_STATEMENT 					= Namespace.PREFIX + ":alterViewStatement";

    // This is required to attach additional properties
    public static final String TYPE_ALTER_TABLE_STATEMENT_POSTGRES 		= Namespace.PREFIX + ":alterTableStatement";
    
    
    public static final String TYPE_ABORT_STATEMENT 					= Namespace.PREFIX + ":abortStatement";
    public static final String TYPE_ANALYZE_STATEMENT 				= Namespace.PREFIX + ":analyzeStatement";
    public static final String TYPE_CLUSTER_STATEMENT 				= Namespace.PREFIX + ":clusterStatement";
    public static final String TYPE_COMMENT_ON_STATEMENT 				= Namespace.PREFIX + ":commentOnStatement";
    public static final String TYPE_COPY_STATEMENT 					= Namespace.PREFIX + ":copyStatement";
    public static final String TYPE_DEALLOCATE_STATEMENT				= Namespace.PREFIX + ":deallocateStatement";
    public static final String TYPE_DECLARE_STATEMENT 				= Namespace.PREFIX + ":declareStatement";
    public static final String TYPE_DISCARD_STATEMENT 				= Namespace.PREFIX + ":discardStatement";
    public static final String TYPE_EXPLAIN_STATEMENT 				= Namespace.PREFIX + ":explainStatement";
    public static final String TYPE_FETCH_STATEMENT 					= Namespace.PREFIX + ":fetchStatement";
    public static final String TYPE_LISTEN_STATEMENT 					= Namespace.PREFIX + ":listenStatement";
    public static final String TYPE_LOAD_STATEMENT 					= Namespace.PREFIX + ":loadStatement";
    public static final String TYPE_LOCK_TABLE_STATEMENT 				= Namespace.PREFIX + ":lockStatement";
    public static final String TYPE_MOVE_STATEMENT 					= Namespace.PREFIX + ":moveStatement";
    public static final String TYPE_NOTIFY_STATEMENT 					= Namespace.PREFIX + ":notifyStatement";
    public static final String TYPE_PREPARE_STATEMENT 				= Namespace.PREFIX + ":prepareStatement";
    public static final String TYPE_REASSIGN_OWNED_STATEMENT 			= Namespace.PREFIX + ":reassignOwnedStatement";
    public static final String TYPE_REINDEX_STATEMENT 				= Namespace.PREFIX + ":reindexStatement";
    public static final String TYPE_RELEASE_SAVEPOINT_STATEMENT 		= Namespace.PREFIX + ":releaseSavepointStatement";
    public static final String TYPE_ROLLBACK_STATEMENT 				= Namespace.PREFIX + ":rollbackStatement";
    public static final String TYPE_SELECT_INTO_STATEMENT 			= Namespace.PREFIX + ":selectIntoStatement";
    public static final String TYPE_SHOW_STATEMENT 					= Namespace.PREFIX + ":showStatement";
    public static final String TYPE_TRUNCATE_STATEMENT 				= Namespace.PREFIX + ":truncateStatement";
    public static final String TYPE_UNLISTEN_STATEMENT 				= Namespace.PREFIX + ":unlistenStatement";
    public static final String TYPE_VACUUM_STATEMENT 					= Namespace.PREFIX + ":vacuumStatement";
    
    public static final String TYPE_GRANT_ON_SEQUENCE_STATEMENT       = Namespace.PREFIX + ":grantOnSequenceStatement";
    public static final String TYPE_GRANT_ON_DATABASE_STATEMENT       = Namespace.PREFIX + ":grantOnDatabaseStatement";
    public static final String TYPE_GRANT_ON_FOREIGN_DATA_WRAPPER_STATEMENT  = Namespace.PREFIX + ":grantOnForeignDataWrapperStatement";
    public static final String TYPE_GRANT_ON_FOREIGN_SERVER_STATEMENT = Namespace.PREFIX + ":grantOnForeignServerStatement";
    public static final String TYPE_GRANT_ON_FUNCTION_STATEMENT       = Namespace.PREFIX + ":grantOnFunctionStatement";
    public static final String TYPE_GRANT_ON_LANGUAGE_STATEMENT       = Namespace.PREFIX + ":grantOnLanguageStatement";
    public static final String TYPE_GRANT_ON_SCHEMA_STATEMENT         = Namespace.PREFIX + ":grantOnSchemaStatement";
    public static final String TYPE_GRANT_ON_TABLESPACE_STATEMENT     = Namespace.PREFIX + ":grantOnTablespaceStatement";
    public static final String TYPE_GRANT_ON_PROCEDURE_STATEMENT      = Namespace.PREFIX + ":grantOnProcedureStatement";
    public static final String TYPE_GRANT_ROLES_STATEMENT             = Namespace.PREFIX + ":grantRolesStatement";
    
    public static final String TYPE_RENAME_COLUMN 					= Namespace.PREFIX + ":renamedColumn";
    
    public static final String SCHEMA_NAME 							= Namespace.PREFIX + ":schemaName";
    public static final String FUNCTION_PARAMETER                     = Namespace.PREFIX + ":functionParameter";
    public static final String FUNCTION_PARAMETER_MODE                = Namespace.PREFIX + ":mode";
    public static final String ROLE                                   = Namespace.PREFIX + ":role";
    
    // PROPERTY NAMES
    public static final String TARGET_OBJECT_TYPE = Namespace.PREFIX + ":targetObjectType";
    public static final String TARGET_OBJECT_NAME = Namespace.PREFIX + ":targetObjectName";
    public static final String COMMENT = Namespace.PREFIX + ":comment";
}
