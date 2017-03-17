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

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.util.StringUtil;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;

/**
 * Class which holds the configuration options for {@link RelationalDb}. 
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public final class RelationalDbConfig {

    public static final String ALIAS1 = "db";
    public static List<String> ALIASES = Arrays.asList(ALIAS1, "database");

    public static final String DROP_ON_EXIT = "dropOnExit";
    public static final String CREATE_ON_START = "createOnStart";
    public static final String TABLE_NAME = "tableName";
    public static final String FETCH_SIZE = "fetchSize";
    public static final String COMPRESS = "compress";
    public static final String CONNECTION_URL = "connectionUrl";
    public static final String DRIVER = "driver";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String DATASOURCE_JNDI_NAME = "dataSourceJndiName";
    public static final String POOL_SIZE = "poolSize";
    
    protected static final List<String> ALL_FIELDS = Arrays.asList(Schematic.TYPE_FIELD, DROP_ON_EXIT, CREATE_ON_START, TABLE_NAME,
                                                                   FETCH_SIZE, COMPRESS, CONNECTION_URL, DRIVER, USERNAME,
                                                                   PASSWORD, DATASOURCE_JNDI_NAME, POOL_SIZE);
    
    protected static final String DEFAULT_CONNECTION_URL = "jdbc:h2:mem:modeshape;DB_CLOSE_DELAY=0;MVCC=TRUE";
    protected static final String DEFAULT_DRIVER = "org.h2.Driver";
    protected static final String DEFAULT_USERNAME = "sa";
    protected static final String DEFAULT_PASSWORD = "";
    protected static final String DEFAULT_TABLE_NAME = "MODESHAPE_REPOSITORY";
    protected static final String DEFAULT_MAX_POOL_SIZE = "5";
    protected static final String DEFAULT_MIN_IDLE = "1";
    protected static final String DEFAULT_IDLE_TIMEOUT = String.valueOf(TimeUnit.MINUTES.toMillis(1));
    protected static final int DEFAULT_FETCH_SIZE = 1000;
    
    private final Document config;
    private final boolean createOnStart;
    private final boolean dropOnExit;
    private final String tableName;
    private final int fetchSize;
    private final boolean compress;
    private final String connectionUrl;
    private final String datasourceJNDIName; 
    
    protected RelationalDbConfig(Document document) {
        this.config = document;
        this.datasourceJNDIName = config.getString(DATASOURCE_JNDI_NAME, null);
        this.createOnStart = propertyAsBoolean(config, CREATE_ON_START, true);
        this.dropOnExit = propertyAsBoolean(config, DROP_ON_EXIT, false);
        this.tableName = config.getString(TABLE_NAME, DEFAULT_TABLE_NAME);
        this.fetchSize = propertyAsInt(config, FETCH_SIZE, DEFAULT_FETCH_SIZE);
        this.compress = propertyAsBoolean(config, COMPRESS, false);
        this.connectionUrl = config.getString(CONNECTION_URL, DEFAULT_CONNECTION_URL);
    }

    protected boolean isDatasourceManaged() {
        return !StringUtil.isBlank(datasourceJNDIName);
    }
    
    protected Properties datasourceConfig() {
        Properties datasourceCfg = new Properties();
        EditableDocument localCopy = config.edit(true);

        // remove the generic configuration fields
        ALL_FIELDS.forEach(localCopy::remove);
    
        // convert each of the properties to their Hikari names 
        datasourceCfg.setProperty("jdbcUrl", connectionUrl);
        datasourceCfg.setProperty("driverClassName", config.getString(DRIVER, DEFAULT_DRIVER));
        datasourceCfg.setProperty("username", config.getString(USERNAME, DEFAULT_USERNAME));
        datasourceCfg.setProperty("password", config.getString(PASSWORD, DEFAULT_PASSWORD));
        datasourceCfg.setProperty("maximumPoolSize", propertyAsString(config, POOL_SIZE, DEFAULT_MAX_POOL_SIZE));
        datasourceCfg.setProperty("minimumIdle", DEFAULT_MIN_IDLE);
        datasourceCfg.setProperty("idleTimeout", DEFAULT_IDLE_TIMEOUT);
    
        // pass all the other fields as they are (this will also overwrite any of the previous values if they are explicitly configured)
        localCopy.fields().forEach(field -> datasourceCfg.setProperty(field.getName(), field.getValue().toString()));   
        return datasourceCfg;
    }
    
    protected String name() {
        return datasourceJNDIName != null ? datasourceJNDIName : connectionUrl;   
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
    
    private String propertyAsString(Document document, String fieldName, String defaultValue) {
        Object value = document.get(fieldName);
        return value == null ? defaultValue : value.toString();
    }
    
    private int propertyAsInt(Document document, String propertyName, int defaultValue) {
        Object value = document.get(propertyName);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else {
            return Integer.valueOf(value.toString());
        }
    }
    
    private boolean propertyAsBoolean(Document document, String propertyName, boolean defaultValue) {
        Object value = document.get(propertyName);
        if (value == null) {
            return defaultValue;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else {
            return Boolean.valueOf(value.toString());
        }
    }
}
