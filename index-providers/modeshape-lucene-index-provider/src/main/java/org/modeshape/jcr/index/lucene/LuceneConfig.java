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
package org.modeshape.jcr.index.lucene;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.NoLockFactory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.store.SimpleFSLockFactory;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Reflection;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.Environment;

/**
 * Holder of various Lucene configuration options.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since  4.5
 */
@Immutable
@ThreadSafe
public final class LuceneConfig {

    protected static final String LAST_SUCCESSFUL_COMMIT_TIME = "last_commit_time";

    private final LockFactory lockFactory;
    private final String directoryClass;
    private final Analyzer analyzer;
    private final Codec codec;
    private final String basePath;   
    private final AtomicLong lastSuccessfulCommitTime;
    
    protected static LuceneConfig inMemory() {
        return new LuceneConfig(null, null, null, null, null, null);
    }

    protected static LuceneConfig onDisk(String baseDir) {
        return new LuceneConfig(baseDir, null, null, null, null, null);
    }
    
    protected LuceneConfig(String baseDir, String lockFactoryClass, String directoryClass, String analyzerClass,
                           String codecName, Environment environment) {
        this.directoryClass = directoryClass;
        this.lockFactory = lockFactory(lockFactoryClass);
        this.analyzer = analyzer(analyzerClass, environment);
        this.codec = codec(codecName);
        this.basePath = baseDir;
        this.lastSuccessfulCommitTime = new AtomicLong(-1);
    }
    
    protected IndexWriter newWriter( String workspaceName, String indexName ) {
        CheckArg.isNotNull(indexName, "indexName");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        try {
            Directory directory = directory(directoryClass, workspaceName, indexName);
            IndexWriter indexWriter = new IndexWriter(directory, newIndexWriterConfig());
            if (DirectoryReader.indexExists(directory)) {
                readLatestCommitTime(indexWriter);
            } 
            return indexWriter;
        } catch (IOException e) {
            throw new LuceneIndexException("Cannot create index writer", e);
        }
    }
   
    private IndexWriterConfig newIndexWriterConfig() {
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        writerConfig.setCommitOnClose(true);
        writerConfig.setCodec(codec);
        return writerConfig;
    }
    
    protected SearcherManager searchManager( IndexWriter writer ) {
        try {
            return new SearcherManager(writer, true, true, null);
        } catch (IOException e) {
            throw new LuceneIndexException("Cannot create index writer", e);
        }
    }
    
    protected long lastSuccessfulCommitTime() {
        return lastSuccessfulCommitTime.get();
    }
    
    protected int refreshTimeSeconds() {
        return 30;
    }

    /**
     * Returns the analyzer configured for Lucene.
     * 
     * @return a {@link Analyzer} instance; never null
     */
    public Analyzer getAnalyzer() {
        return analyzer;
    }
    
    private void readLatestCommitTime( IndexWriter writer ) {
        Map<String, String> commitData = writer.getCommitData();
        String timestampString = commitData.get(LAST_SUCCESSFUL_COMMIT_TIME);
        if (timestampString == null) {
            return;
        }
        long timestamp = Long.valueOf(timestampString);
        long currentTs = lastSuccessfulCommitTime.get();
        if (timestamp > currentTs) {
            lastSuccessfulCommitTime.compareAndSet(currentTs, timestamp);
        }
    }

    private Codec codec(String name) {
        return StringUtil.isBlank(name) ? Codec.getDefault() : Codec.forName(name);
    }

    private LockFactory lockFactory(String lockFactoryClass) {
        if (StringUtil.isBlank(lockFactoryClass)) {
            return null; 
        }
        switch (lockFactoryClass) {
            case "org.apache.lucene.store.NativeFSLockFactory" : {
                return NativeFSLockFactory.INSTANCE;
            }
            case "org.apache.lucene.store.NoLockFactory" : {
                return NoLockFactory.INSTANCE;                
            }
            case "org.apache.lucene.store.SimpleFSLockFactory" : {
                return SimpleFSLockFactory.INSTANCE;
            }
            default:
                throw new IllegalArgumentException("Unknown lock factory implementation: " + lockFactoryClass);
        }
    }

    private Analyzer analyzer(String analyzerClass, Environment environment) {
        if (StringUtil.isBlank(analyzerClass)) {
            // we don't want any stop words by default
            return new StandardAnalyzer(CharArraySet.EMPTY_SET);    
        } else {
            return Reflection.getInstance(analyzerClass, environment.getClassLoader(this));
        }
    }

    private Directory directory( String directoryClass, String workspaceName, String indexName ) throws IOException {
        boolean useLockFactory = lockFactory != null;
        if (StringUtil.isBlank(basePath)) {
            return useLockFactory ?  new RAMDirectory(lockFactory) : new RAMDirectory();
        }
        Path path = Paths.get(basePath, workspaceName, indexName);
        if (StringUtil.isBlank(directoryClass)) {             
            return useLockFactory ? FSDirectory.open(path, lockFactory) : FSDirectory.open(path); 
        }
        switch (directoryClass) {
            case "org.apache.lucene.store.RAMDirectory" : {
                return useLockFactory ? new RAMDirectory(lockFactory) : new RAMDirectory();
            }
            case "org.apache.lucene.store.MMapDirectory" : {
                return useLockFactory ? new MMapDirectory(path, lockFactory) : new MMapDirectory(path);
            }
            case "org.apache.lucene.store.NIOFSDirectory" : {
                return useLockFactory ? new NIOFSDirectory(path, lockFactory) : new NIOFSDirectory(path);
            }
            case "org.apache.lucene.store.SimpleFSDirectory" : {
                return useLockFactory ? new SimpleFSDirectory(path, lockFactory) : new SimpleFSDirectory(path);
            }
            default: {
                throw new IllegalArgumentException("Unknown Lucene directory: " + directoryClass);
            }
        }
    }
}
