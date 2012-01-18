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
package org.modeshape.sequencer.msoffice;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import java.io.InputStream;

/**
 * Unit test for {@link MSOfficeMetadataSequencer}
 *
 * @author ?
 * @author Horia Chiorean
 */
public class MSOfficeMetadataTest {

    @Test
    public void shouldBeAbleToCreateMetadataForWord() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("word.doc");
        assertMetadata(is);
    }

    @Test
    public void shouldBeAbleToCreateMetadataForExcel() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("excel.xls");
        assertMetadata(is);
    }

    @Test
    public void shouldBeAbleToCreateMetadataForPowerpoint() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("powerpoint.ppt");
        assertMetadata(is);
    }

    private void assertMetadata( InputStream is ) throws Exception {
        MSOfficeMetadata officeMetadata = MSOfficeMetadataReader.instance(is);
        assertThat(officeMetadata.getComment(), is("Test Comment"));
        assertThat(officeMetadata.getAuthor(), is("Michael Trezzi"));
        assertThat(officeMetadata.getKeywords(), is("jboss, test, dna"));
        assertThat(officeMetadata.getTitle(), is("Test Document"));
        assertThat(officeMetadata.getSubject(), is("Test Subject"));
    }

}

