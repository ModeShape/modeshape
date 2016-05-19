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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import static org.hamcrest.number.IsCloseTo.closeTo;
import org.junit.Test;

/**
 * Unit test for {@link AudioMetadata}.
 */
public class AudioMetadataTest {

    // range of error when comparing Double values (3 digits)
    private static final double ERROR = 1e-3;

    private InputStream getTestAudio( String resourcePath ) {
        return this.getClass().getClassLoader().getResourceAsStream(resourcePath);
    }

    @Test
    public void shouldBeAbleToLoadMp3() throws Exception {
        // GIVEN
        AudioMetadata metadata = new AudioMetadata(getTestAudio("sample.mp3"), "audio/mpeg");

        // WHEN
        assertTrue(metadata.check());

        // THEN
        assertMetaDataProperties(metadata, "mp3", "audio/mpeg", 64L, 44100, "2", 2.664,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg", 3);
    }

    @Test
    public void shouldBeAbleToLoadMp4() throws Exception {
        // GIVEN
        AudioMetadata metadata = new AudioMetadata(getTestAudio("sample.mp4"), "video/quicktime");

        // WHEN
        assertTrue(metadata.check());

        // THEN 
        assertMetaDataProperties(metadata, "mp4", "video/quicktime", 129L, 44100, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg", null);
    }

    @Test
    public void shouldBeAbleToLoadOggVorbis() throws Exception {
        // GIVEN
        AudioMetadata metadata = new AudioMetadata(getTestAudio("sample.ogg"), "audio/vorbis");

        // WHEN
        assertTrue(metadata.check());

        // THEN
        assertMetaDataProperties(metadata, "ogg", "audio/vorbis", 112L, 44100, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg", 3);
    }

    @Test
    public void shouldBeAbleToLoadFlac() throws Exception {
        // GIVEN
        AudioMetadata metadata = new AudioMetadata(getTestAudio("sample.flac"), "audio/x-flac");

        // WHEN
        assertTrue(metadata.check());

        // THEN
        assertMetaDataProperties(metadata, "flac", "audio/x-flac", 426L, 44100, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg", 3);
    }

    @Test
    public void shouldBeAbleToLoadWma() throws Exception {
        // GIVEN
        AudioMetadata metadata = new AudioMetadata(getTestAudio("sample.wma"), "audio/x-ms-wma");

        // WHEN
        assertTrue(metadata.check());

        // THEN
        assertMetaDataProperties(metadata, "wma", "audio/x-ms-wma", 128L, 44100, "2", 2.0,
                "Album", "Artist", "Comment", "Title", "2015", "1", "Speech", "image/jpeg", 3);
        assertThat(metadata.getArtwork().get(0).getType(), is(3));
    }

    @Test
    public void shouldBeAbleToLoadWav() throws Exception {
        // GIVEN
        AudioMetadata metadata = new AudioMetadata(getTestAudio("sample.wav"), "audio/x-wav");

        // WHEN
        assertTrue(metadata.check());

        // THEN
        assertThat(metadata.getFormatName(), is("wav"));
        assertThat(metadata.getBitrate(), is(1411L));
        assertThat(metadata.getSampleRate(), is(44100));
        assertThat(metadata.getChannels(), is("2"));
        assertThat(metadata.getDuration(), is(2.0));
    }

    private void assertMetaDataProperties( AudioMetadata metadata, String formatName, String mimeType, Long bitrate, Integer sampleRate,
                                           String channels, Double duration, String album, String artist, String comment, String title,
                                           String year, String track, String genre, String artworkMimeType, Integer artworkType) {
        assertThat(metadata.getFormatName(), is(formatName));
        assertThat(metadata.getBitrate(), is(bitrate));
        assertThat(metadata.getSampleRate(), is(sampleRate));
        assertThat(metadata.getChannels(), is(channels));
        assertThat(metadata.getDuration(), closeTo(duration, ERROR));

        assertThat(metadata.getAlbum(), is(album));
        assertThat(metadata.getArtist(), is(artist));
        assertThat(metadata.getComment(), is(comment));
        assertThat(metadata.getTitle(), is(title));
        assertThat(metadata.getYear(), is(year));
        assertThat(metadata.getTrack(), is(track));
        assertThat(metadata.getGenre(), is(genre));

        assertThat(metadata.getArtwork().isEmpty(), is(false));
        assertThat(metadata.getArtwork().get(0).getMimeType(), is(artworkMimeType));
        assertThat(metadata.getArtwork().get(0).getType(), is(artworkType));
        assertThat(metadata.getArtwork().get(0).getData().length, greaterThan(0));
    }

}
