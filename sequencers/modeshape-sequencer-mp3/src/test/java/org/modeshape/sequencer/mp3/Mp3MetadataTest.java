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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.junit.Test;

/**
 * Unit test for {@link Mp3Metadata}
 */
public class Mp3MetadataTest {

    private InputStream getTestMp3( String resourcePath ) {
        return this.getClass().getClassLoader().getResourceAsStream(resourcePath);
    }

    @Test
    public void shouldBeAbleToCreateMetadataForSample1() throws Exception {
        Mp3Metadata metadata = Mp3Metadata.instance(getTestMp3("sample1.mp3"));
        assertThat(metadata.getAlbum(), is("Badwater Slim Performs Live"));
        assertThat(metadata.getAuthor(), is("Badwater Slim"));
        assertThat(metadata.getComment(), is("This is a test audio file."));
        assertThat(metadata.getTitle(), is("Sample MP3"));
        assertThat(metadata.getYear(), is("2008"));
    }

}
