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
package org.modeshape.sequencer.msoffice.powerpoint;

import static org.hamcrest.core.Is.is;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * @author Michael Trezzi
 */
public class PowerPointMetadataReaderTest {

    private List<SlideMetadata> pptReader;
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

    protected InputStream getTestDocument( String resourcePath ) {
        return this.getClass().getResourceAsStream("/" + resourcePath);
    }

    @Test
    public void shouldBeAbleToCreateMetadataForPowerPoint() throws Exception {
        pptReader = PowerPointMetadataReader.instance(this.getTestDocument("powerpoint.ppt"));
        SlideMetadata slide = pptReader.get(0);
        assertThat(slide.getTitle(), is("Test Slide"));
        assertThat(slide.getText(), is("This is some text"));
        assertThat(slide.getNotes(), is("My notes"));
        try {
            FileOutputStream out = new FileOutputStream("slide.png");
            out.write(slide.getThumbnail());
            out.close();

            File f = new File("slide.png");
            assertThat(f.exists(), is(true));
            f.delete();
        } catch (Exception e) {
            e.printStackTrace(); // To change body of catch statement use File | Settings | File Templates.
        }

    }
}
