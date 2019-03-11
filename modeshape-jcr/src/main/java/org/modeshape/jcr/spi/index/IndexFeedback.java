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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.value.Path;

/**
 * A feedback mechanism to allow a {@link IndexProvider} to signal to ModeShape that one or more indexes need to be rebuilt. The
 * {@link IndexProvider} calls the {@link #scan(String, IndexingCallback)} and {@link #scan(String, IndexingCallback, Path)}
 * methods one or multiple times, depending upon which portion(s) of the repository content need to be scanned and reindexed.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface IndexFeedback {

    public interface IndexingCallback {
        public static IndexingCallback noop() {
            return new IndexingCallback() {
                
                @Override
                public IndexWriter writer() {
                    return IndexWriter.noop();
                }
                
                @Override
                public void beforeIndexing() {
                }
                
                @Override
                public void afterIndexing() {
                }
            };
        }

        public static IndexingCallback compose(Iterable<IndexingCallback> delegates, Consumer<Exception> handler) {
            Objects.requireNonNull(delegates, "delegates");
            Objects.requireNonNull(handler, "handler");

            List<IndexingCallback> useDelegates = delegates instanceof List<?> ? (List<IndexingCallback>)delegates : StreamSupport.stream(delegates.spliterator(),
                                                                                                                                          false).collect(Collectors.toList());

            if (useDelegates.isEmpty()) {
                return noop();
            }

            return new IndexingCallback() {
                
                @Override
                public IndexWriter writer() {
                    return IndexWriter.compose(() -> useDelegates.stream().map(IndexingCallback::writer).iterator(), handler);
                }

                @Override
                public void beforeIndexing() {
                    for (IndexingCallback indexingCallback : useDelegates) {
                        try {
                            indexingCallback.beforeIndexing();
                        } catch (Exception e) {
                            handler.accept(e);
                        }
                    }
                }

                @Override
                public void afterIndexing() {
                    for (IndexingCallback indexingCallback : useDelegates) {
                        try {
                            indexingCallback.afterIndexing();
                        } catch (Exception e) {
                            handler.accept(e);
                        }
                    }
                }
            };
        }

        void beforeIndexing();

        void afterIndexing();
        
        IndexWriter writer();
    }

    /**
     * Signal that a portion of the repository content at/under the given path in the specified workspace should be scanned.
     *
     * @param workspaceName the name of the workspace containing the content to be scanned; may not be null
     * @param callback the callback that should be invoked when the reindexing has completed; may be null if no callback is
     *        required
     * @param path the path at/under which the content should be scanned and reindexed; may not be null
     */
    void scan( String workspaceName,
               IndexingCallback callback,
               Path path );

    /**
     * Signal that all of the repository content in the specified workspace should be scanned.
     *
     * @param workspaceName the name of the workspace containing the content to be scanned; may not be null
     * @param callback the callback that should be invoked when the reindexing has completed; may be null if no callback is
     *        required
     */
    void scan( String workspaceName,
               IndexingCallback callback );

}
