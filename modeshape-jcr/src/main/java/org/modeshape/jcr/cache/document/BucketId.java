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
package org.modeshape.jcr.cache.document;

import java.util.Objects;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.SecureHash;
import org.modeshape.jcr.value.Name;

/**
 * Abstraction for the ID of a bucket.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @see BucketedChildReferences
 */
@ThreadSafe
@Immutable
final class BucketId implements Comparable<BucketId> {
    private final String id;

    protected BucketId( Name name, int length ) {
        this(name.getString(), length);
    }

    protected BucketId( String nameString, int length ) {
        String sha1 = SecureHash.sha1(nameString);
        this.id = sha1.substring(0, length);
    }

    protected BucketId( String id ) {
        this.id = id;
    }

    @Override
    public int compareTo( BucketId o ) {
        return id.compareTo(o.id);
    }

    @Override
    public boolean equals( Object o ) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        BucketId bucketId = (BucketId)o;
        return Objects.equals(id, bucketId.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
