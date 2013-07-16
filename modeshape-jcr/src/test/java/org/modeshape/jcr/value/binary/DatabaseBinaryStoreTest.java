/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.jcr.value.binary;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.modeshape.jcr.store.DataSourceConfig;

/**
 * @author kulikov
 */
public class DatabaseBinaryStoreTest extends AbstractBinaryStoreTest {

    private static final DataSourceConfig DB_CONFIG = new DataSourceConfig();
    private static DatabaseBinaryStore store;

    @BeforeClass
    public static void beforeClass() {
        String driver = DB_CONFIG.getDriverClassName();
        String url = DB_CONFIG.getUrl();
        String username = DB_CONFIG.getUsername();
        String password = DB_CONFIG.getPassword();
        store = new DatabaseBinaryStore(driver, url, username, password);
        store.start();
    }

    @AfterClass
    public static void afterClass() {
        store.shutdown();
    }

    @Override
    protected BinaryStore getBinaryStore() {
        return store;
    }

    @Override
    public void shouldStoreZeroLengthBinary() throws BinaryStoreException, IOException {
        if (DB_CONFIG.getDriverClassName().toLowerCase().contains("oracle")) {
            //Oracle does not store 0 sized byte arrays
            return;
        }
        super.shouldStoreZeroLengthBinary();
    }
}
