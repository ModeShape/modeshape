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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.QueryResult;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.modeshape.jdbc.delegate.ConnectionInfo;

/**
 * 
 */
public class JcrResultSetMetaDataTest {

    public static final String STRING = TestUtil.STRING;
    public static final String DOUBLE = TestUtil.DOUBLE;
    public static final String LONG = TestUtil.LONG;
    public static final String PATH = TestUtil.PATH;
    public static final String REFERENCE = TestUtil.REFERENCE;

    public static final Class<?> STRING_CLASS = JcrType.builtInTypeMap().get(STRING).getRepresentationClass();

    private JcrResultSetMetaData metadata;
    private JcrResultSetMetaData extMetadata;
    @Mock
    private JcrConnection connection;
    @Mock
    private QueryResult results;
    @Mock
    private org.modeshape.jcr.api.query.QueryResult extendedResults;
    private String[] columnNames = TestUtil.COLUMN_NAMES;
    private String[] tableNames = TestUtil.TABLE_NAMES;
    private String[] typeNames = TestUtil.TYPE_NAMES;
    @Mock
    private ConnectionInfo info;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(results.getColumnNames()).thenReturn(columnNames);
        when(results.getSelectorNames()).thenReturn(tableNames);
        when(extendedResults.getColumnNames()).thenReturn(columnNames);
        when(extendedResults.getSelectorNames()).thenReturn(tableNames);
        when(extendedResults.getColumnTypes()).thenReturn(typeNames);
        // Set up the node types ...
        addPropDefn("typeA", "propA", true);
        addPropDefn("typeA", "propC", false);
        addPropDefn("typeB", "propB", false);
        addPropDefn("typeA", "propE", false);
        // Set up the connection information ...
        when(info.getWorkspaceName()).thenReturn("workspaceName");
        when(info.getRepositoryName()).thenReturn("repositoryName");
        // Set up the mock connection ...
        when(connection.info()).thenReturn(info);

        // Set up the metadata objects ...
        metadata = new JcrResultSetMetaData(connection, results);
        extMetadata = new JcrResultSetMetaData(connection, extendedResults);
    }

    @Test
    public void shouldReturnRepositoryNameAsCatalogName() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.getCatalogName(i), is(info.getRepositoryName()));
        }
        when(info.getRepositoryName()).thenReturn(null);
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.getCatalogName(i), is(info.getRepositoryName()));
        }
    }

    @Test
    public void shouldReturnWorkspaceNameAsSchemaName() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.getSchemaName(i), is(info.getWorkspaceName()));
        }
        when(info.getWorkspaceName()).thenReturn(null);
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.getSchemaName(i), is(info.getWorkspaceName()));
        }
    }

    @Test
    public void shouldReturnColumnCountFromQueryResults() throws SQLException {
        assertThat(metadata.getColumnCount(), is(columnNames.length));
    }

    @Test
    public void shouldReturnColumnNameFromQueryResultColumNames() throws SQLException {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.getColumnName(i + 1), is(columnNames[i]));
        }
    }

    @Test
    public void shouldReturnColumnLabelFromQueryResultColumNames() throws SQLException {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.getColumnLabel(i + 1), is(columnNames[i]));
        }
    }

    @Test
    public void shouldReturnStringForColumnTypeWhenResultIsNotExtendedJcrQueryResult() {
        assertThat(results instanceof org.modeshape.jcr.api.query.QueryResult, is(false));
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.getColumnTypeName(i + 1), is(STRING));
            assertThat(metadata.getColumnType(i + 1), is(Types.VARCHAR));
            assertThat(metadata.getColumnClassName(i + 1), is(String.class.getName()));
        }
    }

    @Test
    public void shouldReturnActualTypeForColumnTypeWhenResultIsExtendedJcrQueryResult() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(extMetadata.getColumnTypeName(i + 1), is(typeNames[i]));
            JcrType expectedType = JcrType.typeInfo(typeNames[i]);
            assertThat(extMetadata.getColumnType(i + 1), is(expectedType.getJdbcType()));
            assertThat(extMetadata.getColumnClassName(i + 1), is(expectedType.getRepresentationClass().getName()));
        }
    }

    @Test
    public void shouldReturnZeroForPrecisionWhenResultIsNotExtendedJcrQueryResult() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.getPrecision(i + 1), is(JcrType.typeInfo(STRING).getNominalDisplaySize()));
        }
    }

    @Test
    public void shouldReturnPrecisionBasedUponPropertyTypeWhenResultIsNotExtendedJcrQueryResult() {
        assertThat(extMetadata.getPrecision(1), is(JcrType.typeInfo(STRING).getNominalDisplaySize())); // STRING
        assertThat(extMetadata.getPrecision(2), is(JcrType.typeInfo(LONG).getNominalDisplaySize())); // LONG
        assertThat(extMetadata.getPrecision(3), is(JcrType.typeInfo(PATH).getNominalDisplaySize())); // PATH
        assertThat(extMetadata.getPrecision(4), is(JcrType.typeInfo(REFERENCE).getNominalDisplaySize())); // REFERENCE
        assertThat(extMetadata.getPrecision(5), is(JcrType.typeInfo(DOUBLE).getNominalDisplaySize())); // DOUBLE
    }

    @Test
    public void shouldReturnZeroForScaleWhenResultIsNotExtendedJcrQueryResult() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.getScale(i + 1), is(0));
        }
    }

    @Test
    public void shouldReturnScaleBasedUponPropertyTypeWhenResultIsNotExtendedJcrQueryResult() {
        assertThat(extMetadata.getScale(1), is(0)); // STRING
        assertThat(extMetadata.getScale(2), is(0)); // LONG
        assertThat(extMetadata.getScale(3), is(0)); // PATH
        assertThat(extMetadata.getScale(4), is(0)); // REFERENCE
        assertThat(extMetadata.getScale(5), is(3)); // DOUBLE
    }

    @Ignore
    @Test
    public void shouldReturnUnknownNullableWhenResultIsNotExtendedJcrQueryResult() throws SQLException {
        assertThat(metadata.isNullable(1), is(ResultSetMetaData.columnNullableUnknown));
        assertThat(metadata.isNullable(2), is(ResultSetMetaData.columnNullableUnknown));
        assertThat(metadata.isNullable(3), is(ResultSetMetaData.columnNullableUnknown));
        assertThat(metadata.isNullable(4), is(ResultSetMetaData.columnNullableUnknown));
        assertThat(metadata.isNullable(5), is(ResultSetMetaData.columnNullableUnknown));
    }

    @Test
    public void shouldReturnNullableBasedUponNodeTypeWhenResultIsExtendedJcrQueryResult() throws SQLException {
        assertThat(extMetadata.isNullable(1), is(ResultSetMetaData.columnNullable)); // propA is multiple
        assertThat(extMetadata.isNullable(2), is(ResultSetMetaData.columnNoNulls)); // propB is NOT multiple
        assertThat(extMetadata.isNullable(3), is(ResultSetMetaData.columnNoNulls)); // propC is NOT multiple
        assertThat(extMetadata.isNullable(4), is(ResultSetMetaData.columnNullableUnknown)); // propD has no table
        assertThat(extMetadata.isNullable(5), is(ResultSetMetaData.columnNoNulls)); // propE is NOT multiple
        // Add a residual, but since the isNullable() result is cached, this won't matter ...
        addPropDefn("typeB", "*", true);
        assertThat(extMetadata.isNullable(2), is(ResultSetMetaData.columnNoNulls)); // propB is NOT multiple
    }

    @Test
    public void shouldReturnNullableBasedUponNodeTypeIncludingResidualsWhenResultIsExtendedJcrQueryResult() throws SQLException {
        // Add a residual so that column 2 is now considered nullable...
        addPropDefn("typeB", "*", true);
        assertThat(extMetadata.isNullable(1), is(ResultSetMetaData.columnNullable)); // propA is multiple
        assertThat(extMetadata.isNullable(2), is(ResultSetMetaData.columnNullable)); // * is multiple
        assertThat(extMetadata.isNullable(3), is(ResultSetMetaData.columnNoNulls)); // propC is NOT multiple
        assertThat(extMetadata.isNullable(4), is(ResultSetMetaData.columnNullableUnknown)); // propD has no table
        assertThat(extMetadata.isNullable(5), is(ResultSetMetaData.columnNoNulls)); // propC is NOT multiple
    }

    @Test
    public void shouldReturnCaseSensitiveWhenResultIsNotExtendedJcrQueryResult() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.isCaseSensitive(i + 1), is(true));
        }
    }

    @Test
    public void shouldReturnCaseSensitiveBasedUponNodeTypeWhenResultIsExtendedJcrQueryResult() {
        assertThat(extMetadata.isCaseSensitive(1), is(true)); // STRING
        assertThat(extMetadata.isCaseSensitive(2), is(false)); // LONG
        assertThat(extMetadata.isCaseSensitive(3), is(true)); // PATH
        assertThat(extMetadata.isCaseSensitive(4), is(false)); // REFERENCE
        assertThat(extMetadata.isCaseSensitive(5), is(false)); // DOUBLE
    }

    @Test
    public void shouldReturnFalseFromSignedWhenResultIsNotExtendedJcrQueryResult() {
        assertThat(metadata.isCaseSensitive(1), is(true));
        assertThat(metadata.isCaseSensitive(2), is(true));
        assertThat(metadata.isCaseSensitive(3), is(true));
        assertThat(metadata.isCaseSensitive(4), is(true));
        assertThat(metadata.isCaseSensitive(5), is(true));
    }

    @Test
    public void shouldReturnSignedBasedUponNodeTypeWhenResultIsExtendedJcrQueryResult() {
        assertThat(extMetadata.isSigned(1), is(false)); // STRING
        assertThat(extMetadata.isSigned(2), is(true)); // LONG
        assertThat(extMetadata.isSigned(3), is(false)); // PATH
        assertThat(extMetadata.isSigned(4), is(false)); // REFERENCE
        assertThat(extMetadata.isSigned(5), is(true)); // DOUBLE
    }

    @Test
    public void shouldAlwaysReturnFalseForIsAutoincrement() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.isAutoIncrement(i + 1), is(false));
            assertThat(extMetadata.isAutoIncrement(i + 1), is(false));
        }
    }

    @Test
    public void shouldAlwaysReturnTrueForIsSearchable() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.isSearchable(i + 1), is(true));
            assertThat(extMetadata.isSearchable(i + 1), is(true));
        }
    }

    @Test
    public void shouldAlwaysReturnFalseForIsCurrency() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.isCurrency(i + 1), is(false));
            assertThat(extMetadata.isCurrency(i + 1), is(false));
        }
    }

    @Test
    public void shouldAlwaysReturnFalseForIsDefinitelyWritable() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.isDefinitelyWritable(i + 1), is(false));
            assertThat(extMetadata.isDefinitelyWritable(i + 1), is(false));
        }
    }

    @Test
    public void shouldAlwaysReturnTrueForIsReadOnly() {
        for (int i = 0; i != columnNames.length; ++i) {
            assertThat(metadata.isReadOnly(i + 1), is(true));
            assertThat(extMetadata.isReadOnly(i + 1), is(true));
        }
    }

    protected void addPropDefn( String nodeTypeName,
                                String propName,
                                boolean isMultiple ) throws SQLException {
        // Find the mock node type ...
        NodeType nodeType = connection.nodeType(nodeTypeName);
        if (nodeType == null) {
            nodeType = Mockito.mock(NodeType.class);
        }

        // Create the new definition ...
        PropertyDefinition defn = Mockito.mock(PropertyDefinition.class);
        when(defn.getName()).thenReturn(propName);
        when(defn.isMandatory()).thenReturn(isMultiple);

        // Get the list of existing property definitions ...
        PropertyDefinition[] defns = nodeType.getPropertyDefinitions();
        if (defns == null) {
            defns = new PropertyDefinition[] {defn};
        } else {
            PropertyDefinition[] newDefns = new PropertyDefinition[defns.length + 1];
            System.arraycopy(defns, 0, newDefns, 0, defns.length);
            newDefns[defns.length] = defn;
            defns = newDefns;
        }
        when(nodeType.getPropertyDefinitions()).thenReturn(defns);
        when(connection.nodeType(nodeTypeName)).thenReturn(nodeType);
    }

}
