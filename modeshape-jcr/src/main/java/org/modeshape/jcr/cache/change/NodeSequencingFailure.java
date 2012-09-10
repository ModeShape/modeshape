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

package org.modeshape.jcr.cache.change;

import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Path;

/**
 * Change which is triggered if the sequencing of a node fails
 * 
 * @author Horia Chiorean
 */
public class NodeSequencingFailure extends AbstractSequencingChange {

    private static final long serialVersionUID = 1L;

    /**
     * The cause of the sequencing failure
     */
    private Throwable cause;

    public NodeSequencingFailure( NodeKey sequencedNodeKey,
                                  Path sequencedNodePath,
                                  String outputPath,
                                  String userId,
                                  String selectedPath,
                                  String sequencerName,
                                  Throwable cause ) {
        super(sequencedNodeKey, sequencedNodePath, outputPath, userId, selectedPath, sequencerName);
        assert cause != null;
        this.cause = cause;
    }

    /**
     * Get the cause of the failure.
     * 
     * @return the exception; never null
     */
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Failure while sequencing the node at:").append(getPath()).append(" with key:").append(getKey());
        sb.append(". Cause: ").append(cause.getMessage());
        return sb.toString();
    }
}
