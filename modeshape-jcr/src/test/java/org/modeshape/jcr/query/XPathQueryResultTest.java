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
package org.modeshape.jcr.query;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.process.QueryResultColumns;
import org.modeshape.graph.query.validate.Schemata;

public class XPathQueryResultTest {

    private XPathQueryResult result;
    private JcrQueryContext context;
    private String query;
    private QueryResults graphResult;
    private Schemata schemata;
    private Columns resultColumns;
    private List<String> columnTypes;
    private List<String> columnNames;
    private List<Column> columns;

    @Before
    public void beforeEach() {
        schemata = mock(Schemata.class);
        context = mock(JcrQueryContext.class);
        query = "SELECT jcr:primaryType, foo:bar FROM nt:unstructured";
        graphResult = mock(QueryResults.class);
        columnTypes = Arrays.asList("STRING", "LONG");
        columnNames = Arrays.asList("jcr:primaryType", "foo:bar");
        SelectorName tableName = new SelectorName("nt:unstructured");
        columns = Arrays.asList(new Column(tableName, columnNames.get(0), columnNames.get(0)), new Column(tableName,
                                                                                                          columnNames.get(1),
                                                                                                          columnNames.get(1)));
        resultColumns = new QueryResultColumns(columns, columnTypes, true);
        when(graphResult.getColumns()).thenReturn(resultColumns);

        result = new XPathQueryResult(context, query, graphResult, schemata);
    }

    @Test
    public void shouldHaveSameNumberOfColumnNamesAsTypes() {
        assertThat(result.getColumnNames(), is(new String[] {"jcr:path", "jcr:score", "jcr:primaryType", "foo:bar"}));
        assertThat(result.getColumnTypes(), is(new String[] {"STRING", "DOUBLE", "STRING", "LONG"}));
    }
}
