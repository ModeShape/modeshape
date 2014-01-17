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
