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
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.test.integration.sequencer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.junit.Test;

/**
 * Integration test for the text sequencers: {@link org.modeshape.sequencer.text.DelimitedTextSequencer}
 * and {@link org.modeshape.sequencer.text.FixedWidthTextSequencer}
 */
public class TextSequencerIntegrationTest extends AbstractSequencerTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.test.ModeShapeUnitTest#getPathToDefaultConfiguration()
     */
    @Override
    protected String getPathToDefaultConfiguration() {
        return "config/configRepositoryForTextSequencing.xml";
    }

    @Test
    public void shouldSequenceDelimitedTextFile() throws Exception {
        uploadFile("sequencers/text/delimitedFile.csv", "/files/");

        // Find the sequenced node ...
        String path = "/sequenced/delimitedFile.csv";
        Node rootNode = waitUntilSequencedNodeIsAvailable(path, "nt:unstructured");
        
        String[] expectedColumnValues = new String[] {"foo", "bar", "baz"};
        for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
            assertRowWithColumns(path, rowIdx, expectedColumnValues);
        }

        printOutput(rootNode);
    }

    private void printOutput( Node rootNode ) throws RepositoryException {
        //print=true;
        printSubgraph(rootNode);
    }

    private void assertRowWithColumns(String rootPath, int rowIndex, String[] columnData) throws Exception {
        String rowPath = rootPath + "/text:row[" + (rowIndex + 1) + "]";
        assertNode(rowPath, "nt:unstructured", "mode:derived");

        for (int i = 0; i < columnData.length; i++) {
            String columnPath = rowPath + "/text:column[" + (i + 1) + "]";
            Node column = assertNode(columnPath, "nt:unstructured", "text:column");
            assertSingleValueProperty(column, "text:data", columnData[i]);
        }    
    }
}
