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
package org.jboss.dna.graph.property.basic;

import java.util.Collections;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.Inflector;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.InvalidPathException;
import org.jboss.dna.graph.property.Path;

/**
 * A basic implementation of {@link Path}.
 * 
 * @author Randall Hauch
 * @author John Verhaeg
 */
@Immutable
public class BasicPath extends AbstractPath {

    /**
     * The initial serializable version. Version {@value}
     */
    private static final long serialVersionUID = 1L;

    private static final List<Segment> EMPTY_SEGMENTS = Collections.emptyList();

    public static final Path EMPTY_RELATIVE = new BasicPath(EMPTY_SEGMENTS, false);

    public static final Path SELF_PATH = new BasicPath(Collections.singletonList(Path.SELF_SEGMENT), false);

    public static final Path PARENT_PATH = new BasicPath(Collections.singletonList(Path.PARENT_SEGMENT), false);

    private final List<Segment> segments;
    private final boolean absolute;
    private final boolean normalized;

    /**
     * @param segments the segments
     * @param absolute true if this path is absolute, or false otherwise
     */
    public BasicPath( List<Segment> segments,
                      boolean absolute ) {
        assert segments != null;
        this.segments = Collections.unmodifiableList(segments);
        this.absolute = absolute;
        this.normalized = isNormalized(this.segments);
    }

    /**
     * {@inheritDoc}
     */
    public Path getAncestor( int degree ) {
        CheckArg.isNonNegative(degree, "degree");
        if (degree == 0) return this;
        int endIndex = this.segments.size() - degree;
        if (endIndex == 0) return this.isAbsolute() ? RootPath.INSTANCE : null;
        if (endIndex < 0) {
            String msg = GraphI18n.pathAncestorDegreeIsInvalid.text(this.getString(), Inflector.getInstance().ordinalize(degree));
            throw new InvalidPathException(msg);
        }
        return subpath(0, endIndex);
    }

    /**
     * {@inheritDoc}
     */
    public List<Segment> getSegmentsList() {
        return this.segments;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAbsolute() {
        return this.absolute;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isNormalized() {
        return this.normalized;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRoot() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return this.segments.size();
    }

}
