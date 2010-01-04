/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.meta.jdbc;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.stub;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.path.PathNode;
import org.jboss.dna.graph.connector.path.PathWorkspace;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.Path.Segment;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

public class JdbcMetadataRepositoryTest {

    private JdbcMetadataSource source;
    @Mock
    private RepositoryContext repositoryContext;
    private ExecutionContext context;
    private JdbcMetadataRepository repository;
    private PathWorkspace workspace;
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

        stub(repositoryContext.getExecutionContext()).toReturn(context);

        // Set the connection properties using the environment defined in the POM files ...
        this.source = TestEnvironment.configureJdbcMetadataSource("Test Repository", this);
        this.source.initialize(repositoryContext);

        source.getConnection().close(); // Need to call this once to instantiate the repository
        this.repository = source.repository();
        workspace = repository.getWorkspace(source.getDefaultWorkspaceName());
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

    @Test
    public void shouldNotReturnInvalidWorkspace() {
        workspace = repository.getWorkspace(source.getDefaultWorkspaceName() + "Invalid");
        assertThat(workspace, is(nullValue()));
    }

    @Test
    public void shouldReturnRootNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);
        Map<Name, Property> properties = rootNode.getProperties();

        assertThat(rootNode.getPath(), is(rootPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(6));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(DnaLexicon.ROOT));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.DATABASE_ROOT));
        assertThat(properties.get(JdbcMetadataLexicon.DATABASE_PRODUCT_NAME), is(notNullValue()));
        assertThat(properties.get(JdbcMetadataLexicon.DATABASE_PRODUCT_VERSION), is(notNullValue()));
        assertThat(properties.get(JdbcMetadataLexicon.DATABASE_MAJOR_VERSION), is(notNullValue()));
        assertThat(properties.get(JdbcMetadataLexicon.DATABASE_MINOR_VERSION), is(notNullValue()));

        assertThat(rootNode.getChildSegments().isEmpty(), is(false));
    }

    @Test
    public void shouldReturnCatalogNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);

        Segment catalogSegment = rootNode.getChildSegments().get(0);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Map<Name, Property> properties = catalogNode.getProperties();

        assertThat(catalogNode.getPath(), is(catalogPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(2));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.CATALOG));

        assertThat(rootNode.getChildSegments().isEmpty(), is(false));
    }

    @Test
    public void shouldNotReturnInvalidCatalogNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);
        Segment catalogSegment = rootNode.getChildSegments().get(0);

        Name invalidCatalogName = nameFactory.create(catalogSegment.getName().getLocalName() + "-InvalidCatalog");
        Path catalogPath = pathFactory.createAbsolutePath(invalidCatalogName);
        assertThat(workspace.getNode(catalogPath), is(nullValue()));
    }

    @Test
    public void shouldReturnSchemaNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);

        Segment catalogSegment = rootNode.getChildSegments().get(0);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildSegments().get(0);
        Path schemaPath = pathFactory.createAbsolutePath(catalogSegment, schemaSegment);
        PathNode schemaNode = workspace.getNode(schemaPath);

        Map<Name, Property> properties = schemaNode.getProperties();

        assertThat(schemaNode.getPath(), is(schemaPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(2));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.SCHEMA));

        assertThat(rootNode.getChildSegments().isEmpty(), is(false));
    }

    @Test
    public void shouldNotReturnInvalidSchemaNode() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);

        Segment catalogSegment = rootNode.getChildSegments().get(0);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildSegments().get(0);
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

        assertThat(tablesNode.getPath(), is(tablesPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(2));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.TABLES));

        assertThat(tablesNode.getChildSegments().isEmpty(), is(false));
    }

    @Test
    public void shouldNotReturnTablesNodeForInvalidSchema() {
        Segment catalogSegment = pathFactory.createSegment(nullSafeCatalogName);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildSegments().get(0);
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

        assertThat(tableNode.getPath(), is(tablePath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size() >= 2, is(true));
        assertThat(properties.size() <= 9, is(true));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.TABLE));

        assertThat(tableNode.getChildSegments().size(), is(4));
    }

    @Test
    public void shouldNotReturnTableNodeForInvalidSchema() {
        Segment catalogSegment = pathFactory.createSegment(nullSafeCatalogName);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildSegments().get(0);
        Name invalidSchemaName = nameFactory.create(schemaSegment.getName().getLocalName() + "-InvalidSchema");
        Path invalidSchemaPath = pathFactory.createAbsolutePath(catalogSegment.getName(),
                                                                invalidSchemaName,
                                                                nameFactory.create(JdbcMetadataRepository.TABLES_SEGMENT_NAME),
                                                                nameFactory.create(identifier("sales")));
        assertThat(workspace.getNode(invalidSchemaPath), is(nullValue()));
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

        assertThat(columnNode.getPath(), is(columnPath));
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
        // assertThat(columnNode.getChildSegments().isEmpty(), is(true));
    }

    @Test
    public void shouldNotReturnColumnNodeForInvalidSchema() {
        Segment catalogSegment = pathFactory.createSegment(nullSafeCatalogName);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildSegments().get(0);
        Name invalidSchemaName = nameFactory.create(schemaSegment.getName().getLocalName() + "-InvalidSchema");
        Path invalidSchemaPath = pathFactory.createAbsolutePath(catalogSegment.getName(),
                                                                invalidSchemaName,
                                                                nameFactory.create(JdbcMetadataRepository.TABLES_SEGMENT_NAME),
                                                                nameFactory.create(identifier("sales")),
                                                                nameFactory.create(identifier("amount")));
        assertThat(workspace.getNode(invalidSchemaPath), is(nullValue()));
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

        assertThat(proceduresNode.getPath(), is(proceduresPath));
        assertThat(properties, is(notNullValue()));
        assertThat(properties.size(), is(2));
        assertThat(nameFor(properties.get(JcrLexicon.PRIMARY_TYPE)), is(JcrNtLexicon.UNSTRUCTURED));
        assertThat(nameFor(properties.get(JcrLexicon.MIXIN_TYPES)), is(JdbcMetadataLexicon.PROCEDURES));

        // Not all schemas will have stored procs
        // assertThat(proceduresNode.getChildSegments().isEmpty(), is(false));
    }

    @Test
    public void shouldNotReturnProceduresNodeForInvalidSchema() {
        Path rootPath = pathFactory.createRootPath();
        PathNode rootNode = workspace.getNode(rootPath);

        Segment catalogSegment = rootNode.getChildSegments().get(0);
        Path catalogPath = pathFactory.createAbsolutePath(catalogSegment);
        PathNode catalogNode = workspace.getNode(catalogPath);

        Segment schemaSegment = catalogNode.getChildSegments().get(0);
        Name invalidSchemaName = nameFactory.create(schemaSegment.getName().getLocalName() + "-InvalidSchema");
        Path invalidSchemaPath = pathFactory.createAbsolutePath(catalogSegment.getName(),
                                                                invalidSchemaName,
                                                                nameFactory.create(JdbcMetadataRepository.PROCEDURES_SEGMENT_NAME));
        assertThat(workspace.getNode(invalidSchemaPath), is(nullValue()));
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
