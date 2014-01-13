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
package org.modeshape.jboss.service;

import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.Default;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

/**
 * 
 */
public class IndexStorage {

    private final EditableDocument queryConfig;
    /**
     * Optional member, which will be set only if an ISPN cache is configured
     */
    private CacheContainer cacheContainer;

    IndexStorage( EditableDocument queryConfig ) {
        this.queryConfig = queryConfig;
    }

    void setDefaultValuesForIndexStorage( String dataDirPath ) {
        if (isEnabled()) {
            EditableDocument indexStorage = queryConfig.getOrCreateDocument(RepositoryConfiguration.FieldName.INDEX_STORAGE);
            indexStorage.set(RepositoryConfiguration.FieldName.TYPE, RepositoryConfiguration.FieldValue.INDEX_STORAGE_FILESYSTEM);
            indexStorage.set(RepositoryConfiguration.FieldName.INDEX_STORAGE_LOCATION, dataDirPath + "/indexes");
        }
    }

    boolean useDefaultValuesForIndexStorage() {
        return !queryConfig.containsField(RepositoryConfiguration.FieldName.INDEX_STORAGE);
    }

    public boolean isEnabled() {
        return this.queryConfig.getBoolean(FieldName.QUERY_ENABLED, Default.QUERY_ENABLED);
    }

    /**
     * @return the repository's query configuration
     */
    public EditableDocument getQueryConfiguration() {
        return isEnabled() ? queryConfig : Schematic.newDocument(FieldName.QUERY_ENABLED, false);
    }

    CacheContainer getCacheContainer() {
        return cacheContainer;
    }

    void setCacheContainer( CacheContainer cacheContainer ) {
        this.cacheContainer = cacheContainer;
    }
}
