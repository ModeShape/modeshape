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

package org.modeshape.sequencer.msoffice.word;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;

/**
 * Unit test for {@link WordMetadataTest}
 * 
 * @author ?
 * @author Horia Chiorean
 */
public class WordMetadataTest {

    private static final String[] TEST_HEADERS_TEXT = new String[] {
        "Test Heading 1", "Test Heading 1.1", "Test Heading 1.2",
        "Test Heading 1.2.1", "Test Heading 2", "Test Heading 2.1", "Test Heading 2.2",};
    private static final int[] TEST_HEADERS_LEVEL = new int[] {1, 2, 2, 3, 1, 2, 2};

    @Test
    public void shouldBeAbleToParseHeadingsForWord() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("word.doc");

        WordMetadata wordMetadata = WordMetadataReader.instance(is);
        List<WordMetadata.WordHeading> headings = wordMetadata.getHeadings();

        assertThat(headings.size(), is(TEST_HEADERS_TEXT.length));

        for (int i = 0; i < headings.size(); i++) {
            assertThat(headings.get(i).getText(), is(TEST_HEADERS_TEXT[i]));
            assertThat(headings.get(i).getHeaderLevel(), is(TEST_HEADERS_LEVEL[i]));
        }
    }
}
