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
package org.modeshape.jcr;

import java.util.UUID;
import org.modeshape.persistence.relational.RelationalDbConfig;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;

/**
 * {@link Environment} implementation used for testing.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class TestingEnvironment extends LocalEnvironment {
    
    @Override
    public Document defaultPersistenceConfiguration() {
        return super.defaultPersistenceConfiguration();
    }
    
    protected Document dbPersistenceConfiguration() {
        EditableDocument config = super.defaultPersistenceConfiguration().edit(false);
        //use a random mem db each time, to avoid possible conflicts....
        config.setString(RepositoryConfiguration.FieldName.TYPE, RelationalDbConfig.ALIAS1);
        config.setString(RelationalDbConfig.CONNECTION_URL, "jdbc:h2:mem:" + UUID.randomUUID().toString() + ";DB_CLOSE_DELAY=0");
        config.setBoolean(RelationalDbConfig.DROP_ON_EXIT, true);
        return config;
    }
}
