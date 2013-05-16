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

package org.modeshape.jcr.mimetype;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.InMemoryTestBinary;

/**
 * Unit test for {@link TikaMimeTypeDetector}
 * 
 * @author Horia Chiorean
 */
public class TikaMimeTypeDetectorTest {

    public static final String TXT_FILE = "mimetype/modeshape.txt";
    public static final String PDF_FILE = "mimetype/modeshape_gs.pdf";
    public static final String POSTSCRIPT_FILE = "mimetype/modeshape.ps";
    public static final String WORD_FILE = "mimetype/modeshape.doc";
    public static final String WORD_OPEN_XML_FILE = "mimetype/modeshape.docx";
    public static final String EXCEL_FILE = "mimetype/msoffice_file.xls";
    public static final String XSD_FILE = "mimetype/xsd_file.xsd";

    private TikaMimeTypeDetector detector = new TikaMimeTypeDetector(getClass().getClassLoader());

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
    @Ignore("MODE-1934")
    public void shouldDetectMsWordWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_APPLICATION_MS_WORD, detector.mimeTypeOf(null, binaryFromFile(WORD_FILE)));
    }

    @Test
    public void shouldDetectMsWordWithName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_APPLICATION_MS_WORD, detector.mimeTypeOf(WORD_FILE, binaryFromFile(WORD_FILE)));
    }

    @Test
    @Ignore("MODE-1934")
    public void shouldDetectMsWordOpenXMLWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_WORD_OPEN_XML, detector.mimeTypeOf(null, binaryFromFile(WORD_OPEN_XML_FILE)));
    }

    @Test
    @Ignore("MODE-1934")
    public void shouldDetectMsWordOpenXMLWithName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_WORD_OPEN_XML,
                     detector.mimeTypeOf(WORD_OPEN_XML_FILE, binaryFromFile(WORD_OPEN_XML_FILE)));
    }

    @Test
    @Ignore("MODE-1934")
    public void shouldDetectExcelDocumentWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_EXCEL, detector.mimeTypeOf(null, binaryFromFile(EXCEL_FILE)));
    }

    @Test
    @Ignore("MODE-1934")
    public void shouldDetectExcelDocumentWithName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_EXCEL, detector.mimeTypeOf(EXCEL_FILE, binaryFromFile(EXCEL_FILE)));
    }

    @Test
    public void shouldDetectXSDWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.APPLICATION_XML, detector.mimeTypeOf(null, binaryFromFile(XSD_FILE)));
    }

    @Test
    public void shouldDetectXSDWithName() throws Exception {
        assertEquals(MimeTypeConstants.APPLICATION_XML, detector.mimeTypeOf(XSD_FILE, binaryFromFile(XSD_FILE)));
    }

    @Test
    public void shouldDetectMimeTypeEventIfNameIsWrong() throws Exception {
        assertEquals(MimeTypeConstants.TEXT_PLAIN, detector.mimeTypeOf(PDF_FILE, binaryFromFile(TXT_FILE)));
    }

    private InMemoryTestBinary binaryFromFile( String filePath ) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(filePath);
        assertThat(stream, is(notNullValue()));
        return new InMemoryTestBinary(stream);
    }
}
