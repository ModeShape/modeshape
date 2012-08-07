/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.jcr.value.binary;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.junit.*;
import static org.junit.Assert.*;
import org.modeshape.jcr.value.BinaryKey;

/**
 *
 * @author kulikov
 */
public class DatabaseTest {

    private Database database;
    private Database.SQLBuilder sqlBuilder;

    public DatabaseTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws BinaryStoreException {
        database = new Database(null);
        sqlBuilder = database.new SQLBuilder();
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testDefaultInsertStatementBuilding() {
        String sql = sqlBuilder
                .insert().into("xxx").columns("c1", "c2", "c3").values("?","?","?").getSQL().toLowerCase();
        assertEquals("insert into xxx (c1, c2, c3) values (?, ?, ?)", sql);
    }

    @Test
    public void testDefaultSelectStatement() {
        String sql = sqlBuilder
                .select().columns("c1", "c2", "c3").from("xxx")
                .where().condition("c1", "integer", "<", "?")
                .and().condition("c2", "integer", "=", "?").getSQL().toLowerCase();
        assertEquals("select c1, c2, c3 from xxx where c1<? and c2=?", sql);
    }

    @Test
    public void testDefaultDeleteStatement() {
        String sql = sqlBuilder
                .delete().from("xxx")
                .where().condition("c1", "integer", "<", "?")
                .and().condition("c2", "integer", "=", "?").getSQL().toLowerCase();
        assertEquals("delete  from xxx where c1<? and c2=?", sql);
    }

    @Test
    public void testDefaultUpdateStatement() {
        String sql = sqlBuilder
                .update("xxx")
                .set("c1", "?")
                .where().condition("c1", "integer", "<", "?")
                .and().condition("c2", "integer", "=", "?").getSQL().toLowerCase();
        assertEquals("update xxx set c1=? where c1<? and c2=?", sql);
    }


    @Test
    public void testDefaultUpdateStatement2() {
        String sql = sqlBuilder
                .update("content_store")
                    .set("usage", "?")
                    .set("timestamp", "?")
                    .where().condition("id", "integer", "=", "?").getSQL().toLowerCase();
        System.out.println(sql);
    }

    @Test
    public void testSybaseSelectStatement() {
        database.setDatabaseType(Database.Type.SYBASE);
        String sql = sqlBuilder
                .select().columns("c1", "c2", "c3").from("xxx")
                .where().condition("c1", "integer", "<", "?")
                .and().condition("c2", "integer", "=", "?").getSQL().toLowerCase();
        assertEquals("select c1, c2, c3 from xxx where c1<convert(integer,?) and c2=convert(integer,?)", sql);
    }

    @Test
    public void testPostgresSelectStatement() {
        database.setDatabaseType(Database.Type.POSTGRES);
        String sql = sqlBuilder
                .select().columns("c1", "c2", "c3").from("xxx")
                .where().condition("c1", "integer", "<", "?")
                .and().condition("c2", "integer", "=", "?").getSQL().toLowerCase();
        assertEquals("select c1, c2, c3 from xxx where c1<cast(? as integer) and c2=cast(? as integer)", sql);
    }

}
