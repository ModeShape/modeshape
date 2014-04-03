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
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.transaction.Transaction;
import org.modeshape.jcr.NodeTypeSchemata;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.engine.NoOpQueryIndexWriter;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.IndexWriter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * A composition of multiple QueryIndexWriter instances.
 */
public class CompositeIndexWriter implements IndexWriter {

    public static IndexWriter create( Iterable<IndexProvider> providers ) {
        final List<IndexWriter> writers = new ArrayList<>();
        for (IndexProvider provider : providers) {
            if (provider != null) {
                IndexWriter writer = provider.getQueryIndexWriter();
                if (writer != null && !writer.canBeSkipped()) writers.add(writer);
            }
        }
        if (writers.isEmpty()) return NoOpQueryIndexWriter.INSTANCE;
        if (writers.size() == 1) return writers.get(0);
        return new CompositeIndexWriter(writers);
    }

    protected static final class Context implements IndexingContext {
        private final Transaction txn;

        protected Context( Transaction txn ) {
            this.txn = txn;
        }

        @Override
        public Transaction getTransaction() {
            return txn;
        }
    }

    private final List<IndexWriter> writers;

    protected CompositeIndexWriter( List<IndexWriter> writers ) {
        this.writers = writers;
    }

    @Override
    public IndexingContext createIndexingContext( Transaction txn ) {
        return new Context(txn);
    }

    @Override
    public boolean initializedIndexes() {
        for (IndexWriter writer : writers) {
            if (!writer.initializedIndexes()) return false;
        }
        return true;
    }

    @Override
    public boolean canBeSkipped() {
        return writers.isEmpty();
    }

    @Override
    public void addToIndex( String workspace,
                            NodeKey key,
                            Path path,
                            Name primaryType,
                            Set<Name> mixinTypes,
                            Iterator<Property> propertiesIterator,
                            NodeTypeSchemata schemata,
                            IndexingContext txnCtx ) {
        for (IndexWriter writer : writers) {
            writer.addToIndex(workspace, key, path, primaryType, mixinTypes, propertiesIterator, schemata, txnCtx);
        }
    }

    @Override
    public void updateIndex( String workspace,
                             NodeKey key,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes,
                             Iterator<Property> properties,
                             NodeTypeSchemata schemata,
                             IndexingContext txnCtx ) {
        for (IndexWriter writer : writers) {
            writer.updateIndex(workspace, key, path, primaryType, mixinTypes, properties, schemata, txnCtx);
        }
    }

    @Override
    public void removeFromIndex( String workspace,
                                 Iterable<NodeKey> keys,
                                 IndexingContext txnCtx ) {
        for (IndexWriter writer : writers) {
            writer.removeFromIndex(workspace, keys, txnCtx);
        }
    }

    @Override
    public void addBinaryToIndex( Binary binary,
                                  IndexingContext txnCtx ) {
        for (IndexWriter writer : writers) {
            writer.addBinaryToIndex(binary, txnCtx);
        }
    }

    @Override
    public void removeBinariesFromIndex( Iterable<String> sha1s,
                                         IndexingContext txnCtx ) {
        for (IndexWriter writer : writers) {
            writer.removeBinariesFromIndex(sha1s, txnCtx);
        }
    }

}
