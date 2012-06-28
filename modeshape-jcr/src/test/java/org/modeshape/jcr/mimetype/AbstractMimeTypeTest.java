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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.modeshape.jcr.InMemoryTestBinary;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import java.io.InputStream;

/**
 * Base test class for the unit tests of {@link MimeTypeDetector} implementations
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractMimeTypeTest {

    protected void testMimeType( String name, String mimeType ) throws Exception {
        String filePath = "mimetype/" + name;

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        String actualMimeType = getDetector().mimeTypeOf(name, new InMemoryTestBinary(inputStream));

        if (mimeType == null) {
            assertNull(actualMimeType);
        }
        else {
            assertThat(actualMimeType, is(mimeType));
        }
    }
    
    protected abstract MimeTypeDetector getDetector();
}
