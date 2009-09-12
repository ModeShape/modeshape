/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.mp3;

import java.io.InputStream;
import org.jboss.dna.graph.sequencer.SequencerOutput;
import org.jboss.dna.graph.sequencer.StreamSequencer;
import org.jboss.dna.graph.sequencer.StreamSequencerContext;

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

    public static final String METADATA_NODE = "mp3:metadata";
    public static final String MP3_PRIMARY_TYPE = "jcr:primaryType";
    public static final String MP3_TITLE = "mp3:title";
    public static final String MP3_AUTHOR = "mp3:author";
    public static final String MP3_ALBUM = "mp3:album";
    public static final String MP3_YEAR = "mp3:year";
    public static final String MP3_COMMENT = "mp3:comment";

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
            output.setProperty(METADATA_NODE, MP3_PRIMARY_TYPE, "mp3:metadata");
            output.setProperty(METADATA_NODE, MP3_TITLE, metadata.getTitle());
            output.setProperty(METADATA_NODE, MP3_AUTHOR, metadata.getAuthor());
            output.setProperty(METADATA_NODE, MP3_ALBUM, metadata.getAlbum());
            output.setProperty(METADATA_NODE, MP3_YEAR, metadata.getYear());
            output.setProperty(METADATA_NODE, MP3_COMMENT, metadata.getComment());
        }
    }
}
