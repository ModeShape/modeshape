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

        if (teiidType != null) {
            final DataType type = new DataType(teiidType.toDdl());

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
