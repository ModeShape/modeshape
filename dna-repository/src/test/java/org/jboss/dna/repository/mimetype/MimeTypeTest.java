/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.mimetype;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.InputStream;
import org.junit.Test;

/**
 * @author John Verhaeg
 */
public class MimeTypeTest {

    private final void testMimeType( String name,
                                     String mimeType ) throws Exception {
        InputStream content = new File("src/test/resources/" + name).toURL().openStream();
        assertThat(MimeType.of(name, content), is(mimeType));
    }

    @Test
    public void shouldProvideDefaultTextMimeTypeWhenNoDetectorsRegisteredAndNoNullsInContent() throws Exception {
        testMimeType("test.txt", "text/plain");
    }

    @Test
    public void shouldProvideDefaultBinaryMimeTypeWhenNoDetectorsRegisteredAndNoNullsInContent() throws Exception {
        testMimeType("docs/plain-text-utf16be.txt", "application/octet-stream");
    }
}
