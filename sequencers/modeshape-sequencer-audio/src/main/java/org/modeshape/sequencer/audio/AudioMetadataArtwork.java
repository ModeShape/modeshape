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

/**
 * Class for saving metadata of artwork.
 * 
 * @since 5.1
 */
public class AudioMetadataArtwork {

    private String mimeType;
    private Integer type;
    private byte[] data;


    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType( String mimeType ) {
        this.mimeType = mimeType;
    }

    public Integer getType() {
        return type;
    }

    public void setType( Integer type ) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData( byte[] data ) {
        this.data = data;
    }
}
