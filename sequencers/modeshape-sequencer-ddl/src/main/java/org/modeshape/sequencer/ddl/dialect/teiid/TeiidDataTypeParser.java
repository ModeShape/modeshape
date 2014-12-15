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
package org.modeshape.sequencer.ddl.dialect.teiid;

import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.datatype.DataType;
import org.modeshape.sequencer.ddl.datatype.DataTypeParser;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidDataType;

class TeiidDataTypeParser extends DataTypeParser {

    static final int[] DEFAULT_PRECISION_SCALE = new int[] {DataType.DEFAULT_PRECISION, DataType.DEFAULT_SCALE};

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#isCustomDataType(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    protected boolean isCustomDataType( final DdlTokenStream tokens ) throws ParsingException {
        throw new TeiidDdlParsingException(tokens, "isCustomDataType(DdlTokenStream) should not be called");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.ddl.datatype.DataTypeParser#parse(org.modeshape.sequencer.ddl.DdlTokenStream)
     */
    @Override
    public DataType parse( final DdlTokenStream tokens ) throws ParsingException {
        TeiidDataType teiidType = null;
        long length = DataType.DEFAULT_LENGTH;
        int[] precisionScale = DEFAULT_PRECISION_SCALE;
        int arrayDimensions = DataType.DEFAULT_ARRAY_DIMENSIONS;

        // flags for dealing with the SERIAL pseudo data type
        boolean autoIncrement = false;
        boolean notNull = false;

        for (final TeiidDataType teiidDataType : TeiidDataType.values()) {
            if (tokens.canConsume(teiidDataType.toDdl())) {
                teiidType = teiidDataType;

                if (teiidDataType == TeiidDataType.BIGDECIMAL) {
                    // ( BIGDECIMAL ( <lparen> <unsigned integer> ( <comma> <unsigned integer> )? <rparen> )? )
                    precisionScale = parseDecimal(tokens);
                } else if (teiidDataType == TeiidDataType.BIGINTEGER) {
                    // ( BIGINTEGER ( <lparen> <unsigned integer> <rparen> )? )
                    length = parseLength(tokens);
                } else if (teiidDataType == TeiidDataType.BLOB) {
                    // ( BLOB ( <lparen> <unsigned integer> <rparen> )? )
                    length = parseLength(tokens);
                } else if (teiidDataType == TeiidDataType.CHAR) {
                    // ( CHAR ( <lparen> <unsigned integer> <rparen> )? )
                    length = parseLength(tokens);
                } else if (teiidDataType == TeiidDataType.CLOB) {
                    // ( CLOB ( <lparen> <unsigned integer> <rparen> )? )
                    length = parseLength(tokens);
                } else if (teiidDataType == TeiidDataType.DECIMAL) {
                    // ( DECIMAL ( <lparen> <unsigned integer> ( <comma> <unsigned integer> )? <rparen> )? )
                    precisionScale = parseDecimal(tokens);
                } else if (teiidDataType == TeiidDataType.STRING) {
                    // ( STRING ( <lparen> <unsigned integer> <rparen> )? )
                    length = parseLength(tokens);
                } else if (teiidDataType == TeiidDataType.VARBINARY) {
                    // ( VARBINARY ( <lparen> <unsigned integer> <rparen> )? )
                    length = parseLength(tokens);
                } else if (teiidDataType == TeiidDataType.VARCHAR) {
                    // ( VARCHAR ( <lparen> <unsigned integer> <rparen> )? )
                    length = parseLength(tokens);
                } else if (teiidDataType == TeiidDataType.OBJECT) {
                    // ( OBJECT ( <lparen> <unsigned integer> <rparen> )? )
                    length = parseLength(tokens);
                }

                break;
            }
        }

        if (teiidType == null && tokens.canConsume(TeiidDdlConstants.TeiidNonReservedWord.SERIAL.toDdl())) {
            // SERIAL is an alias for an auto-incremented not-null integer
            teiidType = TeiidDataType.INTEGER;
            autoIncrement = true;
            notNull = true;
        }

        if (teiidType != null) {
            final DataType type = new DataType(teiidType.toDdl());

            // set auto increment
            type.setAutoIncrement(autoIncrement);

            // set not null
            type.setNotNull(notNull);

            // set length
            if (length != DataType.DEFAULT_LENGTH) {
                type.setLength(length);
            }

            // set precision and scale
            assert ((precisionScale != null) && (precisionScale.length > 0));

            if (precisionScale != DEFAULT_PRECISION_SCALE) {
                if (precisionScale[0] != DataType.DEFAULT_PRECISION) {
                    type.setPrecision(precisionScale[0]);
                }

                if ((precisionScale.length == 2) && (precisionScale[1] != DataType.DEFAULT_SCALE)) {
                    type.setScale(precisionScale[1]);
                }
            }
            // Array dimensions of the data type
            while (tokens.canConsume(LS_BRACE)) {
                tokens.consume(RS_BRACE);
                arrayDimensions++;
            }
            type.setArrayDimensions(arrayDimensions);

            return type;
        }

        throw new TeiidDdlParsingException(tokens, "Unparsable data type");
    }

    /**
     * <code>
     * ( BIGDECIMAL ( <lparen> <unsigned integer> ( <comma> <unsigned integer> )? <rparen> )? )
     * </code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @return a long array of length one containing the precision, a long array of length two containing precision and scale, or
     *         <code>null</code> if precision and scale are not found
     * @throws NumberFormatException if the precision or scale is not an int
     */
    private int[] parseDecimal( final DdlTokenStream tokens ) {
        if (tokens.canConsume(L_PAREN)) {
            int precision = Integer.parseInt(tokens.consume());
            int scale = DataType.DEFAULT_SCALE;

            if (tokens.canConsume(COMMA)) {
                scale = Integer.parseInt(tokens.consume());
            }

            tokens.consume(R_PAREN); // right paren

            if (scale == DataType.DEFAULT_SCALE) {
                return new int[] {precision};
            }

            return new int[] {precision, scale};
        }

        return DEFAULT_PRECISION_SCALE;
    }

    /**
     * <code>
     * ( <lparen> <unsigned integer> <rparen> )?
     * </code>
     * 
     * @param tokens the tokens being processed (cannot be <code>null</code>)
     * @return the length or the default length if length is not found
     * @see DataType#DEFAULT_LENGTH
     */
    private long parseLength( final DdlTokenStream tokens ) {
        if (tokens.canConsume(L_PAREN)) {
            final String value = tokens.consume();
            tokens.consume(R_PAREN);
            return parseLong(value);
        }

        return DataType.DEFAULT_LENGTH;
    }

}
