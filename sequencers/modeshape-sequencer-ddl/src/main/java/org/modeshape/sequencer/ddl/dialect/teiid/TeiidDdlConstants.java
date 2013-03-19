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
 * 02110-1301 USA, or see the FSF site(http://www.fsf.org.
 */
package org.modeshape.sequencer.ddl.dialect.teiid;

import java.util.ArrayList;
import java.util.List;
import org.modeshape.sequencer.ddl.DdlConstants;

/**
 * DDL constants specific to the Teiid dialect. Teiid's <a
 * href="https://docs.jboss.org/author/display/TEIID/BNF+for+SQL+Grammar">BNF for SQL Grammar (updated Feb 14, 2013)</a> was used.
 */
public interface TeiidDdlConstants extends DdlConstants {

    /**
     * An object that has a DDL representation.
     */
    interface DdlElement {

        /**
         * @return the DDL string representation (never <code>null</code> or empty)
         */
        String toDdl();

    }

    /**
     * <code>
     * ( <create table> | <create procedure> | <option namespace> | <alter options> | <create trigger> ) ( <semicolon> )?
     * </code>
     */
    enum DdlStatement implements DdlElement {

        /**
         * Create Table
         * <p>
         * <code>
         * CREATE ( FOREIGN TABLE | ( VIRTUAL )? VIEW ) <identifier> <create table body> ( AS <query expression> )?
         * </code>
         */
        CREATE_FOREIGN_TABLE(CREATE, FOREIGN, TABLE),
        CREATE_VIRTUAL_VIEW(CREATE, TeiidReservedWord.VIRTUAL.name(), VIEW),
        CREATE_VIEW(CREATE, VIEW),

        /**
         * Create Procedure
         * <p>
         * <code>
         * CREATE ( VIRTUAL | FOREIGN )? ( PROCEDURE | FUNCTION ) ( <identifier> <lparen> ( <procedure parameter> ( <comma> <procedure parameter> )* )? <rparen> ( RETURNS ( ( ( TABLE )? <lparen> <procedure result column> ( <comma> <procedure result column> )* <rparen> ) | <data type> ) )? ( <options clause> )? ( AS <statement> )? )
         * </code>
         */
        CREATE_VIRTUAL_FUNCTION(CREATE, TeiidReservedWord.VIRTUAL.name(), TeiidReservedWord.FUNCTION.name()),
        CREATE_VIRTUAL_PROCEDURE(CREATE, TeiidReservedWord.VIRTUAL.name(), TeiidReservedWord.PROCEDURE.name()),
        CREATE_FOREIGN_FUNCTION(CREATE, FOREIGN, TeiidReservedWord.FUNCTION.name()),
        CREATE_FOREIGN_PROCEDURE(CREATE, FOREIGN, TeiidReservedWord.PROCEDURE.name()),
        CREATE_FUNCTION(CREATE, TeiidReservedWord.FUNCTION.name()),
        CREATE_PROCEDURE(CREATE, TeiidReservedWord.PROCEDURE.name()),

        /**
         * Option Namespace
         * <p>
         * <code>
         * SET NAMESPACE <string> AS <identifier>
         * </code>
         */
        OPTION_NAMESPACE(SET, TeiidNonReservedWord.NAMESPACE.name()),

        /**
         * Alter Options
         * <p>
         * <code>
         * ALTER ( VIRTUAL | FOREIGN )? ( TABLE | VIEW | PROCEDURE ) <identifier> ( <alter options list> | <alter column options> )
         * </code>
         */
        ALTER_VIRTUAL_PROCEDURE(ALTER, TeiidReservedWord.VIRTUAL.name(), TeiidReservedWord.PROCEDURE.name()),
        ALTER_VIRTUAL_TABLE(ALTER, TeiidReservedWord.VIRTUAL.name(), TABLE),
        ALTER_VIRTUAL_VIEW(ALTER, TeiidReservedWord.VIRTUAL.name(), VIEW),
        ALTER_FOREIGN_PROCEDURE(ALTER, FOREIGN, TeiidReservedWord.PROCEDURE.name()),
        ALTER_FOREIGN_TABLE(ALTER, FOREIGN, TABLE),
        ALTER_FOREIGN_VIEW(ALTER, FOREIGN, VIEW),
        ALTER_PROCEDURE(ALTER, TeiidReservedWord.PROCEDURE.name()),
        ALTER_TABLE(ALTER, TABLE),
        ALTER_VIEW(ALTER, VIEW),

        /**
         * Create Trigger
         * <p>
         * <code>
         * CREATE TRIGGER ON <identifier> INSTEAD OF ( INSERT | UPDATE | DELETE ) AS <for each row trigger action>
         * </code>
         */
        CREATE_TRIGGER(CREATE, TeiidReservedWord.TRIGGER.name(), ON);

        private final String[] tokens;

        private DdlStatement( final String... tokens ) {
            this.tokens = tokens;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlElement#toDdl()
         */
        @Override
        public String toDdl() {
            boolean firstTime = true;
            final StringBuilder ddl = new StringBuilder();

            for (final String token : this.tokens) {
                if (firstTime) {
                    firstTime = false;
                } else {
                    ddl.append(' ');
                }

                ddl.append(token);
            }

            return ddl.toString();
        }

        /**
         * @return the tokens used by the DDL tokenizer (never <code>null</code> or empty)
         */
        public String[] tokens() {
            return this.tokens;
        }

    }

    /**
     * Teiid data types.
     */
    enum TeiidDataType implements DdlElement {

        BIGDECIMAL,
        BIGINT,
        BIGINTEGER,
        BLOB,
        BOOLEAN,
        BYTE,
        CHAR,
        CLOB,
        DATE,
        DECIMAL,
        DOUBLE,
        FLOAT,
        INTEGER,
        LONG,
        OBJECT,
        REAL,
        SHORT,
        SMALLINT,
        STRING,
        TIME,
        TIMESTAMP,
        TINYINT,
        VARBINARY,
        VARCHAR,
        XML;

        private static List<String> _startWords;

        public static List<String> getStartWords() {
            if (_startWords == null) {
                final TeiidDataType[] values = values();
                _startWords = new ArrayList<String>(values.length);

                for (final TeiidDataType type : values) {
                    _startWords.add(type.toDdl());
                }
            }

            return _startWords;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlElement#toDdl()
         */
        @Override
        public String toDdl() {
            return toString();
        }

    }

    /**
     * Teiid future reserved words.
     */
    enum TeiidFutureReserveWord {

        ALLOCATE,
        ARE,
        ARRAY,
        ASENSITIVE,
        ASYMETRIC,
        AUTHORIZATION,
        BINARY,
        CALLED,
        CASCADED,
        CHARACTER,
        CHECK,
        CLOSE,
        COLLATE,
        COMMIT,
        CONNECT,
        CORRESPONDING,
        CRITERIA,
        CURRENT_DATE,
        CURRENT_TIME,
        CURRENT_TIMESTAMP,
        CURRENT_USER,
        CURSOR,
        CYCLE,
        DATALINK,
        DEALLOCATE,
        DEC,
        DEREF,
        DESCRIBE,
        DETERMINISTIC,
        DISCONNECT,
        DLNEWCOPY,
        DLPREVIOUSCOPY,
        DLURLCOMPLETE,
        DLURLCOMPLETEONLY,
        DLURLCOMPLETEWRITE,
        DLURLPATH,
        DLURLPATHONLY,
        DLURLPATHWRITE,
        DLURLSCHEME,
        DLURLSERVER,
        DLVALUE,
        DYNAMIC,
        ELEMENT,
        EXTERNAL,
        FREE,
        GET,
        GLOBAL,
        GRANT,
        HAS,
        HOLD,
        IDENTITY,
        IMPORT,
        INDICATOR,
        INPUT,
        INSENSITIVE,
        INT,
        INTERVAL,
        ISOLATION,
        LARGE,
        LOCALTIME,
        LOCALTIMESTAMP,
        MATCH,
        MEMBER,
        METHOD,
        MODIFIES,
        MODULE,
        MULTISET,
        NATIONAL,
        NATURAL,
        NCHAR,
        NCLOB,
        NEW,
        NONE,
        NUMERIC,
        OLD,
        OPEN,
        OUTPUT,
        OVERLAPS,
        PRECISION,
        PREPARE,
        RANGE,
        READS,
        RECURSIVE,
        REFERENCING,
        RELEASE,
        REVOKE,
        ROLLBACK,
        ROLLUP,
        SAVEPOINT,
        SCROLL,
        SEARCH,
        SENSITIVE,
        SESSION_USER,
        SPECIFIC,
        SPECIFICTYPE,
        SQL,
        START,
        STATIC,
        SUBMULTILIST,
        SYMETRIC,
        SYSTEM,
        SYSTEM_USER,
        TIMEZONE_HOUR,
        TIMEZONE_MINUTE,
        TRANSLATION,
        TREAT,
        VALUE,
        VARYING,
        WHENEVER,
        WINDOW,
        WITHIN,
        XMLBINARY,
        XMLCAST,
        XMLDOCUMENT,
        XMLEXISTS,
        XMLITERATE,
        XMLTEXT,
        XMLVALIDATE

    }

    /**
     * Teiid non-reserved words.
     */
    enum TeiidNonReservedWord implements DdlElement {

        ACCESSPATTERN,
        ARRAYTABLE,
        AUTO_INCREMENT,
        AVG,
        CHAIN,
        COLUMNS,
        CONTENT,
        COUNT,
        DELIMITER,
        DENSE_RANK,
        DISABLED,
        DOCUMENT,
        EMPTY,
        ENABLED,
        ENCODING,
        EVERY,
        EXCEPTION,
        EXCLUDING,
        EXTRACT,
        FIRST,
        HEADER,
        INCLUDING,
        INDEX,
        INSTEAD,
        JSONARRAY_AGG,
        JSONOBJECT,
        KEY,
        LAST,
        MAX,
        MIN,
        NAME,
        NAMESPACE,
        NEXT,
        NULLS,
        OBJECTTABLE,
        ORDINALITY,
        PASSING,
        PATH,
        QUERYSTRING,
        QUOTE,
        RAISE,
        RANK,
        RESULT,
        ROW_NUMBER,
        SELECTOR,
        SERIAL,
        SKIP,
        SQL_TSI_DAY,
        SQL_TSI_FRAC_SECOND,
        SQL_TSI_HOUR,
        SQL_TSI_MINUTE,
        SQL_TSI_MONTH,
        SQL_TSI_QUARTER,
        SQL_TSI_SECOND,
        SQL_TSI_WEEK,
        SQL_TSI_YEAR,
        STDDEV_POP,
        STDDEV_SAMP,
        SUBSTRING,
        SUM,
        TEXTAGG,
        TEXTTABLE,
        TIMESTAMPADD,
        TIMESTAMPDIFF,
        TO_BYTES,
        TO_CHARS,
        TRIM,
        VAR_POP,
        VAR_SAMP,
        VARIADIC,
        VERSION,
        VIEW,
        WELLFORMED,
        WIDTH,
        XMLDECLARATION;

        private static List<String> _list;

        public static List<String> asList() {
            if (_list == null) {
                final TeiidNonReservedWord[] values = values();
                _list = new ArrayList<String>(values.length);

                for (final TeiidNonReservedWord word : values) {
                    _list.add(word.name());
                }
            }

            return _list;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlElement#toDdl()
         */
        @Override
        public String toDdl() {
            return toString();
        }

    }

    /**
     * Teiid reserved words.
     */
    enum TeiidReservedWord implements DdlElement {

        ADD,
        ALL,
        ALLOCATE,
        ALTER,
        AND,
        ANY,
        ARRAY_AGG,
        AS,
        ASC,
        ATOMIC,
        BEGIN,
        BETWEEN,
        BOTH,
        BREAK,
        BY,
        CALL,
        CASE,
        COLUMN,
        CONSTRAINT,
        CONTINUE,
        CREATE,
        CROSS,
        DAY,
        DECLARE,
        DEFAULT,
        DELETE,
        DESC,
        DISTINCT,
        DROP,
        EACH,
        ELSE,
        END,
        ERROR,
        ESCAPE,
        EXCEPT,
        EXEC,
        EXECUTE,
        EXISTS,
        FALSE,
        FETCH,
        FILTER,
        FOR,
        FOREIGN,
        FROM,
        FULL,
        FUNCTION,
        GROUP,
        HAVING,
        HOUR,
        IF,
        IMMEDIATE,
        IN,
        INNER,
        INOUT,
        INSERT,
        INTERSECT,
        INTO,
        IS,
        JOIN,
        LANGUAGE,
        LATERAL,
        LEADING,
        LEAVE,
        LEFT,
        LIKE,
        LIKE_REGEX,
        LIMIT,
        LOCAL,
        LOOP,
        MAKEDEP,
        MAKENOTDEP,
        MERGE,
        MINUTE,
        MONTH,
        NO,
        NOCACHE,
        NOT,
        NULL,
        OF,
        OFFSET,
        ON,
        ONLY,
        OPTION,
        OPTIONS,
        OR,
        ORDER,
        OUT,
        OUTER,
        OVER,
        PARAMETER,
        PARTITION,
        PREPARE,
        PRIMARY,
        PROCEDURE,
        REFERENCES,
        RETURN,
        RETURNS,
        RIGHT,
        ROLLUP,
        ROW,
        ROWS,
        SECOND,
        SELECT,
        SET,
        SIMILAR,
        SOME,
        SQLEXCEPTION,
        SQLSTATE,
        SQLWARNING,
        TABLE,
        TEMPORARY,
        THEN,
        TO,
        TRAILING,
        TRANSLATE,
        TRIGGER,
        TRUE,
        UNION,
        UNIQUE,
        UNKNOWN,
        UPDATE,
        USER,
        USING,
        VALUES,
        VIRTUAL,
        WHEN,
        WHERE,
        WHILE,
        WITH,
        WITHOUT,
        XMLAGG,
        XMLATTRIBUTES,
        XMLCOMMENT,
        XMLCONCAT,
        XMLELEMENT,
        XMLFOREST,
        XMLNAMESPACES,
        XMLPARSE,
        XMLPI,
        XMLQUERY,
        XMLSERIALIZE,
        XMLTABLE,
        YEAR;

        private static List<String> _list;

        public static List<String> asList() {
            if (_list == null) {
                final TeiidReservedWord[] values = values();
                _list = new ArrayList<String>(values.length);

                for (final TeiidReservedWord word : values) {
                    _list.add(word.name());
                }
            }

            return _list;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlElement#toDdl()
         */
        @Override
        public String toDdl() {
            return toString();
        }

    }

    /**
     * Indicates if element is foreign/physical or virtual.
     */
    enum SchemaElementType implements DdlElement {
        FOREIGN,
        VIRTUAL;

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.DdlElement#toDdl()
         */
        @Override
        public String toDdl() {
            return toString();
        }
    }

}
