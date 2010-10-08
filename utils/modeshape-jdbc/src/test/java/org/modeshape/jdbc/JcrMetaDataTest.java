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
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.QueryResult;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.ModeShapeRoles;
import org.modeshape.jcr.JcrRepository.Option;
import org.modeshape.jdbc.delegate.ConnectionInfo;
import org.modeshape.jdbc.delegate.RepositoryDelegate;
import org.modeshape.jdbc.util.ResultsComparator;

public class JcrMetaDataTest extends ResultsComparator {

    private static JcrEngine engine;
    private static Repository repository;
    private static Session session;
    private static boolean print = false;
    private JcrMetaData metadata;
    @Mock
    private JcrConnection connection;
    @Mock
    private RepositoryDelegate delegate;
    @Mock
    private ConnectionInfo connInfo;
    @Mock
    private QueryResult queryResult;

    // ----------------------------------------------------------------------------------------------------------------
    // Setup/Teardown methods
    // ----------------------------------------------------------------------------------------------------------------

    @BeforeClass
    public static void beforeAll() throws Exception {
        JcrConfiguration configuration = new JcrConfiguration();
        configuration.repositorySource("source").usingClass(InMemoryRepositorySource.class).setDescription("The content store");
        configuration.repository("repo")
                     .setSource("source")
                     .registerNamespace("car", "http://www.modeshape.org/examples/cars/1.0")
                     .addNodeTypes(resourceUrl("cars.cnd"))
                     .setOption(Option.ANONYMOUS_USER_ROLES,
                                ModeShapeRoles.READONLY + "," + ModeShapeRoles.READWRITE + "," + ModeShapeRoles.ADMIN)
                     .setOption(Option.JAAS_LOGIN_CONFIG_NAME, "modeshape-jcr");
        engine = configuration.build();
        engine.start();

        // Start the repository ...
        repository = engine.getRepository("repo");

        // Create the session and load the content ...
        session = repository.login();
        assertImport("cars-system-view-with-uuids.xml", "/");

    }

    @AfterClass
    public static void afterAll() {
        try {
            if (session != null) session.logout();
        } finally {
            session = null;
            try {
                engine.shutdown();
            } finally {
                engine = null;
            }
        }
    }

    @Before
    public void beforeEach() throws RepositoryException {
        MockitoAnnotations.initMocks(this);

        print = false;

        when(connection.getRepositoryDelegate()).thenReturn(delegate);
        Set<String> names = new HashSet<String>();
        names.add("repo");

        when(connection.getCatalog()).thenReturn("repo");

        metadata = new JcrMetaData(connection);

        when(delegate.getConnectionInfo()).thenReturn(connInfo);
        when(delegate.execute(anyString(), anyString())).thenReturn(queryResult);

        when(connInfo.getRepositoryName()).thenReturn("repoName");

        when(queryResult.getColumnNames()).thenReturn(TestUtil.COLUMN_NAMES);
        compareColumns = false;
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

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------------------------------------------------

    protected static URL resourceUrl( String name ) {
        return JcrMetaDataTest.class.getClassLoader().getResource(name);
    }

    protected static InputStream resourceStream( String name ) {
        return JcrMetaDataTest.class.getClassLoader().getResourceAsStream(name);
    }

    protected static Node assertImport( String resourceName,
                                        String pathToParent ) throws RepositoryException, IOException {
        InputStream istream = resourceStream(resourceName);
        return assertImport(istream, pathToParent);
    }

    protected static Node assertImport( File resource,
                                        String pathToParent ) throws RepositoryException, IOException {
        InputStream istream = new FileInputStream(resource);
        return assertImport(istream, pathToParent);
    }

    protected static Node assertImport( InputStream istream,
                                        String pathToParent ) throws RepositoryException, IOException {
        // Make the parent node if it does not exist ...
        Path parentPath = path(pathToParent);
        assertThat(parentPath.isAbsolute(), is(true));
        Node node = session.getRootNode();
        boolean found = true;
        for (Path.Segment segment : parentPath) {
            String name = asString(segment);
            if (found) {
                try {
                    node = node.getNode(name);
                    found = true;
                } catch (PathNotFoundException e) {
                    found = false;
                }
            }
            if (!found) {
                node = node.addNode(name, "nt:unstructured");
            }
        }
        if (!found) {
            // We added at least one node, so we need to save it before importing ...
            session.save();
        }

        // Verify that the parent node does exist now ...
        assertNode(pathToParent);

        // Now, load the content of the resource being imported ...
        assertThat(istream, is(notNullValue()));
        try {
            session.getWorkspace().importXML(pathToParent, istream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
        } finally {
            istream.close();
        }

        session.save();
        return node;
    }

    protected static Path path( String path ) {
        return engine.getExecutionContext().getValueFactories().getPathFactory().create(path);
    }

    protected static String relativePath( String path ) {
        return !path.startsWith("/") ? path : path.substring(1);
    }

    protected static String asString( Object value ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(value);
    }

    protected static void assertNode( String path ) throws RepositoryException {
        // Verify that the parent node does exist now ...
        String relativePath = relativePath(path);
        Node root = session.getRootNode();
        if (relativePath.trim().length() == 0) {
            // This is the root path, so of course it exists ...
            assertThat(root, is(notNullValue()));
            return;
        }
        if (print && !root.hasNode(relativePath)) {
            Node parent = root;
            int depth = 0;
            for (Segment segment : path(path)) {
                if (!parent.hasNode(asString(segment))) {
                    System.out.println("Unable to find '" + path + "'; lowest node is '" + parent.getPath() + "'");
                    break;
                }
                parent = parent.getNode(asString(segment));
                ++depth;
            }
        }
        assertThat(root.hasNode(relativePath), is(true));
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
        expected.put("getDriverMajorVersion", TestUtil.majorVersion());
        expected.put("getDriverMinorVersion", TestUtil.minorVersion());
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
        // TODO: change expected value;
        expected.put("getSQLStateType", new Integer(0));

        // return type -- String
        expected.put("getCatalogSeparator", null);
        expected.put("getCatalogTerm", "Repository");
        // expected.put("getDatabaseProductName", "Teiid Embedded");
        // expected.put("getDatabaseProductVersion", "7.1");
        expected.put("getDriverName", JdbcI18n.driverName.text());
        expected.put("getDriverVersion", JdbcI18n.driverVersion.text());
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
