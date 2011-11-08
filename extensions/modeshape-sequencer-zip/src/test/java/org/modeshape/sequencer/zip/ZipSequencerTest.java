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
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.*;
import org.modeshape.graph.sequencer.MockSequencerContext;
import org.modeshape.graph.sequencer.MockSequencerOutput;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Michael Trezzi
 * @author Horia Chiorean
 */
public class ZipSequencerTest {

    private static final org.modeshape.common.util.Logger LOGGER = Logger.getLogger(ZipSequencerTest.class);

    private InputStream zipStream;
    private MockSequencerContext seqContext;
    private MockSequencerOutput seqOutput;

    @Before
    public void beforeEach() {
        seqContext = new MockSequencerContext();
        seqOutput = new MockSequencerOutput(seqContext);
    }

    @After
    public void afterEach() throws Exception {
        logPotentialErrors();
        closeZipStream();
    }

    private void logPotentialErrors() {
        if (seqContext.getProblems().hasErrors()) {
            seqContext.getProblems().writeTo(LOGGER);
        }
    }

    private void closeZipStream() throws IOException {
        if (zipStream != null) {
            try {
                zipStream.close();
            } finally {
                zipStream = null;
            }
        }
    }

    @Test
    public void shouldBeAbleToExtractZip() {
        loadZipStream("testzip.zip");

        new ZipSequencer().sequence(zipStream, seqOutput, seqContext);

        PathFactory pathFactory = seqContext.getValueFactories().getPathFactory();
        NameFactory nameFactory = seqContext.getValueFactories().getNameFactory();
        ValueFactory<String> stringFactory = seqContext.getValueFactories().getStringFactory();

        Name folderName = nameFactory.create("test subfolder");
        Name fileName = nameFactory.create("test2.txt");

        Path nodePath = pathFactory.createRelativePath(ZipLexicon.CONTENT, folderName, fileName, JcrLexicon.CONTENT);

        Property property = seqOutput.getProperty(nodePath, JcrLexicon.DATA);
        assertThat(property, is(notNullValue()));
        assertThat(stringFactory.create(property.getFirstValue()), is("This is a test content of file2\n"));
    }

    private void loadZipStream( String resourcePath ) {
        zipStream = this.getClass().getResourceAsStream("/" + resourcePath);
        assertNotNull(zipStream);
    }
}
