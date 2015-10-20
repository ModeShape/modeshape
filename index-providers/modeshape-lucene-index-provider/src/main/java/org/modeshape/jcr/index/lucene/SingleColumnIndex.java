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

import java.io.IOException;
import java.util.Map;
import org.apache.lucene.document.Document;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.index.lucene.query.LuceneQueryFactory;
import org.modeshape.jcr.value.PropertyType;

/**
 * Lucene index which only supports a single column. This should perform better in most cases than {@link MultiColumnIndex}
 * because there is no real document updating. Each document is removed and then added with new fields.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
@Immutable
@ThreadSafe
class SingleColumnIndex extends LuceneIndex {
    
    protected SingleColumnIndex( String name, 
                                 String workspaceName, 
                                 LuceneConfig config,
                                 Map<String, PropertyType> propertyTypesByName,
                                 ExecutionContext context ) {
        super(name, workspaceName, config, propertyTypesByName, context);
    }

    @Override
    public void add( String nodeKey, String propertyName, Object[] values ) {
        CheckArg.isNotNull(nodeKey, "nodeKey");
        CheckArg.isNotNull(propertyName, "propertyName");
        CheckArg.isNotNull(values, "values");

        try {
            Document document = new Document();
            addProperty(nodeKey, document, propertyName, values);
            // since multiple columns are not possible we'll just update the entire document
            // which means removing the old one and creating a new one (which is what Lucene does anyway)
            logger.debug("Adding the document '{0}' in the Lucene Index '{1}' with the property '{2}' and values '{3}",
                         nodeKey, name, propertyName, values);
            writer.updateDocument(FieldUtil.idTerm(nodeKey), document);
        } catch (IOException e) {
            throw new LuceneIndexException(e);
        }
    }

    @Override
    protected void remove( String nodeKey, String propertyName ) {
        // simply remove the document with this key, since if this method was called, `propertyName` is already tracked by this index
        // and there's can't be more than 1 column
        try {
            writer.deleteDocuments(FieldUtil.idTerm(nodeKey));
        } catch (IOException e) {
            throw new LuceneIndexException(e);
        }
    }

    @Override
    protected LuceneQueryFactory queryFactory( Map<String, Object> variables ) {
        return LuceneQueryFactory.forSingleColumnIndex(context.getValueFactories(), variables, propertyTypesByName);
    }
}
