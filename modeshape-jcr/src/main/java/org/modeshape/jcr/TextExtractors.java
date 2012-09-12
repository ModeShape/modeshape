/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

/**
 * Facility for managing {@link TextExtractor} instances and submitting text extraction work
 */
@Immutable
public final class TextExtractors {

    private static final Logger LOGGER = Logger.getLogger(TextExtractors.class);

    private final List<TextExtractor> extractors;
    private final ExecutorService extractingQueue;
    private final ConcurrentHashMap<BinaryKey, CountDownLatch> workerLatches;
    private final boolean fullTextSearchEnabled;

    public TextExtractors( ExecutorService extractingQueue,
                           boolean fullTextSearchEnabled,
                           List<TextExtractor> extractors ) {
        this.extractingQueue = extractingQueue;
        this.workerLatches = new ConcurrentHashMap<BinaryKey, CountDownLatch>();
        this.fullTextSearchEnabled = fullTextSearchEnabled;
        this.extractors = extractors;
    }

    TextExtractors( JcrRepository.RunningState repository,
                    RepositoryConfiguration.TextExtracting extracting ) {
        this(repository.context().getCachedTreadPool(extracting.getThreadPoolName()), repository.isFullTextSearchEnabled(),
             getConfiguredExtractors(repository, extracting));
    }

    protected void shutdown() {
        extractors.clear();
        extractingQueue.shutdown();
    }

    public boolean extractionEnabled() {
        return fullTextSearchEnabled && !extractors.isEmpty();
    }

    public String extract( InMemoryBinaryValue inMemoryBinaryValue,
                           TextExtractor.Context context ) {
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

            String extractedText = output.getText();
            return extractedText;
        } catch (Exception e) {
            LOGGER.error(JcrI18n.errorExtractingTextFromBinary, inMemoryBinaryValue.getHexHash(), e.getLocalizedMessage());
        }
        return null;
    }

    public void extract( AbstractBinaryStore store,
                         BinaryValue binaryValue,
                         TextExtractor.Context context ) {
        if (!extractionEnabled()) {
            return;
        }
        if (binaryValue instanceof InMemoryBinaryValue) {
            // We never extract the text for binary values this way ...
            return;
        }
        CheckArg.isNotNull(binaryValue, "binaryValue");
        CountDownLatch latch = getWorkerLatch(binaryValue.getKey(), true);
        extractingQueue.execute(new Worker(store, binaryValue, context, latch));
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
                                                                RepositoryConfiguration.TextExtracting extracting ) {
        List<Component> extractorComponents = extracting.getTextExtractors();
        List<TextExtractor> extractors = new ArrayList<TextExtractor>(extractorComponents.size());
        for (Component component : extractorComponents) {
            try {
                TextExtractor extractor = component.createInstance(TextExtractors.class.getClassLoader());
                extractor.setLogger(ExtensionLogger.getLogger(extractor.getClass()));
                extractors.add(extractor);
            } catch (Throwable t) {
                String desc = component.getName();
                String repoName = repository.name();
                LOGGER.error(t, JcrI18n.unableToInitializeTextExtractor, desc, repoName, t.getMessage());
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
            } catch (Exception e) {
                LOGGER.error(JcrI18n.errorExtractingTextFromBinary, binaryValue.getHexHash(), e.getLocalizedMessage());
            } finally {
                // decrement the latch regardless of success/failure to avoid blocking, as extraction is not retried
                latch.countDown();
            }
        }
    }
}
