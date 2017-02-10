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
package org.modeshape.jcr.index.lucene.query;

import org.apache.lucene.search.Query;
import org.modeshape.common.annotation.Immutable;

/**
 * Lucene query which returns the documents that have any value for a given field.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
@Immutable
public class FieldExistsQuery extends ConstantScoreWeightQuery {
    
    protected FieldExistsQuery( String field ) {
        super(field);
    }

    @Override
    public String toString( String field ) {
        return "(FIELD " + field + " EXISTS)";
    }

    @Override
    protected boolean accepts(String value) {
        // always valid, since the base class will only call this for a document which actually has the fields...
        return true;
    }

    @Override
    public Query clone() {
        return new FieldExistsQuery(field());
    }
}
