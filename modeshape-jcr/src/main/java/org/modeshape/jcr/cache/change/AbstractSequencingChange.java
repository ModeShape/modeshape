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
 * Base class for the changes involving sequencing
 * 
 * @author Horia Chiorean
 */
public abstract class AbstractSequencingChange extends AbstractNodeChange {

    private static final long serialVersionUID = 1L;

    private final String userId;
    private final String selectedPath;
    private final String outputPath;
    private final String sequencerName;

    protected AbstractSequencingChange( NodeKey sequencedNodeKey,
                                        Path sequencedNodePath,
                                        String outputPath,
                                        String userId,
                                        String selectedPath,
                                        String sequencerName ) {
        super(sequencedNodeKey, sequencedNodePath);
        assert outputPath != null;
        assert userId != null;
        assert selectedPath != null;
        assert sequencerName != null;
        this.outputPath = outputPath;
        this.userId = userId;
        this.selectedPath = selectedPath;
        this.sequencerName = sequencerName;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getSelectedPath() {
        return selectedPath;
    }

    public String getSequencerName() {
        return sequencerName;
    }

    public String getUserId() {
        return userId;
    }
}
