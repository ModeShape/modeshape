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

import java.sql.SQLException;
import java.util.Map;

/**
 * Statements specialization for Microsoft SQL Server.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.4
 */
public class SQLServerStatements extends DefaultStatements {
    protected SQLServerStatements(RelationalDbConfig config, Map<String, String> statements) {
        super(config, statements);
    }
    
    @Override
    protected void processSQLException(String statementId, SQLException e) throws SQLException {
        int errorCode = e.getErrorCode();
        if (errorCode == 2714 && CREATE_TABLE.equals(statementId)) {
            logTableInfo("Table {0} already exists");
        } else if (errorCode == 3701 && DELETE_TABLE.equals(statementId)) {
            logTableInfo("Table {0} does not exist");
        } else {
            throw e;
        }
    }
}
