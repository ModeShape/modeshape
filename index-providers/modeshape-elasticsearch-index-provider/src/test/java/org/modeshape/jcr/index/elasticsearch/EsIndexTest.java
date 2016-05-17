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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.jcr.PropertyType;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.JoinCondition;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.elasticsearch.client.EsClient;
import org.modeshape.jcr.query.model.And;
import org.modeshape.jcr.query.model.Between;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.Length;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.LowerCase;
import org.modeshape.jcr.query.model.Not;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.query.model.PropertyExistence;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.SetCriteria;
import org.modeshape.jcr.query.model.UpperCase;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.provider.Filter;
import org.modeshape.jcr.spi.index.provider.Filter.Results;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.ValueFactories;

/**
 *
 * @author kulikov
 */
public class EsIndexTest {

    private final static ExecutionContext context = new ExecutionContext();
    private final static EsIndexColumn def1 = new EsIndexColumn(context, "field1", PropertyType.STRING);
    private final static EsIndexColumn def2 = new EsIndexColumn(context, "field2", PropertyType.DECIMAL);
    private final static EsIndexColumn def3 = new EsIndexColumn(context, "field3", PropertyType.STRING);
    private final static EsIndexColumn def4 = new EsIndexColumn(context, "mixinTypes", PropertyType.NAME);
    private final static EsIndexColumn def5 = new EsIndexColumn(context, "myfield", PropertyType.STRING);
    private static Node esNode;
    private static EsClient client;
    private static EsIndex index;

    public EsIndexTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        FileUtil.delete("target/data");

        EsIndexColumns cols = new EsIndexColumns(def1, def2, def3, def4, def5);
        esNode = NodeBuilder.nodeBuilder().local(false)
                .settings(Settings.settingsBuilder().put("path.home", "target/data"))
                .node();
        client = new EsClient("localhost", 9200);
        index = new EsIndex(client, cols, context, "test", "workspace");

        initialIndex();
        Thread.currentThread().sleep(1000);
    }

    @AfterClass
    public static void tearDownClass() {
        index.shutdown(true);
        esNode.close();
        FileUtil.delete("target/data");
    }

    private static void initialIndex() {
        index.add(key("key1"), def1.getName(), "node1 - value1");
        index.add(key("key1"), def2.getName(), 1);
        index.add(key("key1"), def3.getName(), new String[]{"a", "b", "c"});

        index.add(key("key2"), def1.getName(), "node2 - value2");
        index.add(key("key2"), def2.getName(), 2);
        index.add(key("key2"), def3.getName(), new String[]{"d", "e", "f"});

        index.add(key("key3"), def1.getName(), "node3 - value3");
        index.add(key("key3"), def2.getName(), 3);
        index.add(key("key3"), def3.getName(), new String[]{"h", "i", "g"});

        index.add(key("key4"), def1.getName(), "node4 - value4");
        index.add(key("key4"), def2.getName(), 4);
        index.add(key("key4"), def3.getName(), new String[]{"j", "k", "l"});

        index.add(key("key5"), def1.getName(), "node5 - value5");
        index.add(key("key5"), def2.getName(), 5);
        index.add(key("key5"), def3.getName(), new String[]{"m", "n", "o"});

        index.add(key("key6"), def1.getName(), "the quick Brown fox jumps over to the dog in at the gate");
        index.add(key("key6"), def2.getName(), 55);
        index.add(key("key6"), def3.getName(), new String[]{"m", "n", "o"});

        index.add(key("key7"), def5.getName(), "asd-sdf-dfg");
        index.commit();
    }

    /**
     * Test of reindexNode method, of class EsIndexImpl.
     */
    @Test
    public void shouldSupportBetweenConstraint() {
        //range including bounds
        Between c1 = new Between(propertyValue(def2), new Literal(2), new Literal(4));
        validate(c1, "key2", "key3", "key4");

        //range excluding bounds
        Between c2 = new Between(propertyValue(def2), new Literal(2), new Literal(4), false, false);
        validate(c2, "key3");
    }

    @Test
    public void shouldSupportComparisonConstraint() {
        Comparison gt = new Comparison(propertyValue(def2), Operator.GREATER_THAN, new Literal(2));
        validate(gt, "key3", "key4", "key5", "key6");

        Comparison gte = new Comparison(propertyValue(def2), Operator.GREATER_THAN_OR_EQUAL_TO, new Literal(2));
        validate(gte, "key2", "key3", "key4", "key5", "key6");

        Comparison eq = new Comparison(propertyValue(def2), Operator.EQUAL_TO, new Literal(3));
        validate(eq, "key3");

        Comparison lt = new Comparison(propertyValue(def2), Operator.LESS_THAN, new Literal(2));
        validate(lt, "key1");

        Comparison lte = new Comparison(propertyValue(def2), Operator.LESS_THAN_OR_EQUAL_TO, new Literal(2));
        validate(lte, "key1", "key2");

        Comparison ne = new Comparison(propertyValue(def2), Operator.NOT_EQUAL_TO, new Literal(2));
        validate(ne, "key1", "key3", "key4", "key5", "key6", "key7");

        Comparison like = new Comparison(propertyValue(def2), Operator.LIKE, new Literal("value3"));
        validate(like, "key3");
    }

    @Test
    public void shouldSupportSetConstraint() {
        SetCriteria sc = new SetCriteria(propertyValue(def3), new Literal("a"));
        validate(sc, "key1");

        SetCriteria sc1 = new SetCriteria(propertyValue(def2), new Literal(3), new Literal(4));
        validate(sc1, "key3", "key4");
    }

    @Test
    public void shouldSupportFullTextSearchConstraint() {
        validate(fullTextSearch("node1 - value1"), "key1");
        validate(fullTextSearch("the quick Brown fox jumps over to the dog in at the gate"), "key6");
        validate(fullTextSearch("the quick Dog"), "key6");
    }

    @Test
    public void shouldSupportPropertyExistanceConstraint() {
        PropertyExistence pe = new PropertyExistence(new SelectorName("test"), def2.getName());
        validate(pe, "key1", "key2", "key3", "key4", "key5", "key6");
    }

    @Test
    public void testOrConstraint() {
        Comparison eq1 = new Comparison(propertyValue(def2), Operator.EQUAL_TO, new Literal(3));
        Comparison eq2 = new Comparison(propertyValue(def2), Operator.EQUAL_TO, new Literal(2));
        validate(new Or(eq1, eq2), "key2", "key3");
    }

    @Test
    public void testAndConstraint() {
        Comparison gt = new Comparison(propertyValue(def2), Operator.GREATER_THAN, new Literal(2));
        Comparison lt = new Comparison(propertyValue(def2), Operator.LESS_THAN, new Literal(4));
        validate(new And(gt, lt), "key3");
    }

    @Test
    public void testNotConstraint() {
        Comparison ne = new Comparison(propertyValue(def2), Operator.NOT_EQUAL_TO, new Literal(3));
        validate(new Not(ne), "key3");
    }

    @Test
    public void shouldSupportMultipleValues() {
        SetCriteria sc = new SetCriteria(propertyValue(def3),
                new Literal[]{new Literal("a"), new Literal("e")});
        validate(sc, "key1", "key2");
    }

    @Test
    public void shouldSupportLowerCase() {
        LowerCase lowerCase = new LowerCase(propertyValue(def1));
        Comparison eq = new Comparison(lowerCase, Operator.EQUAL_TO, new Literal("value3"));
        validate(eq, "key3");
    }

    @Test
    public void shouldSupportUpperCase() {
        UpperCase upperCase = new UpperCase(propertyValue(def1));
        Comparison eq = new Comparison(upperCase, Operator.EQUAL_TO, new Literal("VALUE3"));
        validate(eq, "key3");
    }

    @Test
    public void shouldSupportLength() {
        Length len = new Length(propertyValue(def2));
        Comparison eq = new Comparison(len, Operator.EQUAL_TO, new Literal(1));
        validate(eq, "key1", "key2", "key3", "key4", "key5");
    }

    @Test
    public void shouldNotTokenizeOnIntrawordDelimeter() {
        SetCriteria sc = new SetCriteria(propertyValue(def5), new Literal("asd-sdf-dfg"));
        validate(sc, "key7");
    }

    @Test
    public void shouldConvertValuesToCoreTypes() {
        ExecutionContext ctx = new ExecutionContext();

        Name n1 = ctx.getValueFactories().getNameFactory().create("mix:title");
        Name n2 = ctx.getValueFactories().getNameFactory().create("mix:titl");

        index.add(key("key8"), "mixinTypes", new Object[]{n1});
        index.commit();
        
        SetCriteria sc = new SetCriteria(propertyValue(def4), new Literal(n1), new Literal(n2));
        validate(sc, "key8");
    }

    private void validate(Constraint constraint, String... keys) {
        Results results = index.filter(constraints(constraint), keys.length);
        Filter.ResultBatch batch = results.getNextBatch(100);
        assertTrue(checkResults(batch.keys(), keys));
    }

    private PropertyValue propertyValue(EsIndexColumn column) {
        return new PropertyValue(new SelectorName("test"), column.getName());
    }

    private FullTextSearch fullTextSearch(String query) {
        return new FullTextSearch(new SelectorName("test"), query);
    }

    protected IndexConstraints constraints(final Constraint constraint) {
        return new IndexConstraints() {
            @Override
            public Collection<Constraint> getConstraints() {
                return Collections.singletonList(constraint);
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

    private boolean checkResults(Iterable<NodeKey> res, String... expected) {
        int count = 0;
        for (NodeKey k : res) {
            ++count;
            for (int i = 0; i < expected.length; i++) {
                String s = key(expected[i]);
                if (s.equals(k.toString())) {
                    return true;
                }
            }
        }
        assertEquals(expected.length, count);
        return false;
    }
    
    private static final String NODE_KEY_PREFIX = "12345671234567-";

    protected static String key(String value) {
        return NODE_KEY_PREFIX + value;
    }
}