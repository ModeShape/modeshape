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
package org.modeshape.web.jcr.rest.client;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import org.junit.Test;

/**
 *
 */
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
