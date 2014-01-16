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

/**
 *
 * @author kulikov
 */
public class DataType {
    public static final Rule LEFT_PAREN = new Identifier("left-paren", "\\(");
    public static final Rule RIGHT_PAREN = new Identifier("right-paren", "\\)");
    public static final Rule COMMA = new Identifier("comma", ",");
    
    
    public static final Rule LENGTH = new Identifier("length", "\\d+");
    public static final Rule LENGTH_DEF = new Sequence("length-value", LEFT_PAREN, LENGTH, RIGHT_PAREN);

    public static final Rule CHAR = new Identifier("char", "CHAR");
    public static final Rule CHARACTER = new Identifier("character", "CHARACTER");
    public static final Rule VARYING = new Identifier("varying", "VARYING");
    public static final Rule CHAR_VARYING = new Sequence("char-varying", CHAR, VARYING);    
    public static final Rule CHARACTER_VARYING = new Sequence("character-varying", CHARACTER, VARYING);
    public static final Rule VARCHAR = new Identifier("varchar", "VARCHAR");
    
    public static final Rule NATIONAL = new Identifier("national", "NATIONAL");
    public static final Rule NCHAR = new Identifier("nchar", "NCHAR");
    public static final Rule NATIONAL_CHAR = new Sequence(NATIONAL, CHAR);
    public static final Rule NATIONAL_CHARACTER = new Sequence(NATIONAL, CHARACTER);
    public static final Rule NATIONAL_CHAR_VARYING = new Sequence(NATIONAL, CHAR, VARYING);
    public static final Rule NATIONAL_CHARACTER_VARYING = new Sequence(NATIONAL, CHARACTER, VARYING);
    public static final Rule NCHAR_VARYING = new Sequence(NCHAR, VARYING);
    
    public static final Rule CHARACTER_STRING_TYPE = new Choise("character-string-type",
            new Sequence("char-", CHAR, LENGTH_DEF.optional()),
            new Sequence("character-"), CHARACTER, LENGTH_DEF.optional(),
            new Sequence("char-varying"), CHAR_VARYING, LENGTH_DEF.optional(),
            new Sequence("character-varying"), CHARACTER_VARYING, LENGTH_DEF.optional(),
            new Sequence("varchar"), VARCHAR, LENGTH_DEF.optional()
            );

    public static final Rule NATIONAL_CHARACTER_STRING_TYPE = new Choise("national-character-string-type",
            new Sequence("nchar-", NCHAR, LENGTH_DEF.optional()),
            new Sequence("nchar"), NATIONAL_CHAR, LENGTH_DEF.optional(),
            new Sequence("ncharacter-"), NATIONAL_CHARACTER, LENGTH_DEF.optional(),
            new Sequence("nchar-varying"), NATIONAL_CHAR_VARYING, LENGTH_DEF.optional(),
            new Sequence("ncharacter-varying"), NATIONAL_CHARACTER_VARYING, LENGTH_DEF.optional(),
            new Sequence("nvarchar"), NCHAR_VARYING, LENGTH_DEF.optional()
            );
    
    /** BIT STRING TYPE */
    public static final Rule BIT = new Sequence("bit", new Keyword("BIT"), LENGTH_DEF.optional());
    public static final Rule BIT_VARYING = new Sequence("bit", new Keyword("BIT"), new Keyword("VARYING"), LENGTH_DEF.optional());    
    public static final Rule BIT_STRING_TYPE = new Choise("bit-string-type", BIT, BIT_VARYING);
    
    /** NUMERIC TYPE */
    public static final Rule PRECISION = new Identifier("precision", "\\d+");
    public static final Rule SCALE = new Identifier("scale", "\\d+");
    public static final Rule SIZE = new Sequence(LEFT_PAREN, PRECISION, new Sequence(COMMA, SCALE).optional(), RIGHT_PAREN);
    
    public static final Rule NUMERIC = new Keyword("NUMERIC");
    public static final Rule DECIMAL = new Keyword("DECIMAL");
    public static final Rule DEC = new Keyword("DEC");
    public static final Rule INTEGER = new Keyword("INTEGER");
    public static final Rule INT = new Keyword("INT");
    public static final Rule SMALLINT = new Keyword("SMALLINT");
    
    public static final Rule FLOAT = new Keyword("FLOAT");
    public static final Rule REAL = new Keyword("REAL");
    public static final Rule DOUBLE_PRECISION = new Sequence(new Keyword("DOUBLE"), new Keyword("PRECISION"));
    
    public static final Rule EXACT_NUMERIC_TYPE = new Choise("exact-numeric-type",
            new Sequence(NUMERIC, SIZE.optional()),
            new Sequence(DECIMAL, SIZE.optional()),
            new Sequence(DEC, SIZE.optional()),
            INTEGER,
            INT, 
            SMALLINT);
    
    public static final Rule APPROXIMATE_NUMERIC_TYPE = new Choise("approximate-numeric-type",
            new Sequence(FLOAT, LENGTH_DEF.optional()),
            REAL,
            DOUBLE_PRECISION);
    public static final Rule NUMERIC_TYPE = new Choise("numeric-type", 
            EXACT_NUMERIC_TYPE, 
            APPROXIMATE_NUMERIC_TYPE);
    
    /** Date time type */
    public static final Rule TIME_PRECISION = new Identifier("time-precision", "\\d+");
    public static final Rule DATE = new Keyword("DATE");
    public static final Rule TIME = new Keyword("TIME");
    public static final Rule TIMESTAMP = new Keyword("TIMESTAMP");
    public static final Rule WITH_TIME_ZONE = new Sequence(
            new Keyword("WITH"),
            new Keyword("TIME"),
            new Keyword("ZONE"));
    public static final Rule DATETIME_TYPE = new Choise("datetime-type",
            DATE,
            new Sequence(TIME, new Sequence(LEFT_PAREN, TIME_PRECISION, RIGHT_PAREN).optional()), WITH_TIME_ZONE.optional(),
            new Sequence(TIMESTAMP, new Sequence(LEFT_PAREN, TIME_PRECISION, RIGHT_PAREN).optional()), WITH_TIME_ZONE.optional());
    
    /** Interval type */
    public static final Rule INTERVAL = new Keyword("INTERVAL");
    public static final Rule TO = new Keyword("TO");
    public static final Rule YEAR = new Keyword("YEAR");
    public static final Rule MONTH = new Keyword("MONTH");
    public static final Rule DAY = new Keyword("DAY");
    public static final Rule HOUR = new Keyword("HOUR");
    public static final Rule MINUTE = new Keyword("MINUTE");
    public static final Rule SECOND = new Keyword("SECOND");
    
    public static final Rule NON_SECOND_DATETIME = new Choise("non-second-datetime-field",
            YEAR,
            MONTH,
            DAY,
            HOUR,
            MINUTE);
    public static final Rule SINGLE_DATE_TIME = new Choise("single-datetime-field",
            new Sequence(NON_SECOND_DATETIME, LENGTH_DEF.optional()),
            new Sequence(SECOND, SIZE.optional()));
    
    public static final Rule START_FIELD = new Sequence(NON_SECOND_DATETIME, LENGTH_DEF.optional());
    public static final Rule END_FIELD = new Choise("end-field",
            NON_SECOND_DATETIME,
            new Sequence(SECOND, LENGTH_DEF.optional()));
    
    public static final Rule INTERVAL_QUALIFIER = new Choise("interval-qualifier",
            new Sequence(START_FIELD, TO, END_FIELD),
            SINGLE_DATE_TIME);
    
    public static final Rule INTERVAL_TYPE = new Sequence(INTERVAL, INTERVAL_QUALIFIER);
    
}
