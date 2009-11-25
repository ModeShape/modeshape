package org.jboss.dna.sequencer.ddl.dialect.db2;

import org.jboss.dna.sequencer.ddl.StandardDdlParser;



public class Db2DdlParser extends StandardDdlParser {
	private static final String[] COMMENT_ON = {"COMMENT", "ON"};

	
	/*
	
	 * ===========================================================================================================================
	 * Data Types

		|--+-SMALLINT---------------------------------------------------------------+--|
		   +-+-INTEGER-+------------------------------------------------------------+
		   | '-INT-----'                                                            |
		   +-BIGINT-----------------------------------------------------------------+
		   +-+-FLOAT--+---------------+-+-------------------------------------------+
		   | |        '-(--integer--)-' |                                           |
		   | +-REAL---------------------+                                           |
		   | |         .-PRECISION-.    |                                           |
		   | '-DOUBLE--+-----------+----'                                           |
		   +-+-DECIMAL-+--+-----------------------------+---------------------------+
		   | +-DEC-----+  '-(--integer--+----------+--)-'                           |
		   | +-NUMERIC-+                '-,integer-'                                |
		   | '-NUM-----'                                                            |
		   +-+-+-CHARACTER-+--+-----------+--------------+--+---------------------+-+
		   | | '-CHAR------'  '-(integer)-'              |  |  (1)                | |
		   | +-+-VARCHAR----------------+--(--integer--)-+  '--------FOR BIT DATA-' |
		   | | '-+-CHARACTER-+--VARYING-'                |                          |
		   | |   '-CHAR------'                           |                          |
		   | '-LONG VARCHAR------------------------------'                          |
		   +-+-+-BLOB----------------+---------+--+----------------------+----------+
		   | | '-BINARY LARGE OBJECT-'         |  '-2 (--2 integer--+---+--2 )-'          |
		   | +-+-CLOB------------------------+-+                +-2 K-+               |
		   | | '-+-CHARACTER-+--LARGE OBJECT-' |                +-2 M-+               |
		   | |   '-CHAR------'                 |                '-2 G-'               |
		   | '-DBCLOB--------------------------'                                    |
		   +-GRAPHIC--+-----------+-------------------------------------------------+
		   |          '-(integer)-'                                                 |
		   +-VARGRAPHIC--(integer)--------------------------------------------------+
		   +-LONG VARGRAPHIC--------------------------------------------------------+
		   +-DATE-------------------------------------------------------------------+
		   +-TIME-------------------------------------------------------------------+
		   +-TIMESTAMP--------------------------------------------------------------+
		   +-DATALINK--+---------------+--------------------------------------------+
		   |           '-(--integer--)-'                                            |
		   +-distinct-type-name-----------------------------------------------------+
		   +-structured-type-name---------------------------------------------------+
		   '-REF--(type-name2)------------------------------------------------------'


	
	 * ===========================================================================================================================
	 * Data Definition Statements
	 ALLOCATE CURSOR
	 ALTER [BUFFERPON | (DATABASE PARTITION GROUP) | DATABASE | FUNCTION | METHOD | NICKNAME | PROCEDURE | SEQUENCE | SERVER 
	      | TABLE | TABLESPACE | TYPE | (USER MAPPING) | VIEW | WRAPPER]
	 ASSOCIATE LOCATORS
	 CASE
	 COMMENT ON
	 COMMIT [WORK]
	 CREATE [ALIAS | BUFFERPOOL | (DATABASE PARTITION GROUP) | (DISTINCT TYPE) | (EVENT MONITOR) | FUNCTION | (FUNCTION MAPPING) 
	      | INDEX | METHOD | NICKNAME | PROCEDURE | SCHEMA | SEQUENCE | SERVER | TABLE | TABLESPACE | TRANSFORM | TRIGGER | TYPE
	      | (TYPE MAPPING) | (USER MAPPING) | VIEW | WRAPPER]
	 DECLARE GLOBAL TEMPORARY TABLE
	 DELETE FROM
	 DROP [ALIAS | BUFFERPOOL | (DATABASE PARTITION GROUP) | (EVENT MONITOR) | FUNCTION | (SPECIFIC FUNCTION) | (FUNCTION MAPPING) 
	      | INDEX | (INDEX EXTENSION) | METHOD | (SPECIFIC METHOD) | NICKNAME | PACKAGE | PROCEDURE | (SPECIFIC PROCEDURE) | SCHEMA 
	      | SEQUENCE | SERVER | TABLE | (TABLE HIERARCHY) | TABLESPACE[S] | TRANSFORM[S] | TRIGGER | TYPE | (TYPE MAPPING) 
	      | (USER MAPPING) | (USER MAPPING FOR) | VIEW | (VIEW HIERARCHY) | WRAPPER]
	
	*/
	
	private static final String TERMINATOR = "%"; 
	
	public Db2DdlParser() {
		super();
		initialize();
	}
	
	
	private void initialize() {
		registerStatementStartPhrase(COMMENT_ON);
		setTerminator(TERMINATOR);
	}
	// ===========================================================================================================================
	// ===========================================================================================================================
	
	
	// ===========================================================================================================================
	// ===========================================================================================================================
	
	
	// ===========================================================================================================================
	// ===========================================================================================================================

}
