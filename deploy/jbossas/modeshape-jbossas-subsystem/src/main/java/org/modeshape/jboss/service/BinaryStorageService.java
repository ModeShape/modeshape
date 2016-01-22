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

import org.modeshape.schematic.document.EditableDocument;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class BinaryStorageService implements Service<BinaryStorage> {

    private final InjectedValue<String> binaryStorageBasePathInjector = new InjectedValue<String>();

    private final BinaryStorage binaryStorage;

    public static BinaryStorageService createDefault() {
        return new BinaryStorageService(BinaryStorage.defaultConfig());        
    }
    
    public static BinaryStorageService createWithConfiguration( EditableDocument binaryConfig ) {
        return new BinaryStorageService(new BinaryStorage(binaryConfig));
    } 
    
    private BinaryStorageService(BinaryStorage storage) {
        this.binaryStorage = storage;
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

    @Override
    public BinaryStorage getValue() throws IllegalStateException, IllegalArgumentException {
        assert binaryStorage != null;
        return binaryStorage;
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
            String trashPath = binaryConfig.getString(FieldName.TRASH_DIRECTORY);
            if (trashPath != null) {
                binaryConfig.set(FieldName.TRASH_DIRECTORY, binaryStorageBasePath + trashPath);                
            }
        }
    }

    @Override
    public void stop( StopContext arg0 ) {
        // Nothing to do
    }
}
