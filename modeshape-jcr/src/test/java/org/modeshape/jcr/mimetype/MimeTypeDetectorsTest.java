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
import static org.modeshape.jcr.mimetype.MimeTypeConstants.APPLICATION_XML;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.AU;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.FLI;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.GTAR;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.JPEG;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.OCTET_STREAM;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.OPEN_DOC_PRESENTATION;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.PCX;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.PDF;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.PHOTOSHOP;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.PNG;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.WAV;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.WSDL_XML;
import static org.modeshape.jcr.mimetype.MimeTypeConstants.XSD_XML;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.InMemoryTestBinary;

/**
 * Unit test for {@link org.modeshape.jcr.mimetype.MimeTypeDetectors}.
 * 
 * @author Horia Chiorean
 */
public class MimeTypeDetectorsTest {

    protected void testMimeType( String name,
                                 String... mimeTypes ) throws Exception {
        String filePath = "mimetype/" + name;

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        String actualMimeType = getDetector().mimeTypeOf(name, new InMemoryTestBinary(inputStream));

        if (mimeTypes != null && mimeTypes.length != 0) {
            Set<String> acceptableMimeTypes = new HashSet<String>();
            for (String mimeType : mimeTypes) {
                if (!StringUtil.isBlank(mimeType)) acceptableMimeTypes.add(mimeType);
            }
            assertThat("Expected " + acceptableMimeTypes + " but found " + actualMimeType,
                       acceptableMimeTypes.contains(actualMimeType),
                       is(true));
        }
    }

    protected MimeTypeDetector getDetector() {
        return new MimeTypeDetectors();
    }

    @Test
    public void shouldProvideMimeTypeForAu() throws Exception {
        testMimeType("test.au", AU);
    }

    @Test
    public void shouldProvideMimeTypeForBin() throws Exception {
        testMimeType("test.bin", OCTET_STREAM);
    }

    @Test
    public void shouldProvideMimeTypeForEmf() throws Exception {
        testMimeType("test.emf", "application/x-emf");
    }

    @Test
    public void shouldProvideMimeTypeForFli() throws Exception {
        testMimeType("test.fli", FLI);
    }

    @Test
    public void shouldProvideMimeTypeForPcx() throws Exception {
        testMimeType("test.pcx", PCX);
    }

    @Test
    public void shouldProvideMimeTypeForPict() throws Exception {
        testMimeType("test.pict", OCTET_STREAM);
    }

    @Test
    public void shouldProvideMimeTypeForPsd() throws Exception {
        testMimeType("test.psd", PHOTOSHOP);
    }

    @Test
    public void shouldProvideMimeTypeForTar() throws Exception {
        testMimeType("test.tar", GTAR);
    }

    @Test
    public void shouldProvideMimeTypeForPdf() throws Exception {
        testMimeType("modeshape_pdfcontext.pdf", PDF);
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficePresentation() throws Exception {
        testMimeType("component-architecture.odp", OPEN_DOC_PRESENTATION);
    }

    @Test
    public void shouldProvideMimeTypeForXml() throws Exception {
        testMimeType("master.xml", APPLICATION_XML);
    }

    @SuppressWarnings( "deprecation" )
    @Test
    public void shouldProvideMimeTypeForXsd() throws Exception {
        testMimeType("xsd_file.xsd", APPLICATION_XML, XSD_XML);
    }

    @Test
    public void shouldProvideMimeTypeForWsdl() throws Exception {
        testMimeType("uddi_api_v3_portType.wsdl", WSDL_XML);
    }

    @Test
    public void shouldProvideMimeTypeForBitmap() throws Exception {
        testMimeType("test.bmp", "image/x-ms-bmp");
    }

    @Test
    public void shouldProvideMimeTypeForPng() throws Exception {
        testMimeType("test.png", PNG);
    }

    @Test
    public void shouldProvideMimeTypeForJpg() throws Exception {
        testMimeType("test.jpg", JPEG);
    }

    @Test
    public void shouldProvideMimeTypeForOgg() throws Exception {
        testMimeType("test.ogg", "audio/vorbis", "audio/x-ogg", "audio/ogg");
    }

    @Test
    public void shouldProvideMimeTypeForWave() throws Exception {
        testMimeType("test.wav", WAV);
    }

    @Test
    public void shouldProvideMimeTypeForJavaClass() throws Exception {
        testMimeType("test_1.2.class", "application/java-vm");
    }
}
