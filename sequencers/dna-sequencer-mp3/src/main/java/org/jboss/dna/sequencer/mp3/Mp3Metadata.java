package org.jboss.dna.sequencer.mp3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.logging.Level;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.Tag;

/**
 * Utility for extracting metadata from MP3 files.
 * @author Stefano Maestri
 */
public class Mp3Metadata {

    private String title;
    private String author;
    private String album;
    private String year;
    private String comment;

    private Mp3Metadata() {

    }

    public static Mp3Metadata instance( InputStream stream ) {

        Mp3Metadata me = null;
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("dna-sequencer-mp3", ".mp3");
            FileOutputStream writer = new FileOutputStream(tmpFile);
            byte[] b = new byte[128];
            while (stream.read(b) != -1) {
                writer.write(b);
            }
            writer.close();
            AudioFileIO.logger.getParent().setLevel(Level.OFF);
            AudioFile f = AudioFileIO.read(tmpFile);
            Tag tag = f.getTag();

            me = new Mp3Metadata();

            me.author = tag.getFirstArtist();
            me.album = tag.getFirstAlbum();
            me.title = tag.getFirstTitle();
            me.comment = tag.getFirstComment();
            me.year = tag.getFirstYear();

        } catch (Exception e) {
            e.printStackTrace();
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
