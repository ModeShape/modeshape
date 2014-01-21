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
package org.modeshape.connector.filesystem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.jcr.value.binary.FileUrlBinaryValue;
import org.modeshape.jcr.value.binary.UrlBinaryValue;

public class LargeFileSystemConnector extends FileSystemConnector {

    /**
     * Utility method to create a {@link BinaryValue} object for the given file overriding FileSystemConnector. Subclasses should
     * rarely override this method, since the {@link UrlBinaryValue} will be applicable in most situations, but in this subclass
     * is overriding using {@link FileUrlBinaryValue} to lazily compute a contentBased Hash when the key is a URI based Hash.
     * Option contentBasedSha1=false should only be used in "write-once" directories as modifying the file will not change stored
     * hash
     * 
     * @param file the file for which the {@link BinaryValue} is to be created; never null
     * @return the binary value; never null
     * @throws IOException if there is an error creating the value
     */
    @Override
    protected ExternalBinaryValue createBinaryValue( File file ) throws IOException {
        URL content = createUrlForFile(file);
        if (contentBasedSha1()) {
            return new UrlBinaryValue(sha1(file), getSourceName(), content, file.length(), file.getName(), getMimeTypeDetector());
        }
        return new FileUrlBinaryValue(sha1(file), getSourceName(), content, file.length(), file.getName(), getMimeTypeDetector());
    }
}
