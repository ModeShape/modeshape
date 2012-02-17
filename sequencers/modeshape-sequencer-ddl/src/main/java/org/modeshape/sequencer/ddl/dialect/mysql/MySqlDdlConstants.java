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

import java.util.Arrays;
import java.util.List;

import org.modeshape.sequencer.ddl.DdlConstants;

/**
 * @author blafond
 *
 */
public interface MySqlDdlConstants extends DdlConstants {
	public static final String[] CUSTOM_KEYWORDS = {
		"DATABASE", "DEFINER", "EVENT", "LOGFILE", "TABLESPACE", "TRIGGER",
		"TINYINT", "MEDIUMINT", "BIGINT", "FIXED", "BOOL", "BOOLEAN", "BINARY", "VARBINARY", "TINYBLOB",
  		"TINYTEXT", "MEDIUMBLOB", "MEDIUMTEXT", "LONGBLOB", "LONGTEXT", "TEXT", "ENUM", "SET", "DATETIME", "YEAR", "BLOB", "RENAME",
  		"ALGORITHM", "SECURITY"
	};

	static final String[] STMT_ALTER_ALGORITHM 			= {"ALTER", "ALGORITHM"};
	static final String[] STMT_ALTER_DATABASE 			= {"ALTER", "DATABASE"};
	static final String[] STMT_ALTER_DEFINER 			= {"ALTER", "DEFINER"}; // = ALTER EVENT, ALTER VIEW
	static final String[] STMT_ALTER_EVENT	 			= {"ALTER", "EVENT"};
	static final String[] STMT_ALTER_FUNCTION 			= {"ALTER", "FUNCTION"};
	static final String[] STMT_ALTER_LOGFILE_GROUP		= {"ALTER", "LOGFILE", "GROUP"};
	static final String[] STMT_ALTER_PROCEDURE 			= {"ALTER", "PROCEDURE"};
	static final String[] STMT_ALTER_SCHEMA 			= {"ALTER", "SCHEMA"};
	static final String[] STMT_ALTER_SERVER 			= {"ALTER", "SERVER"};
	static final String[] STMT_ALTER_SQL_SECURITY 		= {"ALTER", "SQL", "SECURITY"};
	static final String[] STMT_ALTER_TABLESPACE			= {"ALTER", "TABLESPACE"};
	
	// ALTER [ONLINE | OFFLINE] [IGNORE] TABLE
	static final String[] STMT_ALTER_IGNORE_TABLE			= {"ALTER", "IGNORE", "TABLE"};
	static final String[] STMT_ALTER_ONLINE_TABLE			= {"ALTER", "ONLINE", "TABLE"};
	static final String[] STMT_ALTER_OFFLINE_TABLE			= {"ALTER", "OFFLINE", "TABLE"};
	static final String[] STMT_ALTER_ONLINE_IGNORE_TABLE	= {"ALTER", "ONLINE", "IGNORE", "TABLE"};
	static final String[] STMT_ALTER_OFFLINE_IGNORE_TABLE	= {"ALTER", "OFFLINE", "IGNORE", "TABLE"};
	
	interface MySqlStatementStartPhrases {
	    static final String[][] ALTER_PHRASES = {
	    	STMT_ALTER_DATABASE,
	    	STMT_ALTER_DEFINER,
	    	STMT_ALTER_EVENT,
	    	STMT_ALTER_FUNCTION,
	    	STMT_ALTER_LOGFILE_GROUP,
	    	STMT_ALTER_PROCEDURE,
	    	STMT_ALTER_SCHEMA,
	    	STMT_ALTER_SERVER,
	    	STMT_ALTER_TABLESPACE,
	    	STMT_ALTER_IGNORE_TABLE, 
	    	STMT_ALTER_ONLINE_TABLE,
	    	STMT_ALTER_OFFLINE_TABLE,
	    	STMT_ALTER_ONLINE_IGNORE_TABLE,
	    	STMT_ALTER_OFFLINE_IGNORE_TABLE
		};
	    
		static final String[] STMT_CREATE_DATABASE 			= {"CREATE", "DATABASE"};
		static final String[] STMT_CREATE_DEFINER 			= {"CREATE", "DEFINER"}; // TRIGGER, FUNCTION, PROCEDURE, EVENT
		static final String[] STMT_CREATE_EVENT 			= {"CREATE", "EVENT"};
		static final String[] STMT_CREATE_FUNCTION 			= {"CREATE", "FUNCTION"};
		static final String[] STMT_CREATE_INDEX 			= {"CREATE", "INDEX"};
		static final String[] STMT_CREATE_LOGFILE_GROUP 	= {"CREATE", "LOGFILE", "GROUP"};
		static final String[] STMT_CREATE_UNIQUE_INDEX 		= {"CREATE", "UNIQUE", "INDEX"};
		static final String[] STMT_CREATE_PROCEDURE 		= {"CREATE", "PROCEDURE"};
		static final String[] STMT_CREATE_SERVER 			= {"CREATE", "SERVER"};
		static final String[] STMT_CREATE_TABLESPACE 		= {"CREATE", "TABLESPACE"};
		static final String[] STMT_CREATE_TRIGGER 			= {"CREATE", "TRIGGER"};

	    static final String[][] CREATE_PHRASES = { 
	    	STMT_CREATE_DATABASE,
	    	STMT_CREATE_DEFINER,
	    	STMT_CREATE_EVENT,
	    	STMT_CREATE_FUNCTION,
	    	STMT_CREATE_INDEX,
	    	STMT_CREATE_LOGFILE_GROUP,
	    	STMT_CREATE_UNIQUE_INDEX,
	    	STMT_CREATE_PROCEDURE,
	    	STMT_CREATE_SERVER,
	    	STMT_CREATE_TABLESPACE,
	    	STMT_CREATE_TRIGGER
		};
	    
	    /*
	     * TODO:  WORK REQUIRED HERE
	     * 
	     * NOTE:  The CREATE VIEW syntax for MySQL is convoluted as shown below. Basically you have to check for multiple properties
	     * BEFORE the actual "VIEW view_name (...) AS ...." is defined.
	     * Will make it difficult to parse cleanly.
	     * 
	     * THIS ALSO affects ALTER VIEW
	     * 
				CREATE
				    [OR REPLACE]
				    [ALGORITHM = {UNDEFINED | MERGE | TEMPTABLE}]
				    [DEFINER = { user | CURRENT_USER }]
				    [SQL SECURITY { DEFINER | INVOKER }]
				    VIEW view_name [(column_list)]
				    AS select_statement
				    [WITH [CASCADED | LOCAL] CHECK OPTION]
	     * 
	     * 
	     */
	    
		static final String[] STMT_DROP_DATABASE 		= {"DROP", "DATABASE"};
		static final String[] STMT_DROP_EVENT 			= {"DROP", "EVENT"};
		static final String[] STMT_DROP_FUNCTION 		= {"DROP", "FUNCTION"};
		static final String[] STMT_DROP_INDEX 			= {"DROP", "INDEX"};
		static final String[] STMT_DROP_ONLINE_INDEX 	= {"DROP", "ONLINE", "INDEX"};
		static final String[] STMT_DROP_OFFLINE_INDEX 	= {"DROP", "OFFLINE", "INDEX"};
		static final String[] STMT_DROP_LOGFILE_GROUP 	= {"DROP", "LOGFILE", "GROUP"};
		static final String[] STMT_DROP_PROCEDURE 		= {"DROP", "PROCEDURE"};
		static final String[] STMT_DROP_SERVER 			= {"DROP", "SERVER"};
		static final String[] STMT_DROP_TABLESPACE 		= {"DROP", "TABLESPACE"};
		static final String[] STMT_DROP_TRIGGER 		= {"DROP", "TRIGGER"};
	    
	    static final String[][] DROP_PHRASES = {
	    	STMT_DROP_DATABASE,
	    	STMT_DROP_EVENT,
	    	STMT_DROP_FUNCTION,
	    	STMT_DROP_INDEX,
	    	STMT_DROP_ONLINE_INDEX,
	    	STMT_DROP_OFFLINE_INDEX,
	    	STMT_DROP_LOGFILE_GROUP,
	    	STMT_DROP_PROCEDURE,
	    	STMT_DROP_SERVER,
	    	STMT_DROP_TABLESPACE,
	    	STMT_DROP_TRIGGER
		};
	    static final String[][] SET_PHRASES = { 
	
		};

	    // RENAME {DATABASE | SCHEMA} db_name TO new_db_name;
	    static final String[] STMT_RENAME_DATABASE 		= {"RENAME", "DATABASE"};
	    static final String[] STMT_RENAME_SCHEMA 		= {"RENAME", "SCHEMA"};
	    // RENAME TABLE tbl_name TO new_tbl_name [, tbl_name2 TO new_tbl_name2] ...
	    static final String[] STMT_RENAME_TABLE 		= {"RENAME", "TABLE"};
	    
	    static final String[][] MISC_PHRASES = {
	    	STMT_RENAME_DATABASE, STMT_RENAME_SCHEMA, STMT_RENAME_TABLE
		};
	}
	
	static final String[] DTYPE_NATIONAL_VARCHAR 			= {"NATIONAL", "VARCHAR"};// BOOLEAN
	
	static final String[] DTYPE_TINYINT 					= {"TINYINT"}; // TINYINT[(M)] [UNSIGNED] [ZEROFILL]
	static final String[] DTYPE_MEDIUMINT 					= {"MEDIUMINT"}; // MEDIUMINT[(M)] [UNSIGNED] [ZEROFILL]
	static final String[] DTYPE_BIGINT 						= {"BIGINT"}; // BIGINT[(M)] [UNSIGNED] [ZEROFILL]
	static final String[] DTYPE_FIXED 						= {"FIXED"};// FIXED[(M[,D])] [UNSIGNED] [ZEROFILL]
	static final String[] DTYPE_BOOL 						= {"BOOL"};// BOOL
	static final String[] DTYPE_BOOLEAN 					= {"BOOLEAN"};// BOOLEAN
	
	static final String[] DTYPE_DOUBLE 						= {"DOUBLE"}; // DOUBLE[(M,D)] [UNSIGNED] [ZEROFILL]
	
	static final String[] DTYPE_BINARY 						= {"BINARY"};
	static final String[] DTYPE_VARBINARY 					= {"VARBINARY"};
	static final String[] DTYPE_BLOB 						= {"BLOB"};
	static final String[] DTYPE_TINYBLOB 					= {"TINYBLOB"};
	static final String[] DTYPE_TINYTEXT 					= {"TINYTEXT"};
	static final String[] DTYPE_TEXT 						= {"TEXT"};
	static final String[] DTYPE_MEDIUMBLOB 					= {"MEDIUMBLOB"};
	static final String[] DTYPE_MEDIUMTEXT 					= {"MEDIUMTEXT"};
	static final String[] DTYPE_LONGBLOB 					= {"LONGBLOB"};
	static final String[] DTYPE_LONGTEXT 					= {"LONGTEXT"};
	static final String[] DTYPE_ENUM 						= {"ENUM"};
	static final String[] DTYPE_SET 						= {"SET"};
	
	static final String[] DTYPE_DATETIME 					= {"DATETIME"};
	static final String[] DTYPE_YEAR 						= {"YEAR"};
	
	interface MySqlDataTypes {
		static final List<String[]> CUSTOM_DATATYPE_START_PHRASES = 
	  		Arrays.asList(new String[][] {
	  				DTYPE_BIGINT, DTYPE_BOOL, DTYPE_BOOLEAN, DTYPE_FIXED, DTYPE_DOUBLE, DTYPE_MEDIUMINT, DTYPE_TINYINT,
	  				DTYPE_NATIONAL_VARCHAR, DTYPE_BINARY, DTYPE_VARBINARY, DTYPE_TINYBLOB, DTYPE_TINYTEXT, DTYPE_TEXT,
	  				DTYPE_MEDIUMBLOB, DTYPE_MEDIUMTEXT, DTYPE_LONGBLOB, DTYPE_LONGTEXT, DTYPE_ENUM, DTYPE_SET,
	  				DTYPE_DATETIME, DTYPE_YEAR, DTYPE_BLOB
	  	  	});
		
	  	static final List<String> CUSTOM_DATATYPE_START_WORDS = 
	  		Arrays.asList(new String[] {
	  			"TINYINT", "MEDIUMINT", "BIGINT", "FIXED", "BOOL", "BOOLEAN", "BINARY", "VARBINARY", "TINYBLOB",
	  			"TINYTEXT", "MEDIUMBLOB", "MEDIUMTEXT", "LONGBLOB", "LONGTEXT", "TEXT", "ENUM", "SET", "DATETIME", "YEAR", "BLOB"
	  	  	});
		/* 
		 * ===========================================================================================================================
		 * DATATYPES

				data_type:
				    BIT[(length)]
				  | TINYINT[(length)] [UNSIGNED] [ZEROFILL]
				  | SMALLINT[(length)] [UNSIGNED] [ZEROFILL]
				  | MEDIUMINT[(length)] [UNSIGNED] [ZEROFILL]
				  | INT[(length)] [UNSIGNED] [ZEROFILL]
				  | INTEGER[(length)] [UNSIGNED] [ZEROFILL]
				  | BIGINT[(length)] [UNSIGNED] [ZEROFILL]
				  | REAL[(length,decimals)] [UNSIGNED] [ZEROFILL]
				  | DOUBLE[(length,decimals)] [UNSIGNED] [ZEROFILL]
				  | FLOAT[(length,decimals)] [UNSIGNED] [ZEROFILL]
				  | DECIMAL[(length[,decimals])] [UNSIGNED] [ZEROFILL]
				  | NUMERIC[(length[,decimals])] [UNSIGNED] [ZEROFILL]
				  | DATE
				  | TIME
				  | TIMESTAMP
				  | DATETIME
				  | YEAR
				  | CHAR[(length)]
				      [CHARACTER SET charset_name] [COLLATE collation_name]
				  | VARCHAR(length)
				      [CHARACTER SET charset_name] [COLLATE collation_name]
				  | BINARY[(length)]
				  | VARBINARY(length)
				  | TINYBLOB
				  | BLOB
				  | MEDIUMBLOB
				  | LONGBLOB
				  | TINYTEXT [BINARY]
				      [CHARACTER SET charset_name] [COLLATE collation_name]
				  | TEXT [BINARY]
				      [CHARACTER SET charset_name] [COLLATE collation_name]
				  | MEDIUMTEXT [BINARY]
				      [CHARACTER SET charset_name] [COLLATE collation_name]
				  | LONGTEXT [BINARY]
				      [CHARACTER SET charset_name] [COLLATE collation_name]
				  | ENUM(value1,value2,value3,...)
				      [CHARACTER SET charset_name] [COLLATE collation_name]
				  | SET(value1,value2,value3,...)
				      [CHARACTER SET charset_name] [COLLATE collation_name]
				  | spatial_type


			 * 
			 */
	}
}
