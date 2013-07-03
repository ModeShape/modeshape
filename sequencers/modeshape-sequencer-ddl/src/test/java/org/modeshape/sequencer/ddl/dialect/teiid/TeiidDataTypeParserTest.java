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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.DdlTokenStream;
import org.modeshape.sequencer.ddl.datatype.DataType;
import org.modeshape.sequencer.ddl.dialect.teiid.TeiidDdlConstants.TeiidDataType;

/**
 * A test class for {@link TeiidDataTypeParser}.
 */
public class TeiidDataTypeParserTest {

    private TeiidDataTypeParser parser;

    @Before
    public void beforeEach() {
        this.parser = new TeiidDataTypeParser();
    }

    @Test
    public void shouldParseBigDecimal() {
        assertNameAndDefaults(TeiidDataType.BIGDECIMAL);
    }

    @Test
    public void shouldParseBigDecimalWithPrecision() {
        assertPrecisionAndScale(TeiidDataType.BIGDECIMAL, new int[] {9});
    }

    @Test
    public void shouldParseBigDecimalWithPrecisionAndScale() {
        assertPrecisionAndScale(TeiidDataType.BIGDECIMAL, new int[] {9, 3});
    }

    @Test
    public void shouldParseBigInteger() {
        assertNameAndDefaults(TeiidDataType.BIGINTEGER);
    }

    @Test
    public void shouldParseBigIntegerWithLength() {
        assertLength(TeiidDataType.BIGINTEGER, 50);
    }

    @Test
    public void shouldParseBigInt() {
        assertNameAndDefaults(TeiidDataType.BIGINT);
    }

    @Test
    public void shouldParseBlob() {
        assertNameAndDefaults(TeiidDataType.BLOB);
    }

    @Test
    public void shouldParseBlobWithLength() {
        assertLength(TeiidDataType.BLOB, 50);
    }

    @Test
    public void shouldParseBoolean() {
        assertNameAndDefaults(TeiidDataType.BOOLEAN);
    }

    @Test
    public void shouldParseByte() {
        assertNameAndDefaults(TeiidDataType.BYTE);
    }

    @Test
    public void shouldParseChar() {
        assertNameAndDefaults(TeiidDataType.CHAR);
    }

    @Test
    public void shouldParseCharWithLength() {
        assertLength(TeiidDataType.CHAR, 50);
    }

    @Test
    public void shouldParseClob() {
        assertNameAndDefaults(TeiidDataType.CLOB);
    }

    @Test
    public void shouldParseClobWithLength() {
        assertLength(TeiidDataType.CLOB, 50);
    }

    @Test
    public void shouldParseDate() {
        assertNameAndDefaults(TeiidDataType.DATE);
    }

    @Test
    public void shouldParseDecimal() {
        assertNameAndDefaults(TeiidDataType.DECIMAL);
    }

    @Test
    public void shouldParseDecimalWithPrecision() {
        assertPrecisionAndScale(TeiidDataType.DECIMAL, new int[] {9});
    }

    @Test
    public void shouldParseDecimalWithPrecisionAndScale() {
        assertPrecisionAndScale(TeiidDataType.DECIMAL, new int[] {9, 3});
    }

    @Test
    public void shouldParseDouble() {
        assertNameAndDefaults(TeiidDataType.DOUBLE);
    }

    @Test
    public void shouldParseFloat() {
        assertNameAndDefaults(TeiidDataType.FLOAT);
    }

    @Test
    public void shouldParseInteger() {
        assertNameAndDefaults(TeiidDataType.INTEGER);
    }

    @Test
    public void shouldParseLong() {
        assertNameAndDefaults(TeiidDataType.LONG);
    }

    @Test
    public void shouldParseObject() {
        assertNameAndDefaults(TeiidDataType.OBJECT);
    }

    @Test
    public void shouldParseObjectWithLength() {
        assertLength(TeiidDataType.OBJECT, 49);
    }

    @Test
    public void shouldParseReal() {
        assertNameAndDefaults(TeiidDataType.REAL);
    }

    @Test
    public void shouldParseShort() {
        assertNameAndDefaults(TeiidDataType.SHORT);
    }

    @Test
    public void shouldParseSmallInt() {
        assertNameAndDefaults(TeiidDataType.SMALLINT);
    }

    @Test
    public void shouldParseString() {
        assertNameAndDefaults(TeiidDataType.STRING);
    }

    @Test
    public void shouldParseStringWithLength() {
        assertLength(TeiidDataType.STRING, 50);
    }

    @Test
    public void shouldParseTime() {
        assertNameAndDefaults(TeiidDataType.TIME);
    }

    @Test
    public void shouldParseTimestamp() {
        assertNameAndDefaults(TeiidDataType.TIMESTAMP);
    }

    @Test
    public void shouldParseTinyInt() {
        assertNameAndDefaults(TeiidDataType.TINYINT);
    }

    @Test
    public void shouldParseVarBinary() {
        assertNameAndDefaults(TeiidDataType.VARBINARY);
    }

    @Test
    public void shouldParseVarBinaryWithLength() {
        assertLength(TeiidDataType.VARBINARY, 50);
    }

    @Test
    public void shouldParseVarChar() {
        assertNameAndDefaults(TeiidDataType.VARCHAR);
    }

    @Test
    public void shouldParseVarCharWithLength() {
        assertLength(TeiidDataType.VARCHAR, 50);
    }

    @Test
    public void shouldParseXml() {
        assertNameAndDefaults(TeiidDataType.XML);
    }

    @Test( expected = ParsingException.class )
    public void shouldNotParseInvalidDataType() {
        final DdlTokenStream tokens = getTokens("invalidDataType");
        this.parser.parse(tokens);
    }

    @Test( expected = ParsingException.class )
    public void shouldNotParseEmptDataType() {
        final DdlTokenStream tokens = getTokens("");
        this.parser.parse(tokens);
    }

    // ********* helper methods ***********

    private void assertNameAndDefaults( final TeiidDataType dataType ) {
        final String name = dataType.toDdl();
        final DdlTokenStream tokens = getTokens(name);
        final DataType actual = this.parser.parse(tokens);

        assertThat(actual.getName(), is(name));
        assertThat(actual.getLength(), is(DataType.DEFAULT_LENGTH));
        assertThat(actual.getPrecision(), is(DataType.DEFAULT_PRECISION));
        assertThat(actual.getScale(), is(DataType.DEFAULT_SCALE));
    }

    private void assertLength( final TeiidDataType dataType,
                               final long length ) {
        final String content = dataType.toDdl() + '(' + length + ')';
        final DdlTokenStream tokens = getTokens(content);
        final DataType actual = this.parser.parse(tokens);
        assertThat(actual.getLength(), is(length));
    }

    /**
     * @param dataType the data type being checked (cannot be <code>null</code>)
     * @param precisionScale the precision and an optional scale second element or <code>null</code> if defaults are being checked
     */
    private void assertPrecisionAndScale( final TeiidDataType dataType,
                                          final int[] precisionScale ) {
        final StringBuilder content = new StringBuilder(dataType.toDdl());
        int precision = DataType.DEFAULT_PRECISION;
        int scale = DataType.DEFAULT_SCALE;

        if ((precisionScale != null) && (precisionScale != TeiidDataTypeParser.DEFAULT_PRECISION_SCALE)) {
            precision = precisionScale[0];
            content.append('(').append(precisionScale[0]);

            if ((precisionScale.length == 2) && (precisionScale[1] != DataType.DEFAULT_SCALE)) {
                scale = precisionScale[1];
                content.append(',').append(precisionScale[1]);
            }

            content.append(')');
        }

        final DdlTokenStream tokens = getTokens(content.toString());
        final DataType actual = this.parser.parse(tokens);
        assertThat(actual.getPrecision(), is(precision));
        assertThat(actual.getScale(), is(scale));
    }

    private DdlTokenStream getTokens( final String content ) {
        final DdlTokenStream tokens = new DdlTokenStream(content, DdlTokenStream.ddlTokenizer(false), false);
        tokens.start();
        return tokens;
    }

}
