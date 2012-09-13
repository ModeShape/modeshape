/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.jcr.value.binary;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.modeshape.jcr.store.DataSourceConfig;

/**
 * @author kulikov
 */
public class DatabaseBinaryStoreTest extends AbstractBinaryStoreTest {

    private static DatabaseBinaryStore store;

    @BeforeClass
    public static void beforeClass() {
        DataSourceConfig dbConfig = new DataSourceConfig();
        String driver = dbConfig.getDriverClassName();
        String url = dbConfig.getUrl();
        String username = dbConfig.getUsername();
        String password = dbConfig.getPassword();
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
}
