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
import static org.modeshape.jcr.api.query.qom.Operator.GREATER_THAN;
import static org.modeshape.jcr.api.query.qom.Operator.GREATER_THAN_OR_EQUAL_TO;
import static org.modeshape.jcr.api.query.qom.Operator.LESS_THAN;
import static org.modeshape.jcr.api.query.qom.Operator.LESS_THAN_OR_EQUAL_TO;
import static org.modeshape.jcr.api.query.qom.Operator.LIKE;
import static org.modeshape.jcr.api.query.qom.Operator.NOT_EQUAL_TO;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.BINARY_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.BOOLEAN_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.DATE_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.DECIMAL_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.DOUBLE_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.LONG_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.NAME_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.PATH_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.REF_PROP;
import static org.modeshape.jcr.index.lucene.PropertiesTestUtil.STRING_PROP;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.ValueFormatException;
import org.modeshape.jcr.value.basic.ModeShapeDateTime;

/**
 * Tests the search behavior of the {@link SingleColumnIndex}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class SingleColumnIndexSearchTest extends AbstractLuceneIndexSearchTest {

    @Override
    protected LuceneIndex createIndex( String name ) {
        return new MultiColumnIndex(name + "-multi-valued", "default", config, PropertiesTestUtil.ALLOWED_PROPERTIES, context);
    }
    
    @Test
    public void shouldSearchForStringPropertyValueInComparisonConstraint() throws Exception {
        List<String> nodeKeys = indexNodes(STRING_PROP, "s1", "s2", "s1");

        // nodes where value = s1
        Constraint constraint = propertyValue(STRING_PROP, EQUAL_TO, "s1");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodeKeys.get(0), nodeKeys.get(2));

        // nodes where value = s2
        constraint = propertyValue(STRING_PROP, EQUAL_TO, "s2");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(1));

        //nodes where value = s3
        constraint = propertyValue(STRING_PROP, EQUAL_TO, "s3");
        validateCardinality(constraint, 0);
        validateFilterResults(constraint, 0, false);

        // nodes where value > s1
        constraint = propertyValue(STRING_PROP, GREATER_THAN, "s1");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(1));

        // nodes where value >= s1
        constraint = propertyValue(STRING_PROP, GREATER_THAN_OR_EQUAL_TO, "s1");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 3, false, nodeKeys.toArray(new String[3]));

        // nodes where value < s2
        constraint = propertyValue(STRING_PROP, LESS_THAN, "s2");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodeKeys.get(0), nodeKeys.get(2));

        // nodes where value <= s2
        constraint = propertyValue(STRING_PROP, LESS_THAN_OR_EQUAL_TO, "s2");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodeKeys.toArray(new String[3]));

        // nodes where values LIKE 's'
        constraint = propertyValue(STRING_PROP, LIKE, "s%");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 1, false, nodeKeys.toArray(new String[3]));

        constraint = propertyValue(STRING_PROP, LIKE, "%s%");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 1, false, nodeKeys.toArray(new String[3]));

        // nodes where values != s11
        constraint = propertyValue(STRING_PROP, NOT_EQUAL_TO, "s1");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(1));
    }
    
    @Test
    public void shouldSearchForMultiValueProperty() throws Exception {
        // validate that for an indexed multi-valued property the queries still work if *at least one* of the value matches
        // the constraint
        String nodeKey = UUID.randomUUID().toString();
        addValues(nodeKey, STRING_PROP, "a", "ab", "abc");

        Constraint equality = propertyValue(STRING_PROP, EQUAL_TO, "a");
        validateCardinality(equality, 1);
        validateFilterResults(equality, 1, false, nodeKey);
        
        equality = propertyValue(STRING_PROP, EQUAL_TO, "ab");
        validateCardinality(equality, 1);
        validateFilterResults(equality, 1, false, nodeKey);

        equality = propertyValue(STRING_PROP, EQUAL_TO, "abc");
        validateCardinality(equality, 1);
        validateFilterResults(equality, 1, false, nodeKey);

        equality = propertyValue(STRING_PROP, EQUAL_TO, "d");
        validateCardinality(equality, 0);
        
        Constraint length = length(STRING_PROP, EQUAL_TO, 1);
        validateCardinality(length, 1);
        validateFilterResults(length, 1, false, nodeKey); 
        
        length = length(STRING_PROP, EQUAL_TO, 2);
        validateCardinality(length, 1);
        validateFilterResults(length, 1, false, nodeKey);

        length = length(STRING_PROP, EQUAL_TO, 3);
        validateCardinality(length, 1);
        validateFilterResults(length, 1, false, nodeKey);
        
        length = length(STRING_PROP, EQUAL_TO, 4);
        validateCardinality(length, 0);
    }

    @Test
    public void shouldSearchForPathPropertyValueInComparisonConstraint() throws Exception {
        PathFactory pathFactory = valueFactories.getPathFactory();
        List<String> nodeKeys = indexNodes(PropertiesTestUtil.PATH_PROP, pathFactory.create("/a/b"), pathFactory.create("/a/d"),
                                           pathFactory.create("/b"));

        // nodes where value = /a/b
        Constraint constraint = propertyValue(PropertiesTestUtil.PATH_PROP, EQUAL_TO, "/a/b");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0));


        //nodes where value = /c
        constraint = propertyValue(PropertiesTestUtil.PATH_PROP, EQUAL_TO, "/c");
        validateCardinality(constraint, 0);
        validateFilterResults(constraint, 0, false);

        // nodes where value > /a/a
        constraint = propertyValue(PropertiesTestUtil.PATH_PROP, GREATER_THAN, "/a/a");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodeKeys.toArray(new String[3]));

        // nodes where value >= /a/b
        constraint = propertyValue(PropertiesTestUtil.PATH_PROP, GREATER_THAN_OR_EQUAL_TO, "/a/b");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodeKeys.toArray(new String[3]));

        // nodes where value < /z
        constraint = propertyValue(PropertiesTestUtil.PATH_PROP, LESS_THAN, "/z");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodeKeys.toArray(new String[3]));

        // nodes where value <= /b
        constraint = propertyValue(PropertiesTestUtil.PATH_PROP, LESS_THAN_OR_EQUAL_TO, "/b");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodeKeys.toArray(new String[3]));

        // nodes where values LIKE '/a/%'
        constraint = propertyValue(PropertiesTestUtil.PATH_PROP, LIKE, "/a/%");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0), nodeKeys.get(1));

        // nodes where values != '/a/b'
        constraint = propertyValue(PropertiesTestUtil.PATH_PROP, NOT_EQUAL_TO, "/a/b");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodeKeys.get(0), nodeKeys.get(1));
    }

    @Test
    public void shouldSearchForNamePropertyValueInComparisonConstraint() throws Exception {
        NameFactory nameFactory = valueFactories.getNameFactory();
        List<String> nodeKeys = indexNodes(NAME_PROP,
                                           nameFactory.create("jcr:name1"),
                                           nameFactory.create("jcr:name2"),
                                           nameFactory.create("mode:name1"));

        // nodes where value = jcr:name1
        Constraint constraint = propertyValue(NAME_PROP, EQUAL_TO, "jcr:name1");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0));


        //nodes where value = /jcr:name3
        constraint = propertyValue(NAME_PROP, EQUAL_TO, "jcr:name3");
        validateCardinality(constraint, 0);
        validateFilterResults(constraint, 0, false);

        // nodes where value > jcr:name
        constraint = propertyValue(NAME_PROP, GREATER_THAN, "jcr:name");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodeKeys.toArray(new String[3]));

        // nodes where value >= jcr:name1
        constraint = propertyValue(NAME_PROP, GREATER_THAN_OR_EQUAL_TO, "jcr:name1");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodeKeys.toArray(new String[3]));

        // nodes where value < "mode:name1"
        constraint = propertyValue(NAME_PROP, LESS_THAN, "mode:name1");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 2, false, nodeKeys.get(0));

        // nodes where value <= mode:name1
        constraint = propertyValue(NAME_PROP, LESS_THAN_OR_EQUAL_TO, "mode:name1");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodeKeys.get(0), nodeKeys.get(2));

        // nodes where values LIKE 'jcr:'
        constraint = propertyValue(NAME_PROP, LIKE, "jcr:%");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0), nodeKeys.get(1));

        // nodes where values != 'mode:name1'
        constraint = propertyValue(NAME_PROP, NOT_EQUAL_TO, "mode:name1");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodeKeys.get(1), nodeKeys.get(2));
    }

    @Test
    public void shouldSearchForDecimalPropertyValueInComparisonConstraint() throws Exception {
        List<String> nodesWithDecimalProp = indexNodes(DECIMAL_PROP,
                                                       BigDecimal.valueOf(1.1),
                                                       BigDecimal.valueOf(1.3),
                                                       BigDecimal.valueOf(1.5));

        // = 
        Constraint constraint = propertyValue(DECIMAL_PROP, EQUAL_TO, "1.1");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithDecimalProp.get(0));

        // >
        constraint = propertyValue(DECIMAL_PROP, GREATER_THAN, "1.1");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 1, false, nodesWithDecimalProp.get(1), nodesWithDecimalProp.get(2));

        // >=
        constraint = propertyValue(DECIMAL_PROP, GREATER_THAN_OR_EQUAL_TO, "1.1");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodesWithDecimalProp.toArray(new String[3]));

        // <
        constraint = propertyValue(DECIMAL_PROP, LESS_THAN, "1.3");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 2, false, nodesWithDecimalProp.get(0));

        // <=
        constraint = propertyValue(DECIMAL_PROP, LESS_THAN_OR_EQUAL_TO, "1.3");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithDecimalProp.get(0), nodesWithDecimalProp.get(1));

        // LIKE (should work because big decimals are stored as strings...)
        constraint = propertyValue(DECIMAL_PROP, LIKE, "1%");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 1, false, nodesWithDecimalProp.toArray(new String[3]));

        // !=
        constraint = propertyValue(DECIMAL_PROP, NOT_EQUAL_TO, "1.3");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithDecimalProp.get(0), nodesWithDecimalProp.get(2));
    }

    @Test
    public void shouldSearchForLongPropertyValueInComparisonConstraint() throws Exception {
        List<String> nodesWithLongProp = indexNodes(LONG_PROP, 101l, 103l, 105l);

        // = 
        Constraint constraint = propertyValue(LONG_PROP, EQUAL_TO, "103");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithLongProp.get(1));

        // >
        constraint = propertyValue(LONG_PROP, GREATER_THAN, "101");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 1, false, nodesWithLongProp.get(1), nodesWithLongProp.get(2));

        // >=
        constraint = propertyValue(LONG_PROP, GREATER_THAN_OR_EQUAL_TO, "101");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodesWithLongProp.toArray(new String[3]));

        // <
        constraint = propertyValue(LONG_PROP, LESS_THAN, "103");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 2, false, nodesWithLongProp.get(0));

        // <=
        constraint = propertyValue(LONG_PROP, LESS_THAN_OR_EQUAL_TO, "103");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithLongProp.get(0), nodesWithLongProp.get(1));

        // LIKE (compare as strings)
        constraint = propertyValue(LONG_PROP, LIKE, "10%");
        try {
            validateCardinality(constraint, 3);
            fail("Should not be able to use the LIKE operator on LONG values");
        } catch (ValueFormatException e) {
            //expected (can't search using LIKE)            
        }

        // !=
        constraint = propertyValue(LONG_PROP, NOT_EQUAL_TO, "103");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithLongProp.get(0), nodesWithLongProp.get(2));
    }

    @Test
    public void shouldSearchForDoublePropertyValueInComparisonConstraint() throws Exception {
        List<String> nodesWithDoubleProp = indexNodes(DOUBLE_PROP, 10d, 13d, 15d);

        // =
        Constraint constraint = propertyValue(DOUBLE_PROP, EQUAL_TO, "15");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithDoubleProp.get(2));

        // >
        constraint = propertyValue(DOUBLE_PROP, GREATER_THAN, "10");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 1, false, nodesWithDoubleProp.get(1), nodesWithDoubleProp.get(2));

        // >=
        constraint = propertyValue(DOUBLE_PROP, GREATER_THAN_OR_EQUAL_TO, "10");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodesWithDoubleProp.toArray(new String[3]));

        // <
        constraint = propertyValue(DOUBLE_PROP, LESS_THAN, "13");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 2, false, nodesWithDoubleProp.get(0));

        // <=
        constraint = propertyValue(DOUBLE_PROP, LESS_THAN_OR_EQUAL_TO, "13");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithDoubleProp.get(0), nodesWithDoubleProp.get(1));

        // LIKE (compare as strings)
        constraint = propertyValue(DOUBLE_PROP, LIKE, "10%");
        try {
            validateCardinality(constraint, 3);
            fail("Should not be able to use the LIKE operator on DOUBLE values");
        } catch (ValueFormatException e) {
            //expected (can't search using LIKE)            
        }

        // !=
        constraint = propertyValue(DOUBLE_PROP, NOT_EQUAL_TO, "13");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithDoubleProp.get(0), nodesWithDoubleProp.get(2));
    }

    @Test
    public void shouldSearchForDatePropertyValueInComparisonConstraint() throws Exception {
        List<String> nodesWithDateProp = indexNodes(DATE_PROP,
                                                    new ModeShapeDateTime("2015-10-13"),
                                                    new ModeShapeDateTime("2015-10-15"),
                                                    new ModeShapeDateTime("2015-10-17"));

        // =
        Constraint constraint = propertyValue(DATE_PROP, EQUAL_TO, "2015-10-17");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithDateProp.get(2));

        // >
        constraint = propertyValue(DATE_PROP, GREATER_THAN, "2015-10-13");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 1, false, nodesWithDateProp.get(1), nodesWithDateProp.get(2));

        // >=
        constraint = propertyValue(DATE_PROP, GREATER_THAN_OR_EQUAL_TO, "2015-10-13");
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodesWithDateProp.toArray(new String[3]));

        // <
        constraint = propertyValue(DATE_PROP, LESS_THAN, "2015-10-15");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 2, false, nodesWithDateProp.get(0));

        // <=
        constraint = propertyValue(DATE_PROP, LESS_THAN_OR_EQUAL_TO, "2015-10-15");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithDateProp.get(0), nodesWithDateProp.get(1));

        // LIKE (compare as strings)
        constraint = propertyValue(DATE_PROP, LIKE, "2015-10-15%");
        try {
            validateCardinality(constraint, 3);
            fail("Should not be able to use the LIKE operator on DOUBLE values");
        } catch (ValueFormatException e) {
            //expected (can't search using LIKE)            
        }

        // !=
        constraint = propertyValue(DATE_PROP, NOT_EQUAL_TO, "2015-10-15");
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithDateProp.get(0), nodesWithDateProp.get(2));
    }

    @Test
    public void shouldSearchForBooleanPropertyValueInComparisonConstraint() throws Exception {
        List<String> nodesWithBooleanProp = indexNodes(BOOLEAN_PROP, false, true, false);

        // =
        Constraint constraint = propertyValue(BOOLEAN_PROP, EQUAL_TO, true);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithBooleanProp.get(1));

        // >
        constraint = propertyValue(BOOLEAN_PROP, GREATER_THAN, false);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithBooleanProp.get(1));

        constraint = propertyValue(BOOLEAN_PROP, GREATER_THAN, true);
        validateCardinality(constraint, 0);

        // >=
        constraint = propertyValue(BOOLEAN_PROP, GREATER_THAN_OR_EQUAL_TO, false);
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodesWithBooleanProp.toArray(new String[3]));

        // <
        constraint = propertyValue(BOOLEAN_PROP, LESS_THAN, true);
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithBooleanProp.get(0), nodesWithBooleanProp.get(1));
        constraint = propertyValue(BOOLEAN_PROP, LESS_THAN, false);
        validateCardinality(constraint, 0);


        // <=
        constraint = propertyValue(BOOLEAN_PROP, LESS_THAN_OR_EQUAL_TO, true);
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 2, false, nodesWithBooleanProp.toArray(new String[3]));

        // LIKE (compare as strings)
        constraint = propertyValue(BOOLEAN_PROP, LIKE, "true%");
        try {
            validateCardinality(constraint, 3);
            fail("Should not be able to use the LIKE operator on DOUBLE values");
        } catch (LuceneIndexException e) {
            //expected (can't search using LIKE)            
        }

        // !=
        constraint = propertyValue(BOOLEAN_PROP, NOT_EQUAL_TO, true);
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodesWithBooleanProp.get(0), nodesWithBooleanProp.get(2));
    }

    @Test
    public void shouldSearchForReferencePropertyValueInComparisonConstraint() throws Exception {
        String ref1 = UUID.randomUUID().toString();
        String ref2 = UUID.randomUUID().toString();
        List<String> nodesWithRefProp = indexNodes(REF_PROP, ref1, ref2);

        // =
        Constraint constraint = propertyValue(REF_PROP, EQUAL_TO, ref1);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithRefProp.get(0));

        // !=
        constraint = propertyValue(REF_PROP, NOT_EQUAL_TO, ref1);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithRefProp.get(1));
    }

    @Test
    public void shouldSearchForReferenceValueInComparisonConstraint() throws Exception {
        String ref1 = UUID.randomUUID().toString();
        String ref2 = UUID.randomUUID().toString();
        List<String> nodesWithRefProp = indexNodes(REF_PROP, ref1, ref2);

        Constraint constraint = referenceValue(REF_PROP, EQUAL_TO, ref1, true, true);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithRefProp.get(0));
        constraint = referenceValue(null, EQUAL_TO, ref1, true, true);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodesWithRefProp.get(0));
    }

    @Test
    public void shouldSearchForLengthInComparisonConstraint() throws Exception {
        String stringVal = "1234";
        String stringNode = indexNodes(STRING_PROP, stringVal).get(0);

        NameFactory nameFactory = valueFactories.getNameFactory();
        StringFactory stringFactory = valueFactories.getStringFactory();
        String nameVal = stringFactory.create(nameFactory.create("jcr:value"));
        String nameNode = indexNodes(NAME_PROP, nameVal).get(0);

        PathFactory pathFactory = valueFactories.getPathFactory();
        String pathVal = stringFactory.create(pathFactory.create("/a/b"));
        String pathNode = indexNodes(PATH_PROP, pathVal).get(0);

        BigDecimal decimal = BigDecimal.valueOf(12.4);
        String decimalStr = stringFactory.create(decimal);
        String decimalNode = indexNodes(DECIMAL_PROP, decimal).get(0);

        long longValue = 1234l;
        String longStr = stringFactory.create(longValue);
        String longNode = indexNodes(LONG_PROP, longValue).get(0);

        double doubleValue = 12346.35d;
        String doubleStr = stringFactory.create(doubleValue);
        String doubleNode = indexNodes(DOUBLE_PROP, doubleValue).get(0);

        org.modeshape.jcr.api.value.DateTime dateValue = valueFactories.getDateFactory().create("2015-10-10");
        String dateStr = stringFactory.create(dateValue);
        String dateNode = indexNodes(DATE_PROP, dateValue).get(0);

        String ref = UUID.randomUUID().toString();
        String refNode = indexNodes(REF_PROP, ref).get(0);

        boolean booleanValue = true;
        String booleanStr= stringFactory.create(booleanValue);
        String booleanNode = indexNodes(BOOLEAN_PROP, booleanValue).get(0);

        BinaryValue binaryValue = valueFactories.getBinaryFactory().create("some_binary");
        String binaryNode = indexNodes(BINARY_PROP, binaryValue).get(0);

        assertLengthComparisonConstraint(STRING_PROP, stringNode, stringVal.length());
        assertLengthComparisonConstraint(NAME_PROP, nameNode, nameVal.length());
        assertLengthComparisonConstraint(PATH_PROP, pathNode, pathVal.length());
        assertLengthComparisonConstraint(DECIMAL_PROP, decimalNode, decimalStr.length());
        assertLengthComparisonConstraint(LONG_PROP, longNode, longStr.length());
        assertLengthComparisonConstraint(DOUBLE_PROP, doubleNode, doubleStr.length());
        assertLengthComparisonConstraint(DATE_PROP, dateNode, dateStr.length());
        assertLengthComparisonConstraint(REF_PROP, refNode, ref.length());
        assertLengthComparisonConstraint(BOOLEAN_PROP, booleanNode, booleanStr.length());
        assertLengthComparisonConstraint(BINARY_PROP, binaryNode, (int)binaryValue.getSize());
    }

    @Test
    public void shouldSearchForLowerAndUpperCasesInComparisonConstraint() throws Exception {
        List<String> nodeKeys = indexNodes(STRING_PROP, "A", "b");

        Constraint constraint = lowerCase(STRING_PROP, EQUAL_TO, "a");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0));

        constraint = lowerCase(STRING_PROP, EQUAL_TO, "b");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(1));

        constraint = upperCase(STRING_PROP, EQUAL_TO, "B");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(1));

        constraint = upperCase(STRING_PROP, EQUAL_TO, "A");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0));
    }

    @Test
    public void shouldSearchForAndConstraint() throws Exception {
        List<String> nodeKeys = indexNodes(LONG_PROP, 1l, 3l, 5l);
        // >1 && <5
        Constraint constraint = and(propertyValue(LONG_PROP, GREATER_THAN, 1),
                                    propertyValue(LONG_PROP, LESS_THAN, 5));
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(1));
    }

    @Test
    public void shouldSearchForOrConstraint() throws Exception {
        List<String> nodeKeys = indexNodes(LONG_PROP, 1l, 3l, 5l);
        //>1 || <5
        Constraint constraint = or(propertyValue(LONG_PROP, GREATER_THAN, 1),
                                   propertyValue(LONG_PROP, LESS_THAN, 5));
        validateCardinality(constraint, 3);
        validateFilterResults(constraint, 3, false, nodeKeys.toArray(new String[nodeKeys.size()]));
    }

    @Test
    public void shouldSearchForNotConstraint() throws Exception {
        List<String> nodeKeys = indexNodes(LONG_PROP, 1l, 3l, 5l);
        // >1
        Constraint constraint = not(propertyValue(LONG_PROP, GREATER_THAN, 1));
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0));
    }

    @Test
    public void shouldSearchForSetConstraint() throws Exception {
        List<String> nodeKeys = indexNodes(LONG_PROP, 1l, 3l, 5l);
        // IN (1,2,3,4)
        Constraint constraint = set(LONG_PROP, 1, 2, 3, 4);
        validateCardinality(constraint, 2);
        validateFilterResults(constraint, 2, false, nodeKeys.get(0), nodeKeys.get(1));
    }

    @Test
    public void shouldSearchForPropertyExistence() throws Exception {
        String longNode = indexNodes(LONG_PROP, 1l).get(0);
        Constraint constraint = propertyExistence(LONG_PROP);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, longNode);

        String stringNode = indexNodes(STRING_PROP, "a").get(0);
        constraint = propertyExistence(STRING_PROP);
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, stringNode);

        constraint = propertyExistence(DATE_PROP);
        validateCardinality(constraint, 0);
    }

    @Test
    public void shouldSearchForBetweenConstraint() throws Exception {
        List<String> nodeKeys = indexNodes(LONG_PROP, 1l, 3l);
        // (0,2)
        Constraint between = between(LONG_PROP, 0, false, 2, false);
        validateCardinality(between, 1);
        validateFilterResults(between, 1, false, nodeKeys.get(0));
        // [1,2)
        between = between(LONG_PROP, 1, true, 2, false);
        validateCardinality(between, 1);
        validateFilterResults(between, 1, false, nodeKeys.get(0));
        // (1,2)
        between = between(LONG_PROP, 1, false, 2, false);
        validateCardinality(between, 0);
        // (2,4)
        between = between(LONG_PROP, 2, false, 4, false);
        validateCardinality(between, 1);
        validateFilterResults(between, 1, false, nodeKeys.get(1));
        // (3,5)
        between = between(LONG_PROP, 3, false, 5, false);
        validateCardinality(between, 0);
    }

    @Test
    public void shouldSearchForRelikeConstraint() throws Exception {
        String node = indexNodes(STRING_PROP, "string%").get(0);
        Constraint relike = relike("string-1", STRING_PROP);
        validateCardinality(relike, 1);
        validateFilterResults(relike, 1, false, node);
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void shouldSupportFTSConstraint() throws Exception {
        validateCardinality(fullTextSearch(STRING_PROP, "some string"), 1);                        
    }
    
    @Test
    @Ignore("perf test")
    public void stringSearchPerfTest() throws Exception {
        int nodeCount = 500000;
        List<String> nodeKeys = new ArrayList<>(nodeCount);
        List<String> evenKeys = new ArrayList<>(nodeCount / 2);
        for (int i = 0; i < nodeCount; i++) {
            boolean even = i % 2 == 0;
            String value = "string_" + (even ? "even" : "odd");
            List<String> keys = indexNodes(STRING_PROP, value);
            nodeKeys.addAll(keys);
            if (even) {
                evenKeys.addAll(keys);    
            }
        }
        index.commit();
        assertEquals(nodeCount, index.estimateTotalCount());
        long start = System.nanoTime();        
        Constraint constraint = propertyValue(STRING_PROP, Operator.EQUAL_TO, "string_even");
        validateCardinality(constraint, nodeCount / 2);
        validateFilterResults(constraint, 1000, false, evenKeys.toArray(new String[evenKeys.size()]));
        long duration = System.nanoTime() - start;
        long searchTime = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
        System.out.println(Thread.currentThread().getName() + ": (" + index.getName() + ") Total time to search " + nodeKeys.size() + " nodes: " + searchTime/1000d + " seconds");
    }

    @Test
    @FixFor( "MODE-2567" )
    public void shouldSearchForLikeConstraintContainingSpaceAmpersand() throws Exception {
        List<String> nodeKeys = indexNodes(STRING_PROP, "Law & Order - S01E01");

        // no leading or trailing space
        Constraint constraint = propertyValue(STRING_PROP, LIKE, "%&%");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0));

        // leading space
        constraint = propertyValue(STRING_PROP, LIKE, "% &%");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0));

        // trailing space
        constraint = propertyValue(STRING_PROP, LIKE, "%& %");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0));

        // part of a larger search string
        constraint = propertyValue(STRING_PROP, LIKE, "%Law & Order%");
        validateCardinality(constraint, 1);
        validateFilterResults(constraint, 1, false, nodeKeys.get(0));
    }
}
