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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.query.QueryResult;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jdbc.delegate.ConnectionInfo;
import org.modeshape.jdbc.delegate.RepositoryDelegate;

public class JcrMetaDataTest extends MultiUseAbstractTest {

    private JcrMetaData metadata;
    private DriverInfo driverInfo;
    @Mock
    private JcrConnection connection;
    @Mock
    private RepositoryDelegate delegate;
    @Mock
    private ConnectionInfo connInfo;
    @Mock
    private QueryResult queryResult;

    @BeforeClass
    public static void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

        registerNodeTypes("cars.cnd");
        importContent("/", "cars-system-view-with-uuids.xml", ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();

        MockitoAnnotations.initMocks(this);

        driverInfo = new DriverInfo(JdbcLocalI18n.driverName.text(), JdbcLocalI18n.driverVendor.text(),
                                    JdbcLocalI18n.driverVendorUrl.text(), JdbcLocalI18n.driverVersion.text());
        print = false;

        when(connection.getRepositoryDelegate()).thenReturn(delegate);
        Set<String> names = new HashSet<String>();
        names.add("repo");

        when(connection.getCatalog()).thenReturn("repo");
        when(connection.driverInfo()).thenReturn(driverInfo);

        metadata = new JcrMetaData(connection);

        when(delegate.getConnectionInfo()).thenReturn(connInfo);
        when(delegate.execute(anyString(), anyString())).thenReturn(queryResult);

        when(connInfo.getRepositoryName()).thenReturn("repoName");

        when(queryResult.getColumnNames()).thenReturn(TestUtil.COLUMN_NAMES);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Test methods
    // ----------------------------------------------------------------------------------------------------------------

    @Test
    public void shouldHaveSession() {
        assertThat(session, is(notNullValue()));
    }

    @Test
    public void shouldHaveMetaData() {
        assertThat(metadata, is(notNullValue()));
    }

    @Test
    public void shouldImplementGetTables() throws Exception {
        ResultSet result = metadata.getTables(null, null, null, null);
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void shouldImplementGetColumns() throws Exception {
        ResultSet result = metadata.getColumns(null, null, null, null);
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void shouldImplementGetProcedures() throws Exception {
        ResultSet result = metadata.getProcedures(null, null, null);
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void shouldImplementGetImportedKeys() throws Exception {
        ResultSet result = metadata.getImportedKeys(null, null, null);
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void shouldImplementGetExportedKeys() throws Exception {
        ResultSet result = metadata.getExportedKeys(null, null, null);
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void shouldImplementGetUniqueIndexes() throws Exception {
        ResultSet result = metadata.getIndexInfo(null, null, null, true, false);
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void shouldImplementGetNonUniqueIndexes() throws Exception {
        ResultSet result = metadata.getIndexInfo(null, null, null, false, false);
        assertThat(result, is(notNullValue()));
    }

    /**
     * Test all the non-query methods
     * 
     * @throws Exception
     */
    @SuppressWarnings( "unchecked" )
    @Test
    public void testMethodsWithoutParams() throws Exception {
        Class<?> dbmdClass = metadata.getClass();
        // non-query Methods return String, boolean or int
        Method[] methods = dbmdClass.getDeclaredMethods();
        Map<String, Object> expectedMap = new HashMap<String, Object>();

        List<String> failedMessages = new ArrayList<String>();
        expectedMap = getExpected();
        // SYS.out.println(" -- total method == " + methods.length + ", non-query == " + expectedMap.size());
        for (int i = 0; i < methods.length; i++) {
            if (expectedMap.containsKey(methods[i].getName())) {

                Object actualValue = null;
                Object expectedValue = null;
                Object expectedReturn = expectedMap.get(methods[i].getName());
                Object[] params = null;

                if (expectedReturn instanceof List<?>) {
                    // has input parameters
                    List<Object[]> returned = (List<Object[]>)expectedReturn;
                    params = returned.get(1);
                    // SYS.out.println(" params == " + params[0]);
                    expectedValue = returned.get(0);
                    actualValue = methods[i].invoke(metadata, params);
                } else {
                    // without params
                    expectedValue = expectedReturn;
                    actualValue = methods[i].invoke(metadata, new Object[0]);
                }

                if (expectedValue == null || actualValue == null) {
                    if (expectedValue == null && actualValue != null) {
                        failedMessages.add(" Expected doesn't match with actual for method - " + methods[i].getName()
                                           + " expected: <" + expectedValue + "> but was: < " + actualValue + "> ");

                    } else if (expectedValue != null && actualValue == null) {
                        failedMessages.add(" Expected doesn't match with actual for method - " + methods[i].getName()
                                           + " expected: <" + expectedValue + "> but was: < " + actualValue + "> ");

                    }
                } else if (!expectedValue.equals(actualValue)) {
                    failedMessages.add(" Expected doesn't match with actual for method - " + methods[i].getName()
                                       + " expected: <" + expectedValue + "> but was: < " + actualValue + "> ");
                }
            }
        }

        assertThat(failedMessages.toString().trim(), is("[]"));

    }

    /**
     * Test all the methods that throw exception
     * 
     * @throws Exception
     */
    @Test
    public void testMethodsWithExceptions() throws Exception {
        Class<?> metadataClass = metadata.getClass();
        Method[] methods = metadataClass.getDeclaredMethods();

        Map<String, Object> expectedMap = new HashMap<String, Object>(); // none expected
        // SYS.out.println(" -- total method == " + methods.length + ", non-query == " + expectedMap.size());
        for (int i = 0; i < methods.length; i++) {
            if (expectedMap.containsKey(methods[i].getName())) {
                methods[i].invoke(metadata, new Object[0]);
            }
        }
    }

    // ////////////////////Expected Result//////////////////

    // constant
    private static final int NO_LIMIT = JcrMetaData.NO_LIMIT;

    private Map<String, Object> getExpected() {
        Map<String, Object> expected = new HashMap<String, Object>();
        // return type -- boolean
        expected.put("allProceduresAreCallable", Boolean.FALSE);
        expected.put("allTablesAreSelectable", Boolean.FALSE);
        expected.put("doesMaxRowSizeIncludeBlobs", Boolean.FALSE);
        expected.put("isCatalogAtStart", Boolean.TRUE);
        expected.put("isReadOnly", Boolean.TRUE);
        expected.put("locatorsUpdateCopy", Boolean.FALSE);
        expected.put("nullPlusNonNullIsNull", Boolean.FALSE);
        expected.put("nullsAreSortedAtEnd", Boolean.FALSE);
        expected.put("nullsAreSortedAtStart", Boolean.FALSE);
        expected.put("nullsAreSortedHigh", Boolean.FALSE);
        expected.put("nullsAreSortedLow", Boolean.TRUE);
        expected.put("storesLowerCaseIdentifiers", Boolean.FALSE);
        expected.put("storesLowerCaseQuotedIdentifiers", Boolean.FALSE);
        expected.put("storesMixedCaseIdentifiers", Boolean.FALSE);
        expected.put("storesMixedCaseQuotedIdentifiers", Boolean.FALSE);
        expected.put("storesUpperCaseIdentifiers", Boolean.FALSE);
        expected.put("storesUpperCaseQuotedIdentifiers", Boolean.FALSE);
        expected.put("supportsAlterTableWithAddColumn", Boolean.FALSE);
        expected.put("supportsAlterTableWithDropColumn", Boolean.FALSE);
        expected.put("supportsANSI92EntryLevelSQL", Boolean.FALSE);
        expected.put("supportsANSI92FullSQL", Boolean.FALSE);
        expected.put("supportsANSI92IntermediateSQL", Boolean.FALSE);
        expected.put("supportsBatchUpdates", Boolean.FALSE);
        expected.put("supportsCatalogsInDataManipulation", Boolean.FALSE);
        expected.put("supportsCatalogsInIndexDefinitions", Boolean.FALSE);
        expected.put("supportsCatalogsInPrivilegeDefinitions", Boolean.FALSE);
        expected.put("supportsCatalogsInProcedureCalls", Boolean.FALSE);
        expected.put("supportsCatalogsInTableDefinitions", Boolean.FALSE);
        expected.put("supportsColumnAliasing", Boolean.FALSE);
        expected.put("supportsCorrelatedSubqueries", Boolean.FALSE);
        expected.put("supportsCoreSQLGrammar", Boolean.FALSE);
        expected.put("supportsDataDefinitionAndDataManipulationTransactions", Boolean.FALSE);
        expected.put("supportsDataManipulationTransactionsOnly", Boolean.FALSE);
        expected.put("supportsDifferentTableCorrelationNames", Boolean.FALSE);
        expected.put("supportsExpressionsInOrderBy", Boolean.FALSE);
        expected.put("supportsExtendedSQLGrammar", Boolean.FALSE);
        expected.put("supportsFullOuterJoins", Boolean.FALSE);
        expected.put("supportsGetGeneratedKeys", Boolean.FALSE);
        expected.put("supportsGroupBy", Boolean.FALSE);
        expected.put("supportsGroupByBeyondSelect", Boolean.FALSE);
        expected.put("supportsGroupByUnrelated", Boolean.FALSE);
        expected.put("supportsIntegrityEnhancementFacility", Boolean.FALSE);
        expected.put("supportsLikeEscapeClause", Boolean.FALSE);
        expected.put("supportsLimitedOuterJoins", Boolean.FALSE);
        expected.put("supportsMinimumSQLGrammar", Boolean.FALSE);
        expected.put("supportsMixedCaseIdentifiers", Boolean.FALSE);
        expected.put("supportsMixedCaseQuotedIdentifiers", Boolean.FALSE);
        expected.put("supportsOpenCursorsAcrossCommit", Boolean.FALSE);
        expected.put("supportsMultipleResultSets", Boolean.FALSE);
        expected.put("supportsMultipleOpenResults", Boolean.FALSE);
        expected.put("supportsMultipleTransactions", Boolean.FALSE);
        expected.put("supportsNamedParameters", Boolean.FALSE);
        expected.put("supportsNonNullableColumns", Boolean.FALSE);
        expected.put("supportsOpenCursorsAcrossRollback", Boolean.FALSE);
        expected.put("supportsOpenStatementsAcrossCommit", Boolean.FALSE);
        expected.put("supportsOpenStatementsAcrossRollback", Boolean.FALSE);
        expected.put("supportsOrderByUnrelated", Boolean.FALSE);
        expected.put("supportsOuterJoins", Boolean.TRUE);
        expected.put("supportsPositionedDelete", Boolean.FALSE);
        expected.put("supportsPositionedUpdate", Boolean.FALSE);
        expected.put("supportsSavepoints", Boolean.FALSE);
        expected.put("supportsSchemasInDataManipulation", Boolean.FALSE);
        expected.put("supportsSchemasInIndexDefinitions", Boolean.FALSE);
        expected.put("supportsSchemasInPrivilegeDefinitions", Boolean.FALSE);
        expected.put("supportsSchemasInProcedureCalls", Boolean.FALSE);
        expected.put("supportsSchemasInTableDefinitions", Boolean.FALSE);
        expected.put("supportsSelectForUpdate", Boolean.FALSE);
        expected.put("supportsStatementPooling", Boolean.FALSE);
        expected.put("supportsStoredProcedures", Boolean.FALSE);
        expected.put("supportsSubqueriesInComparisons", Boolean.FALSE);
        expected.put("supportsSubqueriesInExists", Boolean.FALSE);
        expected.put("supportsSubqueriesInIns", Boolean.FALSE);
        expected.put("supportsSubqueriesInQuantifieds", Boolean.FALSE);
        expected.put("supportsTableCorrelationNames", Boolean.TRUE);
        expected.put("supportsTransactions", Boolean.FALSE);
        expected.put("supportsUnion", Boolean.FALSE);
        expected.put("supportsUnionAll", Boolean.FALSE);
        expected.put("usesLocalFilePerTable", Boolean.FALSE);
        expected.put("usesLocalFiles", Boolean.FALSE);
        expected.put("usesLocalFilePerTable", Boolean.FALSE);

        // return type -- int
        // expected.put("getDatabaseMinorVersion", new Integer(ApplicationInfo.getInstance().getMinorReleaseVersion()));
        // expected.put("getDatabaseMajorVersion", new Integer(ApplicationInfo.getInstance().getMajorReleaseVersion()));
        expected.put("getJDBCMajorVersion", new Integer(2));
        expected.put("getJDBCMinorVersion", new Integer(0));
        expected.put("getDefaultTransactionIsolation", Connection.TRANSACTION_NONE);
        expected.put("getDriverMajorVersion", TestUtil.majorVersion(JdbcLocalI18n.driverVersion.text()));
        expected.put("getDriverMinorVersion", TestUtil.minorVersion(JdbcLocalI18n.driverVersion.text()));
        expected.put("getMaxBinaryLiteralLength", new Integer(NO_LIMIT));
        expected.put("getMaxCatalogNameLength", new Integer(NO_LIMIT));
        expected.put("getMaxCharLiteralLength", new Integer(NO_LIMIT));
        expected.put("getMaxColumnNameLength", new Integer(NO_LIMIT));
        expected.put("getMaxColumnsInGroupBy", new Integer(NO_LIMIT));
        expected.put("getMaxColumnsInIndex", new Integer(NO_LIMIT));
        expected.put("getMaxColumnsInOrderBy", new Integer(NO_LIMIT));
        expected.put("getMaxColumnsInSelect", new Integer(NO_LIMIT));
        expected.put("getMaxColumnsInTable", new Integer(NO_LIMIT));
        expected.put("getMaxConnections", new Integer(NO_LIMIT));
        expected.put("getMaxCursorNameLength", new Integer(NO_LIMIT));
        expected.put("getMaxIndexLength", new Integer(NO_LIMIT));
        expected.put("getMaxProcedureNameLength", new Integer(0));
        expected.put("getMaxRowSize", new Integer(NO_LIMIT));
        expected.put("getMaxStatementLength", new Integer(NO_LIMIT));
        expected.put("getMaxStatements", new Integer(NO_LIMIT));
        expected.put("getMaxTableNameLength", new Integer(NO_LIMIT));
        expected.put("getMaxTablesInSelect", new Integer(NO_LIMIT));
        expected.put("getMaxUserNameLength", new Integer(NO_LIMIT));
        expected.put("getSQLStateType", new Integer(0));

        // return type -- String
        expected.put("getCatalogSeparator", null);
        expected.put("getCatalogTerm", "Repository");
        // expected.put("getDatabaseProductName", "Teiid Embedded");
        // expected.put("getDatabaseProductVersion", "7.1");
        expected.put("getDriverName", JdbcLocalI18n.driverName.text());
        expected.put("getDriverVersion", JdbcLocalI18n.driverVersion.text());
        // expected.put("getExtraNameCharacters", ".@");
        expected.put("getIdentifierQuoteString", "\"");
        // expected.put("getNumericFunctions", DatabaseMetaDataImpl.NUMERIC_FUNCTIONS);
        // expected.put("getSearchStringEscape", "\\");
        // expected.put("getSQLKeywords", DatabaseMetaDataImpl.KEY_WORDS);
        // expected.put("getStringFunctions", DatabaseMetaDataImpl.STRING_FUNCTIONS);
        // expected.put("getSystemFunctions", DatabaseMetaDataImpl.SYSTEM_FUNCTIONS);
        // expected.put("getTimeDateFunctions", DatabaseMetaDataImpl.DATE_FUNCTIONS);
        // expected.put("getUrl", primaryUrl + serverUrl);
        // expected.put("getUserName", CoreConstants.DEFAULT_ANON_USERNAME);

        // ========== NOT SUPPORTED ======//
        // expected.put("getProcedureTerm", "StoredProcedure");
        // expected.put("getSchemaTerm", "Schema");
        // expected.put("getMaxSchemaNameLength", new Integer(NO_LIMIT));

        return expected;
    }

}
