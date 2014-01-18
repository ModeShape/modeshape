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
package org.modeshape.sequencer.msoffice;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import java.io.InputStream;

/**
 * Unit test for {@link MSOfficeMetadataSequencer}
 *
 * @author ?
 * @author Horia Chiorean
 */
public class MSOfficeMetadataTest {

    @Test
    public void shouldBeAbleToCreateMetadataForWord() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("word.doc");
        assertMetadata(is);
    }

    @Test
    public void shouldBeAbleToCreateMetadataForExcel() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("excel.xls");
        assertMetadata(is);
    }

    @Test
    public void shouldBeAbleToCreateMetadataForPowerpoint() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("powerpoint.ppt");
        assertMetadata(is);
    }

    private void assertMetadata( InputStream is ) throws Exception {
        MSOfficeMetadata officeMetadata = MSOfficeMetadataReader.instance(is);
        assertThat(officeMetadata.getComment(), is("Test Comment"));
        assertThat(officeMetadata.getAuthor(), is("Michael Trezzi"));
        assertThat(officeMetadata.getKeywords(), is("jboss, test, dna"));
        assertThat(officeMetadata.getTitle(), is("Test Document"));
        assertThat(officeMetadata.getSubject(), is("Test Subject"));
    }

}

