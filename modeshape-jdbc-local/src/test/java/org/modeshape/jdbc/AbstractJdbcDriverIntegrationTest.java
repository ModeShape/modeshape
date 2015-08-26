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
package org.modeshape.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.modeshape.common.FixFor;

/**
 * Common test class for the JDBC integration tests
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractJdbcDriverIntegrationTest extends AbstractJdbcDriverTest {

    // ----------------------------------------------------------------------------------------------------------------
    // JCR-SQL2 Queries
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldBeAbleToExecuteSqlSelectAllNodes() throws SQLException {
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:mixinTypes[STRING]    jcr:path[STRING]    jcr:name[STRING]    mode:localName[STRING]    mode:depth[LONG]",
            "mode:root    null    /            0", "nt:unstructured    null    /Cars    Cars    Cars    1",
            "nt:unstructured    null    /Cars/Hybrid    Hybrid    Hybrid    2",
            "car:Car    null    /Cars/Hybrid/Nissan Altima    Nissan Altima    Nissan Altima    3",
            "car:Car    null    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    Toyota Highlander    3",
            "car:Car    null    /Cars/Hybrid/Toyota Prius    Toyota Prius    Toyota Prius    3",
            "nt:unstructured    null    /Cars/Luxury    Luxury    Luxury    2",
            "car:Car    null    /Cars/Luxury/Bentley Continental    Bentley Continental    Bentley Continental    3",
            "car:Car    null    /Cars/Luxury/Cadillac DTS    Cadillac DTS    Cadillac DTS    3",
            "car:Car    null    /Cars/Luxury/Lexus IS350    Lexus IS350    Lexus IS350    3",
            "nt:unstructured    null    /Cars/Sports    Sports    Sports    2",
            "car:Car    null    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    Aston Martin DB9    3",
            "car:Car    null    /Cars/Sports/Infiniti G37    Infiniti G37    Infiniti G37    3",
            "nt:unstructured    null    /Cars/Utility    Utility    Utility    2",
            "car:Car    null    /Cars/Utility/Ford F-150    Ford F-150    Ford F-150    3",
            "car:Car    null    /Cars/Utility/Hummer H3    Hummer H3    Hummer H3    3",
            "car:Car    null    /Cars/Utility/Land Rover LR2    Land Rover LR2    Land Rover LR2    3",
            "car:Car    null    /Cars/Utility/Land Rover LR3    Land Rover LR3    Land Rover LR3    3",
            "nt:unstructured    null    /NodeB    NodeB    NodeB    1",
            "nt:unstructured    null    /Other    Other    Other    1",
            "nt:unstructured    null    /Other/NodeA    NodeA    NodeA    2",
            "nt:unstructured    null    /Other/NodeA[2]    NodeA    NodeA    2",
            "nt:unstructured    null    /Other/NodeA[3]    NodeA    NodeA    2"};

        // make sure system nodes are avoided
        executeQuery("SELECT [jcr:primaryType], [jcr:mixinTypes], [jcr:path], [jcr:name], [mode:localName], [mode:depth] FROM [nt:base] "
                     + "WHERE [jcr:path] NOT LIKE '/jcr:*' AND [jcr:path] NOT LIKE '/jcr:*/%' AND [jcr:path] NOT LIKE '/jcr:*/%/%' ORDER BY [jcr:path]",
                     expected, 23);
    }

    @Test
    public void shouldBeAbleToExecuteSqlSelectAllCars() throws SQLException {
        String[] expected = allCarsAsc();
        executeQuery("SELECT" + carColumns() + " FROM [car:Car] ORDER BY [jcr:name]", expected, 12);
    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseUsingDefault() throws SQLException {
        String[] expected = allCarsAsc();
        executeQuery("SELECT" + carColumns() + "FROM [car:Car] ORDER BY [car:maker], [car:model]", expected, 12);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderByClauseAsc() throws SQLException {
        String[] expected = {"car:model[STRING]", "Altima", "Continental", "DB9", "DTS", "F-150", "G37", "H3", "Highlander",
            "IS350", "LR2", "LR3", "Prius"};

        executeQuery("SELECT car.[car:model] FROM [car:Car] As car WHERE car.[car:model] IS NOT NULL ORDER BY car.[car:model] ASC",
                     expected, 12);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryWithOrderedByClauseDesc() throws SQLException {
        String[] expected = allCarsDesc();
        executeQuery("SELECT" + carColumns() + " FROM [car:Car] ORDER BY [jcr:name] DESC", expected, 12);
    }

    @Test
    public void shouldBeAbleToCreateAndExecuteSqlQueryToFindAllCarsUnderHybrid() throws SQLException {

        String[] expected = {"car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]",
            "Nissan    Altima    2008    $18,260", "Toyota    Prius    2008    $21,500",
            "Toyota    Highlander    2008    $34,200"};

        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        executeQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car WHERE PATH(car) LIKE '%/Hybrid/%' ORDER BY [car:msrp]",
                     expected, 3);

    }

    @FixFor( "MODE-722" )
    @Test
    public void shouldBeAbleToExecuteSqlQueryUsingJoinToFindAllCarsUnderHybrid() throws SQLException {
        String[] expected = {"car.car:maker[STRING]    car.car:model[STRING]    car.car:year[STRING]    car.car:msrp[STRING]",
            "Nissan    Altima    2008    $18,260", "Toyota    Prius    2008    $21,500",
            "Toyota    Highlander    2008    $34,200"};

        // Results are sorted by lexicographic MSRP (as a string, not as a number)!!!
        executeQuery("SELECT car.[car:maker], car.[car:model], car.[car:year], car.[car:msrp] FROM [car:Car] AS car JOIN [nt:unstructured] AS hybrid ON ISCHILDNODE(car,hybrid) WHERE NAME(hybrid) = 'Hybrid' ORDER BY car.[car:msrp]",
                     expected, 3);

    }

    @Test
    public void shouldBeAbleToExecuteSqlQueryToFindAllUnstructuredNodes() throws SQLException {
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:mixinTypes[STRING]    jcr:path[STRING]    jcr:name[STRING]    mode:localName[STRING]    mode:depth[LONG]",
            "car:Car    null    /Cars/Hybrid/Nissan Altima    Nissan Altima    Nissan Altima    3",
            "car:Car    null    /Cars/Hybrid/Toyota Highlander    Toyota Highlander    Toyota Highlander    3",
            "car:Car    null    /Cars/Hybrid/Toyota Prius    Toyota Prius    Toyota Prius    3",
            "car:Car    null    /Cars/Luxury/Bentley Continental    Bentley Continental    Bentley Continental    3",
            "car:Car    null    /Cars/Luxury/Cadillac DTS    Cadillac DTS    Cadillac DTS    3",
            "car:Car    null    /Cars/Luxury/Lexus IS350    Lexus IS350    Lexus IS350    3",
            "car:Car    null    /Cars/Sports/Aston Martin DB9    Aston Martin DB9    Aston Martin DB9    3",
            "car:Car    null    /Cars/Sports/Infiniti G37    Infiniti G37    Infiniti G37    3",
            "car:Car    null    /Cars/Utility/Ford F-150    Ford F-150    Ford F-150    3",
            "car:Car    null    /Cars/Utility/Hummer H3    Hummer H3    Hummer H3    3",
            "car:Car    null    /Cars/Utility/Land Rover LR2    Land Rover LR2    Land Rover LR2    3",
            "car:Car    null    /Cars/Utility/Land Rover LR3    Land Rover LR3    Land Rover LR3    3",
            "nt:unstructured    null    /Cars    Cars    Cars    1",
            "nt:unstructured    null    /Cars/Hybrid    Hybrid    Hybrid    2",
            "nt:unstructured    null    /Cars/Luxury    Luxury    Luxury    2",
            "nt:unstructured    null    /Cars/Sports    Sports    Sports    2",
            "nt:unstructured    null    /Cars/Utility    Utility    Utility    2",
            "nt:unstructured    null    /NodeB    NodeB    NodeB    1",
            "nt:unstructured    null    /Other    Other    Other    1",
            "nt:unstructured    null    /Other/NodeA    NodeA    NodeA    2",
            "nt:unstructured    null    /Other/NodeA[2]    NodeA    NodeA    2",
            "nt:unstructured    null    /Other/NodeA[3]    NodeA    NodeA    2",};
        // SELECT * FROM ... except the [jcr:score] column ...
        executeQuery("SELECT [jcr:primaryType], [jcr:mixinTypes], [jcr:path], [jcr:name], [mode:localName], [mode:depth] FROM [nt:unstructured] WHERE [jcr:path] NOT LIKE '/jcr:*' ORDER BY [jcr:primaryType], [jcr:path]",
                     expected, 22);
    }

    private String carColumns() {
        return " [car:maker], [car:model], [car:year], [car:msrp], [car:userRating], [car:valueRating], [car:mpgCity], [car:mpgHighway], [car:lengthInInches], [car:wheelbaseInInches], [car:engine], [jcr:path], [jcr:name]";
    }

    private String[] allCarsDesc() {
        List<String> allCarsAsc = new ArrayList<String>(Arrays.asList(allCarsAsc()));
        String header = allCarsAsc.remove(0);
        Collections.reverse(allCarsAsc);
        allCarsAsc.add(0, header);
        return allCarsAsc.toArray(new String[0]);
    }

    private String[] allCarsAsc() {
        return new String[] {
            "car:maker[STRING]    car:model[STRING]    car:year[STRING]    car:msrp[STRING]    car:userRating[LONG]    car:valueRating[LONG]    car:mpgCity[LONG]    car:mpgHighway[LONG]    car:lengthInInches[DOUBLE]    car:wheelbaseInInches[DOUBLE]    car:engine[STRING]    jcr:path[STRING]    jcr:name[STRING]",
            "Aston Martin    DB9    2008    $171,600    5    null    12    19    185.5    108.0    5,935 cc 5.9 liters V 12    /Cars/Sports/Aston Martin DB9    Aston Martin DB9",
            "Bentley    Continental    2008    $170,990    null    null    10    17    null    null    null    /Cars/Luxury/Bentley Continental    Bentley Continental",
            "Cadillac    DTS    2008    null    1    null    null    null    null    null    3.6 liter V6    /Cars/Luxury/Cadillac DTS    Cadillac DTS",
            "Ford    F-150    2008    $23,910    5    1    14    20    null    null    null    /Cars/Utility/Ford F-150    Ford F-150",
            "Hummer    H3    2008    $30,595    3    4    13    16    null    null    null    /Cars/Utility/Hummer H3    Hummer H3",
            "Infiniti    G37    2008    $34,900    3    4    18    24    null    null    null    /Cars/Sports/Infiniti G37    Infiniti G37",
            "Land Rover    LR2    2008    $33,985    4    5    16    23    null    null    null    /Cars/Utility/Land Rover LR2    Land Rover LR2",
            "Land Rover    LR3    2008    $48,525    5    2    12    17    null    null    null    /Cars/Utility/Land Rover LR3    Land Rover LR3",
            "Lexus    IS350    2008    $36,305    4    5    18    25    null    null    null    /Cars/Luxury/Lexus IS350    Lexus IS350",
            "Nissan    Altima    2008    $18,260    null    null    23    32    null    null    null    /Cars/Hybrid/Nissan Altima    Nissan Altima",
            "Toyota    Highlander    2008    $34,200    4    5    27    25    null    null    null    /Cars/Hybrid/Toyota Highlander    Toyota Highlander",
            "Toyota    Prius    2008    $21,500    4    5    48    45    null    null    null    /Cars/Hybrid/Toyota Prius    Toyota Prius"};
    }

    /**
     * Tests that the child nodes (but no grandchild nodes) are returned.
     * 
     * @throws SQLException
     */
    @Test
    public void shouldBeAbleToExecuteSqlQueryWithChildAxisCriteria() throws SQLException {
        String[] expected = {
            "jcr:primaryType[STRING]    jcr:mixinTypes[STRING]    jcr:path[STRING]    jcr:name[STRING]    mode:localName[STRING]    mode:depth[LONG]",
            "nt:unstructured    null    /Cars/Hybrid    Hybrid    Hybrid    2",
            "nt:unstructured    null    /Cars/Luxury    Luxury    Luxury    2",
            "nt:unstructured    null    /Cars/Sports    Sports    Sports    2",
            "nt:unstructured    null    /Cars/Utility    Utility    Utility    2"};

        executeQuery("SELECT [jcr:primaryType], [jcr:mixinTypes], [jcr:path], [jcr:name], [mode:localName], [mode:depth] FROM [nt:base] WHERE [jcr:path] LIKE '/Cars/%' AND DEPTH() = 2 ORDER BY [jcr:path]",
                     expected, 4);

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
            "cars    null    car:Car    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mix:created    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:etag    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:language    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:lastModified    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:lifecycle    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:lockable    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:managedRetention    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:mimeType    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:referenceable    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:shareable    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:simpleVersionable    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:title    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mix:versionable    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mode:accessControllable    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mode:derived    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mode:federation    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:hashed    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mode:index    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:indexColumn    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:indexProvider    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:indexes    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:lock    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:locks    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:namespace    VIEW    Is Mixin: false    null    null    null    mode:uri    DERIVED",
            "cars    null    mode:namespaces    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:nodeTypes    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:projection    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:publishArea    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mode:repository    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:resource    VIEW    Is Mixin: false    null    null    null    jcr:data    DERIVED",
            "cars    null    mode:root    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:share    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:system    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:unorderedCollection    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mode:unorderedHugeCollection    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mode:unorderedLargeCollection    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mode:unorderedSmallCollection    VIEW    Is Mixin: true    null    null    null    null    DERIVED",
            "cars    null    mode:unorderedTinyCollection    VIEW    Is Mixin: true    null    null    null    null    DERIVED",             
            "cars    null    mode:versionHistoryFolder    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    mode:versionStorage    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:activity    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:address    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:base    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:childNodeDefinition    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:configuration    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:file    VIEW    Is Mixin: false    null    null    null    jcr:content    DERIVED",
            "cars    null    nt:folder    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:frozenNode    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:hierarchyNode    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:linkedFile    VIEW    Is Mixin: false    null    null    null    jcr:content    DERIVED",
            "cars    null    nt:naturalText    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:nodeType    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:propertyDefinition    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:query    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:resource    VIEW    Is Mixin: false    null    null    null    jcr:data    DERIVED",
            "cars    null    nt:unstructured    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:version    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:versionHistory    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:versionLabels    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:versionedChild    VIEW    Is Mixin: false    null    null    null    null    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "%", new String[] {});
        assertResultsSetEquals(rs, expected);
        assertRowCount(44);

    }

    @Test
    public void shouldGetNTPrefixedTables() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "cars    null    nt:activity    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:address    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:base    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:childNodeDefinition    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:configuration    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:file    VIEW    Is Mixin: false    null    null    null    jcr:content    DERIVED",
            "cars    null    nt:folder    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:frozenNode    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:hierarchyNode    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:linkedFile    VIEW    Is Mixin: false    null    null    null    jcr:content    DERIVED",
            "cars    null    nt:naturalText    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:nodeType    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:propertyDefinition    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:query    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:resource    VIEW    Is Mixin: false    null    null    null    jcr:data    DERIVED",
            "cars    null    nt:unstructured    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:version    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:versionHistory    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:versionLabels    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:versionedChild    VIEW    Is Mixin: false    null    null    null    null    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "nt:%", new String[] {});
        assertResultsSetEquals(rs, expected);
        assertRowCount(20);
    }

    @Test
    public void shouldGetResourceSuffixedTables() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "cars    null    mode:resource    VIEW    Is Mixin: false    null    null    null    jcr:data    DERIVED",
            "cars    null    nt:resource    VIEW    Is Mixin: false    null    null    null    jcr:data    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "%:resource", new String[] {});
        assertResultsSetEquals(rs, expected);
        assertRowCount(20);
    }

    @Test
    public void shouldGetTablesThatContainNodeTpe() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    TABLE_TYPE[String]    REMARKS[String]    TYPE_CAT[String]    TYPE_SCHEM[String]    TYPE_NAME[String]    SELF_REFERENCING_COL_NAME[String]    REF_GENERATION[String]",
            "cars    null    mode:nodeTypes    VIEW    Is Mixin: false    null    null    null    null    DERIVED",
            "cars    null    nt:nodeType    VIEW    Is Mixin: false    null    null    null    null    DERIVED"};

        ResultSet rs = dbmd.getTables("%", "%", "%nodeType%", new String[] {});
        assertResultsSetEquals(rs, expected);
        assertRowCount(2);
    }

    @Test
    public void shouldGetAllColumnsFor1Table() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "cars    null    car:Car    car:engine    12    STRING    50    null    0    0    2        null    0    0    0    1    YES    null    null    null    0",
            "cars    null    car:Car    car:lengthInInches    8    DOUBLE    20    null    0    0    2        null    0    0    0    2    YES    null    null    null    0",
            "cars    null    car:Car    car:maker    12    STRING    50    null    0    0    2        null    0    0    0    3    YES    null    null    null    0",
            "cars    null    car:Car    car:model    12    STRING    50    null    0    0    2        null    0    0    0    4    YES    null    null    null    0",
            "cars    null    car:Car    car:mpgCity    -5    LONG    20    null    0    0    2        null    0    0    0    5    YES    null    null    null    0",
            "cars    null    car:Car    car:mpgHighway    -5    LONG    20    null    0    0    2        null    0    0    0    6    YES    null    null    null    0",
            "cars    null    car:Car    car:msrp    12    STRING    50    null    0    0    2        null    0    0    0    7    YES    null    null    null    0",
            "cars    null    car:Car    car:userRating    -5    LONG    20    null    0    0    2        null    0    0    0    8    YES    null    null    null    0",
            "cars    null    car:Car    car:valueRating    -5    LONG    20    null    0    0    2        null    0    0    0    9    YES    null    null    null    0",
            "cars    null    car:Car    car:wheelbaseInInches    8    DOUBLE    20    null    0    0    2        null    0    0    0    10    YES    null    null    null    0",
            "cars    null    car:Car    car:year    12    STRING    50    null    0    0    2        null    0    0    0    11    YES    null    null    null    0",
            "cars    null    car:Car    jcr:name    12    STRING    20    null    0    0    2        null    0    0    0    12    YES    null    null    null    0",
            "cars    null    car:Car    jcr:path    12    STRING    50    null    0    0    2        null    0    0    0    13    YES    null    null    null    0",
            "cars    null    car:Car    jcr:primaryType    12    STRING    20    null    0    0    1        null    0    0    0    14    NO    null    null    null    0",
            "cars    null    car:Car    jcr:score    8    DOUBLE    20    null    0    0    2        null    0    0    0    15    YES    null    null    null    0",
            "cars    null    car:Car    mode:depth    -5    LONG    20    null    0    0    2        null    0    0    0    16    YES    null    null    null    0",
            "cars    null    car:Car    mode:id    12    STRING    50    null    0    0    2        null    0    0    0    17    YES    null    null    null    0",
            "cars    null    car:Car    mode:localName    12    STRING    50    null    0    0    2        null    0    0    0    18    YES    null    null    null    0"};
        ResultSet rs = dbmd.getColumns("%", "%", "car:Car", "%");

        assertResultsSetEquals(rs, expected);
        assertRowCount(17);

    }

    @Test
    public void shouldGetOnlyColumnsForCarPrefixedTables() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "cars    null    car:Car    car:engine    12    STRING    50    null    0    0    2        null    0    0    0    1    YES    null    null    null    0",
            "cars    null    car:Car    car:lengthInInches    8    DOUBLE    20    null    0    0    2        null    0    0    0    2    YES    null    null    null    0",
            "cars    null    car:Car    car:maker    12    STRING    50    null    0    0    2        null    0    0    0    3    YES    null    null    null    0",
            "cars    null    car:Car    car:model    12    STRING    50    null    0    0    2        null    0    0    0    4    YES    null    null    null    0",
            "cars    null    car:Car    car:mpgCity    -5    LONG    20    null    0    0    2        null    0    0    0    5    YES    null    null    null    0",
            "cars    null    car:Car    car:mpgHighway    -5    LONG    20    null    0    0    2        null    0    0    0    6    YES    null    null    null    0",
            "cars    null    car:Car    car:msrp    12    STRING    50    null    0    0    2        null    0    0    0    7    YES    null    null    null    0",
            "cars    null    car:Car    car:userRating    -5    LONG    20    null    0    0    2        null    0    0    0    8    YES    null    null    null    0",
            "cars    null    car:Car    car:valueRating    -5    LONG    20    null    0    0    2        null    0    0    0    9    YES    null    null    null    0",
            "cars    null    car:Car    car:wheelbaseInInches    8    DOUBLE    20    null    0    0    2        null    0    0    0    10    YES    null    null    null    0",
            "cars    null    car:Car    car:year    12    STRING    50    null    0    0    2        null    0    0    0    11    YES    null    null    null    0",
            "cars    null    car:Car    jcr:name    12    STRING    20    null    0    0    2        null    0    0    0    12    YES    null    null    null    0",
            "cars    null    car:Car    jcr:path    12    STRING    50    null    0    0    2        null    0    0    0    13    YES    null    null    null    0",
            "cars    null    car:Car    jcr:primaryType    12    STRING    20    null    0    0    1        null    0    0    0    14    NO    null    null    null    0",
            "cars    null    car:Car    jcr:score    8    DOUBLE    20    null    0    0    2        null    0    0    0    15    YES    null    null    null    0",
            "cars    null    car:Car    mode:depth    -5    LONG    20    null    0    0    2        null    0    0    0    16    YES    null    null    null    0",
            "cars    null    car:Car    mode:id    12    STRING    50    null    0    0    2        null    0    0    0    17    YES    null    null    null    0",
            "cars    null    car:Car    mode:localName    12    STRING    50    null    0    0    2        null    0    0    0    18    YES    null    null    null    0"};

        ResultSet rs = dbmd.getColumns("%", "%", "car%", "%");

        assertResultsSetEquals(rs, expected);
        assertRowCount(11);

    }

    @Test
    public void shouldGetOnlyMSRPColumnForCarTable() throws SQLException {
        resultsComparator.compareColumns = false;

        String[] expected = {
            "TABLE_CAT[String]    TABLE_SCHEM[String]    TABLE_NAME[String]    COLUMN_NAME[String]    DATA_TYPE[Long]    TYPE_NAME[String]    COLUMN_SIZE[Long]    BUFFER_LENGTH[Long]    DECIMAL_DIGITS[Long]    NUM_PREC_RADIX[Long]    NULLABLE[Long]    REMARKS[String]    COLUMN_DEF[String]    SQL_DATA_TYPE[Long]    SQL_DATETIME_SUB[Long]    CHAR_OCTET_LENGTH[Long]    ORDINAL_POSITION[Long]    IS_NULLABLE[String]    SCOPE_CATLOG[String]    SCOPE_SCHEMA[String]    SCOPE_TABLE[String]    SOURCE_DATA_TYPE[Long]",
            "cars    null    car:Car    car:msrp    12    STRING    50    null    0    0    2        null    0    0    0    1    YES    null    null    null    0"};

        ResultSet rs = dbmd.getColumns("%", "%", "car:Car", "car:msrp");
        assertResultsSetEquals(rs, expected);
        assertRowCount(1);

    }
}
