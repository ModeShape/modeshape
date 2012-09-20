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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.api.JcrConstants.JCR_MIME_TYPE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.AUTHOR;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.COMMENT;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.EXCEL_SHEET_NODE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.HEADING_LEVEL;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.HEADING_NAME;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.HEADING_NODE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.KEYWORDS;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.METADATA_NODE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.NOTES;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.SHEET_NAME;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.SLIDE_NODE;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.SUBJECT;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.TEXT;
import static org.modeshape.sequencer.msoffice.MSOfficeMetadataLexicon.TITLE;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.msoffice.MSOfficeMetadataSequencer.MimeTypeConstants;

/**
 * Unit test for {@link MSOfficeMetadataSequencer}
 * 
 * @author Horia Chiorean
 */
public class MSOfficeMetadataSequencerTest extends AbstractSequencerTest {

    @SuppressWarnings( "serial" )
    private static final Map<String, Integer> WORD_HEADINGS = new LinkedHashMap<String, Integer>() {
        {
            put("Test Heading 1", 1);
            put("Test Heading 1.1", 2);
            put("Test Heading 1.2", 2);
            put("Test Heading 1.2.1", 3);
            put("Test Heading 2", 1);
            put("Test Heading 2.1", 2);
            put("Test Heading 2.2", 2);
        }
    };

    @SuppressWarnings( "serial" )
    private static final Map<String, String> EXCEL_SHEETS = new LinkedHashMap<String, String>() {
        {
            put("Sheet1", "This is a text");
            put("MySheet2", null);
            put("Sheet3", null);
        }
    };

    @Test
    public void shouldSequenceWordFiles() throws Exception {
        createNodeWithContentFromFile("word.doc", "word.doc");
        Node outputNode = getOutputNode(rootNode, "word.doc/" + METADATA_NODE);
        assertNotNull(outputNode);

        assertEquals(METADATA_NODE, outputNode.getPrimaryNodeType().getName());
        String mimeType = outputNode.getProperty(JCR_MIME_TYPE).getString();
        assertEquals(MimeTypeConstants.MICROSOFT_WORD.equals(mimeType)
                     || MimeTypeConstants.MICROSOFT_APPLICATION_MS_WORD.equals(mimeType),
                     true);
        assertMetadata(outputNode);

        NodeIterator headingsIterator = outputNode.getNodes();
        assertEquals(WORD_HEADINGS.size(), headingsIterator.getSize());
        while (headingsIterator.hasNext()) {
            Node heading = headingsIterator.nextNode();
            assertEquals(HEADING_NODE, heading.getPrimaryNodeType().getName());
            Integer headingLevel = WORD_HEADINGS.get(heading.getProperty(HEADING_NAME).getString());
            assertEquals(headingLevel.longValue(), heading.getProperty(HEADING_LEVEL).getLong());
        }
    }

    private void assertMetadata( Node outputNode ) throws RepositoryException {
        assertEquals("Test Comment", outputNode.getProperty(COMMENT).getString());
        assertEquals("Michael Trezzi", outputNode.getProperty(AUTHOR).getString());
        assertEquals("jboss, test, dna", outputNode.getProperty(KEYWORDS).getString());
        assertEquals("Test Document", outputNode.getProperty(TITLE).getString());
        assertEquals("Test Subject", outputNode.getProperty(SUBJECT).getString());
    }

    @Test
    public void shouldSequenceExcelFiles() throws Exception {
        createNodeWithContentFromFile("excel.xls", "excel.xls");
        Node outputNode = getOutputNode(rootNode, "excel.xls/" + METADATA_NODE);
        assertNotNull(outputNode);

        assertEquals(METADATA_NODE, outputNode.getPrimaryNodeType().getName());
        assertEquals(MimeTypeConstants.MICROSOFT_EXCEL, outputNode.getProperty(JCR_MIME_TYPE).getString());
        assertMetadata(outputNode);

        NodeIterator sheetsIterator = outputNode.getNodes();
        assertEquals(EXCEL_SHEETS.size(), sheetsIterator.getSize());
        while (sheetsIterator.hasNext()) {
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
    public void shouldSequenceAnotherExcelFiles() throws Exception {
        createNodeWithContentFromFile("msoffice_file.xls", "msoffice_file.xls");
        Node outputNode = getOutputNode(rootNode, "msoffice_file.xls/" + METADATA_NODE);
        assertNotNull(outputNode);

        assertEquals(METADATA_NODE, outputNode.getPrimaryNodeType().getName());
        assertEquals(MimeTypeConstants.MICROSOFT_EXCEL, outputNode.getProperty(JCR_MIME_TYPE).getString());
        assertMetadata(outputNode);

        NodeIterator sheetsIterator = outputNode.getNodes();
        assertEquals(EXCEL_SHEETS.size(), sheetsIterator.getSize());
        while (sheetsIterator.hasNext()) {
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
        Node outputNode = getOutputNode(rootNode, "powerpoint.ppt/" + METADATA_NODE);
        assertNotNull(outputNode);

        assertEquals(METADATA_NODE, outputNode.getPrimaryNodeType().getName());
        assertEquals(MimeTypeConstants.MICROSOFT_POWERPOINT, outputNode.getProperty(JCR_MIME_TYPE).getString());
        NodeIterator slidesIterator = outputNode.getNodes();
        assertEquals(1, slidesIterator.getSize());

        Node slide = slidesIterator.nextNode();
        assertEquals(SLIDE_NODE, slide.getPrimaryNodeType().getName());
        assertEquals("Test Slide", slide.getProperty(TITLE).getString());
        assertEquals("This is some text", slide.getProperty(TEXT).getString());
        assertEquals("My notes", slide.getProperty(NOTES).getString());
    }

}
