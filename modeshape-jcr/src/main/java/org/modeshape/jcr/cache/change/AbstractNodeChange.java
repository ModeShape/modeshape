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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * Abstract base class for all internal events.
 */
public abstract class AbstractNodeChange extends Change {

    private static final long serialVersionUID = 1L;

    private final NodeKey key;

    /**
     * An array which will contain both the primary type (on position 0) and the mixin types (on the following positions)
     */
    private final Name[] types;

    protected final Path path;

    protected AbstractNodeChange( NodeKey key,
                                  Path path,
                                  Name primaryType,
                                  Set<Name> mixinTypes ) {
        assert key != null;
        assert path != null;

        this.key = key;
        this.path = path;
        int typesCount = (mixinTypes != null ? mixinTypes.size() : 0) + 1;
        this.types = new Name[typesCount];
        this.types[0] = primaryType;
        if (typesCount > 1) {
            assert mixinTypes != null;
            System.arraycopy(mixinTypes.toArray(new Name[0]), 0, types, 1, mixinTypes.size());
        }
    }

    /**
     * Get the path to the node involved in the change.
     * 
     * @return the path; may not be null
     */
    public Path getPath() {
        return path;
    }

    /**
     * @return key
     */
    public NodeKey getKey() {
        return key;
    }

    /**
     * Returns the primary type of the node
     * 
     * @return a {@link Name} instance; never {@code null}
     */
    public Name getPrimaryType() {
        return types[0];
    }

    /**
     * Returns the mixins for this node.
     * 
     * @return a {@link Set}; never {@code null} but possibly empty.
     */
    public Set<Name> getMixinTypes() {
        if (types.length == 1) {
            return Collections.emptySet();
        }
        return new HashSet<Name>(Arrays.asList(Arrays.copyOfRange(types, 1, types.length)));
    }
}
