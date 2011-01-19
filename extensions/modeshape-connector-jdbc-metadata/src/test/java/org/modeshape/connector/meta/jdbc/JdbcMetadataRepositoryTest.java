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
package org.modeshape.connector.meta.jdbc;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modeshape.connector.meta.jdbc.JdbcMetadataRepository.JdbcMetadataTransaction;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.base.PathNode;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.InvalidWorkspaceException;

public class JdbcMetadataRepositoryTest {

    private JdbcMetadataSource source;
    @Mock
    private RepositoryContext repositoryContext;
    private ExecutionContext context;
    private JdbcMetadataRepository repository;
    private JdbcMetadataWorkspace workspace;
    private JdbcMetadataTransaction txn;
    private PathFactory pathFactory;
    private NameFactory nameFactory;
    private ValueFactory<Long> longFactory;
    private ValueFactory<String> stringFactory;

    /*
     * The catalog in schema into which the test DDL was loaded
     */
    private String loadedCatalogName;
    private String loadedSchemaName;
    private String nullSafeCatalogName;
    private String nullSafeSchemaName;

    private boolean upperCaseIdentifiers;
    private boolean lowerCaseIdentifiers;

    @Before
    public void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        context = new ExecutionContext();
        pathFactory = context.getValueFactories().getPathFactory();
        nameFactory = context.getValueFactories().getNameFactory();
        longFactory = context.getValueFactories().getLongFactory();
        stringFactory = context.getValueFactories().getStringFactory();

        when(repositoryContext.getExecutionContext()).thenReturn(context);

        // Set the connection properties using the environment defined in the POM files ...
        this.source = TestEnvironment.configureJdbcMetadataSource("Test Repository", this);
        this.source.initialize(repositoryContext);

        source.getConnection().close(); // Need to call this once to instantiate the repository
        this.repository = source.repository();
        txn = repository.startTransaction(context, true);
        workspace = txn.getWorkspace(source.getDefaultWorkspaceName(), null);
        assertThat(workspace, is(notNullValue()));

        try {
            TestEnvironment.executeDdl(this.source.getDataSource(), "create.ddl", this);
        } catch (SQLException se) {

        }

        DataSource dataSource = source.getDataSource();
        Connection conn = dataSource.getConnection();
        DatabaseMetaData dmd = conn.getMetaData();

        upperCaseIdentifiers = dmd.storesUpperCaseIdentifiers();
        lowerCaseIdentifiers = dmd.storesLowerCaseIdentifiers();

        // Look up one of the tables that was just loaded to figure out which catalog and schema it's in
        ResultSet rs = dmd.getTables(null, null, identifier("district"), null);

        try {
            if (!rs.next()) {
                throw new IllegalStateException("Table creation failed -- Can't determine which catalog and schema to use");
            }

            loadedCatalogName = rs.getString("TABLE_CAT");
            if (rs.wasNull()) loadedCatalogName = null;

            loadedSchemaName = rs.getString("TABLE_SCHEM");
            if (rs.wasNull()) loadedSchemaName = null;

            if (rs.next()) {
                throw new IllegalStateException(
                                                "There is more than one table named DISTRICT in this database -- Can't determine which catalog and schema to use");
            }

            nullSafeCatalogName = loadedCatalogName == null ? source.getDefaultCatalogName() : loadedCatalogName;
            nullSafeSchemaName = loadedSchemaName == null ? source.getDefaultSchemaName() : loadedSchemaName;

        } finally {
            rs.close();
            conn.close();
        }
    }

    @After
    public void afterEach() throws Exception {
        TestEnvironment.executeDdl(this.source.getDataSource(), "drop.ddl", this);

        this.source.close();
    }

    @Test
    public void shouldOnlyHaveDefaultWorkspace() {
        Set<String> workspaceNames = repository.getWorkspaceNames();

        assertThat(workspaceNames, is(notNullValue()));
        assertThat(workspaceNames.size(), is(1));
        assertThat(workspaceNames.iterator().next(), is(source.getDefaultWorkspaceName()));

    }

    @Test( expected = InvalidWorkspaceException.class )
    public void shouldNotReturnInvalidWorkspace() {
        JdbcMetadataTransaction txn = repository.startTransaction(context, true);
        workspace = txn.getWorkspace(source.getDefaultWorkspaceName() + "Invalid", null);
    }

    @Test
    public void shouldReturnRootNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = txn.getNode(workspace, Location.create(rootPath));
        Map<Name, Property> properties = rootNode.getProperties();

        assertThat(pathFor(rootNode), is(rootPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(6));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(ModeShapeLexicon.ROOT));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.DATABASE_ROOT));
        assertThat(properties.get(JdbcMetadataLexicon.DATABASE_PRODUCT_NAME), is(notNullValue()));
        assertThat(properties.get(JdbcMetadataLexicon.DATABASE_PRODUCT_VERSION), is(notNullValue()));
        assertThat(properties.get(JdbcMetadataLexicon.DATABASE_MAJOR_VERSION), is(notNullValue()));
        assertThat(properties.get(JdbcMetadataLexicon.DATABASE_MINOR_VERSION), is(notNullValue()));

        assertThat(rootNode.getChildren().isEmpty(), is(false));
    }

    @Test
    public void shouldReturnCatalogNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);

        Segment catalogSegment = rootNode.getChildren().get(0);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Map<Name, Property> properties = catalogNode.getProperties();

        assertThat(pathFor(catalogNode), is(catalogPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(2));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.CATALOG));

        assertThat(rootNode.getChildren().isEmpty(), is(false));
    }

    @Test
    public void shouldNotReturnInvalidCatalogNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);
        Segment catalogSegment = rootNode.getChildren().get(0);

        Name invalidCatalogName = nameFactory.create(catalogSegment.getName().getLocalName() + "-InvalidCatalog");
        Path catalogPath = pathFactory.createAbsolutePath(invalidCatalogName);
        assertThat(workspace.getNode(catalogPath), is(nullValue()));
    }

    @Test
    public void shouldReturnSchemaNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);

        Segment catalogSegment = rootNode.getChildren().get(0);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildren().get(0);
        Path schemaPath = pathFactory.createAbsolutePath(catalogSegment, schemaSegment);
        PathNode schemaNode = workspace.getNode(schemaPath);

        Map<Name, Property> properties = schemaNode.getProperties();

        assertThat(pathFor(schemaNode), is(schemaPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(2));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.SCHEMA));

        assertThat(rootNode.getChildren().isEmpty(), is(false));
    }

    @Test
    public void shouldNotReturnInvalidSchemaNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);

        Segment catalogSegment = rootNode.getChildren().get(0);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildren().get(0);
        Name invalidSchemaName = nameFactory.create(schemaSegment.getName().getLocalName() + "-InvalidSchema");
        Path invalidSchemaPath = pathFactory.createAbsolutePath(catalogSegment.getName(), invalidSchemaName);
        assertThat(workspace.getNode(invalidSchemaPath), is(nullValue()));

    }

    @Test
    public void shouldReturnTablesNode() {
        Path tablesPath = pathFactory.createAbsolutePath(pathFactory.createSegment(nullSafeCatalogName),
                                                         pathFactory.createSegment(nullSafeSchemaName),
                                                         pathFactory.createSegment(JdbcMetadataRepository.TABLES_SEGMENT_NAME));
        PathNode tablesNode = workspace.getNode(tablesPath);

        Map<Name, Property> properties = tablesNode.getProperties();

        assertThat(pathFor(tablesNode), is(tablesPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(2));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.TABLES));

        assertThat(tablesNode.getChildren().isEmpty(), is(false));
    }

    @Test
    public void shouldNotReturnTablesNodeForInvalidSchema() {
        Segment catalogSegment = pathFactory.createSegment(nullSafeCatalogName);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildren().get(0);
        Name invalidSchemaName = nameFactory.create(schemaSegment.getName().getLocalName() + "-InvalidSchema");
        Path invalidSchemaPath = pathFactory.createAbsolutePath(catalogSegment.getName(),
                                                                invalidSchemaName,
                                                                nameFactory.create(JdbcMetadataRepository.TABLES_SEGMENT_NAME));
        assertThat(workspace.getNode(invalidSchemaPath), is(nullValue()));
    }

    @Test
    public void shouldReturnTableNode() {
        String nullSafeCatalogName = loadedCatalogName == null ? source.getDefaultCatalogName() : loadedCatalogName;
        String nullSafeSchemaName = loadedSchemaName == null ? source.getDefaultSchemaName() : loadedSchemaName;

        Path tablePath = pathFactory.createAbsolutePath(pathFactory.createSegment(nullSafeCatalogName),
                                                        pathFactory.createSegment(nullSafeSchemaName),
                                                        pathFactory.createSegment(JdbcMetadataRepository.TABLES_SEGMENT_NAME),
                                                        pathFactory.createSegment(identifier("sales")));
        PathNode tableNode = workspace.getNode(tablePath);

        Map<Name, Property> properties = tableNode.getProperties();

        assertThat(pathFor(tableNode), is(tablePath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size() >= 2, is(true));
        assertThat(properties.size() <= 9, is(true));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.TABLE));

        assertThat(tableNode.getChildren().size(), is(4));
    }

    @Test
    public void shouldNotReturnTableNodeForInvalidSchema() {
        Segment catalogSegment = pathFactory.createSegment(nullSafeCatalogName);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildren().get(0);
        Name invalidSchemaName = nameFactory.create(schemaSegment.getName().getLocalName() + "-InvalidSchema");
        Path invalidSchemaPath = pathFactory.createAbsolutePath(catalogSegment.getName(),
                                                                invalidSchemaName,
                                                                nameFactory.create(JdbcMetadataRepository.TABLES_SEGMENT_NAME),
                                                                nameFactory.create(identifier("sales")));
        if (ignoresSchemaPatterns()) {
            assertThat(workspace.getNode(invalidSchemaPath), is(notNullValue()));
        } else {
            assertThat(workspace.getNode(invalidSchemaPath), is(nullValue()));
        }
    }

    @Test
    public void shouldNotReturnInvalidTableNode() {
        String nullSafeCatalogName = loadedCatalogName == null ? source.getDefaultCatalogName() : loadedCatalogName;
        String nullSafeSchemaName = loadedSchemaName == null ? source.getDefaultSchemaName() : loadedSchemaName;

        Path invalidTablePath = pathFactory.createAbsolutePath(pathFactory.createSegment(nullSafeCatalogName),
                                                               pathFactory.createSegment(nullSafeSchemaName),
                                                               pathFactory.createSegment(JdbcMetadataRepository.TABLES_SEGMENT_NAME),
                                                               pathFactory.createSegment("NOT_A_VALID_TABLE"));

        assertThat(workspace.getNode(invalidTablePath), is(nullValue()));
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldReturnColumnNode() {
        String nullSafeCatalogName = loadedCatalogName == null ? source.getDefaultCatalogName() : loadedCatalogName;
        String nullSafeSchemaName = loadedSchemaName == null ? source.getDefaultSchemaName() : loadedSchemaName;

        Path columnPath = pathFactory.createAbsolutePath(pathFactory.createSegment(nullSafeCatalogName),
                                                         pathFactory.createSegment(nullSafeSchemaName),
                                                         pathFactory.createSegment(JdbcMetadataRepository.TABLES_SEGMENT_NAME),
                                                         pathFactory.createSegment(identifier("sales")),
                                                         pathFactory.createSegment(identifier("amount")));
        PathNode columnNode = workspace.getNode(columnPath);

        Map<Name, Property> properties = columnNode.getProperties();

        assertThat(pathFor(columnNode), is(columnPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size() >= 9, is(true));
        assertThat(properties.size() <= 16, is(true));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.COLUMN));

        // Oracle treats all integral types as decimals
        assertThat(longFor(properties.get(JdbcMetadataLexicon.JDBC_DATA_TYPE)), anyOf(is((long)Types.INTEGER),
                                                                                      is((long)Types.DECIMAL)));
        assertThat(stringFor(properties.get(JdbcMetadataLexicon.TYPE_NAME)), is(notNullValue()));
        assertThat(longFor(properties.get(JdbcMetadataLexicon.COLUMN_SIZE)), is(notNullValue()));
        assertThat(longFor(properties.get(JdbcMetadataLexicon.DECIMAL_DIGITS)), is(notNullValue()));
        assertThat(longFor(properties.get(JdbcMetadataLexicon.RADIX)), is(notNullValue()));
        assertThat(longFor(properties.get(JdbcMetadataLexicon.LENGTH)), is(notNullValue()));
        assertThat(longFor(properties.get(JdbcMetadataLexicon.ORDINAL_POSITION)), is(4L));

        // Some DBMSes don't have any procedures in the default schema
        // assertThat(columnNode.getChildren().isEmpty(), is(true));
    }

    @Test
    public void shouldNotReturnColumnNodeForInvalidSchema() {
        Segment catalogSegment = pathFactory.createSegment(nullSafeCatalogName);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildren().get(0);
        Name invalidSchemaName = nameFactory.create(schemaSegment.getName().getLocalName() + "-InvalidSchema");
        Path invalidSchemaPath = pathFactory.createAbsolutePath(catalogSegment.getName(),
                                                                invalidSchemaName,
                                                                nameFactory.create(JdbcMetadataRepository.TABLES_SEGMENT_NAME),
                                                                nameFactory.create(identifier("sales")),
                                                                nameFactory.create(identifier("amount")));
        if (ignoresSchemaPatterns()) {
            assertThat(workspace.getNode(invalidSchemaPath), is(notNullValue()));
        } else {
            assertThat(workspace.getNode(invalidSchemaPath), is(nullValue()));
        }
    }

    @Test
    public void shouldNotReturnColumnNodeForInvalidTable() {
        Path invalidTablePath = pathFactory.createAbsolutePath(pathFactory.createSegment(nullSafeCatalogName),
                                                               pathFactory.createSegment(nullSafeSchemaName),
                                                               pathFactory.createSegment(JdbcMetadataRepository.TABLES_SEGMENT_NAME),
                                                               pathFactory.createSegment("INVALID_TABLE_NAME"),
                                                               pathFactory.createSegment(identifier("id")));

        assertThat(workspace.getNode(invalidTablePath), is(nullValue()));
    }

    @Test
    public void shouldNotReturnInvalidColumnNode() {
        Path invalidColumnPath = pathFactory.createAbsolutePath(pathFactory.createSegment(nullSafeCatalogName),
                                                                pathFactory.createSegment(nullSafeSchemaName),
                                                                pathFactory.createSegment(JdbcMetadataRepository.TABLES_SEGMENT_NAME),
                                                                pathFactory.createSegment(identifier("sales")),
                                                                pathFactory.createSegment("INVALID_COLUMN_NAME"));

        assertThat(workspace.getNode(invalidColumnPath), is(nullValue()));
    }

    @Test
    public void shouldReturnProceduresNode() {
        Path proceduresPath = pathFactory.createAbsolutePath(pathFactory.createSegment(nullSafeCatalogName),
                                                             pathFactory.createSegment(nullSafeSchemaName),
                                                             pathFactory.createSegment(JdbcMetadataRepository.PROCEDURES_SEGMENT_NAME));
        PathNode proceduresNode = workspace.getNode(proceduresPath);

        Map<Name, Property> properties = proceduresNode.getProperties();

        assertThat(pathFor(proceduresNode), is(proceduresPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(2));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.PROCEDURES));

        // Not all schemas will have stored procs
        // assertThat(proceduresNode.getChildren().isEmpty(), is(false));
    }

    @Test
    public void shouldNotReturnProceduresNodeForInvalidSchema() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);

        Segment catalogSegment = rootNode.getChildren().get(0);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildren().get(0);
        Name invalidSchemaName = nameFactory.create(schemaSegment.getName().getLocalName() + "-InvalidSchema");
        Path invalidSchemaPath = pathFactory.createAbsolutePath(catalogSegment.getName(),
                                                                invalidSchemaName,
                                                                nameFactory.create(JdbcMetadataRepository.PROCEDURES_SEGMENT_NAME));
        assertThat(workspace.getNode(invalidSchemaPath), is(nullValue()));
    }

    /**
     * Not all databases will use the schema pattern when getting table and column metadata.
     * 
     * @return true if the database ignores the schema patterns.
     */
    protected boolean ignoresSchemaPatterns() {
        return source.getDriverClassName().toLowerCase().contains("mysql");
    }

    private Path pathFor( PathNode node ) {
        if (node == null) {
            return null;
        }

        if (node.getParent() == null) {
            return pathFactory.createRootPath();
        }

        return pathFactory.create(node.getParent(), node.getName());
    }

    private Name nameFor( Property property ) {
        if (property == null) {
            return null;
        }

        if (property.isEmpty()) {
            return null;
        }

        return nameFactory.create(property.getFirstValue());
    }

    private Long longFor( Property property ) {
        if (property == null) {
            return null;
        }

        if (property.isEmpty()) {
            return null;
        }

        return longFactory.create(property.getFirstValue());
    }

    private String stringFor( Property property ) {
        if (property == null) {
            return null;
        }

        if (property.isEmpty()) {
            return null;
        }

        return stringFactory.create(property.getFirstValue());
    }

    private String identifier( String rawIdentifier ) {
        if (upperCaseIdentifiers) {
            return rawIdentifier.toUpperCase();
        } else if (lowerCaseIdentifiers) {
            return rawIdentifier.toLowerCase();
        }
        return rawIdentifier;
    }
}
