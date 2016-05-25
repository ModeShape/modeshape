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
package org.modeshape.sequencer.mp3;

import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.ALBUM;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.AUTHOR;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.COMMENT;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.METADATA_NODE;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.TITLE;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.YEAR;
import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;

/**
 * A sequencer that processes the binary content of an MP3 audio file, extracts the metadata for the file, and then writes that
 * audio metadata to the repository.
 * <p>
 * This sequencer produces data that corresponds to the following structure:
 * <ul>
 * <li><strong>mp3:metadata</strong> node of type <code>mp3:metadata</code>
 * <ul>
 * <li><strong>mp3:title</strong> - optional string property for the name of the audio file or recording</li>
 * <li><strong>mp3:author</strong> - optional string property for the author of the recording</li>
 * <li><strong>mp3:album</strong> - optional string property for the name of the album</li>
 * <li><strong>mp3:year</strong> - optional integer property for the year the recording as created</li>
 * <li><strong>mp3:comment</strong> - optional string property specifying a comment</li>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * 
 * @deprecated starting with 5.1 this should be replaced with the Audio sequencer; this will be removed in the next major version
 */
@Deprecated
public class Mp3MetadataSequencer extends Sequencer {

    public static final class MimeTypeConstants {
        public static final String MP3 = "audio/mpeg";
    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("mp3.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(MimeTypeConstants.MP3);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        Mp3Metadata metadata = null;
        try (InputStream stream = binaryValue.getStream()) {
            metadata = Mp3Metadata.instance(stream);
        }
        Node sequencedNode = outputNode;
        if (outputNode.isNew()) {
            outputNode.setPrimaryType(METADATA_NODE);
        } else {
            sequencedNode = outputNode.addNode(METADATA_NODE, METADATA_NODE);
        }

        sequencedNode.setProperty(TITLE, metadata.getTitle());
        sequencedNode.setProperty(AUTHOR, metadata.getAuthor());
        sequencedNode.setProperty(ALBUM, metadata.getAlbum());
        sequencedNode.setProperty(YEAR, metadata.getYear());
        sequencedNode.setProperty(COMMENT, metadata.getComment());

        return true;
    }
}
