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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.modeshape.jcr.api.query.qom.Operator.EQUAL_TO;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.jcr.query.qom.JoinCondition;
import org.junit.After;
import org.junit.Before;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.model.And;
import org.modeshape.jcr.query.model.Between;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.Length;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.LowerCase;
import org.modeshape.jcr.query.model.Not;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.query.model.PropertyExistence;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.ReferenceValue;
import org.modeshape.jcr.query.model.Relike;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.SetCriteria;
import org.modeshape.jcr.query.model.StaticOperand;
import org.modeshape.jcr.query.model.UpperCase;
import org.modeshape.jcr.spi.index.provider.Filter;
import org.modeshape.jcr.value.ValueFactories;

/**
 * Base class for testing the search behavior on the various types of Lucene indexes.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class AbstractLuceneIndexSearchTest {

    protected static final SelectorName SELECTOR = new SelectorName("node");
    
    protected ExecutionContext context;
    protected LuceneConfig config;
    protected LuceneIndex index;
    protected ValueFactories valueFactories;

    protected abstract LuceneIndex createIndex( String name );

    @Before
    public void setUp() throws Exception {
        String dir = "target/lucene-index-search-test";
        FileUtil.delete(dir);
        config = LuceneConfig.onDisk(dir);
        context = new ExecutionContext();
        valueFactories = context.getValueFactories();
        index = createIndex("default");
    }
    
    @After
    public void tearDown() throws Exception {
        index.shutdown(false);
    }

    protected void assertLengthComparisonConstraint( String propertyName, String expectedNodeKey, int expectedLength ) {
        Constraint constraint = length(propertyName, EQUAL_TO, expectedLength);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, expectedNodeKey);
    }

    protected void validateCardinality(Constraint constraint, long expected) {
        long actualValue = index.estimateCardinality(Collections.singletonList(constraint), Collections.emptyMap());
        assertEquals("Incorrect returned cardinality:", expected, actualValue);
    }

    protected void validateFilterResults(Constraint constraint, int batchSize, boolean expectRealScore, String...expectedKeys) {
        long cardinalityEstimate = expectedKeys != null ? expectedKeys.length : 0;
        Filter.Results results = index.filter(new TestIndexConstraints(constraint), cardinalityEstimate);
        Map<String, Float> actualResults = new LinkedHashMap<>();
        Filter.ResultBatch nextBatch;
        while ((nextBatch = results.getNextBatch(batchSize)).size() > 0) {
            Iterator<NodeKey> keyIterator = nextBatch.keys().iterator();
            Iterator<Float> scoresIterator = nextBatch.scores().iterator();
            while (keyIterator.hasNext() && scoresIterator.hasNext()) {
                actualResults.put(keyIterator.next().toString(), scoresIterator.next());
            }
        }
        assertEquals("Incorrect number of results:", expectedKeys.length, actualResults.size());
        for (String expectedKey : actualResults.keySet()) {
            Float score = actualResults.get(expectedKey);
            if (score == null) {
                fail("Expected key: " + expectedKey + " not found among results: " + actualResults.keySet());
            }
            if (expectRealScore && score == 1.0f) {
                fail("Expected a real score but found: " + score);
            }
        }
    }

    @SafeVarargs
    protected  final <T> void addValues( String nodeKey, String propertyName, T... values ) {
        index.add(nodeKey, propertyName, values);
    }
  
    private <T> String insertValue( String propertyName, T value ) {
        String nodeKey = UUID.randomUUID().toString();
        index.add(nodeKey, propertyName, value);
        return nodeKey;
    }

    @SafeVarargs
    protected final <T> List<String> indexNodes(String propertyName, T...values) {
        List<String> result = new ArrayList<>();
        for (T value : values) {
            result.add(insertValue(propertyName, value));
        }
        return result;
    }
    
    protected Constraint relike(Object value, String propertyName) {
        PropertyValue propValue = new PropertyValue(SELECTOR, propertyName);
        return new Relike(new Literal(value), propValue);
    }
    
    protected Constraint and(Constraint left, Constraint right) {
        return new And(left, right);
    }  
    
    protected Constraint or(Constraint left, Constraint right) {
        return new Or(left, right);
    }  
    
    protected Constraint not(Constraint constraint) {
        return new Not(constraint);
    }
    
    protected Constraint set(String propertyName, Object...values) {
        PropertyValue propValue = new PropertyValue(SELECTOR, propertyName);
        Collection<StaticOperand> operands = new ArrayList<>(values.length);
        for (Object value : values) {
            operands.add(new Literal(value));                
        }
        return new SetCriteria(propValue, operands);
    }
    
    protected Constraint between(String propertyName, Object lowerBound, boolean includeLowerBound, 
                                 Object upperBound, boolean includeUpperBound) {
        PropertyValue propValue = new PropertyValue(SELECTOR, propertyName);
        return new Between(propValue, new Literal(lowerBound), new Literal(upperBound), includeLowerBound, includeUpperBound);
    }
    
    protected Constraint fullTextSearch(String propertyName, String expression) {
        return new FullTextSearch(SELECTOR, propertyName, expression);
    }
    
    protected Comparison lowerCase( String propertyName, Operator operator, Object value ) {
        PropertyValue propValue = new PropertyValue(SELECTOR, propertyName);
        LowerCase lowerCase = new LowerCase(propValue);
        return new Comparison(lowerCase, operator, new Literal(value));
    } 
    
    protected Comparison upperCase( String propertyName, Operator operator, Object value ) {
        PropertyValue propValue = new PropertyValue(SELECTOR, propertyName);
        UpperCase upperCase = new UpperCase(propValue);
        return new Comparison(upperCase, operator, new Literal(value));
    }

    protected Comparison propertyValue( String propertyName, Operator operator, Object value ) {
        PropertyValue propValue = new PropertyValue(SELECTOR, propertyName);
        return new Comparison(propValue, operator, new Literal(value));
    }
    
    protected Comparison referenceValue( String propertyName, Operator operator, Object value, boolean includeWeak,
                                         boolean includeSimple ) {
        ReferenceValue referenceValue = new ReferenceValue(SELECTOR, propertyName, includeWeak, includeSimple);
        return new Comparison(referenceValue, operator, new Literal(value));
    }

    protected Comparison length( String propertyName, Operator operator, Object value ) {
        PropertyValue propValue = new PropertyValue(SELECTOR, propertyName);
        Length length = new Length(propValue);
        return new Comparison(length, operator, new Literal(value));
    }
    
    protected PropertyExistence propertyExistence(String propertyName) {
        return new PropertyExistence(SELECTOR, propertyName);
    }

    protected class TestIndexConstraints implements org.modeshape.jcr.spi.index.IndexConstraints {

        private final Constraint constraint;

        private TestIndexConstraints( Constraint constraint ) {
            this.constraint = constraint;
        }

        @Override
        public boolean hasConstraints() {
            return true;
        }

        @Override
        public Collection<javax.jcr.query.qom.Constraint> getConstraints() {
            return Collections.<javax.jcr.query.qom.Constraint>singleton(constraint);
        }

        @Override
        public Collection<JoinCondition> getJoinConditions() {
            return Collections.emptySet();
        }

        @Override
        public Map<String, Object> getVariables() {
            return Collections.emptyMap();
        }

        @Override
        public ValueFactories getValueFactories() {
            return context.getValueFactories();
        }

        @Override
        public Map<String, Object> getParameters() {
            return Collections.emptyMap();
        }
    }
}
