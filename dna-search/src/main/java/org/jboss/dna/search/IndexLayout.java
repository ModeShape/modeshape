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
package org.jboss.dna.search;

import java.io.IOException;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.graph.ExecutionContext;

/**
 * The representation of a single layout of one or more Lucene indexes.
 */
@ThreadSafe
public interface IndexLayout {

    /**
     * Create a new session to the indexes.
     * 
     * @param context the execution context for which this session is to be established; may not be null
     * @param sourceName the name of the source; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param overwrite true if the existing indexes should be overwritten, or false if they should be used
     * @param readOnly true if the resulting session can be optimized for use in read-only situations, or false if the session
     *        needs to allow calling the write methods
     * @return the session to the indexes; never null
     */
    IndexSession createSession( ExecutionContext context,
                                String sourceName,
                                String workspaceName,
                                boolean overwrite,
                                boolean readOnly );

    /**
     * Destroy the indexes for the workspace with the supplied name.
     * 
     * @param context the execution context in which the destruction should be performed; may not be null
     * @param sourceName the name of the source; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @return true if the indexes for the workspace were destroyed, or false if there was no such workspace index
     * @throws IOException if there is a problem destroying the indexes
     */
    boolean destroyIndexes( ExecutionContext context,
                            String sourceName,
                            String workspaceName ) throws IOException;

}
