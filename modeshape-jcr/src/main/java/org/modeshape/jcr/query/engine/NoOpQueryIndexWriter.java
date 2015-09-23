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
package org.modeshape.jcr.query.engine;

import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.spi.index.IndexWriter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
@Immutable
public final class NoOpQueryIndexWriter implements IndexWriter {

    public static final NoOpQueryIndexWriter INSTANCE = new NoOpQueryIndexWriter();

    private NoOpQueryIndexWriter() {
    }

    @Override
    public boolean canBeSkipped() {
        return true;
    }

    @Override
    public void clearAllIndexes() {
    }

    @Override
    public void add( String workspace,
                     NodeKey key,
                     Path path,
                     Name primaryType,
                     Set<Name> mixinTypes,
                     Properties properties ) {
    }

    @Override
    public void remove( String workspace, NodeKey key ) {
        
    }
}
