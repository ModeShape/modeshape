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
package org.modeshape.persistence.relational;

import org.modeshape.schematic.document.Document;

/**
 * Class which holds the configuration options for {@link RelationalDb}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public final class RelationalDbConfig {

    public static final String DROP_ON_EXIT = "dropOnExit";
    
    protected static final String CREATE_ON_START = "createOnStart";
    protected static final String TABLE_NAME = "tableName";
    protected static final String FETCH_SIZE = "fetchSize";
    protected static final String COMPRESS = "compress";
    protected static final String CACHE_SIZE = "cacheSize";
    protected static final String CONNECTION_URL = "connectionUrl";
    protected static final String DRIVER = "driver";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String DATASOURCE_JNDI_NAME = "datasourceJNDI";
    protected static final String THREAD_POOL_SIZE = "threadPoolSize";

    protected static final String DEFAULT_CONNECTION_URL = "jdbc:h2:mem:modeshape;DB_CLOSE_DELAY=0;";
    protected static final String DEFAULT_DRIVER = "org.h2.Driver";
    protected static final String DEFAULT_USERNAME = "sa";
    protected static final String DEFAULT_PASSWORD = "";
    protected static final String DEFAULT_TABLE_NAME = "MODESHAPE_REPOSITORY";
    protected static final int DEFAULT_FETCH_SIZE = 1000;
    protected static final int DEFAULT_CACHE_SIZE = 1000;
    protected static final int DEFAULT_THREAD_POOL_SIZE = 1000;
  
    private final boolean createOnStart;
    private final boolean dropOnExit;
    private final String tableName;
    private final int fetchSize;
    private final boolean compress;
    private final int cacheSize;
    private final String connectionUrl;
    private final String driver;
    private final String username;
    private final String password;
    private final String datasourceJNDIName; 
    private final int threadPoolSize;

    protected RelationalDbConfig(Document document) {
        this.connectionUrl = document.getString(CONNECTION_URL, DEFAULT_CONNECTION_URL);
        this.driver = document.getString(DRIVER, DEFAULT_DRIVER);
        this.username = document.getString(USERNAME, DEFAULT_USERNAME);
        this.password = document.getString(PASSWORD, DEFAULT_PASSWORD);
        this.datasourceJNDIName = document.getString(DATASOURCE_JNDI_NAME, null);
        this.createOnStart = document.getBoolean(CREATE_ON_START, true);
        this.dropOnExit = document.getBoolean(DROP_ON_EXIT, false);
        this.tableName = document.getString(TABLE_NAME, DEFAULT_TABLE_NAME);
        this.fetchSize = document.getInteger(FETCH_SIZE, DEFAULT_FETCH_SIZE);
        this.compress = document.getBoolean(COMPRESS, true);
        this.cacheSize = document.getInteger(CACHE_SIZE, DEFAULT_FETCH_SIZE);
        this.threadPoolSize = document.getInteger(THREAD_POOL_SIZE, DEFAULT_THREAD_POOL_SIZE);
    }

    protected String connectionUrl() {
        return connectionUrl;
    }

    protected String driver() {
        return driver;
    }

    protected String password() {
        return password;
    }

    protected String username() {
        return username;
    }

    protected String datasourceJNDIName() {
        return datasourceJNDIName;
    }

    protected boolean createOnStart() {
        return createOnStart;
    }

    protected boolean dropOnExit() {
        return dropOnExit;
    }

    protected String tableName() {
        return tableName;
    }

    protected int fetchSize() {
        return fetchSize;
    }

    protected boolean compress() {
        return compress;
    }

    protected int cacheSize() {
        return cacheSize;
    }
    
    protected int threadPoolSize() { 
        return threadPoolSize; 
    }
    
    @Override
    public String toString() {
        return "RelationalDbConfig[" +
               "createOnStart=" + createOnStart +
               ", dropOnExit=" + dropOnExit +
               ", tableName='" + tableName + '\'' +
               ", fetchSize=" + fetchSize +
               ", compress=" + compress +
               ", cacheSize=" + cacheSize +
               ", connectionUrl='" + connectionUrl + '\'' +
               ", driver='" + driver + '\'' +
               ", username='" + username + '\'' +
               ", password='" + password + '\'' +
               ", datasourceJNDIName='" + datasourceJNDIName + '\'' +
               ']';
    }
}
