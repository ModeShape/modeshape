/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.search;

import java.io.IOException;
import net.jcip.annotations.NotThreadSafe;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.ValueFactory;

/**
 * A set of index readers and writers.
 */
@NotThreadSafe
final class IndexContext {

    private final ExecutionContext context;
    private final Directory pathsIndexDirectory;
    private final Directory contentIndexDirectory;
    private final Analyzer analyzer;
    private final boolean overwrite;
    private final boolean readOnly;
    private final ValueFactory<String> stringFactory;
    private final DateTimeFactory dateFactory;
    private IndexReader pathsReader;
    private IndexWriter pathsWriter;
    private IndexSearcher pathsSearcher;
    private IndexReader contentReader;
    private IndexWriter contentWriter;
    private IndexSearcher contentSearcher;

    IndexContext( ExecutionContext context,
                  Directory pathsIndexDirectory,
                  Directory contentIndexDirectory,
                  Analyzer analyzer,
                  boolean overwrite,
                  boolean readOnly ) {
        assert context != null;
        assert pathsIndexDirectory != null;
        assert contentIndexDirectory != null;
        this.context = context;
        this.pathsIndexDirectory = pathsIndexDirectory;
        this.contentIndexDirectory = contentIndexDirectory;
        this.analyzer = analyzer;
        this.overwrite = overwrite;
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.dateFactory = context.getValueFactories().getDateFactory();
        this.readOnly = readOnly;
    }

    /**
     * @return context
     */
    public ExecutionContext context() {
        return context;
    }

    /**
     * @return stringFactory
     */
    public ValueFactory<String> stringFactory() {
        return stringFactory;
    }

    public DateTimeFactory dateFactory() {
        return dateFactory;
    }

    public PathFactory pathFactory() {
        return context.getValueFactories().getPathFactory();
    }

    public IndexReader getPathsReader() throws IOException {
        if (pathsReader == null) {
            pathsReader = IndexReader.open(pathsIndexDirectory, readOnly);
        }
        return pathsReader;
    }

    public IndexReader getContentReader() throws IOException {
        if (contentReader == null) {
            contentReader = IndexReader.open(contentIndexDirectory, readOnly);
        }
        return contentReader;
    }

    public IndexWriter getPathsWriter() throws IOException {
        if (pathsWriter == null) {
            pathsWriter = new IndexWriter(pathsIndexDirectory, analyzer, overwrite, MaxFieldLength.UNLIMITED);
        }
        return pathsWriter;
    }

    public IndexWriter getContentWriter() throws IOException {
        if (contentWriter == null) {
            contentWriter = new IndexWriter(contentIndexDirectory, analyzer, overwrite, MaxFieldLength.UNLIMITED);
        }
        return contentWriter;
    }

    public IndexSearcher getPathsSearcher() throws IOException {
        if (pathsSearcher == null) {
            pathsSearcher = new IndexSearcher(getPathsReader());
        }
        return pathsSearcher;
    }

    public IndexSearcher getContentSearcher() throws IOException {
        if (contentSearcher == null) {
            contentSearcher = new IndexSearcher(getContentReader());
        }
        return contentSearcher;
    }

    public boolean hasWriters() {
        return pathsWriter != null || contentWriter != null;
    }

    public void commit() throws IOException {
        IOException ioError = null;
        RuntimeException runtimeError = null;
        if (pathsReader != null) {
            try {
                pathsReader.close();
            } catch (IOException e) {
                ioError = e;
            } catch (RuntimeException e) {
                runtimeError = e;
            } finally {
                pathsReader = null;
            }
        }
        if (contentReader != null) {
            try {
                contentReader.close();
            } catch (IOException e) {
                if (ioError == null) ioError = e;
            } catch (RuntimeException e) {
                if (runtimeError == null) runtimeError = e;
            } finally {
                contentReader = null;
            }
        }
        if (pathsWriter != null) {
            try {
                pathsWriter.commit();
            } catch (IOException e) {
                ioError = e;
            } catch (RuntimeException e) {
                runtimeError = e;
            } finally {
                try {
                    pathsWriter.close();
                } catch (IOException e) {
                    ioError = e;
                } catch (RuntimeException e) {
                    runtimeError = e;
                } finally {
                    pathsWriter = null;
                }
            }
        }
        if (contentWriter != null) {
            try {
                contentWriter.commit();
            } catch (IOException e) {
                if (ioError == null) ioError = e;
            } catch (RuntimeException e) {
                if (runtimeError == null) runtimeError = e;
            } finally {
                try {
                    contentWriter.close();
                } catch (IOException e) {
                    ioError = e;
                } catch (RuntimeException e) {
                    runtimeError = e;
                } finally {
                    contentWriter = null;
                }
            }
        }
        if (ioError != null) throw ioError;
        if (runtimeError != null) throw runtimeError;
    }

    public void rollback() throws IOException {
        IOException ioError = null;
        RuntimeException runtimeError = null;
        if (pathsReader != null) {
            try {
                pathsReader.close();
            } catch (IOException e) {
                ioError = e;
            } catch (RuntimeException e) {
                runtimeError = e;
            } finally {
                pathsReader = null;
            }
        }
        if (contentReader != null) {
            try {
                contentReader.close();
            } catch (IOException e) {
                if (ioError == null) ioError = e;
            } catch (RuntimeException e) {
                if (runtimeError == null) runtimeError = e;
            } finally {
                contentReader = null;
            }
        }
        if (pathsWriter != null) {
            try {
                pathsWriter.rollback();
            } catch (IOException e) {
                ioError = e;
            } catch (RuntimeException e) {
                runtimeError = e;
            } finally {
                try {
                    pathsWriter.close();
                } catch (IOException e) {
                    ioError = e;
                } catch (RuntimeException e) {
                    runtimeError = e;
                } finally {
                    pathsWriter = null;
                }
            }
        }
        if (contentWriter != null) {
            try {
                contentWriter.rollback();
            } catch (IOException e) {
                if (ioError == null) ioError = e;
            } catch (RuntimeException e) {
                if (runtimeError == null) runtimeError = e;
            } finally {
                try {
                    contentWriter.close();
                } catch (IOException e) {
                    ioError = e;
                } catch (RuntimeException e) {
                    runtimeError = e;
                } finally {
                    contentWriter = null;
                }
            }
        }
        if (ioError != null) throw ioError;
        if (runtimeError != null) throw runtimeError;
    }

}
