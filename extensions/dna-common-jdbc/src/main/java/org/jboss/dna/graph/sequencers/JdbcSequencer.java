/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.sequencers;

import java.sql.Connection;
import org.jboss.dna.graph.sequencer.SequencerContext;
import org.jboss.dna.graph.sequencer.SequencerOutput;

/**
 * The interface for a DNA sequencer that processes a JDBC connection (for instance, JDBC Metadata) and stores in the repository.
 * <p>
 * Implementations must provide a no-argument constructor.
 * </p>
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface JdbcSequencer {

    /**
     * Sequence the data found in the a JDBC connection (for instance, JDBC Metadata).
     * 
     * @param connection the JDBC connection to be sequenced; never <code>null</code>
     * @param output the output from the sequencing operation; never <code>null</code>
     * @param context the context for the sequencing operation; never <code>null</code>
     */
    void sequence( Connection connection,
                   SequencerOutput output,
                   SequencerContext context );
}
