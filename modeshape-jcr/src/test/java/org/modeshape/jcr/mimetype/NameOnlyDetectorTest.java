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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.apache.tika.mime.MediaType;
import org.junit.Test;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.InMemoryTestBinary;
import org.modeshape.jcr.TestingEnvironment;

/**
 * Unit test for {@link NameOnlyDetector}.
 *
 * @author Horia Chiorean
 */
public class NameOnlyDetectorTest {
    
    private static final MimeTypeDetector DETECTOR = new NameOnlyDetector(new TestingEnvironment());

    protected void testMimeType( String name,
                                 String... mimeTypes ) throws Exception {
        String filePath = "mimetype/" + name;

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        String actualMimeType = DETECTOR.mimeTypeOf(name, new InMemoryTestBinary(inputStream));

        if (mimeTypes != null && mimeTypes.length != 0) {
            Set<String> acceptableMimeTypes = new HashSet<String>();
            for (String mimeType : mimeTypes) {
                if (!StringUtil.isBlank(mimeType)) acceptableMimeTypes.add(mimeType);
            }
            assertThat("Expected " + acceptableMimeTypes + " but found " + actualMimeType,
                       acceptableMimeTypes.contains(actualMimeType), is(true));
        }
    }

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
        testMimeType("test.emf", "application/x-emf");
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
        testMimeType("test.bmp", "image/x-ms-bmp");
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
        testMimeType("test.ogg", "audio/vorbis", "audio/x-ogg", "audio/ogg");
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
}
