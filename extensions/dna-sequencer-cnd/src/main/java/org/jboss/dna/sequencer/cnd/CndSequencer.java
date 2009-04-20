/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.sequencer.cnd;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.jboss.dna.cnd.CndImporter;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.io.Destination;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.sequencer.SequencerContext;
import org.jboss.dna.graph.sequencer.SequencerOutput;
import org.jboss.dna.graph.sequencer.StreamSequencer;

/**
 * 
 */
public class CndSequencer implements StreamSequencer {

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.sequencer.StreamSequencer#sequence(java.io.InputStream,
     *      org.jboss.dna.graph.sequencer.SequencerOutput, org.jboss.dna.graph.sequencer.SequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          SequencerContext context ) {
        // Create the destination that forwards to the sequencer output ...
        Destination destination = new OutputDestination(output, context);
        // Use the CND importer ...
        Path root = context.getValueFactories().getPathFactory().createRootPath();
        CndImporter importer = new CndImporter(destination, root);
        String resourceName = context.getInputPath().getString(context.getNamespaceRegistry());
        try {
            importer.importFrom(stream, context.getProblems(), resourceName);
        } catch (IOException e) {
            context.getProblems().addError(e, CndSequencerI18n.errorSequencingCndContent, e.getLocalizedMessage());
        }
    }

    protected class OutputDestination implements Destination {
        private final SequencerOutput output;
        private final SequencerContext context;

        protected OutputDestination( SequencerOutput output,
                                     SequencerContext context ) {
            this.output = output;
            this.context = context;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.io.Destination#getExecutionContext()
         */
        public ExecutionContext getExecutionContext() {
            return context;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.io.Destination#create(org.jboss.dna.graph.property.Path, java.util.List)
         */
        public void create( Path path,
                            List<Property> properties ) {
            for (Property property : properties) {
                output.setProperty(path, property.getName(), property.getValues());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.io.Destination#create(org.jboss.dna.graph.property.Path,
         *      org.jboss.dna.graph.property.Property, org.jboss.dna.graph.property.Property[])
         */
        public void create( Path path,
                            Property firstProperty,
                            Property... additionalProperties ) {
            output.setProperty(path, firstProperty.getName(), firstProperty.getValues());
            for (Property property : additionalProperties) {
                output.setProperty(path, property.getName(), property.getValues());
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.io.Destination#submit()
         */
        public void submit() {
            // nothing to call on the sequencer output ...
        }
    }

}
