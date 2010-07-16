package org.modeshape.sequencer.ddl.datatype;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.DdlTokenStream;

public class DataTypeParserTest implements DdlConstants {
    private DataTypeParser parser;

    private boolean printTest = false;

    @Before
    public void beforeEach() {
        parser = new DataTypeParser();
    }

    private DdlTokenStream getTokens( String content ) {
        DdlTokenStream tokens = new DdlTokenStream(content, DdlTokenStream.ddlTokenizer(false), false);

        tokens.start();

        return tokens;
    }

    private String getDataTypeString( String[] input ) {
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < input.length; i++) {
            if (i > 0) {
                sb.append(SPACE);
            }
            sb.append(input[i]);
        }

        return sb.toString();
    }

    private void printTest( String value ) {
        if (printTest) {
            System.out.println("TEST:  " + value);
        }
    }

    // private void printResult(String value) {
    // if( printTest ) {
    // System.out.println(value);
    // }
    // }

    /* ===========================================================================================================================
     * UTILITY METHODS
     * ==========================================================================================================================
     */

    @Test
    public void shouldParseBracketedInteger() {
        printTest("shouldParseBracketedInteger()");
        String content = "(255)";
        DdlTokenStream tokens = getTokens(content);
        long value = parser.parseBracketedLong(tokens, new DataType());

        Assert.assertEquals("DataType length is not correct", 255, value);
    }

    @Test
    public void shouldParseKMGInteger() {
        printTest("shouldParseKMGInteger()");
        String content = "1000M";
        DdlTokenStream tokens = getTokens(content);

        long value = parser.parseLong(tokens, new DataType());
        Assert.assertEquals("DataType length is not correct", 1000 * MEGA, value);

        content = "1000G";
        tokens = getTokens(content);
        value = parser.parseLong(tokens, new DataType());

        Assert.assertEquals("DataType length is not correct", 1000 * GIGA, value);

        content = "1000K";
        tokens = getTokens(content);
        value = parser.parseLong(tokens, new DataType());

        Assert.assertEquals("DataType length is not correct", 1000 * KILO, value);

        content = "(1000M)";
        tokens = getTokens(content);
        value = parser.parseBracketedLong(tokens, new DataType());

        Assert.assertEquals("DataType length is not correct", 1000 * MEGA, value);

        content = "(1000G)";
        tokens = getTokens(content);
        value = parser.parseBracketedLong(tokens, new DataType());

        Assert.assertEquals("DataType length is not correct", 1000 * GIGA, value);

        content = "(1000K)";
        tokens = getTokens(content);
        value = parser.parseBracketedLong(tokens, new DataType());

        Assert.assertEquals("DataType length is not correct", 1000 * KILO, value);
    }

    /* ===========================================================================================================================
     * CHARACTER STRING TYPE
     * ==========================================================================================================================
        <character string type> ::=
            CHARACTER [ <left paren> <length> <right paren> ]
          | CHAR [ <left paren> <length> <right paren> ]
          | CHARACTER VARYING <left paren> <length> <right paren>
          | CHAR VARYING <left paren> <length> <right paren>
          | VARCHAR <left paren> <length> <right paren>
    */

    @Test
    public void shouldParseCHAR() {
        printTest("shouldParseCHAR()");
        String typeString = getDataTypeString(DataTypes.DTYPE_CHAR);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (255)";
        tokens = getTokens(content);

        dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());
    }

    @Test
    public void shouldParseCHARACTER() {
        printTest("shouldParseCHARACTER()");
        String typeString = getDataTypeString(DataTypes.DTYPE_CHARACTER);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (255)";
        tokens = getTokens(content);

        dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());
    }

    @Test
    public void shouldParseCHAR_VARYING() {
        printTest("shouldParseCHAR_VARYING()");
        String typeString = getDataTypeString(DataTypes.DTYPE_CHAR_VARYING);
        String content = typeString + " (255)";

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());
    }

    @Test
    public void shouldParseCHARACTER_VARYING() {
        printTest("shouldParseCHARACTER_VARYING()");
        String typeString = getDataTypeString(DataTypes.DTYPE_CHARACTER_VARYING);
        String content = typeString + " (255)";

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());
    }

    @Test
    public void shouldParseVARCHAR() {
        printTest("shouldParseVARCHAR()");
        String typeString = getDataTypeString(DataTypes.DTYPE_VARCHAR);
        String content = typeString + " (255)";

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());

        content = typeString;
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }

        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);
    }

    /* ===========================================================================================================================
     * NATIONAL CHARACTER STRING TYPE
     * ==========================================================================================================================
     * 
     * 
    	<national character string type> ::=
    	      NATIONAL CHARACTER [ <left paren> <length> <right paren> ]
    	    | NATIONAL CHAR [ <left paren> <length> <right paren> ]
    	    | NCHAR [ <left paren> <length> <right paren> ]
    	    | NATIONAL CHARACTER VARYING <left paren> <length> <right paren>
    	    | NATIONAL CHAR VARYING <left paren> <length> <right paren>
    	    | NCHAR VARYING <left paren> <length> <right paren>

     * 
     * 
     */
    @Test
    public void shouldParseNATIONAL_CHAR() {
        printTest("shouldParseNATIONAL_CHAR()");
        String typeString = getDataTypeString(DataTypes.DTYPE_NATIONAL_CHAR);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (255)";
        tokens = getTokens(content);

        dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());
    }

    @Test
    public void shouldParseNATIONAL_CHARACTER() {
        printTest("shouldParseNATIONAL_CHARACTER()");
        String typeString = getDataTypeString(DataTypes.DTYPE_NATIONAL_CHARACTER);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (255)";
        tokens = getTokens(content);

        dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());
    }

    @Test
    public void shouldParseNATIONAL_CHAR_VARYING() {
        printTest("shouldParseNATIONAL_CHAR_VARYING()");
        String typeString = getDataTypeString(DataTypes.DTYPE_NATIONAL_CHAR_VARYING);
        String content = typeString + " (255)";

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());
    }

    @Test
    public void shouldParseNATIONAL_CHARACTER_VARYING() {
        printTest("shouldParseNATIONAL_CHARACTER_VARYING()");
        String typeString = getDataTypeString(DataTypes.DTYPE_NATIONAL_CHARACTER_VARYING);
        String content = typeString + " (255)";

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());
    }

    @Test
    public void shouldParseNCHAR_VARYING() {
        printTest("shouldParseNCHAR_VARYING()");
        String typeString = getDataTypeString(DataTypes.DTYPE_NCHAR_VARYING);
        String content = typeString + " (255)";

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());

        content = typeString;
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }

        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);
    }

    /* ===========================================================================================================================
     * NATIONAL CHARACTER STRING TYPE
     * ==========================================================================================================================
     * 
    	<bit string type> ::=
    	      BIT [ <left paren> <length> <right paren> ]
    	    | BIT VARYING <left paren> <length> <right paren>
     *
     */

    @Test
    public void shouldParseBIT() {
        printTest("shouldParseBIT()");
        String typeString = getDataTypeString(DataTypes.DTYPE_BIT);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
    }

    @Test
    public void shouldParseBITWithLength() {
        printTest("shouldParseBITWithLength()");
        String typeString = getDataTypeString(DataTypes.DTYPE_BIT);
        String content = typeString + " (255)";

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());
    }

    @Test
    public void shouldParseBIT_VARYINGWithLength() {
        printTest("shouldParseBIT_VARYINGWithLength()");
        String typeString = getDataTypeString(DataTypes.DTYPE_BIT_VARYING);
        String content = typeString + " (255)";

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 255, dType.getLength());

        content = typeString;
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }

        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);
    }

    /* ===========================================================================================================================
     * EXACT NUMERIC TYPE
     * ==========================================================================================================================
    	<exact numeric type> ::=
    	      NUMERIC [ <left paren> <precision> [ <comma> <scale> ] <right paren> ]
    	    | DECIMAL [ <left paren> <precision> [ <comma> <scale> ] <right paren> ]
    	    | DEC [ <left paren> <precision> [ <comma> <scale> ] <right paren> ]
    	    | INTEGER
    	    | INT
    	    | SMALLINT
     * 
     */

    @Test
    public void shouldNotParseXXXXXXTYPE() {
        printTest("shouldNotParseXXXXXXTYPE()");
        String typeString = "XXXXXXTYPE";
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }

        Assert.assertNull("DataType should NOT have been found for Type = " + typeString, dType);
    }

    @Test
    public void shouldParseINT() {
        printTest("shouldParseINT()");
        String typeString = getDataTypeString(DataTypes.DTYPE_INT);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
    }

    @Test
    public void shouldParseINTEGER() {
        printTest("shouldParseINTEGER()");
        String typeString = getDataTypeString(DataTypes.DTYPE_INTEGER);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
    }

    @Test
    public void shouldParseSMALLINT() {
        printTest("shouldParseSMALLINT()");
        String typeString = getDataTypeString(DataTypes.DTYPE_SMALLINT);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
    }

    @Test
    public void shouldParseNUMERIC() {
        printTest("shouldParseNUMERIC()");
        String typeString = getDataTypeString(DataTypes.DTYPE_NUMERIC);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (5)";
        tokens = getTokens(content);
        dType = parser.parse(tokens);
        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 5, dType.getPrecision()); // PRECISION

        content = typeString + " (5, 2)";
        tokens = getTokens(content);
        dType = parser.parse(tokens);
        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 5, dType.getPrecision()); // PRECISION
        Assert.assertEquals("DataType length is not correct", 2, dType.getScale()); // SCALE

        // MISSING COMMA
        content = typeString + " (5  2)";
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }
        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);

        // INVALID Scale Integer
        content = typeString + " (5  A)";
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }
        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);
    }

    @Test
    public void shouldParseDECIMAL() {
        printTest("shouldParseDECIMAL()");
        String typeString = getDataTypeString(DataTypes.DTYPE_DECIMAL);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (5)";
        tokens = getTokens(content);
        dType = parser.parse(tokens);
        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 5, dType.getPrecision()); // PRECISION

        content = typeString + " (5, 2)";
        tokens = getTokens(content);
        dType = parser.parse(tokens);
        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 5, dType.getPrecision()); // PRECISION
        Assert.assertEquals("DataType length is not correct", 2, dType.getScale()); // SCALE

        // MISSING COMMA
        content = typeString + " (5  2)";
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }
        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);

        // INVALID Scale Integer
        content = typeString + " (5  A)";
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }
        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);
    }

    @Test
    public void shouldParseDEC() {
        printTest("shouldParseDEC()");
        String typeString = getDataTypeString(DataTypes.DTYPE_DEC);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (5)";
        tokens = getTokens(content);
        dType = parser.parse(tokens);
        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 5, dType.getPrecision()); // PRECISION

        content = typeString + " (5, 2)";
        tokens = getTokens(content);
        dType = parser.parse(tokens);
        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 5, dType.getPrecision()); // PRECISION
        Assert.assertEquals("DataType length is not correct", 2, dType.getScale()); // SCALE

        // MISSING COMMA
        content = typeString + " (5  2)";
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }
        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);

        // INVALID Scale Integer
        content = typeString + " (5  A)";
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }
        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);
    }

    /* ===========================================================================================================================
     * APPROXIMATE NUMERIC TYPE
     * ==========================================================================================================================
     * 
    	<approximate numeric type> ::=
    	      FLOAT [ <left paren> <precision> <right paren> ]
    	    | REAL
    	    | DOUBLE PRECISION
     */

    @Test
    public void shouldParseFLOAT() {
        printTest("shouldParseFLOAT()");
        String typeString = getDataTypeString(DataTypes.DTYPE_FLOAT);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (5)";
        tokens = getTokens(content);
        dType = parser.parse(tokens);
        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 5, dType.getPrecision()); // PRECISION

        // ADDED SCALE
        content = typeString + " (5,  2)";
        tokens = getTokens(content);
        dType = null;
        try {
            dType = parser.parse(tokens);
        } catch (ParsingException e) {
            // Expect exception
        }
        Assert.assertNull("DataType should NOT have been found for Type = " + content, dType);

    }

    @Test
    public void shouldParseREAL() {
        printTest("shouldParseREAL()");
        String typeString = getDataTypeString(DataTypes.DTYPE_REAL);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
    }

    @Test
    public void shouldParseDOUBLE_PRECISION() {
        printTest("shouldParseDOUBLE_PRECISION()");
        String typeString = getDataTypeString(DataTypes.DTYPE_DOUBLE_PRECISION);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
    }

    /* ===========================================================================================================================
     * APPROXIMATE NUMERIC TYPE
     * ==========================================================================================================================
     * 
    	<datetime type> ::=
    	      DATE
    	    | TIME [ <left paren> <time precision> <right paren> ] [ WITH TIME ZONE ]
    	    | TIMESTAMP [ <left paren> <timestamp precision> <right paren> ] [ WITH TIME ZONE ]
    	    
    	    NOTE:  time precision & timestamp precision is an integer from 0 to 9
     */

    @Test
    public void shouldParseDATE() {
        printTest("shouldParseDATE()");
        String typeString = getDataTypeString(DataTypes.DTYPE_DATE);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
    }

    @Test
    public void shouldParseTIME() {
        printTest("shouldParseTIME()");
        String typeString = getDataTypeString(DataTypes.DTYPE_TIME);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (6)";
        tokens = getTokens(content);
        dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 6, dType.getPrecision());
    }

    @Test
    public void shouldParseTIMESTAMP() {
        printTest("shouldParseTIMESTAMP()");
        String typeString = getDataTypeString(DataTypes.DTYPE_TIMESTAMP);
        String content = typeString;

        DdlTokenStream tokens = getTokens(content);

        DataType dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());

        content = typeString + " (6)";
        tokens = getTokens(content);
        dType = parser.parse(tokens);

        Assert.assertNotNull("DataType was NOT found for Type = " + typeString, dType);
        Assert.assertEquals("Wrong DataType found", typeString, dType.getName());
        Assert.assertEquals("DataType length is not correct", 6, dType.getPrecision());
    }
}
