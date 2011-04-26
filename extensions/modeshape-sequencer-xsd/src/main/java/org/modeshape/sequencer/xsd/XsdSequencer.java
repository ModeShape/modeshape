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

package org.modeshape.sequencer.xsd;

import java.io.InputStream;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * A sequencer that processes and extract the schema object model from XML Schema Document files.
 */
public class XsdSequencer implements StreamSequencer {

    protected static final boolean TRACE = true;

    private String[] localPathsToCachedSchemas;

    /**
     * Get the paths to the local cache of XSD, DTD and other files that may be referenced or imported into the XML Schema
     * documents.
     * 
     * @return the paths to directories on the file system where this sequencer should look for cached XML Schema documents, DTD
     *         files, and other imported or referenced files; may be null or empty
     */
    public String[] getLocalPathsToCachedSchemas() {
        return localPathsToCachedSchemas;
    }

    /**
     * Set the paths to the local cache of XSD, DTD and other files that may be referenced or imported into the XML Schema
     * documents.
     * 
     * @param localPathsToCachedSchemas the paths to directories on the file system where this sequencer should look for cached
     *        XML Schema documents, DTD files, and other imported or referenced files; may be null or empty
     */
    public void setLocalPathsToCachedSchemas( String[] localPathsToCachedSchemas ) {
        this.localPathsToCachedSchemas = localPathsToCachedSchemas != null && localPathsToCachedSchemas.length != 0 ? localPathsToCachedSchemas : null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.StreamSequencer#sequence(java.io.InputStream,
     *      org.modeshape.graph.sequencer.SequencerOutput, org.modeshape.graph.sequencer.StreamSequencerContext)
     */
    @Override
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        assert stream != null;
        assert output != null;
        assert context != null;

        // Figure out the name of the archive file ...
        Path pathToArchiveFile = context.getInputPath();
        Name xsdName = null;
        if (pathToArchiveFile != null && !pathToArchiveFile.isRoot()) {
            // Remove the 'jcr:content' node (of type 'nt:resource'), if it is there ...
            if (pathToArchiveFile.getLastSegment().getName().equals(JcrLexicon.CONTENT)) pathToArchiveFile = pathToArchiveFile.getParent();
            if (!pathToArchiveFile.isRoot()) xsdName = pathToArchiveFile.getLastSegment().getName();
        }
        assert xsdName != null;
        final Path docPath = context.getValueFactories().getPathFactory().createRelativePath(xsdName);

        // Parse the XSD and generate the derived content, writing any errors to context.getProblems() ...
        new XsdReader(output, context).read(stream, docPath);
    }
}
