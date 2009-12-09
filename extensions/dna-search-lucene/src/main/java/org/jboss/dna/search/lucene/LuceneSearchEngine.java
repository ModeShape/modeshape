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
package org.jboss.dna.search.lucene;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.util.Version;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.text.UrlEncoder;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.basic.JodaDateTime;
import org.jboss.dna.graph.search.SearchEngine;
import org.jboss.dna.graph.search.SearchEngineException;

/**
 * A {@link SearchEngine} implementation that relies upon two separate indexes to manage the node properties and the node
 * structure (path and children). Using two indexes is more efficient when the node content and structure are updated
 * independently. For example, the structure of the nodes changes whenever same-name-sibling indexes are changed, when sibling
 * nodes are deleted, or when nodes are moved around; in all of these cases, the properties of the nodes do not change.
 */
public class LuceneSearchEngine extends AbstractLuceneSearchEngine<LuceneSearchWorkspace, LuceneSearchProcessor> {

    /**
     * The default set of {@link IndexRules} used by {@link LuceneSearchEngine} instances when no rules are provided. These rules
     * default to index and analyze all properties, and to index the {@link DnaLexicon#UUID dna:uuid} and {@link JcrLexicon#UUID
     * jcr:uuid} properties to be indexed and stored only (not analyzed and not included in full-text search. The rules also treat
     * {@link JcrLexicon#CREATED jcr:created} and {@link JcrLexicon#LAST_MODIFIED jcr:lastModified} properties as dates.
     */
    public static final IndexRules DEFAULT_RULES;

    static {
        // We know that the earliest creation/modified dates cannot be before November 1 2009,
        // which is before this feature was implemented
        long earliestChangeDate = new JodaDateTime(2009, 11, 01, 0, 0, 0, 0).getMilliseconds();

        IndexRules.Builder builder = IndexRules.createBuilder();
        // Configure the default behavior ...
        builder.defaultTo(Field.Store.YES, Field.Index.ANALYZED);
        // Configure the UUID properties to be just indexed and stored (not analyzed, not included in full-text) ...
        builder.stringField(JcrLexicon.UUID, Field.Store.YES, Field.Index.NOT_ANALYZED);
        builder.stringField(DnaLexicon.UUID, Field.Store.YES, Field.Index.NOT_ANALYZED);
        // Configure the properties that we'll treat as dates ...
        builder.dateField(JcrLexicon.CREATED, Field.Store.YES, Field.Index.NOT_ANALYZED, earliestChangeDate);
        builder.dateField(JcrLexicon.LAST_MODIFIED, Field.Store.YES, Field.Index.NOT_ANALYZED, earliestChangeDate);
        DEFAULT_RULES = builder.build();
    }

    protected static final TextEncoder DEFAULT_ENCODER = new UrlEncoder();

    /** A thread-local DateFormat instance that is thread-safe, since a new instance is created for each thread. */
    protected ThreadLocal<DateFormat> dateFormatter = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
        }
    };

    private final LuceneConfiguration configuration;
    private final IndexRules rules;
    private final Analyzer analyzer;

    /**
     * Create a new instance of a {@link SearchEngine} that uses Lucene and a two-index design, and that stores the indexes using
     * the supplied {@link LuceneConfiguration}.
     * 
     * @param sourceName the name of the source that this engine will search over
     * @param connectionFactory the factory for making connections to the source
     * @param verifyWorkspaceInSource true if the workspaces are to be verified using the source, or false if this engine is used
     *        in a way such that all workspaces are known to exist
     * @param configuration the configuration of the Lucene indexes
     * @param rules the index rule, or null if the default index rules should be used
     * @param analyzer the analyzer, or null if the default analyzer should be used
     * @throws IllegalArgumentException if any of the source name, connection factory, or configuration are null
     */
    public LuceneSearchEngine( String sourceName,
                               RepositoryConnectionFactory connectionFactory,
                               boolean verifyWorkspaceInSource,
                               LuceneConfiguration configuration,
                               IndexRules rules,
                               Analyzer analyzer ) {
        super(sourceName, connectionFactory, verifyWorkspaceInSource);
        CheckArg.isNotNull(configuration, "configuration");
        this.configuration = configuration;
        this.analyzer = analyzer != null ? analyzer : new StandardAnalyzer(Version.LUCENE_30);
        this.rules = rules != null ? rules : DEFAULT_RULES;
    }

    /**
     * Create a new instance of a {@link SearchEngine} that uses Lucene and a two-index design, and that stores the indexes in the
     * supplied directory.
     * <p>
     * This is identical to the following:
     * 
     * <pre>
     * TextEncoder encoder = new UrlEncoder();
     * LuceneConfiguration config = LuceneConfigurations.using(indexStorageDirectory, null, encoder, encoder);
     * new LuceneSearchEngine(sourceName, connectionFactory, verifyWorkspaceInSource, config, rules, analyzer);
     * </pre>
     * 
     * where the {@link UrlEncoder} is used to ensure that workspace names and index names can be turned into file system
     * directory names.
     * </p>
     * 
     * @param sourceName the name of the source that this engine will search over
     * @param connectionFactory the factory for making connections to the source
     * @param verifyWorkspaceInSource true if the workspaces are to be verified using the source, or false if this engine is used
     *        in a way such that all workspaces are known to exist
     * @param indexStorageDirectory the file system directory in which the indexes are to be kept
     * @param rules the index rule, or null if the default index rules should be used
     * @param analyzer the analyzer, or null if the default analyzer should be used
     * @throws IllegalArgumentException if any of the source name, connection factory, or directory are null
     */
    public LuceneSearchEngine( String sourceName,
                               RepositoryConnectionFactory connectionFactory,
                               boolean verifyWorkspaceInSource,
                               File indexStorageDirectory,
                               IndexRules rules,
                               Analyzer analyzer ) {
        this(sourceName, connectionFactory, verifyWorkspaceInSource, LuceneConfigurations.using(indexStorageDirectory,
                                                                                                null,
                                                                                                DEFAULT_ENCODER,
                                                                                                DEFAULT_ENCODER), null, null);
    }

    /**
     * Create a new instance of a {@link SearchEngine} that uses Lucene and a two-index design, and that stores the Lucene indexes
     * in memory.
     * <p>
     * This is identical to the following:
     * 
     * <pre>
     * new LuceneSearchEngine(sourceName, connectionFactory, verifyWorkspaceInSource, LuceneConfigurations.inMemory(), rules, analyzer);
     * </pre>
     * 
     * </p>
     * 
     * @param sourceName the name of the source that this engine will search over
     * @param connectionFactory the factory for making connections to the source
     * @param verifyWorkspaceInSource true if the workspaces are to be verified using the source, or false if this engine is used
     *        in a way such that all workspaces are known to exist
     * @param rules the index rule, or null if the default index rules should be used
     * @param analyzer the analyzer, or null if the default analyzer should be used
     * @throws IllegalArgumentException if any of the source name or connection factory are null
     */
    public LuceneSearchEngine( String sourceName,
                               RepositoryConnectionFactory connectionFactory,
                               boolean verifyWorkspaceInSource,
                               IndexRules rules,
                               Analyzer analyzer ) {
        this(sourceName, connectionFactory, verifyWorkspaceInSource, LuceneConfigurations.inMemory(), null, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.search.SearchEngine#createProcessor(org.jboss.dna.graph.ExecutionContext,
     *      org.jboss.dna.graph.search.SearchEngine.Workspaces, org.jboss.dna.graph.observe.Observer,boolean)
     */
    @Override
    protected LuceneSearchProcessor createProcessor( ExecutionContext context,
                                                     Workspaces<LuceneSearchWorkspace> workspaces,
                                                     Observer observer,
                                                     boolean readOnly ) {
        return new LuceneSearchProcessor(getSourceName(), context, workspaces, observer, null, readOnly);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.search.SearchEngine#createWorkspace(org.jboss.dna.graph.ExecutionContext, java.lang.String)
     */
    @Override
    protected LuceneSearchWorkspace createWorkspace( ExecutionContext context,
                                                     String workspaceName ) throws SearchEngineException {
        return new LuceneSearchWorkspace(workspaceName, configuration, rules, analyzer, false);
    }
}
