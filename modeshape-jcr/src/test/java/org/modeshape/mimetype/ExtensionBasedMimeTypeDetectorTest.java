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
package org.modeshape.mimetype;

import org.junit.Test;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.*;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import org.modeshape.jcr.mimetype.ExtensionBasedMimeTypeDetector;

/**
 * Unit test for {@link ExtensionBasedMimeTypeDetector}
 * 
 * @author Randall Hauch
 * @author Horia Chiorean
 */
public class ExtensionBasedMimeTypeDetectorTest extends AbstractMimeTypeTest {

    @Override
    protected MimeTypeDetector getDetector() {
        return new ExtensionBasedMimeTypeDetector();
    }

    @Test
    public void shouldFindMimeTypeForExtenionsInStandardPropertiesFile() throws Exception {
        testMimeType("something.txt", TEXT_PLAIN);
    }

    @Test
    public void shouldFindMimeTypeForOneCharacterExtension() throws Exception {
        testMimeType("something.gzip.Z", LZW);
    }

    @Test
    public void shouldFindMimeTypeForTwoCharacterExtension() throws Exception {
        testMimeType("something.sh", SH);
    }

    @Test
    public void shouldFindMimeTypeForThreeCharacterExtension() throws Exception {
        testMimeType("something.png", PNG);
    }

    @Test
    public void shouldNotFindMimeTypeForNameWithoutExtension() throws Exception {
        testMimeType("something", null);
    }

    @Test
    public void shouldNotFindMimeTypeForNameUnknownExtension() throws Exception {
        testMimeType("something.thisExtensionIsNotKnown", null);
    }

    @Test
    public void shouldNotFindMimeTypeForZeroLengthName() throws Exception {
        testMimeType("", null);
    }

    @Test
    public void shouldNotFindMimeTypeForNameContainingWhitespace() throws Exception {
        testMimeType("/t   /n  ", null);
    }

    @Test
    public void shouldNotFindMimeTypeForNullName() throws Exception {
        testMimeType(null, null);
    }

    @Test
    public void shouldFindMimeTypeForUppercaseExtenionsInStandardPropertiesFile() throws Exception {
        testMimeType("something.TXT", TEXT_PLAIN);
    }

    @Test
    public void shouldFindMimeTypeForNameWithTrailingWhitespace() throws Exception {
        testMimeType("something.txt \t", TEXT_PLAIN);
    }

}
