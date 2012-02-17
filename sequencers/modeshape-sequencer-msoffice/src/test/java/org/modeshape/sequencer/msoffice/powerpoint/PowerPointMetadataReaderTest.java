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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.msoffice.powerpoint;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import java.io.InputStream;

/**
 * Unit test for {@link PowerPointMetadataReader}
 *
 * @author Michael Trezzi
 * @author Horia Chiorean
 */
public class PowerPointMetadataReaderTest {

    @Test
    public void shouldBeAbleToCreateMetadataForPowerPoint() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("powerpoint.ppt");
        PowerpointMetadata powerpointMetadata = PowerPointMetadataReader.instance(is);

        SlideMetadata slide = powerpointMetadata.getSlides().get(0);
        assertThat(slide.getTitle(), is("Test Slide"));
        assertThat(slide.getText(), is("This is some text"));
        assertThat(slide.getNotes(), is("My notes"));
        assertNotNull(slide.getThumbnail());
    }
}
