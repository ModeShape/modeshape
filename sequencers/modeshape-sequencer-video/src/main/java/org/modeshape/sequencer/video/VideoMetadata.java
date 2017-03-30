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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.modeshape.common.util.IoUtil;

/**
 * Utility for extracting Metadata from video formats.
 * 
 * @since 5.1
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
    private List<StreamMetadata> streams = new ArrayList<>();

    private InputStream in;

    public VideoMetadata( InputStream inputStream ) {
            this.in = inputStream;
    }

    /*
     * Check that given file is supported by this sequencer.
     */
    public boolean check() throws Exception {
        // create a temporary copy from input
        File fileCopy = File.createTempFile("modeshape-sequencer-video", ".tmp");
        IoUtil.write(in, new BufferedOutputStream(new FileOutputStream(fileCopy)));

        final Demuxer container = Demuxer.make();
        container.open(fileCopy.getAbsolutePath(), null, false, true, null, null);

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

        KeyValueBag metadata = container.getMetaData();
        for (String key : metadata.getKeys()) {
            if (key.toLowerCase(Locale.ROOT).equals("title")) {
                title = metadata.getValue(key);
            }
            if (key.toLowerCase(Locale.ROOT).equals("comment")) {
                comment = metadata.getValue(key);
            }
            if (key.toLowerCase(Locale.ROOT).equals("encoder")) {
                encoder = metadata.getValue(key);
            }
        }

        if (container.getDuration() != Global.NO_PTS) {
            // convert to seconds
            duration = (float) container.getDuration() / 1000.0 / 1000.0;
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
        container.close();
        return true;
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

}
