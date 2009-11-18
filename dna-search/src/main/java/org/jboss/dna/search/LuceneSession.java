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
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.DateTimeFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryEngine;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.Length;
import org.jboss.dna.graph.query.model.NodeDepth;
import org.jboss.dna.graph.query.model.NodeLocalName;
import org.jboss.dna.graph.query.model.NodeName;
import org.jboss.dna.graph.query.model.NodePath;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.Visitors;
import org.jboss.dna.graph.query.optimize.Optimizer;
import org.jboss.dna.graph.query.optimize.OptimizerRule;
import org.jboss.dna.graph.query.optimize.RuleBasedOptimizer;
import org.jboss.dna.graph.query.plan.CanonicalPlanner;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.Planner;
import org.jboss.dna.graph.query.process.ProcessingComponent;
import org.jboss.dna.graph.query.process.QueryProcessor;
import org.jboss.dna.graph.search.SearchProvider;
import org.jboss.dna.search.DualIndexSearchProvider.PathIndex;

/**
 * 
 */
@NotThreadSafe
public abstract class LuceneSession implements SearchProvider.Session {
    protected final ExecutionContext context;
    protected final String sourceName;
    protected final String workspaceName;
    protected final IndexRules rules;
    protected final Analyzer analyzer;
    protected final boolean overwrite;
    protected final boolean readOnly;
    protected final ValueFactory<String> stringFactory;
    protected final DateTimeFactory dateFactory;
    protected final PathFactory pathFactory;
    private int changeCount;
    private QueryEngine queryEngine;

    protected LuceneSession( ExecutionContext context,
                             String sourceName,
                             String workspaceName,
                             IndexRules rules,
                             Analyzer analyzer,
                             boolean overwrite,
                             boolean readOnly ) {
        this.context = context;
        this.sourceName = sourceName;
        this.workspaceName = workspaceName;
        this.rules = rules;
        this.overwrite = overwrite;
        this.readOnly = readOnly;
        this.analyzer = analyzer;
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.dateFactory = context.getValueFactories().getDateFactory();
        this.pathFactory = context.getValueFactories().getPathFactory();
        assert this.context != null;
        assert this.sourceName != null;
        assert this.workspaceName != null;
        assert this.rules != null;
        assert this.analyzer != null;
        assert this.stringFactory != null;
        assert this.dateFactory != null;
    }

    /**
     * Create the field name that will be used to store the full-text searchable property values.
     * 
     * @param propertyName the name of the property; may not be null
     * @return the field name for the full-text searchable property values; never null
     */
    protected abstract String fullTextFieldName( String propertyName );

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.search.SearchProvider.Session#getContext()
     */
    public final ExecutionContext getContext() {
        return context;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.search.SearchProvider.Session#getSourceName()
     */
    public final String getSourceName() {
        return sourceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.search.SearchProvider.Session#getWorkspaceName()
     */
    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.search.SearchProvider.Session#hasChanges()
     */
    public boolean hasChanges() {
        return changeCount > 0;
    }

    /**
     * Get the Lucene index searcher that should be used to execute queries.
     * 
     * @return the searcher; never null
     * @throws IOException if there is an error obtaining the index searcher
     */
    public abstract IndexSearcher getContentSearcher() throws IOException;

    /**
     * Get the query engine for this session.
     * 
     * @return the query engine; never null
     */
    protected QueryEngine queryEngine() {
        if (queryEngine == null) {
            // Create the query engine ...
            Planner planner = new CanonicalPlanner();
            Optimizer optimizer = new RuleBasedOptimizer() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.query.optimize.RuleBasedOptimizer#populateRuleStack(java.util.LinkedList,
                 *      org.jboss.dna.graph.query.plan.PlanHints)
                 */
                @Override
                protected void populateRuleStack( LinkedList<OptimizerRule> ruleStack,
                                                  PlanHints hints ) {
                    super.populateRuleStack(ruleStack, hints);
                    // Add any custom rules here, either at the front of the stack or at the end
                }
            };
            QueryProcessor processor = new QueryProcessor() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.query.process.QueryProcessor#createAccessComponent(org.jboss.dna.graph.query.model.QueryCommand,
                 *      org.jboss.dna.graph.query.QueryContext, org.jboss.dna.graph.query.plan.PlanNode,
                 *      org.jboss.dna.graph.query.QueryResults.Columns,
                 *      org.jboss.dna.graph.query.process.SelectComponent.Analyzer)
                 */
                @Override
                protected ProcessingComponent createAccessComponent( QueryCommand originalQuery,
                                                                     QueryContext context,
                                                                     PlanNode accessNode,
                                                                     Columns resultColumns,
                                                                     org.jboss.dna.graph.query.process.SelectComponent.Analyzer analyzer ) {
                    try {
                        return LuceneSession.this.createAccessComponent(originalQuery,
                                                                        context,
                                                                        accessNode,
                                                                        resultColumns,
                                                                        analyzer);
                    } catch (IOException e) {
                        I18n msg = SearchI18n.errorWhilePerformingQuery;
                        context.getProblems().addError(e,
                                                       msg,
                                                       Visitors.readable(originalQuery),
                                                       getWorkspaceName(),
                                                       getSourceName(),
                                                       e.getMessage());
                        return null;
                    }
                }
            };

            queryEngine = new QueryEngine(planner, optimizer, processor);
        }
        return queryEngine;
    }

    protected abstract ProcessingComponent createAccessComponent( QueryCommand originalQuery,
                                                                  QueryContext context,
                                                                  PlanNode accessNode,
                                                                  Columns resultColumns,
                                                                  org.jboss.dna.graph.query.process.SelectComponent.Analyzer analyzer )
        throws IOException;

    /**
     * Utility method to create a query to find all of the documents representing nodes with the supplied UUIDs.
     * 
     * @param uuids the UUIDs of the nodes that are to be found; may not be null
     * @return the query; never null
     * @throws IOException if there is a problem creating this query
     */
    public abstract Query findAllNodesWithUuids( Set<UUID> uuids ) throws IOException;

    public abstract Query findAllNodesBelow( Path ancestorPath ) throws IOException;

    /**
     * Return a query that can be used to find all of the documents that represent nodes that are children of the node at the
     * supplied path.
     * 
     * @param parentPath the path of the parent node.
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    public abstract Query findChildNodes( Path parentPath ) throws IOException;

    /**
     * Create a query that can be used to find the one document (or node) that exists at the exact path supplied. This method
     * first queries the {@link PathIndex path index} to find the UUID of the node at the supplied path, and then returns a query
     * that matches the UUID.
     * 
     * @param path the path of the node
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    public abstract Query findNodeAt( Path path ) throws IOException;

    /**
     * Create a query that can be used to find documents (or nodes) that have a field value that satisfies the supplied LIKE
     * expression.
     * 
     * @param fieldName the name of the document field to search
     * @param likeExpression the JCR like expression
     * @return the query; never null
     * @throws IOException if there is an error creating the query
     */
    public abstract Query findNodesLike( String fieldName,
                                         String likeExpression ) throws IOException;

    public abstract Query findNodesWith( Length propertyLength,
                                         Operator operator,
                                         Object value ) throws IOException;

    public abstract Query findNodesWith( PropertyValue propertyValue,
                                         Operator operator,
                                         Object value,
                                         boolean caseSensitive ) throws IOException;

    public abstract Query findNodesWithNumericRange( PropertyValue propertyValue,
                                                     Object lowerValue,
                                                     Object upperValue,
                                                     boolean includesLower,
                                                     boolean includesUpper ) throws IOException;

    public abstract Query findNodesWithNumericRange( NodeDepth depth,
                                                     Object lowerValue,
                                                     Object upperValue,
                                                     boolean includesLower,
                                                     boolean includesUpper ) throws IOException;

    // public abstract Query findNodesWithNumericRange( String field,
    // Object lowerValue,
    // Object upperValue,
    // boolean includesLower,
    // boolean includesUpper ) throws IOException;

    public abstract Query findNodesWith( NodePath nodePath,
                                         Operator operator,
                                         Object value,
                                         boolean caseSensitive ) throws IOException;

    public abstract Query findNodesWith( NodeName nodeName,
                                         Operator operator,
                                         Object value,
                                         boolean caseSensitive ) throws IOException;

    public abstract Query findNodesWith( NodeLocalName nodeName,
                                         Operator operator,
                                         Object value,
                                         boolean caseSensitive ) throws IOException;

    public abstract Query findNodesWith( NodeDepth depthConstraint,
                                         Operator operator,
                                         Object value ) throws IOException;

    // public abstract Query createLocalNameQuery( String likeExpression ) throws IOException;

    // public abstract Query createSnsIndexQuery( String likeExpression ) throws IOException;

    /**
     * Convert the JCR like expression to a Lucene wildcard expression. The JCR like expression uses '%' to match 0 or more
     * characters, '_' to match any single character, '\x' to match the 'x' character, and all other characters to match
     * themselves.
     * 
     * @param likeExpression the like expression; may not be null
     * @return the expression that can be used with a WildcardQuery; never null
     */
    public String toWildcardExpression( String likeExpression ) {
        assert likeExpression != null;
        assert likeExpression.length() > 0;
        return likeExpression.replace('%', '*').replace('_', '?').replaceAll("\\\\(.)", "$1");
    }

    /**
     * Convert the JCR like expression to a regular expression. The JCR like expression uses '%' to match 0 or more characters,
     * '_' to match any single character, '\x' to match the 'x' character, and all other characters to match themselves. Note that
     * if any regex metacharacters appear in the like expression, they will be escaped within the resulting regular expression.
     * 
     * @param likeExpression the like expression; may not be null
     * @return the expression that can be used with a WildcardQuery; never null
     */
    public String toRegularExpression( String likeExpression ) {
        assert likeExpression != null;
        assert likeExpression.length() > 0;
        // Replace all '\x' with 'x' ...
        String result = likeExpression.replaceAll("\\\\(.)", "$1");
        // Escape characters used as metacharacters in regular expressions, including
        // '[', '^', '\', '$', '.', '|', '?', '*', '+', '(', and ')'
        result = result.replaceAll("([[^\\\\$.|?*+()])", "\\$1");
        // Replace '%'->'[.]+' and '_'->'[.]
        result = likeExpression.replace("%", "[.]+").replace("_", "[.]");
        return result;
    }

    public String pathAsString( Path path,
                                ValueFactory<String> stringFactory ) {
        assert path != null;
        if (path.isRoot()) return "/";
        String pathStr = stringFactory.create(path);
        if (!pathStr.endsWith("]")) {
            pathStr = pathStr + '[' + Path.DEFAULT_INDEX + ']';
        }
        return pathStr;
    }
}
