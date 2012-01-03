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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.ddl.dialect.sybase;

import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.StandardDdlParser;

public class SybaseDdlParser extends StandardDdlParser {

    /*
     * 
     * 
     * Character
    	CHAR [ ( max-length ) ]
    	CHARACTER [ ( max-length ) ]
    	CHARACTER VARYING [ ( max-length ) ]
    	VARCHAR [ ( max-length ) ]
    	UNIQUEIDENTIFIERSTR
     * Numeric
    	[ UNSIGNED ] BIGINT
    	[ UNSIGNED ] { INT | INTEGER }
    	SMALLINT
    	TINYINT
    	DECIMAL [ ( precision [ , scale ] ) ]
    	NUMERIC [ ( precision [ , scale ] ) ]
    	DOUBLE
    	FLOAT [ ( precision ) ]
    	REAL
     * Binary
    	BINARY [ ( length ) ]
    	VARBINARY [ ( max-length ) ]
    	UNIQUEIDENTIFIER
     * Bit
    	BIT
     * Date/Time
    	DATE
    	DATETIME
    	SMALLDATETIME
    	TIME
    	TIMESTAMP


     * ===========================================================================================================================
     * Data Definition Statements
     ALLOCATE DESCRIPTOR
     ALTER [DATABASE | DBSPACE | DOMAIN | EVENT | FUNCTION | INDEX | (LOGIN POLICY) | PROCEDURE | SERVER | SERVICE | TABLE | USER | VIEW]
     BACKUP
     BEGIN PARALLEL IQ … END PARALLEL IQ statement
     BEGIN TRAN[SACTION] [ transaction-name ]
     CHECKPOINT
     COMMENT ON
     COMMIT [ WORK ]
     CONFIGURE
     CREATE [DATABASE | DBSPACE | DOMAIN | EVENT | (EXISTING TABLE) | EXTERNLOGIN | FUNCTION | INDEX | (JOIN INDEX) 
          | (LOGIN POLICY) | MESSAGE | PROCEDURE | SCHEMA | SERVER | SERVICE | TABLE | USER | VARIABLE | VIEW]
     DEALLOCATE DESCRIPTOR
     DECLARE LOCAL TEMPORARY TABLE
     DELETE
     DESCRIBE
     DROP [CONNECTION | DATABASE | DBSPACE | DOMAIN | EVENT | EXTERNALLOGIN | FUNCTION | INDEX | (JOIN INDEX) | (LOGIN POLICY) 
          | MESSAGE | PROCEDURE | SERVER | STATEMENT | TABLE | TRIGGER | USER | VARIABLE | VIEW]
     EXEC[UTE]
     EXIT
     FORWARD TO
     GET DESCRIPTOR
     GRANT
     INSERT
     INSTALL JAVA
     IQ UTILITIES
     LOAD TABLE
     LOCK TABLE
     OUTPUT TO
     PARAMETERS
     READ
     RELEASE SAVEPOINT
     REMOVE JAVA
     RESTORE DATABASE
     REVOKE
     ROLLBACK [WORK]
     ROLLBACK TO SAVEPOINT
     SAVEPOINT
     SET [CONNECTION | DESCRIPTION | OPTION | SQLCA]
     SIGNAL
     START [DATABASE | ENGINE | JAVA]
     STOP [DATABASE | ENGINE | JAVA]
     SYNCHRONIZE JOIN INDEX
     TRIGGER EVENT
     TRUNCATE TABLE
     UPDATE
     WAITFOR
     
     * ===========================================================================================================================
     * CREATE TABLE

    */

    private static final String[] COMMENT_ON = {"COMMENT", "ON"};
    private static final String TERMINATOR = "go";

    public SybaseDdlParser() {
        setTerminator(TERMINATOR);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.StandardDdlParser#initializeTokenStream(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    protected void initializeTokenStream( DdlTokenStream tokens ) {
        super.initializeTokenStream(tokens);
        tokens.registerStatementStartPhrase(COMMENT_ON);
    }
}
