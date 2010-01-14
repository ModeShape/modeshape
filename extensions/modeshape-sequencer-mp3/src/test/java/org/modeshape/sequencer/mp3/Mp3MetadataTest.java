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
package org.modeshape.sequencer.mp3;

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
