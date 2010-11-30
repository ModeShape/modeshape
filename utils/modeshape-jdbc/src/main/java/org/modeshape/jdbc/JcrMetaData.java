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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
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

/**
 * This driver's implementation of JDBC {@link DatabaseMetaData}.
 */
public class JcrMetaData implements DatabaseMetaData {

    protected static final List<PropertyDefinition> PSEUDO_COLUMN_DEFNS;

    static {
        List<PropertyDefinition> defns = new ArrayList<PropertyDefinition>();
        boolean auto = true;
        boolean mand = false;
        boolean prot = true;
        boolean mult = false;
        boolean search = true;
        boolean order = true;
        defns.add(new PseudoPropertyDefinition(null, "jcr:path", PropertyType.PATH, auto, mand, prot, mult, search, order));
        defns.add(new PseudoPropertyDefinition(null, "jcr:name", PropertyType.NAME, auto, mand, prot, mult, search, order));
        defns.add(new PseudoPropertyDefinition(null, "jcr:score", PropertyType.DOUBLE, auto, mand, prot, mult, search, order));
        defns.add(new PseudoPropertyDefinition(null, "mode:localName", PropertyType.STRING, auto, mand, prot, mult, search, order));
        defns.add(new PseudoPropertyDefinition(null, "mode:depth", PropertyType.LONG, auto, mand, prot, mult, search, order));
        PSEUDO_COLUMN_DEFNS = Collections.unmodifiableList(defns);
    }

    /** CONSTANTS */
    protected static final String WILDCARD = "%"; //$NON-NLS-1$
    protected static final Integer DEFAULT_ZERO = new Integer(0);
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
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getDriverMajorVersion()
     */
    @Override
    public int getDriverMajorVersion() {
        return JcrDriver.getDriverMetadata().getMajorVersion();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getDriverMinorVersion()
     */
    @Override
    public int getDriverMinorVersion() {
        return JcrDriver.getDriverMetadata().getMinorVersion();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getDriverName()
     */
    @Override
    public String getDriverName() {
        return JcrDriver.getDriverMetadata().getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getDriverVersion()
     */
    @Override
    public String getDriverVersion() {
        return JcrDriver.getDriverMetadata().getVersion();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getDatabaseMajorVersion()
     */
    @Override
    public int getDatabaseMajorVersion() {
        return Integer.parseInt(getDatabaseProductVersion().split("[.-]")[0]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getDatabaseMinorVersion()
     */
    @Override
    public int getDatabaseMinorVersion() {
        return Integer.parseInt(getDatabaseProductVersion().split("[.-]")[1]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getDatabaseProductName()
     */
    @Override
    public String getDatabaseProductName() {
        return this.connection.getRepositoryDelegate().getDescriptor(Repository.REP_NAME_DESC);
    }

    public String getDatabaseProductUrl() {
        return this.connection.getRepositoryDelegate().getDescriptor(Repository.REP_VENDOR_URL_DESC);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getDatabaseProductVersion()
     */
    @Override
    public String getDatabaseProductVersion() {
        return this.connection.getRepositoryDelegate().getDescriptor(Repository.REP_VERSION_DESC);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getJDBCMajorVersion()
     */
    @Override
    public int getJDBCMajorVersion() {
        return 2;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getJDBCMinorVersion()
     */
    @Override
    public int getJDBCMinorVersion() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getConnection()
     */
    @Override
    public Connection getConnection() {
        return connection;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#isReadOnly()
     */
    @Override
    public boolean isReadOnly() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#allProceduresAreCallable()
     */
    @Override
    public boolean allProceduresAreCallable() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#allTablesAreSelectable()
     */
    @Override
    public boolean allTablesAreSelectable() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#autoCommitFailureClosesAllResultSets()
     */
    @Override
    public boolean autoCommitFailureClosesAllResultSets() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#dataDefinitionCausesTransactionCommit()
     */
    @Override
    public boolean dataDefinitionCausesTransactionCommit() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#dataDefinitionIgnoredInTransactions()
     */
    @Override
    public boolean dataDefinitionIgnoredInTransactions() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#deletesAreDetected(int)
     */
    @Override
    public boolean deletesAreDetected( int type ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#doesMaxRowSizeIncludeBlobs()
     */
    @Override
    public boolean doesMaxRowSizeIncludeBlobs() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getAttributes(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getAttributes( String catalog,
                                    String schemaPattern,
                                    String typeNamePattern,
                                    String attributeNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getBestRowIdentifier(java.lang.String, java.lang.String, java.lang.String, int, boolean)
     */
    @Override
    public ResultSet getBestRowIdentifier( String catalog,
                                           String schema,
                                           String table,
                                           int scope,
                                           boolean nullable ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getCatalogSeparator()
     */
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

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.CATALOGS.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        JcrStatement stmt = new JcrStatement(this.connection);
        QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
        ResultSet rs = new JcrResultSet(stmt, queryresult, resultSetMetaData);

        return rs;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getClientInfoProperties()
     */
    @Override
    public ResultSet getClientInfoProperties() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getColumnPrivileges(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getColumnPrivileges( String catalog,
                                          String schema,
                                          String table,
                                          String columnNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getColumns(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getColumns( String catalog,
                                 String schemaPattern,
                                 String tableNamePattern,
                                 String columnNamePattern ) throws SQLException {
        JcrDriver.logger.log(Level.FINE, "getcolumns: " + catalog + ":" + schemaPattern + ":" + tableNamePattern + ":"
                                         + columnNamePattern);

        // Get all tables if tableNamePattern is null
        if (tableNamePattern == null) tableNamePattern = WILDCARD;

        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.COLUMNS.MAX_COLUMNS];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.COLUMN_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.DATA_TYPE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.TYPE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[6] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.COLUMN_SIZE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[7] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.BUFFER_LENGTH,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[8] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.DECIMAL_DIGITS,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[9] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.NUM_PREC_RADIX,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);

        metadataList[10] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.NULLABLE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                              this.connection);
        metadataList[11] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.REMARKS,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                              this.connection);
        metadataList[12] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.COLUMN_DEF,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                              this.connection);
        metadataList[13] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.SQL_DATA_TYPE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                              this.connection);
        metadataList[14] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.SQL_DATETIME_SUB,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                              this.connection);
        metadataList[15] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.CHAR_OCTET_LENGTH,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                              this.connection);
        metadataList[16] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.ORDINAL_POSITION,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                              this.connection);
        metadataList[17] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.IS_NULLABLE,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                              this.connection);
        metadataList[18] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.SCOPE_CATLOG,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                              this.connection);
        metadataList[19] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.SCOPE_SCHEMA,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                              this.connection);

        metadataList[20] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.SCOPE_TABLE,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                              this.connection);
        metadataList[21] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.COLUMNS.SOURCE_DATA_TYPE,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                              this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        List<List<?>> records = new ArrayList<List<?>>();

        try {

            List<NodeType> nodetypes = filterNodeTypes(tableNamePattern);

            Iterator<NodeType> nodeIt = nodetypes.iterator();
            // process each node
            while (nodeIt.hasNext()) {

                NodeType type = nodeIt.next();

                if (type.getPropertyDefinitions() == null) {
                    throw new SQLException("Program Error:  missing propertydefintions for " + type.getName());
                }

                List<PropertyDefinition> defns = filterPropertyDefnitions(columnNamePattern, type);

                int ordinal = 0;
                Iterator<PropertyDefinition> defnsIt = defns.iterator();
                // build the list of records.
                while (defnsIt.hasNext()) {

                    PropertyDefinition propDefn = defnsIt.next();

                    JcrType jcrtype = JcrType.typeInfo(propDefn.getRequiredType());
                    
                    Integer nullable = propDefn.isMandatory() ? ResultsMetadataConstants.NULL_TYPES.NOT_NULL : ResultsMetadataConstants.NULL_TYPES.NULLABLE;
                    
                    List<Object> currentRow = loadCurrentRow( type.getName(), propDefn.getName(), jcrtype, nullable, propDefn.isMandatory(), ordinal);

                    // add the current row to the list of records.
                    records.add(currentRow);

                    ++ordinal;
                }
                // if columns where added and if Teiid Support is requested, then add the mode:properties to the list of columns
                if (ordinal > 0 && this.connection.getRepositoryDelegate().getConnectionInfo().isTeiidSupport()) {
                	if (this.connection.getRepositoryDelegate().getConnectionInfo().isTeiidSupport()) {
                		
                        List<Object> currentRow = loadCurrentRow(type.getName(), 
                        		"mode:properties",  
                        		JcrType.typeInfo(PropertyType.STRING), 
                        		ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                        		Boolean.FALSE.booleanValue(), ordinal);
                        
                        records.add(currentRow);
                	}
                }

            }// end of while

            JcrStatement jcrstmt = new JcrStatement(this.connection);
            QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
            return new JcrResultSet(jcrstmt, queryresult, resultSetMetaData);

        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage());
        }
    }
    
    private List<Object> loadCurrentRow(String tableName, String columnName, JcrType jcrtype, Integer nullable, boolean isMandatory, int ordinal) {
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
        currentRow.add(new Integer(ordinal + 1)); // ORDINAL_POSITION
        currentRow.add(isMandatory ? "NO" : "YES"); // IS_NULLABLE
        currentRow.add("NULL"); // SCOPE_CATLOG
        currentRow.add("NULL"); // SCOPE_SCHEMA

        currentRow.add("NULL"); // SCOPE_TABLE
        currentRow.add(JcrMetaData.DEFAULT_ZERO); // SOURCE_DATA_TYPE
        
        return currentRow;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getCrossReference(java.lang.String, java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getCrossReference( String parentCatalog,
                                        String parentSchema,
                                        String parentTable,
                                        String foreignCatalog,
                                        String foreignSchema,
                                        String foreignTable ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getDefaultTransactionIsolation()
     */
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
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getExtraNameCharacters()
     */
    @Override
    public String getExtraNameCharacters() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getFunctionColumns(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getFunctionColumns( String catalog,
                                         String schemaPattern,
                                         String functionNamePattern,
                                         String columnNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getFunctions(java.lang.String, java.lang.String, java.lang.String)
     */
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
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getIndexInfo(java.lang.String, java.lang.String, java.lang.String, boolean, boolean)
     */
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
        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.NON_UNIQUE,
                                                             JcrType.DefaultDataTypes.BOOLEAN,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.INDEX_QUALIFIER,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.INDEX_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[6] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.TYPE,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[7] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.ORDINAL_POSITION,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[8] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.COLUMN_NAME,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[9] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.INDEX_INFO.ASC_OR_DESC,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);

        metadataList[10] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.INDEX_INFO.CARDINALITY,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                              this.connection);
        metadataList[11] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.INDEX_INFO.PAGES,
                                                              JcrType.DefaultDataTypes.LONG,
                                                              ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                              this.connection);
        metadataList[12] = MetadataProvider.getColumnMetadata(catalogName,
                                                              null,
                                                              JDBCColumnNames.INDEX_INFO.FILTER_CONDITION,
                                                              JcrType.DefaultDataTypes.STRING,
                                                              ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                              this.connection);

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
                currentRow.add(new Short((short)1)); // ORDINAL_POSITION
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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getMaxStatementLength()
     */
    @Override
    public int getMaxStatementLength() {
        return 0; // no limit
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getMaxStatements()
     */
    @Override
    public int getMaxStatements() {
        return 0; // no limit
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getMaxTableNameLength()
     */
    @Override
    public int getMaxTableNameLength() {
        return 0; // no limit
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getMaxTablesInSelect()
     */
    @Override
    public int getMaxTablesInSelect() {
        return 0; // not known (technically there is no limit, but there would be a practical limit)
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getMaxUserNameLength()
     */
    @Override
    public int getMaxUserNameLength() {
        return 0; // no limit
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getNumericFunctions()
     */
    @Override
    public String getNumericFunctions() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getPrimaryKeys(java.lang.String, java.lang.String, java.lang.String)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getPrimaryKeys( String catalog,
                                     String schema,
                                     String tableNamePattern ) throws SQLException {
        // Get primary keys for all tables if tableNamePattern is null
        if (tableNamePattern == null) tableNamePattern = WILDCARD;

        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.PRIMARY_KEYS.MAX_COLUMNS];
        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.PRIMARY_KEYS.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.PRIMARY_KEYS.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.PRIMARY_KEYS.TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.PRIMARY_KEYS.COLUMN_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.PRIMARY_KEYS.KEY_SEQ,
                                                             JcrType.DefaultDataTypes.LONG,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.PRIMARY_KEYS.PK_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);

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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getProcedureColumns(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getProcedureColumns( String catalog,
                                          String schemaPattern,
                                          String procedureNamePattern,
                                          String columnNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getProcedureTerm()
     */
    @Override
    public String getProcedureTerm() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getProcedures(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getProcedures( String catalog,
                                    String schemaPattern,
                                    String procedureNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getResultSetHoldability()
     */
    @Override
    public int getResultSetHoldability() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getRowIdLifetime()
     */
    @Override
    public RowIdLifetime getRowIdLifetime() {
        return RowIdLifetime.ROWID_UNSUPPORTED;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getSQLKeywords()
     */
    @Override
    public String getSQLKeywords() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getSQLStateType()
     */
    @Override
    public int getSQLStateType() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getSchemaTerm()
     */
    @Override
    public String getSchemaTerm() {
        return " ";
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getSchemas()
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getSchemas() throws SQLException {
        List<List<?>> records = new ArrayList<List<?>>(1);

        /***********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         ***********************************************************************/

        Map<?, Object>[] metadataList = new Map[1];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.COLUMNS.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        JcrStatement stmt = new JcrStatement(this.connection);
        QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
        ResultSet rs = new JcrResultSet(stmt, queryresult, resultSetMetaData);

        return rs;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getSchemas(java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getSchemas( String catalog,
                                 String schemaPattern ) throws SQLException {
        return getSchemas();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getSearchStringEscape()
     */
    @Override
    public String getSearchStringEscape() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getStringFunctions()
     */
    @Override
    public String getStringFunctions() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getSuperTables(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getSuperTables( String catalog,
                                     String schemaPattern,
                                     String tableNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getSuperTypes(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getSuperTypes( String catalog,
                                    String schemaPattern,
                                    String typeNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getSystemFunctions()
     */
    @Override
    public String getSystemFunctions() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getTablePrivileges(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getTablePrivileges( String catalog,
                                         String schemaPattern,
                                         String tableNamePattern ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getTableTypes()
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getTableTypes() throws SQLException {

        List<List<?>> records = new ArrayList<List<?>>(1);
        List<String> row = Arrays.asList(new String[] {ResultsMetadataConstants.TABLE_TYPES.VIEW});
        records.add(row);

        /***********************************************************************
         * Hardcoding JDBC column names for the columns returned in results object
         ***********************************************************************/

        Map<?, Object>[] metadataList = new Map[1];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLE_TYPES.TABLE_TYPE,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        JcrStatement stmt = new JcrStatement(this.connection);
        QueryResult queryresult = MetaDataQueryResult.createResultSet(records, resultSetMetaData);
        ResultSet rs = new JcrResultSet(stmt, queryresult, resultSetMetaData);
        return rs;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getTables(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    @SuppressWarnings( "unchecked" )
    @Override
    public ResultSet getTables( String catalog,
                                String schemaPattern,
                                String tableNamePattern,
                                String[] types ) throws SQLException {

        JcrDriver.logger.log(Level.FINE, "getTables: " + catalog + ":" + schemaPattern + ":" + tableNamePattern + ":" + types);

        // Get all tables if tableNamePattern is null
        if (tableNamePattern == null) {
            tableNamePattern = WILDCARD;
        }

        Map<?, Object>[] metadataList = new Map[JDBCColumnPositions.TABLES.MAX_COLUMNS];

        metadataList[0] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.TABLE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[1] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.TABLE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[2] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.TABLE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[3] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.TABLE_TYPE,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NOT_NULL,
                                                             this.connection);
        metadataList[4] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.REMARKS,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[5] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.TYPE_CAT,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[6] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.TYPE_SCHEM,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[7] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.TYPE_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[8] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.SELF_REFERENCING_COL_NAME,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);
        metadataList[9] = MetadataProvider.getColumnMetadata(catalogName,
                                                             null,
                                                             JDBCColumnNames.TABLES.REF_GENERATION,
                                                             JcrType.DefaultDataTypes.STRING,
                                                             ResultsMetadataConstants.NULL_TYPES.NULLABLE,
                                                             this.connection);

        MetadataProvider provider = new MetadataProvider(metadataList);

        ResultSetMetaDataImpl resultSetMetaData = new ResultSetMetaDataImpl(provider);

        List<List<?>> records = new ArrayList<List<?>>();

        try {
            List<NodeType> nodetypes = filterNodeTypes(tableNamePattern);

            Iterator<NodeType> nodeIt = nodetypes.iterator();
            // build the list of records from the nodetypes.
            while (nodeIt.hasNext()) {

                NodeType type = nodeIt.next();
                if (!type.isQueryable()) continue;

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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getTimeDateFunctions()
     */
    @Override
    public String getTimeDateFunctions() {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getTypeInfo()
     */
    @Override
    public ResultSet getTypeInfo() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getUDTs(java.lang.String, java.lang.String, java.lang.String, int[])
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getUserName()
     */
    @Override
    public String getUserName() {
        return connection.info().getUsername();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#getVersionColumns(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public ResultSet getVersionColumns( String catalog,
                                        String schema,
                                        String table ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#insertsAreDetected(int)
     */
    @Override
    public boolean insertsAreDetected( int type ) {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#isCatalogAtStart()
     */
    @Override
    public boolean isCatalogAtStart() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#locatorsUpdateCopy()
     */
    @Override
    public boolean locatorsUpdateCopy() {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#nullPlusNonNullIsNull()
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#othersDeletesAreVisible(int)
     */
    @Override
    public boolean othersDeletesAreVisible( int type ) {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#othersInsertsAreVisible(int)
     */
    @Override
    public boolean othersInsertsAreVisible( int type ) {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#othersUpdatesAreVisible(int)
     */
    @Override
    public boolean othersUpdatesAreVisible( int type ) {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#ownDeletesAreVisible(int)
     */
    @Override
    public boolean ownDeletesAreVisible( int type ) {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#ownInsertsAreVisible(int)
     */
    @Override
    public boolean ownInsertsAreVisible( int type ) {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#ownUpdatesAreVisible(int)
     */
    @Override
    public boolean ownUpdatesAreVisible( int type ) {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#storesLowerCaseIdentifiers()
     */
    @Override
    public boolean storesLowerCaseIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#storesLowerCaseQuotedIdentifiers()
     */
    @Override
    public boolean storesLowerCaseQuotedIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#storesMixedCaseIdentifiers()
     */
    @Override
    public boolean storesMixedCaseIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#storesMixedCaseQuotedIdentifiers()
     */
    @Override
    public boolean storesMixedCaseQuotedIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#storesUpperCaseIdentifiers()
     */
    @Override
    public boolean storesUpperCaseIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#storesUpperCaseQuotedIdentifiers()
     */
    @Override
    public boolean storesUpperCaseQuotedIdentifiers() {
        return false; // JCR node types are case-sensitive
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsANSI92EntryLevelSQL()
     */
    @Override
    public boolean supportsANSI92EntryLevelSQL() {
        return false; // JCR-SQL2 is not entry-level ANSI92 SQL
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsANSI92FullSQL()
     */
    @Override
    public boolean supportsANSI92FullSQL() {
        return false; // JCR-SQL2 is not full ANSI92 SQL
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsANSI92IntermediateSQL()
     */
    @Override
    public boolean supportsANSI92IntermediateSQL() {
        return false; // JCR-SQL2 is not intermediate ANSI92 SQL
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsAlterTableWithAddColumn()
     */
    @Override
    public boolean supportsAlterTableWithAddColumn() {
        // Not in JCR-SQL2
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsAlterTableWithDropColumn()
     */
    @Override
    public boolean supportsAlterTableWithDropColumn() {
        // Not in JCR-SQL2
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsBatchUpdates()
     */
    @Override
    public boolean supportsBatchUpdates() {
        // Not in JCR-SQL2
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsCatalogsInDataManipulation()
     */
    @Override
    public boolean supportsCatalogsInDataManipulation() {
        // Not in JCR-SQL2
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsCatalogsInIndexDefinitions()
     */
    @Override
    public boolean supportsCatalogsInIndexDefinitions() {
        // No such thing in JCR-SQL2
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsCatalogsInPrivilegeDefinitions()
     */
    @Override
    public boolean supportsCatalogsInPrivilegeDefinitions() {
        // No defining privileges in JCR 1.0 or 2.0 or JCR-SQL2
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsCatalogsInProcedureCalls()
     */
    @Override
    public boolean supportsCatalogsInProcedureCalls() {
        // No such thing in JCR-SQL2
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsCatalogsInTableDefinitions()
     */
    @Override
    public boolean supportsCatalogsInTableDefinitions() {
        // No defining tables in JCR 1.0 or 2.0 or JCR-SQL2
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsColumnAliasing()
     */
    @Override
    public boolean supportsColumnAliasing() {
        // JCR-SQL2 does support aliases on column names (section 6.7.39)
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsConvert()
     */
    @Override
    public boolean supportsConvert() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsConvert(int, int)
     */
    @Override
    public boolean supportsConvert( int fromType,
                                    int toType ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsCoreSQLGrammar()
     */
    @Override
    public boolean supportsCoreSQLGrammar() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsCorrelatedSubqueries()
     */
    @Override
    public boolean supportsCorrelatedSubqueries() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsDataDefinitionAndDataManipulationTransactions()
     */
    @Override
    public boolean supportsDataDefinitionAndDataManipulationTransactions() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsDataManipulationTransactionsOnly()
     */
    @Override
    public boolean supportsDataManipulationTransactionsOnly() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsDifferentTableCorrelationNames()
     */
    @Override
    public boolean supportsDifferentTableCorrelationNames() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsExpressionsInOrderBy()
     */
    @Override
    public boolean supportsExpressionsInOrderBy() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsExtendedSQLGrammar()
     */
    @Override
    public boolean supportsExtendedSQLGrammar() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsFullOuterJoins()
     */
    @Override
    public boolean supportsFullOuterJoins() {
        // JCR-SQL2 does not support FULL OUTER JOIN ...
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
     */
    @Override
    public boolean supportsGetGeneratedKeys() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsGroupBy()
     */
    @Override
    public boolean supportsGroupBy() {
        return false; // not in JCR-SQL2;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsGroupByBeyondSelect()
     */
    @Override
    public boolean supportsGroupByBeyondSelect() {
        return false; // not in JCR-SQL2;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsGroupByUnrelated()
     */
    @Override
    public boolean supportsGroupByUnrelated() {
        return false; // not in JCR-SQL2;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsIntegrityEnhancementFacility()
     */
    @Override
    public boolean supportsIntegrityEnhancementFacility() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsLikeEscapeClause()
     */
    @Override
    public boolean supportsLikeEscapeClause() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsLimitedOuterJoins()
     */
    @Override
    public boolean supportsLimitedOuterJoins() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsMinimumSQLGrammar()
     */
    @Override
    public boolean supportsMinimumSQLGrammar() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsMixedCaseIdentifiers()
     */
    @Override
    public boolean supportsMixedCaseIdentifiers() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsMixedCaseQuotedIdentifiers()
     */
    @Override
    public boolean supportsMixedCaseQuotedIdentifiers() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsMultipleOpenResults()
     */
    @Override
    public boolean supportsMultipleOpenResults() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsMultipleResultSets()
     */
    @Override
    public boolean supportsMultipleResultSets() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsMultipleTransactions()
     */
    @Override
    public boolean supportsMultipleTransactions() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsNamedParameters()
     */
    @Override
    public boolean supportsNamedParameters() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsNonNullableColumns()
     */
    @Override
    public boolean supportsNonNullableColumns() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsOpenCursorsAcrossCommit()
     */
    @Override
    public boolean supportsOpenCursorsAcrossCommit() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsOpenCursorsAcrossRollback()
     */
    @Override
    public boolean supportsOpenCursorsAcrossRollback() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsOpenStatementsAcrossCommit()
     */
    @Override
    public boolean supportsOpenStatementsAcrossCommit() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsOpenStatementsAcrossRollback()
     */
    @Override
    public boolean supportsOpenStatementsAcrossRollback() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsOrderByUnrelated()
     */
    @Override
    public boolean supportsOrderByUnrelated() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsOuterJoins()
     */
    @Override
    public boolean supportsOuterJoins() {
        return true; // JCR-SQL2
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsPositionedDelete()
     */
    @Override
    public boolean supportsPositionedDelete() {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsPositionedUpdate()
     */
    @Override
    public boolean supportsPositionedUpdate() {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsResultSetConcurrency(int, int)
     */
    @Override
    public boolean supportsResultSetConcurrency( int type,
                                                 int concurrency ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsResultSetHoldability(int)
     */
    @Override
    public boolean supportsResultSetHoldability( int holdability ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsResultSetType(int)
     */
    @Override
    public boolean supportsResultSetType( int type ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSavepoints()
     */
    @Override
    public boolean supportsSavepoints() {
        return false; // nope
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSchemasInDataManipulation()
     */
    @Override
    public boolean supportsSchemasInDataManipulation() {
        return false; // nope
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSchemasInIndexDefinitions()
     */
    @Override
    public boolean supportsSchemasInIndexDefinitions() {
        return false; // nope
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSchemasInPrivilegeDefinitions()
     */
    @Override
    public boolean supportsSchemasInPrivilegeDefinitions() {
        return false; // nope
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSchemasInProcedureCalls()
     */
    @Override
    public boolean supportsSchemasInProcedureCalls() {
        return false; // nope
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSchemasInTableDefinitions()
     */
    @Override
    public boolean supportsSchemasInTableDefinitions() {
        return false; // nope
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSelectForUpdate()
     */
    @Override
    public boolean supportsSelectForUpdate() {
        return false; // read-only
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsStatementPooling()
     */
    @Override
    public boolean supportsStatementPooling() {
        return false; // nope
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsStoredFunctionsUsingCallSyntax()
     */
    @Override
    public boolean supportsStoredFunctionsUsingCallSyntax() {
        return false; // nope
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsStoredProcedures()
     */
    @Override
    public boolean supportsStoredProcedures() {
        return false; // nope
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSubqueriesInComparisons()
     */
    @Override
    public boolean supportsSubqueriesInComparisons() {
        return false; // no subqueries in JCR-SQL2
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSubqueriesInExists()
     */
    @Override
    public boolean supportsSubqueriesInExists() {
        return false; // no subqueries in JCR-SQL2
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSubqueriesInIns()
     */
    @Override
    public boolean supportsSubqueriesInIns() {
        return false; // no subqueries in JCR-SQL2
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsSubqueriesInQuantifieds()
     */
    @Override
    public boolean supportsSubqueriesInQuantifieds() {
        return false; // no subqueries in JCR-SQL2
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsTableCorrelationNames()
     */
    @Override
    public boolean supportsTableCorrelationNames() {
        // JCR-SQL2 does support table aliases that can be used as prefixes for column names
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsTransactionIsolationLevel(int)
     */
    @Override
    public boolean supportsTransactionIsolationLevel( int level ) {
        return level == Connection.TRANSACTION_READ_COMMITTED;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsTransactions()
     */
    @Override
    public boolean supportsTransactions() {
        // Generally, JCR does support transactions ...
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsUnion()
     */
    @Override
    public boolean supportsUnion() {
        // JCR-SQL2 does not support UNION ...
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#supportsUnionAll()
     */
    @Override
    public boolean supportsUnionAll() {
        // JCR-SQL2 does not support UNION ALL ...
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#updatesAreDetected(int)
     */
    @Override
    public boolean updatesAreDetected( int type ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#usesLocalFilePerTable()
     */
    @Override
    public boolean usesLocalFilePerTable() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.DatabaseMetaData#usesLocalFiles()
     */
    @Override
    public boolean usesLocalFiles() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor( Class<?> iface ) {
        return iface.isInstance(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if (!isWrapperFor(iface)) {
            throw new SQLException(JdbcI18n.classDoesNotImplementInterface.text(DatabaseMetaData.class.getSimpleName(),
                                                                                iface.getName()));
        }

        return iface.cast(this);
    }

    private List<NodeType> filterNodeTypes( String tableNamePattern ) throws RepositoryException {
        List<NodeType> nodetypes = null;

        if (tableNamePattern.trim().equals(WILDCARD)) {

            nodetypes = this.connection.getRepositoryDelegate().nodeTypes();
            Iterator<NodeType> nodeIt = nodetypes.iterator();
            while (nodeIt.hasNext()) {
                NodeType type = nodeIt.next();
                if (!hasColumnedDefined(type)) nodeIt.remove();
            }

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

            List<NodeType> nts = this.connection.getRepositoryDelegate().nodeTypes();
            Iterator<NodeType> nodeIt = nts.iterator();
            // build the list of records from server's Results object.
            while (nodeIt.hasNext()) {

                NodeType type = nodeIt.next();

                if (!hasColumnedDefined(type)) continue;

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
            if (nt != null && hasColumnedDefined(nt)) {
                nodetypes.add(nt);
            }
        }

        if (nodetypes.size() > 1) {
            final Comparator<NodeType> name_order = new Comparator<NodeType>() {
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

        if (columnNamePattern.trim().equals(WILDCARD)) {
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

            Iterator<PropertyDefinition> defnIt = allDefns.iterator();
            while (defnIt.hasNext()) {
                PropertyDefinition defn = defnIt.next();

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

            Iterator<PropertyDefinition> defnIt = allDefns.iterator();
            while (defnIt.hasNext()) {
                PropertyDefinition defn = defnIt.next();
                if (defn.getName().equals(columnNamePattern)) {
                    resultDefns.add(defn);
                }
            }

        }

        if (resultDefns.size() > 1) {
            final Comparator<PropertyDefinition> name_order = new Comparator<PropertyDefinition>() {
                public int compare( PropertyDefinition e1,
                                    PropertyDefinition e2 ) {
                    return e1.getName().compareTo(e2.getName());
                }
            };
            Collections.sort(resultDefns, name_order);
        }

        return resultDefns;
    }

    /**
     * isTableValid determines if the node type should be exposed as a table. A table must have at least one column, and the
     * property definitions and superTypes provide the columns. As long as one is defined, then one table is valid for exposure.
     * 
     * @param nodeType
     * @return true if a column is defined for the table
     */
    private boolean hasColumnedDefined( NodeType nodeType ) {
        List<PropertyDefinition> allDefns = new ArrayList<PropertyDefinition>();
        addPropertyDefinitions(allDefns, nodeType);
        return (allDefns.size() > 0 ? true : false);

    }

    private void addPropertyDefinitions( List<PropertyDefinition> mapDefns,
                                         NodeType nodetype ) {
        for (PropertyDefinition defn : nodetype.getPropertyDefinitions()) {
            // Don't include residual (e.g., '*') properties as columns ...
            if (defn.getName().equalsIgnoreCase("*")) continue;
            // Don't include multi-valued properties as columns ...
            if (defn.isMultiple()) continue;
            // Don't include any properties defined in the "modeint" internal namespace ...
            if (defn.getName().startsWith("modeint:")) continue;
            mapDefns.add(defn);
        }
        // All tables have these pseudo-columns ...
        mapDefns.addAll(PSEUDO_COLUMN_DEFNS);
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

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.PropertyDefinition#getAvailableQueryOperators()
         */
        @Override
        public String[] getAvailableQueryOperators() {
            return queryOps;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.PropertyDefinition#getDefaultValues()
         */
        @Override
        public Value[] getDefaultValues() {
            return defaultValues;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.PropertyDefinition#getRequiredType()
         */
        @Override
        public int getRequiredType() {
            return requiredType;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.PropertyDefinition#getValueConstraints()
         */
        @Override
        public String[] getValueConstraints() {
            return constraints;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.PropertyDefinition#isFullTextSearchable()
         */
        @Override
        public boolean isFullTextSearchable() {
            return isFullTextSearchable;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
         */
        @Override
        public boolean isMultiple() {
            return isMultiple;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.PropertyDefinition#isQueryOrderable()
         */
        @Override
        public boolean isQueryOrderable() {
            return isQueryOrderable;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.ItemDefinition#getDeclaringNodeType()
         */
        @Override
        public NodeType getDeclaringNodeType() {
            return declaringNodeType;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.ItemDefinition#getName()
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.ItemDefinition#getOnParentVersion()
         */
        @Override
        public int getOnParentVersion() {
            return onParentVersioning;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.ItemDefinition#isAutoCreated()
         */
        @Override
        public boolean isAutoCreated() {
            return isAutoCreated;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.ItemDefinition#isMandatory()
         */
        @Override
        public boolean isMandatory() {
            return isMandatory;
        }

        /**
         * {@inheritDoc}
         * 
         * @see javax.jcr.nodetype.ItemDefinition#isProtected()
         */
        @Override
        public boolean isProtected() {
            return isProtected;
        }

    }

}
