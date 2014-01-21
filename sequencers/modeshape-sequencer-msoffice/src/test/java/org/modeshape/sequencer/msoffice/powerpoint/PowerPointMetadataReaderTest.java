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
