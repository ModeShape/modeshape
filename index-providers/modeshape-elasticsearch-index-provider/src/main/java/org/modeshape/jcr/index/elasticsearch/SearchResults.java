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
import org.modeshape.schematic.document.Document;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.elasticsearch.client.EsClient;
import org.modeshape.jcr.index.elasticsearch.client.EsRequest;
import org.modeshape.jcr.index.elasticsearch.client.EsResponse;
import org.modeshape.jcr.spi.index.Index;
import org.modeshape.jcr.spi.index.ResultWriter;

/**
 *
 * @author kulikov
 */
public class SearchResults implements Index.Results {

    private final EsClient client;
    private final EsRequest query;
    private final String index, type;
    
    private int pos = 0;
    private int totalHits;
    
    /**
     * Creates new search result instance for the given search request.
     * 
     * @param searchBuilder search request as request builder.
     */
    public SearchResults(EsClient client, String index, String type, EsRequest query) {
        this.client = client;
        this.query = query;
        this.index = index;
        this.type = type;
    }

    @Override
    public boolean getNextBatch(ResultWriter writer, int batchSize) {
        query.put("from", pos);
        query.put("size", batchSize);
        
        try {
            EsResponse res = client.search(index, type, query);
            Document hits = (Document) res.get("hits");
            totalHits = hits.getInteger("total");
            List items = hits.getArray("hits");
            for (Object doc : items) {
                Document hit = (Document) doc;
                String nodeKey = hit.getString("_id");
                float score = (hit.getDouble("_score").floatValue());
                writer.add(new NodeKey(nodeKey), score);
                pos++;
            }
            return pos < totalHits;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void close() {
    }

    /**
     * Gets cardinality for this search request.
     * 
     * @return total hits matching search request.
     */
    public long getCardinality() {
        if (pos > 0) {
            return totalHits;
        }
        try {
            EsResponse res = client.search(index, type, query);
            Document hits = (Document) res.get("hits");
            totalHits = hits.getInteger("total");
            return totalHits;
        } catch (Exception e) {
            throw new EsIndexException(e);
        }
    }
}
