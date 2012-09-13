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

import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.AU;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.FLI;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OCTET_STREAM;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.PCX;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.PHOTOSHOP;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.TAR;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.XML_DTD;
import org.junit.Test;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;

/**
 * Unit test for {@link org.modeshape.jcr.mimetype.MimeTypeDetectors}.
 * 
 * @author Horia Chiorean
 */
public class MimeTypeDetectorsTest extends ApertureMimeTypeDetectorTest {

    @Override
    protected MimeTypeDetector getDetector() {
        return new MimeTypeDetectors();
    }

    @Test
    @Override
    public void shouldProvideMimeTypeForDtd() throws Exception {
        testMimeType("test.dtd", XML_DTD);
    }

    @Override
    public void shouldProvideMimeTypeForAu() throws Exception {
        testMimeType("test.au", AU);
    }

    @Override
    public void shouldProvideMimeTypeForBin() throws Exception {
        testMimeType("test.bin", OCTET_STREAM);
    }

    @Override
    public void shouldProvideMimeTypeForEmf() throws Exception {
        testMimeType("test.emf", OCTET_STREAM);
    }

    @Override
    public void shouldProvideMimeTypeForFli() throws Exception {
        testMimeType("test.fli", FLI);
    }

    @Override
    public void shouldProvideMimeTypeForPcx() throws Exception {
        testMimeType("test.pcx", PCX);
    }

    @Override
    public void shouldProvideMimeTypeForPict() throws Exception {
        testMimeType("test.pict", OCTET_STREAM);
    }

    @Override
    public void shouldProvideMimeTypeForPsd() throws Exception {
        testMimeType("test.psd", PHOTOSHOP);
    }

    @Override
    public void shouldProvideMimeTypeForTar() throws Exception {
        testMimeType("test.tar", TAR);
    }
}
