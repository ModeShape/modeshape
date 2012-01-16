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

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import static org.jaudiotagger.tag.FieldKey.*;
import org.jaudiotagger.tag.Tag;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;

/**
 * Utility for extracting metadata from MP3 files.
 */
public class Mp3Metadata {

    private String title;
    private String author;
    private String album;
    private String year;
    private String comment;

    private Mp3Metadata() {

    }

    public static Mp3Metadata instance( InputStream stream ) throws Exception {

        Mp3Metadata me = null;
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("modeshape-sequencer-mp3", ".mp3");
            FileOutputStream fileOutputStream = new FileOutputStream(tmpFile);
            byte[] b = new byte[1024];
            while (stream.read(b) != -1) {
                fileOutputStream.write(b);
            }
            AudioFileIO.logger.getParent().setLevel(Level.OFF);
            AudioFile f = AudioFileIO.read(tmpFile);
            Tag tag = f.getTag();

            me = new Mp3Metadata();

            me.author = tag.getFirst(ARTIST);
            me.album = tag.getFirst(ALBUM);
            me.title = tag.getFirst(TITLE);
            me.comment = tag.getFirst(COMMENT);
            me.year = tag.getFirst(YEAR);

        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
        return me;

    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getAlbum() {
        return album;
    }

    public String getYear() {
        return year;
    }

    public String getComment() {
        return comment;
    }

}
