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

import java.util.Iterator;
import java.util.Set;
import javax.transaction.Transaction;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.NodeTypeSchemata;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.spi.index.provider.IndexWriter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

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
    public IndexingContext createIndexingContext( Transaction txn ) {
        return null;
    }

    @Override
    public void clearAllIndexes() {
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
    }

    @Override
    public void removeFromIndex( String workspace,
                                 Iterable<NodeKey> keys,
                                 IndexingContext txnCtx ) {
    }

    @Override
    public void addBinaryToIndex( Binary binary,
                                  IndexingContext txnCtx ) {
    }

    @Override
    public void removeBinariesFromIndex( Iterable<String> sha1s,
                                         IndexingContext txnCtx ) {
    }

}
