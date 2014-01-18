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
package org.modeshape.jcr.cache.change;

import java.util.Set;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;

/**
 * Change representing the renaming of a node.
 */
public class NodeRenamed extends AbstractNodeChange {

    private static final long serialVersionUID = 1L;

    private final Segment oldSegment;

    public NodeRenamed( NodeKey key,
                        Path newPath,
                        Segment oldSegment,
                        Name primaryType,
                        Set<Name> mixinTypes ) {
        super(key, newPath, primaryType, mixinTypes);
        this.oldSegment = oldSegment;
        assert !this.oldSegment.equals(newPath.getLastSegment());
    }

    /**
     * Get the old segment for the node.
     * 
     * @return the old segment; never null
     */
    public Segment getOldSegment() {
        return oldSegment;
    }

    @Override
    public String toString() {
        return "Renamed node '" + this.getKey() + "' to \"" + path + "\" (was '" + oldSegment + "')";
    }
}
