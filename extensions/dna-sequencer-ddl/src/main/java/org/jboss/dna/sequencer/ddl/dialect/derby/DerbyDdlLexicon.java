/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.ddl.dialect.derby;

import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.basic.BasicName;
import org.jboss.dna.sequencer.ddl.StandardDdlLexicon;

/**
 *
 */
public class DerbyDdlLexicon extends StandardDdlLexicon {

    public static class Namespace {
        public static final String URI = "http://www.jboss.org/dna/ddl/derby/1.0";
        public static final String PREFIX = "derbyddl";
    }
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
	 * ===> CREATE FUNCTION
	 * ===> CREATE INDEX
	 * ===> CREATE PROCEDURE
	 * ===> CREATE ROLE
	 * ===> CREATE SYNONYM
	 * ===> CREATE TRIGGER
	 * 
	 *		ALTER TABLE
	 *
	 * ===> GRANT
	 * ===> LOCK TABLE
	 * ===> RENAME TABLE
	 * ===> RENAME INDEX
	 * ===> SET
	 * ===> DECLARE GLOBAL TEMPORARY TABLE
     */
 
    public static final Name TYPE_CREATE_FUNCTION_STATEMENT 		= new BasicName(Namespace.URI, "createFunctionStatement");
    public static final Name TYPE_CREATE_INDEX_STATEMENT 			= new BasicName(Namespace.URI, "createIndexStatement");
    public static final Name TYPE_CREATE_PROCEDURE_STATEMENT 		= new BasicName(Namespace.URI, "createProcedureStatement");
    public static final Name TYPE_CREATE_ROLE_STATEMENT 			= new BasicName(Namespace.URI, "createRoleStatement");
    public static final Name TYPE_CREATE_SYNONYM_STATEMENT 		= new BasicName(Namespace.URI, "createSynonymStatement");
    public static final Name TYPE_CREATE_TRIGGER_STATEMENT 		= new BasicName(Namespace.URI, "createTriggerStatement");
    public static final Name TYPE_LOCK_TABLE_STATEMENT 			= new BasicName(Namespace.URI, "lockTableStatement");
    public static final Name TYPE_RENAME_TABLE_STATEMENT 			= new BasicName(Namespace.URI, "renameTableStatement");
    public static final Name TYPE_RENAME_INDEX_STATEMENT 			= new BasicName(Namespace.URI, "renameIndexStatement");
    public static final Name TYPE_DECLARE_GLOBAL_TEMPORARY_TABLE_STATEMENT = new BasicName(Namespace.URI, "declareGlobalTemporaryTableStatement");

    public static final Name TYPE_DROP_FUNCTION_STATEMENT 		= new BasicName(Namespace.URI, "dropFunctionStatement");
    public static final Name TYPE_DROP_INDEX_STATEMENT 			= new BasicName(Namespace.URI, "dropIndexStatement");
    public static final Name TYPE_DROP_PROCEDURE_STATEMENT 		= new BasicName(Namespace.URI, "dropProcedureStatement");
    public static final Name TYPE_DROP_ROLE_STATEMENT 			= new BasicName(Namespace.URI, "dropRoleStatement");
    public static final Name TYPE_DROP_SYNONYM_STATEMENT 			= new BasicName(Namespace.URI, "dropSynonymStatement");
    public static final Name TYPE_DROP_TRIGGER_STATEMENT 			= new BasicName(Namespace.URI, "dropTriggerStatement");
    
    public static final Name UNIQUE_INDEX 			= new BasicName(Namespace.URI, "unique");
    public static final Name TABLE_NAME 			= new BasicName(Namespace.URI, "tableName");
    public static final Name GENERATED_COLUMN_SPEC_CLAUSE = new BasicName(Namespace.URI, "generatedColumnSpecClause");
}
