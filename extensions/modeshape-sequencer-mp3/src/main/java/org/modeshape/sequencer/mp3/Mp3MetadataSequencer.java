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
package org.modeshape.sequencer.mp3;

import java.io.InputStream;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;

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
public class Mp3MetadataSequencer implements StreamSequencer {

    /**
     * {@inheritDoc}
     * 
     * @see StreamSequencer#sequence(InputStream, SequencerOutput, StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        Mp3Metadata metadata = Mp3Metadata.instance(stream);

        if (metadata != null) {
            // Place the image metadata into the output map ...
            Path metadataNode = context.getValueFactories().getPathFactory().createRelativePath(Mp3MetadataLexicon.METADATA_NODE);

            output.setProperty(metadataNode, JcrLexicon.PRIMARY_TYPE, "mp3:metadata");
            output.setProperty(metadataNode, Mp3MetadataLexicon.TITLE, metadata.getTitle());
            output.setProperty(metadataNode, Mp3MetadataLexicon.AUTHOR, metadata.getAuthor());
            output.setProperty(metadataNode, Mp3MetadataLexicon.ALBUM, metadata.getAlbum());
            output.setProperty(metadataNode, Mp3MetadataLexicon.YEAR, metadata.getYear());
            output.setProperty(metadataNode, Mp3MetadataLexicon.COMMENT, metadata.getComment());
        }
    }
}
