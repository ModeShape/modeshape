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
package org.modeshape.jcr.value.binary;

import java.io.File;
import org.junit.BeforeClass;
import org.modeshape.common.util.FileUtil;

public class TransientBinaryStoreTest extends FileSystemBinaryStoreTest {

    @BeforeClass
    public static void beforeEach() {
        store = TransientBinaryStore.get();
        directory = TransientBinaryStore.TRANSIENT_STORE_DIRECTORY;
        FileUtil.delete(directory);
        directory.mkdirs();
        trash = new File(directory, FileSystemBinaryStore.TRASH_DIRECTORY_NAME);
        store.setMinimumBinarySizeInBytes(MIN_BINARY_SIZE);
        store.setMimeTypeDetector(DEFAULT_DETECTOR);
        print = false;
    }
}
