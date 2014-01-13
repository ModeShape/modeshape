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

/**
 * 
 */
public class BinaryStorage {

    private final EditableDocument binaryConfig;
    private CacheContainer cacheContainer;

    BinaryStorage( EditableDocument binaryConfig ) {
        this.binaryConfig = binaryConfig;
    }

    EditableDocument getBinaryConfiguration() {
        return binaryConfig;
    }

    CacheContainer getCacheContainer() {
        return cacheContainer;
    }

    void setCacheContainer( CacheContainer cacheContainer ) {
        this.cacheContainer = cacheContainer;
    }

    static BinaryStorage defaultStorage(String repositoryName, String dataDirPath) {
        // By default, store the binaries in the data directory ...
        EditableDocument binaries = Schematic.newDocument();
        binaries.set(RepositoryConfiguration.FieldName.TYPE, RepositoryConfiguration.FieldValue.BINARY_STORAGE_TYPE_FILE);
        binaries.set(RepositoryConfiguration.FieldName.DIRECTORY, dataDirPath + "/" + repositoryName + "/binaries");

        return new BinaryStorage(binaries);
    }

    public String getStoreName() {
        return getBinaryConfiguration().getString(RepositoryConfiguration.FieldName.BINARY_STORE_NAME);
    }

}
