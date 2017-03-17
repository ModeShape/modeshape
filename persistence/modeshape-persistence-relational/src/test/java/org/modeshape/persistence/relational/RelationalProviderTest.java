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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.modeshape.common.database.DatabaseType;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.ParsingException;
import org.modeshape.schematic.internal.annotation.FixFor;
import org.modeshape.schematic.internal.document.BasicDocument;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Test for {@link RelationalProvider}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RelationalProviderTest {
    
    @Test
    public void shouldReturnDefaultDbWhenNoExplicitConfigurationGiven() {
        BasicDocument configDocument = new BasicDocument(Schematic.TYPE_FIELD, RelationalDbConfig.ALIAS1);
        RelationalDb db = Schematic.getDb(configDocument);
        assertNotNull(db);
        assertEquals(RelationalDbConfig.DEFAULT_CONNECTION_URL, db.id());
        
        RelationalDbConfig config = db.config();
        assertNotNull(config);
        assertTrue(config.createOnStart());
        assertFalse(config.dropOnExit());
        assertEquals(RelationalDbConfig.DEFAULT_TABLE_NAME, config.tableName());
        assertEquals(RelationalDbConfig.DEFAULT_FETCH_SIZE, config.fetchSize());
        assertFalse(config.compress());
        
        DataSourceManager dsManager = db.dsManager();
        assertNotNull(dsManager);
        assertEquals(DatabaseType.Name.H2, dsManager.dbType().name());    
    }    
    
    @Test
    public void shouldReturnDbConfiguredFromDocument() throws ParsingException {
        RelationalDb db = Schematic.getDb(RelationalProviderTest.class.getClassLoader().getResourceAsStream("db-config-h2-full.json"));
        assertNotNull(db);
        assertEquals("jdbc:h2:mem:modeshape", db.id());

        RelationalDbConfig config = db.config();
        assertNotNull(config);
        assertFalse(config.createOnStart());
        assertTrue(config.dropOnExit());
        assertEquals("REPO", config.tableName());
        assertEquals(100, config.fetchSize());
        assertFalse(config.compress());

        DataSourceManager dsManager = db.dsManager();
        assertNotNull(dsManager);
        assertEquals(DatabaseType.Name.H2, dsManager.dbType().name());
        HikariDataSource dataSource = (HikariDataSource) dsManager.dataSource();
        assertEquals((int) Integer.valueOf(RelationalDbConfig.DEFAULT_MIN_IDLE), dataSource.getMinimumIdle());
        assertEquals((int) Integer.valueOf(RelationalDbConfig.DEFAULT_MAX_POOL_SIZE), dataSource.getMaximumPoolSize());
        assertEquals((long) Integer.valueOf(RelationalDbConfig.DEFAULT_IDLE_TIMEOUT), dataSource.getIdleTimeout());
    }
    
    @Test
    @FixFor("MODE-2674")
    public void shouldAllowCustomHikariPassthroughProperties() throws ParsingException {
        RelationalDb db = Schematic.getDb(RelationalProviderTest.class.getClassLoader().getResourceAsStream("db-config-custom-props.json"));
        assertNotNull(db);
        DataSourceManager dsManager = db.dsManager();
        HikariDataSource dataSource = (HikariDataSource) dsManager.dataSource();
        assertEquals(4, dataSource.getMinimumIdle());        
        assertEquals(4000, dataSource.getLeakDetectionThreshold());        
        assertEquals(5, dataSource.getMaximumPoolSize());        
        assertFalse(dataSource.isReadOnly());        
        assertEquals("testPool", dataSource.getPoolName());        
    }
}
