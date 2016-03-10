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
import org.modeshape.common.util.StringUtil;
import org.modeshape.schematic.SchematicDbProvider;
import org.modeshape.schematic.document.Document;

/**
 * {@link org.modeshape.schematic.SchematicDbProvider} implementation for storing repository data on the FS and in memory.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class FileDbProvider implements SchematicDbProvider<FileDb> {
    
    protected static final String PATH_PARAM = "path"; 
    protected static final String TYPE_MEM = "mem"; 
    protected static final String TYPE_FILE = "file"; 
    
    private static final Logger LOGGER = Logger.getLogger(FileDbProvider.class);

    @Override
    public FileDb getDB( String type, Document configuration ) {
        if (TYPE_MEM.equalsIgnoreCase(type)) {
            LOGGER.debug("Returning new in-memory schematic DB");
            return new FileDb(null);
        } else if (TYPE_FILE.equalsIgnoreCase(type)) {
            String path = configuration.getString(PATH_PARAM);
            if (StringUtil.isBlank(path)) {
                throw new FileProviderException("The 'path' configuration parameter is required by the FS persistence provider");
            }
            LOGGER.debug("Returning new FS schematic DB which will store data at '{0}'", path);
            return new FileDb(path);
        }
        return null;
    }
}
