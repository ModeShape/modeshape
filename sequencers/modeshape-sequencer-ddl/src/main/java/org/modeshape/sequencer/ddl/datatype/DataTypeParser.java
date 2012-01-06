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
package org.modeshape.sequencer.ddl.datatype;

import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_LENGTH;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_PRECISION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_SCALE;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A parser for SQL data types.
 */
public class DataTypeParser implements DdlConstants {
    private static List<String[]> basicCharStringTypes = new ArrayList<String[]>();
    private static List<String[]> basicNationalCharStringTypes = new ArrayList<String[]>();
    private static List<String[]> basicBitStringTypes = new ArrayList<String[]>();
    private static List<String[]> basicExactNumericTypes = new ArrayList<String[]>();
    private static List<String[]> basicApproxNumericStringTypes = new ArrayList<String[]>();
    private static List<String[]> basicDateTimeTypes = new ArrayList<String[]>();
    private static List<String[]> basicMiscTypes = new ArrayList<String[]>();

    private int defaultLength = 255;
    private int defaultPrecision = 0;
    private int defaultScale = 0;

    public DataTypeParser() {
        super();

        initialize();
    }

    private void initialize() {

        basicCharStringTypes.add(DataTypes.DTYPE_CHARACTER);
        basicCharStringTypes.add(DataTypes.DTYPE_CHAR);
        basicCharStringTypes.add(DataTypes.DTYPE_CHARACTER_VARYING);
        basicCharStringTypes.add(DataTypes.DTYPE_CHAR_VARYING);
        basicCharStringTypes.add(DataTypes.DTYPE_VARCHAR);

        basicNationalCharStringTypes.add(DataTypes.DTYPE_NCHAR);
        basicNationalCharStringTypes.add(DataTypes.DTYPE_NATIONAL_CHARACTER);
        basicNationalCharStringTypes.add(DataTypes.DTYPE_NATIONAL_CHARACTER_VARYING);
        basicNationalCharStringTypes.add(DataTypes.DTYPE_NATIONAL_CHAR);
        basicNationalCharStringTypes.add(DataTypes.DTYPE_NATIONAL_CHAR_VARYING);
        basicNationalCharStringTypes.add(DataTypes.DTYPE_NCHAR_VARYING);

        basicBitStringTypes.add(DataTypes.DTYPE_BIT);
        basicBitStringTypes.add(DataTypes.DTYPE_BIT_VARYING);

        basicExactNumericTypes.add(DataTypes.DTYPE_NUMERIC);
        basicExactNumericTypes.add(DataTypes.DTYPE_DEC);
        basicExactNumericTypes.add(DataTypes.DTYPE_DECIMAL);
        basicExactNumericTypes.add(DataTypes.DTYPE_INTEGER);
        basicExactNumericTypes.add(DataTypes.DTYPE_INT);
        basicExactNumericTypes.add(DataTypes.DTYPE_SMALLINT);

        basicApproxNumericStringTypes.add(DataTypes.DTYPE_FLOAT);
        basicApproxNumericStringTypes.add(DataTypes.DTYPE_REAL);
        basicApproxNumericStringTypes.add(DataTypes.DTYPE_DOUBLE_PRECISION);

        basicDateTimeTypes.add(DataTypes.DTYPE_DATE);
        basicDateTimeTypes.add(DataTypes.DTYPE_TIME);
        basicDateTimeTypes.add(DataTypes.DTYPE_TIMESTAMP);

        basicMiscTypes.add(DataTypes.DTYPE_INTERVAL);

    }

    /**
     * Method determines if the next set of tokens matches one of the registered data type token sets.
     * 
     * @param tokens
     * @return is registered data type
     * @throws ParsingException
     */
    public final boolean isDatatype( DdlTokenStream tokens ) throws ParsingException {
        // Loop through the registered statement start string arrays and look for exact matches.

        for (String[] stmts : basicCharStringTypes) {
            if (tokens.matches(stmts)) return true;
        }

        for (String[] stmts : basicNationalCharStringTypes) {
            if (tokens.matches(stmts)) return true;
        }

        for (String[] stmts : basicBitStringTypes) {
            if (tokens.matches(stmts)) return true;
        }

        for (String[] stmts : basicExactNumericTypes) {
            if (tokens.matches(stmts)) return true;
        }

        for (String[] stmts : basicApproxNumericStringTypes) {
            if (tokens.matches(stmts)) return true;
        }

        for (String[] stmts : basicDateTimeTypes) {
            if (tokens.matches(stmts)) return true;
        }

        for (String[] stmts : basicMiscTypes) {
            if (tokens.matches(stmts)) return true;
        }

        // If no type is found, assume it's a custom type
        return isCustomDataType(tokens);
    }

    /**
     * Method determines if the next set of tokens matches one of the registered data type token sets.
     * 
     * @param tokens
     * @param type
     * @return is registered data type
     * @throws ParsingException
     */
    private boolean isDatatype( DdlTokenStream tokens,
                                int type ) throws ParsingException {
        // Loop through the registered statement start string arrays and look for exact matches.

        switch (type) {
            case DataTypes.DTYPE_CODE_CHAR_STRING: {
                for (String[] stmts : basicCharStringTypes) {
                    if (tokens.matches(stmts)) return true;
                }
            }
                break;
            case DataTypes.DTYPE_CODE_NCHAR_STRING: {
                for (String[] stmts : basicNationalCharStringTypes) {
                    if (tokens.matches(stmts)) return true;
                }
            }
                break;
            case DataTypes.DTYPE_CODE_BIT_STRING: {
                for (String[] stmts : basicBitStringTypes) {
                    if (tokens.matches(stmts)) return true;
                }
            }
                break;
            case DataTypes.DTYPE_CODE_EXACT_NUMERIC: {
                for (String[] stmts : basicExactNumericTypes) {
                    if (tokens.matches(stmts)) return true;
                }
            }
                break;
            case DataTypes.DTYPE_CODE_APROX_NUMERIC: {
                for (String[] stmts : basicApproxNumericStringTypes) {
                    if (tokens.matches(stmts)) return true;
                }
            }
                break;
            case DataTypes.DTYPE_CODE_DATE_TIME: {
                for (String[] stmts : basicDateTimeTypes) {
                    if (tokens.matches(stmts)) return true;
                }
            }
                break;
            case DataTypes.DTYPE_CODE_MISC: {
                for (String[] stmts : basicMiscTypes) {
                    if (tokens.matches(stmts)) return true;
                }
            }
                break;
        }

        return false;
    }

    /**
     * Method to determine of next tokens represent a custom data type. Subclasses should override this method and perform token
     * checks for any non-SQL92 spec'd data types.
     * 
     * @param tokens
     * @return is custom data type
     * @throws ParsingException
     */
    protected boolean isCustomDataType( DdlTokenStream tokens ) throws ParsingException {
        return false;
    }

    /**
     * Method which performs the actual parsing of the data type name and applicable values (i.e. VARCHAR(20)) if data type is
     * found.
     * 
     * @param tokens
     * @return the {@link DataType}
     * @throws ParsingException
     */
    public DataType parse( DdlTokenStream tokens ) throws ParsingException {
        DataType result = null;

        if (isDatatype(tokens, DataTypes.DTYPE_CODE_CHAR_STRING)) {
            result = parseCharStringType(tokens);
        } else if (isDatatype(tokens, DataTypes.DTYPE_CODE_NCHAR_STRING)) {
            result = parseNationalCharStringType(tokens);
        } else if (isDatatype(tokens, DataTypes.DTYPE_CODE_BIT_STRING)) {
            result = parseBitStringType(tokens);
        } else if (isDatatype(tokens, DataTypes.DTYPE_CODE_EXACT_NUMERIC)) {
            result = parseExactNumericType(tokens);
        } else if (isDatatype(tokens, DataTypes.DTYPE_CODE_APROX_NUMERIC)) {
            result = parseApproxNumericType(tokens);
        } else if (isDatatype(tokens, DataTypes.DTYPE_CODE_DATE_TIME)) {
            result = parseDateTimeType(tokens);
        } else if (isDatatype(tokens, DataTypes.DTYPE_CODE_MISC)) {
            result = parseMiscellaneousType(tokens);
        } else {
            result = parseCustomType(tokens);
        }

        /*
         * (FROM http://www.postgresql.org/docs/8.4/static/arrays.html) 
        8.14.1. Declaration of Array Types

        To illustrate the use of array types, we create this table:

        CREATE TABLE sal_emp (
            name            text,
            pay_by_quarter  integer[],
            schedule        text[][]
        );

        As shown, an array data type is named by appending square brackets ([]) to the data type name of the array elements. 
        The above command will create a table named sal_emp with a column of type text (name), a one-dimensional array of type 
        integer (pay_by_quarter), which represents the employee's salary by quarter, and a two-dimensional array of text (schedule), 
        which represents the employee's weekly schedule.

        The syntax for CREATE TABLE allows the exact size of arrays to be specified, for example:

        CREATE TABLE tictactoe (
            squares   integer[3][3]
        );

        However, the current implementation ignores any supplied array size limits, i.e., the behavior is the same as for 
        arrays of unspecified length.

        The current implementation does not enforce the declared number of dimensions either. Arrays of a particular element 
        type are all considered to be of the same type, regardless of size or number of dimensions. So, declaring the array size 
        or number of dimensions in CREATE TABLE is simply documentation; it does not affect run-time behavior.

        An alternative syntax, which conforms to the SQL standard by using the keyword ARRAY, can be used for one-dimensional 
        arrays. pay_by_quarter could have been defined as:

            pay_by_quarter  integer ARRAY[4],

        Or, if no array size is to be specified:

            pay_by_quarter  integer ARRAY,
        */

        if (tokens.canConsume('[')) {
            if (!tokens.canConsume(']')) {
                // assume integer value
                tokens.consume();
                tokens.consume(']');
            }

            if (tokens.canConsume('[')) {
                if (!tokens.canConsume(']')) {
                    // assume integer value
                    tokens.consume();
                    tokens.consume(']');
                }
            }
        }

        return result;
    }

    /**
     * Parses SQL-92 Character string data types. <character string type> ::= CHARACTER [ <left paren> <length> <right paren> ] |
     * CHAR [ <left paren> <length> <right paren> ] | CHARACTER VARYING <left paren> <length> <right paren> | CHAR VARYING <left
     * paren> <length> <right paren> | VARCHAR <left paren> <length> <right paren>
     * 
     * @param tokens
     * @return the {@link DataType}
     * @throws ParsingException
     */
    protected DataType parseCharStringType( DdlTokenStream tokens ) throws ParsingException {
        DataType dataType = null;
        String typeName = null;

        if (tokens.matches(DataTypes.DTYPE_VARCHAR)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_VARCHAR);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_VARCHAR);
            long length = parseBracketedLong(tokens, dataType);
            dataType.setLength(length);
        } else if (tokens.matches(DataTypes.DTYPE_CHAR_VARYING)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_CHAR_VARYING);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_CHAR_VARYING);
            long length = parseBracketedLong(tokens, dataType);
            dataType.setLength(length);
        } else if (tokens.matches(DataTypes.DTYPE_CHARACTER_VARYING)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_CHARACTER_VARYING);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_CHARACTER_VARYING);
            long length = parseBracketedLong(tokens, dataType);
            dataType.setLength(length);
        } else if (tokens.matches(DataTypes.DTYPE_CHAR) || tokens.matches(DataTypes.DTYPE_CHARACTER)) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false); // "CHARACTER", "CHAR",
            dataType.setName(typeName);
            long length = getDefaultLength();
            if (tokens.matches(L_PAREN)) {
                length = parseBracketedLong(tokens, dataType);
            }
            dataType.setLength(length);
        }

        return dataType;
    }

    /**
     * Parses SQL-92 National Character string data types. <national character string type> ::= NATIONAL CHARACTER [ <left paren>
     * <length> <right paren> ] | NATIONAL CHAR [ <left paren> <length> <right paren> ] | NCHAR [ <left paren> <length> <right
     * paren> ] | NATIONAL CHARACTER VARYING <left paren> <length> <right paren> | NATIONAL CHAR VARYING <left paren> <length>
     * <right paren> | NCHAR VARYING <left paren> <length> <right paren>
     * 
     * @param tokens
     * @return the {@link DataType}
     * @throws ParsingException
     */
    protected DataType parseNationalCharStringType( DdlTokenStream tokens ) throws ParsingException {
        DataType dataType = null;
        String typeName = null;

        if (tokens.matches(DataTypes.DTYPE_NCHAR_VARYING)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_NCHAR_VARYING);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_NCHAR_VARYING);
            long length = parseBracketedLong(tokens, dataType);

            dataType.setLength(length);
        } else if (tokens.matches(DataTypes.DTYPE_NATIONAL_CHAR_VARYING)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_NATIONAL_CHAR_VARYING);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_NATIONAL_CHAR_VARYING);
            long length = parseBracketedLong(tokens, dataType);

            dataType.setLength(length);
        } else if (tokens.matches(DataTypes.DTYPE_NATIONAL_CHARACTER_VARYING)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_NATIONAL_CHARACTER_VARYING);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_NATIONAL_CHARACTER_VARYING);
            long length = parseBracketedLong(tokens, dataType);

            dataType.setLength(length);
        } else if (tokens.matches(DataTypes.DTYPE_NCHAR)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_NCHAR);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_NCHAR);
            long length = getDefaultLength();
            if (tokens.matches(L_PAREN)) {
                length = parseBracketedLong(tokens, dataType);
            }
            dataType.setLength(length);
        } else if (tokens.matches(DataTypes.DTYPE_NATIONAL_CHAR)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_NATIONAL_CHAR);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_NATIONAL_CHAR);
            long length = getDefaultLength();
            if (tokens.matches(L_PAREN)) {
                length = parseBracketedLong(tokens, dataType);
            }
            dataType.setLength(length);
        } else if (tokens.matches(DataTypes.DTYPE_NATIONAL_CHARACTER)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_NATIONAL_CHARACTER);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_NATIONAL_CHARACTER);
            long length = getDefaultLength();
            if (tokens.matches(L_PAREN)) {
                length = parseBracketedLong(tokens, dataType);
            }
            dataType.setLength(length);
        }

        return dataType;
    }

    /**
     * Parses SQL-92 Bit string data types. <bit string type> ::= BIT [ <left paren> <length> <right paren> ] | BIT VARYING <left
     * paren> <length> <right paren>
     * 
     * @param tokens
     * @return the {@link DataType}
     * @throws ParsingException
     */
    protected DataType parseBitStringType( DdlTokenStream tokens ) throws ParsingException {
        DataType dataType = null;
        String typeName = null;

        if (tokens.matches(DataTypes.DTYPE_BIT_VARYING)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_BIT_VARYING);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_BIT_VARYING);
            long length = parseBracketedLong(tokens, dataType);

            dataType.setLength(length);
        } else if (tokens.matches(DataTypes.DTYPE_BIT)) {
            typeName = getStatementTypeName(DataTypes.DTYPE_BIT);
            dataType = new DataType(typeName);
            consume(tokens, dataType, false, DataTypes.DTYPE_BIT);
            long length = getDefaultLength();
            if (tokens.matches(L_PAREN)) {
                length = parseBracketedLong(tokens, dataType);
            }
            dataType.setLength(length);
        }

        return dataType;
    }

    /**
     * Parses SQL-92 Exact numeric data types. <exact numeric type> ::= NUMERIC [ <left paren> <precision> [ <comma> <scale> ]
     * <right paren> ] | DECIMAL [ <left paren> <precision> [ <comma> <scale> ] <right paren> ] | DEC [ <left paren> <precision> [
     * <comma> <scale> ] <right paren> ] | INTEGER | INT | SMALLINT
     * 
     * @param tokens
     * @return the {@link DataType}
     * @throws ParsingException
     */
    protected DataType parseExactNumericType( DdlTokenStream tokens ) throws ParsingException {
        DataType dataType = null;
        String typeName = null;

        if (tokens.matchesAnyOf("INTEGER", "INT", "SMALLINT")) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false);
            dataType.setName(typeName);
        } else if (tokens.matchesAnyOf("NUMERIC", "DECIMAL", "DEC")) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false);
            dataType.setName(typeName);

            int precision = 0;
            int scale = 0;

            if (tokens.matches(L_PAREN)) {
                consume(tokens, dataType, false, L_PAREN);
                precision = (int)parseLong(tokens, dataType);
                if (canConsume(tokens, dataType, false, COMMA)) {
                    scale = (int)parseLong(tokens, dataType);
                } else {
                    scale = getDefaultScale();
                }
                consume(tokens, dataType, false, R_PAREN);
            } else {
                precision = getDefaultPrecision();
                scale = getDefaultScale();
            }
            dataType.setPrecision(precision);
            dataType.setScale(scale);
        }

        return dataType;
    }

    /**
     * Parses SQL-92 Approximate numeric data types. <approximate numeric type> ::= FLOAT [ <left paren> <precision> <right paren>
     * ] | REAL | DOUBLE PRECISION
     * 
     * @param tokens
     * @return the {@link DataType}
     * @throws ParsingException
     */
    protected DataType parseApproxNumericType( DdlTokenStream tokens ) throws ParsingException {
        DataType dataType = null;
        String typeName = null;

        if (tokens.matches(DataTypes.DTYPE_REAL)) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false, DataTypes.DTYPE_REAL);
            dataType.setName(typeName);
        } else if (tokens.matches(DataTypes.DTYPE_DOUBLE_PRECISION)) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false, DataTypes.DTYPE_DOUBLE_PRECISION);
            dataType.setName(typeName);
        } else if (tokens.matches(DataTypes.DTYPE_FLOAT)) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false, DataTypes.DTYPE_FLOAT);
            dataType.setName(typeName);
            int precision = 0;
            if (tokens.matches(L_PAREN)) {
                precision = (int)parseBracketedLong(tokens, dataType);
            }
            dataType.setPrecision(precision);
        }

        return dataType;
    }

    /**
     * Parses SQL-92 Date and Time data types. <datetime type> ::= DATE | TIME [ <left paren> <time precision> <right paren> ] [
     * WITH TIME ZONE ] | TIMESTAMP [ <left paren> <timestamp precision> <right paren> ] [ WITH TIME ZONE ]
     * 
     * @param tokens
     * @return the {@link DataType}
     * @throws ParsingException
     */
    protected DataType parseDateTimeType( DdlTokenStream tokens ) throws ParsingException {
        DataType dataType = null;
        String typeName = null;

        if (tokens.matches(DataTypes.DTYPE_DATE)) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false, DataTypes.DTYPE_DATE);
            dataType.setName(typeName);
        } else if (tokens.matches(DataTypes.DTYPE_TIME)) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false, DataTypes.DTYPE_TIME);
            dataType.setName(typeName);

            int precision = 0;
            if (tokens.matches(L_PAREN)) {
                precision = (int)parseBracketedLong(tokens, dataType);
            }
            dataType.setPrecision(precision);

            canConsume(tokens, dataType, true, "WITH", "TIME", "ZONE");
        } else if (tokens.matches(DataTypes.DTYPE_TIMESTAMP)) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false, DataTypes.DTYPE_TIMESTAMP);
            dataType.setName(typeName);

            int precision = 0;
            if (tokens.matches(L_PAREN)) {
                precision = (int)parseBracketedLong(tokens, dataType);
            }
            dataType.setPrecision(precision);

            canConsume(tokens, dataType, true, "WITH", "TIME", "ZONE");
        }

        return dataType;
    }

    /**
     * Parses SQL-92 Misc data types. <interval type> ::= INTERVAL <interval qualifier> <interval qualifier> ::= <start field> TO
     * <end field> | <single datetime field> <start field> ::= <non-second datetime field> [ <left paren> <interval leading field
     * precision> <right paren> ] <non-second datetime field> ::= YEAR | MONTH | DAY | HOUR | MINUTE <interval leading field
     * precision> ::= <unsigned integer> <end field> ::= <non-second datetime field> | SECOND [ <left paren> <interval fractional
     * seconds precision> <right paren> ] <interval fractional seconds precision> ::= <unsigned integer> <single datetime field>
     * ::= <non-second datetime field> [ <left paren> <interval leading field precision> <right paren> ] | SECOND [ <left paren>
     * <interval leading field precision> [ <comma> <interval fractional seconds precision> ] <right paren> ]
     * 
     * @param tokens
     * @return the {@link DataType}
     * @throws ParsingException
     */
    protected DataType parseMiscellaneousType( DdlTokenStream tokens ) throws ParsingException {
        DataType dataType = null;
        String typeName = null;

        if (tokens.matches(DataTypes.DTYPE_INTERVAL)) {
            dataType = new DataType();
            typeName = consume(tokens, dataType, false, DataTypes.DTYPE_INTERVAL);
            dataType.setName(typeName);
            // <non-second datetime field> TO <end field>
            // 
            // CASE 2a: { YEAR | MONTH | DAY | HOUR | MINUTE } [ [ <left paren> <interval leading field precision> <right paren> ]
            // CASE 2b: SECOND [ <left paren> <interval leading field precision> [ <comma> <interval fractional seconds precision>
            // ] <right paren> ]

            // CASE 1: { YEAR | MONTH | DAY | HOUR | MINUTE } TO { YEAR | MONTH | DAY | HOUR | MINUTE }
            if (tokens.matchesAnyOf("YEAR", "MONTH", "DAY", "HOUR", "MINUTE")) {
                // Consume first
                consume(tokens, dataType, true);

                if (canConsume(tokens, dataType, true, "TO")) {
                    // CASE 1:
                    // assume "YEAR | MONTH | DAY | HOUR | MINUTE" and consume
                    consume(tokens, dataType, true);
                } else if (tokens.matches(L_PAREN, DdlTokenStream.ANY_VALUE, R_PAREN)) {
                    // CASE 2a:
                    consume(tokens, dataType, true, L_PAREN);
                    consume(tokens, dataType, true);
                    consume(tokens, dataType, true, R_PAREN);
                } else {
                    System.out.println("  WARNING:  PROBLEM parsing INTERVAL data type. Check your DDL for incomplete statement.");
                }
            } else if (canConsume(tokens, dataType, true, "SECOND")) {
                // CASE 2b:
                if (canConsume(tokens, dataType, true, L_PAREN)) {

                    consume(tokens, dataType, true); // PRECISION
                    if (canConsume(tokens, dataType, true, COMMA)) {
                        consume(tokens, dataType, true); // fractional seconds precision
                    }
                    canConsume(tokens, dataType, true, R_PAREN);
                } else {
                    System.out.println("  WARNING:  PROBLEM parsing INTERVAL data type. Check your DDL for incomplete statement.");
                }
            } else {
                System.out.println("  WARNING:  PROBLEM parsing INTERVAL data type. Check your DDL for incomplete statement.");
            }
        }

        return dataType;
    }

    /**
     * General catch-all data type parsing method that sub-classes can override to parse database-specific data types.
     * 
     * @param tokens
     * @return the {@link DataType}
     * @throws ParsingException
     */
    protected DataType parseCustomType( DdlTokenStream tokens ) throws ParsingException {
        return null;
    }

    /**
     * @return integer default value for length
     */
    public int getDefaultLength() {
        return defaultLength;
    }

    /**
     * @param defaultLength
     */
    public void setDefaultLength( int defaultLength ) {
        this.defaultLength = defaultLength;
    }

    /**
     * @return integer default value for precision
     */
    public int getDefaultPrecision() {
        return defaultPrecision;
    }

    /**
     * @param defaultPrecision
     */
    public void setDefaultPrecision( int defaultPrecision ) {
        this.defaultPrecision = defaultPrecision;
    }

    /**
     * @return integer default value for scale
     */
    public int getDefaultScale() {
        return defaultScale;
    }

    /**
     * @param defaultScale
     */
    public void setDefaultScale( int defaultScale ) {
        this.defaultScale = defaultScale;
    }

    /**
     * Returns a long value from the input token stream assuming the long is not bracketed with parenthesis.
     * 
     * @param tokens
     * @param dataType
     * @return the long value
     */
    protected long parseLong( DdlTokenStream tokens,
                              DataType dataType ) {
        String value = consume(tokens, dataType, false);
        return parseLong(value);
    }

    /**
     * Returns a long value from the input token stream assuming the long is bracketed with parenthesis.
     * 
     * @param tokens
     * @param dataType
     * @return the long value
     */
    protected long parseBracketedLong( DdlTokenStream tokens,
                                       DataType dataType ) {
        consume(tokens, dataType, false, L_PAREN);
        String value = consume(tokens, dataType, false);
        consume(tokens, dataType, false, R_PAREN);
        return parseLong(value);
    }

    /**
     * Returns the integer value of the input string. Handles both straight integer string or complex KMG (CLOB or BLOB) value.
     * 
     * @param value
     * @return integer value
     * @throws NumberFormatException if a valid integer is not found
     */
    protected long parseLong( String value ) {
        long factor = 1;
        if (value.endsWith("K")) {
            factor = KILO;
        } else if (value.endsWith("M")) {
            factor = MEGA;
        } else if (value.endsWith("G")) {
            factor = GIGA;
        }
        if (factor > 1) {
            value = value.substring(0, value.length() - 1);
        }
        return new BigInteger(value).longValue() * factor;
    }

    /**
     * @param tokens
     * @param dataType
     * @param addSpacePrefix
     * @return consumed String value
     * @throws ParsingException
     */
    protected String consume( DdlTokenStream tokens,
                              DataType dataType,
                              boolean addSpacePrefix ) throws ParsingException {
        String value = tokens.consume();

        dataType.appendSource(addSpacePrefix, value);

        return value;
    }

    /**
     * @param tokens
     * @param dataType
     * @param addSpacePrefix
     * @param str
     * @return consumed string value
     * @throws ParsingException
     */
    protected String consume( DdlTokenStream tokens,
                              DataType dataType,
                              boolean addSpacePrefix,
                              String str ) throws ParsingException {
        tokens.consume(str);

        dataType.appendSource(addSpacePrefix, str);

        return str;
    }

    /**
     * @param tokens
     * @param dataType
     * @param addSpacePrefix
     * @param initialStr
     * @param additionalStrs
     * @return the consumed String
     * @throws ParsingException
     */
    protected String consume( DdlTokenStream tokens,
                              DataType dataType,
                              boolean addSpacePrefix,
                              String initialStr,
                              String... additionalStrs ) throws ParsingException {
        tokens.consume(initialStr, additionalStrs);
        StringBuffer value = new StringBuffer(initialStr);
        dataType.appendSource(addSpacePrefix, initialStr);

        for (String str : additionalStrs) {
            value.append(SPACE).append(str);
            dataType.appendSource(addSpacePrefix, str);
        }

        return value.toString();
    }

    protected String consume( DdlTokenStream tokens,
                              DataType dataType,
                              boolean addSpacePrefix,
                              String[] additionalStrs ) throws ParsingException {

        tokens.consume(additionalStrs);

        StringBuffer value = new StringBuffer(100);

        int i = 0;

        for (String str : additionalStrs) {
            if (i == 0) {
                value.append(str);
            } else {
                value.append(SPACE).append(str);
            }
            dataType.appendSource(addSpacePrefix, str);
            i++;
        }

        return value.toString();
    }

    /**
     * @param tokens
     * @param dataType
     * @param addSpacePrefix
     * @param initialStr
     * @param additionalStrs
     * @return did consume
     * @throws ParsingException
     */
    protected boolean canConsume( DdlTokenStream tokens,
                                  DataType dataType,
                                  boolean addSpacePrefix,
                                  String initialStr,
                                  String... additionalStrs ) throws ParsingException {
        if (tokens.canConsume(initialStr, additionalStrs)) {
            dataType.appendSource(addSpacePrefix, initialStr);

            for (String str : additionalStrs) {
                dataType.appendSource(addSpacePrefix, str);
            }
            return true;
        }

        return false;
    }

    /**
     * @param tokens
     * @param dataType
     * @param addSpacePrefix
     * @param additionalStrs
     * @return did consume
     * @throws ParsingException
     */
    protected boolean canConsume( DdlTokenStream tokens,
                                  DataType dataType,
                                  boolean addSpacePrefix,
                                  String[] additionalStrs ) throws ParsingException {
        if (tokens.canConsume(additionalStrs)) {

            for (String str : additionalStrs) {
                dataType.appendSource(addSpacePrefix, str);
            }
            return true;
        }

        return false;
    }

    /**
     * @param tokens
     * @param dataType
     * @param addSpacePrefix
     * @param type
     * @return consumed String value
     * @throws ParsingException
     */
    protected boolean canConsume( DdlTokenStream tokens,
                                  DataType dataType,
                                  boolean addSpacePrefix,
                                  int type ) throws ParsingException {
        if (tokens.matches(type)) {
            dataType.appendSource(addSpacePrefix, tokens.consume());
            return true;
        }

        return false;
    }

    /**
     * @param tokens
     * @param dataType
     * @param addSpacePrefix
     * @param initialStr
     * @param additionalStrs
     * @return did consume any
     * @throws ParsingException
     */
    protected boolean canConsumeAnyOf( DdlTokenStream tokens,
                                       DataType dataType,
                                       boolean addSpacePrefix,
                                       String initialStr,
                                       String... additionalStrs ) throws ParsingException {
        if (tokens.canConsume(initialStr)) {
            dataType.appendSource(addSpacePrefix, initialStr);
            return true;
        }
        for (String str : additionalStrs) {
            dataType.appendSource(addSpacePrefix, str);
            return true;
        }

        return false;
    }

    /**
     * @param stmtPhrase
     * @return concatenated name
     */
    public String getStatementTypeName( String[] stmtPhrase ) {
        StringBuffer sb = new StringBuffer(100);
        for (int i = 0; i < stmtPhrase.length; i++) {
            if (i == 0) {
                sb.append(stmtPhrase[0]);
            } else {
                sb.append(SPACE).append(stmtPhrase[i]);
            }
        }

        return sb.toString();
    }

    public void setPropertiesOnNode( AstNode columnNode,
                                     DataType datatype ) {
        columnNode.setProperty(DATATYPE_NAME, datatype.getName());
        if (datatype.getLength() >= 0) {
            columnNode.setProperty(DATATYPE_LENGTH, datatype.getLength());
        }
        if (datatype.getPrecision() >= 0) {
            columnNode.setProperty(DATATYPE_PRECISION, datatype.getPrecision());
        }
        if (datatype.getScale() >= 0) {
            columnNode.setProperty(DATATYPE_SCALE, datatype.getScale());
        }
    }
}
