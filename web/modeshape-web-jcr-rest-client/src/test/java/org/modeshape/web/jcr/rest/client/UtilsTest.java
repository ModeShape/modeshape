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
package org.modeshape.web.jcr.rest.client;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import org.junit.Test;

/**
 *
 */
@SuppressWarnings( "deprecation" )
public final class UtilsTest {

    // ===========================================================================================================================
    // Constants
    // ===========================================================================================================================

    private static final String DEFAULT_MIMETYPE = "application/octet-stream";

    private static final String TEXT_MIMETYPE = "text/plain";

    private static final String XML_MIMETYPE = "application/xml";

    private static final String[] TEXT_EXTENSIONS = new String[] {"classpath", "debug", "epf", "ini", "lock", "mappings", "mf",
        "prefs", "properties", "readme", "svn-base"};

    private static final String[] XML_EXTENSIONS = new String[] {"launch", "project", "template", "xsd"};

    // ===========================================================================================================================
    // Tests
    // ===========================================================================================================================

    @Test
    public void shouldHaveCorrectMimetypeForEclipseTextFiles() {
        for (String extension : TEXT_EXTENSIONS) {
            String mimetype = Utils.getMimeType(new File('.' + extension));
            assertThat(mimetype, is(TEXT_MIMETYPE));
        }
    }

    @Test
    public void shouldHaveCorrectMimetypeForEclipseXmlFiles() {
        for (String extension : XML_EXTENSIONS) {
            String mimetype = Utils.getMimeType(new File('.' + extension));
            assertThat(mimetype, is(XML_MIMETYPE));
        }
    }

    @Test
    public void shouldUseDefaultMimetypeIfUnknownExtension() {
        String mimetype = Utils.getMimeType(new File('.' + "bogusExtension"));
        assertThat(mimetype, is(DEFAULT_MIMETYPE));
    }

    @Test
    public void shouldBeEquivalentIfBothObjectsAreNull() {
        assertThat(Utils.equivalent(null, null), is(true));
    }

    @Test
    public void shouldBeEquivalentIfObjectsAreEqual() {
        String object = "object";
        assertThat(Utils.equivalent(object, object), is(true));

        String object2 = new String(object);
        assertThat(Utils.equivalent(object, object2), is(true));
    }

    @Test
    public void shouldNotBeEquivalentIfOnlyOneObjectIsNull() {
        assertThat(Utils.equivalent(new Object(), null), is(false));
        assertThat(Utils.equivalent(null, new Object()), is(false));
    }

}
