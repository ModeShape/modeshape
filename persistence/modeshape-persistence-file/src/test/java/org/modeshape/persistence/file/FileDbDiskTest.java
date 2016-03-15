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

import org.modeshape.schematic.AbstractSchematicDBTest;
import org.modeshape.schematic.SchematicDb;

/**
 * Unit test for {@link FileDb} when data is only stored on disk.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class FileDbDiskTest extends AbstractSchematicDBTest {
    
    private static final SchematicDb DB = FileDb.onDisk(false, "target/fstest");

    @Override
    protected SchematicDb getDb() throws Exception {
        return DB;
    }

    @Override
    public void after() throws Exception {
        simulateTransaction(() -> {
            db.removeAll();
            return null;
        });
        super.after();
    }
}
