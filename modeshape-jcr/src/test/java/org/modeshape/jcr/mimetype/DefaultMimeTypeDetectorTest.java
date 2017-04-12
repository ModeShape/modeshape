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

import java.io.IOException;
import javax.jcr.RepositoryException;
import org.apache.tika.mime.MediaType;
import org.junit.Test;

/**
 * Unit test for {@link DefaultMimeTypeDetector}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class DefaultMimeTypeDetectorTest {
    
    private MimeTypeDetector detector = new DefaultMimeTypeDetector();
    
    @Test
    public void shouldProvideMimeTypeForAu() throws Exception {
        testMimeType("test.au", "audio/basic");
    }
    
    @Test
    public void shouldProvideMimeTypeForBin() throws Exception {
        testMimeType("test.bin", MediaType.OCTET_STREAM.toString());
    }
    
    @Test
    public void shouldProvideMimeTypeForEmf() throws Exception {
        testMimeType("test.emf", "application/x-msmetafile");
    }
    
    @Test
    public void shouldProvideMimeTypeForFli() throws Exception {
        testMimeType("test.fli", "video/x-fli");
    }
    
    @Test
    public void shouldProvideMimeTypeForPcx() throws Exception {
        testMimeType("test.pcx", "image/x-pcx");
    }
    
    @Test
    public void shouldProvideMimeTypeForPict() throws Exception {
        testMimeType("test.pict", "image/x-pict");
    }
    
    @Test
    public void shouldProvideMimeTypeForPsd() throws Exception {
        testMimeType("test.psd", "image/vnd.adobe.photoshop");
    }
    
    @Test
    public void shouldProvideMimeTypeForTar() throws Exception {
        testMimeType("test.tar", "application/x-tar");
    }
    
    @Test
    public void shouldProvideMimeTypeForPdf() throws Exception {
        testMimeType("modeshape_pdfcontext.pdf", "application/pdf");
    }
    
    @Test
    public void shouldProvideMimeTypeForOpenOfficePresentation() throws Exception {
        testMimeType("component-architecture.odp", "application/vnd.oasis.opendocument.presentation");
    }
    
    @Test
    public void shouldProvideMimeTypeForXml() throws Exception {
        testMimeType("master.xml", MediaType.APPLICATION_XML.toString());
    }
    
    @Test
    public void shouldProvideMimeTypeForXsd() throws Exception {
        testMimeType("xsd_file.xsd", MediaType.APPLICATION_XML.toString());
    }
    
    @Test
    public void shouldProvideMimeTypeForWsdl() throws Exception {
        testMimeType("uddi_api_v3_portType.wsdl", "application/wsdl+xml");
    }
    
    @Test
    public void shouldProvideMimeTypeForBitmap() throws Exception {
        testMimeType("test.bmp", "image/bmp");
    }
    
    @Test
    public void shouldProvideMimeTypeForPng() throws Exception {
        testMimeType("test.png", "image/png");
    }
    
    @Test
    public void shouldProvideMimeTypeForJpg() throws Exception {
        testMimeType("test.jpg", "image/jpeg");
    }
    
    @Test
    public void shouldProvideMimeTypeForOgg() throws Exception {
        testMimeType("test.ogg", "audio/ogg");
    }
    
    @Test
    public void shouldProvideMimeTypeForWave() throws Exception {
        testMimeType("test.wav", "audio/x-wav");
    }
    
    @Test
    public void shouldProvideMimeTypeForJavaClass() throws Exception {
        testMimeType("test_1.2.class", "application/java-vm");
    }
    
    @Test
    public void shouldProvideMimeTypeForCND() throws Exception {
        testMimeType("aircraft.cnd", "text/jcr-cnd");
    }
    
    private void testMimeType(String filename, String expectedMimeType) throws IOException, RepositoryException {
        assertEquals(expectedMimeType, detector.mimeTypeOf(filename, null));
    }
}
