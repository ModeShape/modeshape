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
package org.modeshape.persistence.file;

import org.modeshape.common.logging.Logger;
import org.modeshape.schematic.SchematicDbProvider;
import org.modeshape.schematic.document.Document;

/**
 * {@link org.modeshape.schematic.SchematicDbProvider} implementation for storing repository data on the FS and in memory.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class FileDbProvider implements SchematicDbProvider<FileDb> {

    public static final String TYPE_MEM = "mem";
    public static final String TYPE_FILE = "file";
    public static final String PATH_FIELD = "path";
    public static final String COMPRESS_FIELD = "compress";
    
    private static final Logger LOGGER = Logger.getLogger(FileDbProvider.class);

    @Override
    public FileDb getDB( String type, Document configuration ) {
        if (TYPE_MEM.equalsIgnoreCase(type)) {
            LOGGER.debug("Returning new in-memory schematic DB...");
            return FileDb.inMemory(configuration.getBoolean(COMPRESS_FIELD, false));
        } else if (TYPE_FILE.equalsIgnoreCase(type)) {
            boolean compress = configuration.getBoolean(COMPRESS_FIELD, true);
            String path = configuration.getString(PATH_FIELD, null);
            LOGGER.debug("Returning new disk schematic DB at {0}...", path);
            return FileDb.onDisk(compress, path);
        }
        return null;
    }
}
