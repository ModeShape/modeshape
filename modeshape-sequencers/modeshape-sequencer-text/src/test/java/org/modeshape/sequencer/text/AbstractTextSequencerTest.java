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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import static org.modeshape.sequencer.text.TextSequencerLexicon.COLUMN;
import static org.modeshape.sequencer.text.TextSequencerLexicon.DATA;
import static org.modeshape.sequencer.text.TextSequencerLexicon.ROW;
import java.util.ArrayList;
import java.util.List;

/**
 * Base test class for the implementations for {@link AbstractTextSequencer}
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractTextSequencerTest extends AbstractSequencerTest {

    protected static final String[] TEST_COLUMNS = new String[] {"foo", "bar", "baz"};

    protected void assertRowsWithCustomRowFactory( String sequencedRootPath ) throws Exception {
        final int ROW_COUNT = 6;
        Node sequencedNode = getSequencedNode(rootNode, sequencedRootPath);
        assertNotNull(sequencedNode);
        assertEquals(ROW_COUNT, sequencedNode.getNodes().getSize());

        for (int rowIndex = 1; rowIndex <= ROW_COUNT; rowIndex++) {
            Node row = assertRow(sequencedNode, rowIndex, new String[]{});
            String[] expectedColumns = TEST_COLUMNS;
            for (int colIndex = 0; colIndex < expectedColumns.length; colIndex++) {
                assertEquals(expectedColumns[colIndex], row.getProperty(DATA + colIndex).getString());
            }
        }
    }

    protected void assertFileWithMissingRecords( String filePath ) throws Exception {
        final int ROW_COUNT = 6;
        Node sequencedNode = getSequencedNode(rootNode, filePath);
        assertNotNull(sequencedNode);
        assertEquals(ROW_COUNT, sequencedNode.getNodes().getSize());

        for (int rowIndex = 1; rowIndex <= ROW_COUNT; rowIndex++) {
            if (rowIndex == 3) {
                assertRow(sequencedNode, rowIndex, new String[] {"foo"});
            } else {
                assertRow(sequencedNode, rowIndex, TEST_COLUMNS);
            }
        }
    }

    protected void assertRows( String rootSequencedNodePath, int rowsCount, String[] expectedColumnData ) throws Exception {
        Node sequencedNode = getSequencedNode(rootNode, rootSequencedNodePath);
        assertNotNull(sequencedNode);
        assertEquals(rowsCount, sequencedNode.getNodes().getSize());

        for (int rowIndex = 1; rowIndex <= rowsCount; rowIndex++) {
            assertRow(sequencedNode, rowIndex, expectedColumnData);
        }
    }

    protected Node assertRow( Node rootSequencedNode, int index,  String[] expectedColumnData) throws Exception {
        Node row = (index == 1) ? rootSequencedNode.getNode(ROW) : rootSequencedNode.getNode(ROW + "[" + index + "]");
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
