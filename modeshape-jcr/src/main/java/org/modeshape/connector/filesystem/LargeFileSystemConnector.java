/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
