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
import org.apache.tika.mime.MediaType;
import org.junit.Test;
import org.modeshape.jcr.InMemoryTestBinary;
import org.modeshape.jcr.TestingEnvironment;

/**
 * Unit test for {@link ContentDetector}
 * 
 * @author Horia Chiorean
 */
public class ContentDetectorTest {

    private static final MimeTypeDetector DETECTOR = new ContentDetector(new TestingEnvironment());

    private static final String TXT_FILE = "mimetype/modeshape.txt";
    private static final String PDF_FILE = "mimetype/modeshape_gs.pdf";
    private static final String POSTSCRIPT_FILE = "mimetype/modeshape.ps";
    private static final String WORD_FILE = "mimetype/modeshape.doc";
    private static final String WORD_OPEN_XML_FILE = "mimetype/modeshape.docx";
    private static final String EXCEL_FILE = "mimetype/msoffice_file.xls";
    private static final String XSD_FILE = "mimetype/xsd_file.xsd";
    private static final String CND_FILE = "mimetype/aircraft.cnd";

    @Test
    public void shouldDetectTextPlainMimeTypeWithoutName() throws Exception {
        assertEquals(MediaType.TEXT_PLAIN.toString(), DETECTOR.mimeTypeOf(null, binaryFromFile(TXT_FILE)));
    }

    @Test
    public void shouldDetectTextPlainMimeTypeWithName() throws Exception {
        assertEquals(MediaType.TEXT_PLAIN.toString(), DETECTOR.mimeTypeOf(TXT_FILE, binaryFromFile(TXT_FILE)));
    }

    @Test
    public void shouldDetectPdfMimeTypeWithoutName() throws Exception {
        assertEquals("application/pdf", DETECTOR.mimeTypeOf(null, binaryFromFile(PDF_FILE)));
    }

    @Test
    public void shouldDetectPdfMimeTypeWithName() throws Exception {
        assertEquals("application/pdf", DETECTOR.mimeTypeOf(PDF_FILE, binaryFromFile(PDF_FILE)));
    }

    @Test
    public void shouldDetectPostscriptWithoutName() throws Exception {
        assertEquals("application/postscript", DETECTOR.mimeTypeOf(null, binaryFromFile(POSTSCRIPT_FILE)));
    }

    @Test
    public void shouldDetectPostscriptWithName() throws Exception {
        assertEquals("application/postscript", DETECTOR.mimeTypeOf(POSTSCRIPT_FILE, binaryFromFile(POSTSCRIPT_FILE)));
    }

    @Test
    public void shouldDetectMsWordWithoutName() throws Exception {
        assertEquals("application/msword", DETECTOR.mimeTypeOf(null, binaryFromFile(WORD_FILE)));
    }

    @Test
    public void shouldDetectMsWordWithName() throws Exception {
        assertEquals("application/msword", DETECTOR.mimeTypeOf(WORD_FILE, binaryFromFile(WORD_FILE)));
    }

    @Test
    public void shouldDetectMsWordOpenXMLWithoutName() throws Exception {
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", 
                     DETECTOR.mimeTypeOf(null, binaryFromFile(WORD_OPEN_XML_FILE)));
    }

    @Test
    public void shouldDetectMsWordOpenXMLWithName() throws Exception {
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                     DETECTOR.mimeTypeOf(WORD_OPEN_XML_FILE, binaryFromFile(WORD_OPEN_XML_FILE)));
    }

    @Test
    public void shouldDetectExcelDocumentWithoutName() throws Exception {
        assertEquals("application/vnd.ms-excel", DETECTOR.mimeTypeOf(null, binaryFromFile(EXCEL_FILE)));
    }

    @Test
    public void shouldDetectExcelDocumentWithName() throws Exception {
        assertEquals("application/vnd.ms-excel", DETECTOR.mimeTypeOf(EXCEL_FILE, binaryFromFile(EXCEL_FILE)));
    }

    @Test
    public void shouldDetectXSDWithoutName() throws Exception {
        assertEquals(MediaType.APPLICATION_XML.toString(), DETECTOR.mimeTypeOf(null, binaryFromFile(XSD_FILE)));
    }

    @Test
    public void shouldDetectXSDWithName() throws Exception {
        assertEquals(MediaType.APPLICATION_XML.toString(), DETECTOR.mimeTypeOf(XSD_FILE, binaryFromFile(XSD_FILE)));
    }

    @Test
    public void shouldDetectCNDWithoutNameAsTextPlain() throws Exception {
        assertEquals(MediaType.TEXT_PLAIN.toString(), DETECTOR.mimeTypeOf(null, binaryFromFile(CND_FILE)));
    }

    @Test
    public void shouldDetectCNDWithName() throws Exception {
        assertEquals("text/jcr-cnd", DETECTOR.mimeTypeOf(CND_FILE, binaryFromFile(CND_FILE)));
    }

    @Test
    public void shouldDetectMimeTypeEventIfNameIsWrong() throws Exception {
        assertEquals(MediaType.TEXT_PLAIN.toString(), DETECTOR.mimeTypeOf(PDF_FILE, binaryFromFile(TXT_FILE)));
    }

    private InMemoryTestBinary binaryFromFile( String filePath ) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(filePath);
        assertThat(stream, is(notNullValue()));
        return new InMemoryTestBinary(stream);
    }
}
