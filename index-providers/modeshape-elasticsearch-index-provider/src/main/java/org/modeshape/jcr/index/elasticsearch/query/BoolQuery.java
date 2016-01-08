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
package org.modeshape.jcr.index.elasticsearch.query;

import java.util.ArrayList;
import org.modeshape.jcr.index.elasticsearch.client.EsRequest;

/**
 *
 * @author kulikov
 */
public class BoolQuery extends Query {
    private final ArrayList<Query> must = new ArrayList<Query>();
    private final ArrayList<Query> should = new ArrayList<Query>();
    
    public BoolQuery() {
    }
    
    public BoolQuery must(Query q) {
        must.add(q);
        return this;
    }

    public BoolQuery should(Query q) {
        should.add(q);
        return this;
    }
    
    @Override
    public EsRequest build() {
        EsRequest query = new EsRequest();
        
        if (!must.isEmpty()) {
            query.put("must", documents(must));
        }

        if (!should.isEmpty()) {
            query.put("should", documents(should));
        }
        
        EsRequest body = new EsRequest();
        body.put("bool", query);
        return body;
    }
    
    private EsRequest[] documents(ArrayList<Query> list) {
        EsRequest[] res = new EsRequest[list.size()];
        int i = 0;
        for (Query query : list) {
            res[i++] = query.build();
        }
        return res;
    }
}
