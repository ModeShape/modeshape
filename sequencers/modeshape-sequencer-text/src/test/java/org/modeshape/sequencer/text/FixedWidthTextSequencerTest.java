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
