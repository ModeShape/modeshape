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
package org.jboss.dna.repository.sequencer;

import org.jboss.dna.common.component.ComponentLibrary;
import org.jboss.dna.graph.sequencer.StreamSequencer;

/**
 * @author Randall Hauch
 */
public class SequencerLibrary extends ComponentLibrary<Sequencer, SequencerConfig> {

    /**
     * 
     */
    public SequencerLibrary() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Sequencer doCreateInstance( Class<?> componentClass ) throws InstantiationException, IllegalAccessException {
        Object sequencerImpl = componentClass.newInstance();
        if (sequencerImpl instanceof StreamSequencer) {
            sequencerImpl = new StreamSequencerAdapter((StreamSequencer)sequencerImpl);
        }
        return (Sequencer)sequencerImpl;
    }

}
