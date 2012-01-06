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
package org.modeshape.sequencer.ddl.dialect.derby;

import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.Namespace.*;

/**
 *
 */
public class DerbyDdlLexicon extends StandardDdlLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/ddl/derby/1.0";
        public static final String PREFIX = "derbyddl";
    }
    /*
     * mixin types
     * 
     * SQL-92 Spec
     * 
     *      CREATE SCHEMA
     *      CREATE DOMAIN
     *      CREATE [ { GLOBAL | LOCAL } TEMPORARY ] TABLE
     *      CREATE VIEW         
     *      CREATE ASSERTION 
     *      CREATE CHARACTER SET 
     *      CREATE COLLATION 
     *      CREATE TRANSLATION
     * ===> CREATE FUNCTION
     * ===> CREATE INDEX
     * ===> CREATE PROCEDURE
     * ===> CREATE ROLE
     * ===> CREATE SYNONYM
     * ===> CREATE TRIGGER
     * 
     *      ALTER TABLE
     *
     * ===> GRANT
     * ===> LOCK TABLE
     * ===> RENAME TABLE
     * ===> RENAME INDEX
     * ===> SET
     * ===> DECLARE GLOBAL TEMPORARY TABLE
     */
 
    public static final String TYPE_CREATE_FUNCTION_STATEMENT     = PREFIX + ":createFunctionStatement";
    public static final String TYPE_CREATE_INDEX_STATEMENT        = PREFIX + ":createIndexStatement";
    public static final String TYPE_CREATE_PROCEDURE_STATEMENT    = PREFIX + ":createProcedureStatement";
    public static final String TYPE_CREATE_ROLE_STATEMENT         = PREFIX + ":createRoleStatement";
    public static final String TYPE_CREATE_SYNONYM_STATEMENT      = PREFIX + ":createSynonymStatement";
    public static final String TYPE_CREATE_TRIGGER_STATEMENT      = PREFIX + ":createTriggerStatement";
    public static final String TYPE_LOCK_TABLE_STATEMENT          = PREFIX + ":lockTableStatement";
    public static final String TYPE_RENAME_TABLE_STATEMENT        = PREFIX + ":renameTableStatement";
    public static final String TYPE_RENAME_INDEX_STATEMENT        = PREFIX + ":renameIndexStatement";
    public static final String TYPE_DECLARE_GLOBAL_TEMPORARY_TABLE_STATEMENT = PREFIX + ":declareGlobalTemporaryTableStatement";

    public static final String TYPE_DROP_FUNCTION_STATEMENT       = PREFIX + ":dropFunctionStatement";
    public static final String TYPE_DROP_INDEX_STATEMENT          = PREFIX + ":dropIndexStatement";
    public static final String TYPE_DROP_PROCEDURE_STATEMENT      = PREFIX + ":dropProcedureStatement";
    public static final String TYPE_DROP_ROLE_STATEMENT           = PREFIX + ":dropRoleStatement";
    public static final String TYPE_DROP_SYNONYM_STATEMENT        = PREFIX + ":dropSynonymStatement";
    public static final String TYPE_DROP_TRIGGER_STATEMENT        = PREFIX + ":dropTriggerStatement";
    
    public static final String TYPE_FUNCTION_PARAMETER            = PREFIX + ":functionParameter";
    public static final String TYPE_INDEX_COLUMN_REFERENCE        = PREFIX + ":indexColumnReference";
    public static final String TYPE_GRANT_ON_FUNCTION_STATEMENT   = PREFIX + ":grantOnFunctionStatement";
    public static final String TYPE_GRANT_ON_PROCEDURE_STATEMENT  = PREFIX + ":grantOnProcedureStatement";
    public static final String TYPE_GRANT_ROLES_STATEMENT         = PREFIX + ":grantRolesStatement";
    
    public static final String UNIQUE_INDEX                       = PREFIX + ":unique";
    public static final String ORDER                              = PREFIX + ":order";
    public static final String TABLE_NAME                         = PREFIX + ":tableName";
    public static final String ROLE_NAME                          = PREFIX +  ":roleName";
    public static final String GENERATED_COLUMN_SPEC_CLAUSE       = PREFIX + ":generatedColumnSpecClause";
    public static final String IS_TABLE_TYPE                      = PREFIX + ":isTableType";
    public static final String PARAMETER_STYLE                    = PREFIX + ":parameterStyle";
}
