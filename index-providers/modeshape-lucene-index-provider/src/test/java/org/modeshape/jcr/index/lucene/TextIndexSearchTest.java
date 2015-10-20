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

import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.STRING_PROP;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.value.PropertyType;

/**
 * Tests the search behavior of the {@link TextIndex}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class TextIndexSearchTest extends AbstractLuceneIndexSearchTest {
    
    private static final Map<String, PropertyType> ALLOWED_PROPERTIES = new HashMap<String, PropertyType>(){
        {
            put(STRING_PROP, PropertyType.STRING);
        }
    };    
    
    @Override
    protected LuceneIndex createIndex( String name ) {
        return new TextIndex(name + "-text", "default", config, ALLOWED_PROPERTIES, context);
    }

    @Test
    public void shouldSupportFTSWithSimpleExpressions() throws Exception {
        List<String> nodeKeys = indexNodes(STRING_PROP, "the quick", "brown fox", "green fox");
        
        Constraint fts = fullTextSearch(STRING_PROP, "quick");
        validateCardinality(fts, 1);
        validateFilterResults(fts, 1, true, nodeKeys.get(0));

        fts = fullTextSearch(STRING_PROP, "fox");
        validateCardinality(fts, 2);
        validateFilterResults(fts, 1, true, nodeKeys.get(1), nodeKeys.get(2));

        // any except
        fts = fullTextSearch(STRING_PROP, "-fox");
        validateCardinality(fts, 1);
        validateFilterResults(fts, 1, true, nodeKeys.get(0));

        fts = fullTextSearch(STRING_PROP, "fo*");
        validateCardinality(fts, 2);
        // we can't really expect a real score here because it's searching via regex 
        validateFilterResults(fts, 1, false, nodeKeys.get(1), nodeKeys.get(2));
    }

    @Test
    public void shouldSupportFTSWithSimpleExpressionsAndNoPropertyName() throws Exception {
        List<String> nodeKeys = indexNodes(STRING_PROP, "the quick", "brown fox", "green fox");

        Constraint fts = fullTextSearch(null, "quick");
        validateCardinality(fts, 1);
        validateFilterResults(fts, 1, true, nodeKeys.get(0));
    }
    
    @Test
    public void shouldSupportFTSWithConjunctions() throws Exception {
        List<String> nodeKeys = indexNodes(STRING_PROP, "the quick brown fox", "jumps over");
        
        Constraint fts = fullTextSearch(STRING_PROP, "quick brown");
        validateCardinality(fts, 1);
        validateFilterResults(fts, 1, true, nodeKeys.get(0));

        fts = fullTextSearch(STRING_PROP, "jumps o*");
        validateCardinality(fts, 1);
        validateFilterResults(fts, 1, false, nodeKeys.get(1));

        fts = fullTextSearch(STRING_PROP, "-jumps brown");
        validateCardinality(fts, 1);
        validateFilterResults(fts, 1, true, nodeKeys.get(0));

        fts = fullTextSearch(STRING_PROP, "-jumps -brown");
        validateCardinality(fts, 0);
    } 
    
    @Test
    public void shouldSupportFTSWithDisjunctions() throws Exception {
        List<String> nodeKeys = indexNodes(STRING_PROP, "the quick brown fox", "jumps over");
        
        Constraint fts = fullTextSearch(STRING_PROP, "quick OR over");
        validateCardinality(fts, 2);
        validateFilterResults(fts, 1, true, nodeKeys.toArray(new String[nodeKeys.size()]));

        fts = fullTextSearch(STRING_PROP, "jumps OR o*");
        validateCardinality(fts, 1);
        validateFilterResults(fts, 1, false, nodeKeys.get(1));

        fts = fullTextSearch(STRING_PROP, "-jumps OR brown");
        validateCardinality(fts, 1);
        validateFilterResults(fts, 1, true, nodeKeys.get(0));

        fts = fullTextSearch(STRING_PROP, "-jumps OR -brown");
        validateCardinality(fts, 2);
        validateFilterResults(fts, 2, true, nodeKeys.toArray(new String[nodeKeys.size()]));
    }

    @Test
    public void shouldSupportFTSWithDisjunctionsAndConjunctions() throws Exception {
        List<String> nodeKeys = indexNodes(STRING_PROP, "the quick brown fox", "jumps over");

        // MUST NOT 'over' AND MUST 'jumps'....
        Constraint fts = fullTextSearch(STRING_PROP, "-quick OR -over jumps");
        validateCardinality(fts, 0);

        // (SHOULD 'quick') OR (MUST NOT 'fox' AND MUST 'jumps')  
        fts = fullTextSearch(STRING_PROP, "quick OR -fox jumps");
        validateCardinality(fts, 2);
        validateFilterResults(fts, 1, true, nodeKeys.toArray(new String[nodeKeys.size()]));
    }
}
