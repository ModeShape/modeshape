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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;

/**
 * An immutable snapshot of the referrers to a node.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
@Immutable
public final class ReferrerCounts {

    private static final Map<NodeKey, Integer> EMPTY_COUNTS = Collections.emptyMap();

    /**
     * Create a new instance of the snapshot.
     *
     * @param strongCountsByReferrerKey the map of weak reference counts keyed by referrer's node keys; may be null or empty
     * @param weakCountsByReferrerKey the map of weak reference counts keyed by referrer's node keys; may be null or empty
     * @return the counts snapshot; null if there are no referrers
     */
    public static ReferrerCounts create( Map<NodeKey, Integer> strongCountsByReferrerKey,
                                         Map<NodeKey, Integer> weakCountsByReferrerKey ) {
        if (strongCountsByReferrerKey == null) strongCountsByReferrerKey = EMPTY_COUNTS;
        if (weakCountsByReferrerKey == null) weakCountsByReferrerKey = EMPTY_COUNTS;
        if (strongCountsByReferrerKey.isEmpty() && weakCountsByReferrerKey.isEmpty()) return null;
        return new ReferrerCounts(strongCountsByReferrerKey, weakCountsByReferrerKey);
    }

    public static MutableReferrerCounts createMutable() {
        return new MutableReferrerCounts(null);
    }

    protected static int count( Integer count ) {
        return count != null ? count.intValue() : 0;
    }

    private final Map<NodeKey, Integer> strongCountsByReferrerKey;
    private final Map<NodeKey, Integer> weakCountsByReferrerKey;

    protected ReferrerCounts( Map<NodeKey, Integer> strongCountsByReferrerKey,
                              Map<NodeKey, Integer> weakCountsByReferrerKey ) {
        assert strongCountsByReferrerKey != null;
        assert weakCountsByReferrerKey != null;
        this.strongCountsByReferrerKey = strongCountsByReferrerKey;
        this.weakCountsByReferrerKey = weakCountsByReferrerKey;
    }

    /**
     * Get the set of node keys to the nodes with strong references.
     *
     * @return the referring node keys; never null but possibly empty
     */
    public Set<NodeKey> getStrongReferrers() {
        return strongCountsByReferrerKey.keySet();
    }

    /**
     * Get the set of node keys to the nodes with weak references.
     *
     * @return the referring node keys; never null but possibly empty
     */
    public Set<NodeKey> getWeakReferrers() {
        return weakCountsByReferrerKey.keySet();
    }

    /**
     * Get the number of strong references of a particular referring node.
     *
     * @param referrer the referring node
     * @return the number of references, or 0 if there are none or if {@code referrer} is null
     */
    public int countStrongReferencesFrom( NodeKey referrer ) {
        return count(strongCountsByReferrerKey.get(referrer));
    }

    /**
     * Get the number of weak references of a particular referring node.
     *
     * @param referrer the referring node
     * @return the number of references, or 0 if there are none or if {@code referrer} is null
     */
    public int countWeakReferencesFrom( NodeKey referrer ) {
        return count(weakCountsByReferrerKey.get(referrer));
    }

    /**
     * Get a mutable version of this snapshot.
     *
     * @return the mutable representation
     */
    public MutableReferrerCounts mutable() {
        return new MutableReferrerCounts(this);
    }

    public static final class MutableReferrerCounts {
        private final Map<NodeKey, Integer> strongCountsByReferrerKey = new HashMap<>();
        private final Map<NodeKey, Integer> weakCountsByReferrerKey = new HashMap<>();

        protected MutableReferrerCounts( ReferrerCounts counts ) {
            if (counts != null) {
                for (NodeKey key : counts.getStrongReferrers()) {
                    strongCountsByReferrerKey.put(key, counts.countStrongReferencesFrom(key));
                }
                for (NodeKey key : counts.getStrongReferrers()) {
                    weakCountsByReferrerKey.put(key, counts.countWeakReferencesFrom(key));
                }
            }
        }

        /**
         * Add the specified number of strong reference counts for the given key.
         *
         * @param key the referring node key
         * @param increment the number to increase; may be negative to decrease
         * @return this object for chaining method calls; never null
         */
        public MutableReferrerCounts addStrong( NodeKey key,
                                                int increment ) {
            change(key, increment, strongCountsByReferrerKey);
            return this;
        }

        /**
         * Add the specified number of weak reference counts for the given key.
         *
         * @param key the referring node key
         * @param increment the number to increase; may be negative to decrease
         * @return this object for chaining method calls; never null
         */
        public MutableReferrerCounts addWeak( NodeKey key,
                                              int increment ) {
            change(key, increment, weakCountsByReferrerKey);
            return this;
        }

        /**
         * Freeze this mutable object and create a new immutable {@link ReferrerCounts}.
         *
         * @return the immutable counts snapshot; null if there are no referrers
         */
        public ReferrerCounts freeze() {
            return create(strongCountsByReferrerKey, weakCountsByReferrerKey);
        }

        private void change( NodeKey key,
                             int increment,
                             Map<NodeKey, Integer> counts ) {
            if (key == null) return;
            int count = count(counts.get(key)) + increment;
            if (count > 0) counts.put(key, count);
            else counts.remove(key);
        }
    }

}
