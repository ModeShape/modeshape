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
import org.infinispan.schematic.document.EditableDocument;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class IndexStorageService implements Service<IndexStorage> {

    private static final Logger LOG = Logger.getLogger(IndexStorageService.class.getPackage().getName());

    private final InjectedValue<String> indexStorageBasePathInjector = new InjectedValue<String>();
    private final InjectedValue<String> indexStorageSourceBasePathInjector = new InjectedValue<String>();
    private final InjectedValue<String> dataDirectoryPathInjector = new InjectedValue<String>();
    private final InjectedValue<CacheContainer> cacheContainerInjectedValue = new InjectedValue<CacheContainer>();

    private final IndexStorage indexStorage;

    public IndexStorageService( EditableDocument queryConfig ) {
        this.indexStorage = new IndexStorage(queryConfig);
    }

    private String getIndexStorageBasePath() {
        return appendDirDelim(indexStorageBasePathInjector.getOptionalValue());
    }

    private String getIndexStorageSourceBasePath() {
        return appendDirDelim(indexStorageSourceBasePathInjector.getOptionalValue());
    }

    private String appendDirDelim( String value ) {
        if (value != null && !value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    /**
     * @return the injector used to set the path where the indexes are to be stored
     */
    public InjectedValue<String> getIndexStorageBasePathInjector() {
        return indexStorageBasePathInjector;
    }

    /**
     * @return the injector used to set the path where the source indexes are to be stored
     */
    public InjectedValue<String> getIndexStorageSourceBasePathInjector() {
        return indexStorageSourceBasePathInjector;
    }

    /**
     * @return the injector used to set the data directory for this repository
     */
    public InjectedValue<String> getDataDirectoryPathInjector() {
        return dataDirectoryPathInjector;
    }

    /**
     * @return the injector used to set a custom ISPN cache container
     */
    public InjectedValue<CacheContainer> getCacheContainerInjectedValue() {
        return cacheContainerInjectedValue;
    }

    @Override
    public IndexStorage getValue() throws IllegalStateException, IllegalArgumentException {
        if (indexStorage.isEnabled()) {
            if (indexStorage.useDefaultValuesForIndexStorage()) {
                // use optional value because this service is dynamically queried from AbstractAddIndexStorage
                indexStorage.setDefaultValuesForIndexStorage(dataDirectoryPathInjector.getOptionalValue());
            }
        }
        indexStorage.setCacheContainer(cacheContainerInjectedValue.getOptionalValue());
        return indexStorage;
    }

    @Override
    public void start( StartContext arg0 ) {
        // Not much to do, since we've already captured the properties for the index storage.
        // When this is injected into the RepositoryService, the RepositoryService will use the
        // properties to update the configuration.

        if (indexStorage.isEnabled()) {
            // All we need to do is update the relative paths and make them absolute,
            // given the absolute paths that are injected ...

            String indexStorageBasePath = getIndexStorageBasePath();
            if (indexStorageBasePath != null) {
                // Set the index storage directory ...
                EditableDocument indexStorage = this.indexStorage.getQueryConfiguration().getDocument(FieldName.INDEX_STORAGE);
                String relativePath = indexStorage.getString(FieldName.INDEX_STORAGE_LOCATION);
                if (relativePath != null) {
                    indexStorage.set(FieldName.INDEX_STORAGE_LOCATION, indexStorageBasePath + relativePath);
                }
            }

            String indexStorageSourceBasePath = getIndexStorageSourceBasePath();
            if (indexStorageSourceBasePath != null) {
                // Set the index source storage directory ...
                EditableDocument indexStorage = this.indexStorage.getQueryConfiguration().getDocument(FieldName.INDEX_STORAGE);
                String relativePath = indexStorage.getString(FieldName.INDEX_STORAGE_SOURCE_LOCATION);
                if (relativePath != null) {
                    indexStorage.set(FieldName.INDEX_STORAGE_SOURCE_LOCATION, indexStorageSourceBasePath + relativePath);
                }
            }
        } else {
            LOG.warnv("Queries are disabled for the repository, so all configured index storage will be disabled.");
        }
    }

    @Override
    public void stop( StopContext arg0 ) {
        // Nothing to do
    }
}
