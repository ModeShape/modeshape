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
package org.modeshape.sequencer.text;

import org.junit.Test;

/**
 * Unit test for {@link FixedWidthTextSequencer}
 *
 * @author Horia Chiorean
 */
public class FixedWidthTextSequencerTest extends AbstractTextSequencerTest {

    @Test
    public void shouldSequenceFixedWidthFileWithOneLine() throws Exception {
        String filename = "oneLineFixedWidthFile.txt";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);

        assertRows(filePath, 1, TEST_COLUMNS);
    }

    @Test
    public void shouldSequenceFixedWidthFileWithOneLineAndNoTrailingNewLine() throws Exception {
        String filename = "oneLineFixedWidthFileNoTrailingNewLine.txt";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);

        assertRows(filePath, 1, TEST_COLUMNS);
    }

    @Test
    public void shouldSequenceFixedWidthFileWithMultipleLines() throws Exception {
        String filename = "multiLineFixedWidthFile.txt";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);

        assertRows(filePath, 6, TEST_COLUMNS);
    }

    @Test
    public void shouldSequenceFixedWidthFileWithMultipleLinesAndMissingRecords() throws Exception {
        String filename = "multiLineFixedWidthFileMissingRecords.txt";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);
        
        assertFileWithMissingRecords(filePath);
    }

    @Test
    public void shouldSequenceFixedWidthFileWithCustomRowFactory() throws Exception {
        String filename = "multiLineFixedWidthFile.txt";

        createNodeWithContentFromFile("customrowfactory/" + filename, getTestFilePath(filename));

        String sequencedRootPath = "fixed/customrowfactory/" + filename;
        assertRowsWithCustomRowFactory(sequencedRootPath);   
    }

    @Test
    public void shouldSequenceFixedWidthFileWithComments() throws Exception {
        String filename = "multiLineFixedWidthFileWithComments.txt";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);

        assertRows(filePath, 4, TEST_COLUMNS);
    }

    private String getTestFilePath( String filename ) {
        return "fixed/" + filename;
    }
}
