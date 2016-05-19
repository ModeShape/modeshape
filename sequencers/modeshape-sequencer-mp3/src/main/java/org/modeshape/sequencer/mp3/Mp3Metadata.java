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

import static org.jaudiotagger.tag.FieldKey.ALBUM;
import static org.jaudiotagger.tag.FieldKey.ARTIST;
import static org.jaudiotagger.tag.FieldKey.COMMENT;
import static org.jaudiotagger.tag.FieldKey.TITLE;
import static org.jaudiotagger.tag.FieldKey.YEAR;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;

/**
 * Utility for extracting metadata from MP3 files.
 *
 * @deprecated starting with 5.1 this should be replaced with the Audio sequencer; this will be removed in the next major version
 */
@Deprecated
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
            fileOutputStream.close();
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
