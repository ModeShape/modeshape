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
package org.modeshape.sequencer.ddl.dialect.oracle;

import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.Namespace.PREFIX;
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
    public static final String TYPE_BACKSLASH_TERMINATOR = PREFIX + ":backslashTerminator";

    public static final String TYPE_CREATE_CLUSTER_STATEMENT = PREFIX + ":createClusterStatement";
    public static final String TYPE_CREATE_CONTEXT_STATEMENT = PREFIX + ":createContextStatement";
    public static final String TYPE_CREATE_CONTROLFILE_STATEMENT = PREFIX + ":createControlfileStatement";
    public static final String TYPE_CREATE_DATABASE_STATEMENT = PREFIX + ":createDatabaseStatement";
    public static final String TYPE_CREATE_DIMENSION_STATEMENT = PREFIX + ":createDimensionStatement";
    public static final String TYPE_CREATE_DIRECTORY_STATEMENT = PREFIX + ":createDirectoryStatement";
    public static final String TYPE_CREATE_DISKGROUP_STATEMENT = PREFIX + ":createDiskgroupStatement";
    public static final String TYPE_CREATE_FUNCTION_STATEMENT = PREFIX + ":createFunctionStatement";

    public static final String TYPE_CREATE_CLUSTER_INDEX_STATEMENT = PREFIX + ":createClusterIndexStatement";
    public static final String TYPE_CREATE_TABLE_INDEX_STATEMENT = PREFIX + ":createTableIndexStatement";
    public static final String TYPE_CREATE_BITMAP_JOIN_INDEX_STATEMENT = PREFIX + ":createBitmapIndexStatement";
    public static final String TYPE_CREATE_INDEXTYPE_STATEMENT = PREFIX + ":createIndexTypeStatement";

    public static final String TYPE_CREATE_JAVA_STATEMENT = PREFIX + ":createJavaStatement";
    public static final String TYPE_CREATE_LIBRARY_STATEMENT = PREFIX + ":createLibraryStatement";
    public static final String TYPE_CREATE_MATERIALIZED_VIEW_STATEMENT = PREFIX + ":createMaterializedViewStatement";
    public static final String TYPE_CREATE_MATERIALIZED_VIEW_LOG_STATEMENT = PREFIX + ":createMaterializedViewLogStatement";
    public static final String TYPE_CREATE_OPERATOR_STATEMENT = PREFIX + ":createOperatorStatement";
    public static final String TYPE_CREATE_OUTLINE_STATEMENT = PREFIX + ":createOutlineStatement";
    public static final String TYPE_CREATE_PACKAGE_STATEMENT = PREFIX + ":createPackageStatement";
    public static final String TYPE_CREATE_PFILE_STATEMENT = PREFIX + ":createPfileStatement";
    public static final String TYPE_CREATE_PROCEDURE_STATEMENT = PREFIX + ":createProcedureStatement";
    public static final String TYPE_CREATE_PROFILE_STATEMENT = PREFIX + ":createProfileStatement";
    public static final String TYPE_CREATE_ROLE_STATEMENT = PREFIX + ":createRoleStatement";
    public static final String TYPE_CREATE_ROLLBACK_STATEMENT = PREFIX + ":createRollbackStatement";
    public static final String TYPE_CREATE_SEQUENCE_STATEMENT = PREFIX + ":createSequenceStatement";
    public static final String TYPE_CREATE_SPFILE_STATEMENT = PREFIX + ":createSpfileStatement";
    public static final String TYPE_CREATE_SYNONYM_STATEMENT = PREFIX + ":createSynonymStatement";
    public static final String TYPE_CREATE_TABLESPACE_STATEMENT = PREFIX + ":createTablespaceStatement";
    public static final String TYPE_CREATE_TRIGGER_STATEMENT = PREFIX + ":createTriggerStatement";
    public static final String TYPE_CREATE_TYPE_STATEMENT = PREFIX + ":createTypeStatement";
    public static final String TYPE_CREATE_USER_STATEMENT = PREFIX + ":createUserStatement";

    public static final String TYPE_DROP_CLUSTER_STATEMENT = PREFIX + ":dropIndexStatement";
    public static final String TYPE_DROP_CONTEXT_STATEMENT = PREFIX + ":dropContextStatement";
    public static final String TYPE_DROP_DATABASE_STATEMENT = PREFIX + ":dropDatabaseStatement";
    public static final String TYPE_DROP_DIMENSION_STATEMENT = PREFIX + ":dropDimensionStatement";
    public static final String TYPE_DROP_DIRECTORY_STATEMENT = PREFIX + ":dropDirectoryStatement";
    public static final String TYPE_DROP_DISKGROUP_STATEMENT = PREFIX + ":dropDiskgroupStatement";
    public static final String TYPE_DROP_FUNCTION_STATEMENT = PREFIX + ":dropFunctionStatement";
    public static final String TYPE_DROP_INDEX_STATEMENT = PREFIX + ":dropIndexStatement";
    public static final String TYPE_DROP_INDEXTYPE_STATEMENT = PREFIX + ":dropIndextypeStatement";
    public static final String TYPE_DROP_JAVA_STATEMENT = PREFIX + ":dropJavaStatement";
    public static final String TYPE_DROP_LIBRARY_STATEMENT = PREFIX + ":dropLibraryStatement";
    public static final String TYPE_DROP_MATERIALIZED_STATEMENT = PREFIX + ":dropMaterializedStatement";
    public static final String TYPE_DROP_OPERATOR_STATEMENT = PREFIX + ":dropOperatorStatement";
    public static final String TYPE_DROP_OUTLINE_STATEMENT = PREFIX + ":dropOutlineStatement";
    public static final String TYPE_DROP_PACKAGE_STATEMENT = PREFIX + ":dropPackageStatement";
    public static final String TYPE_DROP_PROCEDURE_STATEMENT = PREFIX + ":dropProcedureStatement";
    public static final String TYPE_DROP_PROFILE_STATEMENT = PREFIX + ":dropProfileStatement";
    public static final String TYPE_DROP_ROLE_STATEMENT = PREFIX + ":dropRoleStatement";
    public static final String TYPE_DROP_ROLLBACK_STATEMENT = PREFIX + ":dropRollbackStatement";
    public static final String TYPE_DROP_SEQUENCE_STATEMENT = PREFIX + ":dropSequenceStatement";
    public static final String TYPE_DROP_SYNONYM_STATEMENT = PREFIX + ":dropSynonymStatement";
    public static final String TYPE_DROP_TABLESPACE_STATEMENT = PREFIX + ":dropTablespaceStatement";
    public static final String TYPE_DROP_TRIGGER_STATEMENT = PREFIX + ":dropTriggerStatement";
    public static final String TYPE_DROP_TYPE_STATEMENT = PREFIX + ":dropTypeStatement";
    public static final String TYPE_DROP_USER_STATEMENT = PREFIX + ":dropUserStatement";

    public static final String TYPE_ALTER_CLUSTER_STATEMENT = PREFIX + ":alterIndexStatement";
    public static final String TYPE_ALTER_DATABASE_STATEMENT = PREFIX + ":alterDatabaseStatement";
    public static final String TYPE_ALTER_DIMENSION_STATEMENT = PREFIX + ":alterDimensionStatement";
    public static final String TYPE_ALTER_DISKGROUP_STATEMENT = PREFIX + ":alterDiskgroupStatement";
    public static final String TYPE_ALTER_FUNCTION_STATEMENT = PREFIX + ":alterFunctionStatement";
    public static final String TYPE_ALTER_INDEX_STATEMENT = PREFIX + ":alterIndexStatement";
    public static final String TYPE_ALTER_INDEXTYPE_STATEMENT = PREFIX + ":alterIndextypeStatement";
    public static final String TYPE_ALTER_JAVA_STATEMENT = PREFIX + ":alterJavaStatement";
    public static final String TYPE_ALTER_MATERIALIZED_STATEMENT = PREFIX + ":alterMaterializedStatement";
    public static final String TYPE_ALTER_OPERATOR_STATEMENT = PREFIX + ":alterOperatorStatement";
    public static final String TYPE_ALTER_OUTLINE_STATEMENT = PREFIX + ":alterOutlineStatement";
    public static final String TYPE_ALTER_PACKAGE_STATEMENT = PREFIX + ":alterPackageStatement";
    public static final String TYPE_ALTER_PROCEDURE_STATEMENT = PREFIX + ":alterProcedureStatement";
    public static final String TYPE_ALTER_PROFILE_STATEMENT = PREFIX + ":alterProfileStatement";
    public static final String TYPE_ALTER_RESOURCE_STATEMENT = PREFIX + ":alterResourceStatement";
    public static final String TYPE_ALTER_ROLE_STATEMENT = PREFIX + ":alterRoleStatement";
    public static final String TYPE_ALTER_ROLLBACK_STATEMENT = PREFIX + ":alterRollbackStatement";
    public static final String TYPE_ALTER_SEQUENCE_STATEMENT = PREFIX + ":alterSequenceStatement";
    public static final String TYPE_ALTER_SESSION_STATEMENT = PREFIX + ":alterSessionStatement";
    public static final String TYPE_ALTER_SYNONYM_STATEMENT = PREFIX + ":alterSynonymStatement";
    public static final String TYPE_ALTER_SYSTEM_STATEMENT = PREFIX + ":alterSystemStatement";
    public static final String TYPE_ALTER_TABLESPACE_STATEMENT = PREFIX + ":alterTablespaceStatement";
    public static final String TYPE_ALTER_TRIGGER_STATEMENT = PREFIX + ":alterTriggerStatement";
    public static final String TYPE_ALTER_TYPE_STATEMENT = PREFIX + ":alterTypeStatement";
    public static final String TYPE_ALTER_USER_STATEMENT = PREFIX + ":alterUserStatement";
    public static final String TYPE_ALTER_VIEW_STATEMENT = PREFIX + ":alterViewStatement";

    public static final String TYPE_ANALYZE_STATEMENT = PREFIX + ":analyzeStatement";
    public static final String TYPE_ASSOCIATE_STATISTICS_STATEMENT = PREFIX + ":associateStatisticsStatement";
    public static final String TYPE_AUDIT_STATEMENT = PREFIX + ":auditStatement";
    public static final String TYPE_COMMIT_STATEMENT = PREFIX + ":commitStatement";
    public static final String TYPE_COMMENT_ON_STATEMENT = PREFIX + ":commentOnStatement";
    public static final String TYPE_DISASSOCIATE_STATISTICS_STATEMENT = PREFIX + ":disassociateStatisticsStatement";
    public static final String TYPE_EXPLAIN_PLAN_STATEMENT = PREFIX + ":explainPlanStatement";
    public static final String TYPE_FLASHBACK_STATEMENT = PREFIX + ":flashbackStatement";
    public static final String TYPE_LOCK_TABLE_STATEMENT = PREFIX + ":lockTableStatement";
    public static final String TYPE_MERGE_STATEMENT = PREFIX + ":mergeStatement";
    public static final String TYPE_NOAUDIT_STATEMENT = PREFIX + ":noAuditStatement";
    public static final String TYPE_PURGE_STATEMENT = PREFIX + ":purgeStatement";
    public static final String TYPE_RENAME_STATEMENT = PREFIX + ":renameStatement";
    public static final String TYPE_REVOKE_STATEMENT = PREFIX + ":revokeStatement";
    public static final String TYPE_ROLLBACK_STATEMENT = PREFIX + ":rollbackStatement";
    public static final String TYPE_SAVEPOINT_STATEMENT = PREFIX + ":savepointStatement";
    public static final String TYPE_SET_CONSTRAINT_STATEMENT = PREFIX + ":setConstraintStatement";
    public static final String TYPE_SET_CONSTRAINTS_STATEMENT = PREFIX + ":setConstraintsStatement";
    public static final String TYPE_SET_ROLE_STATEMENT = PREFIX + ":setRoleStatement";
    public static final String TYPE_SET_TRANSACTION_STATEMENT = PREFIX + ":setTransactionStatement";
    public static final String TYPE_TRUNCATE_STATEMENT = PREFIX + ":truncateStatement";

    public static final String TYPE_RENAME_COLUMN = PREFIX + ":renameColumn";
    public static final String TYPE_RENAME_CONSTRAINT = PREFIX + ":renameConstraint";
    public static final String TYPE_FUNCTION_PARAMETER = PREFIX + ":functionParameter";
    public static final String TYPE_INDEX_ORDERABLE = PREFIX + ":indexOrderable";

    // PROPERTY NAMES
    public static final String TARGET_OBJECT_TYPE = PREFIX + ":targetObjectType";
    public static final String COMMENT = PREFIX + ":comment";
    public static final String UNIQUE_INDEX = PREFIX + ":unique";
    public static final String BITMAP_INDEX = PREFIX + ":bitmap";
    public static final String TABLE_NAME = PREFIX + ":tableName";
    public static final String IN_OUT_NO_COPY = PREFIX + ":inOutNoCopy";
    public static final String AUTHID_VALUE = PREFIX + ":authIdValue";
    public static final String INDEX_TYPE = PREFIX + ":indexType";
    public static final String UNUSABLE_INDEX = PREFIX + ":unusable";
    public static final String CLUSTER_NAME = PREFIX + ":clustereName";
    public static final String TABLE_ALIAS = PREFIX + ":tableAlias";
    public static final String OTHER_INDEX_REFS = PREFIX + ":otherRefs";
    public static final String INDEX_ATTRIBUTES = PREFIX + ":indexAttributes";
    public static final String INDEX_ORDER = PREFIX + ":order";
    public static final String WHERE_CLAUSE = PREFIX + ":whereClause";

}
