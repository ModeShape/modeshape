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

import static org.modeshape.jcr.api.query.qom.Operator.EQUAL_TO;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.DATE_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.LONG_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.REF_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.SIMPLE_REF_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.STRING_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.WEAK_REF_PROP;
import java.util.List;
import java.util.UUID;
import org.junit.Test;
import org.modeshape.jcr.query.model.Constraint;

/**
 * Tests the search behavior of the {@link MultiColumnIndex}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class MultiColumnIndexSearchTest extends SingleColumnIndexSearchTest {

    @Override
    protected LuceneIndex createIndex( String name ) {
        return new MultiColumnIndex(name + "-multi-valued", "default", config, PropertiesTestUtil.ALLOWED_PROPERTIES, context);
    }

    @Override
    @Test
    public void shouldSearchForReferenceValueInComparisonConstraint() throws Exception {
        String strongRef1 = UUID.randomUUID().toString();
        String strongRef2 = UUID.randomUUID().toString();
        List<String> nodesWithRefProp = indexNodes(REF_PROP, strongRef1, strongRef2);
        
        String weakRef1 = UUID.randomUUID().toString(); 
        String weakRef2 = UUID.randomUUID().toString();
        addValues(nodesWithRefProp.get(0), WEAK_REF_PROP, weakRef1);
        addValues(nodesWithRefProp.get(1), WEAK_REF_PROP, weakRef2);

        String simpleRef1 = UUID.randomUUID().toString(); 
        String simpleRef2 = UUID.randomUUID().toString();
        addValues(nodesWithRefProp.get(0), SIMPLE_REF_PROP, simpleRef1);
        addValues(nodesWithRefProp.get(1), SIMPLE_REF_PROP, simpleRef2);
        
        Constraint constraint = referenceValue(REF_PROP, EQUAL_TO, strongRef1, true, true);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithRefProp.get(0));

        constraint = referenceValue(null, EQUAL_TO, weakRef1, true, true);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithRefProp.get(0));

        constraint = referenceValue(null, EQUAL_TO, weakRef1, false, true);
        validateCardinality(constraint, 0);

        constraint = referenceValue(null, EQUAL_TO, simpleRef2, true, true);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithRefProp.get(1));

        constraint = referenceValue(null, EQUAL_TO, simpleRef2, false, false);
        validateCardinality(constraint, 0);
    }

    @Override
    @Test
    public void shouldSearchForPropertyExistence() throws Exception {
        List<String> nodes = indexNodes(LONG_PROP, 1l, 3l);
        String node1 = nodes.get(0);
        addValues(node1, STRING_PROP, "a");
       
        Constraint existsLongProp = propertyExistence(LONG_PROP);
        validateCardinality(existsLongProp, 2);
        validateFilterResults(existsLongProp, 2, false, nodes.toArray(new String[nodes.size()]));

        Constraint existsStringProp = propertyExistence(STRING_PROP);
        validateCardinality(existsStringProp, 1);
        validateFilterResults(existsStringProp, 1, false, node1);

        Constraint nonExistentProp = propertyExistence(DATE_PROP);
        validateCardinality(nonExistentProp, 0);

        index.remove(node1, LONG_PROP);
        validateCardinality(existsLongProp, 1);
        validateFilterResults(existsLongProp, 1, false, nodes.get(1));
    }
    
}
