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
import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class BinaryStorageService implements Service<BinaryStorage> {

    private final Injector<CacheContainer> binaryManagerInjector = new Injector<CacheContainer>() {
        @Override
        public void inject( CacheContainer value ) throws InjectionException {
            binaryStorage().setCacheContainer(value);
        }

        @Override
        public void uninject() {
            binaryStorage().setCacheContainer(null);
        }
    };
    private final InjectedValue<String> binaryStorageBasePathInjector = new InjectedValue<String>();
    private final InjectedValue<String> dataDirectoryPathInjector = new InjectedValue<String>();

    private final String repositoryName;
    private final BinaryStorage binaryStorage;

    public BinaryStorageService( String repositoryName,
                                 EditableDocument binaryConfig ) {
        this.repositoryName = repositoryName;
        this.binaryStorage = new BinaryStorage(binaryConfig);
    }

    public BinaryStorageService( String repositoryName ) {
        this.repositoryName = repositoryName;
        this.binaryStorage = null;
    }

    protected final BinaryStorage binaryStorage() {
        return binaryStorage;
    }

    private String getBinaryStorageBasePath() {
        return appendDirDelim(binaryStorageBasePathInjector.getOptionalValue());
    }

    private String appendDirDelim( String value ) {
        if (value != null && !value.endsWith("/")) {
            value = value + "/";
        }
        return value;
    }

    /**
     * @return the injector used to set the path where the binary files are to be stored
     */
    public InjectedValue<String> getBinaryStorageBasePathInjector() {
        return binaryStorageBasePathInjector;
    }

    /**
     * @return the injector used to set the CacheManager reference used for binary storage
     */
    public Injector<CacheContainer> getBinaryCacheManagerInjector() {
        return binaryManagerInjector;
    }

    @Override
    public BinaryStorage getValue() throws IllegalStateException, IllegalArgumentException {
        return binaryStorage != null ? binaryStorage : BinaryStorage.defaultStorage(repositoryName,
                                                                                    dataDirectoryPathInjector.getValue());
    }

    /**
     * @return the injector used to retrieve the path to the data directory
     */
    public InjectedValue<String> getDataDirectoryPathInjector() {
        return dataDirectoryPathInjector;
    }

    @Override
    public void start( StartContext arg0 ) {
        // Not much to do, since we've already captured the properties for the index storage.
        // When this is injected into the RepositoryService, the RepositoryService will use the
        // properties to update the configuration.

        // All we need to do is update the relative paths and make them absolute, given the absolute paths that are injected ...

        String binaryStorageBasePath = getBinaryStorageBasePath();
        if (binaryStorageBasePath != null) {
            EditableDocument binaryConfig = binaryStorage.getBinaryConfiguration();
            // Set the binary storage directory ...
            String relativePath = binaryConfig.getString(FieldName.DIRECTORY);
            if (relativePath != null) {
                binaryConfig.set(FieldName.DIRECTORY, binaryStorageBasePath + relativePath);
            }
        }
    }

    @Override
    public void stop( StopContext arg0 ) {
        // Nothing to do
    }
}
