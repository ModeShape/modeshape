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

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * Lexicon for DDL concepts.
 */
public class StandardDdlLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/ddl/1.0";
        public static final String PREFIX = "ddl";
    }

    public static final Name PARSER_ID = new BasicName(Namespace.URI, "parserId");

    public static final Name STATEMENTS_CONTAINER = new BasicName(Namespace.URI, "statements");
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

    public static final Name TYPE_MISSING_TERMINATOR = new BasicName(Namespace.URI, "missingTerminator");
    public static final Name TYPE_UNKNOWN_STATEMENT = new BasicName(Namespace.URI, "unknownStatement");

    public static final Name TYPE_OPERATION = new BasicName(Namespace.URI, "operation");
    public static final Name TYPE_OPERAND = new BasicName(Namespace.URI, "operand");
    public static final Name TYPE_STATEMENT = new BasicName(Namespace.URI, "statement");

    public static final Name TYPE_CREATEABLE = new BasicName(Namespace.URI, "creatable");
    public static final Name TYPE_ALTERABLE = new BasicName(Namespace.URI, "alterable");
    public static final Name TYPE_DROPPABLE = new BasicName(Namespace.URI, "droppable");
    public static final Name TYPE_INSERTABLE = new BasicName(Namespace.URI, "insertable");
    public static final Name TYPE_SETTABLE = new BasicName(Namespace.URI, "settable");
    public static final Name TYPE_GRANTABLE = new BasicName(Namespace.URI, "grantable");
    public static final Name TYPE_REVOKABLE = new BasicName(Namespace.URI, "revokable");

    public static final Name TYPE_SCHEMA_OPERAND = new BasicName(Namespace.URI, "schemaOperand");
    public static final Name TYPE_TABLE_OPERAND = new BasicName(Namespace.URI, "tableOperand");
    public static final Name TYPE_DOMAIN_OPERAND = new BasicName(Namespace.URI, "domainOperand");
    public static final Name TYPE_VIEW_OPERAND = new BasicName(Namespace.URI, "viewOperand");
    public static final Name TYPE_ASSERTION_OPERAND = new BasicName(Namespace.URI, "assertionOperand");
    public static final Name TYPE_CHARACTER_SET_OPERAND = new BasicName(Namespace.URI, "characterSetOperand");
    public static final Name TYPE_COLLATION_OPERAND = new BasicName(Namespace.URI, "collationOperand");
    public static final Name TYPE_TRANSLATION_OPERAND = new BasicName(Namespace.URI, "translationOperand");
    public static final Name TYPE_COLUMN_OPERAND = new BasicName(Namespace.URI, "columnOperand");
    public static final Name TYPE_TABLE_CONSTRAINT_OPERAND = new BasicName(Namespace.URI, "tableConstraintOperand");
    public static final Name TYPE_REFERENCE_OPERAND = new BasicName(Namespace.URI, "referenceOperand");

    public static final Name TYPE_CREATE_TABLE_STATEMENT = new BasicName(Namespace.URI, "createTableStatement");
    public static final Name TYPE_CREATE_SCHEMA_STATEMENT = new BasicName(Namespace.URI, "createSchemaStatement");
    public static final Name TYPE_CREATE_VIEW_STATEMENT = new BasicName(Namespace.URI, "createViewStatement");
    public static final Name TYPE_CREATE_DOMAIN_STATEMENT = new BasicName(Namespace.URI, "createDomainStatement");
    public static final Name TYPE_CREATE_ASSERTION_STATEMENT = new BasicName(Namespace.URI, "createAssertionStatement");
    public static final Name TYPE_CREATE_CHARACTER_SET_STATEMENT = new BasicName(Namespace.URI, "createCharacterSetStatement");
    public static final Name TYPE_CREATE_COLLATION_STATEMENT = new BasicName(Namespace.URI, "createCollationStatement");
    public static final Name TYPE_CREATE_TRANSLATION_STATEMENT = new BasicName(Namespace.URI, "createTranslationStatement");

    public static final Name TYPE_ALTER_TABLE_STATEMENT = new BasicName(Namespace.URI, "alterTableStatement");
    public static final Name TYPE_ALTER_DOMAIN_STATEMENT = new BasicName(Namespace.URI, "alterDomainStatement");
    public static final Name TYPE_GRANT_STATEMENT = new BasicName(Namespace.URI, "grantStatement");
    public static final Name TYPE_GRANT_ON_TABLE_STATEMENT = new BasicName(Namespace.URI, "grantOnTableStatement");
    public static final Name TYPE_GRANT_ON_DOMAIN_STATEMENT = new BasicName(Namespace.URI, "grantOnDomainStatement");
    public static final Name TYPE_GRANT_ON_COLLATION_STATEMENT = new BasicName(Namespace.URI, "grantOnCollationStatement");
    public static final Name TYPE_GRANT_ON_CHARACTER_SET_STATEMENT = new BasicName(Namespace.URI, "grantOnCharacterSetStatement");
    public static final Name TYPE_GRANT_ON_TRANSLATION_STATEMENT = new BasicName(Namespace.URI, "grantOnTranslationStatement");
    public static final Name TYPE_REVOKE_STATEMENT = new BasicName(Namespace.URI, "revokeStatement");
    public static final Name TYPE_REVOKE_ON_TABLE_STATEMENT = new BasicName(Namespace.URI, "revokeOnTableStatement");
    public static final Name TYPE_REVOKE_ON_DOMAIN_STATEMENT = new BasicName(Namespace.URI, "revokeOnDomainStatement");
    public static final Name TYPE_REVOKE_ON_COLLATION_STATEMENT = new BasicName(Namespace.URI, "revokeOnCollationStatement");
    public static final Name TYPE_REVOKE_ON_CHARACTER_SET_STATEMENT = new BasicName(Namespace.URI,
                                                                                    "revokeOnCharacterSetStatement");
    public static final Name TYPE_REVOKE_ON_TRANSLATION_STATEMENT = new BasicName(Namespace.URI, "revokeOnTranslationStatement");
    public static final Name TYPE_SET_STATEMENT = new BasicName(Namespace.URI, "setStatement");
    public static final Name TYPE_INSERT_STATEMENT = new BasicName(Namespace.URI, "insertStatement");

    public static final Name TYPE_DROP_SCHEMA_STATEMENT = new BasicName(Namespace.URI, "dropSchemaStatement");
    public static final Name TYPE_DROP_TABLE_STATEMENT = new BasicName(Namespace.URI, "dropTableStatement");
    public static final Name TYPE_DROP_VIEW_STATEMENT = new BasicName(Namespace.URI, "dropViewStatement");
    public static final Name TYPE_DROP_DOMAIN_STATEMENT = new BasicName(Namespace.URI, "dropDomainStatement");
    public static final Name TYPE_DROP_CHARACTER_SET_STATEMENT = new BasicName(Namespace.URI, "dropCharacterSetStatement");
    public static final Name TYPE_DROP_COLLATION_STATEMENT = new BasicName(Namespace.URI, "dropCollationStatement");
    public static final Name TYPE_DROP_TRANSLATION_STATEMENT = new BasicName(Namespace.URI, "dropTranslationStatement");
    public static final Name TYPE_DROP_ASSERTION_STATEMENT = new BasicName(Namespace.URI, "dropAssertionStatement");

    public static final Name TYPE_DROP_COLUMN_DEFINITION = new BasicName(Namespace.URI, "dropColumnDefinition");
    public static final Name TYPE_ALTER_COLUMN_DEFINITION = new BasicName(Namespace.URI, "alterColumnDefinition");
    public static final Name TYPE_ADD_COLUMN_DEFINITION = new BasicName(Namespace.URI, "addColumnDefinition");
    public static final Name TYPE_DROP_TABLE_CONSTRAINT_DEFINITION = new BasicName(Namespace.URI, "dropTableConstraintDefinition");
    public static final Name TYPE_ADD_TABLE_CONSTRAINT_DEFINITION = new BasicName(Namespace.URI, "addTableConstraintDefinition");

    public static final Name TYPE_PROBLEM = new BasicName(Namespace.URI, "problem");
    public static final Name TYPE_COLUMN_DEFINITION = new BasicName(Namespace.URI, "columnDefinition");
    public static final Name TYPE_COLUMN_REFERENCE = new BasicName(Namespace.URI, "columnReference");
    public static final Name TYPE_TABLE_CONSTRAINT = new BasicName(Namespace.URI, "tableConstraint");
    public static final Name TYPE_STATEMENT_OPTION = new BasicName(Namespace.URI, "statementOption");
    public static final Name TYPE_TABLE_REFERENCE = new BasicName(Namespace.URI, "tableReference");
    public static final Name TYPE_FK_COLUMN_REFERENCE = new BasicName(Namespace.URI, "fkColumnReference");
    public static final Name TYPE_CLAUSE = new BasicName(Namespace.URI, "clause");

    public static final Name DDL_EXPRESSION = new BasicName(Namespace.URI, "expression");
    public static final Name DDL_ORIGINAL_EXPRESSION = new BasicName(Namespace.URI, "originalExpression");
    public static final Name DDL_START_LINE_NUMBER = new BasicName(Namespace.URI, "startLineNumber");
    public static final Name DDL_START_COLUMN_NUMBER = new BasicName(Namespace.URI, "startColumnNumber");
    public static final Name DDL_START_CHAR_INDEX = new BasicName(Namespace.URI, "startCharIndex");
    // public static final Name DDL_LENGTH = new BasicName(Namespace.URI, "length");

    /*
     * node property names
     */
    public static final Name NAME = new BasicName(Namespace.URI, "name");
    public static final Name OPTION = new BasicName(Namespace.URI, "option");
    public static final Name TYPE = new BasicName(Namespace.URI, "type");
    public static final Name NEW_NAME = new BasicName(Namespace.URI, "newName");
    public static final Name SQL = new BasicName(Namespace.URI, "sql");
    public static final Name TEMPORARY = new BasicName(Namespace.URI, "temporary");
    public static final Name ON_COMMIT_VALUE = new BasicName(Namespace.URI, "onCommitValue");
    public static final Name NULLABLE = new BasicName(Namespace.URI, "nullable");
    public static final Name DEFAULT_OPTION = new BasicName(Namespace.URI, "defaultOption");
    public static final Name COLLATION_NAME = new BasicName(Namespace.URI, "collationName");
    public static final Name CONSTRAINT_TYPE = new BasicName(Namespace.URI, "constraintType");
    public static final Name DEFERRABLE = new BasicName(Namespace.URI, "deferrable");
    public static final Name CHECK_SEARCH_CONDITION = new BasicName(Namespace.URI, "searchCondition");
    public static final Name DATATYPE_NAME = new BasicName(Namespace.URI, "datatypeName");
    public static final Name DATATYPE_LENGTH = new BasicName(Namespace.URI, "datatypeLength");
    public static final Name DATATYPE_PRECISION = new BasicName(Namespace.URI, "datatypePrecision");
    public static final Name DATATYPE_SCALE = new BasicName(Namespace.URI, "datatypeScale");
    public static final Name DEFAULT_VALUE = new BasicName(Namespace.URI, "defaultValue");
    public static final Name DEFAULT_PRECISION = new BasicName(Namespace.URI, "defaultprecision");
    public static final Name VALUE = new BasicName(Namespace.URI, "value");
    public static final Name DROP_BEHAVIOR = new BasicName(Namespace.URI, "dropBehavior");
    public static final Name PROPERTY_VALUE = new BasicName(Namespace.URI, "propValue");
    public static final Name PROBLEM_LEVEL = new BasicName(Namespace.URI, "problemLevel");
    public static final Name GRANT_PRIVILEGE = new BasicName(Namespace.URI, "grantPrivilege");
    public static final Name ALL_PRIVILEGES = new BasicName(Namespace.URI, "allPrivileges");
    public static final Name WITH_GRANT_OPTION = new BasicName(Namespace.URI, "withGrantOption");
    public static final Name GRANTEE = new BasicName(Namespace.URI, "grantee");

    public static final Name CREATE_VIEW_QUERY_EXPRESSION = new BasicName(Namespace.URI, "queryExpression");
    public static final Name CREATE_VIEW_OPTION_CLAUSE = new BasicName(Namespace.URI, "createViewOption");

    public static final Name MESSAGE = new BasicName(Namespace.URI, "message");

    /*
     * node child types
     */

    public static final Name DROP_OPTION_TYPE = new BasicName(Namespace.URI, "dropOption");
    public static final Name COLUMN_ATTRIBUTE_TYPE = new BasicName(Namespace.URI, "columnAttribute");
    public static final Name CONSTRAINT_ATTRIBUTE_TYPE = new BasicName(Namespace.URI, "constraintAttribute ");
}
