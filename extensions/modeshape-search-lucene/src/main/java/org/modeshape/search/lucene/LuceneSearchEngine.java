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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.modeshape.common.text.FilenameEncoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.basic.JodaDateTime;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.DestroyWorkspaceRequest;
import org.modeshape.graph.request.RemovePropertyRequest;
import org.modeshape.graph.request.RequestType;
import org.modeshape.graph.request.SetPropertyRequest;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.UpdateValuesRequest;
import org.modeshape.graph.search.SearchEngine;
import org.modeshape.graph.search.SearchEngineException;
import org.modeshape.graph.search.SearchEngineIndexer;

/**
 * A {@link SearchEngine} implementation that relies upon two separate indexes to manage the node properties and the node
 * structure (path and children). Using two indexes is more efficient when the node content and structure are updated
 * independently. For example, the structure of the nodes changes whenever same-name-sibling indexes are changed, when sibling
 * nodes are deleted, or when nodes are moved around; in all of these cases, the properties of the nodes do not change.
 */
public class LuceneSearchEngine extends AbstractLuceneSearchEngine<LuceneSearchWorkspace, LuceneSearchProcessor> {

    /**
     * The default set of {@link IndexRules} used by {@link LuceneSearchEngine} instances when no rules are provided. These rules
     * default to index and analyze all properties, and to index the {@link ModeShapeLexicon#UUID dna:uuid} and
     * {@link JcrLexicon#UUID jcr:uuid} properties to be indexed and stored only (not analyzed and not included in full-text
     * search. The rules also treat {@link JcrLexicon#CREATED jcr:created} and {@link JcrLexicon#LAST_MODIFIED jcr:lastModified}
     * properties as dates.
     */
    public static final IndexRules DEFAULT_RULES;

    static {
        // We know that the earliest creation/modified dates cannot be before November 1 2009,
        // which is before this feature was implemented
        long earliestChangeDate = new JodaDateTime(2009, 11, 01, 0, 0, 0, 0).getMilliseconds();

        IndexRules.Builder builder = IndexRules.createBuilder();
        // Configure the default behavior ...
        builder.defaultTo(Field.Store.YES, Field.Index.ANALYZED, true, true);
        // Configure the UUID properties to be just indexed and stored (not analyzed, not included in full-text) ...
        builder.stringField(JcrLexicon.UUID, Field.Store.YES, Field.Index.NOT_ANALYZED, false, false);
        builder.stringField(ModeShapeLexicon.UUID, Field.Store.YES, Field.Index.NOT_ANALYZED, false, false);
        // Configure the properties that we'll treat as dates ...
        builder.dateField(JcrLexicon.CREATED, Field.Store.YES, Field.Index.NOT_ANALYZED, earliestChangeDate);
        builder.dateField(JcrLexicon.LAST_MODIFIED, Field.Store.YES, Field.Index.NOT_ANALYZED, earliestChangeDate);
        DEFAULT_RULES = builder.build();
    }

    /**
     * The default encoder is the FileNameEncoder, which is based upon the {@link UrlEncoder} except that it also encodes the '*'
     * character, which is required for Windows.
     */
    protected static final TextEncoder DEFAULT_ENCODER = new FilenameEncoder();

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
    private final int maxDepthPerIndexRead;

    /**
     * Create a new instance of a {@link SearchEngine} that uses Lucene and a two-index design, and that stores the indexes using
     * the supplied {@link LuceneConfiguration}.
     * 
     * @param sourceName the name of the source that this engine will search over
     * @param connectionFactory the factory for making connections to the source
     * @param verifyWorkspaceInSource true if the workspaces are to be verified using the source, or false if this engine is used
     *        in a way such that all workspaces are known to exist
     * @param maxDepthPerIndexRead the maximum depth that any single request is to read when indexing
     * @param configuration the configuration of the Lucene indexes
     * @param rules the index rule, or null if the default index rules should be used
     * @param analyzer the analyzer, or null if the default analyzer should be used
     * @throws IllegalArgumentException if any of the source name, connection factory, or configuration are null
     */
    public LuceneSearchEngine( String sourceName,
                               RepositoryConnectionFactory connectionFactory,
                               boolean verifyWorkspaceInSource,
                               int maxDepthPerIndexRead,
                               LuceneConfiguration configuration,
                               IndexRules rules,
                               Analyzer analyzer ) {
        super(sourceName, connectionFactory, verifyWorkspaceInSource);
        CheckArg.isNotNull(configuration, "configuration");
        this.configuration = configuration;
        this.analyzer = analyzer != null ? analyzer : new StandardAnalyzer(configuration.getVersion());
        this.rules = rules != null ? rules : DEFAULT_RULES;
        this.maxDepthPerIndexRead = maxDepthPerIndexRead;
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
     * where the {@link #DEFAULT_ENCODER default encoder} is used to ensure that workspace names and index names can be turned
     * into file system directory names.
     * </p>
     * 
     * @param sourceName the name of the source that this engine will search over
     * @param connectionFactory the factory for making connections to the source
     * @param verifyWorkspaceInSource true if the workspaces are to be verified using the source, or false if this engine is used
     *        in a way such that all workspaces are known to exist
     * @param maxDepthPerIndexRead the maximum depth that any single request is to read when indexing
     * @param indexStorageDirectory the file system directory in which the indexes are to be kept
     * @param rules the index rule, or null if the default index rules should be used
     * @param analyzer the analyzer, or null if the default analyzer should be used
     * @throws IllegalArgumentException if any of the source name, connection factory, or directory are null
     */
    public LuceneSearchEngine( String sourceName,
                               RepositoryConnectionFactory connectionFactory,
                               boolean verifyWorkspaceInSource,
                               int maxDepthPerIndexRead,
                               File indexStorageDirectory,
                               IndexRules rules,
                               Analyzer analyzer ) {
        this(sourceName, connectionFactory, verifyWorkspaceInSource, maxDepthPerIndexRead,
             LuceneConfigurations.using(indexStorageDirectory, null, DEFAULT_ENCODER, DEFAULT_ENCODER), null, null);
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
     * @param maxDepthPerIndexRead the maximum depth that any single request is to read when indexing
     * @param rules the index rule, or null if the default index rules should be used
     * @param analyzer the analyzer, or null if the default analyzer should be used
     * @throws IllegalArgumentException if any of the source name or connection factory are null
     */
    public LuceneSearchEngine( String sourceName,
                               RepositoryConnectionFactory connectionFactory,
                               boolean verifyWorkspaceInSource,
                               int maxDepthPerIndexRead,
                               IndexRules rules,
                               Analyzer analyzer ) {
        this(sourceName, connectionFactory, verifyWorkspaceInSource, maxDepthPerIndexRead, LuceneConfigurations.inMemory(), null,
             null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.search.AbstractSearchEngine#createProcessor(ExecutionContext,
     *      org.modeshape.graph.search.AbstractSearchEngine.Workspaces, Observer, boolean)
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
     * @see org.modeshape.graph.search.AbstractSearchEngine#createWorkspace(org.modeshape.graph.ExecutionContext,
     *      java.lang.String)
     */
    @Override
    protected LuceneSearchWorkspace createWorkspace( ExecutionContext context,
                                                     String workspaceName ) throws SearchEngineException {
        return new LuceneSearchWorkspace(workspaceName, configuration, rules, analyzer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.search.SearchEngine#index(org.modeshape.graph.ExecutionContext, java.lang.Iterable)
     */
    public void index( ExecutionContext context,
                       final Iterable<ChangeRequest> changes ) throws SearchEngineException {
        // Process the changes to determine what work needs to be done ...
        WorkForWorkspaces allWork = new WorkForWorkspaces(context);
        for (ChangeRequest change : changes) {
            WorkspaceWork work = allWork.forWorkspace(change.changedWorkspace());
            assert work != null;
            if (!work.add(change)) break;
        }

        // Now do the work ...
        SearchEngineIndexer indexer = new SearchEngineIndexer(context, this, getConnectionFactory(), maxDepthPerIndexRead);
        try {
            for (WorkspaceWork workspaceWork : allWork) {
                if (workspaceWork.indexAllContent) {
                    indexer.index(workspaceWork.workspaceName);
                } else if (workspaceWork.deleteWorkspace) {
                    indexer.process(new DestroyWorkspaceRequest(workspaceWork.workspaceName));
                } else {
                    for (WorkRequest request : workspaceWork) {
                        if (request instanceof CrawlSubgraph) {
                            CrawlSubgraph crawlRequest = (CrawlSubgraph)request;
                            Location location = crawlRequest.location;
                            indexer.index(workspaceWork.workspaceName, location, crawlRequest.depth);
                        } else if (request instanceof ForwardRequest) {
                            ForwardRequest forwardRequest = (ForwardRequest)request;
                            indexer.process(forwardRequest.changeRequest.clone());
                        }
                    }
                }
            }
        } finally {
            indexer.close();
        }
    }

    protected static class WorkForWorkspaces implements Iterable<WorkspaceWork> {
        private Map<String, WorkspaceWork> byWorkspaceName = new HashMap<String, WorkspaceWork>();
        private final ExecutionContext context;

        protected WorkForWorkspaces( ExecutionContext context ) {
            this.context = context;
        }

        protected WorkspaceWork forWorkspace( String workspaceName ) {
            WorkspaceWork work = byWorkspaceName.get(workspaceName);
            if (work == null) {
                work = new WorkspaceWork(context, workspaceName);
                byWorkspaceName.put(workspaceName, work);
            }
            return work;
        }

        public Iterator<WorkspaceWork> iterator() {
            return byWorkspaceName.values().iterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (WorkspaceWork work : byWorkspaceName.values()) {
                sb.append(work.toString()).append('\n');
            }
            return sb.toString();
        }
    }

    protected static class WorkspaceWork implements Iterable<WorkRequest> {
        private final ExecutionContext context;
        protected final String workspaceName;
        protected final Map<Path, WorkRequest> requestByPath = new HashMap<Path, WorkRequest>();
        protected boolean indexAllContent;
        protected boolean deleteWorkspace;

        protected WorkspaceWork( ExecutionContext context,
                                 String workspaceName ) {
            this.context = context;
            this.workspaceName = workspaceName;
        }

        public Iterator<WorkRequest> iterator() {
            return requestByPath.values().iterator();
        }

        protected boolean add( ChangeRequest change ) {
            assert !indexAllContent;
            assert !deleteWorkspace;

            switch (change.getType()) {
                case CLONE_WORKSPACE:
                case CREATE_WORKSPACE:
                    requestByPath.clear();
                    indexAllContent = true;
                    return false;
                case DESTROY_WORKSPACE:
                    requestByPath.clear();
                    deleteWorkspace = true;
                    return false;
                default:
                    // Do nothing with other request types ...
                    break;
            }

            Location changedLocation = change.changedLocation();
            assert changedLocation.hasPath();
            if (isCoveredByExistingCrawl(changedLocation.getPath())) {
                // There's no point in adding the request if it's already covered by a crawl ...
                return true;
            }

            switch (change.getType()) {
                case UPDATE_PROPERTIES:
                    // Try merging this onto any existing work ...
                    if (mergeWithExistingWork(changedLocation, change)) return true;

                    // See if this request removed all existing properties ...
                    UpdatePropertiesRequest update = (UpdatePropertiesRequest)change;
                    if (update.removeOtherProperties()) {
                        // This request specified all of the properties, so we can just forward it on ...
                        forward(changedLocation, update);
                        return true;
                    }
                    // This changed an existing property, and we don't know what that value is ...
                    crawl(update.changedLocation(), 1);
                    return true;
                case SET_PROPERTY:
                    // Try merging this onto any existing work ...
                    if (mergeWithExistingWork(changedLocation, change)) return true;

                    // We can't ADD to a document in the index, so we have to scan it all ..
                    crawl(changedLocation, 1);
                    return true;
                case UPDATE_VALUES:
                    // Try merging this onto any existing work ...
                    if (mergeWithExistingWork(changedLocation, change)) return true;

                    // We can't ADD to a document in the index, so we have to scan it all ..
                    crawl(changedLocation, 1);
                    return true;
                case CREATE_NODE:
                    forward(changedLocation, change);
                    return true;
                default:
                    // Do nothing with other request types ...
                    break;
            }

            // Otherwise just index the whole friggin' workspace ...
            requestByPath.clear();
            indexAllContent = true;
            return false;
        }

        private boolean mergeWithExistingWork( Location location,
                                               ChangeRequest change ) {
            Path path = location.getPath();
            WorkRequest existing = requestByPath.get(path);
            if (existing instanceof CrawlSubgraph) {
                // There's no point in adding the request, but return that it's merged ...
                return true;
            }
            if (existing instanceof ForwardRequest) {
                ChangeRequest existingRequest = ((ForwardRequest)existing).changeRequest;
                // Try to merge the requests ...
                ChangeRequest merged = merge(context, existingRequest, change);
                if (merged != null) {
                    forward(location, merged);
                    return true;
                }
            }
            // Couldn't merge this with any existing work ...
            return false;
        }

        private void forward( Location location,
                              ChangeRequest request ) {
            requestByPath.put(location.getPath(), new ForwardRequest(request));
        }

        private boolean isCoveredByExistingCrawl( Path path ) {
            // Walk up the location, and look for crawling work for a higher node ...
            while (!path.isRoot()) {
                path = path.getParent();
                WorkRequest existing = requestByPath.get(path);
                if (existing == null) continue;
                if (existing instanceof CrawlSubgraph) {
                    CrawlSubgraph crawl = (CrawlSubgraph)existing;
                    if (crawl.depth != 1) {
                        // There's no point in adding the request, but return that it's merged ...
                        return true;
                    }
                }
            }
            return false;
        }

        private void crawl( Location location,
                            int depth ) {
            Path path = location.getPath();
            // Remove all work below this point ...
            Iterator<Map.Entry<Path, WorkRequest>> iter = requestByPath.entrySet().iterator();
            while (iter.hasNext()) {
                if (iter.next().getKey().isDecendantOf(path)) iter.remove();
            }
            requestByPath.put(path, new CrawlSubgraph(location, depth));
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("  Workspace: ").append(workspaceName).append('\n');
            for (Map.Entry<Path, WorkRequest> entry : requestByPath.entrySet()) {
                sb.append("    ")
                  .append(entry.getKey().getString(context.getNamespaceRegistry()))
                  .append("->")
                  .append(entry.getValue().toString(context))
                  .append('\n');
            }
            return sb.toString();
        }
    }

    protected static ChangeRequest merge( ExecutionContext context,
                                          ChangeRequest original,
                                          ChangeRequest change ) {
        assert !original.hasError();
        assert !change.hasError();
        if (RequestType.CREATE_NODE == original.getType()) {
            CreateNodeRequest create = (CreateNodeRequest)original;

            Map<Name, Property> newProperties;
            CreateNodeRequest newRequest;

            switch (change.getType()) {
                case CREATE_NODE:
                    SetPropertyRequest set = (SetPropertyRequest)change;
                    Name newPropertyName = set.property().getName();
                    List<Property> newPropertiesList = new LinkedList<Property>();
                    for (Property property : create.properties()) {
                        if (property.getName().equals(newPropertyName)) continue;
                        newPropertiesList.add(property);
                    }
                    newPropertiesList.add(set.property());
                    newRequest = new CreateNodeRequest(create.under(), create.inWorkspace(), create.named(),
                                                       create.conflictBehavior(), newPropertiesList);
                    newRequest.setActualLocationOfNode(create.getActualLocationOfNode());
                    return newRequest;

                case UPDATE_PROPERTIES:
                    UpdatePropertiesRequest update = (UpdatePropertiesRequest)change;
                    newProperties = new HashMap<Name, Property>();
                    for (Property property : create.properties()) {
                        newProperties.put(property.getName(), property);
                    }
                    newProperties.putAll(update.properties());
                    newRequest = new CreateNodeRequest(create.under(), create.inWorkspace(), create.named(),
                                                       create.conflictBehavior(), newProperties.values());
                    newRequest.setActualLocationOfNode(create.getActualLocationOfNode());
                    return newRequest;

                case REMOVE_PROPERTY:
                    RemovePropertyRequest remove = (RemovePropertyRequest)change;
                    newProperties = new HashMap<Name, Property>();
                    for (Property property : create.properties()) {
                        newProperties.put(property.getName(), property);
                    }
                    newProperties.remove(remove.propertyName());
                    newRequest = new CreateNodeRequest(create.under(), create.inWorkspace(), create.named(),
                                                       create.conflictBehavior(), newProperties.values());
                    newRequest.setActualLocationOfNode(create.getActualLocationOfNode());
                    return newRequest;

                case UPDATE_VALUES:
                    UpdateValuesRequest updateRequest = (UpdateValuesRequest)change;
                    newProperties = new HashMap<Name, Property>();
                    for (Property property : create.properties()) {
                        newProperties.put(property.getName(), property);
                    }
                    Property updated = newProperties.get(updateRequest.property());
                    if (updated != null) {
                        // Changing an existing property ...
                        List<Object> newValues = new LinkedList<Object>();
                        for (Object existingValue : updated) {
                            newValues.add(existingValue);
                        }
                        newValues.removeAll(updateRequest.removedValues());
                        newValues.addAll(updateRequest.addedValues());
                        updated = context.getPropertyFactory().create(updateRequest.property(), newValues);
                    } else {
                        // The property doesn't exist ...
                        updated = context.getPropertyFactory().create(updateRequest.property(), updateRequest.addedValues());
                    }
                    newProperties.put(updated.getName(), updated);
                    newRequest = new CreateNodeRequest(create.under(), create.inWorkspace(), create.named(),
                                                       create.conflictBehavior(), newProperties.values());
                    newRequest.setActualLocationOfNode(create.getActualLocationOfNode());
                    return newRequest;
                default:
                    // Do nothing with other request types ...
                    break;
            }
        }
        if (RequestType.UPDATE_PROPERTIES == original.getType()) {
            UpdatePropertiesRequest update = (UpdatePropertiesRequest)original;
            UpdatePropertiesRequest newRequest;
            Map<Name, Property> newProperties;

            switch (change.getType()) {
                case SET_PROPERTY:
                    SetPropertyRequest set = (SetPropertyRequest)change;
                    Property newProperty = set.property();
                    Name newPropertyName = newProperty.getName();
                    newProperties = new HashMap<Name, Property>(update.properties());
                    newProperties.put(newPropertyName, newProperty);
                    newRequest = new UpdatePropertiesRequest(update.getActualLocationOfNode(), update.inWorkspace(),
                                                             newProperties, update.removeOtherProperties());
                    newRequest.setActualLocationOfNode(update.getActualLocationOfNode());
                    return newRequest;
                case REMOVE_PROPERTY:
                    RemovePropertyRequest remove = (RemovePropertyRequest)change;
                    newProperties = new HashMap<Name, Property>(update.properties());
                    newProperties.remove(remove.propertyName());
                    newRequest = new UpdatePropertiesRequest(update.getActualLocationOfNode(), update.inWorkspace(),
                                                             newProperties, update.removeOtherProperties());
                    newRequest.setActualLocationOfNode(update.getActualLocationOfNode());
                    return newRequest;
                case UPDATE_VALUES:
                    UpdateValuesRequest updateValues = (UpdateValuesRequest)change;
                    newProperties = new HashMap<Name, Property>(update.properties());
                    Property updated = newProperties.get(updateValues.property());
                    if (updated != null) {
                        // Changing an existing property ...
                        List<Object> newValues = new LinkedList<Object>();
                        for (Object existingValue : updated) {
                            newValues.add(existingValue);
                        }
                        newValues.removeAll(updateValues.removedValues());
                        newValues.addAll(updateValues.addedValues());
                        updated = context.getPropertyFactory().create(updateValues.property(), newValues);
                    } else {
                        // The property doesn't exist ...
                        updated = context.getPropertyFactory().create(updateValues.property(), updateValues.addedValues());
                    }
                    newProperties.put(updated.getName(), updated);
                    newRequest = new UpdatePropertiesRequest(update.getActualLocationOfNode(), update.inWorkspace(),
                                                             newProperties, update.removeOtherProperties());
                    newRequest.setActualLocationOfNode(update.getActualLocationOfNode());
                    return newRequest;
                default:
                    // Do nothing with other request types ...
                    break;
            }
        }
        return null;
    }

    @Immutable
    protected static abstract class WorkRequest {
        public abstract String toString( ExecutionContext context );

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return toString(new ExecutionContext());
        }
    }

    @Immutable
    protected static class CrawlSubgraph extends WorkRequest {
        protected final Location location;
        protected final int depth;

        protected CrawlSubgraph( Location location,
                                 int depth ) {
            this.location = location;
            this.depth = depth;
        }

        @Override
        public String toString( ExecutionContext context ) {
            return "Crawl " + location.getPath().getString(context.getNamespaceRegistry());
        }
    }

    @Immutable
    protected static class ForwardRequest extends WorkRequest {
        protected final ChangeRequest changeRequest;

        protected ForwardRequest( ChangeRequest changeRequest ) {
            this.changeRequest = changeRequest;
        }

        @Override
        public String toString( ExecutionContext context ) {
            return "Forward " + changeRequest;
        }
    }
}
