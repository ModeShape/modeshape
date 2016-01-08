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

import org.modeshape.jcr.index.elasticsearch.client.EsRequest;

/**
 *
 * @author kulikov
 */
public class RangeQuery extends Query {
    private String field;
    private Object lowBound, upperBound;
    private boolean includeLowBound, includeUpperBound;
    
    public RangeQuery(String field) {
        this.field = field;
    }
    
    public RangeQuery from(Object value) {
        this.lowBound = value;
        return this;
    }
    
    public RangeQuery to(Object value) {
        this.upperBound = value;
        return this;
    }

    public RangeQuery gt(Object value) {
        this.lowBound = value;
        this.includeLowBound = false;
        return this;
    }

    public RangeQuery gte(Object value) {
        this.lowBound = value;
        this.includeLowBound = true;
        return this;
    }

    public RangeQuery lt(Object value) {
        this.upperBound = value;
        this.includeUpperBound = false;
        return this;
    }

    public RangeQuery lte(Object value) {
        this.upperBound = value;
        this.includeUpperBound = true;
        return this;
    }
    
    public RangeQuery includeLower(boolean value) {
        this.includeLowBound = value;
        return this;
    }

    public RangeQuery includeUpper(boolean value) {
        this.includeUpperBound = value;
        return this;
    }

    @Override
    public EsRequest build() {
        EsRequest doc = new EsRequest();
            EsRequest range = new EsRequest();
                EsRequest property = new EsRequest();
                    if (lowBound != null) {
                        property.put(includeLowBound ? "gte" : "gt", lowBound);
                    }
                    if (upperBound != null) {
                        property.put(includeUpperBound ? "lte" : "lt", upperBound);
                    }
            range.put(field, property);
        doc.put("range", range);
        return doc;
    }
    
}
