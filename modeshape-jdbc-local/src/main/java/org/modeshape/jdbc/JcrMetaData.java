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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.QueryResult;
import javax.jcr.version.OnParentVersionAction;
import org.modeshape.jdbc.metadata.JDBCColumnNames;
import org.modeshape.jdbc.metadata.JDBCColumnPositions;
import org.modeshape.jdbc.metadata.MetaDataQueryResult;
import org.modeshape.jdbc.metadata.MetadataProvider;
import org.modeshape.jdbc.metadata.ResultSetMetaDataImpl;
import org.modeshape.jdbc.metadata.ResultsMetadataConstants;
import org.modeshape.jdbc.metadata.ResultsMetadataConstants.NULL_TYPES;

/**
 * This driver's implementation of JDBC {@link DatabaseMetaData}.
 */
public class JcrMetaData implements DatabaseMetaData {

    protected static final List<PropertyDefinition> PSEUDO_COLUMN_DEFNS;
    protected static final List<String> PSEUDO_COLUMN_NAMES;

    static {
        List<PropertyDefinition> defns = new ArrayList<PropertyDefinition>();
        List<String> defnNames = new ArrayList<String>();
        boolean auto = true;
        boolean mand = false;
        boolean prot = true;
        boolean mult = false;
        boolean search = true;
        boolean order = true;
        defns.add(new PseudoPropertyDefinition(null, "jcr:path", PropertyType.PATH, auto, mand, prot, mult, search, order));
        defnNames.add("jcr:path");
        defns.add(new PseudoPropertyDefinition(null, "jcr:name", PropertyType.NAME, auto, mand, prot, mult, search, order));
        defnNames.add("jcr:name");
        defns.add(new PseudoPropertyDefinition(null, "jcr:score", PropertyType.DOUBLE, auto, mand, prot, mult, search, order));
        defnNames.add("jcr:score");
        defns.add(new PseudoPropertyDefinition(null, "mode:localName", PropertyType.STRING, auto, mand, prot, mult, search, order));
        defnNames.add("mode:localName");
        defns.add(new PseudoPropertyDefinition(null, "mode:depth", PropertyType.LONG, auto, mand, prot, mult, search, order));
        defnNames.add("mode:depth");
        defns.add(new PseudoPropertyDefinition(null, "mode:id", PropertyType.STRING, auto, mand, prot, mult, search, order));
        defnNames.add("mode:id");

        PSEUDO_COLUMN_DEFNS = Collections.unmodifiableList(defns);
        PSEUDO_COLUMN_NAMES = Collections.unmodifiableList(defnNames);

    }

    /** CONSTANTS */
    protected static final String WILDCARD = "%"; //$NON-NLS-1$
    protected static final Integer DEFAULT_ZERO = 0;
    protected static final int NO_LIMIT = 0;

    private JcrConnection connection;
    private String catalogName;

    public JcrMetaData( JcrConnection connection ) {
        this.connection = connection;
        assert this.connection != null;
        catalogName = connection.getCatalog();
        assert catalogName != null;
    }

    /**
     * This method always returns an emtpy result set. *
     * <p>
     * <em>Note:</em> This method is part of the JDBC API in JDK 1.7.
     * </p>
     * 
     * @param catalog
     * @param schemaPattern
     * @param tableNamePattern
     * @param columnNamePattern
     * @return the pseudo columns
     * @throws SQLException
     */
    @Override
    public ResultSet getPseudoColumns( String catalog,
                                       String schemaPattern,
                                       String tableNamePattern,
                                       String columnNamePattern ) throws SQLException {
        return new JcrResultSet();
    }

    /**
     * This method always returns true. *
     * <p>
     * <em>Note:</em> This method is part of the JDBC API in JDK 1.7.
     * </p>
     * 
     * @return true
     * @throws SQLException
     */
    @Override
    public boolean generatedKeyAlwaysReturned() throws SQLException {
        return true;
    }

    @Override
    public int getDriverMajorVersion() {
        return connection.driverInfo().getMajorVersion();
    }

    @Override
    public int getDriverMinorVersion() {
        return connection.driverInfo().getMinorVersion();
    }

    @Override
    public String getDriverName() {
        return connection.driverInfo().getName();
    }

    @Override
    public String getDriverVersion() {
        return connection.driverInfo().getVersion();
    }

    @Override
    public int getDatabaseMajorVersion() {
        String[] parts = getDatabaseProductVersion().split("[.-]");
        return parts.length > 0 && parts[0] != null ? Integer.parseInt(parts[0]) : 0;
    }

    @Override
    public int getDatabaseMinorVersion() {
        String[] parts = getDatabaseProductVersion().split("[.-]");
        return parts.length > 1 && parts[1] != null ? Integer.parseInt(parts[1]) : 0;
    }

    @Override
    public String getDatabaseProductName() {
        return this.connection.getRepositoryDelegate().getDescriptor(Repository.REP_NAME_DESC);
    }

    public String getDatabaseProductUrl() {
        return this.connection.getRepositoryDelegate().getDescriptor(Repository.REP_VENDOR_URL_DESC);
    }

    @Override
    public String getDatabaseProductVersion() {
        return this.connection.getRepositoryDelegate().getDescriptor(Repository.REP_VERSION_DESC);
    }

    @Override
    public int getJDBCMajorVersion() {
        return 2;
    }

    @Override
    public int getJDBCMinorVersion() {
        return 0;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean allProceduresAreCallable() {
        return false;
    }

    @Override
    public boolean allTablesAreSelectable() {
        return false;
    }

    @Override
    public boolean autoCommitFailureClosesAllResultSets() {
        return false;
    }

    @Override
    public boolean dataDefinitionCausesTransactionCommit() {
        return false;
    }

    @Override
    public boolean dataDefinitionIgnoredInTransactions() {
        return false;
    }

    @Override
    public boolean deletesAreDetected( int type ) {
        return false;
    }

    @Override
    public boolean doesMaxRowSizeIncludeBlobs() {
        return false;
    }

    @Override
    public ResultSet getAttributes( String catalog,
                                    String schemaPattern,
                                    String typeNamePattern,
                                    String attributeNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public ResultSet getBestRowIdentifier( String catalog,
                                           String schema,
                                           String table,
                                           int scope,
                                           boolean nullable ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String getCatalogSeparator() {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This driver maps the repository name as the JDBC catalog name. Therefore, this method returns 'Repository' for the catalog
     * term.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getCatalogTerm()
     */
    @Override
    public String getCatalogTerm() {
        return "Repository";
    }

    /**
     * {@inheritDoc}
     * <p>
     * This driver maps the repository name as the JDBC catalog name. Therefore, this method returns a result set containing only
     * the repository's name.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getCatalogs()
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getCatalogs() throws SQLException {
        List<List<?>> records = new ArrayList<List<?>>(1);

        List<String> row = Arrays.asList(catalogName);
        records.add(row);

        /***********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         ***********************************************************************/

        Map<?, Object>[] metadataList = new Map[1];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.CATALOGS.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        JcrStatement stmt = new JcrStatement(this.connection);
        QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);

        return new JcrResultSet(stmt, queryresult, resultSetMetaData);
    }

    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public ResultSet getColumnPrivileges( String catalog,
                                          String schema,
                                          String table,
                                          String columnNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getColumns( String catalog,
                                 String schemaPattern,
                                 String tableNamePattern,
                                 String columnNamePattern ) throws SQLException {
        LocalJcrDriver.logger.debug("getcolumns: " + catalog + ":" + schemaPattern + ":" + tableNamePattern + ":"
                                    + columnNamePattern);

        // Get all tables if tableNamePattern is null
        if (tableNamePattern == null) tableNamePattern = WILDCARD;

        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.COLUMNS.MAX_COLUMNS];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.COLUMN_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.DATA_TYPE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.TYPE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[6] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.COLUMN_SIZE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[7] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.BUFFER_LENGTH,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[8] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.DECIMAL_DIGITS,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[9] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.NUM_PREC_RADIX,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);

        metadataList[10] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.NULLABLE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[11] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.REMARKS,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[12] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.COLUMN_DEF,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[13] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.SQL_DATA_TYPE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[14] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.SQL_DATETIME_SUB,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[15] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.CHAR_OCTET_LENGTH,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[16] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.ORDINAL_POSITION,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[17] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.IS_NULLABLE,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[18] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.SCOPE_CATLOG,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[19] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.SCOPE_SCHEMA,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);

        metadataList[20] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.SCOPE_TABLE,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[21] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.SOURCE_DATA_TYPE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        List<List<?>> records = new ArrayList<List<?>>();

        try {

            List<NodeType> nodetypes = filterNodeTypes(tableNamePattern);

            // process each node
            for (NodeType type : nodetypes) {

                if (type.getPropertyDefinitions() == null) {
                    throw new SQLException("Program Error:  missing propertydefintions for " + type.getName());
                }

                List<PropertyDefinition> defns = filterPropertyDefnitions(columnNamePattern, type);

                int ordinal = 0;
                // build the list of records.
                for (PropertyDefinition propDefn : defns) {

                    JcrType jcrtype = JcrType.typeInfo(propDefn.getRequiredType());

                    Integer nullable = propDefn.isMandatory() ? ResultsMetadataConstants.NULL_TYPES.NOT_NULL : ResultsMetadataConstants.NULL_TYPES.NULLABLE;

                    List<Object> currentRow = loadCurrentRow(type.getName(), propDefn.getName(), jcrtype, nullable,
                                                             propDefn.isMandatory(), ordinal);

                    // add the current row to the list of records.
                    records.add(currentRow);

                    ++ordinal;
                }
                // if columns where added and if Teiid Support is requested, then add the mode:properties to the list of columns
                if (ordinal > 0 && this.connection.getRepositoryDelegate().getConnectionInfo().isTeiidSupport()) {
                    if (this.connection.getRepositoryDelegate().getConnectionInfo().isTeiidSupport()) {
                        List<Object> currentRow = loadCurrentRow(type.getName(), "mode:properties",
                                                                 JcrType.typeInfo(PropertyType.STRING),
                                                                 ResultsMetadataConstants.NULL_TYPES.NULLABLE, false, ordinal);

                        records.add(currentRow);
                    }
                }

            }

            JcrStatement jcrstmt = new JcrStatement(this.connection);
            QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
            return new JcrResultSet(jcrstmt, queryresult, resultSetMetaData);

        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    private List<Object> loadCurrentRow( String tableName,
                                         String columnName,
                                         JcrType jcrtype,
                                         Integer nullable,
                                         boolean isMandatory,
                                         int ordinal ) {
        // list represents a record on the Results object.
        List<Object> currentRow = new ArrayList<Object>(JDBCColumnPositions.COLUMNS.MAX_COLUMNS);

        currentRow.add(catalogName); // TABLE_CAT
        currentRow.add("NULL"); // TABLE_SCHEM
        currentRow.add(tableName); // TABLE_NAME
        currentRow.add(columnName); // COLUMN_NAME
        currentRow.add(jcrtype.getJdbcType()); // DATA_TYPE
        currentRow.add(jcrtype.getJdbcTypeName()); // TYPE_NAME
        currentRow.add(jcrtype.getNominalDisplaySize()); // COLUMN_SIZE
        currentRow.add("NULL"); // BUFFER_LENGTH
        currentRow.add(JcrMetaData.DEFAULT_ZERO); // DECIMAL_DIGITS
        currentRow.add(JcrMetaData.DEFAULT_ZERO); // NUM_PREC_RADIX

        currentRow.add(nullable); // NULLABLE
        currentRow.add(""); // REMARKS
        currentRow.add("NULL"); // COLUMN_DEF
        currentRow.add(JcrMetaData.DEFAULT_ZERO); // COLUMN_DEF
        currentRow.add(JcrMetaData.DEFAULT_ZERO); // SQL_DATETIME_SUB

        currentRow.add(JcrMetaData.DEFAULT_ZERO); // CHAR_OCTET_LENGTH
        currentRow.add(ordinal + 1); // ORDINAL_POSITION
        currentRow.add(isMandatory ? "NO" : "YES"); // IS_NULLABLE
        currentRow.add("NULL"); // SCOPE_CATLOG
        currentRow.add("NULL"); // SCOPE_SCHEMA

        currentRow.add("NULL"); // SCOPE_TABLE
        currentRow.add(JcrMetaData.DEFAULT_ZERO); // SOURCE_DATA_TYPE

        return currentRow;
    }

    @Override
    public ResultSet getCrossReference( String parentCatalog,
                                        String parentSchema,
                                        String parentTable,
                                        String foreignCatalog,
                                        String foreignSchema,
                                        String foreignTable ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getDefaultTransactionIsolation() {
        return Connection.TRANSACTION_NONE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This driver maps REFERENCE properties as keys, and therefore it represents as imported keys those REFERENCE properties on
     * the type identified by the table name.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getExportedKeys(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getExportedKeys( String catalog,
                                      String schema,
                                      String table ) throws SQLException {
        return getImportedKeys(catalog, schema, table); // empty, but same resultsetmetadata
    }

    @Override
    public String getExtraNameCharacters() {
        return null;
    }

    @Override
    public ResultSet getFunctionColumns( String catalog,
                                         String schemaPattern,
                                         String functionNamePattern,
                                         String columnNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public ResultSet getFunctions( String catalog,
                                   String schemaPattern,
                                   String functionNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * JCR-SQL2 allows identifiers to be surrounded by matching single quotes, double quotes, or opening and closing square
     * brackets. Therefore, this method returns a single-quote character as the quote string.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getIdentifierQuoteString()
     */
    @Override
    public String getIdentifierQuoteString() {
        return "\"";
    }

    /**
     * {@inheritDoc}
     * <p>
     * This driver maps REFERENCE properties as keys, and therefore it represents as imported keys those properties on other types
     * referencing the type identified by the table name.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getImportedKeys(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getImportedKeys( String catalog,
                                      String schema,
                                      String table ) throws SQLException {
        @SuppressWarnings( "unchecked" )
        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.REFERENCE_KEYS.MAX_COLUMNS];
        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.REFERENCE_KEYS_INFO.PK_TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName, null,
                                                             JDBCColumnNames.REFERENCE_KEYS_INFO.PK_TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName, null,
                                                             JDBCColumnNames.REFERENCE_KEYS_INFO.PK_TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName, null,
                                                             JDBCColumnNames.REFERENCE_KEYS_INFO.PK_COLUMN_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.REFERENCE_KEYS_INFO.FK_TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName, null,
                                                             JDBCColumnNames.REFERENCE_KEYS_INFO.FK_TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[6] = MetadataProvider.getColumnMetadata(catalogName, null,
                                                             JDBCColumnNames.REFERENCE_KEYS_INFO.FK_TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[7] = MetadataProvider.getColumnMetadata(catalogName, null,
                                                             JDBCColumnNames.REFERENCE_KEYS_INFO.FK_COLUMN_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[8] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.REFERENCE_KEYS_INFO.KEY_SEQ,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[9] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.REFERENCE_KEYS_INFO.UPDATE_RULE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[10] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.REFERENCE_KEYS_INFO.DELETE_RULE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[11] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.REFERENCE_KEYS_INFO.FK_NAME,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[12] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.REFERENCE_KEYS_INFO.PK_NAME,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[13] = MetadataProvider.getColumnMetadata(catalogName, null,
                                                              JDBCColumnNames.REFERENCE_KEYS_INFO.DEFERRABILITY,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        JcrStatement jcrstmt = new JcrStatement(this.connection);
        MetadataProvider provider = new MetadataProvider(metadataList);
        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);
        List<List<?>> records = Collections.emptyList();
        QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
        return new JcrResultSet(jcrstmt, queryresult, resultSetMetaData);
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getIndexInfo( String catalog,
                                   String schema,
                                   String tableNamePattern,
                                   boolean unique,
                                   boolean approximate ) throws SQLException {

        // Get index information for all tables if tableNamePattern is null
        if (tableNamePattern == null) tableNamePattern = WILDCARD;

        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.INDEX_INFO.MAX_COLUMNS];
        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.NON_UNIQUE,
                                                             JcrType.DefaultDataTypes.BOOLEAN,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.INDEX_QUALIFIER,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.INDEX_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[6] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.TYPE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[7] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.ORDINAL_POSITION,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[8] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.COLUMN_NAME,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[9] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.ASC_OR_DESC,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);

        metadataList[10] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.CARDINALITY,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[11] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.PAGES,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[12] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.INDEX_INFO.FILTER_CONDITION,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);

        try {
            Boolean nonUnique = Boolean.FALSE;
            List<List<?>> records = new ArrayList<List<?>>();
            for (NodeType type : filterNodeTypes(tableNamePattern)) {
                // Create a unique key for each "jcr:uuid" property, so do this only for those node types
                // that somehow extend "mix:referenceable" ...
                if (!type.isNodeType("mix:referenceable")) continue;

                // Every table has a "jcr:path" pseudo-column that is the primary key ...
                List<Object> currentRow = new ArrayList<Object>(JDBCColumnPositions.INDEX_INFO.MAX_COLUMNS);
                currentRow.add(catalogName); // TABLE_CAT
                currentRow.add("NULL"); // TABLE_SCHEM
                currentRow.add(type.getName()); // TABLE_NAME
                currentRow.add(nonUnique); // NON_UNIQUE
                currentRow.add(catalogName); // INDEX_QUALIFIER
                currentRow.add(type.getName() + "_UK"); // INDEX_NAME
                currentRow.add(DatabaseMetaData.tableIndexHashed); // TYPE
                currentRow.add((short)1); // ORDINAL_POSITION
                currentRow.add(type.getName()); // COLUMN_NAME
                currentRow.add("A"); // ASC_OR_DESC
                currentRow.add(0); // CARDINALITY
                currentRow.add(1); // PAGES
                currentRow.add(null); // FILTER_CONDITION

                // add the current row to the list of records.
                records.add(currentRow);
            }

            JcrStatement jcrstmt = new JcrStatement(this.connection);
            MetadataProvider provider = new MetadataProvider(metadataList);
            ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);
            QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
            return new JcrResultSet(jcrstmt, queryresult, resultSetMetaData);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no maximum length of binary literals (or if there is a limit it is not known), so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxBinaryLiteralLength()
     */
    @Override
    public int getMaxBinaryLiteralLength() {
        return JcrMetaData.NO_LIMIT; // no limit
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no maximum length of the catalog (repository) names - or the limit is not known.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxCatalogNameLength()
     */
    @Override
    public int getMaxCatalogNameLength() {
        return JcrMetaData.NO_LIMIT; // none
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no maximum length of character literals (or if there is a limit it is not known), so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxCharLiteralLength()
     */
    @Override
    public int getMaxCharLiteralLength() {
        return JcrMetaData.NO_LIMIT;
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no maximum length of column names (or if there is a limit it is not known), so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxColumnNameLength()
     */
    @Override
    public int getMaxColumnNameLength() {
        return JcrMetaData.NO_LIMIT; // no limit
    }

    /**
     * {@inheritDoc}
     * <p>
     * JCR-SQL2 does not support GROUP BY, so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxColumnsInGroupBy()
     */
    @Override
    public int getMaxColumnsInGroupBy() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no limit to the number of columns in an index (or if there is a limit it is not known), so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxColumnsInIndex()
     */
    @Override
    public int getMaxColumnsInIndex() {
        return JcrMetaData.NO_LIMIT; // no limit
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no limit to the number of columns in an ORDER BY statement (or if there is a limit it is not known), so this
     * method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxColumnsInOrderBy()
     */
    @Override
    public int getMaxColumnsInOrderBy() {
        return JcrMetaData.NO_LIMIT; // not known (technically there is no limit, but there would be a practical limit)
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no limit to the number of columns in a select statement (or if there is a limit it is not known), so this method
     * returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxColumnsInSelect()
     */
    @Override
    public int getMaxColumnsInSelect() {
        return JcrMetaData.NO_LIMIT; // no limit
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no limit to the number of columns in a table (or if there is a limit it is not known), so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxColumnsInTable()
     */
    @Override
    public int getMaxColumnsInTable() {
        return JcrMetaData.NO_LIMIT; // no limit
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no limit to the number of connections (or if there is a limit it is not known), so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxConnections()
     */
    @Override
    public int getMaxConnections() {
        return JcrMetaData.NO_LIMIT; // no limit
    }

    /**
     * {@inheritDoc}
     * <p>
     * There are no cursors (or there is no limit), so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxCursorNameLength()
     */
    @Override
    public int getMaxCursorNameLength() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * There are no indexes (or there is no limit), so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxIndexLength()
     */
    @Override
    public int getMaxIndexLength() {
        return 0; // no limit
    }

    /**
     * {@inheritDoc}
     * <p>
     * There are no procedures, so this method returns 0.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxProcedureNameLength()
     */
    @Override
    public int getMaxProcedureNameLength() {
        return 0; // no limit
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no maximum row size.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxRowSize()
     */
    @Override
    public int getMaxRowSize() {
        return JcrMetaData.NO_LIMIT; // no limit
    }

    /**
     * {@inheritDoc}
     * <p>
     * There is no maximum length of the schema (workspace) names - or the limit is not known.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getMaxSchemaNameLength()
     */
    @Override
    public int getMaxSchemaNameLength() {
        return JcrMetaData.NO_LIMIT; // none
    }

    @Override
    public int getMaxStatementLength() {
        return 0; // no limit
    }

    @Override
    public int getMaxStatements() {
        return 0; // no limit
    }

    @Override
    public int getMaxTableNameLength() {
        return 0; // no limit
    }

    @Override
    public int getMaxTablesInSelect() {
        return 0; // not known (technically there is no limit, but there would be a practical limit)
    }

    @Override
    public int getMaxUserNameLength() {
        return 0; // no limit
    }

    @Override
    public String getNumericFunctions() {
        return null;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getPrimaryKeys( String catalog,
                                     String schema,
                                     String tableNamePattern ) throws SQLException {
        // Get primary keys for all tables if tableNamePattern is null
        if (tableNamePattern == null) tableNamePattern = WILDCARD;

        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.PRIMARY_KEYS.MAX_COLUMNS];
        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PRIMARY_KEYS.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PRIMARY_KEYS.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PRIMARY_KEYS.TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PRIMARY_KEYS.COLUMN_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PRIMARY_KEYS.KEY_SEQ,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PRIMARY_KEYS.PK_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);

        try {
            List<List<?>> records = new ArrayList<List<?>>();
            for (NodeType type : filterNodeTypes(tableNamePattern)) {
                // Every table has a "jcr:path" pseudo-column that is the primary key ...
                List<Object> currentRow = new ArrayList<Object>(JDBCColumnPositions.PRIMARY_KEYS.MAX_COLUMNS);
                currentRow.add(catalogName); // TABLE_CAT
                currentRow.add("NULL"); // TABLE_SCHEM
                currentRow.add(type.getName()); // TABLE_NAME
                currentRow.add("jcr:path"); // COLUMN_NAME
                currentRow.add(1); // KEY_SEQ
                currentRow.add(type.getName() + "_PK"); // PK_NAME

                // add the current row to the list of records.
                records.add(currentRow);
            }

            JcrStatement jcrstmt = new JcrStatement(this.connection);
            MetadataProvider provider = new MetadataProvider(metadataList);
            ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);
            QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
            return new JcrResultSet(jcrstmt, queryresult, resultSetMetaData);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    @Override
    public ResultSet getProcedureColumns( String catalog,
                                          String schemaPattern,
                                          String procedureNamePattern,
                                          String columnNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String getProcedureTerm() {
        return null;
    }

    @Override
    public ResultSet getProcedures( String catalog,
                                    String schemaPattern,
                                    String procedureNamePattern ) throws SQLException {
        @SuppressWarnings( "unchecked" )
        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.PROCEDURES.MAX_COLUMNS];
        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PROCEDURES.PROCEDURE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PROCEDURES.PROCEDURE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PROCEDURES.PROCEDURE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PROCEDURES.RESERVED1,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PROCEDURES.RESERVED2,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PROCEDURES.RESERVED3,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[6] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PROCEDURES.REMARKS,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[7] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PROCEDURES.PROCEDURE_TYPE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[8] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.PROCEDURES.SPECIFIC_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);

        JcrStatement jcrstmt = new JcrStatement(this.connection);
        MetadataProvider provider = new MetadataProvider(metadataList);
        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);
        List<List<?>> records = Collections.emptyList();
        QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
        return new JcrResultSet(jcrstmt, queryresult, resultSetMetaData);
    }

    @Override
    public int getResultSetHoldability() {
        return 0;
    }

    @Override
    public RowIdLifetime getRowIdLifetime() {
        return RowIdLifetime.ROWID_UNSUPPORTED;
    }

    @Override
    public String getSQLKeywords() {
        return null;
    }

    @Override
    public int getSQLStateType() {
        return 0;
    }

    @Override
    public String getSchemaTerm() {
        return " ";
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getSchemas() throws SQLException {
        List<List<?>> records = new ArrayList<List<?>>(1);

        /***********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         ***********************************************************************/

        Map<?, Object>[] metadataList = new Map[1];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.COLUMNS.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        JcrStatement stmt = new JcrStatement(this.connection);
        QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
        ResultSet rs = new JcrResultSet(stmt, queryresult, resultSetMetaData);

        return rs;
    }

    @Override
    public ResultSet getSchemas( String catalog,
                                 String schemaPattern ) throws SQLException {
        return getSchemas();
    }

    @Override
    public String getSearchStringEscape() {
        return null;
    }

    @Override
    public String getStringFunctions() {
        return null;
    }

    @Override
    public ResultSet getSuperTables( String catalog,
                                     String schemaPattern,
                                     String tableNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public ResultSet getSuperTypes( String catalog,
                                    String schemaPattern,
                                    String typeNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String getSystemFunctions() {
        return null;
    }

    @Override
    public ResultSet getTablePrivileges( String catalog,
                                         String schemaPattern,
                                         String tableNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getTableTypes() throws SQLException {

        List<List<?>> records = new ArrayList<List<?>>(1);
        List<String> row = Arrays.asList(ResultsMetadataConstants.TABLE_TYPES.VIEW);
        records.add(row);

        /***********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         ***********************************************************************/

        Map<?, Object>[] metadataList = new Map[1];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLE_TYPES.TABLE_TYPE,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        JcrStatement stmt = new JcrStatement(this.connection);
        QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
        return new JcrResultSet(stmt, queryresult, resultSetMetaData);
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getTables( String catalog,
                                String schemaPattern,
                                String tableNamePattern,
                                String[] types ) throws SQLException {

        LocalJcrDriver.logger.debug("getTables: " + catalog + ":" + schemaPattern + ":" + tableNamePattern + ":" + types);

        // Get all tables if tableNamePattern is null
        if (tableNamePattern == null) {
            tableNamePattern = WILDCARD;
        }

        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.TABLES.MAX_COLUMNS];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.TABLE_TYPE,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.REMARKS,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.TYPE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[6] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.TYPE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[7] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.TYPE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[8] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.SELF_REFERENCING_COL_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[9] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TABLES.REF_GENERATION,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        List<List<?>> records = new ArrayList<List<?>>();

        try {
            List<NodeType> nodetypes = filterNodeTypes(tableNamePattern);

            // build the list of records from the nodetypes.
            for (NodeType type : nodetypes) {

                if (!type.isQueryable()) {
                    continue;
                }

                // list represents a record on the Results object.
                List<Object> currentRow = new ArrayList<Object>(JDBCColumnPositions.TABLES.MAX_COLUMNS);
                // add values in the current record on the Results object to the list
                // number of values to be fetched from each row is MAX_COLUMNS.

                currentRow.add(catalogName); // TABLE_CAT
                currentRow.add("NULL"); // TABLE_SCHEM
                currentRow.add(type.getName()); // TABLE_NAME
                currentRow.add(ResultsMetadataConstants.TABLE_TYPES.VIEW); // TABLE_TYPE
                currentRow.add("Is Mixin: " + type.isMixin()); // REMARKS
                currentRow.add("NULL"); // TYPE_CAT
                currentRow.add("NULL"); // TYPE_SCHEM
                currentRow.add("NULL"); // TYPE_NAME
                currentRow.add(type.getPrimaryItemName()); // SELF_REF
                currentRow.add("DERIVED"); // REF_GEN

                // add the current row to the list of records.
                records.add(currentRow);
            }// end of while

            JcrStatement jcrstmt = new JcrStatement(this.connection);
            QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);

            return new JcrResultSet(jcrstmt, queryresult, resultSetMetaData);

        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }

    @Override
    public String getTimeDateFunctions() {
        return null;
    }

    private static List<List<?>> typeInfoRows() {
        List<List<?>> rows = new ArrayList<List<?>>();
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.STRING, Types.VARCHAR, Integer.MAX_VALUE, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.BINARY, Types.BLOB, Integer.MAX_VALUE, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.BOOLEAN, Types.BOOLEAN, 1, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.DATE, Types.TIMESTAMP, Integer.MAX_VALUE, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.DECIMAL, Types.DECIMAL, Integer.MAX_VALUE, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.DOUBLE, Types.DOUBLE, 18, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.LONG, Types.BIGINT, 18, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.NAME, Types.VARCHAR, Integer.MAX_VALUE, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.PATH, Types.VARCHAR, Integer.MAX_VALUE, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.REFERENCE, Types.VARCHAR, Integer.MAX_VALUE, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.WEAK_REF, Types.VARCHAR, Integer.MAX_VALUE, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        rows.add(typeInfoRow(JcrType.DefaultDataTypes.URI, Types.VARCHAR, Integer.MAX_VALUE, NULL_TYPES.NULLABLE, true,
                             ResultsMetadataConstants.SEARCH_TYPES.SEARCHABLE, false, false, 0, 0));
        return rows;
    }

    private static List<?> typeInfoRow( String typeName,
                                        int sqlType,
                                        int precision,
                                        Integer nullability,
                                        boolean caseSensitive,
                                        Integer searchable,
                                        boolean isUnsigned,
                                        boolean canBeAutoIncremented,
                                        int minimumScale,
                                        int maximumScale ) {
        List<Object> row = new ArrayList<Object>(JDBCColumnPositions.TYPE_INFO.MAX_COLUMNS);
        row.add(typeName);
        row.add(sqlType);
        row.add(precision);
        row.add(null); // literal prefix
        row.add(null); // literal suffix
        row.add(null); // create params
        row.add(nullability);
        row.add(caseSensitive);
        row.add(searchable);
        row.add(isUnsigned);
        row.add(false); // is money
        row.add(canBeAutoIncremented);
        row.add(null); // localized type name
        row.add(minimumScale);
        row.add(maximumScale);
        row.add(0); // unused
        row.add(0); // unused
        row.add(10); // radix
        return row;
    }

    @Override
    public ResultSet getTypeInfo() throws SQLException {
        @SuppressWarnings( "unchecked" )
        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.TYPE_INFO.MAX_COLUMNS];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.TYPE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.DATA_TYPE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.PRECISION,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.LITERAL_PREFIX,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.LITERAL_SUFFIX,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.CREATE_PARAMS,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[6] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.NULLABLE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[7] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.CASE_SENSITIVE,
                                                             JcrType.DefaultDataTypes.BOOLEAN,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[8] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.SEARCHABLE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[9] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.UNSIGNED_ATTRIBUTE,
                                                             JcrType.DefaultDataTypes.BOOLEAN,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[10] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.FIXED_PREC_SCALE,
                                                              JcrType.DefaultDataTypes.BOOLEAN,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[11] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.AUTOINCREMENT,
                                                              JcrType.DefaultDataTypes.BOOLEAN,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[12] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.LOCAL_TYPE_NAME,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE, this.connection);
        metadataList[13] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.MINIMUM_SCALE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[14] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.MAXIMUM_SCALE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[15] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.SQL_DATA_TYPE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[16] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.SQL_DATETIME_SUB,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);
        metadataList[17] = MetadataProvider.getColumnMetadata(catalogName, null, JDBCColumnNames.TYPE_INFO.NUM_PREC_RADIX,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL, this.connection);

        // Build the result set metadata ...
        MetadataProvider provider = new MetadataProvider(metadataList);
        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);
        // The rows ...
        List<List<?>> records = typeInfoRows();
        // And the result set ...
        JcrStatement jcrstmt = new JcrStatement(this.connection);
        QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
        return new JcrResultSet(jcrstmt, queryresult, resultSetMetaData);
    }

    @Override
    public ResultSet getUDTs( String catalog,
                              String schemaPattern,
                              String typeNamePattern,
                              int[] types ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the effective URL of the connection, which includes all connection properties (even if those properties
     * were passed in via the Properties argument). Note that each character in the password is replaced with a '*' character.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#getURL()
     */
    @Override
    public String getURL() {
        return connection.info().getEffectiveUrl();
    }

    @Override
    public String getUserName() {
        return connection.info().getUsername();
    }

    @Override
    public ResultSet getVersionColumns( String catalog,
                                        String schema,
                                        String table ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean insertsAreDetected( int type ) {
        return false; // read-only
    }

    @Override
    public boolean isCatalogAtStart() {
        return true;
    }

    @Override
    public boolean locatorsUpdateCopy() {
        return false; // read-only
    }

    @Override
    public boolean nullPlusNonNullIsNull() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Assumed to be false for JCR implementations (meaning that sort order IS used), though section 6.7.37 of JCR 2.0
     * specification says ordering of null values is implementation-determined.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#nullsAreSortedAtEnd()
     */
    @Override
    public boolean nullsAreSortedAtEnd() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Assumed to be false for JCR implementations (meaning that sort order IS used), though section 6.7.37 of JCR 2.0
     * specification says ordering of null values is implementation-determined.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#nullsAreSortedAtStart()
     */
    @Override
    public boolean nullsAreSortedAtStart() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Assumed to be false for JCR implementations, though section 6.7.37 of JCR 2.0 specification says ordering of null values is
     * implementation-determined.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#nullsAreSortedHigh()
     */
    @Override
    public boolean nullsAreSortedHigh() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Assumed to be true for JCR implementations, though section 6.7.37 of JCR 2.0 specification says ordering of null values is
     * implementation-determined.
     * </p>
     * 
     * @see java.sql.DatabaseMetaData#nullsAreSortedLow()
     */
    @Override
    public boolean nullsAreSortedLow() {
        return true;
    }

    @Override
    public boolean othersDeletesAreVisible( int type ) {
        return false; // read-only
    }

    @Override
    public boolean othersInsertsAreVisible( int type ) {
        return false; // read-only
    }

    @Override
    public boolean othersUpdatesAreVisible( int type ) {
        return false; // read-only
    }

    @Override
    public boolean ownDeletesAreVisible( int type ) {
        return false; // read-only
    }

    @Override
    public boolean ownInsertsAreVisible( int type ) {
        return false; // read-only
    }

    @Override
    public boolean ownUpdatesAreVisible( int type ) {
        return false; // read-only
    }

    @Override
    public boolean storesLowerCaseIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    @Override
    public boolean storesLowerCaseQuotedIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    @Override
    public boolean storesMixedCaseIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    @Override
    public boolean storesMixedCaseQuotedIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    @Override
    public boolean storesUpperCaseIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    @Override
    public boolean storesUpperCaseQuotedIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    @Override
    public boolean supportsANSI92EntryLevelSQL() {
        return false; // JCR-SQL2 is not entry-level ANSI92 SQL
    }

    @Override
    public boolean supportsANSI92FullSQL() {
        return false; // JCR-SQL2 is not full ANSI92 SQL
    }

    @Override
    public boolean supportsANSI92IntermediateSQL() {
        return false; // JCR-SQL2 is not intermediate ANSI92 SQL
    }

    @Override
    public boolean supportsAlterTableWithAddColumn() {
        // Not in JCR-SQL2
        return false;
    }

    @Override
    public boolean supportsAlterTableWithDropColumn() {
        // Not in JCR-SQL2
        return false;
    }

    @Override
    public boolean supportsBatchUpdates() {
        // Not in JCR-SQL2
        return false;
    }

    @Override
    public boolean supportsCatalogsInDataManipulation() {
        // Not in JCR-SQL2
        return false;
    }

    @Override
    public boolean supportsCatalogsInIndexDefinitions() {
        // No such thing in JCR-SQL2
        return false;
    }

    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() {
        // No defining privileges in JCR 1.0 or 2.0 or JCR-SQL2
        return false;
    }

    @Override
    public boolean supportsCatalogsInProcedureCalls() {
        // No such thing in JCR-SQL2
        return false;
    }

    @Override
    public boolean supportsCatalogsInTableDefinitions() {
        // No defining tables in JCR 1.0 or 2.0 or JCR-SQL2
        return false;
    }

    @Override
    public boolean supportsColumnAliasing() {
        // JCR-SQL2 does support aliases on column names (section 6.7.39)
        return false;
    }

    @Override
    public boolean supportsConvert() {
        return false;
    }

    @Override
    public boolean supportsConvert( int fromType,
                                    int toType ) {
        return false;
    }

    @Override
    public boolean supportsCoreSQLGrammar() {
        return false;
    }

    @Override
    public boolean supportsCorrelatedSubqueries() {
        return false;
    }

    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() {
        return false;
    }

    @Override
    public boolean supportsDataManipulationTransactionsOnly() {
        return false;
    }

    @Override
    public boolean supportsDifferentTableCorrelationNames() {
        return false;
    }

    @Override
    public boolean supportsExpressionsInOrderBy() {
        return false;
    }

    @Override
    public boolean supportsExtendedSQLGrammar() {
        return false;
    }

    @Override
    public boolean supportsFullOuterJoins() {
        // JCR-SQL2 does not support FULL OUTER JOIN ...
        return false;
    }

    @Override
    public boolean supportsGetGeneratedKeys() {
        return false;
    }

    @Override
    public boolean supportsGroupBy() {
        return false; // not in JCR-SQL2;
    }

    @Override
    public boolean supportsGroupByBeyondSelect() {
        return false; // not in JCR-SQL2;
    }

    @Override
    public boolean supportsGroupByUnrelated() {
        return false; // not in JCR-SQL2;
    }

    @Override
    public boolean supportsIntegrityEnhancementFacility() {
        return false;
    }

    @Override
    public boolean supportsLikeEscapeClause() {
        return false;
    }

    @Override
    public boolean supportsLimitedOuterJoins() {
        return false;
    }

    @Override
    public boolean supportsMinimumSQLGrammar() {
        return false;
    }

    @Override
    public boolean supportsMixedCaseIdentifiers() {
        return false;
    }

    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() {
        return false;
    }

    @Override
    public boolean supportsMultipleOpenResults() {
        return false;
    }

    @Override
    public boolean supportsMultipleResultSets() {
        return false;
    }

    @Override
    public boolean supportsMultipleTransactions() {
        return false;
    }

    @Override
    public boolean supportsNamedParameters() {
        return false;
    }

    @Override
    public boolean supportsNonNullableColumns() {
        return false;
    }

    @Override
    public boolean supportsOpenCursorsAcrossCommit() {
        return false;
    }

    @Override
    public boolean supportsOpenCursorsAcrossRollback() {
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossCommit() {
        return false;
    }

    @Override
    public boolean supportsOpenStatementsAcrossRollback() {
        return false;
    }

    @Override
    public boolean supportsOrderByUnrelated() {
        return false;
    }

    @Override
    public boolean supportsOuterJoins() {
        return true; // JCR-SQL2
    }

    @Override
    public boolean supportsPositionedDelete() {
        return false; // read-only
    }

    @Override
    public boolean supportsPositionedUpdate() {
        return false; // read-only
    }

    @Override
    public boolean supportsResultSetConcurrency( int type,
                                                 int concurrency ) {
        return false;
    }

    @Override
    public boolean supportsResultSetHoldability( int holdability ) {
        return false;
    }

    @Override
    public boolean supportsResultSetType( int type ) {
        return false;
    }

    @Override
    public boolean supportsSavepoints() {
        return false; // nope
    }

    @Override
    public boolean supportsSchemasInDataManipulation() {
        return false; // nope
    }

    @Override
    public boolean supportsSchemasInIndexDefinitions() {
        return false; // nope
    }

    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() {
        return false; // nope
    }

    @Override
    public boolean supportsSchemasInProcedureCalls() {
        return false; // nope
    }

    @Override
    public boolean supportsSchemasInTableDefinitions() {
        return false; // nope
    }

    @Override
    public boolean supportsSelectForUpdate() {
        return false; // read-only
    }

    @Override
    public boolean supportsStatementPooling() {
        return false; // nope
    }

    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() {
        return false; // nope
    }

    @Override
    public boolean supportsStoredProcedures() {
        return false; // nope
    }

    @Override
    public boolean supportsSubqueriesInComparisons() {
        return false; // no subqueries in JCR-SQL2
    }

    @Override
    public boolean supportsSubqueriesInExists() {
        return false; // no subqueries in JCR-SQL2
    }

    @Override
    public boolean supportsSubqueriesInIns() {
        return false; // no subqueries in JCR-SQL2
    }

    @Override
    public boolean supportsSubqueriesInQuantifieds() {
        return false; // no subqueries in JCR-SQL2
    }

    @Override
    public boolean supportsTableCorrelationNames() {
        // JCR-SQL2 does support table aliases that can be used as prefixes for column names
        return true;
    }

    @Override
    public boolean supportsTransactionIsolationLevel( int level ) {
        return level == Connection.TRANSACTION_READ_COMMITTED;
    }

    @Override
    public boolean supportsTransactions() {
        // Generally, JCR does support transactions ...
        return false;
    }

    @Override
    public boolean supportsUnion() {
        // JCR-SQL2 does not support UNION ...
        return false;
    }

    @Override
    public boolean supportsUnionAll() {
        // JCR-SQL2 does not support UNION ALL ...
        return false;
    }

    @Override
    public boolean updatesAreDetected( int type ) {
        return false;
    }

    @Override
    public boolean usesLocalFilePerTable() {
        return false;
    }

    @Override
    public boolean usesLocalFiles() {
        return false;
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) {
        return iface.isInstance(this);
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if (!isWrapperFor(iface)) {
            throw new SQLException(JdbcLocalI18n.classDoesNotImplementInterface.text(DatabaseMetaData.class.getSimpleName(),
                                                                                     iface.getName()));
        }

        return iface.cast(this);
    }

    private List<NodeType> filterNodeTypes( String tableNamePattern ) throws RepositoryException {
        List<NodeType> nodetypes = null;

        if (tableNamePattern.trim().equals(WILDCARD)) {

            nodetypes = new ArrayList<>(this.connection.getRepositoryDelegate().nodeTypes());

        } else if (tableNamePattern.contains(WILDCARD)) {
            nodetypes = new ArrayList<NodeType>();
            String partName = null;
            boolean isLeading = false;
            boolean isTrailing = false;
            partName = tableNamePattern;

            if (partName.startsWith(WILDCARD)) {
                partName = partName.substring(1);
                isLeading = true;
            }
            if (partName.endsWith(WILDCARD) && partName.length() > 1) {
                partName = partName.substring(0, partName.length() - 1);
                isTrailing = true;
            }

            List<NodeType> nts = new ArrayList<>(this.connection.getRepositoryDelegate().nodeTypes());
            // build the list of records from server's Results object.
            for (NodeType type : nts) {
                if (isLeading) {
                    if (isTrailing) {
                        if (type.getName().indexOf(partName, 1) > -1) {
                            nodetypes.add(type);
                        }
                    } else if (type.getName().endsWith(partName)) {
                        nodetypes.add(type);
                    }

                } else if (isTrailing) {
                    if (type.getName().startsWith(partName)) {
                        nodetypes.add(type);
                    }
                }
            }

        } else {
            NodeType nt = this.connection.getRepositoryDelegate().nodeType(tableNamePattern);
            nodetypes = new ArrayList<NodeType>(1);
            if (nt != null) {
                nodetypes.add(nt);
            }
        }

        if (nodetypes.size() > 1) {
            final Comparator<NodeType> name_order = new Comparator<NodeType>() {
                @Override
                public int compare( NodeType e1,
                                    NodeType e2 ) {
                    return e1.getName().compareTo(e2.getName());
                }
            };
            Collections.sort(nodetypes, name_order);
        }

        return nodetypes;
    }

    private List<PropertyDefinition> filterPropertyDefnitions( String columnNamePattern,
                                                               NodeType nodeType ) {

        List<PropertyDefinition> allDefns = new ArrayList<PropertyDefinition>();
        addPropertyDefinitions(allDefns, nodeType);

        List<PropertyDefinition> resultDefns = null;

        if (columnNamePattern == null || columnNamePattern.trim().equals(WILDCARD)) {
            resultDefns = allDefns;
        } else if (columnNamePattern.contains(WILDCARD)) {
            resultDefns = new ArrayList<PropertyDefinition>();
            String partName = null;
            boolean isLeading = false;
            boolean isTrailing = false;
            partName = columnNamePattern;

            if (partName.startsWith(WILDCARD)) {
                partName = partName.substring(1);
                isLeading = true;
            }
            if (partName.endsWith(WILDCARD) && partName.length() > 1) {
                partName = partName.substring(0, partName.length() - 1);
                isTrailing = true;
            }

            for (PropertyDefinition defn : allDefns) {
                if (isLeading) {
                    if (isTrailing) {
                        if (defn.getName().indexOf(partName, 1) > -1) {
                            resultDefns.add(defn);
                        }
                    } else if (defn.getName().endsWith(partName)) {
                        resultDefns.add(defn);
                    }

                } else if (isTrailing) {
                    if (defn.getName().startsWith(partName)) {
                        resultDefns.add(defn);
                    }
                }
            }

        } else {
            resultDefns = new ArrayList<PropertyDefinition>();

            for (PropertyDefinition defn : allDefns) {
                if (defn.getName().equals(columnNamePattern)) {
                    resultDefns.add(defn);
                }
            }

        }

        if (resultDefns.size() > 1) {
            final Comparator<PropertyDefinition> name_order = new Comparator<PropertyDefinition>() {
                @Override
                public int compare( PropertyDefinition e1,
                                    PropertyDefinition e2 ) {
                    return e1.getName().compareTo(e2.getName());
                }
            };
            Collections.sort(resultDefns, name_order);
        }

        return resultDefns;
    }

    private void addPropertyDefinitions( List<PropertyDefinition> mapDefns,
                                         NodeType nodetype ) {

        Set<String> colNames = new HashSet<String>(mapDefns.size());
        // All tables have these pseudo-columns ...
        mapDefns.addAll(PSEUDO_COLUMN_DEFNS);
        colNames.addAll(PSEUDO_COLUMN_NAMES);

        for (PropertyDefinition defn : nodetype.getPropertyDefinitions()) {
            // Don't include residual (e.g., '*') properties as columns ...
            if (defn.getName().equalsIgnoreCase("*")) continue;
            // Don't include multi-valued properties as columns ...
            if (defn.isMultiple()) continue;
            // Don't include any properties defined in the "modeint" internal namespace ...
            if (defn.getName().startsWith("modeint:")) continue;

            if (!colNames.contains(defn.getName())) {
                mapDefns.add(defn);
                colNames.add(defn.getName());
            }
        }
    }

    protected static class PseudoPropertyDefinition implements PropertyDefinition {

        private static final String[] NO_STRINGS = new String[] {};
        private static final Value[] NO_VALUES = new Value[] {};

        private final String[] queryOps;
        private final Value[] defaultValues;
        private final int requiredType;
        private final String[] constraints;
        private final boolean isFullTextSearchable;
        private final boolean isMultiple;
        private final boolean isQueryOrderable;
        private final boolean isAutoCreated;
        private final boolean isMandatory;
        private final boolean isProtected;
        private final String name;
        private final NodeType declaringNodeType;
        private final int onParentVersioning;

        protected PseudoPropertyDefinition( NodeType declaringNodeType,
                                            String name,
                                            int requiredType,
                                            boolean autoCreated,
                                            boolean mandatory,
                                            boolean isProtected,
                                            boolean multiple,
                                            boolean fullTextSearchable,
                                            boolean queryOrderable,
                                            Value[] defaultValues,
                                            String[] constraints,
                                            int onParentVersioning,
                                            String[] queryOps ) {
            this.declaringNodeType = declaringNodeType;
            this.name = name;
            this.queryOps = queryOps != null ? queryOps : NO_STRINGS;
            this.defaultValues = defaultValues != null ? defaultValues : NO_VALUES;
            this.requiredType = requiredType;
            this.constraints = constraints != null ? constraints : NO_STRINGS;
            this.isFullTextSearchable = fullTextSearchable;
            this.isAutoCreated = autoCreated;
            this.isMultiple = multiple;
            this.isQueryOrderable = queryOrderable;
            this.isMandatory = mandatory;
            this.isProtected = isProtected;
            this.onParentVersioning = onParentVersioning;
        }

        protected PseudoPropertyDefinition( NodeType declaringNodeType,
                                            String name,
                                            int requiredType,
                                            boolean autoCreated,
                                            boolean mandatory,
                                            boolean isProtected,
                                            boolean multiple,
                                            boolean fullTextSearchable,
                                            boolean queryOrderable ) {
            this(declaringNodeType, name, requiredType, autoCreated, mandatory, isProtected, multiple, fullTextSearchable,
                 queryOrderable, null, null, OnParentVersionAction.COPY, null);
        }

        @Override
        public String[] getAvailableQueryOperators() {
            return queryOps;
        }

        @Override
        public Value[] getDefaultValues() {
            return defaultValues;
        }

        @Override
        public int getRequiredType() {
            return requiredType;
        }

        @Override
        public String[] getValueConstraints() {
            return constraints;
        }

        @Override
        public boolean isFullTextSearchable() {
            return isFullTextSearchable;
        }

        @Override
        public boolean isMultiple() {
            return isMultiple;
        }

        @Override
        public boolean isQueryOrderable() {
            return isQueryOrderable;
        }

        @Override
        public NodeType getDeclaringNodeType() {
            return declaringNodeType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getOnParentVersion() {
            return onParentVersioning;
        }

        @Override
        public boolean isAutoCreated() {
            return isAutoCreated;
        }

        @Override
        public boolean isMandatory() {
            return isMandatory;
        }

        @Override
        public boolean isProtected() {
            return isProtected;
        }

    }

}
