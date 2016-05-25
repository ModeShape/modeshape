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

/**
 * Java class representing metadata within video or audio stream.
 * 
 * @since 5.1
 */
public class StreamMetadata {

    private String streamType;
    private String codec;
    private Integer bitrate;
    private Double framerate;
    private Integer samplerate;
    private Integer channels;
    private Integer width;
    private Integer height;


    public String getStreamType() {
        return streamType;
    }

    public String getCodec() {
        return codec;
    }

    public Integer getBitrate() {
        return bitrate;
    }

    public Double getFramerate() {
        return framerate;
    }

    public Integer getSamplerate() {
        return samplerate;
    }

    public Integer getChannels() {
        return channels;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setStreamType(String streamType) {
        this.streamType = streamType;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public void setBitrate(Integer bitrate) {
        this.bitrate = bitrate;
    }

    public void setFramerate(Double framerate) {
        this.framerate = framerate;
    }

    public void setSamplerate(Integer samplerate) {
        this.samplerate = samplerate;
    }

    public void setChannels(Integer channels) {
        this.channels = channels;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
