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

package org.modeshape.jcr.mimetype;

import static org.junit.Assert.assertEquals;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
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
        assertEquals(MimeTypeConstants.MICROSOFT_WORD_OPEN_XML,
                     detector.mimeTypeOf(WORD_OPEN_XML_FILE, binaryFromFile(WORD_OPEN_XML_FILE)));
    }

    @Test
    public void shouldDetectExcelDocumentWithoutName() throws Exception {
        assertEquals(MimeTypeConstants.MICROSOFT_EXCEL, detector.mimeTypeOf(null, binaryFromFile(EXCEL_FILE)));
    }

    @Test
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
