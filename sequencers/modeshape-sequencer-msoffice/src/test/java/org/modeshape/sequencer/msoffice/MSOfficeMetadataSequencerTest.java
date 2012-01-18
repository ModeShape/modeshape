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
package org.modeshape.sequencer.msoffice;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.modeshape.jcr.api.JcrConstants;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;
import org.modeshape.jcr.api.mimetype.MimeTypeConstants;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_EXCEL;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_POWERPOINT;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_WORD;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.*;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Unit test for {@link MSOfficeMetadataSequencer}
 *
 * @author Horia Chiorean
 */
public class MSOfficeMetadataSequencerTest extends AbstractSequencerTest {

    private static final Map<String, Integer> WORD_HEADINGS = new LinkedHashMap<String, Integer>() {{
        put("Test Heading 1", 1);
        put("Test Heading 1.1", 2);
        put("Test Heading 1.2", 2);
        put("Test Heading 1.2.1", 3);
        put("Test Heading 2", 1);
        put("Test Heading 2.1", 2);
        put("Test Heading 2.2", 2);
    }};

    private static final Map<String,String> EXCEL_SHEETS = new LinkedHashMap<String,String>() {{
        put("Sheet1", "This is a text");
        put("MySheet2", null);
        put("Sheet3", null);
    }};

    @Test
    public void shouldSequenceWordFiles() throws Exception {
        createNodeWithContentFromFile("word.doc", "word.doc");
        Node sequencedNode = getSequencedNode(rootNode, "word.doc/" + METADATA_NODE);
        assertNotNull(sequencedNode);
        
        assertEquals(METADATA_NODE, sequencedNode.getPrimaryNodeType().getName());
        assertEquals(MICROSOFT_WORD, sequencedNode.getProperty(JCR_MIME_TYPE).getString());
        assertMetadata(sequencedNode);

        NodeIterator headingsIterator = sequencedNode.getNodes();
        assertEquals(WORD_HEADINGS.size(), headingsIterator.getSize());
        while(headingsIterator.hasNext()) {
            Node heading = headingsIterator.nextNode();
            assertEquals(HEADING_NODE, heading.getPrimaryNodeType().getName());
            Integer headingLevel = WORD_HEADINGS.get(heading.getProperty(HEADING_NAME).getString());
            assertEquals(headingLevel.longValue(), heading.getProperty(HEADING_LEVEL).getLong());
        }
    }

    private void assertMetadata( Node sequencedNode ) throws RepositoryException {
        assertEquals("Test Comment", sequencedNode.getProperty(COMMENT).getString());
        assertEquals("Michael Trezzi", sequencedNode.getProperty(AUTHOR).getString());
        assertEquals("jboss, test, dna", sequencedNode.getProperty(KEYWORDS).getString());
        assertEquals("Test Document", sequencedNode.getProperty(TITLE).getString());
        assertEquals("Test Subject", sequencedNode.getProperty(SUBJECT).getString());
    }
    
    @Test
    public void shouldSequenceExcelFiles() throws Exception {
        createNodeWithContentFromFile("excel.xls", "excel.xls");
        Node sequencedNode = getSequencedNode(rootNode, "excel.xls/" + METADATA_NODE);
        assertNotNull(sequencedNode);

        assertEquals(METADATA_NODE, sequencedNode.getPrimaryNodeType().getName());
        assertEquals(MICROSOFT_EXCEL, sequencedNode.getProperty(JCR_MIME_TYPE).getString());
        assertMetadata(sequencedNode);

        NodeIterator sheetsIterator = sequencedNode.getNodes();
        assertEquals(EXCEL_SHEETS.size(), sheetsIterator.getSize());
        while(sheetsIterator.hasNext()) {
            Node sheet = sheetsIterator.nextNode();
            assertEquals(EXCEL_SHEET_NODE, sheet.getPrimaryNodeType().getName());

            String sheetName = sheet.getProperty(SHEET_NAME).getString();
            assertTrue(EXCEL_SHEETS.containsKey(sheetName));
            String text = EXCEL_SHEETS.get(sheetName);
            if (text != null) {
                assertTrue(sheet.getProperty(TEXT).getString().contains(text));
            }
        }
    }

    @Test
    public void shouldSequencePowerpointFiles() throws Exception {
        createNodeWithContentFromFile("powerpoint.ppt", "powerpoint.ppt");
        Node sequencedNode = getSequencedNode(rootNode, "powerpoint.ppt/" + METADATA_NODE);
        assertNotNull(sequencedNode);

        assertEquals(METADATA_NODE, sequencedNode.getPrimaryNodeType().getName());
        assertEquals(MICROSOFT_POWERPOINT, sequencedNode.getProperty(JCR_MIME_TYPE).getString());
        NodeIterator slidesIterator = sequencedNode.getNodes();
        assertEquals(1, slidesIterator.getSize());
        
        Node slide = slidesIterator.nextNode();
        assertEquals(SLIDE_NODE, slide.getPrimaryNodeType().getName());
        assertEquals("Test Slide", slide.getProperty(TITLE).getString());
        assertEquals("This is some text", slide.getProperty(TEXT).getString());
        assertEquals("My notes", slide.getProperty(NOTES).getString());
    }

}
