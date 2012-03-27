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
package org.modeshape.jcr.query.lucene.basic;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.NodeTypeSchemata;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.IndexRules;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.lucene.FieldUtil;
import org.modeshape.jcr.query.lucene.LuceneException;
import org.modeshape.jcr.query.lucene.LuceneProcessingContext;
import org.modeshape.jcr.query.lucene.LuceneQuery;
import org.modeshape.jcr.query.lucene.LuceneQueryEngine.TupleCollector;
import org.modeshape.jcr.query.lucene.LuceneQueryFactory;
import org.modeshape.jcr.query.lucene.LuceneSchema;
import org.modeshape.jcr.query.lucene.basic.NodeInfoIndex.FieldName;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.SetCriteria;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * The LuceneSchema implementation that uses a single index for {@link NodeInfoIndex node information}.
 */
public class BasicLuceneSchema implements LuceneSchema {

    private final Integer TRUE_VALUE = new Integer(1);
    private final Integer FALSE_VALUE = new Integer(0);

    private final SearchFactoryImplementor searchFactory;
    private final Version version;
    private final NamespaceRegistry namespaces;
    private final ExecutionContext context;
    private final Logger logger;
    private final BinaryStore binaryStore;
    private final ValueFactory<String> stringFactory;

    /**
     * @param context the execution context for the repository
     * @param searchFactory the search factory for accessing the indexes
     * @param version the Lucene version
     */
    public BasicLuceneSchema( ExecutionContext context,
                              SearchFactoryImplementor searchFactory,
                              Version version ) {
        this.searchFactory = searchFactory;
        this.context = context;
        this.stringFactory = this.context.getValueFactories().getStringFactory();
        this.binaryStore = this.context.getBinaryStore();
        this.version = version;
        // TODO: This should use the durable namespaces ...
        this.namespaces = context.getNamespaceRegistry();
        this.logger = Logger.getLogger(getClass());
        assert this.searchFactory != null;
    }

    @Override
    public LuceneQueryFactory createLuceneQueryFactory( QueryContext context,
                                                        SearchFactory searchFactory ) {
        return new BasicLuceneQueryFactory(context, searchFactory, version);
    }

    protected final String stringFrom( Path path ) {
        if (path.isRoot()) return "/";
        StringBuilder sb = new StringBuilder();
        for (Path.Segment segment : path) {
            sb.append('/');
            sb.append(segment.getName().getString(namespaces));
            sb.append('[');
            sb.append(segment.getIndex());
            sb.append(']');
        }
        return sb.toString();
    }

    protected final String stringFrom( Name name ) {
        return name.getString(namespaces);
    }

    protected final NodeInfo nodeInfo( String id,
                                       String workspace,
                                       Path path,
                                       Name primaryType,
                                       Set<Name> mixinTypes,
                                       Iterator<Property> propertyIterator,
                                       NodeTypeSchemata schemata ) {
        String pathStr = null;
        String name = null;
        String localName = null;
        int depth = 0;
        if (path.isRoot()) {
            pathStr = "/";
            name = "";
            localName = "";
            depth = 0;
        } else {
            pathStr = stringFrom(path);
            Name nodeName = path.getLastSegment().getName();
            name = stringFrom(nodeName);
            localName = nodeName.getLocalName();
            depth = path.size();
        }

        // Get the schemata tables for the primary type and mixin types ...
        IndexRules rules = schemata.getIndexRules();

        // Start accumulating the full-text search value for the whole node ...
        StringBuilder fullText = new StringBuilder();
        fullText.append(localName);

        // Process the properties ...
        DynamicField dynamicField = null;
        while (propertyIterator.hasNext()) {
            Property property = propertyIterator.next();
            Name propertyName = property.getName();
            String propName = stringFrom(propertyName);
            if (property.isEmpty()) continue;
            IndexRules.Rule rule = rules.getRule(propertyName);
            if (rule.isSkipped()) continue;
            Field.Store store = rule.getStoreOption();
            boolean fullTextSearchable = rule.isFullTextSearchable();

            // TODO: Query - fulltext search (remove next line)
            fullTextSearchable = false;
            if (property.isSingle()) {
                // Create a field with the value ...
                Object value = property.getFirstValue();
                dynamicField = addDynamicField(propName, value, dynamicField, store, fullTextSearchable, fullText);
            } else {
                Iterator<?> iter = property.getValues();
                while (iter.hasNext()) {
                    Object value = iter.next();
                    dynamicField = addDynamicField(propName, value, dynamicField, store, fullTextSearchable, fullText);
                }
            }
        }

        // TODO: Query - fulltext search (add back in)
        // Add the full-text searchable text for the whole node ...
        // dynamicField = new DynamicField(dynamicField, NodeInfoIndex.FieldName.FULL_TEXT, fullText.toString(), true, false);

        // Return the node information object ...
        return new NodeInfo(id, workspace, pathStr, name, localName, depth, dynamicField);
    }

    /**
     * Create a dynamic field to store the property value.
     * 
     * @param propertyName the name of the field in which the property value is to be stored; never null
     * @param value the property value; may be null
     * @param previous the previous DynamicField; may be null
     * @param stored true if the value is to be stored so it can be used in comparison criteria
     * @param fullTextSearchable true if this property value should be full-text searchable
     * @param fullText the node's full-text search text to which any full-text searchable terms should be added; will not be null
     *        if <code>fullTextSearchable</code> is <code>true</code>
     * @return the new dynamic field, or the <code>previous</code> if none are created
     */
    protected final DynamicField addDynamicField( String propertyName,
                                                  Object value,
                                                  DynamicField previous,
                                                  Field.Store stored,
                                                  boolean fullTextSearchable,
                                                  StringBuilder fullText ) {
        // Create a field with the value ...
        boolean isStored = stored == Field.Store.YES;
        if (value instanceof String) {
            String str = (String)value;
            // Add a field with the string value and do NOT analyze (so we can compare exact matches) ...
            previous = new DynamicField(previous, propertyName, str, false, true);

            // Add a field with the length of the value ...
            previous = new DynamicField(previous, FieldName.LENGTH_PREFIX + propertyName, str.length(), false, true);

            if (fullTextSearchable) {
                // Also add a field with the string value and DO analyze it so we can find it with full-text search ...
                String ftsPropName = FieldName.FULL_TEXT_PREFIX + propertyName;
                previous = new DynamicField(previous, ftsPropName, str, true, false); // never store

                // Add add the string to the node's full-text value ...
                fullText.append(' ').append(str);
            }

        } else if (value instanceof Binary) {
            Binary binary = (Binary)value;

            // Add a field with the length of the binary value ...
            previous = new DynamicField(previous, FieldName.LENGTH_PREFIX + propertyName, binary.getSize(), false, true);

            if (fullTextSearchable) {
                // Get the full text ...
                try {
                    String fullTextTerms = binaryStore.getText(binary);
                    if (fullTextTerms != null) {
                        previous = new DynamicField(previous, propertyName, fullTextTerms, true, false); // never store

                        // Add add the string to the node's full-text value ...
                        fullText.append(' ').append(fullTextTerms);
                    }
                } catch (BinaryStoreException e) {
                    // Log it ...
                    logger.error(e, JcrI18n.errorExtractingTextFromBinary, binary, e.getLocalizedMessage());
                }
            }

            // Get the SHA-1 ...
            String sha1 = binary.getHexHash();
            String sha1PropName = FieldName.BINARY_SHA1_PREFIX + propertyName;
            previous = new DynamicField(previous, sha1PropName, sha1, false, false); // never stored

        } else if (value instanceof DateTime) {
            // ModeShape 2.x indexed timestamps as longs, enabling use of range queries.
            // Note that Hibernate Search generally uses string representations.
            DateTime timestamp = (DateTime)value;
            previous = new DynamicField(previous, propertyName, timestamp.getMillisecondsInUtc(), false, isStored);

            // Add a field with the length of the value ...
            String strValue = stringFactory.create(timestamp);
            previous = new DynamicField(previous, FieldName.LENGTH_PREFIX + propertyName, strValue.length(), false, true);

            // No full-text search field for date-times ...

        } else if (value instanceof BigDecimal) {
            BigDecimal decimal = (BigDecimal)value;

            // Convert big decimals to a string, using a specific format ...
            String strValue = FieldUtil.decimalToString(decimal);
            previous = new DynamicField(previous, propertyName, strValue, false, isStored);

            // Add a field with the length of the value ...
            strValue = stringFactory.create(decimal);
            previous = new DynamicField(previous, FieldName.LENGTH_PREFIX + propertyName, strValue.length(), false, true);

            // No full-text search field for decimals ...

        } else if (value instanceof Boolean) {
            Boolean bool = (Boolean)value;

            // Convert big decimals to a string, using a specific format ...
            previous = new DynamicField(previous, propertyName, bool ? TRUE_VALUE : FALSE_VALUE, false, true);

            // No length or full-text search field for numbers ...

        } else if (value instanceof Number) {
            Number number = (Number)value;

            // Convert big decimals to a string, using a specific format ...
            previous = new DynamicField(previous, propertyName, number, false, isStored);

            // No length or full-text search field for numbers ...

        } else if (value instanceof Reference) {
            // We just will extract the string-form of the reference value and index it in the property field ...
            Reference ref = (Reference)value;
            String stringifiedRef = ref.getString();
            previous = new DynamicField(previous, propertyName, stringifiedRef, false, true); // must store references since not
                                                                                              // analyzed

            // And add it to the field for all the weak or strong references from this node ...
            String propName = ref.isWeak() ? FieldName.ALL_REFERENCES : FieldName.STRONG_REFERENCES;
            previous = new DynamicField(previous, propName, stringifiedRef, false, isStored);

            // Add a field with the length of the value ...
            previous = new DynamicField(previous, FieldName.LENGTH_PREFIX + propertyName, stringifiedRef.length(), false, true);

            // No full-text search field for references ...

        } else if (value != null) {
            // Don't care what type it is because it's not full text searchable and should not be analyzed ...
            String str = stringFactory.create(value);
            previous = new DynamicField(previous, propertyName, str, false, isStored);

            // Add a field with the length of the value (yes, we have to compute this) ...
            previous = new DynamicField(previous, FieldName.LENGTH_PREFIX + propertyName, str.length(), false, true);
        }
        return previous;
    }

    @Override
    public void addToIndex( String workspace,
                            NodeKey key,
                            Path path,
                            Name primaryType,
                            Set<Name> mixinTypes,
                            Collection<Property> properties,
                            NodeTypeSchemata schemata,
                            TransactionContext txnCtx ) {
        String id = key.toString();
        NodeInfo nodeInfo = nodeInfo(id, workspace, path, primaryType, mixinTypes, properties.iterator(), schemata);
        logger.trace("index for \"{0}\" workspace: ADD    {1} ", workspace, nodeInfo);
        Work<NodeInfo> work = new Work<NodeInfo>(nodeInfo, id, WorkType.ADD);
        searchFactory.getWorker().performWork(work, txnCtx);
    }

    @Override
    public void updateIndex( String workspace,
                             NodeKey key,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes,
                             Iterator<Property> properties,
                             NodeTypeSchemata schemata,
                             TransactionContext txnCtx ) {
        String id = key.toString();
        NodeInfo nodeInfo = nodeInfo(id, workspace, path, primaryType, mixinTypes, properties, schemata);
        logger.trace("index for \"{0}\" workspace: UPDATE {1} ", workspace, nodeInfo);
        Work<NodeInfo> work = new Work<NodeInfo>(nodeInfo, id, WorkType.UPDATE);
        searchFactory.getWorker().performWork(work, txnCtx);
    }

    @Override
    public void removeFromIndex( String workspace,
                                 Iterable<NodeKey> keys,
                                 TransactionContext txnCtx ) {
        // Use the worker to "PURGE" the document. Note that in Hibernate Search, "PURGE" is used to remove an item from
        // the indexes when the item is not removed from the database, while "DELETE" removes the item from the database
        // and from the indexes. But with how we're using Hibernate Search, there isn't much of a difference.
        // So use "PURGE" since it is semantically closer to what we want to do.
        Worker worker = searchFactory.getWorker();
        for (NodeKey key : keys) {
            String id = key.toString();

            // Remove the node info ...
            Work<NodeInfo> work = new Work<NodeInfo>(NodeInfo.class, id, WorkType.PURGE);
            logger.trace("index for \"{0}\" workspace: REMOVE {1} ", workspace, id);
            worker.performWork(work, txnCtx);
        }
    }

    @Override
    public void removeAllFromIndex( String workspace,
                                    TransactionContext txnCtx ) {
        // Remove all the node infos ...
        Work<Object> work = new Work<Object>(Object.class, (Serializable)null, WorkType.PURGE_ALL);
        logger.trace("index for \"{0}\" workspace: REMOVEALL", workspace);
        searchFactory.getWorker().performWork(work, txnCtx);
    }

    @Override
    public void addBinaryToIndex( org.modeshape.jcr.api.Binary binary,
                                  TransactionContext txnCtx ) {
        // Do nothing here, since this schema does not index binary information separately...
    }

    @Override
    public void removeBinariesFromIndex( Iterable<String> sha1s,
                                         TransactionContext txnCtx ) {
        // Do nothing here, since this schema does not index binary information separately...
    }

    @Override
    public LuceneQuery createQuery( SelectorName selectorName,
                                    List<Constraint> andedConstraints,
                                    LuceneProcessingContext processingContext ) throws LuceneException {
        try {
            LuceneQuery queries = new LuceneQuery(NodeInfoIndex.INDEX_NAME);
            LuceneQueryFactory queryFactory = processingContext.getQueryFactory();

            // Create a constraint that limits the query to the workspace(s) we're interested in ...
            Set<String> workspaceNames = processingContext.getWorkspaceNames();
            int numWorkspaceNames = workspaceNames.size();
            if (numWorkspaceNames > 0) {
                DynamicOperand workspaceField = new PropertyValue(selectorName, NodeInfoIndex.FieldName.WORKSPACE);
                Constraint workspacesConstraint = null;
                if (numWorkspaceNames == 1) {
                    // Use a simple comparison criteria ...
                    String workspaceName = workspaceNames.iterator().next();
                    Literal workspaceValue = new Literal(workspaceName);
                    workspacesConstraint = new Comparison(workspaceField, Operator.EQUAL_TO, workspaceValue);
                } else if (numWorkspaceNames > 1) {
                    // Use a set criteria ...
                    List<Literal> workspaceValues = new ArrayList<Literal>(numWorkspaceNames);
                    for (String workspaceName : workspaceNames) {
                        workspaceValues.add(new Literal(workspaceName));
                    }
                    workspacesConstraint = new SetCriteria(workspaceField, workspaceValues);
                }
                // Now create a query for the workspaces constraint ...
                Query constraintQuery = queryFactory.createQuery(selectorName, workspacesConstraint);
                queries.addQuery(constraintQuery);
            }

            // Create a query for each constraint ...
            for (Constraint andedConstraint : andedConstraints) {
                Query constraintQuery = queryFactory.createQuery(selectorName, andedConstraint);

                // and add it to our queries ...
                if (constraintQuery != null) {
                    queries.addQuery(constraintQuery);
                } else {
                    // The AND-ed constraint _cannot_ be represented as a push-down Lucene query ...
                    queries.addConstraintForPostprocessing(andedConstraint);
                }
            }
            return queries;

        } catch (IOException e) {
            // There was a error working with the constraints (such as a ValueFormatException) ...
            throw new LuceneException(e);
        } catch (RuntimeException e) {
            // There was a error working with the constraints (such as a ValueFormatException) ...
            throw e;
        }
    }

    @Override
    public TupleCollector createTupleCollector( QueryContext queryContext,
                                                Columns columns ) {
        return new BasicTupleCollector(queryContext, columns);
    }
}
