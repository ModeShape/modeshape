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

package org.modeshape.sequencer.xsd;

import static org.modeshape.sequencer.xsd.XsdLexicon.SCHEMA_DOCUMENT;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.sequencer.sramp.AbstractSrampSequencer;

/**
 * A sequencer that processes and extract the schema object model from XML Schema Document files.
 */
public class XsdSequencer extends AbstractSrampSequencer {

    public static final class MimeTypeConstants {
        public static final String APPLICATION_XML = "application/xml";
        public static final String TEXT_XML = "text/xml";

        /** This is not a valid MIME type, but we're using it in case other people use it */
        @Deprecated
        public static final String XSD = "application/xsd";
        @Deprecated
        public static final String XSD_XML = "application/xsd+xml";
    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);
        registerNodeTypes("xsd.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(MimeTypeConstants.XSD, // just in case clients set these mime type
                                 MimeTypeConstants.XSD_XML, // on nodes to be sequenced
                                 MimeTypeConstants.APPLICATION_XML,
                                 MimeTypeConstants.TEXT_XML);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        if (outputNode.isNew()) {
            outputNode.setPrimaryType(SCHEMA_DOCUMENT);
        } else {
            outputNode = outputNode.addNode(SCHEMA_DOCUMENT, SCHEMA_DOCUMENT);
        }
        try (InputStream stream = binaryValue.getStream()) {
            new XsdReader(context).read(stream, outputNode);
        }
        return true;
    }
}
