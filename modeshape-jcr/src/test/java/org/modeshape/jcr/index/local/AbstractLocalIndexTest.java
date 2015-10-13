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

package org.modeshape.jcr.index.local;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.JoinCondition;
import org.junit.Before;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.index.local.MapDB.Serializers;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.StaticOperand;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.ResultWriter;
import org.modeshape.jcr.spi.index.provider.Filter;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

public abstract class AbstractLocalIndexTest {

    protected Serializers serializers;
    protected ExecutionContext context;
    protected DB db;
    protected String propertyName = "indexedProperty";

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        db = DBMaker.newMemoryDB().make();
        serializers = MapDB.serializers(context.getValueFactories());
    }

    protected void loadLongIndex( LocalUniqueIndex<Long> index,
                                  int numValues ) {
        for (int i = 1; i <= numValues; ++i) {
            index.add(key(i), "test", (long)(i * 10));
        }
    }

    protected void loadStringIndex( LocalUniqueIndex<String> index,
                                    int numValues ) {
        for (int i = 1; i <= numValues; ++i) {
            index.add(key(i), "test", "" + (i * 10));
        }
    }

    protected void loadLongIndexWithNoDuplicates( LocalDuplicateIndex<Long> index,
                                                  int numValues ) {
        for (int i = 1; i <= numValues; ++i) {
            index.add(key(i), "test", (long)(i * 10));
        }
    }

    protected void loadStringIndexWithNoDuplicates( LocalDuplicateIndex<String> index,
                                                    int numValues ) {
        for (int i = 1; i <= numValues; ++i) {
            index.add(key(i), "test", "" + (i * 10));
        }
    }

    @SuppressWarnings( "unchecked" )
    protected <T> LocalUniqueIndex<T> uniqueValueIndex( Class<T> valueType ) {
        PropertyType type = PropertyType.discoverType(valueType);
        ValueFactory<T> valueFactory = (ValueFactory<T>)context.getValueFactories().getValueFactory(type);
        Converter<T> converter = IndexValues.converter(valueFactory);
        Serializer<T> serializer = (Serializer<T>)serializers.serializerFor(type.getValueClass());
        BTreeKeySerializer<T> keySerializer = (BTreeKeySerializer<T>)serializers.bTreeKeySerializerFor(type.getValueClass(),
                                                                                                       type.getComparator(),
                                                                                                       false);
        return new LocalUniqueIndex<T>("myIndex", "myWorkspace", db, converter, keySerializer, serializer);
    }

    @SuppressWarnings( "unchecked" )
    protected <T> LocalDuplicateIndex<T> duplicateValueIndex( Class<T> valueType ) {
        PropertyType type = PropertyType.discoverType(valueType);
        Comparator<T> comparator = (Comparator<T>)type.getComparator();
        ValueFactory<T> valueFactory = (ValueFactory<T>)context.getValueFactories().getValueFactory(type);
        Converter<T> converter = IndexValues.converter(valueFactory);
        Serializer<T> serializer = (Serializer<T>)serializers.serializerFor(type.getValueClass());
        return new LocalDuplicateIndex<T>("myIndex", "myWorkspace", db, converter, serializer, comparator);
    }

    public <T> void assertNoMatch( LocalUniqueIndex<T> index,
                                   Operator op,
                                   T value ) {
        assertMatch(index, op, value, new String[] {});
    }

    public <T> void assertNoMatch( LocalDuplicateIndex<T> index,
                                   Operator op,
                                   T value ) {
        assertMatch(index, op, value, new String[] {});
    }

    public <T> void assertMatch( LocalUniqueIndex<T> index,
                                 Operator op,
                                 T value,
                                 String... keys ) {
        assertMatch(index, op, value, keyList(keys));
    }

    public <T> void assertMatch( LocalUniqueIndex<T> index,
                                 Operator op,
                                 T value,
                                 int... keys ) {
        assertMatch(index, op, value, keyList(keys));
    }

    public <T> void assertMatch( LocalUniqueIndex<T> index,
                                 Operator op,
                                 T value,
                                 LinkedList<String> expectedValues ) {
        Filter.Results results = index.filter(constraints(propertyName, op, value));
        ResultWriter writer = verify(expectedValues);
        for (;;) {
            if (!results.getNextBatch(writer, Integer.MAX_VALUE)) break;
        }
        assertTrue("Not all expected values were found in results: " + expectedValues, expectedValues.isEmpty());
    }

    public <T> void assertMatch( LocalDuplicateIndex<T> index,
                                 Operator op,
                                 T value,
                                 String... keys ) {
        assertMatch(index, op, value, keyList(keys));
    }

    public <T> void assertMatch( LocalDuplicateIndex<T> index,
                                 Operator op,
                                 T value,
                                 int... keys ) {
        assertMatch(index, op, value, keyList(keys));
    }

    public <T> void assertMatch( LocalDuplicateIndex<T> index,
                                 Operator op,
                                 T value,
                                 LinkedList<String> expectedValues ) {
        Filter.Results results = index.filter(constraints(propertyName, op, value));
        ResultWriter writer = verify(expectedValues);
        for (;;) {
            if (!results.getNextBatch(writer, Integer.MAX_VALUE)) break;
        }
        assertTrue("Not all expected values were found in results: " + expectedValues, expectedValues.isEmpty());
    }

    protected static SelectorName selector() {
        return selector("selectorA");
    }

    protected static SelectorName selector( String name ) {
        return new SelectorName(name);
    }

    protected <T> IndexConstraints constraints( String propertyName,
                                                Operator op,
                                                Object literalValue ) {
        DynamicOperand dynOp = new PropertyValue(selector(), propertyName);
        StaticOperand statOp = new Literal(literalValue);
        return constraints(new Comparison(dynOp, op, statOp));
    }

    protected IndexConstraints constraints( final Constraint comparison ) {
        return new IndexConstraints() {
            @Override
            public Collection<Constraint> getConstraints() {
                return Collections.singletonList(comparison);
            }

            @Override
            public Map<String, Object> getParameters() {
                return Collections.emptyMap();
            }

            @Override
            public ValueFactories getValueFactories() {
                return context.getValueFactories();
            }

            @Override
            public Map<String, Object> getVariables() {
                return Collections.emptyMap();
            }

            @Override
            public boolean hasConstraints() {
                return true;
            }

            @Override
            public Collection<JoinCondition> getJoinConditions() {
                return Collections.emptyList();
            }
        };
    }

    protected LinkedList<String> keyList( int... keys ) {
        LinkedList<String> expected = new LinkedList<String>();
        for (int i = 0; i != keys.length; ++i) {
            expected.add(key(keys[i]));
        }
        return expected;
    }

    protected LinkedList<String> keyList( String... keys ) {
        LinkedList<String> expected = new LinkedList<String>();
        for (int i = 0; i != keys.length; ++i) {
            expected.add(keys[i]);
        }
        return expected;
    }

    protected ResultWriter verify( final LinkedList<String> keys ) {
        return new ResultWriter() {
            @Override
            public void add( Iterable<NodeKey> nodeKeys,
                             float score ) {
                for (NodeKey actual : nodeKeys) {
                    assertTrue("Got actual result '" + actual + "' but expected nothing", !keys.isEmpty());
                    assertThat(actual, is(nodeKey(keys.removeFirst())));
                }
            }

            @Override
            public void add( Iterator<NodeKey> nodeKeys,
                             float score ) {
                while (nodeKeys.hasNext()) {
                    NodeKey actual = nodeKeys.next();
                    assertTrue("Got actual result '" + actual + "' but expected nothing", !keys.isEmpty());
                    assertThat(actual, is(nodeKey(keys.removeFirst())));
                }
            }

            @Override
            public void add( NodeKey nodeKey,
                             float score ) {
                assertTrue("Got actual result '" + nodeKey + "' but expected nothing", !keys.isEmpty());
                assertThat(nodeKey, is(nodeKey(keys.removeFirst())));
            }
        };
    }

    private static final String NODE_KEY_PREFIX = "12345671234567-";

    protected static String key( int value ) {
        return NODE_KEY_PREFIX + value;
    }

    protected static String key( String value ) {
        return NODE_KEY_PREFIX + value;
    }

    protected static NodeKey nodeKey( String value ) {
        return new NodeKey(value);
    }
}
