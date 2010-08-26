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

import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.jcip.annotations.Immutable;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.search.SearchEngineWorkspace;

/**
 * The {@link SearchEngineWorkspace} implementation for the {@link LuceneSearchEngine}.
 */
@Immutable
public class LuceneSearchWorkspace implements SearchEngineWorkspace {

    /**
     * Apparently Lucene indexes must always be optimized prior to committing, so this value is set to '1'.
     */
    protected static final int CHANGES_BEFORE_OPTIMIZATION = 1;

    protected static final String INDEX_NAME = "content";

    /**
     * Given the name of a property field of the form "&lt;namespace>:&lt;local>" (where &lt;namespace> can be zero-length), this
     * provider also stores the value(s) for free-text searching in a field named ":ft:&lt;namespace>:&lt;local>". Thus, even if
     * the namespace is zero-length, the free-text search field will be named ":ft::&lt;local>" and will not clash with any other
     * property name.
     */
    protected static final String FULL_TEXT_PREFIX = ":ft:";

    /**
     * This index stores these fields <i>plus</i> all properties. Therefore, we have to worry about name clashes, which is why
     * these field names are prefixed with '::', which is something that does appear in property names as they are serialized.
     */
    static class ContentIndex {
        public static final String PATH = "::pth";
        public static final String NODE_NAME = "::nam";
        public static final String LOCAL_NAME = "::loc";
        public static final String SNS_INDEX = "::sns";
        public static final String LOCATION_ID_PROPERTIES = "::idp";
        public static final String DEPTH = "::dep";
        public static final String FULL_TEXT = "::fts";
        public static final String REFERENCES = "::ref";
        public static final String STRONG_REFERENCES = "::refInt";
    }

    private final String workspaceName;
    private final String workspaceDirectoryName;
    protected final IndexRules rules;
    private final LuceneConfiguration configuration;
    protected final Directory contentDirectory;
    protected final Analyzer analyzer;
    private final Lock changesLock = new ReentrantLock();
    private int changes = 0;

    protected LuceneSearchWorkspace( String workspaceName,
                                     LuceneConfiguration configuration,
                                     IndexRules rules,
                                     Analyzer analyzer ) {
        assert workspaceName != null;
        assert configuration != null;
        this.workspaceName = workspaceName;
        this.workspaceDirectoryName = workspaceName.trim().length() != 0 ? workspaceName : UUID.randomUUID().toString();
        this.analyzer = analyzer != null ? analyzer : new StandardAnalyzer(configuration.getVersion());
        this.rules = rules != null ? rules : LuceneSearchEngine.DEFAULT_RULES;
        this.configuration = configuration;
        this.contentDirectory = this.configuration.getDirectory(workspaceDirectoryName, INDEX_NAME);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.search.SearchEngineWorkspace#getWorkspaceName()
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.search.SearchEngineWorkspace#destroy(org.modeshape.graph.ExecutionContext)
     */
    public void destroy( ExecutionContext context ) {
        configuration.destroyDirectory(workspaceDirectoryName, INDEX_NAME);
    }

    /**
     * @return rules
     */
    public IndexRules getRules() {
        return rules;
    }

    /**
     * Give the number of changes that have been made in a session, determine whether optimization is required on the workspace
     * indexes.
     * 
     * @param changesInSession the number of changes made within a session using this workspace
     * @return true if the workspace indexes should be optimized, or false otherwise
     */
    protected boolean isOptimizationRequired( int changesInSession ) {
        if (changesInSession == 0) return false;
        assert changesInSession > 0;
        try {
            changesLock.lock();
            changes += changesInSession;
            if (changes >= CHANGES_BEFORE_OPTIMIZATION) {
                changes = 0;
                return true;
            }
            return false;
        } finally {
            changesLock.unlock();
        }
    }

    /**
     * Get the version information for Lucene.
     * 
     * @return the version information; never null
     * @see org.modeshape.search.lucene.LuceneConfiguration#getVersion()
     */
    public Version getVersion() {
        return configuration.getVersion();
    }

}
