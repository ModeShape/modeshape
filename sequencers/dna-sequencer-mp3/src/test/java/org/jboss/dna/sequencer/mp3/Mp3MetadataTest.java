package org.jboss.dna.sequencer.mp3;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.junit.After;
import org.junit.Test;

public class Mp3MetadataTest {

    private Mp3Metadata metadata;
    private InputStream imageStream;

    @After
    public void afterEach() throws Exception {
        if (imageStream != null) {
            try {
                imageStream.close();
            } finally {
                imageStream = null;
            }
        }
    }

    protected InputStream getTestMp3( String resourcePath ) {
        return this.getClass().getResourceAsStream("/" + resourcePath);
    }

    @Test
    public void shouldBeAbleToCreateMetadataForSample1() {
        metadata = Mp3Metadata.instance(this.getTestMp3("sample1.mp3"));
        assertThat(metadata.getAlbum(), is("Badwater Slim Performs Live"));
        assertThat(metadata.getAuthor(), is("Badwater Slim"));
        assertThat(metadata.getComment(), is("This is a test audio file."));
        assertThat(metadata.getTitle(), is("Sample MP3"));
        assertThat(metadata.getYear(), is("2008"));
    }

}
