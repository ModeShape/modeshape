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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Statements specialization for Oracle DB.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @author Illia Khokholkov
 * 
 * @since 5.0
 */
public class OracleStatements extends DefaultStatements {

    private static final List<Integer> IGNORABLE_ERROR_CODES = Arrays.asList(942, 955);
    
    /**
     * The maximum number of parameters the {@code IN} clause can have.
     */
    private static final int MAX_IN_CLAUSE_PARAMS = 1_000;
    
    private final Map<String, String> statements;
    
    protected OracleStatements( RelationalDbConfig config, Map<String, String> statements ) {
        super(config, statements);
        this.statements = statements;
    }

    @Override
    public Void createTable( Connection connection ) throws SQLException {
        try {
            return super.createTable(connection);
        } catch (SQLException e) {
            int errorCode = e.getErrorCode();
            if (IGNORABLE_ERROR_CODES.contains(errorCode)) {
                logger.debug(e, "Ignoring Oracle SQL exception for database {0} with error code {1}", tableName(), errorCode);
                return null;
            }
            throw e;
        }
    }

    @Override
    public Void dropTable( Connection connection ) throws SQLException {
        try {
            return super.dropTable(connection);
        } catch (SQLException e) {
            int errorCode = e.getErrorCode();
            if (IGNORABLE_ERROR_CODES.contains(e.getErrorCode())) {
                logger.debug(e, "Ignoring Oracle SQL exception for database {0} with error code {1}", tableName(), errorCode);
                return null;
            }
            throw e;
        }
    }
    
    @Override
    String formatStatementWithMultipleParams(String statement, List<String> ids) {
        List<List<String>> partitions = new ArrayList<>();
        int totalElements = ids.size();
        
        for (int i = 0; i < totalElements; i += MAX_IN_CLAUSE_PARAMS) {
            int end = Math.min(totalElements, i + MAX_IN_CLAUSE_PARAMS);
            partitions.add(ids.subList(i, end));
        }
        
        String conditionalInClause = partitions.stream()
                .map(partition -> super.formatStatementWithMultipleParams(statements.get(Statements.ID_IN_CLAUSE), partition))
                .collect(Collectors.joining(" OR "));
        
        return statement.replaceAll("#", conditionalInClause);
    }
}
