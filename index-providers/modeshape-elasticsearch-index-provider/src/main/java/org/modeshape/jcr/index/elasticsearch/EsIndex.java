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
package org.modeshape.jcr.index.elasticsearch;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.jcr.query.qom.Constraint;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.index.elasticsearch.client.EsClient;
import org.modeshape.jcr.index.elasticsearch.client.EsRequest;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.provider.ProvidedIndex;

/**
 * Index stored in Elasticsearch.
 *
 * @author kulikov
 */
public class EsIndex implements ProvidedIndex {

    private final String name;
    private final String workspace;
    private final EsIndexColumns columns;
    private final Operations operations;
    private final EsClient client;
    
    /**
     * Creates new index.
     * 
     * @param client provides access to the elasticsearch functions.
     * @param context Modeshape execution context.
     * @param defn index definition.
     * @param workspace workspace name where this index will be created.
     */
    public EsIndex(EsClient client, ExecutionContext context, IndexDefinition defn, String workspace) {
        this.client = client;
        this.name = defn.getName();
        this.workspace = workspace;
        this.columns = new EsIndexColumns(context, defn);
        this.operations = new Operations(context.getValueFactories(), columns);
        this.createIndex();
    }

    /**
     * Creates new index.
     * 
     * @param client provides access to the elasticsearch functions.
     * @param columns columns definition.
     * @param context Modeshape execution context.
     * @param name the name of the index
     * @param workspace workspace name where this index will be created.
     */
    protected EsIndex(EsClient client, EsIndexColumns columns, ExecutionContext context, String name, String workspace) {
        this.client = client;
        this.name = name;
        this.workspace = workspace;
        this.columns = columns;
        this.operations = new Operations(context.getValueFactories(), columns);
        this.createIndex();
    }

    /**
     * Generates local index name.
     * 
     * @return 
     */
    private String name() {
        return name.toLowerCase() + "-" + workspace;
    }

    /**
     * Executes create index action.
     */
    private void createIndex() {
        try {
            client.createIndex(name(), workspace, columns.mappings(workspace));
            client.flush(name());
        } catch (IOException e) {
            throw new EsIndexException(e);
        }
    }

    @Override
    public void add(String nodeKey, String propertyName, Object value) {
        CheckArg.isNotNull(nodeKey, "nodeKey");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(value, "value");

        EsIndexColumn column = columns.column(propertyName);
        assert column != null : "Unexpected column for the index " + name();

        try {
            EsRequest doc = findOrCreateDoc(nodeKey);
            putValue(doc, column, value);
            client.storeDocument(name(), workspace, nodeKey, doc);
        } catch (IOException e) {
            throw new EsIndexException(e);
        }
    }

    @Override
    public void add(String nodeKey, String propertyName, Object[] values) {
        CheckArg.isNotNull(nodeKey, "nodeKey");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(values, "values");

        EsIndexColumn column = columns.column(propertyName);
        assert column != null : "Unexpected column for the index " + name();

        try {
            EsRequest doc = findOrCreateDoc(nodeKey);
            putValues(doc, column, values);
            client.storeDocument(name(), workspace, nodeKey, doc);
        } catch (IOException e) {
            throw new EsIndexException(e);
        }
    }

    @Override
    public void remove(String nodeKey) {
        CheckArg.isNotNull(nodeKey, "nodeKey");
        try {
            client.deleteDocument(name(), workspace, nodeKey);
        } catch (IOException e) {
            throw new EsIndexException(e);
        }
    }

    @Override
    public void remove(String nodeKey, String propertyName, Object value) {
        CheckArg.isNotNull(nodeKey, "nodeKey");
        CheckArg.isNotNull(propertyName, "propertyName");

        try {
            EsRequest doc = find(nodeKey);
            if (doc == null) {
                return;
            }
            doc.remove(propertyName);            
            client.storeDocument(name(), workspace, nodeKey, doc);
        } catch (IOException e) {
            throw new EsIndexException(e);
        }
    }

    @Override
    public void remove(String nodeKey, String propertyName, Object[] values) {
        CheckArg.isNotNull(nodeKey, "nodeKey");
        CheckArg.isNotNull(propertyName, "propertyName");

        try {
            EsRequest doc = find(nodeKey);
            doc.remove(propertyName);
            client.storeDocument(name(), workspace, nodeKey, doc);
        } catch (Exception e) {
            throw new EsIndexException(e);
        }
    }

    /**
     * Searches indexed node's properties by node key.
     * 
     * @param nodeKey node key being indexed.
     * @return list of stored properties as json document.
     * @throws IOException 
     */
    private EsRequest find(String nodeKey) throws IOException {
        return client.getDocument(name(), workspace, nodeKey);
    }

    /**
     * Searches indexed node's properties by node key or creates new empty list.
     * 
     * @param nodeKey node key being indexed.
     * @return list of stored properties as json document or empty document 
     * if not found.
     * @throws IOException 
     */
    private EsRequest findOrCreateDoc(String nodeKey) throws IOException {
        EsRequest doc = client.getDocument(name(), workspace, nodeKey);
        return doc != null ? doc : new EsRequest();
    }

    /**
     * Appends specified value for the given column and related pseudo columns
     * into list of properties.
     * 
     * @param doc list of properties in json format
     * @param column colum definition
     * @param value column's value.
     */
    private void putValue(EsRequest doc, EsIndexColumn column, Object value) {
        Object columnValue = column.columnValue(value);
        String stringValue = column.stringValue(value);
        doc.put(column.getName(), columnValue);
        doc.put(column.getLowerCaseFieldName(), stringValue.toLowerCase());
        doc.put(column.getUpperCaseFieldName(), stringValue.toUpperCase());
        doc.put(column.getLengthFieldName(), stringValue.length());
    }

    /**
     * Appends specified values for the given column and related pseudo columns
     * into list of properties.
     * 
     * @param doc list of properties in json format
     * @param column colum definition
     * @param value column's value.
     */
    private void putValues(EsRequest doc, EsIndexColumn column, Object[] value) {
        Object[] columnValue = column.columnValues(value);
        int[] ln = new int[columnValue.length];
        String[] lc = new String[columnValue.length];
        String[] uc = new String[columnValue.length];

        for (int i = 0; i < columnValue.length; i++) {
            String stringValue = column.stringValue(columnValue[i]);
            lc[i] = stringValue.toLowerCase();
            uc[i] = stringValue.toUpperCase();
            ln[i] = stringValue.length();
        }

        doc.put(column.getName(), columnValue);
        doc.put(column.getLowerCaseFieldName(), lc);
        doc.put(column.getUpperCaseFieldName(), uc);
        doc.put(column.getLengthFieldName(), ln);
    }

    @Override
    public void commit() {
        try {
            client.refresh(name());
        } catch (IOException e) {
            throw new EsIndexException(e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Results filter(IndexConstraints constraints, long cardinalityEstimate) {
        EsRequest query = operations.createQuery(constraints.getConstraints(), constraints.getVariables());
        return new SearchResults(client, name(), workspace, query);
    }

    @Override
    public long estimateCardinality(List<Constraint> constraints, Map<String, Object> variables) {
        EsRequest query = operations.createQuery(constraints, variables);
        return new SearchResults(client, name(), workspace, query).getCardinality();
    }

    @Override
    public long estimateTotalCount() {
        try {
            return client.count(name(), workspace);
        } catch (IOException e) {
            throw new EsIndexException(e);
        }
    }

    @Override
    public boolean requiresReindexing() {
        return true;
    }

    @Override
    public void clearAllData() {
        try {
            client.deleteAll(name(), workspace);
        } catch (IOException e) {
            throw new EsIndexException(e);
        }
    }

    @Override
    public void shutdown(boolean destroyed) {
        if (destroyed) {
            try {
                client.deleteIndex(name());
            } catch (Exception e) {
                throw new EsIndexException(e);
            }
        }
    }
}
