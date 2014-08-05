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
package org.modeshape.jcr.cache;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.basic.BasicPathSegment;

/**
 * An immutable reference to a child node.
 */
@Immutable
public class ChildReference implements Comparable<ChildReference> {

    private final NodeKey key;
    private final Segment segment;

    public ChildReference( NodeKey key,
                           Name name,
                           int snsIndex ) {
        this.key = key;
        this.segment = new BasicPathSegment(name, snsIndex);
    }

    public ChildReference( NodeKey key,
                           Segment segment ) {
        this.key = key;
        this.segment = segment;
    }

    /**
     * @return key
     */
    public NodeKey getKey() {
        return key;
    }

    /**
     * @return name
     */
    public Name getName() {
        return segment.getName();
    }

    /**
     * @return snsIndex
     */
    public int getSnsIndex() {
        return segment.getIndex();
    }

    public Segment getSegment() {
        return segment;
    }

    @Override
    public int compareTo( ChildReference that ) {
        if (that == this) return 0;
        int diff = this.segment.compareTo(that.segment);
        if (diff != 0) return diff;
        return this.key.compareTo(that.key);
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof ChildReference) {
            ChildReference that = (ChildReference)obj;
            return this.key.equals(that.key);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return this.segment.getString() + " (key=" + key + ")";
    }

    public String getSegmentAsString( NamespaceRegistry registry ) {
        return this.segment.getString(registry);
    }

    public String toString( NamespaceRegistry registry ) {
        return "\"" + this.segment.getString(registry) + "\"=" + key;
    }

    public ChildReference with( int snsIndex ) {
        return snsIndex == segment.getIndex() ? this : new ChildReference(key, segment.getName(), snsIndex);
    }

    public ChildReference with( Name name,
                                int snsIndex ) {
        if (snsIndex == segment.getIndex() && segment.getName().equals(name)) return this;
        return new ChildReference(key, name, snsIndex);
    }

}
