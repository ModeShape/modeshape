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
package org.modeshape.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


/**
 * This is a test suite that operates against a complete JcrRepository instance created and managed using the JcrEngine.
 * Essentially this is an integration test, but it does test lower-level functionality of the implementation of the JCR interfaces
 * related to querying. (It is simply more difficult to unit test these implementations because of the difficulty in mocking the
 * many other components to replicate the same functionality.)
 * <p>
 * Also, because queries are read-only, the engine is set up once and used for the entire set of test methods.
 * </p>
 * <p>
 * The following are the SQL semantics that the tests will be covering:
 * <li>variations of simple SELECT * FROM</li>
 * <li>JOIN
 * </p>
 * <p>
 * To create the expected results to be used to run a test, use the test and print method: example:
 * DriverTestUtil.executeTestAndPrint(this.connection, "SELECT * FROM [nt:base]"); This will print the expected results like this:
 * String[] expected = { "jcr:primaryType[STRING]", "mode:root", "car:Car", "car:Car", "nt:unstructured" } Now copy the expected
 * results to the test method. Then change the test to run the executeTest method passing in the <code>expected</code> results:
 * example: DriverTestUtil.executeTest(this.connection, "SELECT * FROM [nt:base]", expected);
 * </p>
 */
public class JcrDriverHttpIntegrationTest extends ConnectionResultsComparator {

	
	public JcrDriverHttpIntegrationTest() {
		super();
	}

    private JcrDriver driver;
    private String serverName= "localhost:8090";
    private String repositoryName= "mode:repository";
    private String workspaceName= "default";
    private String url = JcrDriver.HTTP_URL_PREFIX + serverName + "/resources/" + repositoryName + "/" + workspaceName + "?username=dnauser&password=password";
    
    private Properties properties;

    private JcrConnection connection;
    private DatabaseMetaData dbmd;

    @Before
    public void beforeEach() throws Exception {

         properties = new Properties();
        
        driver = new JcrDriver();
        connection = (JcrConnection)driver.connect(url, properties);
        
       	dbmd = this.connection.getMetaData();
       	
       	// only test were comparing metadata is not available at this time
        this.compareColumns = true;

    }

    @After
    public void afterEach() throws Exception {
        DriverManager.deregisterDriver(driver);

        if (connection != null) {
            try {
                connection.close();
            } finally {
                connection = null;
            }
        }
        driver = null;
        dbmd = null;
    }

    
    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL2 Queries
    // ----------------------------------------------------------------------------------------------------------------
//
//    @Test
//    public void shouldBeAbleToExecuteSqlSelectAllNodes() throws SQLException {
//        String[] expected = {"jcr:primaryType[STRING]", "mode:root", "car:Car", "car:Car", "nt:unstructured", "nt:unstructured",
//            "car:Car", "nt:unstructured", "car:Car", "car:Car", "car:Car", "car:Car", "nt:unstructured", "car:Car",
//            "nt:unstructured", "car:Car", "car:Car", "car:Car", "car:Car", "nt:unstructured", "nt:unstructured",
//            "nt:unstructured", "nt:unstructured", "nt:unstructured"};
//
//        executeTest(this.connection, "SELECT * FROM [nt:base]", expected, 23);
//    }
//
//    @Test
//    public void shouldBeAbleToExecuteSqlSelectAllCars() throws SQLException {
//
//        String[] expected = {
//            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]",
//            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car",
//            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car",
//            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car",
//            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car",
//            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car",
//            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car",
//            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car",
//            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car",
//            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car",
//            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car",
//            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car",
//            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car",};
//
//        ConnectionResultsComparator.executeTest(this.connection, "SELECT * FROM [car:Car]", expected, 12);
//    }
//
//    @Test
//    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseUsingDefault() throws SQLException {
//        String[] expected = {
//            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]",
//            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car",
//            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car",
//            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car",
//            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car",
//            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car",
//            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car",
//            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car",
//            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car",
//            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car",
//            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car",
//            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car",
//            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car"};
//
//        ConnectionResultsComparator.executeTest(this.connection, "SELECT * FROM [car:Car] ORDER BY [car:maker]", expected, 12);
//
//    }
//
//    @Test
//    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseAsc() throws SQLException {
//        String[] expected = {"car:model[STRING]", "Altima", "Continental", "DB9", "DTS", "F-150", "G37", "H3", "Highlander",
//            "IS350", "LR2", "LR3", "Prius"};
//
//        ConnectionResultsComparator.executeTest(this.connection,
//                                   "SELECT car.[car:model] FROM [car:Car] As car WHERE car.[car:model] IS NOT NULL ORDER BY car.[car:model] ASC",
//                                   expected,
//                                   12);
//
//    }
//
//    @Test
//    public void shouldBeAbleToExecuteSqlQueryWithOrderedByClauseDesc() throws SQLException {
//        String[] expected = {
//            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]",
//            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car",
//            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car",
//            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car",
//            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car",
//            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car",
//            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car",
//            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car",
//            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car",
//            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car",
//            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car",
//            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car",
//            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car"};
//        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
//        ConnectionResultsComparator.executeTest(this.connection, "SELECT * FROM [car:Car] ORDER BY [car:msrp] DESC", expected, 12);
//
//    }
//
//    @Test
//    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllCarsUnderHybrid() throws SQLException {
//
//        String[] expected = {"car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]",
//            "Nissan    Altima    2008    $18,260", "Toyota    Prius    2008    $21,500",
//            "Toyota    Highlander    2008    $34,200"};
//
//        ConnectionResultsComparator.executeTest(this.connection,
//                                   "SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%'",
//                                   expected,
//                                   3);
//
//    }
//
//    /*
//     * FixFor( "MODE-722" )
//     */
//    @Test
//    public void shouldBeAbleToExecuteSqlQueryUsingJoinToFindAllCarsUnderHybrid() throws SQLException {
//        String[] expected = {"car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]",
//            "Nissan    Altima    2008    $18,260", "Toyota    Prius    2008    $21,500",
//            "Toyota    Highlander    2008    $34,200"};
//
//        ConnectionResultsComparator.executeTest(this.connection,
//                                   "SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car JOIN [nt:unstructured] AS hybrid ON ISCHILDNODE(car,hybrid) WHERE NAME(hybrid) = 'Hybrid'",
//                                   expected,
//                                   3);
//
//    }
//
//    @Test
//    public void shouldBeAbleToExecuteSqlQueryToFindAllUnstructuredNodes() throws SQLException {
//        String[] expected = {"jcr:primaryType[STRING]", "nt:unstructured", "nt:unstructured", "nt:unstructured",
//            "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured", "nt:unstructured",
//            "nt:unstructured", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car", "car:Car",
//            "car:Car", "car:Car", "car:Car"};
//
//        ConnectionResultsComparator.executeTest(this.connection, "SELECT * FROM [nt:unstructured]", expected, 22);
//
//    }
//
//    /**
//     * Tests that the child nodes (but no grandchild nodes) are returned.
//     * 
//     * @throws SQLException
//     */
//    @Test
//    public void shouldBeAbleToExecuteSqlQueryWithChildAxisCriteria() throws SQLException {
//        String[] expected = {"jcr:path[STRING]    jcr:score[DOUBLE]    jcr:primaryType[STRING]",
//            "/Cars/Utility    1.0    nt:unstructured", "/Cars/Hybrid    1.0    nt:unstructured",
//            "/Cars/Sports    1.0    nt:unstructured", "/Cars/Luxury    1.0    nt:unstructured"};
//        ConnectionResultsComparator.executeTest(this.connection,
//                                   "SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%' ",
//                                   expected,
//                                   4,
//                                   QueryLanguage.JCR_SQL);
//
//    }
//
//    /**
//     * Tests that the child nodes (but no grandchild nodes) are returned.
//     * 
//     * @throws SQLException
//     */
//    @Test
//    public void shouldBeAbleToExecuteSqlQueryWithContainsCriteria() throws SQLException {
//        String[] expected = {"jcr:path[STRING]    jcr:score[DOUBLE]    jcr:primaryType[STRING]",
//            "/Cars/Utility    1.0    nt:unstructured", "/Cars/Hybrid    1.0    nt:unstructured",
//            "/Cars/Sports    1.0    nt:unstructured", "/Cars/Luxury    1.0    nt:unstructured"};
//
//        ConnectionResultsComparator.executeTest(this.connection,
//                                   "SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%'",
//                                   expected,
//                                   4,
//                                   QueryLanguage.JCR_SQL);
//
//    }
    
    @Test
    public void shouldGetCatalogs() throws SQLException {
    	this.compareColumns = false;
    	String[] expected = {
    			"TABLE_CAT[String]",
    			"mode:repository"
    			};

    	ResultSet rs = dbmd.getCatalogs();
	    assertResultsSetEquals(rs, expected); 	    
	    assertRowCount(1);
    }
    
    @Test
    public void shouldGetSchemas() throws SQLException {
    	ResultSet rs = dbmd.getSchemas();
    	assertNotNull(rs);
    	assertEquals(rs.next(), Boolean.FALSE.booleanValue());

    }
    
    @Test
    public void shouldGetTableTypes() throws SQLException {
    	this.compareColumns = false;
    	String[] expected = {
    			"TABLE_TYPE[String]",
    			"VIEW"
    			};

    	ResultSet rs = dbmd.getTableTypes();
	    assertResultsSetEquals(rs, expected); 
	    assertRowCount(1);
    }
    
    @Ignore
    @Test
    public void shouldGetAllTables() throws SQLException {
    	this.compareColumns = false;

    	String[] expected = {
    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
    			"cars    NULL    car:Car    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
    			"cars    NULL    mix:created    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
    	};

    	ResultSet rs = dbmd.getTables("%", "%", "%", new String[] {});
    	
    	printResults(rs, false);
//	    assertResultsSetEquals(rs, expected); 
//	    assertRowCount(44);
    }
//    
//    @Test
//    public void shouldGetNTPrefixedTables() throws SQLException {
//    	this.compareColumns = false;
//
//    	String[] expected = {
//    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
//     			"cars    NULL    nt:activity    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:address    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:base    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:childNodeDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:configuration    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:file    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
//    			"cars    NULL    nt:folder    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:frozenNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:hierarchyNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:linkedFile    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
//    			"cars    NULL    nt:naturalText    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:nodeType    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:propertyDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:query    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
//    			"cars    NULL    nt:unstructured    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:version    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:versionHistory    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:versionLabels    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:versionedChild    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED"    			};
//
//
//    	ResultSet rs = dbmd.getTables("%", "%", "nt:%", new String[] {});
//	    assertResultsSetEquals(rs, expected); 
//	    assertRowCount(20);
//    }
//    
//    @Test
//    public void shouldGetResourceSuffixedTables() throws SQLException {
//    	this.compareColumns = false;
//
//    	String[] expected = {
//    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
//    			"cars    NULL    mode:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
//    			"cars    NULL    nt:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED"
//    			};
//
//    	ResultSet rs = dbmd.getTables("%", "%", "%:resource", new String[] {});
//	    assertResultsSetEquals(rs, expected); 
//	    assertRowCount(20);
//    }
//    
//    @Test
//    public void shouldGetTablesThatContainNodeTpe() throws SQLException {
//    	this.compareColumns = false;
//
//    	String[] expected = {
//    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
//    			"cars    NULL    mode:nodeTypes    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
//    			"cars    NULL    nt:nodeType    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED"
//    			};
//
//
//    	ResultSet rs = dbmd.getTables("%", "%", "%nodeType%", new String[] {});
//	    assertResultsSetEquals(rs, expected); 
//	    assertRowCount(2);
//    }
//    
    @Ignore
    @Test
    public void shouldGetAllColumnsFor1Table() throws SQLException {
    	this.compareColumns = false;
    	
    	String[] expected = {
    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
    			"cars    NULL    car:Car    *    12    undefined    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    *    12    undefined    50    NULL    0    0    2        NULL    0    0    0    2    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:engine    12    String    50    NULL    0    0    2        NULL    0    0    0    3    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:lengthInInches    6    Double    20    NULL    0    0    2        NULL    0    0    0    4    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:maker    12    String    50    NULL    0    0    2        NULL    0    0    0    5    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:model    12    String    50    NULL    0    0    2        NULL    0    0    0    6    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:mpgCity    -5    Long    20    NULL    0    0    2        NULL    0    0    0    7    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:mpgHighway    -5    Long    20    NULL    0    0    2        NULL    0    0    0    8    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:msrp    12    String    50    NULL    0    0    2        NULL    0    0    0    9    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:userRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    10    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:valueRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    11    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:wheelbaseInInches    6    Double    20    NULL    0    0    2        NULL    0    0    0    12    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    car:year    12    String    50    NULL    0    0    2        NULL    0    0    0    13    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    jcr:mixinTypes    12    Name    20    NULL    0    0    2        NULL    0    0    0    14    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    jcr:primaryType    12    Name    20    NULL    0    0    1        NULL    0    0    0    15    NO    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    modeint:multiValuedProperties    12    String    50    NULL    0    0    2        NULL    0    0    0    16    YES    NULL    NULL    NULL    0",
    			"cars    NULL    car:Car    modeint:nodeDefinition    12    String    50    NULL    0    0    2        NULL    0    0    0    17    YES    NULL    NULL    NULL    0"
    			};


    	ResultSet rs = dbmd.getColumns("%", "%", "%", "%");
    	printResults(rs, false);
//	    assertResultsSetEquals(rs, expected); 
//	    assertRowCount(17);
 
    }
//    
//    
//    @Test
//    public void shouldGetOnlyColumnsForCarPrefixedTables() throws SQLException {
//    	this.compareColumns = false;
//
//    	String[] expected = {
//    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
//    			"cars    NULL    car:Car    *    12    undefined    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    *    12    undefined    50    NULL    0    0    2        NULL    0    0    0    2    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:engine    12    String    50    NULL    0    0    2        NULL    0    0    0    3    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:lengthInInches    6    Double    20    NULL    0    0    2        NULL    0    0    0    4    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:maker    12    String    50    NULL    0    0    2        NULL    0    0    0    5    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:model    12    String    50    NULL    0    0    2        NULL    0    0    0    6    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:mpgCity    -5    Long    20    NULL    0    0    2        NULL    0    0    0    7    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:mpgHighway    -5    Long    20    NULL    0    0    2        NULL    0    0    0    8    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:msrp    12    String    50    NULL    0    0    2        NULL    0    0    0    9    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:userRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    10    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:valueRating    -5    Long    20    NULL    0    0    2        NULL    0    0    0    11    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:wheelbaseInInches    6    Double    20    NULL    0    0    2        NULL    0    0    0    12    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    car:year    12    String    50    NULL    0    0    2        NULL    0    0    0    13    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    jcr:mixinTypes    12    Name    20    NULL    0    0    2        NULL    0    0    0    14    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    jcr:primaryType    12    Name    20    NULL    0    0    1        NULL    0    0    0    15    NO    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    modeint:multiValuedProperties    12    String    50    NULL    0    0    2        NULL    0    0    0    16    YES    NULL    NULL    NULL    0",
//    			"cars    NULL    car:Car    modeint:nodeDefinition    12    String    50    NULL    0    0    2        NULL    0    0    0    17    YES    NULL    NULL    NULL    0"
//    			};   	
//
//    	ResultSet rs = dbmd.getColumns("%", "%", "car%", "%");  
//	    assertResultsSetEquals(rs, expected); 
//	    assertRowCount(11);
// 
//    }
//    
//    
//    
//    @Test
//    public void shouldGetOnlyMSRPColumnForCarTable() throws SQLException {
//    	this.compareColumns = false;
//    	
//    	String[] expected = {
//    			"TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
//    			"cars    NULL    car:Car    car:msrp    12    String    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0"
//    			};
//
//
//    	ResultSet rs = dbmd.getColumns("%", "%", "car:Car", "car:msrp");
//	    assertResultsSetEquals(rs, expected); 
//	    assertRowCount(1);
// 
//    }
 
}
