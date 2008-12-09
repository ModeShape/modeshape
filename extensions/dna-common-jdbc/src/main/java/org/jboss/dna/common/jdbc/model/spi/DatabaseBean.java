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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import org.jboss.dna.common.jdbc.model.api.*;

/**
 * Provides RDBMS wide meta data retrieved from java.sql.DatabaseMetaData.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DatabaseBean extends CoreMetaDataBean implements Database {
    private static final long serialVersionUID = 6634428066138252064L;
    // database metadata provider exception list
    private List<DatabaseMetaDataMethodException> exceptionList = new ArrayList<DatabaseMetaDataMethodException>();
    private String name;
    private Boolean allProceduresAreCallable;
    private Boolean allTablesAreSelectable;
    private String url;
    private String userName;
    private Boolean readOnly;
    private Boolean nullsAreSortedHigh;
    private Boolean nullsAreSortedLow;
    private Boolean nullsAreSortedAtStart;
    private Boolean nullsAreSortedAtEnd;
    private String databaseProductName;
    private String databaseProductVersion;
    private String driverName;
    private String driverVersion;
    private Integer driverMajorVersion;
    private Integer driverMinorVersion;
    private Boolean usesLocalFiles;
    private Boolean usesLocalFilePerTable;
    private Boolean supportsMixedCaseIdentifiers;
    private Boolean storesUpperCaseIdentifiers;
    private Boolean storesLowerCaseIdentifiers;
    private Boolean storesMixedCaseIdentifiers;
    private Boolean supportsMixedCaseQuotedIdentifiers;
    private Boolean storesUpperCaseQuotedIdentifiers;
    private Boolean storesLowerCaseQuotedIdentifiers;
    private Boolean storesMixedCaseQuotedIdentifiers;
    private String identifierQuoteString;
    private Set<String> sqlKeywords = new HashSet<String>();
    private Set<String> numericFunctions = new HashSet<String>();
    private Set<String> stringFunctions = new HashSet<String>();
    private Set<String> systemFunctions = new HashSet<String>();
    private Set<String> timeDateFunctions = new HashSet<String>();
    private String searchStringEscape;
    private String extraNameCharacters;
    private Boolean supportsAlterTableWithAddColumn;
    private Boolean supportsAlterTableWithDropColumn;
    private Boolean supportsColumnAliasing;
    private Boolean nullPlusNonNullIsNull;
    private Boolean supportsConvert;
    private Boolean supportsTableCorrelationNames;
    private Boolean supportsDifferentTableCorrelationNames;
    private Boolean supportsExpressionsInOrderBy;
    private Boolean supportsOrderByUnrelated;
    private Boolean supportsGroupBy;
    private Boolean supportsGroupByUnrelated;
    private Boolean supportsGroupByBeyondSelect;
    private Boolean supportsLikeEscapeClause;
    private Boolean supportsMultipleResultSets;
    private Boolean supportsMultipleTransactions;
    private Boolean supportsNonNullableColumns;
    private Boolean supportsMinimumSQLGrammar;
    private Boolean supportsCoreSQLGrammar;
    private Boolean supportsExtendedSQLGrammar;
    private Boolean supportsANSI92EntryLevelSQL;
    private Boolean supportsANSI92IntermediateSQL;
    private Boolean supportsANSI92FullSQL;
    private Boolean supportsIntegrityEnhancementFacility;
    private Boolean supportsOuterJoins;
    private Boolean supportsFullOuterJoins;
    private Boolean supportsLimitedOuterJoins;
    private String schemaTerm;
    private String procedureTerm;
    private String catalogTerm;
    private Boolean catalogAtStart;
    private String catalogSeparator;
    private Boolean supportsSchemasInDataManipulation;
    private Boolean supportsSchemasInProcedureCalls;
    private Boolean supportsSchemasInTableDefinitions;
    private Boolean supportsSchemasInIndexDefinitions;
    private Boolean supportsSchemasInPrivilegeDefinitions;
    private Boolean supportsCatalogsInDataManipulation;
    private Boolean supportsCatalogsInProcedureCalls;
    private Boolean supportsCatalogsInTableDefinitions;
    private Boolean supportsCatalogsInIndexDefinitions;
    private Boolean supportsCatalogsInPrivilegeDefinitions;
    private Boolean supportsPositionedDelete;
    private Boolean supportsPositionedUpdate;
    private Boolean supportsSelectForUpdate;
    private Boolean supportsStoredProcedures;
    private Boolean supportsSubqueriesInComparisons;
    private Boolean supportsSubqueriesInExists;
    private Boolean supportsSubqueriesInIns;
    private Boolean supportsSubqueriesInQuantifieds;
    private Boolean supportsCorrelatedSubqueries;
    private Boolean supportsUnion;
    private Boolean supportsUnionAll;
    private Boolean supportsOpenCursorsAcrossCommit;
    private Boolean supportsOpenCursorsAcrossRollback;
    private Boolean supportsOpenStatementsAcrossCommit;
    private Boolean supportsOpenStatementsAcrossRollback;
    private Integer maxBinaryLiteralLength;
    private Integer maxCharLiteralLength;
    private Integer maxColumnNameLength;
    private Integer maxColumnsInGroupBy;
    private Integer maxColumnsInIndex;
    private Integer maxColumnsInOrderBy;
    private Integer maxColumnsInSelect;
    private Integer maxColumnsInTable;
    private Integer maxConnections;
    private Integer maxCursorNameLength;
    private Integer maxIndexLength;
    private Integer maxSchemaNameLength;
    private Integer maxProcedureNameLength;
    private Integer maxCatalogNameLength;
    private Integer maxRowSize;
    private Boolean maxRowSizeIncludeBlobs;
    private Integer maxStatementLength;
    private Integer maxStatements;
    private Integer maxTableNameLength;
    private Integer maxTablesInSelect;
    private Integer maxUserNameLength;
    private Integer defaultTransactionIsolation;
    private Boolean supportsTransactions;
    private Set<TransactionIsolationLevelType> supportedTransactionIsolationLevels = new HashSet<TransactionIsolationLevelType>();
    private Boolean supportsDataDefinitionAndDataManipulationTransactions;
    private Boolean supportsDataManipulationTransactionsOnly;
    private Boolean dataDefinitionCausesTransactionCommit;
    private Boolean dataDefinitionIgnoredInTransactions;
    private Set<StoredProcedure> storedProcedures = new HashSet<StoredProcedure>();
    private Set<Table> tables = new HashSet<Table>();
    private Set<Schema> schemas = new HashSet<Schema>();
    private Set<Catalog> catalogs = new HashSet<Catalog>();
    private Set<TableType> tableTypes = new HashSet<TableType>();
    private Set<SqlTypeInfo> sqlTypeInfos = new HashSet<SqlTypeInfo>();
    private Set<ResultSetType> supportedResultSetTypes = new HashSet<ResultSetType>();
    private Set<UserDefinedType> userDefinedTypes = new HashSet<UserDefinedType>();
    private Set<SqlTypeConversionPair> supportedConversions = new HashSet<SqlTypeConversionPair>();
    private Set<ResultSetConcurrencyType> supportedForwardOnlyResultSetConcurrencies = new HashSet<ResultSetConcurrencyType>();
    private Set<ResultSetConcurrencyType> supportedScrollInsensitiveResultSetConcurrencies = new HashSet<ResultSetConcurrencyType>();
    private Set<ResultSetConcurrencyType> supportedScrollSensitiveResultSetConcurrencies = new HashSet<ResultSetConcurrencyType>();
    private Boolean forwardOnlyResultSetOwnUpdatesAreVisible;
    private Boolean scrollInsensitiveResultSetOwnUpdatesAreVisible;
    private Boolean scrollSensitiveResultSetOwnUpdatesAreVisible;
    private Boolean forwardOnlyResultSetOwnDeletesAreVisible;
    private Boolean scrollInsensitiveResultSetOwnDeletesAreVisible;
    private Boolean scrollSensitiveResultSetOwnDeletesAreVisible;
    private Boolean forwardOnlyResultSetOwnInsertsAreVisible;
    private Boolean scrollInsensitiveResultSetOwnInsertsAreVisible;
    private Boolean scrollSensitiveResultSetOwnInsertsAreVisible;
    private Boolean forwardOnlyResultSetOthersUpdatesAreVisible;
    private Boolean scrollInsensitiveResultSetOthersUpdatesAreVisible;
    private Boolean scrollSensitiveResultSetOthersUpdatesAreVisible;
    private Boolean forwardOnlyResultSetOthersDeletesAreVisible;
    private Boolean scrollInsensitiveResultSetOthersDeletesAreVisible;
    private Boolean scrollSensitiveResultSetOthersDeletesAreVisible;
    private Boolean forwardOnlyResultSetOthersInsertsAreVisible;
    private Boolean scrollInsensitiveResultSetOthersInsertsAreVisible;
    private Boolean scrollSensitiveResultSetOthersInsertsAreVisible;
    private Boolean forwardOnlyResultSetUpdatesAreDetected;
    private Boolean scrollInsensitiveResultSetUpdatesAreDetected;
    private Boolean scrollSensitiveResultSetUpdatesAreDetected;
    private Boolean forwardOnlyResultSetDeletesAreDetected;
    private Boolean scrollInsensitiveResultSetDeletesAreDetected;
    private Boolean scrollSensitiveResultSetDeletesAreDetected;
    private Boolean forwardOnlyResultInsertsAreDetected;
    private Boolean scrollInsensitiveResultInsertsAreDetected;
    private Boolean scrollSensitiveResultInsertsAreDetected;
    private Boolean supportsBatchUpdates;
    private Boolean supportsSavepoints;
    private Boolean supportsNamedParameters;
    private Boolean supportsMultipleOpenResults;
    private Boolean supportsGetGeneratedKeys;
    private Boolean supportsResultSetHoldCurrorsOverCommitHoldability;
    private Boolean supportsResultSetCloseCurrorsAtCommitHoldability;
    private ResultSetHoldabilityType resultSetHoldabilityType;
    private Integer databaseMajorVersion;
    private Integer databaseMinorVersion;
    private Integer jdbcMajorVersion;
    private Integer jdbcMinorVersion;
    private SQLStateType sqlStateType;
    private Boolean locatorsUpdateCopy;
    private Boolean supportsStatementPooling;

    /**
     * Default constructor
     */
    public DatabaseBean() {
    }

    /**
     * Returns list of failed database metadata methods through the DatabaseMetaDataMethodExceptions
     * 
     * @return list of failed database metadata methods through the DatabaseMetaDataMethodExceptions
     */
    public List<DatabaseMetaDataMethodException> getExceptionList() {
        return exceptionList;
    }

    /**
     * Adds the DatabaseMetaDataMethodException to the DatabaseMetadataProvider exception list
     * 
     * @param exception the DatabaseMetaDataMethodException
     */
    public void addException( DatabaseMetaDataMethodException exception ) {
        exceptionList.add(exception);
    }

    /**
     * Searches the DatabaseMetaDataMethodException by method name
     * 
     * @param methodName the name of method that caused exception
     * @return the DatabaseMetaDataMethodException if found, otherwise returns null
     */
    public DatabaseMetaDataMethodException findException( String methodName ) {
        // trying to find exception
        for (DatabaseMetaDataMethodException exception : exceptionList) {
            if (exception.getMethodName().equals(methodName)) {
                return exception;
            }
        }
        return null;
    }

    /**
     * Checks that specified database metadata method of provider is failed or not
     * 
     * @param methodName the name of method that caused exception
     * @return true if method failed; false otherwise
     */
    public boolean isDatabaseMetaDataMethodFailed( String methodName ) {
        return (findException(methodName) != null);
    }

    /**
     * Gets database name
     * 
     * @return database name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets database name
     * 
     * @param name the database name
     */
    public void setName( String name ) {
        this.name = name;
    }

    // ----------------------------------------------------------------------
    // A variety of minor information about the target database.
    // ----------------------------------------------------------------------

    /**
     * Retrieves whether the current user can call all the procedures returned by the method
     * <code>DatabaseMetaData.getProcedures</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isAllProceduresAreCallable() {
        return allProceduresAreCallable;
    }

    /**
     * sets whether the current user can call all the procedures returned by the method
     * <code>DatabaseMetaData.getProcedures</code>.
     * 
     * @param allProceduresAreCallable <code>true</code> if so; <code>false</code> otherwise
     */
    public void setAllProceduresAreCallable( Boolean allProceduresAreCallable ) {
        this.allProceduresAreCallable = allProceduresAreCallable;
    }

    /**
     * Retrieves whether the current user can use all the tables returned by the method <code>DatabaseMetaData.getTables</code> in
     * a <code>SELECT</code> statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isAllTablesAreSelectable() {
        return allTablesAreSelectable;
    }

    /**
     * Sets whether the current user can use all the tables returned by the method <code>DatabaseMetaData.getTables</code> in a
     * <code>SELECT</code> statement.
     * 
     * @param allTablesAreSelectable <code>true</code> if so; <code>false</code> otherwise
     */
    public void setAllTablesAreSelectable( Boolean allTablesAreSelectable ) {
        this.allTablesAreSelectable = allTablesAreSelectable;
    }

    /**
     * Retrieves the URL for this DBMS.
     * 
     * @return the URL for this DBMS or <code>null</code> if it cannot be generated
     */
    public String getURL() {
        return url;
    }

    /**
     * Sets the URL for this DBMS.
     * 
     * @param url the URL for this DBMS or <code>null</code> if it cannot be generated
     */
    public void setURL( String url ) {
        this.url = url;
    }

    /**
     * Retrieves the user name as known to this database.
     * 
     * @return the database user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Sets the user name as known to this database.
     * 
     * @param userName the database user name
     */
    public void setUserName( String userName ) {
        this.userName = userName;
    }

    /**
     * Retrieves whether this database is in read-only mode.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets whether this database is in read-only mode.
     * 
     * @param readOnly <code>true</code> if so; <code>false</code> otherwise
     */
    public void setReadOnly( Boolean readOnly ) {
        this.readOnly = readOnly;
    }

    /**
     * Retrieves whether <code>NULL</code> values are sorted high. Sorted high means that <code>NULL</code> values sort higher
     * than any other value in a domain. In an ascending order, if this method returns <code>true</code>, <code>NULL</code> values
     * will appear at the end. By contrast, the method <code>DatabaseMetaData.nullsAreSortedAtEnd</code> indicates whether
     * <code>NULL</code> values are sorted at the end regardless of sort order.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isNullsAreSortedHigh() {
        return nullsAreSortedHigh;
    }

    /**
     * Sets whether <code>NULL</code> values are sorted high. Sorted high means that <code>NULL</code> values sort higher than any
     * other value in a domain. In an ascending order, if this method returns <code>true</code>, <code>NULL</code> values will
     * appear at the end. By contrast, the method <code>DatabaseMetaData.nullsAreSortedAtEnd</code> indicates whether
     * <code>NULL</code> values are sorted at the end regardless of sort order.
     * 
     * @param nullsAreSortedHigh <code>true</code> if so; <code>false</code> otherwise
     */
    public void setNullsAreSortedHigh( Boolean nullsAreSortedHigh ) {
        this.nullsAreSortedHigh = nullsAreSortedHigh;
    }

    /**
     * Retrieves whether <code>NULL</code> values are sorted low. Sorted low means that <code>NULL</code> values sort lower than
     * any other value in a domain. In an ascending order, if this method returns <code>true</code>, <code>NULL</code> values will
     * appear at the beginning. By contrast, the method <code>DatabaseMetaData.nullsAreSortedAtStart</code> indicates whether
     * <code>NULL</code> values are sorted at the beginning regardless of sort order.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isNullsAreSortedLow() {
        return nullsAreSortedLow;
    }

    /**
     * Sets whether <code>NULL</code> values are sorted low. Sorted low means that <code>NULL</code> values sort lower than any
     * other value in a domain. In an ascending order, if this method returns <code>true</code>, <code>NULL</code> values will
     * appear at the beginning. By contrast, the method <code>DatabaseMetaData.nullsAreSortedAtStart</code> indicates whether
     * <code>NULL</code> values are sorted at the beginning regardless of sort order.
     * 
     * @param nullsAreSortedLow <code>true</code> if so; <code>false</code> otherwise
     */
    public void setNullsAreSortedLow( Boolean nullsAreSortedLow ) {
        this.nullsAreSortedLow = nullsAreSortedLow;
    }

    /**
     * Retrieves whether <code>NULL</code> values are sorted at the start regardless of sort order.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isNullsAreSortedAtStart() {
        return nullsAreSortedAtStart;
    }

    /**
     * Sets whether <code>NULL</code> values are sorted at the start regardless of sort order.
     * 
     * @param nullsAreSortedAtStart <code>true</code> if so; <code>false</code> otherwise
     */
    public void setNullsAreSortedAtStart( Boolean nullsAreSortedAtStart ) {
        this.nullsAreSortedAtStart = nullsAreSortedAtStart;
    }

    /**
     * Retrieves whether <code>NULL</code> values are sorted at the end regardless of sort order.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isNullsAreSortedAtEnd() {
        return nullsAreSortedAtEnd;
    }

    /**
     * Sets whether <code>NULL</code> values are sorted at the end regardless of sort order.
     * 
     * @param nullsAreSortedAtEnd <code>true</code> if so; <code>false</code> otherwise
     */
    public void setNullsAreSortedAtEnd( Boolean nullsAreSortedAtEnd ) {
        this.nullsAreSortedAtEnd = nullsAreSortedAtEnd;
    }

    /**
     * Retrieves the name of this database product.
     * 
     * @return database product name
     */
    public String getDatabaseProductName() {
        return databaseProductName;
    }

    /**
     * Sets the name of this database product.
     * 
     * @param databaseProductName database product name
     */
    public void setDatabaseProductName( String databaseProductName ) {
        this.databaseProductName = databaseProductName;
    }

    /**
     * Retrieves the version number of this database product.
     * 
     * @return database version number
     */
    public String getDatabaseProductVersion() {
        return databaseProductVersion;
    }

    /**
     * Sets the version number of this database product.
     * 
     * @param databaseProductVersion database version number
     */
    public void setDatabaseProductVersion( String databaseProductVersion ) {
        this.databaseProductVersion = databaseProductVersion;
    }

    /**
     * Retrieves the name of this JDBC driver.
     * 
     * @return JDBC driver name
     */
    public String getDriverName() {
        return driverName;
    }

    /**
     * Sets the name of this JDBC driver.
     * 
     * @param driverName JDBC driver name
     */
    public void setDriverName( String driverName ) {
        this.driverName = driverName;
    }

    /**
     * Retrieves the version number of this JDBC driver as a <code>String</code>.
     * 
     * @return JDBC driver version
     */
    public String getDriverVersion() {
        return driverVersion;
    }

    /**
     * Sets the version number of this JDBC driver as a <code>String</code>.
     * 
     * @param driverVersion the JDBC driver version
     */
    public void setDriverVersion( String driverVersion ) {
        this.driverVersion = driverVersion;
    }

    /**
     * Retrieves this JDBC driver's minor version number.
     * 
     * @return JDBC driver minor version number
     */
    public Integer getDriverMajorVersion() {
        return driverMajorVersion;
    }

    /**
     * Sets this JDBC driver's major version number.
     * 
     * @param driverMajorVersion the JDBC driver major version
     */
    public void setDriverMajorVersion( Integer driverMajorVersion ) {
        this.driverMajorVersion = driverMajorVersion;
    }

    /**
     * Retrieves this JDBC driver's minor version number.
     * 
     * @return JDBC driver minor version number
     */
    public Integer getDriverMinorVersion() {
        return driverMinorVersion;
    }

    /**
     * Sets this JDBC driver's minor version number.
     * 
     * @param driverMinorVersion the JDBC driver minor version number
     */
    public void setDriverMinorVersion( Integer driverMinorVersion ) {
        this.driverMinorVersion = driverMinorVersion;
    }

    /**
     * Retrieves whether this database stores tables in a local file.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isUsesLocalFiles() {
        return usesLocalFiles;
    }

    /**
     * Sets whether this database stores tables in a local file.
     * 
     * @param usesLocalFiles <code>true</code> if so; <code>false</code> otherwise
     */
    public void setUsesLocalFiles( Boolean usesLocalFiles ) {
        this.usesLocalFiles = usesLocalFiles;
    }

    /**
     * Retrieves whether this database uses a file for each table.
     * 
     * @return <code>true</code> if this database uses a local file for each table; <code>false</code> otherwise
     */
    public Boolean isUsesLocalFilePerTable() {
        return usesLocalFilePerTable;
    }

    /**
     * Sets whether this database uses a file for each table.
     * 
     * @param usesLocalFilePerTable <code>true</code> if this database uses a local file for each table; <code>false</code>
     *        otherwise
     */
    public void setUsesLocalFilePerTable( Boolean usesLocalFilePerTable ) {
        this.usesLocalFilePerTable = usesLocalFilePerTable;
    }

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as case sensitive and as a result stores them in
     * mixed case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsMixedCaseIdentifiers() {
        return supportsMixedCaseIdentifiers;
    }

    /**
     * Sets whether this database treats mixed case unquoted SQL identifiers as case sensitive and as a result stores them in
     * mixed case.
     * 
     * @param supportsMixedCaseIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsMixedCaseIdentifiers( Boolean supportsMixedCaseIdentifiers ) {
        this.supportsMixedCaseIdentifiers = supportsMixedCaseIdentifiers;
    }

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in upper
     * case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isStoresUpperCaseIdentifiers() {
        return storesUpperCaseIdentifiers;
    }

    /**
     * Sets whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in upper case.
     * 
     * @param storesUpperCaseIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    public void setStoresUpperCaseIdentifiers( Boolean storesUpperCaseIdentifiers ) {
        this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
    }

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in lower
     * case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isStoresLowerCaseIdentifiers() {
        return storesLowerCaseIdentifiers;
    }

    /**
     * sets whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in lower case.
     * 
     * @param storesLowerCaseIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    public void setStoresLowerCaseIdentifiers( Boolean storesLowerCaseIdentifiers ) {
        this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;
    }

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in mixed
     * case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isStoresMixedCaseIdentifiers() {
        return storesMixedCaseIdentifiers;
    }

    /**
     * Sets whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in mixed case.
     * 
     * @param storesMixedCaseIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    public void setStoresMixedCaseIdentifiers( Boolean storesMixedCaseIdentifiers ) {
        this.storesMixedCaseIdentifiers = storesMixedCaseIdentifiers;
    }

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as case sensitive and as a result stores them in
     * mixed case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsMixedCaseQuotedIdentifiers() {
        return supportsMixedCaseQuotedIdentifiers;
    }

    /**
     * Sets whether this database treats mixed case quoted SQL identifiers as case sensitive and as a result stores them in mixed
     * case.
     * 
     * @param supportsMixedCaseQuotedIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsMixedCaseQuotedIdentifiers( Boolean supportsMixedCaseQuotedIdentifiers ) {
        this.supportsMixedCaseQuotedIdentifiers = supportsMixedCaseQuotedIdentifiers;
    }

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in upper case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isStoresUpperCaseQuotedIdentifiers() {
        return storesUpperCaseQuotedIdentifiers;
    }

    /**
     * Sets whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in upper case.
     * 
     * @param storesUpperCaseQuotedIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    public void setStoresUpperCaseQuotedIdentifiers( Boolean storesUpperCaseQuotedIdentifiers ) {
        this.storesUpperCaseQuotedIdentifiers = storesUpperCaseQuotedIdentifiers;
    }

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in lower case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isStoresLowerCaseQuotedIdentifiers() {
        return storesLowerCaseQuotedIdentifiers;
    }

    /**
     * Sets whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in lower case.
     * 
     * @param storesLowerCaseQuotedIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    public void setStoresLowerCaseQuotedIdentifiers( Boolean storesLowerCaseQuotedIdentifiers ) {
        this.storesLowerCaseQuotedIdentifiers = storesLowerCaseQuotedIdentifiers;
    }

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in mixed case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isStoresMixedCaseQuotedIdentifiers() {
        return storesMixedCaseQuotedIdentifiers;
    }

    /**
     * Sets whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in mixed case.
     * 
     * @param storesMixedCaseQuotedIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    public void setStoresMixedCaseQuotedIdentifiers( Boolean storesMixedCaseQuotedIdentifiers ) {
        this.storesMixedCaseQuotedIdentifiers = storesMixedCaseQuotedIdentifiers;
    }

    /**
     * Retrieves the string used to quote SQL identifiers. This method returns a space " " if identifier quoting is not supported.
     * 
     * @return the quoting string or a space if quoting is not supported
     */
    public String getIdentifierQuoteString() {
        return identifierQuoteString;
    }

    /**
     * Sets the string used to quote SQL identifiers. This method returns a space " " if identifier quoting is not supported.
     * 
     * @param identifierQuoteString the quoting string or a space if quoting is not supported
     */
    public void setIdentifierQuoteString( String identifierQuoteString ) {
        this.identifierQuoteString = identifierQuoteString;
    }

    /**
     * Retrieves a list of all of this database's SQL keywords that are NOT also SQL92 keywords.
     * 
     * @return the list of this database's keywords that are not also SQL92 keywords
     */
    public Set<String> getSQLKeywords() {
        return sqlKeywords;
    }

    /**
     * Adds SQL keyword
     * 
     * @param sqlKeyword the SQL keyword to add
     */
    public void addSQLKeyword( String sqlKeyword ) {
        sqlKeywords.add(sqlKeyword);
    }

    /**
     * Deletes SQL keyword
     * 
     * @param sqlKeyword the SQL keyword to delete
     */
    public void deleteSQLKeyword( String sqlKeyword ) {
        sqlKeywords.remove(sqlKeyword);
    }

    /**
     * Is SQL keyword supported
     * 
     * @param sqlKeyword the SQL keyword to search
     * @return true if supported; false otherwiose
     */
    public Boolean isSQLKeywordSupported( String sqlKeyword ) {
        for (String s : sqlKeywords) {
            if (s.equals(sqlKeyword)) {
                return Boolean.TRUE;
            }
        }
        // return false
        return Boolean.FALSE;
    }

    /**
     * Retrieves a list of math functions available with this database. These are the Open /Open CLI math function names used in
     * the JDBC function escape clause.
     * 
     * @return the list of math functions supported by this database
     */
    public Set<String> getNumericFunctions() {
        return numericFunctions;
    }

    /**
     * Adds numeric function
     * 
     * @param functionName the name of numeric function to add
     */
    public void addNumericFunction( String functionName ) {
        numericFunctions.add(functionName);
    }

    /**
     * Deletes numeric function
     * 
     * @param functionName the name of numeric function to delete
     */
    public void deleteNumericFunction( String functionName ) {
        numericFunctions.remove(functionName);
    }

    /**
     * Is Numeric function supported
     * 
     * @param functionName the name of numeric function
     * @return true is supported; false otherwise
     */
    public Boolean isNumericFunctionSupported( String functionName ) {
        for (String s : numericFunctions) {
            if (s.equals(functionName)) {
                return Boolean.TRUE;
            }
        }
        // return false
        return Boolean.FALSE;
    }

    /**
     * Retrieves a list of string functions available with this database. These are the Open Group CLI string function names used
     * in the JDBC function escape clause.
     * 
     * @return the list of string functions supported by this database
     */
    public Set<String> getStringFunctions() {
        return stringFunctions;
    }

    /**
     * Adds String function
     * 
     * @param functionName the name of String function to add
     */
    public void addStringFunction( String functionName ) {
        stringFunctions.add(functionName);
    }

    /**
     * Deletes String function
     * 
     * @param functionName the name of String function to delete
     */
    public void deleteStringFunction( String functionName ) {
        stringFunctions.remove(functionName);
    }

    /**
     * Is String function supported
     * 
     * @param functionName the name of String function
     * @return true is supported; false otherwise
     */
    public Boolean isStringFunctionSupported( String functionName ) {
        for (String s : stringFunctions) {
            if (s.equals(functionName)) {
                return Boolean.TRUE;
            }
        }
        // return false
        return Boolean.FALSE;
    }

    /**
     * Retrieves a list of system functions available with this database. These are the Open Group CLI system function names used
     * in the JDBC function escape clause.
     * 
     * @return a list of system functions supported by this database
     */
    public Set<String> getSystemFunctions() {
        return systemFunctions;
    }

    /**
     * Adds System function
     * 
     * @param functionName the name of System function to add
     */
    public void addSystemFunction( String functionName ) {
        systemFunctions.add(functionName);
    }

    /**
     * deletes System function
     * 
     * @param functionName the name of System function to delete
     */
    public void deleteSystemFunction( String functionName ) {
        systemFunctions.remove(functionName);
    }

    /**
     * Is System function supported
     * 
     * @param functionName the name of System function
     * @return true is supported; false otherwise
     */
    public Boolean isSystemFunctionSupported( String functionName ) {
        for (String s : systemFunctions) {
            if (s.equals(functionName)) {
                return Boolean.TRUE;
            }
        }
        // return false
        return Boolean.FALSE;
    }

    /**
     * Retrieves a list of the time and date functions available with this database.
     * 
     * @return the list of time and date functions supported by this database
     */
    public Set<String> getTimeDateFunctions() {
        return timeDateFunctions;
    }

    /**
     * Adds Time/Date function
     * 
     * @param functionName the name of Time/Date function to add
     */
    public void addTimeDateFunction( String functionName ) {
        timeDateFunctions.add(functionName);
    }

    /**
     * deletes Time/Date function
     * 
     * @param functionName the name of Time/Date function to delete
     */
    public void deleteTimeDateFunction( String functionName ) {
        timeDateFunctions.remove(functionName);
    }

    /**
     * Is Time/Date function supported
     * 
     * @param functionName the name of Time/Date function
     * @return true is supported; false otherwise
     */
    public Boolean isTimeDateFunctionSupported( String functionName ) {
        for (String s : timeDateFunctions) {
            if (s.equals(functionName)) {
                return Boolean.TRUE;
            }
        }
        // return false
        return Boolean.FALSE;
    }

    /**
     * Retrieves the string that can be used to escape wildcard characters. This is the string that can be used to escape '_' or
     * '%' in the catalog search parameters that are a pattern (and therefore use one of the wildcard characters).
     * <P>
     * The '_' character represents any single character; the '%' character represents any sequence of zero or more characters.
     * 
     * @return the string used to escape wildcard characters
     */
    public String getSearchStringEscape() {
        return searchStringEscape;
    }

    /**
     * Sets the string that can be used to escape wildcard characters. This is the string that can be used to escape '_' or '%' in
     * the catalog search parameters that are a pattern (and therefore use one of the wildcard characters).
     * <P>
     * The '_' character represents any single character; the '%' character represents any sequence of zero or more characters.
     * 
     * @param searchStringEscape the string used to escape wildcard characters
     */
    public void setSearchStringEscape( String searchStringEscape ) {
        this.searchStringEscape = searchStringEscape;
    }

    /**
     * Retrieves all the "extra" characters that can be used in unquoted identifier names (those beyond a-z, A-Z, 0-9 and _).
     * 
     * @return the string containing the extra characters
     */
    public String getExtraNameCharacters() {
        return extraNameCharacters;
    }

    /**
     * Sets all the "extra" characters that can be used in unquoted identifier names (those beyond a-z, A-Z, 0-9 and _).
     * 
     * @param extraNameCharacters the string containing the extra characters
     */
    public void setExtraNameCharacters( String extraNameCharacters ) {
        this.extraNameCharacters = extraNameCharacters;
    }

    // --------------------------------------------------------------------
    // Functions describing which features are supported.
    // --------------------------------------------------------------------

    /**
     * Retrieves whether this database supports <code>ALTER TABLE</code> with add column.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsAlterTableWithAddColumn() {
        return supportsAlterTableWithAddColumn;
    }

    /**
     * Sets whether this database supports <code>ALTER TABLE</code> with add column.
     * 
     * @param supportsAlterTableWithAddColumn <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsAlterTableWithAddColumn( Boolean supportsAlterTableWithAddColumn ) {
        this.supportsAlterTableWithAddColumn = supportsAlterTableWithAddColumn;
    }

    /**
     * Retrieves whether this database supports <code>ALTER TABLE</code> with drop column.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsAlterTableWithDropColumn() {
        return supportsAlterTableWithDropColumn;
    }

    /**
     * Sets whether this database supports <code>ALTER TABLE</code> with drop column.
     * 
     * @param supportsAlterTableWithDropColumn <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsAlterTableWithDropColumn( Boolean supportsAlterTableWithDropColumn ) {
        this.supportsAlterTableWithDropColumn = supportsAlterTableWithDropColumn;
    }

    /**
     * Retrieves whether this database supports column aliasing.
     * <P>
     * If so, the SQL AS clause can be used to provide names for computed columns or to provide alias names for columns as
     * required.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsColumnAliasing() {
        return supportsColumnAliasing;
    }

    /**
     * Sets whether this database supports column aliasing.
     * <P>
     * If so, the SQL AS clause can be used to provide names for computed columns or to provide alias names for columns as
     * required.
     * 
     * @param supportsColumnAliasing <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsColumnAliasing( Boolean supportsColumnAliasing ) {
        this.supportsColumnAliasing = supportsColumnAliasing;
    }

    /**
     * Retrieves whether this database supports concatenations between <code>NULL</code> and non-<code>NULL</code> values being
     * <code>NULL</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isNullPlusNonNullIsNull() {
        return nullPlusNonNullIsNull;
    }

    /**
     * Sets whether this database supports concatenations between <code>NULL</code> and non-<code>NULL</code> values being
     * <code>NULL</code>.
     * @param nullPlusNonNullIsNull true if so
     * 
     */
    public void setNullPlusNonNullIsNull( Boolean nullPlusNonNullIsNull ) {
        this.nullPlusNonNullIsNull = nullPlusNonNullIsNull;
    }

    /**
     * Retrieves whether this database supports the <code>CONVERT</code> function between SQL types.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsConvert() {
        return supportsConvert;
    }

    /**
     * Sets whether this database supports the <code>CONVERT</code> function between SQL types.
     * 
     * @param supportsConvert <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsConvert( Boolean supportsConvert ) {
        this.supportsConvert = supportsConvert;
    }

    /**
     * Retrieves whether this database supports the <code>CONVERT</code> for given SQL types. It uses original
     * <code>DatabaseMetaData.supportsConvert</code> to check common (NOT ALL POSSIBLE) conversions.
     * 
     * @return list of common (NOT ALL POSSIBLE) conversions.
     * @see java.sql.Types
     */
    public Set<SqlTypeConversionPair> getSupportedConversions() {
        return supportedConversions;
    }

    /**
     * Adds SqlTypeConversionPair
     * 
     * @param sqlTypeConversionPair the SqlTypeConversionPair
     */
    public void addSqlTypeConversionPair( SqlTypeConversionPair sqlTypeConversionPair ) {
        supportedConversions.add(sqlTypeConversionPair);
    }

    /**
     * deletes SqlTypeConversionPair
     * 
     * @param sqlTypeConversionPair the SqlTypeConversionPair
     */
    public void deleteSqlTypeConversionPair( SqlTypeConversionPair sqlTypeConversionPair ) {
        supportedConversions.remove(sqlTypeConversionPair);
    }

    /**
     * Searches set of SqlTypeConversionPair by SrcType
     * @param srcType 
     * 
     * @return set of SqlTypeConversionPair
     */
    public Set<SqlTypeConversionPair> findSqlTypeConversionPairBySrcType( String srcType ) {
        // create set holder
        Set<SqlTypeConversionPair> set = new HashSet<SqlTypeConversionPair>();
        // if found
        for (SqlTypeConversionPair c : supportedConversions) {
            if (c.getSrcType().toString().equals(srcType)) {
                set.add(c);
            }
        }
        // return nothing
        return set;
    }

    /**
     * Retrieves whether this database supports table correlation names.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsTableCorrelationNames() {
        return supportsTableCorrelationNames;
    }

    /**
     * Sets whether this database supports table correlation names.
     * 
     * @param supportsTableCorrelationNames <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsTableCorrelationNames( Boolean supportsTableCorrelationNames ) {
        this.supportsTableCorrelationNames = supportsTableCorrelationNames;
    }

    /**
     * Retrieves whether, when table correlation names are supported, they are restricted to being different from the names of the
     * tables.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsDifferentTableCorrelationNames() {
        return supportsDifferentTableCorrelationNames;
    }

    /**
     * Sets whether, when table correlation names are supported, they are restricted to being different from the names of the
     * tables.
     * 
     * @param supportsDifferentTableCorrelationNames <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsDifferentTableCorrelationNames( Boolean supportsDifferentTableCorrelationNames ) {
        this.supportsDifferentTableCorrelationNames = supportsDifferentTableCorrelationNames;
    }

    /**
     * Retrieves whether this database supports expressions in <code>ORDER BY</code> lists.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsExpressionsInOrderBy() {
        return supportsExpressionsInOrderBy;
    }

    /**
     * Sets whether this database supports expressions in <code>ORDER BY</code> lists.
     * 
     * @param supportsExpressionsInOrderBy <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsExpressionsInOrderBy( Boolean supportsExpressionsInOrderBy ) {
        this.supportsExpressionsInOrderBy = supportsExpressionsInOrderBy;
    }

    /**
     * Retrieves whether this database supports using a column that is not in the <code>SELECT</code> statement in an
     * <code>ORDER BY</code> clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsOrderByUnrelated() {
        return supportsOrderByUnrelated;
    }

    /**
     * Sets whether this database supports using a column that is not in the <code>SELECT</code> statement in an
     * <code>ORDER BY</code> clause.
     * @param supportsOrderByUnrelated true if so
     * 
     */
    public void setSupportsOrderByUnrelated( Boolean supportsOrderByUnrelated ) {
        this.supportsOrderByUnrelated = supportsOrderByUnrelated;
    }

    /**
     * Retrieves whether this database supports some form of <code>GROUP BY</code> clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsGroupBy() {
        return supportsGroupBy;
    }

    /**
     * Sets whether this database supports some form of <code>GROUP BY</code> clause.
     * 
     * @param supportsGroupBy <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsGroupBy( Boolean supportsGroupBy ) {
        this.supportsGroupBy = supportsGroupBy;
    }

    /**
     * Retrieves whether this database supports using a column that is not in the <code>SELECT</code> statement in a
     * <code>GROUP BY</code> clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsGroupByUnrelated() {
        return supportsGroupByUnrelated;
    }

    /**
     * Sets whether this database supports using a column that is not in the <code>SELECT</code> statement in a
     * <code>GROUP BY</code> clause.
     * 
     * @param supportsGroupByUnrelated <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsGroupByUnrelated( Boolean supportsGroupByUnrelated ) {
        this.supportsGroupByUnrelated = supportsGroupByUnrelated;
    }

    /**
     * Retrieves whether this database supports using columns not included in the <code>SELECT</code> statement in a
     * <code>GROUP BY</code> clause provided that all of the columns in the <code>SELECT</code> statement are included in the
     * <code>GROUP BY</code> clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsGroupByBeyondSelect() {
        return supportsGroupByBeyondSelect;
    }

    /**
     * Sets whether this database supports using columns not included in the <code>SELECT</code> statement in a
     * <code>GROUP BY</code> clause provided that all of the columns in the <code>SELECT</code> statement are included in the
     * <code>GROUP BY</code> clause.
     * 
     * @param supportsGroupByBeyondSelect <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsGroupByBeyondSelect( Boolean supportsGroupByBeyondSelect ) {
        this.supportsGroupByBeyondSelect = supportsGroupByBeyondSelect;
    }

    /**
     * Retrieves whether this database supports specifying a <code>LIKE</code> escape clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsLikeEscapeClause() {
        return supportsLikeEscapeClause;
    }

    /**
     * Sets whether this database supports specifying a <code>LIKE</code> escape clause.
     * 
     * @param supportsLikeEscapeClause <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsLikeEscapeClause( Boolean supportsLikeEscapeClause ) {
        this.supportsLikeEscapeClause = supportsLikeEscapeClause;
    }

    /**
     * Retrieves whether this database supports getting multiple <code>ResultSet</code> objects from a single call to the method
     * <code>execute</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsMultipleResultSets() {
        return supportsMultipleResultSets;
    }

    /**
     * Sets whether this database supports getting multiple <code>ResultSet</code> objects from a single call to the method
     * <code>execute</code>.
     * 
     * @param supportsMultipleResultSets <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsMultipleResultSets( Boolean supportsMultipleResultSets ) {
        this.supportsMultipleResultSets = supportsMultipleResultSets;
    }

    /**
     * Retrieves whether this database allows having multiple transactions open at once (on different connections).
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsMultipleTransactions() {
        return supportsMultipleTransactions;
    }

    /**
     * Sets whether this database allows having multiple transactions open at once (on different connections).
     * 
     * @param supportsMultipleTransactions <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsMultipleTransactions( Boolean supportsMultipleTransactions ) {
        this.supportsMultipleTransactions = supportsMultipleTransactions;
    }

    /**
     * Retrieves whether columns in this database may be defined as non-nullable.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsNonNullableColumns() {
        return supportsNonNullableColumns;
    }

    /**
     * Sets whether columns in this database may be defined as non-nullable.
     * 
     * @param supportsNonNullableColumns <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsNonNullableColumns( Boolean supportsNonNullableColumns ) {
        this.supportsNonNullableColumns = supportsNonNullableColumns;
    }

    /**
     * Retrieves whether this database supports the ODBC Minimum SQL grammar.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsMinimumSQLGrammar() {
        return supportsMinimumSQLGrammar;
    }

    /**
     * Sets whether this database supports the ODBC Minimum SQL grammar.
     * 
     * @param supportsMinimumSQLGrammar <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsMinimumSQLGrammar( Boolean supportsMinimumSQLGrammar ) {
        this.supportsMinimumSQLGrammar = supportsMinimumSQLGrammar;
    }

    /**
     * Retrieves whether this database supports the ODBC Core SQL grammar.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsCoreSQLGrammar() {
        return supportsCoreSQLGrammar;
    }

    /**
     * Sets whether this database supports the ODBC Core SQL grammar.
     * 
     * @param supportsCoreSQLGrammar <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsCoreSQLGrammar( Boolean supportsCoreSQLGrammar ) {
        this.supportsCoreSQLGrammar = supportsCoreSQLGrammar;
    }

    /**
     * Retrieves whether this database supports the ODBC Extended SQL grammar.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsExtendedSQLGrammar() {
        return supportsExtendedSQLGrammar;
    }

    /**
     * Sets whether this database supports the ODBC Extended SQL grammar.
     * 
     * @param supportsExtendedSQLGrammar <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsExtendedSQLGrammar( Boolean supportsExtendedSQLGrammar ) {
        this.supportsExtendedSQLGrammar = supportsExtendedSQLGrammar;
    }

    /**
     * Retrieves whether this database supports the ANSI92 entry level SQL grammar.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsANSI92EntryLevelSQL() {
        return supportsANSI92EntryLevelSQL;
    }

    /**
     * Sets whether this database supports the ANSI92 entry level SQL grammar.
     * 
     * @param supportsANSI92EntryLevelSQL <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsANSI92EntryLevelSQL( Boolean supportsANSI92EntryLevelSQL ) {
        this.supportsANSI92EntryLevelSQL = supportsANSI92EntryLevelSQL;
    }

    /**
     * Retrieves whether this database supports the ANSI92 intermediate SQL grammar supported.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsANSI92IntermediateSQL() {
        return supportsANSI92IntermediateSQL;
    }

    /**
     * Sets whether this database supports the ANSI92 intermediate SQL grammar supported.
     * 
     * @param supportsANSI92IntermediateSQL <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsANSI92IntermediateSQL( Boolean supportsANSI92IntermediateSQL ) {
        this.supportsANSI92IntermediateSQL = supportsANSI92IntermediateSQL;
    }

    /**
     * Retrieves whether this database supports the ANSI92 full SQL grammar supported.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsANSI92FullSQL() {
        return supportsANSI92FullSQL;
    }

    /**
     * Sets whether this database supports the ANSI92 full SQL grammar supported.
     * 
     * @param supportsANSI92FullSQL <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsANSI92FullSQL( Boolean supportsANSI92FullSQL ) {
        this.supportsANSI92FullSQL = supportsANSI92FullSQL;
    }

    /**
     * Retrieves whether this database supports the SQL Integrity Enhancement Facility.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsIntegrityEnhancementFacility() {
        return supportsIntegrityEnhancementFacility;
    }

    /**
     * Sets whether this database supports the SQL Integrity Enhancement Facility.
     * 
     * @param supportsIntegrityEnhancementFacility <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsIntegrityEnhancementFacility( Boolean supportsIntegrityEnhancementFacility ) {
        this.supportsIntegrityEnhancementFacility = supportsIntegrityEnhancementFacility;
    }

    /**
     * Retrieves whether this database supports some form of outer join.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsOuterJoins() {
        return supportsOuterJoins;
    }

    /**
     * Sets whether this database supports some form of outer join.
     * 
     * @param supportsOuterJoins <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsOuterJoins( Boolean supportsOuterJoins ) {
        this.supportsOuterJoins = supportsOuterJoins;
    }

    /**
     * Retrieves whether this database supports full nested outer joins.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsFullOuterJoins() {
        return supportsFullOuterJoins;
    }

    /**
     * Sets whether this database supports full nested outer joins.
     * 
     * @param supportsFullOuterJoins <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsFullOuterJoins( Boolean supportsFullOuterJoins ) {
        this.supportsFullOuterJoins = supportsFullOuterJoins;
    }

    /**
     * Retrieves whether this database provides limited support for outer joins. (This will be <code>true</code> if the method
     * <code>DatabaseMetaData.supportsFullOuterJoins</code> returns <code>true</code>).
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsLimitedOuterJoins() {
        return supportsLimitedOuterJoins;
    }

    /**
     * Sets whether this database provides limited support for outer joins. (This will be <code>true</code> if the method
     * <code>DatabaseMetaData.supportsFullOuterJoins</code> returns <code>true</code>).
     * 
     * @param supportsLimitedOuterJoins <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsLimitedOuterJoins( Boolean supportsLimitedOuterJoins ) {
        this.supportsLimitedOuterJoins = supportsLimitedOuterJoins;
    }

    /**
     * Retrieves the database vendor's preferred term for "schema".
     * 
     * @return the vendor term for "schema"
     */
    public String getSchemaTerm() {
        return schemaTerm;
    }

    /**
     * Sets the database vendor's preferred term for "schema".
     * 
     * @param schemaTerm the vendor term for "schema"
     */
    public void setSchemaTerm( String schemaTerm ) {
        this.schemaTerm = schemaTerm;
    }

    /**
     * Retrieves the database vendor's preferred term for "procedure".
     * 
     * @return the vendor term for "procedure"
     */
    public String getProcedureTerm() {
        return procedureTerm;
    }

    /**
     * Sets the database vendor's preferred term for "procedure".
     * 
     * @param procedureTerm the vendor term for "procedure"
     */
    public void setProcedureTerm( String procedureTerm ) {
        this.procedureTerm = procedureTerm;
    }

    /**
     * Retrieves the database vendor's preferred term for "catalog".
     * 
     * @return the vendor term for "catalog"
     */
    public String getCatalogTerm() {
        return catalogTerm;
    }

    /**
     * Sets the database vendor's preferred term for "catalog".
     * 
     * @param catalogTerm the vendor term for "catalog"
     */
    public void setCatalogTerm( String catalogTerm ) {
        this.catalogTerm = catalogTerm;
    }

    /**
     * Retrieves whether a catalog appears at the start of a fully qualified table name. If not, the catalog appears at the end.
     * 
     * @return <code>true</code> if the catalog name appears at the beginning of a fully qualified table name; <code>false</code>
     *         otherwise
     */
    public Boolean isCatalogAtStart() {
        return catalogAtStart;
    }

    /**
     * Sets whether a catalog appears at the start of a fully qualified table name. If not, the catalog appears at the end.
     * 
     * @param catalogAtStart <code>true</code> if the catalog name appears at the beginning of a fully qualified table name;
     *        <code>false</code> otherwise
     */
    public void setCatalogAtStart( Boolean catalogAtStart ) {
        this.catalogAtStart = catalogAtStart;
    }

    /**
     * Retrieves the <code>String</code> that this database uses as the separator between a catalog and table name.
     * 
     * @return the separator string
     */
    public String getCatalogSeparator() {
        return catalogSeparator;
    }

    /**
     * Sets the <code>String</code> that this database uses as the separator between a catalog and table name.
     * 
     * @param catalogSeparator the separator string
     */
    public void setCatalogSeparator( String catalogSeparator ) {
        this.catalogSeparator = catalogSeparator;
    }

    /**
     * Retrieves whether a schema name can be used in a data manipulation statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSchemasInDataManipulation() {
        return supportsSchemasInDataManipulation;
    }

    /**
     * Sets whether a schema name can be used in a data manipulation statement.
     * 
     * @param supportsSchemasInDataManipulation <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSchemasInDataManipulation( Boolean supportsSchemasInDataManipulation ) {
        this.supportsSchemasInDataManipulation = supportsSchemasInDataManipulation;
    }

    /**
     * Retrieves whether a schema name can be used in a procedure call statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSchemasInProcedureCalls() {
        return supportsSchemasInProcedureCalls;
    }

    /**
     * Sets whether a schema name can be used in a procedure call statement.
     * 
     * @param supportsSchemasInProcedureCalls <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSchemasInProcedureCalls( Boolean supportsSchemasInProcedureCalls ) {
        this.supportsSchemasInProcedureCalls = supportsSchemasInProcedureCalls;
    }

    /**
     * Retrieves whether a schema name can be used in a table definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSchemasInTableDefinitions() {
        return supportsSchemasInTableDefinitions;
    }

    /**
     * Sets whether a schema name can be used in a table definition statement.
     * 
     * @param supportsSchemasInTableDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSchemasInTableDefinitions( Boolean supportsSchemasInTableDefinitions ) {
        this.supportsSchemasInTableDefinitions = supportsSchemasInTableDefinitions;
    }

    /**
     * Retrieves whether a schema name can be used in an index definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSchemasInIndexDefinitions() {
        return supportsSchemasInIndexDefinitions;
    }

    /**
     * Sets whether a schema name can be used in an index definition statement.
     * 
     * @param supportsSchemasInIndexDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSchemasInIndexDefinitions( Boolean supportsSchemasInIndexDefinitions ) {
        this.supportsSchemasInIndexDefinitions = supportsSchemasInIndexDefinitions;
    }

    /**
     * Retrieves whether a schema name can be used in a privilege definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSchemasInPrivilegeDefinitions() {
        return supportsSchemasInPrivilegeDefinitions;
    }

    /**
     * Sets whether a schema name can be used in a privilege definition statement.
     * 
     * @param supportsSchemasInPrivilegeDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSchemasInPrivilegeDefinitions( Boolean supportsSchemasInPrivilegeDefinitions ) {
        this.supportsSchemasInPrivilegeDefinitions = supportsSchemasInPrivilegeDefinitions;
    }

    /**
     * Retrieves whether a catalog name can be used in a data manipulation statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsCatalogsInDataManipulation() {
        return supportsCatalogsInDataManipulation;
    }

    /**
     * Sets whether a catalog name can be used in a data manipulation statement.
     * 
     * @param supportsCatalogsInDataManipulation <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsCatalogsInDataManipulation( Boolean supportsCatalogsInDataManipulation ) {
        this.supportsCatalogsInDataManipulation = supportsCatalogsInDataManipulation;
    }

    /**
     * Retrieves whether a catalog name can be used in a procedure call statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsCatalogsInProcedureCalls() {
        return supportsCatalogsInProcedureCalls;
    }

    /**
     * Sets whether a catalog name can be used in a procedure call statement.
     * 
     * @param supportsCatalogsInProcedureCalls <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsCatalogsInProcedureCalls( Boolean supportsCatalogsInProcedureCalls ) {
        this.supportsCatalogsInProcedureCalls = supportsCatalogsInProcedureCalls;
    }

    /**
     * Retrieves whether a catalog name can be used in a table definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsCatalogsInTableDefinitions() {
        return supportsCatalogsInTableDefinitions;
    }

    /**
     * Sets whether a catalog name can be used in a table definition statement.
     * 
     * @param supportsCatalogsInTableDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsCatalogsInTableDefinitions( Boolean supportsCatalogsInTableDefinitions ) {
        this.supportsCatalogsInTableDefinitions = supportsCatalogsInTableDefinitions;
    }

    /**
     * Retrieves whether a catalog name can be used in an index definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsCatalogsInIndexDefinitions() {
        return supportsCatalogsInIndexDefinitions;
    }

    /**
     * Sets whether a catalog name can be used in an index definition statement.
     * 
     * @param supportsCatalogsInIndexDefinitions <code>true</code> if so; <code>false</code> otherwise
     * 
     */
    public void setSupportsCatalogsInIndexDefinitions( Boolean supportsCatalogsInIndexDefinitions ) {
        this.supportsCatalogsInIndexDefinitions = supportsCatalogsInIndexDefinitions;
    }

    /**
     * Retrieves whether a catalog name can be used in a privilege definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsCatalogsInPrivilegeDefinitions() {
        return supportsCatalogsInPrivilegeDefinitions;
    }

    /**
     * Sets whether a catalog name can be used in a privilege definition statement.
     * 
     * @param supportsCatalogsInPrivilegeDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsCatalogsInPrivilegeDefinitions( Boolean supportsCatalogsInPrivilegeDefinitions ) {
        this.supportsCatalogsInPrivilegeDefinitions = supportsCatalogsInPrivilegeDefinitions;
    }

    /**
     * Retrieves whether this database supports positioned <code>DELETE</code> statements.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsPositionedDelete() {
        return supportsPositionedDelete;
    }

    /**
     * Sets whether this database supports positioned <code>DELETE</code> statements.
     * 
     * @param supportsPositionedDelete <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsPositionedDelete( Boolean supportsPositionedDelete ) {
        this.supportsPositionedDelete = supportsPositionedDelete;
    }

    /**
     * Retrieves whether this database supports positioned <code>UPDATE</code> statements.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsPositionedUpdate() {
        return supportsPositionedUpdate;
    }

    /**
     * Sets whether this database supports positioned <code>UPDATE</code> statements.
     * 
     * @param supportsPositionedUpdate <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsPositionedUpdate( Boolean supportsPositionedUpdate ) {
        this.supportsPositionedUpdate = supportsPositionedUpdate;
    }

    /**
     * Retrieves whether this database supports <code>SELECT FOR UPDATE</code> statements.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSelectForUpdate() {
        return supportsSelectForUpdate;
    }

    /**
     * Sets whether this database supports <code>SELECT FOR UPDATE</code> statements.
     * 
     * @param supportsSelectForUpdate <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSelectForUpdate( Boolean supportsSelectForUpdate ) {
        this.supportsSelectForUpdate = supportsSelectForUpdate;
    }

    /**
     * Retrieves whether this database supports stored procedure calls that use the stored procedure escape syntax.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsStoredProcedures() {
        return supportsStoredProcedures;
    }

    /**
     * Sets whether this database supports stored procedure calls that use the stored procedure escape syntax.
     * 
     * @param supportsStoredProcedures <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsStoredProcedures( Boolean supportsStoredProcedures ) {
        this.supportsStoredProcedures = supportsStoredProcedures;
    }

    /**
     * Retrieves whether this database supports subqueries in comparison expressions.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSubqueriesInComparisons() {
        return supportsSubqueriesInComparisons;
    }

    /**
     * Retrieves whether this database supports subqueries in comparison expressions.
     * 
     * @param supportsSubqueriesInComparisons <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSubqueriesInComparisons( Boolean supportsSubqueriesInComparisons ) {
        this.supportsSubqueriesInComparisons = supportsSubqueriesInComparisons;
    }

    /**
     * Retrieves whether this database supports subqueries in <code>EXISTS</code> expressions.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSubqueriesInExists() {
        return supportsSubqueriesInExists;
    }

    /**
     * Sets whether this database supports subqueries in <code>EXISTS</code> expressions.
     * 
     * @param supportsSubqueriesInExists <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSubqueriesInExists( Boolean supportsSubqueriesInExists ) {
        this.supportsSubqueriesInExists = supportsSubqueriesInExists;
    }

    /**
     * Retrieves whether this database supports subqueries in <code>IN</code> statements.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSubqueriesInIns() {
        return supportsSubqueriesInIns;
    }

    /**
     * Sets whether this database supports subqueries in <code>IN</code> statements.
     * 
     * @param supportsSubqueriesInIns <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSubqueriesInIns( Boolean supportsSubqueriesInIns ) {
        this.supportsSubqueriesInIns = supportsSubqueriesInIns;
    }

    /**
     * Retrieves whether this database supports subqueries in quantified expressions.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsSubqueriesInQuantifieds() {
        return supportsSubqueriesInQuantifieds;
    }

    /**
     * Sets whether this database supports subqueries in quantified expressions.
     * 
     * @param supportsSubqueriesInQuantifieds <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsSubqueriesInQuantifieds( Boolean supportsSubqueriesInQuantifieds ) {
        this.supportsSubqueriesInQuantifieds = supportsSubqueriesInQuantifieds;
    }

    /**
     * Retrieves whether this database supports correlated subqueries.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsCorrelatedSubqueries() {
        return supportsCorrelatedSubqueries;
    }

    /**
     * Sets whether this database supports correlated subqueries.
     * 
     * @param supportsCorrelatedSubqueries <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsCorrelatedSubqueries( Boolean supportsCorrelatedSubqueries ) {
        this.supportsCorrelatedSubqueries = supportsCorrelatedSubqueries;
    }

    /**
     * Retrieves whether this database supports SQL <code>UNION</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsUnion() {
        return supportsUnion;
    }

    /**
     * Sets whether this database supports SQL <code>UNION</code>.
     * 
     * @param supportsUnion <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsUnion( Boolean supportsUnion ) {
        this.supportsUnion = supportsUnion;
    }

    /**
     * Retrieves whether this database supports SQL <code>UNION ALL</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsUnionAll() {
        return supportsUnionAll;
    }

    /**
     * Sets whether this database supports SQL <code>UNION ALL</code>.
     * 
     * @param supportsUnionAll <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsUnionAll( Boolean supportsUnionAll ) {
        this.supportsUnionAll = supportsUnionAll;
    }

    /**
     * Retrieves whether this database supports keeping cursors open across commits.
     * 
     * @return <code>true</code> if cursors always remain open; <code>false</code> if they might not remain open
     */
    public Boolean isSupportsOpenCursorsAcrossCommit() {
        return supportsOpenCursorsAcrossCommit;
    }

    /**
     * Sets whether this database supports keeping cursors open across commits.
     * 
     * @param supportsOpenCursorsAcrossCommit <code>true</code> if cursors always remain open; <code>false</code> if they might
     *        not remain open
     */
    public void setSupportsOpenCursorsAcrossCommit( Boolean supportsOpenCursorsAcrossCommit ) {
        this.supportsOpenCursorsAcrossCommit = supportsOpenCursorsAcrossCommit;
    }

    /**
     * Retrieves whether this database supports keeping cursors open across rollbacks.
     * 
     * @return <code>true</code> if cursors always remain open; <code>false</code> if they might not remain open
     */
    public Boolean isSupportsOpenCursorsAcrossRollback() {
        return supportsOpenCursorsAcrossRollback;
    }

    /**
     * Sets whether this database supports keeping cursors open across rollbacks.
     * 
     * @param supportsOpenCursorsAcrossRollback <code>true</code> if cursors always remain open; <code>false</code> if they might
     *        not remain open
     */
    public void setSupportsOpenCursorsAcrossRollback( Boolean supportsOpenCursorsAcrossRollback ) {
        this.supportsOpenCursorsAcrossRollback = supportsOpenCursorsAcrossRollback;
    }

    /**
     * Retrieves whether this database supports keeping statements open across commits.
     * 
     * @return <code>true</code> if statements always remain open; <code>false</code> if they might not remain open
     */
    public Boolean isSupportsOpenStatementsAcrossCommit() {
        return supportsOpenStatementsAcrossCommit;
    }

    /**
     * sets whether this database supports keeping statements open across commits.
     * 
     * @param supportsOpenStatementsAcrossCommit <code>true</code> if statements always remain open; <code>false</code> if they
     *        might not remain open
     */
    public void setSupportsOpenStatementsAcrossCommit( Boolean supportsOpenStatementsAcrossCommit ) {
        this.supportsOpenStatementsAcrossCommit = supportsOpenStatementsAcrossCommit;
    }

    /**
     * Retrieves whether this database supports keeping statements open across rollbacks.
     * 
     * @return <code>true</code> if statements always remain open; <code>false</code> if they might not remain open
     */
    public Boolean isSupportsOpenStatementsAcrossRollback() {
        return supportsOpenStatementsAcrossRollback;
    }

    /**
     * Sets whether this database supports keeping statements open across rollbacks.
     * 
     * @param supportsOpenStatementsAcrossRollback <code>true</code> if statements always remain open; <code>false</code> if they
     *        might not remain open
     */
    public void setSupportsOpenStatementsAcrossRollback( Boolean supportsOpenStatementsAcrossRollback ) {
        this.supportsOpenStatementsAcrossRollback = supportsOpenStatementsAcrossRollback;
    }

    // ----------------------------------------------------------------------
    // The following group of methods exposes various limitations based on the target
    // database with the current driver. Unless otherwise specified, a result of zero
    // means there is no limit, or the limit is not known.
    // ----------------------------------------------------------------------

    /**
     * Retrieves the maximum number of hex characters this database allows in an inline binary literal.
     * 
     * @return max the maximum length (in hex characters) for a binary literal; a result of zero means that there is no limit or
     *         the limit is not known
     */
    public Integer getMaxBinaryLiteralLength() {
        return maxBinaryLiteralLength;
    }

    /**
     * sets the maximum number of hex characters this database allows in an inline binary literal.
     * 
     * @param maxBinaryLiteralLength max the maximum length (in hex characters) for a binary literal; a result of zero means that
     *        there is no limit or the limit is not known
     */
    public void setMaxBinaryLiteralLength( Integer maxBinaryLiteralLength ) {
        this.maxBinaryLiteralLength = maxBinaryLiteralLength;
    }

    /**
     * Retrieves the maximum number of characters this database allows for a character literal.
     * 
     * @return the maximum number of characters allowed for a character literal; a result of zero means that there is no limit or
     *         the limit is not known
     */
    public Integer getMaxCharLiteralLength() {
        return maxCharLiteralLength;
    }

    /**
     * Sets the maximum number of characters this database allows for a character literal.
     * 
     * @param maxCharLiteralLength the maximum number of characters allowed for a character literal; a result of zero means that
     *        there is no limit or the limit is not known
     */
    public void setMaxCharLiteralLength( Integer maxCharLiteralLength ) {
        this.maxCharLiteralLength = maxCharLiteralLength;
    }

    /**
     * Retrieves the maximum number of characters this database allows for a column name.
     * 
     * @return the maximum number of characters allowed for a column name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxColumnNameLength() {
        return maxColumnNameLength;
    }

    /**
     * Sets the maximum number of characters this database allows for a column name.
     * 
     * @param maxColumnNameLength the maximum number of characters allowed for a column name; a result of zero means that there is
     *        no limit or the limit is not known
     */
    public void setMaxColumnNameLength( Integer maxColumnNameLength ) {
        this.maxColumnNameLength = maxColumnNameLength;
    }

    /**
     * Retrieves the maximum number of columns this database allows in a <code>GROUP BY</code> clause.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    public Integer getMaxColumnsInGroupBy() {
        return maxColumnsInGroupBy;
    }

    /**
     * Sets the maximum number of columns this database allows in a <code>GROUP BY</code> clause.
     * 
     * @param maxColumnsInGroupBy the maximum number of columns allowed; a result of zero means that there is no limit or the
     *        limit is not known
     */
    public void setMaxColumnsInGroupBy( Integer maxColumnsInGroupBy ) {
        this.maxColumnsInGroupBy = maxColumnsInGroupBy;
    }

    /**
     * Retrieves the maximum number of columns this database allows in an index.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    public Integer getMaxColumnsInIndex() {
        return maxColumnsInIndex;
    }

    /**
     * Sets the maximum number of columns this database allows in an index.
     * 
     * @param maxColumnsInIndex the maximum number of columns allowed; a result of zero means that there is no limit or the limit
     *        is not known
     */
    public void setMaxColumnsInIndex( Integer maxColumnsInIndex ) {
        this.maxColumnsInIndex = maxColumnsInIndex;
    }

    /**
     * Retrieves the maximum number of columns this database allows in an <code>ORDER BY</code> clause.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    public Integer getMaxColumnsInOrderBy() {
        return maxColumnsInOrderBy;
    }

    /**
     * Sets the maximum number of columns this database allows in an <code>ORDER BY</code> clause.
     * 
     * @param maxColumnsInOrderBy the maximum number of columns allowed; a result of zero means that there is no limit or the
     *        limit is not known
     */
    public void setMaxColumnsInOrderBy( Integer maxColumnsInOrderBy ) {
        this.maxColumnsInOrderBy = maxColumnsInOrderBy;
    }

    /**
     * Retrieves the maximum number of columns this database allows in a <code>SELECT</code> list.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    public Integer getMaxColumnsInSelect() {
        return maxColumnsInSelect;
    }

    /**
     * Sets the maximum number of columns this database allows in a <code>SELECT</code> list.
     * 
     * @param maxColumnsInSelect the maximum number of columns allowed; a result of zero means that there is no limit or the limit
     *        is not known
     */
    public void setMaxColumnsInSelect( Integer maxColumnsInSelect ) {
        this.maxColumnsInSelect = maxColumnsInSelect;
    }

    /**
     * Retrieves the maximum number of columns this database allows in a table.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    public Integer getMaxColumnsInTable() {
        return maxColumnsInTable;
    }

    /**
     * Sets the maximum number of columns this database allows in a table.
     * 
     * @param maxColumnsInTable the maximum number of columns allowed; a result of zero means that there is no limit or the limit
     *        is not known
     */
    public void setMaxColumnsInTable( Integer maxColumnsInTable ) {
        this.maxColumnsInTable = maxColumnsInTable;
    }

    /**
     * Retrieves the maximum number of concurrent connections to this database that are possible.
     * 
     * @return the maximum number of active connections possible at one time; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxConnections() {
        return maxConnections;
    }

    /**
     * Sets the maximum number of concurrent connections to this database that are possible.
     * 
     * @param maxConnections the maximum number of active connections possible at one time; a result of zero means that there is
     *        no limit or the limit is not known
     */
    public void setMaxConnections( Integer maxConnections ) {
        this.maxConnections = maxConnections;
    }

    /**
     * Retrieves the maximum number of characters that this database allows in a cursor name.
     * 
     * @return the maximum number of characters allowed in a cursor name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxCursorNameLength() {
        return maxCursorNameLength;
    }

    /**
     * Sets the maximum number of characters that this database allows in a cursor name.
     * 
     * @param maxCursorNameLength the maximum number of characters allowed in a cursor name; a result of zero means that there is
     *        no limit or the limit is not known
     */
    public void setMaxCursorNameLength( Integer maxCursorNameLength ) {
        this.maxCursorNameLength = maxCursorNameLength;
    }

    /**
     * Retrieves the maximum number of bytes this database allows for an index, including all of the parts of the index.
     * 
     * @return the maximum number of bytes allowed; this limit includes the composite of all the constituent parts of the index; a
     *         result of zero means that there is no limit or the limit is not known
     */
    public Integer getMaxIndexLength() {
        return maxIndexLength;
    }

    /**
     * Sets the maximum number of bytes this database allows for an index, including all of the parts of the index.
     * 
     * @param maxIndexLength the maximum number of bytes allowed; this limit includes the composite of all the constituent parts
     *        of the index; a result of zero means that there is no limit or the limit is not known
     */
    public void setMaxIndexLength( Integer maxIndexLength ) {
        this.maxIndexLength = maxIndexLength;
    }

    /**
     * Retrieves the maximum number of characters that this database allows in a schema name.
     * 
     * @return the maximum number of characters allowed in a schema name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxSchemaNameLength() {
        return maxSchemaNameLength;
    }

    /**
     * Sets the maximum number of characters that this database allows in a schema name.
     * 
     * @param maxSchemaNameLength the maximum number of characters allowed in a schema name; a result of zero means that there is
     *        no limit or the limit is not known
     */
    public void setMaxSchemaNameLength( Integer maxSchemaNameLength ) {
        this.maxSchemaNameLength = maxSchemaNameLength;
    }

    /**
     * Retrieves the maximum number of characters that this database allows in a procedure name.
     * 
     * @return the maximum number of characters allowed in a procedure name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxProcedureNameLength() {
        return maxProcedureNameLength;
    }

    /**
     * Sets the maximum number of characters that this database allows in a procedure name.
     * 
     * @param maxProcedureNameLength the maximum number of characters allowed in a procedure name; a result of zero means that
     *        there is no limit or the limit is not known
     */
    public void setMaxProcedureNameLength( Integer maxProcedureNameLength ) {
        this.maxProcedureNameLength = maxProcedureNameLength;
    }

    /**
     * Retrieves the maximum number of characters that this database allows in a catalog name.
     * 
     * @return the maximum number of characters allowed in a catalog name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxCatalogNameLength() {
        return maxCatalogNameLength;
    }

    /**
     * Sets the maximum number of characters that this database allows in a catalog name.
     * 
     * @param maxCatalogNameLength the maximum number of characters allowed in a catalog name; a result of zero means that there
     *        is no limit or the limit is not known
     */
    public void setMaxCatalogNameLength( Integer maxCatalogNameLength ) {
        this.maxCatalogNameLength = maxCatalogNameLength;
    }

    /**
     * Retrieves the maximum number of bytes this database allows in a single row.
     * 
     * @return the maximum number of bytes allowed for a row; a result of zero means that there is no limit or the limit is not
     *         known
     */
    public Integer getMaxRowSize() {
        return maxRowSize;
    }

    /**
     * Sets the maximum number of bytes this database allows in a single row.
     * 
     * @param maxRowSize the maximum number of bytes allowed for a row; a result of zero means that there is no limit or the limit
     *        is not known
     */
    public void setMaxRowSize( Integer maxRowSize ) {
        this.maxRowSize = maxRowSize;
    }

    /**
     * Retrieves whether the return value for the method <code>getMaxRowSize</code> includes the SQL data types
     * <code>LONGVARCHAR</code> and <code>LONGVARBINARY</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isMaxRowSizeIncludeBlobs() {
        return maxRowSizeIncludeBlobs;
    }

    /**
     * Sets whether the return value for the method <code>getMaxRowSize</code> includes the SQL data types
     * <code>LONGVARCHAR</code> and <code>LONGVARBINARY</code>.
     * 
     * @param maxRowSizeIncludeBlobs <code>true</code> if so; <code>false</code> otherwise
     */
    public void setMaxRowSizeIncludeBlobs( Boolean maxRowSizeIncludeBlobs ) {
        this.maxRowSizeIncludeBlobs = maxRowSizeIncludeBlobs;
    }

    /**
     * Retrieves the maximum number of characters this database allows in an SQL statement.
     * 
     * @return the maximum number of characters allowed for an SQL statement; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxStatementLength() {
        return maxStatementLength;
    }

    /**
     * Sets the maximum number of characters this database allows in an SQL statement.
     * 
     * @param maxStatementLength the maximum number of characters allowed for an SQL statement; a result of zero means that there
     *        is no limit or the limit is not known
     */
    public void setMaxStatementLength( Integer maxStatementLength ) {
        this.maxStatementLength = maxStatementLength;
    }

    /**
     * Retrieves the maximum number of active statements to this database that can be open at the same time.
     * 
     * @return the maximum number of statements that can be open at one time; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxStatements() {
        return maxStatements;
    }

    /**
     * Sets the maximum number of active statements to this database that can be open at the same time.
     * 
     * @param maxStatements the maximum number of statements that can be open at one time; a result of zero means that there is no
     *        limit or the limit is not known
     */
    public void setMaxStatements( Integer maxStatements ) {
        this.maxStatements = maxStatements;
    }

    /**
     * Retrieves the maximum number of characters this database allows in a table name.
     * 
     * @return the maximum number of characters allowed for a table name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxTableNameLength() {
        return maxTableNameLength;
    }

    /**
     * Sets the maximum number of characters this database allows in a table name.
     * 
     * @param maxTableNameLength the maximum number of characters allowed for a table name; a result of zero means that there is
     *        no limit or the limit is not known
     */
    public void setMaxTableNameLength( Integer maxTableNameLength ) {
        this.maxTableNameLength = maxTableNameLength;
    }

    /**
     * Retrieves the maximum number of tables this database allows in a <code>SELECT</code> statement.
     * 
     * @return the maximum number of tables allowed in a <code>SELECT</code> statement; a result of zero means that there is no
     *         limit or the limit is not known
     */
    public Integer getMaxTablesInSelect() {
        return maxTablesInSelect;
    }

    /**
     * Sets the maximum number of tables this database allows in a <code>SELECT</code> statement.
     * 
     * @param maxTablesInSelect the maximum number of tables allowed in a <code>SELECT</code> statement; a result of zero means
     *        that there is no limit or the limit is not known
     */
    public void setMaxTablesInSelect( Integer maxTablesInSelect ) {
        this.maxTablesInSelect = maxTablesInSelect;
    }

    /**
     * Retrieves the maximum number of characters this database allows in a user name.
     * 
     * @return the maximum number of characters allowed for a user name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    public Integer getMaxUserNameLength() {
        return maxUserNameLength;
    }

    /**
     * Sets the maximum number of characters this database allows in a user name.
     * 
     * @param maxUserNameLength the maximum number of characters allowed for a user name; a result of zero means that there is no
     *        limit or the limit is not known
     */
    public void setMaxUserNameLength( Integer maxUserNameLength ) {
        this.maxUserNameLength = maxUserNameLength;
    }

    /**
     * Retrieves this database's default transaction isolation level. The possible values are defined in
     * <code>java.sql.Connection</code>.
     * 
     * @return the default isolation level
     * @see java.sql.Connection
     */
    public Integer getDefaultTransactionIsolation() {
        return defaultTransactionIsolation;
    }

    /**
     * Sets this database's default transaction isolation level. The possible values are defined in
     * <code>java.sql.Connection</code>.
     * 
     * @param defaultTransactionIsolation the default isolation level
     * @see java.sql.Connection
     */
    public void setDefaultTransactionIsolation( Integer defaultTransactionIsolation ) {
        this.defaultTransactionIsolation = defaultTransactionIsolation;
    }

    /**
     * Retrieves whether this database supports transactions. If not, invoking the method <code>commit</code> is a noop, and the
     * isolation level is <code>TRANSACTION_NONE</code>.
     * 
     * @return <code>true</code> if transactions are supported; <code>false</code> otherwise
     */
    public Boolean isSupportsTransactions() {
        return supportsTransactions;
    }

    /**
     * Sets whether this database supports transactions. If not, invoking the method <code>commit</code> is a noop, and the
     * isolation level is <code>TRANSACTION_NONE</code>.
     * 
     * @param supportsTransactions <code>true</code> if transactions are supported; <code>false</code> otherwise
     */
    public void setSupportsTransactions( Boolean supportsTransactions ) {
        this.supportsTransactions = supportsTransactions;
    }

    /**
     * Retrieves list of database supported transaction isolation levels.
     * 
     * @return list of database supported transaction isolation levels.
     * @see java.sql.Connection
     */
    public Set<TransactionIsolationLevelType> getSupportedTransactionIsolationLevels() {
        return supportedTransactionIsolationLevels;
    }

    /**
     * Adds TransactionIsolationLevelType
     * 
     * @param transactionIsolationLevelType the Transaction Isolation Level Type
     */
    public void addSupportedTransactionIsolationLevelType( TransactionIsolationLevelType transactionIsolationLevelType ) {
        supportedTransactionIsolationLevels.add(transactionIsolationLevelType);
    }

    /**
     * Deletes TransactionIsolationLevelType
     * 
     * @param transactionIsolationLevelType the Transaction Isolation Level Type
     */
    public void deleteSupportedTransactionIsolationLevelType( TransactionIsolationLevelType transactionIsolationLevelType ) {
        supportedTransactionIsolationLevels.remove(transactionIsolationLevelType);
    }

    /**
     * Is supported TransactionIsolationLevelType
     * 
     * @param transactionIsolationLevelType the Transaction Isolation Level Type
     * @return true if supported
     */
    public Boolean isSupportedTransactionIsolationLevelType( TransactionIsolationLevelType transactionIsolationLevelType ) {
        for (TransactionIsolationLevelType t : supportedTransactionIsolationLevels) {
            if (t.equals(transactionIsolationLevelType)) {
                return Boolean.TRUE;
            }
        }
        // return false
        return Boolean.FALSE;
    }

    /**
     * Retrieves whether this database supports both data definition and data manipulation statements within a transaction.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsDataDefinitionAndDataManipulationTransactions() {
        return supportsDataDefinitionAndDataManipulationTransactions;
    }

    /**
     * Sets whether this database supports both data definition and data manipulation statements within a transaction.
     * 
     * @param supportsDataDefinitionAndDataManipulationTransactions <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsDataDefinitionAndDataManipulationTransactions( Boolean supportsDataDefinitionAndDataManipulationTransactions ) {
        this.supportsDataDefinitionAndDataManipulationTransactions = supportsDataDefinitionAndDataManipulationTransactions;
    }

    /**
     * Retrieves whether this database supports only data manipulation statements within a transaction.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isSupportsDataManipulationTransactionsOnly() {
        return supportsDataManipulationTransactionsOnly;
    }

    /**
     * Sets whether this database supports only data manipulation statements within a transaction.
     * 
     * @param supportsDataManipulationTransactionsOnly <code>true</code> if so; <code>false</code> otherwise
     */
    public void setSupportsDataManipulationTransactionsOnly( Boolean supportsDataManipulationTransactionsOnly ) {
        this.supportsDataManipulationTransactionsOnly = supportsDataManipulationTransactionsOnly;
    }

    /**
     * Retrieves whether a data definition statement within a transaction forces the transaction to commit.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isDataDefinitionCausesTransactionCommit() {
        return dataDefinitionCausesTransactionCommit;
    }

    /**
     * Sets whether a data definition statement within a transaction forces the transaction to commit.
     * 
     * @param dataDefinitionCausesTransactionCommit <code>true</code> if so; <code>false</code> otherwise
     */
    public void setDataDefinitionCausesTransactionCommit( Boolean dataDefinitionCausesTransactionCommit ) {
        this.dataDefinitionCausesTransactionCommit = dataDefinitionCausesTransactionCommit;
    }

    /**
     * Retrieves whether this database ignores a data definition statement within a transaction.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    public Boolean isDataDefinitionIgnoredInTransactions() {
        return dataDefinitionIgnoredInTransactions;
    }

    /**
     * Sets whether this database ignores a data definition statement within a transaction.
     * 
     * @param dataDefinitionIgnoredInTransactions <code>true</code> if so; <code>false</code> otherwise
     */
    public void setDataDefinitionIgnoredInTransactions( Boolean dataDefinitionIgnoredInTransactions ) {
        this.dataDefinitionIgnoredInTransactions = dataDefinitionIgnoredInTransactions;
    }

    /**
     * Retrieves a description of the stored procedures available in the given catalog.
     * 
     * @return a set of stored procedures available
     */
    public Set<StoredProcedure> getStoredProcedures() {
        return storedProcedures;
    }

    /**
     * Adds Stored Procedure
     * 
     * @param storedProcedure the Stored Procedure
     */
    public void addStoredProcedure( StoredProcedure storedProcedure ) {
        storedProcedures.add(storedProcedure);
    }

    /**
     * Deletes Stored Procedure
     * 
     * @param storedProcedure the Stored Procedure
     */
    public void deleteStoredProcedure( StoredProcedure storedProcedure ) {
        storedProcedures.remove(storedProcedure);
    }

    /**
     * Finds stored procedute by its name.
     * 
     * @param catalog a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a
     *        catalog; <code>null</code> means that the catalog name should not be used to narrow the search
     * @param schema a schema name; must match the schema name as it is stored in the database; "" retrieves those without a
     *        schema; <code>null</code> means that the schema name should not be used to narrow the search
     * @param procedureName a procedure name; must match the procedure name as it is stored in the database
     * @return stored procedure or null if not found
     */
    public StoredProcedure findStoredProcedureByName( String catalog,
                                                      String schema,
                                                      String procedureName ) {
        for (StoredProcedure sp : storedProcedures) {
            // if name equals then trying to match catalog and schema
            if (sp.getName().equals(procedureName)) {
                boolean catalogNameEquals = (sp.getCatalog() != null) ? sp.getCatalog().getName().equals(catalog) : catalog == null;
                boolean schemaNameEquals = (sp.getSchema() != null) ? sp.getSchema().getName().equals(schema) : schema == null;
                if (catalogNameEquals && schemaNameEquals) {
                    return sp;
                }
            }
        }
        // return nothing
        return null;
    }

    /**
     * Retrieves a description of the tables available in the given catalog.
     * 
     * @return a set of tables available
     */
    public Set<Table> getTables() {
        return tables;
    }

    /**
     * Adds Table
     * 
     * @param table the table to add
     */
    public void addTable( Table table ) {
        tables.add(table);
    }

    /**
     * Deletes Table
     * 
     * @param table the table to delete
     */
    public void deleteTable( Table table ) {
        tables.remove(table);
    }

    /**
     * Finds table by its name.
     * 
     * @param catalog a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a
     *        catalog; <code>null</code> means that the catalog name should not be used to narrow the search
     * @param schema a schema name; must match the schema name as it is stored in the database; "" retrieves those without a
     *        schema; <code>null</code> means that the schema name should not be used to narrow the search
     * @param tableName a table name; must match the table name as it is stored in the database
     * @return table or null if not found
     */
    public Table findTableByName( String catalog,
                                  String schema,
                                  String tableName ) {
        for (Table t : tables) {
            // if name equals then trying to match catalog and schema
            if (t.getName().equals(tableName)) {
                boolean catalogNameEquals = (t.getCatalog() != null) ? t.getCatalog().getName().equals(catalog) : catalog == null;
                boolean schemaNameEquals = (t.getSchema() != null) ? t.getSchema().getName().equals(schema) : schema == null;
                if (catalogNameEquals && schemaNameEquals) {
                    return t;
                }
            }
        }
        // return nothing
        return null;
    }

    /**
     * Retrieves the schemas available in this database. The results are ordered by schema name.
     * 
     * @return schemas available in this database.
     */
    public Set<Schema> getSchemas() {
        return schemas;
    }

    /**
     * Adds Schema
     * 
     * @param schema the Schema
     */
    public void addSchema( Schema schema ) {
        schemas.add(schema);
    }

    /**
     * Deletes Schema
     * 
     * @param schema the Schema
     */
    public void deleteSchema( Schema schema ) {
        schemas.remove(schema);
    }

    /**
     * Finds schema by its name.
     * 
     * @param catalog a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a
     *        catalog; <code>null</code> means that the catalog name should not be used to narrow the search
     * @param schemaName a schema name; must match the schema name as it is stored in the database;
     * @return schema or null if not found
     */
    public Schema findSchemaByName( String catalog,
                                    String schemaName ) {
        for (Schema s : schemas) {
            // if name equals then trying to match catalog and schema
            if (s.getName().equals(schemaName)) {
                boolean catalogNameEquals = (s.getCatalog() != null) ? s.getCatalog().getName().equals(catalog) : catalog == null;
                if (catalogNameEquals) {
                    return s;
                }
            }
        }
        // return nothing
        return null;
    }

    /**
     * Retrieves the catalogs available in this database
     * 
     * @return catalogs available in this database
     */
    public Set<Catalog> getCatalogs() {
        return catalogs;
    }

    /**
     * Adds Catalog
     * 
     * @param catalog the catalog to add
     */
    public void addCatalog( Catalog catalog ) {
        catalogs.add(catalog);
    }

    /**
     * Deletes Catalog
     * 
     * @param catalog the catalog to delete
     */
    public void deleteCatalog( Catalog catalog ) {
        catalogs.remove(catalog);
    }

    /**
     * Finds catalog by its name.
     * 
     * @param catalogName a catalog name; must match the catalog name as it is stored in the database;
     * @return catalog or null if not found
     */
    public Catalog findCatalogByName( String catalogName ) {
        for (Catalog c : catalogs) {
            // if name equals then return
            if (c.getName().equals(catalogName)) {
                return c;
            }
        }
        // return nothing
        return null;
    }

    /**
     * Retrieves the table types available in this database. The results are ordered by table type.
     * <P>
     * The table type is:
     * <OL>
     * <LI><B>TABLE_TYPE</B> String => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY",
     * "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     * </OL>
     * 
     * @return table types available in this database
     */
    public Set<TableType> getTableTypes() {
        return tableTypes;
    }

    /**
     * Adds TableType
     * 
     * @param tableType the table type to add
     */
    public void addTableType( TableType tableType ) {
        tableTypes.add(tableType);
    }

    /**
     * Deletes TableType
     * 
     * @param tableType the table type to delete
     */
    public void deleteTableType( TableType tableType ) {
        tableTypes.remove(tableType);
    }

    /**
     * Finds table type by its name.
     * 
     * @param typeName a table type name; must match the type name as it is stored in the database;
     * @return table type or null if not found
     */
    public TableType findTableTypeByTypeName( String typeName ) {
        for (TableType tt : tableTypes) {
            // if name equals then return
            if (tt.getName().equals(typeName)) {
                return tt;
            }
        }
        // return nothing
        return null;
    }

    /**
     * Retrieves a description of all the standard SQL types supported by this database
     * 
     * @return all the standard SQL types supported by this database
     */
    public Set<SqlTypeInfo> getSqlTypeInfos() {
        return sqlTypeInfos;
    }

    /**
     * Adds SqlTypeInfo
     * 
     * @param sqlTypeInfo the SQL type to add
     */
    public void addSqlTypeInfo( SqlTypeInfo sqlTypeInfo ) {
        sqlTypeInfos.add(sqlTypeInfo);
    }

    /**
     * Deletes SqlTypeInfo
     * 
     * @param sqlTypeInfo the SQL type to delete
     */
    public void deleteSqlTypeInfo( SqlTypeInfo sqlTypeInfo ) {
        sqlTypeInfos.remove(sqlTypeInfo);
    }

    /**
     * Finds SQL type by its name.
     * 
     * @param typeName a table type name; must match the type name as it is stored in the database;
     * @return table type or null if not found
     */
    public SqlTypeInfo findSqlTypeInfoByTypeName( String typeName ) {
        for (SqlTypeInfo sti : sqlTypeInfos) {
            // if name equals then return
            if (sti.getName().equals(typeName)) {
                return sti;
            }
        }
        // return nothing
        return null;
    }

    // ===============================================================
    // --------------------------JDBC 2.0-----------------------------
    // ===============================================================

    /**
     * Retrieves database supported result set types.
     * 
     * @return database supported result set types.
     * @see java.sql.Connection
     * @since 1.2 (JDBC 2.0)
     */
    public Set<ResultSetType> getSupportedResultSetTypes() {
        return supportedResultSetTypes;
    }

    /**
     * Adds supported ResultSetType
     * 
     * @param resultSetType the ResultSetType
     */
    public void addSupportedResultSetType( ResultSetType resultSetType ) {
        supportedResultSetTypes.add(resultSetType);
    }

    /**
     * Deletes supported ResultSetType
     * 
     * @param resultSetType the ResultSetType
     */
    public void deleteSupportedResultSetType( ResultSetType resultSetType ) {
        supportedResultSetTypes.remove(resultSetType);
    }

    /**
     * Retrieves database supported concurrencies for the given result set type.
     * 
     * @return database supported concurrencies for the given result set type.
     * @see java.sql.Connection
     * @since 1.2 (JDBC 2.0)
     */
    public Set<ResultSetConcurrencyType> getSupportedForwardOnlyResultSetConcurrencies() {
        return supportedForwardOnlyResultSetConcurrencies;
    }

    /**
     * Adds ResultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    public void addSupportedForwardOnlyResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType ) {
        supportedForwardOnlyResultSetConcurrencies.add(resultSetConcurrencyType);
    }

    /**
     * Deletes ResultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    public void deleteSupportedForwardOnlyResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType ) {
        supportedForwardOnlyResultSetConcurrencies.remove(resultSetConcurrencyType);
    }

    /**
     * Retrieves database supported concurrencies for the given result set type.
     * 
     * @return database supported concurrencies for the given result set type.
     * @see java.sql.Connection
     * @since 1.2 (JDBC 2.0)
     */
    public Set<ResultSetConcurrencyType> getSupportedScrollInsensitiveResultSetConcurrencies() {
        return supportedScrollInsensitiveResultSetConcurrencies;
    }

    /**
     * Adds ResultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    public void addSupportedScrollInsensitiveResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType ) {
        supportedScrollInsensitiveResultSetConcurrencies.add(resultSetConcurrencyType);
    }

    /**
     * Delete ResultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    public void deleteSupportedScrollInsensitiveResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType ) {
        supportedScrollInsensitiveResultSetConcurrencies.remove(resultSetConcurrencyType);
    }

    /**
     * Retrieves database supported concurrencies for the given result set type.
     * 
     * @return database supported concurrencies for the given result set type.
     * @see java.sql.Connection
     * @since 1.2 (JDBC 2.0)
     */
    public Set<ResultSetConcurrencyType> getSupportedScrollSensitiveResultSetConcurrencies() {
        return supportedScrollSensitiveResultSetConcurrencies;
    }

    /**
     * Adds resultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    public void addSupportedScrollSensitiveResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType ) {
        supportedScrollSensitiveResultSetConcurrencies.add(resultSetConcurrencyType);
    }

    /**
     * deletes resultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    public void deleteSupportedScrollSensitiveResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType ) {
        supportedScrollSensitiveResultSetConcurrencies.remove(resultSetConcurrencyType);
    }

    /**
     * Retrieves whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @return <code>true</code> if updates are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isForwardOnlyResultSetOwnUpdatesAreVisible() {
        return forwardOnlyResultSetOwnUpdatesAreVisible;
    }

    /**
     * Sets whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @param forwardOnlyResultSetOwnUpdatesAreVisible <code>true</code> if updates are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setForwardOnlyResultSetOwnUpdatesAreVisible( Boolean forwardOnlyResultSetOwnUpdatesAreVisible ) {
        this.forwardOnlyResultSetOwnUpdatesAreVisible = forwardOnlyResultSetOwnUpdatesAreVisible;
    }

    /**
     * Retrieves whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @return <code>true</code> if updates are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollInsensitiveResultSetOwnUpdatesAreVisible() {
        return scrollInsensitiveResultSetOwnUpdatesAreVisible;
    }

    /**
     * Sets whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @param scrollInsensitiveResultSetOwnUpdatesAreVisible <code>true</code> if updates are visible for the given result set
     *        type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollInsensitiveResultSetOwnUpdatesAreVisible( Boolean scrollInsensitiveResultSetOwnUpdatesAreVisible ) {
        this.scrollInsensitiveResultSetOwnUpdatesAreVisible = scrollInsensitiveResultSetOwnUpdatesAreVisible;
    }

    /**
     * Retrieves whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @return <code>true</code> if updates are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollSensitiveResultSetOwnUpdatesAreVisible() {
        return scrollSensitiveResultSetOwnUpdatesAreVisible;
    }

    /**
     * Sets whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @param scrollSensitiveResultSetOwnUpdatesAreVisible <code>true</code> if updates are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollSensitiveResultSetOwnUpdatesAreVisible( Boolean scrollSensitiveResultSetOwnUpdatesAreVisible ) {
        this.scrollSensitiveResultSetOwnUpdatesAreVisible = scrollSensitiveResultSetOwnUpdatesAreVisible;
    }

    /**
     * Retrieves whether a result set's own deletes are visible.
     * 
     * @return <code>true</code> if deletes are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isForwardOnlyResultSetOwnDeletesAreVisible() {
        return forwardOnlyResultSetOwnDeletesAreVisible;
    }

    /**
     * Sets whether a result set's own deletes are visible.
     * 
     * @param forwardOnlyResultSetOwnDeletesAreVisible <code>true</code> if deletes are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setForwardOnlyResultSetOwnDeletesAreVisible( Boolean forwardOnlyResultSetOwnDeletesAreVisible ) {
        this.forwardOnlyResultSetOwnDeletesAreVisible = forwardOnlyResultSetOwnDeletesAreVisible;
    }

    /**
     * Retrieves whether a result set's own deletes are visible.
     * 
     * @return <code>true</code> if deletes are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollInsensitiveResultSetOwnDeletesAreVisible() {
        return scrollInsensitiveResultSetOwnDeletesAreVisible;
    }

    /**
     * Sets whether a result set's own deletes are visible.
     * 
     * @param scrollInsensitiveResultSetOwnDeletesAreVisible <code>true</code> if deletes are visible for the given result set
     *        type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollInsensitiveResultSetOwnDeletesAreVisible( Boolean scrollInsensitiveResultSetOwnDeletesAreVisible ) {
        this.scrollInsensitiveResultSetOwnDeletesAreVisible = scrollInsensitiveResultSetOwnDeletesAreVisible;
    }

    /**
     * Retrieves whether a result set's own deletes are visible.
     * 
     * @return <code>true</code> if deletes are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollSensitiveResultSetOwnDeletesAreVisible() {
        return scrollSensitiveResultSetOwnDeletesAreVisible;
    }

    /**
     * Sets whether a result set's own deletes are visible.
     * 
     * @param scrollSensitiveResultSetOwnDeletesAreVisible <code>true</code> if deletes are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollSensitiveResultSetOwnDeletesAreVisible( Boolean scrollSensitiveResultSetOwnDeletesAreVisible ) {
        this.scrollSensitiveResultSetOwnDeletesAreVisible = scrollSensitiveResultSetOwnDeletesAreVisible;
    }

    /**
     * Retrieves whether a result set's own inserts are visible.
     * 
     * @return <code>true</code> if inserts are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isForwardOnlyResultSetOwnInsertsAreVisible() {
        return forwardOnlyResultSetOwnInsertsAreVisible;
    }

    /**
     * Sets whether a result set's own inserts are visible.
     * 
     * @param forwardOnlyResultSetOwnInsertsAreVisible <code>true</code> if inserts are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setForwardOnlyResultSetOwnInsertsAreVisible( Boolean forwardOnlyResultSetOwnInsertsAreVisible ) {
        this.forwardOnlyResultSetOwnInsertsAreVisible = forwardOnlyResultSetOwnInsertsAreVisible;
    }

    /**
     * Retrieves whether a result set's own inserts are visible.
     * 
     * @return <code>true</code> if inserts are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollInsensitiveResultSetOwnInsertsAreVisible() {
        return scrollInsensitiveResultSetOwnInsertsAreVisible;
    }

    /**
     * Sets whether a result set's own inserts are visible.
     * 
     * @param scrollInsensitiveResultSetOwnInsertsAreVisible <code>true</code> if inserts are visible for the given result set
     *        type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollInsensitiveResultSetOwnInsertsAreVisible( Boolean scrollInsensitiveResultSetOwnInsertsAreVisible ) {
        this.scrollInsensitiveResultSetOwnInsertsAreVisible = scrollInsensitiveResultSetOwnInsertsAreVisible;
    }

    /**
     * Retrieves whether a result set's own inserts are visible.
     * 
     * @return <code>true</code> if inserts are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollSensitiveResultSetOwnInsertsAreVisible() {
        return scrollSensitiveResultSetOwnInsertsAreVisible;
    }

    /**
     * Sets whether a result set's own inserts are visible.
     * 
     * @param scrollSensitiveResultSetOwnInsertsAreVisible <code>true</code> if inserts are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollSensitiveResultSetOwnInsertsAreVisible( Boolean scrollSensitiveResultSetOwnInsertsAreVisible ) {
        this.scrollSensitiveResultSetOwnInsertsAreVisible = scrollSensitiveResultSetOwnInsertsAreVisible;
    }

    /**
     * Retrieves whether updates made by others are visible.
     * 
     * @return <code>true</code> if updates made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isForwardOnlyResultSetOthersUpdatesAreVisible() {
        return forwardOnlyResultSetOthersUpdatesAreVisible;
    }

    /**
     * Sets whether updates made by others are visible.
     * 
     * @param forwardOnlyResultSetOthersUpdatesAreVisible <code>true</code> if updates made by others are visible for the given
     *        result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setForwardOnlyResultSetOthersUpdatesAreVisible( Boolean forwardOnlyResultSetOthersUpdatesAreVisible ) {
        this.forwardOnlyResultSetOthersUpdatesAreVisible = forwardOnlyResultSetOthersUpdatesAreVisible;
    }

    /**
     * Retrieves whether updates made by others are visible.
     * 
     * @return <code>true</code> if updates made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollInsensitiveResultSetOthersUpdatesAreVisible() {
        return scrollInsensitiveResultSetOthersUpdatesAreVisible;
    }

    /**
     * Sets whether updates made by others are visible.
     * 
     * @param scrollInsensitiveResultSetOthersUpdatesAreVisible <code>true</code> if updates made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollInsensitiveResultSetOthersUpdatesAreVisible( Boolean scrollInsensitiveResultSetOthersUpdatesAreVisible ) {
        this.scrollInsensitiveResultSetOthersUpdatesAreVisible = scrollInsensitiveResultSetOthersUpdatesAreVisible;
    }

    /**
     * Retrieves whether updates made by others are visible.
     * 
     * @return <code>true</code> if updates made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollSensitiveResultSetOthersUpdatesAreVisible() {
        return scrollSensitiveResultSetOthersUpdatesAreVisible;
    }

    /**
     * Sets whether updates made by others are visible.
     * 
     * @param scrollSensitiveResultSetOthersUpdatesAreVisible <code>true</code> if updates made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollSensitiveResultSetOthersUpdatesAreVisible( Boolean scrollSensitiveResultSetOthersUpdatesAreVisible ) {
        this.scrollSensitiveResultSetOthersUpdatesAreVisible = scrollSensitiveResultSetOthersUpdatesAreVisible;
    }

    /**
     * Retrieves whether deletes made by others are visible.
     * 
     * @return <code>true</code> if deletes made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isForwardOnlyResultSetOthersDeletesAreVisible() {
        return forwardOnlyResultSetOthersDeletesAreVisible;
    }

    /**
     * Sets whether deletes made by others are visible.
     * 
     * @param forwardOnlyResultSetOthersDeletesAreVisible <code>true</code> if deletes made by others are visible for the given
     *        result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setForwardOnlyResultSetOthersDeletesAreVisible( Boolean forwardOnlyResultSetOthersDeletesAreVisible ) {
        this.forwardOnlyResultSetOthersDeletesAreVisible = forwardOnlyResultSetOthersDeletesAreVisible;
    }

    /**
     * Retrieves whether deletes made by others are visible.
     * 
     * @return <code>true</code> if deletes made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollInsensitiveResultSetOthersDeletesAreVisible() {
        return scrollInsensitiveResultSetOthersDeletesAreVisible;
    }

    /**
     * Sets whether deletes made by others are visible.
     * 
     * @param scrollInsensitiveResultSetOthersDeletesAreVisible <code>true</code> if deletes made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollInsensitiveResultSetOthersDeletesAreVisible( Boolean scrollInsensitiveResultSetOthersDeletesAreVisible ) {
        this.scrollInsensitiveResultSetOthersDeletesAreVisible = scrollInsensitiveResultSetOthersDeletesAreVisible;
    }

    /**
     * Retrieves whether deletes made by others are visible.
     * 
     * @return <code>true</code> if deletes made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollSensitiveResultSetOthersDeletesAreVisible() {
        return scrollSensitiveResultSetOthersDeletesAreVisible;
    }

    /**
     * Sets whether deletes made by others are visible.
     * 
     * @param scrollSensitiveResultSetOthersDeletesAreVisible <code>true</code> if deletes made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollSensitiveResultSetOthersDeletesAreVisible( Boolean scrollSensitiveResultSetOthersDeletesAreVisible ) {
        this.scrollSensitiveResultSetOthersDeletesAreVisible = scrollSensitiveResultSetOthersDeletesAreVisible;
    }

    /**
     * Retrieves whether inserts made by others are visible.
     * 
     * @return <code>true</code> if inserts made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isForwardOnlyResultSetOthersInsertsAreVisible() {
        return forwardOnlyResultSetOthersInsertsAreVisible;
    }

    /**
     * Sets whether inserts made by others are visible.
     * 
     * @param forwardOnlyResultSetOthersInsertsAreVisible <code>true</code> if inserts made by others are visible for the given
     *        result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setForwardOnlyResultSetOthersInsertsAreVisible( Boolean forwardOnlyResultSetOthersInsertsAreVisible ) {
        this.forwardOnlyResultSetOthersInsertsAreVisible = forwardOnlyResultSetOthersInsertsAreVisible;
    }

    /**
     * Retrieves whether inserts made by others are visible.
     * 
     * @return <code>true</code> if inserts made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollInsensitiveResultSetOthersInsertsAreVisible() {
        return scrollInsensitiveResultSetOthersInsertsAreVisible;
    }

    /**
     * Sets whether inserts made by others are visible.
     * 
     * @param scrollInsensitiveResultSetOthersInsertsAreVisible <code>true</code> if inserts made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollInsensitiveResultSetOthersInsertsAreVisible( Boolean scrollInsensitiveResultSetOthersInsertsAreVisible ) {
        this.scrollInsensitiveResultSetOthersInsertsAreVisible = scrollInsensitiveResultSetOthersInsertsAreVisible;
    }

    /**
     * Retrieves whether inserts made by others are visible.
     * 
     * @return <code>true</code> if inserts made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollSensitiveResultSetOthersInsertsAreVisible() {
        return scrollSensitiveResultSetOthersInsertsAreVisible;
    }

    /**
     * Sets whether inserts made by others are visible.
     * 
     * @param scrollSensitiveResultSetOthersInsertsAreVisible <code>true</code> if inserts made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollSensitiveResultSetOthersInsertsAreVisible( Boolean scrollSensitiveResultSetOthersInsertsAreVisible ) {
        this.scrollSensitiveResultSetOthersInsertsAreVisible = scrollSensitiveResultSetOthersInsertsAreVisible;
    }

    /**
     * Retrieves whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @return <code>true</code> if changes are detected by the result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isForwardOnlyResultSetUpdatesAreDetected() {
        return forwardOnlyResultSetUpdatesAreDetected;
    }

    /**
     * Sets whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @param forwardOnlyResultSetUpdatesAreDetected <code>true</code> if changes are detected by the result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setForwardOnlyResultSetUpdatesAreDetected( Boolean forwardOnlyResultSetUpdatesAreDetected ) {
        this.forwardOnlyResultSetUpdatesAreDetected = forwardOnlyResultSetUpdatesAreDetected;
    }

    /**
     * Retrieves whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @return <code>true</code> if changes are detected by the result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollInsensitiveResultSetUpdatesAreDetected() {
        return scrollInsensitiveResultSetUpdatesAreDetected;
    }

    /**
     * Sets whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @param scrollInsensitiveResultSetUpdatesAreDetected <code>true</code> if changes are detected by the result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollInsensitiveResultSetUpdatesAreDetected( Boolean scrollInsensitiveResultSetUpdatesAreDetected ) {
        this.scrollInsensitiveResultSetUpdatesAreDetected = scrollInsensitiveResultSetUpdatesAreDetected;
    }

    /**
     * Retrieves whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @return <code>true</code> if changes are detected by the result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollSensitiveResultSetUpdatesAreDetected() {
        return scrollSensitiveResultSetUpdatesAreDetected;
    }

    /**
     * Sets whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @param scrollSensitiveResultSetUpdatesAreDetected <code>true</code> if changes are detected by the result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollSensitiveResultSetUpdatesAreDetected( Boolean scrollSensitiveResultSetUpdatesAreDetected ) {
        this.scrollSensitiveResultSetUpdatesAreDetected = scrollSensitiveResultSetUpdatesAreDetected;
    }

    /**
     * Retrieves whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If
     * the method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the
     * result set.
     * 
     * @return <code>true</code> if deletes are detected by the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isForwardOnlyResultSetDeletesAreDetected() {
        return forwardOnlyResultSetDeletesAreDetected;
    }

    /**
     * Sets whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If the
     * method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the result
     * set.
     * 
     * @param forwardOnlyResultSetDeletesAreDetected <code>true</code> if deletes are detected by the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setForwardOnlyResultSetDeletesAreDetected( Boolean forwardOnlyResultSetDeletesAreDetected ) {
        this.forwardOnlyResultSetDeletesAreDetected = forwardOnlyResultSetDeletesAreDetected;
    }

    /**
     * Retrieves whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If
     * the method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the
     * result set.
     * 
     * @return <code>true</code> if deletes are detected by the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollInsensitiveResultSetDeletesAreDetected() {
        return scrollInsensitiveResultSetDeletesAreDetected;
    }

    /**
     * Sets whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If the
     * method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the result
     * set.
     * 
     * @param scrollInsensitiveResultSetDeletesAreDetected <code>true</code> if deletes are detected by the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollInsensitiveResultSetDeletesAreDetected( Boolean scrollInsensitiveResultSetDeletesAreDetected ) {
        this.scrollInsensitiveResultSetDeletesAreDetected = scrollInsensitiveResultSetDeletesAreDetected;
    }

    /**
     * Retrieves whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If
     * the method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the
     * result set.
     * 
     * @return <code>true</code> if deletes are detected by the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollSensitiveResultSetDeletesAreDetected() {
        return scrollSensitiveResultSetDeletesAreDetected;
    }

    /**
     * Sets whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If the
     * method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the result
     * set.
     * 
     * @param scrollSensitiveResultSetDeletesAreDetected <code>true</code> if deletes are detected by the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollSensitiveResultSetDeletesAreDetected( Boolean scrollSensitiveResultSetDeletesAreDetected ) {
        this.scrollSensitiveResultSetDeletesAreDetected = scrollSensitiveResultSetDeletesAreDetected;
    }

    /**
     * Retrieves whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @return <code>true</code> if changes are detected by the specified result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isForwardOnlyResultInsertsAreDetected() {
        return forwardOnlyResultInsertsAreDetected;
    }

    /**
     * Sets whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @param forwardOnlyResultInsertsAreDetected <code>true</code> if changes are detected by the specified result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setForwardOnlyResultInsertsAreDetected( Boolean forwardOnlyResultInsertsAreDetected ) {
        this.forwardOnlyResultInsertsAreDetected = forwardOnlyResultInsertsAreDetected;
    }

    /**
     * Retrieves whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @return <code>true</code> if changes are detected by the specified result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollInsensitiveResultInsertsAreDetected() {
        return scrollInsensitiveResultInsertsAreDetected;
    }

    /**
     * Sets whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @param scrollInsensitiveResultInsertsAreDetected <code>true</code> if changes are detected by the specified result set
     *        type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollInsensitiveResultInsertsAreDetected( Boolean scrollInsensitiveResultInsertsAreDetected ) {
        this.scrollInsensitiveResultInsertsAreDetected = scrollInsensitiveResultInsertsAreDetected;
    }

    /**
     * Retrieves whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @return <code>true</code> if changes are detected by the specified result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isScrollSensitiveResultInsertsAreDetected() {
        return scrollSensitiveResultInsertsAreDetected;
    }

    /**
     * Sets whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @param scrollSensitiveResultInsertsAreDetected <code>true</code> if changes are detected by the specified result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setScrollSensitiveResultInsertsAreDetected( Boolean scrollSensitiveResultInsertsAreDetected ) {
        this.scrollSensitiveResultInsertsAreDetected = scrollSensitiveResultInsertsAreDetected;
    }

    /**
     * Retrieves whether this database supports batch updates.
     * 
     * @return <code>true</code> if this database supports batch upcates; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public Boolean isSupportsBatchUpdates() {
        return supportsBatchUpdates;
    }

    /**
     * Sets whether this database supports batch updates.
     * 
     * @param supportsBatchUpdates <code>true</code> if this database supports batch upcates; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    public void setSupportsBatchUpdates( Boolean supportsBatchUpdates ) {
        this.supportsBatchUpdates = supportsBatchUpdates;
    }

    /**
     * Retrieves a description of the UDT available in the given catalog.
     * 
     * @return a set of UDT available
     */
    public Set<UserDefinedType> getUserDefinedTypes() {
        return userDefinedTypes;
    }

    /**
     * Adds UDT
     * 
     * @param udt the UDT to add
     */
    public void addUserDefinedType( UserDefinedType udt ) {
        userDefinedTypes.add(udt);
    }

    /**
     * Deletes UDT
     * 
     * @param udt the UDT to delete
     */
    public void deleteUserDefinedType( UserDefinedType udt ) {
        userDefinedTypes.remove(udt);
    }

    /**
     * Finds UDT by its name.
     * 
     * @param catalog a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a
     *        catalog; <code>null</code> means that the catalog name should not be used to narrow the search
     * @param schema a schema name; must match the schema name as it is stored in the database; "" retrieves those without a
     *        schema; <code>null</code> means that the schema name should not be used to narrow the search
     * @param tableName a table name; must match the table name as it is stored in the database
     * @return table or null if not found
     */
    public UserDefinedType findUserDefinedTypeByName( String catalog,
                                                      String schema,
                                                      String tableName ) {
        for (UserDefinedType udt : userDefinedTypes) {
            if (udt.getName().equals(tableName)) {
                boolean catalogNameEquals = (udt.getCatalog() != null) ? udt.getCatalog().getName().equals(catalog) : catalog == null;
                boolean schemaNameEquals = (udt.getSchema() != null) ? udt.getSchema().getName().equals(schema) : schema == null;
                if (catalogNameEquals && schemaNameEquals) {
                    return udt;
                }
            }
        }
        // return nothing
        return null;
    }

    // ===============================================================
    // ------------------- JDBC 3.0 ---------------------------------
    // ===============================================================

    /**
     * Retrieves whether this database supports savepoints.
     * 
     * @return <code>true</code> if savepoints are supported; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public Boolean isSupportsSavepoints() {
        return supportsSavepoints;
    }

    /**
     * Sets whether this database supports savepoints.
     * 
     * @param supportsSavepoints <code>true</code> if savepoints are supported; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public void setSupportsSavepoints( Boolean supportsSavepoints ) {
        this.supportsSavepoints = supportsSavepoints;
    }

    /**
     * Retrieves whether this database supports named parameters to callable statements.
     * 
     * @return <code>true</code> if named parameters are supported; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public Boolean isSupportsNamedParameters() {
        return supportsNamedParameters;
    }

    /**
     * Sets whether this database supports named parameters to callable statements.
     * 
     * @param supportsNamedParameters <code>true</code> if named parameters are supported; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public void setSupportsNamedParameters( Boolean supportsNamedParameters ) {
        this.supportsNamedParameters = supportsNamedParameters;
    }

    /**
     * Retrieves whether it is possible to have multiple <code>ResultSet</code> objects returned from a
     * <code>CallableStatement</code> object simultaneously.
     * 
     * @return <code>true</code> if a <code>CallableStatement</code> object can return multiple <code>ResultSet</code> objects
     *         simultaneously; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public Boolean isSupportsMultipleOpenResults() {
        return supportsMultipleOpenResults;
    }

    /**
     * Sets whether it is possible to have multiple <code>ResultSet</code> objects returned from a <code>CallableStatement</code>
     * object simultaneously.
     * 
     * @param supportsMultipleOpenResults <code>true</code> if a <code>CallableStatement</code> object can return multiple
     *        <code>ResultSet</code> objects simultaneously; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public void setSupportsMultipleOpenResults( Boolean supportsMultipleOpenResults ) {
        this.supportsMultipleOpenResults = supportsMultipleOpenResults;
    }

    /**
     * Retrieves whether auto-generated keys can be retrieved after a statement has been executed.
     * 
     * @return <code>true</code> if auto-generated keys can be retrieved after a statement has executed; <code>false</code>
     *         otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public Boolean isSupportsGetGeneratedKeys() {
        return supportsGetGeneratedKeys;
    }

    /**
     * Sets whether auto-generated keys can be retrieved after a statement has been executed.
     * 
     * @param supportsGetGeneratedKeys <code>true</code> if auto-generated keys can be retrieved after a statement has executed;
     *        <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public void setSupportsGetGeneratedKeys( Boolean supportsGetGeneratedKeys ) {
        this.supportsGetGeneratedKeys = supportsGetGeneratedKeys;
    }

    /**
     * Retrieves whether this database supports the given result set holdability.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @see java.sql.Connection
     * @since 1.4 (JDBC 3.0)
     */
    public Boolean isSupportsResultSetHoldCurrorsOverCommitHoldability() {
        return supportsResultSetHoldCurrorsOverCommitHoldability;
    }

    /**
     * Sets whether this database supports the given result set holdability.
     * 
     * @param supportsResultSetHoldCurrorsOverCommitHoldability <code>true</code> if so; <code>false</code> otherwise
     * @see java.sql.Connection
     * @since 1.4 (JDBC 3.0)
     */
    public void setSupportsResultSetHoldCurrorsOverCommitHoldability( Boolean supportsResultSetHoldCurrorsOverCommitHoldability ) {
        this.supportsResultSetHoldCurrorsOverCommitHoldability = supportsResultSetHoldCurrorsOverCommitHoldability;
    }

    /**
     * Retrieves whether this database supports the given result set holdability.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @see java.sql.Connection
     * @since 1.4 (JDBC 3.0)
     */
    public Boolean isSupportsResultSetCloseCurrorsAtCommitHoldability() {
        return supportsResultSetCloseCurrorsAtCommitHoldability;
    }

    /**
     * Sets whether this database supports the given result set holdability.
     * 
     * @param supportsResultSetCloseCurrorsAtCommitHoldability <code>true</code> if so; <code>false</code> otherwise
     * @see java.sql.Connection
     * @since 1.4 (JDBC 3.0)
     */
    public void setSupportsResultSetCloseCurrorsAtCommitHoldability( Boolean supportsResultSetCloseCurrorsAtCommitHoldability ) {
        this.supportsResultSetCloseCurrorsAtCommitHoldability = supportsResultSetCloseCurrorsAtCommitHoldability;
    }

    /**
     * Retrieves the default holdability of this <code>ResultSet</code> object.
     * 
     * @return the default holdability; either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * 
     * @since 1.4
     */
    public ResultSetHoldabilityType getResultSetHoldabilityType() {
        return resultSetHoldabilityType;
    }

    /**
     * Sets the default holdability of this <code>ResultSet</code> object.
     * 
     * @param resultSetHoldabilityType the ResultSetHoldabilityType
     *  the default holdability; either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * 
     * @since 1.4
     */
    public void setResultSetHoldabilityType( ResultSetHoldabilityType resultSetHoldabilityType ) {
        this.resultSetHoldabilityType = resultSetHoldabilityType;
    }

    /**
     * Retrieves the major version number of the underlying database.
     * 
     * @return the underlying database's major version
     * @since 1.4 (JDBC 3.0)
     */
    public Integer getDatabaseMajorVersion() {
        return databaseMajorVersion;
    }

    /**
     * Sets the major version number of the underlying database.
     * 
     * @param databaseMajorVersion the underlying database's major version
     * @since 1.4 (JDBC 3.0)
     */
    public void setDatabaseMajorVersion( Integer databaseMajorVersion ) {
        this.databaseMajorVersion = databaseMajorVersion;
    }

    /**
     * Retrieves the minor version number of the underlying database.
     * 
     * @return underlying database's minor version
     * @since 1.4 (JDBC 3.0)
     */
    public Integer getDatabaseMinorVersion() {
        return databaseMinorVersion;
    }

    /**
     * Sets the minor version number of the underlying database.
     * 
     * @param databaseMinorVersion underlying database's minor version
     * @since 1.4 (JDBC 3.0)
     */
    public void setDatabaseMinorVersion( Integer databaseMinorVersion ) {
        this.databaseMinorVersion = databaseMinorVersion;
    }

    /**
     * Retrieves the major JDBC version number for this driver.
     * 
     * @return JDBC version major number
     * @since 1.4 (JDBC 3.0)
     */
    public Integer getJDBCMajorVersion() {
        return jdbcMajorVersion;
    }

    /**
     * Sets the major JDBC version number for this driver.
     * 
     * @param jdbcMajorVersion JDBC version major number
     * @since 1.4 (JDBC 3.0)
     */
    public void setJDBCMajorVersion( Integer jdbcMajorVersion ) {
        this.jdbcMajorVersion = jdbcMajorVersion;
    }

    /**
     * Retrieves the minor JDBC version number for this driver.
     * 
     * @return JDBC version minor number
     * @since 1.4 (JDBC 3.0)
     */
    public Integer getJDBCMinorVersion() {
        return jdbcMinorVersion;
    }

    /**
     * Sets the minor JDBC version number for this driver.
     * 
     * @param jdbcMinorVersion JDBC version minor number
     * @since 1.4 (JDBC 3.0)
     */
    public void setJDBCMinorVersion( Integer jdbcMinorVersion ) {
        this.jdbcMinorVersion = jdbcMinorVersion;
    }

    /**
     * Indicates whether the SQLSTATE returned by <code>SQLException.getSQLState</code> is X/Open (now known as Open Group) SQL
     * CLI or SQL99.
     * 
     * @return the type of SQLSTATE; one of: sqlStateXOpen or sqlStateSQL99
     * @since 1.4 (JDBC 3.0)
     */
    public SQLStateType getSQLStateType() {
        return sqlStateType;
    }

    /**
     * Indicates whether the SQLSTATE returned by <code>SQLException.getSQLState</code> is X/Open (now known as Open Group) SQL
     * CLI or SQL99.
     * 
     * @param sqlStateType the type of SQLSTATE; one of: sqlStateXOpen or sqlStateSQL99
     * @since 1.4 (JDBC 3.0)
     */
    public void setSQLStateType( SQLStateType sqlStateType ) {
        this.sqlStateType = sqlStateType;
    }

    /**
     * Indicates whether updates made to a LOB are made on a copy or directly to the LOB.
     * 
     * @return <code>true</code> if updates are made to a copy of the LOB; <code>false</code> if updates are made directly to the
     *         LOB
     * @since 1.4 (JDBC 3.0)
     */
    public Boolean isLocatorsUpdateCopy() {
        return locatorsUpdateCopy;
    }

    /**
     * Indicates whether updates made to a LOB are made on a copy or directly to the LOB.
     * 
     * @param locatorsUpdateCopy <code>true</code> if updates are made to a copy of the LOB; <code>false</code> if updates are
     *        made directly to the LOB
     * @since 1.4 (JDBC 3.0)
     */
    public void setLocatorsUpdateCopy( Boolean locatorsUpdateCopy ) {
        this.locatorsUpdateCopy = locatorsUpdateCopy;
    }

    /**
     * Retrieves whether this database supports statement pooling.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public Boolean isSupportsStatementPooling() {
        return supportsStatementPooling;
    }

    /**
     * Sets whether this database supports statement pooling.
     * 
     * @param supportsStatementPooling <code>true</code> if so; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    public void setSupportsStatementPooling( Boolean supportsStatementPooling ) {
        this.supportsStatementPooling = supportsStatementPooling;
    }

}
