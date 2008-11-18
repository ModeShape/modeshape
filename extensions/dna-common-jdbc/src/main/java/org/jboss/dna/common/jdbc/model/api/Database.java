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
package org.jboss.dna.common.jdbc.model.api;

import java.util.List;
import java.util.Set;

/**
 * Provides RDBMS wide meta data retrieved from java.sql.DatabaseMetaData.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface Database extends CoreMetaData {

    /**
     * Returns list of failed database metadata methods through the DatabaseMetaDataMethodExceptions
     * 
     * @return list of failed database metadata methods through the DatabaseMetaDataMethodExceptions
     */
    List<DatabaseMetaDataMethodException> getExceptionList();

    /**
     * Adds the DatabaseMetaDataMethodException to the DatabaseMetadataProvider exception list
     * 
     * @param exception the DatabaseMetaDataMethodException
     */
    void addException( DatabaseMetaDataMethodException exception );

    /**
     * Searches the DatabaseMetaDataMethodException by method name
     * 
     * @param methodName the name of method that caused exception
     * @return the DatabaseMetaDataMethodException if found, otherwise returns null
     */
    DatabaseMetaDataMethodException findException( String methodName );

    /**
     * Checks that specified database metadata method of provider is failed or not
     * 
     * @param methodName the name of method that caused exception
     * @return true if method failed; false otherwise
     */
    boolean isDatabaseMetaDataMethodFailed( String methodName );

    /**
     * Gets database name
     * 
     * @return database name
     */
    String getName();

    /**
     * Sets database name
     * 
     * @param name the database name
     */
    void setName( String name );

    // ----------------------------------------------------------------------
    // A variety of minor information about the target database.
    // ----------------------------------------------------------------------

    /**
     * Retrieves whether the current user can call all the procedures returned by the method
     * <code>DatabaseMetaData.getProcedures</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isAllProceduresAreCallable();

    /**
     * sets whether the current user can call all the procedures returned by the method
     * <code>DatabaseMetaData.getProcedures</code>.
     * 
     * @param allProceduresAreCallable <code>true</code> if so; <code>false</code> otherwise
     */
    void setAllProceduresAreCallable( Boolean allProceduresAreCallable );

    /**
     * Retrieves whether the current user can use all the tables returned by the method <code>DatabaseMetaData.getTables</code> in
     * a <code>SELECT</code> statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isAllTablesAreSelectable();

    /**
     * Sets whether the current user can use all the tables returned by the method <code>DatabaseMetaData.getTables</code> in a
     * <code>SELECT</code> statement.
     * 
     * @param allTablesAreSelectable <code>true</code> if so; <code>false</code> otherwise
     */
    void setAllTablesAreSelectable( Boolean allTablesAreSelectable );

    /**
     * Retrieves the URL for this DBMS.
     * 
     * @return the URL for this DBMS or <code>null</code> if it cannot be generated
     */
    String getURL();

    /**
     * Sets the URL for this DBMS.
     * 
     * @param url the URL for this DBMS or <code>null</code> if it cannot be generated
     */
    void setURL( String url );

    /**
     * Retrieves the user name as known to this database.
     * 
     * @return the database user name
     */
    String getUserName();

    /**
     * Sets the user name as known to this database.
     * 
     * @param userName the database user name
     */
    void setUserName( String userName );

    /**
     * Retrieves whether this database is in read-only mode.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isReadOnly();

    /**
     * Sets whether this database is in read-only mode.
     * 
     * @param readOnly <code>true</code> if so; <code>false</code> otherwise
     */
    void setReadOnly( Boolean readOnly );

    /**
     * Retrieves whether <code>NULL</code> values are sorted high. Sorted high means that <code>NULL</code> values sort higher
     * than any other value in a domain. In an ascending order, if this method returns <code>true</code>, <code>NULL</code> values
     * will appear at the end. By contrast, the method <code>DatabaseMetaData.nullsAreSortedAtEnd</code> indicates whether
     * <code>NULL</code> values are sorted at the end regardless of sort order.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isNullsAreSortedHigh();

    /**
     * Sets whether <code>NULL</code> values are sorted high. Sorted high means that <code>NULL</code> values sort higher than any
     * other value in a domain. In an ascending order, if this method returns <code>true</code>, <code>NULL</code> values will
     * appear at the end. By contrast, the method <code>DatabaseMetaData.nullsAreSortedAtEnd</code> indicates whether
     * <code>NULL</code> values are sorted at the end regardless of sort order.
     * 
     * @param nullsAreSortedHigh <code>true</code> if so; <code>false</code> otherwise
     */
    void setNullsAreSortedHigh( Boolean nullsAreSortedHigh );

    /**
     * Retrieves whether <code>NULL</code> values are sorted low. Sorted low means that <code>NULL</code> values sort lower than
     * any other value in a domain. In an ascending order, if this method returns <code>true</code>, <code>NULL</code> values will
     * appear at the beginning. By contrast, the method <code>DatabaseMetaData.nullsAreSortedAtStart</code> indicates whether
     * <code>NULL</code> values are sorted at the beginning regardless of sort order.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isNullsAreSortedLow();

    /**
     * Sets whether <code>NULL</code> values are sorted low. Sorted low means that <code>NULL</code> values sort lower than any
     * other value in a domain. In an ascending order, if this method returns <code>true</code>, <code>NULL</code> values will
     * appear at the beginning. By contrast, the method <code>DatabaseMetaData.nullsAreSortedAtStart</code> indicates whether
     * <code>NULL</code> values are sorted at the beginning regardless of sort order.
     * 
     * @param nullsAreSortedLow <code>true</code> if so; <code>false</code> otherwise
     */
    void setNullsAreSortedLow( Boolean nullsAreSortedLow );

    /**
     * Retrieves whether <code>NULL</code> values are sorted at the start regardless of sort order.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isNullsAreSortedAtStart();

    /**
     * Sets whether <code>NULL</code> values are sorted at the start regardless of sort order.
     * 
     * @param nullsAreSortedAtStart <code>true</code> if so; <code>false</code> otherwise
     */
    void setNullsAreSortedAtStart( Boolean nullsAreSortedAtStart );

    /**
     * Retrieves whether <code>NULL</code> values are sorted at the end regardless of sort order.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isNullsAreSortedAtEnd();

    /**
     * Sets whether <code>NULL</code> values are sorted at the end regardless of sort order.
     * 
     * @param nullsAreSortedAtEnd <code>true</code> if so; <code>false</code> otherwise
     */
    void setNullsAreSortedAtEnd( Boolean nullsAreSortedAtEnd );

    /**
     * Retrieves the name of this database product.
     * 
     * @return database product name
     */
    String getDatabaseProductName();

    /**
     * Sets the name of this database product.
     * 
     * @param databaseProductName database product name
     */
    void setDatabaseProductName( String databaseProductName );

    /**
     * Retrieves the version number of this database product.
     * 
     * @return database version number
     */
    String getDatabaseProductVersion();

    /**
     * Sets the version number of this database product.
     * 
     * @param databaseProductVersion database version number
     */
    void setDatabaseProductVersion( String databaseProductVersion );

    /**
     * Retrieves the name of this JDBC driver.
     * 
     * @return JDBC driver name
     */
    String getDriverName();

    /**
     * Sets the name of this JDBC driver.
     * 
     * @param driverName JDBC driver name
     */
    void setDriverName( String driverName );

    /**
     * Retrieves the version number of this JDBC driver as a <code>String</code>.
     * 
     * @return JDBC driver version
     */
    String getDriverVersion();

    /**
     * Sets the version number of this JDBC driver as a <code>String</code>.
     * 
     * @param driverVersion the JDBC driver version
     */
    void setDriverVersion( String driverVersion );

    /**
     * Retrieves this JDBC driver's minor version number.
     * 
     * @return JDBC driver minor version number
     */
    Integer getDriverMajorVersion();

    /**
     * Sets this JDBC driver's major version number.
     * 
     * @param driverMajorVersion the JDBC driver major version
     */
    void setDriverMajorVersion( Integer driverMajorVersion );

    /**
     * Retrieves this JDBC driver's minor version number.
     * 
     * @return JDBC driver minor version number
     */
    Integer getDriverMinorVersion();

    /**
     * Sets this JDBC driver's minor version number.
     * 
     * @param driverMinorVersion the JDBC driver minor version number
     */
    void setDriverMinorVersion( Integer driverMinorVersion );

    /**
     * Retrieves whether this database stores tables in a local file.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isUsesLocalFiles();

    /**
     * Sets whether this database stores tables in a local file.
     * 
     * @param usesLocalFiles <code>true</code> if so; <code>false</code> otherwise
     */
    void setUsesLocalFiles( Boolean usesLocalFiles );

    /**
     * Retrieves whether this database uses a file for each table.
     * 
     * @return <code>true</code> if this database uses a local file for each table; <code>false</code> otherwise
     */
    Boolean isUsesLocalFilePerTable();

    /**
     * Sets whether this database uses a file for each table.
     * 
     * @param usesLocalFilePerTable <code>true</code> if this database uses a local file for each table; <code>false</code>
     *        otherwise
     */
    void setUsesLocalFilePerTable( Boolean usesLocalFilePerTable );

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as case sensitive and as a result stores them in
     * mixed case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsMixedCaseIdentifiers();

    /**
     * Sets whether this database treats mixed case unquoted SQL identifiers as case sensitive and as a result stores them in
     * mixed case.
     * 
     * @param supportsMixedCaseIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsMixedCaseIdentifiers( Boolean supportsMixedCaseIdentifiers );

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in upper
     * case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isStoresUpperCaseIdentifiers();

    /**
     * Sets whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in upper case.
     * 
     * @param storesUpperCaseIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    void setStoresUpperCaseIdentifiers( Boolean storesUpperCaseIdentifiers );

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in lower
     * case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isStoresLowerCaseIdentifiers();

    /**
     * sets whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in lower case.
     * 
     * @param storesLowerCaseIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    void setStoresLowerCaseIdentifiers( Boolean storesLowerCaseIdentifiers );

    /**
     * Retrieves whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in mixed
     * case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isStoresMixedCaseIdentifiers();

    /**
     * Sets whether this database treats mixed case unquoted SQL identifiers as case insensitive and stores them in mixed case.
     * 
     * @param storesMixedCaseIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    void setStoresMixedCaseIdentifiers( Boolean storesMixedCaseIdentifiers );

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as case sensitive and as a result stores them in
     * mixed case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsMixedCaseQuotedIdentifiers();

    /**
     * Sets whether this database treats mixed case quoted SQL identifiers as case sensitive and as a result stores them in mixed
     * case.
     * 
     * @param supportsMixedCaseQuotedIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsMixedCaseQuotedIdentifiers( Boolean supportsMixedCaseQuotedIdentifiers );

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in upper case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isStoresUpperCaseQuotedIdentifiers();

    /**
     * Sets whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in upper case.
     * 
     * @param storesUpperCaseQuotedIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    void setStoresUpperCaseQuotedIdentifiers( Boolean storesUpperCaseQuotedIdentifiers );

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in lower case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isStoresLowerCaseQuotedIdentifiers();

    /**
     * Sets whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in lower case.
     * 
     * @param storesLowerCaseQuotedIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    void setStoresLowerCaseQuotedIdentifiers( Boolean storesLowerCaseQuotedIdentifiers );

    /**
     * Retrieves whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in mixed case.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isStoresMixedCaseQuotedIdentifiers();

    /**
     * Sets whether this database treats mixed case quoted SQL identifiers as case insensitive and stores them in mixed case.
     * 
     * @param storesMixedCaseQuotedIdentifiers <code>true</code> if so; <code>false</code> otherwise
     */
    void setStoresMixedCaseQuotedIdentifiers( Boolean storesMixedCaseQuotedIdentifiers );

    /**
     * Retrieves the string used to quote SQL identifiers. This method returns a space " " if identifier quoting is not supported.
     * 
     * @return the quoting string or a space if quoting is not supported
     */
    String getIdentifierQuoteString();

    /**
     * Sets the string used to quote SQL identifiers. This method returns a space " " if identifier quoting is not supported.
     * 
     * @param identifierQuoteString the quoting string or a space if quoting is not supported
     */
    void setIdentifierQuoteString( String identifierQuoteString );

    /**
     * Retrieves a list of all of this database's SQL keywords that are NOT also SQL92 keywords.
     * 
     * @return the list of this database's keywords that are not also SQL92 keywords
     */
    Set<String> getSQLKeywords();

    /**
     * Adds SQL keyword
     * 
     * @param sqlKeyword the SQL keyword to add
     */
    void addSQLKeyword( String sqlKeyword );

    /**
     * Deletes SQL keyword
     * 
     * @param sqlKeyword the SQL keyword to delete
     */
    void deleteSQLKeyword( String sqlKeyword );

    /**
     * Is SQL keyword supported
     * 
     * @param sqlKeyword the SQL keyword to search
     * @return true if supported; false otherwiose
     */
    Boolean isSQLKeywordSupported( String sqlKeyword );

    /**
     * Retrieves a list of math functions available with this database. These are the Open /Open CLI math function names used in
     * the JDBC function escape clause.
     * 
     * @return the list of math functions supported by this database
     */
    Set<String> getNumericFunctions();

    /**
     * Adds numeric function
     * 
     * @param functionName the name of numeric function to add
     */
    void addNumericFunction( String functionName );

    /**
     * Deletes numeric function
     * 
     * @param functionName the name of numeric function to delete
     */
    void deleteNumericFunction( String functionName );

    /**
     * Is Numeric function supported
     * 
     * @param functionName the name of numeric function
     * @return true is supported; false otherwise
     */
    Boolean isNumericFunctionSupported( String functionName );

    /**
     * Retrieves a list of string functions available with this database. These are the Open Group CLI string function names used
     * in the JDBC function escape clause.
     * 
     * @return the list of string functions supported by this database
     */
    Set<String> getStringFunctions();

    /**
     * Adds String function
     * 
     * @param functionName the name of String function to add
     */
    void addStringFunction( String functionName );

    /**
     * Deletes String function
     * 
     * @param functionName the name of String function to delete
     */
    void deleteStringFunction( String functionName );

    /**
     * Is String function supported
     * 
     * @param functionName the name of String function
     * @return true is supported; false otherwise
     */
    Boolean isStringFunctionSupported( String functionName );

    /**
     * Retrieves a list of system functions available with this database. These are the Open Group CLI system function names used
     * in the JDBC function escape clause.
     * 
     * @return a list of system functions supported by this database
     */
    Set<String> getSystemFunctions();

    /**
     * Adds System function
     * 
     * @param functionName the name of System function to add
     */
    void addSystemFunction( String functionName );

    /**
     * deletes System function
     * 
     * @param functionName the name of System function to delete
     */
    void deleteSystemFunction( String functionName );

    /**
     * Is System function supported
     * 
     * @param functionName the name of System function
     * @return true is supported; false otherwise
     */
    Boolean isSystemFunctionSupported( String functionName );

    /**
     * Retrieves a list of the time and date functions available with this database.
     * 
     * @return the list of time and date functions supported by this database
     */
    Set<String> getTimeDateFunctions();

    /**
     * Adds Time/Date function
     * 
     * @param functionName the name of Time/Date function to add
     */
    void addTimeDateFunction( String functionName );

    /**
     * deletes Time/Date function
     * 
     * @param functionName the name of Time/Date function to delete
     */
    void deleteTimeDateFunction( String functionName );

    /**
     * Is Time/Date function supported
     * 
     * @param functionName the name of Time/Date function
     * @return true is supported; false otherwise
     */
    Boolean isTimeDateFunctionSupported( String functionName );

    /**
     * Retrieves the string that can be used to escape wildcard characters. This is the string that can be used to escape '_' or
     * '%' in the catalog search parameters that are a pattern (and therefore use one of the wildcard characters).
     * <P>
     * The '_' character represents any single character; the '%' character represents any sequence of zero or more characters.
     * 
     * @return the string used to escape wildcard characters
     */
    String getSearchStringEscape();

    /**
     * Sets the string that can be used to escape wildcard characters. This is the string that can be used to escape '_' or '%' in
     * the catalog search parameters that are a pattern (and therefore use one of the wildcard characters).
     * <P>
     * The '_' character represents any single character; the '%' character represents any sequence of zero or more characters.
     * 
     * @param searchStringEscape the string used to escape wildcard characters
     */
    void setSearchStringEscape( String searchStringEscape );

    /**
     * Retrieves all the "extra" characters that can be used in unquoted identifier names (those beyond a-z, A-Z, 0-9 and _).
     * 
     * @return the string containing the extra characters
     */
    String getExtraNameCharacters();

    /**
     * Sets all the "extra" characters that can be used in unquoted identifier names (those beyond a-z, A-Z, 0-9 and _).
     * 
     * @param extraNameCharacters the string containing the extra characters
     */
    void setExtraNameCharacters( String extraNameCharacters );

    // --------------------------------------------------------------------
    // Functions describing which features are supported.
    // --------------------------------------------------------------------

    /**
     * Retrieves whether this database supports <code>ALTER TABLE</code> with add column.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsAlterTableWithAddColumn();

    /**
     * Sets whether this database supports <code>ALTER TABLE</code> with add column.
     * 
     * @param supportsAlterTableWithAddColumn <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsAlterTableWithAddColumn( Boolean supportsAlterTableWithAddColumn );

    /**
     * Retrieves whether this database supports <code>ALTER TABLE</code> with drop column.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsAlterTableWithDropColumn();

    /**
     * Sets whether this database supports <code>ALTER TABLE</code> with drop column.
     * 
     * @param supportsAlterTableWithDropColumn <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsAlterTableWithDropColumn( Boolean supportsAlterTableWithDropColumn );

    /**
     * Retrieves whether this database supports column aliasing.
     * <P>
     * If so, the SQL AS clause can be used to provide names for computed columns or to provide alias names for columns as
     * required.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsColumnAliasing();

    /**
     * Sets whether this database supports column aliasing.
     * <P>
     * If so, the SQL AS clause can be used to provide names for computed columns or to provide alias names for columns as
     * required.
     * 
     * @param supportsColumnAliasing <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsColumnAliasing( Boolean supportsColumnAliasing );

    /**
     * Retrieves whether this database supports concatenations between <code>NULL</code> and non-<code>NULL</code> values being
     * <code>NULL</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isNullPlusNonNullIsNull();

    /**
     * Sets whether this database supports concatenations between <code>NULL</code> and non-<code>NULL</code> values being
     * <code>NULL</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    void setNullPlusNonNullIsNull( Boolean nullPlusNonNullIsNull );

    /**
     * Retrieves whether this database supports the <code>CONVERT</code> function between SQL types.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsConvert();

    /**
     * Sets whether this database supports the <code>CONVERT</code> function between SQL types.
     * 
     * @param supportsConvert <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsConvert( Boolean supportsConvert );

    /**
     * Retrieves whether this database supports the <code>CONVERT</code> for given SQL types. It uses original
     * <code>DatabaseMetaData.supportsConvert</code> to check common (NOT ALL POSSIBLE) conversions.
     * 
     * @return list of common (NOT ALL POSSIBLE) conversions.
     * @see Types
     */
    Set<SqlTypeConversionPair> getSupportedConversions();

    /**
     * Adds SqlTypeConversionPair
     * 
     * @param sqlTypeConversionPair the SqlTypeConversionPair
     */
    void addSqlTypeConversionPair( SqlTypeConversionPair sqlTypeConversionPair );

    /**
     * deletes SqlTypeConversionPair
     * 
     * @param sqlTypeConversionPair the SqlTypeConversionPair
     */
    void deleteSqlTypeConversionPair( SqlTypeConversionPair sqlTypeConversionPair );

    /**
     * Searches set of SqlTypeConversionPair by SrcType
     * 
     * @return set of SqlTypeConversionPair
     */
    Set<SqlTypeConversionPair> findSqlTypeConversionPairBySrcType( String srcType );

    /**
     * Retrieves whether this database supports table correlation names.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsTableCorrelationNames();

    /**
     * Sets whether this database supports table correlation names.
     * 
     * @param supportsTableCorrelationNames <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsTableCorrelationNames( Boolean supportsTableCorrelationNames );

    /**
     * Retrieves whether, when table correlation names are supported, they are restricted to being different from the names of the
     * tables.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsDifferentTableCorrelationNames();

    /**
     * Sets whether, when table correlation names are supported, they are restricted to being different from the names of the
     * tables.
     * 
     * @param supportsDifferentTableCorrelationNames <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsDifferentTableCorrelationNames( Boolean supportsDifferentTableCorrelationNames );

    /**
     * Retrieves whether this database supports expressions in <code>ORDER BY</code> lists.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsExpressionsInOrderBy();

    /**
     * Sets whether this database supports expressions in <code>ORDER BY</code> lists.
     * 
     * @param supportsExpressionsInOrderBy <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsExpressionsInOrderBy( Boolean supportsExpressionsInOrderBy );

    /**
     * Retrieves whether this database supports using a column that is not in the <code>SELECT</code> statement in an
     * <code>ORDER BY</code> clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsOrderByUnrelated();

    /**
     * Sets whether this database supports using a column that is not in the <code>SELECT</code> statement in an
     * <code>ORDER BY</code> clause.
     * 
     * @param <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsOrderByUnrelated( Boolean supportsOrderByUnrelated );

    /**
     * Retrieves whether this database supports some form of <code>GROUP BY</code> clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsGroupBy();

    /**
     * Sets whether this database supports some form of <code>GROUP BY</code> clause.
     * 
     * @param supportsGroupBy <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsGroupBy( Boolean supportsGroupBy );

    /**
     * Retrieves whether this database supports using a column that is not in the <code>SELECT</code> statement in a
     * <code>GROUP BY</code> clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsGroupByUnrelated();

    /**
     * Sets whether this database supports using a column that is not in the <code>SELECT</code> statement in a
     * <code>GROUP BY</code> clause.
     * 
     * @param supportsGroupByUnrelated <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsGroupByUnrelated( Boolean supportsGroupByUnrelated );

    /**
     * Retrieves whether this database supports using columns not included in the <code>SELECT</code> statement in a
     * <code>GROUP BY</code> clause provided that all of the columns in the <code>SELECT</code> statement are included in the
     * <code>GROUP BY</code> clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsGroupByBeyondSelect();

    /**
     * Sets whether this database supports using columns not included in the <code>SELECT</code> statement in a
     * <code>GROUP BY</code> clause provided that all of the columns in the <code>SELECT</code> statement are included in the
     * <code>GROUP BY</code> clause.
     * 
     * @param supportsGroupByBeyondSelect <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsGroupByBeyondSelect( Boolean supportsGroupByBeyondSelect );

    /**
     * Retrieves whether this database supports specifying a <code>LIKE</code> escape clause.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsLikeEscapeClause();

    /**
     * Sets whether this database supports specifying a <code>LIKE</code> escape clause.
     * 
     * @param supportsLikeEscapeClause <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsLikeEscapeClause( Boolean supportsLikeEscapeClause );

    /**
     * Retrieves whether this database supports getting multiple <code>ResultSet</code> objects from a single call to the method
     * <code>execute</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsMultipleResultSets();

    /**
     * Sets whether this database supports getting multiple <code>ResultSet</code> objects from a single call to the method
     * <code>execute</code>.
     * 
     * @param supportsMultipleResultSets <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsMultipleResultSets( Boolean supportsMultipleResultSets );

    /**
     * Retrieves whether this database allows having multiple transactions open at once (on different connections).
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsMultipleTransactions();

    /**
     * Sets whether this database allows having multiple transactions open at once (on different connections).
     * 
     * @param supportsMultipleTransactions <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsMultipleTransactions( Boolean supportsMultipleTransactions );

    /**
     * Retrieves whether columns in this database may be defined as non-nullable.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsNonNullableColumns();

    /**
     * Sets whether columns in this database may be defined as non-nullable.
     * 
     * @param supportsNonNullableColumns <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsNonNullableColumns( Boolean supportsNonNullableColumns );

    /**
     * Retrieves whether this database supports the ODBC Minimum SQL grammar.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsMinimumSQLGrammar();

    /**
     * Sets whether this database supports the ODBC Minimum SQL grammar.
     * 
     * @param supportsMinimumSQLGrammar <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsMinimumSQLGrammar( Boolean supportsMinimumSQLGrammar );

    /**
     * Retrieves whether this database supports the ODBC Core SQL grammar.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsCoreSQLGrammar();

    /**
     * Sets whether this database supports the ODBC Core SQL grammar.
     * 
     * @param supportsCoreSQLGrammar <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsCoreSQLGrammar( Boolean supportsCoreSQLGrammar );

    /**
     * Retrieves whether this database supports the ODBC Extended SQL grammar.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsExtendedSQLGrammar();

    /**
     * Sets whether this database supports the ODBC Extended SQL grammar.
     * 
     * @param supportsExtendedSQLGrammar <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsExtendedSQLGrammar( Boolean supportsExtendedSQLGrammar );

    /**
     * Retrieves whether this database supports the ANSI92 entry level SQL grammar.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsANSI92EntryLevelSQL();

    /**
     * Sets whether this database supports the ANSI92 entry level SQL grammar.
     * 
     * @param supportsANSI92EntryLevelSQL <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsANSI92EntryLevelSQL( Boolean supportsANSI92EntryLevelSQL );

    /**
     * Retrieves whether this database supports the ANSI92 intermediate SQL grammar supported.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsANSI92IntermediateSQL();

    /**
     * Sets whether this database supports the ANSI92 intermediate SQL grammar supported.
     * 
     * @param supportsANSI92IntermediateSQL <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsANSI92IntermediateSQL( Boolean supportsANSI92IntermediateSQL );

    /**
     * Retrieves whether this database supports the ANSI92 full SQL grammar supported.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsANSI92FullSQL();

    /**
     * Sets whether this database supports the ANSI92 full SQL grammar supported.
     * 
     * @param supportsANSI92FullSQL <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsANSI92FullSQL( Boolean supportsANSI92FullSQL );

    /**
     * Retrieves whether this database supports the SQL Integrity Enhancement Facility.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsIntegrityEnhancementFacility();

    /**
     * Sets whether this database supports the SQL Integrity Enhancement Facility.
     * 
     * @param supportsIntegrityEnhancementFacility <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsIntegrityEnhancementFacility( Boolean supportsIntegrityEnhancementFacility );

    /**
     * Retrieves whether this database supports some form of outer join.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsOuterJoins();

    /**
     * Sets whether this database supports some form of outer join.
     * 
     * @param supportsOuterJoins <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsOuterJoins( Boolean supportsOuterJoins );

    /**
     * Retrieves whether this database supports full nested outer joins.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsFullOuterJoins();

    /**
     * Sets whether this database supports full nested outer joins.
     * 
     * @param supportsFullOuterJoins <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsFullOuterJoins( Boolean supportsFullOuterJoins );

    /**
     * Retrieves whether this database provides limited support for outer joins. (This will be <code>true</code> if the method
     * <code>DatabaseMetaData.supportsFullOuterJoins</code> returns <code>true</code>).
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsLimitedOuterJoins();

    /**
     * Sets whether this database provides limited support for outer joins. (This will be <code>true</code> if the method
     * <code>DatabaseMetaData.supportsFullOuterJoins</code> returns <code>true</code>).
     * 
     * @param supportsLimitedOuterJoins <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsLimitedOuterJoins( Boolean supportsLimitedOuterJoins );

    /**
     * Retrieves the database vendor's preferred term for "schema".
     * 
     * @return the vendor term for "schema"
     */
    String getSchemaTerm();

    /**
     * Sets the database vendor's preferred term for "schema".
     * 
     * @param schemaTerm the vendor term for "schema"
     */
    void setSchemaTerm( String schemaTerm );

    /**
     * Retrieves the database vendor's preferred term for "procedure".
     * 
     * @return the vendor term for "procedure"
     */
    String getProcedureTerm();

    /**
     * Sets the database vendor's preferred term for "procedure".
     * 
     * @param procedureTerm the vendor term for "procedure"
     */
    void setProcedureTerm( String procedureTerm );

    /**
     * Retrieves the database vendor's preferred term for "catalog".
     * 
     * @return the vendor term for "catalog"
     */
    String getCatalogTerm();

    /**
     * Sets the database vendor's preferred term for "catalog".
     * 
     * @param catalogTerm the vendor term for "catalog"
     */
    void setCatalogTerm( String catalogTerm );

    /**
     * Retrieves whether a catalog appears at the start of a fully qualified table name. If not, the catalog appears at the end.
     * 
     * @return <code>true</code> if the catalog name appears at the beginning of a fully qualified table name; <code>false</code>
     *         otherwise
     */
    Boolean isCatalogAtStart();

    /**
     * Sets whether a catalog appears at the start of a fully qualified table name. If not, the catalog appears at the end.
     * 
     * @param catalogAtStart <code>true</code> if the catalog name appears at the beginning of a fully qualified table name;
     *        <code>false</code> otherwise
     */
    void setCatalogAtStart( Boolean catalogAtStart );

    /**
     * Retrieves the <code>String</code> that this database uses as the separator between a catalog and table name.
     * 
     * @return the separator string
     */
    String getCatalogSeparator();

    /**
     * Sets the <code>String</code> that this database uses as the separator between a catalog and table name.
     * 
     * @param catalogSeparator the separator string
     */
    void setCatalogSeparator( String catalogSeparator );

    /**
     * Retrieves whether a schema name can be used in a data manipulation statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSchemasInDataManipulation();

    /**
     * Sets whether a schema name can be used in a data manipulation statement.
     * 
     * @param supportsSchemasInDataManipulation <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSchemasInDataManipulation( Boolean supportsSchemasInDataManipulation );

    /**
     * Retrieves whether a schema name can be used in a procedure call statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSchemasInProcedureCalls();

    /**
     * Sets whether a schema name can be used in a procedure call statement.
     * 
     * @param supportsSchemasInProcedureCalls <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSchemasInProcedureCalls( Boolean supportsSchemasInProcedureCalls );

    /**
     * Retrieves whether a schema name can be used in a table definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSchemasInTableDefinitions();

    /**
     * Sets whether a schema name can be used in a table definition statement.
     * 
     * @param supportsSchemasInTableDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSchemasInTableDefinitions( Boolean supportsSchemasInTableDefinitions );

    /**
     * Retrieves whether a schema name can be used in an index definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSchemasInIndexDefinitions();

    /**
     * Sets whether a schema name can be used in an index definition statement.
     * 
     * @param supportsSchemasInIndexDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSchemasInIndexDefinitions( Boolean supportsSchemasInIndexDefinitions );

    /**
     * Retrieves whether a schema name can be used in a privilege definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSchemasInPrivilegeDefinitions();

    /**
     * Sets whether a schema name can be used in a privilege definition statement.
     * 
     * @param supportsSchemasInPrivilegeDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSchemasInPrivilegeDefinitions( Boolean supportsSchemasInPrivilegeDefinitions );

    /**
     * Retrieves whether a catalog name can be used in a data manipulation statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsCatalogsInDataManipulation();

    /**
     * Sets whether a catalog name can be used in a data manipulation statement.
     * 
     * @param supportsCatalogsInDataManipulation <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsCatalogsInDataManipulation( Boolean supportsCatalogsInDataManipulation );

    /**
     * Retrieves whether a catalog name can be used in a procedure call statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsCatalogsInProcedureCalls();

    /**
     * Sets whether a catalog name can be used in a procedure call statement.
     * 
     * @param supportsCatalogsInProcedureCalls <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsCatalogsInProcedureCalls( Boolean supportsCatalogsInProcedureCalls );

    /**
     * Retrieves whether a catalog name can be used in a table definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsCatalogsInTableDefinitions();

    /**
     * Sets whether a catalog name can be used in a table definition statement.
     * 
     * @param supportsCatalogsInTableDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsCatalogsInTableDefinitions( Boolean supportsCatalogsInTableDefinitions );

    /**
     * Retrieves whether a catalog name can be used in an index definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    Boolean isSupportsCatalogsInIndexDefinitions();

    /**
     * Sets whether a catalog name can be used in an index definition statement.
     * 
     * @param supportsCatalogsInIndexDefinitions <code>true</code> if so; <code>false</code> otherwise
     * @exception SQLException if a database access error occurs
     */
    void setSupportsCatalogsInIndexDefinitions( Boolean supportsCatalogsInIndexDefinitions );

    /**
     * Retrieves whether a catalog name can be used in a privilege definition statement.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsCatalogsInPrivilegeDefinitions();

    /**
     * Sets whether a catalog name can be used in a privilege definition statement.
     * 
     * @param supportsCatalogsInPrivilegeDefinitions <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsCatalogsInPrivilegeDefinitions( Boolean supportsCatalogsInPrivilegeDefinitions );

    /**
     * Retrieves whether this database supports positioned <code>DELETE</code> statements.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsPositionedDelete();

    /**
     * Sets whether this database supports positioned <code>DELETE</code> statements.
     * 
     * @param supportsPositionedDelete <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsPositionedDelete( Boolean supportsPositionedDelete );

    /**
     * Retrieves whether this database supports positioned <code>UPDATE</code> statements.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsPositionedUpdate();

    /**
     * Sets whether this database supports positioned <code>UPDATE</code> statements.
     * 
     * @param supportsPositionedUpdate <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsPositionedUpdate( Boolean supportsPositionedUpdate );

    /**
     * Retrieves whether this database supports <code>SELECT FOR UPDATE</code> statements.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSelectForUpdate();

    /**
     * Sets whether this database supports <code>SELECT FOR UPDATE</code> statements.
     * 
     * @param supportsSelectForUpdate <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSelectForUpdate( Boolean supportsSelectForUpdate );

    /**
     * Retrieves whether this database supports stored procedure calls that use the stored procedure escape syntax.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsStoredProcedures();

    /**
     * Sets whether this database supports stored procedure calls that use the stored procedure escape syntax.
     * 
     * @param supportsStoredProcedures <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsStoredProcedures( Boolean supportsStoredProcedures );

    /**
     * Retrieves whether this database supports subqueries in comparison expressions.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSubqueriesInComparisons();

    /**
     * Retrieves whether this database supports subqueries in comparison expressions.
     * 
     * @param supportsSubqueriesInComparisons <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSubqueriesInComparisons( Boolean supportsSubqueriesInComparisons );

    /**
     * Retrieves whether this database supports subqueries in <code>EXISTS</code> expressions.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSubqueriesInExists();

    /**
     * Sets whether this database supports subqueries in <code>EXISTS</code> expressions.
     * 
     * @param supportsSubqueriesInExists <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSubqueriesInExists( Boolean supportsSubqueriesInExists );

    /**
     * Retrieves whether this database supports subqueries in <code>IN</code> statements.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSubqueriesInIns();

    /**
     * Sets whether this database supports subqueries in <code>IN</code> statements.
     * 
     * @param supportsSubqueriesInIns <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSubqueriesInIns( Boolean supportsSubqueriesInIns );

    /**
     * Retrieves whether this database supports subqueries in quantified expressions.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsSubqueriesInQuantifieds();

    /**
     * Sets whether this database supports subqueries in quantified expressions.
     * 
     * @param supportsSubqueriesInQuantifieds <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsSubqueriesInQuantifieds( Boolean supportsSubqueriesInQuantifieds );

    /**
     * Retrieves whether this database supports correlated subqueries.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsCorrelatedSubqueries();

    /**
     * Sets whether this database supports correlated subqueries.
     * 
     * @param supportsCorrelatedSubqueries <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsCorrelatedSubqueries( Boolean supportsCorrelatedSubqueries );

    /**
     * Retrieves whether this database supports SQL <code>UNION</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsUnion();

    /**
     * Sets whether this database supports SQL <code>UNION</code>.
     * 
     * @param supportsUnion <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsUnion( Boolean supportsUnion );

    /**
     * Retrieves whether this database supports SQL <code>UNION ALL</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsUnionAll();

    /**
     * Sets whether this database supports SQL <code>UNION ALL</code>.
     * 
     * @param supportsUnionAll <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsUnionAll( Boolean supportsUnionAll );

    /**
     * Retrieves whether this database supports keeping cursors open across commits.
     * 
     * @return <code>true</code> if cursors always remain open; <code>false</code> if they might not remain open
     */
    Boolean isSupportsOpenCursorsAcrossCommit();

    /**
     * Sets whether this database supports keeping cursors open across commits.
     * 
     * @param supportsOpenCursorsAcrossCommit <code>true</code> if cursors always remain open; <code>false</code> if they might
     *        not remain open
     */
    void setSupportsOpenCursorsAcrossCommit( Boolean supportsOpenCursorsAcrossCommit );

    /**
     * Retrieves whether this database supports keeping cursors open across rollbacks.
     * 
     * @return <code>true</code> if cursors always remain open; <code>false</code> if they might not remain open
     */
    Boolean isSupportsOpenCursorsAcrossRollback();

    /**
     * Sets whether this database supports keeping cursors open across rollbacks.
     * 
     * @param supportsOpenCursorsAcrossRollback <code>true</code> if cursors always remain open; <code>false</code> if they might
     *        not remain open
     */
    void setSupportsOpenCursorsAcrossRollback( Boolean supportsOpenCursorsAcrossRollback );

    /**
     * Retrieves whether this database supports keeping statements open across commits.
     * 
     * @return <code>true</code> if statements always remain open; <code>false</code> if they might not remain open
     */
    Boolean isSupportsOpenStatementsAcrossCommit();

    /**
     * sets whether this database supports keeping statements open across commits.
     * 
     * @param supportsOpenStatementsAcrossCommit <code>true</code> if statements always remain open; <code>false</code> if they
     *        might not remain open
     */
    void setSupportsOpenStatementsAcrossCommit( Boolean supportsOpenStatementsAcrossCommit );

    /**
     * Retrieves whether this database supports keeping statements open across rollbacks.
     * 
     * @return <code>true</code> if statements always remain open; <code>false</code> if they might not remain open
     */
    Boolean isSupportsOpenStatementsAcrossRollback();

    /**
     * Sets whether this database supports keeping statements open across rollbacks.
     * 
     * @param supportsOpenStatementsAcrossRollback <code>true</code> if statements always remain open; <code>false</code> if they
     *        might not remain open
     */
    void setSupportsOpenStatementsAcrossRollback( Boolean supportsOpenStatementsAcrossRollback );

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
    Integer getMaxBinaryLiteralLength();

    /**
     * sets the maximum number of hex characters this database allows in an inline binary literal.
     * 
     * @param maxBinaryLiteralLength max the maximum length (in hex characters) for a binary literal; a result of zero means that
     *        there is no limit or the limit is not known
     */
    void setMaxBinaryLiteralLength( Integer maxBinaryLiteralLength );

    /**
     * Retrieves the maximum number of characters this database allows for a character literal.
     * 
     * @return the maximum number of characters allowed for a character literal; a result of zero means that there is no limit or
     *         the limit is not known
     */
    Integer getMaxCharLiteralLength();

    /**
     * Sets the maximum number of characters this database allows for a character literal.
     * 
     * @param maxCharLiteralLength the maximum number of characters allowed for a character literal; a result of zero means that
     *        there is no limit or the limit is not known
     */
    void setMaxCharLiteralLength( Integer maxCharLiteralLength );

    /**
     * Retrieves the maximum number of characters this database allows for a column name.
     * 
     * @return the maximum number of characters allowed for a column name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxColumnNameLength();

    /**
     * Sets the maximum number of characters this database allows for a column name.
     * 
     * @param maxColumnNameLength the maximum number of characters allowed for a column name; a result of zero means that there is
     *        no limit or the limit is not known
     */
    void setMaxColumnNameLength( Integer maxColumnNameLength );

    /**
     * Retrieves the maximum number of columns this database allows in a <code>GROUP BY</code> clause.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    Integer getMaxColumnsInGroupBy();

    /**
     * Sets the maximum number of columns this database allows in a <code>GROUP BY</code> clause.
     * 
     * @param maxColumnsInGroupBy the maximum number of columns allowed; a result of zero means that there is no limit or the
     *        limit is not known
     */
    void setMaxColumnsInGroupBy( Integer maxColumnsInGroupBy );

    /**
     * Retrieves the maximum number of columns this database allows in an index.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    Integer getMaxColumnsInIndex();

    /**
     * Sets the maximum number of columns this database allows in an index.
     * 
     * @param maxColumnsInIndex the maximum number of columns allowed; a result of zero means that there is no limit or the limit
     *        is not known
     */
    void setMaxColumnsInIndex( Integer maxColumnsInIndex );

    /**
     * Retrieves the maximum number of columns this database allows in an <code>ORDER BY</code> clause.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    Integer getMaxColumnsInOrderBy();

    /**
     * Sets the maximum number of columns this database allows in an <code>ORDER BY</code> clause.
     * 
     * @param maxColumnsInOrderBy the maximum number of columns allowed; a result of zero means that there is no limit or the
     *        limit is not known
     */
    void setMaxColumnsInOrderBy( Integer maxColumnsInOrderBy );

    /**
     * Retrieves the maximum number of columns this database allows in a <code>SELECT</code> list.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    Integer getMaxColumnsInSelect();

    /**
     * Sets the maximum number of columns this database allows in a <code>SELECT</code> list.
     * 
     * @param maxColumnsInSelect the maximum number of columns allowed; a result of zero means that there is no limit or the limit
     *        is not known
     */
    void setMaxColumnsInSelect( Integer maxColumnsInSelect );

    /**
     * Retrieves the maximum number of columns this database allows in a table.
     * 
     * @return the maximum number of columns allowed; a result of zero means that there is no limit or the limit is not known
     */
    Integer getMaxColumnsInTable();

    /**
     * Sets the maximum number of columns this database allows in a table.
     * 
     * @param maxColumnsInTable the maximum number of columns allowed; a result of zero means that there is no limit or the limit
     *        is not known
     */
    void setMaxColumnsInTable( Integer maxColumnsInTable );

    /**
     * Retrieves the maximum number of concurrent connections to this database that are possible.
     * 
     * @return the maximum number of active connections possible at one time; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxConnections();

    /**
     * Sets the maximum number of concurrent connections to this database that are possible.
     * 
     * @param maxConnections the maximum number of active connections possible at one time; a result of zero means that there is
     *        no limit or the limit is not known
     */
    void setMaxConnections( Integer maxConnections );

    /**
     * Retrieves the maximum number of characters that this database allows in a cursor name.
     * 
     * @return the maximum number of characters allowed in a cursor name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxCursorNameLength();

    /**
     * Sets the maximum number of characters that this database allows in a cursor name.
     * 
     * @param maxCursorNameLength the maximum number of characters allowed in a cursor name; a result of zero means that there is
     *        no limit or the limit is not known
     */
    void setMaxCursorNameLength( Integer maxCursorNameLength );

    /**
     * Retrieves the maximum number of bytes this database allows for an index, including all of the parts of the index.
     * 
     * @return the maximum number of bytes allowed; this limit includes the composite of all the constituent parts of the index; a
     *         result of zero means that there is no limit or the limit is not known
     */
    Integer getMaxIndexLength();

    /**
     * Sets the maximum number of bytes this database allows for an index, including all of the parts of the index.
     * 
     * @param maxIndexLength the maximum number of bytes allowed; this limit includes the composite of all the constituent parts
     *        of the index; a result of zero means that there is no limit or the limit is not known
     */
    void setMaxIndexLength( Integer maxIndexLength );

    /**
     * Retrieves the maximum number of characters that this database allows in a schema name.
     * 
     * @return the maximum number of characters allowed in a schema name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxSchemaNameLength();

    /**
     * Sets the maximum number of characters that this database allows in a schema name.
     * 
     * @param maxSchemaNameLength the maximum number of characters allowed in a schema name; a result of zero means that there is
     *        no limit or the limit is not known
     */
    void setMaxSchemaNameLength( Integer maxSchemaNameLength );

    /**
     * Retrieves the maximum number of characters that this database allows in a procedure name.
     * 
     * @return the maximum number of characters allowed in a procedure name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxProcedureNameLength();

    /**
     * Sets the maximum number of characters that this database allows in a procedure name.
     * 
     * @param maxProcedureNameLength the maximum number of characters allowed in a procedure name; a result of zero means that
     *        there is no limit or the limit is not known
     */
    void setMaxProcedureNameLength( Integer maxProcedureNameLength );

    /**
     * Retrieves the maximum number of characters that this database allows in a catalog name.
     * 
     * @return the maximum number of characters allowed in a catalog name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxCatalogNameLength();

    /**
     * Sets the maximum number of characters that this database allows in a catalog name.
     * 
     * @param maxCatalogNameLength the maximum number of characters allowed in a catalog name; a result of zero means that there
     *        is no limit or the limit is not known
     */
    void setMaxCatalogNameLength( Integer maxCatalogNameLength );

    /**
     * Retrieves the maximum number of bytes this database allows in a single row.
     * 
     * @return the maximum number of bytes allowed for a row; a result of zero means that there is no limit or the limit is not
     *         known
     */
    Integer getMaxRowSize();

    /**
     * Sets the maximum number of bytes this database allows in a single row.
     * 
     * @param maxRowSize the maximum number of bytes allowed for a row; a result of zero means that there is no limit or the limit
     *        is not known
     */
    void setMaxRowSize( Integer maxRowSize );

    /**
     * Retrieves whether the return value for the method <code>getMaxRowSize</code> includes the SQL data types
     * <code>LONGVARCHAR</code> and <code>LONGVARBINARY</code>.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isMaxRowSizeIncludeBlobs();

    /**
     * Sets whether the return value for the method <code>getMaxRowSize</code> includes the SQL data types
     * <code>LONGVARCHAR</code> and <code>LONGVARBINARY</code>.
     * 
     * @param maxRowSizeIncludeBlobs <code>true</code> if so; <code>false</code> otherwise
     */
    void setMaxRowSizeIncludeBlobs( Boolean maxRowSizeIncludeBlobs );

    /**
     * Retrieves the maximum number of characters this database allows in an SQL statement.
     * 
     * @return the maximum number of characters allowed for an SQL statement; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxStatementLength();

    /**
     * Sets the maximum number of characters this database allows in an SQL statement.
     * 
     * @param maxStatementLength the maximum number of characters allowed for an SQL statement; a result of zero means that there
     *        is no limit or the limit is not known
     */
    void setMaxStatementLength( Integer maxStatementLength );

    /**
     * Retrieves the maximum number of active statements to this database that can be open at the same time.
     * 
     * @return the maximum number of statements that can be open at one time; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxStatements();

    /**
     * Sets the maximum number of active statements to this database that can be open at the same time.
     * 
     * @param maxStatements the maximum number of statements that can be open at one time; a result of zero means that there is no
     *        limit or the limit is not known
     */
    void setMaxStatements( Integer maxStatements );

    /**
     * Retrieves the maximum number of characters this database allows in a table name.
     * 
     * @return the maximum number of characters allowed for a table name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxTableNameLength();

    /**
     * Sets the maximum number of characters this database allows in a table name.
     * 
     * @param maxTableNameLength the maximum number of characters allowed for a table name; a result of zero means that there is
     *        no limit or the limit is not known
     */
    void setMaxTableNameLength( Integer maxTableNameLength );

    /**
     * Retrieves the maximum number of tables this database allows in a <code>SELECT</code> statement.
     * 
     * @return the maximum number of tables allowed in a <code>SELECT</code> statement; a result of zero means that there is no
     *         limit or the limit is not known
     */
    Integer getMaxTablesInSelect();

    /**
     * Sets the maximum number of tables this database allows in a <code>SELECT</code> statement.
     * 
     * @param maxTablesInSelect the maximum number of tables allowed in a <code>SELECT</code> statement; a result of zero means
     *        that there is no limit or the limit is not known
     */
    void setMaxTablesInSelect( Integer maxTablesInSelect );

    /**
     * Retrieves the maximum number of characters this database allows in a user name.
     * 
     * @return the maximum number of characters allowed for a user name; a result of zero means that there is no limit or the
     *         limit is not known
     */
    Integer getMaxUserNameLength();

    /**
     * Sets the maximum number of characters this database allows in a user name.
     * 
     * @param maxUserNameLength the maximum number of characters allowed for a user name; a result of zero means that there is no
     *        limit or the limit is not known
     */
    void setMaxUserNameLength( Integer maxUserNameLength );

    /**
     * Retrieves this database's default transaction isolation level. The possible values are defined in
     * <code>java.sql.Connection</code>.
     * 
     * @return the default isolation level
     * @see Connection
     */
    Integer getDefaultTransactionIsolation();

    /**
     * Sets this database's default transaction isolation level. The possible values are defined in
     * <code>java.sql.Connection</code>.
     * 
     * @param defaultTransactionIsolation the default isolation level
     * @see Connection
     */
    void setDefaultTransactionIsolation( Integer defaultTransactionIsolation );

    /**
     * Retrieves whether this database supports transactions. If not, invoking the method <code>commit</code> is a noop, and the
     * isolation level is <code>TRANSACTION_NONE</code>.
     * 
     * @return <code>true</code> if transactions are supported; <code>false</code> otherwise
     */
    Boolean isSupportsTransactions();

    /**
     * Sets whether this database supports transactions. If not, invoking the method <code>commit</code> is a noop, and the
     * isolation level is <code>TRANSACTION_NONE</code>.
     * 
     * @param supportsTransactions <code>true</code> if transactions are supported; <code>false</code> otherwise
     */
    void setSupportsTransactions( Boolean supportsTransactions );

    /**
     * Retrieves list of database supported transaction isolation levels.
     * 
     * @return list of database supported transaction isolation levels.
     * @see Connection
     */
    Set<TransactionIsolationLevelType> getSupportedTransactionIsolationLevels();

    /**
     * Adds TransactionIsolationLevelType
     * 
     * @param transactionIsolationLevelType the Transaction Isolation Level Type
     */
    void addSupportedTransactionIsolationLevelType( TransactionIsolationLevelType transactionIsolationLevelType );

    /**
     * Deletes TransactionIsolationLevelType
     * 
     * @param transactionIsolationLevelType the Transaction Isolation Level Type
     */
    void deleteSupportedTransactionIsolationLevelType( TransactionIsolationLevelType transactionIsolationLevelType );

    /**
     * Is supported TransactionIsolationLevelType
     * 
     * @param transactionIsolationLevelType the Transaction Isolation Level Type
     */
    Boolean isSupportedTransactionIsolationLevelType( TransactionIsolationLevelType transactionIsolationLevelType );

    /**
     * Retrieves whether this database supports both data definition and data manipulation statements within a transaction.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsDataDefinitionAndDataManipulationTransactions();

    /**
     * Sets whether this database supports both data definition and data manipulation statements within a transaction.
     * 
     * @param supportsDataDefinitionAndDataManipulationTransactions <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsDataDefinitionAndDataManipulationTransactions( Boolean supportsDataDefinitionAndDataManipulationTransactions );

    /**
     * Retrieves whether this database supports only data manipulation statements within a transaction.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isSupportsDataManipulationTransactionsOnly();

    /**
     * Sets whether this database supports only data manipulation statements within a transaction.
     * 
     * @param supportsDataManipulationTransactionsOnly <code>true</code> if so; <code>false</code> otherwise
     */
    void setSupportsDataManipulationTransactionsOnly( Boolean supportsDataManipulationTransactionsOnly );

    /**
     * Retrieves whether a data definition statement within a transaction forces the transaction to commit.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isDataDefinitionCausesTransactionCommit();

    /**
     * Sets whether a data definition statement within a transaction forces the transaction to commit.
     * 
     * @param dataDefinitionCausesTransactionCommit <code>true</code> if so; <code>false</code> otherwise
     */
    void setDataDefinitionCausesTransactionCommit( Boolean dataDefinitionCausesTransactionCommit );

    /**
     * Retrieves whether this database ignores a data definition statement within a transaction.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     */
    Boolean isDataDefinitionIgnoredInTransactions();

    /**
     * Sets whether this database ignores a data definition statement within a transaction.
     * 
     * @param dataDefinitionIgnoredInTransactions <code>true</code> if so; <code>false</code> otherwise
     */
    void setDataDefinitionIgnoredInTransactions( Boolean dataDefinitionIgnoredInTransactions );

    /**
     * Retrieves a description of the stored procedures available in the given catalog.
     * 
     * @return a set of stored procedures available
     */
    Set<StoredProcedure> getStoredProcedures();

    /**
     * Adds Stored Procedure
     * 
     * @param storedProcedure the Stored Procedure
     */
    void addStoredProcedure( StoredProcedure storedProcedure );

    /**
     * Deletes Stored Procedure
     * 
     * @param storedProcedure the Stored Procedure
     */
    void deleteStoredProcedure( StoredProcedure storedProcedure );

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
    StoredProcedure findStoredProcedureByName( String catalog,
                                               String schema,
                                               String procedureName );

    /**
     * Retrieves a description of the tables available in the given catalog.
     * 
     * @return a set of tables available
     */
    Set<Table> getTables();

    /**
     * Adds Table
     * 
     * @param table the table to add
     */
    void addTable( Table table );

    /**
     * Deletes Table
     * 
     * @param table the table to delete
     */
    void deleteTable( Table table );

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
    Table findTableByName( String catalog,
                           String schema,
                           String tableName );

    /**
     * Retrieves the schemas available in this database. The results are ordered by schema name.
     * 
     * @return schemas available in this database.
     */
    Set<Schema> getSchemas();

    /**
     * Adds Schema
     * 
     * @param schema the Schema
     */
    void addSchema( Schema schema );

    /**
     * Deletes Schema
     * 
     * @param schema the Schema
     */
    void deleteSchema( Schema schema );

    /**
     * Finds schema by its name.
     * 
     * @param catalog a catalog name; must match the catalog name as it is stored in the database; "" retrieves those without a
     *        catalog; <code>null</code> means that the catalog name should not be used to narrow the search
     * @param schemaName a schema name; must match the schema name as it is stored in the database;
     * @return schema or null if not found
     */
    Schema findSchemaByName( String catalog,
                             String schemaName );

    /**
     * Retrieves the catalogs available in this database
     * 
     * @return catalogs available in this database
     */
    Set<Catalog> getCatalogs();

    /**
     * Adds Catalog
     * 
     * @param catalog the catalog to add
     */
    void addCatalog( Catalog catalog );

    /**
     * Deletes Catalog
     * 
     * @param catalog the catalog to delete
     */
    void deleteCatalog( Catalog catalog );

    /**
     * Finds catalog by its name.
     * 
     * @param catalogName a catalog name; must match the catalog name as it is stored in the database;
     * @return catalog or null if not found
     */
    Catalog findCatalogByName( String catalogName );

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
    Set<TableType> getTableTypes();

    /**
     * Adds TableType
     * 
     * @param tableType the table type to add
     */
    void addTableType( TableType tableType );

    /**
     * Deletes TableType
     * 
     * @param tableType the table type to delete
     */
    void deleteTableType( TableType tableType );

    /**
     * Finds table type by its name.
     * 
     * @param typeName a table type name; must match the type name as it is stored in the database;
     * @return table type or null if not found
     */
    TableType findTableTypeByTypeName( String typeName );

    /**
     * Retrieves a description of all the standard SQL types supported by this database
     * 
     * @return all the standard SQL types supported by this database
     */
    Set<SqlTypeInfo> getSqlTypeInfos();

    /**
     * Adds SqlTypeInfo
     * 
     * @param sqlTypeInfo the SQL type to add
     */
    void addSqlTypeInfo( SqlTypeInfo sqlTypeInfo );

    /**
     * Deletes SqlTypeInfo
     * 
     * @param sqlTypeInfo the SQL type to delete
     */
    void deleteSqlTypeInfo( SqlTypeInfo sqlTypeInfo );

    /**
     * Finds SQL type by its name.
     * 
     * @param typeName a table type name; must match the type name as it is stored in the database;
     * @return table type or null if not found
     */
    SqlTypeInfo findSqlTypeInfoByTypeName( String typeName );

    // ===============================================================
    // --------------------------JDBC 2.0-----------------------------
    // ===============================================================

    /**
     * Retrieves database supported result set types.
     * 
     * @return database supported result set types.
     * @see Connection
     * @since 1.2 (JDBC 2.0)
     */
    Set<ResultSetType> getSupportedResultSetTypes();

    /**
     * Adds supported ResultSetType
     * 
     * @param resultSetType the ResultSetType
     */
    void addSupportedResultSetType( ResultSetType resultSetType );

    /**
     * Deletes supported ResultSetType
     * 
     * @param resultSetType the ResultSetType
     */
    void deleteSupportedResultSetType( ResultSetType resultSetType );

    /**
     * Retrieves database supported concurrencies for the given result set type.
     * 
     * @return database supported concurrencies for the given result set type.
     * @see Connection
     * @since 1.2 (JDBC 2.0)
     */
    Set<ResultSetConcurrencyType> getSupportedForwardOnlyResultSetConcurrencies();

    /**
     * Adds ResultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    void addSupportedForwardOnlyResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType );

    /**
     * Deletes ResultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    void deleteSupportedForwardOnlyResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType );

    /**
     * Retrieves database supported concurrencies for the given result set type.
     * 
     * @return database supported concurrencies for the given result set type.
     * @see Connection
     * @since 1.2 (JDBC 2.0)
     */
    Set<ResultSetConcurrencyType> getSupportedScrollInsensitiveResultSetConcurrencies();

    /**
     * Adds ResultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    void addSupportedScrollInsensitiveResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType );

    /**
     * Delete ResultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    void deleteSupportedScrollInsensitiveResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType );

    /**
     * Retrieves database supported concurrencies for the given result set type.
     * 
     * @return database supported concurrencies for the given result set type.
     * @see Connection
     * @since 1.2 (JDBC 2.0)
     */
    Set<ResultSetConcurrencyType> getSupportedScrollSensitiveResultSetConcurrencies();

    /**
     * Adds resultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    void addSupportedScrollSensitiveResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType );

    /**
     * delete resultSetConcurrencyType
     * 
     * @param resultSetConcurrencyType the ResultSetConcurrencyType
     */
    void deleteSupportedScrollSensitiveResultSetConcurrency( ResultSetConcurrencyType resultSetConcurrencyType );

    /**
     * Retrieves whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @return <code>true</code> if updates are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isForwardOnlyResultSetOwnUpdatesAreVisible();

    /**
     * Sets whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @param forwardOnlyResultSetOwnUpdatesAreVisible <code>true</code> if updates are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setForwardOnlyResultSetOwnUpdatesAreVisible( Boolean forwardOnlyResultSetOwnUpdatesAreVisible );

    /**
     * Retrieves whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @return <code>true</code> if updates are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollInsensitiveResultSetOwnUpdatesAreVisible();

    /**
     * Sets whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @param scrollInsensitiveResultSetOwnUpdatesAreVisible <code>true</code> if updates are visible for the given result set
     *        type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollInsensitiveResultSetOwnUpdatesAreVisible( Boolean scrollInsensitiveResultSetOwnUpdatesAreVisible );

    /**
     * Retrieves whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @return <code>true</code> if updates are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollSensitiveResultSetOwnUpdatesAreVisible();

    /**
     * Sets whether for the given type of <code>ResultSet</code> object, the result set's own updates are visible.
     * 
     * @param scrollSensitiveResultSetOwnUpdatesAreVisible <code>true</code> if updates are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollSensitiveResultSetOwnUpdatesAreVisible( Boolean scrollSensitiveResultSetOwnUpdatesAreVisible );

    /**
     * Retrieves whether a result set's own deletes are visible.
     * 
     * @return <code>true</code> if deletes are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isForwardOnlyResultSetOwnDeletesAreVisible();

    /**
     * Sets whether a result set's own deletes are visible.
     * 
     * @param forwardOnlyResultSetOwnDeletesAreVisible <code>true</code> if deletes are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setForwardOnlyResultSetOwnDeletesAreVisible( Boolean forwardOnlyResultSetOwnDeletesAreVisible );

    /**
     * Retrieves whether a result set's own deletes are visible.
     * 
     * @return <code>true</code> if deletes are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollInsensitiveResultSetOwnDeletesAreVisible();

    /**
     * Sets whether a result set's own deletes are visible.
     * 
     * @param scrollInsensitiveResultSetOwnDeletesAreVisible <code>true</code> if deletes are visible for the given result set
     *        type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollInsensitiveResultSetOwnDeletesAreVisible( Boolean scrollInsensitiveResultSetOwnDeletesAreVisible );

    /**
     * Retrieves whether a result set's own deletes are visible.
     * 
     * @return <code>true</code> if deletes are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollSensitiveResultSetOwnDeletesAreVisible();

    /**
     * Sets whether a result set's own deletes are visible.
     * 
     * @param scrollSensitiveResultSetOwnDeletesAreVisible <code>true</code> if deletes are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollSensitiveResultSetOwnDeletesAreVisible( Boolean scrollSensitiveResultSetOwnDeletesAreVisible );

    /**
     * Retrieves whether a result set's own inserts are visible.
     * 
     * @return <code>true</code> if inserts are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isForwardOnlyResultSetOwnInsertsAreVisible();

    /**
     * Sets whether a result set's own inserts are visible.
     * 
     * @param forwardOnlyResultSetOwnInsertsAreVisible <code>true</code> if inserts are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setForwardOnlyResultSetOwnInsertsAreVisible( Boolean forwardOnlyResultSetOwnInsertsAreVisible );

    /**
     * Retrieves whether a result set's own inserts are visible.
     * 
     * @return <code>true</code> if inserts are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollInsensitiveResultSetOwnInsertsAreVisible();

    /**
     * Sets whether a result set's own inserts are visible.
     * 
     * @param scrollInsensitiveResultSetOwnInsertsAreVisible <code>true</code> if inserts are visible for the given result set
     *        type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollInsensitiveResultSetOwnInsertsAreVisible( Boolean scrollInsensitiveResultSetOwnInsertsAreVisible );

    /**
     * Retrieves whether a result set's own inserts are visible.
     * 
     * @return <code>true</code> if inserts are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollSensitiveResultSetOwnInsertsAreVisible();

    /**
     * Sets whether a result set's own inserts are visible.
     * 
     * @param scrollSensitiveResultSetOwnInsertsAreVisible <code>true</code> if inserts are visible for the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollSensitiveResultSetOwnInsertsAreVisible( Boolean scrollSensitiveResultSetOwnInsertsAreVisible );

    /**
     * Retrieves whether updates made by others are visible.
     * 
     * @return <code>true</code> if updates made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isForwardOnlyResultSetOthersUpdatesAreVisible();

    /**
     * Sets whether updates made by others are visible.
     * 
     * @param forwardOnlyResultSetOthersUpdatesAreVisible <code>true</code> if updates made by others are visible for the given
     *        result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setForwardOnlyResultSetOthersUpdatesAreVisible( Boolean forwardOnlyResultSetOthersUpdatesAreVisible );

    /**
     * Retrieves whether updates made by others are visible.
     * 
     * @return <code>true</code> if updates made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollInsensitiveResultSetOthersUpdatesAreVisible();

    /**
     * Sets whether updates made by others are visible.
     * 
     * @param scrollInsensitiveResultSetOthersUpdatesAreVisible <code>true</code> if updates made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollInsensitiveResultSetOthersUpdatesAreVisible( Boolean scrollInsensitiveResultSetOthersUpdatesAreVisible );

    /**
     * Retrieves whether updates made by others are visible.
     * 
     * @return <code>true</code> if updates made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollSensitiveResultSetOthersUpdatesAreVisible();

    /**
     * Sets whether updates made by others are visible.
     * 
     * @param scrollSensitiveResultSetOthersUpdatesAreVisible <code>true</code> if updates made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollSensitiveResultSetOthersUpdatesAreVisible( Boolean scrollSensitiveResultSetOthersUpdatesAreVisible );

    /**
     * Retrieves whether deletes made by others are visible.
     * 
     * @return <code>true</code> if deletes made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isForwardOnlyResultSetOthersDeletesAreVisible();

    /**
     * Sets whether deletes made by others are visible.
     * 
     * @param forwardOnlyResultSetOthersDeletesAreVisible <code>true</code> if deletes made by others are visible for the given
     *        result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setForwardOnlyResultSetOthersDeletesAreVisible( Boolean forwardOnlyResultSetOthersDeletesAreVisible );

    /**
     * Retrieves whether deletes made by others are visible.
     * 
     * @return <code>true</code> if deletes made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollInsensitiveResultSetOthersDeletesAreVisible();

    /**
     * Sets whether deletes made by others are visible.
     * 
     * @param scrollInsensitiveResultSetOthersDeletesAreVisible <code>true</code> if deletes made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollInsensitiveResultSetOthersDeletesAreVisible( Boolean scrollInsensitiveResultSetOthersDeletesAreVisible );

    /**
     * Retrieves whether deletes made by others are visible.
     * 
     * @return <code>true</code> if deletes made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollSensitiveResultSetOthersDeletesAreVisible();

    /**
     * Sets whether deletes made by others are visible.
     * 
     * @param scrollSensitiveResultSetOthersDeletesAreVisible <code>true</code> if deletes made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollSensitiveResultSetOthersDeletesAreVisible( Boolean scrollSensitiveResultSetOthersDeletesAreVisible );

    /**
     * Retrieves whether inserts made by others are visible.
     * 
     * @return <code>true</code> if inserts made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isForwardOnlyResultSetOthersInsertsAreVisible();

    /**
     * Sets whether inserts made by others are visible.
     * 
     * @param forwardOnlyResultSetOthersInsertsAreVisible <code>true</code> if inserts made by others are visible for the given
     *        result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setForwardOnlyResultSetOthersInsertsAreVisible( Boolean forwardOnlyResultSetOthersInsertsAreVisible );

    /**
     * Retrieves whether inserts made by others are visible.
     * 
     * @return <code>true</code> if inserts made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollInsensitiveResultSetOthersInsertsAreVisible();

    /**
     * Sets whether inserts made by others are visible.
     * 
     * @param scrollInsensitiveResultSetOthersInsertsAreVisible <code>true</code> if inserts made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollInsensitiveResultSetOthersInsertsAreVisible( Boolean scrollInsensitiveResultSetOthersInsertsAreVisible );

    /**
     * Retrieves whether inserts made by others are visible.
     * 
     * @return <code>true</code> if inserts made by others are visible for the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollSensitiveResultSetOthersInsertsAreVisible();

    /**
     * Sets whether inserts made by others are visible.
     * 
     * @param scrollSensitiveResultSetOthersInsertsAreVisible <code>true</code> if inserts made by others are visible for the
     *        given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollSensitiveResultSetOthersInsertsAreVisible( Boolean scrollSensitiveResultSetOthersInsertsAreVisible );

    /**
     * Retrieves whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @return <code>true</code> if changes are detected by the result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isForwardOnlyResultSetUpdatesAreDetected();

    /**
     * Sets whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @param forwardOnlyResultSetUpdatesAreDetected <code>true</code> if changes are detected by the result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setForwardOnlyResultSetUpdatesAreDetected( Boolean forwardOnlyResultSetUpdatesAreDetected );

    /**
     * Retrieves whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @return <code>true</code> if changes are detected by the result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollInsensitiveResultSetUpdatesAreDetected();

    /**
     * Sets whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @param scrollInsensitiveResultSetUpdatesAreDetected <code>true</code> if changes are detected by the result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollInsensitiveResultSetUpdatesAreDetected( Boolean scrollInsensitiveResultSetUpdatesAreDetected );

    /**
     * Retrieves whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @return <code>true</code> if changes are detected by the result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollSensitiveResultSetUpdatesAreDetected();

    /**
     * Sets whether or not a visible row update can be detected by calling the method <code>ResultSet.rowUpdated</code>.
     * 
     * @param scrollSensitiveResultSetUpdatesAreDetected <code>true</code> if changes are detected by the result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollSensitiveResultSetUpdatesAreDetected( Boolean scrollSensitiveResultSetUpdatesAreDetected );

    /**
     * Retrieves whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If
     * the method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the
     * result set.
     * 
     * @return <code>true</code> if deletes are detected by the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isForwardOnlyResultSetDeletesAreDetected();

    /**
     * Sets whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If the
     * method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the result
     * set.
     * 
     * @param forwardOnlyResultSetDeletesAreDetected <code>true</code> if deletes are detected by the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setForwardOnlyResultSetDeletesAreDetected( Boolean forwardOnlyResultSetDeletesAreDetected );

    /**
     * Retrieves whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If
     * the method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the
     * result set.
     * 
     * @return <code>true</code> if deletes are detected by the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollInsensitiveResultSetDeletesAreDetected();

    /**
     * Sets whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If the
     * method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the result
     * set.
     * 
     * @param scrollInsensitiveResultSetDeletesAreDetected <code>true</code> if deletes are detected by the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollInsensitiveResultSetDeletesAreDetected( Boolean scrollInsensitiveResultSetDeletesAreDetected );

    /**
     * Retrieves whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If
     * the method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the
     * result set.
     * 
     * @return <code>true</code> if deletes are detected by the given result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollSensitiveResultSetDeletesAreDetected();

    /**
     * Sets whether or not a visible row delete can be detected by calling the method <code>ResultSet.rowDeleted</code>. If the
     * method <code>deletesAreDetected</code> returns <code>false</code>, it means that deleted rows are removed from the result
     * set.
     * 
     * @param scrollSensitiveResultSetDeletesAreDetected <code>true</code> if deletes are detected by the given result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollSensitiveResultSetDeletesAreDetected( Boolean scrollSensitiveResultSetDeletesAreDetected );

    /**
     * Retrieves whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @return <code>true</code> if changes are detected by the specified result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isForwardOnlyResultInsertsAreDetected();

    /**
     * Sets whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @param forwardOnlyResultInsertsAreDetected <code>true</code> if changes are detected by the specified result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setForwardOnlyResultInsertsAreDetected( Boolean forwardOnlyResultInsertsAreDetected );

    /**
     * Retrieves whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @return <code>true</code> if changes are detected by the specified result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollInsensitiveResultInsertsAreDetected();

    /**
     * Sets whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @param scrollInsensitiveResultInsertsAreDetected <code>true</code> if changes are detected by the specified result set
     *        type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollInsensitiveResultInsertsAreDetected( Boolean scrollInsensitiveResultInsertsAreDetected );

    /**
     * Retrieves whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @return <code>true</code> if changes are detected by the specified result set type; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isScrollSensitiveResultInsertsAreDetected();

    /**
     * Sets whether or not a visible row insert can be detected by calling the method <code>ResultSet.rowInserted</code>.
     * 
     * @param scrollSensitiveResultInsertsAreDetected <code>true</code> if changes are detected by the specified result set type;
     *        <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setScrollSensitiveResultInsertsAreDetected( Boolean scrollSensitiveResultInsertsAreDetected );

    /**
     * Retrieves whether this database supports batch updates.
     * 
     * @return <code>true</code> if this database supports batch upcates; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    Boolean isSupportsBatchUpdates();

    /**
     * Sets whether this database supports batch updates.
     * 
     * @param supportsBatchUpdates <code>true</code> if this database supports batch upcates; <code>false</code> otherwise
     * @since 1.2 (JDBC 2.0)
     */
    void setSupportsBatchUpdates( Boolean supportsBatchUpdates );

    /**
     * Retrieves a description of the UDT available in the given catalog.
     * 
     * @return a set of UDT available
     */
    Set<UserDefinedType> getUserDefinedTypes();

    /**
     * Adds UDT
     * 
     * @param udt the UDT to add
     */
    void addUserDefinedType( UserDefinedType udt );

    /**
     * Deletes UDT
     * 
     * @param udt the UDT to delete
     */
    void deleteUserDefinedType( UserDefinedType udt );

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
    UserDefinedType findUserDefinedTypeByName( String catalog,
                                               String schema,
                                               String tableName );

    // ===============================================================
    // ------------------- JDBC 3.0 ---------------------------------
    // ===============================================================

    /**
     * Retrieves whether this database supports savepoints.
     * 
     * @return <code>true</code> if savepoints are supported; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    Boolean isSupportsSavepoints();

    /**
     * Sets whether this database supports savepoints.
     * 
     * @param supportsSavepoints <code>true</code> if savepoints are supported; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    void setSupportsSavepoints( Boolean supportsSavepoints );

    /**
     * Retrieves whether this database supports named parameters to callable statements.
     * 
     * @return <code>true</code> if named parameters are supported; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    Boolean isSupportsNamedParameters();

    /**
     * Sets whether this database supports named parameters to callable statements.
     * 
     * @param supportsNamedParameters <code>true</code> if named parameters are supported; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    void setSupportsNamedParameters( Boolean supportsNamedParameters );

    /**
     * Retrieves whether it is possible to have multiple <code>ResultSet</code> objects returned from a
     * <code>CallableStatement</code> object simultaneously.
     * 
     * @return <code>true</code> if a <code>CallableStatement</code> object can return multiple <code>ResultSet</code> objects
     *         simultaneously; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    Boolean isSupportsMultipleOpenResults();

    /**
     * Sets whether it is possible to have multiple <code>ResultSet</code> objects returned from a <code>CallableStatement</code>
     * object simultaneously.
     * 
     * @param supportsMultipleOpenResults <code>true</code> if a <code>CallableStatement</code> object can return multiple
     *        <code>ResultSet</code> objects simultaneously; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    void setSupportsMultipleOpenResults( Boolean supportsMultipleOpenResults );

    /**
     * Retrieves whether auto-generated keys can be retrieved after a statement has been executed.
     * 
     * @return <code>true</code> if auto-generated keys can be retrieved after a statement has executed; <code>false</code>
     *         otherwise
     * @since 1.4 (JDBC 3.0)
     */
    Boolean isSupportsGetGeneratedKeys();

    /**
     * Sets whether auto-generated keys can be retrieved after a statement has been executed.
     * 
     * @param supportsGetGeneratedKeys <code>true</code> if auto-generated keys can be retrieved after a statement has executed;
     *        <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    void setSupportsGetGeneratedKeys( Boolean supportsGetGeneratedKeys );

    /**
     * Retrieves whether this database supports the given result set holdability.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @see Connection
     * @since 1.4 (JDBC 3.0)
     */
    Boolean isSupportsResultSetHoldCurrorsOverCommitHoldability();

    /**
     * Sets whether this database supports the given result set holdability.
     * 
     * @param supportsResultSetHoldCurrorsOverCommitHoldability <code>true</code> if so; <code>false</code> otherwise
     * @see Connection
     * @since 1.4 (JDBC 3.0)
     */
    void setSupportsResultSetHoldCurrorsOverCommitHoldability( Boolean supportsResultSetHoldCurrorsOverCommitHoldability );

    /**
     * Retrieves whether this database supports the given result set holdability.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @see Connection
     * @since 1.4 (JDBC 3.0)
     */
    Boolean isSupportsResultSetCloseCurrorsAtCommitHoldability();

    /**
     * Sets whether this database supports the given result set holdability.
     * 
     * @param supportsResultSetCloseCurrorsAtCommitHoldability <code>true</code> if so; <code>false</code> otherwise
     * @see Connection
     * @since 1.4 (JDBC 3.0)
     */
    void setSupportsResultSetCloseCurrorsAtCommitHoldability( Boolean supportsResultSetCloseCurrorsAtCommitHoldability );

    /**
     * Retrieves the default holdability of this <code>ResultSet</code> object.
     * 
     * @return the default holdability; either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @exception SQLException if a database access error occurs
     * @since 1.4
     */
    ResultSetHoldabilityType getResultSetHoldabilityType();

    /**
     * Sets the default holdability of this <code>ResultSet</code> object.
     * 
     * @param resultSetHoldabilityType the ResultSetHoldabilityType
     * @return the default holdability; either <code>ResultSet.HOLD_CURSORS_OVER_COMMIT</code> or
     *         <code>ResultSet.CLOSE_CURSORS_AT_COMMIT</code>
     * @exception SQLException if a database access error occurs
     * @since 1.4
     */
    void setResultSetHoldabilityType( ResultSetHoldabilityType resultSetHoldabilityType );

    /**
     * Retrieves the major version number of the underlying database.
     * 
     * @return the underlying database's major version
     * @since 1.4 (JDBC 3.0)
     */
    Integer getDatabaseMajorVersion();

    /**
     * Sets the major version number of the underlying database.
     * 
     * @param databaseMajorVersion the underlying database's major version
     * @since 1.4 (JDBC 3.0)
     */
    void setDatabaseMajorVersion( Integer databaseMajorVersion );

    /**
     * Retrieves the minor version number of the underlying database.
     * 
     * @return underlying database's minor version
     * @since 1.4 (JDBC 3.0)
     */
    Integer getDatabaseMinorVersion();

    /**
     * Sets the minor version number of the underlying database.
     * 
     * @param databaseMinorVersion underlying database's minor version
     * @since 1.4 (JDBC 3.0)
     */
    void setDatabaseMinorVersion( Integer databaseMinorVersion );

    /**
     * Retrieves the major JDBC version number for this driver.
     * 
     * @return JDBC version major number
     * @since 1.4 (JDBC 3.0)
     */
    Integer getJDBCMajorVersion();

    /**
     * Sets the major JDBC version number for this driver.
     * 
     * @param jdbcMajorVersion JDBC version major number
     * @since 1.4 (JDBC 3.0)
     */
    void setJDBCMajorVersion( Integer jdbcMajorVersion );

    /**
     * Retrieves the minor JDBC version number for this driver.
     * 
     * @return JDBC version minor number
     * @since 1.4 (JDBC 3.0)
     */
    Integer getJDBCMinorVersion();

    /**
     * Sets the minor JDBC version number for this driver.
     * 
     * @param jdbcMinorVersion JDBC version minor number
     * @since 1.4 (JDBC 3.0)
     */
    void setJDBCMinorVersion( Integer jdbcMinorVersion );

    /**
     * Indicates whether the SQLSTATE returned by <code>SQLException.getSQLState</code> is X/Open (now known as Open Group) SQL
     * CLI or SQL99.
     * 
     * @return the type of SQLSTATE; one of: sqlStateXOpen or sqlStateSQL99
     * @since 1.4 (JDBC 3.0)
     */
    SQLStateType getSQLStateType();

    /**
     * Indicates whether the SQLSTATE returned by <code>SQLException.getSQLState</code> is X/Open (now known as Open Group) SQL
     * CLI or SQL99.
     * 
     * @param sqlStateType the type of SQLSTATE; one of: sqlStateXOpen or sqlStateSQL99
     * @since 1.4 (JDBC 3.0)
     */
    void setSQLStateType( SQLStateType sqlStateType );

    /**
     * Indicates whether updates made to a LOB are made on a copy or directly to the LOB.
     * 
     * @return <code>true</code> if updates are made to a copy of the LOB; <code>false</code> if updates are made directly to the
     *         LOB
     * @since 1.4 (JDBC 3.0)
     */
    Boolean isLocatorsUpdateCopy();

    /**
     * Indicates whether updates made to a LOB are made on a copy or directly to the LOB.
     * 
     * @param locatorsUpdateCopy <code>true</code> if updates are made to a copy of the LOB; <code>false</code> if updates are
     *        made directly to the LOB
     * @since 1.4 (JDBC 3.0)
     */
    void setLocatorsUpdateCopy( Boolean locatorsUpdateCopy );

    /**
     * Retrieves whether this database supports statement pooling.
     * 
     * @return <code>true</code> if so; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    Boolean isSupportsStatementPooling();

    /**
     * Sets whether this database supports statement pooling.
     * 
     * @param supportsStatementPooling <code>true</code> if so; <code>false</code> otherwise
     * @since 1.4 (JDBC 3.0)
     */
    void setSupportsStatementPooling( Boolean supportsStatementPooling );

}
