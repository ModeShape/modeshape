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
package org.modeshape.connector.meta.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.sql.DataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.MultiUseAbstractTest;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Session;
import org.modeshape.jcr.api.federation.FederationManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit test for {@link JdbcMetadataConnector}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JdbcMetadataConnectorTest extends MultiUseAbstractTest {

    private static final String FOREIGN_KEYS = "foreignKeys";
    private static boolean upperCaseIdentifiers;
    private static boolean lowerCaseIdentifiers;

    //The catalog & schema into which the test DDL was loaded
    private static String catalogName;
    private static String schemaName;

    @BeforeClass
    public static void beforeAll() throws Exception {
        initTestDatabase();

        RepositoryConfiguration config = RepositoryConfiguration.read("config/repo-config-jdbc-meta-federation.json");
        startRepository(config);

        Session session = getSession();
        Node testRoot = session.getRootNode().addNode("testRoot");
        session.save();

        FederationManager fedMgr = session.getWorkspace().getFederationManager();
        fedMgr.createProjection(testRoot.getPath(), "jdbc-meta", "/", "database");
    }

    private static void initTestDatabase() throws Exception {
        DataSource testDs = DatasourceHelper.getDataSource();
        DatasourceHelper.executeDdl("create.ddl");

        Connection connection = testDs.getConnection();
        DatabaseMetaData dmd = connection.getMetaData();

        upperCaseIdentifiers = dmd.storesUpperCaseIdentifiers();
        lowerCaseIdentifiers = dmd.storesLowerCaseIdentifiers();

        // Look up one of the tables that was just loaded to figure out which catalog and schema it's in
        ResultSet rs = dmd.getTables(null, null, dbString("district"), null);

        try {
            if (!rs.next()) {
                throw new IllegalStateException("Table creation failed -- Can't determine which catalog and schema to use");
            }

            catalogName = rs.getString("TABLE_CAT");
            if (rs.wasNull()) {
                //try to check if there's 1 catalog in the db and if so, use that
                //Postgres seems to behave in this way
                ResultSet catalogsRs = dmd.getCatalogs();
                List<String> catalogs = new ArrayList<String>();
                while (catalogsRs.next()) {
                    String catalog = catalogsRs.getString("TABLE_CAT");
                    if (!StringUtil.isBlank(catalog)) {
                        catalogs.add(catalog);
                    }
                }

                catalogName = catalogs.size() == 1 ? catalogs.get(0) : JdbcMetadataConnector.DEFAULT_NAME_OF_DEFAULT_CATALOG;
            }

            schemaName = rs.getString("TABLE_SCHEM");
            if (rs.wasNull()) {
                schemaName = JdbcMetadataConnector.DEFAULT_NAME_OF_DEFAULT_SCHEMA;
            }

            if (rs.next()) {
                throw new IllegalStateException(
                        "There is more than one table named DISTRICT in this database -- Can't determine which catalog and schema to use");
            }
        } finally {
            rs.close();
            connection.close();
        }

        DatasourceHelper.bindInJNDI("testDS");
    }

    private static String dbString( String string ) {
        if (upperCaseIdentifiers) {
            return string.toUpperCase();
        } else if (lowerCaseIdentifiers) {
            return string.toLowerCase();
        }
        return string;
    }

    @AfterClass
    public static void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
        DatasourceHelper.executeDdl("drop.ddl");
        DatasourceHelper.closeDataSource();
    }

    @Test
    public void shouldReadAllJDBCMetadata() throws Exception {
        Node database = session.getNode("/testRoot/database");
        assertDatabaseRoot(database);

        Node catalog = database.getNode(catalogName);
        assertCatalog(catalog);

        Node schema = catalog.getNode(schemaName);
        assertSchema(schema);

        Node procedures = schema.getNode("procedures");
        assertProcedures(procedures);

        Node tables = schema.getNode("tables");
        assertChildrenInclude(tables, dbString("chain"), dbString("area"), dbString("district"), dbString("sales"));
        assertHasMixins(tables, "mj:tables");

        Node chain = tables.getNode(dbString("chain"));
        assertTable(chain, dbString("ID"), dbString("NAME"), FOREIGN_KEYS);
        assertFKs(chain);

        Node area = tables.getNode(dbString("area"));
        assertTable(area, dbString("ID"), dbString("NAME"), dbString("CHAIN_ID"), FOREIGN_KEYS);
        assertFKs(area, dbString("CHAIN_ID"));

        Node region = tables.getNode(dbString("region"));
        assertTable(region, dbString("ID"), dbString("NAME"), dbString("AREA_ID"), FOREIGN_KEYS);
        assertFKs(region, dbString("AREA_ID"));

        Node district = tables.getNode(dbString("district"));
        assertTable(district, dbString("ID"), dbString("NAME"), dbString("REGION_ID"), FOREIGN_KEYS);
        assertFKs(district, dbString("REGION_ID"));

        Node sales = tables.getNode(dbString("sales"));
        assertTable(sales, dbString("ID"), dbString("SALES_DATE"), dbString("DISTRICT_ID"), dbString("AMOUNT"), FOREIGN_KEYS);
        assertFKs(sales);
    }

    private void assertProcedures( Node procedures ) throws RepositoryException {
        assertEquals("nt:unstructured", procedures.getPrimaryNodeType().getName());
        assertHasMixins(procedures, "mj:procedures");

        //there are no stored procedures tested atm
        assertEquals(0, procedures.getNodes().getSize());
    }

    private void assertFKs( Node table, String...expectedKeys ) throws RepositoryException {
        Node foreignKeys = table.getNode(FOREIGN_KEYS);
        assertEquals("nt:unstructured", foreignKeys.getPrimaryNodeType().getName());
        assertHasMixins(foreignKeys, "mj:foreignKeys");
        if (expectedKeys.length == 0) {
            return;
        }
        assertChildrenInclude(foreignKeys, expectedKeys);
        for (String fk : expectedKeys) {
            Node foreignKey = foreignKeys.getNode(fk);
            //only assert mandatory properties
            assertNotNull(foreignKey.getProperty(JdbcMetadataLexicon.PRIMARY_KEY_TABLE_NAME.toString()));
            assertNotNull(foreignKey.getProperty(JdbcMetadataLexicon.PRIMARY_KEY_COLUMN_NAME.toString()));
            assertNotNull(foreignKey.getProperty(JdbcMetadataLexicon.FOREIGN_KEY_TABLE_NAME.toString()));
            assertNotNull(foreignKey.getProperty(JdbcMetadataLexicon.FOREIGN_KEY_COLUMN_NAME.toString()));
        }
    }

    private void assertTable( Node table, String...expectedChildren ) throws RepositoryException {
        assertEquals("nt:unstructured", table.getPrimaryNodeType().getName());
        assertHasMixins(table, "mj:table");
        //only assert the mandatory properties
        assertNotNull(table.getProperty(JdbcMetadataLexicon.TABLE_TYPE.toString()));

        assertChildrenInclude(table, expectedChildren);
        for (String child : expectedChildren) {
            if (child.equalsIgnoreCase(FOREIGN_KEYS)) {
                continue;
            }
            Node column = table.getNode(child);
            assertEquals("nt:unstructured", column.getPrimaryNodeType().getName());
            assertHasMixins(column, "mj:column");
            //only assert mandatory properties
            assertNotNull(column.getProperty(JdbcMetadataLexicon.JDBC_DATA_TYPE.toString()));
            assertNotNull(column.getProperty(JdbcMetadataLexicon.COLUMN_SIZE.toString()));
            assertNotNull(column.getProperty(JdbcMetadataLexicon.DECIMAL_DIGITS.toString()));
            assertNotNull(column.getProperty(JdbcMetadataLexicon.RADIX.toString()));
            assertNotNull(column.getProperty(JdbcMetadataLexicon.ORDINAL_POSITION.toString()));
        }
    }

    private void assertSchema( Node schema ) throws RepositoryException {
        assertEquals("nt:unstructured", schema.getPrimaryNodeType().getName());
        assertHasMixins(schema, "mj:schema");
        assertChildrenInclude(schema, "tables", "procedures");
    }

    private void assertCatalog( Node catalog ) throws RepositoryException {
        assertEquals("nt:unstructured", catalog.getPrimaryNodeType().getName());
        assertHasMixins(catalog, "mj:catalog");
        assertChildrenInclude(catalog, schemaName);
    }

    private void assertDatabaseRoot( Node database ) throws RepositoryException {
        assertEquals("nt:unstructured", database.getPrimaryNodeType().getName());
        assertHasMixins(database, "mj:databaseRoot");

        assertNotNull(database.getProperty(JdbcMetadataLexicon.DATABASE_PRODUCT_NAME.toString()));
        assertNotNull(database.getProperty(JdbcMetadataLexicon.DATABASE_PRODUCT_VERSION.toString()));
        assertNotNull(database.getProperty(JdbcMetadataLexicon.DATABASE_MAJOR_VERSION.toString()));
        assertNotNull(database.getProperty(JdbcMetadataLexicon.DATABASE_MINOR_VERSION.toString()));

        assertChildrenInclude(database, catalogName);
    }
}
