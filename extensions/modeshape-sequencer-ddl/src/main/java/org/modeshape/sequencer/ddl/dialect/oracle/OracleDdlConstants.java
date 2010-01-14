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

import java.util.Arrays;
import java.util.List;

import org.modeshape.graph.property.Name;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 * Oracle-specific constants including key words and statement start phrases.
 * 
 * @author blafond
 *
 */
public interface OracleDdlConstants extends DdlConstants {
	
	public static final String[] CUSTOM_KEYWORDS = {
		"ANALYZE", "ASSOCIATE", "TRUNCATE",	"MATERIALIZED",	"SAVEPOINT", "PURGE", "LOCK", "TRIGGER", "EXPLAIN",	"PLAN",	"DIMENSION", 
		"DIRECTORY", "DATABASE", "CONTROLFILE", "DISKGROUP", "INDEXTYPE", "SYNONYM", "SEQUENCE", "LIBRARY", "CLUSTER", "OUTLINE",
		"PACKAGE", "SPFILE", "PFILE", "AUDIT", "COMMIT", "PURGE", "MERGE", "RENAME", "FLASHBACK", "NOAUDIT", "DISASSOCIATE", 
		"NESTED", "REVOKE", "COMMENT", INDEX, "VARCHAR2", "NVARCHAR2", "NUMBER",
	  	"BINARY_FLOAT", "BINARY_DOUBLE", "LONG", "RAW", "BLOB", "CLOB", "NCLOB", "BFILE", "INTERVAL"
	};
	
	interface OracleStatementStartPhrases {
		static final String[] STMT_ALTER_CLUSTER = {ALTER, "CLUSTER"};
		static final String[] STMT_ALTER_DATABASE = {ALTER, "DATABASE"};
		static final String[] STMT_ALTER_DIMENSION = {ALTER, "DIMENSION"};
		static final String[] STMT_ALTER_DISKGROUP = {ALTER, "DISKGROUP"};
		static final String[] STMT_ALTER_FUNCTION = {ALTER, "FUNCTION"};
		static final String[] STMT_ALTER_INDEX = {ALTER, INDEX};
		static final String[] STMT_ALTER_INDEXTYPE = {ALTER, "INDEXTYPE"};
		static final String[] STMT_ALTER_JAVA = {ALTER, "JAVA"};
		static final String[] STMT_ALTER_MATERIALIZED = {ALTER, "MATERIALIZED"};
		static final String[] STMT_ALTER_OPERATOR = {ALTER, "OPERATOR"};
		static final String[] STMT_ALTER_OUTLINE = {ALTER, "OUTLINE"};
		static final String[] STMT_ALTER_PACKAGE = {ALTER, "PACKAGE"};
		static final String[] STMT_ALTER_PROCEDURE = {ALTER, "PROCEDURE"};
		static final String[] STMT_ALTER_PROFILE = {ALTER, "PROFILE"};
		static final String[] STMT_ALTER_RESOURCE = {ALTER, "RESOURCE"};
		static final String[] STMT_ALTER_ROLE = {ALTER, "ROLE"};
		static final String[] STMT_ALTER_ROLLBACK = {ALTER, "ROLLBACK"};
		static final String[] STMT_ALTER_SEQUENCE = {ALTER, "SEQUENCE"};
		static final String[] STMT_ALTER_SESSION = {ALTER, "SESSION"};
		static final String[] STMT_ALTER_SYSTEM = {ALTER, "SYSTEM"};
		static final String[] STMT_ALTER_TABLESPACE = {ALTER, "TABLESPACE"};
		static final String[] STMT_ALTER_TRIGGER = {ALTER, "TRIGGER"};
		static final String[] STMT_ALTER_TYPE = {ALTER, "TYPE"};
		static final String[] STMT_ALTER_USER = {ALTER, "USER"};
		static final String[] STMT_ALTER_VIEW = {ALTER, "VIEW"};
		
	    static final String[][] ALTER_PHRASES = { STMT_ALTER_CLUSTER, STMT_ALTER_DATABASE, 
			STMT_ALTER_DIMENSION, STMT_ALTER_DISKGROUP, STMT_ALTER_FUNCTION, STMT_ALTER_INDEX, STMT_ALTER_INDEXTYPE, STMT_ALTER_JAVA, 
			STMT_ALTER_MATERIALIZED, STMT_ALTER_OPERATOR, STMT_ALTER_OUTLINE, STMT_ALTER_PACKAGE, STMT_ALTER_PROCEDURE, 
			STMT_ALTER_PROFILE, STMT_ALTER_RESOURCE, STMT_ALTER_ROLE, STMT_ALTER_ROLLBACK, STMT_ALTER_SEQUENCE, 
			STMT_ALTER_SESSION, STMT_ALTER_SYSTEM, STMT_ALTER_TABLESPACE, STMT_ALTER_TRIGGER, STMT_ALTER_TYPE, 
			STMT_ALTER_USER, STMT_ALTER_VIEW  
		};
		
		static final String[] STMT_ANALYZE = {"ANALYZE"};
		static final String[] STMT_ASSOCIATE_STATISTICS = {"ASSOCIATE", "STATISTICS"};
		static final String[] STMT_AUDIT = {"AUDIT"};
		
		/*
                COMMIT [ WORK ] [ [ COMMENT string ]
                    | [ WRITE [ IMMEDIATE | BATCH ] [ WAIT | NOWAIT ] ]
                    | FORCE string [, integer ] ] ;
                
                COMMIT WORK COMMENT "some comment"
                
                COMMIT COMMENT "some comment"
                
                COMMIT WORK WRITE [ IMMEDIATE | BATCH ] [ WAIT | NOWAIT ]
                
                COMMIT WRITE IMMEDIATE NOWAIT;
                
                COMMIT WORK WRITE IMMEDIATE NOWAIT;
                
                COMMIT FORCE "some string", 10;
		 */
		static final String[] STMT_COMMIT_WORK = {"COMMIT", "WORK"};
		static final String[] STMT_COMMIT_WRITE = {"COMMIT", "WRITE"};
		static final String[] STMT_COMMIT_FORCE = {"COMMIT", "FORCE"};
		static final String[] STMT_COMMIT       = {"COMMIT"}; // DON"T REGISTER THIS STMT
		static final String[] STMT_COMMENT_ON = {"COMMENT", "ON"};
		
		static final String[] STMT_CREATE_CLUSTER = {CREATE, "CLUSTER"};
		static final String[] STMT_CREATE_CONTEXT = {CREATE, "CONTEXT"};
		static final String[] STMT_CREATE_CONTROLFILE = {CREATE, "CONTROLFILE"};
		static final String[] STMT_CREATE_DATABASE = {CREATE, "DATABASE"};
		static final String[] STMT_CREATE_DIMENSION = {CREATE, "DIMENSION"};
		static final String[] STMT_CREATE_DIRECTORY = {CREATE, "DIRECTORY"};
		static final String[] STMT_CREATE_DISKGROUP = {CREATE, "DISKGROUP"};
		static final String[] STMT_CREATE_FUNCTION = {CREATE, "FUNCTION"};   // PARSE UNTIL '/'
		static final String[] STMT_CREATE_INDEX = {CREATE, "INDEX"};
		static final String[] STMT_CREATE_INDEXTYPE = {CREATE, "INDEXTYPE"};
		static final String[] STMT_CREATE_JAVA = {CREATE, "JAVA"};
		static final String[] STMT_CREATE_LIBRARY = {CREATE, "LIBRARY"}; // PARSE UNTIL '/'
		static final String[] STMT_CREATE_MATERIALIZED_VIEW = {CREATE, "MATERIALIZED", "VIEW"};
		static final String[] STMT_CREATE_MATERIALIZED_VEIW_LOG = {CREATE, "MATERIALIZED", "VIEW", "LOG"};
		static final String[] STMT_CREATE_OPERATOR = {CREATE, "OPERATOR"};
		static final String[] STMT_CREATE_OR_REPLACE_DIRECTORY = {CREATE, "OR", "REPLACE", "DIRECTORY"};    // PARSE UNTIL '/'
		static final String[] STMT_CREATE_OR_REPLACE_FUNCTION = {CREATE, "OR", "REPLACE", "FUNCTION"};    // PARSE UNTIL '/'
		static final String[] STMT_CREATE_OR_REPLACE_LIBRARY = {CREATE, "OR", "REPLACE", "LIBRARY"};    // PARSE UNTIL '/'
		static final String[] STMT_CREATE_OR_REPLACE_OUTLINE = {CREATE,"OR", "REPLACE", "OUTLINE"};
		static final String[] STMT_CREATE_OR_REPLACE_PUBLIC_OUTLINE = {CREATE,"OR", "REPLACE", "PUBLIC", "OUTLINE"}; // TODO: BML
		static final String[] STMT_CREATE_OR_REPLACE_PRIVATE_OUTLINE = {CREATE,"OR", "REPLACE", "PRIVATE", "OUTLINE"}; // TODO: BML
		static final String[] STMT_CREATE_OR_REPLACE_PACKAGE = {CREATE,"OR", "REPLACE", "PACKAGE"};
		static final String[] STMT_CREATE_OR_REPLACE_PROCEDURE = {CREATE,"OR", "REPLACE", "PROCEDURE"}; // PARSE UNTIL '/'
		static final String[] STMT_CREATE_OR_REPLACE_PUBLIC_SYNONYM = {CREATE, "OR", "REPLACE", "PUBLIC", "SYNONYM"};
		static final String[] STMT_CREATE_OR_REPLACE_SYNONYM = {CREATE, "OR", "REPLACE", "SYNONYM"};
		static final String[] STMT_CREATE_OR_REPLACE_TRIGGER = {CREATE, "OR", "REPLACE", "TRIGGER"}; // PARSE UNTIL '/
		static final String[] STMT_CREATE_OR_REPLACE_TYPE = {CREATE, "OR", "REPLACE", "TYPE"};
		static final String[] STMT_CREATE_OUTLINE = {CREATE, "OUTLINE"};
		static final String[] STMT_CREATE_PACKAGE = {CREATE, "PACKAGE"};   // PARSE UNTIL '/'
		static final String[] STMT_CREATE_PFILE = {CREATE, "PFILE"};
		static final String[] STMT_CREATE_PROCEDURE = {CREATE, "PROCEDURE"}; // PARSE UNTIL '/'
		static final String[] STMT_CREATE_PROFILE = {CREATE, "PROFILE"};
		static final String[] STMT_CREATE_PUBLIC_DATABASE = {CREATE, "PUBLIC", "DATABASE"};
		static final String[] STMT_CREATE_PUBLIC_ROLLBACK = {CREATE, "PUBLIC", "ROLLBACK"};
		static final String[] STMT_CREATE_PUBLIC_SYNONYM = {CREATE, "PUBLIC", "SYNONYM"};
		static final String[] STMT_CREATE_ROLE = {CREATE, "ROLE"};
		static final String[] STMT_CREATE_ROLLBACK = {CREATE, "ROLLBACK"};
		static final String[] STMT_CREATE_SEQUENCE = {CREATE, "SEQUENCE"};
		static final String[] STMT_CREATE_SPFILE = {CREATE, "SPFILE"};
		static final String[] STMT_CREATE_SYNONYM = {CREATE, "SYNONYM"};
		static final String[] STMT_CREATE_TABLESPACE = {CREATE, "TABLESPACE"};
		static final String[] STMT_CREATE_TRIGGER = {CREATE, "TRIGGER"};
		static final String[] STMT_CREATE_TYPE = {CREATE, "TYPE"};
		static final String[] STMT_CREATE_USER = {CREATE, "USER"};
		static final String[] STMT_CREATE_UNIQUE_INDEX = {CREATE, "UNIQUE", "INDEX"};
		static final String[] STMT_CREATE_BITMAP_INDEX = {CREATE, "BITMAP", "INDEX"};
	
		public static final String[][] CREATE_PHRASES = {
			STMT_CREATE_CLUSTER, STMT_CREATE_CONTEXT, STMT_CREATE_CONTROLFILE, STMT_CREATE_DATABASE, STMT_CREATE_DIMENSION, 
			STMT_CREATE_DIRECTORY, STMT_CREATE_DISKGROUP, STMT_CREATE_FUNCTION, STMT_CREATE_INDEX, STMT_CREATE_INDEXTYPE, 
			STMT_CREATE_JAVA, STMT_CREATE_MATERIALIZED_VIEW, STMT_CREATE_MATERIALIZED_VEIW_LOG, STMT_CREATE_OPERATOR, 
			STMT_CREATE_OR_REPLACE_DIRECTORY, STMT_CREATE_OR_REPLACE_FUNCTION, STMT_CREATE_LIBRARY,
            STMT_CREATE_OR_REPLACE_LIBRARY, STMT_CREATE_OR_REPLACE_OUTLINE, STMT_CREATE_OR_REPLACE_PROCEDURE,
			STMT_CREATE_OR_REPLACE_PUBLIC_SYNONYM, STMT_CREATE_OR_REPLACE_SYNONYM, STMT_CREATE_OR_REPLACE_PACKAGE, STMT_CREATE_OR_REPLACE_TRIGGER,
			STMT_CREATE_OR_REPLACE_TYPE, STMT_CREATE_OUTLINE, STMT_CREATE_PACKAGE, STMT_CREATE_PFILE, STMT_CREATE_PROCEDURE, 
			STMT_CREATE_PROFILE, STMT_CREATE_PUBLIC_DATABASE, STMT_CREATE_PUBLIC_ROLLBACK, STMT_CREATE_PUBLIC_SYNONYM, STMT_CREATE_ROLE,
			STMT_CREATE_ROLLBACK, STMT_CREATE_SEQUENCE, STMT_CREATE_SPFILE, STMT_CREATE_SYNONYM, STMT_CREATE_TABLESPACE, STMT_CREATE_TRIGGER, 
			STMT_CREATE_TYPE, STMT_CREATE_USER, STMT_CREATE_UNIQUE_INDEX, STMT_CREATE_BITMAP_INDEX,
			STMT_CREATE_TABLESPACE, STMT_CREATE_PROCEDURE
		};
		
	      static final String[][] SLASHED_STMT_PHRASES = {
	          STMT_CREATE_FUNCTION, 
	          STMT_CREATE_LIBRARY, 
	          STMT_CREATE_OR_REPLACE_DIRECTORY, 
	          STMT_CREATE_OR_REPLACE_FUNCTION,
	          STMT_CREATE_OR_REPLACE_LIBRARY, 
	          STMT_CREATE_OR_REPLACE_PROCEDURE, 
	          STMT_CREATE_OR_REPLACE_TRIGGER,
	          STMT_CREATE_PACKAGE, 
	          STMT_CREATE_PROCEDURE
	        };
		
		static final String[] STMT_DISASSOCIATE_STATISTICS = {"DISASSOCIATE", "STATISTICS"};
		
		static final String[] STMT_DROP_CLUSTER = {DROP, "CLUSTER"};
		static final String[] STMT_DROP_CONTEXT = {DROP, "CONTEXT"};
		static final String[] STMT_DROP_DATABASE = {DROP, "DATABASE"};
		static final String[] STMT_DROP_DIMENSION = {DROP, "DIMENSION"};
		static final String[] STMT_DROP_DIRECTORY = {DROP, "DIRECTORY"};
		static final String[] STMT_DROP_DISKGROUP = {DROP, "DISKGROUP"};
		static final String[] STMT_DROP_FUNCTION = {DROP, "FUNCTION"};
		static final String[] STMT_DROP_INDEX = {DROP, "INDEX"};
		static final String[] STMT_DROP_INDEXTYPE = {DROP, "INDEXTYPE"};
		static final String[] STMT_DROP_JAVA = {DROP, "JAVA"};
		static final String[] STMT_DROP_LIBRARY = {DROP, "LIBRARY"};
		static final String[] STMT_DROP_MATERIALIZED = {DROP, "MATERIALIZED"};
		static final String[] STMT_DROP_OPERATOR = {DROP, "OPERATOR"};
		static final String[] STMT_DROP_OUTLINE = {DROP, "OUTLINE"};
		static final String[] STMT_DROP_PACKAGE = {DROP, "PACKAGE"};
		static final String[] STMT_DROP_PROCEDURE = {DROP, "PROCEDURE"};
		static final String[] STMT_DROP_PROFILE = {DROP, "PROFILE"};
		static final String[] STMT_DROP_ROLE = {DROP, "ROLE"};
		static final String[] STMT_DROP_ROLLBACK = {DROP, "ROLLBACK"};
		static final String[] STMT_DROP_SEQUENCE = {DROP, "SEQUENCE"};
		static final String[] STMT_DROP_SYNONYM = {DROP, "SYNONYM"};
		static final String[] STMT_DROP_TABLESPACE = {DROP, "TABLESPACE"};
		static final String[] STMT_DROP_TRIGGER = {DROP, "TRIGGER"};
		static final String[] STMT_DROP_TYPE = {DROP, "TYPE"};
		static final String[] STMT_DROP_USER = {DROP, "USER"};
		
		static final String[] STMT_DROP_PUBLIC_DATABASE = {DROP, "PUBLIC", "DATABASE"};
		static final String[] STMT_DROP_PUBLIC_SYNONYM = {DROP, "PUBLIC", "SYNONYM"};
		
		static final String[][] DROP_PHRASES = {
			STMT_DROP_CLUSTER, STMT_DROP_CONTEXT, STMT_DROP_DATABASE, STMT_DROP_DIMENSION, STMT_DROP_DIRECTORY, STMT_DROP_DISKGROUP,
			STMT_DROP_FUNCTION, STMT_DROP_INDEX, STMT_DROP_INDEXTYPE, STMT_DROP_JAVA, STMT_DROP_LIBRARY, STMT_DROP_MATERIALIZED,
			STMT_DROP_OPERATOR, STMT_DROP_OUTLINE, STMT_DROP_PACKAGE, STMT_DROP_PROCEDURE, STMT_DROP_PROFILE, STMT_DROP_ROLE, 
			STMT_DROP_ROLLBACK, STMT_DROP_SEQUENCE, STMT_DROP_SYNONYM, STMT_DROP_TABLESPACE, STMT_DROP_TRIGGER, STMT_DROP_TYPE,
			STMT_DROP_USER, STMT_DROP_PUBLIC_DATABASE, STMT_DROP_PUBLIC_SYNONYM
		};
		
		static final String[] STMT_EXPLAIN_PLAN = {"EXPLAIN", "PLAN"};
		static final String[] STMT_FLASHBACK = {"FLASHBACK"};
		static final String[] STMT_LOCK_TABLE = {"LOCK", "TABLE"};
		static final String[] STMT_MERGE = {"MERGE"};
		static final String[] STMT_NOAUDIT = {"NOAUDIT"};
		static final String[] STMT_PURGE = {"PURGE"};
		static final String[] STMT_RENAME = {"RENAME"};
		static final String[] STMT_ROLLBACK_TO_SAVEPOINT = {"ROLLBACK", "TO", "SAVEPOINT"};
		static final String[] STMT_ROLLBACK_WORK = {"ROLLBACK", "WORK"};
		static final String[] STMT_ROLLBACK = {"ROLLBACK"};
		static final String[] STMT_SAVEPOINT = {"SAVEPOINT"};
		static final String[] STMT_SET_CONSTRAINT = {SET, "CONSTRAINT"};
		static final String[] STMT_SET_CONSTRAINTS = {SET, "CONSTRAINTS"};
		static final String[] STMT_SET_ROLE = {SET, "ROLE"};
		static final String[] STMT_SET_TRANSACTION = {SET, "TRANSACTION"};
		static final String[] STMT_TRUNCATE = {"TRUNCATE"};
		
		static final String[][] SET_PHRASES = {
			STMT_SET_CONSTRAINT, STMT_SET_CONSTRAINTS, STMT_SET_ROLE, STMT_SET_TRANSACTION
		};
		
		static final String[][] MISC_PHRASES = {
			STMT_ANALYZE, STMT_ASSOCIATE_STATISTICS, STMT_AUDIT, STMT_COMMIT_WORK, STMT_COMMIT_WRITE, STMT_COMMIT_FORCE, 
			STMT_COMMENT_ON, STMT_DISASSOCIATE_STATISTICS,
			STMT_EXPLAIN_PLAN, STMT_FLASHBACK, STMT_LOCK_TABLE, STMT_MERGE, STMT_NOAUDIT, STMT_PURGE, 
			STMT_RENAME, STMT_ROLLBACK_TO_SAVEPOINT, STMT_ROLLBACK_WORK, STMT_ROLLBACK, STMT_SAVEPOINT, STMT_TRUNCATE
		};
		
	    // CREATE TABLE, CREATE VIEW, and GRANT statements.
	    public final static Name[] VALID_SCHEMA_CHILD_STMTS = {
	    	StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT, 
	    	StandardDdlLexicon.TYPE_CREATE_VIEW_STATEMENT,
	    	StandardDdlLexicon.TYPE_GRANT_STATEMENT
	  	};
	    
	    public final static Name[] COMPLEX_STMT_TYPES = {
	    	OracleDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT
	    };
	}
	
	interface OracleDataTypes {
		static final String[] DTYPE_CHAR_ORACLE = {"CHAR"}; //CHAR(size [BYTE | CHAR])
		static final String[] DTYPE_VARCHAR2 = {"VARCHAR2"}; // VARCHAR2(size [BYTE | CHAR])
		static final String[] DTYPE_NVARCHAR2 = {"NVARCHAR2"}; // NVARCHAR2(size)
		static final String[] DTYPE_NUMBER = {"NUMBER"}; // NUMBER(p,s)
		static final String[] DTYPE_BINARY_FLOAT = {"BINARY_FLOAT "};
		static final String[] DTYPE_BINARY_DOUBLE = {"BINARY_DOUBLE"};
		static final String[] DTYPE_LONG = {"LONG"};
		static final String[] DTYPE_LONG_RAW = {"LONG", "RAW"};
		static final String[] DTYPE_RAW = {"RAW"}; // RAW(size)
		static final String[] DTYPE_BLOB = {"BLOB"};
		static final String[] DTYPE_CLOB = {"CLOB"};
		static final String[] DTYPE_NCLOB = {"NCLOB"};
		static final String[] DTYPE_BFILE = {"BFILE"};
		static final String[] DTYPE_INTERVAL_YEAR = {"INTERVAL", "YEAR"}; //INTERVAL YEAR (year_precision) TO MONTH
		static final String[] DTYPE_INTERVAL_DAY = {"INTERVAL", "DAY"}; //	INTERVAL DAY (day_precision) TO SECOND (fractional_seconds_precision)
	
	  	static final List<String[]> CUSTOM_DATATYPE_START_PHRASES = 
	  		Arrays.asList(new String[][] {
	  				DTYPE_CHAR_ORACLE, DTYPE_VARCHAR2, DTYPE_NVARCHAR2, DTYPE_NUMBER, DTYPE_BINARY_FLOAT, DTYPE_BINARY_DOUBLE,
	  				DTYPE_LONG, DTYPE_LONG_RAW, DTYPE_RAW, DTYPE_BLOB, DTYPE_CLOB, DTYPE_NCLOB, DTYPE_BFILE, DTYPE_INTERVAL_YEAR,
	  				DTYPE_INTERVAL_DAY
	  	  	});
		
	  	static final List<String> CUSTOM_DATATYPE_START_WORDS = 
	  		Arrays.asList(new String[] {"VARCHAR2", "NVARCHAR2", "NUMBER",
	  	  		"BINARY_FLOAT", "BINARY_DOUBLE", "LONG", "RAW", "BLOB", "CLOB", "NCLOB", "BFILE", "INTERVAL"
	  	  	});
	}
}
