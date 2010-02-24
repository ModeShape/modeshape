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
package org.modeshape.jcr;

import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.basic.BasicPath;

/**
 * A correspondence identifier is defined to be the pair of the UUID of the nearest referenceable ancestor and the relative path
 * from that referenceable ancestor to the node of interest. If any node is a referenceable node, then the correspondence
 * identifier is just the UUID of the node. Per Section 4.10.2 of JSR-170, version 1.0.1.
 * <p>
 * Note that per Section 6.2.8, two non-referenceable nodes are the same if they have the same correspondence identifier.
 * </p>
 */
@Immutable
class CorrespondenceId {

    private static final Path NO_PATH = BasicPath.SELF_PATH;

    private final String referenceableId;
    private final Path relativePath;
    private final int hc;

    public CorrespondenceId( String referenceableId ) {
        this(referenceableId, NO_PATH);
    }

    public CorrespondenceId( String referenceableId,
                             Path relativePath ) {
        CheckArg.isNotNull(referenceableId, "referenceableId");
        CheckArg.isNotNull(relativePath, "relativePath");
        assert !relativePath.isAbsolute();
        this.referenceableId = referenceableId;
        this.relativePath = relativePath;
        this.hc = HashCode.compute(this.referenceableId, this.relativePath);
    }

    /**
     * @return referenceableId
     */
    public String getReferenceableId() {
        return referenceableId;
    }

    /**
     * @return relativePath
     */
    public Path getRelativePath() {
        return relativePath;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hc;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof CorrespondenceId) {
            CorrespondenceId that = (CorrespondenceId)obj;
            return this.referenceableId.equals(that.referenceableId) && this.relativePath.equals(that.relativePath);
        }
        return false;
    }

}
