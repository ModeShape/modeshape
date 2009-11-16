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

import java.util.List;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.process.AbstractAccessComponent;
import org.jboss.dna.graph.query.process.ProcessingComponent;
import org.jboss.dna.graph.query.process.SelectComponent.Analyzer;
import org.jboss.dna.graph.request.ChangeRequest;

/**
 * An {@link IndexStrategy} implementation that stores all content within a set of two indexes: one for the node content and a
 * second one for paths and UUIDs.
 */
@ThreadSafe
class KitchenSinkIndexStrategy extends DualIndexStrategy {

    /**
     * The default set of {@link IndexRules} used by {@link KitchenSinkIndexStrategy} instances when no rules are provided.
     */
    public static final IndexRules DEFAULT_RULES;

    static {
        IndexRules.Builder builder = IndexRules.createBuilder();
        // Configure the default behavior ...
        builder.defaultTo(IndexRules.INDEX | IndexRules.ANALYZE);
        // Configure the UUID properties to be just indexed (not stored, not analyzed, not included in full-text) ...
        builder.index(JcrLexicon.UUID, DnaLexicon.UUID);
        // Configure the properties that we'll treat as dates ...
        builder.treatAsDates(JcrLexicon.CREATED, JcrLexicon.LAST_MODIFIED);
        DEFAULT_RULES = builder.build();
    }

    /**
     * Create a new indexing strategy instance uses the {@link #DEFAULT_RULES default indexing rules}.
     */
    public KitchenSinkIndexStrategy() {
        this(null);
    }

    /**
     * Create a new indexing strategy instance.
     * 
     * @param rules the indexing rules that govern how properties are to be index, or null if the {@link #DEFAULT_RULES default
     *        rules} are to be used
     */
    public KitchenSinkIndexStrategy( IndexRules rules ) {
        super(rules != null ? rules : DEFAULT_RULES);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.DualIndexStrategy#createAccessComponent(org.jboss.dna.search.SearchContext,
     *      org.jboss.dna.graph.query.plan.PlanNode, org.jboss.dna.graph.query.QueryResults.Columns,
     *      org.jboss.dna.graph.query.process.SelectComponent.Analyzer)
     */
    @Override
    protected ProcessingComponent createAccessComponent( final SearchContext context,
                                                         PlanNode accessNode,
                                                         Columns resultColumns,
                                                         Analyzer analyzer ) {
        // Create a processing component for this access query ...
        return new AbstractAccessComponent(context, resultColumns, accessNode) {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.query.process.ProcessingComponent#execute()
             */
            @Override
            public List<Object[]> execute() {
                return null;
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.search.IndexStrategy#apply(Iterable, IndexContext)
     */
    public int apply( Iterable<ChangeRequest> changes,
                      IndexContext indexes ) /*throws IOException*/{
        for (ChangeRequest change : changes) {
            if (change != null) continue;
        }
        return 0;
    }
}
