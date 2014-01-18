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
package org.modeshape.sequencer.image;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.number.IsCloseTo.closeTo;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import java.io.InputStream;

/**
 * Unit test for {@link ImageMetadata}.
 *
 * @author Randall Hauch
 * @author Horia Chiorean
 */
public class ImageMetadataTest {

    private ImageMetadata image;
    private InputStream imageStream;

    @Before
    public void beforeEach() {
        this.image = new ImageMetadata();
    }

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

    protected InputStream getTestImage( String resourcePath ) {
        return this.getClass().getResourceAsStream("/" + resourcePath);
    }

    @Test
    public void shouldBeAbleToCreateWithDefaultConstructor() {
        assertThat(new ImageMetadata(), is(notNullValue()));
    }

    @Test
    public void shouldBeAbleToLoadPngImage() {
        // Get an input stream to a test file ...
        InputStream stream = getTestImage("caution.png");
        image.setInput(stream);
        image.setDetermineImageNumber(true);
        image.setCollectComments(true);

        // Process the image ...
        assertThat(image.check(), is(true));

        assertThat(image.getFormatName(), is("PNG"));
        assertThat(image.getMimeType(), is("image/png"));
        assertThat(image.getWidth(), is(48));
        assertThat(image.getHeight(), is(48));
        assertThat(image.getBitsPerPixel(), is(24));
        assertThat(image.isProgressive(), is(false));
        assertThat(image.getNumberOfImages(), is(1));
        assertThat(image.getPhysicalWidthDpi(), is(-1));
        assertThat(image.getPhysicalHeightDpi(), is(-1));
        assertThat(image.getPhysicalWidthInch(), is(-1.0f));
        assertThat(image.getPhysicalHeightInch(), is(-1.0f));

        assertThat(image.getNumberOfComments(), is(0));
        // assertThat(image.getComment(0), is(""));
    }

    @Test
    public void shouldBeAbleToLoadGifImage() {
        // Get an input stream to a test file ...
        InputStream stream = getTestImage("caution.gif");
        image.setInput(stream);
        image.setDetermineImageNumber(true);
        image.setCollectComments(true);

        // Process the image ...
        assertThat(image.check(), is(true));

        assertThat(image.getFormatName(), is("GIF"));
        assertThat(image.getMimeType(), is("image/gif"));
        assertThat(image.getWidth(), is(48));
        assertThat(image.getHeight(), is(48));
        assertThat(image.getBitsPerPixel(), is(8));
        assertThat(image.isProgressive(), is(false));
        assertThat(image.getNumberOfImages(), is(1));
        assertThat(image.getPhysicalWidthDpi(), is(-1));
        assertThat(image.getPhysicalHeightDpi(), is(-1));
        assertThat(image.getPhysicalWidthInch(), is(-1.0f));
        assertThat(image.getPhysicalHeightInch(), is(-1.0f));

        assertThat(image.getNumberOfComments(), is(0));
        // assertThat(image.getComment(0), is(""));
    }

    @Test
    public void shouldBeAbleToLoadJpegImage() {
        // Get an input stream to a test file ...
        InputStream stream = getTestImage("caution.jpg");
        image.setInput(stream);
        image.setDetermineImageNumber(true);
        image.setCollectComments(true);

        // Process the image ...
        assertThat(image.check(), is(true));

        assertThat(image.getFormatName(), is("JPEG"));
        assertThat(image.getMimeType(), is("image/jpeg"));
        assertThat(image.getWidth(), is(48));
        assertThat(image.getHeight(), is(48));
        assertThat(image.getBitsPerPixel(), is(24));
        assertThat(image.isProgressive(), is(false));
        assertThat(image.getNumberOfImages(), is(1));
        assertThat(image.getPhysicalWidthDpi(), is(72));
        assertThat(image.getPhysicalHeightDpi(), is(72));
        assertThat((double)image.getPhysicalWidthInch(), is(closeTo(0.66666666d, 0.00001d)));
        assertThat((double)image.getPhysicalHeightInch(), is(closeTo(0.66666666d, 0.00001d)));

        assertThat(image.getNumberOfComments(), is(0));
        // assertThat(image.getComment(0), is(""));
    }

    @Test
    public void shouldNotBeAbleToLoadPictImage() {
        // Get an input stream to a test file ...
        InputStream stream = getTestImage("caution.pict");
        image.setInput(stream);
        image.setDetermineImageNumber(true);
        image.setCollectComments(true);

        // Process the image ...
        assertThat(image.check(), is(false));
    }
}
