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
package org.modeshape.connector.cmis;

import java.io.InputStream;
import java.util.UUID;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;

/**
 * {@link ExternalBinaryValue} implementation for the binary content exposed by the CMIS Connector.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 5.0
 */
public class CmisConnectorBinary extends ExternalBinaryValue {
    
    private final ContentStream contentStream;

    protected CmisConnectorBinary(ContentStream contentStream, 
                                  String sourceName, 
                                  String documentId, 
                                  MimeTypeDetector mimeTypeDetector) {
        // the internal key can be some random  string since this in an external binary....
        super(new BinaryKey(UUID.randomUUID().toString()), sourceName, documentId, contentStream.getLength(),
              contentStream.getFileName(), mimeTypeDetector);
        this.contentStream = contentStream;
    }

    @Override
    protected InputStream internalStream() throws Exception {
        return contentStream.getStream();
    }
}
