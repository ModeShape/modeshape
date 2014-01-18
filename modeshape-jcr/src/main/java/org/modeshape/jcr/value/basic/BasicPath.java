/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.value.basic;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.value.InvalidPathException;
import org.modeshape.jcr.value.Path;

/**
 * A basic implementation of {@link Path}.
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

    private/*final*/List<Segment> segments;
    private/*final*/boolean absolute;
    private/*final*/boolean normalized;

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

    @Override
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

    @Override
    protected Iterator<Segment> getSegmentsOfParent() {
        int size = this.segments.size();
        if (size == 1) return EMPTY_PATH_ITERATOR;
        return this.segments.subList(0, size - 1).iterator();
    }

    @Override
    public List<Segment> getSegmentsList() {
        return this.segments;
    }

    @Override
    public boolean isAbsolute() {
        return this.absolute;
    }

    @Override
    public boolean isNormalized() {
        return this.normalized;
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public int size() {
        return this.segments.size();
    }

    /**
     * Custom deserialization is needed, since the 'segments' list may not be serializable (e.g., java.util.RandomAccessSubList).
     * 
     * @param aStream the input stream to which this object should be serialized; never null
     * @throws IOException if there is a problem reading from the stream
     * @throws ClassNotFoundException if there is a problem loading any required classes
     */
    @SuppressWarnings( "unchecked" )
    private void readObject( ObjectInputStream aStream ) throws IOException, ClassNotFoundException {
        absolute = aStream.readBoolean();
        normalized = aStream.readBoolean();
        segments = (List<Path.Segment>)aStream.readObject();
    }

    /**
     * Custom serialization is needed, since the 'segments' list may not be serializable (e.g., java.util.RandomAccessSubList).
     * 
     * @param aStream the input stream to which this object should be serialized; never null
     * @throws IOException if there is a problem writing to the stream
     */
    private void writeObject( ObjectOutputStream aStream ) throws IOException {
        aStream.writeBoolean(absolute);
        aStream.writeBoolean(normalized);
        aStream.writeObject(Collections.unmodifiableList(new ArrayList<Path.Segment>(segments))); // make a copy!
    }

}
