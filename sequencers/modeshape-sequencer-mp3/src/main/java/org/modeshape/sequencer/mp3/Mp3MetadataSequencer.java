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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.mp3;

import javax.jcr.*;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import static org.modeshape.sequencer.mp3.Mp3MetadataLexicon.*;
import java.io.IOException;


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
 */
public class Mp3MetadataSequencer extends Sequencer {

    @Override
    public void initialize( NamespaceRegistry registry, NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("mp3.cnd", nodeTypeManager, true);
    }

    @Override
    public boolean execute( Property inputProperty, Node outputNode, Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        try {
            Mp3Metadata metadata = Mp3Metadata.instance(binaryValue.getStream());
            Node sequencedNode = outputNode;
            if (outputNode.isNew()) {
                outputNode.setPrimaryType(METADATA_NODE);
            }
            else {
                sequencedNode = outputNode.addNode(METADATA_NODE, METADATA_NODE);
            }
            
            sequencedNode.setProperty(TITLE, metadata.getTitle());
            sequencedNode.setProperty(AUTHOR, metadata.getAuthor());
            sequencedNode.setProperty(ALBUM, metadata.getAlbum());
            sequencedNode.setProperty(YEAR, metadata.getYear());
            sequencedNode.setProperty(COMMENT, metadata.getComment());

            return true;

        } catch (Exception e) {
            getLogger().error("Cannot sequence mp3 content ", e);
            return false;
        }
    }
}
