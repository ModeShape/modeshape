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
package org.modeshape.sequencer.ddl.dialect.oracle;

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 *
 */
public class OracleDdlLexicon extends StandardDdlLexicon {
    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/ddl/oracle/1.0";
        public static final String PREFIX = "oracleddl";
    }

    // MIXINS
    public static final Name TYPE_BACKSLASH_TERMINATOR = new BasicName(Namespace.URI, "backslashTerminator");
    
    public static final Name TYPE_CREATE_CLUSTER_STATEMENT 			= new BasicName(Namespace.URI, "createIndexStatement");
    public static final Name TYPE_CREATE_CONTEXT_STATEMENT 			= new BasicName(Namespace.URI, "createContextStatement");
    public static final Name TYPE_CREATE_CONTROLFILE_STATEMENT 		= new BasicName(Namespace.URI, "createControlfileStatement");
    public static final Name TYPE_CREATE_DATABASE_STATEMENT 		= new BasicName(Namespace.URI, "createDatabaseStatement");
    public static final Name TYPE_CREATE_DIMENSION_STATEMENT 		= new BasicName(Namespace.URI, "createDimensionStatement");
    public static final Name TYPE_CREATE_DIRECTORY_STATEMENT 		= new BasicName(Namespace.URI, "createDirectoryStatement");
    public static final Name TYPE_CREATE_DISKGROUP_STATEMENT 		= new BasicName(Namespace.URI, "createDiskgroupStatement");
    public static final Name TYPE_CREATE_FUNCTION_STATEMENT 		= new BasicName(Namespace.URI, "createFunctionStatement");
    public static final Name TYPE_CREATE_INDEX_STATEMENT 			= new BasicName(Namespace.URI, "createIndexStatement");
    public static final Name TYPE_CREATE_INDEXTYPE_STATEMENT 		= new BasicName(Namespace.URI, "createIndextypeStatement");
    public static final Name TYPE_CREATE_JAVA_STATEMENT 			= new BasicName(Namespace.URI, "createJavaStatement");
    public static final Name TYPE_CREATE_LIBRARY_STATEMENT 			= new BasicName(Namespace.URI, "createLibraryStatement");
    public static final Name TYPE_CREATE_MATERIALIZED_VIEW_STATEMENT 	      = new BasicName(Namespace.URI, "createMaterializedViewStatement");
    public static final Name TYPE_CREATE_MATERIALIZED_VIEW_LOG_STATEMENT      = new BasicName(Namespace.URI, "createMaterializedViewLogStatement");
    public static final Name TYPE_CREATE_OPERATOR_STATEMENT 		= new BasicName(Namespace.URI, "createOperatorStatement");
    public static final Name TYPE_CREATE_OUTLINE_STATEMENT 			= new BasicName(Namespace.URI, "createOutlineStatement");
    public static final Name TYPE_CREATE_PACKAGE_STATEMENT 			= new BasicName(Namespace.URI, "createPackageStatement");
    public static final Name TYPE_CREATE_PFILE_STATEMENT 			= new BasicName(Namespace.URI, "createPfileStatement");
    public static final Name TYPE_CREATE_PROCEDURE_STATEMENT 		= new BasicName(Namespace.URI, "createProcedureStatement");
    public static final Name TYPE_CREATE_PROFILE_STATEMENT 			= new BasicName(Namespace.URI, "createProfileStatement");
    public static final Name TYPE_CREATE_ROLE_STATEMENT 			= new BasicName(Namespace.URI, "createRoleStatement");
    public static final Name TYPE_CREATE_ROLLBACK_STATEMENT 		= new BasicName(Namespace.URI, "createRollbackStatement");
    public static final Name TYPE_CREATE_SEQUENCE_STATEMENT 		= new BasicName(Namespace.URI, "createSequenceStatement");
    public static final Name TYPE_CREATE_SPFILE_STATEMENT 			= new BasicName(Namespace.URI, "createSpfileStatement");
    public static final Name TYPE_CREATE_SYNONYM_STATEMENT 			= new BasicName(Namespace.URI, "createSynonymStatement");
    public static final Name TYPE_CREATE_TABLESPACE_STATEMENT 		= new BasicName(Namespace.URI, "createTablespaceStatement");
    public static final Name TYPE_CREATE_TRIGGER_STATEMENT 			= new BasicName(Namespace.URI, "createTriggerStatement");
    public static final Name TYPE_CREATE_TYPE_STATEMENT 			= new BasicName(Namespace.URI, "createTypeStatement");
    public static final Name TYPE_CREATE_USER_STATEMENT 			= new BasicName(Namespace.URI, "createUserStatement");
    
    
    public static final Name TYPE_DROP_CLUSTER_STATEMENT 		= new BasicName(Namespace.URI, "dropIndexStatement");
    public static final Name TYPE_DROP_CONTEXT_STATEMENT 		= new BasicName(Namespace.URI, "dropContextStatement");
    public static final Name TYPE_DROP_DATABASE_STATEMENT 		= new BasicName(Namespace.URI, "dropDatabaseStatement");
    public static final Name TYPE_DROP_DIMENSION_STATEMENT 		= new BasicName(Namespace.URI, "dropDimensionStatement");
    public static final Name TYPE_DROP_DIRECTORY_STATEMENT 		= new BasicName(Namespace.URI, "dropDirectoryStatement");
    public static final Name TYPE_DROP_DISKGROUP_STATEMENT 		= new BasicName(Namespace.URI, "dropDiskgroupStatement");
    public static final Name TYPE_DROP_FUNCTION_STATEMENT		= new BasicName(Namespace.URI, "dropFunctionStatement");
    public static final Name TYPE_DROP_INDEX_STATEMENT 			= new BasicName(Namespace.URI, "dropIndexStatement");
    public static final Name TYPE_DROP_INDEXTYPE_STATEMENT 		= new BasicName(Namespace.URI, "dropIndextypeStatement");
    public static final Name TYPE_DROP_JAVA_STATEMENT 			= new BasicName(Namespace.URI, "dropJavaStatement");
    public static final Name TYPE_DROP_LIBRARY_STATEMENT 		= new BasicName(Namespace.URI, "dropLibraryStatement");
    public static final Name TYPE_DROP_MATERIALIZED_STATEMENT 	= new BasicName(Namespace.URI, "dropMaterializedStatement");
    public static final Name TYPE_DROP_OPERATOR_STATEMENT 		= new BasicName(Namespace.URI, "dropOperatorStatement");
    public static final Name TYPE_DROP_OUTLINE_STATEMENT 		= new BasicName(Namespace.URI, "dropOutlineStatement");
    public static final Name TYPE_DROP_PACKAGE_STATEMENT		= new BasicName(Namespace.URI, "dropPackageStatement");
    public static final Name TYPE_DROP_PROCEDURE_STATEMENT 		= new BasicName(Namespace.URI, "dropProcedureStatement");
    public static final Name TYPE_DROP_PROFILE_STATEMENT		= new BasicName(Namespace.URI, "dropProfileStatement");
    public static final Name TYPE_DROP_ROLE_STATEMENT 			= new BasicName(Namespace.URI, "dropRoleStatement");
    public static final Name TYPE_DROP_ROLLBACK_STATEMENT 		= new BasicName(Namespace.URI, "dropRollbackStatement");
    public static final Name TYPE_DROP_SEQUENCE_STATEMENT 		= new BasicName(Namespace.URI, "dropSequenceStatement");
    public static final Name TYPE_DROP_SYNONYM_STATEMENT 		= new BasicName(Namespace.URI, "dropSynonymStatement");
    public static final Name TYPE_DROP_TABLESPACE_STATEMENT 	= new BasicName(Namespace.URI, "dropTablespaceStatement");
    public static final Name TYPE_DROP_TRIGGER_STATEMENT 		= new BasicName(Namespace.URI, "dropTriggerStatement");
    public static final Name TYPE_DROP_TYPE_STATEMENT 			= new BasicName(Namespace.URI, "dropTypeStatement");
    public static final Name TYPE_DROP_USER_STATEMENT 			= new BasicName(Namespace.URI, "dropUserStatement");

    
    public static final Name TYPE_ALTER_CLUSTER_STATEMENT 		= new BasicName(Namespace.URI, "alterIndexStatement");
    public static final Name TYPE_ALTER_DATABASE_STATEMENT		= new BasicName(Namespace.URI, "alterDatabaseStatement");
    public static final Name TYPE_ALTER_DIMENSION_STATEMENT 	= new BasicName(Namespace.URI, "alterDimensionStatement");
    public static final Name TYPE_ALTER_DIRECTORY_STATEMENT 	= new BasicName(Namespace.URI, "alterDirectoryStatement");
    public static final Name TYPE_ALTER_DISKGROUP_STATEMENT 	= new BasicName(Namespace.URI, "alterDiskgroupStatement");
    public static final Name TYPE_ALTER_FUNCTION_STATEMENT 		= new BasicName(Namespace.URI, "alterFunctionStatement");
    public static final Name TYPE_ALTER_INDEX_STATEMENT 		= new BasicName(Namespace.URI, "alterIndexStatement");
    public static final Name TYPE_ALTER_INDEXTYPE_STATEMENT 	= new BasicName(Namespace.URI, "alterIndextypeStatement");
    public static final Name TYPE_ALTER_JAVA_STATEMENT 			= new BasicName(Namespace.URI, "alterJavaStatement");
    public static final Name TYPE_ALTER_LIBRARY_STATEMENT 		= new BasicName(Namespace.URI, "alterLibraryStatement");
    public static final Name TYPE_ALTER_MATERIALIZED_STATEMENT 	= new BasicName(Namespace.URI, "alterMaterializedStatement");
    public static final Name TYPE_ALTER_OPERATOR_STATEMENT 		= new BasicName(Namespace.URI, "alterOperatorStatement");
    public static final Name TYPE_ALTER_OUTLINE_STATEMENT 		= new BasicName(Namespace.URI, "alterOutlineStatement");
    public static final Name TYPE_ALTER_PACKAGE_STATEMENT 		= new BasicName(Namespace.URI, "alterPackageStatement");
    public static final Name TYPE_ALTER_PROCEDURE_STATEMENT 	= new BasicName(Namespace.URI, "alterProcedureStatement");
    public static final Name TYPE_ALTER_PROFILE_STATEMENT 		= new BasicName(Namespace.URI, "alterProfileStatement");
    public static final Name TYPE_ALTER_RESOURCE_STATEMENT 		= new BasicName(Namespace.URI, "alterResourceStatement");
    public static final Name TYPE_ALTER_ROLE_STATEMENT 			= new BasicName(Namespace.URI, "alterRoleStatement");
    public static final Name TYPE_ALTER_ROLLBACK_STATEMENT 		= new BasicName(Namespace.URI, "alterRollbackStatement");
    public static final Name TYPE_ALTER_SEQUENCE_STATEMENT		= new BasicName(Namespace.URI, "alterSequenceStatement");
    public static final Name TYPE_ALTER_SESSION_STATEMENT 		= new BasicName(Namespace.URI, "alterSessionStatement");
    public static final Name TYPE_ALTER_SYNONYM_STATEMENT 		= new BasicName(Namespace.URI, "alterSynonymStatement");
    public static final Name TYPE_ALTER_SYSTEM_STATEMENT 		= new BasicName(Namespace.URI, "alterSystemStatement");
    public static final Name TYPE_ALTER_TABLESPACE_STATEMENT 	= new BasicName(Namespace.URI, "alterTablespaceStatement");
    public static final Name TYPE_ALTER_TRIGGER_STATEMENT 		= new BasicName(Namespace.URI, "alterTriggerStatement");
    public static final Name TYPE_ALTER_TYPE_STATEMENT 			= new BasicName(Namespace.URI, "alterTypeStatement");
    public static final Name TYPE_ALTER_USER_STATEMENT 			= new BasicName(Namespace.URI, "alterUserStatement");
    public static final Name TYPE_ALTER_VIEW_STATEMENT 			= new BasicName(Namespace.URI, "alterViewStatement");
    
    public static final Name TYPE_ANALYZE_STATEMENT	 				= new BasicName(Namespace.URI, "analyzeStatement");
    public static final Name TYPE_ASSOCIATE_STATISTICS_STATEMENT	= new BasicName(Namespace.URI, "associateStatisticsStatement");
    public static final Name TYPE_AUDIT_STATEMENT		 			= new BasicName(Namespace.URI, "auditStatement");
    public static final Name TYPE_COMMIT_STATEMENT	 				= new BasicName(Namespace.URI, "commitStatement");
    public static final Name TYPE_COMMENT_ON_STATEMENT 				= new BasicName(Namespace.URI, "commentOnStatement");
    public static final Name TYPE_DISASSOCIATE_STATISTICS_STATEMENT	= new BasicName(Namespace.URI, "disassociateStatisticsStatement");
    public static final Name TYPE_EXPLAIN_PLAN_STATEMENT 			= new BasicName(Namespace.URI, "explainPlanStatement");
    public static final Name TYPE_FLASHBACK_STATEMENT 				= new BasicName(Namespace.URI, "flashbackStatement");
    public static final Name TYPE_LOCK_TABLE_STATEMENT 				= new BasicName(Namespace.URI, "lockTableStatement");
    public static final Name TYPE_MERGE_STATEMENT 					= new BasicName(Namespace.URI, "mergeStatement");
    public static final Name TYPE_NOAUDIT_STATEMENT 				= new BasicName(Namespace.URI, "noAuditStatement");
    public static final Name TYPE_PURGE_STATEMENT 					= new BasicName(Namespace.URI, "purgeStatement");
    public static final Name TYPE_RENAME_STATEMENT 					= new BasicName(Namespace.URI, "renameStatement");
    public static final Name TYPE_REVOKE_STATEMENT 					= new BasicName(Namespace.URI, "revokeStatement");
    public static final Name TYPE_ROLLBACK_STATEMENT 				= new BasicName(Namespace.URI, "rollbackStatement");
    public static final Name TYPE_SAVEPOINT_STATEMENT 				= new BasicName(Namespace.URI, "savepointStatement");
    public static final Name TYPE_SET_CONSTRAINT_STATEMENT 			= new BasicName(Namespace.URI, "setConstraintStatement");
    public static final Name TYPE_SET_CONSTRAINTS_STATEMENT 		= new BasicName(Namespace.URI, "setConstraintsStatement");
    public static final Name TYPE_SET_ROLE_STATEMENT 				= new BasicName(Namespace.URI, "setRoleStatement");
    public static final Name TYPE_SET_TRANSACTION_STATEMENT 		= new BasicName(Namespace.URI, "setTransactionStatement");
    public static final Name TYPE_TRUNCATE_STATEMENT 				= new BasicName(Namespace.URI, "truncateStatement");
    
    public static final Name TYPE_RENAME_COLUMN 					= new BasicName(Namespace.URI, "renameColumn");
    public static final Name TYPE_RENAME_CONSTRAINT 				= new BasicName(Namespace.URI, "renameConstraint");
    public static final Name TYPE_FUNCTION_PARAMETER                = new BasicName(Namespace.URI, "functionParameter");

    // PROPERTY NAMES
    public static final Name TARGET_OBJECT_TYPE = new BasicName(Namespace.URI, "targetObjectType");
    public static final Name TARGET_OBJECT_NAME = new BasicName(Namespace.URI, "targetObjectName");
    public static final Name COMMENT            = new BasicName(Namespace.URI, "comment");
    public static final Name UNIQUE_INDEX       = new BasicName(Namespace.URI, "unique");
    public static final Name BITMAP_INDEX       = new BasicName(Namespace.URI, "bitmap");
    public static final Name TABLE_NAME         = new BasicName(Namespace.URI, "tableName");
    public static final Name DEFAULT            = new BasicName(Namespace.URI, "default");
    public static final Name DEFAULT_EXPRESSION = new BasicName(Namespace.URI, "defaultExpression");
    public static final Name IN_OUT_NO_COPY     = new BasicName(Namespace.URI, "inOutNoCopy");
    public static final Name AUTHID_VALUE       = new BasicName(Namespace.URI, "authIdValue");
    

}
