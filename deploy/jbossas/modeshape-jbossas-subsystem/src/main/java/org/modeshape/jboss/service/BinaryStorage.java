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

import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * 
 */
public class BinaryStorage {

    private final EditableDocument binaryConfig;
    
    BinaryStorage( EditableDocument binaryConfig ) {
        this.binaryConfig = binaryConfig;
    }

    EditableDocument getBinaryConfiguration() {
        return binaryConfig;
    }

    /**
     * Creates a default storage configuration, which will be used whenever no specific binary store is configured.
     * @return a {@link org.modeshape.jboss.service.BinaryStorage} instance, never {@code null}
     */
    static BinaryStorage defaultConfig() {
        // By default binaries are not stored on disk
        EditableDocument binaries = Schematic.newDocument();
        binaries.set(RepositoryConfiguration.FieldName.TYPE, RepositoryConfiguration.FieldValue.BINARY_STORAGE_TYPE_TRANSIENT);
        return new BinaryStorage(binaries);
    }
}
