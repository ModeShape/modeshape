/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.ddl.dialect.derby;

import java.util.Arrays;
import java.util.List;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 * @author blafond
 */
public interface DerbyDdlConstants extends DdlConstants {
    public static final String[] CUSTOM_KEYWORDS = {"TRIGGER", "SYNOMYM", "LOCK", "ISOLATION", "SQLID", INDEX, "RENAME",
        "DECLARE", "RESTART", "LOCKSIZE", "INCREMENT", "GENERATED", "ALWAYS", "BIGINT", "CLOB", "BLOB", "EXCLUSIVE",
        "REFERENCING"};

    interface DerbyStatementStartPhrases {

        static final String[][] ALTER_PHRASES = {};

        static final String[] STMT_CREATE_FUNCTION = {CREATE, "FUNCTION"};
        static final String[] STMT_CREATE_INDEX = {CREATE, "INDEX"};
        static final String[] STMT_CREATE_UNIQUE_INDEX = {CREATE, "UNIQUE", INDEX};
        static final String[] STMT_CREATE_PROCEDURE = {CREATE, "PROCEDURE"};
        static final String[] STMT_CREATE_ROLE = {CREATE, "ROLE"};
        static final String[] STMT_CREATE_SYNONYM = {CREATE, "SYNONYM"};
        static final String[] STMT_CREATE_TRIGGER = {CREATE, "TRIGGER"};

        static final String[][] CREATE_PHRASES = {STMT_CREATE_FUNCTION, STMT_CREATE_INDEX, STMT_CREATE_UNIQUE_INDEX,
            STMT_CREATE_PROCEDURE, STMT_CREATE_ROLE, STMT_CREATE_SYNONYM, STMT_CREATE_TRIGGER};

        static final String[] STMT_DECLARE_GLOBAL_TEMP_TABLE = {"DECLARE", "GLOBAL", "TEMPORARY", "TABLE"};
        static final String[] STMT_DROP_FUNCTION = {DROP, "FUNCTION"};
        static final String[] STMT_DROP_INDEX = {DROP, "INDEX"};
        static final String[] STMT_DROP_PROCEDURE = {DROP, "PROCEDURE"};
        static final String[] STMT_DROP_ROLE = {DROP, "ROLE"};
        static final String[] STMT_DROP_SYNONYM = {DROP, "SYNONYM"};
        static final String[] STMT_DROP_TRIGGER = {DROP, "TRIGGER"};

        static final String[][] DROP_PHRASES = {STMT_DROP_FUNCTION, STMT_DROP_INDEX, STMT_DROP_PROCEDURE, STMT_DROP_ROLE,
            STMT_DROP_SYNONYM, STMT_DROP_TRIGGER};

        static final String[] STMT_LOCK_TABLE = {"LOCK", TABLE};
        static final String[] STMT_RENAME_TABLE = {"RENAME", TABLE};
        static final String[] STMT_RENAME_INDEX = {"RENAME", INDEX};

        static final String[] STMT_SET_ISOLATION = {SET, "ISOLATION"};
        static final String[] STMT_SET_CURRENT_ISOLATION = {SET, "CURRENT", "ISOLATION"};
        static final String[] STMT_SET_ROLE = {SET, "ROLE"};
        static final String[] STMT_SET_SCHEMA = {SET, "SCHEMA"};
        static final String[] STMT_SET_CURRENT_SCHEMA = {SET, "CURRENT", SCHEMA};
        static final String[] STMT_SET_CURRENT_SQLID = {SET, "CURRENT", "SQLID"};

        static final String[][] SET_PHRASES = {STMT_SET_ISOLATION, STMT_SET_CURRENT_ISOLATION, STMT_SET_ROLE, STMT_SET_SCHEMA,
            STMT_SET_CURRENT_SCHEMA, STMT_SET_CURRENT_SQLID};

        static final String[][] MISC_PHRASES = {STMT_LOCK_TABLE, STMT_RENAME_TABLE, STMT_RENAME_INDEX,
            STMT_DECLARE_GLOBAL_TEMP_TABLE};

        // COULD NOT FIND ACTUAL REFERENCE... assuming the following....

        public final static String[] VALID_SCHEMA_CHILD_STMTS = {StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT,
            StandardDdlLexicon.TYPE_CREATE_VIEW_STATEMENT, StandardDdlLexicon.TYPE_GRANT_ON_TABLE_STATEMENT};
    }

    interface DerbyDataTypes {
        static final String[] DTYPE_BIGINT = {"BIGINT"};
        static final String[] DTYPE_LONG_VARCHAR = {"LONG", "VARCHAR"};
        static final String[] DTYPE_LONG_VARCHAR_FBD = {"LONG", "VARCHAR", "FOR", "BIT", "DATA"};

        static final String[] DTYPE_DOUBLE = {"DOUBLE"};
        static final String[] DTYPE_XML = {"XML"};
        static final String[] DTYPE_CLOB = {"CLOB"}; // CLOB [ ( length [{K |M |G }] ) ]
        static final String[] DTYPE_CHARACTER_LARGE_OBJECT = {"CHARACTER", "LARGE", "OBJECT"}; // [ ( length [{K |M |G }] ) ]
        static final String[] DTYPE_BLOB = {"BLOB"}; // BLOB [ ( length [{K |M |G }] ) ]
        static final String[] DTYPE_BINARY_LARGE_OBJECT = {"BINARY", "LARGE", "OBJECT"}; // [ ( length [{K |M |G }] ) ]

        static final List<String[]> CUSTOM_DATATYPE_START_PHRASES = Arrays.asList(DTYPE_BIGINT,
                                                                                  DTYPE_LONG_VARCHAR,
                                                                                  DTYPE_LONG_VARCHAR_FBD,
                                                                                  DTYPE_DOUBLE,
                                                                                  DTYPE_XML,
                                                                                  DTYPE_CLOB,
                                                                                  DTYPE_CHARACTER_LARGE_OBJECT,
                                                                                  DTYPE_BLOB,
                                                                                  DTYPE_BINARY_LARGE_OBJECT);

        static final List<String> CUSTOM_DATATYPE_START_WORDS = Arrays.asList("BIGINT",
                                                                              "LONG",
                                                                              "DOUBLE",
                                                                              "XML",
                                                                              "CLOB",
                                                                              "CHARACTER",
                                                                              "BLOB",
                                                                              "BINARY");
    }
}
