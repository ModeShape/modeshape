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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.lucene.LuceneException;
import org.modeshape.jcr.query.lucene.LuceneProcessingContext;
import org.modeshape.jcr.query.lucene.LuceneQuery;
import org.modeshape.jcr.query.lucene.LuceneQueryEngine.TupleCollector;
import org.modeshape.jcr.query.lucene.LuceneQueryFactory;
import org.modeshape.jcr.query.lucene.LuceneSchema;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * 
 */
public class BasicLuceneSchema implements LuceneSchema {

    private final SearchFactoryImplementor searchFactory;
    private final NamespaceRegistry namespaces;
    private final ExecutionContext context;
    private final Logger logger;

    /**
     * @param context the execution context for the repository
     * @param searchFactory the search factory for accessing the indexes
     */
    public BasicLuceneSchema( ExecutionContext context,
                              SearchFactoryImplementor searchFactory ) {
        this.searchFactory = searchFactory;
        this.context = context;
        // TODO: This should use the durable namespaces ...
        this.namespaces = context.getNamespaceRegistry();
        this.logger = Logger.getLogger(getClass());
        assert this.searchFactory != null;
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
                                       Collection<Property> properties ) {
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
        // Process the properties ...
        StringBuilder fullText = null;
        Map<String, Object> propertiesByName = new HashMap<String, Object>();
        for (Property property : properties) {
            String propName = stringFrom(property.getName());
            if (property.isEmpty()) {
                continue;
            } else if (property.isSingle()) {
                propertiesByName.put(propName, property.getFirstValue());
            } else {
                propertiesByName.put(propName, property.getValuesAsArray());
            }
        }
        String fullTextStr = fullText != null ? fullText.toString() : null;
        return new NodeInfo(id, workspace, pathStr, name, localName, depth, propertiesByName, fullTextStr);
    }

    @Override
    public void addToIndex( String workspace,
                            NodeKey key,
                            Path path,
                            Collection<Property> properties,
                            TransactionContext txnCtx ) {
        String id = key.toString();
        NodeInfo nodeInfo = nodeInfo(id, workspace, path, properties);
        Work<NodeInfo> work = new Work<NodeInfo>(nodeInfo, id, WorkType.ADD);
        searchFactory.getWorker().performWork(work, txnCtx);
    }

    @Override
    public void updateIndex( String workspace,
                             NodeKey key,
                             Path path,
                             Iterator<Property> propertiesIterator,
                             TransactionContext txnCtx ) {
        Collection<Property> props = new LinkedList<Property>();
        while (propertiesIterator.hasNext()) {
            props.add(propertiesIterator.next());
        }
        updateIndex(workspace, key, path, props, txnCtx);
    }

    @Override
    public void updateIndex( String workspace,
                             NodeKey key,
                             Path path,
                             Collection<Property> properties,
                             TransactionContext txnCtx ) {
        String id = key.toString();
        NodeInfo nodeInfo = nodeInfo(id, workspace, path, properties);
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
            worker.performWork(work, txnCtx);
        }
    }

    @Override
    public void removeAllFromIndex( String workspace,
                                    TransactionContext txnCtx ) {
        // Remove all the node infos ...
        Work<Object> work = new Work<Object>(Object.class, (Serializable)null, WorkType.PURGE_ALL);
        searchFactory.getWorker().performWork(work, txnCtx);
    }

    @Override
    public void addBinaryToIndex( Binary binary,
                                  TransactionContext txnCtx ) {
        String sha1 = binary.getHexHash();
        // Extract the text from the binary content ...
        try {
            String text = context.getBinaryStore().getText((org.modeshape.jcr.value.Binary)binary);
            BinaryInfo binaryInfo = new BinaryInfo(sha1, text);
            Work<BinaryInfo> binaryWork = new Work<BinaryInfo>(binaryInfo, binaryInfo.getSha1(), WorkType.ADD);
            searchFactory.getWorker().performWork(binaryWork, txnCtx);
        } catch (Throwable t) {
            logger.error(t, JcrI18n.errorAddingBinaryTextToIndex, sha1);
        }

    }

    @Override
    public void removeBinariesFromIndex( Iterable<String> sha1s,
                                         TransactionContext txnCtx ) {
        // Use the worker to "PURGE" the document. Note that in Hibernate Search, "PURGE" is used to remove an item from
        // the indexes when the item is not removed from the database, while "DELETE" removes the item from the database
        // and from the indexes. But with how we're using Hibernate Search, there isn't much of a difference.
        // So use "PURGE" since it is semantically closer to what we want to do.
        Worker worker = searchFactory.getWorker();
        for (String sha1 : sha1s) {
            // Remove the binary info ...
            Work<BinaryInfo> binaryWork = new Work<BinaryInfo>(BinaryInfo.class, sha1, WorkType.PURGE);
            worker.performWork(binaryWork, txnCtx);
        }
    }

    @Override
    public LuceneQuery createQuery( List<Constraint> andedConstraints,
                                    LuceneProcessingContext processingContext ) throws LuceneException {
        try {
            LuceneQuery queries = new LuceneQuery(NodeInfoIndex.INDEX_NAME);
            // Create a query for each constraint ...
            LuceneQueryFactory queryFactory = processingContext.getQueryFactory();
            for (Constraint andedConstraint : andedConstraints) {
                Query constraintQuery = queryFactory.createQuery(andedConstraint);

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
