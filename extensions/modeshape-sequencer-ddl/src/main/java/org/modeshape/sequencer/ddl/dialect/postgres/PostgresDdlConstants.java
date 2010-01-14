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

import java.util.Arrays;
import java.util.List;

import org.modeshape.graph.property.Name;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 *
 */
public interface PostgresDdlConstants extends DdlConstants {
	public static final String[] CUSTOM_KEYWORDS = {
		"SHOW", "LISTEN", "UNLISTEN", "REINDEX", "MOVE", "ABORT", "ANALYZE", "TRUNCATE", "REASSIGN", "RELEASE",
		"RESET", "REVOKE", "ROLLBACK", "FETCH", "EXPLAIN", "DISCARD", "COPY", "CLUSTER", "NOTIFY", "LOAD", "COMMENT", "LOCK",
		"SERVER", "SEARCH", "PARSER", "DICTIONARY", "WRAPPER", "PROCEDURAL", "CONVERSION", "AGGREGATE", "TEMPLATE", 
		"MAPPING", "TRUSTED", "TRIGGER", "VACUUM", "FAMILTY",
		"BIGSERIAL", "BOX", "BOOLEAN", "BOX", "BYTEA", "CIDR", "CIRCLE", "FLOAT4", "FLOAT8", "INET", "INT2", "INT4", "INT8",
        "LINE", "LSEG", "MACADDR", "MONEY", "PATH", "POINT", "POLYGON",
        "SERIAL", "SERIAL4", "SERIAL8", "TEXT", "TIMESTAMPZ", "TSQUERY",
        "TSVECTOR", "TXID_SNAPSHOT", "UUID", "XML"
	};

	interface PostgresStatementStartPhrases {
		static final String[] STMT_ALTER_AGGREGATE = {ALTER, "AGGREGATE"};
		static final String[] STMT_ALTER_CONVERSION = {ALTER, "CONVERSION"};
		static final String[] STMT_ALTER_DATABASE = {ALTER, "DATABASE"};
		static final String[] STMT_ALTER_FOREIGN_DATA_WRAPPER = {ALTER, "FOREIGN", "DATA", "WRAPPER"};
		static final String[] STMT_ALTER_FUNCTION = {ALTER, "FUNCTION"};
		static final String[] STMT_ALTER_GROUP = {ALTER, "GROUP", DdlTokenStream.ANY_VALUE, DdlTokenStream.ANY_VALUE, DdlTokenStream.ANY_VALUE };
		static final String[] STMT_ALTER_INDEX = {ALTER, "INDEX"};
		static final String[] STMT_ALTER_LANGUAGE = {ALTER, "LANGUAGE"};
		static final String[] STMT_ALTER_PROCEDURAL_LANGUAGE = {ALTER, "PROCEDURAL", "LANGUAGE"};
		static final String[] STMT_ALTER_OPERATOR = {ALTER, "OPERATOR"};
		static final String[] STMT_ALTER_OPERATOR_CLASS = {
			ALTER, "OPERATOR", "CLASS", DdlTokenStream.ANY_VALUE, 
			DdlTokenStream.ANY_VALUE, DdlTokenStream.ANY_VALUE, DdlTokenStream.ANY_VALUE
		};
		static final String[] STMT_ALTER_OPERATOR_FAMILY = {
			ALTER, "OPERATOR", "FAMILY", DdlTokenStream.ANY_VALUE, 
			DdlTokenStream.ANY_VALUE, DdlTokenStream.ANY_VALUE, DdlTokenStream.ANY_VALUE
		};
		static final String[] STMT_ALTER_ROLE = {ALTER, "ROLE"};
		static final String[] STMT_ALTER_SCHEMA = {ALTER, "SCHEMA"};
		static final String[] STMT_ALTER_SEQUENCE = {ALTER, "SEQUENCE"};
		static final String[] STMT_ALTER_SERVER = {ALTER, "SERVER"};
		static final String[] STMT_ALTER_TABLESPACE = {ALTER, "TABLESPACE"};
		static final String[] STMT_ALTER_TEXT_SEARCH_CONFIGURATION = {ALTER, "TEXT", "SEARCH", "CONFIGURATION"};
		static final String[] STMT_ALTER_TEXT_SEARCH_DICTIONARY = {ALTER, "TEXT", "SEARCH", "DICTIONARY"};
		static final String[] STMT_ALTER_TEXT_SEARCH_PARSER = {ALTER, "TEXT", "SEARCH", "PARSER"};
		static final String[] STMT_ALTER_TEXT_SEARCH_TEMPLATE = {ALTER, "TEXT", "SEARCH", "TEMPLATE"};
		static final String[] STMT_ALTER_TEXT_SEARCH = {ALTER, "TEXT", "SEARCH"};
		static final String[] STMT_ALTER_TRIGGER = {ALTER, "TRIGGER"};
		static final String[] STMT_ALTER_TYPE = {ALTER, "TYPE"};
		static final String[] STMT_ALTER_USER = {ALTER, "USER"};
		static final String[] STMT_ALTER_USER_MAPPING = {ALTER, "USER", "MAPPING"};
		static final String[] STMT_ALTER_VIEW = {ALTER, "VIEW"};
		
	    static final String[][] ALTER_PHRASES = { 
	    	STMT_ALTER_AGGREGATE, STMT_ALTER_CONVERSION, STMT_ALTER_DATABASE, STMT_ALTER_FOREIGN_DATA_WRAPPER, STMT_ALTER_FUNCTION,
	    	STMT_ALTER_GROUP, STMT_ALTER_INDEX, STMT_ALTER_PROCEDURAL_LANGUAGE, STMT_ALTER_LANGUAGE, STMT_ALTER_OPERATOR_CLASS, 
	    	STMT_ALTER_OPERATOR_FAMILY, STMT_ALTER_OPERATOR,
	    	STMT_ALTER_ROLE, STMT_ALTER_SCHEMA, STMT_ALTER_SEQUENCE, STMT_ALTER_SERVER, STMT_ALTER_TABLESPACE,
	    	STMT_ALTER_TEXT_SEARCH_CONFIGURATION, STMT_ALTER_TEXT_SEARCH_DICTIONARY, STMT_ALTER_TEXT_SEARCH_PARSER,
	    	STMT_ALTER_TEXT_SEARCH_TEMPLATE, STMT_ALTER_TEXT_SEARCH, STMT_ALTER_TRIGGER, STMT_ALTER_TYPE, STMT_ALTER_USER_MAPPING, STMT_ALTER_USER,
	    	STMT_ALTER_VIEW
		};
	    
	    static final String[] STMT_CREATE_AGGREGATE = {CREATE, "AGGREGATE"};
	    static final String[] STMT_CREATE_CAST = {CREATE, "CAST"};
	    static final String[] STMT_CREATE_CONSTRAINT_TRIGGER = {CREATE, "CONSTRAINT", "TRIGGER"};
	    static final String[] STMT_CREATE_CONVERSION = {CREATE, "CONVERSION"};
	    static final String[] STMT_CREATE_DATABASE = {CREATE, "DATABASE"};
	    static final String[] STMT_CREATE_FOREIGN_DATA_WRAPPER = {CREATE, "FOREIGN", "DATA", "WRAPPER"};
	    static final String[] STMT_CREATE_FUNCTION = {CREATE, "FUNCTION"};
	    static final String[] STMT_CREATE_OR_REPLACE_FUNCTION = {CREATE, "OR", "REPLACE", "FUNCTION"};
	    static final String[] STMT_CREATE_GROUP = {CREATE, "GROUP"};
	    static final String[] STMT_CREATE_INDEX = {CREATE, "INDEX"};
	    static final String[] STMT_CREATE_UNIQUE_INDEX = {CREATE, "UNIQUE", "INDEX"};
	    static final String[] STMT_CREATE_LANGUAGE = {CREATE, "LANGUAGE"};
	    static final String[] STMT_CREATE_PROCEDURAL_LANGUAGE = {CREATE, "PROCEDURAL", "LANGUAGE"};
	    static final String[] STMT_CREATE_TRUSTED_PROCEDURAL_LANGUAGE = {CREATE, "TRUSTED", "PROCEDURAL", "LANGUAGE"};
	    static final String[] STMT_CREATE_OPERATOR = {CREATE, "OPERATOR"};
	    static final String[] STMT_CREATE_OPERATOR_CLASS = {CREATE, "OPERATOR", "CLASS"};
	    static final String[] STMT_CREATE_OPERATOR_FAMILY = {CREATE, "OPERATOR", "FAMILY"};
	    static final String[] STMT_CREATE_ROLE = {CREATE, "ROLE"};
	    static final String[] STMT_CREATE_RULE = {CREATE, "RULE"};
	    static final String[] STMT_CREATE_OR_REPLACE_RULE = {CREATE, "OR", "REPLACE", "RULE"};
	    static final String[] STMT_CREATE_TEMP_TABLE = {CREATE, "TEMP", TABLE};
	  	static final String[] STMT_CREATE_GLOBAL_TEMP_TABLE = {CREATE, "GLOBAL", "TEMP", TABLE};
	  	static final String[] STMT_CREATE_LOCAL_TEMP_TABLE = {CREATE, "LOCAL", "TEMP", TABLE};
	    static final String[] STMT_CREATE_SEQUENCE = {CREATE, "SEQUENCE"};
	    static final String[] STMT_CREATE_TEMP_SEQUENCE = {CREATE, "TEMP", "SEQUENCE"};
	    static final String[] STMT_CREATE_TEMPORARY_SEQUENCE = {CREATE, "TEMPORARY", "SEQUENCE"};
	    static final String[] STMT_CREATE_SERVER = {CREATE, "SERVER"};
	    static final String[] STMT_CREATE_TABLESPACE = {CREATE, "TABLESPACE"};
		static final String[] STMT_CREATE_TEXT_SEARCH_CONFIGURATION = {CREATE, "TEXT", "SEARCH", "CONFIGURATION"};
		static final String[] STMT_CREATE_TEXT_SEARCH_DICTIONARY = {CREATE, "TEXT", "SEARCH", "DICTIONARY"};
		static final String[] STMT_CREATE_TEXT_SEARCH_PARSER = {CREATE, "TEXT", "SEARCH", "PARSER"};
		static final String[] STMT_CREATE_TEXT_SEARCH_TEMPLATE = {CREATE, "TEXT", "SEARCH", "TEMPLATE"};
		static final String[] STMT_CREATE_TEXT_SEARCH = {CREATE, "TEXT", "SEARCH"};
		static final String[] STMT_CREATE_TRIGGER = {CREATE, "TRIGGER"};
	    static final String[] STMT_CREATE_TYPE = {CREATE, "TYPE"};
	    static final String[] STMT_CREATE_USER = {CREATE, "USER"};
	    static final String[] STMT_CREATE_USER_MAPPING = {CREATE, "USER", "MAPPING"};

	    
	    static final String[][] CREATE_PHRASES = { 
	    	STMT_CREATE_AGGREGATE, STMT_CREATE_CAST, STMT_CREATE_CONSTRAINT_TRIGGER, STMT_CREATE_CONVERSION,
	    	STMT_CREATE_DATABASE, STMT_CREATE_FOREIGN_DATA_WRAPPER, STMT_CREATE_FUNCTION, STMT_CREATE_OR_REPLACE_FUNCTION,
	    	STMT_CREATE_GROUP, STMT_CREATE_INDEX, STMT_CREATE_UNIQUE_INDEX, STMT_CREATE_PROCEDURAL_LANGUAGE, 
	    	STMT_CREATE_TRUSTED_PROCEDURAL_LANGUAGE,
	    	STMT_CREATE_LANGUAGE, STMT_CREATE_OPERATOR_CLASS, STMT_CREATE_OPERATOR_FAMILY,
	    	STMT_CREATE_OPERATOR, STMT_CREATE_ROLE, STMT_CREATE_RULE, STMT_CREATE_OR_REPLACE_RULE,  
	    	STMT_CREATE_TEMP_TABLE, STMT_CREATE_GLOBAL_TEMP_TABLE, STMT_CREATE_LOCAL_TEMP_TABLE,
	    	STMT_CREATE_SEQUENCE, STMT_CREATE_TEMP_SEQUENCE, STMT_CREATE_TEMPORARY_SEQUENCE,
	    	STMT_CREATE_SERVER, STMT_CREATE_TABLESPACE, STMT_CREATE_TEXT_SEARCH_CONFIGURATION, STMT_CREATE_TEXT_SEARCH_DICTIONARY,
	    	STMT_CREATE_TEXT_SEARCH_PARSER,STMT_CREATE_TEXT_SEARCH_TEMPLATE, STMT_CREATE_TEXT_SEARCH, 
	    	STMT_CREATE_TRIGGER, STMT_CREATE_TYPE,
	    	STMT_CREATE_USER_MAPPING, STMT_CREATE_USER
		};
	    
	    static final String[] STMT_DROP_AGGREGATE = {DROP, "AGGREGATE"};
	    static final String[] STMT_DROP_CAST = {DROP, "CAST"};
	    static final String[] STMT_DROP_CONSTRAINT_TRIGGER = {DROP, "CONSTRAINT", "TRIGGER"};
	    static final String[] STMT_DROP_CONVERSION = {DROP, "CONVERSION"};
	    static final String[] STMT_DROP_DATABASE = {DROP, "DATABASE"};
	    static final String[] STMT_DROP_FOREIGN_DATA_WRAPPER = {DROP, "FOREIGN", "DATA", "WRAPPER"};
	    static final String[] STMT_DROP_FUNCTION = {DROP, "FUNCTION"};
	    static final String[] STMT_DROP_GROUP = {DROP, "GROUP"};
	    static final String[] STMT_DROP_INDEX = {DROP, "INDEX"};
	    static final String[] STMT_DROP_LANGUAGE = {DROP, "LANGUAGE"};
	    static final String[] STMT_DROP_PROCEDURAL_LANGUAGE = {DROP, "PROCEDURAL", "LANGUAGE"};
	    static final String[] STMT_DROP_OPERATOR = {DROP, "OPERATOR"};
	    static final String[] STMT_DROP_OPERATOR_CLASS = {DROP, "OPERATOR", "CLASS"};
	    static final String[] STMT_DROP_OPERATOR_FAMILY = {DROP, "OPERATOR", "FAMILY"};
	    static final String[] STMT_DROP_OWNED_BY = {DROP, "OWNED", "BY"};
	    static final String[] STMT_DROP_ROLE = {DROP, "ROLE"};
	    static final String[] STMT_DROP_RULE = {DROP, "RULE"};
	    static final String[] STMT_DROP_SEQUENCE = {DROP, "SEQUENCE"};
	    static final String[] STMT_DROP_SERVER = {DROP, "SERVER"};
	    static final String[] STMT_DROP_TABLESPACE = {DROP, "TABLESPACE"};
		static final String[] STMT_DROP_TEXT_SEARCH_CONFIGURATION = {DROP, "TEXT", "SEARCH", "CONFIGURATION"};
		static final String[] STMT_DROP_TEXT_SEARCH_DICTIONARY = {DROP, "TEXT", "SEARCH", "DICTIONARY"};
		static final String[] STMT_DROP_TEXT_SEARCH_PARSER = {DROP, "TEXT", "SEARCH", "PARSER"};
		static final String[] STMT_DROP_TEXT_SEARCH_TEMPLATE = {DROP, "TEXT", "SEARCH", "TEMPLATE"};
		static final String[] STMT_DROP_TEXT_SEARCH = {DROP, "TEXT", "SEARCH"};
		static final String[] STMT_DROP_TRIGGER = {DROP, "TRIGGER"};
	    static final String[] STMT_DROP_TYPE = {DROP, "TYPE"};
	    static final String[] STMT_DROP_USER = {DROP, "USER"};
	    static final String[] STMT_DROP_USER_MAPPING = {DROP, "USER", "MAPPING"};

	    
	    static final String[][] DROP_PHRASES = { 
	    	STMT_DROP_AGGREGATE, STMT_DROP_CAST, STMT_DROP_CONSTRAINT_TRIGGER, STMT_DROP_CONVERSION,
	    	STMT_DROP_DATABASE, STMT_DROP_FOREIGN_DATA_WRAPPER, STMT_DROP_FUNCTION,
	    	STMT_DROP_GROUP, STMT_DROP_INDEX, STMT_DROP_PROCEDURAL_LANGUAGE,
	    	STMT_DROP_LANGUAGE, STMT_DROP_OPERATOR_CLASS, STMT_DROP_OPERATOR_FAMILY, STMT_DROP_OWNED_BY,
	    	STMT_DROP_OPERATOR, STMT_DROP_ROLE, STMT_DROP_RULE, STMT_DROP_SEQUENCE,
	    	STMT_DROP_SERVER, STMT_DROP_TABLESPACE, STMT_DROP_TEXT_SEARCH_CONFIGURATION, STMT_DROP_TEXT_SEARCH_DICTIONARY,
	    	STMT_DROP_TEXT_SEARCH_PARSER,STMT_DROP_TEXT_SEARCH_TEMPLATE, STMT_DROP_TEXT_SEARCH, STMT_DROP_TRIGGER, STMT_DROP_TYPE,
	    	STMT_DROP_USER_MAPPING, STMT_DROP_USER
		};
	    
	    static final String[] STMT_SET_CONSTRAINTS = {"SET", "CONSTRAINTS"};
	    static final String[] STMT_SET_ROLE = {"SET", "ROLE"};
	    static final String[] STMT_SET_SESSION_AUTHORIZATION= {"SET", "SESSION", "AUTHORIZATION"};
	    static final String[] STMT_SET_TRANSACTION = {"SET", "TRANSACTION"};
	    
	    static final String[][] SET_PHRASES = { 
	    	STMT_SET_CONSTRAINTS, STMT_SET_ROLE, STMT_SET_SESSION_AUTHORIZATION, STMT_SET_TRANSACTION
		};
	    
	    static final String[] STMT_ABORT = {"ABORT"};
	    static final String[] STMT_ANALYZE = {"ANALYZE"};
	    static final String[] STMT_CLUSTER = {"CLUSTER"};
	    static final String[] STMT_COMMENT_ON = {"COMMENT", "ON"};
	    static final String[] STMT_COMMIT = {"COMMIT"};
	    static final String[] STMT_COPY = {"COPY"};
	    static final String[] STMT_DEALLOCATE_PREPARE = {"DEALLOCATE", "PREPARE"};
	    static final String[] STMT_DEALLOCATE = {"DEALLOCATE"};
	    static final String[] STMT_DECLARE = {"DECLARE"};
	    static final String[] STMT_DISCARD = {"DISCARD"};
	    static final String[] STMT_EXPLAIN_ANALYZE = {"EXPLAIN", "ANALYZE"};
	    static final String[] STMT_EXPLAIN = {"EXPLAIN"};
	    static final String[] STMT_FETCH = {"FETCH"};
	    static final String[] STMT_LISTEN = {"LISTEN"};
	    static final String[] STMT_LOAD = {"LOAD"};
		static final String[] STMT_LOCK_TABLE = {"LOCK", "TABLE"};
	    static final String[] STMT_MOVE = {"MOVE"};
	    static final String[] STMT_NOTIFY = {"NOTIFY"};
	    static final String[] STMT_PREPARE = {"PREPARE"};
	    static final String[] STMT_PREPARE_TRANSATION = {"PREPARE", "TRANSATION"};
	    static final String[] STMT_REASSIGN_OWNED = {"REASSIGN", "OWNED"};
	    static final String[] STMT_REINDEX = {"REINDEX"};
	    static final String[] STMT_RELEASE_SAVEPOINT = {"RELEASE", "SAVEPOINT"};
	    static final String[] STMT_REVOKE = {"REVOKE"};
	    static final String[] STMT_ROLLBACK = {"ROLLBACK"};
	    static final String[] STMT_ROLLBACK_PREPARED = {"ROLLBACK", "PREPARED"};
	    static final String[] STMT_ROLLBACK_TO_SAVEPOINT = {"ROLLBACK", "TO", "SAVEPOINT"};
	    static final String[] STMT_SELECT_INTO = {"SELECT", "INTO"};

	    static final String[] STMT_SHOW = {"SHOW"};
	    static final String[] STMT_TRUNCATE = {"TRUNCATE"};
	    static final String[] STMT_UNLISTEN = {"UNLISTEN"};
	    static final String[] STMT_VACUUM = {"VACUUM"};
	    //static final String[] STMT_VALUES = {"VALUES"};
	    
	    static final String[][] MISC_PHRASES = {
	    	STMT_ABORT, STMT_ANALYZE, STMT_CLUSTER, STMT_COMMENT_ON, STMT_COMMIT, STMT_COPY, STMT_DEALLOCATE_PREPARE, STMT_DEALLOCATE,
	    	STMT_DECLARE, STMT_DISCARD, STMT_EXPLAIN_ANALYZE, STMT_EXPLAIN, STMT_FETCH, STMT_LISTEN, STMT_LOAD, STMT_LOCK_TABLE,
	    	STMT_MOVE, STMT_NOTIFY, STMT_PREPARE, STMT_PREPARE_TRANSATION, STMT_REASSIGN_OWNED, STMT_REINDEX, 
	    	STMT_RELEASE_SAVEPOINT, STMT_REVOKE, STMT_ROLLBACK_TO_SAVEPOINT, STMT_ROLLBACK_PREPARED, 
	    	STMT_ROLLBACK, STMT_SELECT_INTO, STMT_SHOW, STMT_TRUNCATE, STMT_UNLISTEN, STMT_VACUUM   //, STMT_VALUES
		};
	    
	    // CREATE TABLE, CREATE VIEW, CREATE INDEX, CREATE SEQUENCE, CREATE TRIGGER and GRANT
	    public final static Name[] VALID_SCHEMA_CHILD_STMTS = {
	    	StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT, 
	    	StandardDdlLexicon.TYPE_CREATE_VIEW_STATEMENT,
	    	StandardDdlLexicon.TYPE_GRANT_ON_TABLE_STATEMENT,
	    	PostgresDdlLexicon.TYPE_CREATE_INDEX_STATEMENT,
	    	PostgresDdlLexicon.TYPE_CREATE_SEQUENCE_STATEMENT,
	    	PostgresDdlLexicon.TYPE_CREATE_TRIGGER_STATEMENT,
	    	PostgresDdlLexicon.TYPE_GRANT_ON_SEQUENCE_STATEMENT,
	    	PostgresDdlLexicon.TYPE_GRANT_ON_SCHEMA_STATEMENT
	  	};
	    
	    public final static Name[] COMPLEX_STMT_TYPES = {
	    	PostgresDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT
	    };
	}
	
		//SPEC  Name				Aliases		Description
		//
		//	X	bigint					int8		signed eight-byte integer
		//		bigserial				serial8		autoincrementing eight-byte integer
		//	X	bit [ (n) ]	 						fixed-length bit string
		//	X	bit varying [ (n) ]		varbit		variable-length bit string
		//		boolean					bool		logical Boolean (true/false)
		//		box	 								rectangular box on a plane
		//		bytea	 							binary data ("byte array")
		//	X	character varying [ (n) ]	varchar [ (n) ]	variable-length character string
		//	X	character [ (n) ]		char [ (n) ]	fixed-length character string
		//		cidr	 							IPv4 or IPv6 network address
		//		circle	 							circle on a plane
		//	X	date	 							calendar date (year, month, day)
		//	X	double precision		float8		double precision floating-point number (8 bytes)
		//		inet	 							IPv4 or IPv6 host address
		//	X	integer					int, int4	signed four-byte integer
		//	X	interval [ fields ] [ (p) ]	 		time span
		//		line	 							infinite line on a plane
		//		lseg	 							line segment on a plane
		//		macaddr	 							MAC (Media Access Control) address
		//		money	 							currency amount
		//	X	numeric [ (p, s) ]		decimal [ (p, s) ]	exact numeric of selectable precision
		//		path	 							geometric path on a plane
		//		point	 							geometric point on a plane
		//		polygon	 							closed geometric path on a plane
		//	X	real					float4		single precision floating-point number (4 bytes)
		//	X	smallint				int2		signed two-byte integer
		//		serial					serial4		auto incrementing four-byte integer
		//		text	 							variable-length character string
		//		time [ (p) ] [ without time zone ]	 	time of day (no time zone)
		//	X	time [ (p) ] with time zone	timetz		time of day, including time zone
		//		timestamp [ (p) ] [ without time zone ]	 	date and time (no time zone)
		//	X	timestamp [ (p) ] with time zone	timestamptz	date and time, including time zone
		//		tsquery	 							text search query
		//		tsvector	 						text search document
		//		txid_snapshot	 					user-level transaction ID snapshot
		//		uuid	 							universally unique identifier
		//	X	xml	 								XML data
		//      interval hour to minute
	
	interface PostgresDataTypes {
		static final String[] DTYPE_BIGSERIAL = {"BIGSERIAL"};
		static final String[] DTYPE_BOX = {"BOX"};
		static final String[] DTYPE_BYTEA = {"BYTEA"};
		static final String[] DTYPE_CIDR = {"CIDR"}; 
		static final String[] DTYPE_CIRCLE = {"CIRCLE"}; 
		static final String[] DTYPE_INET = {"INET"}; 
		static final String[] DTYPE_LINE = {"LINE"}; 
		static final String[] DTYPE_LSEG = {"LSEG"}; 
		static final String[] DTYPE_MACADDR = {"MACADDR"}; 
		static final String[] DTYPE_MONEY = {"MONEY"}; 
		static final String[] DTYPE_PATH = {"PATH"}; 
		static final String[] DTYPE_POINT = {"POINT"}; 
		static final String[] DTYPE_POLYGON = {"POLYGON"}; 
		static final String[] DTYPE_SERIAL = {"SERIAL"}; 
		static final String[] DTYPE_TEXT = {"TEXT"}; 
		static final String[] DTYPE_TSQUERY = {"TSQUERY"}; 
		static final String[] DTYPE_TSVECTOR = {"TSVECTOR"}; 
		static final String[] DTYPE_TXID_SNAPSHOT = {"TXID_SNAPSHOT"}; 
		static final String[] DTYPE_UUID = {"UUID"}; 
		static final String[] DTYPE_XML = {"XML"}; 
		static final String[] DTYPE_BOOLEAN = {"BOOLEAN"}; 
		static final String[] DTYPE_BOOL = {"BOOL"}; 
		static final String[] DTYPE_FLOAT4 = {"FLOAT4"}; 
		static final String[] DTYPE_FLOAT8 = {"FLOAT8"}; 
		static final String[] DTYPE_INT2 = {"INT2"}; 
		static final String[] DTYPE_INT4 = {"INT4"}; 
		static final String[] DTYPE_INT8 = {"INT8"}; 
		static final String[] DTYPE_SERIAL4 = {"SERIAL4"};
		static final String[] DTYPE_SERIAL8 = {"SERIAL8"}; 
		static final String[] DTYPE_TIMESTAMPZ = {"TIMESTAMPZ"}; 
		static final String[] DTYPE_VARBIT = {"VARBIT"}; 

		
		static final List<String[]> CUSTOM_DATATYPE_START_PHRASES = 
	  		Arrays.asList(new String[][] {
	  				DTYPE_BIGSERIAL, DTYPE_BOOL, DTYPE_BOOLEAN, DTYPE_BOX, DTYPE_BYTEA,
	  				DTYPE_CIDR, DTYPE_CIRCLE, DTYPE_FLOAT4, DTYPE_FLOAT8, DTYPE_INET, DTYPE_INT2, DTYPE_INT4, DTYPE_INT8,
	  				DTYPE_LINE, DTYPE_LSEG, DTYPE_MACADDR, DTYPE_MONEY, DTYPE_PATH, DTYPE_POINT, DTYPE_POLYGON,
	  				DTYPE_SERIAL, DTYPE_SERIAL4, DTYPE_SERIAL8, DTYPE_TEXT, DTYPE_TIMESTAMPZ, DTYPE_TSQUERY,
	  				DTYPE_TSVECTOR, DTYPE_TXID_SNAPSHOT, DTYPE_UUID, DTYPE_XML
	  	  	});
		
	  	static final List<String> CUSTOM_DATATYPE_START_WORDS = 
	  		Arrays.asList(new String[] {
	  				"BIGSERIAL", "BOX", "BOOLEAN", "BOX", "BYTEA",
	  				"CIDR", "CIRCLE", "FLOAT4", "FLOAT8", "INET", "INT2", "INT4", "INT8",
	  				"LINE", "LSEG", "MACADDR", "MONEY", "PATH", "POINT", "POLYGON",
	  				"SERIAL", "SERIAL4", "SERIAL8", "TEXT", "TIMESTAMPZ", "TSQUERY",
	  				"TSVECTOR", "TXID_SNAPSHOT", "UUID", "XML"
	  	  	});
	}
}
