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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import static org.modeshape.sequencer.text.TextSequencerLexicon.COLUMN;
import static org.modeshape.sequencer.text.TextSequencerLexicon.DATA;
import static org.modeshape.sequencer.text.TextSequencerLexicon.ROW;
import java.util.ArrayList;
import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;

/**
 * Base test class for the implementations for {@link AbstractTextSequencer}
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractTextSequencerTest extends AbstractSequencerTest {

    protected static final String[] TEST_COLUMNS = new String[] {"foo", "bar", "baz"};

    protected void assertRowsWithCustomRowFactory( String sequencedRootPath ) throws Exception {
        final int ROW_COUNT = 6;
        Node outputNode = getOutputNode(rootNode, sequencedRootPath);
        assertNotNull(outputNode);
        assertEquals(ROW_COUNT, outputNode.getNodes().getSize());

        for (int rowIndex = 1; rowIndex <= ROW_COUNT; rowIndex++) {
            Node row = assertRow(outputNode, rowIndex, new String[]{});
            String[] expectedColumns = TEST_COLUMNS;
            for (int colIndex = 0; colIndex < expectedColumns.length; colIndex++) {
                assertEquals(expectedColumns[colIndex], row.getProperty(DATA + colIndex).getString());
            }
        }
    }

    protected void assertFileWithMissingRecords( String filePath ) throws Exception {
        final int ROW_COUNT = 6;
        Node outputNode = getOutputNode(rootNode, filePath);
        assertNotNull(outputNode);
        assertEquals(ROW_COUNT, outputNode.getNodes().getSize());

        for (int rowIndex = 1; rowIndex <= ROW_COUNT; rowIndex++) {
            if (rowIndex == 3) {
                assertRow(outputNode, rowIndex, new String[] {"foo"});
            } else {
                assertRow(outputNode, rowIndex, TEST_COLUMNS);
            }
        }
    }

    protected void assertRows( String rootoutputNodePath, int rowsCount, String[] expectedColumnData ) throws Exception {
        Node outputNode = getOutputNode(rootNode, rootoutputNodePath);
        assertNotNull(outputNode);
        assertEquals(rowsCount, outputNode.getNodes().getSize());

        for (int rowIndex = 1; rowIndex <= rowsCount; rowIndex++) {
            assertRow(outputNode, rowIndex, expectedColumnData);
        }
    }

    protected Node assertRow( Node rootoutputNode, int index,  String[] expectedColumnData) throws Exception {
        Node row = (index == 1) ? rootoutputNode.getNode(ROW) : rootoutputNode.getNode(ROW + "[" + index + "]");
        assertEquals(NT_UNSTRUCTURED, row.getPrimaryNodeType().getName());
        assertEquals(expectedColumnData.length, row.getNodes().getSize());
        for (int colIndex = 0; colIndex < expectedColumnData.length; colIndex++) {
            assertColumn(row, colIndex, expectedColumnData[colIndex]);
        }
        return row;
    }

    protected void assertColumn( Node row, int index, String data ) throws RepositoryException {
        Node column = row.getNode("text:column[" + (index + 1) + "]");
        assertEquals(NT_UNSTRUCTURED, column.getPrimaryNodeType().getName());

        List<String> mixinNames = new ArrayList<String>();
        for (NodeType mixinType : column.getMixinNodeTypes()) {
            mixinNames.add(mixinType.getName());
        }
        assertTrue(mixinNames.contains(COLUMN));
        assertEquals(data, column.getProperty(DATA).getString());
    }
}
