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
package org.modeshape.sequencer.ddl.validator;

import java.util.ArrayList;
import static org.modeshape.sequencer.ddl.validator.DataType.BIT_STRING_TYPE;
import static org.modeshape.sequencer.ddl.validator.DataType.CHARACTER_STRING_TYPE;
import static org.modeshape.sequencer.ddl.validator.DataType.DATETIME_TYPE;
import static org.modeshape.sequencer.ddl.validator.DataType.INTERVAL_TYPE;
import static org.modeshape.sequencer.ddl.validator.DataType.NATIONAL_CHARACTER_STRING_TYPE;
import static org.modeshape.sequencer.ddl.validator.DataType.NUMERIC_TYPE;

/**
 *
 * @author kulikov
 */
public class Rules {
    private static final ArrayList<Rule> rules = new ArrayList();
    
    private static final Rule IDENTIFIER = new Keyword("\\w+");
    
    public static final Rule LEFT_PAREN = new Identifier("left-paren", "\\(");
    public static final Rule RIGHT_PAREN = new Identifier("right-paren", "\\)");
    public static final Rule COMMA = new Identifier("comma", ",");
    public static final Rule PERIOD = new Identifier("period", ".");
    
    //TODO
    private static final Rule LITERAL = new Keyword("\\w+");
    
    private static final Rule CREATE = new Keyword("CREATE");
    private static final Rule TABLE = new Keyword("TABLE");
    private static final Rule GLOBAL = new Keyword("GLOBAL");
    private static final Rule LOCAL = new Keyword("LOCAL");
    private static final Rule TEMPORARY = new Keyword("TEMPORARY");
    private static final Rule GLOBAL_LOCAL = new Choise("--", GLOBAL, LOCAL);
    private static final Rule GLOBAL_LOCAL_TEMPORARY = new Sequence("--", GLOBAL_LOCAL, TEMPORARY);
    
    public static final Rule ON = new Keyword("ON");
    public static final Rule COMMIT = new Keyword("COMMIT");
    public static final Rule DELETE = new Keyword("DELETE");
    public static final Rule PRESERVE = new Keyword("PRESERVE");
    public static final Rule ROWS = new Keyword("ROWS");
    
    public static final Rule CHARACTER = new Keyword("CHARACTER");
    public static final Rule SET = new Keyword("SET");
    public static final Rule CHARACTER_SET = new Sequence(CHARACTER, SET);

    public static final Rule UNQUALIFIED_SCHEMA_NAME = IDENTIFIER;
    public static final Rule CATALOG_NAME = IDENTIFIER;
    public static final Rule SCHEMA_NAME = new Sequence(new Sequence(CATALOG_NAME, PERIOD).optional(), UNQUALIFIED_SCHEMA_NAME);
    public static final Rule CHARSET_NAME = new Sequence(new Sequence(SCHEMA_NAME, PERIOD).optional(), IDENTIFIER);
    public static final Rule STANDARD_CHARACTER_REPERTOIR_NAME = CHARSET_NAME;
    public static final Rule IMPLEMENTATION_DEFINED_CHARACTER_REPERTOIR_NAME = CHARSET_NAME;
    public static final Rule USER_DEFINED_CHARACTER_REPERTOIR_NAME = CHARSET_NAME;
    public static final Rule STANDARD_UNIVERSAL_CHARACTER_REPERTOIR_NAME = CHARSET_NAME;
    public static final Rule IMPLEMENTATION_DEFINED_UNIVERSAL_CHARACTER_REPERTOIR_NAME = CHARSET_NAME;
    public static final Rule CHARACTER_SET_SPEC = new Choise("charset-spec",
            STANDARD_CHARACTER_REPERTOIR_NAME,
            IMPLEMENTATION_DEFINED_CHARACTER_REPERTOIR_NAME,
            USER_DEFINED_CHARACTER_REPERTOIR_NAME,
            STANDARD_UNIVERSAL_CHARACTER_REPERTOIR_NAME,
            IMPLEMENTATION_DEFINED_UNIVERSAL_CHARACTER_REPERTOIR_NAME);
    
    public static final Rule CHARSET = new Sequence(CHARACTER_SET, CHARACTER_SET_SPEC);
    
    private static final Rule TABLE_NAME=new Keyword("\\w+");
    
    public static final Rule DATA_TYPE = new Choise("data-type",
            new Sequence(CHARACTER_STRING_TYPE, CHARSET.optional()),
            NATIONAL_CHARACTER_STRING_TYPE,
            BIT_STRING_TYPE,
            NUMERIC_TYPE,
            DATETIME_TYPE,
            INTERVAL_TYPE);
    
    private static final Rule DEFAULT = new Keyword("DEFAULT");
    
    
    private static final Rule DEFAULT_OPTION = new Choise("default-option", LITERAL);
    private static final Rule DEFAULT_CLAUSE = new Sequence("default-clause", DEFAULT, DEFAULT_OPTION);
    
    private static final Rule COLUMN_NAME = IDENTIFIER;
    
    //TODO
    private static final Rule COLUMN_CONSTRAINT_DEFINITION = new Sequence("--", new Keyword("NOT"), new Keyword("NULL"));;
    
    private static final Rule COLUMN_DEFINITION = new Sequence("column-definition", 
            COLUMN_NAME, DATA_TYPE, 
            DEFAULT_CLAUSE.optional(),
            COLUMN_CONSTRAINT_DEFINITION.optional());
    
    //TODO
    private static final Rule TABLE_CONSTRAINT_DEFINITION = new Keyword("");
    
    private static final Rule TABLE_ELEMENT = new Choise("table-element", COLUMN_DEFINITION, TABLE_CONSTRAINT_DEFINITION);
    private static final Rule TABLE_ELEMENT_LIST = new Sequence("table-element-list", LEFT_PAREN, new List("--", TABLE_ELEMENT), RIGHT_PAREN);
    
    private static final Rule TABLE_DEFINITION = new Sequence("table-definition",
            CREATE, GLOBAL_LOCAL_TEMPORARY.optional(), TABLE, TABLE_NAME, TABLE_ELEMENT_LIST,
            new Sequence(ON, COMMIT, new Choise("--", DELETE, PRESERVE), ROWS).optional());
    
    static {
        rules.add(new Keyword("CREATE"));
        rules.add(new Keyword("GLOBAL"));
        rules.add(new Keyword("TEMPORARY"));
        
        rules.add(TABLE_DEFINITION);
    }
    
    public static Rule find(String lhs) {
        for (Rule rule : rules) {
            if (rule.lhs().equals(lhs)) {
                return rule;
            }
        }
        return null;
    }
}
