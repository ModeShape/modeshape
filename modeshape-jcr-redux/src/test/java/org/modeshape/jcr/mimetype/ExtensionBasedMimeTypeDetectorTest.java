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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class ExtensionBasedMimeTypeDetectorTest {

    private ExtensionBasedMimeTypeDetector detector;
    private InputStream stream;

    @Before
    public void beforeEach() throws Exception {
        detector = new ExtensionBasedMimeTypeDetector();
        stream = null;
    }

    @Test
    public void shouldFindMimeTypeForExtenionsInStandardPropertiesFile() {
        assertThat(detector.mimeTypeOf("something.txt", stream), is("text/plain"));
    }

    @Test
    public void shouldFindMimeTypeForOneCharacterExtension() {
        assertThat(detector.mimeTypeOf("something.gzip.Z", stream), is("application/x-compress"));
    }

    @Test
    public void shouldFindMimeTypeForTwoCharacterExtension() {
        assertThat(detector.mimeTypeOf("something.sh", stream), is("application/x-sh"));
    }

    @Test
    public void shouldFindMimeTypeForThreeCharacterExtension() {
        assertThat(detector.mimeTypeOf("something.png", stream), is("image/png"));
    }

    @Test
    public void shouldNotFindMimeTypeForNameWithoutExtension() {
        assertThat(detector.mimeTypeOf("something", stream), is(nullValue()));
    }

    @Test
    public void shouldNotFindMimeTypeForNameUnknownExtension() {
        assertThat(detector.mimeTypeOf("something.thisExtensionIsNotKnown", stream), is(nullValue()));
    }

    @Test
    public void shouldNotFindMimeTypeForZeroLengthName() {
        assertThat(detector.mimeTypeOf(null, stream), is(nullValue()));
    }

    @Test
    public void shouldNotFindMimeTypeForNameContainingWhitespace() {
        assertThat(detector.mimeTypeOf("/t   /n  ", stream), is(nullValue()));
    }

    @Test
    public void shouldNotFindMimeTypeForNullName() {
        assertThat(detector.mimeTypeOf(null, stream), is(nullValue()));
    }

    @Test
    public void shouldFindMimeTypeForUppercaseExtenionsInStandardPropertiesFile() {
        assertThat(detector.mimeTypeOf("something.TXT", stream), is("text/plain"));
    }

    @Test
    public void shouldFindMimeTypeForNameWithTrailingWhitespace() {
        assertThat(detector.mimeTypeOf("something.txt \t", stream), is("text/plain"));
    }

}
