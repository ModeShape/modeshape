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
package org.modeshape.sequencer.video;

import io.humble.video.Decoder;
import io.humble.video.Demuxer;
import io.humble.video.DemuxerStream;
import io.humble.video.Global;
import io.humble.video.KeyValueBag;
import io.humble.video.MediaDescriptor.Type;

import java.io.DataInput;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for extracting Metadata from video formats.
 */
public class VideoMetadata {

    static final String[] MIME_TYPE_STRINGS = { "video/x-matroska",  // mkv
                                                "video/quicktime",  // mov
                                                "video/x-msvideo",  // avi
                                                "video/x-ms-wmv",  // wmv
                                                "video/x-flv",  // flv
                                                "video/3gpp",  // 3gpp
                                                "video/webm",  // webp
                                                "video/mp4",  // mp4
                                                "video/ogg"  //ogg
    };

    private Double duration;
    private Integer bitrate;
    private String title;
    private String comment;
    private String encoder;
    private List<StreamMetadata> streams = new ArrayList<StreamMetadata>();

    private InputStream in;
    private DataInput din;

    /*
     * Check that given file is supported by this sequencer.
     */
    public boolean check() {
        try {
            // create a temporary copy from output
            File fileCopy = File.createTempFile("modeshape-sequencer-video", ".tmp");
            FileOutputStream fileOutputStream = new FileOutputStream(fileCopy);
            byte[] b = new byte[1024];
            while (read(b) != -1) {
                fileOutputStream.write(b);
            }
            fileOutputStream.close();

            final Demuxer container = Demuxer.make();
            container.open(fileCopy.getAbsolutePath(), null, false, true, null, null);

            KeyValueBag metadata = container.getMetaData();
            for (String key : metadata.getKeys()) {
                if (key.toLowerCase().equals("title")) {
                    title = metadata.getValue(key);
                }
                if (key.toLowerCase().equals("comment")) {
                    comment = metadata.getValue(key);
                }
                if (key.toLowerCase().equals("encoder")) {
                    encoder = metadata.getValue(key);
                }
            }

            if (container.getDuration() != Global.NO_PTS) {
                duration = (float) container.getDuration() / 1000.0 / 1000.0; // convert to seconds
            }
            if (container.getBitRate() > 0) {
                bitrate = container.getBitRate();
            }


            for (int i = 0; i < container.getNumStreams(); i++) {
                    StreamMetadata streamMetadata = new StreamMetadata();
                    DemuxerStream stream = container.getStream(i);

                    Decoder coder = stream.getDecoder();

                    if (coder.getCodecType() == Type.MEDIA_AUDIO) {
                        streamMetadata.setStreamType("audio");
                    } else if (coder.getCodecType() == Type.MEDIA_VIDEO) {
                        streamMetadata.setStreamType("video");
                    } else {
                        streamMetadata.setStreamType("unknown");
                    }

                    if (coder.getCodec() != null) {
                        streamMetadata.setCodec(coder.getCodec().getName());
                    }
                    streamMetadata.setSamplerate(nullValueIfInvalid(coder.getSampleRate()));
                    streamMetadata.setChannels(nullValueIfInvalid(coder.getChannels()));
                    streamMetadata.setWidth(nullValueIfInvalid(coder.getWidth()));
                    streamMetadata.setHeight(nullValueIfInvalid(coder.getHeight()));
                    if (coder.getTimeBase() != null) {
                        streamMetadata.setFramerate(nullValueIfInvalid(1.0 / coder.getTimeBase().getDouble()));
                    }
                    streams.add(streamMetadata);
                }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private Integer nullValueIfInvalid( int value ) {
        if (value > 0) {
            return value;
        } else {
            return null;
        }
    }

    private Double nullValueIfInvalid( double value ) {
        if (value > 0) {
            return value;
        } else {
            return null;
        }
    }

    public Double getDuration() {
        return duration;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public String getTitle() {
        return title;
    }

    public String getComment() {
        return comment;
    }

    public String getEncoder() {
        return encoder;
    }

    public List<StreamMetadata> getStreams() {
        return streams;
    }

    private int read( byte[] a ) throws IOException {
        if (in != null) {
            return in.read(a);
        }
        din.readFully(a);
        return a.length;
    }


    /**
     * Set the input stream to the argument stream (or file). Note that {@link java.io.RandomAccessFile} implements
     * {@link java.io.DataInput}.
     *
     * @param dataInput the input stream to read from
     */
    public void setInput( DataInput dataInput ) {
        din = dataInput;
        in = null;
    }

    /**
     * Set the input stream to the argument stream (or file).
     *
     * @param inputStream the input stream to read from
     */
    public void setInput( InputStream inputStream ) {
        in = inputStream;
        din = null;
    }
}
