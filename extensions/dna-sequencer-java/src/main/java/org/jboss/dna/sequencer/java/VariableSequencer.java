/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.sequencer.java;

import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.sequencer.SequencerOutput;
import org.jboss.dna.sequencer.java.metadata.Variable;

/**
 * Sequencer for variabels.
 */
public class VariableSequencer implements JavaSourceCndDefinition {

    /**
     * Sequence a variable.
     * 
     * @param output - the {@link SequencerOutput}.
     * @param nameFactory - the {@link NameFactory}.
     * @param variable - the variable to be added in the tree.
     * @param path - the path
     */
    public static void sequenceTheVariable( SequencerOutput output,
                                            NameFactory nameFactory,
                                            Variable variable,
                                            Path path ) {
        output.setProperty(path, nameFactory.create(JAVA_VARIABLE_NAME), variable.getName());
    }
}
