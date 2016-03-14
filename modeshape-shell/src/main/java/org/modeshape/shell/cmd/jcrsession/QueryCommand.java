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
package org.modeshape.shell.cmd.jcrsession;

import org.modeshape.shell.cmd.ShellCommand;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.shell.ShellSession;
import org.modeshape.shell.ShellI18n;

/**
 *
 * @author kulikov
 */
public class QueryCommand extends ShellCommand {

    public QueryCommand() {
        super("query", ShellI18n.queryHelp);
    }
    
    @Override
    public String exec(ShellSession session) throws Exception {
        String lang = optionValue("--lang");
        String queryString = args(0);
        
        if (!this.allArgsSpecified(lang, queryString)) {
            return help();
        }
        
        //execute query
        Query query = session.jcrSession().getWorkspace().getQueryManager().createQuery(queryString, lang);
        QueryResult res = query.execute();

        StringBuilder builder = new StringBuilder();

        printColumns(res.getColumnNames(), builder);
        printRows(res.getRows(), builder);
        
        return builder.toString();
    }

    private void printColumns(String[] columns, StringBuilder builder) {
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                builder.append(TAB3);
            }
            builder.append(columns[i]);
        }
        builder.append(EOL);
    }
    
    private void printRows(RowIterator it, StringBuilder builder) throws RepositoryException {
        while (it.hasNext()) {
            Row row = it.nextRow();
            Value[] values = row.getValues();
            for (int i = 0; i < values.length; i++) {
                if (i > 0) builder.append(TAB3);
                builder.append(values[i]);
            }
            builder.append(EOL);
        }
    }
}
