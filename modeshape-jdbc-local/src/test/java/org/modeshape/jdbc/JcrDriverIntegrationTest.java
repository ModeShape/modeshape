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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import javax.naming.Context;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrRepository.QueryLanguage;

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
public class JcrDriverIntegrationTest extends AbstractJdbcDriverTest {

    @Override
    protected Driver createDriver( JcrRepository repository ) throws Exception {
        // Create a LocalJcrDriver instance that uses JNDI ...
        final Context jndi = mock(Context.class);
        when(jndi.lookup(anyString())).thenReturn(repository);
        LocalJcrDriver.JcrContextFactory contextFactory = new LocalJcrDriver.JcrContextFactory() {
            @Override
            public Context createContext( Properties properties ) {
                return jndi;
            }
        };

        return new LocalJcrDriver(contextFactory);
    }

    @Override
    protected String createConnectionUrl( JcrRepository repository ) throws Exception {
        return LocalJcrDriver.JNDI_URL_PREFIX + "jcr/local?repositoryName=" + repository.getName();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL2 Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldBeAbleToExecuteSqlSelectAllNodes() throws SQLException {
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:mixinTypes[STRING]    jcr:path[STRING]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "mode:root    null    /        1.3703413009643555        0",
            "nt:unstructured    null    /Cars    Cars    1.3703413009643555    Cars    1",
            "nt:unstructured    null    /Cars/Hybrid    Hybrid    1.3703413009643555    Hybrid    2",
            "car:Car    null    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.3703413009643555    Nissan Altima    3",
            "car:Car    null    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.3703413009643555    Toyota Highlander    3",
            "car:Car    null    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.3703413009643555    Toyota Prius    3",
            "nt:unstructured    null    /Cars/Luxury    Luxury    1.3703413009643555    Luxury    2",
            "car:Car    null    /Cars/Luxury/Bentley Continental    Bentley Continental    1.3703413009643555    Bentley Continental    3",
            "car:Car    null    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.3703413009643555    Cadillac DTS    3",
            "car:Car    null    /Cars/Luxury/Lexus IS350    Lexus IS350    1.3703413009643555    Lexus IS350    3",
            "nt:unstructured    null    /Cars/Sports    Sports    1.3703413009643555    Sports    2",
            "car:Car    null    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.3703413009643555    Aston Martin DB9    3",
            "car:Car    null    /Cars/Sports/Infiniti G37    Infiniti G37    1.3703413009643555    Infiniti G37    3",
            "nt:unstructured    null    /Cars/Utility    Utility    1.3703413009643555    Utility    2",
            "car:Car    null    /Cars/Utility/Ford F-150    Ford F-150    1.3703413009643555    Ford F-150    3",
            "car:Car    null    /Cars/Utility/Hummer H3    Hummer H3    1.3703413009643555    Hummer H3    3",
            "car:Car    null    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.3703413009643555    Land Rover LR2    3",
            "car:Car    null    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.3703413009643555    Land Rover LR3    3",
            "nt:unstructured    null    /NodeB    NodeB    1.3703413009643555    NodeB    1",
            "nt:unstructured    null    /Other    Other    1.3703413009643555    Other    1",
            "nt:unstructured    null    /Other/NodeA    NodeA    1.3703413009643555    NodeA    2",
            "nt:unstructured    null    /Other/NodeA[2]    NodeA    1.3703413009643555    NodeA    2",
            "nt:unstructured    null    /Other/NodeA[3]    NodeA    1.3703413009643555    NodeA    2"};

        // executeQuery("SELECT * FROM [nt:base] WHERE NOT( [jcr:path] LIKE '/jcr:*' ) ORDER BY [jcr:path]", expected, 23);
        executeQuery("SELECT * FROM [nt:base] WHERE [jcr:path] NOT LIKE '/jcr:*' ORDER BY [jcr:path]", expected, 23);
    }

    @Test
    public void shouldBeAbleToExecuteSqlSelectAllCars() throws SQLException {

        String[] expected = {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]    jcr:mixinTypes[STRING]    jcr:path[STRING]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car    null    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.7017179727554321    Aston Martin DB9    3",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car    null    /Cars/Luxury/Bentley Continental    Bentley Continental    1.7017179727554321    Bentley Continental    3",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car    null    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.7017179727554321    Cadillac DTS    3",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car    null    /Cars/Utility/Ford F-150    Ford F-150    1.7017179727554321    Ford F-150    3",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car    null    /Cars/Utility/Hummer H3    Hummer H3    1.7017179727554321    Hummer H3    3",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car    null    /Cars/Sports/Infiniti G37    Infiniti G37    1.7017179727554321    Infiniti G37    3",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car    null    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.7017179727554321    Land Rover LR2    3",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car    null    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.7017179727554321    Land Rover LR3    3",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car    null    /Cars/Luxury/Lexus IS350    Lexus IS350    1.7017179727554321    Lexus IS350    3",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car    null    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.7017179727554321    Nissan Altima    3",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car    null    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.7017179727554321    Toyota Highlander    3",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car    null    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.7017179727554321    Toyota Prius    3"};
        executeQuery("SELECT * FROM [car:Car] ORDER BY [jcr:name]", expected, 12);
    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseUsingDefault() throws SQLException {
        String[] expected = {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]    jcr:mixinTypes[STRING]    jcr:path[STRING]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car    null    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.7017179727554321    Aston Martin DB9    3",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car    null    /Cars/Luxury/Bentley Continental    Bentley Continental    1.7017179727554321    Bentley Continental    3",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car    null    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.7017179727554321    Cadillac DTS    3",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car    null    /Cars/Utility/Ford F-150    Ford F-150    1.7017179727554321    Ford F-150    3",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car    null    /Cars/Utility/Hummer H3    Hummer H3    1.7017179727554321    Hummer H3    3",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car    null    /Cars/Sports/Infiniti G37    Infiniti G37    1.7017179727554321    Infiniti G37    3",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car    null    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.7017179727554321    Land Rover LR2    3",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car    null    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.7017179727554321    Land Rover LR3    3",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car    null    /Cars/Luxury/Lexus IS350    Lexus IS350    1.7017179727554321    Lexus IS350    3",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car    null    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.7017179727554321    Nissan Altima    3",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car    null    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.7017179727554321    Toyota Highlander    3",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car    null    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.7017179727554321    Toyota Prius    3"};

        executeQuery("SELECT * FROM [car:Car] ORDER BY [car:maker], [car:model]", expected, 12);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseAsc() throws SQLException {
        String[] expected = {"car:model[STRING]", "Altima", "Continental", "DB9", "DTS", "F-150", "G37", "H3", "Highlander",
            "IS350", "LR2", "LR3", "Prius"};

        executeQuery("SELECT car.[car:model] FROM [car:Car] As car WHERE car.[car:model] IS NOT NULL ORDER BY car.[car:model] ASC",
                     expected,
                     12);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderedByClauseDesc() throws SQLException {
        String[] expected = {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:primaryType[STRING]    jcr:mixinTypes[STRING]    jcr:path[STRING]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    car:Car    null    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.7017179727554321    Land Rover LR3    3",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    car:Car    null    /Cars/Luxury/Lexus IS350    Lexus IS350    1.7017179727554321    Lexus IS350    3",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    car:Car    null    /Cars/Sports/Infiniti G37    Infiniti G37    1.7017179727554321    Infiniti G37    3",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    car:Car    null    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.7017179727554321    Toyota Highlander    3",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    car:Car    null    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.7017179727554321    Land Rover LR2    3",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    car:Car    null    /Cars/Utility/Hummer H3    Hummer H3    1.7017179727554321    Hummer H3    3",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    car:Car    null    /Cars/Utility/Ford F-150    Ford F-150    1.7017179727554321    Ford F-150    3",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    car:Car    null    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.7017179727554321    Toyota Prius    3",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    car:Car    null    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.7017179727554321    Nissan Altima    3",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    car:Car    null    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.7017179727554321    Aston Martin DB9    3",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    car:Car    null    /Cars/Luxury/Bentley Continental    Bentley Continental    1.7017179727554321    Bentley Continental    3",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    car:Car    null    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.7017179727554321    Cadillac DTS    3",};
        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        executeQuery("SELECT * FROM [car:Car] ORDER BY [car:msrp] DESC", expected, 12);

    }

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllCarsUnderHybrid() throws SQLException {

        String[] expected = {"car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]",
            "Nissan    Altima    2008    $18,260", "Toyota    Prius    2008    $21,500",
            "Toyota    Highlander    2008    $34,200"};

        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        executeQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%' ORDER BY [car:msrp]",
                     expected,
                     3);

    }

    @FixFor( "MODE-722" )
    @Test
    public void shouldBeAbleToExecuteSqlQueryUsingJoinToFindAllCarsUnderHybrid() throws SQLException {
        String[] expected = {"car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]",
            "Nissan    Altima    2008    $18,260", "Toyota    Prius    2008    $21,500",
            "Toyota    Highlander    2008    $34,200"};

        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        executeQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car JOIN [nt:unstructured] AS hybrid ON ISCHILDNODE(car,hybrid) WHERE NAME(hybrid) = 'Hybrid' ORDER BY car.[car:msrp]",
                     expected,
                     3);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryToFindAllUnstructuredNodes() throws SQLException {
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:mixinTypes[STRING]    jcr:path[STRING]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "car:Car    null    /Cars/Hybrid/Nissan Altima    Nissan Altima    1.7911769151687622    Nissan Altima    3",
            "car:Car    null    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    1.7911769151687622    Toyota Highlander    3",
            "car:Car    null    /Cars/Hybrid/Toyota Prius    Toyota Prius    1.7911769151687622    Toyota Prius    3",
            "car:Car    null    /Cars/Luxury/Bentley Continental    Bentley Continental    1.7911769151687622    Bentley Continental    3",
            "car:Car    null    /Cars/Luxury/Cadillac DTS    Cadillac DTS    1.7911769151687622    Cadillac DTS    3",
            "car:Car    null    /Cars/Luxury/Lexus IS350    Lexus IS350    1.7911769151687622    Lexus IS350    3",
            "car:Car    null    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    1.7911769151687622    Aston Martin DB9    3",
            "car:Car    null    /Cars/Sports/Infiniti G37    Infiniti G37    1.7911769151687622    Infiniti G37    3",
            "car:Car    null    /Cars/Utility/Ford F-150    Ford F-150    1.7911769151687622    Ford F-150    3",
            "car:Car    null    /Cars/Utility/Hummer H3    Hummer H3    1.7911769151687622    Hummer H3    3",
            "car:Car    null    /Cars/Utility/Land Rover LR2    Land Rover LR2    1.7911769151687622    Land Rover LR2    3",
            "car:Car    null    /Cars/Utility/Land Rover LR3    Land Rover LR3    1.7911769151687622    Land Rover LR3    3",
            "nt:unstructured    null    /Cars    Cars    1.8816214799880981    Cars    1",
            "nt:unstructured    null    /Cars/Hybrid    Hybrid    1.8816214799880981    Hybrid    2",
            "nt:unstructured    null    /Cars/Luxury    Luxury    1.8816214799880981    Luxury    2",
            "nt:unstructured    null    /Cars/Sports    Sports    1.8816214799880981    Sports    2",
            "nt:unstructured    null    /Cars/Utility    Utility    1.8816214799880981    Utility    2",
            "nt:unstructured    null    /NodeB    NodeB    1.8816214799880981    NodeB    1",
            "nt:unstructured    null    /Other    Other    1.8816214799880981    Other    1",
            "nt:unstructured    null    /Other/NodeA    NodeA    1.8816214799880981    NodeA    2",
            "nt:unstructured    null    /Other/NodeA[2]    NodeA    1.8816214799880981    NodeA    2",
            "nt:unstructured    null    /Other/NodeA[3]    NodeA    1.8816214799880981    NodeA    2",};
        executeQuery("SELECT * FROM [nt:unstructured] WHERE [jcr:path] NOT LIKE '/jcr:*' ORDER BY [jcr:primaryType], [jcr:path]",
                     expected,
                     22);
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws SQLException
     */
    @Ignore
    @Test
    public void shouldBeAbleToExecuteSqlQueryWithChildAxisCriteria() throws SQLException {
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:path[STRING]    jcr:name[STRING]    jcr:score[DOUBLE]    mode:localName[STRING]    mode:depth[LONG]",
            "nt:unstructured    /Cars/Utility    Utility    1.4142135381698608    Utility    2",
            "nt:unstructured    /Cars/Hybrid    Hybrid    1.4142135381698608    Hybrid    2",
            "nt:unstructured    /Cars/Sports    Sports    1.4142135381698608    Sports    2",
            "nt:unstructured    /Cars/Luxury    Luxury    1.4142135381698608    Luxury    2"};
        executeQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/Cars/%' AND NOT jcr:path LIKE '/Cars/%/%' AND jcr:path NOT LIKE '/jcr:*'",
                     expected,
                     4,
                     QueryLanguage.JCR_SQL);

    }

    @Test
    public void shouldGetCatalogs() throws SQLException {
        resultsComparator.compareColumns = false;
        String[] expected = {"TABLE_CAT[String]", "cars"};

        ResultSet rs = dbmd.getCatalogs();
        assertResultsSetEquals(rs, expected);
        assertRowCount(1);
    }

    @Test
    public void shouldGetTableTypes() throws SQLException {
        resultsComparator.compareColumns = false;
        String[] expected = {"TABLE_TYPE[String]", "VIEW"};

        ResultSet rs = dbmd.getTableTypes();
        assertResultsSetEquals(rs, expected);
        assertRowCount(1);
    }

    @Test
    public void shouldGetAllTables() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "cars    NULL    car:Car    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:created    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:etag    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:language    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:lastModified    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:lifecycle    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:lockable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:managedRetention    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:mimeType    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:referenceable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:shareable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:simpleVersionable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:title    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mix:versionable    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:derived    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:hashed    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:lock    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:locks    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:namespace    VIEW    Is Mixin: false    NULL    NULL    NULL    mode:uri    DERIVED",
            "cars    NULL    mode:namespaces    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:nodeTypes    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:publishArea    VIEW    Is Mixin: true    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
            "cars    NULL    mode:root    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:share    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:system    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:versionHistoryFolder    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    mode:versionStorage    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:activity    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:address    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:base    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:childNodeDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:configuration    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:file    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
            "cars    NULL    nt:folder    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:frozenNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:hierarchyNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:linkedFile    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
            "cars    NULL    nt:naturalText    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:nodeType    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:propertyDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:query    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
            "cars    NULL    nt:unstructured    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:version    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:versionHistory    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:versionLabels    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:versionedChild    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "%", new String[] {});
        assertResultsSetEquals(rs, expected);
        assertRowCount(44);

    }

    @Test
    public void shouldGetNTPrefixedTables() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "cars    NULL    nt:activity    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:address    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:base    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:childNodeDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:configuration    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:file    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
            "cars    NULL    nt:folder    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:frozenNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:hierarchyNode    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:linkedFile    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:content    DERIVED",
            "cars    NULL    nt:naturalText    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:nodeType    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:propertyDefinition    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:query    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
            "cars    NULL    nt:unstructured    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:version    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:versionHistory    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:versionLabels    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:versionedChild    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "nt:%", new String[] {});
        assertResultsSetEquals(rs, expected);
        assertRowCount(20);
    }

    @Test
    public void shouldGetResourceSuffixedTables() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "cars    NULL    mode:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED",
            "cars    NULL    nt:resource    VIEW    Is Mixin: false    NULL    NULL    NULL    jcr:data    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "%:resource", new String[] {});
        assertResultsSetEquals(rs, expected);
        assertRowCount(20);
    }

    @Test
    public void shouldGetTablesThatContainNodeTpe() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "cars    NULL    mode:nodeTypes    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED",
            "cars    NULL    nt:nodeType    VIEW    Is Mixin: false    NULL    NULL    NULL    null    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "%nodeType%", new String[] {});
        assertResultsSetEquals(rs, expected);
        assertRowCount(2);
    }

    @Test
    public void shouldGetAllColumnsFor1Table() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "cars    NULL    car:Car    car:engine    12    STRING    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:lengthInInches    8    DOUBLE    20    NULL    0    0    2        NULL    0    0    0    2    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:maker    12    STRING    50    NULL    0    0    2        NULL    0    0    0    3    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:model    12    STRING    50    NULL    0    0    2        NULL    0    0    0    4    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:mpgCity    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    5    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:mpgHighway    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    6    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:msrp    12    STRING    50    NULL    0    0    2        NULL    0    0    0    7    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:userRating    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    8    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:valueRating    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    9    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:wheelbaseInInches    8    DOUBLE    20    NULL    0    0    2        NULL    0    0    0    10    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:year    12    STRING    50    NULL    0    0    2        NULL    0    0    0    11    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    jcr:name    12    STRING    20    NULL    0    0    2        NULL    0    0    0    12    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    jcr:path    12    STRING    50    NULL    0    0    2        NULL    0    0    0    13    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    jcr:primaryType    12    STRING    20    NULL    0    0    1        NULL    0    0    0    14    NO    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    jcr:score    8    DOUBLE    20    NULL    0    0    2        NULL    0    0    0    15    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    mode:depth    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    16    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    mode:localName    12    STRING    50    NULL    0    0    2        NULL    0    0    0    17    YES    NULL    NULL    NULL    0"};
        ResultSet rs = dbmd.getColumns("%", "%", "car:Car", "%");

        assertResultsSetEquals(rs, expected);
        assertRowCount(17);

    }

    @Test
    public void shouldGetOnlyColumnsForCarPrefixedTables() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "cars    NULL    car:Car    car:engine    12    STRING    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:lengthInInches    8    DOUBLE    20    NULL    0    0    2        NULL    0    0    0    2    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:maker    12    STRING    50    NULL    0    0    2        NULL    0    0    0    3    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:model    12    STRING    50    NULL    0    0    2        NULL    0    0    0    4    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:mpgCity    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    5    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:mpgHighway    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    6    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:msrp    12    STRING    50    NULL    0    0    2        NULL    0    0    0    7    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:userRating    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    8    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:valueRating    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    9    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:wheelbaseInInches    8    DOUBLE    20    NULL    0    0    2        NULL    0    0    0    10    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    car:year    12    STRING    50    NULL    0    0    2        NULL    0    0    0    11    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    jcr:name    12    STRING    20    NULL    0    0    2        NULL    0    0    0    12    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    jcr:path    12    STRING    50    NULL    0    0    2        NULL    0    0    0    13    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    jcr:primaryType    12    STRING    20    NULL    0    0    1        NULL    0    0    0    14    NO    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    jcr:score    8    DOUBLE    20    NULL    0    0    2        NULL    0    0    0    15    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    mode:depth    -5    LONG    20    NULL    0    0    2        NULL    0    0    0    16    YES    NULL    NULL    NULL    0",
            "cars    NULL    car:Car    mode:localName    12    STRING    50    NULL    0    0    2        NULL    0    0    0    17    YES    NULL    NULL    NULL    0"};

        ResultSet rs = dbmd.getColumns("%", "%", "car%", "%");

        assertResultsSetEquals(rs, expected);
        assertRowCount(11);

    }

    @Test
    public void shouldGetOnlyMSRPColumnForCarTable() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "cars    NULL    car:Car    car:msrp    12    STRING    50    NULL    0    0    2        NULL    0    0    0    1    YES    NULL    NULL    NULL    0"};

        ResultSet rs = dbmd.getColumns("%", "%", "car:Car", "car:msrp");
        assertResultsSetEquals(rs, expected);
        assertRowCount(1);

    }
}
