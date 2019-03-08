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
package org.modeshape.jcr.spi.index;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * Interface used to record in the indexes the changes to content.
 * 
 * @see IndexProvider#getIndexWriter()
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface IndexWriter {
    public static IndexWriter noop() {
        return new IndexWriter() {

            @Override
            public boolean canBeSkipped() {
                return true;
            }

            @Override
            public void clearAllIndexes() {
            }

            @Override
            public boolean add( String workspace,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties ) {
                return false;
            }

            @Override
            public boolean remove( String workspace,
                                   NodeKey key ) {
                return false;
            }

            @Override
            public void commit( String workspace ) {
            }
        };
    }

    public static IndexWriter compose(Iterable<IndexWriter> delegates, Consumer<Exception> handler) {
        Objects.requireNonNull(delegates, "delegates");
        Objects.requireNonNull(handler, "handler");

        List<IndexWriter> useDelegates = StreamSupport.stream(delegates.spliterator(),
                                                              false).filter(t -> !t.canBeSkipped()).collect(Collectors.toList());        

        if (useDelegates.isEmpty()) {
            return noop();
        }

        return new IndexWriter() {

            @Override
            public boolean remove( String workspace,
                                   NodeKey key ) {
                boolean result = false;
                for (IndexWriter indexWriter : delegates) {
                    try {
                        result = indexWriter.remove(workspace, key) || result;
                    } catch (Exception e) {
                        handler.accept(e);
                    }
                }
                return result;
            }

            @Override
            public void commit( String workspace ) {
                delegates.forEach(d -> {
                    try {
                        d.commit(workspace);
                    } catch (Exception e) {
                        handler.accept(e);
                    }
                });
            }

            @Override
            public void clearAllIndexes() {
                delegates.forEach(t -> {
                    try {
                        t.clearAllIndexes();
                    } catch (Exception e) {
                        handler.accept(e);
                    }
                });
            }

            @Override
            public boolean canBeSkipped() {
                for (IndexWriter indexWriter : delegates) {
                    try {
                        if (!indexWriter.canBeSkipped()) return false;
                    } catch (Exception e) {
                        handler.accept(e);
                    }
                }
                return true;
            }

            @Override
            public boolean add( String workspace,
                                NodeKey key,
                                Path path,
                                Name primaryType,
                                Set<Name> mixinTypes,
                                Properties properties ) {
                boolean result = false;
                for (IndexWriter indexWriter : delegates) {
                    try {
                        result = indexWriter.add(workspace, key, path, primaryType, mixinTypes, properties) || result;
                    } catch (Exception e) {
                        handler.accept(e);
                    }
                }
                return result;
            }
        };
    }

    /**
     * Flag that defines whether this index may be skipped. This is usually the case when the writer has no indexes behind it.
     * 
     * @return true if this index writer does not need to be called, or false otherwise
     */
    boolean canBeSkipped();

    /**
     * Clear all indexes of content. This method is typically called prior to a re-indexing operation.
     */
    void clearAllIndexes();

    /**
     * Add to the index the information about a node.
     *
     * @param workspace the workspace in which the node information should be available; may not be null
     * @param key the unique key for the node; may not be null
     * @param path the path of the node; may not be null
     * @param primaryType the primary type of the node; may not be null
     * @param mixinTypes the mixin types for the node; may not be null but may be empty
     * @param properties the properties of the node; may not be null but may be empty
     * @return {@code true} if the operation was successful and data was written to at least one index, {@code false} otherwise
     */
    boolean add( String workspace,
                 NodeKey key,
                 Path path,
                 Name primaryType,
                 Set<Name> mixinTypes,
                 Properties properties );

    /**
     * Removes information from the indexes about a node.
     *
     * @param workspace the workspace to which the node belongs; may not be null
     * @param key a {@link NodeKey} instance, never {@code null}
     * @return {@code true} if the operation was successful and data was written to at least one index, {@code false} otherwise
     */
    boolean remove( String workspace, NodeKey key );

    /**
     * Commits changes made to the indexes for a particular workspace.
     * 
     * @param workspace the workspace to which the node belongs; may not be null
     */
    void commit(String workspace);

}
