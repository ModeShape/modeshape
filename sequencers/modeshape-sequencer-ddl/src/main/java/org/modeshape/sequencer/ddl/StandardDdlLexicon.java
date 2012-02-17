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
package org.modeshape.sequencer.ddl;

import static org.modeshape.sequencer.ddl.StandardDdlLexicon.Namespace.PREFIX;

/**
 * Lexicon for DDL concepts.
 */
public class StandardDdlLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/ddl/1.0";
        public static final String PREFIX = "ddl";
    }

    public static final String PARSER_ID = PREFIX + ":parserId";
    public static final String STATEMENTS_CONTAINER = PREFIX + ":statements";
    /*
     * mixin types
     *
     * SQL-92 Spec
     *
     * 		CREATE SCHEMA
     *		CREATE DOMAIN
     * 		CREATE [ { GLOBAL | LOCAL } TEMPORARY ] TABLE
     *		CREATE VIEW
     *		CREATE ASSERTION
     *		CREATE CHARACTER SET
     *		CREATE COLLATION
     *		CREATE TRANSLATION
     *
     *		ALTER TABLE
     */

    public static final String TYPE_MISSING_TERMINATOR = PREFIX + ":missingTerminator";
    public static final String TYPE_UNKNOWN_STATEMENT = PREFIX + ":unknownStatement";

    public static final String TYPE_OPERATION = PREFIX +  ":operation";
    public static final String TYPE_OPERAND = PREFIX + ":operand";
    public static final String TYPE_STATEMENT = PREFIX + ":statement";

    public static final String TYPE_CREATEABLE = PREFIX + ":creatable";
    public static final String TYPE_ALTERABLE = PREFIX + ":alterable";
    public static final String TYPE_DROPPABLE = PREFIX + ":droppable";
    public static final String TYPE_INSERTABLE = PREFIX + ":insertable";
    public static final String TYPE_SETTABLE = PREFIX + ":settable";
    public static final String TYPE_GRANTABLE = PREFIX + ":grantable";
    public static final String TYPE_REVOKABLE = PREFIX + ":revokable";

    public static final String TYPE_SCHEMA_OPERAND = PREFIX + ":schemaOperand";
    public static final String TYPE_TABLE_OPERAND = PREFIX + ":tableOperand";
    public static final String TYPE_DOMAIN_OPERAND = PREFIX + ":domainOperand";
    public static final String TYPE_VIEW_OPERAND = PREFIX + ":viewOperand";
    public static final String TYPE_ASSERTION_OPERAND = PREFIX + ":assertionOperand";
    public static final String TYPE_CHARACTER_SET_OPERAND = PREFIX + ":characterSetOperand";
    public static final String TYPE_COLLATION_OPERAND = PREFIX + ":collationOperand";
    public static final String TYPE_TRANSLATION_OPERAND = PREFIX + ":translationOperand";
    public static final String TYPE_COLUMN_OPERAND = PREFIX + ":columnOperand";
    public static final String TYPE_TABLE_CONSTRAINT_OPERAND = PREFIX + ":tableConstraintOperand";
    public static final String TYPE_REFERENCE_OPERAND = PREFIX + ":referenceOperand";

    public static final String TYPE_CREATE_TABLE_STATEMENT = PREFIX + ":createTableStatement";
    public static final String TYPE_CREATE_SCHEMA_STATEMENT = PREFIX + ":createSchemaStatement";
    public static final String TYPE_CREATE_VIEW_STATEMENT = PREFIX + ":createViewStatement";
    public static final String TYPE_CREATE_DOMAIN_STATEMENT = PREFIX + ":createDomainStatement";
    public static final String TYPE_CREATE_ASSERTION_STATEMENT = PREFIX + ":createAssertionStatement";
    public static final String TYPE_CREATE_CHARACTER_SET_STATEMENT = PREFIX + ":createCharacterSetStatement";
    public static final String TYPE_CREATE_COLLATION_STATEMENT = PREFIX + ":createCollationStatement";
    public static final String TYPE_CREATE_TRANSLATION_STATEMENT = PREFIX + ":createTranslationStatement";

    public static final String TYPE_ALTER_TABLE_STATEMENT = PREFIX + ":alterTableStatement";
    public static final String TYPE_ALTER_DOMAIN_STATEMENT = PREFIX + ":alterDomainStatement";
    public static final String TYPE_GRANT_STATEMENT = PREFIX + ":grantStatement";
    public static final String TYPE_GRANT_ON_TABLE_STATEMENT = PREFIX + ":grantOnTableStatement";
    public static final String TYPE_GRANT_ON_DOMAIN_STATEMENT = PREFIX + ":grantOnDomainStatement";
    public static final String TYPE_GRANT_ON_COLLATION_STATEMENT = PREFIX + ":grantOnCollationStatement";
    public static final String TYPE_GRANT_ON_CHARACTER_SET_STATEMENT = PREFIX + ":grantOnCharacterSetStatement";
    public static final String TYPE_GRANT_ON_TRANSLATION_STATEMENT = PREFIX + ":grantOnTranslationStatement";
    public static final String TYPE_REVOKE_STATEMENT = PREFIX + ":revokeStatement";
    public static final String TYPE_REVOKE_ON_TABLE_STATEMENT = PREFIX + ":revokeOnTableStatement";
    public static final String TYPE_REVOKE_ON_DOMAIN_STATEMENT = PREFIX + ":revokeOnDomainStatement";
    public static final String TYPE_REVOKE_ON_COLLATION_STATEMENT = PREFIX + ":revokeOnCollationStatement";
    public static final String TYPE_REVOKE_ON_CHARACTER_SET_STATEMENT = PREFIX + ":revokeOnCharacterSetStatement";
    public static final String TYPE_REVOKE_ON_TRANSLATION_STATEMENT = PREFIX + ":revokeOnTranslationStatement";
    public static final String TYPE_SET_STATEMENT = PREFIX + ":setStatement";
    public static final String TYPE_INSERT_STATEMENT = PREFIX + ":insertStatement";

    public static final String TYPE_DROP_SCHEMA_STATEMENT = PREFIX + ":dropSchemaStatement";
    public static final String TYPE_DROP_TABLE_STATEMENT = PREFIX + ":dropTableStatement";
    public static final String TYPE_DROP_VIEW_STATEMENT = PREFIX + ":dropViewStatement";
    public static final String TYPE_DROP_DOMAIN_STATEMENT = PREFIX + ":dropDomainStatement";
    public static final String TYPE_DROP_CHARACTER_SET_STATEMENT = PREFIX + ":dropCharacterSetStatement";
    public static final String TYPE_DROP_COLLATION_STATEMENT = PREFIX + ":dropCollationStatement";
    public static final String TYPE_DROP_TRANSLATION_STATEMENT = PREFIX + ":dropTranslationStatement";
    public static final String TYPE_DROP_ASSERTION_STATEMENT = PREFIX + ":dropAssertionStatement";

    public static final String TYPE_DROP_COLUMN_DEFINITION = PREFIX + ":dropColumnDefinition";
    public static final String TYPE_ALTER_COLUMN_DEFINITION = PREFIX + ":alterColumnDefinition";
    public static final String TYPE_ADD_COLUMN_DEFINITION = PREFIX + ":addColumnDefinition";
    public static final String TYPE_DROP_TABLE_CONSTRAINT_DEFINITION = PREFIX + ":dropTableConstraintDefinition";
    public static final String TYPE_ADD_TABLE_CONSTRAINT_DEFINITION = PREFIX + ":addTableConstraintDefinition";

    public static final String TYPE_PROBLEM = PREFIX + ":ddlProblem";
    public static final String TYPE_COLUMN_DEFINITION = PREFIX + ":columnDefinition";
    public static final String TYPE_COLUMN_REFERENCE = PREFIX + ":columnReference";
    public static final String TYPE_TABLE_CONSTRAINT = PREFIX + ":tableConstraint";
    public static final String TYPE_STATEMENT_OPTION = PREFIX + ":statementOption";
    public static final String TYPE_TABLE_REFERENCE = PREFIX + ":tableReference";
    public static final String TYPE_FK_COLUMN_REFERENCE = PREFIX + ":fkColumnReference";
    public static final String TYPE_CLAUSE = PREFIX + ":clause";
    public static final String TYPE_DOMAIN_CONSTRAINT = PREFIX + ":domainConstraint";
    public static final String TYPE_SIMPLE_PROPERTY = PREFIX + ":simpleProperty";
    /*
     * node property names
     */
    public static final String DDL_EXPRESSION = PREFIX + ":expression";
    public static final String DDL_ORIGINAL_EXPRESSION = PREFIX + ":originalExpression";
    public static final String DDL_START_LINE_NUMBER = PREFIX + ":startLineNumber";
    public static final String DDL_START_COLUMN_NUMBER = PREFIX + ":startColumnNumber";
    public static final String DDL_START_CHAR_INDEX = PREFIX + ":startCharIndex";
    public static final String DDL_PROBLEM = PREFIX + ":problem";
    public static final String DDL_LENGTH = PREFIX + ":length";

    public static final String OPTION = PREFIX + ":option";
    public static final String TYPE = PREFIX + ":type";
    public static final String NEW_NAME = PREFIX +":newName";
    public static final String SQL = PREFIX + ":sql";
    public static final String TEMPORARY = PREFIX + ":temporary";
    public static final String ON_COMMIT_VALUE = PREFIX + ":onCommitValue";
    public static final String NULLABLE = PREFIX + ":nullable";
    public static final String DEFAULT_OPTION = PREFIX + ":defaultOption";
    public static final String COLLATION_NAME = PREFIX + ":collationName";
    public static final String CONSTRAINT_TYPE = PREFIX + ":constraintType";
    public static final String DEFERRABLE = PREFIX + ":deferrable";
    public static final String CHECK_SEARCH_CONDITION = PREFIX + ":searchCondition";
    public static final String DATATYPE_NAME = PREFIX + ":datatypeName";
    public static final String DATATYPE_LENGTH = PREFIX + ":datatypeLength";
    public static final String DATATYPE_PRECISION = PREFIX + ":datatypePrecision";
    public static final String DATATYPE_SCALE = PREFIX + ":datatypeScale";
    public static final String DEFAULT_VALUE = PREFIX + ":defaultValue";
    public static final String DEFAULT_PRECISION = PREFIX + ":defaultprecision";
    public static final String VALUE = PREFIX + ":value";
    public static final String DROP_BEHAVIOR = PREFIX + ":dropBehavior";
    public static final String PROPERTY_VALUE = PREFIX + ":propValue";
    public static final String PROBLEM_LEVEL = PREFIX + ":problemLevel";
    public static final String GRANT_PRIVILEGE = PREFIX + ":grantPrivilege";
    public static final String ALL_PRIVILEGES = PREFIX + ":allPrivileges";
    public static final String WITH_GRANT_OPTION = PREFIX + ":withGrantOption";
    public static final String GRANTEE = PREFIX + ":grantee";

    public static final String CREATE_VIEW_QUERY_EXPRESSION = PREFIX + ":queryExpression";
    public static final String CREATE_VIEW_OPTION_CLAUSE = PREFIX + ":createViewOption";

    public static final String MESSAGE = PREFIX + ":message";

    public static final String EXISTING_NAME = PREFIX + ":existingName";
    public static final String COLLATION_CHARACTER_SET_NAME = PREFIX + ":characterSetName";
    public static final String COLLATION_SOURCE = PREFIX + ":collationSource";
    public static final String PAD_ATTRIBUTE = PREFIX + ":padAttribute";

    public static final String SOURCE_CHARACTER_SET_NAME = PREFIX + ":sourceCharacterSetName";
    public static final String TARGET_CHARACTER_SET_NAME = PREFIX + ":targetCharacterSetName";


    public static final String DROP_OPTION = PREFIX + ":dropOption";
    public static final String COLUMN_ATTRIBUTE = PREFIX + ":columnAttribute";

    /**
     * value constraints
     */
    public static final String PAD_ATTRIBUTE_PAD = "PAD SPACE";
    public static final String PAD_ATTRIBUTE_NO_PAD = "NO PAD";
    public static final String DEFAULT_ID_LITERAL = "LITERAL";
    public static final String DEFAULT_ID_DATETIME = "DATETIME";
    public static final String DEFAULT_ID_USER = "USER";
    public static final String DEFAULT_ID_CURRENT_USER = "CURRENT_USER";
    public static final String DEFAULT_ID_SESSION_USER = "SESSION_USER";
    public static final String DEFAULT_ID_SYSTEM_USER = "SYSTEM_USER";
    public static final String DEFAULT_ID_NULL = "NULL";

    /*
     * node child types
     */
    public static final String TYPE_DROP_OPTION = PREFIX + ":dropOption";
    public static final String TYPE_CONSTRAINT_ATTRIBUTE = PREFIX + ":constraintAttribute";
}
