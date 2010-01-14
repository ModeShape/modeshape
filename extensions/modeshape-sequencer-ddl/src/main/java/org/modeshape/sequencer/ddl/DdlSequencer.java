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
package org.modeshape.sequencer.ddl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A sequencer of DDL files.
 */
public class DdlSequencer implements StreamSequencer {
    protected Destination destination;
    protected Path outputPath;
    protected PathFactory pathFactory;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.StreamSequencer#sequence(java.io.InputStream,
     *      org.modeshape.graph.sequencer.SequencerOutput, org.modeshape.graph.sequencer.StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {

        // Create path factory

        this.pathFactory = context.getValueFactories().getPathFactory();
        
        DdlParsers parsers = new DdlParsers();

        AstNode rootNode = null;

        try {
            // Perform the parsing
            rootNode = parsers.parse(IoUtil.read(stream));
            
            
            // The AstNode objects getPath() method returns a ChildPath object which is built with a parent path. If the parent
            // path is NULL, as in the case of the parsed DDL, the parent path is set to the default ("/") which already has a parent.
            // So we need to set the root node with relative path. This creates a BasicPath path which can be directly re-parented.
            //Path nodePath = pathFactory.createRelativePath(StandardDdlLexicon.STATEMENTS_CONTAINER);
            
            Path nodePath = rootNode.getPath(context);

            for (Property property : rootNode.getProperties()) {
                output.setProperty(nodePath, property.getName(), property.getValuesAsArray());
            }

            convertAstNodesToGraphNodes(rootNode, nodePath, output, context);

        } catch (ParsingException e) {
            context.getProblems().addError(e, DdlSequencerI18n.errorParsingDdlContent, e.getLocalizedMessage());
        } catch (IOException e) {
            context.getProblems().addError(e, DdlSequencerI18n.errorSequencingDdlContent, e.getLocalizedMessage());
        }

    }

    protected void convertAstNodesToGraphNodes( AstNode parentNode, Path parentPath, SequencerOutput output, StreamSequencerContext context ) {
        // Walk the nodes and set all properties, recursively

        List<AstNode> children = parentNode.getChildren();

        for (AstNode child : children) {
            Path nodePath = child.getPath(context); //pathFactory.create(parentPath, child.getName());

            for (Property property : child.getProperties()) {
                output.setProperty(nodePath, property.getName(), property.getValuesAsArray());
            }
            convertAstNodesToGraphNodes(child, nodePath, output, context);
        }
    }

}
