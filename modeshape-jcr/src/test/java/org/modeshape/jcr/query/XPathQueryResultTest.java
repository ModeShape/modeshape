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
package org.modeshape.jcr.query;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.engine.ScanningQueryEngine;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.plan.PlanHints;

public class XPathQueryResultTest {

    private XPathQueryResult result;
    private JcrQueryContext context;
    private String query;
    private QueryResults graphResult;
    private Columns resultColumns;
    private List<String> columnTypes;
    private List<String> columnNames;
    private List<Column> columns;

    @Before
    public void beforeEach() {
        context = mock(JcrQueryContext.class);
        query = "SELECT jcr:primaryType, foo:bar FROM nt:unstructured";
        graphResult = mock(QueryResults.class);
        columnTypes = Arrays.asList("STRING", "LONG");
        columnNames = Arrays.asList("jcr:primaryType", "foo:bar");
        SelectorName tableName = new SelectorName("nt:unstructured");
        columns = Arrays.asList(new Column(tableName, columnNames.get(0), columnNames.get(0)),
                                new Column(tableName, columnNames.get(1), columnNames.get(1)));
        resultColumns = new ScanningQueryEngine.ResultColumns(columns, columnTypes, true, null);
        when(graphResult.getColumns()).thenReturn(resultColumns);
        when(graphResult.getRows()).thenReturn(NodeSequence.emptySequence(1));

        PlanHints hints = new PlanHints();
        result = new XPathQueryResult(context, query, graphResult, hints.restartable, hints.rowsKeptInMemory);
    }

    @Test
    public void shouldHaveSameNumberOfColumnNamesAsTypes() {
        assertThat(result.getColumnNames(), is(new String[] {"jcr:path", "jcr:score", "jcr:primaryType", "foo:bar"}));
        assertThat(result.getColumnTypes(), is(new String[] {"STRING", "DOUBLE", "STRING", "LONG"}));
    }
}
