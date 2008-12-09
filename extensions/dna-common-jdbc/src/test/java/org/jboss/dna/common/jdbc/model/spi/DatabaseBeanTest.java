/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.jdbc.model.spi;

import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.jboss.dna.common.jdbc.model.DefaultModelFactory;
import org.jboss.dna.common.jdbc.model.ModelFactory;
import org.jboss.dna.common.jdbc.model.api.Catalog;
import org.jboss.dna.common.jdbc.model.api.Database;
import org.jboss.dna.common.jdbc.model.api.DatabaseMetaDataMethodException;
import org.jboss.dna.common.jdbc.model.api.ResultSetConcurrencyType;
import org.jboss.dna.common.jdbc.model.api.ResultSetHoldabilityType;
import org.jboss.dna.common.jdbc.model.api.ResultSetType;
import org.jboss.dna.common.jdbc.model.api.SQLStateType;
import org.jboss.dna.common.jdbc.model.api.Schema;
import org.jboss.dna.common.jdbc.model.api.SqlType;
import org.jboss.dna.common.jdbc.model.api.SqlTypeConversionPair;
import org.jboss.dna.common.jdbc.model.api.SqlTypeInfo;
import org.jboss.dna.common.jdbc.model.api.StoredProcedure;
import org.jboss.dna.common.jdbc.model.api.Table;
import org.jboss.dna.common.jdbc.model.api.TableType;
import org.jboss.dna.common.jdbc.model.api.TransactionIsolationLevelType;
import org.jboss.dna.common.jdbc.model.api.UserDefinedType;

/**
 * DatabaseBean test
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DatabaseBeanTest extends TestCase {

    private Database bean;
    private ModelFactory factory;

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // create
        bean = new DatabaseBean();
        factory = new DefaultModelFactory();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        // release
        bean = null;
        factory = null;
        super.tearDown();
    }

    /**
     * testDatabaseMetadataExceptions
     */
    public void testGetExceptions() {
        // get list of exceptions
        List<DatabaseMetaDataMethodException> exceptionList = bean.getExceptionList();

        // check that the exception list in not null
        assertNotNull("Database Metadata Method Exception List should not be null", exceptionList);

        // check that exception list is empty
        assertTrue("Database Metadata Method Exception List should be empty", exceptionList.isEmpty());

    }

    /**
     * addDatabaseMetaDataMethodException
     */
    public void testAddException() {
        String MESSAGE = "Database MetaData Method Exception";
        String METHOD_NAME = "myMethod";

        // create DatabaseMetaDataMethodException
        DatabaseMetaDataMethodException exception = new DatabaseMetaDataMethodException(MESSAGE, METHOD_NAME, null);
        // add Exception to error list
        bean.addException(exception);

        // get list of exceptions
        List<DatabaseMetaDataMethodException> exceptionList = bean.getExceptionList();
        // check that the exception added to the list
        assertTrue("Unable to add DatabaseMetaDataMethodException to the error list for provider ",
                   exceptionList.contains(exception));
    }

    /**
     * findDatabaseMetaDataMethodException
     */
    public void testFindException() {
        String MESSAGE = "Database MetaData Method Exception";
        String METHOD_NAME = "myMethod";

        // create DatabaseMetaDataMethodException
        DatabaseMetaDataMethodException exception = new DatabaseMetaDataMethodException(MESSAGE, METHOD_NAME, null);
        // add Exception to error list
        bean.addException(exception);

        // check that the exception found
        assertSame("Unable to find database metadata method exception", exception, bean.findException(METHOD_NAME));
    }

    /**
     * isDatabaseMetaDataMethodFailed
     */
    public void testDatabaseMetaDataMethodFailed() {
        String MESSAGE = "Database MetaData Method Exception";
        String METHOD_NAME = "myMethod";

        // create DatabaseMetaDataMethodException
        DatabaseMetaDataMethodException exception = new DatabaseMetaDataMethodException(MESSAGE, METHOD_NAME, null);
        // add Exception to error list
        bean.addException(exception);

        // check that the exception found
        assertTrue("The database metadata method " + METHOD_NAME + " should be failed",
                   bean.isDatabaseMetaDataMethodFailed(METHOD_NAME));

    }

    public void testSetName() {
        String VALUE = "My name";
        // set
        bean.setName(VALUE);
        // check
        assertSame(VALUE, bean.getName());
    }

    public void testSetAllProceduresAreCallable() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setAllProceduresAreCallable(VALUE);
        // check
        assertSame(VALUE, bean.isAllProceduresAreCallable());
    }

    public void testSetAllTablesAreSelectable() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setAllTablesAreSelectable(VALUE);
        // check
        assertSame(VALUE, bean.isAllTablesAreSelectable());
    }

    public void testSetURL() {
        String VALUE = "My URL";
        // set
        bean.setURL(VALUE);
        // check
        assertSame(VALUE, bean.getURL());
    }

    public void testSetUserName() {
        String VALUE = "My name";
        // set
        bean.setUserName(VALUE);
        // check
        assertSame(VALUE, bean.getUserName());
    }

    public void testSetReadOnly() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setReadOnly(VALUE);
        // check
        assertSame(VALUE, bean.isReadOnly());
    }

    public void testSetNullsAreSortedHigh() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setNullsAreSortedHigh(VALUE);
        // check
        assertSame(VALUE, bean.isNullsAreSortedHigh());
    }

    public void testSetNullsAreSortedLow() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setNullsAreSortedLow(VALUE);
        // check
        assertSame(VALUE, bean.isNullsAreSortedLow());
    }

    public void testSetNullsAreSortedAtStart() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setNullsAreSortedAtStart(VALUE);
        // check
        assertSame(VALUE, bean.isNullsAreSortedAtStart());
    }

    public void testSetNullsAreSortedAtEnd() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setNullsAreSortedAtEnd(VALUE);
        // check
        assertSame(VALUE, bean.isNullsAreSortedAtEnd());
    }

    public void testSetDatabaseProductName() {
        String VALUE = "My product name";
        // set
        bean.setDatabaseProductName(VALUE);
        // check
        assertSame(VALUE, bean.getDatabaseProductName());
    }

    public void testSetDatabaseProductVersion() {
        String VALUE = "My version";
        // set
        bean.setDatabaseProductVersion(VALUE);
        // check
        assertSame(VALUE, bean.getDatabaseProductVersion());
    }

    public void testSetDriverName() {
        String VALUE = "My driver name";
        // set
        bean.setDriverName(VALUE);
        // check
        assertSame(VALUE, bean.getDriverName());
    }

    public void testSetDriverVersion() {
        String VALUE = "My driver version";
        // set
        bean.setDriverVersion(VALUE);
        // check
        assertSame(VALUE, bean.getDriverVersion());
    }

    public void testSetDriverMajorVersion() {
        Integer VALUE = new Integer(1);
        // set
        bean.setDriverMajorVersion(VALUE);
        // check
        assertSame(VALUE, bean.getDriverMajorVersion());
    }

    public void testSetDriverMinorVersion() {
        Integer VALUE = new Integer(1);
        // set
        bean.setDriverMinorVersion(VALUE);
        // check
        assertSame(VALUE, bean.getDriverMinorVersion());
    }

    public void testSetUsesLocalFiles() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setUsesLocalFiles(VALUE);
        // check
        assertSame(VALUE, bean.isUsesLocalFiles());
    }

    public void testSetUsesLocalFilePerTable() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setUsesLocalFilePerTable(VALUE);
        // check
        assertSame(VALUE, bean.isUsesLocalFilePerTable());
    }

    public void testSetSupportsMixedCaseIdentifiers() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsMixedCaseIdentifiers(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsMixedCaseIdentifiers());
    }

    public void testSetStoresUpperCaseIdentifiers() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setStoresUpperCaseIdentifiers(VALUE);
        // check
        assertSame(VALUE, bean.isStoresUpperCaseIdentifiers());
    }

    public void testSetStoresLowerCaseIdentifiers() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setStoresLowerCaseIdentifiers(VALUE);
        // check
        assertSame(VALUE, bean.isStoresLowerCaseIdentifiers());
    }

    public void testSetStoresMixedCaseIdentifiers() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setStoresMixedCaseIdentifiers(VALUE);
        // check
        assertSame(VALUE, bean.isStoresMixedCaseIdentifiers());
    }

    public void testSetSupportsMixedCaseQuotedIdentifiers() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsMixedCaseQuotedIdentifiers(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsMixedCaseQuotedIdentifiers());
    }

    public void testSetStoresUpperCaseQuotedIdentifiers() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setStoresUpperCaseQuotedIdentifiers(VALUE);
        // check
        assertSame(VALUE, bean.isStoresUpperCaseQuotedIdentifiers());
    }

    public void testSetStoresLowerCaseQuotedIdentifiers() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setStoresLowerCaseQuotedIdentifiers(VALUE);
        // check
        assertSame(VALUE, bean.isStoresLowerCaseQuotedIdentifiers());
    }

    public void testSetStoresMixedCaseQuotedIdentifiers() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setStoresMixedCaseQuotedIdentifiers(VALUE);
        // check
        assertSame(VALUE, bean.isStoresMixedCaseQuotedIdentifiers());
    }

    public void testSetIdentifierQuoteString() {
        String VALUE = "My id";
        // set
        bean.setIdentifierQuoteString(VALUE);
        // check
        assertSame(VALUE, bean.getIdentifierQuoteString());
    }

    public void testGetSQLKeywords() {
        Set<String> set = bean.getSQLKeywords();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSQLKeyword() {
        String NAME = "My name";
        // add
        bean.addSQLKeyword(NAME);
        // check
        assertFalse(bean.getSQLKeywords().isEmpty());
    }

    public void testDeleteSQLKeyword() {
        String NAME = "My name";
        // add
        bean.addSQLKeyword(NAME);
        // check
        assertFalse(bean.getSQLKeywords().isEmpty());

        // delete
        bean.deleteSQLKeyword(NAME);
        // check
        assertTrue(bean.getSQLKeywords().isEmpty());
    }

    public void testIsSQLKeywordSupported() {
        String NAME = "My name";
        // add
        bean.addSQLKeyword(NAME);

        // check
        assertSame(Boolean.TRUE, bean.isSQLKeywordSupported(NAME));
    }

    public void testGetNumericFunctions() {
        // get
        Set<String> set = bean.getNumericFunctions();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddNumericFunction() {
        String NAME = "My name";
        // add
        bean.addNumericFunction(NAME);
        // check
        assertFalse(bean.getNumericFunctions().isEmpty());
    }

    public void testDeleteNumericFunction() {
        String NAME = "My name";
        // add
        bean.addNumericFunction(NAME);
        // check
        assertFalse(bean.getNumericFunctions().isEmpty());

        // delete
        bean.deleteNumericFunction(NAME);
        // check
        assertTrue(bean.getNumericFunctions().isEmpty());
    }

    public void testIsNumericFunctionSupported() {
        String NAME = "My name";
        // add
        bean.addNumericFunction(NAME);

        // check
        assertSame(Boolean.TRUE, bean.isNumericFunctionSupported(NAME));
    }

    public void testGetStringFunctions() {
        // get
        Set<String> set = bean.getStringFunctions();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddStringFunction() {
        String NAME = "My name";
        // add
        bean.addStringFunction(NAME);
        // check
        assertFalse(bean.getStringFunctions().isEmpty());
    }

    public void testDeleteStringFunction() {
        String NAME = "My name";
        // add
        bean.addStringFunction(NAME);
        // check
        assertFalse(bean.getStringFunctions().isEmpty());

        // delete
        bean.deleteStringFunction(NAME);
        // check
        assertTrue(bean.getStringFunctions().isEmpty());
    }

    public void testIsStringFunctionSupported() {
        String NAME = "My name";
        // add
        bean.addStringFunction(NAME);

        // check
        assertSame(Boolean.TRUE, bean.isStringFunctionSupported(NAME));
    }

    public void testGetSystemFunctions() {
        // get
        Set<String> set = bean.getSystemFunctions();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSystemFunction() {
        String NAME = "My name";
        // add
        bean.addSystemFunction(NAME);
        // check
        assertFalse(bean.getSystemFunctions().isEmpty());
    }

    public void testDeleteSystemFunction() {
        String NAME = "My name";
        // add
        bean.addSystemFunction(NAME);
        // check
        assertFalse(bean.getSystemFunctions().isEmpty());

        // delete
        bean.deleteSystemFunction(NAME);
        // check
        assertTrue(bean.getSystemFunctions().isEmpty());
    }

    public void testIsSystemFunctionSupported() {
        String NAME = "My name";
        // add
        bean.addSystemFunction(NAME);

        // check
        assertSame(Boolean.TRUE, bean.isSystemFunctionSupported(NAME));
    }

    public void testGetTimeDateFunctions() {
        // get
        Set<String> set = bean.getTimeDateFunctions();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddTimeDateFunction() {
        String NAME = "My name";
        // add
        bean.addTimeDateFunction(NAME);
        // check
        assertFalse(bean.getTimeDateFunctions().isEmpty());
    }

    public void testDeleteTimeDateFunction() {
        String NAME = "My name";
        // add
        bean.addTimeDateFunction(NAME);
        // check
        assertFalse(bean.getTimeDateFunctions().isEmpty());

        // delete
        bean.deleteTimeDateFunction(NAME);
        // check
        assertTrue(bean.getTimeDateFunctions().isEmpty());
    }

    public void testIsTimeDateFunctionSupported() {
        String NAME = "My name";
        // add
        bean.addTimeDateFunction(NAME);

        // check
        assertSame(Boolean.TRUE, bean.isTimeDateFunctionSupported(NAME));
    }

    public void testSetSearchStringEscape() {
        String VALUE = "My search";
        // set
        bean.setSearchStringEscape(VALUE);
        // check
        assertSame(VALUE, bean.getSearchStringEscape());
    }

    public void testSetExtraNameCharacters() {
        String VALUE = "My characters";
        // set
        bean.setExtraNameCharacters(VALUE);
        // check
        assertSame(VALUE, bean.getExtraNameCharacters());
    }

    public void testSetSupportsAlterTableWithAddColumn() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsAlterTableWithAddColumn(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsAlterTableWithAddColumn());
    }

    public void testSetSupportsAlterTableWithDropColumn() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsAlterTableWithDropColumn(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsAlterTableWithDropColumn());
    }

    public void testSetSupportsColumnAliasing() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsColumnAliasing(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsColumnAliasing());
    }

    public void testSetNullPlusNonNullIsNull() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setNullPlusNonNullIsNull(VALUE);
        // check
        assertSame(VALUE, bean.isNullPlusNonNullIsNull());
    }

    public void testSetSupportsConvert() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsConvert(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsConvert());
    }

    public void testGetSupportedConversions() {
        // get
        Set<SqlTypeConversionPair> set = bean.getSupportedConversions();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSqlTypeConversionPair() {
        // create
        SqlTypeConversionPair object = new DefaultModelFactory().createSqlTypeConversionPair();
        // set
        object.setSrcType(SqlType.INTEGER);
        object.setDestType(SqlType.VARCHAR);

        // add
        bean.addSqlTypeConversionPair(object);
        // check
        assertFalse(bean.getSupportedConversions().isEmpty());
    }

    public void testDeleteSqlTypeConversionPair() {
        // create
        SqlTypeConversionPair object = new DefaultModelFactory().createSqlTypeConversionPair();
        // set
        object.setSrcType(SqlType.INTEGER);
        object.setDestType(SqlType.VARCHAR);

        // add
        bean.addSqlTypeConversionPair(object);
        // check
        assertFalse(bean.getSupportedConversions().isEmpty());

        // delete
        bean.deleteSqlTypeConversionPair(object);
        // check
        assertTrue(bean.getSupportedConversions().isEmpty());
    }

    public void testFindSqlTypeConversionPair() {
        // create
        SqlTypeConversionPair object = new DefaultModelFactory().createSqlTypeConversionPair();
        // set
        object.setSrcType(SqlType.INTEGER);
        object.setDestType(SqlType.VARCHAR);

        // add
        bean.addSqlTypeConversionPair(object);
        // check
        assertFalse(bean.findSqlTypeConversionPairBySrcType(object.getSrcType().toString()).isEmpty());
    }

    public void testSetSupportsTableCorrelationNames() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsTableCorrelationNames(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsTableCorrelationNames());
    }

    public void testSetSupportsDifferentTableCorrelationNames() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsDifferentTableCorrelationNames(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsDifferentTableCorrelationNames());
    }

    public void testSetSupportsExpressionsInOrderBy() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsExpressionsInOrderBy(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsExpressionsInOrderBy());
    }

    public void testSetSupportsOrderByUnrelated() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsOrderByUnrelated(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsOrderByUnrelated());
    }

    public void testSetSupportsGroupBy() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsGroupBy(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsGroupBy());
    }

    public void testSetSupportsGroupByUnrelated() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsGroupByUnrelated(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsGroupByUnrelated());
    }

    public void testSetSupportsGroupByBeyondSelect() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsGroupByBeyondSelect(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsGroupByBeyondSelect());
    }

    public void testSetSupportsLikeEscapeClause() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsLikeEscapeClause(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsLikeEscapeClause());
    }

    public void testSetSupportsMultipleResultSets() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsMultipleResultSets(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsMultipleResultSets());
    }

    public void testSetSupportsMultipleTransactions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsMultipleTransactions(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsMultipleTransactions());
    }

    public void testSetSupportsNonNullableColumns() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsNonNullableColumns(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsNonNullableColumns());
    }

    public void testSetSupportsMinimumSQLGrammar() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsMinimumSQLGrammar(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsMinimumSQLGrammar());
    }

    public void testSetSupportsCoreSQLGrammar() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsCoreSQLGrammar(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsCoreSQLGrammar());
    }

    public void testSetSupportsExtendedSQLGrammar() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsExtendedSQLGrammar(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsExtendedSQLGrammar());
    }

    public void testSetSupportsANSI92EntryLevelSQL() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsANSI92EntryLevelSQL(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsANSI92EntryLevelSQL());
    }

    public void testSetSupportsANSI92IntermediateSQL() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsANSI92IntermediateSQL(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsANSI92IntermediateSQL());
    }

    public void testSetSupportsANSI92FullSQL() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsANSI92FullSQL(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsANSI92FullSQL());
    }

    public void testSetSupportsIntegrityEnhancementFacility() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsIntegrityEnhancementFacility(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsIntegrityEnhancementFacility());
    }

    public void testSetSupportsOuterJoins() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsOuterJoins(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsOuterJoins());
    }

    public void testSetSupportsFullOuterJoins() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsFullOuterJoins(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsFullOuterJoins());
    }

    public void testSetSupportsLimitedOuterJoins() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsLimitedOuterJoins(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsLimitedOuterJoins());
    }

    public void testSetSchemaTerm() {
        String VALUE = "My value";
        // set
        bean.setSchemaTerm(VALUE);
        // check
        assertSame(VALUE, bean.getSchemaTerm());
    }

    public void testSetProcedureTerm() {
        String VALUE = "My value";
        // set
        bean.setProcedureTerm(VALUE);
        // check
        assertSame(VALUE, bean.getProcedureTerm());
    }

    public void testSetCatalogTerm() {
        String VALUE = "My value";
        // set
        bean.setCatalogTerm(VALUE);
        // check
        assertSame(VALUE, bean.getCatalogTerm());
    }

    public void testSetCatalogAtStart() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setCatalogAtStart(VALUE);
        // check
        assertSame(VALUE, bean.isCatalogAtStart());
    }

    public void testSetCatalogSeparator() {
        String VALUE = "My value";
        // set
        bean.setCatalogSeparator(VALUE);
        // check
        assertSame(VALUE, bean.getCatalogSeparator());
    }

    public void testSetSupportsSchemasInDataManipulation() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSchemasInDataManipulation(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSchemasInDataManipulation());
    }

    public void testSetSupportsSchemasInProcedureCalls() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSchemasInProcedureCalls(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSchemasInProcedureCalls());
    }

    public void testSetSupportsSchemasInTableDefinitions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSchemasInTableDefinitions(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSchemasInTableDefinitions());
    }

    public void testSetSupportsSchemasInIndexDefinitions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSchemasInIndexDefinitions(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSchemasInIndexDefinitions());
    }

    public void testSetSupportsSchemasInPrivilegeDefinitions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSchemasInPrivilegeDefinitions(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSchemasInPrivilegeDefinitions());
    }

    public void testSetSupportsCatalogsInDataManipulation() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsCatalogsInDataManipulation(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsCatalogsInDataManipulation());
    }

    public void testSetSupportsCatalogsInProcedureCalls() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsCatalogsInProcedureCalls(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsCatalogsInProcedureCalls());
    }

    public void testSetSupportsCatalogsInTableDefinitions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsCatalogsInTableDefinitions(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsCatalogsInTableDefinitions());
    }

    public void testSetSupportsCatalogsInIndexDefinitions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsCatalogsInIndexDefinitions(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsCatalogsInIndexDefinitions());
    }

    public void testSetSupportsCatalogsInPrivilegeDefinitions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsCatalogsInPrivilegeDefinitions(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsCatalogsInPrivilegeDefinitions());
    }

    public void testSetSupportsPositionedDelete() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsPositionedDelete(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsPositionedDelete());
    }

    public void testSetSupportsPositionedUpdate() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsPositionedUpdate(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsPositionedUpdate());
    }

    public void testSetSupportsSelectForUpdate() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSelectForUpdate(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSelectForUpdate());
    }

    public void testSetSupportsStoredProcedures() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsStoredProcedures(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsStoredProcedures());
    }

    public void testSetSupportsSubqueriesInComparisons() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSubqueriesInComparisons(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSubqueriesInComparisons());
    }

    public void testSetSupportsSubqueriesInExists() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSubqueriesInExists(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSubqueriesInExists());
    }

    public void testSetSupportsSubqueriesInIns() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSubqueriesInIns(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSubqueriesInIns());
    }

    public void testSetSupportsSubqueriesInQuantifieds() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSubqueriesInQuantifieds(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSubqueriesInQuantifieds());
    }

    public void testSetSupportsCorrelatedSubqueries() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsCorrelatedSubqueries(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsCorrelatedSubqueries());
    }

    public void testSetSupportsUnion() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsUnion(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsUnion());
    }

    public void testSetSupportsUnionAll() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsUnionAll(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsUnionAll());
    }

    public void testSetSupportsOpenCursorsAcrossCommit() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsOpenCursorsAcrossCommit(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsOpenCursorsAcrossCommit());
    }

    public void testSetSupportsOpenCursorsAcrossRollback() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsOpenCursorsAcrossRollback(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsOpenCursorsAcrossRollback());
    }

    public void testSetSupportsOpenStatementsAcrossCommit() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsOpenStatementsAcrossCommit(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsOpenStatementsAcrossCommit());
    }

    public void testSetSupportsOpenStatementsAcrossRollback() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsOpenStatementsAcrossRollback(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsOpenStatementsAcrossRollback());
    }

    public void testSetMaxBinaryLiteralLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxBinaryLiteralLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxBinaryLiteralLength());
    }

    public void testSetMaxCharLiteralLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxCharLiteralLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxCharLiteralLength());
    }

    public void testSetMaxColumnNameLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxColumnNameLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxColumnNameLength());
    }

    public void testSetMaxColumnsInGroupBy() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxColumnsInGroupBy(VALUE);
        // check
        assertSame(VALUE, bean.getMaxColumnsInGroupBy());
    }

    public void testSetMaxColumnsInIndex() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxColumnsInIndex(VALUE);
        // check
        assertSame(VALUE, bean.getMaxColumnsInIndex());
    }

    public void testSetMaxColumnsInOrderBy() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxColumnsInOrderBy(VALUE);
        // check
        assertSame(VALUE, bean.getMaxColumnsInOrderBy());
    }

    public void testSetMaxColumnsInSelect() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxColumnsInSelect(VALUE);
        // check
        assertSame(VALUE, bean.getMaxColumnsInSelect());
    }

    public void testSetMaxColumnsInTable() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxColumnsInTable(VALUE);
        // check
        assertSame(VALUE, bean.getMaxColumnsInTable());
    }

    public void testSetMaxConnections() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxConnections(VALUE);
        // check
        assertSame(VALUE, bean.getMaxConnections());
    }

    public void testSetMaxCursorNameLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxCursorNameLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxCursorNameLength());
    }

    public void testSetMaxIndexLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxIndexLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxIndexLength());
    }

    public void testSetMaxSchemaNameLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxSchemaNameLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxSchemaNameLength());
    }

    public void testSetMaxProcedureNameLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxProcedureNameLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxProcedureNameLength());
    }

    public void testSetMaxCatalogNameLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxCatalogNameLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxCatalogNameLength());
    }

    public void testSetMaxRowSize() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxRowSize(VALUE);
        // check
        assertSame(VALUE, bean.getMaxRowSize());
    }

    public void testSetMaxRowSizeIncludeBlobs() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setMaxRowSizeIncludeBlobs(VALUE);
        // check
        assertSame(VALUE, bean.isMaxRowSizeIncludeBlobs());
    }

    public void testSetMaxStatementLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxStatementLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxStatementLength());
    }

    public void testSetMaxStatements() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxStatements(VALUE);
        // check
        assertSame(VALUE, bean.getMaxStatements());
    }

    public void testSetMaxTableNameLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxTableNameLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxTableNameLength());
    }

    public void testSetMaxTablesInSelect() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxTablesInSelect(VALUE);
        // check
        assertSame(VALUE, bean.getMaxTablesInSelect());
    }

    public void testSetMaxUserNameLength() {
        Integer VALUE = new Integer(1);
        // set
        bean.setMaxUserNameLength(VALUE);
        // check
        assertSame(VALUE, bean.getMaxUserNameLength());
    }

    public void testSetDefaultTransactionIsolation() {
        Integer VALUE = new Integer(1);
        // set
        bean.setDefaultTransactionIsolation(VALUE);
        // check
        assertSame(VALUE, bean.getDefaultTransactionIsolation());
    }

    public void testSetSupportsTransactions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsTransactions(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsTransactions());
    }

    public void testGetSupportedTransactionIsolationLevels() {
        // get
        Set<TransactionIsolationLevelType> set = bean.getSupportedTransactionIsolationLevels();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSupportedTransactionIsolationLevelType() {
        // get
        TransactionIsolationLevelType object = TransactionIsolationLevelType.READ_COMMITTED;
        // add
        bean.addSupportedTransactionIsolationLevelType(object);
        // check
        assertFalse(bean.getSupportedTransactionIsolationLevels().isEmpty());
    }

    public void testDeleteSupportedTransactionIsolationLevelType() {
        // get
        TransactionIsolationLevelType object = TransactionIsolationLevelType.READ_COMMITTED;
        // add
        bean.addSupportedTransactionIsolationLevelType(object);
        // check
        assertFalse(bean.getSupportedTransactionIsolationLevels().isEmpty());

        // delete
        bean.deleteSupportedTransactionIsolationLevelType(object);
        // check
        assertTrue(bean.getSupportedTransactionIsolationLevels().isEmpty());
    }

    public void testIsSupportedTransactionIsolationLevelType() {
        // get
        TransactionIsolationLevelType object = TransactionIsolationLevelType.READ_COMMITTED;
        // add
        bean.addSupportedTransactionIsolationLevelType(object);
        // check
        assertSame(Boolean.TRUE, bean.isSupportedTransactionIsolationLevelType(object));
    }

    public void testSetSupportsDataDefinitionAndDataManipulationTransactions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsDataDefinitionAndDataManipulationTransactions(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsDataDefinitionAndDataManipulationTransactions());
    }

    public void testSetSupportsDataManipulationTransactionsOnly() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsDataManipulationTransactionsOnly(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsDataManipulationTransactionsOnly());
    }

    public void testSetDataDefinitionCausesTransactionCommit() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setDataDefinitionCausesTransactionCommit(VALUE);
        // check
        assertSame(VALUE, bean.isDataDefinitionCausesTransactionCommit());
    }

    public void testSetDataDefinitionIgnoredInTransactions() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setDataDefinitionIgnoredInTransactions(VALUE);
        // check
        assertSame(VALUE, bean.isDataDefinitionIgnoredInTransactions());
    }

    public void testGetStoredProcedures() {
        // get
        Set<StoredProcedure> set = bean.getStoredProcedures();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddStoredProcedure() {
        String NAME = "My Name";
        // create
        StoredProcedure object = new DefaultModelFactory().createStoredProcedure();
        // set
        object.setName(NAME);
        // add
        bean.addStoredProcedure(object);
        // check
        assertFalse(bean.getStoredProcedures().isEmpty());
    }

    public void testDeleteStoredProcedure() {
        String NAME = "My Name";
        // create
        StoredProcedure object = new DefaultModelFactory().createStoredProcedure();
        // set
        object.setName(NAME);
        // add
        bean.addStoredProcedure(object);
        // check
        assertFalse(bean.getStoredProcedures().isEmpty());

        // delete
        bean.deleteStoredProcedure(object);
        // check
        assertTrue(bean.getStoredProcedures().isEmpty());
    }

    public void testFindStoredProcedureByName() {
        String NAME = "My Name";
        // create
        StoredProcedure object = new DefaultModelFactory().createStoredProcedure();
        // set
        object.setName(NAME);
        // add
        bean.addStoredProcedure(object);
        // check
        assertSame(object, bean.findStoredProcedureByName(null, null, object.getName()));
    }

    public void testGetTables() {
        // get
        Set<Table> set = bean.getTables();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddTable() {
        String NAME = "My Name";
        // create
        Table object = new DefaultModelFactory().createTable();
        // set
        object.setName(NAME);
        // add
        bean.addTable(object);
        // check
        assertFalse(bean.getTables().isEmpty());
    }

    public void testDeleteTable() {
        String NAME = "My Name";
        // create
        Table object = new DefaultModelFactory().createTable();
        // set
        object.setName(NAME);
        // add
        bean.addTable(object);
        // check
        assertFalse(bean.getTables().isEmpty());

        // delete
        bean.deleteTable(object);
        // check
        assertTrue(bean.getTables().isEmpty());
    }

    public void testFindTableByName() {
        String NAME = "My Name";
        // create
        Table object = new DefaultModelFactory().createTable();
        // set
        object.setName(NAME);
        // add
        bean.addTable(object);
        // check
        assertSame(object, bean.findTableByName(null, null, object.getName()));
    }

    public void testGetSchemas() {
        // get
        Set<Schema> set = bean.getSchemas();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSchema() {
        String NAME = "My Name";
        // create
        Schema object = new DefaultModelFactory().createSchema();
        // set
        object.setName(NAME);
        // add
        bean.addSchema(object);
        // check
        assertFalse(bean.getSchemas().isEmpty());
    }

    public void testDeleteSchema() {
        String NAME = "My Name";
        // create
        Schema object = new DefaultModelFactory().createSchema();
        // set
        object.setName(NAME);
        // add
        bean.addSchema(object);
        // check
        assertFalse(bean.getSchemas().isEmpty());

        // delete
        bean.deleteSchema(object);
        // check
        assertTrue(bean.getSchemas().isEmpty());
    }

    public void testFindSchemaByName() {
        String NAME = "My Name";
        // create
        Schema object = new DefaultModelFactory().createSchema();
        // set
        object.setName(NAME);
        // add
        bean.addSchema(object);
        // check
        assertSame(object, bean.findSchemaByName(null, object.getName()));
    }

    public void testGetCatalogs() {
        // get
        Set<Catalog> set = bean.getCatalogs();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddCatalog() {
        String NAME = "My Name";
        // create
        Catalog object = new DefaultModelFactory().createCatalog();
        // set
        object.setName(NAME);
        // add
        bean.addCatalog(object);
        // check
        assertFalse(bean.getCatalogs().isEmpty());
    }

    public void testDeleteCatalog() {
        String NAME = "My Name";
        // create
        Catalog object = new DefaultModelFactory().createCatalog();
        // set
        object.setName(NAME);
        // add
        bean.addCatalog(object);
        // check
        assertFalse(bean.getCatalogs().isEmpty());

        // delete
        bean.deleteCatalog(object);
        // check
        assertTrue(bean.getCatalogs().isEmpty());
    }

    public void testFindCatalogByName() {
        String NAME = "My Name";
        // create
        Catalog object = new DefaultModelFactory().createCatalog();
        // set
        object.setName(NAME);
        // add
        bean.addCatalog(object);
        // check
        assertSame(object, bean.findCatalogByName(object.getName()));
    }

    public void testGetTableTypes() {
        // get
        Set<TableType> set = bean.getTableTypes();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddTableType() {
        String NAME = "My Name";
        // create
        TableType object = new DefaultModelFactory().createTableType();
        // set
        object.setName(NAME);
        // add
        bean.addTableType(object);
        // check
        assertFalse(bean.getTableTypes().isEmpty());
    }

    public void testDeleteTableType() {
        String NAME = "My Name";
        // create
        TableType object = new DefaultModelFactory().createTableType();
        // set
        object.setName(NAME);
        // add
        bean.addTableType(object);
        // check
        assertFalse(bean.getTableTypes().isEmpty());

        // delete
        bean.deleteTableType(object);
        // check
        assertTrue(bean.getTableTypes().isEmpty());
    }

    public void testFindTableTypeByTypeName() {
        String NAME = "My Name";
        // create
        TableType object = new DefaultModelFactory().createTableType();
        // set
        object.setName(NAME);
        // add
        bean.addTableType(object);
        // check
        assertSame(object, bean.findTableTypeByTypeName(object.getName()));
    }

    public void testGetSqlTypeInfos() {
        // get
        Set<SqlTypeInfo> set = bean.getSqlTypeInfos();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSqlTypeInfo() {
        String NAME = "My Name";
        // create
        SqlTypeInfo object = new DefaultModelFactory().createSqlTypeInfo();
        // set
        object.setName(NAME);
        // add
        bean.addSqlTypeInfo(object);
        // check
        assertFalse(bean.getSqlTypeInfos().isEmpty());
    }

    public void testDeleteSqlTypeInfo() {
        String NAME = "My Name";
        // create
        SqlTypeInfo object = new DefaultModelFactory().createSqlTypeInfo();
        // set
        object.setName(NAME);
        // add
        bean.addSqlTypeInfo(object);
        // check
        assertFalse(bean.getSqlTypeInfos().isEmpty());
        // delete
        bean.deleteSqlTypeInfo(object);
        // check
        assertTrue(bean.getSqlTypeInfos().isEmpty());
    }

    public void testFindSqlTypeInfoByTypeName() {
        String NAME = "My Name";
        // create
        SqlTypeInfo object = new DefaultModelFactory().createSqlTypeInfo();
        // set
        object.setName(NAME);
        // add
        bean.addSqlTypeInfo(object);
        // check
        assertSame(object, bean.findSqlTypeInfoByTypeName(object.getName()));
    }

    public void testGetSupportedResultSetTypes() {
        // get
        Set<ResultSetType> set = bean.getSupportedResultSetTypes();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSupportedResultSetType() {
        // add
        bean.addSupportedResultSetType(ResultSetType.SCROLL_INSENSITIVE);
        // check
        assertFalse(bean.getSupportedResultSetTypes().isEmpty());
    }

    public void testDeleteSupportedResultSetType() {
        // add
        bean.addSupportedResultSetType(ResultSetType.SCROLL_INSENSITIVE);
        // check
        assertFalse(bean.getSupportedResultSetTypes().isEmpty());

        // delete
        bean.deleteSupportedResultSetType(ResultSetType.SCROLL_INSENSITIVE);
        // check
        assertTrue(bean.getSupportedResultSetTypes().isEmpty());
    }

    public void testGetSupportedForwardOnlyResultSetConcurrencies() {
        // get
        Set<ResultSetConcurrencyType> set = bean.getSupportedForwardOnlyResultSetConcurrencies();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSupportedForwardOnlyResultSetConcurrency() {
        // add
        bean.addSupportedForwardOnlyResultSetConcurrency(ResultSetConcurrencyType.UPDATABLE);
        // check
        assertFalse(bean.getSupportedForwardOnlyResultSetConcurrencies().isEmpty());
    }

    public void testDeleteSupportedForwardOnlyResultSetConcurrency() {
        // add
        bean.addSupportedForwardOnlyResultSetConcurrency(ResultSetConcurrencyType.UPDATABLE);
        // check
        assertFalse(bean.getSupportedForwardOnlyResultSetConcurrencies().isEmpty());
        // delete
        bean.deleteSupportedForwardOnlyResultSetConcurrency(ResultSetConcurrencyType.UPDATABLE);
        // check
        assertTrue(bean.getSupportedForwardOnlyResultSetConcurrencies().isEmpty());
    }

    public void testGetSupportedScrollInsensitiveResultSetConcurrencies() {
        // get
        Set<ResultSetConcurrencyType> set = bean.getSupportedScrollInsensitiveResultSetConcurrencies();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSupportedScrollInsensitiveResultSetConcurrency() {
        // add
        bean.addSupportedScrollInsensitiveResultSetConcurrency(ResultSetConcurrencyType.UPDATABLE);
        // check
        assertFalse(bean.getSupportedScrollInsensitiveResultSetConcurrencies().isEmpty());
    }

    public void testDeleteSupportedScrollInsensitiveResultSetConcurrency() {
        // add
        bean.addSupportedScrollInsensitiveResultSetConcurrency(ResultSetConcurrencyType.UPDATABLE);
        // check
        assertFalse(bean.getSupportedScrollInsensitiveResultSetConcurrencies().isEmpty());
        // delete
        bean.deleteSupportedScrollInsensitiveResultSetConcurrency(ResultSetConcurrencyType.UPDATABLE);
        // check
        assertTrue(bean.getSupportedScrollInsensitiveResultSetConcurrencies().isEmpty());
    }

    public void testGetSupportedScrollSensitiveResultSetConcurrencies() {
        // get
        Set<ResultSetConcurrencyType> set = bean.getSupportedScrollSensitiveResultSetConcurrencies();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddSupportedScrollSensitiveResultSetConcurrency() {
        // add
        bean.addSupportedScrollSensitiveResultSetConcurrency(ResultSetConcurrencyType.UPDATABLE);
        // check
        assertFalse(bean.getSupportedScrollSensitiveResultSetConcurrencies().isEmpty());
    }

    public void testDeleteSupportedScrollSensitiveResultSetConcurrency() {
        // add
        bean.addSupportedScrollSensitiveResultSetConcurrency(ResultSetConcurrencyType.UPDATABLE);
        // check
        assertFalse(bean.getSupportedScrollSensitiveResultSetConcurrencies().isEmpty());
        // delete
        bean.deleteSupportedScrollSensitiveResultSetConcurrency(ResultSetConcurrencyType.UPDATABLE);
        // check
        assertTrue(bean.getSupportedScrollSensitiveResultSetConcurrencies().isEmpty());
    }

    public void testSetForwardOnlyResultSetOwnUpdatesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setForwardOnlyResultSetOwnUpdatesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isForwardOnlyResultSetOwnUpdatesAreVisible());
    }

    public void testSetScrollInsensitiveResultSetOwnUpdatesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollInsensitiveResultSetOwnUpdatesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollInsensitiveResultSetOwnUpdatesAreVisible());
    }

    public void testSetScrollSensitiveResultSetOwnUpdatesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollSensitiveResultSetOwnUpdatesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollSensitiveResultSetOwnUpdatesAreVisible());
    }

    public void testSetForwardOnlyResultSetOwnDeletesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setForwardOnlyResultSetOwnDeletesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isForwardOnlyResultSetOwnDeletesAreVisible());
    }

    public void testSetScrollInsensitiveResultSetOwnDeletesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollInsensitiveResultSetOwnDeletesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollInsensitiveResultSetOwnDeletesAreVisible());
    }

    public void testSetScrollSensitiveResultSetOwnDeletesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollSensitiveResultSetOwnDeletesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollSensitiveResultSetOwnDeletesAreVisible());
    }

    public void testSetScrollInsensitiveResultSetOwnInsertsAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollInsensitiveResultSetOwnInsertsAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollInsensitiveResultSetOwnInsertsAreVisible());
    }

    public void testSetScrollSensitiveResultSetOwnInsertsAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollSensitiveResultSetOwnInsertsAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollSensitiveResultSetOwnInsertsAreVisible());
    }

    public void testSetForwardOnlyResultSetOthersUpdatesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setForwardOnlyResultSetOthersUpdatesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isForwardOnlyResultSetOthersUpdatesAreVisible());
    }

    public void testSetScrollInsensitiveResultSetOthersUpdatesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollInsensitiveResultSetOthersUpdatesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollInsensitiveResultSetOthersUpdatesAreVisible());
    }

    public void testSetScrollSensitiveResultSetOthersUpdatesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollSensitiveResultSetOthersUpdatesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollSensitiveResultSetOthersUpdatesAreVisible());
    }

    public void testSetForwardOnlyResultSetOthersDeletesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setForwardOnlyResultSetOthersDeletesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isForwardOnlyResultSetOthersDeletesAreVisible());
    }

    public void testSetScrollInsensitiveResultSetOthersDeletesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollInsensitiveResultSetOthersDeletesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollInsensitiveResultSetOthersDeletesAreVisible());
    }

    public void testSetScrollSensitiveResultSetOthersDeletesAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollSensitiveResultSetOthersDeletesAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollSensitiveResultSetOthersDeletesAreVisible());
    }

    public void testSetForwardOnlyResultSetOthersInsertsAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setForwardOnlyResultSetOthersInsertsAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isForwardOnlyResultSetOthersInsertsAreVisible());
    }

    public void testSetScrollInsensitiveResultSetOthersInsertsAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollInsensitiveResultSetOthersInsertsAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollInsensitiveResultSetOthersInsertsAreVisible());
    }

    public void testSetScrollSensitiveResultSetOthersInsertsAreVisible() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollSensitiveResultSetOthersInsertsAreVisible(VALUE);
        // check
        assertSame(VALUE, bean.isScrollSensitiveResultSetOthersInsertsAreVisible());
    }

    public void testSetForwardOnlyResultSetUpdatesAreDetected() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setForwardOnlyResultSetUpdatesAreDetected(VALUE);
        // check
        assertSame(VALUE, bean.isForwardOnlyResultSetUpdatesAreDetected());
    }

    public void testSetScrollInsensitiveResultSetUpdatesAreDetected() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollInsensitiveResultSetUpdatesAreDetected(VALUE);
        // check
        assertSame(VALUE, bean.isScrollInsensitiveResultSetUpdatesAreDetected());
    }

    public void testSetScrollSensitiveResultSetUpdatesAreDetected() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollSensitiveResultSetUpdatesAreDetected(VALUE);
        // check
        assertSame(VALUE, bean.isScrollSensitiveResultSetUpdatesAreDetected());
    }

    public void testSetForwardOnlyResultSetDeletesAreDetected() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setForwardOnlyResultSetDeletesAreDetected(VALUE);
        // check
        assertSame(VALUE, bean.isForwardOnlyResultSetDeletesAreDetected());
    }

    public void testSetScrollInsensitiveResultSetDeletesAreDetected() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollInsensitiveResultSetDeletesAreDetected(VALUE);
        // check
        assertSame(VALUE, bean.isScrollInsensitiveResultSetDeletesAreDetected());
    }

    public void testSetScrollSensitiveResultSetDeletesAreDetected() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollSensitiveResultSetDeletesAreDetected(VALUE);
        // check
        assertSame(VALUE, bean.isScrollSensitiveResultSetDeletesAreDetected());
    }

    public void testSetForwardOnlyResultInsertsAreDetected() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setForwardOnlyResultInsertsAreDetected(VALUE);
        // check
        assertSame(VALUE, bean.isForwardOnlyResultInsertsAreDetected());
    }

    public void testSetScrollInsensitiveResultInsertsAreDetected() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollInsensitiveResultInsertsAreDetected(VALUE);
        // check
        assertSame(VALUE, bean.isScrollInsensitiveResultInsertsAreDetected());
    }

    public void testSetScrollSensitiveResultInsertsAreDetected() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setScrollSensitiveResultInsertsAreDetected(VALUE);
        // check
        assertSame(VALUE, bean.isScrollSensitiveResultInsertsAreDetected());
    }

    public void testSetSupportsBatchUpdates() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsBatchUpdates(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsBatchUpdates());
    }

    public void testGetUserDefinedTypes() {
        // get
        Set<UserDefinedType> set = bean.getUserDefinedTypes();
        // check
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    public void testAddUserDefinedType() {
        String NAME = "My name";
        // create
        UserDefinedType object = new DefaultModelFactory().createUserDefinedType();
        // set name
        object.setName(NAME);
        // add
        bean.addUserDefinedType(object);
        // check
        assertFalse(bean.getUserDefinedTypes().isEmpty());
    }

    public void testDeleteUserDefinedType() {
        String NAME = "My name";
        // create
        UserDefinedType object = new DefaultModelFactory().createUserDefinedType();
        // set name
        object.setName(NAME);
        // add
        bean.addUserDefinedType(object);
        // check
        assertFalse(bean.getUserDefinedTypes().isEmpty());
        // delete
        bean.deleteUserDefinedType(object);
        // check
        assertTrue(bean.getUserDefinedTypes().isEmpty());
    }

    public void testFindUserDefinedTypeByName() {
        String NAME = "My name";
        // create
        UserDefinedType object = new DefaultModelFactory().createUserDefinedType();
        // set name
        object.setName(NAME);
        // add
        bean.addUserDefinedType(object);
        // check
        assertSame(object, bean.findUserDefinedTypeByName(null, null, object.getName()));
    }

    public void testSetSupportsSavepoints() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsSavepoints(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsSavepoints());
    }

    public void testSetSupportsNamedParameters() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsNamedParameters(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsNamedParameters());
    }

    public void testSetSupportsMultipleOpenResults() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsMultipleOpenResults(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsMultipleOpenResults());
    }

    public void testSetSupportsGetGeneratedKeys() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsGetGeneratedKeys(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsGetGeneratedKeys());
    }

    public void testSetSupportsResultSetHoldCurrorsOverCommitHoldability() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsResultSetHoldCurrorsOverCommitHoldability(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsResultSetHoldCurrorsOverCommitHoldability());
    }

    public void testSetSupportsResultSetCloseCurrorsAtCommitHoldability() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsResultSetCloseCurrorsAtCommitHoldability(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsResultSetCloseCurrorsAtCommitHoldability());
    }

    public void testSetResultSetHoldabilityType() {
        // set
        bean.setResultSetHoldabilityType(ResultSetHoldabilityType.HOLD_CURSORS_OVER_COMMIT);
        // check
        assertSame(ResultSetHoldabilityType.HOLD_CURSORS_OVER_COMMIT, bean.getResultSetHoldabilityType());
    }

    public void testSetDatabaseMajorVersion() {
        Integer VALUE = new Integer(1);
        // set
        bean.setDatabaseMajorVersion(VALUE);
        // check
        assertSame(VALUE, bean.getDatabaseMajorVersion());
    }

    public void testSetDatabaseMinorVersion() {
        Integer VALUE = new Integer(1);
        // set
        bean.setDatabaseMinorVersion(VALUE);
        // check
        assertSame(VALUE, bean.getDatabaseMinorVersion());
    }

    public void testSetJDBCMajorVersion() {
        Integer VALUE = new Integer(1);
        // set
        bean.setJDBCMajorVersion(VALUE);
        // check
        assertSame(VALUE, bean.getJDBCMajorVersion());
    }

    public void testSetJDBCMinorVersion() {
        Integer VALUE = new Integer(1);
        // set
        bean.setJDBCMinorVersion(VALUE);
        // check
        assertSame(VALUE, bean.getJDBCMinorVersion());
    }

    public void testSetSQLStateType() {
        // set
        bean.setSQLStateType(SQLStateType.SQL99);
        // check
        assertSame(SQLStateType.SQL99, bean.getSQLStateType());
    }

    public void testSetLocatorsUpdateCopy() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setLocatorsUpdateCopy(VALUE);
        // check
        assertSame(VALUE, bean.isLocatorsUpdateCopy());
    }

    public void testSetSupportsStatementPooling() {
        Boolean VALUE = Boolean.TRUE;
        // set
        bean.setSupportsStatementPooling(VALUE);
        // check
        assertSame(VALUE, bean.isSupportsStatementPooling());
    }

}
