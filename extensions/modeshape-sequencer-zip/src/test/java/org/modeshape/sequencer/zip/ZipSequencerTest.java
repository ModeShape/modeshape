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

package org.modeshape.sequencer.zip;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.junit.After;
import org.junit.Test;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * @author Michael Trezzi
 */
public class ZipSequencerTest {
    private InputStream imageStream;

    @After
    public void afterEach() throws Exception {
        if (imageStream != null) {
            try {
                imageStream.close();
            } finally {
                imageStream = null;
            }
        }
    }

    protected InputStream getTestZip( String resourcePath ) {
        return this.getClass().getResourceAsStream("/" + resourcePath);
    }

    @Test
    public void shouldBeAbleToExtractZip() {
        InputStream is = getTestZip("testzip.zip");
        ZipSequencer zs = new ZipSequencer();
        StreamSequencerContext context = new MockSequencerContext();

        MockSequencerOutput seqtest = new MockSequencerOutput(context);

        zs.sequence(is, seqtest, context);

        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        NameFactory nameFactory = context.getValueFactories().getNameFactory();
        ValueFactory<String> stringFactory = context.getValueFactories().getStringFactory();

        Name folderName = nameFactory.create("test subfolder");
        Name fileName = nameFactory.create("test2.txt");

        Path nodePath = pathFactory.createRelativePath(ZipLexicon.CONTENT, folderName, fileName, JcrLexicon.CONTENT);

        Property property = seqtest.getProperty(nodePath, JcrLexicon.DATA);
        assertThat(property, is(notNullValue()));
        assertThat(stringFactory.create(property.getFirstValue()), is("This is a test content of file2\n"));
    }

}
