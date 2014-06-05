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
package org.modeshape.connector.git;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.AbstractBinary;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

/**
 * A {@link BinaryValue} implementation used to read the content of a specific object ID from the supplied repository. This class
 * computes the {@link AbstractBinary#getMimeType() MIME type} lazily or upon serialization.
 */
public class GitBinaryValue extends ExternalBinaryValue {
    private static final long serialVersionUID = 1L;

    private transient ObjectLoader loader;

    public GitBinaryValue( ObjectId id,
                           ObjectLoader loader,
                           String sourceName,
                           String nameHint,
                           MimeTypeDetector mimeTypeDetector ) {
        super(new BinaryKey(id.getName()), sourceName, id.getName(), loader.getSize(), nameHint, mimeTypeDetector);
        this.loader = loader;
    }

    @Override
    protected InputStream internalStream() throws IOException {
        return new BufferedInputStream(loader.openStream());
    }
}
