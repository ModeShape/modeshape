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
import org.modeshape.common.FixFor;

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

    @Test
    @FixFor( "MODE-1873" )
    public void shouldBeAbleToQueryForDerivedContent() throws Exception {
        String filename = "multiLineCommaDelimitedFile.csv";
        createNodeWithContentFromFile("maxlines/" + filename, "delimited/" + filename);

        String sequencedPath = "/maxlines/" + filename;
        String sequencedOutputPath = "delimited/maxlines/" + filename;
        assertRows(sequencedOutputPath, 3, TEST_COLUMNS);

        // print = true;
        print("/maxlines");
        print("/delimited");

        // Now query for the results ...
        assertJcrSql2Query("SELECT [jcr:path] FROM [nt:base] WHERE PATH() = '" + sequencedPath + "'", 1);
        assertJcrSql2Query("SELECT [jcr:path] FROM [nt:base] WHERE PATH() = '" + sequencedPath + "/jcr:content'", 1);
        assertJcrSql2Query("SELECT [jcr:path] FROM [nt:base] WHERE [jcr:path] IN (SELECT [jcr:path] FROM [nt:base] WHERE PATH() = '/maxlines/multiLineCommaDelimitedFile.csv/jcr:content' )",
                           1);
        assertJcrSql2Query("SELECT [jcr:path] FROM [nt:base] WHERE PATH() IN (SELECT [jcr:path] FROM [nt:base] WHERE PATH() = '/maxlines/multiLineCommaDelimitedFile.csv/jcr:content' )",
                           1);
        assertJcrSql2Query("SELECT [jcr:path],[jcr:mixinTypes] FROM [nt:base] WHERE ISDESCENDANTNODE('/maxlines')", 2);
        assertJcrSql2Query("SELECT [mode:derivedFrom] FROM [nt:base] WHERE [mode:derivedFrom] IS NOT NULL", 1);
        assertJcrSql2Query("SELECT [jcr:path] FROM [mode:derived] AS d WHERE d.[mode:derivedFrom] LIKE '/maxlines%'", 1);
        assertJcrSql2Query("SELECT [jcr:path] FROM [mode:derived] AS d WHERE d.[mode:derivedFrom] = '/maxlines/multiLineCommaDelimitedFile.csv/jcr:content'",
                           1);
        assertJcrSql2Query("SELECT [jcr:path] FROM [mode:derived] AS d WHERE d.[mode:derivedFrom] IN ( '/maxlines/multiLineCommaDelimitedFile.csv/jcr:content' )",
                           1);
        assertJcrSql2Query("SELECT [jcr:path] FROM [mode:derived] AS d WHERE d.[mode:derivedFrom] in (SELECT [jcr:path] FROM [nt:base] WHERE PATH() = '/maxlines/multiLineCommaDelimitedFile.csv/jcr:content' )",
                           1);
        assertJcrSql2Query("SELECT [jcr:path] FROM [mode:derived] AS d WHERE d.[mode:derivedFrom] in (SELECT [jcr:path] FROM [nt:base] WHERE PATH() = '"
                           + sequencedPath + "/jcr:content' )",
                           1);
    }

    private String getTestFilePath( String filename ) {
        return "delimited/" + filename;
    }
}
