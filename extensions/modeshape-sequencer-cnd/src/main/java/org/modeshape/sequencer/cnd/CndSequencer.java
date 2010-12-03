/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.modeshape.sequencer.cnd;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.modeshape.cnd.CndImporter;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.io.Destination;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;

/**
 * A sequencer of CND files.
 */
public class CndSequencer implements StreamSequencer {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.StreamSequencer#sequence(java.io.InputStream,
     *      org.modeshape.graph.sequencer.SequencerOutput, org.modeshape.graph.sequencer.StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        // Create the destination that forwards to the sequencer output ...
        Destination destination = new OutputDestination(output, context);
        // Use the CND importer ...
        Path root = context.getValueFactories().getPathFactory().createRootPath();
        CndImporter importer = new CndImporter(destination, root);
        Path inputPath = context.getInputPath();
        if (inputPath.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
            inputPath = inputPath.getParent();
        }
        String resourceName = inputPath.getLastSegment().getString(context.getNamespaceRegistry());
        try {
            importer.importFrom(stream, context.getProblems(), resourceName);
        } catch (IOException e) {
            context.getProblems().addError(e, CndSequencerI18n.errorSequencingCndContent, e.getLocalizedMessage());
        }
    }

    protected class OutputDestination implements Destination {
        private final SequencerOutput output;
        private final StreamSequencerContext context;
        private final Map<Path, AtomicInteger> paths = new HashMap<Path, AtomicInteger>();
        private final PathFactory pathFactory;

        protected OutputDestination( SequencerOutput output,
                                     StreamSequencerContext context ) {
            this.output = output;
            this.context = context;
            this.pathFactory = context.getValueFactories().getPathFactory();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.io.Destination#getExecutionContext()
         */
        public ExecutionContext getExecutionContext() {
            return context;
        }

        protected Path checkPath( Path path ) {
            path = path.relativeToRoot();
            AtomicInteger count = paths.get(path);
            if (count == null) {
                count = new AtomicInteger(1);
                paths.put(path, count);
                return path;
            }
            int snsIndex = count.incrementAndGet();
            Path parent = path.getParent();
            if (parent == null) {
                // We've actually seen this node type name before (an artifact of the way the CndImporter works),
                // so just use it ...
                return path;
            }
            return pathFactory.create(parent, path.getLastSegment().getName(), snsIndex);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.io.Destination#create(Path, Iterable)
         */
        public void create( Path path,
                            Iterable<Property> properties ) {
            path = checkPath(path);
            for (Property property : properties) {
                output.setProperty(path, property.getName(), property.getValuesAsArray());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.io.Destination#create(org.modeshape.graph.property.Path,
         *      org.modeshape.graph.property.Property, org.modeshape.graph.property.Property[])
         */
        public void create( Path path,
                            Property firstProperty,
                            Property... additionalProperties ) {
            path = checkPath(path);
            output.setProperty(path, firstProperty.getName(), firstProperty.getValues());
            for (Property property : additionalProperties) {
                output.setProperty(path, property.getName(), property.getValuesAsArray());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.io.Destination#setProperties(org.modeshape.graph.property.Path,
         *      org.modeshape.graph.property.Property[])
         */
        public void setProperties( Path path,
                                   Property... properties ) {
            path = checkPath(path);
            for (Property property : properties) {
                output.setProperty(path, property.getName(), property.getValuesAsArray());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.io.Destination#submit()
         */
        public void submit() {
            // nothing to call on the sequencer output ...
        }
    }

}
