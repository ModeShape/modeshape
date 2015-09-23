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
package org.modeshape.jcr.query;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.engine.NoOpQueryIndexWriter;
import org.modeshape.jcr.spi.index.IndexWriter;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * A composition of multiple QueryIndexWriter instances.
 */
public class CompositeIndexWriter implements IndexWriter {

    public static IndexWriter create( Iterable<IndexProvider> providers ) {
        final List<IndexWriter> writers = new ArrayList<>();
        for (IndexProvider provider : providers) {
            if (provider != null) {
                IndexWriter writer = provider.getIndexWriter();
                if (writer != null && !writer.canBeSkipped()) writers.add(writer);
            }
        }
        if (writers.isEmpty()) return NoOpQueryIndexWriter.INSTANCE;
        if (writers.size() == 1) return writers.get(0);
        return new CompositeIndexWriter(writers);
    }

    private final List<IndexWriter> writers;

    protected CompositeIndexWriter( List<IndexWriter> writers ) {
        this.writers = writers;
    }

    @Override
    public void clearAllIndexes() {
        for (IndexWriter writer : writers) {
            writer.clearAllIndexes();
        }
    }

    @Override
    public boolean canBeSkipped() {
        if (writers.isEmpty()) return true;
        for (IndexWriter writer : writers) {
            if (!writer.canBeSkipped()) return false;
        }
        return true;
    }

    @Override
    public void add( String workspace,
                     NodeKey key,
                     Path path,
                     Name primaryType,
                     Set<Name> mixinTypes,
                     Properties properties ) {
        for (IndexWriter writer : writers) {
            writer.add(workspace, key, path, primaryType, mixinTypes, properties);
        }
    }

    @Override
    public void remove( String workspace, NodeKey key ) {
        for (IndexWriter writer : writers) {
            writer.remove(workspace, key);
        } 
    }
}
