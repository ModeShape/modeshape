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
package org.modeshape.jcr.index.lucene;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.index.lucene.query.LuceneQueryFactory;
import org.modeshape.jcr.value.PropertyType;

/**
 * Lucene index which stores strings or binary values that can then be used for FTS. 
 * <p>
 * This type of index is only used for full text searching and will not store any other information.
 * </p> 
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
@Immutable
@ThreadSafe
class TextIndex extends SingleColumnIndex {
    
    protected TextIndex( String name, 
                         String workspaceName, 
                         LuceneConfig config,
                         Map<String, PropertyType> propertyTypesByName,
                         ExecutionContext context ) {
        super(name, workspaceName, config, propertyTypesByName, context);
    }

    @Override
    protected LuceneQueryFactory queryFactory( Map<String, Object> variables ) {
        return LuceneQueryFactory.forTextIndex(context.getValueFactories(), variables, propertyTypesByName, config);
    }

    @Override
    protected void addStringField( String propertyName, String value, List<Field> fields ) {
        // never store the actual field
        fields.add(new TextField(propertyName, value, Field.Store.NO));
    }

    @Override
    protected void addBooleanField( String propertyName, Boolean value, List<Field> fields ) {
        // not supported 
    }

    @Override
    protected void addDateField( String propertyName, DateTime value, List<Field> fields ) {
        // not supported
    }

    @Override
    protected void addBinaryField( String propertyName, Object value, List<Field> fields ) {
        String valueString = value instanceof String ? (String)value : stringFactory.create(value); 
        fields.add(new TextField(propertyName, valueString, Field.Store.NO));
    }

    @Override
    protected void addDecimalField( String propertyName, BigDecimal value, List<Field> fields ) {
        // not supported
    }

    @Override
    protected void addDoubleField( String propertyName, Double value, List<Field> fields ) {
        // not supported
    }

    @Override
    protected void addLongField( String propertyName, Long value, List<Field> fields ) {
        // not supported
    }
}
