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
package org.modeshape.persistence.relational;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.modeshape.schematic.document.Document;

/**
 * Unit tests for {@link OracleStatements}. 
 * 
 * @author Illia Khokholkov
 *
 */
public class OracleStatementsTest {
    
    private static final String SELECT_STATEMENT_PATTERN = "SELECT CONTENT FROM MODESHAPE WHERE #";
    private static final String ID_IN_CLAUSE_PATTERN = "ID IN (#)";
    
    private OracleStatements oracleStatements;
    
    @Before
    public void setUp() {
        Map<String, String> statements = new HashMap<>();
        statements.put(Statements.ID_IN_CLAUSE, ID_IN_CLAUSE_PATTERN);
        
        RelationalDbConfig dbConfig = new RelationalDbConfig(mock(Document.class));
        oracleStatements = new OracleStatements(dbConfig, statements);
    }
    
    @Test
    public void formatStatementParamsWithinLimit() {
        List<String> ids = IntStream.range(0, 1000).mapToObj(i -> Integer.toString(i))
                .collect(Collectors.toList());
        
        String expectedParams = ids.stream().map(entry -> "?")
                .collect(Collectors.joining(","));
        
        assertEquals(
                String.format("SELECT CONTENT FROM MODESHAPE WHERE ID IN (%s)", expectedParams),
                oracleStatements.formatStatementWithMultipleParams(SELECT_STATEMENT_PATTERN, ids));
    }
    
    @Test
    public void formatStatementMaxParamsReached() {
        List<String> ids = IntStream.range(0, 2100).mapToObj(i -> Integer.toString(i))
                .collect(Collectors.toList());
        
        String partitionOne = IntStream.range(0, 1000).mapToObj(i -> "?").collect(Collectors.joining(","));
        String partitionTwo = partitionOne;
        String partitionThree = IntStream.range(0, 100).mapToObj(i -> "?").collect(Collectors.joining(","));
        
        assertEquals(
                String.format("SELECT CONTENT FROM MODESHAPE WHERE ID IN (%s) OR ID IN (%s) OR ID IN (%s)",
                        partitionOne, partitionTwo, partitionThree),
                oracleStatements.formatStatementWithMultipleParams(SELECT_STATEMENT_PATTERN, ids));
    }
}
