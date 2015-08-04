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
package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.api.text.TextExtractor;
import org.modeshape.jcr.text.TextExtractorOutput;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.AbstractBinaryStore;
import org.modeshape.jcr.value.binary.InMemoryBinaryValue;

/**
 * Facility for managing {@link TextExtractor} instances and submitting text extraction work
 */
@Immutable
public final class TextExtractors {

    private static final Logger LOGGER = Logger.getLogger(TextExtractors.class);

    private final List<TextExtractor> extractors;
    private final ExecutorService extractingQueue;
    private final List<Future<?>> extractionResults;
    private final ConcurrentHashMap<BinaryKey, CountDownLatch> workerLatches;
    private volatile boolean active;

    public TextExtractors( ExecutorService extractingQueue,
                           List<TextExtractor> extractors ) {
        this.extractingQueue = extractingQueue;
        this.workerLatches = new ConcurrentHashMap<>();
        this.extractionResults = new ArrayList<>();
        this.extractors = extractors;
        this.active = true;
    }

    protected TextExtractors( JcrRepository.RunningState repository,
                    RepositoryConfiguration.TextExtraction extracting ) {
        this(repository.context().getCachedTreadPool(extracting.getThreadPoolName(), extracting.getMaxPoolSize()), 
             getConfiguredExtractors(repository, extracting));
    }

    protected void shutdown() {
        this.active = false;
        this.extractors.clear();
        this.extractingQueue.shutdown();
        for (Future<?>  extractionResult : extractionResults ) {
            extractionResult.cancel(true);
        }
        extractionResults.clear();
    }

    public boolean extractionEnabled() {
        return active && !extractors.isEmpty();
    }

    public String extract( InMemoryBinaryValue inMemoryBinaryValue,
                           TextExtractor.Context context ) {
        if (!extractionEnabled()) {
            return null;
        }
        try {
            String mimeType = inMemoryBinaryValue.getMimeType();
            TextExtractorOutput output = new TextExtractorOutput();
            // Run through the extractors and have them extract the text - the first one which accepts the mime-type will win
            for (TextExtractor extractor : extractors) {
                if (!extractor.supportsMimeType(mimeType)) {
                    continue;
                }
                extractor.extractFrom(inMemoryBinaryValue, output, context);
                break;
            }

            return output.getText();
        } catch (Exception e) {
            LOGGER.error(e, JcrI18n.errorExtractingTextFromBinary, inMemoryBinaryValue.getHexHash(), e.getLocalizedMessage());
        }
        return null;
    }

    public CountDownLatch extract( AbstractBinaryStore store,
                                   BinaryValue binaryValue,
                                   TextExtractor.Context context ) {
        if (!extractionEnabled()) {
            return null;
        }
        if (binaryValue instanceof InMemoryBinaryValue) {
            // We never extract the text for binary values this way ...
            return null;
        }
        CheckArg.isNotNull(binaryValue, "binaryValue");
        CountDownLatch latch = getWorkerLatch(binaryValue.getKey(), true);
        extractionResults.add(extractingQueue.submit(new Worker(store, binaryValue, context, latch)));
        return latch;
    }

    public CountDownLatch getWorkerLatch( BinaryKey binaryKey,
                                          boolean createIfMissing ) {
        if (createIfMissing) {
            CountDownLatch latch = new CountDownLatch(1);
            CountDownLatch existingLatch = workerLatches.putIfAbsent(binaryKey, latch);
            return existingLatch != null ? existingLatch : latch;
        }
        return workerLatches.get(binaryKey);
    }

    private static List<TextExtractor> getConfiguredExtractors( JcrRepository.RunningState repository,
                                                                RepositoryConfiguration.TextExtraction extracting ) {
        List<Component> extractorComponents = extracting.getTextExtractors(repository.problems());
        List<TextExtractor> extractors = new ArrayList<TextExtractor>(extractorComponents.size());
        for (Component component : extractorComponents) {
            try {
                TextExtractor extractor = component.createInstance(TextExtractors.class.getClassLoader());
                extractor.setLogger(ExtensionLogger.getLogger(extractor.getClass()));
                extractors.add(extractor);
            } catch (Throwable t) {
                String desc = component.getName();
                String repoName = repository.name();
                repository.error(t, JcrI18n.unableToInitializeTextExtractor, desc, repoName, t.getMessage());
            }
        }
        return extractors;
    }

    /**
     * A unit of work which extracts text from a binary value, stores that text in a store and notifies a latch that the
     * extraction operation has finished.
     */
    protected final class Worker implements Runnable {
        private final BinaryValue binaryValue;
        private final TextExtractor.Context context;
        private final AbstractBinaryStore store;
        private final CountDownLatch latch;

        protected Worker( AbstractBinaryStore store,
                          BinaryValue binaryValue,
                          TextExtractor.Context context,
                          CountDownLatch latch ) {
            this.store = store;
            this.binaryValue = binaryValue;
            this.context = context;
            this.latch = latch;
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void run() {
            if (!active) {
                return;
            }
            try {
                // only extract text if there isn't a stored value for the binary key (note that any changes in the binary will
                // produce a different key)
                if (store.getExtractedText(binaryValue) != null) {
                    return;
                }

                String mimeType = binaryValue.getMimeType();
                TextExtractorOutput output = new TextExtractorOutput();
                // Run through the extractors and have them extract the text - the first one which accepts the mime-type will win
                for (TextExtractor extractor : extractors) {
                    if (!extractor.supportsMimeType(mimeType)) {
                        continue;
                    }
                    extractor.extractFrom(binaryValue, output, context);
                    break;
                }

                String extractedText = output.getText();
                if (extractedText != null && !StringUtil.isBlank(extractedText)) {
                    store.storeExtractedText(binaryValue, extractedText);
                }
            }  catch (InterruptedException ie) {
                Thread.interrupted();
                LOGGER.warn(RepositoryI18n.shutdownWhileExtractingText, binaryValue.getKey(), ie.getMessage());
            } catch (Throwable t) {
                if (!active) {
                    LOGGER.warn(RepositoryI18n.shutdownWhileExtractingText, binaryValue.getKey(), t.getMessage());
                } else {
                    LOGGER.error(t, JcrI18n.errorExtractingTextFromBinary, binaryValue.getHexHash(), t.getLocalizedMessage());
                }
            } finally {
                // decrement the latch regardless of success/failure to avoid blocking, as extraction is not retried
                latch.countDown();
            }
        }
    }
}
