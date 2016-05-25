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
package org.modeshape.sequencer.audio;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.asf.AsfFileReader;
import org.jaudiotagger.audio.flac.FlacFileReader;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3FileReader;
import org.jaudiotagger.audio.mp4.Mp4AudioHeader;
import org.jaudiotagger.audio.mp4.Mp4FileReader;
import org.jaudiotagger.audio.ogg.OggFileReader;
import org.jaudiotagger.audio.wav.WavFileReader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.datatype.Artwork;
import org.modeshape.common.util.IoUtil;

/**
 * Utility for extracting metadata from audio files.
 * 
 * @since 5.1
 */
public class AudioMetadata {

    /**
     * Return value of {@link #getFormat()} for MP3 streams. AudioMetadata can extract metadata and audio information from
     * MP3 streams.
     */
    public static final int FORMAT_MP3 = 0;

    /**
     * Return value of {@link #getFormat()} for MP4 streams. AudioMetadata can extract metadata and audio information from
     * MP4 streams.
     */
    public static final int FORMAT_MP4 = 1;

    /**
     * Return value of {@link #getFormat()} for Vorbis streams. AudioMetadata can extract metadata and audio information from
     * Vorbis stream and Ogg container.
     */
    public static final int FORMAT_VORBIS = 2;

    /**
     * Return value of {@link #getFormat()} for FLAC streams. AudioMetadata can extract metadata and audio information from
     * FLAC stream and Ogg container.
     */
    public static final int FORMAT_FLAC = 3;

    /**
     * Return value of {@link #getFormat()} for WMA streams. AudioMetadata can extract metadata and audio information from
     * WMA stream and ASF container.
     */
    public static final int FORMAT_WMA = 4;

    /**
     * Return value of {@link #getFormat()} for WAVE streams. AudioMetadata can extract metadata and audio information from
     * WAVE stream and ASF container.
     */
    public static final int FORMAT_WAV = 5;


    public static final int FORMAT_UNSUPPORTED = -1;


    /**
     * The names of the MIME types for all supported file formats.
     */
    static final String[] MIME_TYPE_STRINGS = {"audio/mpeg",
                                               "audio/mp4", "video/mp4", "video/quicktime",
                                               "audio/vorbis", "audio/x-vorbis", "audio/ogg",
                                               "audio/flac", "audio/x-flac",
                                               "audio/vnd.ms-asf", "audio/x-ms-wma", "audio/x-ms-asf",
                                               "audio/x-wav", "audio/wav", "audio/wave"};

    /**
     * The extensions of all supported file formats. The FORMAT_xyz int constants can be used as index values for
     * this array.
     */
    static final String[] FORMAT_NAMES = {"mp3", "mp4", "ogg", "flac", "wma", "wav", "real"};

    private int format;
    private InputStream in;
    private AudioFile audioFile;

    private Long bitrate;
    private Integer sampleRate;
    private Double duration;
    private String channels;

    private String title;
    private String artist;
    private String album;
    private String year;
    private String comment;
    private String track;
    private String genre;

    private List<AudioMetadataArtwork> artwork;

    private String mimeType;

    public AudioMetadata( InputStream inputStream,
                          final String mimeType ) {
        this.in = inputStream;
        this.mimeType = mimeType;
    }

    /*
     * Check that given file is supported by this sequencer and find out the format.
     */
    public boolean check() throws Exception {
        format = FORMAT_UNSUPPORTED;
        // create a temporary copy from input
        File fileCopy = File.createTempFile("modeshape-sequencer-audio", ".tmp");
        IoUtil.write(in, new BufferedOutputStream(new FileOutputStream(fileCopy)));

        if (mimeType.startsWith("audio/mpeg")) {
            format = FORMAT_MP3;
            audioFile = new MP3FileReader().read(fileCopy);
        } else if (mimeType.startsWith("audio/vorbis") || mimeType.startsWith("audio/x-vorbis")) {
            format = FORMAT_VORBIS;
            audioFile = new OggFileReader().read(fileCopy);
        } else if (mimeType.startsWith("audio/flac") || mimeType.startsWith("audio/x-flac")) {
            format = FORMAT_FLAC;
            audioFile = new FlacFileReader().read(fileCopy);
        } else if (mimeType.equals("audio/mp4") || mimeType.equals("video/mp4") || mimeType.equals("video/quicktime")) {
            format = FORMAT_MP4;
            audioFile = new Mp4FileReader().read(fileCopy);
        } else if (mimeType.equals("audio/x-ms-wma")) {
            format = FORMAT_WMA;
            audioFile = new AsfFileReader().read(fileCopy);
        } else if (mimeType.startsWith("audio/x-wav") || mimeType.startsWith("audio/wav")) {
            format = FORMAT_WAV;
            audioFile = new WavFileReader().read(fileCopy);
        }

        // try to delete the file immediately or on JVM exit
        boolean deleted = false;
        try {
            deleted = fileCopy.delete();
        } catch (SecurityException e) {
            // ignore
        }
        if (!deleted) {
            fileCopy.deleteOnExit();
        }
        return checkSupportedAudio();
    }

    /**
     * Parse tags common for all audio files.
     */
    private boolean checkSupportedAudio() {
        AudioHeader header = audioFile.getAudioHeader();
        bitrate = header.getBitRateAsNumber();
        sampleRate = header.getSampleRateAsNumber();
        channels = header.getChannels();
        if (header.getChannels().toLowerCase().contains("stereo")) {
            channels = "2";
        }

        if (header instanceof MP3AudioHeader) {
            duration = ((MP3AudioHeader) header).getPreciseTrackLength();
        } else if (header instanceof Mp4AudioHeader) {
            duration = (double) ((Mp4AudioHeader) header).getPreciseLength();
        } else {
            duration = (double) header.getTrackLength();
        }

        // generic frames
        Tag tag = audioFile.getTag();
        artist = tag.getFirst(FieldKey.ARTIST);
        album = tag.getFirst(FieldKey.ALBUM);
        title = tag.getFirst(FieldKey.TITLE);
        comment = tag.getFirst(FieldKey.COMMENT);
        year = tag.getFirst(FieldKey.YEAR);
        track = tag.getFirst(FieldKey.TRACK);
        genre = tag.getFirst(FieldKey.GENRE);

        artwork = new ArrayList<>();
        for (Artwork a : tag.getArtworkList()) {
            AudioMetadataArtwork ama = new AudioMetadataArtwork();
            ama.setMimeType(a.getMimeType());
            if (a.getPictureType() >= 0) {
                ama.setType(a.getPictureType());
            }
            ama.setData(a.getBinaryData());

            artwork.add(ama);
        }

        return true;
    }

    /**
     * If {@link #check()} was successful, returns the audio format as one of the FORMAT_xyz constants from this class. Use
     * {@link #getFormatName()} to get a textual description of the file format.
     *
     * @return file format as a FORMAT_xyz constant
     */
    public int getFormat() {
        return format;
    }

    /**
     * If {@link #check()} was successful, returns the audio format's name. Use {@link #getFormat()} to get a unique number.
     *
     * @return file format name
     */
    public String getFormatName() {
        if (format >= 0 && format < FORMAT_NAMES.length) {
            return FORMAT_NAMES[format];
        }
        return "?";
    }

    public Long getBitrate() {
        return bitrate;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public String getChannels() {
        return channels;
    }

    public Double getDuration() {
        return duration;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
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

    public String getTrack() {
        return track;
    }

    public String getGenre() {
        return genre;
    }

    public List<AudioMetadataArtwork> getArtwork() {
        return artwork;
    }
}
