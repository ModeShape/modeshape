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
package org.modeshape.jcr.mimetype;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import org.apache.tika.metadata.Metadata;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.SelfClosingInputStream;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.Environment;

/**
 * A base class for the {@link MimeTypeDetector}s that use the Tika library.
 */
@Immutable
@ThreadSafe
public abstract class TikaMimeTypeDetector implements MimeTypeDetector {

    protected final Logger logger;
    
    protected TikaMimeTypeDetector( Environment environment ) {
        assert environment != null;
        this.logger = Logger.getLogger(getClass());
        // the extra classpath entry is the package name of the tika extractor, so it can be located inside AS7 (see
        // RepositoryService)
        ClassLoader loader = environment.getClassLoader(getClass().getClassLoader(), "org.modeshape.extractor.tika");
        logger.debug("Initializing mime-type detector...");
        initDetector(loader);
        logger.debug("Successfully initialized detector: {0}", getClass().getName());
    }

    @Override
    public String mimeTypeOf( final String name,
                              final Binary binaryValue ) throws RepositoryException, IOException {
        Metadata metadata = new Metadata();
        if (!StringUtil.isBlank(name)) {
            metadata.set(Metadata.RESOURCE_NAME_KEY, name);
        }

        if (binaryValue == null) {
            return name == null ? null : detect(null, metadata);
        }

        InputStream stream = binaryValue.getStream();
        if (stream instanceof SelfClosingInputStream) {
            //we need to avoid the SelfClosingInputStream because Tika will read and mark from this stream multiple times
            stream = ((SelfClosingInputStream)stream).wrappedStream();
        }
        return detect(stream, metadata);
    }

    protected abstract void initDetector( ClassLoader loader );

    protected abstract String detect(InputStream inputStream, Metadata metadata);
}
