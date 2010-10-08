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
package org.modeshape.search.lucene;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.search.SearchEngineException;

/**
 * A family of {@link LuceneConfiguration} implementations.
 */
public class LuceneConfigurations {

    /**
     * Return a new {@link LuceneConfiguration} that creates in-memory directories.
     * 
     * @return the new directory configuration; never null
     */
    public static final LuceneConfiguration inMemory() {
        return new RamDirectoryFactory();
    }

    /**
     * Return a new {@link LuceneConfiguration} that creates {@link FSDirectory} instances mapped to folders under a parent
     * folder, where the workspace name is used to create the workspace folder. Note that this has ramifications on the allowable
     * workspace names.
     * 
     * @param parent the parent folder
     * @return the new directory configuration; never null
     * @throws IllegalArgumentException if the parent file is null
     */
    public static final LuceneConfiguration using( File parent ) {
        CheckArg.isNotNull(parent, "parent");
        return new FileSystemDirectoryFromNameFactory(parent);
    }

    /**
     * Return a new {@link LuceneConfiguration} that creates {@link FSDirectory} instances mapped to folders under a parent
     * folder, where the workspace name is used to create the workspace folder. Note that this has ramifications on the allowable
     * workspace names.
     * 
     * @param parent the parent folder
     * @param lockFactory the lock factory; may be null
     * @return the new directory configuration; never null
     * @throws IllegalArgumentException if the parent file is null
     */
    public static final LuceneConfiguration using( File parent,
                                                   LockFactory lockFactory ) {
        CheckArg.isNotNull(parent, "parent");
        return new FileSystemDirectoryFromNameFactory(parent, lockFactory);
    }

    /**
     * Return a new {@link LuceneConfiguration} that creates {@link FSDirectory} instances mapped to folders under a parent
     * folder, where the workspace name is used to create the workspace folder. Note that this has ramifications on the allowable
     * workspace names.
     * 
     * @param parent the parent folder
     * @param workspaceNameEncoder the encoder that should be used for encoding the workspace name into a directory name
     * @param indexNameEncoder the encoder that should be used for encoding the index name into a directory name
     * @return the new directory configuration; never null
     * @throws IllegalArgumentException if the parent file is null
     */
    public static final LuceneConfiguration using( File parent,
                                                   TextEncoder workspaceNameEncoder,
                                                   TextEncoder indexNameEncoder ) {
        CheckArg.isNotNull(parent, "parent");
        return new FileSystemDirectoryFromNameFactory(parent, workspaceNameEncoder, indexNameEncoder);
    }

    /**
     * Return a new {@link LuceneConfiguration} that creates {@link FSDirectory} instances mapped to folders under a parent
     * folder, where the workspace name is used to create the workspace folder. Note that this has ramifications on the allowable
     * workspace names.
     * 
     * @param parent the parent folder
     * @param lockFactory the lock factory; may be null
     * @param workspaceNameEncoder the encoder that should be used for encoding the workspace name into a directory name
     * @param indexNameEncoder the encoder that should be used for encoding the index name into a directory name
     * @return the new directory configuration; never null
     * @throws IllegalArgumentException if the parent file is null
     */
    public static final LuceneConfiguration using( File parent,
                                                   LockFactory lockFactory,
                                                   TextEncoder workspaceNameEncoder,
                                                   TextEncoder indexNameEncoder ) {
        CheckArg.isNotNull(parent, "parent");
        return new FileSystemDirectoryFromNameFactory(parent, lockFactory, workspaceNameEncoder, indexNameEncoder);
    }

    /**
     * A {@link LuceneConfiguration} implementation that creates {@link Directory} instances of the supplied type for each
     * workspace and pools the results, ensuring that the same {@link Directory} instance is always returned for the same
     * workspace name.
     * 
     * @param <DirectoryType> the concrete type of the directory
     */
    @ThreadSafe
    protected static abstract class PoolingDirectoryFactory<DirectoryType extends Directory> implements LuceneConfiguration {
        private final ConcurrentHashMap<IndexId, DirectoryType> directories = new ConcurrentHashMap<IndexId, DirectoryType>();

        /**
         * {@inheritDoc}
         * 
         * @see LuceneConfiguration#getDirectory(java.lang.String, java.lang.String)
         */
        public Directory getDirectory( String workspaceName,
                                       String indexName ) throws SearchEngineException {
            CheckArg.isNotNull(workspaceName, "workspaceName");
            IndexId id = new IndexId(workspaceName, indexName);
            DirectoryType result = directories.get(id);
            if (result == null) {
                DirectoryType newDirectory = createDirectory(workspaceName, indexName);
                result = directories.putIfAbsent(id, newDirectory);
                if (result == null) result = newDirectory;
            }
            return result;
        }

        /**
         * {@inheritDoc}
         * 
         * @see LuceneConfiguration#destroyDirectory(java.lang.String, java.lang.String)
         */
        public boolean destroyDirectory( String workspaceName,
                                         String indexName ) throws SearchEngineException {
            CheckArg.isNotNull(workspaceName, "workspaceName");
            IndexId id = new IndexId(workspaceName, indexName);
            DirectoryType result = directories.remove(id);
            return result != null ? doDestroy(result) : false;
        }

        /**
         * Method implemented by subclasses to create a new Directory implementation.
         * 
         * @param workspaceName the name of the workspace for which the {@link Directory} is to be created; never null
         * @param indexName the name of the index to be created
         * @return the new directory; may not be null
         * @throws SearchEngineException if there is a problem creating the directory
         */
        protected abstract DirectoryType createDirectory( String workspaceName,
                                                          String indexName ) throws SearchEngineException;

        protected abstract boolean doDestroy( DirectoryType directory ) throws SearchEngineException;
    }

    /**
     * A {@link LuceneConfiguration} implementation that creates {@link RAMDirectory} instances for each workspace and index name.
     * Each factory instance maintains a pool of {@link RAMDirectory} instances, ensuring that the same {@link RAMDirectory} is
     * always returned for the same workspace name.
     */
    @ThreadSafe
    public static class RamDirectoryFactory extends PoolingDirectoryFactory<RAMDirectory> {
        protected RamDirectoryFactory() {
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.search.lucene.LuceneConfiguration#getVersion()
         */
        @Override
        public Version getVersion() {
            return Version.LUCENE_30;
        }

        @Override
        protected RAMDirectory createDirectory( String workspaceName,
                                                String indexName ) {
            return new RAMDirectory();
        }

        /**
         * {@inheritDoc}
         * 
         * @see LuceneConfigurations.PoolingDirectoryFactory#doDestroy(org.apache.lucene.store.Directory)
         */
        @Override
        protected boolean doDestroy( RAMDirectory directory ) throws SearchEngineException {
            return directory != null;
        }
    }

    /**
     * A {@link LuceneConfiguration} implementation that creates {@link FSDirectory} instances for each workspace and index name.
     * This factory is created with a parent directory under which all workspace and index directories are created.
     * <p>
     * This uses the supplied encoders to translate the workspace and index names into valid directory names. By default, no
     * encoding is performed, meaning that the workspace and index names are used explicitly as directory names. This default
     * behavior, then, means that not all values of workspace names or index names will work. If you want to be sure that all
     * workspace names work, supply an encoder for the workspace names. (Index names are currently such that they will always be
     * valid directory names, but you can always supply an encoder if you'd like.)
     * </p>
     */
    public static class FileSystemDirectoryFromNameFactory extends PoolingDirectoryFactory<FSDirectory> {
        private final File parentFile;
        private final LockFactory lockFactory;
        private final TextEncoder workspaceNameEncoder;
        private final TextEncoder indexNameEncoder;

        /**
         * Create a new {@link LuceneConfiguration} that creates {@link FSDirectory} instances mapped to folders under a parent
         * folder, where the workspace name is used to create the workspace folder. Note that this has ramifications on the
         * allowable workspace names.
         * 
         * @param parent the parent folder
         * @throws IllegalArgumentException if the parent file is null
         */
        protected FileSystemDirectoryFromNameFactory( File parent ) {
            this(parent, null, null, null);
        }

        /**
         * Create a new {@link LuceneConfiguration} that creates {@link FSDirectory} instances mapped to folders under a parent
         * folder, where the workspace name is used to create the workspace folder. Note that this has ramifications on the
         * allowable workspace names.
         * 
         * @param parent the parent folder
         * @param lockFactory the lock factory; may be null
         * @throws IllegalArgumentException if the parent file is null
         */
        protected FileSystemDirectoryFromNameFactory( File parent,
                                                      LockFactory lockFactory ) {
            this(parent, lockFactory, null, null);
        }

        /**
         * Create a new {@link LuceneConfiguration} that creates {@link FSDirectory} instances mapped to folders under a parent
         * folder, where the workspace name is used to create the workspace folder. Note that this has ramifications on the
         * allowable workspace names.
         * 
         * @param parent the parent folder
         * @param workspaceNameEncoder the encoder that should be used for encoding the workspace name into a directory name
         * @param indexNameEncoder the encoder that should be used for encoding the index name into a directory name
         * @throws IllegalArgumentException if the parent file is null
         */
        protected FileSystemDirectoryFromNameFactory( File parent,
                                                      TextEncoder workspaceNameEncoder,
                                                      TextEncoder indexNameEncoder ) {
            this(parent, null, workspaceNameEncoder, indexNameEncoder);
        }

        /**
         * Create a new {@link LuceneConfiguration} that creates {@link FSDirectory} instances mapped to folders under a parent
         * folder, where the workspace name is used to create the workspace folder. Note that this has ramifications on the
         * allowable workspace names.
         * 
         * @param parent the parent folder
         * @param lockFactory the lock factory; may be null
         * @param workspaceNameEncoder the encoder that should be used for encoding the workspace name into a directory name
         * @param indexNameEncoder the encoder that should be used for encoding the index name into a directory name
         * @throws IllegalArgumentException if the parent file is null
         */
        protected FileSystemDirectoryFromNameFactory( File parent,
                                                      LockFactory lockFactory,
                                                      TextEncoder workspaceNameEncoder,
                                                      TextEncoder indexNameEncoder ) {
            CheckArg.isNotNull(parent, "parent");
            this.parentFile = parent;
            this.lockFactory = lockFactory;
            this.workspaceNameEncoder = workspaceNameEncoder != null ? workspaceNameEncoder : new NoOpEncoder();
            this.indexNameEncoder = indexNameEncoder != null ? indexNameEncoder : new NoOpEncoder();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.search.lucene.LuceneConfiguration#getVersion()
         */
        @Override
        public Version getVersion() {
            return Version.LUCENE_30;
        }

        @Override
        protected FSDirectory createDirectory( String workspaceName,
                                               String indexName ) {
            File workspaceFile = new File(parentFile, workspaceNameEncoder.encode(workspaceName));
            if (!workspaceFile.exists()) {
                workspaceFile.mkdirs();
            } else {
                if (!workspaceFile.isDirectory()) {
                    I18n msg = LuceneI18n.locationForIndexesIsNotDirectory;
                    throw new SearchEngineException(msg.text(workspaceFile.getAbsolutePath(), workspaceName));
                }
                if (!workspaceFile.canRead()) {
                    I18n msg = LuceneI18n.locationForIndexesCannotBeRead;
                    throw new SearchEngineException(msg.text(workspaceFile.getAbsolutePath(), workspaceName));
                }
                if (!workspaceFile.canWrite()) {
                    I18n msg = LuceneI18n.locationForIndexesCannotBeWritten;
                    throw new SearchEngineException(msg.text(workspaceFile.getAbsolutePath(), workspaceName));
                }
            }
            File directory = workspaceFile;
            if (indexName != null) {
                File indexFile = new File(workspaceFile, indexNameEncoder.encode(indexName));
                if (!indexFile.exists()) {
                    Logger.getLogger(LuceneConfigurations.class).debug("Creating index folders for the '{0}' workspace at '{1}'",
                                                                       workspaceName,
                                                                       workspaceFile);
                    indexFile.mkdirs();
                } else {
                    if (!indexFile.isDirectory()) {
                        I18n msg = LuceneI18n.locationForIndexesIsNotDirectory;
                        throw new SearchEngineException(msg.text(indexFile.getAbsolutePath(), workspaceName));
                    }
                    if (!indexFile.canRead()) {
                        I18n msg = LuceneI18n.locationForIndexesCannotBeRead;
                        throw new SearchEngineException(msg.text(indexFile.getAbsolutePath(), workspaceName));
                    }
                    if (!indexFile.canWrite()) {
                        I18n msg = LuceneI18n.locationForIndexesCannotBeWritten;
                        throw new SearchEngineException(msg.text(indexFile.getAbsolutePath(), workspaceName));
                    }
                }
                directory = indexFile;
            }
            try {
                Logger.getLogger(LuceneConfigurations.class)
                      .debug("Initializing index files for the '{0}' workspace indexes under '{1}'", workspaceName, workspaceFile);
                return create(directory, lockFactory);
            } catch (IOException e) {
                throw new SearchEngineException(e);
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see LuceneConfigurations.PoolingDirectoryFactory#doDestroy(org.apache.lucene.store.Directory)
         */
        @Override
        protected boolean doDestroy( FSDirectory directory ) throws SearchEngineException {
            File file = directory.getFile();
            if (file.exists()) {
                return FileUtil.delete(file);
            }
            return false;
        }

        /**
         * Override this method to define which subclass of {@link FSDirectory} should be created.
         * 
         * @param directory the file system directory; never null
         * @param lockFactory the lock factory; may be null
         * @return the {@link FSDirectory} instance
         * @throws IOException if there is a problem creating the FSDirectory instance
         */
        protected FSDirectory create( File directory,
                                      LockFactory lockFactory ) throws IOException {
            return FSDirectory.open(directory, lockFactory);
        }
    }

    @Immutable
    protected static final class IndexId {
        private final String workspaceName;
        private final String indexName;
        private final int hc;

        protected IndexId( String workspaceName,
                           String indexName ) {
            assert workspaceName != null;
            this.workspaceName = workspaceName;
            this.indexName = indexName;
            this.hc = HashCode.compute(this.workspaceName, this.indexName);
        }

        /**
         * @return indexName
         */
        public String getIndexName() {
            return indexName;
        }

        /**
         * @return workspaceName
         */
        public String getWorkspaceName() {
            return workspaceName;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return hc;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof IndexId) {
                IndexId that = (IndexId)obj;
                if (this.hashCode() != that.hashCode()) return false;
                if (!this.workspaceName.equals(that.workspaceName)) return false;
                if (!this.indexName.equals(that.indexName)) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return indexName != null ? workspaceName + "/" + this.indexName : this.workspaceName;
        }
    }
}
