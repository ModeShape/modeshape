/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.sequencer.image;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.modeshape.sequencer.image.ImageMetadata;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
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
