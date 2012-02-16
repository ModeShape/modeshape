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
package org.modeshape.sequencer.text;

import org.junit.Test;

/**
 * Unit test for {@link DelimitedTextSequencer}
 *
 * @author Horia Chiorean
 */
public class DelimitedTextSequencerTest extends AbstractTextSequencerTest {

    @Test
    public void shouldSequenceCommaDelimitedFileWithOneLine() throws Exception {
        String filename = "oneLineCommaDelimitedFile.csv";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);

        assertRows(filePath, 1, TEST_COLUMNS);
    }
   @Test
    public void shouldSequenceCommaDelimitedFileWithOneLineAndNoTrailingNewLine() throws Exception {
        String filename = "oneLineCommaDelimitedFileNoTrailingNewLine.csv";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);

        assertRows(filePath, 1, TEST_COLUMNS);
    }

    @Test
    public void shouldSequenceCommaDelimitedFileWithMultipleLines() throws Exception {
        String filename = "multiLineCommaDelimitedFile.csv";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);

        assertRows(filePath, 6, TEST_COLUMNS);
    }

    @Test
    public void shouldSequenceCommaDelimitedFileWithMultipleLinesAndMissingRecords() throws Exception {
        String filename = "multiLineCommaDelimitedFileMissingRecords.csv";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);

        assertFileWithMissingRecords(filePath);
    }

    @Test
    public void shouldSequencePipeDelimitedFileWithMultipleLines() throws Exception {
        String filename = "multiLinePipeDelimitedFile.csv";
        createNodeWithContentFromFile("customsplitpattern/" + filename, "delimited/" + filename);

        String rootSequencedNodePath = "delimited/customsplitpattern/" + filename;
        assertRows(rootSequencedNodePath, 6, TEST_COLUMNS);
    }

    @Test
    public void shouldSequenceCommaDelimitedFileWithCustomRowFactory() throws Exception {
        String filename = "multiLineCommaDelimitedFile.csv";
        createNodeWithContentFromFile("customrowfactory/" + filename, getTestFilePath("multiLineCommaDelimitedFile.csv"));

        String sequencedRootPath = "delimited/customrowfactory/" + filename;
        assertRowsWithCustomRowFactory(sequencedRootPath);
    }

    @Test
    public void shouldSequenceCommaDelimitedFileWithComments() throws Exception {
        String filename = "multiLineCommaDelimitedFileWithComments.csv";
        String filePath = getTestFilePath(filename);
        createNodeWithContentFromFile(filename, filePath);

        assertRows(filePath, 4, TEST_COLUMNS);
    }


    @Test
    public void shouldSequenceCommaDelimitedFileUpToMaximumLinesSetting() throws Exception {
        String filename = "multiLineCommaDelimitedFile.csv";
        createNodeWithContentFromFile("maxlines/" + filename, "delimited/" + filename);

        String sequencedPath = "delimited/maxlines/" + filename;
        assertRows(sequencedPath, 3, TEST_COLUMNS);
    }

    private String getTestFilePath( String filename ) {
        return "delimited/" + filename;
    }
}
