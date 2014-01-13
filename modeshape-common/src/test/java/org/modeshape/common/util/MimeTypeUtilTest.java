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
package org.modeshape.common.util;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

@Deprecated
public class MimeTypeUtilTest {

    private MimeTypeUtil detector;

    @Before
    public void beforeEach() throws Exception {
        detector = new MimeTypeUtil();
    }

    @Test
    public void shouldFindMimeTypeForExtenionsInStandardPropertiesFile() {
        assertThat(detector.mimeTypeOf("something.txt"), is("text/plain"));
    }

    @Test
    public void shouldFindMimeTypeForOneCharacterExtension() {
        assertThat(detector.mimeTypeOf("something.gzip.Z"), is("application/x-compress"));
    }

    @Test
    public void shouldFindMimeTypeForTwoCharacterExtension() {
        assertThat(detector.mimeTypeOf("something.sh"), is("application/x-sh"));
    }

    @Test
    public void shouldFindMimeTypeForThreeCharacterExtension() {
        assertThat(detector.mimeTypeOf("something.png"), is("image/png"));
    }

    @Test
    public void shouldNotFindMimeTypeForNameWithoutExtension() {
        assertThat(detector.mimeTypeOf("something"), is(nullValue()));
    }

    @Test
    public void shouldNotFindMimeTypeForNameUnknownExtension() {
        assertThat(detector.mimeTypeOf("something.thisExtensionIsNotKnown"), is(nullValue()));
    }

    @Test
    public void shouldNotFindMimeTypeForZeroLengthName() {
        assertThat(detector.mimeTypeOf(""), is(nullValue()));
    }

    @Test
    public void shouldNotFindMimeTypeForNameContainingWhitespace() {
        assertThat(detector.mimeTypeOf("/t   /n  "), is(nullValue()));
    }

    @Test
    public void shouldNotFindMimeTypeForNullName() {
        assertThat(detector.mimeTypeOf((String)null), is(nullValue()));
    }

    @Test
    public void shouldNotFindMimeTypeForNullFile() {
        assertThat(detector.mimeTypeOf((File)null), is(nullValue()));
    }

    @Test
    public void shouldFindMimeTypeForUppercaseExtenionsInStandardPropertiesFile() {
        assertThat(detector.mimeTypeOf("something.TXT"), is("text/plain"));
    }

    @Test
    public void shouldFindMimeTypeForNameWithTrailingWhitespace() {
        assertThat(detector.mimeTypeOf("something.txt \t"), is("text/plain"));
    }

    @Test
    public void shouldLoadAdditionalMimeTypeMappings() {
        String iniFile = ".ini";
        assertNull(this.detector.mimeTypeOf(iniFile));

        // load custom map
        InputStream stream = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResourceAsStream("org/modeshape/common/util/additionalmime.types");
        Map<String, String> customMap = MimeTypeUtil.load(stream, null);
        assertThat(customMap.size(), is(3));

        // construct with custom map
        this.detector = new MimeTypeUtil(customMap, false);
        assertThat(this.detector.mimeTypeOf(iniFile), is("text/plain"));

        // make sure all other extensions have been loaded correctly
        assertThat(this.detector.mimeTypeOf(".properties"), is("text/plain"));
        assertThat(this.detector.mimeTypeOf(".xsd"), is("application/xml"));
    }

    @Test
    public void shouldFindMimeTypeOfHiddenFiles() {
        assertThat(this.detector.mimeTypeOf(".txt"), is("text/plain"));
    }

}
