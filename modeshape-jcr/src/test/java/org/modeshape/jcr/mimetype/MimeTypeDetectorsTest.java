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
