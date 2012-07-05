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

package org.modeshape.extractor.tika;

import static junit.framework.Assert.assertEquals;
import org.junit.Test;
import org.modeshape.jcr.InMemoryTestBinary;
import org.modeshape.jcr.api.mimetype.MimeTypeConstants;
import java.io.IOException;

/**
 * Unit test for {@link TikaMimeTypeDetector}
 *
 * @author Horia Chiorean
 */
public class TikaMimeTypeDetectorTest {

    public static final String TXT_FILE = "modeshape.txt";
    public static final String PDF_FILE = "modeshape_gs.pdf";
    public static final String POSTSCRIPT_FILE = "modeshape.ps";
    public static final String WORD_FILE = "modeshape.doc";
    public static final String WORD_OPEN_XML_FILE = "modeshape.docx";
    private TikaMimeTypeDetector detector = new TikaMimeTypeDetector();

    @Test
    public void shouldDetectTextPlainMimeTypeWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.TEXT_PLAIN, detector.mimeTypeOf(null, binaryFromFile(TXT_FILE)));
    }

    @Test
    public void shouldDetectTextPlainMimeTypeWithName() throws Exception {
        assertEquals(MimeTypeConstants.TEXT_PLAIN, detector.mimeTypeOf(TXT_FILE, binaryFromFile(TXT_FILE)));
    }

    @Test
    public void shouldDetectPdfMimeTypeWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.PDF, detector.mimeTypeOf(null, binaryFromFile(PDF_FILE)));
    }

    @Test
    public void shouldDetectPdfMimeTypeWithName() throws Exception {
        assertEquals(MimeTypeConstants.PDF, detector.mimeTypeOf(PDF_FILE, binaryFromFile(PDF_FILE)));
    }

    @Test
    public void shouldDetectPostscriptWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.POSTSCRIPT, detector.mimeTypeOf(null, binaryFromFile(POSTSCRIPT_FILE)));
    }

    @Test
    public void shouldDetectPostscriptWithName() throws Exception {
        assertEquals(MimeTypeConstants.POSTSCRIPT, detector.mimeTypeOf(POSTSCRIPT_FILE, binaryFromFile(POSTSCRIPT_FILE)));
    }

    @Test
    public void shouldDetectMsWordWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_APPLICATION_MS_WORD, detector.mimeTypeOf(null, binaryFromFile(WORD_FILE)));
    }

    @Test
    public void shouldDetectMsWordWithName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_APPLICATION_MS_WORD, detector.mimeTypeOf(WORD_FILE, binaryFromFile(WORD_FILE)));
    }

    @Test
    public void shouldDetectMsWordOpenXMLWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_WORD_OPEN_XML, detector.mimeTypeOf(null, binaryFromFile(WORD_OPEN_XML_FILE)));
    }

    @Test
    public void shouldDetectMsWordOpenXMLWithName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_WORD_OPEN_XML, detector.mimeTypeOf(WORD_OPEN_XML_FILE, binaryFromFile(WORD_OPEN_XML_FILE)));
    }

    @Test
    public void shouldDetectMimeTypeEventIfNameIsWrong() throws Exception {
        assertEquals(MimeTypeConstants.TEXT_PLAIN, detector.mimeTypeOf(PDF_FILE, binaryFromFile(TXT_FILE)));
    }

    private InMemoryTestBinary binaryFromFile(String filePath) throws IOException {
        return new InMemoryTestBinary(getClass().getClassLoader().getResourceAsStream(filePath));
    }
}
